package newdist;

import com.esotericsoftware.kryonet.Client;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class pathUtility{

    static int validateDirectory(String token){
        if(token.length() < 2) return 0;
        if(token.substring(0 , 2).equals("..")){
            token = token.substring(2 , token.length());
        }
        System.out.println(token);
        System.out.println(token.substring(0 , 2));
        if(token.charAt(0) == '.'){
            token = token.substring(1 , token.length());
        }
        System.out.println(token);
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

class commandUtility{
    static JSONObject getFormatCommand(String tokens[]){
        if(tokens.length != 1){
            return null;
        }
        JSONObject ret = new JSONObject();
        return ret;
    }

    static JSONObject getLoginCommand(String tokens[]){
        if(tokens.length != 3){
           return null;
        }
        if(!tokens[1].matches("^[a-zA-Z0-9._-]{3,}$")){
           return null;
        }
        if(!tokens[2].matches("[^\\s]{6,}")){
           return null;
        }
        JSONObject ret = new JSONObject();
        ret.put("username" , tokens[1]);
        ret.put("password" , tokens[2]);
        return ret;
    }

    static JSONObject singleFileCommand(String tokens[]){
        if(tokens.length  != 2){
            return null;
        }

        int okDir = pathUtility.validateDirectory(tokens[1]);

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
        if(pathUtility.validateDirectory(tokens[1]) == 0 || pathUtility.validateDirectory(tokens[2]) == 0 || pathUtility.pathType(tokens[1]).equals("None") || pathUtility.pathType(tokens[2]).equals("None")){
            return null;
        }
        JSONObject ret = new JSONObject();
        ret.put("sourcepath",tokens[1]);
        ret.put("targetpath",tokens[2]);
        return ret;
    }

}

public class ClientCMD implements Runnable {

    private List<String> commands;
    private ClientApp.InputListener listener;

    ClientCMD(){
        commands = new ArrayList<String>();
        commands.add("login");
        commands.add("format");
        commands.add("create");
        commands.add("download");
        commands.add("delete");
        commands.add("info");
        commands.add("copy");
        commands.add("move");
        commands.add("opendir");
        commands.add("lsdir");
        commands.add("mkdir");
        commands.add("deldir");
    }

    void setListener(ClientApp.InputListener _listener){
        assert(_listener != null);
        listener = _listener;
    }


    void stateError(){
        System.out.println("Please input valid query");
    }

    public void run() {

        System.out.println("wtfffff");
        BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
        String line = "";

        while(true) {

            try {
                line = sc.readLine().trim();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(line.compareTo("quit") == 0) {
                System.out.println("goodbye");
                break;
            }

            System.out.println(line);

            //if (line.length() >= 0) continue;

            String tokens[] = line.split("\\s+");

            for (String token : tokens) {
                token = token.trim();
            }

            if (tokens.length < 1) {
                stateError();
                continue;
            }

            String cmd = tokens[0];

            if (commands.indexOf(cmd) == -1) {
                stateError();
                continue;
            }


            JSONObject jsonCommand = new JSONObject();


            //jsonCommand.put("command" , cmd);

            if (cmd.equals("format")) {
                jsonCommand = commandUtility.getFormatCommand(tokens);
                if (jsonCommand == null) {
                    stateError();
                    continue;
                }
            }

            if (cmd.equals("login")) {
                jsonCommand = commandUtility.getLoginCommand(tokens);
                if (jsonCommand == null) {
                    System.out.println("user name must be at least 3 characters/digits");
                    System.out.println("password must be at least 6 characters");
                    continue;
                }
            }

            if (cmd.equals("delete") || cmd.equals("download") || cmd.equals("info") || cmd.equals("create")) {
                jsonCommand = commandUtility.singleFileCommand(tokens);
                if (jsonCommand == null) {
                    System.out.println("Enter valid directory with no spaces");
                    continue;
                }
            }

            if (cmd.equals("copy") || cmd.equals("move")) {
                jsonCommand = commandUtility.doubleFileCommand(tokens);
                if (jsonCommand == null) {
                    System.out.println("Enter valid two directories with no spaces");
                    continue;
                }
            }

            assert (jsonCommand != null);
            jsonCommand.put("command", cmd);
            System.out.println(cmd);
            listener.notifyCommand(jsonCommand);
        }
    }


}
