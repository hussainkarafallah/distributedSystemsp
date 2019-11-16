package newdist;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;


class NameNodeProxy implements Runnable{

    List<ThreadSafeClient> sockets;
    ArrayList<InetSocketAddress> dataNodes;
     //   List<Semaphore> semaphores;

    Random rng = new Random(1997);

    InetSocketAddress getAvailableDataNode(){

        int sz = dataNodes.size();

        if(sz == 0) return null;

        int idx = Math.abs(rng.nextInt());

        return dataNodes.get(idx % sz);

    }

    public JSONObject forwardJobToAll(JSONObject job){
        return forwardJob(dataNodes , job);
    }
    public JSONObject forwardJob(ArrayList<InetSocketAddress>addresses , JSONObject job){
        JSONObject ret;
        String errors = "";
        for(InetSocketAddress dataNode : addresses){
            int idx = dataNodes.indexOf(dataNode);
            if(idx == -1){
                System.out.println("we need to discuss this later");
                continue;
            }

            try {
                System.out.println(dataNode.getPort());
                Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
                ret = (JSONObject) (response);
                assert (ret != null);
                if(!ret.get("status").equals("OK"))
                    errors=errors.concat("\n" + ret.get("report"));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        if(errors.isEmpty()){
            return ResponseUtil.getResponse(job , "OK" , "forwarding was successful");
        }

        return ResponseUtil.getResponse(job , "NO" , errors);
    }

    public JSONObject askForUpload(InetSocketAddress dataNode , JSONObject _job){
        int idx = dataNodes.indexOf(dataNode);
        System.out.println("new request to fwd" + _job.toString());
        assert(idx != -1);
        JSONObject job = new JSONObject(_job, JSONObject.getNames(_job));
        job.remove("command");
        job.put("command" , "startupload");
        JSONObject ret = ResponseUtil.getResponse(job , "No" , "unknown error happened");
        try {
            System.out.println("new request22 to fwd" + job.toString());
            System.out.println("namenode proxy thread" + Thread.currentThread());
            Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
            ret = (JSONObject)(response);
            assert(ret != null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    public JSONObject askForDelete(InetSocketAddress dataNode , JSONObject job){
        int idx = dataNodes.indexOf(dataNode);

        assert(idx != -1);

        JSONObject ret = ResponseUtil.getResponse(job , "No" , "unknown error happened");
        try {
            System.out.println("new request22 to fwd" + job.toString());
            System.out.println("namenode proxy thread" + Thread.currentThread());
            Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
            ret = (JSONObject)(response);
            assert(ret != null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }
    public JSONObject manipulateDir(InetSocketAddress dataNode, JSONObject job){
        int idx = dataNodes.indexOf(dataNode);

        assert(idx != -1);

        JSONObject ret = ResponseUtil.getResponse(job , "No" , "unknown error happened");
        try {
            System.out.println("new request22 to fwd" + job.toString());
            System.out.println("namenode proxy thread" + Thread.currentThread());
            Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
            ret = (JSONObject)(response);
            assert(ret != null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }
    public  JSONObject MvCp(InetSocketAddress dataNode , JSONObject job){
        int idx = dataNodes.indexOf(dataNode);

        assert(idx != -1);

        JSONObject ret = ResponseUtil.getResponse(job , "No" , "unknown error happened");
        try {
            System.out.println("new request22 to fwd" + job.toString());
            System.out.println("namenode proxy thread" + Thread.currentThread());
            Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
            ret = (JSONObject)(response);
            assert(ret != null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }
    public JSONObject askForInfo(InetSocketAddress dataNode , JSONObject job){
        int idx = dataNodes.indexOf(dataNode);

        assert(idx != -1);

        JSONObject ret = ResponseUtil.getResponse(job , "No" , "unknown error happened");
        try {
            System.out.println("new request22 to fwd" + job.toString());
            System.out.println("namenode proxy thread" + Thread.currentThread());
            Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
            ret = (JSONObject)(response);
            assert(ret != null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    public boolean isAvailable(InetSocketAddress dataNode){
        return dataNodes.indexOf(dataNode) != -1;
    }

    public boolean addDataNode(final InetSocketAddress datanode){

        if( dataNodes.indexOf(datanode) != -1)
            return false;

        dataNodes.add(datanode);


        ThreadSafeClient client = new ThreadSafeClient(datanode.getAddress().getHostAddress(), datanode.getPort());

        Kryo kryo = client.getKryo();
        kryo.register(JSONObject.class);
        kryo.register(java.util.HashMap.class);

        client.Launch();


        System.out.println("Trying to add datanode");

        sockets.add(client);

        //semaphores.add(new Semaphore(1));

        JSONObject request = new JSONObject();
        request.put("command" , "connect");
        try {
            Object ret = client.sendSafeTCP(request, new JSONObject());
            JSONObject response = (JSONObject)(ret);
            handle(response);
        }
        catch (Exception e){
            e.printStackTrace();
        }


        return true;

    }

    void handle(JSONObject response){
        if(response.getString("command").equals("connect")){
            if(response.getString("status").equals("OK")){
                System.out.println(response.getString("report"));
            }
            return;
        }

    }

    public void run(){
        sockets = new ArrayList<>();
        dataNodes = new ArrayList<>();
        //semaphores = new ArrayList<>();

        System.out.println("Hey proxy started :)");


    }

}