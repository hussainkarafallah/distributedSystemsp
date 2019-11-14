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
    JSONObject ls(JSONObject job){
        String strPath = "./"+ job.get("username") + job.getString("path");
        File folder = new File(strPath);
        File[] listOfFiles = folder.listFiles();
        System.out.println(Boolean.toString(folder.isDirectory()));
        StringBuilder filesListString = new StringBuilder();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                filesListString.append("File ").append(listOfFiles[i].getName()).append("\n");
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
                filesListString.append("Directory ").append(listOfFiles[i].getName()).append("\n");
            }
        }
        return newdist.ResponseUtil.getResponse(job, "OK", filesListString.toString());
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

    JSONObject performJob(JSONObject job) {
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
