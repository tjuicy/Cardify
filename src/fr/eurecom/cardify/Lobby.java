package fr.eurecom.cardify;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import fr.eurecom.cardify.R.drawable;
import fr.eurecom.messaging.Client;
import fr.eurecom.messaging.PeerListener;
import fr.eurecom.messaging.WiFiDirectBroadcastReceiver;
import fr.eurecom.util.CustomButton;
import fr.eurecom.util.CustomTextView;

public class Lobby extends Activity implements ConnectionInfoListener {

	private WifiP2pManager mManager;
	private Channel mChannel;
	private WiFiDirectBroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	private WifiP2pDeviceList peers;
	private Client client;
	private ProgressDialog progressDialog;
	private int initCards;
	private String currentTargetDeviceName;
	private String thisDeviceName;
	private Set<String> deviceNames;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);
		
		//Keeps screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		deviceNames = new HashSet<String>();

		peers = new WifiP2pDeviceList();
		initCards = 0;
		progressDialog = new ProgressDialog(this);
		
		setUpWiFiDirect();
		disconnectFromDevices();
		findPeers();
	}

	protected void setUpWiFiDirect() {
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);
		
		mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
		
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		
	}

	public void resetPeerList() {
		((CustomTextView) findViewById(R.id.lobby_connectedPeersList)).setText("");
		((LinearLayout) findViewById(R.id.lobby_availablePeersList)).removeAllViews();
	}
	
	public void setPeers(WifiP2pDeviceList peers) {
		this.peers = peers;
		resetPeerList();
		printPeers();
	}
	
	private void printPeers() {
		((Button) findViewById(R.id.lobby_refreshPeersBtn)).clearAnimation();
		for (WifiP2pDevice device : peers.getDeviceList()) {
			if (device.status == WifiP2pDevice.CONNECTED) {
				addToListOfConnectedPeers(device);
			} else {
				addToListOfAvailablePeers(device);
			}
		}
		
	}
	
	private void addToListOfAvailablePeers(final WifiP2pDevice device) {
		CustomButton view = new CustomButton(this);
		view.setText(device.deviceName);
		view.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				connectToDevice(device);
				deviceNames.add(device.deviceName);
			}
		});
		view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		view.setCompoundDrawablesWithIntrinsicBounds(drawable.ic_action_add_person,0,0,0);
		((ViewGroup) findViewById(R.id.lobby_availablePeersList)).addView(view);
	}
	
	private void addToListOfConnectedPeers(final WifiP2pDevice device) {
		CustomTextView view = (CustomTextView) findViewById(R.id.lobby_connectedPeersList);
		view.append("- " + device.deviceName + "\n");
		CustomButton btn = (CustomButton) findViewById(R.id.disconnect);
		btn.setVisibility(Button.VISIBLE);
	}
		
	private void connectToDevice(WifiP2pDevice device) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		currentTargetDeviceName = device.deviceName;

		config.groupOwnerIntent = 15;
		mManager.connect(mChannel, config, new LobbyActionListener("Not connected to peer", "Connected to peer"));
		showProgressDialog("Connecting to device", "The player you're trying to connect to has to accept");
		timerDelayRemoveDialog(30000, progressDialog);
	}
	
	public String getCurrentTargetDeviceName() {
		return currentTargetDeviceName;
	}
	
	public void setThisDeviceName(String name) {
		thisDeviceName = name;
	}
	
	private void showProgressDialog(String title, String message) {
		this.progressDialog.setTitle(title);
		this.progressDialog.setMessage(message);
		this.progressDialog.setCancelable(false);
		this.progressDialog.show();
	}
	
	public void timerDelayRemoveDialog(long time, final Dialog d){
	    new Handler().postDelayed(new Runnable() {
	        public void run() {                
	            if (d.isShowing()) { 
		        	d.dismiss();  
		            cancelConnect();
	            }
	        }
	    }, time); 
	}
	
	public void dismissProgressDialog() {
		if (progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}
	
	private void disconnectFromDevices() {
		mManager.removeGroup(mChannel, new LobbyActionListener("Failed disconnecting", "Disconnected"));
	}
	
	public void disconnectFromDevices(View v) {
		disconnectFromDevices();
	}
	

	private void findPeers() {
		mChannel = mManager.initialize(this, getMainLooper(), null);
		mManager.discoverPeers(mChannel, new ActionListener() {
			@Override
			public void onSuccess() {
			}
			
			@Override
			public void onFailure(int reason) {
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby, menu);
		return true;
	}

	/* register the broadcast receiver with the intent values to be matched */
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mReceiver, mIntentFilter);
	}

	/* unregister the broadcast receiver */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}
	
	public void refreshPeers(View view) {
		resetPeerList();
		findPeers();
		view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely));
	}
	
	// Start a new game as host
	public void startGame(View view) {
		showCardHandSizeSelector();
	}
	
	private void showCardHandSizeSelector() {
		int maxHandSize = (int) Math.floor(52/(client.getReceivers().size()+1));
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Select initial card hand: ");

		final NumberPicker np = new NumberPicker(this);
		
		String[] numbers = new String[maxHandSize+1];
		
		for (int i = 0; i < maxHandSize+1; i++) {
			numbers[i] = Integer.toString(i);
		}
		
		np.setMinValue(0);
		np.setMaxValue(maxHandSize);
		np.setWrapSelectorWheel(false);
		np.setDisplayedValues(numbers);
		np.setValue(0);

		alert.setPositiveButton("Start game",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						initCards = np.getValue();
						client.broadcastStartGame();
					}
				});
		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Cancel.
					}
				});
		alert.setView(np);
		alert.show();
	}
	
	public void startGameActivity(Set<InetAddress> receivers, boolean isHost){
		client.disconnect();
		Intent intent = new Intent(this, Game.class);
		String addresses = "";
		for (InetAddress addr : receivers){
			addresses += addr.toString() + ",";
		}
		intent.putExtra("isSolitaire", false);
		intent.putExtra("receivers", addresses);
		intent.putExtra("isHost", isHost);
		intent.putExtra("deviceName", thisDeviceName);
		if (isHost) {
			intent.putExtra("cardsPerPlayer", initCards);
		}
		resetLobby();
		
		this.startActivity(intent);
	}
	
	public void startGameActivity(Set<InetAddress> receivers){
		startGameActivity(receivers, false);
	}
	
	@Override
	protected void onDestroy() {
		disconnectClient();
		resetLobby();
		super.onDestroy();
	}
	
	// Every time new connection is available
	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		if (info.groupFormed) {
			if (info.isGroupOwner) {
				setUpHost(info);
			} else {
				setUpClient(info);
			}	
		}
	}
	
	// Create host if not already done
	private void setUpHost(WifiP2pInfo info) {
		if (this.client == null){
			Button startButton = (Button) findViewById(R.id.lobby_startGameBtn);
			startButton.setVisibility(Button.VISIBLE);
			this.client = new Client(this);
			this.client.changeToHost();
		}
	}
	
	public void removeStartButton() {
		Button startButton = (Button) findViewById(R.id.lobby_startGameBtn);
		startButton.setVisibility(Button.INVISIBLE);
	}
	public void removeDisconnectButton() {
		Button disconnect = (Button) findViewById(R.id.disconnect);
		disconnect.setVisibility(Button.INVISIBLE);
	}
	
	private void removeClient() {
		this.client = null;
	}
	
	private void disconnectClient() {
		if (this.client != null) {
			client.disconnect();
		} else {
		}
	}
	
	public void resetLobby() {	
		disconnectClient();

		removeClient();
		removeStartButton();
		removeDisconnectButton();
		resetPeerList();
		peers = new WifiP2pDeviceList();
		dismissProgressDialog();
		initCards = 0;
	}
	
	// Create client and register at host
	private void setUpClient(WifiP2pInfo info) {
		this.client = new Client(this);
		this.client.addReceiver(info.groupOwnerAddress);
		
		//this.client.registerAtHost();
		mManager.requestPeers(mChannel, new PeerListener(this.client));
		showProgressDialog("Waiting for host", "The host must start the game");
	}
	
	private void cancelConnect() {
		mManager.cancelConnect(mChannel, new LobbyActionListener("Failed cancel connect", "Cancelled connect"));
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	}
	
	public void deviceDisconnected() {
		resetLobby();
	}
	public void setHostDeviceName(String[] allDeviceNames) {
		for (String deviceName : allDeviceNames) {
			if (!deviceNames.contains(deviceName)) {
				thisDeviceName = deviceName;
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		if (client != null) {
			disconnectFromDevices();
			resetLobby();
		}
		super.onBackPressed();
	}
		
	private class LobbyActionListener implements ActionListener {
		
		private LobbyActionListener(String failureMessage, String successMessage) {
		}
		
		@Override
		public void onFailure(int reason) {
		}

		@Override
		public void onSuccess() {
		}
	}
	
}
