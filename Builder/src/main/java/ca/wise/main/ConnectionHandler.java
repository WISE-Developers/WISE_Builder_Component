package ca.wise.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import ca.wise.fbp.FbpCalculator;
import ca.wise.forecast.ForecastCalculator;
import ca.wise.fwi.FwiCalculator;
import ca.wise.json.Job;
import ca.wise.json.Shutdown;
import ca.wise.solar.SolarCalculator;
import ca.wise.fgm.archive.Archiver;
import ca.wise.fgm.license.License;
import ca.wise.fgm.output.DefaultTypes;
import ca.wise.fgm.output.IDataCombiner;
import ca.wise.fgm.output.IDataWriter;
import ca.wise.fgm.output.IOutputOptions;
import ca.wise.fgm.output.Message;
import ca.wise.fgm.output.OutputType;
import ca.wise.fgm.output.SocketDecoder;
import ca.wise.defaults.proto.JobDefaults;
import ca.wise.fgm.tools.BufferedReaderEx;
import ca.wise.fgm.tools.WISELogger;
import ca.hss.times.TimeZoneInfo;
import ca.hss.times.WorldLocation;
import ca.wise.weather.WeatherCalculator;

public class ConnectionHandler implements Runnable {
	protected Socket m_socket;
	protected ConnectionHandlerEventListener m_listener;
	protected BufferedReaderEx in;
	protected PrintWriter out;
	private boolean admin_kill = false;
	private long m_id;
	private FwiCalculator fwi = new FwiCalculator();
	private FbpCalculator fbp = new FbpCalculator();
	private ForecastCalculator forecast = new ForecastCalculator();
	private WeatherCalculator weather = new WeatherCalculator();

	public ConnectionHandler(Socket socket, ConnectionHandlerEventListener listener) {
		this.m_socket = socket;
		this.m_listener = listener;
	}

