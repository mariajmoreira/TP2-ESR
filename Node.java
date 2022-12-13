import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class Node {

    private Integer estadoStreamNodo;//tabela a dizer se esta a receber stream 0-> n tem stream | 1-> tem stream
    private Map<InetAddress, Integer> tabelaCusto;
    private InetAddress ip;
    protected DatagramSocket socket;
    protected DatagramSocket socket1;
    protected DatagramSocket socketActivate;
    protected DatagramSocket socketOverlay;
    protected DatagramSocket socketPing;
    protected DatagramSocket socketPingRouter;
    protected InetAddress prev_node = null;

    private List<InetAddress> vizinhanca;


    public Node(InetAddress ipserver) throws IOException {


        this.socket = new DatagramSocket(4000);
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

                DatagramPacket request = new DatagramPacket(p.serialize(), p.serialize().length, ipserver, 4000);
                //System.out.println("tou aqui");
                socket.send(request);

                byte [] buffer = new byte[1024];

                DatagramPacket response = new DatagramPacket(buffer, buffer.length);

                socket.receive(response);

                buffer = response.getData();

                Packet pResposta = new Packet(buffer);

                vizinhanca.addAll(pResposta.getVizinhos());

                System.out.println("node: Recebi os vizinhos");

                /*for(InetAddress x: pResposta.getVizinhos()){

                    tabelaEstado.put(x,0);
                    //ping_table.put(x,0); //caso ping para Cliente e Router

                    System.out.println("Vizinho " + x.toString());
                    System.out.println("Envia msg para " + x.toString());

                    Packet pnew = new Packet(0,0,null);
                    byte[] dataNew = pnew.serialize();

                    DatagramPacket newNode = new DatagramPacket(dataNew, dataNew.length, x,4000);
                    socket.send(newNode);

                    Thread.sleep(50);
                }*/

                /*while(true){//Espera atualizacao na rede Overlay

                    byte [] data = new byte[1024];

                    DatagramPacket responseO = new DatagramPacket(data, data.length); //novos nodos na rede
                    socket.receive(responseO);

                    data = responseO.getData();
                    Packet pReceive = new Packet(data);

                    if(pReceive.getMsgType()==0) {//msg de atualizar overlay
                        tabelaEstado.put(responseO.getAddress(), 1);//ATIVA NODO <--------------------------
                    }
                }*/

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> { // THREAD PARa receber msg do cliente e enviar ou pedir stream ao nodo anterior
            try {

                while(true){

                    byte [] data = new byte[1024];

                    DatagramPacket responseO = new DatagramPacket(data, data.length); //novos nodos na rede
                    socket.receive(responseO);

                    data = responseO.getData();
                    Packet pReceive = new Packet(data);

                    if(pReceive.getMsgType()==5) {//cliente pede stream

                        if((estadoStreamNodo!=1)){

                            Packet pStream = new Packet(5,0,null);//pede stream
                            //byte[] dataNew = pStream.serialize();

                            InetAddress maisProximo = null;
                            int v = 999;
                            for(Map.Entry<InetAddress,Integer> e : tabelaCusto.entrySet()){//ver qual nodo está mais proximo
                                if(e.getValue()<v) maisProximo = e.getKey();
                            }

                            DatagramPacket newNode = new DatagramPacket(pStream.serialize(), pStream.serialize().length, maisProximo,4000);
                            socket.send(newNode);

                        }else{
                            //enviar stream ao cliente
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> { // THREAD para flood
            try {

                    //..-
                while(true) {
                    //....
                    byte [] data = new byte[512];

                    DatagramPacket receiveP = new DatagramPacket(data, data.length); // Recebe packet a dizer qual o custo

                    socket.receive(receiveP);
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
                                    DatagramPacket pResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 4000);
                                    socket.send(pResponse);
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
                                        DatagramPacket pktResponse = new DatagramPacket(dataResponse, dataResponse.length, inet, 4000);
                                        socket.send(pktResponse);
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




    }


}
