package newdist;


import java.io.*;
import java.net.*;

import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.minlog.Log;
import org.json.JSONObject;

public class ClientApp implements Runnable{
    private String hostName , clientName , currentDirectory , userName;
    private int portNumber;
    private int isLogged;
    Client client;
    ClientCMD cmdInterface;
    ClientManager manager;

    void setLogged(String _username){
        isLogged = 1;
        userName = _username;
        currentDirectory = "/";

    }
    public ClientApp(String[] args) {

        if (args.length != 3) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        hostName = args[0];
        portNumber = Integer.parseInt(args[1]);
        clientName = args[2];
        isLogged = 0;
        currentDirectory = "";
        userName = "";
        manager = new ClientManager(this);

    }

    class InputListener{
        int checkLogin(JSONObject cmd){
            if(isLogged == 0 && !cmd.get("command").equals("login"))
                return 0;
            return 1;
        }
        void notifyCommand(JSONObject cmd){
            System.out.println(cmd.toString(2));
            if(checkLogin(cmd) == 0){
                System.out.println("Please log in");
                return;
            }

            if(isLogged == 0){
                assert(cmd.get("command").equals("login"));

            }

            else if(isLogged == 1){
                cmd.put("username" , userName);
                cmd.put("directory",currentDirectory);
            }
            client.sendTCP(cmd);
        }
    }

    public void run(){
        client = new Client();

        client.start();
        client.getKryo().register(JSONObject.class);
        client.getKryo().register(java.util.HashMap.class);


        //return;

        System.out.println(hostName + portNumber);
        try {
            client.connect(7000, hostName, portNumber);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        cmdInterface = new ClientCMD();
        cmdInterface.setListener(new InputListener());
        Thread cmdThread = new Thread(cmdInterface);
        cmdThread.start();

        client.addListener(new Listener.ThreadedListener(new Listener() {
            public void received (Connection connection, Object object) {
                if (object instanceof JSONObject) {
                    JSONObject answer = (JSONObject) object;
                    assert(answer != null);
                    manager.handleResponse(answer);
                }
            }
        }));

       /* for(int i = 0 ; i < 5 ; i++){
            JSONObject obj = new JSONObject();
            obj.put("sender" , clientName);
            obj.put("task" , String.valueOf(i));
            client.sendTCP(obj);
        }*/



    }
}
