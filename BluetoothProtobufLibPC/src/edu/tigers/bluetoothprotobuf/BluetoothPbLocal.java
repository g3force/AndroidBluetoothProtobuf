package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.apache.log4j.Logger;


public class BluetoothPbLocal extends ABluetoothPb
{
	
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	private static final Logger	log				= Logger.getLogger(BluetoothPbLocal.class.getName());
	
	private AcceptThread				acceptThread	= null;
	private StreamConnection		currentConnection;
	private OutputStream				currentOutputStream;
	private String						currentService;
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	public BluetoothPbLocal(final MessageContainer msgContainer)
	{
		super(msgContainer);
	}
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	@Override
	public void start()
	{
		log.debug("Start btpb");
		acceptThread = new AcceptThread();
		new Thread(acceptThread, "Btpb_accept").start();
	}
	
	
	@Override
	public void stop()
	{
		log.debug("Stop btpb");
		if (acceptThread != null)
		{
			acceptThread.cancel();
		}
		if (currentConnection != null)
		{
			try
			{
				currentConnection.close();
			} catch (final IOException e)
			{
				log.error("Could not close current connection");
			}
		}
		currentService = null;
	}
	
	
	private void onConnectionEstablished(final StreamConnection con)
	{
		log.debug("New connection");
		if (currentConnection != null)
		{
			try
			{
				currentConnection.close();
			} catch (final IOException e)
			{
				log.error("Could not close current connection");
			}
		}
		currentConnection = con;
		try
		{
			final InputStream in = currentConnection.openDataInputStream();
			openInputConnection(in);
		} catch (final IOException e)
		{
			log.error("Could not open input stream for connection");
		}
		notifyConnectionEstablished();
	}
	
	
	@Override
	public void sendMessage(final IMessageType msgType, final byte[] data)
	{
		log.trace("Sending message");
		
		if (currentConnection == null)
		{
			currentService = findOpponentConnection();
			if (currentService == null)
			{
				log.error("No current service available!");
				return;
			}
			
			try
			{
				currentConnection = BluetoothService.createStreamConnection(currentService);
			} catch (final IOException e1)
			{
				log.error("Could not create stream connection for service " + currentService);
				return;
			}
			
			try
			{
				currentOutputStream = currentConnection.openOutputStream();
			} catch (final IOException e)
			{
				log.error("Could not get output stream for service " + currentService);
				return;
			}
			
			try
			{
				final InputStream in = currentConnection.openInputStream();
				openInputConnection(in);
			} catch (final IOException e)
			{
				log.error("Could not get input stream for service " + currentService);
			}
		}
		
		sendMessage(msgType, data, currentOutputStream);
	}
	
	
	private String findOpponentConnection()
	{
		if (currentService != null)
		{
			return currentService;
		}
		
		final List<RemoteDevice> devices = BluetoothService.discoverDevices();
		
		if (devices.isEmpty())
		{
			log.error("No device found.");
			return "";
		} else if (devices.size() > 1)
		{
			log.warn("More than one device found!");
		}
		
		final String service = BluetoothService.discoverService(APP_UUID_STR_VAR2, devices.get(0));
		return service;
	}
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
	/**
	 * Wait for new bluetooth connections
	 * 
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * 
	 */
	private class AcceptThread implements Runnable
	{
		private boolean						active	= true;
		private StreamConnectionNotifier	notifier;
		
		
		/**
		 * @param consumer
		 */
		public AcceptThread()
		{
		}
		
		
		@Override
		public void run()
		{
			// retrieve the local Bluetooth device object
			// LocalDevice local = null;
			
			// setup the server to listen for connection
			try
			{
				// local = LocalDevice.getLocalDevice();
				// local.setDiscoverable(DiscoveryAgent.GIAC);
				
				final UUID uuid = new UUID("fa87c0d0afac11de8a390800200c9a66", false);
				final String url = "btspp://localhost:" + uuid.toString() + ";name=RemoteBluetooth";
				notifier = (StreamConnectionNotifier) Connector.open(url);
			} catch (final BluetoothStateException err)
			{
				log.error("Error creating bluetooth connection", err);
				log.error("Try to enable device discovery via OS first or start Sumatra as root :/");
				log.error("For Linux you need extra packages. For ubuntu: apt-get install bluez libbluetooth-dev");
				return;
			} catch (final IOException err)
			{
				log.error("Error creating bluetooth connection", err);
				return;
			}
			
			while (true)
			{
				waitForConnection();
			}
		}
		
		
		/** Waiting for connection from devices */
		private void waitForConnection()
		{
			StreamConnection connection = null;
			
			// waiting for connection
			while (active)
			{
				try
				{
					log.info("waiting for bluetooth connection...");
					connection = notifier.acceptAndOpen();
					log.info("Bluetooth device connected.");
					
					onConnectionEstablished(connection);
				} catch (final IOException e)
				{
					log.error("Error acceptAndOpen()", e);
					return;
				}
			}
		}
		
		
		public void cancel()
		{
			active = false;
			try
			{
				notifier.close();
			} catch (final IOException e)
			{
				log.error("Could not close notifier");
			}
			
		}
	}
}
