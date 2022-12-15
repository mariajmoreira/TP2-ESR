import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {

    private Integer estadoStreamNodo;//tabela a dizer se esta a receber stream 0-> n tem stream | 1-> tem stream
    private Map<InetAddress, Integer> tabelaCusto;
    private InetAddress ip;
    private DatagramSocket socketEnviar;
    private DatagramSocket socketReceber;
    private InetAddress prev_node = null;

    private List<InetAddress> vizinhanca;

    private List<InetAddress> destinosStream;

    //para stream a partir do nodo

    DatagramPacket rcvdp; //UDP packet received from the server (to receive)
    DatagramSocket RTPsocketEnviar;//socket to be used to send and receive UDP packet

    DatagramSocket RTPsocketReceber;

    DatagramSocket socketPedirVizinhos;
    static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets
    Timer cTimer; //timer used to receive data from the UDP socket
    byte[] cBuf;

    Integer teste;


    public Node(InetAddress ipserver) throws IOException {

        this.vizinhanca = new ArrayList<>();
        this.tabelaCusto = new HashMap<>();
        this.destinosStream = new ArrayList<>();

        socketEnviar = new DatagramSocket(4000);
        socketReceber = new DatagramSocket(4321);

        socketPedirVizinhos = new DatagramSocket(5000);

        RTPsocketEnviar = new DatagramSocket(25000);
        RTPsocketReceber = new DatagramSocket(30000);


        this.cBuf = new byte[15000];
        //RTPsocket = new DatagramSocket(6000);

       // this.socket1 = new DatagramSocket(3210);
        /*this.socketActivate = new DatagramSocket(5678, this.ip);
        this.socketOverlay = new DatagramSocket(4321, this.ip);
        this.socketPing = new DatagramSocket(8765, this.ip);
        this.socketPingRouter = new DatagramSocket(9546, this.ip);*/

        //DatagramSocket socket = new DatagramSocket();
        estadoStreamNodo = 0;

        new Thread(() -> { // THREAD PARA CRIAR O OVERLAY (receber vizinhos)
            try {

                Packet p = new Packet(2,0,null);

                DatagramPacket request = new DatagramPacket(p.serialize(), p.serialize().length, ipserver, 4321);
                
                socketEnviar.send(request);
                System.out.println("node: Pedido tipo 2 (Vizinhos) enviado ao servidor!");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        //if(teste==1){}

        new Thread(() -> { // THREAD PARA receber e enviar pacotes RTP
            try {

                while(true) {

                    //Thread.sleep(1000);

                    rcvdp = new DatagramPacket(cBuf, cBuf.length);
                    RTPsocketReceber.receive(rcvdp);

                    estadoStreamNodo=1;

                    System.out.println("node: Recebi um pacote RTP do ip [ " + rcvdp.getAddress() + " ]");

                    RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

                    for(InetAddress quemQuer : destinosStream){

                        int packet_length = rtp_packet.getlength();

                        byte[] packetBits = new byte[packet_length];
                        rtp_packet.getpacket(packetBits);

                        DatagramPacket senddp = new DatagramPacket(packetBits, packet_length, quemQuer,30000 );
                        RTPsocketEnviar.send(senddp);

                        System.out.println("node: Stream enviada para o ip [ "+ quemQuer.toString()+ " ]");

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();



        new Thread(() -> { // THREAD PARa receber msg do cliente e enviar ou pedir stream ao nodo anterior
            try {
                while(true){

                    byte [] data = new byte[1024];

                    DatagramPacket responseO = new DatagramPacket(data, data.length); //novos nodos na rede
                    socketReceber.receive(responseO);

                    //ystem.out.println("node: recebi um pacote! msg(1/2)");

                    data = responseO.getData();
                    Packet pReceive = new Packet(data);

                    System.out.println("node: recebi um pacote do tipo " + pReceive.getMsgType() + " do ip [ " + responseO.getAddress() + " ] ");
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    if(pReceive.getMsgType()==5) {//cliente pede stream

                        System.out.println("node: Pedido de Stream (Tipo 5)");
                        System.out.println();

                        destinosStream.add(responseO.getAddress());//adiciona a quem quer stream

                        if((estadoStreamNodo!=1)) {//nodo ainda n tem streaming

                            Packet pStream = new Packet(5, 0, null);//pede stream
                            //byte[] dataNew = pStream.serialize();

                            InetAddress maisProximo = null;

                            int v = 999;

                            for (Map.Entry<InetAddress, Integer> e : tabelaCusto.entrySet()) {//ver qual nodo está mais proximo
                                if (e.getValue() < v) {
                                    v = e.getValue();
                                    maisProximo = e.getKey();
                                }
                            }

                            System.out.println("node: Vou pedir stream ao IP [ "+ maisProximo + " ]");

                            DatagramPacket newNode = new DatagramPacket(pStream.serialize(), pStream.serialize().length, maisProximo, 4321);
                            socketEnviar.send(newNode);

                            //teste=1;

                            System.out.println("node: Pedido de stream enviado!");

                        }else if(estadoStreamNodo==1){//nodo ja tem streaming

                            System.out.println("node: Já tenho stream!");


                        }else System.out.println("node: ERRO! recebi pedido tipo 5 mas n funfa!");
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }else if (pReceive.getMsgType() == 4) {//recebe vizinhos a partir do servidor

                        System.out.println("node: A espera de vizinhos (Tipo 4)");
                        System.out.println();

                        vizinhanca.addAll(pReceive.getVizinhos());
                        //vizinhanca.addAll(pReceive.getVizinhos());

                        System.out.println("node: Recebi os vizinhos:");
                        for(InetAddress vv : pReceive.getVizinhos()){
                            System.out.println(vv.toString());
                        }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }else if(pReceive.getMsgType()==3){//FLOODING MSG

                        System.out.println("node: Começar o flood! (Tipo 3)");

                        InetAddress nodoFloodRecebido = responseO.getAddress();

                        int custo = pReceive.getCusto();

                        System.out.println("node: Recebi de: [ " + nodoFloodRecebido + " ] com custo : " + custo);
                        System.out.println();

                        if (prev_node == null) { // 1ª iteração
                            prev_node = nodoFloodRecebido;
                            tabelaCusto.put(nodoFloodRecebido, custo); // Guardar na tabela de custos qual a origem e o custo a partir dessa origem

                            System.out.println("node: ITERACAO 1: A enviar para vizinhos | Custo atual : " + custo);

                            for (InetAddress inet : vizinhanca) {
                                if (!inet.equals(prev_node)) {//não enviar para o nodo anterior

                                    Packet msg = new Packet(3, custo + 1, null);//FLOOD MSG
                                    byte[] dataResponse = msg.serialize();
                                    DatagramPacket pResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 4321);
                                    socketEnviar.send(pResponse);

                                    System.out.println("node: flooding to [ "+ inet.toString() + " ]");
                                }
                            }
                            System.out.println();
                        }else { //Vezes seguintes a chegar ao nodo
                            int custoAnterior = tabelaCusto.get(prev_node);
                            if (custo <= custoAnterior) { //Atualizar o antecessor
                                prev_node = nodoFloodRecebido;
                                System.out.println("node: ITERAÇÃO X : A enviar para vizinhos | custo : " + custo);

                                    // envia msg aos seus vizinhos
                                for (InetAddress inet : vizinhanca) {
                                    if (!inet.equals(prev_node)) {

                                        Packet msg1 = new Packet(3,custo+1,null);
                                        byte[] dataResponse = msg1.serialize();
                                        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 4321);
                                        socketEnviar.send(pktResponse);

                                        System.out.println("node: flooding to [ "+ inet.toString() + " ]");
                                    }
                                }
                                System.out.println();
                            }

                            if (tabelaCusto.containsKey(nodoFloodRecebido)) { // Atualização do valor
                                int custoAntigo = tabelaCusto.get(nodoFloodRecebido);
                                if (custoAntigo >= custo) tabelaCusto.put(nodoFloodRecebido, custo);
                            } else { // Inserção do valor
                                tabelaCusto.put(nodoFloodRecebido, custo);
                            }
                        }
                    } /*else if (pReceive.getMsgType() == 6) {//recebe vizinhos a partir do servidor

                        System.out.println("node: Notificação de stream (Tipo 6)");
                        System.out.println();
                        //

                        //thread rtp


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }*/else System.out.println("ERRO: mensagem de tipo desconhecido!)");
                            //enviar stream ao cliente

                    }
                } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
