package newdist;


import org.json.JSONObject;
import org.springframework.util.FileCopyUtils;
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

    JSONObject format(JSONObject job){
        String dir = "./" + job.getString("username") + "/";
        Path path = Paths.get(dir);
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            } else {
                File f = new File(dir);
                boolean res = FileSystemUtils.deleteRecursively(path);
                assert (res);
                Files.createDirectory(path);
            }
        }catch (IOException e){
            return ResponseUtil.getResponse(job , "NO" , "error formatting some datanode");
        }
        System.out.println("data node formatted with no problems");
        return ResponseUtil.getResponse(job , "OK" , "data node was formatted successfully");
    }
    JSONObject delete(JSONObject job) {
        String strPath = "./"+ job.get("username") + job.getString("path");
        File f = new File(strPath);
        if(!f.exists())
            return newdist.ResponseUtil.getResponse(job,"NO","File does not exist on this datanode file: " + strPath);
        if (!f.delete())
            return  ResponseUtil.getResponse(job , "NO" , "Some error on data node disk");

        return newdist.ResponseUtil.getResponse(job, "OK", "file  ought to be deleted");
    }
    JSONObject MvCp(JSONObject job) throws IOException {
        String strPathFrom = "./"+ job.get("username") + job.getString("pathFrom");
        String strPathTo = "./"+ job.get("username") + job.getString("pathTo");

        System.out.println(job.toString());

        File f = new File(strPathFrom);
        if(!f.exists())
            return newdist.ResponseUtil.getResponse(job,"NO","File does not exist on this datanode file: "+ strPathFrom);

        Path targetDir = Paths.get(strPathTo).getParent();
        if(targetDir != null && !Files.exists(targetDir))
            Files.createDirectories(targetDir);

        System.out.println(job.toString());
        if(job.getString("command").equals("mv")){
            Files.move(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        else{
            Files.copy(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        return newdist.ResponseUtil.getResponse(job,"OK","Great Success");
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

    JSONObject removeDirectory(JSONObject job) throws IOException {
        String strPath = "./"+ job.get("username") + job.getString("path");

        File f = new File(strPath);

        if(!f.exists() || !f.isDirectory()) {
            throw new IOException("Inconsisitent: directory doesn't exist on data node");
       //     return newdist.ResponseUtil.getResponse(job, "NO", "File is not dir or does not even exist file: " + strPath);
        }

        if(job.getString("force").equals("none")){
            if(f.listFiles().length!=0) {
                throw new IOException("Inconsistent: directory contains files on datanode and seems empty on namenode");
                //return newdist.ResponseUtil.getResponse(job, "NO", "DataNode error permission to remove all files within directory");
            }
        }
        boolean res = FileSystemUtils.deleteRecursively(f);
        assert (res);
        return newdist.ResponseUtil.getResponse(job,"OK","Directory was removed from datanode successfully");
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
        if (job.get("command").equals("format"))
            return format(job);
        if (job.get("command").equals("delete"))
            return delete(job);
        if(job.getString("command").equals("info"))
            return info(job);
        if(job.getString("command").equals("cp")||job.getString("command").equals("mv"))
            return MvCp(job);;
        if(job.getString("command").equals("rmdir"))
            return removeDirectory(job);

        JSONObject crap = new JSONObject();
        crap.put("status", "wholyshit");
        return crap;
    }



}
