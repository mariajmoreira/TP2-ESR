import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Node {

    private Map<InetAddress, String> tabelaEstado;
    private Map<InetAddress, Integer> tabelaCusto;


    public Node(){


        new Thread(() -> {
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

    }).start();

    }


}
