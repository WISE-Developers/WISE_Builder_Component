package ca.wise.fgm.output;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.base.Strings;

import ca.wise.config.proto.ServerConfiguration;
import ca.wise.fgm.xml.CoordinateType;
import ca.wise.fgm.xml.Data;
import ca.wise.fgm.xml.MqttConfig;
import ca.wise.fgm.xml.Outputs;
import ca.wise.fgm.xml.WcsData;
import ca.wise.fgm.xml.WfsData;
import ca.wise.api.LatLon;
import ca.wise.api.WISE;
import ca.wise.api.input.BurningConditions;
import ca.wise.api.input.FuelBreak;
import ca.wise.api.input.FuelPatch;
import ca.wise.api.input.GridFile;
import ca.wise.api.input.Ignition;
import ca.wise.api.input.IgnitionReference;
import ca.wise.api.input.LayerInfo;
import ca.wise.api.input.Scenario;
import ca.wise.api.input.StationStream;
import ca.wise.api.input.WeatherGrid;
import ca.wise.api.input.WeatherGridGridFile;
import ca.wise.api.input.WeatherPatch;
import ca.wise.api.input.WeatherStation;
import ca.wise.api.input.WeatherStream;
import ca.wise.api.output.SummaryFile;
import ca.wise.api.output.VectorFile;
import ca.wise.api.units.AreaUnit;
import ca.wise.api.units.DistanceUnit;

public class DataCombiner implements IDataCombiner {
	private WISE input;
	private ServerConfiguration config;
	private String jobDir;
	private String baseDir;
	private List<Integer> threads;
	
	DataCombiner(Object options) { }

	@Override
	public void initialize(WISE input, ServerConfiguration config, String jobDir) {
		this.input = input;
		this.config = config;
		this.jobDir = jobDir;
		this.baseDir = Paths.get(jobDir).getParent().toAbsolutePath().toString();
		new File(jobDir + "/Inputs").mkdirs();
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
	
	private String verbosityToString(ca.wise.config.proto.ServerConfiguration.Verbosity value) {
		switch (value) {
		case MAX:
			return "MAX";
		case NONE:
			return "NONE";
		case SEVERE:
			return "SEVERE";
		case WARN:
			return "WARN";
		default:
			return "INFO";
		}
	}

	private void InitParams(Data val) {
		Data.Params p = new Data.Params();
		//logger settings
		Data.Params.Log l = new Data.Params.Log();
		p.setLog(l);
		l.setName(config.getLog().getFilename());
		l.setVerbosity(verbosityToString(config.getLog().getVerbosity()));
		//job started/stopped signal files
		Data.Params.Signals s = new Data.Params.Signals();
		p.setSignals(s);
		s.setStart(config.getSignals().getStart());
		s.setComplete(config.getSignals().getComplete());
		//options for the resources that the job is allowed to use
		Data.Params.Hardware h = new Data.Params.Hardware();
		p.setHardware(h);
		int totalCores = config.getHardware().getCores();
		int numJobs = this.input.getInputs().getScenarios().size();
		threads = new ArrayList<Integer>(numJobs);
		if (numJobs >= totalCores) {
			h.setCores(totalCores);
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
			h.setCores(numJobs);
		}
		h.setProcesses(config.getHardware().getProcesses());
		//MQTT communication options
		if (config.hasMqtt()) {
			MqttConfig config = new MqttConfig();
			config.setDebug(false);
			config.setHostName(this.config.getMqtt().getHostname());
			config.setPassword(this.config.getMqtt().getPassword());
			config.setPort(this.config.getMqtt().getPort());
			config.setQos(this.config.getMqtt().getQos());
			config.setTopic(this.config.getMqtt().getTopic());
			config.setUserName(this.config.getMqtt().getUsername());
			config.setVerbosity(verbosityToString(this.config.getMqtt().getVerbosity()));
			p.setMqtt(config);
		}
		val.setParams(p);
	}

	private void InitJob(Data val, String jobname) {
		Data.Job j = new Data.Job();
		j.setName(jobname);

		Data.Job.Inputs i = new Data.Job.Inputs();
		j.setInputs(i);

		Data.Job.Inputs.Files f = new Data.Job.Inputs.Files();
		i.setFiles(f);
		Data.Job.Inputs.WeatherStations ws = new Data.Job.Inputs.WeatherStations();
		i.setWeatherStations(ws);
		Data.Job.Inputs.Ignitions ig = new Data.Job.Inputs.Ignitions();
		i.setIgnitions(ig);
		Data.Job.Inputs.Scenarios sc = new Data.Job.Inputs.Scenarios();
		i.setScenarios(sc);
		Data.Job.Inputs.Timezone tz = new Data.Job.Inputs.Timezone();
		i.setTimezone(tz);

		Data.Job.Inputs.Files.FuelbreakFiles fbf = new Data.Job.Inputs.Files.FuelbreakFiles();
		f.setFuelbreakFiles(fbf);
		Data.Job.Inputs.Files.FuelPatchFiles fpf = new Data.Job.Inputs.Files.FuelPatchFiles();
		f.setFuelPatchFiles(fpf);
		Data.Job.Inputs.Files.WeatherGridFiles wgf = new Data.Job.Inputs.Files.WeatherGridFiles();
		f.setWeatherGridFiles(wgf);
		Data.Job.Inputs.Files.GridFiles gf = new Data.Job.Inputs.Files.GridFiles();
		f.setGridFiles(gf);
		Data.Job.Inputs.Files.WeatherPatchFiles wpf = new Data.Job.Inputs.Files.WeatherPatchFiles();
		f.setWeatherPatchFiles(wpf);

		Outputs o = new Outputs();
		j.setOutputs(o);

		Outputs.GridFiles ogf = new Outputs.GridFiles();
		o.setGridFiles(ogf);
		Outputs.VectorFiles ovf = new Outputs.VectorFiles();
		o.setVectorFiles(ovf);
		Outputs.SummaryFiles osf = new Outputs.SummaryFiles();
		o.setSummaryFiles(osf);
		val.setJob(j);
	}

	private Data.Job.Inputs.Scenarios.Scenario CreateScenario(Scenario input, int index) {
		Data.Job.Inputs.Scenarios.Scenario retval = new Data.Job.Inputs.Scenarios.Scenario();
		retval.setName("Scenario");

		Data.Job.Inputs.Scenarios.Scenario.FgmOptions fgm = new Data.Job.Inputs.Scenarios.Scenario.FgmOptions();
		retval.setFgmOptions(fgm);
		Data.Job.Inputs.Scenarios.Scenario.FbpOptions fbp = new Data.Job.Inputs.Scenarios.Scenario.FbpOptions();
		retval.setFbpOptions(fbp);
		Data.Job.Inputs.Scenarios.Scenario.FmcOptions fmc = new Data.Job.Inputs.Scenarios.Scenario.FmcOptions();
		fmc.setAccurateLocation(true);
		retval.setFmcOptions(fmc);
		Data.Job.Inputs.Scenarios.Scenario.FwiOptions fwi = new Data.Job.Inputs.Scenarios.Scenario.FwiOptions();
		retval.setFwiOptions(fwi);
		PopulateScenario(retval, input, index);
		return retval;
	}

	private Data.Job.Inputs.Scenarios.ScenarioCopy CreateScenarioCopy(Scenario input, int index) {
		Data.Job.Inputs.Scenarios.ScenarioCopy retval = new Data.Job.Inputs.Scenarios.ScenarioCopy();
		retval.setName("Scenario");
		PopulateScenarioCopy(retval, input, index);
		return retval;
	}

	private XMLGregorianCalendar CreateDateTime(String input) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
		XMLGregorianCalendar retval = null;
		try {
			Date dt = format.parse(input);
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(dt);
			retval = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
					0, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
		} catch (ParseException e) {
		} catch (DatatypeConfigurationException e) {
		}
		return retval;
	}

