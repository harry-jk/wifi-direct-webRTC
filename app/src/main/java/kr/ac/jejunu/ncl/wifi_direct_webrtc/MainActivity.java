package kr.ac.jejunu.ncl.wifi_direct_webrtc;

import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.model.User;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.client.Client;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.server.Server;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.util.Util;

public class MainActivity extends AppCompatActivity {
    private User mAppUser;
    private ScheduledExecutorService executor;
    private Client client;

    // webRTC API
    private EglBase rootEglBase;

    // layouts
    private SurfaceViewRenderer remoteRenderer;
    private SurfaceViewRenderer localRenderer;
    private Spinner remoteListView;
    private ArrayAdapter<String> remoteListAdapter;

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
        remoteListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        remoteListView.setAdapter(remoteListAdapter);
        remoteListAdapter.add("No Connection");

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

                Intent intent = getIntent();
                client = new Client(MainActivity.this, intent,
                        rootEglBase, remoteRenderer, localRenderer);
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
                Manifest.permission.ACCESS_NETWORK_STATE);
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
