package ca.wise.fgm.output;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Strings;

import ca.wise.fgm.tools.DoubleHelper;
import ca.wise.fgm.tools.IntegerHelper;
import ca.wise.fgm.tools.WISELogger;
import ca.wise.api.LoadBalanceType;
import ca.wise.api.WISE;
import ca.wise.api.admin.MqttSettings;
import ca.wise.api.input.AssetFile;
import ca.wise.api.input.AssetOperation;
import ca.wise.api.input.AssetReference;
import ca.wise.api.input.AssetShapeType;
import ca.wise.api.input.AttributeEntry;
import ca.wise.api.input.BurningConditionRelative;
import ca.wise.api.input.BurningConditions;
import ca.wise.api.input.FromFuel;
import ca.wise.api.input.FuelBreak;
import ca.wise.api.input.FuelBreakType;
import ca.wise.api.input.FuelOption;
import ca.wise.api.input.FuelPatch;
import ca.wise.api.input.FuelPatchType;
import ca.wise.api.input.GridFileType;
import ca.wise.api.input.GustingOptions;
import ca.wise.api.input.HFFMCMethod;
import ca.wise.api.input.Ignition;
import ca.wise.api.input.IgnitionReference;
import ca.wise.api.input.IgnitionType;
import ca.wise.api.input.LayerInfo;
import ca.wise.api.input.SeasonalCondition;
import ca.wise.api.input.SeasonalConditionType;
import ca.wise.api.input.StationStream;
import ca.wise.api.input.StopModellingOptions;
import ca.wise.api.input.TargetFile;
import ca.wise.api.input.TargetReference;
import ca.wise.api.input.Timezone;
import ca.wise.api.input.WeatherGrid;
import ca.wise.api.input.WeatherGridGridFile;
import ca.wise.api.input.WeatherGridSector;
import ca.wise.api.input.WeatherGridType;
import ca.wise.api.input.WeatherPatch;
import ca.wise.api.input.WeatherPatchOperation;
import ca.wise.api.input.WeatherPatchType;
import ca.wise.api.input.WeatherStation;
import ca.wise.api.input.WeatherStream;
import ca.wise.api.output.AssetStatsFile;
import ca.wise.api.output.ExportTimeOverride;
import ca.wise.api.output.FuelGridFile;
import ca.wise.api.output.GeoServerOutputStreamInfo;
import ca.wise.api.output.GlobalStatistics;
import ca.wise.api.output.GridFile;
import ca.wise.api.output.GridFileCompression;
import ca.wise.api.output.GridFileInterpolation;
import ca.wise.api.output.MqttOutputStreamInfo;
import ca.wise.api.output.PerimeterTimeOverride;
import ca.wise.api.output.StatsFile;
import ca.wise.api.output.StatsFileType;
import ca.wise.api.output.SummaryFile;
import ca.wise.api.output.SummaryOutputs;
import ca.wise.api.output.VectorFile;
import ca.wise.api.output.VectorMetadata;
import ca.wise.api.units.AngleUnit;
import ca.wise.api.units.AreaUnit;
import ca.wise.api.units.CoordinateUnit;
import ca.wise.api.units.DistanceUnit;
import ca.wise.api.units.EnergyUnit;
import ca.wise.api.units.IntensityUnit;
import ca.wise.api.units.MassAreaUnit;
import ca.wise.api.units.MassUnit;
import ca.wise.api.units.TemperatureUnit;
import ca.wise.api.units.TimeUnit;
import ca.wise.api.units.VelocityUnit;
import ca.wise.api.units.VolumeUnit;
import ca.hss.times.TimeZoneInfo;
import ca.hss.times.WorldLocation;

public class SocketDecoder {
	public static final boolean DEBUG = false;
	
	private enum PHP_TYPE {
		GLOBAL_COMMENTS("GLOBALCOMMENTS"),
		PROJ("projfile"),
		LUT("lutfile"),
		FUELMAP("fuelmapfile"),
		ELEVATION("elevationfile"),
		FUELBREAK("fuelbreakfile"),
		FUELPATCH("fuelpatch"),
		WEATHER_GRID("weathergrid"),
		WEATHER_GRID_V2("weathergrid_2"),
		WEATHER_PATCH("weatherpatch"),
		INPUT_GRID_FILE("inputgridfile"),
		WEATHERSTATION("weatherstation"),
		WEATHERSTREAM("weatherstream"),
		IGNITION("ignition"),
		ASSET_FILE("asset_file"),
		TARGET_FILE("target_file"),
		SCENARIONAME("scenarioname"),
		DISPLAY_INTERVAL("displayinterval"),
		SCENARIO_TO_COPY("scenariotocopy"),
		STARTTIME("starttime"),
		ENDTIME("endtime"),
		COMMENTS("comments"),
		MAXACCTS("maxaccts"),
		DISTRES("distres"),
		PERIMRES("perimres"),
		MINSPREADROS("fgm_minspreadros"),
		STOPGRIDEND("stopatgridends"),
		BREACHING("breaching"),
		DYNAMICTHRESHOLD("fgm_dynamicthreshold"),
		SPOTTING("spotting"),
		PURGENONDISPLAY("fgm_purgenondisplay"),
		FGM_DX("fgm_dx"),
		FGM_DY("fgm_dy"),
		FGM_DT("fgm_dt"),
		FGM_DWD("fgm_dwd"),
		FGM_OWD("fgm_owd"),
		FGM_DVD("fgm_dvd"),
		FGM_OVD("fgm_ovd"),
		GROWTHAPPLIED("fgm_growthPercApplied"),
		GROWTHPERC("fgm_growthPercentile"),
		SUPPRESS_TIGHT_CONCAVE("fgm_suppressTightConcave"),
		NON_FUELS_AS_VECTOR_BREAKS("fgm_nonFuelsAsVectorBreaks"),
		NON_FUELS_TO_VECTOR_BREAKS("fgm_nonFuelsToVectorBreaks"),
		USE_INDEPENDENT_TIMESTEPS("fgm_useIndependentTimesteps"),
		PERIMETER_SPACING("fgm_perimeterSpacing"),
		FALSE_ORIGIN("fgm_falseOrigin"),
		FALSE_SCALING("fgm_falseScaling"),
		TERRAINEFF("terraineffect"),
		WINDEFF("windeffect"),
		PEROVER("peroverride"),
		PEROVERVAL("peroverridevalue"),
		NODATAELEV("nodataelev"),
		TERRAIN("fmc_terrain"),
		ACCURATELOCATION("fmc_accuratelocation"),
		FWISPACIAL("fwispacinterp"),
		FWIFROMSPACIAL("fwifromspacweather"),
		HISTORYFWI("historyonfwi"),
		BURNINGCONDON("burningconditionon"),
		FWITEMPORALINTERP("fwitemporalinterp"),
		BURNINGCONDITION("burningcondition"),
		VECTOR_REF("vectorref"),
		STREAM_REF("streamref"),
		LAYER_INFO("layerinfo"),
		PRIMARY_STREAM("primarystream"),
		IGNITION_REF("ignitionref"),
		ASSET_REF("asset_ref"),
		WIND_TARGET_REF("wind_target_ref"),
		VECTOR_TARGET_REF("vector_target_ref"),
		TIMEZONE("timezone"),
		TIMETOEXEC("timetoexec"),
		GRIDINFO("gridinfo"),
		LOCATIONINFO("locationinfo"),
		ELEVINFO("elevationinfo"),
		INPUTSUMM("inputsummary"),
		VECTORFILE("vectorfile"),
		SUMMARYFILE("summaryfile"),
		GRIDFILE("gridfile"),
		FUEL_GRID_EXPORT("fuel_grid_export"),
		ASSET_STATS_EXPORT("asset_stats_export"),
		STATSFILE("statsfile"),
		SIMULATION_PROPERTIES("simulation_properties"),
		MQTT_HOST("mng_mqtt_host"),
		MQTT_PORT("mng_mqtt_port"),
		MQTT_USERNAME("mng_mqtt_username"),
		MQTT_PASSWORD("mng_mqtt_password"),
		MQTT_TOPIC("mng_mqtt_topic"),
		MQTT_QOS("mng_mqtt_qos"),
		MQTT_VERBOSITY("mng_mqtt_verbosity"),
		EMIT_STATISTIC("mng_statistic"),
		OUTPUT_STREAM("output_stream"),
		FILE_ATTACHMENT("file_attachment"),
		FILE_ATTACHMENT_END("file_attachment_end"),
		FUEL_OPTION("fuel_option_setting"),
		FGM_SETTINGS("fgm_settings"),
		UNIT_SETTINGS("export_units"),
		SCENARIO_SEASONAL("scenario_seasonal"),
		STOP_MODELLING("stop_modelling"),
		GUSTING_OPTIONS("gusting_options");
		
		private String val;
		private static List<String> valList = null;
		
		PHP_TYPE(String val) {
			this.val = val;
		}
		
		public static List<String> stringValues() {
			if (valList == null) {
				valList = new ArrayList<String>();
				PHP_TYPE[] v =PHP_TYPE.values();
				for (PHP_TYPE p : v) {
					valList.add(p.val);
				}
			}
			return valList;
		}
	}
	
	static final String TYPE_SCENARIO_BEGIN = "scenariostart";
	static final String TYPE_SCENARIO_END = "scenarioend";
	
	static final String MSG_STARTUP = "STARTUP";
	static final String MSG_SHUTDOWN = "SHUTDOWN";
	
