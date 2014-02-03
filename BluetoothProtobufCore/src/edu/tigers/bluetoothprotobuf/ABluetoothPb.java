package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.google.protobuf.Message;
import com.google.protobuf.UninitializedMessageException;


public abstract class ABluetoothPb implements IStartStopConnection, IMessageGateway
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	private static final Logger				log					= Logger.getLogger(ABluetoothPb.class.getName());
	
	/** Name for the SDP record when creating server socket */
	protected static final String				APP_NAME				= "BluetoothBtService";
	
	/** Unique UUID for this application */
	protected static final String				APP_UUID_STR_VAR1	= "04c6093b-0000-1000-8000-00805f9b34fb";
	protected static final String				APP_UUID_STR_VAR2	= "04c6093b00001000800000805f9b34fb";
	
	private final List<IMessageObserver>	observers			= new CopyOnWriteArrayList<IMessageObserver>();
	private final MessageContainer			msgContainer;
	private boolean								active				= false;
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	public ABluetoothPb(final MessageContainer msgContainer)
	{
		this.msgContainer = msgContainer;
	}
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	public void addObserver(final IMessageObserver observer)
	{
		synchronized (observers)
		{
			observers.add(observer);
		}
	}
	
	
	public void removeObserver(final IMessageObserver observer)
	{
		synchronized (observers)
		{
			observers.remove(observer);
		}
	}
	
	
	protected void notifyNewMessageArrived(final IMessageType msgType, final Message message)
	{
		synchronized (observers)
		{
			for (final IMessageObserver observer : observers)
			{
				observer.onNewMessageArrived(msgType, message);
			}
		}
	}
	
	
	protected void notifyConnectionEstablished()
	{
		synchronized (observers)
		{
			for (final IMessageObserver observer : observers)
			{
				observer.onConnectionEstablished();
			}
		}
	}
	
	
	protected void notifyConnectionLost()
	{
		synchronized (observers)
		{
			for (final IMessageObserver observer : observers)
			{
				observer.onConnectionLost();
			}
		}
	}
	
	
	@Override
	public void start()
	{
		active = true;
	}
	
	
	@Override
	public void stop()
	{
		
		active = false;
	}
	
	
	protected void sendMessage(final IMessageType msgType, final byte[] data, final OutputStream out)
	{
		try
		{
			final byte[] message = new byte[data.length + 2];
			System.arraycopy(data, 0, message, 2, data.length);
			message[0] = Byte.valueOf(String.valueOf(msgType.getId()));
			message[1] = Byte.valueOf(String.valueOf(data.length));
			out.write(message);
		} catch (final IOException err)
		{
			log.error("Error opening output stream for sending message " + msgType.name(), err);
		}
	}
	
	
	@Override
	public abstract void sendMessage(final IMessageType msgType, final byte[] data);
	
	
	protected abstract void onConnectionLost(String id);
	
	
	protected void openInputConnection(final InputStream in, final String id)
	{
		if (in != null)
		{
			new Thread(new InputConnectionThread(in, id), "InputConnection-" + id).start();
		}
	}
	
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
	
	/**
	 * @return the msgContainer
	 */
	public final MessageContainer getMsgContainer()
	{
		return msgContainer;
	}
	
	
	/**
	 * @return the active
	 */
	public final boolean isActive()
	{
		return active;
	}
	
	
	private class InputConnectionThread implements Runnable
	{
		private final InputStream	in;
		private boolean				active	= true;
		private final String			id;
		
		
		/**
		 * @param socket
		 */
		private InputConnectionThread(final InputStream in, final String id)
		{
			this.in = in;
			this.id = id;
		}
		
		
		@Override
		public void run()
		{
			final byte[] buffer = new byte[1024];
			int bytes;
			
			log.debug("Start InputConnectionThread");
			
			while (active)
			{
				try
				{
					// Read from the InputStream
					bytes = in.read(buffer);
					
					if (bytes > 1)
					{
						final byte id = buffer[0];
						final int length = buffer[1] & 0xFF;
						final IMessageType mt = getMsgContainer().getMessageTypeFromId(id);
						
						if (mt == null)
						{
							log.error("Unknown message id: " + id);
							continue;
						}
						
						if ((bytes - 2) != length)
						{
							log.error("Invalid message length: " + (bytes - 1) + " should be: " + length);
							continue;
						}
						final byte[] messageData = new byte[length];
						System.arraycopy(buffer, 2, messageData, 0, length);
						final Message msg = mt.getProtoMsg().newBuilderForType().mergeFrom(messageData).build();
						notifyNewMessageArrived(mt, msg);
					} else
					{
						log.info("No data available. Closing stream.");
						active = false;
					}
				} catch (final IOException e)
				{
					log.info("disconnected from input connection");
					active = false;
				} catch (final UninitializedMessageException e)
				{
					log.error("Message is uninitialzed?!", e);
				}
			}
			onConnectionLost(id);
		}
	}
}
