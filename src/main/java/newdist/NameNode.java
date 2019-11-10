package newdist;

import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.*;
import org.json.*;
import java.io.IOException;


public class NameNode implements Runnable{

    private int portNumber;
    NameNodeManager manager;
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
            //System.out.println("Currently processing " + task.toString(2));

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


//            return;
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


    }
}
