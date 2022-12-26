package ca.wise.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

import ca.wise.main.CommandLine.AlreadyInitializedException;
import ca.wise.fgm.license.License;
import ca.wise.defaults.proto.JobDefaults;
import ca.wise.defaults.proto.JobDefaults.VectorFileMetadata.AreaUnits;
import ca.wise.defaults.proto.JobDefaults.VectorFileMetadata.PerimeterUnit;
import ca.wise.config.proto.ServerConfiguration;
import ca.wise.config.proto.ServerConfiguration.OutputType;
import ca.wise.config.proto.ServerConfiguration.Verbosity;
import ca.wise.fgm.tools.WISELogger;
import ca.wise.fgm.xml.Defaults;

public class Main {
	private static JobDefaults defaults;
	private static ServerConfiguration config;
	private static Object synchro = new Object();
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	private static MQTTClient mqttClient = null;
	private static Listener mainLooper = null;
	//TODO change these on release
	private static int versionBuild = /*%vb%*/0/*%vb%*/;
	private static int versionMaintanence = /*%vn%*/12/*%vn%*/;
	private static int versionMinor = /*%vm%*/2022/*%vm%*/;
	private static int versionMajor = /*%vM%*/7/*%vM%*/;
	private static String restartCommand;
	private static boolean restartOnExit = false;

	public static JobDefaults getDefaults() {
		synchronized (synchro) {
			return defaults;
		}
	}
	
	public static ServerConfiguration getConfiguration() {
		synchronized (synchro) {
			return config;
		}
	}

	public static void reloadDefaults() {
		synchronized (synchro) {
			try (FileReader reader = new FileReader(CommandLine.get().getJobDirectory() + "/defaults.json")) {
				JobDefaults.Builder builder = JobDefaults.newBuilder();
				JsonFormat.parser()
					.ignoringUnknownFields()
					.merge(reader, builder);
				defaults = builder.build();
			}
			catch (IOException e) {
				defaults = getDefaultJobDefaults().build();
				e.printStackTrace();
			}
			try (FileReader reader = new FileReader(CommandLine.get().getJobDirectory() + "/config.json")) {
				ServerConfiguration.Builder builder = ServerConfiguration.newBuilder();
				JsonFormat.parser()
					.ignoringUnknownFields()
					.merge(reader, builder);
				config = builder.build();
			}
			catch (IOException e) {
				config = getDefaultServerConfiguration(CommandLine.get().getJobDirectory()).build();
				e.printStackTrace();
			}
		}
	}

	public static String getNewJobName() {
		synchronized (synchro) {
			return "job_" + sdf.format(Calendar.getInstance().getTime());
		}
	}
	
	public static Listener getLooper() {
		return mainLooper;
	}
	
	public static boolean useMqtt() {
		return mqttClient != null;
	}
	
	public static MQTTClient getMqttClient() {
		return mqttClient;
	}
	
	public static String getVersion() {
		return String.valueOf(versionMinor) + "." + String.valueOf(versionMaintanence) + "." + String.valueOf(versionBuild);
	}
	
	public static boolean canRestart() {
		return restartCommand != null;
	}
	
	public static void setRestartOnExit(boolean restart) {
		restartOnExit = restart;
	}
	
