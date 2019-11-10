package newdist;

import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.*;
import org.json.*;
import java.io.IOException;


public class NameNode implements Runnable{

    private int portNumber;
    private int id;
    public NameNode(String[] args) {

        id = 0;

        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }

        portNumber = Integer.parseInt(args[0]);
    }

    class Worker implements Runnable{
        private int id;
        private JSONObject task;
        private Connection c;
        Worker(Connection _c , int _id , JSONObject _task){
            c = _c;
            id = _id;
            task = _task;
        }
        public void run(){
            System.out.println("Currently thread number " + id + " is executing the shit " + task.get("sender") + "  " + task.get("task"));
         //   c.sendTCP("ok done by "  + String.valueOf(id));
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
                        new Thread(new Worker(connection , ++id , request)).start();
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