	static final String SOCKET_PORT = "32477";
	static final String SOCKET_ADDRESS = "192.168.0.10";
	static final String SOCKET_NEWLINE = "\n";
	
	public WISE wise = new WISE();
	
	public String gis = ""; //I don't yet know what this will need to be.
	public FileAttachment currentAttachment = null;
	public String currentLineEnd = null;
	
	protected String nextVal = null;
	protected ca.wise.api.input.Scenario currentScen = null;
	
	public class NextType {
		public int binarySize = -1;
		public String errorMessage = null;
		
		public NextType() { }
		
		public NextType(String error) {
			errorMessage = error;
		}
	}
	
	/**
	 * Can the passed value be decoded by this class.
	 * @param key The key to check if it can be decoded.
	 * @param newLine What character(s) were found to cause the current line read to end.
	 * @param skippedLineFeed Did the current key begin with a skipped newline character from the last key.
	 * @return Empty if this class cannot decode the key, true if the key can be decoded but another line
	 *         is required as part of the data, false if this class can decode the value and no further
	 *         information is needed.
	 */
	public Optional<Boolean> canDecode(String key, String newLine, boolean skippedLineFeed) {
		Optional<Boolean> retval = Optional.empty();
		nextVal = key;
		//add an attachment that was being built to the list
		if (nextVal.equals(PHP_TYPE.FILE_ATTACHMENT_END.val)) {
			retval = Optional.of(false);
			currentLineEnd = null;
			if (currentAttachment != null) {
				if (currentAttachment.data != null)
				wise.getAttachments().add(new ca.wise.api.FileAttachment(currentAttachment.filename, currentAttachment.data));
				else
				wise.getAttachments().add(new ca.wise.api.FileAttachment(currentAttachment.filename, currentAttachment.contents.toString()));
				currentAttachment = null;
			}
		}
		else if (PHP_TYPE.stringValues().contains(key)) {
			retval = Optional.of(true);
		}
		else if (currentAttachment != null) {
			retval = Optional.of(false);
			if (skippedLineFeed)
			currentAttachment.contents.append("\n");
			if (currentLineEnd != null)
			currentAttachment.contents.append(currentLineEnd);
			currentAttachment.contents.append(nextVal);
			currentLineEnd = newLine;
		}
		else if (key.equals(TYPE_SCENARIO_BEGIN)) {
			currentScen = new ca.wise.api.input.Scenario();
			retval = Optional.of(false);
		}
		else if (key.equals(TYPE_SCENARIO_END)) {
			if (currentScen != null) {
				wise.getInputs().getScenarios().add(currentScen);
				currentScen = null;
			}
			retval = Optional.of(false);
		}
		else
			nextVal = null;
		return retval;
	}
	
	public NextType decode(String data, String newLine, boolean skippedLineFeed) {
		NextType retval = new NextType();
		//either start a new attachment or append data to an attachments contents
		if (nextVal.equals(PHP_TYPE.FILE_ATTACHMENT.val)) {
			String[] split = data.split("\\|");
			currentAttachment = new FileAttachment();
			if (split.length == 1) {
				currentAttachment.filename = data;
			}
			else if (split.length == 2) {
				currentAttachment.filename = split[0];
				retval.binarySize = Integer.parseInt(split[1]);
			}
			currentAttachment.contents = new StringBuilder();
		}
		else {
			retval.errorMessage = decodeInternal(data, newLine, skippedLineFeed);
		}
		
		return retval;
	}
	
	public NextType decode(char[] data) {
		if (currentAttachment != null) {
			currentAttachment.data = new byte[data.length];
			for (int i = 0; i < data.length; i++)
			currentAttachment.data[i] = (byte)data[i];
			
			return new NextType();
		}
		
		return new NextType("Not currently parsing an attachment");
	}
	
