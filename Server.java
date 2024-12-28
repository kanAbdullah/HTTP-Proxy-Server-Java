
import java.io.*;
import java.net.*;

class Server {

    public static void main(String[] args) {
        ServerSocket server = null;

        try {
            // Scanner sc = new Scanner(System.in);
            // System.out.println("Enter the port number: ");
            // int portNumber = sc.nextInt();
            // sc.close();

            server = new ServerSocket(8080);
            server.setReuseAddress(true);

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
            try (
                    OutputStream out = clientSocket.getOutputStream(); BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String request;
                while ((request = in.readLine()) != null) {
                    System.out.printf("Received from client: %s\n", request);

                    try {
                        // parse the request
                        int documentSize = parseRequest(request);
                        String document = createDocument(documentSize);
                        out.write(document.getBytes());
                        out.flush();
                    } catch (BadRequestException e) {
                        // Bad request durumunda hata mesajı gönder
                        sendResponse(out, "400 Bad Request");
                    } catch (NotImplementedException e) {
                        // Diğer hatalar için 501 Not Implemented

                        sendResponse(out, "501 Not Implemented");
                    } catch (Exception e) {
                        // Diğer hatalar için 500 Internal Server Error
                        sendResponse(out, "500 Internal Server Error");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Özel hata sınıfı
        private static class BadRequestException extends Exception {

            public BadRequestException(String message) {
                super(message);
            }
        }
        // Özel hata sınıfı

        private static class NotImplementedException extends Exception {

            public NotImplementedException(String message) {
                super(message);
            }
        }

        private void sendResponse(OutputStream out, String status) throws IOException {
            String response = String.format("HTTP/1.1 %s\r\n", status
            );

            out.write(response.getBytes());
            out.flush();
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
                if (size <= 100 || size > 20000) {
                    throw new BadRequestException("Document size is not valid");
                }

                return size;
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid document size format");
            } catch (IndexOutOfBoundsException e) {
                throw new BadRequestException("Invalid request format");
            }
        }

        public String createDocument(int size) {
            StringBuilder document = new StringBuilder();
            String documentHeader = "Response Document";
            String responsePath = "./House-M.D.-pilot-script.txt";

            try (BufferedReader reader = new BufferedReader(new FileReader(responsePath))) {
                char[] buffer = new char[size - 78];
                int charsRead = reader.read(buffer, 0, size - 78);

                if (charsRead > 0) {
                    document.append(new String(buffer, 0, charsRead));
                    System.out.println("Read text length: " + charsRead);
                } else {
                    document.append("No content available");
                }
            } catch (IOException e) {
                e.printStackTrace();
                document.append("Error reading file");
            }

            // HTML formatında dökümanı oluştur
            return "<HTML>\n<HEAD>\n<TITLE>" + documentHeader
                    + "</TITLE>\n</HEAD>\n<BODY>\n" + document.toString()
                    + "\n</BODY>\n</HTML>";
        }

    }
}
