package ca.wise.main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Strings;
import com.google.protobuf.util.JsonFormat;

import ca.wise.fgm.output.DataCombiner;
import ca.wise.fgm.output.IOutputOptions;
import ca.wise.fgm.output.OutputType;
import ca.wise.fgm.output.PBCombiner;
import ca.wise.config.proto.ServerConfiguration;
import lombok.Getter;

/**
 * A parser for command line arguments. Holds a singleton
 * that can be used for querying which arguments were
 * specified.
 * 
 * @author Travis Redpath
 */
public final class CommandLine {

	/**
	 * Should MQTT be used to communicate with W.I.S.E. Manager.
	 */
	@Getter private boolean useMqtt = false;
	/**
	 * Has the user requested the application version.
	 */
	@Getter private boolean getVersion;
	/**
	 * Has the user requested the help menu be displayed.
	 */
	@Getter private boolean showHelp;
	/**
	 * The IP address of W.I.S.E. Manager to connect to (if using sockets).
	 */
	@Getter private String managerIp;
	/**
	 * The port number to use when connecting to W.I.S.E. Manager (if using sockets).
	 */
	@Getter private Integer managerPort;
	/**
	 * The directory that W.I.S.E. jobs are to be stored in. The defaults XML file
	 * will also be located here.
	 */
	@Getter private String jobDirectory;
	/**
	 * The local port to communicate with the PHP or JavaScript API with.
	 */
	@Getter private Integer localPort;
	/**
	 * The requested log level.
	 */
	@Getter private Level logLevel = Level.INFO;
	/**
	 * Should the restart command be output for testing.
	 */
	@Getter private boolean testRestart = false;
	/**
	 * The default job file output type.
	 */
	@Getter private OutputType outputType = OutputType.XML;
	/**
	 * The location to create the MQTT persistence directory.
	 */
	@Getter private String mqttDirectory = null;
	/**
	 * If client applications should be allowed to set the MQTT settings.
	 */
	@Getter private boolean userMqttSettings = false;
	/**
	 * Wrap all input files into the protobuf file so that only a single
	 * input file is needed.
	 */
	@Getter private boolean useSingleFile = false;
	/**
	 * The URL for the MQTT client to connect to.
	 */
	@Getter URI mqttUrl = null;
	/**
	 * Should the license banner be suppressed.
	 */
	@Getter boolean hideBanner = false;
	/**
	 * Should W.I.S.E. Builder exit after showing the license banner.
	 */
	@Getter boolean showBanner = false;
	/**
	 * The username to use to connect to the MQTT broker.
	 */
	@Getter String mqttUsername = null;
	/**
	 * The password to use to connect to the MQTT broker.
	 */
	@Getter String mqttPassword = null;
	/**
	 * The base topic name to use when communicating using MQTT.
	 */
	@Getter String mqttTopic = null;
	/**
	 * The IP address that the builder is running on.
	 */
	@Getter String myIpAddress = null;
	
	private static CommandLine instance;
	private static StringBuilder collectedErrors = new StringBuilder();
	
	/**
	 * Get the output format options.
	 */
	public IOutputOptions getOutputOptions() {
		if (outputType == OutputType.XML) {
			DataCombiner.OutputOptions options = new DataCombiner.OutputOptions();
			options.singleFile = useSingleFile;
			return options;
		}
		else {
			PBCombiner.OutputOptions options = new PBCombiner.OutputOptions();
			options.prettyPrint = outputType == OutputType.PROTO || outputType == OutputType.PROTO_V2;
			options.useBinary = outputType == OutputType.BINARY || outputType == OutputType.BINARY_V2;
			options.allowUserConnectionSettings = userMqttSettings;
			options.singleFile = useSingleFile;
			options.version = (outputType == OutputType.PROTO || outputType == OutputType.BINARY || outputType == OutputType.MINIMAL_PROTO) ? 1 : 2;
			return options;
		}
	}
	
	/**
	 * Get the singleton instance that contains the parsed command line arguments.
	 * Will be {@code null} if not initialized or if there was an error parsing
	 * the command line arguments.
	 */
	public static CommandLine get() { return instance; }

