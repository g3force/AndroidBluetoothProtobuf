package edu.tigers.bluetoothprotobuf;

import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.protobuf.Message;

import edu.tigers.bluetoothprotobuf.SimpleMessageProtos.SimpleMessage;


public class BtpbTestClient
{
	// --------------------------------------------------------------------------
	// --- variables and constants ----------------------------------------------
	// --------------------------------------------------------------------------
	
	private static final Logger	log	= Logger.getLogger(BtpbTestClient.class.getName());
	
	
	// --------------------------------------------------------------------------
	// --- constructors ---------------------------------------------------------
	// --------------------------------------------------------------------------
	
	
	// --------------------------------------------------------------------------
	// --- methods --------------------------------------------------------------
	// --------------------------------------------------------------------------
	
	public static void main(final String[] args)
	{
		System.setProperty("log4j.configuration", "log4j.properties");
		PropertyConfigurator.configure(System.getProperties());
		
		final BluetoothPbLocal btpb = new BluetoothPbLocal(new MessageContainer(EMessage.values()));
		btpb.addObserver(new MessageReceiver());
		btpb.start();
		
		
		final Scanner scanner = new Scanner(System.in);
		while (true)
		{
			System.out.println("Command:");
			final String s = scanner.next();
			if (s.equals("q"))
			{
				break;
			}
			if (s.equals("c"))
			{
				btpb.connectToAllAvailableDevices();
				continue;
			}
			final SimpleMessage.Builder builder = (SimpleMessage.Builder) EMessage.SIMPLE_MESSAGE.getProtoMsg()
					.newBuilderForType();
			builder.setMessage(s);
			btpb.sendMessage(EMessage.SIMPLE_MESSAGE, builder.build().toByteArray());
		}
		scanner.close();
		System.exit(0);
	}
	
	private static class MessageReceiver implements IMessageObserver
	{
		
		
		@Override
		public void onNewMessageArrived(final IMessageType msgType, final Message message)
		{
			log.info("Message arrived: " + ((SimpleMessage) message).getMessage());
		}
		
		
		@Override
		public void onConnectionEstablished()
		{
			log.info("Connection established");
		}
		
		
		@Override
		public void onConnectionLost()
		{
			log.info("Connection lost");
		}
		
	}
	// --------------------------------------------------------------------------
	// --- getter/setter --------------------------------------------------------
	// --------------------------------------------------------------------------
}
