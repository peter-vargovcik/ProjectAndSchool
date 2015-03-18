package vargovcik.peter.atasp_companionapp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.DecimalFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.camera.simplemjpeg.MjpegInputStream;
import com.camera.simplemjpeg.MjpegView;

import vargovcik.peter.atasp_companionapp.helpers.HelperMethods;
import vargovcik.peter.atasp_companionapp.helpers.Updateobject;
import vargovcik.peter.atasp_companionapp.uiaddons.AnalogPad;
import vargovcik.peter.atasp_companionapp.uiaddons.AnalogPadInterface;
import vargovcik.peter.compationApp.CompanionAppData;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class TrexController extends Activity {
	private static final boolean DEBUG = true;
	private static final int OBJECT_OUTPUT_STREAM_S_LOOPS_TRESHOLD = 500;
	private static final int INITIAL_TREX_POWER_LEVEL = 30;
	private static final String TAG = "MJPEG";
	private Activity activity;

	private Socket connection;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private boolean connectionStatus, remoteControlChangeRequested, maxPlatformPowerConfirmed, 
		newMaxPlatformPowerRequested, whatchStream, keyUp, suspending, idVideoStreamEnabled,
		searchPaused, proximityOverride;
	private ToggleButton btnConnect, btnVideoFeed, btnRemoteControl, btnFeeds,toggleMissionControll,toggleSearchControll,toggleProximityOverride;
	private SeekBar platformPowerOverdriveSeekBar;
	private Button btnPlatformPowerConfirm;
	
	private ImageView proximityB1,proximityB2,proximityB3,proximityB4,proximityF1,proximityF2,proximityF3,proximityF4;
	private boolean[] proximityLaststate = new boolean[8];
	private LinearLayout proximityView;
	
	private Context context;
	boolean remoteControlled = false;
	private byte[] trexCommand = new byte[2];
	private int motorA, motorB, trexMaxPower,trexMaxPowerChanged, pan, tilt;

	private AnalogPad analogPadDrive, analogPadPanTilt;
	private TextView textView,lightSensorTV,distanceSensorTV,temperatureSensorTV,barPressureSensorTV,altitudeSensorTV,platformPowerTV;
	private HelperMethods helperMethods;

	// video Stream Stuff

	private MjpegView mv = null;
	private String URL;

	// for settings (network and resolution)
	private static final int REQUEST_SETTINGS = 0;

	private int width = 640;
	private int height = 360;

	private int ip_ad1 = 192;
	private int ip_ad2 = 168;
	private int ip_ad3 = 0;
	private int ip_ad4 = 41;
	private int ip_port = 9000;
	private String ip_command = "?action=stream";

	final Handler handler = new Handler();
	private LinearLayout feedsDashboard, missionControllDashboard;
	private FrameLayout videoScreenBlinderLayout, proximityLayout;
	private Animation feedsDashboardSlideIn, feedsDashboardSlideOut,videoScreenSlideIn, 
			videoScreenSlideOut,proximityViewSlideIn, proximityViewSlideOut,
			missionControllDashboardSlideIn,missionControllDashboardSlideOut;
	
	private AnalogPadInterface analogPadInterfaceDrive = new AnalogPadInterface() {
		@Override
		public void analogPadEvent(float xPosition, float yPosition) {
			int x = (int) xPosition;
			int y = (int) yPosition;
			calculateTrexDrive(x, y);
		}
		@Override
		public void analogPadKeyEvent(ANALOG_PAD event) {
			if (event.toString().equals(event.KEY_UP.toString())) {
				Toast.makeText(context, "Key Up - analogPadDrive",
						Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	private AnalogPadInterface analogPadInterfacePanTilt = new AnalogPadInterface() {

		@Override
		public void analogPadEvent(float xPosition, float yPosition) {
			int x = (int) helperMethods.map((long) xPosition, -100, 100, 0,	255);
			int y = (int) helperMethods.map((long) yPosition, -100, 100,0, 255);
			pan = y;
			tilt = x;
			textView.setText("tilt: " + x + " pan: " + y);
		}
		@Override
		public void analogPadKeyEvent(ANALOG_PAD event) {
			if (event.toString().equals(event.KEY_UP.toString())) {
				Toast.makeText(context, "Key Up - analogPadPanTilt",
						Toast.LENGTH_SHORT).show();
			}
		}
	};
	private OnSeekBarChangeListener platformPowerSeekBarListener = new OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			trexMaxPowerChanged = progress;
			platformPowerTV.setText(""+progress+" %");
			if(trexMaxPower != trexMaxPowerChanged){
				btnPlatformPowerConfirm.setEnabled(true);
			}
		}
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}		
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trex_controller);
		
		booleansInit();
		
		context = this;
		helperMethods = HelperMethods.instance;
		activity = this; 
		
		Bitmap bmpAnalogDisabled = BitmapFactory.decodeResource(getResources(),
				R.drawable.analog_nob_disabled);
		Bitmap bmpBase = BitmapFactory.decodeResource(getResources(),
				R.drawable.analog_nob_base);
		Bitmap bmpNobActive = BitmapFactory.decodeResource(getResources(),
				R.drawable.analog_nob_active);
		Bitmap bmpNobIdle = BitmapFactory.decodeResource(getResources(),
				R.drawable.analog_nob_idle);

		btnConnect 				= (ToggleButton) findViewById(R.id.toggleConnect);
		btnVideoFeed			= (ToggleButton) findViewById(R.id.toggLiveSteam); 
		btnRemoteControl		= (ToggleButton) findViewById(R.id.toggtoggleControll); 
		btnFeeds				= (ToggleButton) findViewById(R.id.toggtoggleDashBoard);
		toggleMissionControll	= (ToggleButton) findViewById(R.id.toggleMissionControll);
		toggleSearchControll	= (ToggleButton) findViewById(R.id.toggle_search_override_toggle);
		toggleProximityOverride	= (ToggleButton) findViewById(R.id.toggle_proximity_override);
		
		analogPadDrive 		= (AnalogPad) findViewById(R.id.analogPad);
		analogPadPanTilt 	= (AnalogPad) findViewById(R.id.analogPad2);
		

		textView 			= (TextView) findViewById(R.id.textView1);
		lightSensorTV 		= (TextView) findViewById(R.id.tv_sensor_light);
		distanceSensorTV 	= (TextView) findViewById(R.id.tv_sensor_distance);
		temperatureSensorTV	= (TextView) findViewById(R.id.tv_sensor_temperature);
		barPressureSensorTV	= (TextView) findViewById(R.id.tv_sensor_bar_pressure);
		altitudeSensorTV	= (TextView) findViewById(R.id.tv_sensor_bar_altitute);
		platformPowerTV		= (TextView) findViewById(R.id.tv_platform_power);
		
		toggleProximityOverride.setChecked(true);
		toggleSearchControll.setChecked(false);
		
		platformPowerOverdriveSeekBar = (SeekBar) findViewById(R.id.seekBar_platform_power);
		platformPowerOverdriveSeekBar.setOnSeekBarChangeListener(platformPowerSeekBarListener);
		
		btnPlatformPowerConfirm = (Button) findViewById(R.id.btn_platform_powerConfirm);
		btnPlatformPowerConfirm.setEnabled(false);
		
		trexMaxPower = INITIAL_TREX_POWER_LEVEL;
		platformPowerOverdriveSeekBar.setProgress(INITIAL_TREX_POWER_LEVEL);
		
		analogPadDrive.setAnalogPadInterface(analogPadInterfaceDrive);
		analogPadDrive.setBackgroundBitmap(bmpBase);
		analogPadDrive.setKnobActiveBitmap(bmpNobActive);
		analogPadDrive.setKnobIdleBitmap(bmpNobIdle);
		analogPadDrive.setDisabledBitmap(bmpAnalogDisabled);
		
		analogPadPanTilt.setAnalogPadInterface(analogPadInterfacePanTilt);
		analogPadPanTilt.setBackgroundBitmap(bmpBase);
		analogPadPanTilt.setKnobActiveBitmap(bmpNobActive);
		analogPadPanTilt.setKnobIdleBitmap(bmpNobIdle);
		analogPadPanTilt.setDisabledBitmap(bmpAnalogDisabled);
		
//		proximityView = (LinearLayout) findViewById(R.id.proximity_ui_layout);
				
		feedsDashboard 	= (LinearLayout) findViewById(R.id.feeds_dashboard);
		missionControllDashboard = (LinearLayout) findViewById(R.id.mission_controll_dashboard);
		
		feedsDashboard.setVisibility(View.INVISIBLE);
		missionControllDashboard.setVisibility(View.INVISIBLE);
		
		videoScreenBlinderLayout = (FrameLayout) findViewById(R.id.videoScreenBlinderLayout);
		videoScreenBlinderLayout.setVisibility(View.VISIBLE);
		proximityLayout = (FrameLayout) findViewById(R.id.proximity_layout);
		proximityLayout.setVisibility(View.INVISIBLE);
		
		feedsDashboardSlideIn 				= AnimationUtils.loadAnimation(context, R.anim.dashboard_slide_in);
		feedsDashboardSlideOut				= AnimationUtils.loadAnimation(context, R.anim.dashboard_slide_out);
		missionControllDashboardSlideIn 	= AnimationUtils.loadAnimation(context, R.anim.dashboard_slide_in);
		missionControllDashboardSlideOut	= AnimationUtils.loadAnimation(context, R.anim.dashboard_slide_out);
		videoScreenSlideIn					= AnimationUtils.loadAnimation(context, R.anim.video_screen_slide_in); 
		videoScreenSlideOut					= AnimationUtils.loadAnimation(context, R.anim.video_screen_slide_out);
		proximityViewSlideIn				= AnimationUtils.loadAnimation(context, R.anim.video_screen_slide_in); 
		proximityViewSlideOut				= AnimationUtils.loadAnimation(context, R.anim.video_screen_slide_out);
		
		//init the Proximity UI
		initProximityUI();
		
		btnVideoFeed.setEnabled(false); 
		btnRemoteControl.setEnabled(false); 
		btnFeeds.setEnabled(false);
		analogPadDrive.setEnabled(false);
		analogPadPanTilt.setEnabled(false);
		toggleMissionControll.setEnabled(false);
		toggleSearchControll.setEnabled(false);
		
		//demo and test override
		btnConnect.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				btnConnect.setEnabled(true);
				btnVideoFeed.setEnabled(true); 
				btnRemoteControl.setEnabled(true); 
				btnFeeds.setEnabled(true);
				toggleMissionControll.setEnabled(true);
				analogPadDrive.setEnabled(true);
				analogPadPanTilt.setEnabled(true);
				toggleSearchControll.setEnabled(true);
				return false;
			}
		});

		trexMaxPower = 30;	

		textView.setText("Ready");

		// textView2 = (TextView) findViewById(R.id.textView2);
		// textView2.setText("Ready");

		// Video Stream Stuff

		SharedPreferences preferences = getSharedPreferences("SAVED_VALUES",
				MODE_PRIVATE);
		width = preferences.getInt("width", width);
		height = preferences.getInt("height", height);
		ip_ad1 = preferences.getInt("ip_ad1", ip_ad1);
		ip_ad2 = preferences.getInt("ip_ad2", ip_ad2);
		ip_ad3 = preferences.getInt("ip_ad3", ip_ad3);
		ip_ad4 = preferences.getInt("ip_ad4", ip_ad4);
		ip_port = preferences.getInt("ip_port", ip_port);
		ip_command = preferences.getString("ip_command", ip_command);

		StringBuilder sb = new StringBuilder();
		String s_http = "http://";
		String s_dot = ".";
		String s_colon = ":";
		String s_slash = "/";
		sb.append(s_http);
		sb.append(ip_ad1);
		sb.append(s_dot);
		sb.append(ip_ad2);
		sb.append(s_dot);
		sb.append(ip_ad3);
		sb.append(s_dot);
		sb.append(ip_ad4);
		sb.append(s_colon);
		sb.append(ip_port);
		sb.append(s_slash);
		sb.append(ip_command);
		URL = new String(sb);

		mv = (MjpegView) findViewById(R.id.mv);
		if (mv != null) {
			mv.setResolution(width, height);
		}

		// setTitle(R.string.title_connecting);
