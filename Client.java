
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
                System.out.print("Enter request: ");
                userInput = scanner.nextLine();

                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }

                String formattedRequest = formatRequest(userInput);
                System.out.println("Sending request: " + formattedRequest);
                out.print(formattedRequest);
                out.flush();

                // Read and process the response
                ResponseData response = readResponse(in);
                if (response != null) {
                    System.out.println("\nResponse from server:");
                    String fullResponse = response.toString();
                    System.out.println(fullResponse);
                    
                    if (response.isSuccess()) {
                        saveResponseToFile(fullResponse);
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

    private static class ResponseData {

        String statusCode;
        String headers;
        int contentLength;
        String body;

        boolean isSuccess() {
            return statusCode != null && statusCode.startsWith("200");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 ").append(statusCode).append("\n");
            sb.append(headers);
            sb.append("\n");
            sb.append(body);
            return sb.toString();
        }
    }

    private static String formatRequest(String userInput) {
        String formattedRequest;

        if (userInput.matches("\\d+")) {
            formattedRequest = String.format("GET http://%s:%d/%s HTTP/1.1",
                    WEB_SERVER_HOST, WEB_SERVER_PORT, userInput);
        } else if (userInput.startsWith("GET /")) {
            formattedRequest = userInput.replace("GET /",
                    String.format("GET http://%s:%d/", WEB_SERVER_HOST, WEB_SERVER_PORT));
        } else {
            formattedRequest = userInput;
        }

        StringBuilder fullRequest = new StringBuilder();
        fullRequest.append(formattedRequest).append("\r\n");
        fullRequest.append("Host: ").append(WEB_SERVER_HOST).append(":").append(WEB_SERVER_PORT).append("\r\n");
        fullRequest.append("User-Agent: JavaClient\r\n");
        fullRequest.append("Accept: */*\r\n");
        fullRequest.append("Connection: keep-alive\r\n");
        fullRequest.append("\r\n");

        return fullRequest.toString();
    }

    private static ResponseData readResponse(BufferedReader in) throws IOException {
        ResponseData response = new ResponseData();

        // Read the HTTP response line
        String responseLine = in.readLine();
        if (responseLine == null) {
            return null;
        }

        // Parse status line
        String[] statusParts = responseLine.split(" ", 3);
        if (statusParts.length >= 2) {
            response.statusCode = statusParts[1];
            System.out.println("Response Status Code: " + response.statusCode);
        }

        // Read headers
        StringBuilder headersBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            headersBuilder.append(line).append("\n");
            if (line.toLowerCase().startsWith("content-length:")) {
                response.contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }
        response.headers = headersBuilder.toString();

        // Read body based on content-length
        if (response.contentLength > 0) {
            char[] buffer = new char[response.contentLength];
            int totalRead = 0;
            while (totalRead < response.contentLength) {
                int read = in.read(buffer, totalRead, response.contentLength - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            response.body = new String(buffer, 0, totalRead);
        } else {
            // If no content-length, read until connection closes
            StringBuilder bodyBuilder = new StringBuilder();
            while ((line = in.readLine()) != null) {
                bodyBuilder.append(line).append("\n");
            }
            response.body = bodyBuilder.toString();
        }

        return response;
    }

    private static void saveResponseToFile(String content) {
        if (content == null || content.trim().isEmpty()) {
            System.out.println("No content to save");
            return;
        }

        String filename = "response_" + System.currentTimeMillis() + ".html";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(content);
            writer.flush();
            System.out.println("Response saved to file: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to save response to file: " + e.getMessage());
        }
    }
}
