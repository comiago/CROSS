package server;

import org.json.JSONObject;

import java.io.PrintWriter;

public class RequestController {
    private static Network network;

    public RequestController(Network network) {
        this.network = network;
    }

    public static JSONObject handleRegister(JSONObject request) {
        JSONObject response = new JSONObject();
        String username = request.getString("username");
        String password = request.getString("password");

        if (password.isEmpty()) {
            network.sendError(response, 101, "Password vuota");
        }
//        else if (userExists(username)) {
//            network.sendError(response, 102, "Username già esistente");
//        } else {
//            network.createUser(username, password);
//            response.put("response", 100);
//            response.put("errorMessage", "");
//        }
        return response;
    }
}
