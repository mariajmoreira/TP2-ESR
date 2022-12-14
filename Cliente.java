import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Cliente {

    //Dado pelos stores
    //GUI
    //----
    JFrame f = new JFrame("Cliente de Testes");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;


    //RTP variables:
    //----------------
    DatagramPacket rcvdp; //UDP packet received from the server (to receive)
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packet
    static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

    Timer cTimer; //timer used to receive data from the UDP socket
    byte[] cBuf; //buffer used to store data received from the server

    private DatagramSocket socketEnviar;
    private DatagramSocket socketReceber;

    //private Map<InetAddress, Integer> tabelaEstado;
    private InetAddress router;

public Cliente(InetAddress ipserver) throws SocketException {

    this.socketEnviar = new DatagramSocket(4000);
    this.socketReceber = new DatagramSocket(4321);

    new Thread(() -> { // THREAD PARA CRIAR O OVERLAY
        try {

            Packet p = new Packet(2, 0, null);
            DatagramPacket request = new DatagramPacket(p.serialize(), p.serialize().length, ipserver, 4321);
            socketEnviar.send(request);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }).start();

    new Thread(() -> { // THRead de recber do cliente
        try {
            while (true) {

                //System.out.println("client: A espera de vizinhos");

                byte[] buffer = new byte[1024];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socketReceber.receive(response);

                buffer = response.getData();
                Packet pResposta = new Packet(buffer);

                if (pResposta.getMsgType() == 4) {//recebe vizinhos a partir do servidor

                    for (InetAddress x : pResposta.getVizinhos()) {
                        router = x;
                    }
                    System.out.println("client: Recebi os vizinhos:");//print do router do sv
                    for (InetAddress vv : pResposta.getVizinhos()) {
                        System.out.println(vv.toString());
                    }
                } else if (pResposta.getMsgType() == 3) {//flood

                    System.out.println("client: Recebi pacote tipo 3 (flood) de " + response.getAddress());

                } else {
                    System.out.println("ERRO: mensagem de tipo desconhecido!)");
                }
            }
        } catch(IOException e){
            e.printStackTrace();

        }
    }).start();

    System.out.println("Pedir STREAM do video? 1 : yes | 0 : no");
    Scanner s = new Scanner(System.in);
    int i= s.nextInt();
    //boolean testa=false;
    if(i==1) {

        new Thread(() -> { // pedir stream
            try {

                Packet p = new Packet(5, 0, null);//a pedir stream
                DatagramPacket request = new DatagramPacket(p.serialize(), p.serialize().length, router, 4321);
                //System.out.println("tou aqui");
                socketEnviar.send(request);

                while (true) {//Espera resposta do router

                    byte[] data = new byte[1024];

                    DatagramPacket responseR = new DatagramPacket(data, data.length);
                    socketReceber.receive(responseR);

                    data = responseR.getData();
                    Packet pReceive = new Packet(data);

                    if (pReceive.getMsgType() == 6) {//router envia stream

                        System.out.println("client: Recebi pacote tipo 6 (stream) do router [ " + responseR.getAddress() + " ]");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    }

    public Cliente() {

        //build GUI
        //--------------------------

        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);

        // handlers... (so dois)
        playButton.addActionListener(new playButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390,370));
        f.setVisible(true);

        //init para a parte do cliente
        //--------------------------
        cTimer = new Timer(20, new clientTimerListener());
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000]; //allocate enough memory for the buffer used to receive data from the server

        try {
            // socket e video
            RTPsocket = new DatagramSocket(RTP_RCV_PORT); //init RTP socket (o mesmo para o cliente e servidor)
            RTPsocket.setSoTimeout(5000); // setimeout to 5s
        } catch (SocketException e) {
            System.out.println("Cliente: erro no socket: " + e.getMessage());
        }
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void streamingClient() throws Exception {
        Cliente t = new Cliente();
    }

    //------------------------------------
    //Handler for buttons
    //------------------------------------

    //Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            System.out.println("Play Button pressed !");
            //start the timers ...
            cTimer.start();
        }
    }

    //Handler for tear button
    //-----------------------
    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            System.out.println("Teardown Button pressed !");
            //stop the timer
            cTimer.stop();
            //exit
            System.exit(0);
        }
    }

    //------------------------------------
    //Handler for timer (para cliente)
    //------------------------------------

    class clientTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(cBuf, cBuf.length);

            try{
                //receive the DP from the socket:
                RTPsocket.receive(rcvdp);

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                //display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            }
            catch (InterruptedIOException iioe){
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }
    }





}
