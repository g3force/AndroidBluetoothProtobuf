package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


public class BluetoothPbRemote extends ABluetoothPb
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	private static final String		TAG					= BluetoothPbRemote.class.getName();
	/** debug */
	private static final boolean		D						= true;
	
	private final BluetoothAdapter	mAdapter;
	
	private AcceptThread					acceptThread		= null;
	private BluetoothSocket				currentBtSocket	= null;
	
	private BluetoothDevice				currentBtDevice	= null;
	
	static
	{
		ConfigureLog4J.configure();
	}
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	public BluetoothPbRemote(final MessageContainer msgContainer)
	{
		super(msgContainer);
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		
		final Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
		if (devices.isEmpty())
		{
			Log.e(TAG, "No devices to connect to...");
			return;
		}
		currentBtDevice = devices.iterator().next();
		if (devices.size() > 1)
		{
			Log.w(TAG, "More than device available! Choose " + currentBtDevice.getName());
		}
	}
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	@Override
	public void start()
	{
		if (D)
		{
			Log.d(TAG, "BEGIN start btPb service");
		}
		if (isActive())
		{
			stop();
		}
		acceptThread = new AcceptThread();
		new Thread(acceptThread, "Bt_Accept").start();
		super.start();
		if (D)
		{
			Log.d(TAG, "END start btPb service");
		}
	}
	
	
	@Override
	public void stop()
	{
		super.stop();
		if (D)
		{
			Log.d(TAG, "BEGIN stop btPb service");
		}
		if (acceptThread != null)
		{
			acceptThread.cancel();
		}
		if (currentBtSocket != null)
		{
			try
			{
				currentBtSocket.close();
			} catch (final IOException e)
			{
				Log.e(TAG, "current bt socket could not be closed", e);
			}
		}
		if (D)
		{
			Log.d(TAG, "END stop btPb service");
		}
	}
	
	
	private void onConnectionEstablished(final BluetoothSocket socket)
	{
		if (currentBtSocket != null)
		{
			Log.w(TAG, "New connection established, but old connection still exists. Closing old one.");
			try
			{
				currentBtSocket.close();
			} catch (final IOException e)
			{
				Log.e(TAG, "current bt socket could not be closed", e);
			}
		}
		currentBtSocket = socket;
		try
		{
			openInputConnection(socket.getInputStream());
		} catch (final IOException e)
		{
			Log.e(TAG, "Could not open inputstream for connection");
		}
		Log.d(TAG, "Connection established");
		notifyConnectionEstablished();
	}
	
	
	@Override
	public void sendMessage(final IMessageType msgType, final byte[] data)
	{
		refreshOutgoingConnection();
		if (currentBtSocket == null)
		{
			Log.e(TAG, "Could not send message. No open socket.");
			return;
		}
		try
		{
			final OutputStream out = currentBtSocket.getOutputStream();
			sendMessage(msgType, data, out);
			final InputStream in = currentBtSocket.getInputStream();
			openInputConnection(in);
		} catch (final IOException e)
		{
			Log.e(TAG, "Could not open outputstream for bt socket");
		}
	}
	
	
	private void refreshOutgoingConnection()
	{
		if ((currentBtSocket != null) && currentBtSocket.isConnected())
		{
			return;
		}
		
		try
		{
			final BluetoothSocket socket = currentBtDevice.createRfcommSocketToServiceRecord(UUID
					.fromString(APP_UUID_STR_VAR1));
			socket.connect();
			currentBtSocket = socket;
		} catch (final IOException e)
		{
			Log.e(TAG, "Could not connect to " + currentBtDevice.getName(), e);
		}
	}
	
	
	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted
	 * (or until cancelled).
	 */
	private class AcceptThread implements Runnable
	{
		// The local server socket
		private final BluetoothServerSocket	mmServerSocket;
		
		
		/**
		 */
		public AcceptThread()
		{
			BluetoothServerSocket tmp = null;
			
			// Create a new listening server socket
			try
			{
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, UUID.fromString(APP_UUID_STR_VAR1));
			} catch (final IOException e)
			{
				Log.e(TAG, "Could not listen on bt adapter", e);
			}
			mmServerSocket = tmp;
		}
		
		
		@Override
		public void run()
		{
			if (D)
			{
				Log.d(TAG, "BEGIN mAcceptThread");
			}
			
			while (isActive())
			{
				try
				{
					// This is a blocking call and will only return on a
					// successful connection or an exception
					final BluetoothSocket socket = mmServerSocket.accept();
					onConnectionEstablished(socket);
				} catch (final IOException e)
				{
					Log.e(TAG, "Canceled accept()");
				}
			}
			if (D)
			{
				Log.d(TAG, "END mAcceptThread");
			}
			closeSocket();
		}
		
		
		public void cancel()
		{
			if (D)
			{
				Log.d(TAG, "cancel");
			}
			closeSocket();
		}
		
		
		private void closeSocket()
		{
			try
			{
				mmServerSocket.close();
			} catch (final IOException e)
			{
				Log.e(TAG, "close() of server failed");
			}
		}
	}
	
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	public Set<BluetoothDevice> getAvailableBluetoothDevices()
	{
		final Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
		return devices;
	}
	
	
	/**
	 * @return the currentBtDevice
	 */
	public final BluetoothDevice getCurrentBtDevice()
	{
		return currentBtDevice;
	}
	
	
	/**
	 * @param currentBtDevice the currentBtDevice to set
	 */
	public final void setCurrentBtDevice(final BluetoothDevice currentBtDevice)
	{
		stop();
		this.currentBtDevice = currentBtDevice;
		start();
	}
	
}
