
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

class Server {

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
            try {
                if (server != null) {
                    server.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
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
            try (
                    OutputStream out = clientSocket.getOutputStream(); BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String requestLine = in.readLine(); // İlk satırı oku
                System.out.printf("Received from client: %s\n", requestLine); // Gelen isteği yazdır

                // // Başlıkları atla (boş bir satıra kadar oku)
                // String headerLine;
                // while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                //     System.out.printf("Header: %s\n", headerLine);
                // }
                try {
                    // İstek işleme
                    int documentSize = parseRequest(requestLine);
                    String document = createDocument(documentSize);
                    sendResponse(out, "200 OK", document);
                    System.out.println("Sent to client:\n" + document); // Gönderilen yanıtı yazdır
                } catch (BadRequestException e) {
                    sendResponse(out, "400 Bad Request", "400 Invalid Request");
                    System.out.println("Sent to client: 400 Bad Request");
                } catch (NotImplementedException e) {
                    sendResponse(out, "501 Not Implemented", "501 Method Not Allowed");
                    System.out.println("Sent to client: 501 Not Implemented");
                } catch (Exception e) {
                    sendResponse(out, "500 Internal Server Error", "500 Internal Server Error");
                    System.out.println("Sent to client: 500 Internal Server Error");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
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
            try {
                String[] parts = req.split(" ");
                // HTTP format kontrolü
                if (parts.length != 3 || !parts[0].equals("GET") || !parts[2].startsWith("HTTP/")) {

                    if (parts[0].equals("POST") || parts[0].equals("PUT") || parts[0].equals("DELETE")) {
                        throw new NotImplementedException("Method not allowed");
                    }
                    throw new BadRequestException("Invalid HTTP request format");
                }

                // Path kontrolü
                String sizePart = parts[1].substring(1); // Remove leading '/'
                int size = Integer.parseInt(sizePart);

                // Size sınırlamaları
                if (size < 100 || size > 20000) {
                    throw new BadRequestException("Document size is not valid");
                }

                return size;
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid document size format");
            } catch (IndexOutOfBoundsException e) {
                throw new BadRequestException("Invalid request format");
            }
        }

        public String createDocument(int requestedSize) {
            StringBuilder document = new StringBuilder();
            String documentHeader = "Response Document";
            String htmlStart = "<HTML>\n<HEAD>\n<TITLE>" + documentHeader + "</TITLE>\n</HEAD>\n<BODY>\n";
            String htmlEnd = "\n</BODY>\n</HTML>";
            
            String content = "";
            String responsePath = "./House-M.D.-pilot-script.txt";
            
            try (BufferedReader reader = new BufferedReader(new FileReader(responsePath))) {
                // İstenen boyut kadar oku
                char[] buffer = new char[requestedSize];
                int charsRead = reader.read(buffer);
                if (charsRead > 0) {
                    content = new String(buffer, 0, charsRead);
                }
            } catch (IOException e) {
                content = "Error reading file: " + e.getMessage();
            }
            
            // Tam HTML dökümanını oluştur
            document.append(htmlStart)
                   .append(content)
                   .append(htmlEnd);
                   
            String finalDocument = document.toString();
            
            // Debug için boyut bilgisini yazdır
            System.out.println("Created document with size: " + finalDocument.getBytes(StandardCharsets.UTF_8).length);
            
            return finalDocument;
        }

        private void sendResponse(OutputStream out, String status, String body) throws IOException {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            int contentLength = bodyBytes.length;
            
            StringBuilder headerBuilder = new StringBuilder();
            headerBuilder.append(String.format("HTTP/1.1 %s\r\n", status));
            headerBuilder.append("Content-Type: text/html\r\n");
            headerBuilder.append(String.format("Content-Length: %d\r\n", contentLength));
            headerBuilder.append("\r\n");
            
            // Headers'ı ve body'yi ayrı ayrı yazalım
            out.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();
            
            System.out.println("Sent response with Content-Length: " + contentLength);
        }
    }
}
