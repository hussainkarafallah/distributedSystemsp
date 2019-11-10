package newdist;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.esotericsoftware.kryonet.JsonSerialization;
import org.json.*;
public class App {
    public static void main(String [] args){
//        System.err.close();
  //      System.setErr(System.out);


        String arg1[] = new String [3];
        String arg2[] = new String[1];
        arg1[0] = "localhost";
        arg1[1] = "20123";
        arg2[0] = arg1[1];
        System.out.println(arg1.length);
        Thread t2 = new Thread(new NameNode(arg2));
        t2.start();
        arg1[2] = "client1";
        Thread t1 = new Thread( new ClientApp(arg1));
        t1.start();
    //    arg1[2] = "client2";
      //  Thread t3 = new Thread(new ClientApp(arg1));
       // t3.start();
      /*  BufferedReader stdIn =
                new BufferedReader(
                        new InputStreamReader(System.in));
        try {
            String userInput;
            while ((userInput = stdIn.readLine().trim()).charAt(0) != 'f') {
                //out.println(userInput);
                System.out.println(userInput);

                //   System.out.println("echo: " + in.readLine());
            }
        }
        catch (IOException e){

        }*/

    }
}
