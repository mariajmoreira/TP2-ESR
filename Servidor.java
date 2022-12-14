import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;


public class Servidor extends JFrame implements ActionListener{

    private InetAddress ip;
    private List<InetAddress> vizinhos;
    private DatagramSocket socketEnviar;
    private DatagramSocket socketReceber;
    private ReentrantLock lock = new ReentrantLock();
    private Map<InetAddress,Integer> tabelaEstado = new HashMap<>(); // se o nodo esta a receber stream || 1 - ativo || 0- desativado
    private List<InetAddress> nodosRede = new ArrayList<>();
    private ReentrantLock lockNodosRede = new ReentrantLock();

    private List<InetAddress> destinosStream;

    //private Condition condNodos = lockNodosRede.newCondition();


    public Servidor(InetAddress ipserver) throws IOException {

        this.socketEnviar = new DatagramSocket(4000);
        this.socketReceber = new DatagramSocket(4321);

        this.destinosStream = new ArrayList<>();

        this.vizinhos = new ArrayList<>();

        Database database = new Database();

        // Thread Criacao do Overlay
        new Thread(() -> { //thread que se vai encarregar de receber novos nodos e de lhe dar os seus vizinhos (initOverlay)
            try {
                List<InetAddress> vizinhos = database.getNeighbours(ipserver);

                System.out.println();
                System.out.println("server: Vizinhos:");
                for(InetAddress xxx : vizinhos){
                    System.out.println(xxx.toString());
                }

                for (InetAddress x : vizinhos) {
                    tabelaEstado.put(x, 0); // Inicialmente ng tem stream

                }
                try {
                    lockNodosRede.lock();
                    nodosRede.add(ipserver);
                } finally {
                    lockNodosRede.unlock();
                }
                while (true) {

                    byte[] msg = new byte[1024];
                    DatagramPacket receiveP = new DatagramPacket(msg, msg.length);
                    socketReceber.receive(receiveP);

                    msg = receiveP.getData();
                    Packet p = new Packet(msg);
                    InetAddress nodeAdr = receiveP.getAddress();
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    if (p.getMsgType() == 2) {//msg de pedir vizinhos(overlay)

                        System.out.println("sv: Nodo [ " + nodeAdr + " ] lido!");

                        try {
                            lockNodosRede.lock();
                            nodosRede.add(nodeAdr);
                        } finally {
                            lockNodosRede.unlock();
                        }

                        /*try {
                            lockNodosRede.lock();

                            if (nodosRede.containsAll(database.getAllNodos())) {//1 vez | ?????????????????????????
                                condNodos.signalAll();
                                System.out.println("sv: A Iniciar Flood!");
                            }
                        } finally{
                            lockNodosRede.unlock();
                        }*/

                        List<InetAddress> listVizinhos = database.getNeighbours(nodeAdr);

                        Packet send = new Packet(4,0, listVizinhos); //MSG tipo 4 -> Sv envia vizinhos

                        DatagramPacket pResponse = new DatagramPacket(send.serialize(), send.serialize().length, nodeAdr, 4321);
                        socketEnviar.send(pResponse);
                        System.out.println("sv: Enviei pacote tipo 4 (vizinhos) ao nodo [ " + nodeAdr + " ]");

                    } else if(p.getMsgType() == 5){//pedir streaming

                        destinosStream.add(nodeAdr);

                        tabelaEstado.put(nodeAdr,1);//nodo vai passar a trasnmitir stream

                        new Thread(() -> { //Thread encarregue de fazer stream
                            try {
                                streaming();

                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                    }else System.out.println("sv: ERRO: mensagem recebida não reconhecida!");
                    /////////////////////////////////////////////////////////////////
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }).start();

        new Thread(() -> { //FLOODING
            try {
                /*try {
                    lockNodosRede.lock();

                    while (!nodosRede.containsAll(database.getAllNodos()))
                        condNodos.await();  //A thread fica adormecida enquanto não temos todos os nodos

                } finally{
                    lockNodosRede.unlock();
                }*/
                while (true) {
                    Thread.sleep(1000);//os nodos precisam receber primeiro os seus vizinhos
                    System.out.println("sv: Flood Iniciado!");

                    vizinhos = database.getNeighbours(ipserver);

                    for (InetAddress x : vizinhos) {
                        //System.out.println("antes de enviar");

                        Packet msg = new Packet(3, 1,null);//custo 1 msg de FLOOD

                        DatagramPacket pResponse = new DatagramPacket(msg.serialize(), msg.serialize().length, x, 4321);
                        socketEnviar.send(pResponse);
                        //System.out.println("dps de enviar");
                    }
                    Thread.sleep(30000);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();


    }

    //GUI:
    //----------------
    JLabel label;

    //RTP variables:
    //----------------
    DatagramPacket senddp; //UDP packet containing the video frames (to send)A
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    int RTP_dest_port = 25000; //destination port for RTP packets
    InetAddress ClientIPAddr; //Client IP address

    static String VideoFileName; //video file to request to the server

    //Video constants:
    //------------------
    int imagenb = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer sTimer; //timer used to send the images at the video frame rate
    byte[] sBuf; //buffer used to store the images to send to the client

    //--------------------------
    //Constructor
    //--------------------------


    public Servidor() throws Exception{
        //init Frame
        super("Servidor");

        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD,this); //init Timer para servidor
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000]; //allocate memory for the sending buffer

        try {
            RTPsocket = new DatagramSocket(); //init RTP socket
            ClientIPAddr = InetAddress.getByName("127.0.0.1");
            System.out.println("Servidor: socket " + ClientIPAddr);
            video = new VideoStream(VideoFileName); //init the VideoStream object:
            System.out.println("Servidor: vai enviar video da file " + VideoFileName);

        } catch (SocketException e) {
            System.out.println("Servidor: erro no socket: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Servidor: erro no video: " + e.getMessage());
        }

        //Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                sTimer.stop();
                System.exit(0);
            }});

        //GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        sTimer.start();
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void streaming() throws Exception {

        File f = new File("movie.Mjpeg");
        if (f.exists()) {
            //Create a Main object
            Servidor s = new Servidor();
            //show GUI: (opcional!)
            //s.pack();
            //s.setVisible(true);
        } else
            System.out.println("Ficheiro de video não existe: " + VideoFileName);
    }

    //------------------------
    //Handler for timer
    //------------------------
    public void actionPerformed(ActionEvent e) {

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH)
        {
            //update current imagenb
            imagenb++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(sBuf);

                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, sBuf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);


                for(InetAddress nd : destinosStream){

                    senddp = new DatagramPacket(packet_bits, packet_length, nd, RTP_dest_port);///---------------------------------------------------------<
                    RTPsocket.send(senddp);

                }
                //send the packet as a DatagramPacket over the UDP socket

                System.out.println("Send frame #"+imagenb);
                //print the header bitstream
                rtp_packet.printheader();

                //update GUI
                //label.setText("Send frame #" + imagenb);
            }
            catch(Exception ex)
            {
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }
        else
        {
            //if we have reached the end of the video file, stop the timer
            sTimer.stop();
        }
    }
}


