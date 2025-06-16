package client;

import com.google.gson.JsonObject;
import util.Hash;

public class CommandHandler {

    public static JsonObject parseCommand(String input) throws IllegalArgumentException {
        String[] parts = input.trim().split("\\s+"); // Usa regex per spazi multipli
        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Comando vuoto");
        }

        JsonObject request = new JsonObject();
        JsonObject values = new JsonObject();

        switch (parts[0].toLowerCase()) { // supporta anche maiuscole/minuscole
            case "help":
                request.addProperty("operation", "help");
                if (parts.length > 1) {
                    values.addProperty("command", parts[1]);
                }
                break;

            case "register":
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Formato: register <username> <password>");
                }
                request.addProperty("operation", "register");
                values.addProperty("username", parts[1]);
                values.addProperty("password", Hash.sha256(parts[2]));
                break;

            case "login":
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Formato: login <username> <password>");
                }
                request.addProperty("operation", "login");
                values.addProperty("username", parts[1]);
                values.addProperty("password", Hash.sha256(parts[2]));
                break;

            case "logout":
                if (parts.length != 1) {
                    throw new IllegalArgumentException("Formato: logout");
                }
                request.addProperty("operation", "logout");
                break;

            case "updatecredentials": // uso lower case per uniformità
                if (parts.length != 4) {
                    throw new IllegalArgumentException("Formato: updateCredentials <username> <oldPassword> <newPassword>");
                }
                request.addProperty("operation", "updateCredentials");
                values.addProperty("username", parts[1]);
                values.addProperty("oldPassword", Hash.sha256(parts[2]));
                values.addProperty("newPassword", Hash.sha256(parts[3]));
                break;

            default:
                throw new IllegalArgumentException("Comando non supportato: " + parts[0]);
        }

        request.add("values", values);
        return request;
    }
}
