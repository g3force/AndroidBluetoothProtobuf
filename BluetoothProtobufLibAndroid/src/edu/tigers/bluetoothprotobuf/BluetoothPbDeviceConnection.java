package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import android.bluetooth.BluetoothSocket;


public class BluetoothPbDeviceConnection
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	private static final Logger	log	= Logger.getLogger(BluetoothPbDeviceConnection.class.getName());
	private final BluetoothSocket	socket;
	private final OutputStream		outputStream;
	private final InputStream		inputStream;
	private final String				remoteDeviceName;
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	/**
	 * 
	 * @param socket
	 * @param remoteDeviceName
	 */
	public BluetoothPbDeviceConnection(final BluetoothSocket socket, final String remoteDeviceName)
	{
		this.socket = socket;
		this.remoteDeviceName = remoteDeviceName;
		
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
		
		log.debug("New connection for device " + remoteDeviceName);
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
	public final String getRemoteDeviceName()
	{
		return remoteDeviceName;
	}
}
