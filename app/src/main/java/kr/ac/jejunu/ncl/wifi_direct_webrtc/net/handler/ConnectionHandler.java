package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.PeerConnectionClient;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.Global;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.User;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.ConnectionParameter;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.protocol.RTCProtocol;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.protocol.WiFiP2PProtocol;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.socket.Socket;

/**
 * Created by jinhy on 2016-12-04.
 */

public abstract class ConnectionHandler
        implements PeerConnectionClient.PeerConnectionEvents, Socket.ChannelEvent {
    protected final ExecutorService executor;
    protected ConnectionParameter connectionParameter;
    protected HandleProtocol handleProtocol;
    protected Socket socket;

    protected RTCProtocol rtcProtocol;
    protected WiFiP2PProtocol p2pProtocol;
    protected HandleConnection handleConnection;
    protected SignalingParameters signalingParameters;
    protected boolean isServer;

    public ConnectionHandler(HandleConnection handleConnection, HandleProtocol handleProtocol) {
        rtcProtocol = new RTCProtocol();
        p2pProtocol = new WiFiP2PProtocol();
        rtcProtocol.setRoomState(RTCProtocol.ConnectionState.NEW);
        isServer = false;
        this.executor = Executors.newSingleThreadExecutor();
        this.handleProtocol = handleProtocol;
        this.handleConnection = handleConnection;
        this.signalingParameters = null;
    }

    public void setSignalingParameters(SignalingParameters signalingParameters) {
        this.signalingParameters = signalingParameters;
    }

    public void connectToRoom(ConnectionParameter connectionParameter) {
        this.connectionParameter = connectionParameter;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        });
    }

    public void disconnectFromRoom() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });
    }

    public void requestUserInfo() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = p2pProtocol.getRequestUserInfo(Global.getInstance().getUser());
                if(json != null) socket.send(json.toString());
            }
        });
    }

    public void answerUserInfo() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = p2pProtocol.getAnswerUserInfo(Global.getInstance().getUser());
                if(json != null) socket.send(json.toString());
            }
        });
    }

    protected void connect() {
        rtcProtocol.setRoomState(RTCProtocol.ConnectionState.NEW);
        socket.start();
    }

    protected void disconnect() {
        rtcProtocol.setRoomState(RTCProtocol.ConnectionState.CLOSED);
        if(socket != null) {
            socket.disconnect();
//            socket = null;
        }
    }

    @Override
    public void onLocalDescription(SessionDescription sdp) {
        if(rtcProtocol != null) {
            JSONObject json = null;
            if(signalingParameters.initiator) {
                json = rtcProtocol.getOfferSdp(sdp);
            } else {
                json = rtcProtocol.getAnswerSdp(sdp);
            }
            if(json != null) send(json.toString());
        }
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        if(rtcProtocol != null) {
            JSONObject json = rtcProtocol.getLocalIceCandidate(candidate);
            if(json != null) send(json.toString());
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        if(rtcProtocol != null) {
            JSONObject json = rtcProtocol.getLocalIceCandidateRemovals(candidates);
            if(json != null) send(json.toString());
        }
    }

    @Override
    public void onIceConnected() {
        handleConnection.onIceConnected();
    }

    @Override
    public void onIceDisconnected() {
        handleConnection.onIceDisconnected();
    }

    @Override
    public void onPeerConnectionClosed() {

    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {

    }

    @Override
    public void onPeerConnectionError(String description) {

    }

    protected void send(String message) {
        socket.send(message);
    }

    @Override
    public void onConnected(boolean isServer) {
        this.isServer = isServer;
        if (isServer) {
            handleConnection.onConnect();
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");
            if(type.equals("candidate")) {
                handleProtocol.onRemoteIceCandidate(RTCProtocol.toJavaCandidate(json));
            } else if(type.equals("remove-candidates")) {
                JSONArray candidateArray = json.getJSONArray("candidates");
                IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                for(int i = 0; i < candidateArray.length(); ++i) {
                    candidates[i] = RTCProtocol.toJavaCandidate(candidateArray.getJSONObject(i));
                }
                handleProtocol.onRemoteIceCandidatesRemoved(candidates);
            } else if(type.equals("answer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type),
                        json.getString("sdp"));
                handleProtocol.onRemoteDescription(sdp);
            } else if(type.equals("offer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type),
                        json.getString("sdp"));

                SignalingParameters parameters = new SignalingParameters(
                        // Ice servers are not needed for direct connections.
                        new LinkedList<PeerConnection.IceServer>(),
                        false, // This code will only be run on the client side. So, we are not the initiator.
                        null, // clientId
                        null, // wssUrl
                        null, // wssPostUrl
                        sdp, // offerSdp
                        null // iceCandidates
                );
                rtcProtocol.setRoomState(RTCProtocol.ConnectionState.CONNECTED);
                handleProtocol.onConnectedToRoom(parameters);
            } else if(type.equals("p2p-request")) {
                String key = json.getString("key");
                String name = json.getString("name");
                String ip = json.getString("ip");
                User user = new User(key);
                user.setName(name);
                user.setIp(ip);
                handleConnection.onRequestUserInfo(user);
            } else if(type.equals("p2p-answer")) {
                rtcProtocol.setRoomState(RTCProtocol.ConnectionState.CONNECTED);

                SignalingParameters parameters = new SignalingParameters(
                        // Ice servers are not needed for direct connections.
                        new LinkedList<PeerConnection.IceServer>(),
                        isServer, // Server side acts as the initiator on direct connections.
                        null, // clientId
                        null, // wssUrl
                        null, // wwsPostUrl
                        null, // offerSdp
                        null // iceCandidates
                );
                handleProtocol.onConnectedToRoom(parameters);
            } else {
                handleProtocol.onChannelError("JSON parsing error : " + message);
            }
        } catch (JSONException e) {
            handleProtocol.onChannelError(e.toString());
        }
    }

    @Override
    public void onError(String description) {
        handleProtocol.onChannelError(description);
    }

    @Override
    public void onClose() {
        handleProtocol.onChannelClose();
    }

    public static class SignalingParameters {
        public final List<PeerConnection.IceServer> iceServers;
        public final boolean initiator;
        public final String clientId;
        public final String wssUrl;
        public final String wssPostUrl;
        public final SessionDescription offerSdp;
        public final List<IceCandidate> iceCandidates;

        public SignalingParameters(
                List<PeerConnection.IceServer> iceServers,
                boolean initiator, String clientId,
                String wssUrl, String wssPostUrl,
                SessionDescription offerSdp, List<IceCandidate> iceCandidates) {
            this.iceServers = iceServers;
            this.initiator = initiator;
            this.clientId = clientId;
            this.wssUrl = wssUrl;
            this.wssPostUrl = wssPostUrl;
            this.offerSdp = offerSdp;
            this.iceCandidates = iceCandidates;
        }
    }

    public interface HandleProtocol {
        void onConnectedToRoom(SignalingParameters params);
        void onRemoteDescription(SessionDescription sdp);
        void onRemoteIceCandidate(IceCandidate candidate);
        void onRemoteIceCandidatesRemoved(IceCandidate[] candidates);
        void onChannelClose();
        void onChannelError(String description);
    }

    public interface HandleConnection {
        void onConnect();
        void onRequestUserInfo(User user);
        void onIceConnected();
        void onIceDisconnected();
    }
}
