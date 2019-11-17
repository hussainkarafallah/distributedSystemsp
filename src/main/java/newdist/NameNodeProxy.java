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
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;



class NameNodeProxy implements Runnable{

    final int replicationFactor = 2;

    List<ThreadSafeClient> sockets;

    ArrayList<InetSocketAddress> dataNodes;
    HashMap<InetSocketAddress,ArrayList<JSONObject>> pending = new HashMap<>();
     //   List<Semaphore> semaphores;

    Random rng = new Random(1997);

    InetSocketAddress getAvailableDataNode(){

        int sz = dataNodes.size();

        if(sz == 0) return null;

        int idx = Math.abs(rng.nextInt());

        return dataNodes.get(idx % sz);

    }

    ArrayList < InetSocketAddress > getReplicas(InetSocketAddress mainNode){
        //System.out.println(dataNodes.size());
        ArrayList < InetSocketAddress > ret = new ArrayList<>();
        int take = Math.min(dataNodes.size() - 1 , replicationFactor - 1);
        take = Math.max(take , 0);
        while (ret.size() < take){
            int idx = Math.abs(rng.nextInt()) % dataNodes.size();
            InetSocketAddress add = dataNodes.get(idx);
            if(add.equals(mainNode)) continue;
            if(ret.indexOf(add) != -1) continue;
            ret.add(dataNodes.get(idx));
        }
        return ret;
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
                if(!pending.containsKey(dataNode))
                    pending.put(dataNode,new ArrayList<JSONObject>());
                pending.get(dataNode).add(job);
                continue;

            }

            try {
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
    public JSONObject getDFSsize(JSONObject job){
        JSONObject response = newdist.ResponseUtil.getResponse(job,"OK","");
        long tsize=0,fsize=0;
        for(ThreadSafeClient s : sockets) {
            try {
                JSONObject temp =(JSONObject)s.sendSafeTCP(job, new JSONObject());
                tsize+=Long.parseLong(temp.getString("tsize"));
                fsize+=Long.parseLong(temp.getString("fsize"));
            }
            catch (Exception e){
                e.printStackTrace();
            }

        }
        System.out.println("size debug");
        System.out.println(fsize);
        System.out.println(tsize);
        StringBuilder sb = new StringBuilder();
        sb.append("MullanurovDFS has space of (free/total): ");
        sb.append(humanReadableByteCount(fsize,false));
        sb.append("/"+humanReadableByteCount(tsize,false));
        sb.append("\n Free space "+String.valueOf(fsize*100/tsize)+"%, Format successful");
        response.put("report",sb.toString());
        return response;
    }
    public static String humanReadableByteCount(long bytes, boolean si) {
        return String.valueOf(bytes/(1<<30)) + " GB";
    }
    public JSONObject askForUpload(InetSocketAddress dataNode , JSONObject _job){
        int idx = dataNodes.indexOf(dataNode);
        assert(idx != -1);
        JSONObject job = new JSONObject(_job, JSONObject.getNames(_job));
        job.remove("command");
        job.put("command" , "startupload");
        JSONObject ret = ResponseUtil.getResponse(job , "No" , "unknown error happened");
        try {

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


        client.addListener(new Listener.ThreadedListener(new Listener(){
            @Override
            public void disconnected(Connection connection) {
                System.out.println(" one datanode disconnected");
                int i =0;
                for(ThreadSafeClient s : sockets){
                    if(!s.isConnected())break;
                    i++;
                }
                dataNodes.remove(i);
                sockets.remove(i);

            }
        }));


        System.out.println("Trying to add datanode " + datanode.getAddress().getHostAddress() + ":" + datanode.getPort());

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
        if(pending.containsKey(datanode)){
            for(JSONObject j : pending.get(datanode)){
                try {
                    Object ret = client.sendSafeTCP(j, new JSONObject());
                    JSONObject response = (JSONObject)ret;
                    System.out.println(response.toString());
                }
                catch (Exception e){

                }
            }
            pending.remove(datanode);
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