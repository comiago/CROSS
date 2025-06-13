package util;

import com.google.gson.JsonObject;

public class MessageBuilder {
    public JsonObject buildRequest(String operation, JsonObject values) {
        JsonObject requestObject = new JsonObject();
        requestObject.addProperty("operation", operation);
        requestObject.add("values", values);
        return requestObject;
    }

    public JsonObject buildResponse(int response, String errorMessage) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("response", response);
        responseObject.addProperty("errorMessage", errorMessage);
        return responseObject;
    }

    public JsonObject makeMessage(String header, String message) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("header", header);
        jsonMessage.addProperty("message", message);
        return jsonMessage;
    }
}
