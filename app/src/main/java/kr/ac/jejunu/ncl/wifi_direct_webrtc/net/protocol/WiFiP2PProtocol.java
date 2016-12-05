package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.protocol;

import org.json.JSONException;
import org.json.JSONObject;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.User;

/**
 * Created by jinhy on 2016-12-05.
 */

public class WiFiP2PProtocol {

    public JSONObject getRequestUserInfo(User user) {
        JSONObject json = new JSONObject();
        jsonPut(json, "name", user.getName());
        jsonPut(json, "ip", user.getIp());
        jsonPut(json, "key", user.getKey());
        jsonPut(json, "type", "p2p-request");
        return json;
    }

    public JSONObject getAnswerUserInfo(User user) {
        JSONObject json = new JSONObject();
        jsonPut(json, "name", user.getName());
        jsonPut(json, "ip", user.getIp());
        jsonPut(json, "key", user.getKey());
        jsonPut(json, "type", "p2p-answer");
        return json;
    }

    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
