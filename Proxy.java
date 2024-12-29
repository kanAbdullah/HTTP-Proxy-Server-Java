
import java.io.*;
import java.net.*;

public class Proxy {

    private static final int PORT = 8888;
    private static final int MAX_REQUESTED_SIZE = 9999;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Proxy Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ProxyHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ProxyHandler implements Runnable {

        private final Socket clientSocket;

        public ProxyHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    InputStream clientIn = clientSocket.getInputStream(); OutputStream clientOut = clientSocket.getOutputStream(); BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientIn))) {

                while (!clientSocket.isClosed()) {
                    String requestLine = clientReader.readLine();

                    if (requestLine == null) {
                        break;
                    }

                    if (requestLine.isEmpty()) {
                        continue;
                    }

                    try {
                        handleGetRequest(requestLine, clientReader, clientOut);
                    } catch (CustomException e) {
                        sendErrorResponse(clientOut, e.getStatusCode(), e.getMessage(), clientReader);

                    }
                }
            } catch (SocketException e) {
                System.err.println("Client disconnected: " + e.getMessage());
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

        public class CustomException extends Exception {

            private final String statusCode;

            public CustomException(String statusCode, String message) {
                super(message);
                this.statusCode = statusCode;
            }

            public String getStatusCode() {
                return statusCode;
            }
        }

        private void handleGetRequest(String requestLine, BufferedReader clientReader, OutputStream clientOut)
                throws IOException, CustomException {
            String uri = extractUriFromRequestLine(requestLine);
            // URI'den başındaki "/" karakterini kaldır
            if (uri.startsWith("/")) {
                uri = uri.substring(1);
            }
            // URI'nin bir sayı olup olmadığını kontrol edin
            int requestedSize;
            try {
                System.out.println("Request Line.:" + requestLine);
                System.out.println("Requested size: " + uri);
                requestedSize = Integer.parseInt(uri.trim());
            } catch (NumberFormatException e) {
                throw new CustomException("400 Bad Request", "Invalid URI format, expected a numeric value.");
            }

            if (requestedSize > MAX_REQUESTED_SIZE) {
                throw new CustomException("414 Request-URI Too Long", "Requested file size exceeds the limit of " + MAX_REQUESTED_SIZE);
            }

            forwardRequestToServer(requestLine, clientReader, clientOut);
        }

        private String extractUriFromRequestLine(String requestLine) {
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return "";
            }

            String absoluteUri = parts[1];
            if (absoluteUri.startsWith("http://")) {
                int startIndex = absoluteUri.indexOf('/', 7); // Skip "http://"

                return startIndex != -1 ? absoluteUri.substring(startIndex) : "/";
            }

            return absoluteUri; // Default to relative URI
        }

        private void forwardRequestToServer(String requestLine, BufferedReader clientReader, OutputStream clientOut) throws IOException {
            try (Socket serverSocket = new Socket("localhost", 8080); OutputStream serverOut = serverSocket.getOutputStream(); InputStream serverIn = serverSocket.getInputStream()) {

                // Rewrite the request line to use the relative URI
                String rewrittenRequestLine = requestLine.replaceFirst("http://[^/]+", "");
                serverOut.write((rewrittenRequestLine + "\r\n").getBytes());
                System.out.println("Forwarded request to web server: " + rewrittenRequestLine);
                // Forward additional headers
                String headerLine;
                while ((headerLine = clientReader.readLine()) != null && !headerLine.isEmpty()) {
                    serverOut.write((headerLine + "\r\n").getBytes());
                }
                serverOut.write("\r\n".getBytes());

                serverOut.flush();

                // Relay response from web server to client
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = serverIn.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                }
                System.out.println("Relayed response from web server to client.\n");
                clientOut.flush();
            } catch (ConnectException e) {
                sendErrorResponse(clientOut, "404 Not Found", "Web Server Not Found", clientReader);

            }
        }

        private void sendErrorResponse(OutputStream out, String status, String body, BufferedReader clientReader) throws IOException {
            System.out.println("Sending error response to client: " + status);
            String response = String.format(
                    "HTTP/1.1 %s\r\nContent-Type: text/html\r\nContent-Length: %d\r\n\r\n%s",
                    status, body.getBytes().length, body);
            // Kalan verileri atlamak için okuma işlemini temizle
            while (clientReader.ready()) {
                clientReader.readLine(); // Satırları okuyarak geç
            }

            out.write(response.getBytes());
            out.flush();
        }

    }
}
