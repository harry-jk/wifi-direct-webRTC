package kr.ac.jejunu.ncl.wifi_direct_webrtc.net.server;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.DirectRTCClient;
import org.webrtc.Camera2Enumerator;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.PeerConnectionClient;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.Params;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.receiver.WifiDirectBroadcastReceiver;

/**
 * Created by jinhy on 2016-11-22.
 */

public class Server extends Service
        implements Params, AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents {
    private final IBinder mBinder = new LocalBinder();

    private static final int STAT_CALLBACK_PERIOD = 1000;

    // Wifi Direct
    private static BroadcastReceiver mReceiver;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private PeerConnectionClient peerConnectionClient = null;
    private ExecutorService executor;
    private AppRTCClient.SignalingParameters signalingParameters;


    private AppRTCClient appRtcClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//        mChannel = mManager.initialize(this, getMainLooper(), null);
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, mPeerListener);
        registerReceiver(mReceiver, mIntentFilter);

//        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                mManager.requestPeers(mChannel, mPeerListener);
//            }
//
//            @Override
//            public void onFailure(int i) {
//            }
//        });

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
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 0;
        peerConnectionClient.setPeerConnectionFactoryOptions(options);
        peerConnectionClient.createPeerConnectionFactory(
                Server.this, null, null, peerConnectionParameters, Server.this);

        appRtcClient = new DirectRTCClient(this);
        AppRTCClient.RoomConnectionParameters roomConnectionParameters = new AppRTCClient.RoomConnectionParameters(
                "", "0.0.0.0:8888", false);
        appRtcClient.connectToRoom(roomConnectionParameters);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void callConnected() {
        if (peerConnectionClient == null) {
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    }

    private void disconnect() {
    }

    @Override
    public void onConnectedToRoom(AppRTCClient.SignalingParameters params) {
        signalingParameters = params;
        peerConnectionClient.createPeerConnection(null, null, null, signalingParameters);
        peerConnectionClient.createOffer();
    }

    @Override
    public void onRemoteDescription(SessionDescription sdp) {
        if (peerConnectionClient == null) {
            return;
        }
        peerConnectionClient.setRemoteDescription(sdp);
        if (!signalingParameters.initiator) {
            // Create answer. Answer SDP will be sent to offering client in
            // PeerConnectionEvents.onLocalDescription event.
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
        callConnected();
    }

    @Override
    public void onIceDisconnected() {
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

    public class LocalBinder extends Binder {
        public Server getService() {
            return Server.this;
        }
    }

    WifiP2pManager.PeerListListener mPeerListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            for (final WifiP2pDevice peer : peers.getDeviceList()) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = peer.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                            @Override
                            public void onGroupInfoAvailable(WifiP2pGroup group) {
                                Log.i("TEST", group == null ? "" : group.toString());
                            }
                        });
                        mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                Log.i("TEST", info == null ? "" : info.toString());
                            }
                        });
                    }

                    @Override
                    public void onFailure(int i) {
                        switch (i) {
                            case WifiP2pManager.P2P_UNSUPPORTED:
                                break;
                            case WifiP2pManager.BUSY:
                                break;
                            case WifiP2pManager.ERROR:
                                break;
                            default:
                                break;
                        }
                    }
                });
            }
        }
    };
}
