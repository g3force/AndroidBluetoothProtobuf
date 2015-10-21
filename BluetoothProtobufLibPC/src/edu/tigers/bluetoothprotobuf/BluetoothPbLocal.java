package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.apache.log4j.Logger;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class BluetoothPbLocal extends ABluetoothPb
{
	private static final Logger							log					= Logger.getLogger(BluetoothPbLocal.class
																									.getName());
	
	private AcceptThread										acceptThread		= null;
	
	private final List<BluetoothPbDeviceConnection>	deviceConnections	= new LinkedList<BluetoothPbDeviceConnection>();
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * @param msgContainer
	 */
	public BluetoothPbLocal(final MessageContainer msgContainer)
	{
		super(msgContainer);
	}
	
	
	@Override
	public void start()
	{
		if (isActive())
		{
			return;
		}
		super.start();
		log.debug("Start btpb");
		acceptThread = new AcceptThread();
		new Thread(acceptThread, "Btpb_accept").start();
	}
	
	
	@Override
	public void stop()
	{
		super.stop();
		log.debug("Stop btpb");
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
	}
	
	
	private void onConnectionEstablished(final StreamConnection con, String deviceId)
	{
		log.debug("New connection");
		
		if (deviceId == null)
		{
			deviceId = "Unknown-" + Long.toHexString(Double.doubleToLongBits(Math.random()));
		}
		
		final BluetoothPbDeviceConnection devCon = new BluetoothPbDeviceConnection(con, deviceId);
		deviceConnections.add(devCon);
		
		openInputConnection(devCon.getInputStream(), deviceId);
		
		notifyConnectionEstablished();
	}
	
	
	@Override
	protected void onConnectionLost(final String id)
	{
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
	}
	
	
	@Override
	public void sendMessage(final IMessageType msgType, final byte[] data)
	{
		if (!deviceConnections.isEmpty())
		{
			log.trace("Sending message to " + deviceConnections.size() + " open connections.");
			
			for (final BluetoothPbDeviceConnection devCon : deviceConnections)
			{
				sendMessage(msgType, data, devCon);
			}
		}
	}
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * @param msgType
	 * @param data
	 * @param devCon
	 */
	public void sendMessage(final IMessageType msgType, final byte[] data, final BluetoothPbDeviceConnection devCon)
	{
		sendMessage(msgType, data, devCon.getOutputStream());
	}
	
	
	/**
	 * Establish a connection to the remote device.
	 * Use {@link BluetoothService#retrieveKnownDevices()} or {@link BluetoothService#discoverDevices()} to get a list of
	 * valid devices.
	 * 
	 * @param device
	 */
	public void connectToDevice(final RemoteDevice device)
	{
		String deviceName = null;
		try
		{
			deviceName = device.getFriendlyName(false);
			
			for (final BluetoothPbDeviceConnection devCon : deviceConnections)
			{
				if (deviceName.equals(devCon.getRemoteDeviceId()))
				{
					log.debug("Device already connected.");
					return;
				}
			}
		} catch (final IOException e1)
		{
			log.error("Could not get device name for device.", e1);
		}
		
		log.debug("Connecting to device " + deviceName);
		
		final String service = BluetoothService.discoverService(APP_UUID_STR_VAR2, device);
		if (service == null)
		{
			log.error("No service found for device " + deviceName);
			return;
		}
		try
		{
			final StreamConnection streamCon = BluetoothService.createStreamConnection(service);
			onConnectionEstablished(streamCon, device.getBluetoothAddress());
		} catch (final IOException e)
		{
			log.error("Could not create stream connection for service: " + service + " on device " + deviceName);
		}
	}
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 */
	public void connectToAllAvailableDevices()
	{
		List<RemoteDevice> devices = BluetoothService.retrieveKnownDevices();
		if (devices.isEmpty())
		{
			devices = BluetoothService.discoverDevices();
		}
		for (final RemoteDevice dev : devices)
		{
			connectToDevice(dev);
		}
	}
	
	
	@Override
	public boolean isConnected()
	{
		return !deviceConnections.isEmpty();
	}
	
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
	/**
	 * Wait for new bluetooth connections
	 * 
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 */
	private class AcceptThread implements Runnable
	{
		private boolean						active	= true;
		private StreamConnectionNotifier	notifier;
		
		
		/**
		 */
		public AcceptThread()
		{
		}
		
		
		@Override
		public void run()
		{
			// device discovery needs root privileges...
			// try
			// {
			// if (!(LocalDevice.getLocalDevice().getDiscoverable() == 1))
			// {
			// LocalDevice.getLocalDevice().setDiscoverable(DiscoveryAgent.GIAC);
			// }
			// } catch (final BluetoothStateException err)
			// {
			// log.error("Error creating bluetooth connection", err);
			// log.error("Try to enable device discovery via OS first or start application as root :/");
			// log.error("For Linux you need extra packages. For ubuntu: apt-get install bluez libbluetooth-dev");
			// }
			try
			{
				final UUID uuid = new UUID(APP_UUID_STR_VAR2, false);
				final String url = "btspp://localhost:" + uuid.toString() + ";name=" + APP_NAME;
				notifier = (StreamConnectionNotifier) Connector.open(url);
			} catch (final Throwable err)
			{
				log.error("Error creating bluetooth connection", err);
				return;
			}
			
			waitForConnection();
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
					
					// do not know how to determine device...
					onConnectionEstablished(connection, null);
				} catch (final IOException e)
				{
					log.debug("AcceptThread canceled");
					return;
				}
			}
		}
		
		
		public void cancel()
		{
			active = false;
			if (notifier != null)
			{
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
}
