package edu.tigers.bluetoothprotobuf;

import org.apache.log4j.Level;

import de.mindpipe.android.logging.log4j.LogConfigurator;


/**
 * Call {@link #configure()} from your application's activity.
 */
public class ConfigureLog4J
{
	/**
	 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
	 */
	public static void configure()
	{
		final LogConfigurator logConfigurator = new LogConfigurator();
		
		// logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "myapp.log");
		logConfigurator.setUseFileAppender(false);
		logConfigurator.setRootLevel(Level.DEBUG);
		// Set log level of a specific logger
		logConfigurator.setLevel("org.apache", Level.ERROR);
		logConfigurator.configure();
	}
}
