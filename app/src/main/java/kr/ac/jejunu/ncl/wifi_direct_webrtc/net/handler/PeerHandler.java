package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.handler;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.PeerConnectionClient;

/**
 * Created by jinhy on 2016-12-04.
 */

public class PeerHandler implements TCPHandler.HandleProtocol {
    private PeerConnectionClient peerConnectionClient = null;
    private TCPHandler.SignalingParameters signalingParameters;
    private HandlePeerConnection handlePeerConnection;

    public PeerHandler(PeerConnectionClient peerConnectionClient, HandlePeerConnection handlePeerConnection) {
        this.peerConnectionClient = peerConnectionClient;
        this.handlePeerConnection = handlePeerConnection;
    }

    public void setSignalingParameters(TCPHandler.SignalingParameters signalingParameters) {
        this.signalingParameters = signalingParameters;
    }

    @Override
    public void onConnectedToRoom(ConnectionHandler.SignalingParameters params) {
        handlePeerConnection.onConnectedToRoom(params);
    }

    @Override
    public void onRemoteDescription(SessionDescription sdp) {
        if (peerConnectionClient == null) {
            return;
        }
        peerConnectionClient.setRemoteDescription(sdp);
        if (!signalingParameters.initiator) {
            peerConnectionClient.createAnswer();
        }
    }

    @Override
    public void onRemoteIceCandidate(IceCandidate candidate) {
        if (peerConnectionClient == null) {
            return;
        }
        peerConnectionClient.addRemoteIceCandidate(candidate);
    }

    @Override
    public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {
        if (peerConnectionClient == null) {
            return;
        }
        peerConnectionClient.removeRemoteIceCandidates(candidates);
    }

    @Override
    public void onChannelClose() {
        handlePeerConnection.onChannelClose();
    }

    @Override
    public void onChannelError(String description) {
        handlePeerConnection.onChannelError(description);
    }

    public interface HandlePeerConnection {
        void onConnect();
        void onConnectedToRoom(ConnectionHandler.SignalingParameters params);
        void onChannelClose();
        void onChannelError(String description);
    }
}
