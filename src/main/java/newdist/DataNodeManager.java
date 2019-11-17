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
        JSONObject response = ResponseUtil.getResponse(job , "OK" , "data node was formatted successfully");
        File f = new File(".");


        return response;
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

        File f = new File(strPathFrom);
        if(!f.exists())
            return newdist.ResponseUtil.getResponse(job,"NO","File does not exist on this datanode file: "+ strPathFrom);

        Path targetDir = Paths.get(strPathTo).getParent();
        if(targetDir != null && !Files.exists(targetDir))
            Files.createDirectories(targetDir);

        if(job.getString("command").equals("mv")){
            Files.move(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        else{
            Files.copy(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        return newdist.ResponseUtil.getResponse(job,"OK","Great Success");
    }

    void replicate(String filePath , String replicasString){
        Scanner sc = new Scanner(replicasString);
        while(sc.hasNextLine()){
            String line = sc.nextLine();
            if(line.isEmpty()) continue;
            JSONObject replica = new JSONObject(line);
            System.out.println(replica.toString());

            String dataNodeIP = replica.getString("ip");
            int dataNodePort = replica.getInt("port")       ;
            String clientPath    = filePath;
            String writePath = filePath;

            JSONObject additionalInfo = new JSONObject();
            additionalInfo.put("replication","YES");
            int port = SocketUtils.findAvailableTcpPort();

            Uploader uploader = new Uploader(port , clientPath , writePath , new InetSocketAddress(dataNodeIP , dataNodePort) , additionalInfo);
        }
    }
    void askForReplicas(JSONObject fileJob){
        JSONObject job = new JSONObject();
        job.put("command" , "getreplicas");
        job.put("path",fileJob.get("writepath"));
        try{
            Object obj = dataNode.nameNodeClient.sendSafeTCP(job , new JSONObject());
            JSONObject response = (JSONObject)(obj);
            System.out.println(fileJob.toString());

            if(response.getString("status").equals("NO")){
                throw new Exception("some weird error happened at name node");
            }
            replicate(fileJob.getString("writepath") , response.getString("replicas"));
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    void startDownloadJob(JSONObject job) {
        String ip = job.getString("ip");
        int port = job.getInt("port");
        String path = job.getString("writepath");
        Downloader downloader = new Downloader(ip, port, path);
        if(job.getString("replication").equals("NO"))
            askForReplicas(job);

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
    JSONObject getDFSsize(JSONObject job){
        File f = new File(".");
        long fsize = f.getFreeSpace();
        long tsize = f.getTotalSpace();
        JSONObject response = newdist.ResponseUtil.getResponse(job,"OK","");
        response.put("fsize",String.valueOf(fsize) );
        response.put("tsize",String.valueOf(tsize));
        System.out.println("Size debug");
        System.out.println(fsize);
        System.out.println(tsize);
        return response;
    }


    JSONObject performJob(JSONObject job) throws IOException {
       // System.out.println("Wohoo we have new job "+job.getString("command") + " thats it");
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
            Uploader uploader = new Uploader(port, job.getString("serverpath"), null, null, null);
            try {
                response.put("ip", InetAddress.getLocalHost().getHostAddress());
                response.put("port", port);

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
        if(job.getString("command").equals("getsize"))
            return getDFSsize(job);
     //   if(job.getString("command").equals("create"))
     //       return createFile(job);

        JSONObject crap = new JSONObject();
        crap.put("status", "wholyshit");
        return crap;
    }



}
