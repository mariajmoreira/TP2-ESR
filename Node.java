import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Node {

    private Map<InetAddress, Integer> tabelaEstado;
    private Map<InetAddress, Integer> tabelaCusto;
    private InetAddress ip;
    protected DatagramSocket socket;
    protected DatagramSocket socketFlood;
    protected DatagramSocket socketActivate;
    protected DatagramSocket socketOverlay;
    protected DatagramSocket socketPing;
    protected DatagramSocket socketPingRouter;


    public Node() throws IOException {

        this.socket = new DatagramSocket(1234, this.ip);
        /*this.socketActivate = new DatagramSocket(5678, this.ip);
        this.socketOverlay = new DatagramSocket(4321, this.ip);
        this.socketPing = new DatagramSocket(8765, this.ip);
        this.socketPingRouter = new DatagramSocket(9546, this.ip);*/

        DatagramSocket socket = new DatagramSocket();

        new Thread(() -> { // THREAD PARA CRIAR O OVERLAY
            try {
                InetAddress serverAddress =InetAddress.getByName("172.16.0.20");
                int serverPort = 3000;


                Packet p = new Packet(2,0,null);

                DatagramPacket request = new DatagramPacket(p.serialize(), p.serialize().length, serverAddress, serverPort);
                socket.send(request);

                byte [] buffer = new byte[512];

                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);

                buffer = response.getData();

                Packet pResposta = new Packet(buffer);

                for(InetAddress x: pResposta.getVizinhos()){

                    tabelaEstado.put(x,0);
                    //ping_table.put(x,0); //caso ping para Cliente e Router
                    System.out.println("vizinho " + x.toString());

                    String msgNew = x.toString();

                    System.out.println("Envia msg para " + msgNew);

                    Packet pnew = new Packet(0,0,null);
                    byte[] dataNew = pnew.serialize();

                    DatagramPacket newNode = new DatagramPacket(dataNew, dataNew.length, x,4321);
                    socketOverlay.send(newNode);

                    Thread.sleep(50);
                }

                while(true){//Espera atualizacao na rede Overlay

                    byte [] data = new byte[512];

                    DatagramPacket responseO = new DatagramPacket(data, data.length); //novos nodos na rede
                    socket.receive(responseO);

                    data = responseO.getData();
                    Packet pReceive = new Packet(data);

                    if(pReceive.getMsgType()==0) {//msg de atualizar overlay
                        tabelaEstado.put(responseO.getAddress(), 0);
                    }
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


    }


}
