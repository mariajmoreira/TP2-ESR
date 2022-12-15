
import java.io.*;
import java.net.*;
import java.util.*;


public class Database {

     //Para guardar informção do ficheiro de configurações
     private Map<InetAddress, List<InetAddress>> configFile; //ip, <ips dos vizinhos>

     public Database() throws IOException {
        System.out.println("Database Criada!");
        parser("configFile2.txt");
    
        System.out.println("CONFIG: " + configFile.toString());
    
        //
    
    }

    public List<InetAddress> getAllNodos(){

        return new ArrayList<>(configFile.keySet());
    }

     // Parser

  public void parser(String path) throws IOException {

    BufferedReader reader = new BufferedReader(new FileReader(path));
    String line = reader.readLine();
    System.out.println(line);

    this.configFile = new HashMap<>();

    while (line != null){

        String[] partes = line.split(":");

        String[] ips = partes[1].split(";");

        String[] ipnodo = partes[0].split(",");

        //

        InetAddress inetnodo = InetAddress.getByName(ipnodo[1]);


        List<InetAddress> lista = new ArrayList<>();

        for (String nomeip : ips){

          String[] partes2 = nomeip.split(",");

          InetAddress inetvizinho = InetAddress.getByName(partes2[1]);
            
          lista.add(inetvizinho);

        }


      this.configFile.put(inetnodo,lista);

        line = reader.readLine();
  }
  System.out.println("Parsing done!");
  reader.close();

    }   

    public List getNeighbours(InetAddress nodo){
        List<InetAddress> neighbours = new ArrayList<>();
        for(InetAddress key : configFile.keySet()){
            if(key.equals(nodo)){
               neighbours = configFile.get(key);
            }
        }
        return neighbours;
    }
    
}
