package newdist;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.springframework.util.FileSystemUtils;
import org.springframework.util.FileSystemUtils.*;

class ResponseUtil {
    static JSONObject getResponse(JSONObject command, String status, String report) {
        JSONObject ret = new JSONObject(command, JSONObject.getNames(command));
        assert (command.get("command") != ret.get("command"));
        ret.put("status", status);
        ret.put("report", report);
        return ret;
    }
}

class NameNodeManager {

    NameNode nameNode;

    final String defaultDir = "./namenode/";

    NameNodeManager(NameNode _nameNode) {
        assert (_nameNode != null);
        nameNode = _nameNode;
    }

    JSONObject performJob(JSONObject job) {
        try {
            if (job.get("command").equals("login"))
                return login(job);
            if (job.get("command").equals("format"))
                return format(job);
            if (job.get("command").equals("upload"))
                return upload(job);
            if (job.get("command").equals("download"))
                return download(job);
            if (job.get("command").equals("delete"))
                return delete(job);
            if (job.get("command").equals("ls"))
                return ls(job);
            if (job.get("command").equals("info"))
                return info(job);
            if(job.get("command").equals("cp") || job.get("command").equals("mv"))
                return MvCp(job);
            if(job.get("command").equals("rmdir"))
                return removeDirectory(job);
            if(job.get("command").equals("mkdir"))
                return makeDirectory(job);
            if(job.get("command").equals("cd"))
                return changeDirectory(job);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseUtil.getResponse(job, "NO", "Namenode storage error");
        }

        JSONObject crap = new JSONObject();
        crap.put("status", "wholyshit");
        return crap;
    }

    private ArrayList<InetSocketAddress> getDataNodesFromFile(File f) throws FileNotFoundException {
        ArrayList<InetSocketAddress> vec = new ArrayList<InetSocketAddress>();
        Scanner sc = new Scanner(f);
        JSONObject response = new JSONObject();
        int total = 0, found = 0;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) continue;
            // System.out.println(line);
            JSONObject obj = new JSONObject(line);
            assert (obj != null);
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            InetSocketAddress address = new InetSocketAddress(ip, port);

