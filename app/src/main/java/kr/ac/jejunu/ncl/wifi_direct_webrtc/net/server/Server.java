package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.webrtc.Camera2Enumerator;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import java.util.LinkedList;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.PeerConnectionClient;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.Global;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.User;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.ConnectionParameter;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.Params;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.handler.ConnectionHandler;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.handler.PeerHandler;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.handler.TCPHandler;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.util.Util;

/**
 * Created by jinhy on 2016-11-22.
 */

public class Server extends Service implements Params,
        ConnectionHandler.HandleConnection, PeerHandler.HandlePeerConnection {
    private final IBinder mBinder = new LocalBinder();
    private static final int STAT_CALLBACK_PERIOD = 1000;

    private PeerConnectionClient peerConnectionClient = null;
    TCPHandler tcpHandler;
    PeerHandler peerHandler;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) return super.onStartCommand(intent, flags, startId);
        boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);

        boolean useCamera2 = Camera2Enumerator.isSupported()
                && intent.getBooleanExtra(EXTRA_CAMERA2, false);
        PeerConnectionClient.PeerConnectionParameters peerConnectionParameters
                = new PeerConnectionClient.PeerConnectionParameters(
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

        peerConnectionClient = PeerConnectionClient.getInstance(true);
        peerHandler = new PeerHandler(peerConnectionClient, this);
        tcpHandler = new TCPHandler(this, peerHandler);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 0;
        peerConnectionClient.setPeerConnectionFactoryOptions(options);
        peerConnectionClient.createPeerConnectionFactory(Server.this, null, null, peerConnectionParameters, tcpHandler);

        ConnectionParameter connectionParameter = new ConnectionParameter("", "0.0.0.0:8888", false);
        tcpHandler.connectToRoom(connectionParameter);
        ConnectionHandler.SignalingParameters params = new ConnectionHandler.SignalingParameters(
                // Ice servers are not needed for direct connections.
                new LinkedList<PeerConnection.IceServer>(),
                true, // Server side acts as the initiator on direct connections.
                null, // clientId
                null, // wssUrl
                null, // wwsPostUrl
                null, // offerSdp
                null // iceCandidates
        );
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (tcpHandler != null) {
            tcpHandler.disconnectFromRoom();
            tcpHandler = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void disconnect() {
    }

    private void setSignalingParameters(ConnectionHandler.SignalingParameters params) {
        tcpHandler.setSignalingParameters(params);
        peerHandler.setSignalingParameters(params);
    }

    @Override
    public void onConnect() {
        Global.getInstance().getUser().setIp(Util.getIPAddress(true));
        tcpHandler.requestUserInfo();
    }

    @Override
    public void onRequestUserInfo(User user) {
        tcpHandler.answerUserInfo();
    }

    @Override
    public void onConnectedToRoom(ConnectionHandler.SignalingParameters params) {
        setSignalingParameters(params);
        peerConnectionClient.createPeerConnection(Global.getInstance().getRootEglBase().getEglBaseContext(),
                null, null, params);
        peerConnectionClient.createOffer();
    }

    @Override
    public void onChannelClose() {
        disconnect();
    }

    @Override
    public void onChannelError(String description) {
        disconnect();
    }

    @Override
    public void onIceConnected() {
        if (peerConnectionClient == null) {
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    @Override
    public void onIceDisconnected() {
        disconnect();
    }

    public class LocalBinder extends Binder {
        public Server getService() {
            return Server.this;
        }
    }
}
