package newdist;


import java.io.*;
import java.net.*;
import java.util.Scanner;

import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.minlog.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ClientApp {

    String hostName , clientName , currentDirectory , userName;
    private int portNumber;
    private int isLogged;

    private ThreadSafeClient client;
    private ClientManager manager;

    ClientCMD cmdInterface;

    void setLogged(String _username){
        isLogged = 1;
        userName = _username;
        currentDirectory = "/";
    }
    int isLoggedIn(){
        return isLogged;
    }

    ClientApp(String [] args){
        hostName = args[0];
        portNumber = Integer.parseInt(args[1]);
        clientName = args[2];
        isLogged = 0;
        currentDirectory = "";
        userName = "";
        manager = new ClientManager(this);
    }

    public static void main(String[] args) throws IOException {
        args = new String[3];
        File f = new File("./client_hosts.conf");
        System.out.println(new File(".").getCanonicalPath());
        if(!f.exists() || f.isDirectory()) {
            System.out.println("please read readmefile and configure client_hosts.conf file");
            return;
        }
        Scanner sc = new Scanner(f);
        int ind = 0;
        while(sc.hasNext()){
            if(ind>=3){
                System.out.println("Too many args check client_hosts.conf");
            }
            args[ind++] = sc.next();
        }

        if (args.length != 3) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }
        ClientApp app = new ClientApp(args);
        app.run();
    }


    void notify(JSONObject cmd){

        try{

            System.out.println(cmd.toString());
            Object ret = client.sendSafeTCP(cmd , new JSONObject());
            JSONObject response = (JSONObject)(ret);
            manager.handleResponse(response);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    public void run(){

        client = new ThreadSafeClient(hostName , portNumber);
        client.getKryo().register(JSONObject.class);
        client.getKryo().register(java.util.HashMap.class);
        client.Launch();


        cmdInterface = new ClientCMD(this);
        Thread cmdThread = new Thread(cmdInterface);
        cmdThread.start();

    }
}
