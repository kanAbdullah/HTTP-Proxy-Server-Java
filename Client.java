
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); Scanner sc = new Scanner(System.in)) {

            String line = null;

            while (!"exit".equalsIgnoreCase(line)) {
                line = sc.nextLine(); // Kullanıcıdan veri al
                out.println(line); // Veriyi sunucuya gönder

                StringBuilder response = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;
                String http200 = "HTTP/1.1 200 OK";
                String http400 = "HTTP/1.1 400 Bad Request";
                String http501 = "HTTP/1.1 501 Not Implemented";
                String http500 = "HTTP/1.1 500 Internal Server Error";
                String receivedMessage = "Received from server: ";
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    response.append(buffer, 0, bytesRead);
                    if (response.toString().toLowerCase().contains("</html>")
                            || // Büyük/küçük harf duyarsız kontrol
                            response.toString().contains(http400)
                            || response.toString().contains(http501)
                            || response.toString().contains(http500)) {
                        break;
                    }
                }

                String completeMessage = response.toString();
                if (completeMessage.contains(http400)) {
                    System.out.println(receivedMessage + http400);
                } else if (completeMessage.contains(http501)) {
                    System.out.println(receivedMessage + http501);
                } else if (completeMessage.contains(http500)) {
                    System.out.println(receivedMessage + http500);
                } else {
                    System.out.println(receivedMessage + http200);
                    saveFile(completeMessage);
                }
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
