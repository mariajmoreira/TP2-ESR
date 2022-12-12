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
    protected InetAddress prev_node = null;


    public Node(InetAddress ipserver) throws IOException {

        this.socket = new DatagramSocket(3000, this.ip);
        /*this.socketActivate = new DatagramSocket(5678, this.ip);
        this.socketOverlay = new DatagramSocket(4321, this.ip);
        this.socketPing = new DatagramSocket(8765, this.ip);
        this.socketPingRouter = new DatagramSocket(9546, this.ip);*/

        //DatagramSocket socket = new DatagramSocket();

        new Thread(() -> { // THREAD PARA CRIAR O OVERLAY
            try {
                InetAddress serverAddress =InetAddress.getByName("172.16.0.20");
                int serverPort = 3000;


                Packet p = new Packet(2,0,null);
                System.out.println("PACKET DATA: " + p.getData());

                byte[] packetBytes = p.serialize();

                DatagramPacket request = new DatagramPacket(packetBytes, packetBytes.length, ipserver, serverPort);
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

        new Thread(() -> { // THREAD para flood
            try {
                byte [] data = new byte[512];

                DatagramPacket receiveP = new DatagramPacket(data, data.length); // Recebe packet a dizer qual o custo

                while(true) {

                    System.out.println("Começar o flood!");

                    socketFlood.receive(receiveP);
                    //Thread.sleep(50);

                    byte[] dataReceived = receiveP.getData();
                    Packet p = new Packet(data);

                    if(p.getMsgType()==3){//flood msg

                        InetAddress origin = receiveP.getAddress();

                        int custo = p.getData();

                        System.out.println("Recebido : " + origin + " com custo : " + custo);

                        if (prev_node == null) { // 1ª iteração
                            prev_node = origin;
                            tabelaCusto.put(origin, custo); // Guardar na tabela de custos qual a origem e o custo a partir dessa origem

                            System.out.println("1 ITERACAO: Vou enviar para os meus vizinhos com custo : " + custo);

                            //envia msg aos seus vizinhos
                            for (InetAddress inet : routing_table.keySet()) {
                                if (!inet.equals(prev_node)) {
                                    Packet msg = new Packet(1,custo+1,null);
                                    byte[] dataResponse = msg.serialize();
                                    DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 1234);
                                    socketFlood.send(pktResponse);
                                }
                            }

                        } else { //Vezes seguintes a chegar ao nodo
                            int custo_anterior = cost_table.get(prev_node);
                            if (custo <= custo_anterior) { //Atualizar o antecessor
                                prev_node = origin;
                                System.out.println("OUTRAS ITERAÇÕES: Vou enviar para os meus vizinhos com custo : " + custo);

                                // envia msg aos seus vizinhos
                                for (InetAddress inet : routing_table.keySet()) {
                                    if (!inet.equals(prev_node)) {
                                        Packet msg1 = new Packet(1,custo+1,null);
                                        byte[] dataResponse = msg1.serialize();
                                        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 1234);
                                        socketFlood.send(pktResponse);
                                    }
                                }
                            }

                            if (cost_table.containsKey(origin)) { // Atualização do valor
                                int custo_antigo_origem = cost_table.get(origin);
                                if (custo_antigo_origem >= custo) cost_table.put(origin, custo);
                            } else { // Inserção do valor
                                cost_table.put(origin, custo);
                            }
                        }
                    } else{
                        System.out.println("Recebeu mensagem do tipo errado (Client - Tipo 1 unico tipo aceite)");
                    }
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();




    }


}
