package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;


public class BluetoothPbRemote extends ABluetoothPb
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	private static final Logger							log					= Logger.getLogger(BluetoothPbRemote.class
																									.getName());
	
	private final BluetoothAdapter						mAdapter;
	
	private AcceptThread										acceptThread		= null;
	
	private final BluetoothDevice							currentBtDevice;
	
	private final List<BluetoothPbDeviceConnection>	deviceConnections	= new LinkedList<BluetoothPbDeviceConnection>();
	
	static
	{
		ConfigureLog4J.configure();
	}
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	public BluetoothPbRemote(final MessageContainer msgContainer, final BluetoothDevice device)
	{
		super(msgContainer);
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		currentBtDevice = device;
	}
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	@Override
	public void start()
	{
		log.debug("BEGIN start btPb service");
		stop();
		super.start();
		acceptThread = new AcceptThread();
		new Thread(acceptThread, "Bt_Accept").start();
		log.debug("END start btPb service");
	}
	
	
	@Override
	public void stop()
	{
		super.stop();
		log.debug("BEGIN stop btPb service");
		if (acceptThread != null)
		{
			acceptThread.cancel();
			acceptThread = null;
		}
		for (final BluetoothPbDeviceConnection devCon : deviceConnections)
		{
			devCon.close();
		}
		deviceConnections.clear();
		log.debug("END stop btPb service");
	}
	
	
	private void onConnectionEstablished(final BluetoothSocket socket)
	{
		final BluetoothPbDeviceConnection devCon = new BluetoothPbDeviceConnection(socket, currentBtDevice.getAddress());
		deviceConnections.add(devCon);
		this.openInputConnection(devCon.getInputStream(), currentBtDevice.getAddress());
		log.debug("Connection established");
		notifyConnectionEstablished();
	}
	
	
	@Override
	protected void onConnectionLost(final String id)
	{
		log.debug("Connection lost to " + id);
		final List<BluetoothPbDeviceConnection> toBeRemoved = new LinkedList<BluetoothPbDeviceConnection>();
		for (final BluetoothPbDeviceConnection devCon : deviceConnections)
		{
			if ((id != null) && id.equals(devCon.getRemoteDeviceId()))
			{
				toBeRemoved.add(devCon);
				devCon.close();
			}
		}
		deviceConnections.removeAll(toBeRemoved);
		log.debug("Remaining connections: " + deviceConnections.size());
		if (deviceConnections.isEmpty())
		{
			log.debug("All connections lost");
			notifyConnectionLost();
		}
	}
	
	
	@Override
	public void sendMessage(final IMessageType msgType, final byte[] data)
	{
		refreshOutgoingConnection();
		if (deviceConnections.isEmpty())
		{
			log.error("Could not send message. No open connections.");
			return;
		}
		sendMessage(msgType, data, deviceConnections.get(0).getOutputStream());
	}
	
	
	private void refreshOutgoingConnection()
	{
		if (!deviceConnections.isEmpty())
		{
			return;
		}
		
		try
		{
			final BluetoothSocket socket = currentBtDevice.createRfcommSocketToServiceRecord(UUID
					.fromString(APP_UUID_STR_VAR1));
			socket.connect();
			onConnectionEstablished(socket);
		} catch (final IOException e)
		{
			log.error("Could not connect to " + currentBtDevice.getName());
		}
	}
	
	
	@Override
	public boolean isConnected()
	{
		return !deviceConnections.isEmpty();
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
				log.error("Could not listen on bt adapter", e);
			}
			mmServerSocket = tmp;
		}
		
		
		@Override
		public void run()
		{
			log.debug("BEGIN mAcceptThread");
			
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
					log.info("Canceled accept()");
				}
			}
			log.debug("END mAcceptThread");
			closeSocket();
		}
		
		
		public void cancel()
		{
			log.debug("cancel");
			closeSocket();
		}
		
		
		private void closeSocket()
		{
			try
			{
				mmServerSocket.close();
			} catch (final IOException e)
			{
				log.error("close() of server failed");
			}
		}
	}
	
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
}
