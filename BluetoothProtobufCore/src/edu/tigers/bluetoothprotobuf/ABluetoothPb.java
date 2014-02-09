package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.google.protobuf.Message;
import com.google.protobuf.UninitializedMessageException;


public abstract class ABluetoothPb implements IMessageGateway
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	private static final Logger				log						= Logger.getLogger(ABluetoothPb.class.getName());
	
	/** Name for the SDP record when creating server socket */
	protected static final String				APP_NAME					= "BluetoothBtService";
	
	/** Unique UUID for this application */
	protected static final String				APP_UUID_STR_VAR1		= "04c6093b-0000-1000-8000-00805f9b34fb";
	protected static final String				APP_UUID_STR_VAR2		= "04c6093b00001000800000805f9b34fb";
	
	private static final int					LENGTH_HEADER_SIZE	= 4;
	
	private final List<IMessageObserver>	observers				= new CopyOnWriteArrayList<IMessageObserver>();
	private final MessageContainer			msgContainer;
	private boolean								active					= false;
	
	
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
			final byte[] message = new byte[data.length + 1 + LENGTH_HEADER_SIZE];
			System.arraycopy(data, 0, message, 1 + LENGTH_HEADER_SIZE, data.length);
			message[0] = Byte.valueOf(String.valueOf(msgType.getId()));
			final byte[] bytes = ByteBuffer.allocate(LENGTH_HEADER_SIZE).putInt(data.length).array();
			System.arraycopy(bytes, 0, message, 1, bytes.length);
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
			final byte[] headerBuffer = new byte[1 + LENGTH_HEADER_SIZE];
			
			
			log.debug("Start InputConnectionThread");
			
			while (active)
			{
				try
				{
					// Read from the InputStream
					final int headerBytes = in.read(headerBuffer);
					
					if (headerBytes <= 1)
					{
						
						log.info("No data available. Closing stream.");
						active = false;
					} else if (headerBytes == (1 + LENGTH_HEADER_SIZE))
					{
						final byte id = headerBuffer[0];
						final ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH_HEADER_SIZE).put(headerBuffer, 1,
								LENGTH_HEADER_SIZE);
						byteBuffer.position(0);
						final int length = byteBuffer.getInt();
						final IMessageType mt = getMsgContainer().getMessageTypeFromId(id);
						
						if (length > 8e6)
						{
							log.warn("Dropped message (id=" + id + ") with invalid length: " + length);
							clearBuffer(in);
							continue;
						}
						
						if (mt == null)
						{
							log.error("Unknown message id: " + id);
							clearBuffer(in);
							continue;
						}
						
						final byte[] buffer = new byte[length];
						int bytes = 0;
						while (bytes < length)
						{
							bytes += in.read(buffer, bytes, length - bytes);
						}
						
						if (bytes != length)
						{
							log.error("Invalid message length: " + (bytes) + " should be: " + length);
							continue;
						}
						final Message msg = mt.getProtoMsg().newBuilderForType().mergeFrom(buffer).build();
						notifyNewMessageArrived(mt, msg);
					} else
					{
						log.error("wrong number of header bytes: " + headerBytes);
						clearBuffer(in);
					}
				} catch (final IOException e)
				{
					log.info("disconnected from input connection");
					active = false;
				} catch (final UninitializedMessageException e)
				{
					log.error("Message is uninitialzed?!", e);
					clearBuffer(in);
				}
			}
			onConnectionLost(id);
		}
		
		
		private void clearBuffer(final InputStream in)
		{
			try
			{
				in.skip(1024);
			} catch (final IOException e)
			{
				log.error("Error while skipping data.", e);
			}
		}
	}
}