	private static final String IPADDRESS_PATTERN =
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	private static Pattern pattern = null;

	public static boolean isIpAddressValid(final String address) {
		if (pattern == null)
			pattern = Pattern.compile(IPADDRESS_PATTERN);
		Matcher matcher = pattern.matcher(address);
		return matcher.matches();
	}
	
	private CommandLine() { }
	
	private static Options getOptions() {
		Options options = new Options();
		options.addOption(new Option("v", "version", false, "Print the application version then stop."));
		options.addOption(new Option("h", "help", false, "Print this message."));
		options.addOption(new Option("m", "mqtt", false, "If specified MQTT will be used to communicate with the W.I.S.E. Manager instead of sockets."));
		options.addOption(new Option("b", "mqttaddress", true, "A URL for the MQTT client to use to connect to the broker. Must be a valid URL. If absent the MQTT information from the config.json file in the job directory will be used."));
		options.addOption(new Option("a", "manager", true, "The address of the W.I.S.E. Manager instance. Will be ignored if MQTT is used."));
		Option op = new Option("p", "port", true, "The port that W.I.S.E. Manager is listening on. Will be ignored if MQTT is used.");
		op.setType(Integer.class);
		options.addOption(op);
		options.addOption(new Option("j", "jobs", true, "The directory that W.I.S.E. jobs will be stored in."));
		op = new Option("l", "local", true, "The port to use to communicate with the local PHP or JavaScript API.");
		op.setType(Integer.class);
		options.addOption(op);
		options.addOption(new Option("w", "warn", true, "The logging output level. Must be one of warn, severe, info, off, or all."));
		options.addOption(new Option("r", "restart", false, "Test the restart command. Just prints the parsed command to the command line."));
		options.addOption(new Option("o", "output", true, "The default job output type. Possible values are xml (XML output), json (Protobuf JSON output), json_mini (Minified Protobuf JSON output), and binary_v2 (Protobuf binary schema v2 output), json_v2 (Protobuf JSON schema v2 output), json_mini_v2 (Minified Protobuf JSON schema v2 output), and binary (Protobuf binary output). If not specified or invalid XML will be used."));
		options.addOption(new Option("e", "persistence", true, "The location to create the MQTT persistence directory. If absent the directory will be created beside the jar file. Use :memory to use in memory persistence instead (no persistence across restart)."));
		options.addOption(new Option("u", "usersettings", false, "Allow client applications to update the MQTT connection settings. Disabled by default."));
		options.addOption(new Option("s", "single", false, "Wrap all inputs files into a single protobuf file. May require additional JVM memory. If single file and MQTT is enabled Builder will attempt to send files to remote Manager instances and not write directly to the filesystem."));
		options.addOption(new Option("i", "ip", true, "The IP address that this W.I.S.E. Builder instance is running at."));
		options.addOption(new Option(null, "no-banner", false, "Disable the license text when starting W.I.S.E. Builder"));
		options.addOption(new Option(null, "only-banner", false, "Display the list of third party licenses used by W.I.S.E. Builder then exit."));
		
		return options;
	}
	
	/**
	 * Reload the command line arguments.
	 * @param args The command line arguments.
	 * @return True if the arguments were parsed, false if an error occurred.
	 */
	public static boolean reset(String[] args) throws AlreadyInitializedException {
		instance = null;
		return initialize(args);
	}
	
