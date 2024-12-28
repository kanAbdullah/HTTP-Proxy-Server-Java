
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
            OutputStream out;
            try  {
                out = clientSocket.getOutputStream(); 
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String request;
                while ((request = in.readLine()) != null) {
                    System.out.printf("Received from client: %s\n", request);

                    // parse the request
                    int documentSize = parseRequest(request);
                    errorHandler(documentSize);
                    String document = createDocument(documentSize);

                    // Send the document using byte stream instead of PrintWriter
                    byte[] documentBytes = document.getBytes();
                    out.write(documentBytes);
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // 400 Bad Request
                try {
                    System.out.println("Bad Request");
                    out = clientSocket.getOutputStream();
                    System.out.println("outputstream created");
                    String badRequest = "HTTP/1.1 400 Bad Request\n\n";
                    System.out.println("bad request created");
                    out.write(badRequest.getBytes());
                    out.flush();
                    System.out.println("Bad Request sent to client");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            } finally {
                try {
                    System.out.println("Closing the connection");
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public int parseRequest(String req) {
            try {
                // Daha güvenli parsing
                String[] parts = req.split(" ");
                if (parts.length >= 2) {
                    String sizePart = parts[1].substring(1); // Remove leading '/'
                    int size = Integer.parseInt(sizePart);
                    return size;
                }
                return 100; // Default size if parsing fails
            } catch (Exception e) {
                System.out.println("Error parsing request: " + req);
                return 100; // Default size
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

        public void errorHandler(int size) {
            // 400 Bad Request
            if (size < 100 || size > 20000) {
                //return bad request 401
                throw new IllegalArgumentException("Bad Request");
            }
        }
    }
}
