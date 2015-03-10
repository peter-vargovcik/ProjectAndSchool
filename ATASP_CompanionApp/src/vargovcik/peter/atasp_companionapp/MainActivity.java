package vargovcik.peter.atasp_companionapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

import android.support.v7.app.ActionBarActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {
	
	OutputStream outStream;
	InputStream inStream;
	Socket socket;
	Context context;
	boolean remoteControlled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = this;
	}

	@Override
	protected void onStop() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
		super.onStop();
	}
	
	public void trexActivity(View view){
		Intent intent= new Intent(this, TrexController.class);
		startActivity(intent);
	}

	public void connect(View view) {
		if (socket == null) {
			new PlatformConnect().execute();
		}
	}

	public void start(View view) {
		// Toast.makeText(this, "start", Toast.LENGTH_SHORT).show();
		try {
			outStream.write(0x1);
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	public void stop(View view) {
		// Toast.makeText(this, "stop", Toast.LENGTH_SHORT).show();
		try {
			outStream.write(0x2);
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	public void sendString(View view) {
		// Toast.makeText(this, "stop", Toast.LENGTH_SHORT).show();
		PrintWriter p = new PrintWriter(outStream);
		p.print("STRINGPeter Vargovcik");
		p.flush();
	}

	public void toggleControll(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		remoteControlled = on;
		if (on) {
			Toast.makeText(this, "Toggleed", Toast.LENGTH_SHORT).show();
			try {
				outStream.write(0x3);
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		} else {
			try {
				outStream.write(0x4);
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
	}
	
	
	public void drive(View view) {
		if (remoteControlled) {
			String s="0554d4";
			 byte[] command = new BigInteger(s,16).toByteArray();
			try {
				outStream.write(command);
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
	}

	public void stopDriving(View view) {
		if (remoteControlled) {
			String s="0540c0";
			 byte[] command = new BigInteger(s,16).toByteArray();
			try {
				outStream.write(command);
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	class PlatformConnect extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				InetAddress inet = InetAddress.getByName("192.168.192.41");
				socket = new Socket(inet, 8000);
				outStream = socket.getOutputStream();
				inStream = socket.getInputStream();
			} catch (IOException e) {
				Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG)
						.show();
				e.printStackTrace();
			}
			return null;
		}
	}
}
