import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

class Server {
    // Main method remains the same
    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(8080);
            server.setReuseAddress(true);
            System.err.println("Server started on port 8080");
            while (true) {
                Socket client = server.accept();
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());
                ClientHandler clientSock = new ClientHandler(client);
                new Thread(clientSock).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (OutputStream out = clientSocket.getOutputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String requestLine = in.readLine();
                System.out.printf("Received from client: %s\n", requestLine);

                try {
                    int documentSize = parseRequest(requestLine);
                    String document = createDocument(documentSize);
                    sendResponse(out, "200 OK", document);
                    System.out.println("Sent to client:\n" + document);
                } catch (BadRequestException e) {
                    sendResponse(out, "400 Bad Request", "<HTML><BODY>400 Bad Request</BODY></HTML>");
                } catch (NotImplementedException e) {
                    sendResponse(out, "501 Not Implemented", "<HTML><BODY>501 Method Not Implemented</BODY></HTML>");
                } catch (Exception e) {
                    sendResponse(out, "500 Internal Server Error",
                            "<HTML><BODY>500 Internal Server Error</BODY></HTML>");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private static class BadRequestException extends Exception {
            public BadRequestException(String message) {
                super(message);
            }
        }

        private static class NotImplementedException extends Exception {
            public NotImplementedException(String message) {
                super(message);
            }
        }

        public int parseRequest(String req) throws BadRequestException, NotImplementedException {
            if (req == null) {
                throw new BadRequestException("Null request");
            }

            String[] parts = req.split(" ");
            if (parts.length != 3) {
                throw new BadRequestException("Invalid request format");
            }

            if (!parts[0].equals("GET")) {
                if (parts[0].equals("POST") || parts[0].equals("PUT") || parts[0].equals("DELETE")) {
                    throw new NotImplementedException("Method not allowed");
                }
                throw new BadRequestException("Invalid method");
            }

            if (!parts[2].startsWith("HTTP/")) {
                throw new BadRequestException("Invalid HTTP version");
            }

            try {
                String sizePart = parts[1].substring(1); // Remove leading '/'
                int size = Integer.parseInt(sizePart);
                if (size < 100 || size > 20000) {
                    throw new BadRequestException("Size out of range");
                }
                return size;
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid size format");
            }
        }

        public String createDocument(int requestedSize) {
            String htmlPrefix = "<HTML>\n<HEAD>\n<TITLE>Response</TITLE>\n</HEAD>\n<BODY>\n";
            String htmlSuffix = "\n</BODY>\n</HTML>";

            // Calculate how many bytes we can use for content
            byte[] prefixBytes = htmlPrefix.getBytes(StandardCharsets.UTF_8);
            byte[] suffixBytes = htmlSuffix.getBytes(StandardCharsets.UTF_8);
            int fixedPartsSize = prefixBytes.length + suffixBytes.length;

            // Calculate how many content bytes we need
            int contentBytesNeeded = requestedSize - fixedPartsSize;

            if (contentBytesNeeded < 0) {
                // If we can't fit the HTML structure, we'll generate a minimal document
                return generateExactSizeDocument(requestedSize);
            }

            // Generate content that will give us exactly the right number of bytes
            StringBuilder content = new StringBuilder();
            String baseContent = "This is line number ";
            int lineNumber = 1;

            while (true) {
                String nextLine = baseContent + lineNumber + ".\n";
                byte[] lineBytes = nextLine.getBytes(StandardCharsets.UTF_8);

                if (content.toString().getBytes(StandardCharsets.UTF_8).length
                        + lineBytes.length > contentBytesNeeded) {
                    // We would exceed our target size with this line, so stop
                    break;
                }

                content.append(nextLine);
                lineNumber++;
            }

            // Add padding if necessary
            byte[] currentBytes = (htmlPrefix + content + htmlSuffix).getBytes(StandardCharsets.UTF_8);
            while (currentBytes.length < requestedSize) {
                content.append(" ");
                currentBytes = (htmlPrefix + content + htmlSuffix).getBytes(StandardCharsets.UTF_8);
            }

            return htmlPrefix + content + htmlSuffix;
        }

        private String generateExactSizeDocument(int size) {
            StringBuilder doc = new StringBuilder("<HTML><BODY>");
            while (doc.toString().getBytes(StandardCharsets.UTF_8).length < size - 14) { // 14 is "</BODY></HTML>"
                doc.append("X");
            }
            doc.append("</BODY></HTML>");
            return doc.toString();
        }

        private void sendResponse(OutputStream out, String status, String body) throws IOException {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            StringBuilder response = new StringBuilder();
            response.append("HTTP/1.1 ").append(status).append("\r\n");
            response.append("Content-Type: text/html\r\n");
            response.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
            response.append("\r\n");

            out.write(response.toString().getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();

            System.out.println("Response sent. Content-Length: " + bodyBytes.length);
        }
    }
}