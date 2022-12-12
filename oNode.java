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
        switch(args[0]){
            case "S":
                // java oNode S
                Servidor s = new Servidor();
            case "C":
                // java oNode C ipserver
                if(args[1]!=""){
                    String ipserver = args[1];
                    Node c = new Node(InetAddress.getByName(ipserver));
                }

            default:
                //java oNode C ipserver
                if(args[0]!=""){
                    String ipserver = args[0];
                    Node c = new Node(InetAddress.getByName(ipserver));
                }
        }
    }
/*
        if (args.length < 2) {
            System.out.println("Syntax: oNode C/S <hostname>");
            return;
        }

        String ipserver = args[1];
        //int port = Integer.parseInt(args[2]);

        if (args[0].equals("S")){
            Servidor s = new Servidor();
        }
        else if (args[0].equals("C")) {
            //Cliente c = new Cliente(InetAddress.getByName(ipserver));
        }
        else {
            // se n especificar
            Node n = new Node(InetAddress.getByName(ipserver));
        }
    }
    */
}
