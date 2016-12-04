package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.client;

import android.content.Context;
import android.content.Intent;

import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.DirectRTCClient;
import org.webrtc.Camera2Enumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.PeerConnectionClient;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.Params;

/**
 * Created by jinhy on 2016-11-22.
 */

public class Client implements Params, PeerConnectionClient.PeerConnectionEvents,
        AppRTCClient.SignalingEvents {
    private ScheduledExecutorService executor;

    private AppRTCClient.SignalingParameters signalingParameters;
    private AppRTCClient appRtcClient;
    private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private PeerConnectionClient peerConnectionClient = null;
    private boolean commandLineRun;
    private boolean iceConnected;

    private EglBase rootEglBase;
    private SurfaceViewRenderer remoteRenderer;
    private SurfaceViewRenderer localRenderer;

    public Client(Context context, Intent intent, EglBase rootEglBase,
                  SurfaceViewRenderer remoteRenderer, SurfaceViewRenderer localRenderer) {
        this.rootEglBase = rootEglBase;
        this.remoteRenderer = remoteRenderer;
        this.localRenderer = localRenderer;
        executor = Executors.newSingleThreadScheduledExecutor();

        boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);

        boolean useCamera2 = Camera2Enumerator.isSupported()
                && intent.getBooleanExtra(EXTRA_CAMERA2, false);
        peerConnectionParameters = new PeerConnectionClient.PeerConnectionParameters(
                intent.getBooleanExtra(EXTRA_VIDEO_CALL, true),
                loopback,
                tracing,
                useCamera2,
                intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0),
                intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0),
                intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0),
                intent.getStringExtra(EXTRA_VIDEOCODEC),
                intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                intent.getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, useCamera2),
                intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0),
                intent.getStringExtra(EXTRA_AUDIOCODEC),
                intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
                intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
                intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
                intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
                intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
                intent.getBooleanExtra(EXTRA_ENABLE_LEVEL_CONTROL, false));
        commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
        appRtcClient = new DirectRTCClient(this);
        roomConnectionParameters = new AppRTCClient.RoomConnectionParameters(
                "", "127.0.0.1:8888", loopback);

        peerConnectionClient = PeerConnectionClient.getInstance(false);
        if (loopback) {
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = 0;
            peerConnectionClient.setPeerConnectionFactoryOptions(options);
        }
        peerConnectionClient.createPeerConnectionFactory(
                context, localRenderer, remoteRenderer,
                peerConnectionParameters, this);


        executor.schedule(new Runnable() {
            @Override
            public void run() {
                startCall();
            }
        }, 1, TimeUnit.SECONDS);
    }

    public void startVideo() {
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    public void stopVideo() {
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    public void disconnect() {
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
    }

    private void startCall() {
        if (appRtcClient == null) {
            return;
        }
        // Start room connection.
        appRtcClient.connectToRoom(roomConnectionParameters);
    }

    @Override
    public void onLocalDescription(SessionDescription sdp) {
        if (appRtcClient != null) {
            if (signalingParameters.initiator) {
                appRtcClient.sendOfferSdp(sdp);
            } else {
                appRtcClient.sendAnswerSdp(sdp);
            }
        }
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidate(candidate);
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidateRemovals(candidates);
        }
    }

    @Override
    public void onIceConnected() {
        iceConnected = true;
        if (peerConnectionClient == null) {
            return;
        }
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    @Override
    public void onIceDisconnected() {
        iceConnected = false;
        disconnect();
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

    @Override
    public void onConnectedToRoom(AppRTCClient.SignalingParameters params) {
        signalingParameters = params;
        peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(),
                localRenderer, remoteRenderer, signalingParameters);

        if (signalingParameters.initiator) {
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(params.offerSdp);
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
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
        disconnect();
    }

    @Override
    public void onChannelError(String description) {
        disconnect();
    }
}
