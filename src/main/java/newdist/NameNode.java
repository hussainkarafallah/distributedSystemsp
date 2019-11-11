package newdist;

import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.*;
import org.json.*;

import javax.print.attribute.standard.Severity;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;


public class NameNode implements Runnable{

    private int portNumber;

    NameNodeManager manager;
    Proxy proxy;

    public NameNode(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }

        portNumber = Integer.parseInt(args[0]);
        manager = new NameNodeManager(this);
    }

    class Proxy implements Runnable{

        List<Client> sockets;
        List<InetSocketAddress> dataNodes;
        List<Semaphore> semaphores;


        public boolean addDataNode(InetAddress datanode , int port){

            /*if( dataNodes.indexOf(datanode) != -1)
                return false;

            dataNodes.add(datanode);
            Client client = new Client();

            Kryo kryo = client.getKryo();
            kryo.register(JSONObject.class);
            kryo.register(java.util.HashMap.class);


           // datanode.getAddress().toString();

            sockets.add(client);

            semaphores.add(new Semaphore(1));

            client.addListener(new Listener.ThreadedListener(new Listener() {

                @Override
                public void idle(Connection connection) {
                    InetSocketAddress add = connection.getRemoteAddressTCP();
                    int idx = sockets.indexOf(add);
                    assert(idx != -1);
                    semaphores.get(idx).release();
                }

            }));*/

            System.out.println(datanode.getHostName());
            System.out.println(portNumber);

            return true;

        }
        public void run(){
            sockets = new ArrayList<>();
            dataNodes = new ArrayList<>();
            semaphores = new ArrayList<>();

            System.out.println("Hey proxy started :)");


        }
        /*Server server;


        void init(){
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
        }
        public void run(){
            System.out.println("Hey radar started :)");



        }*/
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

                boolean result = proxy.addDataNode(c.getRemoteAddressTCP().getAddress() , task.getInt("port"));

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

        proxy = new Proxy();
        Thread proxyThread = new Thread(proxy);
        proxyThread.start();


    }

/*    class EndPoint{
        String ipAddress;
        int portNumber;
        EndPoint(String _ip , int _port){
            ipAddress = _ip;
            portNumber = _port;
        }

        public String getIpAddress(){
            return ipAddress;
        }

        public int getPortNumber(){
            return portNumber;
        }

        public boolean equals(Object o){
            if(o == null) return false;
            if(o == this) return true;
            if(!(o instanceof EndPoint)) return false;
            EndPoint other = (EndPoint)(o);
            return other.getIpAddress() == this.ipAddress && other.getPortNumber() == this.portNumber;
        }

    }*/
}


