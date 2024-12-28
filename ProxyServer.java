import java.io.*;
import java.net.*;

public class ProxyServer {
    static final int PROXY_PORT = 8888;
    static final String WEB_SERVER_HOST = "127.0.0.1";
    static final int WEB_SERVER_PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PROXY_PORT)) {
            System.out.println("Proxy server is running on port " + PROXY_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ProxyHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ProxyHandler implements Runnable {
    private Socket clientSocket;

    public ProxyHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
        ) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientInput));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientOutput));

            String requestLine = reader.readLine();
            if (requestLine == null || !requestLine.startsWith("GET ")) {
                writer.write("HTTP/1.0 400 Bad Request\r\n\r\n");
                writer.flush();
                return;
            }

            String[] requestParts = requestLine.split(" ");
            String uri = requestParts[1];

            if (uri.length() > 9999) {
                writer.write("HTTP/1.0 414 Request-URI Too Long\r\n\r\n");
                writer.flush();
                return;
            }

            // Translate absolute URI to relative URI for web server
            if (uri.startsWith("http://")) {
                uri = uri.substring(uri.indexOf('/', 7));
            }

            try (Socket webServerSocket = new Socket(ProxyServer.WEB_SERVER_HOST, ProxyServer.WEB_SERVER_PORT)) {
                // Forward request to web server
                BufferedWriter webServerWriter = new BufferedWriter(new OutputStreamWriter(webServerSocket.getOutputStream()));
                webServerWriter.write("GET " + uri + " HTTP/1.0\r\n");
                webServerWriter.write("Host: " + ProxyServer.WEB_SERVER_HOST + "\r\n\r\n");
                webServerWriter.flush();

                // Relay response from web server to client
                InputStream webServerInput = webServerSocket.getInputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = webServerInput.read(buffer)) != -1) {
                    clientOutput.write(buffer, 0, bytesRead);
                }
                clientOutput.flush();
            } catch (IOException e) {
                writer.write("HTTP/1.0 404 Not Found\r\n\r\n");
                writer.flush();
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
}
