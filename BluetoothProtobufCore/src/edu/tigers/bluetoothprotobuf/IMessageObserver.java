package edu.tigers.bluetoothprotobuf;

import com.google.protobuf.Message;


public interface IMessageObserver
{
	void onNewMessageArrived(IMessageType msgType, Message message);
	
	
	void onConnectionEstablished();
}
