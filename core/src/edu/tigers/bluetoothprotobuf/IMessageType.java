package edu.tigers.bluetoothprotobuf;

import com.google.protobuf.Message;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public interface IMessageType
{
	
	/**
	 * Get unique id of message type
	 * 
	 * @return
	 */
	int getId();
	
	
	/**
	 * Get readable name of message type
	 * 
	 * @return
	 */
	String name();
	
	
	/**
	 * Get protobuf message
	 * 
	 * @return
	 */
	Message getProtoMsg();
}
