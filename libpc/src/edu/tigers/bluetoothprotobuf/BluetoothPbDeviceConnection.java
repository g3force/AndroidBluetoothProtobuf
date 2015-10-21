package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.StreamConnection;

import org.apache.log4j.Logger;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class BluetoothPbDeviceConnection
{
	private static final Logger		log	= Logger.getLogger(BluetoothPbDeviceConnection.class.getName());
	private final StreamConnection	streamConnection;
	private final OutputStream			outputStream;
	private final InputStream			inputStream;
	private final String					remoteDeviceId;
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * @param stream
	 * @param remoteDeviceId
	 */
	public BluetoothPbDeviceConnection(final StreamConnection stream, final String remoteDeviceId)
	{
		streamConnection = stream;
		this.remoteDeviceId = remoteDeviceId;
		
		InputStream in = null;
		try
		{
			in = streamConnection.openInputStream();
		} catch (final IOException e)
		{
			log.error("Could not open input stream");
		}
		
		OutputStream out = null;
		try
		{
			out = streamConnection.openOutputStream();
		} catch (final IOException e)
		{
			log.error("Could not open output stream");
		}
		
		inputStream = in;
		outputStream = out;
		
		log.debug("New connection for device " + remoteDeviceId);
	}
	
	
	/**
	 * @return the streamConnection
	 */
	public final StreamConnection getStreamConnection()
	{
		return streamConnection;
	}
	
	
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
			streamConnection.close();
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
