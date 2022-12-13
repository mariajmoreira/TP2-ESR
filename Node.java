import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Node {

    private Integer estadoStreamNodo;//tabela a dizer se esta a receber stream 0-> n tem stream | 1-> tem stream
    private Map<InetAddress, Integer> tabelaCusto;
    private InetAddress ip;
    private DatagramSocket socketEnviar;
    private DatagramSocket socketReceber;
    private InetAddress prev_node = null;

    private List<InetAddress> vizinhanca;


    public Node(InetAddress ipserver) throws IOException {

        this.vizinhanca = new ArrayList<>();
        this.tabelaCusto = new HashMap<>();

        socketEnviar = new DatagramSocket(4000);
        socketReceber = new DatagramSocket(4321);

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

        new Thread(() -> { // THREAD PARa receber msg do cliente e enviar ou pedir stream ao nodo anterior
            try {
                while(true){

                    byte [] data = new byte[1024];

                    DatagramPacket responseO = new DatagramPacket(data, data.length); //novos nodos na rede
                    socketReceber.receive(responseO);

                    System.out.println("node: recebi um pacote! msg(1/2)");

                    data = responseO.getData();
                    Packet pReceive = new Packet(data);

                    System.out.println("node: recebi um pacote do tipo " + pReceive.getMsgType() + " do ip [ " + responseO.getAddress() + " ] msg(2/2)");
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    if(pReceive.getMsgType()==5) {//cliente pede stream

                        if((estadoStreamNodo!=1)) {

                            Packet pStream = new Packet(5, 0, null);//pede stream
                            //byte[] dataNew = pStream.serialize();

                            InetAddress maisProximo = null;
                            int v = 999;
                            for (Map.Entry<InetAddress, Integer> e : tabelaCusto.entrySet()) {//ver qual nodo está mais proximo
                                if (e.getValue() < v) maisProximo = e.getKey();
                            }

                            DatagramPacket newNode = new DatagramPacket(pStream.serialize(), pStream.serialize().length, maisProximo, 4321);
                            socketEnviar.send(newNode);
                        }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }else if (pReceive.getMsgType() == 4) {//recebe vizinhos a partir do servidor

                        System.out.println("node: A espera de vizinhos");

                        vizinhanca.addAll(pReceive.getVizinhos());
                        //vizinhanca.addAll(pReceive.getVizinhos());

                        System.out.println("node: Recebi os vizinhos:");
                        for(InetAddress vv : pReceive.getVizinhos()){
                            System.out.println(vv.toString());
                        }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    }else if(pReceive.getMsgType()==3){//flood msg

                        System.out.println("node: Começar o flood!");

                        InetAddress nodoFloodRecebido = responseO.getAddress();

                        int custo = pReceive.getCusto();

                        System.out.println("node: Recebi de: [ " + nodoFloodRecebido + " ] com custo : " + custo);

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
                                }
                            }
                        }else { //Vezes seguintes a chegar ao nodo
                            int custoAnterior = tabelaCusto.get(prev_node);
                            if (custo < custoAnterior) { //Atualizar o antecessor
                                prev_node = nodoFloodRecebido;
                                System.out.println("node: ITERAÇÃO X : A enviar para vizinhos | custo : " + custo);

                                    // envia msg aos seus vizinhos
                                for (InetAddress inet : vizinhanca) {
                                    if (!inet.equals(prev_node)) {

                                        Packet msg1 = new Packet(3,custo+1,null);
                                        byte[] dataResponse = msg1.serialize();
                                        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 4321);
                                        socketEnviar.send(pktResponse);
                                    }
                                }
                            }

                            if (tabelaCusto.containsKey(nodoFloodRecebido)) { // Atualização do valor
                                int custoAntigo = tabelaCusto.get(nodoFloodRecebido);
                                if (custoAntigo >= custo) tabelaCusto.put(nodoFloodRecebido, custo);
                            } else { // Inserção do valor
                                tabelaCusto.put(nodoFloodRecebido, custo);
                            }
                        }
                    } else{
                            System.out.println("ERRO: mensagem de tipo desconhecido!)");
                        }
                            //enviar stream ao cliente

                    }
                } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        /*new Thread(() -> { // THREAD para flood
            try {

                    //..-
                while(true) {
                    //....
                    byte [] data = new byte[512];

                    DatagramPacket receiveP = new DatagramPacket(data, data.length); // Recebe packet a dizer qual o custo

                    socketReceber.receive(receiveP);
                    //Thread.sleep(50);

                    byte[] dataReceived = receiveP.getData();
                    Packet p = new Packet(data);

                    if(p.getMsgType()==3){//flood msg

                        System.out.println("node: Começar o flood!");

                        InetAddress nodoOrigem = receiveP.getAddress();

                        int custo = p.getCusto();

                        System.out.println("node: Recebido : " + nodoOrigem + " com custo : " + custo);

                        if (prev_node == null) { // 1ª iteração
                            prev_node = nodoOrigem;
                            tabelaCusto.put(nodoOrigem, custo); // Guardar na tabela de custos qual a origem e o custo a partir dessa origem

                            System.out.println("node: ITERACAO 1: A enviar para vizinhos | custo : " + custo);

                            for (InetAddress inet : vizinhanca) {
                                if (!inet.equals(prev_node)) {//não enviar para o nodo anterior

                                    Packet msg = new Packet(3,custo+1,null);//FLOOD MSG
                                    byte[] dataResponse = msg.serialize();
                                    DatagramPacket pResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 4321);
                                    socketEnviar.send(pResponse);
                                }
                            }

                        } else { //Vezes seguintes a chegar ao nodo
                            int custoAnterior = tabelaCusto.get(prev_node);
                            if (custo < custoAnterior) { //Atualizar o antecessor
                                prev_node = nodoOrigem;
                                System.out.println("node: ITERAÇÃO X : A enviar para vizinhos | custo : " + custo);

                                // envia msg aos seus vizinhos
                                for (InetAddress inet : vizinhanca) {
                                    if (!inet.equals(prev_node)) {

                                        Packet msg1 = new Packet(3,custo+1,null);
                                        byte[] dataResponse = msg1.serialize();
                                        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 4321);
                                        socketEnviar.send(pktResponse);
                                    }
                                }
                            }

                            if (tabelaCusto.containsKey(nodoOrigem)) { // Atualização do valor
                                int custoAntigo = tabelaCusto.get(nodoOrigem);
                                if (custoAntigo >= custo) tabelaCusto.put(nodoOrigem, custo);
                            } else { // Inserção do valor
                                tabelaCusto.put(nodoOrigem, custo);
                            }
                        }
                    } else{
                        System.out.println("node: Erro! (msg não tipo 3 (flood)");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();




    }*/


}
}
