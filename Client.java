import java.net.*;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.crypto.Data;

import java.io.*;

public class Client{

    public static void main(String args[]){
    
        try{

            Socket s = new Socket("localhost", 6666);
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());
            DataInputStream din = new DataInputStream(s.getInputStream());  //maybe use buferedStream

            int documentSize = 1000;
            String request = "GET /" + documentSize + " HTTP/1.0";

            String reqMessage = "";

            dout.writeUTF(request);
            while (!reqMessage.equals("Over"))
            {
                try
                {
                    reqMessage = din.readUTF();
                    System.out.println(reqMessage);
                    saveFile(reqMessage);
                }
                catch(IOException i)
                {
                    System.out.println(i);
                    break;
                }
            }
            System.out.println("Closing connection");
 
            
            dout.flush();
            dout.close();
            s.close();

        }catch(Exception e){System.out.println(e);}
    }

    public static void saveFile(String receivedMessage){
        try {
            FileWriter messageWriter = new FileWriter("receivedMessage.txt");
            
            messageWriter.write(receivedMessage);
            messageWriter.close();
            
            System.out.println("Successfully wrote to the file.");
          } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
          }
    }
}