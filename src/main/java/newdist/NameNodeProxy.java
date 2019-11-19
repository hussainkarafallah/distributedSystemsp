package newdist;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Semaphore;


class NameNodeProxy implements Runnable {

    final int replicationFactor = 2;

    List<ThreadSafeClient> sockets;

    ArrayList<InetSocketAddress> dataNodes;
    ArrayList<InetSocketAddress> historyDataNodes = new ArrayList<>();
    ArrayList<File> toReplicate = new ArrayList<>();
    HashMap<InetSocketAddress, ArrayList<JSONObject>> pending = new HashMap<>();
    ArrayList < String > usersToFormat = new ArrayList<>();



    //   List<Semaphore> semaphores;

    Random rng = new Random(1997);

    InetSocketAddress getAvailableDataNode() {

        int sz = dataNodes.size();

        if (sz == 0) return null;

        int idx = Math.abs(rng.nextInt());

        return dataNodes.get(idx % sz);

    }

    ArrayList<InetSocketAddress> getReplicas(InetSocketAddress mainNode) {
        //System.out.println(dataNodes.size());
        ArrayList<InetSocketAddress> ret = new ArrayList<>();
        int take = Math.min(dataNodes.size() - 1, replicationFactor - 1);
        take = Math.max(take, 0);
        while (ret.size() < take) {
            int idx = Math.abs(rng.nextInt()) % dataNodes.size();
            InetSocketAddress add = dataNodes.get(idx);
            if (add.equals(mainNode)) continue;
            if (ret.indexOf(add) != -1) continue;
            ret.add(dataNodes.get(idx));
        }
        return ret;
    }

    public JSONObject forwardJobToAll(JSONObject job) {
        return forwardJob(dataNodes, job);
    }

