import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Cliente {
    public static void main(String[] args) {
        // ARGS for server : oNode S 
        // ARGS for client : oNode C
        /* 
        if (args.length < 2) {
            System.out.println("Syntax: oNodeClient <hostname> <port>");
            return;
        }*/
 
       //String hostname = args[0];
        //int port = Integer.parseInt(args[1]);
 
        try {
            InetAddress serverAddress =InetAddress.getByName("172.16.0.20");
            int serverPort = 3000;
            DatagramSocket socket = new DatagramSocket();
 
           // while (true) {
                // DatagramPacket(byte[] buffer, buffer length, endereco de envio, porta de envio)
                byte[] requestBuffer = "Who are my neighbours?".getBytes(StandardCharsets.UTF_8);
                DatagramPacket request = new DatagramPacket(requestBuffer,requestBuffer.length, serverAddress, serverPort);
                socket.send(request);
 
                byte[] buffer = new byte[512];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);
 
                String quote = new String(buffer, 0, response.getLength());
 
                System.out.println(quote);
                System.out.println();

                socket.close();
            //    Thread.sleep(10000);
            //}
 
        } catch (SocketTimeoutException ex) {
            System.out.println("Timeout error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Client error: " + ex.getMessage());
            ex.printStackTrace();
        } //catch (InterruptedException ex) {
         //   ex.printStackTrace();
        //}
    }
}
