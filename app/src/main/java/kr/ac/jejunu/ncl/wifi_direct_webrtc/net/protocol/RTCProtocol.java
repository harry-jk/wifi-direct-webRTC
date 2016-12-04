package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.protocol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Created by jinhy on 2016-12-04.
 */

public class RTCProtocol {
    public enum ConnectionState {
        NEW, CONNECTED, CLOSED, ERROR
    };
    private ConnectionState roomState;

    public RTCProtocol() {

    }

    public void setRoomState(ConnectionState roomState) {
        this.roomState = roomState;
    }

    /**
     * Send offer SDP to the other participant.
     */
    public JSONObject getOfferSdp(final SessionDescription sdp) {
        if (roomState != ConnectionState.CONNECTED) {
            return null;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        return json;
    }

    /**
     * Send answer SDP to the other participant.
     */
    public JSONObject getAnswerSdp(final SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
        return json;
    }

    /**
     * Send Ice candidate to the other participant.
     */
    public JSONObject getLocalIceCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "candidate");
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);

        if (roomState != ConnectionState.CONNECTED) {
            return null;
        }
        return json;
    }

    /**
     * Send removed ICE candidates to the other participant.
     */
    public JSONObject getLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray =  new JSONArray();
        for (final IceCandidate candidate : candidates) {
            jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);

        if (roomState != ConnectionState.CONNECTED) {
            return null;
        }
        return json;
    }

    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Converts a Java candidate to a JSONObject.
    public static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    public static IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
        return new IceCandidate(json.getString("id"),
                json.getInt("label"),
                json.getString("candidate"));
    }
}