	@Override
	public void run() {
		m_id = Thread.currentThread().getId();
		WISELogger.debug("Socket handler " + m_id + " started");
		SocketDecoder prom = new SocketDecoder();
		boolean dataincoming = false;
		try {
			in = new BufferedReaderEx(new InputStreamReader(m_socket.getInputStream(), StandardCharsets.ISO_8859_1));
			out = new PrintWriter(m_socket.getOutputStream(), true);
			String nextline = in.readLine(StandardCharsets.UTF_8).trim();
			Message m = Message.fromString(nextline);
			if (m == Message.STARTUP) {
		        while (!admin_kill) {
		            nextline = in.readLine(StandardCharsets.UTF_8).trim();
		            m = Message.fromString(nextline);
		            if (m == Message.SHUTDOWN)
		            	break;
		            else if (m == Message.ADMIN) {
		            	Admin a = new Admin(nextline);
		            	if (!m_listener.adminMessage(a)) {
		            		if (a.listCompleteJobs) {
		            			getCompletedJobs();
		            		}
		            	}
		            }
		            else if (m == Message.LICENSES) {
		            	sendLicenses();
		            }
		            else if (m == Message.ACK) {
		            	out.println(Message.ACK);
		            }
		            else if (m == Message.BEGINDATA)
		            	dataincoming = true;
		            else if (m == Message.ENDDATA)
		            	dataincoming = false;
		            else if (m == Message.STARTJOB || m == Message.STARTJOB_PB || m == Message.STARTJOB_PB_V2) {
		            	//force the output to proto if the default is set
		            	if (CommandLine.get().getOutputType() == OutputType.XML)
		            		m = Message.STARTJOB;
		            	else if (CommandLine.get().getOutputType() == OutputType.PROTO ||
		            			CommandLine.get().getOutputType() == OutputType.BINARY ||
		            			CommandLine.get().getOutputType() == OutputType.MINIMAL_PROTO)
		            		m = Message.STARTJOB_PB;
		            	else if (CommandLine.get().getOutputType() == OutputType.PROTO_V2 ||
		            			CommandLine.get().getOutputType() == OutputType.BINARY_V2 ||
		            			CommandLine.get().getOutputType() == OutputType.MINIMAL_PROTO_V2)
		            		m = Message.STARTJOB_PB_V2;
									WISELogger.debug("Starting job " + m_id);
		            	String basedir = CommandLine.get().getJobDirectory();
		            	String jobname = Main.getNewJobName();
		            	WISELogger.debug("Sending " + jobname + " to API.");
		            	out.write(jobname);
		            	out.write("\n");
		            	out.flush();
		            	String jobDir = basedir + jobname;
		            	IOutputOptions options = CommandLine.get().getOutputOptions();
		            	IDataCombiner combine = IDataCombiner.create(m, options);
		            	combine.initialize(prom.wise, Main.getConfiguration(), jobDir);
		            	IDataWriter data = combine.combine(jobname);
		            	if (options.shouldStream() && Main.useMqtt()) {
		            		byte[] bytes = data.stream(jobname);
		            		if (bytes != null) {
		            			try {
		            				startJob(jobname, data.getType(), data.getCoreCount(), data.getPriority(),
		            				        data.isValidate(), bytes);
		            			}
			            		catch (IOException e) {
			            			e.printStackTrace();
			            		}
		            		}
		            	}
		            	else {
			            	boolean error = !data.write(jobname);
			            	if (!error) {
			            		try {
			            			startJob(jobname, data.getType(), data.getCoreCount(),
			            			        data.getPriority(), data.isValidate());
			            		}
			            		catch (IOException e) {
			            			e.printStackTrace();
			            		}
			            	}
		            	}
		            }
		            else if (m == Message.GETDEFAULTS) {
		            	sendDefaults();
		            	break;
		            }
		            else if (fwi.isFwi(m)) {
		            	out.println(fwi.calculate(m, in.readLine(StandardCharsets.UTF_8).trim()));
		            }
		            else if (fbp.isFbp(m)) {
		            	String data = null;
		            	if (m == Message.FBP_CALCULATE)
		            		data = in.readLine(StandardCharsets.UTF_8).trim();
		            	String s = fbp.calculate(m, data);
		            	//WISELogger.info("Sending: " + s);
		            	out.println(s);
		            }
		            else if (forecast.isForecast(m)) {
		            	String data = in.readLine(StandardCharsets.UTF_8).trim();
		            	String s = forecast.handle(m, data);
		            	out.println(s);
		            }
		            else if (weather.isWeather(m)) {
		            	String data = in.readLine(StandardCharsets.UTF_8).trim();
		            	String s = weather.handle(m, data);
		            	out.println(s);
		            }
		            else if (SolarCalculator.canHandle(m)) {
		            	String data = in.readLine(StandardCharsets.UTF_8).trim();
		            	String s = SolarCalculator.calculateSunrise(m, data);
		            	out.println(s);
		            }
		            else if (nextline != null) {
		            	Optional<Boolean> success;
		            	if (dataincoming && (success = prom.canDecode(nextline, in.getLastBreakString(), in.getSkippedNewline())).isPresent()) {
		            		//if the decoder is expecting another line to parse as part of this message
		            		if (success.get()) {
			            		nextline = in.readLine(StandardCharsets.UTF_8).trim();
			            		SocketDecoder.NextType r = prom.decode(nextline, in.getLastBreakString(), in.getSkippedNewline());
			            		if (!Strings.isNullOrEmpty(r.errorMessage))
												WISELogger.warn("ERROR: " + r.errorMessage);
			            		else if (r.binarySize > 0) {
			            		    char[] buffer = new char[r.binarySize];
			            		    int read = 0;
			            		    do {
			            		        int toRead = r.binarySize - read;
			            		        int temp = in.read(buffer, read, toRead);
			            		        read += temp;
			            		    } while (read < r.binarySize);
			            		    prom.decode(buffer);
			            		}
			            		//out.println();
		            		}
		            	}
		            	else
										WISELogger.debug(m_id + "> Unknown message: " + nextline);
		            }
		            else
		            	admin_kill = true;
		        }
			}
            else if (m == Message.LICENSES) {
            	sendLicenses();
            }
            else if (m == Message.STOP_JOB) {
        		stopJob(m.data);
            }
			else if (m == Message.ARCHIVE_TAR) {
				Archiver arc = new Archiver(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
				arc.archiveJobTar(m.data);
			}
			else if (m == Message.ARCHIVE_ZIP) {
				Archiver arc = new Archiver(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
				arc.archiveJobZip(m.data);
			}
			else if (m == Message.ARCHIVE_DELETE) {
				Archiver arc = new Archiver(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
				arc.archiveJobDelete(m.data);
			}
			else if (m == Message.JOB_OPTIONS) {
				String list[] = getCompletedJobs();
				for (String s : list) {
	            	out.println(s);
				}
				out.println("COMPLETE");
			}
			else if (m == Message.JOB_OPTIONS_RUNNING) {
				String list[] = getRunningJobs();
				for (String s : list) {
	            	out.println(s);
				}
				out.println("COMPLETE");
			}
			else if (m == Message.JOB_OPTIONS_QUEUED) {
				String list[] = getQueuedJobs();
				for (String s : list) {
	            	out.println(s);
				}
				out.println("COMPLETE");
			}
			else if (m == Message.LIST_TIMEZONES) {
				TimeZoneInfo[] zones = WorldLocation.getList();
				StringBuilder timezones = new StringBuilder();
				int i = 0;
				for (TimeZoneInfo info : zones) {
					timezones.append(info.getCode());
					timezones.append(" (UTC");
					long offset = info.getTimezoneOffset().getTotalHours() + info.getDSTAmount().getTotalHours();
					if (offset > 0)
						timezones.append("+");
					timezones.append(offset);
					if (info.getTimezoneOffset().getMinutes() > 0) {
						timezones.append(":");
						String min = "00" + info.getTimezoneOffset().getMinutes();
						min = min.substring(min.length() - 2);
						timezones.append(min);
					}
					timezones.append(")");
					timezones.append("|");
					timezones.append(i);
					i++;
					timezones.append("|");
				}
				timezones.deleteCharAt(timezones.length() - 1);
				out.println(timezones.toString());
				out.println("COMPLETE");
			}
		}
		catch (SocketException e) { }
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				out.close();
		        in.close();
		        m_socket.close();
			}
			catch (Exception e) { }
		}
		WISELogger.info("Socket handler " + m_id + " closed");
		m_listener.closed(this);
	}

	protected void writeData(DefaultTypes type, int data) {
		out.print(type);
		out.print("\n");
		out.print(data);
		out.print("\n");
	}

	protected void writeData(DefaultTypes type, boolean data) {
		out.print(type);
		out.print("\n");
		out.print(data);
		out.print("\n");
	}

	protected void writeData(DefaultTypes type, double data) {
		out.print(type);
		out.print("\n");
		out.print(data);
		out.print("\n");
	}

	protected void writeData(DefaultTypes type, String data) {
		out.print(type);
		out.print("\n");
		out.print(data);
		out.print("\n");
	}
	
	protected void sendLicenses() {
		List<License> licenses = License.getList();
		StringBuilder retval = new StringBuilder();
		for (License license : licenses) {
			retval.append(license.getComponents().stream().map(x -> x.stream()).collect(Collectors.joining(",")));
			retval.append("|");
			retval.append(license.getName().replace("|", "%7C"));
			retval.append("|");
			retval.append(license.geturl().replace("|", "%7C"));
			retval.append("|");
			retval.append(license.getLicenseName().replace("|", "%7C"));
			retval.append("|");
			retval.append(license.getLicenseUrl().replace("|", "%7C"));
			retval.append("|_|");
		}
		out.println(retval.toString());
	}

	public static void startJob(String jobname, OutputType type, int cores, int priority, boolean validate) throws UnknownHostException, IOException {
		WISELogger.debug("Sending job to " + CommandLine.get().getManagerIp() + ":" + CommandLine.get().getManagerPort());
		if (Main.useMqtt()) {
			Job request = new Job();
			request.cores = cores;
			request.name = jobname;
			request.type = type;
	        request.priority = priority;
	        if (validate)
	            request.validationState = Job.VALIDATE_CURRENT;
	        else
	            request.validationState = Job.VALIDATE_NONE;
			
			Main.getMqttClient().sendJobRequest(request);
		}
		else {
			Socket socket = new Socket(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.write("NAME," + jobname);
			out.write("\n");
			out.write("CORES," + cores);
			out.write("\n");
			if (priority != 0) {
                out.write("PRIORITY," + priority);
                out.write("\n");
			}
			if (validate) {
			    out.write("VALIDATE," + Job.VALIDATE_CURRENT);
			    out.write("\n");
			}
			out.close();
			socket.close();
		}
	}
	
	/**
	 * Start a job on a remote computer. Uses MQTT to request a machine to run the job and transmits the file
	 * instead of writing the file to the file system.
	 * @param jobname The name of the job.
	 * @param extension The file extension of data when written to a file.
	 * @param cores The number of cores the job is allowed to use.
	 * @param data The binary file data.
	 * @param priority The priority to sort the new job with in the job queue.
	 * @param validate Should the FGM be validated before running.
	 */
	public static void startJob(String jobname, OutputType type, int cores, int priority, boolean validate, byte[] data) throws UnknownHostException, IOException {
		WISELogger.debug("Looking for remote host to run job");
		Job request = new Job();
		request.cores = cores;
		request.name = jobname;
		request.type = type;
		request.filedata = data;
		request.priority = priority;
		if (validate)
		    request.validationState = Job.VALIDATE_CURRENT;
		else
		    request.validationState = Job.VALIDATE_NONE;
		
		Main.getMqttClient().sendJobRequest(request);
	}

	protected String[] getCompletedJobs() throws UnknownHostException, IOException {
		WISELogger.info("Getting completed jobs");
		Socket socket = new Socket(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
		BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
		write.write("QUERY,COMPLETE");
		write.write("\n");
		write.flush();
		String line;
		List<String> retval = new ArrayList<String>();
		try {
			while ((line = read.readLine().trim()) != null) {
				if (line.equals("COMPLETE")) {
					break;
				}
				else {
					retval.add(line);
				}
			}
		}
		catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		write.close();
		read.close();
		socket.close();
		return retval.toArray(new String[0]);
	}

	protected String[] getRunningJobs() throws UnknownHostException, IOException {
		WISELogger.info("Getting running jobs");
		Socket socket = new Socket(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
		BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
		write.write("QUERY,RUNNING");
		write.write("\n");
		write.flush();
		String line;
		List<String> retval = new ArrayList<String>();
		try {
			while ((line = read.readLine().trim()) != null) {
				if (line.equals("COMPLETE")) {
					break;
				}
				else {
					retval.add(line);
				}
			}
		}
		catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		write.close();
		read.close();
		socket.close();
		return retval.toArray(new String[0]);
	}

	protected String[] getQueuedJobs() throws UnknownHostException, IOException {
		WISELogger.info("Getting queued jobs");
		Socket socket = new Socket(CommandLine.get().getManagerIp(), CommandLine.get().getManagerPort());
		BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
		write.write("QUERY,QUEUED");
		write.write("\n");
		write.flush();
		String line;
		List<String> retval = new ArrayList<String>();
		try {
			while ((line = read.readLine().trim()) != null) {
				if (line.equals("COMPLETE")) {
					break;
				}
				else {
					retval.add(line);
				}
			}
		}
		catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		write.close();
		read.close();
		socket.close();
		return retval.toArray(new String[0]);
	}
	
	protected void sendDefaults() {
		WISELogger.debug("Sending Defaults");
		JobDefaults def = Main.getDefaults();
		writeData(DefaultTypes.JOBLOCATION, CommandLine.get().getJobDirectory());
		//FGM option defaults
		writeData(DefaultTypes.MAXACCTS, def.getFgmOptions().getMaxAccTs());
		writeData(DefaultTypes.DISTRES, def.getFgmOptions().getDistanceResolution());
		writeData(DefaultTypes.PERIMRES, def.getFgmOptions().getPerimeterResolution());
		writeData(DefaultTypes.MINSPREADROS, def.getFgmOptions().getMinimumSpreadingRos());
		writeData(DefaultTypes.STOPGRIDEND, def.getFgmOptions().getStopAtGridEnd());
		writeData(DefaultTypes.BREACHING, def.getFgmOptions().getBreaching());
		writeData(DefaultTypes.DYNAMICTHRESHOLD, def.getFgmOptions().getDynamicSpatialThreshold());
		writeData(DefaultTypes.SPOTTING, def.getFgmOptions().getSpotting());
		writeData(DefaultTypes.PURGENONDISPLAY, def.getFgmOptions().getPurgeNonDisplayable());
		writeData(DefaultTypes.DX, def.getFgmOptions().getDx());
		writeData(DefaultTypes.DY, def.getFgmOptions().getDy());
		writeData(DefaultTypes.DT, def.getFgmOptions().getDt().toString());
		writeData(DefaultTypes.GROWTHAPPLIED, def.getFgmOptions().getGrowthPercentileApplied());
		writeData(DefaultTypes.GROWTHPERC, def.getFgmOptions().getGrowthPercentile());
		//FBP option defaults
		writeData(DefaultTypes.TERRAINEFF, def.getFbpOptions().getTerrainEffect());
		writeData(DefaultTypes.WINDEFFECT, def.getFbpOptions().getWindEffect());
		//FMC option defaults
		writeData(DefaultTypes.PEROVERVAL, def.getFmcOptions().getPercentOverride());
		writeData(DefaultTypes.NODATAELEV, def.getFmcOptions().getNoDataElev());
		writeData(DefaultTypes.TERRAIN, def.getFmcOptions().getTerrain());
		writeData(DefaultTypes.ACCURATE_LOCATION, true);
		//FWI option defaults
		writeData(DefaultTypes.FWISPACINTERP, def.getFwiOptions().getFwiSpatialInterpolation());
		writeData(DefaultTypes.FWIFROMSPACWEATH, def.getFwiOptions().getFwiFromSpatialWeather());
		writeData(DefaultTypes.HISTORYONFWI, def.getFwiOptions().getHistoryOnEffectedFwi());
		writeData(DefaultTypes.BURNINGCONDITIONSON, def.getFwiOptions().getBurningConditionsOn());
		writeData(DefaultTypes.TEMPORALINTERP, def.getFwiOptions().getFwiTemporalInterpolation());
		//Output file defaults
		writeData(DefaultTypes.VERSION, def.getVectorFileMetadata().getWiseVersion());
		writeData(DefaultTypes.SCENNAME, def.getVectorFileMetadata().getScenarioName());
		writeData(DefaultTypes.JOBNAME, def.getVectorFileMetadata().getJobName());
		writeData(DefaultTypes.IGNAME, def.getVectorFileMetadata().getIgnitionName());
		writeData(DefaultTypes.SIMDATE, def.getVectorFileMetadata().getSimulationDate());
		writeData(DefaultTypes.FIRESIZE, def.getVectorFileMetadata().getFireSize());
		writeData(DefaultTypes.PERIMTOTAL, def.getVectorFileMetadata().getTotalPerimeter());
		writeData(DefaultTypes.PERIMACTIVE, def.getVectorFileMetadata().getActivePerimeter());
		writeData(DefaultTypes.AREAUNIT, 5);
		writeData(DefaultTypes.PERIMUNIT, 1);
		out.print("ENDDEFAULTS");
		out.print("\n");
	}
	
	protected void stopJob(String data) {
		if (Main.useMqtt()) {
			String[] split = data.split("\\|");
			if (split != null && split.length > 0) {
				Shutdown shutdown = new Shutdown();
				String job = split[0];
				shutdown.priority = 0;
				if (split.length > 1) {
					try {
						shutdown.priority = Integer.parseInt(split[1]);
						shutdown.priority = Math.min(2, Math.max(shutdown.priority, 0));
					}
					catch (NumberFormatException ex) {
						WISELogger.warn("Invalid stop priority: " + split[1], ex);
					}
				}
				Main.getMqttClient().sendStopJobRequest(job, shutdown);
			}
		}
	}

	public void terminate() {
		admin_kill = true;
		try {
			if (!m_socket.isClosed()) {
				out.println(Message.SHUTDOWN);
				m_socket.close();
			}
		}
		catch (Exception e) { }
	}

	public static interface ConnectionHandlerEventListener {
		public abstract void closed(ConnectionHandler handler);

		public abstract boolean adminMessage(Admin message);
	}
}