            vec.add(address);

        }
        return vec;
    }

    private static Path getNormalizedPath(String currentDirectory , String toAppend){

        try{
            Path p = Paths.get(currentDirectory , toAppend);
            return p.normalize();
        }
        catch (InvalidPathException e){
            e.printStackTrace();
        }

        return null;

    }


    private JSONObject changeDirectory(JSONObject job){
        Path normalized = getNormalizedPath(job.getString("directory") , job.getString("path"));
        String strPath = defaultDir + job.get("username") + "/" + normalized.toString();
        job.remove("path"); job.put("path", normalized.toString());
        File f = new File(strPath);
        if (!f.isDirectory()) {
            return ResponseUtil.getResponse(job, "NO", "Directory doesn't exist on the server or you entered a file not a directory");
        }
        return newdist.ResponseUtil.getResponse(job, "OK", "Directory changed with no issues");
    }
    private JSONObject ls(JSONObject job) {
        Path normalized = getNormalizedPath(job.getString("directory") , job.getString("path"));
        String strPath = defaultDir + job.get("username") + "/" + normalized.toString();
        job.remove("path"); job.put("path", normalized.toString());

        File f = new File(strPath);
        if (!f.isDirectory()) {
            return ResponseUtil.getResponse(job, "NO", "Directory doesn't exist on the server or you entered a file not a directory");
        }
        File folder = new File(strPath);
        File[] listOfFiles = folder.listFiles();

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
    private JSONObject delete(JSONObject job) throws FileNotFoundException {

        Path normalized = getNormalizedPath(job.getString("directory") , job.getString("path"));
        String strPath = defaultDir + job.get("username") + "/" + normalized.toString();
        job.put("path", normalized.toString());

        Path filePath = Paths.get(strPath);

        if (!Files.exists(filePath)) {
            return ResponseUtil.getResponse(job, "NO", "file doesn't exist on the server");
        }

        int found = 0, total = 0;

        File fileToRead = new File(filePath.toString());
        ArrayList < InetSocketAddress > addresses = getDataNodesFromFile(fileToRead);

        JSONObject okFWD = nameNode.proxy.forwardJob(addresses , job);

        boolean deleted = false;
        try {
            deleted = Files.deleteIfExists(filePath);
        }
        catch (IOException e){
            e.printStackTrace();;
        }

        if(!deleted)  return ResponseUtil.getResponse(job, "NO", "Some error happened within namenode disk");
        if(okFWD.get("status").equals("OK"))

            return ResponseUtil.getResponse(job , "OK" , "File was deleted successfully");

        else return ResponseUtil.getResponse(job, "NO", okFWD.getString("report"));

    }
    private JSONObject MvCp(JSONObject job) throws IOException {
        Path pathFrom = getNormalizedPath(job.getString("directory") , job.getString("pathFrom"));
        job.put("pathFrom" , pathFrom.toString());
        Path pathTo = getNormalizedPath(job.getString("directory") , job.getString("pathTo"));
        job.put("pathTo" , pathTo.toString());

        String strPathFrom = defaultDir + job.get("username") + pathFrom.toString();
        String strPathTo = defaultDir + job.get("username") + pathTo.toString();

        File f = new File(strPathFrom);
        File f2 = new File(strPathTo);

        // you have only to check if

        if (!f.exists() || f.isDirectory()) {
            return ResponseUtil.getResponse(job, "NO", "You either have specified an non existing file or it is a directory not a file");
        }

        if(f2.exists()){
            return ResponseUtil.getResponse(job, "NO", "Your target file already exists");
        }

        File f3 = f2.getParentFile();


        System.out.println(strPathFrom+" ::  "+strPathTo);
        if(!f3.isDirectory()){
            return ResponseUtil.getResponse(job,"NO", "Your target file parent directory doesn't exist: " + strPathTo);
        }

        ArrayList<InetSocketAddress> addresses = getDataNodesFromFile(f);
        JSONObject okFWD = nameNode.proxy.forwardJob(addresses , job);


       /* JSONObject response = new JSONObject();
        int total = 0, found = 0;

        for(InetSocketAddress address : datanodes){
            total++;
            if (nameNode.proxy.isAvailable(address)) {
                found++;
                if (found > 1) continue;
                response = nameNode.proxy.MvCp(address, job);
                assert (response != null);
                break;
            }
        }*/

        // move/cp the file on namenode also :D
        String token;

        if(job.getString("command").equals("mv")){
            Files.move(Paths.get(strPathFrom),Paths.get(strPathTo));
            token = "moved";
        }
        else{
            Files.copy(Paths.get(strPathFrom),Paths.get(strPathTo));
            token = "copied";
        }

        if(okFWD.get("status").equals("OK"))

            return ResponseUtil.getResponse(job , "OK" , "File was " + token + " successfully");

        else return ResponseUtil.getResponse(job, "NO", okFWD.getString("report"));
    }


    private ArrayList<InetSocketAddress> getDataNodesFromAllSubDirectory(File root) throws FileNotFoundException {
        /*
         * this will get you a list of all the datanodes that have any chunck in this file subtree
         */
        HashSet<String> set = new HashSet<>();
        ArrayList<InetSocketAddress> ret = new ArrayList<>();
        Queue<File> q = new LinkedList<File>();
        q.add(root);
        while(!q.isEmpty()){
            File top = q.remove();
            if(top.isDirectory()){
                File ar []= top.listFiles();
                for(File zbr : ar){
                    q.add(zbr);
                }
            }
            else{
                ArrayList<InetSocketAddress> ar = getDataNodesFromFile(top);
                for(InetSocketAddress address : ar){
                    if(set.contains(address.toString())){
                        continue;
                    }
                    set.add(address.toString());
                    ret.add(address);
                }
            }
        }
        return ret;
    }

    private JSONObject makeDirectory(JSONObject job) throws IOException{
        Path normalized = getNormalizedPath(job.getString("directory") , job.getString("path"));
        String strPath = defaultDir + job.get("username") + "/" + normalized.toString();
        job.put("path", normalized.toString());
        File f = new File(strPath);
        if(f.exists() && f.isDirectory()){
            return ResponseUtil.getResponse(job, "NO", "another directory or a file exist with the same name");
        }
        f.mkdir();
        return ResponseUtil.getResponse(job,"OK","New Directory is created successfully");

    }

    private JSONObject removeDirectory(JSONObject job) throws IOException {
        Path normalized = getNormalizedPath(job.getString("directory") , job.getString("path"));
        String strPath = defaultDir + job.get("username") + "/" + normalized.toString();
        job.put("path", normalized.toString());

        File f = new File(strPath);

        if (!f.exists() || !f.isDirectory()) {
            return ResponseUtil.getResponse(job, "NO", "Directory does not exist or this is a file name");
        }

        if(job.getString("force").equals("none")){
            if(f.listFiles().length!=0)
                return ResponseUtil.getResponse(job,"NO", "File has content please use -r to force delete recursively");

        }

        ArrayList<InetSocketAddress> addresses = getDataNodesFromAllSubDirectory(f);
        JSONObject okFWD = nameNode.proxy.forwardJob(addresses , job);

        boolean res = FileSystemUtils.deleteRecursively(Paths.get(strPath));
        assert(res);

        if(okFWD.get("status").equals("OK"))

            return ResponseUtil.getResponse(job , "OK" , "Directory was deleted successfully");

        else return ResponseUtil.getResponse(job, "NO", okFWD.getString("report"));
    }

    private JSONObject info(JSONObject job) throws FileNotFoundException {
        String strPath = defaultDir + job.get("username") + job.getString("path");
        File f = new File(strPath);
        if (!f.exists() || f.isDirectory()) {
            return ResponseUtil.getResponse(job, "NO", "File " + job.getString("path") + " does not exist!");
        }
        f = new File(strPath);
        Integer size = (int) f.length();
        File fileToRead = new File(strPath);

        Scanner sc = new Scanner(fileToRead);
        JSONObject response = new JSONObject();
        int total = 0, found = 0;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) continue;

            JSONObject obj = new JSONObject(line);
            assert (obj != null);
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            InetSocketAddress address = new InetSocketAddress(ip, port);
            total++;
            if (nameNode.proxy.isAvailable(address)) {
                found++;
                if (found > 1) continue;
                response = nameNode.proxy.askForInfo(address, job);
                assert (response != null);
                break;
            }
        }
        response.put("report",response.getString("report") + "replica ratio is: " + Integer.toString(found*100/total) + "%");
        return response;
    }

    private JSONObject login(JSONObject job) {

        int found = 0;
        String username = job.getString("username");
        String password = job.getString("password");
        File f = new File("./namenode/users.conf");
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                JSONObject curline = new JSONObject(line);
                if (curline.isNull(username)) continue;
                if (curline.getString(username) != null)
                    if (curline.getString(username).equals(password)) {
                        found = 1;
                        break;
                    }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (found == 1)
            return ResponseUtil.getResponse(job, "OK", "Login Successful");
        return ResponseUtil.getResponse(job, "NO", "Login Failed");

    }

    private JSONObject format(JSONObject job) throws IOException {
        //this function needs to be upgraded to delete all files but for now it just creates new directory
        String dir = defaultDir + job.getString("username") + "/";
        Path path = Paths.get(dir);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        } else {
            File f = new File(dir);
            boolean res = FileSystemUtils.deleteRecursively(path);
            assert(res);
            Files.createDirectory(path);
        }
        JSONObject okFWD = nameNode.proxy.forwardJobToAll(job);
        if(okFWD.get("status").equals("OK"))
            return ResponseUtil.getResponse(job , "OK" , "All current datanodeds were formatted");
        else return ResponseUtil.getResponse(job, "NO", okFWD.getString("report"));
    }

    private JSONObject download(JSONObject job) throws IOException {
        String strPath = defaultDir + job.get("username") + job.getString("serverpath");
        String solPath = job.getString("serverpath");

        job.remove("serverpath");
        job.put("serverpath", "./" + job.get("username") + "/" + solPath);

        Path path = Paths.get(strPath);

        if (!Files.exists(path)) {
            return ResponseUtil.getResponse(job, "NO", "file doesn't exist on the server");
        }

        JSONObject response = ResponseUtil.getResponse(job, "OK", "file normally exists");

        int found = 0;

        File fileToRead = new File(path.toString());

        Scanner sc = new Scanner(fileToRead);

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) continue;
            // System.out.println(line);
            JSONObject obj = new JSONObject(line);
            assert (obj != null);
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            InetSocketAddress add = new InetSocketAddress(ip, port);

            if (nameNode.proxy.isAvailable(add)) {
                found = 1;

                System.out.println("namenode manager thread" + Thread.currentThread());

                response = nameNode.proxy.askForUpload(add, job);

                System.out.println("jaaaa  " + response.toString() + " " + response);

                assert (response != null);

                System.out.println("jaaaa  " + response.toString() + " " + response);

                break;
            }
        }
        System.out.println("what the shit why no printing");
        System.out.println(found);
        if (found == 0) return ResponseUtil.getResponse(job, "NO", "File is temporarily unavailable");
        System.out.println("Found" + response.toString(2));
        return response;
    }

    private JSONObject upload(JSONObject job) throws IOException {


        String strPath = defaultDir + job.get("username") + job.getString("writepath");
        String solPath = job.getString("writepath");

        job.remove("writepath");
        job.put("writepath", "./" + job.get("username") + "/" + solPath);

        Path path = Paths.get(strPath);
        Files.deleteIfExists(path);

        Path par = path.getParent();
        if (par != null && !Files.exists(par))
            Files.createDirectories(par);

        Files.createFile(path);

        //System.out.println(path.getFileName());
        InetSocketAddress datanode = nameNode.proxy.getAvailableDataNode();

        JSONObject response = ResponseUtil.getResponse(job, "OK", "namenode created the file");

        response.put("datanodeip", datanode.getAddress().getHostAddress());
        response.put("datanodeport", datanode.getPort());

        JSONObject meta = new JSONObject();
        meta.put("type", "mainnode");
        meta.put("ip", datanode.getAddress().getHostAddress());
        meta.put("port", datanode.getPort());

        PrintWriter pw = new PrintWriter(path.toString());
        pw.write(meta.toString() + "\n");
        pw.flush();
        pw.close();

        return response;


    }



}
