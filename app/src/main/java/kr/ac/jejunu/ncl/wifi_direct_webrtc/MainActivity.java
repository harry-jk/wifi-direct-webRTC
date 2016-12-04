package kr.ac.jejunu.ncl.wifi_direct_webrtc;

import android.Manifest;
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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.User;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.client.Client;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.server.Server;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.receiver.WifiDirectBroadcastReceiver;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.util.Util;

public class MainActivity extends AppCompatActivity {
    private User mAppUser;
    private ScheduledExecutorService executor;
    private Client client;

    // Wifi Direct
    private static BroadcastReceiver mReceiver;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    // webRTC API
    private EglBase rootEglBase;

    // layouts
    private SurfaceViewRenderer remoteRenderer;
    private SurfaceViewRenderer localRenderer;
    private Spinner remoteListView;
    private PeerListAdapter remoteListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // Layout Setup
        remoteRenderer = (SurfaceViewRenderer) findViewById(R.id.remote_video_renderer);
        localRenderer = (SurfaceViewRenderer) findViewById(R.id.local_video_renderer);
        remoteListView = (Spinner) findViewById(R.id.remote_connect_list);
        remoteListAdapter = new PeerListAdapter();
        remoteListView.setAdapter(remoteListAdapter);
        remoteListView.setPrompt("No Connection");
        remoteListView.setOnItemSelectedListener(mUserSelectListener);

        // Default Data
        String address = Util.getMacAddr();
        mAppUser = new User(address);
        executor = Executors.newSingleThreadScheduledExecutor();

        // Layout Renderer Setup
        rootEglBase = EglBase.create();
        remoteRenderer.init(rootEglBase.getEglBaseContext(), null);
        localRenderer.init(rootEglBase.getEglBaseContext(), null);
        localRenderer.setZOrderOnTop(true);
        updateVideoView();

        // Permission
        TedPermission permissionChecker = new TedPermission(this);
        permissionChecker.setPermissionListener(new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Wifi Direct
                        startService(new Intent(MainActivity.this, Server.class));
                    }
                });

                initWiFiDirect();
                Intent intent = getIntent();
                client = new Client(MainActivity.this, intent,
                        rootEglBase, remoteRenderer, localRenderer);

                executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        client.connect();
                    }
                }, 1, TimeUnit.SECONDS);
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                finish();
            }
        });
        permissionChecker.setPermissions(Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_WIFI_STATE);
        permissionChecker.check();


//        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
//        dialogBuilder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//            }
//        });
//        dialogBuilder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(client != null) client.startVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(client != null) client.stopVideo();
    }

    @Override
    protected void onDestroy() {
        client.disconnect();
        if (localRenderer != null) {
            localRenderer.release();
            localRenderer = null;
        }
        if (remoteRenderer != null) {
            remoteRenderer.release();
            remoteRenderer = null;
        }
        rootEglBase.release();
        stopService(new Intent(this, Server.class));
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void initWiFiDirect() {
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, mPeerListener);
        registerReceiver(mReceiver, mIntentFilter);

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
//                new Timer().schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        mManager.requestPeers(mChannel, mPeerListener);
//                    }
//                }, 0, 10000);
                mManager.requestPeers(mChannel, mPeerListener);
            }

            @Override
            public void onFailure(int i) {
            }
        });
    }

    private void updateVideoView() {
        remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        remoteRenderer.setMirror(false);

        localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localRenderer.setMirror(true);

        localRenderer.requestLayout();
        remoteRenderer.requestLayout();
    }

    WifiP2pManager.PeerListListener mPeerListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            for (final WifiP2pDevice peer : peers.getDeviceList()) {
                User user = new User(peer.deviceAddress);
                remoteListAdapter.add(user);
                remoteListAdapter.notifyDataSetChanged();
            }
        }
    };

    AdapterView.OnItemSelectedListener mUserSelectListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            User user = (User) remoteListAdapter.getItem(position);
            if(user.isConnected()) {

            } else {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = user.getKey();
                config.wps.setup = WpsInfo.PBC;
                config.groupOwnerIntent = 15;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        mAppUser.setIp(Util.getIPAddress(true));
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

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    class PeerListAdapter extends BaseAdapter {
        ArrayList<User> users;

        public PeerListAdapter() {
            users = new ArrayList<>();
        }

        public void add(User user) {
            for(User current: users) {
                if(current.getKey().equals(user.getKey())) {
                    return;
                }
            }
            users.add(user);
        }

        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public Object getItem(int position) {
            if(users.size() < position) return null;
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            if(users.size() < position) return 0;
            return users.get(position).getKey().hashCode();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            User user = (User) getItem(position);
            if(user != null) {
                String str = user.isConnected() ? user.getIp() : user.getKey();
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(str);
            } else {
                ((TextView) convertView.findViewById(android.R.id.text1)).setText("");
            }

            return convertView;
        }
    }
}
