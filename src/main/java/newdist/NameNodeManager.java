package newdist;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
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
            if(job.get("command").equals("rmdir") || job.get("command").equals("mkdir"))
                return manipulateDir(job);
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
    private JSONObject manipulateDir(JSONObject job) throws IOException {
        String strPath = defaultDir + job.get("username") + job.getString("directory");
        File f = new File(strPath);

        // you have only to check if
        if (!f.exists() || !f.isDirectory()) {
            return ResponseUtil.getResponse(job, "NO", "Directory does not exist");
        }


        String newDriPath = strPath + "/" + job.get("dirname");
        // move/cp the file on namenode also :D
        JSONObject response = ResponseUtil.getResponse(job, "NO", "Deleted only directories on namenode");

        if(job.getString("command").equals("mkdir")){

           f = new File(newDriPath);
            if(f.exists() && f.isDirectory())
                return ResponseUtil.getResponse(job, "NO", "another directory exist with the same name");
            f.mkdir();
            /// problem is which data node to sent further
            return ResponseUtil.getResponse(job,"OK","Great success");
        }
        else{
            f = new File(newDriPath);
            if(!f.exists() || !f.isDirectory())
                return ResponseUtil.getResponse(job, "NO", "It aint a directory");
            if(!job.getString("force").equals("-r")){
                if(f.listFiles().length!=0)
                    return ResponseUtil.getResponse(job,"NO", "File has content please use -r to force delete recursively");

            }
            ArrayList<InetSocketAddress> ar = getDataNodesFromAllSubDirectory(f);
            int found=0,total = ar.size();
            for(InetSocketAddress address: ar){
                if (nameNode.proxy.isAvailable(address)) {
                    response = nameNode.proxy.manipulateDir(address, job);
                    assert (response != null);
                }
            }
            deleteRecursively(f);
        }
        return response;
    }
    private static void deleteRecursively(File file) throws IOException {

        for (File childFile : file.listFiles()) {

            if (childFile.isDirectory()) {
                deleteRecursively(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }

        if (!file.delete()) {
            throw new IOException();
        }
    }

    private JSONObject MvCp(JSONObject job) throws IOException {
        String strPathFrom = defaultDir + job.get("username") + job.getString("pathFrom");
        String strPathTo = defaultDir + job.get("username") + job.getString("pathTo");
        File f = new File(strPathFrom);
        File f2 = new File(strPathTo);
        // you have only to check if
        if (!f.exists() || f.isDirectory() || f2.exists()) {
            return ResponseUtil.getResponse(job, "NO", "You either have specified an non existing file or it is a directory not a file or the \"to\" file is existing already");
        }
        File f3 = f2.getParentFile();
        System.out.println(f2.getCanonicalPath());
        System.out.println(strPathFrom+" ::  "+strPathTo);
        if(!f3.isDirectory()){
            return ResponseUtil.getResponse(job,"NO", "There is no directory: " + strPathTo);
        }
        ArrayList<InetSocketAddress> datanodes = getDataNodesFromFile(f);

        JSONObject response = new JSONObject();
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
        }

        // move/cp the file on namenode also :D
        if(job.getString("command").equals("mv")){
            Files.move(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        else{
            Files.copy(Paths.get(strPathFrom),Paths.get(strPathTo));
        }
        return response;
    }

    private JSONObject ls(JSONObject job) {
        String strPath = defaultDir + job.get("username") + job.getString("directory");
        File f = new File(strPath);
        if (!f.isDirectory()) {
            return ResponseUtil.getResponse(job, "NO", "Directory doesn't exist on the server");
        }
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
            // System.out.println(line);
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

    private JSONObject delete(JSONObject job) throws FileNotFoundException {
        System.out.println("Name node started deleting file: " + job.getString("path"));

        String strPath = defaultDir + job.get("username") + job.getString("path");
        System.out.println(job.getString("path"));
        System.out.println(strPath);
        job.put("path", job.get("path")); /// here may have an assertion actually

        Path path = Paths.get(strPath);
        System.out.print(strPath);
        System.out.println(path);
        if (!Files.exists(path)) {
            return ResponseUtil.getResponse(job, "NO", "file doesn't exist on the server");
        }

        int found = 0, total = 0;

        File fileToRead = new File(path.toString());

        Scanner sc = new Scanner(fileToRead);
        JSONObject response = new JSONObject();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) continue;
            // System.out.println(line);
            JSONObject obj = new JSONObject(line);
            assert (obj != null);
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            InetSocketAddress address = new InetSocketAddress(ip, port);
            total++;
            if (nameNode.proxy.isAvailable(address)) {
                found++;

                System.out.println("namenode manager thread" + Thread.currentThread());

                response = nameNode.proxy.askForDelete(address, job);

                System.out.println("jaaaa  " + response.toString() + " " + response);

                assert (response != null);

                System.out.println("jaaaa  " + response.toString() + " " + response);

                break;
            }
        }
        File f = new File(strPath);
        if (found == total) {
            if (!f.delete()) {
                response.put("report", "Some wicked error happened!");
                return response;
            }
        } else {
            response.put("report", "There are still some datanodes with the file");
            // neeeds extra handling
            if (!f.delete()) {
                response.put("report", "Some wicked error happened!");
                return response;
            }
        }
        return response;
    }

    private JSONObject login(JSONObject job) {

        int found = 0;
        String username = job.getString("username");
        String password = job.getString("password");
        File f = new File("users.conf");
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                JSONObject curline = new JSONObject(line);
                System.out.println(curline.toString());
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
            Files.createDirectory(path);
        }
        return ResponseUtil.getResponse(job, "OK", "Formatted Successfully");
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
