package kr.ac.jejunu.ncl.wifi_direct_webrtc.net;

/**
 * Created by jinhy on 2016-12-04.
 */

public class ConnectionParameter {
    private final String roomUrl;
    private final String roomId;
    private final boolean loopback;

    public ConnectionParameter(String roomUrl, String roomId, boolean loopback) {
        this.roomUrl = roomUrl;
        this.roomId = roomId;
        this.loopback = loopback;
    }

    public String getRoomUrl() {
        return roomUrl;
    }

    public String getRoomId() {
        return roomId;
    }

    public boolean isLoopback() {
        return loopback;
    }
}