//		suspending = true;
		new DoRead().execute(URL);
	}

	private void booleansInit() {
		connectionStatus 				= true; 
		remoteControlChangeRequested 	= false;
		maxPlatformPowerConfirmed 		= false;
		newMaxPlatformPowerRequested 	= false;
		whatchStream 					= false;
		keyUp 							= false;
		suspending 						= false;
		idVideoStreamEnabled 			= false;
		searchPaused					= true;
		proximityOverride				= false;
	}

	private void initProximityUI() {
		proximityB1	= (ImageView) findViewById(R.id.proximity_alert_b1);
		proximityB2	= (ImageView) findViewById(R.id.proximity_alert_b2);
		proximityB3	= (ImageView) findViewById(R.id.proximity_alert_b3);
		proximityB4	= (ImageView) findViewById(R.id.proximity_alert_b4);
		proximityF1	= (ImageView) findViewById(R.id.proximity_alert_f1);
		proximityF2	= (ImageView) findViewById(R.id.proximity_alert_f2);
		proximityF3	= (ImageView) findViewById(R.id.proximity_alert_f3);
		proximityF4	= (ImageView) findViewById(R.id.proximity_alert_f4);	
	}

	private void calculateTrexDrive(int x, int y) {

		int topMax = -100;
		int bottomMax = 100;
		int leftMax = -100;
		int rightMax = 100;
		int centerPos = 0;
		int m1MaxForward = 127;
		int m1Idle = 64;
		int m1MaxReverse = 1;
		int m2MaxForward = 255;
		int m2Idle = 192;
		int m2MaxReverse = 128;
		int m1 = m1Idle, m2 = m2Idle;

		// apply limit
		m1MaxForward = helperMethods.map(trexMaxPower, 0, 100, m1Idle, m1MaxForward);
		m1MaxReverse = helperMethods.map(trexMaxPower, 0, 100, m1Idle, m1MaxReverse);
		m2MaxForward = helperMethods.map(trexMaxPower, 0, 100, m2Idle, m2MaxForward);
		m2MaxReverse = helperMethods.map(trexMaxPower, 0, 100, m2Idle, m2MaxReverse);

		if (y < centerPos) {
			m1 = helperMethods.map(y, centerPos, topMax, m1Idle, m1MaxForward);
			m2 = helperMethods.map(y, centerPos, topMax, m2Idle, m2MaxForward);
		} else if (y > centerPos) {
			m1 = helperMethods.map(y, centerPos, bottomMax, m1Idle,
					m1MaxReverse);
			m2 = helperMethods.map(y, centerPos, bottomMax, m2Idle,
					m2MaxReverse);
		} else {
			m1 = m1Idle;
			m2 = m2Idle;
		}

		if (x < centerPos) {
			m1 = helperMethods.map(x, centerPos, leftMax, m1, m1MaxReverse);
			m2 = helperMethods.map(x, centerPos, leftMax, m2, m2MaxForward);
		} else if (x > centerPos) {
			m1 = helperMethods.map(x, centerPos, rightMax, m1, m1MaxForward);
			m2 = helperMethods.map(x, centerPos, rightMax, m2, m2MaxReverse);
		}

		textView.setText("M1: " + m1 + " M2: " + m2);

		motorA = m1;
		motorB = m2;

		trexCommand[0] = (byte) m1;
		trexCommand[1] = (byte) m2;
	}

	public void setPlatformMaxPower(View view){
		newMaxPlatformPowerRequested = true;
		trexMaxPower = trexMaxPowerChanged;
		btnPlatformPowerConfirm.setEnabled(false);
	}
	
	public void connect(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			if (connection == null) {
				btnConnect.setSelected(false);
				Toast.makeText(this, "Connecting ...", Toast.LENGTH_SHORT).show();
				new PlatformConnect().execute();
			}
		} else {
			if (connection != null) {
				try {
					connection.close();
				} catch (IOException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG)
							.show();
					e.printStackTrace();
				}
			} else {
				((ToggleButton) view).setChecked(false);
			}
		}
	}

	public void liveStreamView(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		idVideoStreamEnabled =on;

		if (on) {
			if (mv != null) {
				if (suspending ) {
					//mv.invalidate();
					new DoRead().execute(URL);
					suspending = false;
				}
			}
		} else {
			if (mv != null) {
				if (mv.isStreaming()) {
					mv.stopPlayback();
					suspending = true;
				}
			}
		}
		// Shutter control
		if(on){
			videoScreenBlinderLayout.startAnimation(videoScreenSlideOut);
			videoScreenBlinderLayout.setVisibility(View.INVISIBLE);	
		}else{
			videoScreenBlinderLayout.startAnimation(videoScreenSlideIn);
			videoScreenBlinderLayout.setVisibility(View.VISIBLE);			
		}
	}

	public void toggleControll(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			remoteControlled = true;
			remoteControlChangeRequested = true;
			btnRemoteControl.setEnabled(false);
			toggleSearchControll.setEnabled(false);
		} else {
			remoteControlled = false;
			remoteControlChangeRequested = true;
			btnRemoteControl.setEnabled(false);
			toggleSearchControll.setEnabled(true); 
		}
	}
	
	public void overrideProximityDetection(View view){
		boolean on = ((ToggleButton) view).isChecked();
		proximityOverride = on;
		if (DEBUG){Toast.makeText(this, "proximityOverride : "+proximityOverride, Toast.LENGTH_SHORT).show();}
	}
	
	public void searchInterruptControll(View view){
		boolean on = ((ToggleButton) view).isChecked();
		searchPaused = on;
		if (DEBUG){Toast.makeText(this, "searchPaused : "+searchPaused, Toast.LENGTH_SHORT).show();}
	}
	
	public void analogpadTest(View view){
		boolean on = ((ToggleButton) view).isChecked();
		analogPadPanTilt.setEnabled(on);		
	}
	
	public void showFeedsDashboard(View view){
		boolean on = ((ToggleButton) view).isChecked();
		if(on){
			if(toggleMissionControll.isChecked()){
				toggleMissionControll.setChecked(false);
				missionControllDashboard.startAnimation(missionControllDashboardSlideOut);
				missionControllDashboard.setVisibility(View.INVISIBLE);
			}
			feedsDashboard.startAnimation(feedsDashboardSlideIn);
			feedsDashboard.setVisibility(View.VISIBLE);		
		}else{
			feedsDashboard.startAnimation(feedsDashboardSlideOut);
			feedsDashboard.setVisibility(View.INVISIBLE);	
		}
		
	}
	
	public void showMissionControllDashboard(View view){
		boolean on = ((ToggleButton) view).isChecked();
		if(on){
			if(btnFeeds.isChecked()){
				btnFeeds.setChecked(false);
				feedsDashboard.startAnimation(feedsDashboardSlideOut);
				feedsDashboard.setVisibility(View.INVISIBLE);
			}
			missionControllDashboard.startAnimation(missionControllDashboardSlideIn);
			missionControllDashboard.setVisibility(View.VISIBLE);		
		}else{
			missionControllDashboard.startAnimation(missionControllDashboardSlideOut);
			missionControllDashboard.setVisibility(View.INVISIBLE);	
		}
	}
	
	public void showProximityView(View view){
		boolean on = ((ToggleButton) view).isChecked();
		if(on){
			proximityLayout.startAnimation(proximityViewSlideIn);
			proximityLayout.setVisibility(View.VISIBLE);		
		}else{
			proximityLayout.startAnimation(proximityViewSlideOut);
			proximityLayout.setVisibility(View.INVISIBLE);
		}
	}

	private void trexStop() {
	}

	public void onResume() {
		if (DEBUG)
			Log.d(TAG, "onResume()");
		super.onResume();
		if (mv != null) {
			if (suspending) {
				new DoRead().execute(URL);
				suspending = false;
			}
		}

	}

	public void onStart() {
		if (DEBUG)
			Log.d(TAG, "onStart()");
		super.onStart();
	}

	public void onPause() {
		if (DEBUG)
			Log.d(TAG, "onPause()");
		super.onPause();
		if (mv != null) {
			if (mv.isStreaming()) {
				mv.stopPlayback();
				suspending = true;
			}
		}
	}

	public void onStop() {
		if (connection != null) {
			try {
				connection.close();
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
		if (DEBUG)
			Log.d(TAG, "onStop()");
		super.onStop();
	}

	public void onDestroy() {
		if (DEBUG)
			Log.d(TAG, "onDestroy()");

		if (mv != null) {
			mv.freeCameraMemory();
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.trex_controller, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void processResponce(CompanionAppData responce) {
//	    boolean connectionOpen = responce.isConnected();
//	    boolean connected = responce.isConnected(); 
//	    boolean liveStreamEnabled = responce.isLiveStreamEnabled();
	    boolean remoteControllEnabled = responce.isRemoteControllEnabled(); 
	    boolean proximitySensorsEnabled = responce.isProximitySensorsEnabled();
	    boolean searchisGo = responce.isSearchEnabled();
//	    byte[] motorsCommand = responce.getMotorsCommand();
//	    int[] panTiltCommand = responce.getPanTiltCommand();
	    byte proximity = responce.getProximity();
	    int lightSensitivity = responce.getLightSensitivity();
	    int distance = responce.getDistance();
	    int motorPower = responce.getMotorPower();
//	    int messageType = responce.getMessageType();
	    float temperatureReading = responce.getTemperatureReading();
//	    float humidityReading = responce.getHumidityReading();
	    float atmosphericPressure = responce.getAtmosphericPressure();
	    double altitude = responce.getAltitude();
	    
	    updateProximityUI(getProximityArray(proximity));
	    
	    lightSensorTV.setText(""+lightSensitivity);
	    distanceSensorTV.setText(""+distance+" cm");
	    temperatureSensorTV.setText(""+temperatureReading+" C");
	    
	    DecimalFormat f = new DecimalFormat("##.00");
	    
	    barPressureSensorTV.setText(""+f.format(atmosphericPressure/100)+"Kpa");
	    altitudeSensorTV.setText(""+f.format(altitude)); 
	    	    
		if(remoteControlChangeRequested){
			btnRemoteControl.setEnabled(true);
			btnRemoteControl.setChecked(remoteControllEnabled);
			if (remoteControllEnabled) {
				analogPadDrive.setEnabled(true);
				analogPadPanTilt.setEnabled(true);
				toggleSearchControll.setEnabled(false);
			}else{
				analogPadDrive.setEnabled(false);
				analogPadPanTilt.setEnabled(false);
				btnRemoteControl.setChecked(false);
				toggleSearchControll.setEnabled(true);
			}
		}
		
		if((motorPower == trexMaxPower) && !maxPlatformPowerConfirmed){
			maxPlatformPowerConfirmed = true;
		}
		
		if(proximitySensorsEnabled && !toggleProximityOverride.isChecked()){
			toggleProximityOverride.setChecked(proximitySensorsEnabled);
		}
		
		if(searchisGo && !toggleSearchControll.isChecked()){
			toggleSearchControll.setChecked(proximitySensorsEnabled);
		}

	}
	
	
	
	private void updateProximityUI(boolean[] proximityArray) {
		if(proximityArray[0]/* && !proximityLaststate[4]*/){
			proximityF1.setImageResource(R.drawable.trex_proximity_f1_triggered);
		}else{
			proximityF1.setImageResource(R.drawable.trex_proximity_f1_idle);
		}
		
		if(proximityArray[1]/* && !proximityLaststate[5]*/){
			proximityF2.setImageResource(R.drawable.trex_proximity_f2_3_triggered);
		}else{
			proximityF2.setImageResource(R.drawable.trex_proximity_f2_3_idle);
		}
		
		if(proximityArray[2]/* && !proximityLaststate[6]*/){
			proximityF3.setImageResource(R.drawable.trex_proximity_f2_3_triggered);
		}else{
			proximityF3.setImageResource(R.drawable.trex_proximity_f2_3_idle);
		}
		
		if(proximityArray[3]/* && !proximityLaststate[7]*/){
			proximityF4.setImageResource(R.drawable.trex_proximity_f4_triggered);
		}else{
			proximityF4.setImageResource(R.drawable.trex_proximity_f4_idle);
		}
		
		if(proximityArray[4]/* && !proximityLaststate[3]*/){
			proximityB4.setImageResource(R.drawable.trex_proximity_b4_triggered);
		}else{
			proximityB4.setImageResource(R.drawable.trex_proximity_b4_idle);
		}
		
		if(proximityArray[5]/* && !proximityLaststate[2]*/){
			proximityB3.setImageResource(R.drawable.trex_proximity_b2_3_triggered);
		}else{
			proximityB3.setImageResource(R.drawable.trex_proximity_b2_3_idle);
		}
		
		if(proximityArray[6]/* && !proximityLaststate[1]*/){
			proximityB2.setImageResource(R.drawable.trex_proximity_b2_3_triggered);
		}else{
			proximityB2.setImageResource(R.drawable.trex_proximity_b2_3_idle);
		}
		
		if(proximityArray[7] /*&& !proximityLaststate[0]*/){
			proximityB1.setImageResource(R.drawable.trex_proximity_b1_triggered);
		}else{
			proximityB1.setImageResource(R.drawable.trex_proximity_b1_idle);
		}			
	}

	private boolean[] getProximityArray(byte b) {
		boolean[] array = new boolean[8];
		
		Byte myByte = new Byte(b);
		int proximityValue = myByte.intValue();
		
		while (proximityValue != 0) {

			if (proximityValue % 128 == 0) {
				array[0] = true;
				proximityValue -= 128;
			} else if (proximityValue % 64 == 0) {
				array[1] = true;
				proximityValue -= 64;
			} else if (proximityValue % 32 == 0) {
				array[2] = true;
				proximityValue -= 32;
			} else if (proximityValue % 16 == 0) {
				array[3] = true;
				proximityValue -= 16;
			} else if (proximityValue % 8 == 0) {
				array[4] = true;
				proximityValue -= 8;
			} else if (proximityValue % 4 == 0) {
				array[5] = true;
				proximityValue -= 4;
			} else if (proximityValue % 2 == 0) {
				array[6] = true;
				proximityValue -= 2;
			} else {
				array[7] = true;
				proximityValue -= 1;
			}
		}
		return array;
	}

	private CompanionAppData prepareRequest() {
		CompanionAppData request = new CompanionAppData();
		request.setMessageType(CompanionAppData.REQUEST);
		request.setRemoteControllEnabled(remoteControlled);		
		request.setMotorsCommand(new byte[] { (byte) motorA, (byte) motorB });		
		
		//workarround : pan def center :100 tilt def center: 90
		panTiltWorkArround();
		request.setPanTiltCommand(new int[] { pan, tilt });
		
		if(newMaxPlatformPowerRequested){
			request.setMotorPower(trexMaxPower);
			newMaxPlatformPowerRequested = false;
		}
			
		request.setSearchEnabled(!searchPaused);
		request.setProximitySensorsEnabled(!proximityOverride);

		// Log.d("DD","M1["+motorA+"] M2["+motorB+"]");		
		return request;
	}

	private void panTiltWorkArround() {

		int panMin = 0;
		int panmMax = 255;
		int panCenter = 100;
		int tiltMin = 0;
		int tiltMax = 255;
		int tiltCenter = 90;
		int tolerance = 10;
		
		
		//compasate top donw
		pan = helperMethods.map(pan, panMin, panmMax, panmMax,panMin);
		tilt = helperMethods.map(tilt, tiltMin, tiltMax, tiltMax,tiltMin);
		
		
		// centering		
		if(pan > panCenter-tolerance && pan < panCenter+tolerance ){
			pan = panCenter;
		}
		
		if(tilt > tiltCenter-tolerance && tilt < tiltCenter+tolerance ){
			tilt = tiltCenter;
		}		
	}

	class PlatformConnect extends AsyncTask<Void, Updateobject, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				publishProgress(new Updateobject(Updateobject.CONNECTING));
				Log.d("DD", "Connecting to the Platform");
				InetAddress inet = InetAddress.getByName("192.168.0.41");
				connection = new Socket(inet, 8000);
				Log.d("DD", "Connected to the Platform");

				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				publishProgress(new Updateobject(Updateobject.CONNECTING_SUCESS));
				
				int loopCount = 0;

				do {
					if(loopCount>OBJECT_OUTPUT_STREAM_S_LOOPS_TRESHOLD){
						out.reset();
						loopCount = 0;
					}
					try {

						CompanionAppData request = prepareRequest();
						out.writeObject(request);
						out.flush();

						final CompanionAppData responce = (CompanionAppData) in.readObject();
						connectionStatus = responce.isConnectionOpen();
						
						if(!connectionStatus){
							publishProgress(new Updateobject(Updateobject.CONNECTION_TERMINATED));
						}
						activity.runOnUiThread(new Runnable() {
						    public void run() {
						    	processResponce(responce);
						    }
						});						
						try {
							Thread.sleep(30);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} catch (ClassNotFoundException classNot) {
						Log.e("ERROR", "data received in unknown format: "
								+ classNot.getMessage());
					}
					loopCount++;
				} while (connectionStatus);
			} catch (UnknownHostException unknownHost) {
				System.err.println("You are trying to connect to an unknown host!");
				Log.e("ERROR", "You are trying to connect to an unknown host!");
				publishProgress(new Updateobject(Updateobject.CONNECTING_FALED));
			} catch (IOException ioException) {
				ioException.printStackTrace();
				Log.e("ERROR", "IOException: " + ioException.getMessage());
				publishProgress(new Updateobject(Updateobject.CONNECTING_FALED));
			} finally {
				// 4: Closing connection
				try {
					in.close();
					out.close();
					connection.close();
				} catch (IOException | NullPointerException exception) {
					exception.printStackTrace();
					Log.e("ERROR", "IOException: " + exception.getMessage());
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Updateobject... values) {
			if (values[0].getState()== Updateobject.CONNECTING) {
				btnConnect.setEnabled(false);
			} else if (values[0].getState()== Updateobject.CONNECTING_SUCESS) {
				btnConnect.setEnabled(true);
				btnVideoFeed.setEnabled(true); 
				btnRemoteControl.setEnabled(true); 
				btnFeeds.setEnabled(true);
				toggleMissionControll.setEnabled(true);
				toggleSearchControll.setEnabled(true);
				
				btnConnect.setChecked(true);
			}else if (values[0].getState()== Updateobject.CONNECTING_FALED) {
				btnConnect.setEnabled(true);
				btnConnect.setChecked(false);
			}else if (values[0].getState()== Updateobject.CONNECTION_TERMINATED) {
				btnConnect.setEnabled(true);
				btnVideoFeed.setEnabled(false); 
				btnRemoteControl.setEnabled(false); 
				btnFeeds.setEnabled(false);
				toggleMissionControll.setEnabled(false);
				toggleSearchControll.setEnabled(false);
				btnConnect.setChecked(false);
			}
 

			super.onProgressUpdate(values);
		}
	}

	public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
		protected MjpegInputStream doInBackground(String... url) {
			// TODO: if camera has authentication deal with it and don't just
			// not work
			HttpResponse res = null;
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpParams httpParams = httpclient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
			HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);
			if (DEBUG)
				Log.d(TAG, "1. Sending http request");
			try {
				res = httpclient.execute(new HttpGet(URI.create(url[0])));
				if (DEBUG)
					Log.d(TAG, "2. Request finished, status = "
							+ res.getStatusLine().getStatusCode());
				if (res.getStatusLine().getStatusCode() == 401) {
					// You must turn off camera User Access Control before this
					// will work
					return null;
				}
				return new MjpegInputStream(res.getEntity().getContent());
			} catch (ClientProtocolException e) {
				if (DEBUG) {
					e.printStackTrace();
					Log.d(TAG, "Request failed-ClientProtocolException", e);
				}
				// Error connecting to camera
			} catch (IOException e) {
				if (DEBUG) {
					e.printStackTrace();
					Log.d(TAG, "Request failed-IOException", e);
				}
				// Error connecting to camera
			}
			return null;
		}

		protected void onPostExecute(MjpegInputStream result) {
			mv.setSource(result);
			if (result != null) {
				result.setSkip(1);
				setTitle(R.string.app_name);
			} else {
				setTitle(R.string.title_disconnected);
			}
			mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
			mv.showFps(false);
		}
	}

	public class RestartApp extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... v) {
			TrexController.this.finish();
			return null;
		}

		protected void onPostExecute(Void v) {
			startActivity((new Intent(TrexController.this, TrexController.class)));
		}
	}
}
