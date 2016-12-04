package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.socket;

/**
 * Created by jinhy on 2016-12-04.
 */

public interface Socket {
    void start();
    void disconnect();
    void send(String message);

    interface ChannelEvent {
        void onConnected(boolean isServer);
        void onMessage(String message);
        void onError(String description);
        void onClose();
    }
}
