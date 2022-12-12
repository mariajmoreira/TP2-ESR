import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Servidor{

    private InetAddress ip;

    private DatagramSocket socket;
    private DatagramSocket socketFlood;
    private DatagramSocket socketActivate;
    private DatagramSocket socketOverlay;

    private ReentrantLock lock = new ReentrantLock();
    private Map<InetAddress,Integer> tabelaEstado = new HashMap<>(); // tabela de encaminhamento de cada nodo || 1 - ativo || 0- desativado

    private List<InetAddress> nodosRede = new ArrayList<>();
    private ReentrantLock lockNodosRede = new ReentrantLock();

    private Condition condNodos = lockNodosRede.newCondition();



    public Servidor() throws IOException {
        //this.ip = inetAddress;
        this.ip = InetAddress.getByName("172.16.0.20");
        this.socket = new DatagramSocket(3000, this.ip);
        System.out.println("server ip: " +  this.ip);
        Database database = new Database();
        /*this.socketActivate = new DatagramSocket(5678, this.ip);
        this.socketOverlay = new DatagramSocket(4321, this.ip);*/

        // Thread Criacao do Overlay
        new Thread(() -> { //thread que se vai encarregar de receber novos nodos e de lhe dar os seus vizinhos (initOverlay)
            try {
                List<InetAddress> vizinhos = database.getNeighbours(this.ip);

                for (InetAddress x : vizinhos) {
                    tabelaEstado.put(x, 0); // Inicialmente todos os nodos estão desativados

                }
                try {
                    lockNodosRede.lock();
                    nodosRede.add(this.ip);
                } finally {
                    lockNodosRede.unlock();
                }
                while (true) {

                    byte[] msg = new byte[512];
                    DatagramPacket receiveP = new DatagramPacket(msg, msg.length);
                    socket.receive(receiveP);

                    
                    //parseFile();

                    msg = receiveP.getData();
                    Packet p = new Packet(msg);
                    InetAddress nodeAdr = receiveP.getAddress();
                    ///////////////////////////////////////////////////////////////////////
                    if (p.getMsgType() == 2) {//msg de pedir vizinhos(overlay)

                        System.out.println("Nodo " + nodeAdr + "lido!");

                        try {
                            lockNodosRede.lock();
                            nodosRede.add(nodeAdr);
                        } finally {
                            lockNodosRede.unlock();
                        }

                        try {
                            lockNodosRede.lock();

                            if (nodosRede.containsAll(database.getAllNodos())) {//1 vez
                                condNodos.signalAll();
                                System.out.println("A Iniciar Flood!");
                            }
                        } finally{
                            lockNodosRede.unlock();
                        }

                        List<InetAddress> listaVizinhos = database.getNeighbours(nodeAdr);

                        Packet send = new Packet(4,0, listaVizinhos); //MSG tipo 4 -> Sv envia vizinhos

                        DatagramPacket pktResponse = new DatagramPacket(send.serialize(), send.serialize().length, nodeAdr, 1234);
                        socket.send(pktResponse);
                    } else{
                        tabelaEstado.put(nodeAdr,0);
                    }/////////////////////////////////////////////////////////////////
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }).start();

        new Thread(() -> { //Thread encarregue de fazer flood para a rede para determinar as tabelas de encaminhamento
            try {
                try {
                    lockNodosRede.lock();

                    while (!nodosRede.containsAll(database.getAllNodos()))
                        condNodos.await();  //A thread fica adormecida enquanto não temos todos os nodos

                } finally{
                    lockNodosRede.unlock();
                }
                while (true) {
                    Thread.sleep(50);
                    System.out.println("Flood Iniciado!");

                    List<InetAddress> vizinhos = database.getNeighbours(this.ip);

                    for (InetAddress x : vizinhos) {

                        Packet msg = new Packet(3, 1,null);//custo 1 msg de FLOOD

                        DatagramPacket pResponse = new DatagramPacket(msg.serialize(), msg.serialize().length, x, 1234);
                        socketFlood.send(pResponse);
                    }
                    Thread.sleep(20000);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    /*public Servidor() throws IOException {
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
    }*/


    public static void main(String argv[]) throws Exception{
      //  Servidor s = new Servidor();
    }

    /*
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

    /*
    public Servidor() {


        //init Frame
        super("Servidor");

        // init para a parte do servidor
        sTimer = new Timer(FRAME_PERIOD, this); //init Timer para servidor
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
  //main (stream)
  //-----------------------------------
  public static void main(String argv[]) throws Exception
  {
    //get video filename to request:
    if (argv.length >= 1 ) {
        VideoFileName = argv[0];
        System.out.println("Servidor: VideoFileName indicado como parametro: " + VideoFileName);
    } else  {
        VideoFileName = "movie.Mjpeg";
        System.out.println("Servidor: parametro não foi indicado. VideoFileName = " + VideoFileName);
    }

    File f = new File(VideoFileName);
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

	  //send the packet as a DatagramPacket over the UDP socket
	  senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
	  RTPsocket.send(senddp);

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
 */





 
}