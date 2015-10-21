package edu.tigers.bluetoothprotobuf;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public interface IMessageGateway
{
	/**
	 * Send a message to all known connections
	 * 
	 * @param msgType
	 * @param data
	 */
	void sendMessage(final IMessageType msgType, final byte[] data);
	
	
	/**
	 * Is there an established connection?
	 * 
	 * @return
	 */
	boolean isConnected();
	
	
	/**
	 * Start this gateway
	 */
	void start();
	
	
	/**
	 * Stop this gateway
	 */
	void stop();
}
