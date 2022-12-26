package ca.wise.fgm.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class WISELogger {

	private static final Logger logger;
	private static final String lineBreak = System.lineSeparator();
	
	static {
		logger = Logger.getLogger("wise.builder");
		logger.setUseParentHandlers(false);
		MyFormatter formatter = new MyFormatter();
		ConsoleHandler console = new ConsoleHandler();
		console.setFormatter(formatter);
		logger.addHandler(console);
		logger.setLevel(Level.INFO);
	}
	
	public static void debug(String message) {
		log(Level.FINE, message, null);
	}
	
	public static void debug(String message, Throwable ex) {
		log(Level.FINE, message, ex);
	}
	
	public static void info(String message) {
		log(Level.INFO, message, null);
	}
	
	public static void info(String message, Throwable ex) {
		log(Level.INFO, message, ex);
	}
	
	public static void warn(String message) {
		log(Level.WARNING, message, null);
	}
	
	public static void warn(String message, Throwable ex) {
		log(Level.WARNING, message, ex);
	}
	
	public static void error(String message) {
		log(Level.SEVERE, message, null);
	}
	
	public static void error(String message, Throwable ex) {
		log(Level.SEVERE, message, ex);
	}
	
	public static void setLevel(Level level) {
		logger.setLevel(level);
	}
	
	private static void log(Level level, String message, Throwable tr) {
		synchronized (logger) {
			if (tr != null)
				logger.log(level, message, tr);
			else if (level == Level.INFO)
				logger.info(message);
			else if (level == Level.WARNING)
				logger.warning(message);
			else if (level == Level.SEVERE)
				logger.severe(message);
		}
	}
	
	private static class MyFormatter extends Formatter {
		
		private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

		@Override
		public String format(LogRecord record) {
			StringBuilder builder = new StringBuilder();
			builder.append(df.format(new Date(record.getMillis()))).append(" - ");
			String levelStr;
			if (record.getLevel() == Level.WARNING)
				levelStr = "WARN";
			else if (record.getLevel() == Level.INFO)
				levelStr = "INFO";
			else if (record.getLevel() == Level.SEVERE)
				levelStr = "ERROR";
			else
				levelStr = "DEBUG";
	        builder.append("[").append(levelStr).append("] - ");
	        builder.append(formatMessage(record));
	        builder.append("\n");
	        if (record.getThrown() != null) {
	        	try {
	        		StringWriter sw = new StringWriter();
	        		PrintWriter pw = new PrintWriter(sw);
	        		record.getThrown().printStackTrace(pw);
	        		pw.close();
	        		String err = sw.toString();
	        		err = "\t" + err.replace(lineBreak, lineBreak + "\t");
	        		builder.append(err);
	        	}
	        	catch (Exception e) { }
	        }
	        return builder.toString();
		}
	}
}
