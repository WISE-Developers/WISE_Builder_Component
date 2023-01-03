package ca.wise.fgm.output;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;

import com.google.common.base.Strings;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.JsonFormat.TypeRegistry;

import ca.wise.file.ISpatialFile;
import ca.wise.fuel.proto.FbpFuel;
import ca.wise.fuel.proto.FuelName;
import ca.wise.project.proto.PrometheusData;
import ca.wise.project.proto.Project.Outputs.AssetStatsOutput;
import ca.wise.project.proto.Project.Outputs.CompressionType;
import ca.wise.project.proto.Project.Outputs.FuelGridOutput;
import ca.wise.project.proto.Project.Outputs.GridOutput;
import ca.wise.project.proto.Project.Outputs.StatsOutput;
import ca.wise.project.proto.Project.Outputs.SummaryOutput;
import ca.wise.project.proto.Project.Outputs.VectorOutput;
import ca.wise.project.proto.PrometheusData.InputFile;
import ca.wise.project.proto.PrometheusData.SimulationSettings;
import ca.wise.project.proto.PrometheusData.OutputStream.GeoserverStream;
import ca.wise.project.proto.PrometheusData.SimulationSettings.HardwareSettings;
import ca.wise.project.proto.PrometheusData.SimulationSettings.JobSignals;
import ca.wise.project.proto.PrometheusData.SimulationSettings.LogFile;
import ca.wise.project.proto.PrometheusData.SimulationSettings.MqttSettings;
import ca.wise.project.proto.PrometheusData.SimulationSettings.Verbosity;
import ca.wise.config.proto.ServerConfiguration;
import ca.wise.fgm.tools.IntegerHelper;
import ca.wise.weather.proto.WeatherGridFilter.GridTypeOne;
import ca.wise.weather.proto.WeatherGridFilter.GridTypeTwo;
import ca.wise.weather.proto.WindGrid.SectorData.DirectionWrapper.WindDirection;
import ca.wise.api.WISE;
import ca.wise.api.input.FuelOption;
import ca.wise.api.input.HFFMCMethod;
import ca.wise.api.input.WeatherPatchOperation;
import ca.wise.api.output.AssetStatsFile;
import ca.wise.api.output.ExportTimeOverride;
import ca.wise.api.output.FuelGridFile;
import ca.wise.api.output.GeoServerOutputStreamInfo;
import ca.wise.api.output.GlobalStatistics;
import ca.wise.api.output.GridFileInterpolation;
import ca.wise.api.output.MqttOutputStreamInfo;
import ca.wise.api.output.OutputStreamInfo;
import ca.wise.api.output.PerimeterTimeOverride;
import ca.wise.api.output.StatsFile;
import ca.wise.api.output.SummaryFile;
import ca.wise.api.output.VectorFile;
import ca.wise.api.output.GridFile.ExportOptionsOrigin;
import ca.wise.api.units.AreaUnit;
import ca.wise.api.units.DistanceUnit;
import ca.hss.math.HSS_Double;
import ca.hss.math.ProtoWrapper;
import ca.hss.math.proto.GeoPoint;
import ca.hss.math.proto.XYPoint;
import ca.hss.times.WTime;
import ca.hss.times.WTimeManager;
import ca.hss.times.WTimeSpan;
import ca.hss.times.WorldLocation;
import ca.hss.times.serialization.TimeSerializer;

@SuppressWarnings("deprecation")
public abstract class IPBCombiner implements IDataCombiner {
	
	/**
	 * The inputs from the API.
	 */
	protected WISE input;
	/**
	 * The server configuration settings.
	 */
	protected ServerConfiguration config;
	/**
	 * The directory to store files for this job in.
	 */
	protected String jobDir;
	/**
	 * The parent of the job directory.
	 */
	protected String baseDir;
	/**
	 * A list of thread counts for each scenario in the job.
	 */
	protected List<Integer> threads;
	
	/**
	 * Options regarding the output format.
	 */
	protected OutputOptions options;
	/**
	 * The simulations timezone.
	 */
	protected WorldLocation worldLocation;
	
	IPBCombiner(Object options) {
		if (options instanceof OutputOptions) {
			this.options = (OutputOptions)options;
		}
		else {
			this.options = new OutputOptions();
		}
	}

	@Override
	public void initialize(WISE input, ServerConfiguration config, String jobDir) {
		this.input = input;
		this.config = config;
		this.jobDir = jobDir;
		this.baseDir = Paths.get(jobDir).getParent().toAbsolutePath().toString();
	}
	
