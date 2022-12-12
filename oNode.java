import java.io.IOException;
import java.net.InetAddress;

public class oNode {

    // ARGS for server : oNode S
    // ARGS for client : oNode C
        /*
        if (args.length < 2) {
            System.out.println("Syntax: oNodeClient <hostname> <port>");
            return;
        }*/

    //String hostname = args[0];
    //int port = Integer.parseInt(args[1]);

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Syntax: oNode C/S <hostname>");
            return;
        }

        String ipserver = args[1];
        //int port = Integer.parseInt(args[2]);

        if (args[0].equals("S")){
            Servidor s = new Servidor(InetAddress.getByName(ipserver));
        }
        else if (args[0].equals("C")) {
            Cliente c = new Cliente(InetAddress.getByName(ipserver));
        }
        else {
            // se n especificar
            Node n = new Node(InetAddress.getByName(ipserver));
        }
    }
}
