
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8888); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); Scanner sc = new Scanner(System.in)) {

            String line = null;
            while (!"exit".equalsIgnoreCase(line)) {
                System.out.print("Enter request (e.g., GET /100 HTTP/1.1): ");
                line = sc.nextLine();
                out.println(line);

                StringBuilder response = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    response.append(buffer, 0, bytesRead);
                    if (response.toString().toLowerCase().contains("</html>")) {
                        break;
                    }
                }

                System.out.println("Response from server:\n" + response);
                saveFile(response.toString());
            
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveFile(String receivedMessage) {
        try (FileWriter messageWriter = new FileWriter("receivedMessage.html")) {
            messageWriter.write(receivedMessage);
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
