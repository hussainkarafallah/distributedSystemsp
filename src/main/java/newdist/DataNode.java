package newdist;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class DataNode implements Runnable{

    int portNumber , nameNodePort;


    String nameNodeIP ,  dataNodeName, myIp;

    DataNodeManager manager;

    ThreadSafeClient nameNodeClient;

    public DataNode(String[] args) {

        if (args.length != 5) {
            System.err.println("Data node initalization error");
            System.exit(1);
        }
        myIp = args[0];
        portNumber = Integer.parseInt(args[1]);
        nameNodeIP = args[2];
        nameNodePort = Integer.parseInt(args[3]);
        dataNodeName = args[4];

        manager= new DataNodeManager(this);

    }

    public static void main(String [] args) throws IOException {
        System.err.close();
        System.setErr(System.out);
        args = new String[5];
//        Files.deleteIfExists(Paths.get("./zbr"));
//        Files.createFile(Paths.get("./zbr"));

        File f = new File("./datanode_hosts.conf");

        if(!f.exists() || f.isDirectory()) {
            System.out.println("Please make a new file named datanode_hosts.conf and inside please write\n " +
                    "port of datanode, ip of name node, and its port, then a customized name for your datanode\n check please check this file \n " +
                    "https://github.com/hussainkarafallah/distributedSystemsp/blob/master/datanode2/datanode_hosts.conf");
            return;
        }
        Scanner sc = new Scanner(f);
        int ind = 0;
        while(sc.hasNext()){
            if(ind>=4){
                System.out.println("Too many args check datanode_hosts.conf");
            }
            args[ind++] = sc.next();
        }
        DataNode datanode = new DataNode(args);
        datanode.run();
    }

    class Worker implements Runnable{
        private JSONObject task;
        private Connection c;
        Worker(Connection _c  , JSONObject _task){
            c = _c;
            task = _task;
        }
        public void run(){
            try {
                c.sendTCP(manager.performJob(task));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Dispatcher implements Runnable{
        public void run(){
            System.out.println("Dispatcher for datanode with name " + dataNodeName + " is running");
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
                @Override
                public void connected(Connection connection) {
                    //System.out.println("oh yeah we have incoming connection from namenode");
                }

                public void received (Connection connection, Object object) {
                    if (object instanceof JSONObject) {
                        JSONObject request = (JSONObject) object;
                 //       request.put("ip" , connection.getRemoteAddressTCP().getAddress().getHostAddress());
                        new Thread(new DataNode.Worker(connection , request)).start();
                    }
                }
            }));
        }
    }
    public void run(){

        Thread dispatcherThread = new Thread(new Dispatcher());
        dispatcherThread.start();


        nameNodeClient = new ThreadSafeClient(nameNodeIP , nameNodePort);
        nameNodeClient.getKryo().register(JSONObject.class);
        nameNodeClient.getKryo().register(java.util.HashMap.class);
        nameNodeClient.Launch();
/*
        System.out.println(nameNodeIP + " " + nameNodePort);
        try {
            connectionClient.connect(7000, nameNodeIP, nameNodePort);
        }
        catch (IOException e){
            e.printStackTrace();
        }*/

        JSONObject connectionRequest = new JSONObject();
        connectionRequest.put("command" , "datanodeauth");
        connectionRequest.put("port" , portNumber);

        try{
            nameNodeClient.sendSafeTCP(connectionRequest , new JSONObject());
        }
        catch (Exception e){
            e.printStackTrace();
        }



    }





}
