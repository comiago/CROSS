package util;

import com.google.gson.JsonObject;

public class MessageBuilder {

    // Costruisce una richiesta JSON con operation e valori associati
    public JsonObject buildRequest(String operation, JsonObject values) {
        JsonObject requestObject = new JsonObject();
        requestObject.addProperty("operation", operation);
        requestObject.add("values", values);
        return requestObject;
    }

    // Costruisce una risposta JSON con codice e messaggio di errore (o info)
    public JsonObject buildResponse(int responseCode, String errorMessage) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("response", responseCode);
        responseObject.addProperty("errorMessage", errorMessage);
        return responseObject;
    }

    // Metodo per costruire una notifica (da implementare secondo necessità)
    // public JsonObject buildNotification(String notification, JsonObject trades) {
    //     JsonObject notificationObject = new JsonObject();
    //     notificationObject.addProperty("notification", notification);
    //     notificationObject.add("trades", trades);
    //     return notificationObject;
    // }

    // Metodo generico per costruire messaggi con header e messaggio
    public JsonObject makeMessage(String header, String message) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("header", header);
        jsonMessage.addProperty("message", message);
        return jsonMessage;
    }
}
