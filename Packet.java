import java.io.*;
import java.net.InetAddress;
import java.util.List;

public class Packet {

    private int msgType;//tipo de mensagem a enviar
    //1-> activate
    //2-> overlay
    //3->flood
    //4->svenvia vizinhos
    //5->clienbt disconnect
    private int data;
    private List<InetAddress> vizinhos;

    public Packet() {
        this.msgType = 0;
    }

    public Packet(byte[] bytes) {

        try {
            Packet msg = deserialize(bytes);
            this.data = msg.getData();
            this.msgType = msg.getMsgType();
            this.vizinhos = msg.getVizinhos();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    byte[] serialize() throws IOException {

        byte[] msgBytes = new byte[0];
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);

            msgBytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msgBytes;
    }

    public Packet deserialize(byte[] msgBytes) throws IOException, ClassNotFoundException{

        Packet packet = new Packet();
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(msgBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);

            packet = (Packet) ois.readObject();
            //int messageType = packet.getMessageType();
            //int messageData = packet.getMessageData();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return packet;

    }

    public Packet(int msgType, int data, List<InetAddress> vizinhos) {
        this.msgType = msgType;
        this.data = data;
        this.vizinhos = vizinhos;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public List<InetAddress> getVizinhos() {
        return vizinhos;
    }

    public void setVizinhos(List<InetAddress> vizinhos) {
        this.vizinhos = vizinhos;
    }
}
