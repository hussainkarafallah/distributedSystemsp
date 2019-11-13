package newdist;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.esotericsoftware.kryonet.JsonSerialization;
import org.json.*;
public class App {
    public static void main(String [] args){
        byte[] arr = new byte[10];
        System.out.println(arr.getClass().getName());
    }
}
