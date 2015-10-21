package edu.tigers.bluetoothprotobuf;

import com.google.protobuf.Message;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public interface IMessageObserver
{
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * @param msgType
	 * @param message
	 */
	void onNewMessageArrived(IMessageType msgType, Message message);
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 */
	void onConnectionEstablished();
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 */
	void onConnectionLost();
}
