package newdist;

import com.esotericsoftware.kryonet.*;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Uploader {

    Server server;
    int portNumber;
    String filePath , writePath;
    final int ChunkSize = 500000;
    InetSocketAddress otherEnd;
    JSONObject info;
    byte[] arr;

    Uploader(int _portNumber , String _filePath , String _writePath , InetSocketAddress add , JSONObject _info){
        assert(_info != null);
        portNumber = _portNumber;
        filePath = _filePath;
        otherEnd = add;
        writePath = _writePath;
        info = _info;
        try {
            start();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    void notify(InetSocketAddress otherEnd){
        Client client = new Client();

        client.start();
        client.getKryo().register(JSONObject.class);
        client.getKryo().register(java.util.HashMap.class);

        try {
            client.connect(7000 , otherEnd.getAddress().getHostAddress() , otherEnd.getPort());
        }
        catch (IOException e){
            e.printStackTrace();
        }

        client.addListener(new Listener.ThreadedListener(new Listener() {
            public void received (Connection connection, Object object) {
                if (object instanceof JSONObject) {
                    JSONObject answer = (JSONObject) object;
                    assert(answer != null);
                    assert(answer.getString("status").equals("OK"));
                }
            }
        }));

        try {
            //System.out.println(info.toString(2));
            JSONObject request = new JSONObject(info, JSONObject.getNames(info));

            InetAddress inetAddress = InetAddress.getLocalHost();
            request.put("command", "startdownload");
            request.put("ip", inetAddress.getHostAddress());
            request.put("port",portNumber);
            request.put("writepath",writePath);

           // System.out.println(request.toString(2));


            client.sendTCP(request);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    void start() throws IOException{

        System.out.println("here we are uploader started at port " + portNumber);

        server = new Server((int)(1e7) , (int)(1e6));
        server.getKryo().register(byte[].class);

        try{
            server.bind(portNumber);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        server.start();

        File file = new File(filePath);
        FileInputStream stream = new FileInputStream(file);
        arr = new byte[ (int) (file.length())];
        stream.read(arr);
        stream.close();


        server.addListener(new Listener.ThreadedListener(new Listener() {
            int offset = ChunkSize , cur = 0;

            public Object next(int index){
                int end = Math.min(arr.length , index + offset);
                int len = end - index;
                byte chunk[] = new byte[end - index];
                for(int j = index ; j < end ; j++){
                    chunk[j - index] = arr[j];
                }
                Object obj = chunk;
                return obj;
            }
            public void received (Connection connection, Object object) {
                if (object instanceof String) {
                    String str = (String) (object);
                    assert (str.equals("passchunk"));
                    //System.out.println(cur);
                    if (cur >= arr.length) {
                        connection.close();
                        return;
                    }
                    connection.sendTCP(next(cur));
                    cur += offset;
                }
            }

            public void disconnected(Connection connection) {
                System.out.println("ok we are done");
                server.close();
            }
        }));

        if(otherEnd != null)
            notify(otherEnd);

    }

}
