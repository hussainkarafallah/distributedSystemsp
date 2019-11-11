package newdist;

import org.json.JSONObject;

import java.io.*;
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
    NameNodeManager(NameNode _nameNode){
        assert(_nameNode !=  null);
        nameNode = _nameNode;
    }

    JSONObject performJob(JSONObject job){
        if(job.get("command").equals("login"))
            return login(job);
        if(job.get("command").equals("format"))
            return format(job);


        JSONObject crap = new JSONObject();
        crap.put("status" , "wholyshit");
        return crap;
    }

    private JSONObject auth(JSONObject job){
        return new JSONObject();
    }
    private JSONObject login(JSONObject job){

        int found = 0;
        String username = job.getString("username");
        String password = job.getString("password");
        File f = new File("users.conf");
        try(Scanner sc = new Scanner(f)){
            while(sc.hasNextLine()){
                String line = sc.nextLine();
                JSONObject curline = new JSONObject(line);
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

    private JSONObject format(JSONObject job){
        //this function needs to be upgraded to delete all files but for now it just creates new directory
        String dir = "./" + job.getString("username") + "/";
        Path path = Paths.get(dir);
        if(Files.notExists(path)){
            try {
                Files.createDirectory(path);
            }
            catch (IOException e){
                e.printStackTrace();
                return ResponseUtil.getResponse(job , "NO" , "Namenode storage error");

            }
        }
        else{
            File f = new File(dir);
            try{
                boolean res = FileSystemUtils.deleteRecursively(path);
                Files.createDirectory(path);
            }catch (IOException e){
                e.printStackTrace();
                return ResponseUtil.getResponse(job , "NO" , "Namenode storage error");
            }
        }
        return ResponseUtil.getResponse(job , "OK" , "Formatted Successfully");
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
