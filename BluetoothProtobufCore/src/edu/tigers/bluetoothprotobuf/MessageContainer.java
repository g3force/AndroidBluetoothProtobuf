package edu.tigers.bluetoothprotobuf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class MessageContainer
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	
	private final Map<Integer, IMessageType>	messages	= new HashMap<Integer, IMessageType>();
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	public MessageContainer(final Collection<IMessageType> msgs)
	{
		for (final IMessageType msg : msgs)
		{
			messages.put(msg.getId(), msg);
		}
	}
	
	
	public MessageContainer(final IMessageType[] msgs)
	{
		for (final IMessageType msg : msgs)
		{
			messages.put(msg.getId(), msg);
		}
	}
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	/**
	 * @param id
	 * @return
	 */
	public IMessageType getMessageTypeFromId(final int id)
	{
		return messages.get(id);
	}
	
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
}
