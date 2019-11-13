package newdist;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.json.JSONObject;

import java.util.concurrent.Semaphore;

class ThreadSafeClient extends Client {

    Object response;

    Object expectedResponse;

    String serverIP;
    int serverPort;

    final Object resultLock = new Object();

    ThreadSafeClient(String _serverIP , int _serverPort){
        super();
        serverIP = _serverIP;
        serverPort = _serverPort;
        response = new Object();
    }

    void Launch(){

        start();

        try{
            connect(7000, serverIP, serverPort);
        }catch (Exception e){
            e.printStackTrace();
        }

        addListener( new Listener.ThreadedListener(new Listener(){

            public void received(Connection connection, Object o) {

                assert (o != null);
                response = o;
                synchronized (resultLock){
                    resultLock.notify();
                }
            }
        }) );

    }
    synchronized public Object sendSafeTCP(Object request , Object _responseType) throws  Exception{

        expectedResponse = _responseType;

        sendTCP(request);

        synchronized (resultLock){
            resultLock.wait();
        }

        assert(response.getClass() == expectedResponse.getClass());

        Object ret = response;

        return ret;

    }


}