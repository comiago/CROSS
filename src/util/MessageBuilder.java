package util;

import org.json.JSONObject;

public class MessageBuilder {
    public JSONObject makeMessage(String header, String message) {
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put("header", header);
        jsonMessage.put("message", message);
        return jsonMessage;
    }
}
