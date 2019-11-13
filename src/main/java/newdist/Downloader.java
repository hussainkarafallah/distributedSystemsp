package newdist;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryonet.Listener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Downloader {
    String serveIPAddress;
    String filePath;
    int serverPortNumber;
    Client client;

    Semaphore sm = new Semaphore(1);

    Downloader(String _ip , int _port , String _file){
        serveIPAddress = _ip;
        filePath = _file;
        serverPortNumber = _port;
        try{
            run();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    void run() throws Exception{
        client = new Client((int) (1e7), (int) (1e6));
        client.getKryo().register(byte[].class);

        client.start();


        client.addListener( new Listener.ThreadedListener( new Listener() {

            List<byte[]> all = new ArrayList<>();

            int total = 0;

            public void received(Connection connection, Object object) {
                if (object instanceof byte[]) {
                    byte[] arr = (byte[]) (object);
                    total += arr.length;
                    all.add(arr);
                    //System.out.println("addedchunk");
                    connection.sendTCP(new String("passchunk"));
                }
            }


            public void disconnected(Connection connection) {

                System.out.println("Downloaded " + total + " bytes successfully and written to file " + filePath.toString());
                byte[] decodedBytes = new byte[total];
                int iter = 0;
                for (int i = 0; i < all.size(); i++) {
                    int ch = all.get(i).length;
                    for (int j = 0; j < ch; j++)
                        decodedBytes[iter++] = all.get(i)[j];
                }

                Path path = Paths.get(filePath);
                try {

                    Files.deleteIfExists(path);

                    Path par = path.getParent();
                    if(par != null && !Files.exists(par))
                        Files.createDirectories(par);

                    Files.createFile(path);
                    FileOutputStream outstream = new FileOutputStream(filePath);
                    outstream.write(decodedBytes);
                    outstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                sm.release(1);
                client.close();


            }
        }));

        try{
            client.connect(10000     , serveIPAddress , serverPortNumber);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        sm.acquire(1);
        client.sendTCP(new String("passchunk"));
        sm.acquire(1);
    }

}
