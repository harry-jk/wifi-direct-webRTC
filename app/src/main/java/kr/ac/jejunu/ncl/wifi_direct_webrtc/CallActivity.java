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
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.Global;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.User;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.client.Client;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.server.Server;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.util.Util;

public class CallActivity extends AppCompatActivity {
    private User mAppUser;
    private ScheduledExecutorService executor;
    private Client client;
    private static final String SERVER_IP = "server ip";
    private static final String IS_SERVER = "is_server";


    // layouts
    private SurfaceViewRenderer remoteRenderer;
    private SurfaceViewRenderer localRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_call);

        // Layout Setup
        remoteRenderer = (SurfaceViewRenderer) findViewById(R.id.remote_video_renderer);
        localRenderer = (SurfaceViewRenderer) findViewById(R.id.local_video_renderer);

        // Default Data
        String address = Util.getMacAddr();
        mAppUser = new User(address);
        executor = Executors.newSingleThreadScheduledExecutor();

        // Layout Renderer Setup
        remoteRenderer.init(Global.getInstance().getRootEglBase().getEglBaseContext(), null);
        localRenderer.init(Global.getInstance().getRootEglBase().getEglBaseContext(), null);
        localRenderer.setZOrderOnTop(true);
        updateVideoView();

        // Permission
        TedPermission permissionChecker = new TedPermission(this);
        permissionChecker.setPermissionListener(new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Intent intent = getIntent();
                String ip = intent.getStringExtra(SERVER_IP);
                boolean isServer = intent.getBooleanExtra(IS_SERVER, false);
                if(isServer) {
                    client = new Client(CallActivity.this, intent, "0.0.0.0", remoteRenderer, localRenderer);
                } else {
                    client = new Client(CallActivity.this, intent, ip, remoteRenderer, localRenderer);
                }

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
        stopService(new Intent(this, Server.class));
        super.onDestroy();
    }

    private void updateVideoView() {
        remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        remoteRenderer.setMirror(false);

        localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localRenderer.setMirror(true);

        localRenderer.requestLayout();
        remoteRenderer.requestLayout();
    }
}
