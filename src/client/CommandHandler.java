package client;

import com.google.gson.JsonObject;
import util.Hash;
import util.Regex;

/**
 * Classe che gestisce il parsing dei comandi della shell del client.
 * I comandi vengono convertiti in oggetti JSON da inviare al server.
 */
public class CommandHandler {

    /**
     * Parsing di un comando testuale dell'utente.
     * @param input stringa inserita dall'utente
     * @return JsonObject pronto da inviare al server
     * @throws IllegalArgumentException se il comando non è valido
     */
    public static JsonObject parseCommand(String input) throws IllegalArgumentException {
        // Rimuove spazi iniziali/finali e split per spazi multipli
        String[] parts = input.trim().split("\\s+");

        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Comando vuoto");
        }

        JsonObject request = new JsonObject();
        JsonObject values = new JsonObject();
        String command = parts[0].toLowerCase(); // normalizza in minuscolo

        switch (command) {
            case "help":
                request.addProperty("operation", "help");
                if (parts.length > 1) values.addProperty("command", parts[1]);
                break;

            case "register":
                checkArgs(parts, 3, "register <username> <password>");
                request.addProperty("operation", "register");
                values.addProperty("username", parts[1]);
                values.addProperty("password", Hash.sha256(parts[2])); // hash della password
                break;

            case "login":
                checkArgs(parts, 3, "login <username> <password>");
                request.addProperty("operation", "login");
                values.addProperty("username", parts[1]);
                values.addProperty("password", Hash.sha256(parts[2]));
                break;

            case "logout":
                checkArgs(parts, 1, "logout");
                request.addProperty("operation", "logout");
                break;

            case "updatecredentials":
                checkArgs(parts, 4, "updateCredentials <username> <oldPassword> <newPassword>");
                request.addProperty("operation", "updateCredentials");
                values.addProperty("username", parts[1]);
                values.addProperty("oldPassword", Hash.sha256(parts[2]));
                values.addProperty("newPassword", Hash.sha256(parts[3]));
                break;

            case "insertlimitorder":
                checkArgs(parts, 4, "insertLimitOrder <type: ask/bid> <size> <price>");
                validateOrderType(parts[1]);
                values.addProperty("type", parts[1].toUpperCase());
                values.addProperty("size", parsePositiveInt(parts[2], "size"));
                values.addProperty("price", parsePositiveInt(parts[3], "price"));
                request.addProperty("operation", "insertLimitOrder");
                break;

            case "insertmarketorder":
                checkArgs(parts, 3, "insertMarketOrder <type: ask/bid> <size>");
                validateOrderType(parts[1]);
                values.addProperty("type", parts[1].toUpperCase());
                values.addProperty("size", parsePositiveInt(parts[2], "size"));
                request.addProperty("operation", "insertMarketOrder");
                break;

            case "insertstoporder":
                checkArgs(parts, 4, "insertStopOrder <type: ask/bid> <size> <stopPrice>");
                validateOrderType(parts[1]);
                values.addProperty("type", parts[1].toUpperCase());
                values.addProperty("size", parsePositiveInt(parts[2], "size"));
                values.addProperty("stopPrice", parsePositiveInt(parts[3], "stopPrice"));
                request.addProperty("operation", "insertStopOrder");
                break;

            default:
                throw new IllegalArgumentException("Comando non supportato: " + parts[0]);
        }

        request.add("values", values);
        return request;
    }

    // --- Metodi helper privati ---

    /**
     * Controlla il numero corretto di argomenti
     */
    private static void checkArgs(String[] parts, int expected, String usage) {
        if (parts.length != expected) {
            throw new IllegalArgumentException("Formato corretto: " + usage);
        }
    }

    /**
     * Verifica che il tipo di ordine sia valido (ask/bid)
     */
    private static void validateOrderType(String type) {
        if (!type.equalsIgnoreCase("ask") && !type.equalsIgnoreCase("bid")) {
            throw new IllegalArgumentException("Tipo ordine deve essere 'ask' o 'bid'");
        }
    }

    /**
     * Converte una stringa in intero positivo e controlla che sia >0
     */
    private static int parsePositiveInt(String s, String fieldName) {
        if (!Regex.isNumber(s)) {
            throw new IllegalArgumentException("Il campo " + fieldName + " deve essere un numero positivo");
        }
        int value = Integer.parseInt(s);
        if (value <= 0) throw new IllegalArgumentException(fieldName + " deve essere > 0");
        return value;
    }
}
