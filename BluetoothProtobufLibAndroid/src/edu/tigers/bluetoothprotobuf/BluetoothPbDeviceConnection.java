package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import android.bluetooth.BluetoothSocket;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class BluetoothPbDeviceConnection
{
	private static final Logger	log	= Logger.getLogger(BluetoothPbDeviceConnection.class.getName());
	private final BluetoothSocket	socket;
	private final OutputStream		outputStream;
	private final InputStream		inputStream;
	private final String				remoteDeviceId;
	
	
	/**
	 * @param socket
	 * @param remoteDeviceId
	 */
	public BluetoothPbDeviceConnection(final BluetoothSocket socket, final String remoteDeviceId)
	{
		this.socket = socket;
		this.remoteDeviceId = remoteDeviceId;
		
		InputStream in = null;
		try
		{
			in = socket.getInputStream();
		} catch (final IOException e)
		{
			log.error("Could not open input stream");
		}
		
		OutputStream out = null;
		try
		{
			out = socket.getOutputStream();
		} catch (final IOException e)
		{
			log.error("Could not open output stream");
		}
		
		inputStream = in;
		outputStream = out;
		
		log.debug("New connection for device " + remoteDeviceId);
	}
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	/**
	 * @return the outputStream
	 */
	public final OutputStream getOutputStream()
	{
		return outputStream;
	}
	
	
	/**
	 * @return the inputStream
	 */
	public final InputStream getInputStream()
	{
		return inputStream;
	}
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 */
	public void close()
	{
		log.debug("Closing all streams and sockets.");
		try
		{
			outputStream.close();
		} catch (final IOException e)
		{
			log.error("Could not close outputstream for devCon", e);
		}
		try
		{
			inputStream.close();
		} catch (final IOException e)
		{
			log.error("Could not close inputstream for devCon", e);
		}
		try
		{
			socket.close();
		} catch (final IOException e)
		{
			log.error("Could not close streamConnction for devCon", e);
		}
	}
	
	
	/**
	 * @return the remoteDeviceName
	 */
	public final String getRemoteDeviceId()
	{
		return remoteDeviceId;
	}
}
