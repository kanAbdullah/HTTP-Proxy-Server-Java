
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080)) {
            // writing to server 
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            // reading from server 
            InputStream inputStream = socket.getInputStream();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(inputStream));

            Scanner sc = new Scanner(System.in);
            String line = null;

            while (!"exit".equalsIgnoreCase(line)) {
                // reading from user 
                line = sc.nextLine();

                // sending the user input to server 
                out.println(line);
                out.flush();

                // Reading the entire response from server
                StringBuilder response = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;

                // Keep reading until we've got all the data
                while ((bytesRead = in.read(buffer)) != -1) {
                    response.append(buffer, 0, bytesRead);

                    // Check if we've received the complete HTML document
                    if (response.toString().contains("</HTML>")) {
                        break;
                    } else if (response.toString().contains("HTTP/1.1 400 Bad Request")) {
                        break;
                    } else if (response.toString().contains("HTTP/1.1 501 Not Implemented")) {
                        break;
                    } else if (response.toString().contains("HTTP/1.1 500 Internal Server Error")) {
                        break;

                    }
                }

                String completeMessage = response.toString();
                String http400 = "HTTP/1.1 400 Bad Request";
                String http501 = "HTTP/1.1 501 Not Implemented";
                String http500 = "HTTP/1.1 500 Internal Server Error";
                if (completeMessage.contains(http400)) {
                    System.out.println(http400);
                } else if (completeMessage.contains(http501)) {
                    System.out.println(http501);
                } else if (completeMessage.contains(http500)) {
                    System.out.println(http500);
                } else {
                    System.out.println("HTTP/1.1 200 OK");
                    // Save the complete message
                    saveFile(completeMessage);
                }
            }

            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveFile(String receivedMessage) {
        try (FileWriter messageWriter = new FileWriter("receivedMessage.html")) {  // .html uzantısı kullanıyoruz çünkü HTML içerik
            messageWriter.write(receivedMessage);
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
