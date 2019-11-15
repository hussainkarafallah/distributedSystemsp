package newdist;

import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.*;
import org.json.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;


public class NameNode implements Runnable{

    private int portNumber;

    NameNodeManager manager;
    NameNodeProxy proxy;

    public static void main(String [] args) throws IOException {
        args = new String[1];
        System.out.println(new File(".").getCanonicalPath());
        Files.deleteIfExists(Paths.get("./zbr"));
        Files.createFile(Paths.get("./zbr"));
        File f = new File("./namenode/namenode_hosts.conf");
        if(!f.exists() || f.isDirectory()) {
            System.out.println("please read readmefile and configure ./namenode/namenode_hosts.conf file");
            return;
        }
        Files.deleteIfExists(Paths.get("./namenode/zbl"));
        Files.createFile(Paths.get("./namenode/zbl"));
        Scanner sc = new Scanner(f);
        int ind = 0;
        while(sc.hasNext()){
            if(ind>=1){
                System.out.println("Too many args check ./namenode/namenode_hosts.conf");
            }
            args[ind++] = sc.next();
        }
        NameNode nameNode = new NameNode(args);
        nameNode.run();
    }
    public NameNode(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }

        portNumber = Integer.parseInt(args[0]);
        manager = new NameNodeManager(this);
    }


    class Worker implements Runnable{
        private JSONObject task;
        private Connection c;
        Worker(Connection _c  , JSONObject _task){
            c = _c;
            task = _task;
        }
        public void run(){

            if(task.get("command").equals("datanodeauth")){

                boolean result = proxy.addDataNode(new InetSocketAddress(c.getRemoteAddressTCP().getAddress() , task.getInt("port")));

                JSONObject response = new JSONObject();

                if(result){
                    response.put("status" , "OK");
                    response.put("report" , "DataNode is added successfully");
                }
                else{
                    response.put("status" , "NO");
                    response.put("report" , "Datanode already authenticated");
                }
                c.sendTCP(response);
                return;

            }

            c.sendTCP(manager.performJob(task));
        }

    }


    class Dispatcher implements Runnable{
        public void run(){
            System.out.println("Hey Dispatcher started :)");
            Server server = new Server();
            Kryo kryo = server.getKryo();
            kryo.register(JSONObject.class);
            kryo.register(java.util.HashMap.class);

            try {
                server.bind(portNumber);
            }
            catch (IOException e){
                e.printStackTrace();
            }

            server.start();

            server.addListener(new Listener.ThreadedListener(new Listener() {
                public void received (Connection connection, Object object) {
                    if (object instanceof JSONObject) {
                        JSONObject request = (JSONObject) object;
                        new Thread(new Worker(connection , request)).start();
                    }
                }
            }));
        }
    }


    public void run(){
        Thread dispatcherThread = new Thread(new Dispatcher());
        dispatcherThread.start();

        proxy = new NameNodeProxy();
        Thread proxyThread = new Thread(proxy);
        proxyThread.start();
    }
}


