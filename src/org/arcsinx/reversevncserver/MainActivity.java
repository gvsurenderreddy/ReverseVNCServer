package org.arcsinx.reversevncserver;

import java.io.OutputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InetAddress;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.File;

public class MainActivity extends Activity {
	static private String exe_name = "libreversevncserver.so";
	
	private boolean serverStarted = false;
	
	private SharedPreferences prefs;
	
	public String getIpAddress()
	{
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress())
					{
						String addr = inetAddress.getHostAddress();
						if (!addr.contains("::"))
							return inetAddress.getHostAddress();
					}
				}
			}
		}
		catch (SocketException e)
		{
		}
		return "127.0.0.1";
	}

	
	public static boolean checkRootPermissions()
	{
		try
		{
			File su = new File("/system/bin/su");
			if (su.exists() == true)
				return true;
			
			su = new File("/system/xbin/su");
			if (su.exists() == true)
				return true;
		}
		catch (Exception e)
		{
			return false;
		}

		return false;
	}
	
	private boolean checkIfRunning()
	{
		try
		{
			Process p = Runtime.getRuntime().exec("ps");
			p.waitFor();
			StringBuffer sb = new StringBuffer();
			InputStreamReader isr = new InputStreamReader(p.getInputStream());
			int ch;
			char [] buf = new char[1024];
			while((ch = isr.read(buf)) != -1)
				sb.append(buf, 0, ch);
			
			if (sb.indexOf(exe_name) != -1)
				return true;
		}
		catch (Exception e)
		{
			return false;
		}
		return false;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		EditText port_edit = (EditText)findViewById(R.id.port_edit);
		EditText reverse_hostport_edit = (EditText)findViewById(R.id.reversehostport_edit);
		CheckBox reconnect_checkbox = (CheckBox)findViewById(R.id.reconnect_checkbox);
		CheckBox viewonly_checkbox = (CheckBox)findViewById(R.id.viewonly_checkbox);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		reconnect_checkbox.setChecked(prefs.getBoolean("reconnect", false));
		viewonly_checkbox.setChecked(prefs.getBoolean("viewonly", false));
		port_edit.setText(prefs.getString("port", "5901"));
		reverse_hostport_edit.setText(prefs.getString("reverse_hostport",""));
		
		
		if (checkIfRunning())
		{
			Button startStopButton = (Button)findViewById(R.id.startstop_button);
			TextView vnc_connect_text = (TextView)findViewById(R.id.vncConnectStatic);
			TextView http_connect_text = (TextView)findViewById(R.id.httpConnectStatic);
			
			vnc_connect_text.setText(getIpAddress() + ":" + port_edit.getText().toString());
			startStopButton.setText("Stop");
			serverStarted = true;
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	static void writeCommand(OutputStream os, String command) throws Exception
	{
		os.write((command + "\n").getBytes("ASCII"));
	}
	
	public void starStopServer(View view) {
		Button startStopButton = (Button)findViewById(R.id.startstop_button);
		EditText port_edit = (EditText)findViewById(R.id.port_edit);
		EditText reverse_hostport_edit = (EditText)findViewById(R.id.reversehostport_edit);
		CheckBox reconnect_checkbox = (CheckBox)findViewById(R.id.reconnect_checkbox);
		CheckBox viewonly_checkbox = (CheckBox)findViewById(R.id.viewonly_checkbox);
		TextView vnc_connect_text = (TextView)findViewById(R.id.vncConnectStatic);
		TextView http_connect_text = (TextView)findViewById(R.id.httpConnectStatic);
		
		if (serverStarted)
		{
			String kill_cmd = "killall " + exe_name;
			
			if (checkRootPermissions())
			{
				try
				{
					String files_dir = getFilesDir().getAbsolutePath();
					Process sh = Runtime.getRuntime().exec("su", null, new File(files_dir));
					OutputStream os = sh.getOutputStream();
					writeCommand(os, kill_cmd);
				}
				catch (Exception e)
				{}
			}
			else
			{
				try
				{
					Runtime.getRuntime().exec(kill_cmd);
				}
				catch (Exception e)
				{}
			}
			
			vnc_connect_text.setText("");
			startStopButton.setText("Start");
			serverStarted = false;
		}
		else
		{
			String files_dir = getFilesDir().getAbsolutePath();
			String reversevncserver_cmd = getFilesDir().getParent() + "/lib/" + exe_name;
			String reversevncserver_chmod = "chmod a+rwx " + reversevncserver_cmd;
			boolean view_only = viewonly_checkbox.isChecked();
			
			String port = port_edit.getText().toString();
			if (!port.isEmpty())
				reversevncserver_cmd += " -p " + port;
			
		    String reverse_hostport = reverse_hostport_edit.getText().toString();
		    if (!reverse_hostport.isEmpty())
		    	reversevncserver_cmd += " -c " + reverse_hostport;
		    
		    if (reconnect_checkbox.isChecked())
		    	reversevncserver_cmd += " -r";
		    
		    if (view_only)
		    	reversevncserver_cmd += " -v";
			
			if (view_only || !checkRootPermissions())
			{
				try
				{
					Process sh = Runtime.getRuntime().exec(reversevncserver_chmod);
					sh.waitFor();
					Runtime.getRuntime().exec(reversevncserver_cmd);
					
					serverStarted = true;
				}
				catch (Exception e)
				{}
			}
			else
			{
				try
				{
					Process sh = Runtime.getRuntime().exec("su", null, new File(files_dir));
					OutputStream os = sh.getOutputStream();
					writeCommand(os, reversevncserver_chmod);
					writeCommand(os, reversevncserver_cmd);
					
					serverStarted = true;
				}
				catch (Exception e)
				{}
			}
			if (serverStarted)
			{
				vnc_connect_text.setText(getIpAddress() + ":" + port_edit.getText().toString());
				startStopButton.setText("Stop");	
			}
			else
				startStopButton.setText("Failed to start server");
			
			SharedPreferences.Editor e = prefs.edit();
			
			e.putBoolean("reconnect", reconnect_checkbox.isChecked());
			e.putBoolean("viewonly", viewonly_checkbox.isChecked());
			e.putString("port", port_edit.getText().toString());
			e.putString("reverse_hostport",reverse_hostport_edit.getText().toString());
			e.commit();
		}
	}

}