	@SuppressWarnings("deprecation")
	private String decodeInternal(String data, String newLine, boolean skippedLineFeed) {
		if (nextVal.equals(PHP_TYPE.GLOBAL_COMMENTS.val)) {
			wise.setComments(data);
			WISELogger.debug("Found Global Comment: "  + wise.getComments());
		}
		else if (nextVal.equals(PHP_TYPE.PROJ.val)) {
			wise.getInputs().getFiles().setProjFile(data);
			WISELogger.debug("Found Projection File: "  + wise.getInputs().getFiles().getProjFile());
		}
		else if (nextVal.equals(PHP_TYPE.LUT.val)) {
			wise.getInputs().getFiles().setLutFile(data);
			WISELogger.debug("Found LUT File: "  + wise.getInputs().getFiles().getLutFile());
		}
		else if (nextVal.equals(PHP_TYPE.FUELMAP.val)) {
			wise.getInputs().getFiles().setFuelmapFile(data);
			WISELogger.debug("Found Fuel Map File: " + wise.getInputs().getFiles().getFuelmapFile());
		}
		else if (nextVal.equals(PHP_TYPE.ELEVATION.val)) {
			wise.getInputs().getFiles().setElevFile(data);
			WISELogger.debug("Found Elevation File: " + wise.getInputs().getFiles().getElevFile());
		}
		else if (nextVal.equals(PHP_TYPE.FUELBREAK.val)) {
			String list[] = data.split("\\|");
			if (list.length >= 5) {
				try {
					FuelBreak fb = FuelBreak.builder()
							.id(list[0])
							.width(Double.parseDouble(list[1]))
							.comments(list[2])
							.type(FuelBreakType.fromValue(list[3]))
							.build();
					if (fb.getType() == FuelBreakType.FILE) {
						fb.setFilename(list[4]);
					}
					else if (fb.getType() == FuelBreakType.POLYLINE || fb.getType() == FuelBreakType.POLYGON) {
						for (int i = 4; i < list.length; i += 2) {
							if (i < list.length - 1) {
								fb.getFeature().add(new ca.wise.api.LatLon(Double.parseDouble(list[i]), Double.parseDouble(list[i + 1])));
							}
						}
					}
					wise.getInputs().getFiles().getFuelBreakFiles().add(fb);
					WISELogger.debug("Found a Fuel Break File: " + fb.getId());
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Fuel Break File";
				}
			}
			else
				return "Incorrectly Formatted Fuel Break File (2)";
		}
		else if (nextVal.equals(PHP_TYPE.FUELPATCH.val)) {
			String list[] = data.split("\\|");
			if (list.length > 5) {
				try {
					FuelPatch patch = FuelPatch.builder()
					.id(list[0])
					.comments(list[1])
					.build();
					if (IntegerHelper.isInteger(list[2]))
						patch.setToFuelIndex(Integer.parseInt(list[2]));
					else
						patch.setToFuel(list[2]);
					if (list[3].equals("ifuel"))
						patch.setFromFuelIndex(Integer.parseInt(list[4]));
					else if (list[3].equals("rule"))
						patch.setFromFuelRule(FromFuel.fromValue(list[4]));
					else
						patch.setFromFuel(list[4]);
					patch.setType(FuelPatchType.fromValue(list[5]));
					if (patch.getType() == FuelPatchType.FILE) {
						patch.setFilename(list[6]);
					}
					else if (patch.getType() == FuelPatchType.POLYGON) {
						for (int i = 6; i < list.length; i += 2) {
							if (i < list.length - 1) {
								patch.getFeature().add(new ca.wise.api.LatLon(Double.parseDouble(list[i]), Double.parseDouble(list[i + 1])));
							}
						}
					}
					wise.getInputs().getFiles().getFuelPatchFiles().add(patch);
					WISELogger.debug("Found a Fuel Patch File: " + patch.getId());
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Fuel Patch File";
				}
			}
			else
				return "Incorrectly Formatted Fuel Patch File (2)";
		}
		else if (nextVal.equals(PHP_TYPE.WEATHER_GRID.val)) {
			String list[] = data.split("\\|");
			if (list.length > 6) {
				try {
					WeatherGrid grid = WeatherGrid.builder()
							.id(list[0])
							.comments(list[1])
							.startTime(list[2])
							.endTime(list[3])
							.startTimeOfDay(list[4])
							.endTimeOfDay(list[5])
							.type(WeatherGridType.fromValue(list[6]))
							.build();
					for (int i = 7; i < list.length; i += 4) {
						if (i < list.length - 2) {
							grid.getGridData().add(
							WeatherGridGridFile.builder()
									.speed(Double.parseDouble(list[i]))
									.sector(WeatherGridSector.fromValue(Integer.parseInt(list[i + 1])))
									.filename(list[i + 2])
									.projection(list[i + 3])
									.build());
						}
					}
					wise.getInputs().getFiles().getWeatherGridFiles().add(grid);
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Weather Grid File";
				}
			}
			else
				return "Incorrectly Formatted Weather Grid File";
		}
		else if (nextVal.equals(PHP_TYPE.WEATHER_GRID_V2.val)) {
			String list[] = data.split("\\|");
			if (list.length > 6) {
				try {
					WeatherGrid grid = WeatherGrid.builder()
							.id(list[0])
							.comments(list[1])
							.startTime(list[2])
							.endTime(list[3])
							.startTimeOfDay(list[4])
							.endTimeOfDay(list[5])
							.type(WeatherGridType.fromValue(list[6]))
							.build();
					int length = Integer.parseInt(list[7]);
					for (int j = 0, i = 8; j < length && i < list.length; j++, i += 4) {
						if (i < list.length - 2) {
							grid.getGridData().add(
							WeatherGridGridFile.builder()
									.speed(Double.parseDouble(list[i]))
									.sector(WeatherGridSector.fromValue(Integer.parseInt(list[i + 1])))
									.filename(list[i + 2])
									.projection(list[i + 3])
									.build());
						}
					}
					int index = 8 + (length * 4);
					if (index < list.length) {
						grid.setDefaultValuesFile(list[index]);
						grid.setDefaultValuesProjection(list[index + 1]);
						//clear out the values if placeholders were passed
						if (grid.getDefaultValuesFile().equals("-1")) {
							grid.setDefaultValuesFile(null);
							grid.setDefaultValuesProjection(null);
						}
					}
					wise.getInputs().getFiles().getWeatherGridFiles().add(grid);
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Weather Grid File";
				}
			}
			else
				return "Incorrectly Formatted Weather Grid File";
		}
		else if (nextVal.equals(PHP_TYPE.WEATHER_PATCH.val)) {
			String list[] = data.split("\\|");
			if (list.length > 6) {
				try {
					WeatherPatch patch = WeatherPatch.builder()
							.id(list[0])
							.comments(list[1])
							.startTime(list[2])
							.endTime(list[3])
							.startTimeOfDay(list[4])
							.endTimeOfDay(list[5])
							.build();
					for (int i = 6; i < list.length; i++) {
						if (list[i].equals("temperature")) {
							patch.setTemperature(WeatherPatch.Temperature.builder()
									.operation(WeatherPatchOperation.fromValue(Integer.parseInt(list[i + 1])))
									.value(Double.parseDouble(list[i + 2]))
									.build());
							i += 2;
						}
						else if (list[i].equals("rh")) {
							patch.setRh(WeatherPatch.RelativeHumidity.builder()
									.operation(WeatherPatchOperation.fromValue(Integer.parseInt(list[i + 1])))
									.value(Double.parseDouble(list[i + 2]))
									.build());
							i += 2;
						}
						else if (list[i].equals("precip")) {
							patch.setPrecip(WeatherPatch.Precipitation.builder()
									.operation(WeatherPatchOperation.fromValue(Integer.parseInt(list[i + 1])))
									.value(Double.parseDouble(list[i + 2]))
									.build());
							i += 2;
						}
						else if (list[i].equals("windspeed")) {
							patch.setWindSpeed(WeatherPatch.WindSpeed.builder()
									.operation(WeatherPatchOperation.fromValue(Integer.parseInt(list[i + 1])))
									.value(Double.parseDouble(list[i + 2]))
									.build());
							i += 2;
						}
						else if (list[i].equals("winddir")) {
							patch.setWindDirection(WeatherPatch.WindDirection.builder()
									.operation(WeatherPatchOperation.fromValue(Integer.parseInt(list[i + 1])))
									.value(Double.parseDouble(list[i + 2]))
									.build());
							i += 2;
						}
						else {
							WeatherPatchType type = WeatherPatchType.fromValue(list[i]);
							if (type != WeatherPatchType.INVALID) {
								patch.setType(type);
								if (patch.getType() == WeatherPatchType.FILE) {
									patch.setFilename(list[i + 1]);
									i++;
								}
								else if (patch.getType() == WeatherPatchType.POLYGON) {
									for (int j = i + 1; j < list.length; j += 2) {
										if (j < list.length - 1) {
											patch.getFeature().add(new ca.wise.api.LatLon(Double.parseDouble(list[j]), Double.parseDouble(list[j + 1])));
										}
									}
									break;
								}
							}
						}
					}
					
					wise.getInputs().getFiles().getWeatherPatchFiles().add(patch);
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Weather Patch File";
				}
			}
			else
				return "Incorrectly Formatted Weather Patch File (2)";
		}
		else if (nextVal.equals(PHP_TYPE.INPUT_GRID_FILE.val)) {
			String list[] = data.split("\\|");
			if (list.length == 5) {
				try {
					ca.wise.api.input.GridFile igf = ca.wise.api.input.GridFile.builder()
							.id(list[0])
							.comment(list[1])
							.type(GridFileType.fromValue(Integer.parseInt(list[2])))
							.filename(list[3])
							.projection(list[4])
							.build();
					wise.getInputs().getFiles().getGridFiles().add(igf);
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Input Grid File";
				}
			}
			else
				return "Incorrectly Formatted Input Grid File";
		}
		else if (nextVal.equals(PHP_TYPE.WEATHERSTATION.val)) {
			String list[] = data.split("\\|");
			if (list.length >= 4) {
				try {
					WeatherStation station = WeatherStation.builder()
							.id(list[0])
							.location(ca.wise.api.LatLon.builder()
							.latitude(Double.parseDouble(list[1]))
							.longitude(Double.parseDouble(list[2]))
							.build())
							.elevation(Double.parseDouble(list[3]))
							.build();
					if (list.length == 5)
						station.setComments(list[4]);
					wise.getInputs().getWeatherStations().add(station);
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Weather Station File";
				}
			}
			else
				return "Incorrectly Formatted Weather Station File (2)";
		}
		else if (nextVal.equals(PHP_TYPE.WEATHERSTREAM.val)) {
			String list[] = data.split("\\|");
			if (list.length > 11) {
				try {
					WeatherStream stream = WeatherStream.builder()
							.id(list[0])
							.filename(list[1])
							.hffmcValue(Double.parseDouble(list[2]))
							.hffmcHour(Integer.parseInt(list[3]))
							.hffmcMethod(HFFMCMethod.fromValue(Integer.parseInt(list[4])))
							.startingFfmc(Double.parseDouble(list[5]))
							.startingDmc(Double.parseDouble(list[6]))
							.startingDc(Double.parseDouble(list[7]))
							.startingPrecip(Double.parseDouble(list[8]))
							.startTime(list[9])
							.endTime(list[10])
							.parentId(list[11])
							.build();
					if (list.length > 12)
						stream.setComments(list[12]);
					if (list.length == 19) {
						stream.setDiurnalTemperatureAlpha(Double.parseDouble(list[13]));
						stream.setDiurnalTemperatureBeta(Double.parseDouble(list[14]));
						stream.setDiurnalTemperatureGamma(Double.parseDouble(list[15]));
						stream.setDiurnalWindSpeedAlpha(Double.parseDouble(list[16]));
						stream.setDiurnalWindSpeedBeta(Double.parseDouble(list[17]));
						stream.setDiurnalWindSpeedGamma(Double.parseDouble(list[18]));
					}
					for (WeatherStation station : wise.getInputs().getWeatherStations()) {
						if (station.getName().equals(stream.getParentId())) {
							station.getStreams().add(stream);
							break;
						}
					}
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Weather Stream " + data;
				}
			}
			else
				return "Incorrectly Formatted Weather Stream (2)";
		}
		else if (nextVal.equals(PHP_TYPE.IGNITION.val)) {
			String list[] = data.split("\\|");
			if (list.length > 3) {
				try {
					Ignition ig = Ignition.builder()
							.id(list[0])
							.startTime(list[1])
							.comments(list[2])
							.type(IgnitionType.fromValue(list[3]))
							.build();
					int nextIndex = -1;
					if (ig.getType() == IgnitionType.FILE) {
						ig.setFilename(list[4]);
						nextIndex = 5;
					}
					else if (ig.getType() == IgnitionType.POLYGON || ig.getType() == IgnitionType.POLYLINE ||
					ig.getType() == IgnitionType.POINT) {
						for (int i = 4; i < list.length; i += 2) {
							if (list[i].equals("attr")) {
								nextIndex = i;
								break;
							}
							if (i < list.length - 2) {
								ig.getFeature().add(new ca.wise.api.LatLon(Double.parseDouble(list[i]), Double.parseDouble(list[i + 1])));
							}
						}
					}
					if (list.length > nextIndex) {
						if (list[nextIndex].equals("attr")) {
							nextIndex++;
							int length = Integer.parseInt(list[nextIndex]);
							nextIndex++;
							for (int i = 0; i < length && (i + nextIndex) < (list.length - 1); i += 2) {
								String key = list[nextIndex + i];
								String value = list[nextIndex + i + 1];
								if (IntegerHelper.isInteger(value)) {
									ig.getAttributes().add(new AttributeEntry(key, Integer.parseInt(value)));
								}
								else if (DoubleHelper.isDouble(value)) {
									ig.getAttributes().add(new AttributeEntry(key, Double.parseDouble(value)));
								}
								else {
									ig.getAttributes().add(new AttributeEntry(key, value));
								}
							}
						}
					}
					wise.getInputs().getIgnitions().add(ig);
					WISELogger.debug("Found an ignition: " + ig.getFilename());
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted Ignition";
				}
			}
			else
				return "Incorrectly Formatted Ignition (2)";
		}
		else if (nextVal.equals(PHP_TYPE.ASSET_FILE.val)) {
			String list[] = data.split("\\|");
			if (list.length > 4) {
				try {
					AssetFile file = AssetFile.builder()
							.id(list[0])
							.comments(list[1])
							.type(AssetShapeType.fromValue(Integer.parseInt(list[2])))
							.buffer(Double.parseDouble(list[3]))
							.build();
					if (file.getType() == AssetShapeType.FILE)
						file.setFilename(list[4]);
					else {
						for (int i = 4; i < list.length - 1; i += 2) {
							file.getFeature().add(new ca.wise.api.LatLon(Double.parseDouble(list[i]), Double.parseDouble(list[i + 1])));
						}
					}
					
					wise.getInputs().getAssetFiles().add(file);
					WISELogger.debug("Found an asset: " + file.getId());
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted asset file";
				}
			}
		}
		else if (nextVal.equals(PHP_TYPE.TARGET_FILE.val)) {
			String list[] = data.split("\\|");
			if (list.length > 4) {
				try {
					TargetFile file = TargetFile.builder()
							.id(list[0])
							.comments(list[1])
							.type(AssetShapeType.fromValue(Integer.parseInt(list[2])))
							.build();
					if (file.getType() == AssetShapeType.FILE)
					file.setFilename(list[3]);
					else {
						for (int i = 3; i < list.length - 1; i += 2) {
							file.getFeature().add(new ca.wise.api.LatLon(Double.parseDouble(list[i]), Double.parseDouble(list[i + 1])));
						}
					}
					
					wise.getInputs().getTargetFiles().add(file);
					WISELogger.debug("Found a target: " + file.getId());
				}
				catch (NumberFormatException ex) {
					return "Incorrectly Formatted target file";
				}
			}
		}
		else if (currentScen != null) {
			if (nextVal.equals(PHP_TYPE.SCENARIONAME.val)) {
				currentScen.setName(data);
				WISELogger.debug("Found the Scenario Name: " + currentScen.getName());
			}
			else if (nextVal.equals(PHP_TYPE.DISPLAY_INTERVAL.val)) {
				currentScen.setDisplayInterval(data);
				WISELogger.debug("Found the scenarios display interval: " + currentScen.getDisplayInterval());
			}
			else if (nextVal.equals(PHP_TYPE.SCENARIO_TO_COPY.val)) {
				currentScen.setScenToCopy(data);
				WISELogger.debug("Found the name of the scenario to copy: " + currentScen.getScenToCopy());
			}
			else if (nextVal.equals(PHP_TYPE.STARTTIME.val)) {
				currentScen.setStartTime(data);
				WISELogger.debug("Found the Start Time: " + currentScen.getStartTime());
			}
			else if (nextVal.equals(PHP_TYPE.ENDTIME.val)) {
				currentScen.setEndTime(data);
				WISELogger.debug("Found the End Time: " + currentScen.getEndTime());
			}
			else if (nextVal.equals(PHP_TYPE.COMMENTS.val)) {
				currentScen.setComments(data);
				WISELogger.debug("Found Some Comments: " + currentScen.getComments());
			}
			else if (nextVal.equals(PHP_TYPE.MAXACCTS.val)) {
				try {
					currentScen.getFgmOptions().setMaxAccTs(data);
					WISELogger.debug("Found the Maximum Calculation Time Step During Acceleration: " + currentScen.getFgmOptions().getMaxAccTs());
				}
				catch (NumberFormatException ex) {
					return "The Maximum Calculation Time Step During Acceleration Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.DISTRES.val)) {
				try {
					currentScen.getFgmOptions().setDistRes(Double.parseDouble(data));
					WISELogger.debug("Found the Distance Resolution: " + currentScen.getFgmOptions().getDistRes());
				}
				catch (NumberFormatException ex) {
					return "The Distance Resolution Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.PERIMRES.val)) {
				try {
					currentScen.getFgmOptions().setPerimRes(Double.parseDouble(data));
					WISELogger.debug("Found the Perimeter Resolution: " + currentScen.getFgmOptions().getPerimRes());
				}
				catch (NumberFormatException ex) {
					return "The Perimeter Resolution Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.MINSPREADROS.val)) {
				try {
					currentScen.getFgmOptions().setMinimumSpreadingRos(Double.parseDouble(data));
					WISELogger.debug("Found the minimum spreading ROS: " + currentScen.getFgmOptions().getMinimumSpreadingRos());
				}
				catch (NumberFormatException ex) {
					return "The minimum spreading ROS must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.STOPGRIDEND.val)) {
				try {
					currentScen.getFgmOptions().setStopAtGridEnd(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether to Stop at Grid End: " + currentScen.getFgmOptions().isStopAtGridEnd());
				}
				catch (NumberFormatException ex) {
					return "Whether to Stop the Fire at the Grid End Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.BREACHING.val)) {
				try {
					currentScen.getFgmOptions().setBreaching(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether Breaching is Enabled: " + currentScen.getFgmOptions().isBreaching());
				}
				catch (NumberFormatException ex) {
					return "Whether Breaching is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.DYNAMICTHRESHOLD.val)) {
				try {
					currentScen.getFgmOptions().setDynamicSpatialThreshold(Integer.parseInt(data) != 0);
					WISELogger.debug("Found whether dynamic spatial threshold is enabled: " + currentScen.getFgmOptions().isDynamicSpatialThreshold());
				}
				catch (NumberFormatException ex) {
					return "Whether dynamic spatial threshold is enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.SPOTTING.val)) {
				try {
					currentScen.getFgmOptions().setSpotting(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether Spotting is Enabled: " + currentScen.getFgmOptions().isSpotting());
				}
				catch (NumberFormatException ex) {
					return "Whether Spotting is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.PURGENONDISPLAY.val)) {
				try {
					currentScen.getFgmOptions().setPurgeNonDisplayable(Integer.parseInt(data) != 0);
					WISELogger.debug("Found whether purge non displayable is enabled: " + currentScen.getFgmOptions().isPurgeNonDisplayable());
				}
				catch (NumberFormatException ex) {
					return "Whether purge non displayable is enabled must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FGM_DX.val)) {
				try {
					currentScen.getFgmOptions().setDx(Double.parseDouble(data));
					WISELogger.debug("Found DX: " + currentScen.getFgmOptions().getDx());
				}
				catch (NumberFormatException ex) {
					return "DX must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FGM_DY.val)) {
				try {
					currentScen.getFgmOptions().setDy(Double.parseDouble(data));
					WISELogger.debug("Found DY: " + currentScen.getFgmOptions().getDy());
				}
				catch (NumberFormatException ex) {
					return "DY must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FGM_DT.val)) {
				currentScen.getFgmOptions().setDt(data);
				WISELogger.debug("Found DT: " + currentScen.getFgmOptions().getDt());
			}
			else if (nextVal.equals(PHP_TYPE.FGM_DWD.val)) {
				try {
					currentScen.getFgmOptions().setDwd(Double.parseDouble(data));
					WISELogger.debug("Found DWD: " + currentScen.getFgmOptions().getDwd());
				}
				catch (NumberFormatException ex) {
					return "DWD must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FGM_OWD.val)) {
				try {
					currentScen.getFgmOptions().setOwd(Double.parseDouble(data));
					WISELogger.debug("Found OWD: " + currentScen.getFgmOptions().getOwd());
				}
				catch (NumberFormatException ex) {
					return "OWD must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FGM_DVD.val)) {
				try {
					currentScen.getFgmOptions().setDvd(Double.parseDouble(data));
					WISELogger.debug("Found DVD: " + currentScen.getFgmOptions().getDvd());
				}
				catch (NumberFormatException ex) {
					return "DVD must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FGM_OVD.val)) {
				try {
					currentScen.getFgmOptions().setOvd(Double.parseDouble(data));
					WISELogger.debug("Found OVD: " + currentScen.getFgmOptions().getOvd());
				}
				catch (NumberFormatException ex) {
					return "OVD must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.GROWTHAPPLIED.val)) {
				try {
					currentScen.getFgmOptions().setGrowthPercentileApplied(Integer.parseInt(data) != 0);
					WISELogger.debug("Found whether growth percentile is applied: " + currentScen.getFgmOptions().getGrowthPercentileApplied());
				}
				catch (NumberFormatException ex) {
					return "Whether growth percentile is applied must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.GROWTHPERC.val)) {
				try {
					currentScen.getFgmOptions().setGrowthPercentile(Double.parseDouble(data));
					WISELogger.debug("Found growth percentile: " + currentScen.getFgmOptions().getGrowthPercentile());
				}
				catch (NumberFormatException ex) {
					return "Growth percentile must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.SUPPRESS_TIGHT_CONCAVE.val)) {
				try {
					currentScen.getFgmOptions().setSuppressTightConcave(Integer.parseInt(data) != 0);
					WISELogger.debug("Found whether to suppress tight concave: " + currentScen.getFgmOptions().getSuppressTightConcave());
				}
				catch (NumberFormatException ex) {
					return "Whether to suppress tight concave must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.NON_FUELS_AS_VECTOR_BREAKS.val)) {
				try {
					currentScen.getFgmOptions().setNonFuelsAsVectorBreaks(Integer.parseInt(data) != 0);
					WISELogger.debug("Found use non-fuels as vector breaks: " + currentScen.getFgmOptions().getNonFuelsAsVectorBreaks());
				}
				catch (NumberFormatException ex) {
					return "Whether to use non-fuels as vector breaks must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.NON_FUELS_TO_VECTOR_BREAKS.val)) {
				try {
					currentScen.getFgmOptions().setNonFuelsToVectorBreaks(Integer.parseInt(data) != 0);
					WISELogger.debug("Found change non-fuels to vector breaks: " + currentScen.getFgmOptions().getNonFuelsToVectorBreaks());
				}
				catch (NumberFormatException ex) {
					return "Whether to make non-fuels vector breaks must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.USE_INDEPENDENT_TIMESTEPS.val)) {
				try {
					currentScen.getFgmOptions().setUseIndependentTimesteps(Integer.parseInt(data) == 1);
					WISELogger.debug("Found use independent timesteps: " + currentScen.getFgmOptions().getUseIndependentTimesteps());
				}
				catch (NumberFormatException ex) {
					return "Whether independent timesteps are used must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.PERIMETER_SPACING.val)) {
				try {
					currentScen.getFgmOptions().setPerimeterSpacing(Double.parseDouble(data));
					WISELogger.debug("Found perimeter spacing: " + currentScen.getFgmOptions().getPerimeterSpacing());
				}
				catch (NumberFormatException ex) {
					return "Perimeter spacing must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FALSE_ORIGIN.val)) {
				try {
					currentScen.getFgmOptions().setEnableFalseOrigin(Integer.parseInt(data) != 0);
					WISELogger.debug("Found enable false origin: " + currentScen.getFgmOptions().isEnableFalseOrigin());
				}
				catch (NumberFormatException ex) {
					return "Whether to enable false origin must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FALSE_SCALING.val)) {
				try {
					currentScen.getFgmOptions().setEnableFalseScaling(Integer.parseInt(data) != 0);
					WISELogger.debug("Found enable false scaling: " + currentScen.getFgmOptions().isEnableFalseScaling());
				}
				catch (NumberFormatException ex) {
					return "Whether to enable false scaling must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.TERRAINEFF.val)) {
				try {
					currentScen.getFbpOptions().setTerrainEffect(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether Terrain Effect is Enabled: " + currentScen.getFbpOptions().getTerrainEffect());
				}
				catch (NumberFormatException ex) {
					return "Whether Terrain Effect is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.WINDEFF.val)) {
				try {
					currentScen.getFbpOptions().setWindEffect(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether Wind Effect is Enabled: " + currentScen.getFbpOptions().getWindEffect());
				}
				catch (NumberFormatException ex) {
					return "Whether Wind Effect is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.PEROVER.val)) {
				try {
					currentScen.getFmcOptions().setPerOverride(Double.parseDouble(data));
					WISELogger.debug("Found Whether Percent Override is Enabled: " + currentScen.getFmcOptions().getPerOverride());
				}
				catch (NumberFormatException ex) {
					return "Whether Percent Override is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.NODATAELEV.val)) {
				try {
					currentScen.getFmcOptions().setNodataElev(Double.parseDouble(data));
					WISELogger.debug("Found the Elevation Where NODATA or no Grid Exist: " + currentScen.getFmcOptions().getNodataElev());
				}
				catch (NumberFormatException ex) {
					return "The Elevation Where NODATA or No Grid Exists Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.TERRAIN.val)) {
				try {
					currentScen.getFmcOptions().setTerrain(Integer.parseInt(data) != 0);
					WISELogger.debug("Found FMC terrain enabled: " + currentScen.getFmcOptions().getTerrain());
				}
				catch (NumberFormatException ex) {
					return "The FMC terrain enabled must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.ACCURATELOCATION.val)) {
				try {
					currentScen.getFmcOptions().setAccurateLocation(Integer.parseInt(data) != 0);
					WISELogger.debug("Found accurate location enabled: " + currentScen.getFmcOptions().getAccurateLocation());
				}
				catch (NumberFormatException ex) {
					return "The accurate location enabled must be a number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.SIMULATION_PROPERTIES.val)) {
				try {
					String[] split = data.split("\\|");
					if (split.length >= 2) {
						currentScen.getFgmOptions().setIgnitionSize(Double.parseDouble(split[0]));
						currentScen.getFgmOptions().setInitialVertexCount(Integer.parseInt(split[1]));
						if (split.length > 3) {
							currentScen.getFgmOptions().setGlobalAssetOperation(AssetOperation.fromValue(Integer.parseInt(split[2])));
							currentScen.getFgmOptions().setAssetCollisionCount(Integer.parseInt(split[3]));
						}
					}
				}
				catch (NumberFormatException ex) {
					return "The Simulation Settings were not valid.";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FWISPACIAL.val)) {
				try {
					currentScen.getFwiOptions().setFwiSpacInterp(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether FWI Spatial Interpolation is Enabled: " + currentScen.getFwiOptions().getFwiSpacInterp());
				}
				catch (NumberFormatException ex) {
					return "Whether FWI Spatial Interpolation is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FWIFROMSPACIAL.val)) {
				try {
					currentScen.getFwiOptions().setFwiFromSpacWeather(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether to Calculate FWI values from Spatial Interpolated Weather is Enabled: " + currentScen.getFwiOptions().getFwiFromSpacWeather());
				}
				catch (NumberFormatException ex) {
					return "Whether to Calculate FWI Values From Spatially Interpolated Weather is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.HISTORYFWI.val)) {
				try {
					currentScen.getFwiOptions().setHistoryOnEffectedFWI(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether to Apply History to Affected FWI Values: " + currentScen.getFwiOptions().getHistoryOnEffectedFWI());
				}
				catch (NumberFormatException ex) {
					return "Whether to Apply History to Affected FWI Values or Not Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.BURNINGCONDON.val)) {
				try {
					currentScen.getFwiOptions().setBurningConditionsOn(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether Burned Conditions are Enabled: " + currentScen.getFwiOptions().getBurningConditionsOn());
				}
				catch (NumberFormatException ex) {
					return "Whether Burning Conditions are Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.FWITEMPORALINTERP.val)) {
				try {
					currentScen.getFwiOptions().setFwiTemporalInterp(Integer.parseInt(data) != 0);
					WISELogger.debug("Found Whether Temporal Interpolation is Enabled: " + currentScen.getFwiOptions().getFwiTemporalInterp());
				}
				catch (NumberFormatException ex) {
					return "Whether Temporal Interpolation is Enabled Must be a Number";
				}
			}
			else if (nextVal.equals(PHP_TYPE.BURNINGCONDITION.val)) {
				String list[] = data.split("\\|");
				if (list.length >= 6) {
					try {
						BurningConditions cond = BurningConditions.builder()
						.date(list[0])
						.startTime(list[1])
						.endTime(list[2])
						.fwiGreater(Double.parseDouble(list[3]))
						.wsGreater(Double.parseDouble(list[4]))
						.rhLess(Double.parseDouble(list[5]))
						.build();
						if (list.length > 6) {
							cond.setIsiGreater(Double.parseDouble(list[6]));
							if (list.length > 8) {
								cond.setStartTimeOffset(BurningConditionRelative.fromValue(Integer.valueOf(list[7])));
								cond.setEndTimeOffset(BurningConditionRelative.fromValue(Integer.valueOf(list[8])));
							}
						}
						currentScen.getBurningConditions().add(cond);
						WISELogger.debug("Found Burning Condition for Date: " + cond.getDate());
					}
					catch (NumberFormatException ex) {
						return "Invalid Numerical Value in Burning Condition";
					}
				}
				else {
					return "Incorrectly Formatted Burning Condition";
				}
			}
			else if (nextVal.equals(PHP_TYPE.VECTOR_REF.val)) {
				currentScen.getVectorInfo().add(data);
			}
			else if (nextVal.equals(PHP_TYPE.STREAM_REF.val)) {
				String list[] = data.split("\\|");
				if (list.length >= 2) {
					StationStream st = StationStream.builder()
							.station(list[1])
							.stream(list[0])
							.build();
					if (list.length > 2) {
						StationStream.StreamOptions.StreamOptionsBuilder builder = StationStream.StreamOptions.builder();
						if (list[2].length() > 0 && !list[2].equalsIgnoreCase("null"))
							builder.name(list[2]);
						if (list[3].length() > 0 && !list[3].equalsIgnoreCase("null"))
							builder.startTime(list[3]);
						if (list[4].length() > 0 && !list[4].equalsIgnoreCase("null"))
							builder.endTime(list[4]);
						if (list[5].length() > 0 && !list[5].equalsIgnoreCase("null"))
							builder.ignitionTime(list[5]);
						if (list.length > 6) {
							try {
								if (list[6].length() > 0 && !list[6].equalsIgnoreCase("null"))
									builder.windDirection(Double.parseDouble(list[6]));
								if (list[7].length() > 0 && !list[7].equalsIgnoreCase("null"))
									builder.deltaWindDirection(Double.parseDouble(list[7]));
							}
							catch (NumberFormatException e) {
								return "Invalid ignition sub-scenario";
							}
						}
					}
					currentScen.getStationStreams().add(st);
				}
				else {
					return "Incorrectly Formatted scenario weather stream reference" + data;
				}
			}
			else if (nextVal.equals(PHP_TYPE.LAYER_INFO.val)) {
				String list[] = data.split("\\|");
				if (list.length == 2) {
					try {
						int index = Integer.parseInt(list[1]);
						LayerInfo info = LayerInfo.builder()
								.index(index)
								.name(list[0])
								.build();
						if (list.length > 2) {
							int count = Integer.parseInt(list[2]);
							if (count > 0) {
								LayerInfo.Options.OptionsBuilder options = LayerInfo.Options.builder();
								for (int i = 0; i < count; i++) {
									options.subName(list[3 + i]);
								}
								info.setOptions(options.build());
							}
						}
						currentScen.getLayerInfo().add(info);
					}
					catch (NumberFormatException ex) {
						return "Invalid numerical value in layer info.";
					}
				}
				else {
					return "Incorrectly formatted layer info.";
				}
			}
			else if (nextVal.equals(PHP_TYPE.PRIMARY_STREAM.val)) {
				for (StationStream ss : currentScen.getStationStreams()) {
					if (ss.getStream().equals(data)) {
						ss.setPrimaryStream(true);
						break;
					}
				}
			}
			else if (nextVal.equals(PHP_TYPE.IGNITION_REF.val)) {
				String list[] = data.split("\\|");
				IgnitionReference ref = IgnitionReference.builder()
						.ignition(list[0])
						.build();
				if (list.length > 1) {
					try {
						if (list[1].equals("line")) {
							ref.setPolylineIgnitionOptions(IgnitionReference.PolylineIgnitionOptions.builder()
									.name(list[2])
									.pointSpacing(Double.parseDouble(list[3]))
									.polyIndex(Integer.parseInt(list[4]))
									.pointIndex(Integer.parseInt(list[5]))
									.build());
						}
						else if (list[1].equals("mp")) {
							ref.setMultiPointIgnitionOptions(IgnitionReference.MultiPointIgnitionOptions.builder()
									.name(list[2])
									.pointIndex(Integer.parseInt(list[3]))
									.build());
						}
						else if (list[1].equals("sp")) {
							ref.setSinglePointIgnitionOptions(IgnitionReference.SinglePointIgnitionOptions.builder()
									.name(list[2])
									.build());
						}
					}
					catch (NumberFormatException e) {
						return "Invalid ignition sub-scenario";
					}
				}
				currentScen.getIgnitionInfo().add(ref);
			}
			else if (nextVal.equals(PHP_TYPE.ASSET_REF.val)) {
				String list[] = data.split("\\|");
				if (list.length > 1) {
					try {
						AssetReference ref = AssetReference.builder()
								.name(list[0])
								.operation(AssetOperation.fromValue(Integer.parseInt(list[1])))
								.collisionCount(Integer.parseInt(list[2]))
								.build();
						
						currentScen.getAssetFiles().add(ref);
					}
					catch (NumberFormatException e) {
						return "Invalid asset reference";
					}
				}
			}
			else if (nextVal.equals(PHP_TYPE.WIND_TARGET_REF.val)) {
				String list[] = data.split("\\|");
				if (list.length > 1) {
					try {
						currentScen.setWindTargetFile(TargetReference.builder()
								.name(list[0])
								.geometryIndex(Integer.parseInt(list[1]))
								.pointIndex(Integer.parseInt(list[2]))
								.build());
					}
					catch (NumberFormatException e) {
						return "Invalid wind target reference";
					}
				}
			}
			else if (nextVal.equals(PHP_TYPE.VECTOR_TARGET_REF.val)) {
				String list[] = data.split("\\|");
				if (list.length > 1) {
					try {
						currentScen.setVectorTargetFile(TargetReference.builder()
								.name(list[0])
								.geometryIndex(Integer.parseInt(list[1]))
								.pointIndex(Integer.parseInt(list[2]))
								.build());
					}
					catch (NumberFormatException e) {
						return "Invalid vector target reference";
					}
				}
			}
			else if (nextVal.equals(PHP_TYPE.SCENARIO_SEASONAL.val)) {
				String list[] = data.split("\\|");
				if (list.length == 4) {
					try {
						SeasonalCondition condition = SeasonalCondition.builder()
								.type(SeasonalConditionType.fromValue(Integer.valueOf(list[0])))
								.active(Integer.valueOf(list[1]) != 0)
								.startTime(list[3])
								.build();
						if (!list[2].equalsIgnoreCase("null") && !list[2].equalsIgnoreCase("undefined"))
							condition.setValue(Double.parseDouble(list[2]));
						currentScen.getSeasonalConditions().add(condition);
					}
					catch (NumberFormatException e) {
						return "Invalid seasonal burn condition";
					}
				}
			}
			else if (nextVal.equals(PHP_TYPE.STOP_MODELLING.val)) {
				String list[] = data.split("\\|");
				if (list.length > 5) {
					currentScen.setStopModellingOptions(new StopModellingOptions());
					if (!list[0].equals("0") && !list[0].equalsIgnoreCase("null") && list[0].length() > 0) {
						currentScen.getStopModellingOptions().setResponseTime(list[0]);
					}
					int index = 1;
					if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null")) {
						currentScen.getStopModellingOptions().setFi90(
								StopModellingOptions.Threshold.builder()
										.threshold(Double.parseDouble(list[index]))
										.duration(list[index + 1])
										.build()
						);
						index += 2;
					}
					else
					index++;
					if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null")) {
						currentScen.getStopModellingOptions().setFi95(
								StopModellingOptions.Threshold.builder()
										.threshold(Double.parseDouble(list[index]))
										.duration(list[index + 1])
										.build()
						);
						index += 2;
					}
					else
					index++;
					if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null")) {
						currentScen.getStopModellingOptions().setFi100(
								StopModellingOptions.Threshold.builder()
										.threshold(Double.parseDouble(list[index]))
										.duration(list[index + 1])
										.build()
						);
						index += 2;
					}
					else
					index++;
					if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null")) {
						currentScen.getStopModellingOptions().setRh(
								StopModellingOptions.Threshold.builder()
										.threshold(Double.parseDouble(list[index]))
										.duration(list[index + 1])
										.build()
						);
						index += 2;
					}
					else
					index++;
					if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null")) {
						currentScen.getStopModellingOptions().setPrecip(
								StopModellingOptions.Threshold.builder()
										.threshold(Double.parseDouble(list[index]))
										.duration(list[index + 1])
										.build()
						);
						index += 2;
					}
					else
					index++;
					if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null")) {
						currentScen.getStopModellingOptions().setArea(
								StopModellingOptions.Threshold.builder()
										.threshold(Double.parseDouble(list[index]))
										.duration(list[index + 1])
										.build()
						);
						index += 2;
					}
					else
					index++;
				}
			}
            else if (nextVal.equals(PHP_TYPE.GUSTING_OPTIONS.val)) {
                String list[] = data.split("\\|");
                if (list.length == 4) {
                    try {
                        GustingOptions.GustingOptionsBuilder builder = GustingOptions.builder();
                        GustingOptions.Gusting gusting = GustingOptions.Gusting.fromInt(Integer.parseInt(list[0]));
                        builder.gusting(gusting);
                        if (gusting == GustingOptions.Gusting.TIME_DERIVED_GUSTING || gusting == GustingOptions.Gusting.ROS_DERIVED_GUSTING) {
                            if (!Strings.isNullOrEmpty(list[1]) && !list[1].equalsIgnoreCase("null"))
                                builder.gustsPerHour(Integer.parseInt(list[1]));
                            if (!Strings.isNullOrEmpty(list[2]) && !list[2].equalsIgnoreCase("null"))
                                builder.percentGusting(Double.parseDouble(list[2]));
                            if (!Strings.isNullOrEmpty(list[3]) && !list[3].equalsIgnoreCase("null"))
                                builder.gustBias(GustingOptions.GustBias.fromInt(Integer.parseInt(list[3])));
                        }
                        else if (gusting == GustingOptions.Gusting.AVERAGE_GUSTING) {
                            if (!Strings.isNullOrEmpty(list[2]) && !list[2].equalsIgnoreCase("null"))
                                builder.percentGusting(Double.parseDouble(list[2]));
                        }
                        
                        currentScen.setGustingOptions(builder.build());
                    }
                    catch (NumberFormatException e) {
                        return "Invalid gusting options.";
                    }
                }
            }
			else {
				String temp = nextVal;
				nextVal = null;
				return "Unknown scenario value (" + temp + ")";
			}
		}
		else if (nextVal.equals(PHP_TYPE.TIMEZONE.val)) {
			String list[] = data.split("\\|");
			if (list.length  == 2) {
				try {
					wise.getInputs().setTimezone(Timezone.builder()
							.offset(list[0])
							.dst(Integer.parseInt(list[1]) != 0)
							.build());
				}
				catch (NumberFormatException e) {
					return "The Time Zone Must be a Number";
				}
			}
			else if (list.length == 1) {
				try {
					int value = Integer.parseInt(data);
					TimeZoneInfo[] tzlist = WorldLocation.getList();
					if (value < tzlist.length) {
						TimeZoneInfo info = tzlist[value];
						
						String offset;
						if (info.getTimezoneOffset().getTotalHours() < 0)
							offset = "-PT";
						else
							offset = "PT";
						offset += Math.abs(info.getTimezoneOffset().getTotalHours());
						offset += "H";
						if (info.getTimezoneOffset().getMinutes() > 0) {
							offset += info.getTimezoneOffset().getMinutes();
							offset += "M";
						}
						boolean dst;
						if (info.getDSTAmount().getTotalHours() > 0)
							dst = true;
						else
							dst = false;
						wise.getInputs().setTimezone(Timezone.builder()
								.value(info.getUUID())
								.offset(offset)
								.dst(dst)
								.build());
					}
				}
				catch (NumberFormatException e) {
					return "The Time Zone Value Must be a Number";
				}
			}
		}
		else if (nextVal.equals(PHP_TYPE.VECTORFILE.val)) {
			String list[] = data.split("\\|");
			if (list.length >= 10) {
				try {
					VectorFile file = VectorFile.builder()
							.scenarioName(list[0])
							.filename(list[1])
							.multPerim(Integer.parseInt(list[2]) > 0)
							.perimStartTime(list[3])
							.perimEndTime(list[4])
							.removeIslands(Integer.parseInt(list[5]) > 0)
							.mergeContact(Integer.parseInt(list[6]) > 0)
							.perimActive(Integer.parseInt(list[7]) > 0)
							.metadata(VectorMetadata.builder()
									.version(list[8].equals("true"))
									.scenName(list[9].equals("true"))
									.jobName(list[10].equals("true"))
									.igName(list[11].equals("true"))
									.simDate(list[12].equals("true"))
									.fireSize(list[13].equals("true"))
									.perimTotal(list[14].equals("true"))
									.perimActive(list[15].equals("true"))
									.perimUnit(DistanceUnit.fromVectorValue(Integer.parseInt(list[16])))
									.areaUnit(AreaUnit.fromVectorValue(Integer.parseInt(list[17])))
									.build())
							.build();
					if (list.length > 18) {
						file.setShouldStream(Integer.parseInt(list[18]) > 0);
						if (list.length > 19) {
							file.getMetadata().setWxValues(Integer.parseInt(list[19]) > 0);
							file.getMetadata().setFwiValues(Integer.parseInt(list[20]) > 0);
							file.getMetadata().setIgnitionLocation(Integer.parseInt(list[21]) > 0);
							file.getMetadata().setMaxBurnDistance(Integer.parseInt(list[22]) > 0);
							file.getMetadata().setIgnitionAttributes(Integer.parseInt(list[23]) > 0);
							if (list[24].length() != 0)
							file.setSubScenarioName(list[24]);
							int overridesLength = Integer.parseInt(list[25]);
							for (int i = 0; i < overridesLength * 3; i += 3) {
								file.getSubScenarioOverrides().add(PerimeterTimeOverride.builder()
										.subScenarioName(list[26 + i])
										.startTime(list[26 + i + 1])
										.endTime(list[26 + i + 2])
										.build());
							}
							int index = 26 + (overridesLength * 3);
							if (list.length >= (index + 2)) {
								file.getMetadata().setAssetArrivalTime(Integer.parseInt(list[index]) > 0);
								index++;
								file.getMetadata().setAssetArrivalCount(Integer.parseInt(list[index]) > 0);
								index++;
								if (list.length > index) {
									file.getMetadata().setIdentifyFinalPerimeter(Integer.parseInt(list[index]) > 0);
									index++;
									if (list.length > index) {
										file.setCoverageName(list[index]);
										index++;
										if (list.length > index) {
											file.getMetadata().setSimStatus(Integer.parseInt(list[index]) > 0);
										}
									}
								}
							}
						}
					}
					wise.getOutputs().getVectorFiles().add(file);
					WISELogger.debug("Found a Vector File: " + file.getFilename());
				}
				catch (NumberFormatException ex) {
					return "Invalid Numeric Value in Vector File";
				}
			}
			else {
				return "Incorrectly Formatted Vector File";
			}
		}
		else if (nextVal.equals(PHP_TYPE.SUMMARYFILE.val)) {
			String list[] = data.split("\\|");
			if (list.length >= 13) {
				try {
					SummaryFile file = SummaryFile.builder()
							.scenName(list[0])
							.filename(list[1])
							.outputs(SummaryOutputs.builder()
									.outputApplication(Integer.parseInt(list[2]) > 0)
									.outputGeoData(Integer.parseInt(list[3]) > 0)
									.outputScenario(Integer.parseInt(list[4]) > 0)
									.outputScenarioComments(Integer.parseInt(list[5]) > 0)
									.outputInputs(Integer.parseInt(list[6]) > 0)
									.outputLandscape(Integer.parseInt(list[7]) > 0)
									.outputFBPPatches(Integer.parseInt(list[8]) > 0)
									.outputWxPatches(Integer.parseInt(list[9]) > 0)
									.outputIgnitions(Integer.parseInt(list[10]) > 0)
									.outputWxStreams(Integer.parseInt(list[11]) > 0)
									.outputFBP(Integer.parseInt(list[12]) > 0)
									.build())
							.build();
					if (list.length > 13) {
						file.setShouldStream(Integer.parseInt(list[13]) > 0);
						if (list.length > 14) {
							file.getOutputs().setOutputWxData(Integer.parseInt(list[14]) > 0);
							if (list.length > 15) {
								file.getOutputs().setOutputAssetInfo(Integer.parseInt(list[15]) > 0);
							}
						}
					}
					wise.getOutputs().getSummaryFiles().add(file);
					WISELogger.debug("Found a summary file for scenario " + file.getScenName());
				}
				catch (NumberFormatException ex) {
					return "Invalid numeric value in summary file";
				}
			}
			else {
				return "Incorrectly formatted summary file.";
			}
		}
		else if (nextVal.equals(PHP_TYPE.GRIDFILE.val)) {
			String list[] = data.split("\\|");
			if (list.length >= 5) {
				try {
					GridFile file = GridFile.builder()
							.scenarioName(list[0])
							.filename(list[1])
							.outputTime(list[2])
							.statistic(GlobalStatistics.fromValue(Integer.parseInt(list[3])))
							.interpMethod(GridFileInterpolation.fromValue(list[4]))
							.build();
					if (list.length > 5) {
						file.setShouldStream(Integer.parseInt(list[5]) > 0);
						if (list.length > 6) {
							file.setCompression(GridFileCompression.fromValue(Integer.parseInt(list[6])));
							if (list.length > 7) {
								file.setShouldMinimize(list[7].equals("true"));
								if (list[8].length() != 0)
								file.setSubScenarioName(list[8]);
								int length = Integer.parseInt(list[9]);
								for (int i = 0; i < length * 2; i += 2) {
									file.getSubScenarioOverrideTimes().add(ExportTimeOverride.builder()
											.subScenarioName(list[10 + i])
											.exportTime(list[10 + i + 1])
											.build());
								}
								int index = 10 + (length * 2);
								if (index < list.length) {
									if (!list[index].equalsIgnoreCase("null"))
									file.setDiscretize(Integer.parseInt(list[index]));
									index++;
									if (index < list.length) {
										if (!list[index].equalsIgnoreCase("null"))
										file.setStartOutputTime(list[index]);
										index++;
										if (index < list.length) {
											//allow the export options to be skipped for other data in the future
											if (!list[index].equalsIgnoreCase("non")) {
												GridFile.ExportOptions.ExportOptionsBuilder builder = GridFile.ExportOptions.builder();
												if (list[index].equalsIgnoreCase("res")) {
													builder.resolution(Double.parseDouble(list[index + 1]));
													index += 2;
												}
												else if (list[index].equalsIgnoreCase("scl")) {
													builder.scale(Double.parseDouble(list[index + 1]));
													index += 2;
												}
												else {
													builder.useFuelMap(true);
													index++;
												}
												
												if (list[index].equalsIgnoreCase("loc")) {
													ca.wise.api.LatLon ll = new ca.wise.api.LatLon();
													index++;
													ll.setLatitude(Double.parseDouble(list[index]));
													index++;
													ll.setLongitude(Double.parseDouble(list[index]));
													index++;
													builder.location(ll);
												}
												else {
													index++;
													builder.origin(GridFile.ExportOptionsOrigin.fromValue(Integer.parseInt(list[index])));
													index++;
												}
												file.setExportOptions(builder.build());
											}
											else
											index++;
											
											if (index < list.length) {
												if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null"))
												file.setCoverageName(list[index]);
												index++;
												if (index < list.length) {
													if (!Strings.isNullOrEmpty(list[index]) && !list[index].equalsIgnoreCase("null"))
													file.setAssetName(list[index]);
													index++;
													if (index < list.length) {
														int i = Integer.valueOf(list[index]);
														if (i >= 0)
														file.setAssetIndex(i);
														index++;
														if (index < list.length) {
															file.setZeroForNodata(Integer.valueOf(list[index]) != 0);
															index++;
															if (index < list.length) {
																file.setExcludeInteriors(Integer.valueOf(list[index]) != 0);
																index++;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
					wise.getOutputs().getGridFiles().add(file);
					WISELogger.debug("Found a Grid File: " + file.getFilename());
				}
				catch (NumberFormatException ex) {
					return "Invalid Numeric Value in Grid File";
				}
			}
			else {
				return "Incorrectly Formatted Grid File";
			}
		}
		else if (nextVal.equals(PHP_TYPE.FUEL_GRID_EXPORT.val) ) {
			String list[] = data.split("\\|");
			if (list.length >= 4) {
				try {
					FuelGridFile.FuelGridFileBuilder file = FuelGridFile.builder()
							.filename(list[1])
							.compression(GridFileCompression.fromValue(Integer.parseInt(list[2])))
							.shouldStream(Integer.parseInt(list[3]) > 0);
					if (list[0].length() > 0 && !list[0].equalsIgnoreCase("null"))
						file.scenName(list[0]);
					if (list.length > 5)
						file.coverageName(list[4]);
					wise.getOutputs().getFuelGridFiles().add(file.build());
				}
				catch (NumberFormatException ex) {
					return "Invalid Numeric Value in Fuel Grid File export";
				}
			}
		}
		else if (nextVal.equals(PHP_TYPE.ASSET_STATS_EXPORT.val) ) {
			String list[] = data.split("\\|");
			if (list.length >= 5) {
				try {
					AssetStatsFile asf = AssetStatsFile.builder()
							.scenarioName(list[0])
							.filename(list[1])
							.fileType(Integer.parseInt(list[2]))
							.shouldStream(Integer.parseInt(list[3]) > 0)
							.build();
					if (!list[4].equalsIgnoreCase("null") && !list[4].equalsIgnoreCase("undefined"))
					asf.setCriticalPathEmbedded(Integer.parseInt(list[4]) != 0);
					if (!list[5].equalsIgnoreCase("null") && !list[5].equalsIgnoreCase("undefined"))
					asf.setCriticalPathPath(list[5]);
					wise.getOutputs().getAssetStatsFiles().add(asf);
				}
				catch (NumberFormatException ex) {
					return "Invalid Numeric Value in Fuel Grid File export";
				}
			}
		}
		else if (nextVal.equals(PHP_TYPE.STATSFILE.val)) {
			String list[] = data.split("\\|");
			if (list.length > 5) {
				try {
					StatsFile file = StatsFile.builder()
							.scenName(list[0])
							.filename(list[1])
							.fileType(StatsFileType.fromValue(Integer.parseInt(list[2])))
							.shouldStream(Integer.parseInt(list[3]) > 0)
							.build();
					int index;
					if (list[4].equals("loc")) {
						file.setLocation(new ca.wise.api.LatLon(Double.parseDouble(list[5]), Double.parseDouble(list[6])));
						index = 7;
					}
					else {
						if (!list[5].equalsIgnoreCase("null") && !list[5].equalsIgnoreCase("undefined"))
						file.setStreamName(list[5]);
						index = 6;
					}
					int count = Integer.parseInt(list[index]);
					index++;
					for (int i = 0; i < count; i++) {
					}
					index += count;
					if (index < list.length) {
						count = Integer.parseInt(list[index]);
						index++;
						for (int i = 0; i < count; i++) {
							file.addColumn(GlobalStatistics.fromValue(Integer.parseInt(list[index + i])));
						}
						index += count;
						if (index < list.length) {
							if (!list[index].equalsIgnoreCase("null"))
							file.setDiscretize(Integer.parseInt(list[index]));
							index++;
						}
					}
					wise.getOutputs().getStatsFiles().add(file);
					WISELogger.debug("Found a Stats File: " + file.getFilename());
				}
				catch (NumberFormatException ex) {
					return "Invalid Numeric Value in Stats File";
				}
			}
			else {
				return "Incorrectly Formatted Stats File";
			}
		}
		else if (nextVal.equals(PHP_TYPE.MQTT_HOST.val)) {
			if (wise.getMqttSettings() == null)
				wise.setMqttSettings(new MqttSettings());
			wise.getMqttSettings().setHost(data);
		}
		else if (nextVal.equals(PHP_TYPE.MQTT_PORT.val)) {
			if (wise.getMqttSettings() == null)
				wise.setMqttSettings(new MqttSettings());
			try {
				wise.getMqttSettings().setPort(Integer.parseInt(data));
			}
			catch (NumberFormatException e) {
				return "Invalid Numeric Value in MQTT Port";
			}
		}
		else if (nextVal.equals(PHP_TYPE.MQTT_USERNAME.val)) {
			if (wise.getMqttSettings() == null)
				wise.setMqttSettings(new MqttSettings());
			wise.getMqttSettings().setUsername(data);
		}
		else if (nextVal.equals(PHP_TYPE.MQTT_PASSWORD.val)) {
			if (wise.getMqttSettings() == null)
				wise.setMqttSettings(new MqttSettings());
			wise.getMqttSettings().setPassword(data);
		}
		else if (nextVal.equals(PHP_TYPE.MQTT_TOPIC.val)) {
			if (wise.getMqttSettings() == null)
				wise.setMqttSettings(new MqttSettings());
			wise.getMqttSettings().setTopic(data);
		}
		else if (nextVal.equals(PHP_TYPE.MQTT_QOS.val)) {
			if (wise.getMqttSettings() == null)
				wise.setMqttSettings(new MqttSettings());
			try {
				wise.getMqttSettings().setQos(Integer.parseInt(data));
			}
			catch (NumberFormatException e) {
				return "Invalid Numeric Value in MQTT QOS";
			}
		}
		else if (nextVal.equals(PHP_TYPE.MQTT_VERBOSITY.val)) {
			if (wise.getMqttSettings() == null)
				wise.setMqttSettings(new MqttSettings());
			wise.getMqttSettings().setVerbosity(data);
		}
		else if (nextVal.equals(PHP_TYPE.EMIT_STATISTIC.val)) {
			try {
				if (data.startsWith("d|")) {
					String[] split = data.split("\\|");
					if (split.length > 1) {
						wise.getTimestepSettings().setDiscretize(Integer.parseInt(split[1]));
					}
				}
				int i = Integer.parseInt(data);
				wise.getTimestepSettings().addStatistic(GlobalStatistics.fromValue(i));
			}
			catch (Exception e) { }
		}
		else if (nextVal.equals(PHP_TYPE.OUTPUT_STREAM.val)) {
			if (!Strings.isNullOrEmpty(data)) {
				String[] split = data.split("\\|");
				if (split.length > 0) {
					if (split[0].equals("mqtt")) {
						wise.getStreamInfo().add(new MqttOutputStreamInfo());
					}
					else if (split[0].equals("geo")) {
						if (split.length >= 7) {
							wise.getStreamInfo().add(GeoServerOutputStreamInfo.builder()
									.username(split[1])
									.password(split[2])
									.url(split[3])
									.workspace(split[4])
									.coverageStore(split[5])
									.declaredSrs(split[6])
									.build());
						}
					}
				}
			}
		}
		else if (nextVal.equals(PHP_TYPE.FUEL_OPTION.val)) {
			String[] split = data.split("\\|");
			if (split.length == 3) {
				try {
					wise.getInputs().getFuelOptions().add(FuelOption.builder()
							.fuelType(split[0])
							.optionType(ca.wise.api.input.FuelOptionType.fromValue(Integer.parseInt(split[1])))
							.value(Double.parseDouble(split[2]))
							.build());
				}
				catch (NumberFormatException e) {
					return "Unable to parse fuel option data.";
				}
			}
		}
		//global FGM options
		else if (nextVal.equals(PHP_TYPE.FGM_SETTINGS.val)) {
			String list[] = data.split("\\|");
			if (list.length > 0) {
				try {
					wise.getJobOptions().setLoadBalance(LoadBalanceType.fromValue(Integer.parseInt(list[0])));
					if (list.length > 1) {
						wise.getJobOptions().setPriority(Integer.parseInt(list[1]));
						if (list.length > 2) {
							wise.getJobOptions().setValidate(Boolean.parseBoolean(list[2]));
						}
					}
				}
				catch (NumberFormatException e) {
					return "Unable to parse FGM settings";
				}
			}
		}
		else if (nextVal.equals(PHP_TYPE.UNIT_SETTINGS.val)) {
			String list[] = data.split("\\|");
			if (list.length >= 19) {
				try {
					wise.getExportUnits().setSmallMeasureOutput(DistanceUnit.fromValue(Integer.parseInt(list[0])));
					wise.getExportUnits().setSmallDistanceOutput(DistanceUnit.fromValue(Integer.parseInt(list[1])));
					wise.getExportUnits().setDistanceOutput(DistanceUnit.fromValue(Integer.parseInt(list[2])));
					wise.getExportUnits().setAlternateDistanceOutput(DistanceUnit.fromValue(Integer.parseInt(list[3])));
					wise.getExportUnits().setCoordinateOutput(CoordinateUnit.fromValue(Integer.parseInt(list[4])));
					wise.getExportUnits().setAreaOutput(AreaUnit.fromValue(Integer.parseInt(list[5])));
					wise.getExportUnits().setVolumeOutput(VolumeUnit.fromValue(Integer.parseInt(list[6])));
					wise.getExportUnits().setTemperatureOutput(TemperatureUnit.fromValue(Integer.parseInt(list[7])));
					wise.getExportUnits().setMassOutput(MassUnit.fromValue(Integer.parseInt(list[8])));
					wise.getExportUnits().setEnergyOutput(EnergyUnit.fromValue(Integer.parseInt(list[9])));
					wise.getExportUnits().setAngleOutput(AngleUnit.fromValue(Integer.parseInt(list[10])));
					int numer = Integer.parseInt(list[11]);
					int denom = Integer.parseInt(list[12]);
					if (numer >= 0 && denom >= 0)
						wise.getExportUnits().setVelocityOutput(new VelocityUnit(DistanceUnit.fromValue(numer), TimeUnit.fromValue(denom)));
					numer = Integer.parseInt(list[13]);
					denom = Integer.parseInt(list[14]);
					if (numer >= 0 && denom >= 0)
						wise.getExportUnits().setAlternateVelocityOutput(new VelocityUnit(DistanceUnit.fromValue(numer), TimeUnit.fromValue(denom)));
					numer = Integer.parseInt(list[15]);
					denom = Integer.parseInt(list[16]);
					if (numer >= 0 && denom >= 0)
						wise.getExportUnits().setIntensityOutput(new IntensityUnit(EnergyUnit.fromValue(numer), DistanceUnit.fromValue(denom)));
					numer = Integer.parseInt(list[17]);
					denom = Integer.parseInt(list[18]);
					if (numer >= 0 && denom >= 0)
						wise.getExportUnits().setMassAreaOutput(new MassAreaUnit(MassUnit.fromValue(numer), AreaUnit.fromValue(denom)));
				}
				catch (NumberFormatException e) {
					return "Invalid unit setting";
				}
			}
		}
		else {
			nextVal = null;
			return "Unknown value";
		}
		nextVal = null;
		return "";
	}
}