	private XMLGregorianCalendar CreateDate(String input) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		XMLGregorianCalendar retval = null;
		try {
			Date dt = format.parse(input);
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(dt);
			retval = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
					DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
		} catch (ParseException e) {
		} catch (DatatypeConfigurationException e) {
		}
		return retval;
	}

	private XMLGregorianCalendar CreateTime(String input) {
		SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
		XMLGregorianCalendar retval = null;
		try {
			Date dt = format.parse(input);
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(dt);
			retval = DatatypeFactory.newInstance().newXMLGregorianCalendar(DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
					DatatypeConstants.FIELD_UNDEFINED, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
					cal.get(Calendar.SECOND), DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
		} catch (ParseException e) {
		} catch (DatatypeConfigurationException e) {
		}
		return retval;
	}

	private Duration CreateDuration(String input) {
		Duration retval = null;
		if (input.equals("P"))
			input = "PT0H";
		try {
			retval = DatatypeFactory.newInstance().newDuration(input);
		} catch (DatatypeConfigurationException e) {
		}
		return retval;
	}

	private void PopulateScenario(Data.Job.Inputs.Scenarios.Scenario output, Scenario input, int index) {
		output.setStartTime(CreateDateTime(input.getStartTime()));
		output.setEndTime(CreateDateTime(input.getEndTime()));
		if (!input.getComments().isEmpty()) {
			output.setComments(input.getComments());
		}
		output.setName(input.getName());
		output.setDisplayInterval(CreateDuration(input.getDisplayInterval()));

		Data.Job.Inputs.Scenarios.Scenario.BurningConditions burn = new Data.Job.Inputs.Scenarios.Scenario.BurningConditions();
		output.setBurningConditions(burn);
		for (BurningConditions bc : input.getBurningConditions()) {
			ca.wise.fgm.xml.BurnCondition tmp = new ca.wise.fgm.xml.BurnCondition();
			tmp.setDate(CreateDate(bc.getDate()));
			if (bc.getStartTime().length() > 0)
				tmp.setStartTime(CreateDuration(bc.getStartTime()));
			if (bc.getEndTime().length() > 0)
				tmp.setEndTime(CreateDuration(bc.getEndTime()));
			tmp.setFwiGreater(String.valueOf(bc.getFwiGreater()));
			tmp.setWsGreater(String.valueOf(bc.getWsGreater()));
			tmp.setRhLess(String.valueOf(bc.getRhLess()));
			tmp.setIsiGreater(String.valueOf(bc.getIsiGreater()));
			burn.getBurningCondition().add(tmp);
		}

		Data.Job.Inputs.Scenarios.Scenario.VectorInfo vi = new Data.Job.Inputs.Scenarios.Scenario.VectorInfo();
		output.setVectorInfo(vi);
		for (String v : input.getVectorInfo()) {
			vi.getVectorName().add(v);
		}
		output.setVectorInfo(vi);
		Data.Job.Inputs.Scenarios.Scenario.StreamInfo si = new Data.Job.Inputs.Scenarios.Scenario.StreamInfo();
		output.setStreamInfo(si);
		for (StationStream st : input.getStationStreams()) {
			Data.Job.Inputs.Scenarios.Scenario.StreamInfo.StreamName name = new Data.Job.Inputs.Scenarios.Scenario.StreamInfo.StreamName();
			name.setStationName(st.getStation());
			name.setValue(st.getStream());
			name.setPrimaryStream(st.isPrimaryStream());
			si.getStreamName().add(name);
		}
		output.setStreamInfo(si);

		Data.Job.Inputs.Scenarios.Scenario.IgnitionInfo ii = new Data.Job.Inputs.Scenarios.Scenario.IgnitionInfo();
		for (IgnitionReference v : input.getIgnitionInfo()) {
			ii.getIgnitionName().add(v.getIgnition());
		}
		output.setIgnitionInfo(ii);

		Data.Job.Inputs.Scenarios.Scenario.LayerInfo li = new Data.Job.Inputs.Scenarios.Scenario.LayerInfo();
		for (LayerInfo info : input.getLayerInfo())
		{
			Data.Job.Inputs.Scenarios.Scenario.LayerInfo.Filter filter = new Data.Job.Inputs.Scenarios.Scenario.LayerInfo.Filter();
			filter.setFilterIndex(info.getIndex());
			filter.setFilterName(info.getName());
			li.getFilter().add(filter);
		}
		output.setLayerInfo(li);

		if (input.getFgmOptions().getMaxAccTs() != null) {
			output.getFgmOptions().setMaxAccTS(CreateDuration(input.getFgmOptions().getMaxAccTs()));
		}
		if (input.getFgmOptions().getDistRes() >= 0) {
			output.getFgmOptions().setDistRes(String.valueOf(input.getFgmOptions().getDistRes()));
		}
		if (input.getFgmOptions().getPerimRes() >= 0) {
			output.getFgmOptions().setPerimRes(String.valueOf(input.getFgmOptions().getPerimRes()));
		}
		if (input.getFgmOptions().getMinimumSpreadingRos() != null) {
			output.getFgmOptions().setMinimumSpreadingROS(String.valueOf(input.getFgmOptions().getMinimumSpreadingRos()));
		}
		output.getFgmOptions().setStopAtGridEnd(input.getFgmOptions().isStopAtGridEnd());
		output.getFgmOptions().setBreaching(input.getFgmOptions().isBreaching());
		output.getFgmOptions().setDynamicSpatialThreshold(input.getFgmOptions().isDynamicSpatialThreshold());
		output.getFgmOptions().setSpotting(input.getFgmOptions().isSpotting());
		output.getFgmOptions().setPurgeNonDisplayable(input.getFgmOptions().isPurgeNonDisplayable());
		if (input.getFgmOptions().getDx() != null && input.getFgmOptions().getDx() != 0) {
			output.getFgmOptions().setDx(input.getFgmOptions().getDx().toString());
		}
		if (input.getFgmOptions().getDy() != null && input.getFgmOptions().getDy() != 0) {
			output.getFgmOptions().setDy(input.getFgmOptions().getDy().toString());
		}
		if (input.getFgmOptions().getDt() != null && input.getFgmOptions().getDt().length() > 0) {
			output.getFgmOptions().setDt(CreateDuration(input.getFgmOptions().getDt()));
		}
		if (input.getFgmOptions().getGrowthPercentileApplied() != null) {
			output.getFgmOptions().setGrowthPercentileApplied(input.getFgmOptions().getGrowthPercentileApplied());
		}
		if (input.getFgmOptions().getGrowthPercentile() != null) {
			output.getFgmOptions().setGrowthPercentile(String.valueOf(input.getFgmOptions().getGrowthPercentile()));
		}

		if (input.getFbpOptions().getTerrainEffect() != null) {
			output.getFbpOptions().setTerrainEffect(input.getFbpOptions().getTerrainEffect());
		}
		if (input.getFbpOptions().getWindEffect() != null) {
			output.getFbpOptions().setWindEffect(input.getFbpOptions().getWindEffect());
		}

		if (input.getFmcOptions().getPerOverride() != null) {
			output.getFmcOptions().setPerOverride(String.valueOf(input.getFmcOptions().getPerOverride()));
		}
		output.getFmcOptions().setNodataElev(String.valueOf(input.getFmcOptions().getNodataElev()));
		if (input.getFmcOptions().getTerrain() != null) {
			output.getFmcOptions().setTerrain(input.getFmcOptions().getTerrain());
		}
		if (input.getFmcOptions().getAccurateLocation() != null) {
			output.getFmcOptions().setAccurateLocation(input.getFmcOptions().getAccurateLocation());
		}
		else {
			output.getFmcOptions().setAccurateLocation(true);
		}

		if (input.getFwiOptions().getFwiSpacInterp() != null) {
			output.getFwiOptions().setFwiSpacInterp(input.getFwiOptions().getFwiSpacInterp());
		}
		if (input.getFwiOptions().getFwiFromSpacWeather() != null) {
			output.getFwiOptions().setFwiFromSpacWeather(input.getFwiOptions().getFwiFromSpacWeather());
		}
		if (input.getFwiOptions().getHistoryOnEffectedFWI() != null) {
			output.getFwiOptions().setHistoryOnEffectedFWI(input.getFwiOptions().getHistoryOnEffectedFWI());
		}
		if (input.getFwiOptions().getBurningConditionsOn() != null) {
			output.getFwiOptions().setBurningConditionsOn(input.getFwiOptions().getBurningConditionsOn());
		}
		if (input.getFwiOptions().getFwiTemporalInterp() != null) {
			output.getFwiOptions().setFwiTemporalInterp(input.getFwiOptions().getFwiTemporalInterp());
		}
		output.setThreads(threads.get(index));
	}

	private void PopulateScenarioCopy(Data.Job.Inputs.Scenarios.ScenarioCopy output, Scenario input, int index) {
		if (!input.getStartTime().isEmpty())
			output.setStartTime(CreateDateTime(input.getStartTime()));
		if (!input.getEndTime().isEmpty())
			output.setEndTime(CreateDateTime(input.getEndTime()));
		if (!input.getComments().isEmpty()) {
			output.setComments(input.getComments());
		}
		output.setName(input.getName());

		if (input.getBurningConditions().size() > 0) {
			Data.Job.Inputs.Scenarios.ScenarioCopy.BurningConditions burn = new Data.Job.Inputs.Scenarios.ScenarioCopy.BurningConditions();
			output.setBurningConditions(burn);
			for (BurningConditions bc : input.getBurningConditions()) {
				ca.wise.fgm.xml.BurnCondition tmp = new ca.wise.fgm.xml.BurnCondition();
				tmp.setDate(CreateDate(bc.getDate()));
				if (bc.getStartTime().length() > 0)
					tmp.setStartTime(CreateDuration(bc.getStartTime()));
				if (bc.getEndTime().length() > 0)
					tmp.setEndTime(CreateDuration(bc.getEndTime()));
				tmp.setFwiGreater(String.valueOf(bc.getFwiGreater()));
				tmp.setWsGreater(String.valueOf(bc.getWsGreater()));
				tmp.setRhLess(String.valueOf(bc.getRhLess()));
				burn.getBurningCondition().add(tmp);
			}
		}

		if (input.getVectorInfo().size() > 0) {
			Data.Job.Inputs.Scenarios.ScenarioCopy.VectorInfo vi = new Data.Job.Inputs.Scenarios.ScenarioCopy.VectorInfo();
			for (String v : input.getVectorInfo()) {
				vi.getVectorName().add(v);
			}
			output.setVectorInfo(vi);
		}

		if (input.getStationStreams().size() > 0) {
			Data.Job.Inputs.Scenarios.ScenarioCopy.StreamInfo si = new Data.Job.Inputs.Scenarios.ScenarioCopy.StreamInfo();
			for (StationStream st : input.getStationStreams()) {
				Data.Job.Inputs.Scenarios.ScenarioCopy.StreamInfo.StreamName name = new Data.Job.Inputs.Scenarios.ScenarioCopy.StreamInfo.StreamName();
				name.setStationName(st.getStation());
				name.setValue(st.getStream());
				name.setPrimaryStream(st.isPrimaryStream());
				si.getStreamName().add(name);
			}
			output.setStreamInfo(si);
		}

		if (input.getIgnitionInfo().size() > 0) {
			Data.Job.Inputs.Scenarios.ScenarioCopy.IgnitionInfo ii = new Data.Job.Inputs.Scenarios.ScenarioCopy.IgnitionInfo();
			for (IgnitionReference v : input.getIgnitionInfo()) {
				ii.getIgnitionName().add(v.getIgnition());
			}
			output.setIgnitionInfo(ii);
		}

		if (input.getFgmOptions().getMaxAccTs() != null) {
			output.getFgmOptions().setMaxAccTS(CreateDuration(input.getFgmOptions().getMaxAccTs()));
		}
		if (input.getFgmOptions().getDistRes() >= 0) {
			if (output.getFgmOptions() == null) {
				output.setFgmOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FgmOptions());
			}
			output.getFgmOptions().setDistRes(String.valueOf(input.getFgmOptions().getDistRes()));
		}
		if (input.getFgmOptions().getPerimRes() >= 0) {
			if (output.getFgmOptions() == null) {
				output.setFgmOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FgmOptions());
			}
			output.getFgmOptions().setPerimRes(String.valueOf(input.getFgmOptions().getPerimRes()));
		}
		if (input.getFgmOptions().getMinimumSpreadingRos() != null) {
			if (output.getFgmOptions() == null) {
				output.setFgmOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FgmOptions());
			}
			output.getFgmOptions().setMinimumSpreadingROS(String.valueOf(input.getFgmOptions().getMinimumSpreadingRos()));
		}
		if (output.getFgmOptions() == null) {
			output.setFgmOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FgmOptions());
		}
		output.getFgmOptions().setStopAtGridEnd(input.getFgmOptions().isStopAtGridEnd());
		output.getFgmOptions().setBreaching(input.getFgmOptions().isBreaching());
		output.getFgmOptions().setDynamicSpatialThreshold(input.getFgmOptions().isDynamicSpatialThreshold());
		output.getFgmOptions().setSpotting(input.getFgmOptions().isSpotting());
		output.getFgmOptions().setPurgeNonDisplayable(input.getFgmOptions().isPurgeNonDisplayable());
		if (input.getFgmOptions().getDx() != null && input.getFgmOptions().getDx() != 0) {
			output.getFgmOptions().setDx(input.getFgmOptions().getDx().toString());
		}
		if (input.getFgmOptions().getDy() != null && input.getFgmOptions().getDy() != 0) {
			output.getFgmOptions().setDy(input.getFgmOptions().getDy().toString());
		}
		if (input.getFgmOptions().getDt() != null && input.getFgmOptions().getDt().length() > 0) {
			output.getFgmOptions().setDt(CreateDuration(input.getFgmOptions().getDt()));
		}
		if (input.getFgmOptions().getGrowthPercentileApplied() != null) {
			output.getFgmOptions().setGrowthPercentileApplied(input.getFgmOptions().getGrowthPercentileApplied());
		}
		if (input.getFgmOptions().getGrowthPercentile() != null) {
			output.getFgmOptions().setGrowthPercentile(String.valueOf(input.getFgmOptions().getGrowthPercentile()));
		}

		if (input.getFbpOptions().getTerrainEffect() != null) {
			if (output.getFbpOptions() == null) {
				output.setFbpOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FbpOptions());
			}
			output.getFbpOptions().setTerrainEffect(input.getFbpOptions().getTerrainEffect());
		}
		if (input.getFbpOptions().getWindEffect() != null) {
			if (output.getFbpOptions() == null) {
				output.setFbpOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FbpOptions());
			}
			output.getFbpOptions().setWindEffect(input.getFbpOptions().getWindEffect());
		}

		if (output.getFmcOptions() == null) {
			output.setFmcOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FmcOptions());
		}
		if (input.getFmcOptions().getPerOverride() != null) {
			output.getFmcOptions().setPerOverride(String.valueOf(input.getFmcOptions().getPerOverride()));
		}
		output.getFmcOptions().setNodataElev(String.valueOf(input.getFmcOptions().getNodataElev()));
		if (input.getFmcOptions().getTerrain() != null) {
			output.getFmcOptions().setTerrain(input.getFmcOptions().getTerrain());
		}
		if (input.getFmcOptions().getAccurateLocation() != null) {
			output.getFmcOptions().setAccurateLocation(input.getFmcOptions().getAccurateLocation());
		}
		else {
			output.getFmcOptions().setAccurateLocation(true);
		}

		if (input.getFwiOptions().getFwiSpacInterp() != null) {
			if (output.getFwiOptions() == null) {
				output.setFwiOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FwiOptions());
			}
			output.getFwiOptions().setFwiSpacInterp(input.getFwiOptions().getFwiSpacInterp());
		}
		if (input.getFwiOptions().getFwiFromSpacWeather() != null) {
			if (output.getFwiOptions() == null) {
				output.setFwiOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FwiOptions());
			}
			output.getFwiOptions().setFwiFromSpacWeather(input.getFwiOptions().getFwiFromSpacWeather());
		}
		if (input.getFwiOptions().getHistoryOnEffectedFWI() != null) {
			if (output.getFwiOptions() == null) {
				output.setFwiOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FwiOptions());
			}
			output.getFwiOptions().setHistoryOnEffectedFWI(input.getFwiOptions().getHistoryOnEffectedFWI());
		}
		if (input.getFwiOptions().getBurningConditionsOn() != null) {
			if (output.getFwiOptions() == null) {
				output.setFwiOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FwiOptions());
			}
			output.getFwiOptions().setBurningConditionsOn(input.getFwiOptions().getBurningConditionsOn());
		}
		if (input.getFwiOptions().getFwiTemporalInterp() != null) {
			if (output.getFwiOptions() == null) {
				output.setFwiOptions(new Data.Job.Inputs.Scenarios.ScenarioCopy.FwiOptions());
			}
			output.getFwiOptions().setFwiTemporalInterp(input.getFwiOptions().getFwiTemporalInterp());
		}
		output.setThreads(threads.get(index));
	}

	private ca.wise.fgm.xml.WindGridData CreateWeatherGrid(WeatherGrid input) {
		ca.wise.fgm.xml.WindGridData data = new ca.wise.fgm.xml.WindGridData();
		data.setComments(input.getComments());
		data.setName(input.getId());
		data.setEndTime(CreateDateTime(input.getEndTime()));
		if (input.getEndTimeOfDay().equals("24:00:00"))
			data.setEndTimeOfDay(CreateDuration("PT23H59M59S"));
		else {
			XMLGregorianCalendar c = CreateTime(input.getEndTime());
			data.setEndTimeOfDay(CreateDuration("PT" + c.getHour() + "H" + c.getMinute() + "M" + c.getSecond() + "S"));
		}
		data.setStartTime(CreateDateTime(input.getStartTime()));
		XMLGregorianCalendar c = CreateTime(input.getStartTimeOfDay());
		data.setStartTimeOfDay(CreateDuration("PT" + c.getHour() + "H" + c.getMinute() + "M" + c.getSecond() + "S"));
		data.setType(input.getType().value);
		ca.wise.fgm.xml.WeatherGridData raw = new ca.wise.fgm.xml.WeatherGridData();
		data.setGridDataRaw(raw);
		ca.wise.fgm.xml.WeatherGridData.Sectors sectors = new ca.wise.fgm.xml.WeatherGridData.Sectors();
		raw.setSectors(sectors);
		ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList sectorList = new ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList();
		sectors.setSectorList(sectorList);
		for (WeatherGridGridFile d : input.getGridData()) {
			ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList.Sector sector = new ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList.Sector();
			sectorList.getSector().add(sector);
			ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList.Sector.Direction direction = new ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList.Sector.Direction();
			direction.setOrdinalDirection(d.getSector().name().toLowerCase());
			sector.setDirection(direction);
			ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList.Sector.SectorEntries entry = new ca.wise.fgm.xml.WeatherGridData.Sectors.SectorList.Sector.SectorEntries();
			entry.setSpeed(String.valueOf(d.getSpeed()));
			sector.getSectorEntries().add(entry);
			ca.wise.fgm.xml.WcsData wcs = new ca.wise.fgm.xml.WcsData();
			entry.setContents(wcs);
			ca.wise.fgm.xml.WcsData.LocationFile file = new ca.wise.fgm.xml.WcsData.LocationFile();
			file.setFileName(readyFile(jobDir, d.getFilename()));
			file.setProjectionFile(readyFile(jobDir, d.getProjection()));
			wcs.setLocationFile(file);
		}
		return data;
	}

	private Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile CreateWeatherPatch(WeatherPatch input) {
		Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile retval = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile();
		retval.setComments(input.getComments());
		retval.setEndTime(CreateDateTime(input.getEndTime()));
		if (!input.getEndTimeOfDay().isEmpty())
			retval.setEndTimeOfDay(CreateTime(input.getEndTimeOfDay()));
		retval.setName(input.getId());
		retval.setStartTime(CreateDateTime(input.getStartTime()));
		if (!input.getStartTimeOfDay().isEmpty())
			retval.setStartTimeOfDay(CreateTime(input.getStartTimeOfDay()));
		if (input.getTemperature() != null) {
			Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Temperature temp = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Temperature();
			temp.setOperation(input.getTemperature().getOperation().toString());
			temp.setValue(String.valueOf(input.getTemperature().getValue()));
			retval.setTemperature(temp);
		}
		if (input.getRh() != null) {
			Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Rh rh = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Rh();
			rh.setOperation(input.getRh().getOperation().toString());
			rh.setValue(String.valueOf(input.getRh().getValue()));
			retval.setRh(rh);
		}
		if (input.getPrecip() != null) {
			Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Precipitation precip = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Precipitation();
			precip.setOperation(input.getPrecip().getOperation().toString());
			precip.setValue(String.valueOf(input.getPrecip().getValue()));
			retval.setPrecipitation(precip);
		}
		if (input.getWindSpeed() != null) {
			Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.WindSpeed speed = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.WindSpeed();
			speed.setOperation(input.getWindSpeed().getOperation().toString());
			speed.setValue(String.valueOf(input.getWindSpeed().getValue()));
			retval.setWindSpeed(speed);
		}
		if (input.getWindDirection() != null) {
			Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.WindDirection dir = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.WindDirection();
			dir.setOperation(input.getWindDirection().getOperation().toString());
			dir.setValue(String.valueOf(input.getWindDirection().getValue()));
			retval.setWindDirection(dir);
		}
		Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Geographic geo = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Geographic();
		switch (input.getType()) {
		case LANDSCAPE:
			geo.setLandscape(true);
			break;
		case FILE:
			Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Geographic.Coordinates coor = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Geographic.Coordinates();
			WfsData.LocationFile lf = new WfsData.LocationFile();
			lf.setFileName(readyFile(jobDir, input.getFilename()));
			coor.setLocationFile(lf);
			geo.setCoordinates(coor);
			break;
		case POLYGON:
			Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Geographic.Coordinates coors = new Data.Job.Inputs.Files.WeatherPatchFiles.PatchFile.Geographic.Coordinates();
			List<WfsData.VectorData> data = coors.getVectorData();
			WfsData.VectorData vdata = new WfsData.VectorData();
			WfsData.VectorData.Polygon polygon = new WfsData.VectorData.Polygon();
			for (LatLon ll : input.getFeature()) {
				CoordinateType p = new CoordinateType();
				p.setLat(String.valueOf(ll.getLatitude()));
				p.setLon(String.valueOf(ll.getLongitude()));
				polygon.getLocation().add(p);
			}
			vdata.setPolygon(polygon);
			data.add(vdata);
			geo.setCoordinates(coors);
			break;
		default:
			break;
		}
		retval.setGeographic(geo);
		return retval;
	}

	private Data.Job.Inputs.Files.GridFiles.GridFile CreateInputGridFile(GridFile input) {
		Data.Job.Inputs.Files.GridFiles.GridFile retval = new Data.Job.Inputs.Files.GridFiles.GridFile();
		retval.setComments(input.getComment());
		WcsData wdata = new WcsData();
		WcsData.LocationFile lf = new WcsData.LocationFile();
		lf.setFileName(readyFile(jobDir, input.getFilename()));
		lf.setProjectionFile(readyFile(jobDir, input.getProjection()));
		wdata.setLocationFile(lf);
		retval.setContents(wdata);
		retval.setName(input.getName());
		retval.setType(input.getType().toString());
		return retval;
	}

	private Data.Job.Inputs.Files.FuelbreakFiles.FuelbreakFile CreateFuelBreakFile(FuelBreak input) {
		Data.Job.Inputs.Files.FuelbreakFiles.FuelbreakFile retval = new Data.Job.Inputs.Files.FuelbreakFiles.FuelbreakFile();
		retval.setComments(input.getComments());
		retval.setName(input.getId());
		WfsData coor = new WfsData();
		switch (input.getType()) {
		case POLYGON:
			WfsData.VectorData.Polygon polygon = new WfsData.VectorData.Polygon();
			for (LatLon ll : input.getFeature()) {
				CoordinateType type = new CoordinateType();
				type.setLat(String.valueOf(ll.getLatitude()));
				type.setLon(String.valueOf(ll.getLongitude()));
				polygon.getLocation().add(type);
			}
			WfsData.VectorData vdata = new WfsData.VectorData();
			vdata.setPolygon(polygon);
			coor.getVectorData().add(vdata);
			break;
		case POLYLINE:
			WfsData.VectorData.Polyline polyline = new WfsData.VectorData.Polyline();
			for (LatLon ll : input.getFeature()) {
				CoordinateType type = new CoordinateType();
				type.setLat(String.valueOf(ll.getLatitude()));
				type.setLon(String.valueOf(ll.getLongitude()));
				polyline.getLocation().add(type);
			}
			WfsData.VectorData vdata2 = new WfsData.VectorData();
			vdata2.setPolyline(polyline);
			coor.getVectorData().add(vdata2);
			break;
		case FILE:
			WfsData.LocationFile lfile = new WfsData.LocationFile();
			lfile.setFileName(readyFile(jobDir, input.getFilename()));
			coor.setLocationFile(lfile);
			break;
		default:
			break;
		}
		retval.setCoordinates(coor);
		return retval;
	}

	private Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile CreateFuelPatchFile(FuelPatch input) {
		Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile retval = new Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile();
		retval.setComments(input.getComments());
		Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile.FromFuel from = new Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile.FromFuel();
		if (input.getFromFuelRule() != null) {
			from.setFromFuelRule(input.getFromFuelRule().value);
		}
		else {
			from.setFromFuelName(input.getFromFuel());
		}
		retval.setFromFuel(from);
		retval.setName(input.getId());
		retval.setToFuel(input.getToFuel());
		Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile.Geographic geo = new Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile.Geographic();
		Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile.Geographic.Coordinates coor = new Data.Job.Inputs.Files.FuelPatchFiles.FuelPatchFile.Geographic.Coordinates();
		geo.setCoordinates(coor);
		switch (input.getType()) {
		case POLYGON:
			List<WfsData.VectorData> vectors = coor.getVectorData();
			WfsData.VectorData vector = new WfsData.VectorData();
			WfsData.VectorData.Polygon polygon = new WfsData.VectorData.Polygon();
			List<CoordinateType> coors = polygon.getLocation();
			for (LatLon ll : input.getFeature()) {
				CoordinateType point = new CoordinateType();
				point.setLat(String.valueOf(ll.getLatitude()));
				point.setLon(String.valueOf(ll.getLongitude()));
				coors.add(point);
			}
			vectors.add(vector);
			break;
		case LANDSCAPE:
			geo.setLandscape(true);
			break;
		case FILE:
			WfsData.LocationFile file = new WfsData.LocationFile();
			file.setFileName(readyFile(jobDir, input.getFilename()));
			coor.setLocationFile(file);
			break;
		default:
			break;
		}
		retval.setGeographic(geo);
		return retval;
	}

	private ca.wise.fgm.xml.WeatherStation CreateWeatherStation(WeatherStation input) {
		ca.wise.fgm.xml.WeatherStation retval = new ca.wise.fgm.xml.WeatherStation();
		retval.setName(input.getName());
		retval.setComments(input.getComments());
		retval.setElevation(String.valueOf(input.getElevation()));
		CoordinateType ct = new CoordinateType();
		ct.setLat(String.valueOf(input.getLocation().getLatitude()));
		ct.setLon(String.valueOf(input.getLocation().getLongitude()));
		retval.setLocation(ct);
		ca.wise.fgm.xml.WeatherStation.WeatherStreams str = new ca.wise.fgm.xml.WeatherStation.WeatherStreams();
		for (int i = 0; i < input.getStreams().size(); i++) {
			str.getWeatherStream().add(CreateWeatherStream(input.getStreams().get(i)));
		}
		retval.setWeatherStreams(str);
		return retval;
	}

	private ca.wise.fgm.xml.WeatherStream CreateWeatherStream(WeatherStream input) {
		ca.wise.fgm.xml.WeatherStream retval = new ca.wise.fgm.xml.WeatherStream();
		retval.setName(input.getName());
		retval.setComments(input.getComments());
		retval.setFileName(readyFile(jobDir, input.getFilename()));
		ca.wise.fgm.xml.WeatherStream.Hffmc hffmc = new ca.wise.fgm.xml.WeatherStream.Hffmc();
		hffmc.setHffmcMethod(input.getHffmcMethod().toString());
		hffmc.setHour(input.getHffmcHour());
		hffmc.setValue(String.valueOf(input.getHffmcValue()));
		retval.setHffmc(hffmc);
		retval.setStartDate(CreateDate(input.getStartTime()));
		retval.setEndDate(CreateDate(input.getEndTime()));
		ca.wise.fgm.xml.WeatherStream.StartingCodes start = new ca.wise.fgm.xml.WeatherStream.StartingCodes();
		start.setFfmc(String.valueOf(input.getStartingFfmc()));
		start.setDmc(String.valueOf(input.getStartingDmc()));
		start.setDc(String.valueOf(input.getStartingDc()));
		start.setPrecip(String.valueOf(input.getStartingPrecip()));
		retval.setStartingCodes(start);
		if (input.getDiurnalTemperatureAlpha() != null) {
			ca.wise.fgm.xml.WeatherStream.DiurnalParameters d = new ca.wise.fgm.xml.WeatherStream.DiurnalParameters();
			ca.wise.fgm.xml.WeatherStream.DiurnalParameters.Temperature t = new ca.wise.fgm.xml.WeatherStream.DiurnalParameters.Temperature();
			t.setAlpha(String.valueOf(input.getDiurnalTemperatureAlpha()));
			t.setBeta(String.valueOf(input.getDiurnalTemperatureBeta()));
			t.setGamma(String.valueOf(input.getDiurnalTemperatureGamma()));
			d.setTemperature(t);
			ca.wise.fgm.xml.WeatherStream.DiurnalParameters.WindSpeed w = new ca.wise.fgm.xml.WeatherStream.DiurnalParameters.WindSpeed();
			w.setAlpha(String.valueOf(input.getDiurnalWindSpeedAlpha()));
			w.setBeta(String.valueOf(input.getDiurnalWindSpeedBeta()));
			w.setGamma(String.valueOf(input.getDiurnalWindSpeedGamma()));
			d.setWindSpeed(w);
			retval.setDiurnalParameters(d);
		}
		return retval;
	}

	private Data.Job.Inputs.Ignitions.Ignition CreateIgnition(Ignition input) {
		Data.Job.Inputs.Ignitions.Ignition retval = new Data.Job.Inputs.Ignitions.Ignition();
		retval.setName(input.getName());
		retval.setComments(input.getComments());
		retval.setStartTime(CreateDateTime(input.getStartTime()));
		Data.Job.Inputs.Ignitions.Ignition.Coordinates coor = new Data.Job.Inputs.Ignitions.Ignition.Coordinates();
		switch (input.getType()) {
		case FILE:
			WfsData.LocationFile file = new WfsData.LocationFile();
			file.setFileName(readyFile(jobDir, input.getFilename()));
			coor.setLocationFile(file);
			break;
		case POINT:
			WfsData.VectorData.Point point = new WfsData.VectorData.Point();
			for (LatLon ll : input.getFeature()) {
				CoordinateType type = new CoordinateType();
				type.setLat(String.valueOf(ll.getLatitude()));
				type.setLon(String.valueOf(ll.getLongitude()));
				point.getLocation().add(type);
			}
			WfsData.VectorData vdata = new WfsData.VectorData();
			vdata.setPoint(point);
			coor.getVectorData().add(vdata);
			break;
		case POLYGON:
			WfsData.VectorData.Polygon polygon = new WfsData.VectorData.Polygon();
			for (LatLon ll : input.getFeature()) {
				CoordinateType type = new CoordinateType();
				type.setLat(String.valueOf(ll.getLatitude()));
				type.setLon(String.valueOf(ll.getLongitude()));
				polygon.getLocation().add(type);
			}
			WfsData.VectorData vdata2 = new WfsData.VectorData();
			vdata2.setPolygon(polygon);
			coor.getVectorData().add(vdata2);
			break;
		case POLYLINE:
			WfsData.VectorData.Polyline polyline = new WfsData.VectorData.Polyline();
			for (LatLon ll : input.getFeature()) {
				CoordinateType type = new CoordinateType();
				type.setLat(String.valueOf(ll.getLatitude()));
				type.setLon(String.valueOf(ll.getLongitude()));
				polyline.getLocation().add(type);
			}
			WfsData.VectorData vdata3 = new WfsData.VectorData();
			vdata3.setPolyline(polyline);
			coor.getVectorData().add(vdata3);
			break;
		default:
			break;
		}
		retval.setCoordinates(coor);
		return retval;
	}
	
	private <T> T requireNonNullElse(T value, T def) {
	    if (value == null)
	        return def;
	    return value;
	}

	private Outputs.VectorFiles.VectorFile CreateVectorFile(VectorFile input) {
		Outputs.VectorFiles.VectorFile retval = new Outputs.VectorFiles.VectorFile();
		retval.setScenName(input.getScenarioName());
		retval.setFileName(readyOutputFile(input.getFilename()));
		retval.setMultiplePerim(requireNonNullElse(input.getMultPerim(), false));
		retval.setRemoveIslands(requireNonNullElse(input.getRemoveIslands(), false));
		retval.setMergeContact(requireNonNullElse(input.getMergeContact(), false));
		retval.setPerimActive(requireNonNullElse(input.getPerimActive(), false));
		Outputs.VectorFiles.VectorFile.PerimTime tm = new Outputs.VectorFiles.VectorFile.PerimTime();
		tm.setStartTime(CreateDateTime(input.getPerimStartTime()));
		tm.setEndTime(CreateDateTime(input.getPerimEndTime()));
		retval.setPerimTime(tm);
		Outputs.VectorFiles.VectorFile.Metadata meta = new Outputs.VectorFiles.VectorFile.Metadata();
		if (input.getMetadata() != null) {
			meta.setVersion(input.getMetadata().isVersion());
			meta.setScenName(input.getMetadata().isScenName());
			meta.setJobName(input.getMetadata().isJobName());
			meta.setIgName(input.getMetadata().isIgName());
			meta.setSimDate(input.getMetadata().isSimDate());
			meta.setFireSize(input.getMetadata().isFireSize());
			meta.setPerimTotal(input.getMetadata().isPerimTotal());
			meta.setPerimActive(input.getMetadata().isPerimActive());
			if (input.getMetadata().getPerimUnit() != null && input.getMetadata().getPerimUnit() != DistanceUnit.DEFAULT) {
				meta.setPerimUnit(input.getMetadata().getPerimUnit().toPerimString());
			}
			if (input.getMetadata().getAreaUnit() != null && input.getMetadata().getAreaUnit() != AreaUnit.DEFAULT) {
				meta.setAreaUnit(input.getMetadata().getAreaUnit().toPerimString());
			}
		}
		retval.setMetadata(meta);

		return retval;
	}

	private Outputs.SummaryFiles.Summary CreateSummaryFile(SummaryFile input) {
		Outputs.SummaryFiles.Summary retval = new Outputs.SummaryFiles.Summary();
		retval.setFileName(readyOutputFile(input.getFilename()));
		retval.setScenName(input.getScenName());
	    if (input.getOutputs().getOutputApplication() != null) {
			retval.setOutputApplication(input.getOutputs().getOutputApplication());
	    }
	    if (input.getOutputs().getOutputGeoData() != null) {
			retval.setOutputGeoData(input.getOutputs().getOutputGeoData());
	    }
	    if (input.getOutputs().getOutputScenario() != null) {
			retval.setOutputScenario(input.getOutputs().getOutputScenario());
	    }
	    if (input.getOutputs().getOutputScenarioComments() != null) {
			retval.setOutputScenarioComments(input.getOutputs().getOutputScenarioComments());
	    }
	    if (input.getOutputs().getOutputInputs() != null) {
			retval.setOutputInputs(input.getOutputs().getOutputInputs());
	    }
		if (input.getOutputs().getOutputLandscape() != null) {
			retval.setOutputLandscape(input.getOutputs().getOutputLandscape());
		}
		if (input.getOutputs().getOutputFBPPatches() != null) {
			retval.setOutputFBPPatches(input.getOutputs().getOutputFBPPatches());
		}
		if (input.getOutputs().getOutputWxPatches() != null) {
			retval.setOutputWxPatches(input.getOutputs().getOutputWxPatches());
		}
		if (input.getOutputs().getOutputIgnitions() != null) {
			retval.setOutputIgnitions(input.getOutputs().getOutputIgnitions());
		}
		if (input.getOutputs().getOutputWxStreams() != null) {
			retval.setOutputWxStreams(input.getOutputs().getOutputWxStreams());
		}
		if (input.getOutputs().getOutputFBP() != null) {
			retval.setOutputFBP(input.getOutputs().getOutputFBP());
		}
		return retval;
	}

	private Outputs.GridFiles.GridFile CreateGridFile(ca.wise.api.output.GridFile input) {
		Outputs.GridFiles.GridFile retval = new Outputs.GridFiles.GridFile();
		retval.setScenName(input.getScenarioName());
		retval.setFileName(readyOutputFile(input.getFilename()));
		retval.setExportTime(CreateDateTime(input.getOutputTime()));
		retval.setStatistic(input.getStatistic().toString());
		retval.setInterpolationMethod(input.getInterpMethod().toString());
		return retval;
	}

	@Override
	public IDataWriter combine(String jobName) {
		Data retval = new Data();
		retval.setSchemaVersion(new BigDecimal(1.0));
		InitParams(retval);
		InitJob(retval, jobName);

		retval.getJob().setComments(input.getComments());
		//set the grid projection
		Data.Job.Inputs.Files.ProjectionFile pfile = new Data.Job.Inputs.Files.ProjectionFile();
		pfile.setFileName(readyFile(jobDir, input.getInputs().getFiles().getProjFile()));
		retval.getJob().getInputs().getFiles().setProjectionFile(pfile);
		//set the LUT file
		Data.Job.Inputs.Files.LutFile lfile = new Data.Job.Inputs.Files.LutFile();
		lfile.setFileName(readyFile(jobDir, input.getInputs().getFiles().getLutFile()));
		retval.getJob().getInputs().getFiles().setLutFile(lfile);
		//set the fuel map file
		Data.Job.Inputs.Files.FuelmapFile ffile = new Data.Job.Inputs.Files.FuelmapFile();
		WcsData.LocationFile file = new WcsData.LocationFile();
		file.setFileName(readyFile(jobDir, input.getInputs().getFiles().getFuelmapFile()));
		WcsData wdata = new WcsData();
		wdata.setLocationFile(file);
		ffile.setContents(wdata);
		retval.getJob().getInputs().getFiles().setFuelmapFile(ffile);
		//set the elevation file
		Data.Job.Inputs.Files.ElevationFile efile = new Data.Job.Inputs.Files.ElevationFile();
		WcsData.LocationFile file2 = new WcsData.LocationFile();
		file2.setFileName(readyFile(jobDir, input.getInputs().getFiles().getElevFile()));
		WcsData wdata2 = new WcsData();
		wdata2.setLocationFile(file2);
		efile.setContents(wdata2);
		retval.getJob().getInputs().getFiles().setElevationFile(efile);
		for (FuelBreak fl : input.getInputs().getFiles().getFuelBreakFiles()) {
			retval.getJob().getInputs().getFiles().getFuelbreakFiles().getFuelbreakFile().add(CreateFuelBreakFile(fl));
		}
		for (FuelPatch fp : input.getInputs().getFiles().getFuelPatchFiles()) {
			retval.getJob().getInputs().getFiles().getFuelPatchFiles().getFuelPatchFile().add(CreateFuelPatchFile(fp));
		}
		for (WeatherGrid fl : input.getInputs().getFiles().getWeatherGridFiles()) {
			retval.getJob().getInputs().getFiles().getWeatherGridFiles().getWeatherFile().add(CreateWeatherGrid(fl));
		}
		for (WeatherPatch wp : input.getInputs().getFiles().getWeatherPatchFiles()) {
			retval.getJob().getInputs().getFiles().getWeatherPatchFiles().getPatchFile().add(CreateWeatherPatch(wp));
		}
		for (GridFile fl : input.getInputs().getFiles().getGridFiles()) {
			retval.getJob().getInputs().getFiles().getGridFiles().getGridFile().add(CreateInputGridFile(fl));
		}
		for (WeatherStation st : input.getInputs().getWeatherStations()) {
			retval.getJob().getInputs().getWeatherStations().getWeatherStation().add(CreateWeatherStation(st));
		}
		for (Ignition ig : input.getInputs().getIgnitions()) {
			retval.getJob().getInputs().getIgnitions().getIgnition().add(CreateIgnition(ig));
		}
		int index = 0;
		for (Scenario sc : input.getInputs().getScenarios()) {
			if (Strings.isNullOrEmpty(sc.getScenToCopy())) {
				retval.getJob().getInputs().getScenarios().getScenarioOrScenarioCopy().add(CreateScenario(sc, index));
				index++;
			}
		}
		for (Scenario sc : input.getInputs().getScenarios()) {
			if (Strings.isNullOrEmpty(sc.getScenToCopy())) {
				retval.getJob().getInputs().getScenarios().getScenarioOrScenarioCopy().add(CreateScenarioCopy(sc, index));
				index++;
			}
		}
		Data.Job.Inputs.Timezone tz = retval.getJob().getInputs().getTimezone();
		tz.setDst(input.getInputs().getTimezone().isDst());
		tz.setOffset(CreateDuration(input.getInputs().getTimezone().getOffset()));
		for (VectorFile vf : input.getOutputs().getVectorFiles()) {
			retval.getJob().getOutputs().getVectorFiles().getVectorFile().add(CreateVectorFile(vf));
		}
		for (SummaryFile sf : input.getOutputs().getSummaryFiles()) {
			retval.getJob().getOutputs().getSummaryFiles().getSummary().add(CreateSummaryFile(sf));
		}
		for (ca.wise.api.output.GridFile gf : input.getOutputs().getGridFiles()) {
			retval.getJob().getOutputs().getGridFiles().getGridFile().add(CreateGridFile(gf));
		}

		return new XMLDataWriter(baseDir, retval);
	}
	
	public static class XMLDataWriter implements IDataWriter {
		
		private Data data;
		private String jobDir;
		
		private XMLDataWriter(String jobDir, Data data) {
			this.jobDir = jobDir;
			this.data = data;
		}
		
		@Override
		public boolean write(String jobname) {
			boolean retval = false;
			File fl = Paths.get(jobDir, jobname, "job.xml").toFile();
        	try {
            	JAXBContext jaxbContext = JAXBContext.newInstance(Data.class);
            	Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            	jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            	jaxbMarshaller.marshal(data, fl);
            	retval = true;
        	}
        	catch (JAXBException ex) {
        		ex.printStackTrace();
        	}
        	return retval;
		}
		
		@Override
		public byte[] stream(String jobname) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
        	try {
            	JAXBContext jaxbContext = JAXBContext.newInstance(Data.class);
            	Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            	jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            	jaxbMarshaller.marshal(data, stream);
        	}
        	catch (JAXBException ex) {
        		ex.printStackTrace();
        	}
        	return stream.toByteArray();
		}
		
		@Override
		public int getCoreCount() {
			return data.getParams().getHardware().getCores();
		}

		@Override
		public OutputType getType() {
			return OutputType.XML;
		}

        @Override
        public int getPriority() {
            return 0;
        }
        
        @Override
        public boolean isValidate() {
            return false;
        }
	}
	
	public static class OutputOptions implements IOutputOptions {

		public boolean singleFile = false;
		
		@Override
		public boolean shouldStream() {
			return false;
		}
	}
}
