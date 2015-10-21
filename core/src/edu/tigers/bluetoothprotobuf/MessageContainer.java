package edu.tigers.bluetoothprotobuf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class MessageContainer
{
	private final Map<Integer, IMessageType>	messages	= new HashMap<Integer, IMessageType>();
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * @param msgs
	 */
	public MessageContainer(final Collection<IMessageType> msgs)
	{
		for (final IMessageType msg : msgs)
		{
			messages.put(msg.getId(), msg);
		}
	}
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * @param msgs
	 */
	public MessageContainer(final IMessageType[] msgs)
	{
		for (final IMessageType msg : msgs)
		{
			messages.put(msg.getId(), msg);
		}
	}
	
	
	/**
	 * @param id
	 * @return
	 */
	public IMessageType getMessageTypeFromId(final int id)
	{
		return messages.get(id);
	}
}