    public JSONObject forwardJob(ArrayList<InetSocketAddress> addresses, JSONObject job) {
        JSONObject ret;
        String errors = "";
        for (InetSocketAddress dataNode : addresses) {
            int idx = dataNodes.indexOf(dataNode);
            if (idx == -1) {
                System.out.println("we need to discuss this later");
                if (!pending.containsKey(dataNode))
                    pending.put(dataNode, new ArrayList<JSONObject>());
                pending.get(dataNode).add(job);
                continue;

            }

            try {
                Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
                ret = (JSONObject) (response);
                assert (ret != null);
                if (!ret.get("status").equals("OK"))
                    errors = errors.concat("\n" + ret.get("report"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (errors.isEmpty()) {
            return ResponseUtil.getResponse(job, "OK", "forwarding was successful");
        }

        return ResponseUtil.getResponse(job, "NO", errors);
    }

    public JSONObject getDFSsize(JSONObject job) {
        JSONObject response = newdist.ResponseUtil.getResponse(job, "OK", "");
        if(sockets.size()==0){
            return ResponseUtil.getResponse(job,"NO","Datanodes are offline");
        }
        long tsize = 0, fsize = 0;
        for (ThreadSafeClient s : sockets) {
            try {
                JSONObject temp = (JSONObject) s.sendSafeTCP(job, new JSONObject());
                tsize += Long.parseLong(temp.getString("tsize"));
                fsize += Long.parseLong(temp.getString("fsize"));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        StringBuilder sb = new StringBuilder();
        sb.append("MullanurovDFS has space of (free/total): ");
        sb.append(humanReadableByteCount(fsize, false));
        sb.append("/" + humanReadableByteCount(tsize, false));
        sb.append("\n Free space " + String.valueOf(fsize * 100 / tsize) + "%, Format successful");
        response.put("report", sb.toString());
        return response;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        return String.valueOf(bytes / (1 << 30)) + " GB";
    }

    public JSONObject askForUpload(InetSocketAddress dataNode, JSONObject _job) {
        int idx = dataNodes.indexOf(dataNode);
        assert (idx != -1);
        JSONObject job = new JSONObject(_job, JSONObject.getNames(_job));
        job.remove("command");
        job.put("command", "startupload");
        JSONObject ret = ResponseUtil.getResponse(job, "No", "unknown error happened");
        try {

            Object response = sockets.get(idx).sendSafeTCP(job, new JSONObject());
            ret = (JSONObject) (response);
       //     ret.put("ip" , dataNodes.get(idx).getAddress().getHostAddress());
            assert (ret != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }


    public boolean isAvailable(InetSocketAddress dataNode) {
        return dataNodes.indexOf(dataNode) != -1;
    }

    public boolean addDataNode(final InetSocketAddress datanode) {

        if (dataNodes.indexOf(datanode) != -1)
            return false;

        dataNodes.add(datanode);


        InetSocketAddress dataNodeDispatcher = new InetSocketAddress(datanode.getAddress().getHostAddress(), datanode.getPort());
        ThreadSafeClient client = new ThreadSafeClient(datanode.getAddress().getHostAddress(), datanode.getPort());

        Kryo kryo = client.getKryo();
        kryo.register(JSONObject.class);
        kryo.register(java.util.HashMap.class);

        client.Launch();


        client.addListener(new Listener.ThreadedListener(new Listener() {
            @Override
            public void disconnected(Connection connection) {
                System.out.println(" one datanode disconnected");
                int i = 0;
                for (ThreadSafeClient s : sockets) {
                    if (!s.isConnected()) break;
                    i++;
                }

                dataNodes.remove(i);
                sockets.remove(i);
                insure_rep();

            }
        }));



        System.out.println("Trying to add datanode " + datanode.getAddress().getHostAddress() + ":" + datanode.getPort());

        sockets.add(client);



        //semaphores.add(new Semaphore(1));

        JSONObject request = new JSONObject();
        request.put("command", "connect");
        try {
            Object ret = client.sendSafeTCP(request, new JSONObject());
            JSONObject response = (JSONObject) (ret);
            handle(response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!historyDataNodes.contains(datanode)){
            historyDataNodes.add(datanode);
            for(String user : usersToFormat){
                JSONObject job = new JSONObject();
                job.put("command","format");
                job.put("username",user);
                try{
                    client.sendSafeTCP(job , new JSONObject());
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
        if (pending.containsKey(datanode)) {
            for (JSONObject j : pending.get(datanode)) {
                try {
                    Object ret = client.sendSafeTCP(j, new JSONObject());
                    JSONObject response = (JSONObject) ret;
                    System.out.println(response.toString());
                } catch (Exception e) {

                }
            }
            pending.remove(datanode);
        }

        insure_rep();
        return true;

    }

    void handle(JSONObject response) {
        if (response.getString("command").equals("connect")) {
            if (response.getString("status").equals("OK")) {
                System.out.println(response.getString("report"));
            }
            return;
        }

    }

    int check_rep(File f) throws FileNotFoundException {

        Scanner sc = new Scanner(f);

        JSONObject response = new JSONObject();
        int rep = 0;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) continue;
            JSONObject obj = new JSONObject(line);
            assert (obj != null);
            if (!obj.getString("type").equals("replica") && !obj.getString("type").equals("mainnode"))
                continue;
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            InetSocketAddress address = new InetSocketAddress(ip, port);

            if (isAvailable(address))
                rep++;
        }
        return rep;
    }
    ArrayList<InetSocketAddress> getReplicasExcept(ArrayList<InetSocketAddress> avoid,int need) {
        //System.out.println(dataNodes.size());
        ArrayList<InetSocketAddress> ret = new ArrayList<>();
        int take = Math.min(dataNodes.size() - 1, need);
        take = Math.max(take, 0);
        while (ret.size() < take) {
            int idx = Math.abs(rng.nextInt()) % dataNodes.size();
            InetSocketAddress add = dataNodes.get(idx);
            if (avoid.indexOf(add)!=-1) continue;
            if (ret.indexOf(add) != -1) continue;
            ret.add(dataNodes.get(idx));
        }
        return ret;
    }
    private ArrayList<InetSocketAddress> getDataNodesFromFile(File f) throws IOException {

        ArrayList<InetSocketAddress> vec = new ArrayList<InetSocketAddress>();
        Scanner sc = new Scanner(f);
        JSONObject response = new JSONObject();
        int total = 0, found = 0;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) continue;
            JSONObject obj = new JSONObject(line);
            assert (obj != null);
            if(!obj.getString("type").equals("replica") && !obj.getString("type").equals("mainnode"))
                continue;
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            InetSocketAddress address = new InetSocketAddress(ip, port);

            vec.add(address);

        }
        return vec;
    }
    public void walk(String path,int depth) throws FileNotFoundException {

        File root = new File(path);
        File[] list = root.listFiles();

        if (list == null) return;

        for (File f : list) {
            if (f.isDirectory()) {
                walk(f.getAbsolutePath(),depth+1);
            } else {
                if(depth == 0)continue;;
                if(check_rep(f)<replicationFactor)
                    toReplicate.add(f);
            }
        }
    }
    void addInfo(File f,InetSocketAddress replicaNode ) throws IOException {

        FileWriter fw = new FileWriter(f , true);
        PrintWriter pw = new PrintWriter(fw);


        JSONObject meta = new JSONObject();
        meta.put("ip" , replicaNode.getAddress().getHostAddress());
        meta.put("port",replicaNode.getPort());
        meta.put("type","replica");

        pw.println(meta);

        pw.close();
        try {
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    void insure_rep(){
        toReplicate.clear();

        try {
            File oo = new File(".");
            String toCut = oo.getCanonicalPath() + "/namenode/";
            walk("./namenode/",0);

            for(File f: toReplicate){
                ArrayList<InetSocketAddress> blackList = getDataNodesFromFile(f);
                ArrayList<InetSocketAddress> replicas = getReplicasExcept(blackList,replicationFactor-check_rep(f));
//                System.out.println(f.toString());
//                System.out.println(blackList.toString());
//                System.out.println(replicas.size());
                String jd3 = f.getCanonicalPath();
                jd3 = jd3.replace(toCut , "./");
//                System.out.println(jd3);
                InetSocketAddress alive = null;
                for(InetSocketAddress b:blackList){
                    if(isAvailable(b)){
                        alive=b;
                        break;
                    }
                }
                if(alive == null) continue;;
                for(InetSocketAddress d : replicas){
                    JSONObject job = new JSONObject();
                    job.put("command", "replicate");
                    job.put("ip",d.getAddress().getHostAddress());
                    job.put("port",d.getPort());
                    job.put("filepath",jd3);
                    int i = dataNodes.indexOf(alive);
                    addInfo(f,d);
                    sockets.get(i).sendSafeTCP(job,new JSONObject());
                    System.out.println("File " + jd3 + " is being replicated to " + d.toString());
                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void run() {
        sockets = new ArrayList<>();
        dataNodes = new ArrayList<>();
        //semaphores = new ArrayList<>();

        System.out.println("Hey proxy started :)");


    }

}