	/**
	 * Parse the command line arguments.
	 * @param args The command line arguments.
	 * @return True if the arguments were parsed, false if an error occurred.
	 * @throws AlreadyInitializedException If this method has already been called.
	 */
	public static boolean initialize(String[] args) throws AlreadyInitializedException {
		if (instance != null)
			throw new AlreadyInitializedException();
		
		boolean retval = true;
		
		Options options = getOptions();
		
		CommandLineParser parser = new DefaultParser();
		org.apache.commons.cli.CommandLine line = null;
		try {
			line = parser.parse(options, args);
		}
		catch (ParseException e) {
			e.printStackTrace();
			retval = false;
		}
		
		//lookup the command line options
		if (line != null) {
			instance = new CommandLine();
			if (line.hasOption('j')) {
				instance.jobDirectory = line.getOptionValue('j');
				File f = new File(instance.jobDirectory);
				if (!f.isDirectory()) {
					if (!f.exists()) {
						if (!f.mkdirs()) {
							collectedErrors.append("Unable to create job directory");
							collectedErrors.append(System.lineSeparator());
							retval = false;
						}
					}
					else {
						collectedErrors.append("The job directory is not a directory");
						collectedErrors.append(System.lineSeparator());
						retval = false;
					}
				}
				if (instance.jobDirectory.endsWith("\\"))
					instance.jobDirectory = instance.jobDirectory.substring(0, instance.jobDirectory.length() - 1);
				if (!instance.jobDirectory.endsWith("/"))
					instance.jobDirectory = instance.jobDirectory + "/";
				
				try {
					Path path = Paths.get(instance.jobDirectory).resolve("config.json");
					if (Files.exists(path)) {
						try (FileReader reader = new FileReader(path.toFile())) {
							ServerConfiguration.Builder builder = ServerConfiguration.newBuilder();
							JsonFormat.parser()
									.ignoringUnknownFields()
									.merge(reader, builder);
							ServerConfiguration config = builder.build();
							
							if (config != null) {
								if (config.hasBuilder()) {
									instance.localPort = config.getBuilder().getPort();
									switch (config.getBuilder().getFormat()) {
									case BINARY:
										instance.outputType = OutputType.BINARY;
										break;
									case JSON:
										instance.outputType = OutputType.PROTO;
										break;
									case JSON_MINIMAL:
										instance.outputType = OutputType.MINIMAL_PROTO;
										break;
									case BINARY_V2:
										instance.outputType = OutputType.BINARY_V2;
										break;
									case JSON_V2:
										instance.outputType = OutputType.PROTO_V2;
										break;
									case JSON_MINIMAL_V2:
										instance.outputType = OutputType.MINIMAL_PROTO_V2;
										break;
									case XML:
										instance.outputType = OutputType.XML;
										break;
									default:
										break;
									}
								}
								if (config.hasSocket()) {
									instance.managerIp = config.getSocket().getAddress();
									instance.managerPort = config.getSocket().getPort();
								}
								else if (config.hasMqtt()) {
									instance.useMqtt = true;
									instance.mqttUsername = config.getMqtt().getUsername();
									instance.mqttPassword = config.getMqtt().getPassword();
									instance.mqttTopic = config.getMqtt().getTopic();
									String url = config.getMqtt().getHostname();
									if (!Strings.isNullOrEmpty(url)) {
										if (config.getMqtt().getPort() > 0)
											url += ":" + config.getMqtt().getPort();
										if (url.indexOf("://") < 0)
											url = "tcp://" + url;
										instance.mqttUrl = URI.create(url);
									}
								}
							}
						}
					}
				}
				catch (IOException e) {
				}
			}
			instance.getVersion = line.hasOption('v');
			instance.showHelp = line.hasOption('h');
			if (line.hasOption('m'))
				instance.useMqtt = true;
			//if the user requested MQTT from the command line make sure socket settings aren't specified
			if (instance.useMqtt) {
				instance.managerIp = null;
				instance.managerPort = null;
			}
			instance.useSingleFile = line.hasOption('s');
			if (line.hasOption('a')) {
				instance.managerIp = line.getOptionValue('a');
				if (instance.managerIp != null) {
					if (!isIpAddressValid(instance.managerIp)) {
						collectedErrors.append("Invalid W.I.S.E. Manager IP address");
						collectedErrors.append(System.lineSeparator());
						retval = false;
					}
					else if (instance.managerIp.indexOf(':') > 0) {
						String[] split = instance.managerIp.split(":");
						if (split.length == 2) {
							instance.managerIp = split[0];
							try {
								instance.managerPort = Integer.parseInt(split[1]);
							}
							catch (NumberFormatException e) {
								collectedErrors.append("Invalid W.I.S.E. Manager port");
								collectedErrors.append(System.lineSeparator());
								retval = false;
							}
						}
					}
					else if (instance.managerPort == null)
						instance.managerPort = 32478;
				}
			}
			if (line.hasOption('p')) {
				String val = line.getOptionValue('p', "32478");
				try {
					instance.managerPort = Integer.parseInt(val);
				}
				catch (NumberFormatException e) {
					collectedErrors.append("Invalid W.I.S.E. Manager port");
					collectedErrors.append(System.lineSeparator());
					retval = false;
				}
			}
			else if (instance.managerPort == null)
				instance.managerPort = 32478;
			if (line.hasOption('l')) {
				String val = line.getOptionValue('l', "32477");
				try {
					instance.localPort = Integer.parseInt(val);
				}
				catch (NumberFormatException e) {
					collectedErrors.append("Invalid local port");
					collectedErrors.append(System.lineSeparator());
					retval = false;
				}
			}
			else if (instance.localPort == null)
				instance.localPort = 32477;
			if (line.hasOption('w')) {
				String val = line.getOptionValue('w');
				if (val != null) {
					val = val.toLowerCase();
					if (val.equals("warn"))
						instance.logLevel = Level.WARNING;
					else if (val.equals("severe"))
						instance.logLevel = Level.SEVERE;
					else if (val.equals("info"))
						instance.logLevel = Level.INFO;
					else if (val.equals("off"))
						instance.logLevel = Level.OFF;
					else if (val.equals("all"))
						instance.logLevel = Level.ALL;
				}
			}
			if (line.hasOption('r')) {
				instance.testRestart = true;
			}
			if (line.hasOption('o')) {
				String val = line.getOptionValue('o');
				if (val.equalsIgnoreCase("xml"))
					instance.outputType = OutputType.XML;
				else if (val.equalsIgnoreCase("json"))
					instance.outputType = OutputType.PROTO;
				else if (val.equalsIgnoreCase("json_mini"))
					instance.outputType = OutputType.MINIMAL_PROTO;
				else if (val.equalsIgnoreCase("binary"))
					instance.outputType = OutputType.BINARY;
				else if (val.equalsIgnoreCase("json_v2"))
					instance.outputType = OutputType.PROTO_V2;
				else if (val.equalsIgnoreCase("json_mini_v2"))
					instance.outputType = OutputType.MINIMAL_PROTO_V2;
				else if (val.equalsIgnoreCase("binary_v2"))
					instance.outputType = OutputType.BINARY_V2;
			}
			if (line.hasOption('e')) {
				instance.mqttDirectory = line.getOptionValue('e');
				if (instance.mqttDirectory != null && !instance.mqttDirectory.equalsIgnoreCase(":memory")) {
					File f = new File(instance.mqttDirectory);
					if (!f.isDirectory()) {
						if (!f.exists()) {
							if (!f.mkdirs()) {
								collectedErrors.append("Unable to create MQTT directory");
								collectedErrors.append(System.lineSeparator());
								retval = false;
							}
						}
						else {
							collectedErrors.append("The MQTT directory is not a directory");
							collectedErrors.append(System.lineSeparator());
							retval = false;
						}
					}
				}
			}
			if (line.hasOption('u')) {
				instance.userMqttSettings = true;
			}
			if (line.hasOption('b')) {
				try {
					instance.mqttUrl = URI.create(line.getOptionValue('b'));
				}
				catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			if (line.hasOption("no-banner"))
				instance.hideBanner = true;
			if (line.hasOption("only-banner"))
				instance.showBanner = true;
			if (line.hasOption("ip"))
			    instance.myIpAddress = line.getOptionValue('i');
			else
			    instance.myIpAddress = null;
		}
		
		return retval;
	}
	
	/**
	 * Print the application usage. Will also print any command
	 * line parsing errors that occurred.
	 */
	public static void printUsage() {
		String errors = collectedErrors.toString();
		if (errors.length() > 0) {
			System.out.println(errors);
			System.out.println();
		}
		Options options = getOptions();
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar WISE_Builder.jar [OPTIONS]...", options);
	}
	
	public static class AlreadyInitializedException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}
