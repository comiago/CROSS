package client;

import org.json.JSONObject;

public class CommandHandler {
    public static JSONObject parseCommand(String input) throws IllegalArgumentException {
        String[] parts = input.split(" ");
        JSONObject request = new JSONObject();
        JSONObject values = new JSONObject();

        switch (parts[0]) {
            case "register":
                if (parts.length != 3) throw new IllegalArgumentException("Formato: register <username> <password>");
                request.put("operation", "register");
                values.put("username", parts[1]);
                values.put("password", parts[2]);
                break;

            // Aggiungi altri casi qui...
            default:
                throw new IllegalArgumentException("Comando non supportato");
        }

        request.put("values", values);
        return request;
    }
}