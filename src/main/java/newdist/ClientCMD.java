package newdist;

import com.esotericsoftware.kryonet.Client;
import org.json.JSONObject;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class pathUtility {

    static int validateFilePath(String token) {
       /* if (token.length() < 2) return 0;
        if (token.substring(0, 2).equals("..")) {
            token = token.substring(2, token.length());
        }

        if (token.charAt(0) == '.') {
            token = token.substring(1, token.length());
        }

        boolean ret = token.matches("^['\"]?(?:/[^/\\n]+)*['\"]?$");
        if (ret) return 1;
        return 0;*/
        try {
            Path p = Paths.get(token).normalize();
        }
        catch (InvalidPathException e){
            e.printStackTrace();
            return 0;
        }
        return 1;
    }

}


class CommandUtil {

    static List<String> commands;

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
        commands.add("rmdir");
        commands.add("ls"); // same as ls dir i guess
        commands.add("cp");
        commands.add("mv");
        commands.add("cd");
    }

    static JSONObject getErrorObject(String message) {
        JSONObject ret = new JSONObject();
        ret.put("valid", "error");
        ret.put("text", message);
        return ret;
    }

    static JSONObject invalidQueryError() {
        return getErrorObject("Please input invalid query");
    }

    ////////////////////////////////
    static String validateLoginCommand(String tokens[]) {
        String def = "user name must be at least 3 characters/digits\npassword must be at least 6 characters";
        if (tokens.length != 3)
            return def;
        if (!tokens[1].matches("^[a-zA-Z0-9._-]{3,}$"))
            return def;
        if (!tokens[2].matches("[^\\s]{6,}"))
            return def;
        return "OK";
    }

    static JSONObject getLoginCommand(String tokens[]) {
        String validation = validateLoginCommand(tokens);
        if (!validation.equals("OK"))
            return getErrorObject(validation);
        JSONObject ret = new JSONObject();
        ret.put("command", "login");
        ret.put("username", tokens[1]);
        ret.put("password", tokens[2]);
        ret.put("valid", "OK");
        return ret;
    }

    //////////////////////////////////////////

    /////////////////////////////

    static String validateFormatCommand(String tokens[]) {
        if (tokens.length == 1)
            return "OK";
        else return "Format takes no parameters";
    }

    static JSONObject getFormatCommand(String tokens[]) {

        String validation = validateFormatCommand(tokens);
        if (!validation.equals("OK"))
            return getErrorObject(validation);

        JSONObject ret = new JSONObject();
        ret.put("valid", "OK");
        ret.put("command", "format");

        try{ Thread.sleep(700); } catch (Exception e){ e.printStackTrace(); }

        return ret;
    }

    ///////////////////////////////

    static String validateDeleteCommand(String tokens[]) {
        if (tokens.length != 2)
            return "Please enter only one argument for this command 1 file path and make sure it's not a directory";
        if (pathUtility.validateFilePath(tokens[1]) == 0)
            return "Please enter only one argument for this command 1 file path and make sure it's not a directory";
        return "OK";
    }

    static JSONObject getDeleteCommand(String tokens[]) {
        String validation = validateDeleteCommand(tokens);
        if (!validation.equals("OK"))
            return getErrorObject(validation);

        JSONObject ret = new JSONObject();
        ret.put("command", "delete");
        ret.put("path", tokens[1]);
        ret.put("valid", "OK");
        return ret;
    }

    /////////////////////////////
    static String validateCdCommand(String tokens[]){
        if (tokens.length != 2)
            return "Please include exactly 1 parameter indicating the directory you want to navigate to and make sure it's not a file";
        if (pathUtility.validateFilePath(tokens[1]) == 0)
            return "Invalid directory, please include exactly 1 parameter indicating the directory you want to navigate to and make sure it's not a file";
        return "OK";
    }
    static JSONObject getCdCommand(String tokens[]) {
        if (tokens.length != 2)
            return getErrorObject("");

        String validation = validateCdCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject("validation");

        JSONObject ret = new JSONObject();
        ret.put("command", "cd");
        ret.put("valid", "OK");
        ret.put("path" , tokens[1]);

        return ret;
    }
    /////////////////////////////
    static String validateLsCommand(String tokens[]){
        if (tokens.length != 2)
            return "Please include exactly 1 parameter indicating the directory you want to view and make sure it's not a file";
        if (pathUtility.validateFilePath(tokens[1]) == 0)
            return "Invalid directory, please include exactly 1 parameter indicating the directory you want to view and make sure it's not a file";
        return "OK";
    }
    static JSONObject getLsCommand(String tokens[]) {
        if (tokens.length != 2)
            return getErrorObject("");

        String validation = validateLsCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject("validation");

        JSONObject ret = new JSONObject();
        ret.put("command", "ls");
        ret.put("valid", "OK");
        ret.put("path" , tokens[1]);

        return ret;
    }

    /////////////////////////////////////////////
    static String validateMkDirCommand(String [] tokens){
        if(tokens.length != 2)
            return "Provide one argument indicating the name of new directory you want to create";
        if (pathUtility.validateFilePath(tokens[1]) == 0)
            return "Invalid directory name";
        return "OK";

    }
    static JSONObject getMkDirCommand(String tokens[]){
        String validation = validateMkDirCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject("validation");
        JSONObject ret = new JSONObject();
        ret.put("command","mkdir");
        ret.put("path",tokens[1]);
        ret.put("valid", "OK");
        return ret;
    }
    /////////////////////////////////////////////
    static  String validateRmDirCommand(String[] tokens){
        if(tokens.length != 2 && tokens.length != 3)
            return "Provide one argument denoting the directory you want to delete and include flag -r if you want to delete everything inside it in case it has files";
        if (pathUtility.validateFilePath(tokens[1]) == 0)
            return "Invalid directory name";
        if(tokens.length == 3 && !tokens[2].equals("-r"))
            return "Second parameter for this command can be -r only otherwise include only a directory";
        return "OK";

    }
    static JSONObject getRmDirCommand(String tokens[]) {


        String validation = validateRmDirCommand(tokens);
        if(!validation.equals("OK"))
            return getErrorObject("validation");

        JSONObject ret = new JSONObject();
        ret.put("command", "rmdir");
        ret.put("path",tokens[1]);
        ret.put("force","none");
        ret.put("valid", "OK");
        if(tokens.length == 3){
            assert (tokens[2].equals("-r"));
            ret.put("force","forced");
        }
        /*if(tokens[0].equals("rmdir") && tokens[1].equals("-r"))
            ret.put("dirname", tokens[2]);
        else ret.put("dirname",tokens[1]);
        // current directory is automatically added as ["directory"]

        if(tokens[0].equals("rmdir")&&tokens.length==3) {
            if(!tokens[1].equals("-r"))
                return getErrorObject("Only force argument is applicable \"-r\"");
            ret.put("force", tokens[1]);
        }*/

        return ret;
    }
    static JSONObject getCpMvCommand(String tokens[]) {
        if (tokens.length != 3)
            return getErrorObject("Please specify path to file only! ");
        try{ Thread.sleep(500); } catch (Exception e){e.printStackTrace();}
        JSONObject ret = new JSONObject();

        Path p1 = Paths.get(tokens[1]),p2 = Paths.get(tokens[2]);
        String validation = "OK";
        if (pathUtility.validateFilePath(tokens[1]) == 0)
            validation = "No correct Path";
        if (Files.isDirectory(p1)) {
            validation = "The first parameter is a directory, please enter a file path";
        }
        if(Files.isDirectory(p2)){
            validation = "The second parameter is a directory, please enter a file path";
        }
        if (!validation.equals("OK"))
            return getErrorObject(validation);
        ret.put("command", tokens[0]);
        ret.put("pathFrom", tokens[1]);
        ret.put("pathTo",tokens[2]);
        ret.put("valid", "OK");
        return ret;
    }


    static String validateDownloadCommand(String tokens[]) {

        String def = "Enter valid two space separated filepaths, with no extra spaces\nFirst path denoting where you want to write the file you download\nsecond for file path on hdfs";

        if (tokens.length != 3)
            return def;
        if (pathUtility.validateFilePath(tokens[1]) == 0 || pathUtility.validateFilePath(tokens[2]) == 0)
            return def;

        Path p = Paths.get(tokens[1]);

        if (Files.isDirectory(p))
            return "The first parameter is a directory, please enter a file path to write downloaded data to";

        return "OK";
    }

    static JSONObject getDownloadCommand(String tokens[]) {

        String validation = validateDownloadCommand(tokens);
        if (!validation.equals("OK"))
            return getErrorObject(validation);

        JSONObject ret = new JSONObject();
        ;


        ret.put("command", "download");
        ret.put("writepath", tokens[1]);
        ret.put("serverpath", tokens[2]);
        ret.put("valid", "OK");


        return ret;
    }
    /////////////////////////////////////////

    static String validateUploadCommand(String tokens[]) {

        String def = "Enter valid two space separated filepaths, with no extra spaces\nFirst path denoting file you want to upload\nsecond for writing path on hdfs";

        if (tokens.length != 3)
            return def;
        if (pathUtility.validateFilePath(tokens[1]) == 0 || pathUtility.validateFilePath(tokens[2]) == 0)
            return def;
        Path p = Paths.get(tokens[1]);
        if (Files.isDirectory(p))
            return "You entered a directory path";
        if (!Files.exists(p))
            return "File doesn't exist";

        return "OK";
    }

    static JSONObject getUploadCommand(String tokens[]) {

        String validation = validateUploadCommand(tokens);
        if (!validation.equals("OK"))
            return getErrorObject(validation);

        JSONObject ret = new JSONObject();
        ;
        File f = new File(tokens[1]);

        ret.put("command", "upload");
        ret.put("writepath", tokens[2]);
        ret.put("clientpath", tokens[1]);
        ret.put("size",String.valueOf(f.length()));
        ret.put("valid", "OK");
        return ret;

    }
    //////////////////////////////
    static JSONObject getInfoCommand(String tokens[]) {
        if (tokens.length != 2)
            return getErrorObject("Please specify path to file only! ");

        JSONObject ret = new JSONObject();
        ret.put("command", "info");
        Path p = Paths.get(tokens[1]);
        String validation = "OK";
        if (pathUtility.validateFilePath(tokens[1]) == 0)
            validation = "No correct Path";
        if (Files.isDirectory(p)) {
            validation = "The first parameter is a directory, please enter a file path to write downloaded data to";
        }
        if (!validation.equals("OK"))
            return getErrorObject(validation);

        ret.put("path", tokens[1]);
        ret.put("valid", "OK");
        return ret;
    }

    /////////////////////////////////////////////////////

    static JSONObject getCommandObject(String tokens[], ClientApp client) {

        if (tokens.length < 1)
            return invalidQueryError();

        String cmd = tokens[0];

        if (commands.indexOf(cmd) == -1)
            return invalidQueryError();

        JSONObject jsonCommand = null;

        if (!cmd.equals("login") && client.isLoggedIn() == 0) {
            return getErrorObject("Please log in");
        }

        if (cmd.equals("format"))
            jsonCommand = getFormatCommand(tokens);
        if (cmd.equals("download"))
            jsonCommand = getDownloadCommand(tokens);
        if (cmd.equals("upload"))
            jsonCommand = getUploadCommand(tokens);
        if (cmd.equals("login"))
            jsonCommand = getLoginCommand(tokens);
        if (cmd.equals("cd"))
            jsonCommand = getCdCommand(tokens);
        if (cmd.equals("delete"))
            jsonCommand = getDeleteCommand(tokens);
        if (cmd.equals("ls"))
            jsonCommand = getLsCommand(tokens);
        if (cmd.equals("info"))
            jsonCommand = getInfoCommand(tokens);
        if (cmd.equals("cp") || cmd.equals("mv"))
            jsonCommand = getCpMvCommand(tokens);
        if(cmd.equals("mkdir"))
            jsonCommand = getMkDirCommand(tokens);
        if(cmd.equals("rmdir"))
            jsonCommand = getRmDirCommand(tokens);

////////////////////erie
        if (client.isLoggedIn() == 1) {
            jsonCommand.put("username", client.userName);
            jsonCommand.put("directory", client.currentDirectory);
        }


        assert (jsonCommand != null);

        return jsonCommand;

    }
}

public class ClientCMD implements Runnable {

    private List<String> commands;

    ClientApp client;

    ClientCMD(ClientApp _client) {
        assert (_client != null);
        client = _client;
    }


    void stateError() {
        System.out.println("Please input valid query");
    }

    public void run() {

        System.out.println("Welcome to MullanorovDFS this is the client, please login and then enter your commands");

        try {
            //BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader sc = new BufferedReader(new FileReader("./test.in"));

            String line = "";

            while (true) {

                Thread.sleep(100);

                line = sc.readLine();

                if (line == null) break;

                line = line.trim();

                if (line.compareTo("quit") == 0) {
                    System.out.println("goodbye");
                    break;
                }


                String tokens[] = line.split("\\s+");

                for (String token : tokens) {
                    token = token.trim();
                }

                JSONObject jsonCommand = CommandUtil.getCommandObject(tokens, client);

                if (!jsonCommand.get("valid").equals("OK")) {
                    System.out.println(jsonCommand.get("text"));
                    continue;
                }

                jsonCommand.remove("valid");

                client.notify(jsonCommand);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
