package newdist;

import org.json.JSONObject;

public class ClientManager {
    private ClientApp client;
    ClientManager(ClientApp _client){
        assert (_client != null);
        client = _client;
    }
    void normalHandling(JSONObject response){
        System.out.println(response.toString(2));
        System.out.println(response.getString("report"));
    }
    void handleResponse(JSONObject response){
        System.out.println(response.toString(2));
        if(response.getString("command").equals("login")){
            System.out.println(response.get("report"));
            if(response.get("status").equals("OK"));
                client.setLogged(response.getString("username"));
            return;
        }

        normalHandling(response);

    }

}
