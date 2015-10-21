package edu.tigers.bluetoothprotobuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.Message;

import edu.tigers.bluetoothprotobuf.SimpleMessageProtos.SimpleMessage;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class MainActivity extends Activity
{
	
	private static final int	REQUEST_ENABLE_BT	= 0;
	
	private BluetoothPbRemote	btPbService			= null;
	
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final Button btn = (Button) findViewById(R.id.button1);
		btn.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(final View v)
			{
				Log.d(MainActivity.class.getName(), "Sending message");
				final SimpleMessage.Builder builder = (SimpleMessage.Builder) EMessage.SIMPLE_MESSAGE.getProtoMsg()
						.newBuilderForType();
				builder.setMessage("This is a message");
				btPbService.sendMessage(EMessage.SIMPLE_MESSAGE, builder.build().toByteArray());
			}
		});
		
		final TextView txtView = (TextView) findViewById(R.id.editText1);
		txtView.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			
			@Override
			public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event)
			{
				Log.d(MainActivity.class.getName(), "Sending message: " + v.getText());
				final SimpleMessage.Builder builder = (SimpleMessage.Builder) EMessage.SIMPLE_MESSAGE.getProtoMsg()
						.newBuilderForType();
				builder.setMessage(v.getText().toString());
				btPbService.sendMessage(EMessage.SIMPLE_MESSAGE, builder.build().toByteArray());
				v.setText("");
				return true;
			}
		});
		
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
		{
			final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else
		{
			fillSpinner();
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity2, menu);
		return true;
	}
	
	
	@Override
	protected void onActivityResult(final int arg0, final int arg1, final Intent arg2)
	{
		super.onActivityResult(arg0, arg1, arg2);
		if (arg0 == REQUEST_ENABLE_BT)
		{
			if (arg1 == RESULT_OK)
			{
				Toast.makeText(getApplicationContext(), "BT enabled", Toast.LENGTH_LONG).show();
				fillSpinner();
			} else
			{
				Toast.makeText(getApplicationContext(), "BT not enabled", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}
	
	
	private void fillSpinner()
	{
		final Map<CharSequence, BluetoothDevice> devices = new HashMap<CharSequence, BluetoothDevice>();
		for (final BluetoothDevice dev : BluetoothAdapter.getDefaultAdapter().getBondedDevices())
		{
			devices.put(dev.getName(), dev);
		}
		final Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item, new ArrayList<CharSequence>(devices.keySet()));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			
			@Override
			public void onItemSelected(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3)
			{
				if (btPbService != null)
				{
					btPbService.stop();
				}
				btPbService = new BluetoothPbRemote(new MessageContainer(EMessage.values()), devices.get(spinner
						.getSelectedItem()));
				btPbService.addObserver(new MessageReceiver());
				btPbService.start();
			}
			
			
			@Override
			public void onNothingSelected(final AdapterView<?> arg0)
			{
			}
		});
	}
	
	
	@Override
	protected void onStart()
	{
		super.onStart();
	}
	
	
	@Override
	protected void onResume()
	{
		super.onStart();
		if ((btPbService != null))
		{
			btPbService.start();
		}
	}
	
	
	@Override
	protected void onPause()
	{
		super.onStop();
		if ((btPbService != null))
		{
			btPbService.stop();
		}
	}
	
	
	@Override
	protected void onStop()
	{
		super.onStop();
	}
	
	
	private class MessageReceiver implements IMessageObserver
	{
		private final Handler	mHandler	= new Handler();
		
		
		@Override
		public void onNewMessageArrived(final IMessageType msgType, final Message message)
		{
			mHandler.post(new Runnable()
			{
				
				@Override
				public void run()
				{
					final TextView txtView = (TextView) findViewById(R.id.textView1);
					final SimpleMessage msg = (SimpleMessage) message;
					txtView.append(msg.getMessage() + "\n");
					
					final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView1);
					scrollView.fullScroll(View.FOCUS_DOWN);
				}
			});
		}
		
		
		@Override
		public void onConnectionEstablished()
		{
			mHandler.post(new Runnable()
			{
				
				@Override
				public void run()
				{
					final TextView txtView = (TextView) findViewById(R.id.textView1);
					txtView.append("Connection established\n");
					
					final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView1);
					scrollView.fullScroll(View.FOCUS_DOWN);
				}
			});
		}
		
		
		@Override
		public void onConnectionLost()
		{
			mHandler.post(new Runnable()
			{
				
				@Override
				public void run()
				{
					final TextView txtView = (TextView) findViewById(R.id.textView1);
					txtView.append("Connection lost\n");
					
					final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView1);
					scrollView.fullScroll(View.FOCUS_DOWN);
				}
			});
		}
	}
}
