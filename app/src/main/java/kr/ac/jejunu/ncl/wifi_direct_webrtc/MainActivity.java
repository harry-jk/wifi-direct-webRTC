package kr.ac.jejunu.ncl.wifi_direct_webrtc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import kr.ac.jejunu.ncl.wifi_direct_webrtc.net.server.Server;
import kr.ac.jejunu.ncl.wifi_direct_webrtc.util.Util;

/**
 * Streaming the camera output of a host device (server) to a connected peer
 * (client), using the library LibStreaming.
 */
public class MainActivity extends Activity implements ChannelListener, DeviceListFragment.DeviceActionListener{
	private Activity mActivity = this;
	private boolean isWifiP2pEnabled = false;
	private boolean retryChannel = false;
	private BroadcastReceiver receiver = null;

	private final static String TAG = "Video Streaming";
	private Button mServerButton;
	private Button mClientButton;
	private Button mWiFiButton;
	private Spinner mBitrateSpinner;
	private Spinner mResolutionSpinner;
	private ListView mListView;
	private EditText mEnterIp;
	private TextView mUserIp;
	private WifiP2pManager manager;
	private static String mVideoIP;
	private static final String SERVER_IP = "server ip";
	private static final String BITRATE = "bitrate";
	private static final String RESOLUTION = "resolution";
	protected static final String P2P = null;
	private final IntentFilter intentFilter = new IntentFilter();
	WifiP2pManager mManager;
	Channel mChannel;
	BroadcastReceiver mReceiver;
	private Channel channel;
	private WifiP2pDevice device;

	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
		this.isWifiP2pEnabled = isWifiP2pEnabled;
	}

	public void resetData() {
		DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
		DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
				.findFragmentById(R.id.frag_detail);
		if (fragmentList != null) {
			fragmentList.clearPeers();
		}
		if (fragmentDetails != null) {
			fragmentDetails.resetViews();
		}
	}

	@Override
	public void showDetails(WifiP2pDevice device) {
		DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
		fragment.showDetails(device);

	}

	@Override
	public void connect(WifiP2pConfig config) {
		manager.connect(channel, config, new ActionListener() {

			@Override
			public void onSuccess() {
				// WiFiDirectBroadcastReceiver will notify us. Ignore for now.
			}

			@Override
			public void onFailure(int reason) {
				Toast.makeText(mActivity, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void disconnect() {
		final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
				.findFragmentById(R.id.frag_detail);
		fragment.resetViews();
		manager.removeGroup(channel, new ActionListener() {

			@Override
			public void onFailure(int reasonCode) {
				Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
			}

			@Override
			public void onSuccess() {
				fragment.getView().setVisibility(View.GONE);
			}

		});
	}

	@Override
	public void onChannelDisconnected() {
		// we will try once more
		if (manager != null && !retryChannel) {
			Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
			resetData();
			retryChannel = true;
			manager.initialize(this, getMainLooper(), this);
		} else {
			Toast.makeText(this, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void cancelDisconnect() {

		/*
		 * A cancel abort request by user. Disconnect i.e. removeGroup if
		 * already connected. Else, request WifiP2pManager to abort the ongoing
		 * request
		 */
		if (manager != null) {
			final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
					.findFragmentById(R.id.frag_list);
			if (fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
				disconnect();
			} else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
					|| fragment.getDevice().status == WifiP2pDevice.INVITED) {

				manager.cancelConnect(channel, new ActionListener() {

					@Override
					public void onSuccess() {
						Toast.makeText(mActivity, "Aborting connection", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int reasonCode) {
						Toast.makeText(mActivity, "Connect abort request failed. Reason Code: " + reasonCode,
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		}

	}
	
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
		startService(new Intent(MainActivity.this, Server.class));
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);

		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);
		// mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel,
		// this);

		final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);

		// Setting Group Owner Priority

		WifiP2pConfig config = new WifiP2pConfig();
		// config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		config.groupOwnerIntent = 15;

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		String ip = Util.getIPAddress(true);
		mVideoIP = "rtsp://" + ip + ":8988";
		Log.i(TAG, "IP: " + mVideoIP);

		mEnterIp = (EditText) findViewById(R.id.ip_text);
		mUserIp = (TextView) findViewById(R.id.user_ip);
		mEnterIp.setText("192.168.49.1");
		mUserIp.setText("This Device IP : " + ip);
		// Get button references
		mServerButton = (Button) findViewById(R.id.server_button);
		mClientButton = (Button) findViewById(R.id.client_button);
		mWiFiButton = (Button) findViewById(R.id.WiFi_Button);

		// mListView.setAdapter(new );

		mBitrateSpinner = (Spinner) findViewById(R.id.bitrate_spinner);
		mResolutionSpinner = (Spinner) findViewById(R.id.resolution_spinner);
		// Set what happens when buttons are clicked
		mServerButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Opens server activity

				String ip = mEnterIp.getText().toString();
				boolean wifiEnabled = wifiManager.isWifiEnabled();
				if (wifiEnabled == false) {
					Toast.makeText(getApplicationContext(), "WiFi is not Enabled, Enabling WiFi first",
							Toast.LENGTH_SHORT).show();
					wifiManager.setWifiEnabled(true);
					startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 1);
				}
				if (wifiEnabled == true) {
					Intent launchServer = new Intent(getApplicationContext(), CallActivity.class);
					launchServer.putExtra(SERVER_IP, ip);
					launchServer.putExtra(BITRATE, (String) mBitrateSpinner.getSelectedItem());
					launchServer.putExtra(RESOLUTION, (String) mResolutionSpinner.getSelectedItem());
					startActivity(launchServer);
				}
			}
		});

		mClientButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String ip = mEnterIp.getText().toString();
				boolean wifiEnabled = wifiManager.isWifiEnabled();
				if (wifiEnabled == false) {
					Toast.makeText(getApplicationContext(), "WiFi is not Enabled, Enabling WiFi first",
							Toast.LENGTH_SHORT).show();
					wifiManager.setWifiEnabled(true);
					startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 1);
				}
				// ConnectivityManager connManager = (ConnectivityManager)
				// getSystemService(Context.CONNECTIVITY_SERVICE);
				// NetworkInfo mWifi =
				// connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				if (ip.length() < 7)
					Toast.makeText(getApplicationContext(), "Please enter a valid IP!", Toast.LENGTH_SHORT).show();
				else if (wifiEnabled == true) {
					Intent launchClient = new Intent(MainActivity.this, CallActivity.class);
					launchClient.putExtra(SERVER_IP, ip);
					startActivity(launchClient);
				}
			}
		});
		// Function for calling Wifi Direct setting
		mWiFiButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				wifiManager.setWifiEnabled(true);
				if (!isWifiP2pEnabled) {
					Toast.makeText(mActivity, "R.string.p2p_off_warning", Toast.LENGTH_SHORT).show();
				}
				final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
						.findFragmentById(R.id.frag_list);
				fragment.onInitiateDiscovery();
				manager.discoverPeers(channel, new ActionListener() {

					@Override
					public void onSuccess() {
						Toast.makeText(mActivity, "Discovery Initiated", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int reasonCode) {
						Toast.makeText(mActivity, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT).show();
					}
				});

				// //WiFi direct is enable or not
				// try {
				// Class<?> wifiManager = Class
				// .forName("android.net.wifi.p2p.WifiP2pManager");
				//
				// Method method = wifiManager
				// .getMethod(
				// "enableP2p",
				// new Class[] {
				// android.net.wifi.p2p.WifiP2pManager.Channel.class });
				//
				// method.invoke(manager, channel);
				//
				// } catch (Exception e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				//
				// /////////////////////////////////////////////////////////////////////////////
				// manager.discoverPeers(channel, new
				// WifiP2pManager.ActionListener() {
				// @Override
				// public void onSuccess() {
				// Toast.makeText(getApplicationContext(),
				// "Discovery successful", Toast.LENGTH_SHORT)
				// .show();
				// }
				//
				// @Override
				// public void onFailure(int reasonCode) {
				// Toast.makeText(getApplicationContext(),
				// "Fail to discover peers", Toast.LENGTH_SHORT)
				// .show();
				// }
				// });
				//
				//
				// ////////////////////////////////////////////////////////////////////////
				// Toast.makeText(getApplicationContext(),
				// "Entering WiFi Direct Setting Mode", Toast.LENGTH_SHORT)
				// .show();
				// startActivityForResult(new
				// Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), 1);
				// wifiManager.setWifiEnabled(true);

			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onRestart();
		Intent i = new Intent(MainActivity.this, MainActivity.class); // your
																		// class
		startActivity(i);
		finish();
	}

	public void connect(final WifiP2pDevice device) {
		getDeviceStatus(device.status);
		// obtain a peer from the WifiP2pDeviceList
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		config.groupOwnerIntent = 15; // I want this device to become the owner
		WifiP2pManager mManager = null;
		Channel mChannel = null;
		mManager.connect(mChannel, config, new ActionListener() {
			@Override
			public void onSuccess() {
				// success logic
				Log.d(P2P, "connected to device " + device.deviceName);
			}

			@Override
			public void onFailure(int reason) {
				// failure logic
				Log.d(P2P, "unable to establish connection to device " + device.deviceName);
			}
		});
	}

	/*
	 * public void onChannelDisconnected() { boolean retryChannel = false; // we
	 * will try once more if (manager != null && !retryChannel) {
	 * Toast.makeText(this, "Channel lost. Trying again",
	 * Toast.LENGTH_LONG).show(); resetData(); retryChannel = true;
	 * manager.initialize(this, getMainLooper(), (ChannelListener) this); } else
	 * { Toast.makeText(this,
	 * "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P."
	 * , Toast.LENGTH_LONG).show(); } }
	 */

	public WifiP2pDevice getDevice() {
		return device;
	}

	private static String getDeviceStatus(int deviceStatus) {
		switch (deviceStatus) {
		case WifiP2pDevice.AVAILABLE:
			return "Available";
		case WifiP2pDevice.INVITED:
			return "Invited";
		case WifiP2pDevice.CONNECTED:
			return "Connected";
		case WifiP2pDevice.FAILED:
			return "Failed";
		case WifiP2pDevice.UNAVAILABLE:
			return "Unavailable";
		default:
			return "Unknown";

		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Stop RTSP server if it is running
		getApplicationContext().stopService(new Intent(this, Server.class));
	}

}