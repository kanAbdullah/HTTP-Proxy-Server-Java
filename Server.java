import java.net.*;
import java.io.*;
import java.io.FileReader;
import java.io.IOException;


public class Server {

    private Socket          socket   = null;
    private ServerSocket    server   = null;
    private DataInputStream in       =  null;
    DataOutputStream        dout     = null;
 
    // constructor with port
    public Server(int port)
    {
        // starts server and waits for a connection
        try
        {
            server = new ServerSocket(port);
            System.out.println("Server started");
 
            System.out.println("Waiting for a client ...");
 
            socket = server.accept();
            System.out.println("Client accepted");
 
            // takes input from the client socket
            in = new DataInputStream(
                new BufferedInputStream(socket.getInputStream()));

            dout  = new DataOutputStream(socket.getOutputStream());
 
            String line = "";
 
            // reads message from client until "Over" is sent
            while (!line.equals("Over"))
            {
                try
                {
                    line = in.readUTF();
                    int size = parseREquest(line);
                    System.out.println("Request: " + line);
                    System.out.println("Document size: " + size);
                    System.out.println(line);

                    String document = createDocument(size);

                    /*
                     * send the parsed and created document to the client
                     */

                    dout.writeUTF(document);
                }
                catch(IOException i)
                {
                    System.out.println(i);
                    break;
                }
            }
            System.out.println("Closing connection");
 
            // close connection
            socket.close();
            dout.flush();
            dout.close();
            in.close();
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    public int parseREquest(String req){ //needs to be error-proof
        
        int documentSize = 0;
        documentSize = Integer.parseInt(req.substring(5,req.length()-9));
        return documentSize;
    }

    public String createDocument(int size){
       
        String documentHeader= "";
        String document = "";
        String responsePath = "./House-M.D.-pilot-script.txt";
        
        try (FileReader reader = new FileReader(responsePath)) {
            char[] buffer = new char[size];
            int charsRead = reader.read(buffer, 0, size);

            if (charsRead > 0) {
                document = new String(buffer, 0, charsRead);
                System.out.println("Read text:");
                System.out.println(document);
            } else {
                System.out.println("No characters read from the file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        document = "<HTML>\n<HEAD>\n<TITLE>" + documentHeader +"</TITLE>\n</HEAD>\n<BODY>" + document + "</BODY>\n</HTML>";
        return document;
    }

    public static void main(String[] args) {
        System.out.println("\nNice try feds\nI'm not paying my taxes!");
        System.out.println("Length of basic template:"  + "<HTML>\n<HEAD>\n<TITLE></TITLE>\n</HEAD>\n<BODY></BODY>\n</HTML>".length()); //59 btw
        Server currentServer = new Server(6666);
    }
}