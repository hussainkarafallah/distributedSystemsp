package newdist;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.springframework.util.FileSystemUtils;
import org.springframework.util.FileSystemUtils.*;

class ResponseUtil{
    static  JSONObject getResponse(JSONObject command , String status , String report){
        JSONObject ret = new JSONObject(command, JSONObject.getNames(command));
        assert(command.get("command") != ret.get("command"));
        ret.put("status",status);
        ret.put("report" , report);
        return ret;
    }
}
class NameNodeManager {

    NameNode nameNode;

    final String defaultDir = "./namenode/";

    NameNodeManager(NameNode _nameNode){
        assert(_nameNode !=  null);
        nameNode = _nameNode;
    }

    JSONObject performJob(JSONObject job){
        try {
            if (job.get("command").equals("login"))
                return login(job);
            if (job.get("command").equals("format"))
                return format(job);
            if (job.get("command").equals("upload"))
                return upload(job);
            if(job.get("command").equals("download"))
                return download(job);
        }
        catch (IOException e){
            e.printStackTrace();
            return ResponseUtil.getResponse(job , "NO" , "Namenode storage error");
        }

        JSONObject crap = new JSONObject();
        crap.put("status" , "wholyshit");
        return crap;
    }

    private JSONObject login(JSONObject job) {

        int found = 0;
        String username = job.getString("username");
        String password = job.getString("password");
        File f = new File("users.conf");
        try(Scanner sc = new Scanner(f)){
            while(sc.hasNextLine()){
                String line = sc.nextLine();
                JSONObject curline = new JSONObject(line);
                System.out.println(curline.toString());
                if(curline.isNull(username)) continue;
                if(curline.getString(username) != null)
                    if(curline.getString(username).equals(password)){
                        found = 1;
                        break;
                    }
            }
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }

        if(found == 1)
            return ResponseUtil.getResponse(job , "OK" , "Login Successful");
        return ResponseUtil.getResponse(job , "NO" , "Login Failed");

    }

    private JSONObject format(JSONObject job) throws IOException{
        //this function needs to be upgraded to delete all files but for now it just creates new directory
        String dir = defaultDir + job.getString("username") + "/";
        Path path = Paths.get(dir);
        if(Files.notExists(path)){
                Files.createDirectories(path);
        }
        else{
            File f = new File(dir);
            boolean res = FileSystemUtils.deleteRecursively(path);
            Files.createDirectory(path);
        }
        return ResponseUtil.getResponse(job , "OK" , "Formatted Successfully");
    }

    private JSONObject download(JSONObject job) throws IOException{
        String strPath = defaultDir + job.get("username") + job.getString("serverpath");
        String solPath = job.getString("serverpath");

        job.remove("serverpath");
        job.put("serverpath" , "./" + job.get("username") + "/" + solPath);

        Path path = Paths.get(strPath);

        if(!Files.exists(path)){
           return ResponseUtil.getResponse(job , "NO" , "file doesn't exist on the server");
        }

        JSONObject response =  ResponseUtil.getResponse(job , "OK" , "file normally exists");

        int found = 0;

        File fileToRead = new File(path.toString());

        Scanner sc = new Scanner(fileToRead);

        while(sc.hasNextLine()){
            String line = sc.nextLine();
            if(line.isEmpty()) continue;
           // System.out.println(line);
            JSONObject obj = new JSONObject(line);
            assert(obj != null);
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            InetSocketAddress add = new InetSocketAddress(ip , port);

            if(nameNode.proxy.isAvailable(add)){
                found = 1;

                System.out.println("namenode manager thread" + Thread.currentThread());

                response = nameNode.proxy.askForUpload(add , job);

                System.out.println("jaaaa  " + response.toString() + " " + response);

                assert(response != null);

                System.out.println("jaaaa  " + response.toString() + " " + response);

                break;
            }
        }
        System.out.println("what the shit why no printing");
        System.out.println(found);
        if(found == 0)  return  ResponseUtil.getResponse(job , "NO" , "File is temporarily unavailable");
        System.out.println("Found" + response.toString(2));
        return response;
    }
    private JSONObject upload(JSONObject job) throws IOException{


        String strPath = defaultDir + job.get("username") + job.getString("writepath");
        String solPath = job.getString("writepath");

        job.remove("writepath");
        job.put("writepath" , "./" + job.get("username") + "/" + solPath);

        Path path = Paths.get(strPath);
        Files.deleteIfExists(path);

        Path par = path.getParent();
        if(par != null && !Files.exists(par))
            Files.createDirectories(par);

        Files.createFile(path);

        //System.out.println(path.getFileName());
        InetSocketAddress datanode = nameNode.proxy.getAvailableDataNode();

        JSONObject response =  ResponseUtil.getResponse(job , "OK" , "namenode created the file");

        response.put("datanodeip", datanode.getAddress().getHostAddress());
        response.put("datanodeport", datanode.getPort());

        JSONObject meta = new JSONObject();
        meta.put("type","mainnode");
        meta.put("ip", datanode.getAddress().getHostAddress());
        meta.put("port", datanode.getPort());

        PrintWriter pw = new PrintWriter(path.toString());
        pw.write(meta.toString() + "\n");
        pw.flush(); pw.close();

        return response;



    }
 /*   private JSONObject create(){

    }

    private JSONObject read(){

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
