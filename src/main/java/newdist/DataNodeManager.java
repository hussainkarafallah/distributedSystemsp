package newdist;


import org.json.JSONObject;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;



public class DataNodeManager {

    DataNode dataNode;

    DataNodeManager(DataNode _dataNode){
        assert (dataNode != null);
        dataNode = _dataNode;
    }

    static class ResponseUtil{
        static JSONObject getResponse(JSONObject command , String status , String report){
            JSONObject ret = new JSONObject(command, JSONObject.getNames(command));
            assert(command.get("command") != ret.get("command"));
            ret.put("status",status);
            ret.put("report" , report);
            return ret;
        }
    }

    JSONObject performJob(JSONObject job){
        if(job.get("command").equals("ping"))
            return ResponseUtil.getResponse(job , "OK" , "datanode " + dataNode.dataNodeName + " is reached");

        JSONObject crap = new JSONObject();
        crap.put("status" , "wholyshit");
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
