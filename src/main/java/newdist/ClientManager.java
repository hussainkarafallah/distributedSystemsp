package newdist;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.SocketUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ClientManager {
    private ClientApp client;
    ClientManager(ClientApp _client){
        assert (_client != null);
        client = _client;
    }
    void normalHandling(JSONObject response){
     //   System.out.println(response.toString(2));
        System.out.println(response.getString("report"));
    }

    void downloadHandler(JSONObject job){

        String ip = job.getString("ip");
        int port = job.getInt("port");
        String path = job.getString("writepath");
        System.out.println(job.toString(2));
        try{ Thread.sleep(500); } catch (Exception e){ e.printStackTrace(); }
        Downloader downloader = new Downloader(ip , port , path);
    }

    void uploadHandler(JSONObject response){
        assert(response != null);
        //System.out.println("####\n"+response.toString(2));

        String dataNodeIP = response.getString("datanodeip");
        int dataNodePort = response.getInt("datanodeport");
        String clientPath = response.getString("clientpath");
        String writePath = response.getString("writepath");

        JSONObject additionalInfo = new JSONObject();
        additionalInfo.put("replication","NO");

        int port = SocketUtils.findAvailableTcpPort();
        Uploader uploader = new Uploader(port , clientPath , writePath , new InetSocketAddress(dataNodeIP , dataNodePort) , additionalInfo);

    }

    void handleResponse(JSONObject response){
        System.out.println(response.toString());
        if(response.getString("command").equals("login")){
            if(response.get("status").equals("OK"))
                client.setLogged(response.getString("username"));
            return;
        }
        if(response.getString("command").equals("upload")){
            if(response.get("status").equals("OK")){
                uploadHandler(response);
                return;
            }
        }
        if(response.getString("command").equals("launchdownload")){
            if(response.get("status").equals("OK")){
                downloadHandler(response);
                return;
            }
        }
        if(response.getString("command").equals("cd")){
            if(response.get("status").equals("OK")){
                client.setDirectory(response.getString("path"));
                return;
            }
        }
        normalHandling(response);

    }

}