	Set<String> inputFileSet = new HashSet<>();
	Map<String, String> addedFiles = new HashMap<>();
	protected String addFile(PrometheusData.Builder data, String outPath, String filename, String requireName) {
		if (filename.length() < 1) {
			return filename;
		}
		String slash = filename.replaceAll("\\\\", "/");
		if (addedFiles.containsKey(slash)) {
			return addedFiles.get(slash);
		}
		else {
			File in = new File(filename);
			String ext = extension(filename);
			String name = filename(filename);
			String newName;
			if (requireName != null && requireName.length() > 0) {
				newName = requireName;
				//skip files that already exist
				if (inputFileSet.contains(newName))
					return newName;
			}
			else {
				newName = "Inputs/" + name + "." + ext;
				String fullpath = outPath + "/" + newName;
				File out = new File(fullpath);
				int i = 2;
				while (out.exists()) {
					newName = "Inputs/" + name + i + "." + ext;
					fullpath = outPath + "/" + newName;
					out = new File(fullpath);
					i++;
				}
				while (inputFileSet.contains(newName)) {
					newName = "Inputs/" + name + i + "." + ext;
					i++;
				}
			}
	
			inputFileSet.add(newName);
			addedFiles.put(slash, newName);
			InputFile.Builder fileBuilder = data.addInputFilesBuilder()
				.setFilename(newName);
			
			ByteString fileData = ByteString.EMPTY;
			try {
	            fileData = ByteString.readFrom(new FileInputStream(in));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			fileBuilder.setData(fileData);

			return newName;
		}
	}
	
	@Override
	public String readyFile(String outPath, final String filename) {
		if (filename.startsWith("attachment:/")) {
			Optional<ca.wise.api.FileAttachment> att = input.getAttachments().stream().filter(x -> x.getFilename().equals(filename)).findFirst();
			
			if (att.isPresent()) {
				String name = filename.substring(12);
				int index = name.indexOf("/");
				if (index >= 0 && index < (name.length() - 1))
					name = name.substring(index + 1);
				Path newPath = Paths.get(outPath + "/Inputs/" + name);
				if (!Files.exists(newPath)) {
					try (PrintStream out = new PrintStream(new FileOutputStream(newPath.toFile(), false))) {
					    if (att.get().getData() == null)
					        out.print(att.get().getContents());
					    else
					        out.write(att.get().getData(), 0, att.get().getData().length);
						return "Inputs/" + name;
					}
					catch (FileNotFoundException e) {
						e.printStackTrace();
						return null;
					}
				}
				else
					return "Inputs/" + name;
			}
			//error, couldn't find the referenced attachment
			else
				return null;
		}
		else
			return IDataCombiner.super.readyFile(outPath, filename);
	}
	
	/**
	 * Replace the file extension on a filename. Supports multi-part extensions
	 * (ex. filename.tar.gz) but not Unix hidden files (.filename).
	 * @param filename The original filename.
	 * @param newExtension The new extension to place on the filename.
	 * @return The filename with the extension replaced.
	 */
	protected String replaceExtension(String filename, String newExtension) {
		int index = filename.lastIndexOf('/');
		int index2 = filename.lastIndexOf('\\');
		if (index2 > index)
			index = index2;
		if (index >= 0) {
			index = filename.indexOf('.', index);
		}
		else {
			index = filename.indexOf('.');
		}
		if (index > 0) {
			filename = filename.substring(0, index);
			if (!newExtension.startsWith("."))
				filename = filename + ".";
			filename = filename + newExtension;
		}
		return filename;
	}
	
	/**
	 * Prepare a file for use in the FGM. This may mean reading its contents and storing them or moving them
	 * to the correct location on disk. The filename will be modified to what is required for PSaaS to properly
	 * load the file when it runs.
	 * @param data The full output file specification.
	 * @param outPath The output file path for this job.
	 * @param filename The name of the file to process. May be a path to a file on disk or an attachment URL.
	 * @param groupFiles Should files with similar names be processed along with the one supplied (useful for
	 *                   shapefiles which are stored in multiple parts).
	 * @return The name of the file that should be added to the output FGM.
	 */
	protected String readyFile(PrometheusData.Builder data, String outPath, String filename, boolean groupFiles) {
		if (options.singleFile) {
			//if it's an attachment return just the name
			//attachments will all be added to the generated file
			//when single file is enabled, no need to do so here
			if (filename.startsWith("attachment:/")) {
				String name = filename.substring(12);
				int index = name.indexOf("/");
				if (index >= 0 && index < (name.length() - 1))
					name = name.substring(index + 1);
				name = "Inputs/" + name;
				inputFileSet.add(name);
				return name;
			}
			else {
				String retval = addFile(data, outPath, filename, null);
				if (groupFiles) {
				    ISpatialFile description = ISpatialFile.getSpatialFile(filename);
    				Path path = Paths.get(filename);
    				//look for files with the same name but different extensions
    				try {
    					DirectoryStream<Path> stream = Files.newDirectoryStream(path.getParent(),
    							com.google.common.io.Files.getNameWithoutExtension(path.getFileName().toString()) + ".*");
    					for (Path p : stream) {
    						if (!p.equals(path) && description.isFileValid(p)) {
    							String newName = replaceExtension(retval, com.google.common.io.Files.getFileExtension(p.toString()));
    							addFile(data, outPath, p.toString(), newName);
    						}
    					}
    				}
    				catch (IOException e) {
    					e.printStackTrace();
    				}
				}
				return retval;
			}
		}
		else {
			new File(jobDir + "/Inputs").mkdirs();
			return readyFile(outPath, filename);
		}
	}
	
	/**
	 * Convert the logging verbosity from a string to a protbuf enum.
	 * @param value The verbosity to log at.
	 * @return A protobuf enum for the verbosity.
	 */
	protected Verbosity stringToVerbosity(String value) {
		switch (value) {
		case "NONE":
			return Verbosity.NONE;
		case "SEVERE":
			return Verbosity.SEVERE;
		case "INFO":
			return Verbosity.INFO;
		case "MAX":
			return Verbosity.MAX;
		default:
			return Verbosity.WARN;
		}
	}
	
	protected Verbosity verbosityToVerbosity(ca.wise.config.proto.ServerConfiguration.Verbosity value) {
		switch (value) {
		case MAX:
			return Verbosity.MAX;
		case NONE:
			return Verbosity.NONE;
		case SEVERE:
			return Verbosity.SEVERE;
		case WARN:
			return Verbosity.WARN;
		default:
			return Verbosity.INFO;
		}
	}

	/**
	 * Convert a string from the API to a WTime.
	 * @param input A string from the API, expected to be in ISO8601 format.
	 * @return A WTime that holds the time from the string.
	 */
	protected WTime createDateTime(String input) {
		WTime retval = new WTime(new WTimeManager(worldLocation));
		retval.parseDateTime(input, WTime.FORMAT_STRING_ISO8601);
		return retval;
	}

	/**
	 * Convert a timespan from the API to a duration.
	 * @param input A string from the API, expected to be in HH:mm:ss format.
	 * @return A WTimeSpan that holds the timespan from the string.
	 */
	protected WTimeSpan createDuration(String input) {
		return new WTimeSpan(input);
	}

	/**
	 * Initialize the job details from the defaults file.
	 * @param data The protobuf message builder.
	 */
	protected void initParams(PrometheusData.Builder data) {
		SimulationSettings.Builder builder = SimulationSettings.newBuilder();

		//logger settings
		builder.setLogfile(
				LogFile.newBuilder()
					.setFilename(config.getLog().getFilename())
					.setVerbosity(verbosityToVerbosity(config.getLog().getVerbosity()))
				);

		//job started/stopped signal files
		builder.setSignals(
				JobSignals.newBuilder()
					.setStartFilename(config.getSignals().getStart())
					.setCompleteFilename(config.getSignals().getComplete())
				);

		//if an export format was specified, set it
		if (input.getJobOptions().getExportFormat() != null)
		    builder.setExportFormatValue(input.getJobOptions().getExportFormat());
		
		//set the sub-scenario options
		if (input.getJobOptions().getTempOutputInterval() != null)
		    data.getSubScenarioOptionsBuilder()
		        .getTempOutputIntervalBuilder()
		        .setValue(input.getJobOptions().getTempOutputInterval());
		if (input.getJobOptions().getTempOutputCount() != null)
		    data.getSubScenarioOptionsBuilder()
		        .getTempOutputCountBuilder()
		        .setValue(input.getJobOptions().getTempOutputCount());
		
		//stream output files settings
		if (input.getStreamInfo().size() > 0) {
			for (OutputStreamInfo stream : input.getStreamInfo()) {
				if (stream instanceof MqttOutputStreamInfo) {
					data.addOutputLocationBuilder()
						.getMqttBuilder();
				}
				else if (stream instanceof GeoServerOutputStreamInfo) {
					GeoServerOutputStreamInfo geo = (GeoServerOutputStreamInfo)stream;
					GeoserverStream.Builder geoserverBuilder = data.addOutputLocationBuilder()
					        .getGeoserverBuilder();
					geoserverBuilder.setPassword(geo.getPassword())
					    .setUsername(geo.getUsername())
					    .setSrs(geo.getDeclaredSrs())
					    .setUrl(geo.getUrl())
					    .setWorkspace(geo.getWorkspace())
					    .setCoverage(geo.getCoverageStore());
				}
			}
		}

		if (options.version < 2) {
			//options for the resources that the job is allowed to use
			HardwareSettings.Builder hardware = HardwareSettings.newBuilder();
			int totalCores = config.getHardware().getCores();
			int numJobs = input.getInputs().getScenarios().size();
			threads = new ArrayList<Integer>(numJobs);
			if (numJobs >= totalCores) {
				hardware.setCores(totalCores);
				for (int i = 0; i < numJobs; i++)
					threads.add(i, 1);
			}
			else {
				int numCores = 1;
				while (numJobs <= ((int)(((double)totalCores) / 2.0))) {
					totalCores = (int)(((double)totalCores) / 2.0);
					numCores++;
				}
				for (int i = 0; i < numJobs; i++)
					threads.add(i, numCores);
				hardware.setCores(numJobs);
			}
			hardware.setProcesses(config.getHardware().getProcesses());
			builder.setHardware(hardware);
		}

		//MQTT communication options
		MqttSettings.Builder mqtt = null;
		if (config.hasMqtt()) {
			mqtt = MqttSettings.newBuilder();
			mqtt.setDebug(false);
			mqtt.setHostname(config.getMqtt().getHostname());
			mqtt.setPort(ProtoWrapper.ofInt(config.getMqtt().getPort()));
			mqtt.setQos(ProtoWrapper.ofInt(config.getMqtt().getQos()));
			mqtt.setTopic(config.getMqtt().getTopic());
			if (!Strings.isNullOrEmpty(config.getMqtt().getUsername()) && !Strings.isNullOrEmpty(config.getMqtt().getPassword())) {
				mqtt.setUsername(ProtoWrapper.ofString(config.getMqtt().getUsername()));
				mqtt.setPassword(ProtoWrapper.ofString(config.getMqtt().getPassword()));
			}
			mqtt.setVerbosity(verbosityToVerbosity(config.getMqtt().getVerbosity()));
		}
		//only allow the user to change the connection settings if setup on the command line to do so
		if (options != null && options.allowUserConnectionSettings && input.getMqttSettings() != null) {
			if (mqtt == null)
				mqtt = MqttSettings.newBuilder();
			if (input.getMqttSettings().getHost() != null && input.getMqttSettings().getHost().length() > 0)
				mqtt.setHostname(input.getMqttSettings().getHost());
			if (input.getMqttSettings().getPort() != null)
				mqtt.setPort(ProtoWrapper.ofInt(input.getMqttSettings().getPort()));
			if (input.getMqttSettings().getQos() != null)
				mqtt.setQos(ProtoWrapper.ofInt(input.getMqttSettings().getQos()));
			if (input.getMqttSettings().getTopic() != null && input.getMqttSettings().getTopic().length() > 0)
				mqtt.setTopic(input.getMqttSettings().getTopic());
			if (input.getMqttSettings().getVerbosity() != null && input.getMqttSettings().getVerbosity().length() > 0)
				mqtt.setVerbosity(stringToVerbosity(input.getMqttSettings().getVerbosity()));
			if (input.getMqttSettings().getUsername() != null)
				mqtt.setUsername(ProtoWrapper.ofString(input.getMqttSettings().getUsername()));
			if (input.getMqttSettings().getPassword() != null)
				mqtt.setPassword(ProtoWrapper.ofString(input.getMqttSettings().getPassword()));
		}
		if (mqtt != null)
			builder.setMqtt(mqtt);
		
		data.setSettings(builder);
	}

	/**
	 * Initialize the job.
	 * @param data The protobuf message builder.
	 * @param jobname The name of the job.
	 */
	protected void initJob(PrometheusData.Builder data, String jobname) {
		data.setName(ProtoWrapper.ofString(jobname));
	}
	
	/**
	 * Create a color reference from RGB values.
	 * @param r The red component.
	 * @param g The green component.
	 * @param b The blue component.
	 * @return A color value.
	 */
	protected int RGB(String r, String g, String b) {
		try {
			int ir = Integer.parseInt(r);
			int ig = Integer.parseInt(g);
			int ib = Integer.parseInt(b);
			return RGB(ir, ig, ib);
		}
		catch (NumberFormatException e) { }
		return 0;
	}
	
	/**
	 * Create a color reference from RGB values.
	 * @param r The red component.
	 * @param g The green component.
	 * @param b The blue component.
	 * @return A color value.
	 */
	protected int RGB(int r, int g, int b) {
		return r | (g << 8) | (b << 16);
	}
	
	protected int HueToRGB(int n1, int n2, int hue) {
		if (hue < 0)
			hue += HLSMAX;
		if (hue > HLSMAX)
			hue -= HLSMAX;
		
		if (hue < (HLSMAX/6))
			return ((n1 + (((n2-n1)*hue+(HLSMAX/12))/(HLSMAX/6))));
		if (hue < (HLSMAX/2))
			return n2;
		if (hue < ((HLSMAX*2)/3))
			return (n1 + (((n2-n1)*(((HLSMAX*2)/3)-hue)+(HLSMAX/12))/(HLSMAX/6)));
		return n1;
	}
	
	private final int RGBMAX = 255;
	private final int HLSMAX = 255;
	/**
	 * Create a color reference from HSL values.
	 * @param h The hue.
	 * @param s The saturation.
	 * @param l The luminence.
	 * @return A color value.
	 */
	protected int HSL(String h, String s, String l) {
		int ir = 0;
		int ig = 0;
		int ib = 0;
		try {
			int ih = Integer.parseInt(h);
			int is = Integer.parseInt(s);
			int il = Integer.parseInt(l);
			if (is == 0) {
				ir = ig = ib = (il * RGBMAX) / HLSMAX;
			}
			else {
				int magic1, magic2;
				if (il <= (HLSMAX/2))
					magic2 = (il*(HLSMAX + is) + ((HLSMAX/2))/HLSMAX);
				else
					magic2 = (il + is - ((il*is) + (HLSMAX/2))/HLSMAX);
				magic1 = 2*il - magic2;
				ir = (HueToRGB(magic1, magic2, ih+(HLSMAX/3))*RGBMAX + (HLSMAX/2))/HLSMAX;
				ig = (HueToRGB(magic1, magic2, ih)*RGBMAX + (HLSMAX/2))/HLSMAX;
				ib = (HueToRGB(magic1, magic2, (ih - (HLSMAX/3)))*RGBMAX + (HLSMAX/2))/HLSMAX;
			}
		}
		catch (NumberFormatException e) { }
		return RGB(ir, ig, ib);
	}
	
	/**
	 * Try to convert a fuel type's name to a fuel name enum. 
	 * @param name The name of the fuel type.
	 * @return An enum value for the fuel.
	 */
	protected FuelName findFuelName(String name) {
		name = name.replace("-", "").replace("/", "").toLowerCase();
		switch (name) {
		case "c1":
			return FuelName.C1;
		case "c2":
			return FuelName.C2;
		case "c3":
			return FuelName.C3;
		case "c4":
			return FuelName.C4;
		case "c5":
			return FuelName.C5;
		case "c6":
			return FuelName.C6;
		case "c7":
			return FuelName.C7;
		case "d1":
			return FuelName.D1;
		case "d2":
			return FuelName.D2;
		case "d1d2":
			return FuelName.D1D2;
		case "m1":
			return FuelName.M1;
		case "m2":
			return FuelName.M2;
		case "m1m2":
			return FuelName.M1M2;
		case "m3":
			return FuelName.M3;
		case "m4":
			return FuelName.M4;
		case "m3m4":
			return FuelName.M3M4;
		case "o1a":
			return FuelName.O1A;
		case "o1b":
			return FuelName.O1B;
		case "o1ab":
		case "o1ao1b":
			return FuelName.O1AB;
		case "s1":
			return FuelName.S1;
		case "s2":
			return FuelName.S2;
		case "s3":
			return FuelName.S3;
		case "non":
		case "nonfuel":
			return FuelName.Non;
		case "nz2":
			return FuelName.Nz2;
		case "nz15":
			return FuelName.Nz15;
		case "nz30":
			return FuelName.Nz30;
		case "nz31":
			return FuelName.Nz31;
		case "nz32":
			return FuelName.Nz32;
		case "nz33":
			return FuelName.Nz33;
		case "nz40":
			return FuelName.Nz40;
		case "nz41":
			return FuelName.Nz41;
		case "nz43":
			return FuelName.Nz43;
		case "nz44":
			return FuelName.Nz44;
		case "nz45":
			return FuelName.Nz45;
		case "nz46":
			return FuelName.Nz46;
		case "nz47":
			return FuelName.Nz47;
		case "nz50":
			return FuelName.Nz50;
		case "nz51":
			return FuelName.Nz51;
		case "nz52":
			return FuelName.Nz52;
		case "nz53":
			return FuelName.Nz53;
		case "nz54":
			return FuelName.Nz54;
		case "nz55":
			return FuelName.Nz55;
		case "nz56":
			return FuelName.Nz56;
		case "nz57":
			return FuelName.Nz57;
		case "nz58":
			return FuelName.Nz58;
		case "nz60":
			return FuelName.Nz60;
		case "nz61":
			return FuelName.Nz61;
		case "nz62":
			return FuelName.Nz62;
		case "nz63":
			return FuelName.Nz63;
		case "nz64":
			return FuelName.Nz64;
		case "nz65":
			return FuelName.Nz65;
		case "nz66":
			return FuelName.Nz66;
		case "nz67":
			return FuelName.Nz67;
		case "nz68":
			return FuelName.Nz68;
		case "nz69":
			return FuelName.Nz69;
		case "nz70":
			return FuelName.Nz70;
		case "nz71":
			return FuelName.Nz71;
		default:
			return FuelName.UNRECOGNIZED;
		}
	}
	
	/**
	 * The possible columns in an LUT file.
	 * @author Travis Redpath
	 */
	protected enum LUTColumn {
		I_GRID_VALUE,
		E_GRID_VALUE,
		A_FUEL_TYPE,
		F_FUEL_TYPE,
		COLOUR_R,
		COLOUR_G,
		COLOUR_B,
		COLOUR_H,
		COLOUR_S,
		COLOUR_L
	}
	
	/**
	 * Case insensitive keyname lookup.
	 * @param map The map to lookup the key in.
	 * @param names The possible key names.
	 * @return The key name, exactly as found in the map, if found. Null if not present.
	 */
	private String findKeyName(Map<String, Integer> map, String...names) {
		for (String key : map.keySet()) {
			for (String name : names) {
				if (key.compareToIgnoreCase(name) == 0)
					return key;
			}
		}
		return null;
	}
	
	/**
	 * Lookup the column in the LUT CSV file.
	 * @param records The CSV data.
	 * @return A map of known 
	 */
	protected Map<LUTColumn, String> buildColumns(CSVParser records) {
		Map<LUTColumn, String> retval = new HashMap<>();
		Map<String, Integer> headerMap = records.getHeaderMap();
		//check to see what the grid value column header is
		String temp = findKeyName(headerMap, "grid_value", "grid value");
		if (temp != null)
			retval.put(LUTColumn.I_GRID_VALUE, temp);
		//check to see what the export value column header is
		temp = findKeyName(headerMap, "export_value", "export grid_value", "export grid_value", "export_grid_value");
		if (temp != null)
			retval.put(LUTColumn.E_GRID_VALUE, temp);
		//check to see what the name column header is
		temp = findKeyName(headerMap, "descriptive_name", "descriptive name", "agency fuel type", "agency_fuel_type");
		if (temp != null)
			retval.put(LUTColumn.A_FUEL_TYPE, temp);
		//check to see what the fuel type column header is
		temp = findKeyName(headerMap, "fuel_type", "fuel type", "FBP fuel type", "FBP_fuel_type");
		if (temp != null)
			retval.put(LUTColumn.F_FUEL_TYPE, temp);
		//check to see what the red column header is
		temp = findKeyName(headerMap, "R", "Red");
		if (temp != null)
			retval.put(LUTColumn.COLOUR_R, temp);
		//check to see what the green column header is
		temp = findKeyName(headerMap, "G", "Green");
		if (temp != null)
			retval.put(LUTColumn.COLOUR_G, temp);
		//check to see what the blue column header is
		temp = findKeyName(headerMap, "B", "Blue");
		if (temp != null)
			retval.put(LUTColumn.COLOUR_B, temp);
		//check to see what the hue column header is
		temp = findKeyName(headerMap, "H", "hue");
		if (temp != null)
			retval.put(LUTColumn.COLOUR_H, temp);
		//check to see what the saturation column header is
		temp = findKeyName(headerMap, "S", "sat", "Saturation");
		if (temp != null)
			retval.put(LUTColumn.COLOUR_S, temp);
		//check to see what the luminence column header is
		temp = findKeyName(headerMap, "L", "lum", "luminence");
		if (temp != null)
			retval.put(LUTColumn.COLOUR_L, temp);
		return retval;
	}
	
	private double parseDouble(String d) {
	    try {
	        return Double.parseDouble(d);
	    }
	    catch (NumberFormatException e) {
	        return 0.0;
	    }
	}
    
    private int parseInteger(String d) {
        try {
            return Integer.parseInt(d);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private boolean parseBoolean(String d) {
        if (d.equalsIgnoreCase("true"))
            return true;
        else if (d.equalsIgnoreCase("false"))
            return false;
        try {
            return Integer.parseInt(d) != 0;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isNull(String value) {
        return Strings.isNullOrEmpty(value) || value.equalsIgnoreCase("null") || value.equalsIgnoreCase("undefined");
    }
	
	/**
	 * Retrieve a list of options to be applied to a specified fuel type.
	 * @param name The name of the fuel type.
	 * @return The list of options to be applied.
	 */
	protected List<FuelOption> findFuelOptions(String name) {
		final String fname = name.replace("-", "").replace("/", "").toLowerCase();
		return input.getInputs().getFuelOptions().stream().filter(x -> x.getFuelType().replace("-", "").replace("/", "").equalsIgnoreCase(fname)).collect(Collectors.toList());
	}
	
	/**
	 * Parse spread parm data from a fuel definition.
	 * @param data The fuel definition.
	 * @param dataIndex The current index in the fuel definition.
	 * @param builder The builder to place the spread parm data in.
	 */
	private void parseLutDefinitionSpreadParms(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.SpreadParmsAttribute.Builder builder) throws IOException {
	    String type = data[dataIndex.value++];
	    if (type.equals("S1")) {
	        ca.wise.fuel.proto.SpreadParmsS1.Parms.Builder s1 = builder.getS1Builder()
	                .setVersion(1)
	                .getParmsBuilder()
	                .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            if (!isNull(a)) {
                s1.getABuilder()
                    .setValue(parseDouble(a));
            }
            if (!isNull(b)) {
                s1.getBBuilder()
                    .setValue(parseDouble(b));
            }
            if (!isNull(c)) {
                s1.getCBuilder()
                    .setValue(parseDouble(c));
            }
            if (!isNull(q)) {
                s1.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                s1.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                s1.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
	    }
	    else if (type.equals("C1")) {
            ca.wise.fuel.proto.SpreadParmsC1.Parms.Builder c1 = builder.getC1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String height = data[dataIndex.value++];
            String cbh = data[dataIndex.value++];
            String cfl = data[dataIndex.value++];
            if (!isNull(a)) {
                c1.getABuilder()
                    .setValue(parseDouble(a));
            }
            if (!isNull(b)) {
                c1.getBBuilder()
                    .setValue(parseDouble(b));
            }
            if (!isNull(c)) {
                c1.getCBuilder()
                    .setValue(parseDouble(c));
            }
            if (!isNull(q)) {
                c1.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                c1.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                c1.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(height)) {
                c1.getHeightBuilder()
                    .setValue(parseDouble(height));
            }
            if (!isNull(cbh)) {
                c1.getCbhBuilder()
                    .setValue(parseDouble(cbh));
            }
            if (!isNull(cfl)) {
                c1.getCflBuilder()
                    .setValue(parseDouble(cfl));
            }
        }
        else if (type.equals("C6")) {
            ca.wise.fuel.proto.SpreadParmsC6.Parms.Builder c6 = builder.getC6Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String height = data[dataIndex.value++];
            String cbh = data[dataIndex.value++];
            String cfl = data[dataIndex.value++];
            if (!isNull(a)) {
                c6.getABuilder()
                    .setValue(parseDouble(a));
            }
            if (!isNull(b)) {
                c6.getBBuilder()
                    .setValue(parseDouble(b));
            }
            if (!isNull(c)) {
                c6.getCBuilder()
                    .setValue(parseDouble(c));
            }
            if (!isNull(q)) {
                c6.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                c6.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                c6.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(height)) {
                c6.getHeightBuilder()
                    .setValue(parseDouble(height));
            }
            if (!isNull(cbh)) {
                c6.getCbhBuilder()
                    .setValue(parseDouble(cbh));
            }
            if (!isNull(cfl)) {
                c6.getCflBuilder()
                    .setValue(parseDouble(cfl));
            }
        }
        else if (type.equals("D1")) {
            ca.wise.fuel.proto.SpreadParmsD1.Parms.Builder d1 = builder.getD1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String height = data[dataIndex.value++];
            String cbh = data[dataIndex.value++];
            String cfl = data[dataIndex.value++];
            if (!isNull(a)) {
                d1.getABuilder()
                    .setValue(parseDouble(a));
            }
            if (!isNull(b)) {
                d1.getBBuilder()
                    .setValue(parseDouble(b));
            }
            if (!isNull(c)) {
                d1.getCBuilder()
                    .setValue(parseDouble(c));
            }
            if (!isNull(q)) {
                d1.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                d1.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                d1.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(height)) {
                d1.getHeightBuilder()
                    .setValue(parseDouble(height));
            }
            if (!isNull(cbh)) {
                d1.getCbhBuilder()
                    .setValue(parseDouble(cbh));
            }
            if (!isNull(cfl)) {
                d1.getCflBuilder()
                    .setValue(parseDouble(cfl));
            }
        }
        else if (type.equals("Nz")) {
            ca.wise.fuel.proto.SpreadParmsNz.Parms.Builder nz = builder.getNzBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String height = data[dataIndex.value++];
            String cbh = data[dataIndex.value++];
            String cfl = data[dataIndex.value++];
            if (!isNull(a)) {
                nz.getABuilder()
                    .setValue(parseDouble(a));
            }
            if (!isNull(b)) {
                nz.getBBuilder()
                    .setValue(parseDouble(b));
            }
            if (!isNull(c)) {
                nz.getCBuilder()
                    .setValue(parseDouble(c));
            }
            if (!isNull(q)) {
                nz.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                nz.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                nz.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(height)) {
                nz.getHeightBuilder()
                    .setValue(parseDouble(height));
            }
            if (!isNull(cbh)) {
                nz.getCbhBuilder()
                    .setValue(parseDouble(cbh));
            }
            if (!isNull(cfl)) {
                nz.getCflBuilder()
                    .setValue(parseDouble(cfl));
            }
        }
        else if (type.equals("O1")) {
            ca.wise.fuel.proto.SpreadParmsO1.Parms.Builder o1 = builder.getO1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String curingDegree = data[dataIndex.value++];
            if (!isNull(a)) {
                o1.getABuilder()
                    .setValue(parseDouble(a));
            }
            if (!isNull(b)) {
                o1.getBBuilder()
                    .setValue(parseDouble(b));
            }
            if (!isNull(c)) {
                o1.getCBuilder()
                    .setValue(parseDouble(c));
            }
            if (!isNull(q)) {
                o1.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                o1.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                o1.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(curingDegree)) {
                o1.getCuringDegreeBuilder()
                    .setValue(parseDouble(curingDegree));
            }
        }
        else if (type.equals("O1ab")) {
            ca.wise.fuel.proto.SpreadParmsO1ab.Parms.Builder o1ab = builder.getO1AbBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String curingDegree = data[dataIndex.value++];
            String o1AbStandingA = data[dataIndex.value++];
            String o1AbStandingB = data[dataIndex.value++];
            String o1AbStandingC = data[dataIndex.value++];
            if (!isNull(a)) {
                o1ab.getABuilder()
                    .setValue(parseDouble(a));
            }
            if (!isNull(b)) {
                o1ab.getBBuilder()
                    .setValue(parseDouble(b));
            }
            if (!isNull(c)) {
                o1ab.getCBuilder()
                    .setValue(parseDouble(c));
            }
            if (!isNull(q)) {
                o1ab.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                o1ab.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                o1ab.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(curingDegree)) {
                o1ab.getCuringDegreeBuilder()
                    .setValue(parseDouble(curingDegree));
            }
            if (!isNull(o1AbStandingA)) {
                o1ab.getO1AbStandingABuilder()
                    .setValue(parseDouble(o1AbStandingA));
            }
            if (!isNull(o1AbStandingB)) {
                o1ab.getO1AbStandingBBuilder()
                    .setValue(parseDouble(o1AbStandingB));
            }
            if (!isNull(o1AbStandingC)) {
                o1ab.getO1AbStandingCBuilder()
                    .setValue(parseDouble(o1AbStandingC));
            }
        }
        else if (type.equals("Non")) {
            builder.getNonBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
        }
        else if (type.equals("Mix")) {
            ca.wise.fuel.proto.SpreadParmsMixed.Parms.Builder mix = builder.getMixedBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String height = data[dataIndex.value++];
            String cbh = data[dataIndex.value++];
            String cfl = data[dataIndex.value++];
            String pc = data[dataIndex.value++];
            if (!isNull(q)) {
                mix.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                mix.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                mix.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(height)) {
                mix.getHeightBuilder()
                    .setValue(parseDouble(height));
            }
            if (!isNull(cbh)) {
                mix.getCbhBuilder()
                    .setValue(parseDouble(cbh));
            }
            if (!isNull(cfl)) {
                mix.getCflBuilder()
                    .setValue(parseDouble(cfl));
            }
            if (!isNull(pc)) {
                mix.getPcBuilder()
                    .setValue(parseDouble(pc));
            }
            String agency = data[dataIndex.value++];
            //C1 may be null
            if (!isNull(agency)) {
                String fbp = data[dataIndex.value++];
                //skip over the index and the color data
                dataIndex.value += 5;
                parseLutDefinitionFuel(data, dataIndex, fbp, mix.getC2Builder()
                        .setVersion(1)
                        .getDataBuilder()
                        .setName(agency)
                        .getFuelBuilder());
            }
            agency = data[dataIndex.value++];
            //D1 may be null
            if (!isNull(agency)) {
                String fbp = data[dataIndex.value++];
                //skip over the index and the color data
                dataIndex.value += 5;
                parseLutDefinitionFuel(data, dataIndex, fbp, mix.getD1Builder()
                        .setVersion(1)
                        .getDataBuilder()
                        .setName(agency)
                        .getFuelBuilder());
            }
        }
        else if (type.equals("Dead")) {
            ca.wise.fuel.proto.SpreadParmsMixedDead.Parms.Builder mix = builder.getMixedDeadBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String q = data[dataIndex.value++];
            String bui0 = data[dataIndex.value++];
            String maxBe = data[dataIndex.value++];
            String height = data[dataIndex.value++];
            String cbh = data[dataIndex.value++];
            String cfl = data[dataIndex.value++];
            String pdf = data[dataIndex.value++];
            if (!isNull(q)) {
                mix.getQBuilder()
                    .setValue(parseDouble(q));
            }
            if (!isNull(bui0)) {
                mix.getBui0Builder()
                    .setValue(parseDouble(bui0));
            }
            if (!isNull(maxBe)) {
                mix.getMaxBeBuilder()
                    .setValue(parseDouble(maxBe));
            }
            if (!isNull(height)) {
                mix.getHeightBuilder()
                    .setValue(parseDouble(height));
            }
            if (!isNull(cbh)) {
                mix.getCbhBuilder()
                    .setValue(parseDouble(cbh));
            }
            if (!isNull(cfl)) {
                mix.getCflBuilder()
                    .setValue(parseDouble(cfl));
            }
            if (!isNull(pdf)) {
                mix.getPdfBuilder()
                    .setValue(parseDouble(pdf));
            }
            String agency = data[dataIndex.value++];
            //C1 may be null
            if (!isNull(agency)) {
                String fbp = data[dataIndex.value++];
                //skip over the index and the color data
                dataIndex.value += 5;
                parseLutDefinitionFuel(data, dataIndex, fbp, mix.getC2Builder()
                        .setVersion(1)
                        .getDataBuilder()
                        .setName(agency)
                        .getFuelBuilder());
            }
            agency = data[dataIndex.value++];
            //D1 may be null
            if (!isNull(agency)) {
                String fbp = data[dataIndex.value++];
                //skip over the index and the color data
                dataIndex.value += 5;
                parseLutDefinitionFuel(data, dataIndex, fbp, mix.getD1Builder()
                        .setVersion(1)
                        .getDataBuilder()
                        .setName(agency)
                        .getFuelBuilder());
            }
        }
        else
            throw new IOException("Unknown spread parms attribute type (" + type + ").");
	}
    
    /**
     * FMC parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the FMC data in.
     */
    private void parseLutDefinitionFmc(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.FmcAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("Calc")) {
            ca.wise.fuel.proto.FmcCalc.Parms.Builder fmc = builder.getCalcBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String day0 = data[dataIndex.value++];
            if (!isNull(day0)) {
                fmc.setDay0(parseInteger(day0));
            }
        }
        else if (type.equals("No")) {
            builder.getNoCalcBuilder()
                .setVersion(1)
                .getParmsBuilder()
                .setVersion(1);
        }
        else
            throw new IOException("Unknown FMC attribute type (" + type + ").");
    }
    
    /**
     * SFC parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the SFC data in.
     */
    private void parseLutDefinitionSfc(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.SfcAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("C1")) {
            ca.wise.fuel.proto.SfcC1.Parms.Builder sfc = builder.getC1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1 = data[dataIndex.value++];
            String p2 = data[dataIndex.value++];
            String p3 = data[dataIndex.value++];
            String p4 = data[dataIndex.value++];
            String multiplier = data[dataIndex.value++];
            if (!isNull(p1)) {
                sfc.getP1Builder()
                    .setValue(parseInteger(p1));
            }
            if (!isNull(p2)) {
                sfc.getP2Builder()
                    .setValue(parseInteger(p2));
            }
            if (!isNull(p3)) {
                sfc.getP3Builder()
                    .setValue(parseInteger(p3));
            }
            if (!isNull(p4)) {
                sfc.getP4Builder()
                    .setValue(parseInteger(p4));
            }
            if (!isNull(multiplier)) {
                sfc.getMultiplierBuilder()
                    .setValue(parseInteger(multiplier));
            }
        }
        else if (type.equals("C2")) {
            ca.wise.fuel.proto.SfcC2.Parms.Builder sfc = builder.getC2Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1 = data[dataIndex.value++];
            String p2 = data[dataIndex.value++];
            String power = data[dataIndex.value++];
            String multiplier = data[dataIndex.value++];
            if (!isNull(p1)) {
                sfc.getP1Builder()
                    .setValue(parseInteger(p1));
            }
            if (!isNull(p2)) {
                sfc.getP2Builder()
                    .setValue(parseInteger(p2));
            }
            if (!isNull(power)) {
                sfc.getPowerBuilder()
                    .setValue(parseInteger(power));
            }
            if (!isNull(multiplier)) {
                sfc.getMultiplierBuilder()
                    .setValue(parseInteger(multiplier));
            }
        }
        else if (type.equals("C7")) {
            ca.wise.fuel.proto.SfcC7.Parms.Builder sfc = builder.getC7Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1F = data[dataIndex.value++];
            String p2F = data[dataIndex.value++];
            String p3F = data[dataIndex.value++];
            String p1W = data[dataIndex.value++];
            String p2W = data[dataIndex.value++];
            String ffcMultiplier = data[dataIndex.value++];
            String wfcMultiplier = data[dataIndex.value++];
            String sfcMultiplier = data[dataIndex.value++];
            if (!isNull(p1F)) {
                sfc.getP1FBuilder()
                    .setValue(parseInteger(p1F));
            }
            if (!isNull(p2F)) {
                sfc.getP2FBuilder()
                .setValue(parseInteger(p2F));
            }
            if (!isNull(p3F)) {
                sfc.getP3FBuilder()
                .setValue(parseInteger(p3F));
            }
            if (!isNull(p1W)) {
                sfc.getP1WBuilder()
                    .setValue(parseInteger(p1W));
            }
            if (!isNull(p2W)) {
                sfc.getP2WBuilder()
                    .setValue(parseInteger(p2W));
            }
            if (!isNull(ffcMultiplier)) {
                sfc.getFfcMultiplierBuilder()
                    .setValue(parseInteger(ffcMultiplier));
            }
            if (!isNull(wfcMultiplier)) {
                sfc.getWfcMultiplierBuilder()
                    .setValue(parseInteger(wfcMultiplier));
            }
            if (!isNull(sfcMultiplier)) {
                sfc.getSfcMultiplierBuilder()
                    .setValue(parseInteger(sfcMultiplier));
            }
        }
        else if (type.equals("D2")) {
            ca.wise.fuel.proto.SfcD2.Parms.Builder sfc = builder.getD2Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1 = data[dataIndex.value++];
            String p2 = data[dataIndex.value++];
            String power = data[dataIndex.value++];
            String multiplier = data[dataIndex.value++];
            String threshold = data[dataIndex.value++];
            String scale1 = data[dataIndex.value++];
            String scale2 = data[dataIndex.value++];
            if (!isNull(p1)) {
                sfc.getP1Builder()
                    .setValue(parseInteger(p1));
            }
            if (!isNull(p2)) {
                sfc.getP2Builder()
                .setValue(parseInteger(p2));
            }
            if (!isNull(power)) {
                sfc.getPowerBuilder()
                .setValue(parseInteger(power));
            }
            if (!isNull(multiplier)) {
                sfc.getMultiplierBuilder()
                    .setValue(parseInteger(multiplier));
            }
            if (!isNull(threshold)) {
                sfc.getThresholdBuilder()
                    .setValue(parseInteger(threshold));
            }
            if (!isNull(scale1)) {
                sfc.getScale1Builder()
                    .setValue(parseInteger(scale1));
            }
            if (!isNull(scale2)) {
                sfc.getScale2Builder()
                    .setValue(parseInteger(scale2));
            }
        }
        else if (type.equals("M1")) {
            builder.getM1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
        }
        else if (type.equals("O1")) {
            ca.wise.fuel.proto.SfcO1.Parms.Builder sfc = builder.getO1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String fuelLoad = data[dataIndex.value++];
            if (!isNull(fuelLoad)) {
                sfc.getFuelLoadBuilder()
                    .setValue(parseInteger(fuelLoad));
            }
        }
        else if (type.equals("S1")) {
            ca.wise.fuel.proto.SfcS1.Parms.Builder sfc = builder.getS1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1F = data[dataIndex.value++];
            String p2F = data[dataIndex.value++];
            String p1W = data[dataIndex.value++];
            String p2W = data[dataIndex.value++];
            String ffcMultiplier = data[dataIndex.value++];
            String wfcMultiplier = data[dataIndex.value++];
            String sfcMultiplier = data[dataIndex.value++];
            String ffcBuiMultiplier = data[dataIndex.value++];
            String wfcBuiMultiplier = data[dataIndex.value++];
            if (!isNull(p1F)) {
                sfc.getP1FBuilder()
                    .setValue(parseInteger(p1F));
            }
            if (!isNull(p2F)) {
                sfc.getP2FBuilder()
                .setValue(parseInteger(p2F));
            }
            if (!isNull(p1W)) {
                sfc.getP1WBuilder()
                    .setValue(parseInteger(p1W));
            }
            if (!isNull(p2W)) {
                sfc.getP2WBuilder()
                    .setValue(parseInteger(p2W));
            }
            if (!isNull(ffcMultiplier)) {
                sfc.getFfcMultiplierBuilder()
                    .setValue(parseInteger(ffcMultiplier));
            }
            if (!isNull(wfcMultiplier)) {
                sfc.getWfcMultiplierBuilder()
                    .setValue(parseInteger(wfcMultiplier));
            }
            if (!isNull(sfcMultiplier)) {
                sfc.getSfcMultiplierBuilder()
                    .setValue(parseInteger(sfcMultiplier));
            }
            if (!isNull(ffcBuiMultiplier)) {
                sfc.getFfcBuiMultiplierBuilder()
                    .setValue(parseInteger(ffcBuiMultiplier));
            }
            if (!isNull(wfcBuiMultiplier)) {
                sfc.getWfcBuiMultiplierBuilder()
                    .setValue(parseInteger(wfcBuiMultiplier));
            }
        }
        else
            throw new IOException("Unknown SFC attribute type (" + type + ").");
    }
    
    /**
     * RSI parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the RSI data in.
     */
    private void parseLutDefinitionRsi(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.RsiAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("C1")) {
            builder.getC1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
        }
        else if (type.equals("C6")) {
            ca.wise.fuel.proto.RsiC6.Parms.Builder rsi = builder.getC6Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String fmeMultiplier = data[dataIndex.value++];
            String fmePowAdder = data[dataIndex.value++];
            String fmePowMultiplier = data[dataIndex.value++];
            String fmeDivAdder = data[dataIndex.value++];
            String fmeDivMultiplier = data[dataIndex.value++];
            String fmePower = data[dataIndex.value++];
            String rscMultiplier = data[dataIndex.value++];
            String rscExpMultiplier = data[dataIndex.value++];
            String fmeAvg = data[dataIndex.value++];
            if (!isNull(fmeMultiplier)) {
                rsi.getFmeMultiplierBuilder()
                    .setValue(parseInteger(fmeMultiplier));
            }
            if (!isNull(fmePowAdder)) {
                rsi.getFmePowAdderBuilder()
                    .setValue(parseInteger(fmePowAdder));
            }
            if (!isNull(fmePowMultiplier)) {
                rsi.getFmePowMultiplierBuilder()
                    .setValue(parseInteger(fmePowMultiplier));
            }
            if (!isNull(fmeDivAdder)) {
                rsi.getFmeDivAdderBuilder()
                    .setValue(parseInteger(fmeDivAdder));
            }
            if (!isNull(fmeDivMultiplier)) {
                rsi.getFmeDivMultiplierBuilder()
                    .setValue(parseInteger(fmeDivMultiplier));
            }
            if (!isNull(fmePower)) {
                rsi.getFmePowerBuilder()
                    .setValue(parseInteger(fmePower));
            }
            if (!isNull(rscMultiplier)) {
                rsi.getRscMultiplierBuilder()
                    .setValue(parseInteger(rscMultiplier));
            }
            if (!isNull(rscExpMultiplier)) {
                rsi.getRscExpMultiplierBuilder()
                    .setValue(parseInteger(rscExpMultiplier));
            }
            if (!isNull(fmeAvg)) {
                rsi.getFmeAvgBuilder()
                    .setValue(parseInteger(fmeAvg));
            }
        }
        else if (type.equals("D2")) {
            ca.wise.fuel.proto.RsiD2.Parms.Builder rsi = builder.getD2Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String threshold = data[dataIndex.value++];
            String scale1 = data[dataIndex.value++];
            String scale2 = data[dataIndex.value++];
            if (!isNull(threshold)) {
                rsi.getThresholdBuilder()
                    .setValue(parseInteger(threshold));
            }
            if (!isNull(scale1)) {
                rsi.getScale1Builder()
                    .setValue(parseInteger(scale1));
            }
            if (!isNull(scale2)) {
                rsi.getScale2Builder()
                    .setValue(parseInteger(scale2));
            }
        }
        else if (type.equals("M1")) {
            ca.wise.fuel.proto.RsiM1.Parms.Builder rsi = builder.getM1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1 = data[dataIndex.value++];
            if (!isNull(p1)) {
                rsi.getP1Builder()
                    .setValue(parseInteger(p1));
            }
        }
        else if (type.equals("M3")) {
            ca.wise.fuel.proto.RsiM3.Parms.Builder rsi = builder.getM3Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String p = data[dataIndex.value++];
            if (!isNull(a)) {
                rsi.getABuilder()
                    .setValue(parseInteger(a));
            }
            if (!isNull(b)) {
                rsi.getBBuilder()
                    .setValue(parseInteger(b));
            }
            if (!isNull(c)) {
                rsi.getCBuilder()
                    .setValue(parseInteger(c));
            }
            if (!isNull(p)) {
                rsi.getPBuilder()
                    .setValue(parseInteger(p));
            }
        }
        else if (type.equals("M4")) {
            ca.wise.fuel.proto.RsiM4.Parms.Builder rsi = builder.getM4Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            String d1A = data[dataIndex.value++];
            String d1B = data[dataIndex.value++];
            String d1C = data[dataIndex.value++];
            String p = data[dataIndex.value++];
            if (!isNull(a)) {
                rsi.getABuilder()
                    .setValue(parseInteger(a));
            }
            if (!isNull(b)) {
                rsi.getBBuilder()
                    .setValue(parseInteger(b));
            }
            if (!isNull(c)) {
                rsi.getCBuilder()
                    .setValue(parseInteger(c));
            }
            if (!isNull(d1A)) {
                rsi.getD1ABuilder()
                    .setValue(parseInteger(d1A));
            }
            if (!isNull(d1B)) {
                rsi.getD1BBuilder()
                    .setValue(parseInteger(d1B));
            }
            if (!isNull(d1C)) {
                rsi.getD1CBuilder()
                    .setValue(parseInteger(d1C));
            }
            if (!isNull(p)) {
                rsi.getPBuilder()
                    .setValue(parseInteger(p));
            }
        }
        else if (type.equals("O1")) {
            ca.wise.fuel.proto.RsiO1.Parms.Builder rsi = builder.getO1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String threshold = data[dataIndex.value++];
            String f1 = data[dataIndex.value++];
            String f2 = data[dataIndex.value++];
            String f3 = data[dataIndex.value++];
            String f4 = data[dataIndex.value++];
            if (!isNull(threshold)) {
                rsi.getThresholdBuilder()
                    .setValue(parseInteger(threshold));
            }
            if (!isNull(f1)) {
                rsi.getF1Builder()
                    .setValue(parseInteger(f1));
            }
            if (!isNull(f2)) {
                rsi.getF2Builder()
                    .setValue(parseInteger(f2));
            }
            if (!isNull(f3)) {
                rsi.getF3Builder()
                    .setValue(parseInteger(f3));
            }
            if (!isNull(f4)) {
                rsi.getF4Builder()
                    .setValue(parseInteger(f4));
            }
        }
        else if (type.equals("O1")) {
            ca.wise.fuel.proto.RsiConstant.Parms.Builder rsi = builder.getConstantBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String val = data[dataIndex.value++];
            if (!isNull(val)) {
                rsi.getRsiBuilder()
                    .setValue(parseInteger(val));
            }
        }
        else
            throw new IOException("Unknown RSI attribute type (" + type + ").");
    }
    
    /**
     * ISF parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the ISF data in.
     */
    private void parseLutDefinitionIsf(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.IsfAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("C1")) {
            builder.getC1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
        }
        else if (type.equals("M1")) {
            builder.getM1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
        }
        else if (type.equals("M3M4")) {
            ca.wise.fuel.proto.IsfM3M4.Parms.Builder isf = builder.getM3M4Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String a = data[dataIndex.value++];
            String b = data[dataIndex.value++];
            String c = data[dataIndex.value++];
            if (!isNull(a)) {
                isf.getABuilder()
                    .setValue(parseInteger(a));
            }
            if (!isNull(b)) {
                isf.getBBuilder()
                    .setValue(parseInteger(b));
            }
            if (!isNull(c)) {
                isf.getCBuilder()
                    .setValue(parseInteger(c));
            }
        }
        else if (type.equals("O1")) {
            ca.wise.fuel.proto.IsfO1.Parms.Builder isf = builder.getO1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String threshold = data[dataIndex.value++];
            String f1 = data[dataIndex.value++];
            String f2 = data[dataIndex.value++];
            String f3 = data[dataIndex.value++];
            String f4 = data[dataIndex.value++];
            if (!isNull(threshold)) {
                isf.getThresholdBuilder()
                    .setValue(parseInteger(threshold));
            }
            if (!isNull(f1)) {
                isf.getF1Builder()
                    .setValue(parseInteger(f1));
            }
            if (!isNull(f2)) {
                isf.getF2Builder()
                    .setValue(parseInteger(f2));
            }
            if (!isNull(f3)) {
                isf.getF3Builder()
                    .setValue(parseInteger(f3));
            }
            if (!isNull(f4)) {
                isf.getF4Builder()
                    .setValue(parseInteger(f4));
            }
        }
        else
            throw new IOException("Unknown ISF attribute type (" + type + ").");
    }
    
    /**
     * Acc alpha parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the acc alpha data in.
     */
    private void parseLutDefinitionAccAlpha(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.AccAlphaAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("Close")) {
            ca.wise.fuel.proto.AccAlphaClosed.Parms.Builder acc = builder.getAlphaClosedBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String init = data[dataIndex.value++];
            String multiplier = data[dataIndex.value++];
            String power = data[dataIndex.value++];
            String expMultiplier = data[dataIndex.value++];
            if (!isNull(init)) {
                acc.getInitBuilder()
                    .setValue(parseInteger(init));
            }
            if (!isNull(multiplier)) {
                acc.getMultiplierBuilder()
                    .setValue(parseInteger(multiplier));
            }
            if (!isNull(power)) {
                acc.getPowerBuilder()
                    .setValue(parseInteger(power));
            }
            if (!isNull(expMultiplier)) {
                acc.getExpMultiplierBuilder()
                    .setValue(parseInteger(expMultiplier));
            }
        }
        else if (type.equals("Open")) {
            ca.wise.fuel.proto.AccAlphaOpen.Parms.Builder acc = builder.getAlphaOpenBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String init = data[dataIndex.value++];
            if (!isNull(init)) {
                acc.getInitBuilder()
                    .setValue(parseInteger(init));
            }
        }
        else
            throw new IOException("Unknown Acc Alpha attribute type (" + type + ").");
    }
    
    /**
     * LB parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the LB data in.
     */
    private void parseLutDefinitionLb(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.LbAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("C1")) {
            ca.wise.fuel.proto.LbC1.Parms.Builder lb = builder.getC1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String init = data[dataIndex.value++];
            String multiplier = data[dataIndex.value++];
            String expMultiplier = data[dataIndex.value++];
            String power = data[dataIndex.value++];
            if (!isNull(init)) {
                lb.getInitBuilder()
                    .setValue(parseInteger(init));
            }
            if (!isNull(multiplier)) {
                lb.getMultiplierBuilder()
                    .setValue(parseInteger(multiplier));
            }
            if (!isNull(expMultiplier)) {
                lb.getExpMultiplierBuilder()
                    .setValue(parseInteger(expMultiplier));
            }
            if (!isNull(power)) {
                lb.getPowerBuilder()
                    .setValue(parseInteger(power));
            }
        }
        else if (type.equals("O1")) {
            ca.wise.fuel.proto.LbO1.Parms.Builder lb = builder.getO1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String init = data[dataIndex.value++];
            String power = data[dataIndex.value++];
            if (!isNull(init)) {
                lb.getInitBuilder()
                    .setValue(parseInteger(init));
            }
            if (!isNull(power)) {
                lb.getPowerBuilder()
                    .setValue(parseInteger(power));
            }
        }
        else
            throw new IOException("Unknown LB attribute type (" + type + ").");
    }
    
