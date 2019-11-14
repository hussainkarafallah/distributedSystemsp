package newdist;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.InetSocketAddress;

public class DataNode implements Runnable{

    int portNumber , nameNodePort;

    String nameNodeIP ,  dataNodeName;

    DataNodeManager manager;

    Client connectionClient;

    public DataNode(String[] args) {

        if (args.length != 4) {
            System.err.println("Data node initalization error");
            System.exit(1);
        }

        portNumber = Integer.parseInt(args[0]);
        nameNodeIP = args[1];
        nameNodePort = Integer.parseInt(args[2]);
        dataNodeName = args[3];

        manager= new DataNodeManager(this);

    }

    public static void main(String [] args){
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
                    System.out.println("oh yeah we have incoming connection from namenode");
                }

                public void received (Connection connection, Object object) {
                    if (object instanceof JSONObject) {
                        JSONObject request = (JSONObject) object;
                        new Thread(new DataNode.Worker(connection , request)).start();
                    }
                }
            }));
        }
    }
    public void run(){

        Thread dispatcherThread = new Thread(new Dispatcher());
        dispatcherThread.start();


        connectionClient = new Client();

        connectionClient.start();
        connectionClient.getKryo().register(JSONObject.class);
        connectionClient.getKryo().register(java.util.HashMap.class);

        System.out.println(nameNodeIP + " " + nameNodePort);
        try {
            connectionClient.connect(7000, nameNodeIP, nameNodePort);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        System.out.println(nameNodePort + " " + portNumber);
        JSONObject connectionRequest = new JSONObject();
        connectionRequest.put("command" , "datanodeauth");
        connectionRequest.put("port" , portNumber);

        connectionClient.sendTCP(connectionRequest);



    }





}
