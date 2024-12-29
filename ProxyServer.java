
import java.io.*;
import java.net.*;

public class ProxyServer {

    private static final int PORT = 8888;
    private static final int MAX_URI_LENGTH = 9999;

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
                    InputStream clientIn = clientSocket.getInputStream();
                    OutputStream clientOut = clientSocket.getOutputStream();
                    BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientIn))) {

                while (!clientSocket.isClosed()) {
                    String requestLine = clientReader.readLine();

                    if (requestLine == null) {
                        break;
                    }

                    if (requestLine.isEmpty()) {
                        continue;
                    }

                    if (requestLine.startsWith("GET")) {
                        handleGetRequest(requestLine, clientReader, clientOut);
                    } else {
                        sendErrorResponse(clientOut, "501 Not Implemented", "Method Not Allowed");
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

        private void handleGetRequest(String requestLine, BufferedReader clientReader, OutputStream clientOut)
                throws IOException {
            String uri = extractUriFromRequestLine(requestLine);

            if (uri.length() > MAX_URI_LENGTH) {
                sendErrorResponse(clientOut, "414 Request-URI Too Long",
                        "URI length exceeds the limit of " + MAX_URI_LENGTH);
                return;
            }

            forwardRequestToServer(requestLine, clientReader, clientOut, uri);
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

        private void forwardRequestToServer(String requestLine, BufferedReader clientReader, OutputStream clientOut,
                String uri) throws IOException {
            try (Socket serverSocket = new Socket("localhost", 8080);
                    OutputStream serverOut = serverSocket.getOutputStream();
                    InputStream serverIn = serverSocket.getInputStream()) {

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
                System.out.println("Forwarded additional headers to web server");
                serverOut.flush();

                // Relay response from web server to client
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = serverIn.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                }
                System.out.println("Relayed response from web server to client");
                clientOut.flush();
            } catch (ConnectException e) {
                sendErrorResponse(clientOut, "404 Not Found", "Web Server Not Found");
            }
        }

        private void sendErrorResponse(OutputStream out, String status, String body) throws IOException {
            String response = String.format(
                    "HTTP/1.1 %s\r\nContent-Type: text/html\r\nContent-Length: %d\r\n\r\n%s",
                    status, body.getBytes().length, body);
            out.write(response.getBytes());
            out.flush();
        }
    }
}