	/**
	 * Attempt to recreate the command used to start W.I.S.E. Builder
	 * so that it can be restarted remotely.
	 * @param args The command line arguments.
	 */
	private static void buildRestartCommand(String[] args) {
		StringBuilder builder = new StringBuilder();
		builder.append("java");
		RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
		for (String x : mx.getInputArguments()) {
			if (!x.startsWith("-agent")) {
				builder.append(" ");
				builder.append(x);
			}
		}
		File currentJar = null;
		try {
			currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (URISyntaxException e1) {
			builder = null;
		}
		if (currentJar != null && currentJar.exists()) {
			if (currentJar.isFile() && currentJar.getPath().endsWith(".jar")) {
				builder.append(" -jar ");
				builder.append(currentJar.getPath());
			}
			else if (currentJar.isDirectory()) {
				builder.append(" -cp \"");
				builder.append(mx.getClassPath());
				builder.append("\" ");
				builder.append(Main.class.getName());
			}
			else
				builder = null;
			
			if (builder != null) {
				for (String arg : args) {
					builder.append(" ");
					builder.append(arg);
				}
			}
		}
		else {
			builder = null;
		}
		
		if (builder != null) {
			restartCommand = builder.toString();
		}
		else {
			WISELogger.info("Unable to detect startup conditions, remote reboot won't be available");
		}
	}
	
	private static JobDefaults.Builder getDefaultJobDefaults() {
		JobDefaults.Builder builder = JobDefaults.newBuilder();
		builder.getFgmOptionsBuilder()
			.setMaxAccTs("PT0H02M")
			.setDistanceResolution(2)
			.setPerimeterResolution(2)
			.setMinimumSpreadingRos(1.0)
			.setStopAtGridEnd(false)
			.setBreaching(false)
			.setDynamicSpatialThreshold(false)
			.setSpotting(false)
			.setPurgeNonDisplayable(true)
			.setDx(0.0)
			.setDy(0.0)
			.setDt("PT0H")
			.setGrowthPercentileApplied(false)
			.setGrowthPercentile(0.1);
		builder.getFbpOptionsBuilder()
			.setWindEffect(false)
			.setGreenUp(false)
			.setTerrainEffect(true);
		builder.getFmcOptionsBuilder()
			.setNoDataElev(3000.0)
			.setPercentOverride(0.5)
			.setTerrain(false);
		builder.getFwiOptionsBuilder()
			.setFwiSpatialInterpolation(true)
			.setFwiFromSpatialWeather(true)
			.setHistoryOnEffectedFwi(true)
			.setBurningConditionsOn(false)
			.setFwiTemporalInterpolation(false);
		builder.getSummaryFileDefaultsBuilder()
			.setInputSummary(true)
			.setElevationInfo(true)
			.setLocation(true)
			.setGridInfo(true)
			.setTimeToExecute(true);
		builder.getVectorFileMetadataBuilder()
			.setPerimeterUnit(PerimeterUnit.METRES)
			.setAreaUnits(AreaUnits.KILOMETRES_SQUARE)
			.setActivePerimeter(true)
			.setTotalPerimeter(true)
			.setFireSize(true)
			.setSimulationDate(true)
			.setIgnitionName(true)
			.setJobName(true)
			.setScenarioName(true)
			.setWiseVersion(true);
		return builder;
	}
	
	/**
	 * Get the default server configuration settings.
	 * @param jobDirectory The job directory.
	 * @return The default server configuration settings.
	 */
	private static ServerConfiguration.Builder getDefaultServerConfiguration(String jobDirectory) {
		ServerConfiguration.Builder builder = ServerConfiguration.newBuilder();
		builder.getLogBuilder()
			.setFilename("logfile.log")
			.setVerbosity(Verbosity.WARN);
		builder.getSignalsBuilder()
			.setStart("start.txt")
			.setComplete("complete.txt");
		builder.getHardwareBuilder()
			.setProcesses(4)
			.setCores(2);
		builder.getMqttBuilder()
			.setHostname("127.0.0.1")
			.setPort(1883)
			.setTopic("wise")
			.setVerbosity(Verbosity.INFO)
			.setQos(1)
			.setUsername("")
			.setPassword("");
		builder.getBuilderBuilder()
			.setHostname("127.0.0.1")
			.setPort(32479)
			.setFormat(OutputType.JSON);
		builder.setExampleDirectory(jobDirectory);
		return builder;
	}
	
	private static Optional<Double> tryParseDouble(String value) {
		Optional<Double> retval;
		try {
			retval = Optional.of(Double.parseDouble(value));
		}
		catch (NumberFormatException e) {
			retval = Optional.empty();
		}
		return retval;
	}
	
	private static PerimeterUnit getPerimeterUnits(String value) {
		switch (value) {
		case "KM":
			return PerimeterUnit.KILOMETRES;
		case "MI":
			return PerimeterUnit.MILES;
		case "FT":
			return PerimeterUnit.FEET;
		default:
			return PerimeterUnit.METRES;
		}
	}
	
	private static AreaUnits getAreaUnits(String value) {
		switch (value) {
		case "KM2":
			return AreaUnits.KILOMETRES_SQUARE;
		case "MI2":
			return AreaUnits.MILES_SQUARE;
		case "FT2":
			return AreaUnits.FEET_SQUARE;
		default:
			return AreaUnits.METRES_SQUARE;
		}
	}
	
	private static Verbosity getVerbosity(String value) {
		switch (value) {
		case "NONE":
			return Verbosity.NONE;
		case "SEVERE":
			return Verbosity.SEVERE;
		case "WARN":
			return Verbosity.WARN;
		case "MAX":
			return Verbosity.MAX;
		default:
			return Verbosity.INFO;
		}
	}
	
	/**
	 * Write a protobuf message to a file. Writes in pretty
	 * printed JSON.
	 * @param path The file to write to.
	 * @param data The message to write.
	 */
	private static void writeFile(Path path, MessageOrBuilder data) {
		Printer printer = JsonFormat.printer()
				.includingDefaultValueFields();
		try(FileWriter writer = new FileWriter(path.toFile())) {
			String json = printer.print(data);
			writer.write(json);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		buildRestartCommand(args);
		
		//parse the command line arguments
		boolean commandLineSuccess = false;
		try {
			commandLineSuccess = CommandLine.initialize(args);
		}
		catch (AlreadyInitializedException e1) {
		}
		//should the help message be shown
		if  (!commandLineSuccess || CommandLine.get().isShowHelp()) {
			CommandLine.printUsage();
			return;
		}
		//did the user request the application version
		else if (CommandLine.get().isGetVersion()) {
			System.out.println("W.I.S.E. Builder version " + getVersion());
			return;
		}
		else if (CommandLine.get().isTestRestart()) {
			System.out.println(restartCommand);
			return;
		}
		//if the user didn't specify a job directory
		else if (CommandLine.get().getJobDirectory() == null) {
			System.out.println("Missing job directory");
			System.out.println(System.lineSeparator());
			CommandLine.printUsage();
			return;
		}
		
		File fl = new File(CommandLine.get().getJobDirectory() + "/defaults.xml");
		if (fl.exists()) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Defaults.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				Defaults defaults = (Defaults)jaxbUnmarshaller.unmarshal(fl);
				
				Path path = fl.toPath();
				path = path.getParent().resolve("defaults.json");
				//create the JSON defaults file from the original XML defaults
				if (!Files.exists(path)) {
					JobDefaults.Builder jsonDefaults = getDefaultJobDefaults();
					
					if (defaults.getJob() != null) {
						if (defaults.getJob().getFgmOptions() != null) {
							jsonDefaults.getFgmOptionsBuilder()
								.setMaxAccTs(defaults.getJob().getFgmOptions().getMaxAccTS().toString())
								.setDistanceResolution(defaults.getJob().getFgmOptions().getDistRes())
								.setPerimeterResolution(defaults.getJob().getFgmOptions().getPerimRes())
								.setMinimumSpreadingRos(tryParseDouble(defaults.getJob().getFgmOptions().getMinimumSpreadingROS())
										.orElse(jsonDefaults.getFgmOptionsBuilder().getMinimumSpreadingRos()))
								.setStopAtGridEnd(defaults.getJob().getFgmOptions().isStopAtGridEnd())
								.setBreaching(defaults.getJob().getFgmOptions().isBreaching())
								.setDynamicSpatialThreshold(defaults.getJob().getFgmOptions().isDynamicSpatialThreshold())
								.setSpotting(defaults.getJob().getFgmOptions().isSpotting())
								.setPurgeNonDisplayable(defaults.getJob().getFgmOptions().isPurgeNonDisplayable())
								.setDx(tryParseDouble(defaults.getJob().getFgmOptions().getDx())
										.orElse(jsonDefaults.getFgmOptionsBuilder().getDx()))
								.setDy(tryParseDouble(defaults.getJob().getFgmOptions().getDy())
										.orElse(jsonDefaults.getFgmOptionsBuilder().getDy()))
								.setDt(defaults.getJob().getFgmOptions().getDt().toString())
								.setGrowthPercentileApplied(defaults.getJob().getFgmOptions().isGrowthPercentileApplied())
								.setGrowthPercentile(tryParseDouble(defaults.getJob().getFgmOptions().getGrowthPercentile())
										.orElse(jsonDefaults.getFgmOptionsBuilder().getGrowthPercentile()));
						}
						if (defaults.getJob().getFbpOptions() != null) {
							jsonDefaults.getFbpOptionsBuilder()
								.setWindEffect(defaults.getJob().getFbpOptions().isWindEffect())
								.setTerrainEffect(defaults.getJob().getFbpOptions().isTerrainEffect());
						}
						if (defaults.getJob().getFmcOptions() != null) {
							jsonDefaults.getFmcOptionsBuilder()
								.setNoDataElev(tryParseDouble(defaults.getJob().getFmcOptions().getNodataElev())
										.orElse(jsonDefaults.getFmcOptionsBuilder().getNoDataElev()))
								.setPercentOverride(tryParseDouble(defaults.getJob().getFmcOptions().getPerOverride())
										.orElse(jsonDefaults.getFmcOptionsBuilder().getPercentOverride()))
								.setTerrain(defaults.getJob().getFmcOptions().isTerrain());
						}
						if (defaults.getJob().getFwiOptions() != null) {
							jsonDefaults.getFwiOptionsBuilder()
								.setFwiSpatialInterpolation(defaults.getJob().getFwiOptions().isFwiSpacInterp())
								.setFwiFromSpatialWeather(defaults.getJob().getFwiOptions().isFwiFromSpacWeather())
								.setHistoryOnEffectedFwi(defaults.getJob().getFwiOptions().isHistoryOnEffectedFWI())
								.setBurningConditionsOn(defaults.getJob().getFwiOptions().isBurningConditionsOn())
								.setFwiTemporalInterpolation(defaults.getJob().getFwiOptions().isFwiTemporalInterp());
						}
						if (defaults.getJob().getVectorFile() != null && defaults.getJob().getVectorFile().getMetadata() != null) {
							jsonDefaults.getVectorFileMetadataBuilder()
								.setPerimeterUnit(getPerimeterUnits(defaults.getJob().getVectorFile().getMetadata().getPerimUnit()))
								.setAreaUnits(getAreaUnits(defaults.getJob().getVectorFile().getMetadata().getAreaUnit()))
								.setActivePerimeter(defaults.getJob().getVectorFile().getMetadata().isPerimActive())
								.setTotalPerimeter(defaults.getJob().getVectorFile().getMetadata().isPerimTotal())
								.setFireSize(defaults.getJob().getVectorFile().getMetadata().isFireSize())
								.setSimulationDate(defaults.getJob().getVectorFile().getMetadata().isSimDate())
								.setIgnitionName(defaults.getJob().getVectorFile().getMetadata().isIgName())
								.setJobName(defaults.getJob().getVectorFile().getMetadata().isJobName())
								.setScenarioName(defaults.getJob().getVectorFile().getMetadata().isScenName())
								.setWiseVersion(defaults.getJob().getVectorFile().getMetadata().isVersion());
						}
					}
					
					//write the JSON defaults to a file
					writeFile(path, jsonDefaults);
					
					Main.defaults = jsonDefaults.build();
				}
				path = path.getParent().resolve("config.json");
				//create the JSON server configuration from the original defaults
				if (!Files.exists(path)) {
					ServerConfiguration.Builder builder = getDefaultServerConfiguration(CommandLine.get().getJobDirectory());
					
					if (defaults.getLog() != null) {
						builder.getLogBuilder()
							.setFilename(defaults.getLog().getName())
							.setVerbosity(getVerbosity(defaults.getLog().getVerbosity()));
					}
					if (defaults.getSignals() != null) {
						builder.getSignalsBuilder()
							.setStart(defaults.getSignals().getStart())
							.setComplete(defaults.getSignals().getComplete());
					}
					if (defaults.getHardware() != null) {
						builder.getHardwareBuilder()
							.setProcesses(defaults.getHardware().getProcesses())
							.setCores(defaults.getHardware().getCores());
					}
					if (defaults.getMqtt() != null) {
						builder.getMqttBuilder()
							.setHostname(defaults.getMqtt().getHostName())
							.setPort(Optional.ofNullable(defaults.getMqtt().getPort()).orElse(1883))
							.setTopic(defaults.getMqtt().getTopic())
							.setVerbosity(getVerbosity(defaults.getMqtt().getVerbosity()))
							.setQos(Optional.ofNullable(defaults.getMqtt().getQos()).orElse(1))
							.setUsername(defaults.getMqtt().getUserName())
							.setPassword(defaults.getMqtt().getPassword());
					}

					writeFile(path, builder);
					//reset the command line arguments after writing the file
					try {
						commandLineSuccess = CommandLine.reset(args);
					}
					catch (AlreadyInitializedException e1) {
					}
				}
				
				fl.delete();
			}
			catch (JAXBException e) {
				e.printStackTrace();
			}
		}
		else {
			Path path = Paths.get(CommandLine.get().getJobDirectory()).resolve("defaults.json");
			//create defaults file if it doesn't exist
			if (!Files.exists(path)) {
				JobDefaults.Builder jsonDefaults = getDefaultJobDefaults();

				writeFile(path, jsonDefaults);
			}
			path = path.getParent().resolve("config.json");
			//create the server config file if it doesn't exist
			if (!Files.exists(path)) {
				ServerConfiguration.Builder builder = getDefaultServerConfiguration(CommandLine.get().getJobDirectory());

				writeFile(path, builder);
				//reset the command line arguments after writing the file
				try {
					commandLineSuccess = CommandLine.reset(args);
				}
				catch (AlreadyInitializedException e1) {
				}
			}
		}
		
		//if the user didn't want the license banner hidden
		if (!CommandLine.get().isHideBanner()) {
			WISELogger.info("Parts of the W.I.S.E. Suite are licensed as follows:");
			List<License> licenses = License.getList();
			for (License license : licenses) {
				WISELogger.info("    " + license.getName() + " - " + license.getLicenseName());
			}
		}
		//if the user wanted to exit after displaying the licenses.
		if (CommandLine.get().isShowBanner()) {
			return;
		}
		
		WISELogger.setLevel(CommandLine.get().getLogLevel());
		if (defaults == null)
			reloadDefaults();
		if (defaults == null)
			return;
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				WISELogger.info("Shutting down...");
				if (mqttClient != null) {
					mqttClient.shutdown();
				}
			}
		});
		
		//warn the user about potential security issues if the user is
		//allowed to set MQTT connection parameters
		if (CommandLine.get().isUserMqttSettings()) {
			WISELogger.error("Allowing users to set the MQTT connection settings could be a security risk and is not recommended. Proceed with caution.");
		}
		
		//initialize MQTT
		if (CommandLine.get().isUseMqtt()) {
			if (CommandLine.get().getMqttUrl() == null) {
				WISELogger.error("No MQTT parameters available in the defaults XML file");
				return;
			}
			mqttClient = new MQTTClient(CommandLine.get().getMqttUrl(), CommandLine.get().getMqttUsername(),
					CommandLine.get().getMqttPassword(), CommandLine.get().getMqttTopic(), CommandLine.get().getMyIpAddress());
		}
		//initialize the socket connection
		else {
			Socket socket;
			try {
				socket = new Socket(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
				BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
				write.write("ACK");
				write.write("\n");
				write.flush();
				String line = read.readLine();
				if (line.equalsIgnoreCase("ACK")) {
					WISELogger.info("Successful connection to W.I.S.E. Manager.");
					//ConnectionHandler.startJob("job_2016031020490001", 2);
				}
				else
				WISELogger.warn("Error in connection to W.I.S.E. Manager.");
			} catch (IOException e) {
				WISELogger.warn("Failed to connect to W.I.S.E. Manager at " + CommandLine.get().getManagerIp() + ":" + CommandLine.get().getManagerPort() + " (" + e.getMessage() + ").", e);
			}
		}

		mainLooper = new Listener();
		mainLooper.start();
		
		if (restartOnExit) {
			ProcessBuilder pBuilder = new ProcessBuilder(restartCommand);
			try {
				pBuilder.start();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
