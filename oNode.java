import java.io.IOException;
import java.net.InetAddress;

public class oNode {
    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("syntax: java oNode (S for Server | C for Client | N for Node) (server_IP)");
            return;
        }

        String ipserver = args[1];

        if (args[0].equals("S")) {

            Servidor s = new Servidor(InetAddress.getByName(ipserver));

        } else if (args[0].equals("C")) {

            Cliente c = new Cliente(InetAddress.getByName(ipserver));

        } else {

            Node n = new Node(InetAddress.getByName(ipserver));
        }
    }
}

