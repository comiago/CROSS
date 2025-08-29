package util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Classe helper per costruire JSON standardizzati per richieste,
 * risposte e notifiche tra client e server.
 */
public class MessageBuilder {

    /**
     * Costruisce una richiesta JSON con operation e valori associati.
     *
     * @param operation nome dell'operazione
     * @param values    oggetto JSON contenente i parametri
     * @return JsonObject pronto da inviare al server
     */
    public JsonObject buildRequest(String operation, JsonObject values) {
        JsonObject request = new JsonObject();
        request.addProperty("operation", operation);
        request.add("values", values != null ? values : new JsonObject());
        return request;
    }

    /**
     * Costruisce una risposta JSON con codice e messaggio opzionale.
     *
     * @param responseCode codice di stato (es. 100 = OK)
     * @param message      messaggio informativo o di errore
     * @return JsonObject pronto da inviare al client
     */
    public JsonObject buildResponse(int responseCode, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("response", responseCode);
        response.addProperty("errorMessage", message != null ? message : "");
        return response;
    }

    /**
     * Costruisce una notifica JSON opzionale con dati allegati.
     *
     * @param notification tipo di notifica (es. "tradeExecuted")
     * @param data         eventuali dati aggiuntivi (può essere null)
     * @return JsonObject pronto da inviare
     */
    public JsonObject buildNotification(String notification, JsonElement data) {
        JsonObject notificationObject = new JsonObject();
        notificationObject.addProperty("notification", notification);
        if (data != null) {
            notificationObject.add("data", data);
        }
        return notificationObject;
    }

    /**
     * Costruisce un messaggio generico con header e messaggio.
     *
     * @param header  titolo del messaggio
     * @param message contenuto del messaggio
     * @return JsonObject pronto da inviare
     */
    public JsonObject makeMessage(String header, String message) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("header", header != null ? header : "");
        jsonMessage.addProperty("message", message != null ? message : "");
        return jsonMessage;
    }
}
