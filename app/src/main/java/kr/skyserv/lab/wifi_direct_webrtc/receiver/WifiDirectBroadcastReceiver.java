package kr.skyserv.lab.wifi_direct_webrtc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;

import kr.skyserv.lab.wifi_direct_webrtc.MainActivity;

/**
 * Created by jinhy on 2016-11-22.
 */

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager.PeerListListener mPeerListener;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       WifiP2pManager.PeerListListener peerListener) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mPeerListener = peerListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
//                mActivity.setConnectionStatus("Wifi-Direct is Enabled");
            } else {
//                mActivity.setConnectionStatus("Wifi-Direct is Disabled");
//                mActivity.setIsPeerChosen(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed.
//            mActivity.setDiscoveryStatus("Peers updated.");
/*            if (mManager != null) {
                mManager.requestPeers(mChannel, mPeerListener);
            }*/
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Connection state changed!
            NetworkInfo networkState = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            // Check if we connected or disconnected.
            if (networkState.isConnected()) {
//                mActivity.setConnectionStatus("Connected");
//                mActivity.updateGroupIP();
            }
            else {
//                mActivity.setConnectionStatus("Disconnected");
                mManager.cancelConnect(mChannel, null);
//                mActivity.setIsPeerChosen(false);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // This device's wifi state changed

        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)){
            // Peer discovery stopped or started
            int discovery = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
            if (discovery == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
//                mActivity.setDiscoveryStatus("Peer discovery started.");
            } else if (discovery == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
//                mActivity.setDiscoveryStatus("Peer discovery stopped.");
            }
        }
    }
}
