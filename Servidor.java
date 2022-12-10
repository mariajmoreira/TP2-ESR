import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Servidor{

public Servidor() throws IOException {
    Database db = new Database();
    System.out.println("Servidor Criado");

    DatagramSocket socket;
    InetAddress address;
    int port;
    byte message;
    InetAddress clientAddress;
    int clientPort;

    address =InetAddress.getByName("172.16.0.20");
    port = 3000;
    socket = new DatagramSocket(port,address);
    System.out.println("Server listening on " + address + " port: " + port);

    while (true) {
        byte[] clienteBuffer = new byte[512];
        DatagramPacket request = new DatagramPacket(clienteBuffer,clienteBuffer.length);
        socket.receive(request);
        clientAddress = request.getAddress();
        clientPort = request.getPort();
        String quote = new String(clienteBuffer, 0, request.getLength());
        System.out.println("Client " + clientAddress.toString() + " says : " + quote);
        List neighbours = db.getNeighbours(clientAddress);
        answerCliente(neighbours, socket, clientAddress, clientPort);
        //byte[] buffer = "Servidor Recebeu".getBytes(StandardCharsets.UTF_8);
        //DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
        //socket.send(response);
    }
}

public void answerCliente(List neighbours,DatagramSocket socket, InetAddress clientAddress, int clientPort){
    new Thread() {
        
        public void run(){
            byte[] buffer = neighbours.toString().getBytes(StandardCharsets.UTF_8);
            DatagramPacket response = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
            try {
                socket.send(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }.start();
}


public static void main(String argv[]) throws Exception{
    Servidor s = new Servidor();
}

 
}