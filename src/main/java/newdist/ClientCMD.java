package newdist;

import com.esotericsoftware.kryonet.Client;
import org.json.JSONObject;

import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class pathUtility{

    static int validateFilePath(String token){
        if(token.length() < 2) return 0;
        if(token.substring(0 , 2).equals("..")){
            token = token.substring(2 , token.length());
        }

        if(token.charAt(0) == '.'){
            token = token.substring(1 , token.length());
        }

        boolean ret = token.matches("^['\"]?(?:/[^/\\n]+)*['\"]?$");
        if(ret) return 1;
        return 0;
    }

    static String pathType(String token){
        if(token.charAt(0) == '/') return "Absolute";
        if(token.substring(0 , 2).equals("..")) return "Parent";
        if(token.charAt(0) == '.') return "Relative";
        return "None";
    }
}

/*class commandUtility{
    final static long ChunkLength = 20000;
    /*static JSONObject getFormatCommand(String tokens[]){
        if(tokens.length != 1){
            return null;
        }
        JSONObject ret = new JSONObject();
        return ret;
    }



    static JSONObject singleFileCommand(String tokens[]){
        if(tokens.length  != 2){
            return null;
        }

        int okDir = pathUtility.validateFilePath(tokens[1]);

        if(okDir == 0 || pathUtility.pathType(tokens[1]).equals("None")){
            return null;
        }
        JSONObject ret = new JSONObject();
        ret.put("filepath" , tokens[1]);
        ret.put("pathtype" , pathUtility.pathType(tokens[1]));
        return  ret;
    }

    static JSONObject doubleFileCommand(String tokens[]){
        if(tokens.length != 3){
            return null;
        }
        if(pathUtility.validateFilePath(tokens[1]) == 0 || pathUtility.validateFilePath(tokens[2]) == 0 || pathUtility.pathType(tokens[1]).equals("None") || pathUtility.pathType(tokens[2]).equals("None")){
            return null;
        }
        JSONObject ret = new JSONObject();
        ret.put("sourcepath",tokens[1]);
        ret.put("targetpath",tokens[2]);
        return ret;
    }

    static JSONObject downloadCommand(String tokens[]){









    }
    static JSONObject uploadCommand(String tokens[]){



    }

}
*/

class CommandUtil{

    static List <String> commands;

    static {
        commands = new ArrayList<String>();
        commands.add("login");
        commands.add("format");
        commands.add("create");
        commands.add("download");
        commands.add("upload");
        commands.add("delete");
        commands.add("info");
        commands.add("copy");
        commands.add("move");
        commands.add("opendir");
        commands.add("lsdir");
        commands.add("mkdir");
        commands.add("deldir");
    }

    static JSONObject getErrorObject(String message){
        JSONObject ret = new JSONObject();
        ret.put("valid" , "error");
        ret.put("text" , message);
        return ret;
    }
    static JSONObject invalidQueryError(){
        return getErrorObject("Please input invalid query");
    }

    ////////////////////////////////
    static String validateLoginCommand(String tokens[]){
        String def = "user name must be at least 3 characters/digits\npassword must be at least 6 characters";
        if(tokens.length != 3)
            return def;
        if(!tokens[1].matches("^[a-zA-Z0-9._-]{3,}$"))
            return def;
        if(!tokens[2].matches("[^\\s]{6,}"))
            return def;
        return "OK";
    }
    static JSONObject getLoginCommand(String tokens[]){
        String validation = validateLoginCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject(validation);
        JSONObject ret = new JSONObject();
        ret.put("command" , "login");
        ret.put("username" , tokens[1]);
        ret.put("password" , tokens[2]);
        ret.put("valid" , "OK");
        return ret;
    }
    //////////////////////////////

    /////////////////////////////
    static String validateFormatCommand(String tokens[]){
        if(tokens.length == 1)
            return "OK";
        else return "Format takes no parameters";
    }
    static JSONObject getFormatCommand(String tokens[]){

        String validation = validateFormatCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject(validation);

        JSONObject ret = new JSONObject();
        ret.put("valid" , "OK");
        ret.put("command" , "format");

        return ret;
    }

