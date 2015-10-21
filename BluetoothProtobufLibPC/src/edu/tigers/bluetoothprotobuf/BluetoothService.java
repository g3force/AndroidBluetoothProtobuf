package edu.tigers.bluetoothprotobuf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.apache.log4j.Logger;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class BluetoothService
{
	private static final Logger	log	= Logger.getLogger(BluetoothService.class.getName());
	
	
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 * @return
	 */
	public static List<RemoteDevice> discoverDevices()
	{
		final Object inquiryCompletedEvent = new Object();
		final List<RemoteDevice> devicesDiscovered = new LinkedList<RemoteDevice>();
		final DiscoveryListener listener = new DiscoveryListener()
		{
			
			@Override
			public void deviceDiscovered(final RemoteDevice btDevice, final DeviceClass cod)
			{
				log.info("Device " + btDevice.getBluetoothAddress() + " found");
				devicesDiscovered.add(btDevice);
				try
				{
					log.info("     name " + btDevice.getFriendlyName(false));
				} catch (final IOException cantGetDeviceName)
				{
				}
			}
			
			
			@Override
			public void inquiryCompleted(final int discType)
			{
				log.info("Device Inquiry completed!");
				synchronized (inquiryCompletedEvent)
				{
					inquiryCompletedEvent.notifyAll();
				}
			}
			
			
			@Override
			public void serviceSearchCompleted(final int transID, final int respCode)
			{
			}
			
			
			@Override
			public void servicesDiscovered(final int transID, final ServiceRecord[] servRecord)
			{
			}
		};
		
		synchronized (inquiryCompletedEvent)
		{
			boolean started = false;
			try
			{
				started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
			} catch (final BluetoothStateException e)
			{
				log.error("Could not start bluetooth inquery", e);
			}
			if (started)
			{
				log.info("wait for device inquiry to complete...");
				try
				{
					inquiryCompletedEvent.wait();
				} catch (final InterruptedException e)
				{
					log.error("", e);
				}
				log.info(devicesDiscovered.size() + " device(s) found");
			}
		}
		
		return devicesDiscovered;
	}
	
	
	/**
	 * Discover specified service on device and return connection url
	 * 
	 * @param strServiceUUID (without -)
	 * @param btDevice
	 * @return
	 */
	public static String discoverService(final String strServiceUUID, final RemoteDevice btDevice)
	{
		final UUID serviceUUID = new UUID(strServiceUUID, false);
		final Object serviceSearchCompletedEvent = new Object();
		final List<String> discoveredServices = new ArrayList<String>();
		
		final DiscoveryListener listener = new DiscoveryListener()
		{
			
			@Override
			public void deviceDiscovered(final RemoteDevice btDevice, final DeviceClass cod)
			{
			}
			
			
			@Override
			public void inquiryCompleted(final int discType)
			{
			}
			
			
			@Override
			public void servicesDiscovered(final int transID, final ServiceRecord[] servRecord)
			{
				for (ServiceRecord element : servRecord)
				{
					final String url = element.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
					if (url == null)
					{
						continue;
					}
					discoveredServices.add(url);
					final DataElement serviceName = element.getAttributeValue(0x0100);
					if (serviceName != null)
					{
						log.info("service " + serviceName.getValue() + " found " + url);
					} else
					{
						log.info("service found " + url);
					}
				}
			}
			
			
			@Override
			public void serviceSearchCompleted(final int transID, final int respCode)
			{
				log.info("service search completed!");
				synchronized (serviceSearchCompletedEvent)
				{
					serviceSearchCompletedEvent.notifyAll();
				}
			}
			
		};
		
		final UUID[] searchUuidSet = new UUID[] { serviceUUID };
		final int[] attrIDs = null; // new int[] { 0x0100 // Service name
		// };
		
		synchronized (serviceSearchCompletedEvent)
		{
			try
			{
				log.info("search services on " + btDevice.getBluetoothAddress() + " " + btDevice.getFriendlyName(false));
				LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet, btDevice, listener);
				serviceSearchCompletedEvent.wait();
			} catch (final BluetoothStateException e)
			{
				log.error("Error when searching for services", e);
			} catch (final InterruptedException e)
			{
				log.error("Interrupted.", e);
			} catch (final IOException e)
			{
				log.error("Something is wrong with btDevice", e);
			}
		}
		
		if (discoveredServices.isEmpty())
		{
			log.error("No service found");
			return null;
		} else if (discoveredServices.size() > 1)
		{
			log.warn("Found more than one service for device " + btDevice);
		}
		
		return discoveredServices.get(0);
	}
	
	
	/**
	 * @param serviceURL
	 * @return
	 * @throws IOException
	 */
	public static StreamConnection createStreamConnection(final String serviceURL) throws IOException
	{
		log.debug("Connecting to " + serviceURL);
		
		final StreamConnection con = (StreamConnection) Connector.open(serviceURL);
		return con;
	}
	
	
	/**
	 * Retrieve preknown devices. This will be faster than starting an inquiry process,
	 * but may not work depending on the system.
	 * The bluecove-bluez library is needed for Linux and the dbus.jar must be in class path
	 * in order to get devices, because communication via dbus is required.
	 * 
	 * @return
	 */
	public static List<RemoteDevice> retrieveKnownDevices()
	{
		try
		{
			final RemoteDevice[] devices = LocalDevice.getLocalDevice().getDiscoveryAgent()
					.retrieveDevices(DiscoveryAgent.PREKNOWN);
			
			for (final RemoteDevice dev : devices)
			{
				log.debug("Found cached device: " + dev.getFriendlyName(false));
			}
			
			return Arrays.asList(devices);
		} catch (final BluetoothStateException e)
		{
			log.error("Could not retrieve bt devices.", e);
		} catch (final IOException e)
		{
			log.error("Error retrieving bt devices", e);
		}
		
		return new ArrayList<RemoteDevice>();
	}
}
