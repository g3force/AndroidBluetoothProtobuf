package edu.tigers.bluetoothprotobuf;

import com.google.protobuf.Message;

import edu.tigers.bluetoothprotobuf.SimpleMessageProtos.SimpleMessage;


public enum EMessage implements IMessageType
{
	/**  */
	SIMPLE_MESSAGE(1, SimpleMessage.getDefaultInstance());
	
	private final int			id;
	private final Message	protoMsg;
	
	
	private EMessage(final int id, final Message protoMsg)
	{
		this.id = id;
		this.protoMsg = protoMsg;
	}
	
	
	/**
	 * @return the id
	 */
	@Override
	public final int getId()
	{
		return id;
	}
	
	
	/**
	 * @return the protoMsg
	 */
	@Override
	public final Message getProtoMsg()
	{
		return protoMsg;
	}
}
