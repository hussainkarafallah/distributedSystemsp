package newdist;


import org.json.JSONObject;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.SocketUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Date;



public class DataNodeManager {

    DataNode dataNode;

    DataNodeManager(DataNode _dataNode) {
        assert (dataNode != null);
        dataNode = _dataNode;
    }

    static class ResponseUtil {
        static JSONObject getResponse(JSONObject command, String status, String report) {
            JSONObject ret = new JSONObject(command, JSONObject.getNames(command));
            assert (command.get("command") != ret.get("command"));
            ret.put("status", status);
            ret.put("report", report);
            return ret;
        }
    }

    void startDownloadJob(JSONObject job) {
        String ip = job.getString("ip");
        int port = job.getInt("port");
        String path = job.getString("writepath");
        Downloader downloader = new Downloader(ip, port, path);

    }
    JSONObject info(JSONObject job) {
        String strPath = "./"+ job.get("username") + job.getString("path");

        File f = new File(strPath);
        if(!f.exists())
            return newdist.ResponseUtil.getResponse(job,"ERR","File does not exist on this datanode file: "+strPath);
        Integer size = (int)f.length();
        long secs = f.lastModified();
        String dt = new Date(secs).toString();
        StringBuilder sb = new StringBuilder();
        sb.append("Size of the file: ");
        sb.append(size);
        sb.append(" Bytes");
        sb.append("\nLast modified: ").append(dt).append("\n");

        return newdist.ResponseUtil.getResponse(job, "OK", sb.toString());
    }
    JSONObject MvCp(JSONObject job) throws IOException {
        String strPathFrom = "./"+ job.get("username") + job.getString("pathFrom");
        String strPathTo = "./"+ job.get("username") + job.getString("pathTo");

        File f = new File(strPathFrom);
        if(!f.exists())
            return newdist.ResponseUtil.getResponse(job,"ERR","File does not exist on this datanode file: "+strPathFrom);
        if(job.getString("command").equals("mv")){
            Files.move(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        else{
            Files.copy(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        return newdist.ResponseUtil.getResponse(job,"OK","Great Success");
    }
    JSONObject delete(JSONObject job) {
        String strPath = "./"+ job.get("username") + job.getString("path");
        File f = new File(strPath);
        String status = "OKz";

        if(!f.exists())
            return newdist.ResponseUtil.getResponse(job,"ERR","File does not exist on this datanode file: "+strPath);
        System.out.println(Boolean.toString(f.exists()));
        if (!f.delete())
            status = "Error";
        return newdist.ResponseUtil.getResponse(job, status, "file  ought to be deleted");
    }

    JSONObject performJob(JSONObject job) throws IOException {
        System.out.println("Wohoo we have new job "+job.getString("command") + " thats it");
        if (job.get("command").equals("connect"))
            return ResponseUtil.getResponse(job, "OK", "datanode " + dataNode.dataNodeName + ":" + dataNode.portNumber + " is reached");
        if (job.get("command").equals("startdownload")) {
            startDownloadJob(job);
            return ResponseUtil.getResponse(job, "OK", "File found on data node");
        }
        if (job.get("command").equals("startupload")) {
            //startDownloadJob(job);
            System.out.println(job.toString());
            JSONObject response = ResponseUtil.getResponse(job, "OK", "OK");
            response.remove("command");
            response.put("command", "launchdownload");
            int port = SocketUtils.findAvailableTcpPort();
            System.out.println("here is the shit " + response.toString());
            Uploader uploader = new Uploader(port, job.getString("serverpath"), null, null, null);
            try {
                response.put("ip", InetAddress.getLocalHost().getHostAddress());
                response.put("port", port);
                System.out.println("here is the shit II" + response.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("responded with " + response.toString());
            return response;

        }
        if (job.get("command").equals("delete")) {
            return delete(job);
        }
        if(job.getString("command").equals("info"))
            return info(job);
        if(job.getString("command").equals("cp")||job.getString("command").equals("mc"))
            return MvCp(job);
        JSONObject crap = new JSONObject();
        crap.put("status", "wholyshit");
        return crap;
    }


     /*   private JSONObject create(){

    }

    private JSONObject read(){

    }

    private JSONObject write(){

    }

    private JSONObject delete(){

    }

    private JSONObject getInfo(){

    }

    private JSONObject copy(){

    }

    private JSONObject move(){

    }

    private JSONObject opendir(){

    }

    private JSONObject readdir(){

    }

    private JSONObject makedir(){

    }

    private JSONObject deletedir(){

    }*/
}