    ///////////////////////////////
    static String validateDownloadCommand(String tokens[]){

        String def = "Enter valid two space separated filepaths, with no extra spaces\nFirst path denoting where you want to write the file you download\nsecond for file path on hdfs";

        if(tokens.length != 3)
            return def;
        if(pathUtility.validateFilePath(tokens[1]) == 0 || pathUtility.validateFilePath(tokens[2]) == 0)
            return def;

        Path p = Paths.get(tokens[1]);

        if(Files.isDirectory(p))
            return "The first parameter is a directory, please enter a file path to write downloaded data to";

        return "OK";
    }
    static JSONObject getDownloadCommand(String tokens[]){

        String validation = validateDownloadCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject(validation);

        JSONObject ret = new JSONObject();;


        ret.put("command" , "download");
        ret.put("writepath" ,  tokens[1]);
        ret.put("serverpath" , tokens[2]);
        ret.put("valid" , "OK");

      //  System.out.println("new upload" + ret.toString(2));

        return ret;
    }
    /////////////////////////////////////////

    static String validateUploadCommand(String tokens[]){

        String def = "Enter valid two space separated filepaths, with no extra spaces\nFirst path denoting file you want to upload\nsecond for writing path on hdfs";

        if(tokens.length != 3)
            return def;
        if(pathUtility.validateFilePath(tokens[1]) == 0 || pathUtility.validateFilePath(tokens[2]) == 0)
            return def;
        Path p = Paths.get(tokens[1]);
        if(Files.isDirectory(p))
            return "You entered a directory path";
        if(!Files.exists(p))
            return "File doesn't exist";

        return "OK";
    }

    static JSONObject getUploadCommand(String tokens[]){

        String validation = validateUploadCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject(validation);

        JSONObject ret = new JSONObject();;
        ret.put("command","upload");
        ret.put("writepath" ,  tokens[2]);
        ret.put("clientpath" , tokens[1]);
        ret.put("valid" , "OK");
        return ret;

    }

    /////////////////////////////////////////////////////

    static JSONObject getCommandObject(String tokens[] , ClientApp client){

        if (tokens.length < 1)
            return invalidQueryError();

        String cmd = tokens[0];

        if (commands.indexOf(cmd) == -1)
            return invalidQueryError();

        JSONObject jsonCommand = null;

        if(!cmd.equals("login") && client.isLoggedIn() == 0){
            return getErrorObject("Please log in");
        }

        if (cmd.equals("format"))
            jsonCommand = getFormatCommand(tokens);
        if(cmd.equals("download"))
            jsonCommand = getDownloadCommand(tokens);
        if(cmd.equals("upload"))
            jsonCommand = getUploadCommand(tokens);
        if(cmd.equals("login"))
            jsonCommand = getLoginCommand(tokens);

        if(client.isLoggedIn() == 1){
            jsonCommand.put("username" , client.userName);
            jsonCommand.put("directory", client.currentDirectory);
        }

        assert(jsonCommand != null);

        return jsonCommand;

    }
}
public class ClientCMD implements Runnable {

    private List<String> commands;

    ClientApp client;

    ClientCMD(ClientApp _client){
        assert (_client != null);
        client = _client;
    }


    void stateError(){
        System.out.println("Please input valid query");
    }

    public void run() {

        System.out.println("Welcome to MullanorovDFS this is the client, please login and then enter your commands");
        //BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
        try{
            BufferedReader sc = new BufferedReader(new FileReader("./test.in"));

            String line = "";

            while(true) {

                Thread.sleep(100);

                line = sc.readLine();

                if(line == null) break;

                line = line.trim();

                if(line.compareTo("quit") == 0) {
                    System.out.println("goodbye");
                    break;
                }


                String tokens[] = line.split("\\s+");

                for (String token : tokens) {
                    token = token.trim();
                }

                JSONObject jsonCommand = CommandUtil.getCommandObject(tokens , client);

                if(jsonCommand.get("valid") != "OK"){
                    System.out.println(jsonCommand.get("text"));
                    continue;
                }

                jsonCommand.remove("valid" );

                client.notify(jsonCommand);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


}