    /**
     * LB parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the LB data in.
     */
    private void parseLutDefinitionCfb(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.CfbAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("C1")) {
            ca.wise.fuel.proto.CfbC1.Parms.Builder cfb = builder.getC1Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String csiMultiplier = data[dataIndex.value++];
            String csiCbhExponent = data[dataIndex.value++];
            String csiExpAdder = data[dataIndex.value++];
            String csiExpMultiplier = data[dataIndex.value++];
            String csiPower = data[dataIndex.value++];
            String rsoDiv = data[dataIndex.value++];
            String cfbExp = data[dataIndex.value++];
            String cfbPossible = data[dataIndex.value++];
            if (!isNull(csiMultiplier)) {
                cfb.getCsiMultiplierBuilder()
                    .setValue(parseInteger(csiMultiplier));
            }
            if (!isNull(csiCbhExponent)) {
                cfb.getCsiCbhExponentBuilder()
                    .setValue(parseInteger(csiCbhExponent));
            }
            if (!isNull(csiExpAdder)) {
                cfb.getCsiExpAdderBuilder()
                    .setValue(parseInteger(csiExpAdder));
            }
            if (!isNull(csiExpMultiplier)) {
                cfb.getCsiExpMultiplierBuilder()
                    .setValue(parseInteger(csiExpMultiplier));
            }
            if (!isNull(csiPower)) {
                cfb.getCsiPowerBuilder()
                    .setValue(parseInteger(csiPower));
            }
            if (!isNull(rsoDiv)) {
                cfb.getRsoDivBuilder()
                    .setValue(parseInteger(rsoDiv));
            }
            if (!isNull(cfbExp)) {
                cfb.getCfbExpBuilder()
                    .setValue(parseInteger(cfbExp));
            }
            if (!isNull(cfbPossible)) {
                cfb.setCfbPossible(parseBoolean(cfbPossible));
            }
        }
        else if (type.equals("D2")) {
            builder.getD2Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
        }
        else
            throw new IOException("Unknown LB attribute type (" + type + ").");
    }
    
    /**
     * Flame length parm data from a fuel definition.
     * @param data The fuel definition.
     * @param dataIndex The current index in the fuel definition.
     * @param builder The builder to place the flame length data in.
     */
    private void parseLutDefinitionFl(String[] data, IntegerHelper dataIndex, ca.wise.fuel.proto.FlameLengthAttribute.Builder builder) throws IOException {
        String type = data[dataIndex.value++];
        if (type.equals("Alex")) {
            ca.wise.fuel.proto.FlameLengthAlexander82.Parms.Builder fl = builder.getAlexander82Builder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1 = data[dataIndex.value++];
            String p2 = data[dataIndex.value++];
            if (!isNull(p1)) {
                fl.getP1Builder()
                    .setValue(parseInteger(p1));
            }
            if (!isNull(p2)) {
                fl.getP2Builder()
                    .setValue(parseInteger(p2));
            }
        }
        else if (type.equals("AlexTree")) {
            ca.wise.fuel.proto.FlameLengthAlexander82Tree.Parms.Builder fl = builder.getAlexander82TreeBuilder()
                    .setVersion(1)
                    .getParmsBuilder()
                    .setVersion(1);
            String p1 = data[dataIndex.value++];
            String p2 = data[dataIndex.value++];
            String cfb = data[dataIndex.value++];
            String th = data[dataIndex.value++];
            if (!isNull(p1)) {
                fl.getP1Builder()
                    .setValue(parseInteger(p1));
            }
            if (!isNull(p2)) {
                fl.getP2Builder()
                    .setValue(parseInteger(p2));
            }
            if (!isNull(cfb)) {
                fl.getCfbBuilder()
                    .setValue(parseInteger(cfb));
            }
            if (!isNull(th)) {
                fl.getThBuilder()
                    .setValue(parseInteger(th));
            }
        }
        else
            throw new IOException("Unknown LB attribute type (" + type + ").");
    }
	
    protected void parseLutDefinitionFuel(String[] data, IntegerHelper dataIndex, String fbp, FbpFuel.Builder fbpBuilder) throws IOException {
        
        fbpBuilder.setDefaultFuelType(findFuelName(fbp));
        
        while (dataIndex.value < data.length) {
            String dataType = data[dataIndex.value++];
            //spread parms
            if (dataType.equals("SP")) {
                parseLutDefinitionSpreadParms(data, dataIndex, fbpBuilder.getSpreadBuilder().setVersion(1));
            }
            //FMC attributes
            else if (dataType.equals("FMC")) {
                parseLutDefinitionFmc(data, dataIndex, fbpBuilder.getFmcCalculationBuilder().setVersion(1));
            }
            //SFC attributes
            else if (dataType.equals("SFC")) {
                parseLutDefinitionSfc(data, dataIndex, fbpBuilder.getSfcCalculationBuilder().setVersion(1));
            }
            //SFC greenup attributes
            else if (dataType.equals("SFCG")) {
                parseLutDefinitionSfc(data, dataIndex, fbpBuilder.getSfcCalculationGreenupBuilder().setVersion(1));
            }
            //RSI attributes
            else if (dataType.equals("RSI")) {
                parseLutDefinitionRsi(data, dataIndex, fbpBuilder.getRsiCalculationBuilder().setVersion(1));
            }
            //RSI greenup attributes
            else if (dataType.equals("RSIG")) {
                parseLutDefinitionRsi(data, dataIndex, fbpBuilder.getRsiCalculationGreenupBuilder().setVersion(1));
            }
            //ISF attributes
            else if (dataType.equals("ISF")) {
                parseLutDefinitionIsf(data, dataIndex, fbpBuilder.getIsfCalculationBuilder().setVersion(1));
            }
            //ISF greenup attributes
            else if (dataType.equals("ISFG")) {
                parseLutDefinitionIsf(data, dataIndex, fbpBuilder.getIsfCalculationGreenupBuilder().setVersion(1));
            }
            //Acc Alpha attributes
            else if (dataType.equals("AA")) {
                parseLutDefinitionAccAlpha(data, dataIndex, fbpBuilder.getAccAlphaCalculationBuilder().setVersion(1));
            }
            //LB attributes
            else if (dataType.equals("LB")) {
                parseLutDefinitionLb(data, dataIndex, fbpBuilder.getLbCalculationBuilder().setVersion(1));
            }
            //CFB attributes
            else if (dataType.equals("CFB")) {
                parseLutDefinitionCfb(data, dataIndex, fbpBuilder.getCfbCalculationBuilder().setVersion(1));
            }
            //CFB greenup attributes
            else if (dataType.equals("CFBG")) {
                parseLutDefinitionCfb(data, dataIndex, fbpBuilder.getCfbCalculationGreenupBuilder().setVersion(1));
            }
            //Flame length attributes
            else if (dataType.equals("FL")) {
                parseLutDefinitionFl(data, dataIndex, fbpBuilder.getFlCalculationBuilder().setVersion(1));
            }
        }
        
        List<FuelOption> userOptions = findFuelOptions(fbp);
        if (userOptions != null && userOptions.size() > 0) {
            for (FuelOption opt : userOptions) {
                switch (opt.getOptionType()) {
                case PERCENT_CONIFER:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.M1 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.M2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz54 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz69) {
                        //the type has already been initialized with values from the fuel definition
                        if (fbpBuilder.getSpreadBuilder()
                                .getMixedBuilder()
                                .getMsgCase() == ca.wise.fuel.proto.SpreadParmsMixed.MsgCase.PARMS) {
                            fbpBuilder.getSpreadBuilder()
                                .getMixedBuilder()
                                .getParmsBuilder()
                                .getPcBuilder()
                                .setValue(opt.getValue());
                        }
                        //the type has no existing data, set the defaults
                        else
                            fbpBuilder.getSpreadBuilder()
                                .setVersion(1)
                                .getMixedBuilder()
                                .setVersion(1)
                                .getDefaultBuilder()
                                .setDefault(fbpBuilder.getDefaultFuelType())
                                .getPcBuilder()
                                .setValue(opt.getValue());
                    }
                    else if (fbpBuilder.getDefaultFuelType() == FuelName.M1M2) {
                        //the type has already been initialized with values from the fuel definition
                        if (fbpBuilder.getSpreadBuilder()
                                .getMixedBuilder()
                                .getMsgCase() == ca.wise.fuel.proto.SpreadParmsMixed.MsgCase.PARMS) {
                            fbpBuilder.getSpreadBuilder()
                                .getMixedBuilder()
                                .getParmsBuilder()
                                .getPcBuilder()
                                .setValue(opt.getValue());
                        }
                        //the type has no existing data, set the defaults
                        else
                            fbpBuilder.getSpreadBuilder()
                                .setVersion(1)
                                .getMixedBuilder()
                                .setVersion(1)
                                .getDefaultBuilder()
                                .setDefault(FuelName.M1)
                                .getPcBuilder()
                                .setValue(opt.getValue());
                    }
                    break;
                case PERCENT_DEAD_FIR:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.M3 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.M4) {
                        //the type has already been initialized with values from the fuel definition
                        if (fbpBuilder.getSpreadBuilder()
                                .getMixedDeadBuilder()
                                .getMsgCase() == ca.wise.fuel.proto.SpreadParmsMixedDead.MsgCase.PARMS) {
                            fbpBuilder.getSpreadBuilder()
                                .getMixedDeadBuilder()
                                .getParmsBuilder()
                                .getPdfBuilder()
                                .setValue(opt.getValue());
                        }
                        //the type has no existing data, set the defaults
                        else
                            fbpBuilder.getSpreadBuilder()
                                .setVersion(1)
                                .getMixedDeadBuilder()
                                .setVersion(1)
                                .getDefaultBuilder()
                                .setDefault(fbpBuilder.getDefaultFuelType())
                                .getPdfBuilder()
                                .setValue(opt.getValue());
                    }
                    else if (fbpBuilder.getDefaultFuelType() == FuelName.M3M4) {
                        //the type has already been initialized with values from the fuel definition
                        if (fbpBuilder.getSpreadBuilder()
                                .getMixedDeadBuilder()
                                .getMsgCase() == ca.wise.fuel.proto.SpreadParmsMixedDead.MsgCase.PARMS) {
                            fbpBuilder.getSpreadBuilder()
                                .getMixedDeadBuilder()
                                .getParmsBuilder()
                                .getPdfBuilder()
                                .setValue(opt.getValue());
                        }
                        //the type has no existing data, set the defaults
                        else
                            fbpBuilder.getSpreadBuilder()
                                .setVersion(1)
                                .getMixedDeadBuilder()
                                .setVersion(1)
                                .getDefaultBuilder()
                                .setDefault(FuelName.M3)
                                .getPdfBuilder()
                                .setValue(opt.getValue());
                    }
                    break;
                case CROWN_BASE_HEIGHT:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.C1 || 
                            fbpBuilder.getDefaultFuelType() == FuelName.C6 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz60 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz61 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz66 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz67 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz71) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getC6Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCbh(HSS_Double.of(opt.getValue()));
                    }
                    break;
                case CROWN_FUEL_LOAD:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.C1 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.C6 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz60 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz61 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz66 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz67 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz71) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getC6Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCfl(HSS_Double.of(opt.getValue()));
                    }
                    break;
                case GRASS_CURING:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.O1A ||
                            fbpBuilder.getDefaultFuelType() == FuelName.O1B ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz15 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz30 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz31 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz32 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz33 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz40 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz41 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz43 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz46 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz50 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz53 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz62 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz63 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz65) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getO1Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCuringDegree(HSS_Double.of(opt.getValue()));

                        if (!fbpBuilder.getSfcCalculationBuilder()
                                .getO1Builder()
                                .getParmsBuilder()
                                .hasFuelLoad()) {
                            fbpBuilder.getSfcCalculationBuilder()
                                .setVersion(1)
                                .getO1Builder()
                                .setVersion(1)
                                .getParmsBuilder()
                                .setVersion(1)
                                .setFuelLoad(HSS_Double.of(0.35));
                        }
                    }
                    break;
                case GRASS_FUEL_LOAD:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.O1A ||
                            fbpBuilder.getDefaultFuelType() == FuelName.O1B ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz15 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz30 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz31 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz32 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz33 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz40 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz41 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz43 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz46 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz50 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz53 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz62 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz63 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz65) {
                        fbpBuilder.getSfcCalculationBuilder()
                            .setVersion(1)
                            .getO1Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setFuelLoad(HSS_Double.of(opt.getValue()));
                        
                        if (!fbpBuilder.getSpreadBuilder()
                                .getO1Builder()
                                .getParmsBuilder()
                                .hasCuringDegree()) {
                            fbpBuilder.getSpreadBuilder()
                                .setVersion(1)
                                .getO1Builder()
                                .setVersion(1)
                                .getParmsBuilder()
                                .setVersion(1)
                                .setCuringDegree(HSS_Double.of(0.60));
                        }
                    }
                default:
                	break;
                }
            }
        }
	}
	
	private static class OptionData {
		public double value;
		
		public OptionType type = OptionType.NONE;
		
		public String text;
		
		public OptionData(String text, OptionType type) { this.text = text; this.type = type; }
		
		public enum OptionType {
			PERCENT_CONIFER,
			PERCENT_DEAD_FIR,
			CROWN_BASE_HEIGHT,
			GRASS_CURING,
			FUEL_LOAD,
			CROWN_FUEL_LOAD,
			NONE
		}
	}
	
	/**
	 * Try to parse optional settings from the bracketed
	 * portion of a fuel name.
	 * @param options The bracketed portion of a fuel name.
	 * @param fuelType The type of fuel that the options are for (for parsing
	 *        options with no specified name).
	 * @return The parsed optional settings.
	 */
	private List<OptionData> parseOptionData(String options, FuelName fuelType) {
		List<OptionData> retval = new ArrayList<OptionData>();
		String test = options.toLowerCase();
		if (test.contains("conifer") || test.contains("pc"))
		    retval.add(new OptionData(test, OptionData.OptionType.PERCENT_CONIFER));
		else if (test.contains("dead fir") || test.contains("pdf"))
            retval.add(new OptionData(test, OptionData.OptionType.PERCENT_DEAD_FIR));
		else {
		    //assume CBH if the fuel type is C6
		    if (fuelType == FuelName.C6)
	            retval.add(new OptionData(test, OptionData.OptionType.CROWN_BASE_HEIGHT));
		    //assume percent conifer for M1 and M2
		    else if (fuelType == FuelName.M1 || fuelType == FuelName.M2)
	            retval.add(new OptionData(test, OptionData.OptionType.PERCENT_CONIFER));
		    //assume percent dead fir for M3 and M4
		    else if (fuelType == FuelName.M3 || fuelType == FuelName.M4)
	            retval.add(new OptionData(test, OptionData.OptionType.PERCENT_DEAD_FIR));
		    //if this is an O-1 fuel
		    else if (fuelType == FuelName.O1A || fuelType == FuelName.O1B || fuelType == FuelName.O1AB) {
                //assume grass curing only if a % sign is present
		        if (test.contains("%"))
                    retval.add(new OptionData(test, OptionData.OptionType.GRASS_CURING));
		        //if there is a comma, multiple attributes may be present
                else {
		            String[] split = test.split(",");
		            for (String s : split) {
		                if (s.contains("gc"))
		                    retval.add(new OptionData(s, OptionData.OptionType.GRASS_CURING));
		                else if (s.contains("fl"))
		                    retval.add(new OptionData(s, OptionData.OptionType.FUEL_LOAD));
		            }
		        }
		    }
		}
		
		for (OptionData val : retval) {
			String value = "";
			boolean found = false;
			for (int i = 0; i < val.text.length(); i++) {
				char c = val.text.charAt(i);
				if ((c >= '0' && c <= '9') || c == '.') {
					value += c;
					found = true;
				}
				else if (found)
					break;
			}
			if (value.length() > 0) {
				try {
				    val.value = Double.parseDouble(value);
				    //normalize percent to decimal percent
				    if (val.type == OptionData.OptionType.GRASS_CURING) {
				        if (val.value > 1.0)
				            val.value = val.value / 100.0;
				    }
				}
				catch (NumberFormatException e) {
				    val.type = OptionData.OptionType.NONE;
				}
			}
			else
			    val.type = OptionData.OptionType.NONE;
		}
		return retval;
	}
    
    /**
     * Apply options from the LUT itself as well as ones specified by the user to the fuel types.
     * @param fbpBuilder The fuel type protobuf definition.
     * @param userOptions Options created by the user.
     * @param options Options specified for the fuel type in the LUT file.
     */
    protected void applyLutOptions(FbpFuel.Builder fbpBuilder, List<FuelOption> userOptions, String options) {
        if (userOptions != null && userOptions.size() > 0) {
            for (FuelOption opt : userOptions) {
                switch (opt.getOptionType()) {
                case PERCENT_CONIFER:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.M1 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.M2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz54 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz69) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(fbpBuilder.getDefaultFuelType())
                            .getPcBuilder()
                            .setValue(opt.getValue());
                    }
                    else if (fbpBuilder.getDefaultFuelType() == FuelName.M1M2) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(FuelName.M1)
                            .getPcBuilder()
                            .setValue(opt.getValue());
                    }
                    break;
                case PERCENT_DEAD_FIR:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.M3 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.M4) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedDeadBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(fbpBuilder.getDefaultFuelType())
                            .getPdfBuilder()
                            .setValue(opt.getValue());
                    }
                    else if (fbpBuilder.getDefaultFuelType() == FuelName.M3M4) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedDeadBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(FuelName.M3)
                            .getPdfBuilder()
                            .setValue(opt.getValue());
                    }
                    break;
                case CROWN_BASE_HEIGHT:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.C1 || 
                            fbpBuilder.getDefaultFuelType() == FuelName.C6 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz60 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz61 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz66 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz67 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz71) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getC6Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCbh(HSS_Double.of(opt.getValue()));
                    }
                    break;
                case CROWN_FUEL_LOAD:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.C1 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.C6 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz60 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz61 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz66 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz67 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz71) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getC6Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCfl(HSS_Double.of(opt.getValue()));
                    }
                    break;
                case GRASS_CURING:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.O1A ||
                            fbpBuilder.getDefaultFuelType() == FuelName.O1B ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz15 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz30 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz31 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz32 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz33 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz40 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz41 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz43 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz46 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz50 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz53 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz62 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz63 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz65) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getO1Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCuringDegree(HSS_Double.of(opt.getValue()));

                        if (!fbpBuilder.getSfcCalculationBuilder()
                                .getO1Builder()
                                .getParmsBuilder()
                                .hasFuelLoad()) {
                            fbpBuilder.getSfcCalculationBuilder()
                                .setVersion(1)
                                .getO1Builder()
                                .setVersion(1)
                                .getParmsBuilder()
                                .setVersion(1)
                                .setFuelLoad(HSS_Double.of(0.35));
                        }
                    }
                    break;
                case GRASS_FUEL_LOAD:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.O1A ||
                            fbpBuilder.getDefaultFuelType() == FuelName.O1B ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz15 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz30 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz31 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz32 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz33 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz40 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz41 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz43 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz46 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz50 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz53 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz62 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz63 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz65) {
                        fbpBuilder.getSfcCalculationBuilder()
                            .setVersion(1)
                            .getO1Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setFuelLoad(HSS_Double.of(opt.getValue()));
                        
                        if (!fbpBuilder.getSpreadBuilder()
                                .getO1Builder()
                                .getParmsBuilder()
                                .hasCuringDegree()) {
                            fbpBuilder.getSpreadBuilder()
                                .setVersion(1)
                                .getO1Builder()
                                .setVersion(1)
                                .getParmsBuilder()
                                .setVersion(1)
                                .setCuringDegree(HSS_Double.of(0.60));
                        }
                    }
                default:
                	break;
                }
            }
        }
        if (options != null) {
            List<OptionData> optionDatas = parseOptionData(options, fbpBuilder.getDefaultFuelType());
            for (OptionData optionData : optionDatas) {
                switch (optionData.type) { 
                case PERCENT_CONIFER:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.M1 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.M2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz54 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz69) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(fbpBuilder.getDefaultFuelType())
                            .getPcBuilder()
                            .setValue(optionData.value);
                    }
                    else if (fbpBuilder.getDefaultFuelType() == FuelName.M1M2) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(FuelName.M1)
                            .getPcBuilder()
                            .setValue(optionData.value);
                    }
                    break;
                case PERCENT_DEAD_FIR:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.M3 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.M4) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedDeadBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(fbpBuilder.getDefaultFuelType())
                            .getPdfBuilder()
                            .setValue(optionData.value);
                    }
                    else if (fbpBuilder.getDefaultFuelType() == FuelName.M3M4) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getMixedDeadBuilder()
                            .setVersion(1)
                            .getDefaultBuilder()
                            .setDefault(FuelName.M3)
                            .getPdfBuilder()
                            .setValue(optionData.value);
                    }
                    break;
                case CROWN_BASE_HEIGHT:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.C1 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.C6 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz60 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz61 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz66 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz67 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz71) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getC6Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCbh(HSS_Double.of(optionData.value));
                    }
                    break;
                case CROWN_FUEL_LOAD:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.C1 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.C6 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz60 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz61 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz66 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz67 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz71) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getC6Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCfl(HSS_Double.of(optionData.value));
                    }
                    break;
                case GRASS_CURING:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.O1A ||
                            fbpBuilder.getDefaultFuelType() == FuelName.O1B ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz15 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz30 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz31 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz32 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz33 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz40 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz41 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz43 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz46 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz50 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz53 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz62 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz63 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz65) {
                        fbpBuilder.getSpreadBuilder()
                            .setVersion(1)
                            .getO1Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setCuringDegree(HSS_Double.of(optionData.value));

                        if (!fbpBuilder.getSfcCalculationBuilder()
                                .getO1Builder()
                                .getParmsBuilder()
                                .hasFuelLoad()) {
                            fbpBuilder.getSfcCalculationBuilder()
                                .setVersion(1)
                                .getO1Builder()
                                .setVersion(1)
                                .getParmsBuilder()
                                .setVersion(1)
                                .setFuelLoad(HSS_Double.of(0.35));
                        }
                    }
                    break;
                case FUEL_LOAD:
                    if (fbpBuilder.getDefaultFuelType() == FuelName.O1A ||
                            fbpBuilder.getDefaultFuelType() == FuelName.O1B ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz2 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz15 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz30 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz31 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz32 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz33 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz40 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz41 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz43 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz46 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz50 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz53 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz62 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz63 ||
                            fbpBuilder.getDefaultFuelType() == FuelName.Nz65) {
                        fbpBuilder.getSfcCalculationBuilder()
                            .setVersion(1)
                            .getO1Builder()
                            .setVersion(1)
                            .getParmsBuilder()
                            .setVersion(1)
                            .setFuelLoad(HSS_Double.of(optionData.value));
                        
                        if (!fbpBuilder.getSpreadBuilder()
                                .getO1Builder()
                                .getParmsBuilder()
                                .hasCuringDegree()) {
                            fbpBuilder.getSpreadBuilder()
                                .setVersion(1)
                                .getO1Builder()
                                .setVersion(1)
                                .getParmsBuilder()
                                .setVersion(1)
                                .setCuringDegree(HSS_Double.of(0.60));
                        }
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }
	
	/**
	 * Convert a string wind direction from the API to a protobuf enum.
	 * @param direction The wind direction name.
	 * @return The wind direction as an enum. Default is NE if the string is an unknown value.
	 */
	protected WindDirection stringToDirection(String direction) {
		switch (direction.toLowerCase()) {
		case "north":
			return WindDirection.North;
		case "northeast":
			return WindDirection.Northeast;
		case "east":
			return WindDirection.East;
		case "southeast":
			return WindDirection.Southeast;
		case "south":
			return WindDirection.South;
		case "southwest":
			return WindDirection.Southwest;
		case "west":
			return WindDirection.West;
		default:
			return WindDirection.Northwest;
		}
	}
	
	/**
	 * Convert a weather patch operation from the internal representation to a protobuf enum.
	 * @param operation The internal operation enum.
	 * @return A protobuf enum for the operation.
	 */
	protected GridTypeOne.Operation operationToOperationOne(WeatherPatchOperation operation) {
		switch (operation) {
		case EQUAL:
			return GridTypeOne.Operation.Equal;
		case PLUS:
			return GridTypeOne.Operation.Plus;
		case MINUS:
			return GridTypeOne.Operation.Minus;
		case MULTIPLY:
			return GridTypeOne.Operation.Multiply;
		case DIVIDE:
			return GridTypeOne.Operation.Divide;
		default:
			return GridTypeOne.Operation.Disable;
		}
	}
	
	/**
	 * Convert a weather patch operation from the internal representation to a protobuf enum.
	 * Uses the restricted operation list for wind directions.
	 * @param operation The internal operation enum.
	 * @return A protobuf enum for the operation.
	 */
	protected GridTypeTwo.Operation operationToOperationTwo(WeatherPatchOperation operation) {
		switch (operation) {
		case EQUAL:
			return GridTypeTwo.Operation.Equal;
		case PLUS:
			return GridTypeTwo.Operation.Plus;
		case MINUS:
			return GridTypeTwo.Operation.Minus;
		default:
			return GridTypeTwo.Operation.Disable;
		}
	}
	
	/**
	 * Convert from the internal storage format for the FFMC calculation method to a protobuf enum.
	 * @param hffmc The internal FFMC calculation type enum.
	 * @return A protobuf enum for the FFMC calculation method.
	 */
	protected ca.wise.weather.proto.WeatherStream.FFMCMethod hffmcToMethod(HFFMCMethod hffmc) {
		switch (hffmc) {
		case LAWSON:
			return ca.wise.weather.proto.WeatherStream.FFMCMethod.LAWSON;
		case VAN_WAGNER:
			return ca.wise.weather.proto.WeatherStream.FFMCMethod.VAN_WAGNER;
		default:
			return ca.wise.weather.proto.WeatherStream.FFMCMethod.LAWSON;
		}
	}
	
	/**
	 * Convert an internal units enum to an area unit.
	 * @param value An internal unit enum.
	 * @return A protobuf enum for area units. Will be AC if an invalid unit is specified.
	 */
	protected VectorOutput.MetadataOutput.AreaUnits unitsToAreaUnits(AreaUnit value) {
		switch (value) {
		case KM2:
			return VectorOutput.MetadataOutput.AreaUnits.KILOMETRES_SQUARE;
		case M2:
			return VectorOutput.MetadataOutput.AreaUnits.METRES_SQUARE;
		case HECTARE:
			return VectorOutput.MetadataOutput.AreaUnits.HECTARES;
		case MILE2:
			return VectorOutput.MetadataOutput.AreaUnits.MILES_SQUARE;
		case FT2:
			return VectorOutput.MetadataOutput.AreaUnits.FEET_SQUARE;
		case YD2:
			return VectorOutput.MetadataOutput.AreaUnits.YARDS_SQUARE;
		default:
			return VectorOutput.MetadataOutput.AreaUnits.ACRES;
		}
	}
	
	/**
	 * Convert an internal units enum to a length unit.
	 * @param value An internal unit enum.
	 * @return A protobuf enum for length units. Will be miles if an invalid unit is specified.
	 */
	protected VectorOutput.MetadataOutput.PerimeterUnits unitsToPerimeterUnits(DistanceUnit value) {
		switch (value) {
		case KM:
			return VectorOutput.MetadataOutput.PerimeterUnits.KILOMETRES;
		case M:
			return VectorOutput.MetadataOutput.PerimeterUnits.METRES;
		case FOOT:
			return VectorOutput.MetadataOutput.PerimeterUnits.FEET;
		case YARD:
			return VectorOutput.MetadataOutput.PerimeterUnits.YARDS;
		case CHAIN:
			return VectorOutput.MetadataOutput.PerimeterUnits.CHAIN;
		default:
			return VectorOutput.MetadataOutput.PerimeterUnits.MILES;
		}
	}

	/**
	 * Create a vector file output from one of the scenarios for the protobuf message.
	 * @param input Details of the vector output from the API.
	 * @return A vector file that should be exported from a scenario.
	 */
	protected VectorOutput.Builder createVectorFile(VectorFile input) {
		VectorOutput.Builder builder = VectorOutput.newBuilder()
				.setVersion(1)
				.setScenarioName(input.getScenarioName())
				.setFilename(readyOutputFile(input.getFilename()))
				.setMultiplePerimeters(input.getMultPerim())
				.setRemoveIslands(input.getRemoveIslands())
				.setMergeContacting(input.getMergeContact())
				.setActivePerimeters(input.getPerimActive())
				.setStreamOutput(ProtoWrapper.ofBool(input.isShouldStream()));
		
		if (!Strings.isNullOrEmpty(input.getSubScenarioName()))
		    builder.addSubScenarioName(ProtoWrapper.ofString(input.getSubScenarioName()));
        if (!Strings.isNullOrEmpty(input.getCoverageName()))
            builder.setCoverageName(StringValue.of(input.getCoverageName()));
		
		for (PerimeterTimeOverride override : input.getSubScenarioOverrides()) {
		    if (!Strings.isNullOrEmpty(override.subScenarioName) &&
		            !(Strings.isNullOrEmpty(override.getStartTime()) && Strings.isNullOrEmpty(override.getEndTime()))) {
		        VectorOutput.PerimeterTimeOverride.Builder b = builder.addPerimeterTimeOverrideBuilder()
		            .setSubScenarioName(override.subScenarioName);
		        if (!Strings.isNullOrEmpty(override.getStartTime()))
		            b.setStartTime(TimeSerializer.serializeTime(createDateTime(override.getStartTime())));
		        if (!Strings.isNullOrEmpty(override.getEndTime()))
		            b.setEndTime(TimeSerializer.serializeTime(createDateTime(override.getEndTime())));
		    }
		}
		
		builder.getPerimeterTimeBuilder()
			.setStartTime(TimeSerializer.serializeTime(createDateTime(input.getPerimStartTime())))
			.setEndTime(TimeSerializer.serializeTime(createDateTime(input.getPerimEndTime())));
		
		VectorOutput.MetadataOutput.Builder metadata = builder.getMetadataBuilder()
				.setVersion(1);
		
		metadata.setApplicationVersion(input.getMetadata().isVersion());

		metadata.setScenarioName(input.getMetadata().isScenName());

		metadata.setJobName(input.getMetadata().isJobName());

		metadata.setIgnitionName(input.getMetadata().isIgName());

		metadata.setSimulationDate(input.getMetadata().isSimDate());

		metadata.setFireSize(input.getMetadata().isFireSize());
		
		metadata.setPerimeterTotal(input.getMetadata().isPerimTotal());

		metadata.setPerimeterActive(input.getMetadata().isPerimActive());

		if (input.getMetadata().getPerimUnit() != null)
			metadata.setPerimeterUnits(unitsToPerimeterUnits(input.getMetadata().getPerimUnit()));

		if (input.getMetadata().getAreaUnit() != null)
			metadata.setAreaUnits(unitsToAreaUnits(input.getMetadata().getAreaUnit()));
		
		metadata.setWxValues(input.getMetadata().isWxValues());
		
		metadata.setFwiValues(input.getMetadata().isFwiValues());
		
		metadata.setIgnitionLocation(ProtoWrapper.ofBool(input.getMetadata().isIgnitionLocation()));
		
		metadata.setMaxBurnDistance(ProtoWrapper.ofBool(input.getMetadata().isMaxBurnDistance()));
		
		metadata.setIgnitionAttributes(ProtoWrapper.ofBool(input.getMetadata().isIgnitionAttributes()));
        
        metadata.setAssetArrivalTime(ProtoWrapper.ofBool(input.getMetadata().isAssetArrivalTime()));
        
        metadata.setAssetArrivalCount(ProtoWrapper.ofBool(input.getMetadata().isAssetArrivalCount()));
        
        metadata.setIdentifyFinalPerimeter(ProtoWrapper.ofBool(input.getMetadata().isIdentifyFinalPerimeter()));
        
        metadata.setSimulationStatus(ProtoWrapper.ofBool(input.getMetadata().isSimStatus()));

		return builder;
	}

	/**
	 * Create a summary file output from one of the scenarios for the protobuf message.
	 * @param input Details of the summary output from the API.
	 * @return A summary file that should be exported from a scenario.
	 */
	protected SummaryOutput.Builder createSummaryFile(SummaryFile input) {
		SummaryOutput.Builder builder = SummaryOutput.newBuilder()
				.setVersion(1)
				.setScenarioName(input.getScenName())
				.setFilename(readyOutputFile(input.getFilename()))
				.setStreamOutput(ProtoWrapper.ofBool(input.isShouldStream()));
		
	    if (input.getOutputs().getOutputApplication() != null)
	    	builder.getMetadataBuilder().setApplicationName(ProtoWrapper.ofBool(input.getOutputs().getOutputApplication()));

	    if (input.getOutputs().getOutputGeoData() != null)
	    	builder.getMetadataBuilder().setGeoData(ProtoWrapper.ofBool(input.getOutputs().getOutputGeoData()));

	    if (input.getOutputs().getOutputScenario() != null)
	    	builder.getMetadataBuilder().setScnearioName(ProtoWrapper.ofBool(input.getOutputs().getOutputScenario()));

	    if (input.getOutputs().getOutputScenarioComments() != null)
	    	builder.getMetadataBuilder().setScenarioComments(ProtoWrapper.ofBool(input.getOutputs().getOutputScenarioComments()));

	    if (input.getOutputs().getOutputInputs() != null)
	    	builder.getMetadataBuilder().setInputs(ProtoWrapper.ofBool(input.getOutputs().getOutputInputs()));

		if (input.getOutputs().getOutputLandscape() != null)
	    	builder.getMetadataBuilder().setLandscape(ProtoWrapper.ofBool(input.getOutputs().getOutputLandscape()));

		if (input.getOutputs().getOutputFBPPatches() != null)
	    	builder.getMetadataBuilder().setFbpPatches(ProtoWrapper.ofBool(input.getOutputs().getOutputFBPPatches()));

		if (input.getOutputs().getOutputWxPatches() != null)
	    	builder.getMetadataBuilder().setWxPatches(ProtoWrapper.ofBool(input.getOutputs().getOutputWxPatches()));

		if (input.getOutputs().getOutputIgnitions() != null)
	    	builder.getMetadataBuilder().setIgnitions(ProtoWrapper.ofBool(input.getOutputs().getOutputIgnitions()));

		if (input.getOutputs().getOutputWxStreams() != null)
	    	builder.getMetadataBuilder().setWxStreams(ProtoWrapper.ofBool(input.getOutputs().getOutputWxStreams()));

		if (input.getOutputs().getOutputFBP() != null)
	    	builder.getMetadataBuilder().setFbp(ProtoWrapper.ofBool(input.getOutputs().getOutputFBP()));
		
		if (input.getOutputs().getOutputWxData()!= null)
		    builder.getMetadataBuilder().setWxData(ProtoWrapper.ofBool(input.getOutputs().getOutputWxData()));
		
		if (input.getOutputs().getOutputAssetInfo() != null)
		    builder.getMetadataBuilder().setAssetData(ProtoWrapper.ofBool(input.getOutputs().getOutputAssetInfo()));

		return builder;
	}
	
	/**
	 * Convert an internal interpolation method enum to a protobuf enum.
	 * @param method The internal interpolation method enum.
	 * @return A protobuf enum for the interpolation method. Calculate will be used if an unknown method is passed.
	 */
	protected GridOutput.InterpolationMethod interpToInterp(GridFileInterpolation method) {
		switch (method) {
		case CLOSEST_VERTEX:
			return GridOutput.InterpolationMethod.CLOSEST_VERTEX;
		case IDW:
			return GridOutput.InterpolationMethod.IDW;
		case AREA_WEIGHTING:
			return GridOutput.InterpolationMethod.AREA_WEIGHTING;
		case CALCULATE:
		    return GridOutput.InterpolationMethod.CALCULATE;
		case DISCRETIZED:
		    return GridOutput.InterpolationMethod.DISCRETIZED;
		case VORONOI_OVERLAP:
		    return GridOutput.InterpolationMethod.VORONOI_OVERLAP;
		default:
			return GridOutput.InterpolationMethod.CALCULATE;
		}
	}
	
	/**
	 * Create a grid file output from one of the scenarios for the protobuf message.
	 * @param input Details of the grid output from the API.
	 * @return A grid file that should be exported from a scenario.
	 */
	protected GridOutput.Builder createGridFile(ca.wise.api.output.GridFile input) {
		GridOutput.Builder builder = GridOutput.newBuilder()
				.setVersion(1)
				.setScenarioName(input.getScenarioName())
				.setFilename(readyOutputFile(input.getFilename()))
				.setExportTime(TimeSerializer.serializeTime(createDateTime(input.getOutputTime())))
				.setInterpolation(interpToInterp(input.getInterpMethod()))
				.setCompression(CompressionType.forNumber(input.getCompression().value))
				.setStreamOutput(ProtoWrapper.ofBool(input.isShouldStream()))
				.setOutputNodata(BoolValue.of(!input.isZeroForNodata()))
				.setExcludeIgnitionInteriors(ProtoWrapper.ofBool(input.isExcludeInteriors()));
		if (!Strings.isNullOrEmpty(input.getStartOutputTime()))
		    builder.setStartExportTime(TimeSerializer.serializeTime(createDateTime(input.getStartOutputTime())));
		if (input.getDiscretize() != null)
		    builder.getDiscretizedOptionsBuilder()
		        .setVersion(1)
		        .setDiscretize(input.getDiscretize());
		if (!Strings.isNullOrEmpty(input.getCoverageName()))
		    builder.setCoverageName(StringValue.of(input.getCoverageName()));
		if (!Strings.isNullOrEmpty(input.getAssetName()))
		    builder.setAssetName(StringValue.of(input.getAssetName()));
		if (input.getAssetIndex() != null && input.getAssetIndex() >= 0)
		    builder.setAssetIndex(ProtoWrapper.ofInt(input.getAssetIndex()));
		if (input.getExportOptions() != null) {
    		builder.getGridOptionsBuilder()
    		    .setVersion(1);
    		if (input.getExportOptions().getResolution() != null)
    		    builder.getGridOptionsBuilder()
    		        .setResolution(input.getExportOptions().getResolution());
    		else if (input.getExportOptions().getScale() != null)
    		    builder.getGridOptionsBuilder()
    		        .setScale(input.getExportOptions().getScale());
    		else
    		    builder.getGridOptionsBuilder()
    		        .setUseFuelMap(input.getExportOptions().getUseFuelMap() == null || input.getExportOptions().getUseFuelMap());
    		
    		if (input.getExportOptions().getLocation() != null)
    		    builder.getGridOptionsBuilder()
    		        .setLocation(GeoPoint.newBuilder()
    		                .setPoint(XYPoint.newBuilder()
    	                            .setY(HSS_Double.of(input.getExportOptions().getLocation().getLatitude()))
    	                            .setX(HSS_Double.of(input.getExportOptions().getLocation().getLongitude()))));
    		else
    		    builder.getGridOptionsBuilder()
    		        .setDynamicOriginValue(input.getExportOptions().getOrigin() == null ? ExportOptionsOrigin.FUELMAP_ORIGIN.value : input.getExportOptions().getOrigin().value);
		}
		builder.setGlobalStatisticsValue(input.getStatistic().value);
		builder.setMinimizeOutput(ProtoWrapper.ofBool(input.isShouldMinimize()));
		if (!Strings.isNullOrEmpty(input.getSubScenarioName()))
		    builder.addSubScenarioName(ProtoWrapper.ofString(input.getSubScenarioName()));
		for (ExportTimeOverride override : input.getSubScenarioOverrideTimes()) {
		    if (!Strings.isNullOrEmpty(override.getSubScenarioName()) && !Strings.isNullOrEmpty(override.getExportTime()))
		        builder.addSubScenarioExportTimesBuilder()
		            .setSubScenarioName(override.getSubScenarioName())
		            .setTime(TimeSerializer.serializeTime(createDateTime(override.getExportTime())));
		}
		
		return builder;
	}
	
	/**
	 * Create a fuel grid to be added to the FGM.
	 * @param fgf The fuel grid specification.
	 * @return The fuel grid FGM details.
	 */
    protected FuelGridOutput.Builder createFuelGridFile(FuelGridFile input) {
	    FuelGridOutput.Builder builder = FuelGridOutput.newBuilder()
                .setVersion(1)
                .setScenarioName(input.getScenName())
                .setFilename(readyOutputFile(input.getFilename()))
                .setCompression(CompressionType.forNumber(input.getCompression().value))
                .setStreamOutput(ProtoWrapper.ofBool(input.isShouldStream()));
        if (!Strings.isNullOrEmpty(input.getCoverageName()))
            builder.setCoverageName(StringValue.of(input.getCoverageName()));
	    
	    return builder;
	}
	
	/**
	 * Create a stats file output from one of the scenarios for the protobuf message.
	 * @param input Details of the stats output from the API.
	 * @return A stats file that should be exported from a scenario.
	 */
	protected StatsOutput.Builder createStatsFile(StatsFile input) {
	    StatsOutput.Builder builder = StatsOutput.newBuilder()
	            .setVersion(1)
	            .setScenarioName(input.getScenName())
	            .setFilename(readyOutputFile(input.getFilename()))
	            .setFileTypeValue(input.getFileType().value)
	            .setStreamOutput(ProtoWrapper.ofBool(input.isShouldStream()));
	    if (input.getLocation() != null)
	        builder.setLocation(GeoPoint.newBuilder()
	                .setPoint(XYPoint.newBuilder()
	                        .setY(HSS_Double.of(input.getLocation().getLatitude()))
	                        .setX(HSS_Double.of(input.getLocation().getLongitude()))));
	    else
	        builder.setStreamName(input.getStreamName());
	    if (input.getDiscretize() != null)
	        builder.getDiscretizedOptionsBuilder()
	            .setVersion(1)
	            .setDiscretize(input.getDiscretize());
        for (GlobalStatistics stat : input.getColumns())
            builder.addGlobalColumnsValue(stat.value);
	    return builder;
	}
    
    /**
     * Create an asset stats file output from one of the scenarios for the protobuf message.
     * @param input Details of the asset stats output from the API.
     * @return A stats file that should be exported from a scenario.
     */
    protected AssetStatsOutput.Builder createAssetStatsFile(AssetStatsFile input) {
        AssetStatsOutput.Builder builder = AssetStatsOutput.newBuilder()
                .setVersion(1)
                .setScenarioName(input.getScenarioName())
                .setFilename(readyOutputFile(input.getFilename()))
                .setFileTypeValue(input.getFileType())
                .setStreamOutput(ProtoWrapper.ofBool(input.isShouldStream()))
                .setCriticalPathFilename(readyOutputFile(input.getCriticalPathPath()));
        if (input.getCriticalPathEmbedded() != null)
            builder.setCriticalPathEmbedded(ProtoWrapper.ofBool(input.getCriticalPathEmbedded()));
        return builder;
    }
	
	/**
	 * A data writer that can export a protobuf message to a JSON file.
	 * 
	 * @author Travis Redpath
	 */
	public static class PBDataWriter implements IDataWriter {
		
		/**
		 * The protobuf message to be written.
		 */
		private final PrometheusData data;
		/**
		 * The directory that all jobs get written to. This job will be written
		 * to a subdirectory with the job's name.
		 */
		private final String jobDir;
		/**
		 * Output formatting options.
		 */
		private final OutputOptions options;
		/**
		 * The priority to add the job to the job queue with.
		 */
		private final int priority;
		/**
		 * Should the job be validated instead of immediately run.
		 */
		private final boolean validate;
		
		protected PBDataWriter(String jobDir, PrometheusData data, OutputOptions options, int priority, boolean validate) {
			this.jobDir = jobDir;
			this.data = data;
			this.options = options;
			this.priority = priority;
			this.validate = validate;
		}
		
		/**
		 * Write the data to a file.
		 * @param jobname The name of the job. Will be used as a directory to write the JSON file in.
		 * @return True if the file was successfully written, false otherwise.
		 */
		@Override
		public boolean write(String jobname) {
			boolean retval = false;
			if (options.useBinary) {
				File fl = Paths.get(jobDir, jobname, "job.fgmb").toFile();
				try (FileOutputStream stream = new FileOutputStream(fl)) {
					data.writeTo(stream);
					retval = true;
				}
	    		catch (IOException e) {
	        		e.printStackTrace();
	    		}
			}
			else {
				File fl = Paths.get(jobDir, jobname, "job.fgmj").toFile();
	    		try (BufferedWriter stream = new BufferedWriter(new FileWriter(fl))) {
	    			Printer printer;
	    			if (options.prettyPrint)
	    				printer = JsonFormat.printer()
	    					.includingDefaultValueFields();
	    			else
	    				printer = JsonFormat.printer()
	    					.omittingInsignificantWhitespace();
	    			stream.write(printer.print(data));
	            	retval = true;
	    		}
	    		catch (IOException e) {
	        		e.printStackTrace();
	    		}
			}
			return retval;
		}
		
		@Override
		public byte[] stream(String jobname) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			if (options.useBinary) {
				try {
					data.writeTo(stream);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
    			Printer printer;
    			TypeRegistry registry = TypeRegistry.newBuilder()
    			        .add(Int32Value.getDescriptor())
    			        .add(Int64Value.getDescriptor())
    			        .add(DoubleValue.getDescriptor())
    			        .add(StringValue.getDescriptor())
    			        .build();
    			if (options.prettyPrint)
    				printer = JsonFormat.printer()
    					.includingDefaultValueFields()
    					.usingTypeRegistry(registry);
    			else
    				printer = JsonFormat.printer()
    					.omittingInsignificantWhitespace()
                        .usingTypeRegistry(registry);
				try {
					String json = printer.print(data);
	    			stream.write(json.getBytes(StandardCharsets.UTF_8));
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			return stream.toByteArray();
		}
		
		/**
		 * Get the number of cores the job will use.
		 */
		@Override
		public int getCoreCount() {
			return data.getSettings().getHardware().getCores();
		}

		@Override
		public OutputType getType() {
			if (options.useBinary) {
				return OutputType.BINARY;
			}
			else if (options.prettyPrint) {
				return OutputType.PROTO;
			}
			else {
				return OutputType.MINIMAL_PROTO;
			}
		}

        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        public boolean isValidate() {
            return validate;
        }
	}
	
	public static class OutputOptions implements IOutputOptions {
		public boolean useBinary = false;
		public boolean prettyPrint = true;
		public boolean allowUserConnectionSettings = false;
		public boolean singleFile = false;
		public int version;
		
		@Override
		public boolean shouldStream() {
			return singleFile;
		}
	}
}
