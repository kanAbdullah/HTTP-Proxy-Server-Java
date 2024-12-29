
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = 8888;
    private static final String WEB_SERVER_HOST = "localhost";
    private static final int WEB_SERVER_PORT = 8080;

    public static void main(String[] args) {
        try (Socket socket = new Socket(PROXY_HOST, PROXY_PORT); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to proxy server at " + PROXY_HOST + ":" + PROXY_PORT);
            System.out.println("Type 'exit' to quit");

            String userInput;
            while (true) {
                System.out.print("Enter request (e.g., GET http://localhost:8080/500 HTTP/1.0): ");
                userInput = scanner.nextLine();

                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }

                // Format the request if user enters a simplified version
                String formattedRequest = formatRequest(userInput);
                System.out.println("Sending request: " + formattedRequest);
                out.print(formattedRequest);
                out.flush();

                // Read and process the response
                String response = readResponse(in);
                if (response != null && !response.isEmpty()) {
                    System.out.println("\nResponse from server:");
                    System.out.println(response);

                    // Save response to file if it contains HTML content
                    if (response.contains("<HTML>") || response.contains("<html>")) {
                        saveResponseToFile(response);
                    }
                }
            }

        } catch (ConnectException e) {
            System.err.println("Failed to connect to the proxy server. Make sure it's running.");
        } catch (IOException e) {
            System.err.println("Error during communication: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatRequest(String userInput) {
        String formattedRequest;

        // If the input is just a number, format it as a proper HTTP request
        if (userInput.matches("\\d+")) {
            formattedRequest = String.format("GET http://%s:%d/%s HTTP/1.1",
                    WEB_SERVER_HOST, WEB_SERVER_PORT, userInput);
        } // If it's a relative path, convert to absolute URL
        else if (userInput.startsWith("GET /")) {
            formattedRequest = userInput.replace("GET /",
                    String.format("GET http://%s:%d/", WEB_SERVER_HOST, WEB_SERVER_PORT));
        } else {
            formattedRequest = userInput;
        }

        // Add necessary HTTP headers
        StringBuilder fullRequest = new StringBuilder();
        fullRequest.append(formattedRequest).append("\r\n");
        fullRequest.append("Host: ").append(WEB_SERVER_HOST).append(":").append(WEB_SERVER_PORT).append("\r\n");
        fullRequest.append("User-Agent: JavaClient\r\n");
        fullRequest.append("Accept: */*\r\n");
        fullRequest.append("Connection: close\r\n");
        fullRequest.append("\r\n"); // Empty line to indicate end of headers

        return fullRequest.toString();
    }

    private static String readResponse(BufferedReader in) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        int contentLength = -1;
        boolean isChunked = false;

        // Read headers
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            response.append(line).append("\r\n");
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
            if (line.toLowerCase().startsWith("transfer-encoding: chunked")) {
                isChunked = true;
            }
        }
        response.append("\r\n");

        // Read body based on Content-Length or until </html> is found
        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = in.read(buffer, totalRead, contentLength - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            response.append(buffer);
        } else {
            // Fallback to reading until </html> or empty line
            String body;
            while ((body = in.readLine()) != null) {
                response.append(body).append("\r\n");
                if (body.toLowerCase().contains("</html>")) {
                    break;
                }
            }
        }

        return response.toString();
    }

    private static void saveResponseToFile(String response) {
        String filename = "response_" + System.currentTimeMillis() + ".html";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(response);
            System.out.println("Response saved to file: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to save response to file: " + e.getMessage());
        }
    }
}
