package ca.wise.fgm.output;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import ca.wise.fire.proto.CwfgmIgnition;
import ca.wise.fire.proto.CwfgmScenario;
import ca.wise.fire.proto.CwfgmIgnition.IgnitionPoint;
import ca.wise.fire.proto.CwfgmIgnition.IgnitionPoint.IgnitionShape;
import ca.wise.fuel.proto.FbpFuel;
import ca.wise.grid.proto.CwfgmGrid.ElevationFile;
import ca.wise.grid.proto.CwfgmGrid.FuelMapFile;
import ca.wise.grid.proto.CwfgmGrid.ProjectionFile;
import ca.wise.grid.proto.CwfgmTarget;
import ca.wise.grid.proto.CwfgmAsset;
import ca.wise.grid.proto.CwfgmFuelMap;
import ca.wise.grid.proto.TemporalCondition;
import ca.wise.grid.proto.CwfgmFuelMap.CwfgmFuelData;
import ca.wise.grid.proto.CwfgmGrid;
import ca.wise.grid.proto.TemporalCondition.DailyAttribute;
import ca.wise.grid.proto.TemporalCondition.SeasonalAttribute;
import ca.wise.grid.proto.TemporalCondition.SeasonalAttribute.EffectiveAttribute;
import ca.wise.grid.proto.wcsData;
import ca.wise.project.proto.GridCollection.Filter;
import ca.wise.project.proto.GridCollection.Filter.AttributeFilter.AttributeType;
import ca.wise.project.proto.Project.LoadBalancingType;
import ca.wise.project.proto.ProjectAsset;
import ca.wise.project.proto.FuelCollection;
import ca.wise.project.proto.ProjectFire;
import ca.wise.project.proto.ProjectFuel;
import ca.wise.project.proto.ProjectReplaceGridFilterBase;
import ca.wise.project.proto.ProjectReplaceGridFilterBase.FromFuelRule;
import ca.wise.project.proto.ProjectScenario;
import ca.wise.project.proto.ProjectScenario.Reference;
import ca.wise.project.proto.ProjectScenario.WeatherIndex;
import ca.wise.project.proto.ProjectTarget;
import ca.wise.project.proto.ProjectWeatherStation;
import ca.wise.project.proto.PrometheusData;
import ca.wise.project.proto.VectorCollection.Vector;
import ca.wise.fgm.tools.IntegerHelper;
import ca.wise.weather.proto.CwfgmWeatherStation;
import ca.wise.weather.proto.WeatherGridFilter;
import ca.wise.weather.proto.WindGrid;
import ca.wise.weather.proto.WindGrid.GridType;
import ca.wise.weather.proto.WindGrid.SectorData;
import ca.wise.api.LatLon;
import ca.wise.api.input.AssetFile;
import ca.wise.api.input.AssetReference;
import ca.wise.api.input.AssetShapeType;
import ca.wise.api.input.AttributeEntry;
import ca.wise.api.input.BurningConditionRelative;
import ca.wise.api.input.BurningConditions;
import ca.wise.api.input.FuelBreak;
import ca.wise.api.input.FuelBreakType;
import ca.wise.api.input.FuelOption;
import ca.wise.api.input.FuelPatch;
import ca.wise.api.input.FuelPatchType;
import ca.wise.api.input.GridFile;
import ca.wise.api.input.GridFileType;
import ca.wise.api.input.Ignition;
import ca.wise.api.input.IgnitionReference;
import ca.wise.api.input.LayerInfo;
import ca.wise.api.input.Scenario;
import ca.wise.api.input.SeasonalCondition;
import ca.wise.api.input.StationStream;
import ca.wise.api.input.TargetFile;
import ca.wise.api.input.WeatherGrid;
import ca.wise.api.input.WeatherGridGridFile;
import ca.wise.api.input.WeatherPatch;
import ca.wise.api.input.WeatherPatchType;
import ca.wise.api.input.WeatherStation;
import ca.wise.api.input.WeatherStream;
import ca.wise.api.output.AssetStatsFile;
import ca.wise.api.output.FuelGridFile;
import ca.wise.api.output.GlobalStatistics;
import ca.wise.api.output.StatsFile;
import ca.wise.api.output.SummaryFile;
import ca.wise.api.output.VectorFile;
import ca.hss.math.HSS_Double;
import ca.hss.math.ProtoWrapper;
import ca.hss.math.proto.GeoPoly;
import ca.hss.math.proto.GeoUnits;
import ca.hss.math.proto.XYPolygon;
import ca.hss.math.proto.XYPolySet.PolySetEntry.PolyType;
import ca.hss.times.TimeZoneInfo;
import ca.hss.times.WTime;
import ca.hss.times.WTimeManager;
import ca.hss.times.WTimeSpan;
import ca.hss.times.WorldLocation;
import ca.hss.times.serialization.TimeSerializer;

/**
 * Convert the data from the internal storage format for the API to a protobuf message.
 * 
 * @author Travis Redpath
 */
@SuppressWarnings("deprecation")
public class PBCombiner extends IPBCombiner {

	/**
	 * Hide the constructor.
	 */
	PBCombiner(Object options) {
		super(options);
	}
	
	/**
	 * Parse LUT data from a collection of fuel definitions.
	 * @param reader A reader that contains the LUT contents.
	 * @param builder A builder for the fuel collection.
	 * @param fuelsBuilder A builder for the fuel map.
	 */
	private void parseLutDefinition(Reader reader, FuelCollection.Builder builder, CwfgmFuelMap.Builder fuelsBuilder) throws IOException {
	    String lutText = CharStreams.toString(reader);
	    if (lutText.length() > 22) {
	        //split the definition list by fuel
    	    String lutSplit[] = lutText.substring(22).split("\\|API_FUEL\\|");

            Map<String, Integer> existingFuels = new HashMap<>();
            int fuelIndex = 0;
    	    for (String line : lutSplit) {
    	        String agency = null;
    	        try {
        	        //split the fuel by individual data point
        	        String data[] = line.split("\\|");
    
                    agency = data[0];
                    String fbp = data[1];
    
                    ProjectFuel.Builder fuel = builder.addFuelsBuilder()
                            .setVersion(1)
                            .setName(agency)
                            .setDefault(fbp);
                    CwfgmFuelData.Builder fuelData = fuelsBuilder.addDataBuilder();
                    
                    if (data[3].equals("RGB")) {
                        fuel.setColor(RGB(data[4], data[5], data[6]));
                    }
                    else {
                        fuel.setColor(HSL(data[4], data[5], data[6]));
                    }
                    int index = -1;
                    try {
                        index = Integer.parseInt(data[2]);
                    }
                    catch (NumberFormatException e) { }
                    fuelData.setIndex(index);
                    fuelData.setExportIndex(index);
                    IntegerHelper dataIndex = new IntegerHelper(7);
                    
                    //this is a duplicate type
                    if (existingFuels.containsKey(agency)) {
                        fuelData.setFuelIndex(ProtoWrapper.ofInt(existingFuels.get(agency)));
                    }
                    //this is a new type
                    else {
                        existingFuels.put(agency, fuelIndex);
                        fuelIndex++;
                        
                        FbpFuel.Builder fbpBuilder = fuelData.getFuelDataBuilder()
                                .setVersion(1)
                                .getDataBuilder()
                                .setName(agency)
                                .getFuelBuilder()
                                .setVersion(1)
                                .setFuelTypeModified(false);
                        
                        parseLutDefinitionFuel(data, dataIndex, fbp, fbpBuilder);
                    }
    	        }
    	        catch (Exception e) {
    	            if (Strings.isNullOrEmpty(agency))
    	                throw new IOException("Invalid fuel definition, unable to parse.", e);
    	            else
    	                throw new IOException("Invalid fuel definition (" + agency + "), unable to parse.", e);
    	        }
    	    }
	    }
	    else
	        throw new IOException("Incomplete LUT definition.");
	}
	
	private void parseLutFile(Reader reader, FuelCollection.Builder builder, CwfgmFuelMap.Builder fuelsBuilder) throws IOException {
        CSVParser records = CSVFormat.RFC4180
                .withFirstRecordAsHeader()
                .withIgnoreSurroundingSpaces()
                .parse(reader);
        Map<LUTColumn, String> columns = buildColumns(records);
        boolean hasHSL = columns.containsKey(LUTColumn.COLOUR_H) &&
                columns.containsKey(LUTColumn.COLOUR_S) && columns.containsKey(LUTColumn.COLOUR_L);
        boolean hasRGB = columns.containsKey(LUTColumn.COLOUR_R) &&
                columns.containsKey(LUTColumn.COLOUR_G) && columns.containsKey(LUTColumn.COLOUR_B);
        boolean hasExport = columns.containsKey(LUTColumn.E_GRID_VALUE);
        boolean valid = ((hasExport && (columns.size() == 7 || columns.size() == 10)) ||
                (columns.size() == 6 || columns.size() == 9)) &&
                columns.containsKey(LUTColumn.I_GRID_VALUE) && columns.containsKey(LUTColumn.A_FUEL_TYPE) &&
                columns.containsKey(LUTColumn.F_FUEL_TYPE) && (hasHSL || hasRGB);
        
        Map<String, Integer> existingFuels = new HashMap<>();
        int fuelIndex = 0;
        //if the LUT file has the correct number of columns
        if (valid) {
            for (CSVRecord record : records) {
                ProjectFuel.Builder fuel = builder.addFuelsBuilder()
                        .setVersion(1);
                CwfgmFuelData.Builder fuelData = fuelsBuilder.addDataBuilder();
                if (hasRGB)
                    fuel.setColor(RGB(record.get(columns.get(LUTColumn.COLOUR_R)),
                            record.get(columns.get(LUTColumn.COLOUR_G)),
                            record.get(columns.get(LUTColumn.COLOUR_B))));
                else
                    fuel.setColor(HSL(record.get(columns.get(LUTColumn.COLOUR_H)),
                            record.get(columns.get(LUTColumn.COLOUR_S)),
                            record.get(columns.get(LUTColumn.COLOUR_L))));
                int index = -1, exportIndex = -1;
                try {
                    index = Integer.parseInt(record.get(columns.get(LUTColumn.I_GRID_VALUE)));
                    if (hasExport)
                        exportIndex = Integer.parseInt(record.get(columns.get(LUTColumn.E_GRID_VALUE)));
                    else
                        exportIndex = index;
                }
                catch (NumberFormatException e) { }
                fuelData.setIndex(index);
                fuelData.setExportIndex(exportIndex);
                
                String fbp = record.get(columns.get(LUTColumn.F_FUEL_TYPE));
                String agency = record.get(columns.get(LUTColumn.A_FUEL_TYPE));
                fuel.setDefault(fbp);
                fuel.setName(agency);
                
                //this is a duplicate type
                if (existingFuels.containsKey(agency)) {
                    fuelData.setFuelIndex(ProtoWrapper.ofInt(existingFuels.get(agency)));
                }
                //this is a new type
                else {
                    existingFuels.put(agency, fuelIndex);
                    fuelIndex++;
                    
                    FbpFuel.Builder fbpBuilder = fuelData.getFuelDataBuilder()
                            .setVersion(1)
                            .getDataBuilder()
                            .setName(agency)
                            .getFuelBuilder()
                            .setVersion(1);
                    fbpBuilder.setFuelTypeModified(false);
                    String name, options;
                    if (fbp.contains("(") && fbp.substring(fbp.indexOf("(")).contains(")")) {
                        options = fbp.substring(fbp.indexOf("(") + 1, fbp.indexOf(")")).trim();
                        name = fbp.substring(0, fbp.indexOf("(")).trim();
                    }
                    else {
                        name = fbp;
                        options = null;
                    }
                    fbpBuilder.setDefaultFuelType(findFuelName(name));
                    List<FuelOption> userOptions = findFuelOptions(name);
                    applyLutOptions(fbpBuilder, userOptions, options);
                }
            }
        }
	}
	
	/**
	 * Add the grid files (elevation, fuel map, projection, LUT) to the protobuf job.
	 * @param data THe protobuf message builder.
	 */
	private void setGridFiles(PrometheusData.Builder data) {
	    String gridName;
	    gridName = input.getInputs().getFiles().getFuelmapFile();
		CwfgmGrid.Builder gridBuilder = data.getProjectBuilder()
			.setVersion(1)
			.setElevationName(filename(input.getInputs().getFiles().getElevFile()))
			.setProjectionName(filename(input.getInputs().getFiles().getProjFile()))
			.setGridName(filename(gridName))
			.getGridBuilder()
			.setVersion(1)
			.setProjection(
				ProjectionFile.newBuilder()
					.setFilename(ProtoWrapper.ofString(readyFile(data, jobDir, input.getInputs().getFiles().getProjFile(), false)))
			);
		if (!Strings.isNullOrEmpty(input.getInputs().getFiles().getFuelmapFile()))
		    gridBuilder.setFuelMap(
		                FuelMapFile.newBuilder()
		                .setFilename(ProtoWrapper.ofString(readyFile(data, jobDir, input.getInputs().getFiles().getFuelmapFile(), true)))
		            );
		else
		    throw new NullPointerException("No fuel map defined.");
		if (!Strings.isNullOrEmpty(input.getInputs().getFiles().getElevFile())) {
			gridBuilder.setElevation(
					ElevationFile.newBuilder()
						.setFilename(ProtoWrapper.ofString(readyFile(data, jobDir, input.getInputs().getFiles().getElevFile(), true)))
				);
		}
		
		FuelCollection.Builder builder = data.getProjectBuilder()
			.getFuelCollectionBuilder()
			.setVersion(1)
			.setImportedLut(ProtoWrapper.ofBool(true))
			.setLutFilename(ProtoWrapper.ofString("lut.csv"));
		
		CwfgmFuelMap.Builder fuelsBuilder = data.getProjectBuilder()
			.getFuelsBuilder()
			.setVersion(1);
		
		//if the LUT file is an attachment, read that
		if (input.getInputs().getFiles().getLutFile().startsWith("attachment:")) {
		    Optional<ca.wise.api.FileAttachment> file = input.getAttachments().stream().filter(x -> x.getFilename().equals(input.getInputs().getFiles().getLutFile())).findFirst();
		    System.out.println("Found file attachment " + file.get().getFilename());
		    if (file.isPresent()) {
		        if (file.get().getData() == null) {
		            String lutChars = file.get().getContents().toString();
		            boolean isDefn = lutChars.startsWith("API_FUEL_DEF");
    		        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(lutChars.getBytes()))) {
    		            if (isDefn)
    		                parseLutDefinition(reader, builder, fuelsBuilder);
    		            else
    		                parseLutFile(reader, builder, fuelsBuilder);
    		        }
    	            catch (IOException e) {
    	                e.printStackTrace();
    	            }
		        }
		        else {
		            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(file.get().getData()))) {
                        parseLutFile(reader, builder, fuelsBuilder);
		            }
		            catch (IOException e) {
		                e.printStackTrace();
		            }
		        }
		    }
		}
		//if the LUT file exists read it in
		else if (Files.exists(Paths.get(input.getInputs().getFiles().getLutFile()))) {
			try (Reader reader = new FileReader(input.getInputs().getFiles().getLutFile())) {
			    parseLutFile(reader, builder, fuelsBuilder);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Create a new fuel break file for the protobuf message.
	 * @param input The details of the fuel break from the API.
	 * @return A new vector grid.
	 */
	private Vector.Builder createFuelBreakFile(PrometheusData.Builder data, FuelBreak input) {
		Vector.Builder builder = Vector.newBuilder();
		builder.getFilterBuilder()
			.setVersion(1)
			.setComments(input.getComments())
			.setName(input.getId());
		if (input.getType() == FuelBreakType.POLYGON) {
			XYPolygon.Builder poly = builder.getFilterBuilder()
					.getFilterBuilder()
					.setVersion(1)
					.setFireBreakWidth(HSS_Double.of(input.getWidth()))
					.getPolygonsBuilder()
					.setUnits(GeoUnits.LAT_LON)
					.getPolysetBuilder()
					.setVersion(1)
					.addPolysBuilder()
					.setPolyType(PolyType.Polygon)
					.setIsHole(false)
					.getPolygonBuilder()
					.setVersion(1);
			for (LatLon ll : input.getFeature()) {
				poly.addPointsBuilder()
					.setX(HSS_Double.of(ll.getLongitude()))
					.setY(HSS_Double.of(ll.getLatitude()));
			}
		}
		else if (input.getType() == FuelBreakType.POLYLINE) {
			XYPolygon.Builder poly = builder.getFilterBuilder()
					.getFilterBuilder()
					.setVersion(1)
                    .setFireBreakWidth(HSS_Double.of(input.getWidth()))
					.getPolygonsBuilder()
					.setUnits(GeoUnits.LAT_LON)
					.getPolysetBuilder()
					.setVersion(1)
					.addPolysBuilder()
					.setPolyType(PolyType.Polyline)
					.setIsHole(false)
					.getPolygonBuilder()
					.setVersion(1);
			for (LatLon ll : input.getFeature()) {
				poly.addPointsBuilder()
					.setX(HSS_Double.of(ll.getLongitude()))
					.setY(HSS_Double.of(ll.getLatitude()));
			}
		}
		else if (input.getType() == FuelBreakType.FILE) {
			builder.getFilterBuilder()
			    .getFileBuilder()
			    .setFireBreakWidth(HSS_Double.of(input.getWidth()))
				.setFilename(readyFile(data, jobDir, input.getFilename(), true));
		}
		return builder;
	}

	/**
	 * Create a fuel patch for the protobuf message.
	 * @param input The details of the fuel patch from the API.
	 * @return A new fuel patch.
	 */
	private Filter.Builder createFuelPatchFile(PrometheusData.Builder data, FuelPatch input) {
		Filter.Builder builder = Filter.newBuilder();
		ProjectReplaceGridFilterBase.Builder filter = null;
		if (input.getType() == FuelPatchType.POLYGON) {
			filter = builder.getPolyReplaceBuilder()
				.setVersion(1)
				.setName(input.getId())
				.setComments(input.getComments())
				.getFilterBuilder();
			
			GeoPoly.Builder poly = builder.getPolyReplaceBuilder().getPolygonBuilder().getPolygonsBuilder();
			poly.setUnits(GeoUnits.LAT_LON);
			poly.getPolygonBuilder()
				.setVersion(1);
			for (LatLon ll : input.getFeature()) {
				poly.getPolygonBuilder().addPointsBuilder()
					.setX(HSS_Double.of(ll.getLongitude()))
					.setY(HSS_Double.of(ll.getLatitude()));
			}
		}
		else if (input.getType() == FuelPatchType.LANDSCAPE) {
			filter = builder.getReplaceBuilder()
				.setVersion(1)
				.setName(input.getId())
				.setComments(input.getComments())
				.getFilterBuilder();
		}
		else if (input.getType() == FuelPatchType.FILE) {
			filter = builder.getPolyReplaceBuilder()
				.setVersion(1)
				.setName(input.getId())
				.setComments(input.getComments())
				.setFilename(ProtoWrapper.ofString(readyFile(data, jobDir, input.getFilename(), true)))
				.getFilterBuilder();
		}
		
		if (filter != null) {
			filter.setVersion(1);
			if (input.getToFuelIndex() != null)
			    filter.setToFuelIndex(input.getToFuelIndex());
			else
			    filter.setToFuelName(input.getToFuel());
			if (input.getFromFuelRule() != null) {
				switch (input.getFromFuelRule()) {
				case NODATA:
					filter.setFromFuelRule(FromFuelRule.NODATA);
					break;
				case ALL:
					filter.setFromFuelRule(FromFuelRule.ALL_FUELS);
					break;
				case ALL_COMBUSTABLE:
					filter.setFromFuelRule(FromFuelRule.ALL_COMBUSTIBLE_FUELS);
					break;
                default:
                	break;
				}
			}
			else if (input.getFromFuelIndex() != null)
			    filter.setFromFuelIndex(input.getFromFuelIndex());
			else
				filter.setFromFuelName(input.getFromFuel());
		}
		
		return builder;
	}

	/**
	 * Create a wind speed or direction grid for the protobuf message.
	 * @param input The details of the grid from the API.
	 * @return A new wind speed or direction grid.
	 */
	private Filter.Builder createWeatherGrid(PrometheusData.Builder data, WeatherGrid input) {
		Filter.Builder builder = Filter.newBuilder();
		WindGrid.Builder filter = builder.getWindBuilder()
			.setVersion(1)
			.setName(input.getId())
			.setComments(input.getComments())
			.getGridBuilder()
			.setVersion(1)
			.setStartTime(TimeSerializer.serializeTime(createDateTime(input.getStartTime())))
			.setEndTime(TimeSerializer.serializeTime(createDateTime(input.getEndTime())))
			.setStartSpan(TimeSerializer.serializeTimeSpan(createDuration(input.getStartTimeOfDay())));
		if (input.getEndTimeOfDay().equals("24:00:00"))
			filter.setEndSpan(TimeSerializer.serializeTimeSpan(createDuration("23:59:59")));
		else
			filter.setEndSpan(TimeSerializer.serializeTimeSpan(createDuration(input.getEndTimeOfDay())));
		switch (input.getType()) {
		case DIRECTION:
			builder.getWindBuilder().setType(GridType.WindDirection);
			filter.setType(GridType.WindDirection);
			break;
		default:
			builder.getWindBuilder().setType(GridType.WindSpeed);
			filter.setType(GridType.WindSpeed);
			break;
		}
		
		if (input.getGridData().size() > 0)
		    filter.setApplyFileSectors(true);
		
		for (WeatherGridGridFile d : input.getGridData()) {
			SectorData.Builder sector = filter.addSectorDataBuilder()
					.setVersion(1);
			
			sector.addSectorEntriesBuilder()
					.setVersion(1)
					.setSpeed(HSS_Double.of(d.getSpeed()))
					.getDataBuilder()
					.setVersion(1)
					.getFileBuilder()
					.setVersion(1)
					.setFilename(readyFile(data, jobDir, d.getFilename(), true))
					.setProjectionFilename(ProtoWrapper.ofString(readyFile(data, jobDir, d.getProjection(), false)));
			
			sector.getDirectionBuilder()
				.setCardinalDirection(stringToDirection(d.getSector().name().toLowerCase()));
		}
        //add the default sector data if the user specified it
		if (!Strings.isNullOrEmpty(input.getDefaultValuesFile()) && !Strings.isNullOrEmpty(input.getDefaultValuesProjection())) {
            wcsData.Builder defaults = filter.getDefaultSectorDataBuilder();
            defaults.setVersion(1);
            defaults.setXSize(0);
            defaults.setYSize(0);
            defaults.getFileBuilder()
                .setVersion(1)
                .setFilename(input.getDefaultValuesFile())
                .setProjectionFilename(ProtoWrapper.ofString(input.getDefaultValuesProjection()));
		}
		return builder;
	}

	/**
	 * Create a weather patch for protobuf message.
	 * @param input The details of the weather patch from the API.
	 * @return A weather patch.
	 */
	private Filter.Builder createWeatherPatch(PrometheusData.Builder data, WeatherPatch input) {
		Filter.Builder builder = Filter.newBuilder();
		WeatherGridFilter.Builder weather = null;
		switch (input.getType()) {
		case LANDSCAPE:
			weather = builder.getPolyWeatherBuilder()
				.setName(input.getId())
				.setComments(input.getComments())
				.setLandscape(true)
                .getFilterBuilder();
		case POLYGON:
			weather = builder.getPolyWeatherBuilder()
				.setVersion(1)
				.setName(input.getId())
				.setComments(input.getComments())
				.getFilterBuilder();
			break;
		case FILE:
			weather = builder.getPolyWeatherBuilder()
				.setVersion(1)
				.setName(input.getId())
				.setComments(input.getComments())
				.getFilterBuilder()
				.setFilename(readyFile(data, jobDir, input.getFilename(), true));
			break;
		default:
			assert false;
			builder = null;
			break;
		}
		
		if (weather != null) {
			weather
				.setVersion(1)
				.setStartTime(TimeSerializer.serializeTime(createDateTime(input.getStartTime())))
				.setEndTime(TimeSerializer.serializeTime(createDateTime(input.getEndTime())));
			if (!input.getEndTimeOfDay().isEmpty())
				weather.setEndTimeOfDay(TimeSerializer.serializeTimeSpan(createDuration(input.getEndTimeOfDay())));
			if (!input.getStartTimeOfDay().isEmpty())
				weather.setStartTimeOfDay(TimeSerializer.serializeTimeSpan(createDuration(input.getStartTimeOfDay())));
			
			if (input.getTemperature() != null) {
				weather.getTemperatureBuilder()
					.setVersion(1)
					.setValue(HSS_Double.of(input.getTemperature().getValue()))
					.setOperation(operationToOperationOne(input.getTemperature().getOperation()));
			}
			if (input.getRh() != null) {
				weather.getRhBuilder()
					.setVersion(1)
					.setValue(HSS_Double.of(input.getRh().getValue()))
					.setOperation(operationToOperationOne(input.getRh().getOperation()));
			}
			if (input.getPrecip() != null) {
				weather.getPrecipitationBuilder()
					.setVersion(1)
					.setValue(HSS_Double.of(input.getPrecip().getValue()))
					.setOperation(operationToOperationOne(input.getPrecip().getOperation()));
			}
			if (input.getWindSpeed() != null) {
				weather.getWindSpeedBuilder()
					.setVersion(1)
					.setValue(HSS_Double.of(input.getWindSpeed().getValue()))
					.setOperation(operationToOperationOne(input.getWindSpeed().getOperation()));
			}
			if (input.getWindDirection() != null) {
				weather.getWindDirectionBuilder()
					.setVersion(1)
					.setValue(HSS_Double.of(input.getWindDirection().getValue()))
					.setOperation(operationToOperationTwo(input.getWindDirection().getOperation()));
			}
			
			if (input.getType() == WeatherPatchType.POLYGON) {
				GeoPoly.Builder geo = weather.getPolygonsBuilder()
						.setUnits(GeoUnits.LAT_LON);
				geo.getPolygonBuilder()
					.setVersion(1);
				for (LatLon ll : input.getFeature()) {
					geo.getPolygonBuilder()
						.addPointsBuilder()
						.setX(HSS_Double.of(ll.getLongitude()))
						.setY(HSS_Double.of(ll.getLatitude()));
				}
			}
		}
		else
			assert false;
		
		return builder;
	}
	
	/**
	 * Convert an attribute grid filter type from the internal representation to a protobuf enum.
	 * @param type The internal attribute grid filter type enum.
	 * @return A protobuf enum for the attribute grid filter type. Will be {@link AttributeType#FUEL_GRID}
	 * if the attribute grid type is not known.
	 */
	private AttributeType typeToAttribute(GridFileType type) {
		switch (type) {
		case DEGREE_CURING:
			return AttributeType.DEGREE_CURING;
		case GREEN_UP:
			return AttributeType.GREENUP;
		case PERCENT_CONIFER:
			return AttributeType.PERCENT_CONIFER;
		case PERCENT_DEAD_FIR:
			return AttributeType.PERCENT_DEAD_FIR;
		case CROWN_BASE_HEIGHT:
			return AttributeType.CROWN_BASE_HEIGHT;
		case TREE_HEIGHT:
			return AttributeType.TREE_HEIGHT;
		case FUEL_LOAD:
		    return AttributeType.FUEL_LOAD;
		case FBP_VECTOR:
		    return AttributeType.FBP_VECTOR;
		default:
			return AttributeType.FUEL_GRID;
		}
	}

	/**
	 * Create an attribute grid filter for the protobuf message.
	 * @param input The details of the grid filter from the API.
	 * @return An attribute grid filter.
	 */
	private Filter.Builder createInputGridFile(PrometheusData.Builder data, GridFile input) {
		Filter.Builder builder = Filter.newBuilder();
		builder.getAttributeBuilder()
			.setType(typeToAttribute(input.getType()))
			.getFilterBuilder()
			.setVersion(1)
			.setName(input.getName())
			.setComments(input.getComment())
			.getFilterBuilder()
			.setVersion(1)
			.getFileBuilder()
			.setFilename(readyFile(data, jobDir, input.getFilename(), true))
			.setProjection(readyFile(data, jobDir, input.getProjection(), false));
		return builder;
	}
	
	/**
	 * Add a weather stream to a weather station.
	 * @param input The details of the weather stream from the API.
	 * @param station The weather station to add the weather stream to.
	 */
	private void createWeatherStream(PrometheusData.Builder data, WeatherStream input, ProjectWeatherStation.Builder station) {
		ca.wise.weather.proto.WeatherStream.Builder stream = station.getStationBuilder()
			.addStreamsBuilder()
			.setVersion(1)
			.setName(ProtoWrapper.ofString(input.getName()))
			.setComments(ProtoWrapper.ofString(input.getComments()))
			.getConditionBuilder()
			.setVersion(1)
			.setDataImportedFromFile(true)
			//TODO skipped end time
			.setFilename(readyFile(data, jobDir, input.getFilename(), false))
			.setHffmc(HSS_Double.of(input.getHffmcValue()))
			.setHffmcTime(TimeSerializer.serializeTimeSpan(new WTimeSpan(0, input.getHffmcHour(), 0, 0)))
			.setHffmcMethod(hffmcToMethod(input.getHffmcMethod()));
		
		if (input.getStartTime() != null && input.getStartTime().length() > 0)
			stream.setStartTime(TimeSerializer.serializeTime(createDateTime(input.getStartTime())));
		
		ca.wise.weather.proto.WeatherStream.StartingCodes.Builder starting =  stream.getStartingCodesBuilder()
			.setFfmc(HSS_Double.of(input.getStartingFfmc()))
			.setDmc(HSS_Double.of(input.getStartingDmc()))
			.setDc(HSS_Double.of(input.getStartingDc()))
			.setPrecipitation(HSS_Double.of(input.getStartingPrecip()));
		if (input.getStartingBui() != null)
		    starting.setBui(HSS_Double.of(input.getStartingBui()));
		
		if (input.getDiurnalTemperatureAlpha() != null) {
			stream.getTemperatureBuilder()
				.setAlpha(HSS_Double.of(input.getDiurnalTemperatureAlpha()))
				.setBeta(HSS_Double.of(input.getDiurnalTemperatureBeta()))
				.setGamma(HSS_Double.of(input.getDiurnalTemperatureGamma()));
			stream.getWindBuilder()
				.setAlpha(HSS_Double.of(input.getDiurnalWindSpeedAlpha()))
				.setBeta(HSS_Double.of(input.getDiurnalWindSpeedBeta()))
				.setGamma(HSS_Double.of(input.getDiurnalWindSpeedGamma()));
		}
	}

	/**
	 * Create a weather station for the protobuf message.
	 * @param input The details of the weather station from the API.
	 * @return A weather station.
	 */
	private ProjectWeatherStation.Builder createWeatherStation(PrometheusData.Builder data, WeatherStation input) {
		ProjectWeatherStation.Builder builder = ProjectWeatherStation.newBuilder()
				.setVersion(1)
				.setName(input.getName())
				.setComments(input.getComments() == null ? "" : input.getComments());
		
		CwfgmWeatherStation.Builder station = builder.getStationBuilder()
				.setVersion(1)
				.setElevation(HSS_Double.of(input.getElevation()))
				.setSkipStream(ProtoWrapper.ofBool(true));
		
		station.getLocationBuilder()
			.setUnits(GeoUnits.LAT_LON)
			.getPointBuilder()
			.setX(HSS_Double.of(input.getLocation().getLongitude()))
			.setY(HSS_Double.of(input.getLocation().getLatitude()));
		
		for (WeatherStream stream : input.getStreams()) {
			createWeatherStream(data, stream, builder);
		}
		return builder;
	}

	/**
	 * Create an ignition for the protobuf message.
	 * @param input Details of the ignition from the API.
	 * @return An ignition.
	 */
	private ProjectFire.Builder createIgnition(PrometheusData.Builder data, Ignition input) {
		ProjectFire.Builder builder = ProjectFire.newBuilder()
				.setVersion(1)
				.setName(input.getName())
				.setComments(input.getComments())
				.setImported(true)
                .setColor(255)
                .setSize(3);
		
		CwfgmIgnition.Builder ignition = builder.getIgnitionBuilder()
			.setVersion(1)
			.setStartTime(TimeSerializer.serializeTime(createDateTime(input.getStartTime())));
		
		IgnitionPoint.Builder pointBuilder = null;
		switch (input.getType()) {
		case FILE:
			ignition.setFilename(readyFile(data, jobDir, input.getFilename(), true));
			break;
		case POINT:
			pointBuilder = ignition.getIgnitionsBuilder()
				.addIgnitionsBuilder()
				.setPolyType(IgnitionShape.POINT);
			break;
		case POLYGON:
			pointBuilder = ignition.getIgnitionsBuilder()
				.addIgnitionsBuilder()
				.setPolyType(IgnitionShape.POLYGON_OUT);
			break;
		case POLYLINE:
			pointBuilder = ignition.getIgnitionsBuilder()
				.addIgnitionsBuilder()
				.setPolyType(IgnitionShape.LINE);
			break;
		default:
			assert false;
			break;
		}
		
		if (pointBuilder != null) {
			GeoPoly.Builder geo = pointBuilder.getPolygonBuilder()
					.setUnits(GeoUnits.LAT_LON);
			geo.getPolygonBuilder()
				.setVersion(1);
			for (LatLon ll : input.getFeature()) {
				geo.getPolygonBuilder()
					.addPointsBuilder()
					.setX(HSS_Double.of(ll.getLongitude()))
					.setY(HSS_Double.of(ll.getLatitude()));
			}
			for (AttributeEntry attr : input.getAttributes()) {
			    if (attr.getValue() instanceof String) {
			        pointBuilder.addAttributesBuilder()
			            .setName(attr.getKey())
			            .setValue(Any.pack(ProtoWrapper.ofString((String)attr.getValue())));
			    }
			    else if (attr.getValue() instanceof Double) {
                    pointBuilder.addAttributesBuilder()
                        .setName(attr.getKey())
                        .setValue(Any.pack(ProtoWrapper.ofDouble((Double)attr.getValue())));
			    }
			    else if (attr.getValue() instanceof Integer) {
                    pointBuilder.addAttributesBuilder()
                        .setName(attr.getKey())
                        .setValue(Any.pack(ProtoWrapper.ofInt((Integer)attr.getValue())));
			    }
			    else if (attr.getValue() instanceof Long) {
			        pointBuilder.addAttributesBuilder()
			            .setName(attr.getKey())
			            .setValue(Any.pack(ProtoWrapper.ofLong((Long)attr.getValue())));
			    }
			}
		}
		return builder;
	}
	
	/**
     * Create an asset for the protobuf message.
     * @param input Details of the asset from the API.
     * @return An asset.
	 */
	private ProjectAsset.Builder createAssetFile(PrometheusData.Builder data, AssetFile input) {
	    ProjectAsset.Builder builder = ProjectAsset.newBuilder()
                .setVersion(1)
                .setName(input.getName())
                .setComments(input.getComments())
                .setColor(0xff00)
                .setFillColor(0xaa00ff00)
                .setWidth(3)
                .setImported(true);
	    
	    if (input.getType() == AssetShapeType.FILE) {
	        ProjectAsset.VectorFile.Builder file = builder.getFileBuilder()
	                .setFilename(readyFile(data, jobDir, input.getFilename(), true));
	        if (input.getBuffer() != null && input.getBuffer() > 0.0)
	            file.setAssetBoundary(HSS_Double.of(input.getBuffer()));
	    }
	    else {
	        CwfgmAsset.Builder file = builder.getFilterBuilder();
	        for (LatLon ll : input.getFeature())
	            file.getAssetsBuilder()
    	            .getPolygonBuilder()
                        .addPointsBuilder()
                            .setX(HSS_Double.of(ll.getLongitude()))
                            .setY(HSS_Double.of(ll.getLatitude()));
	        if (input.getBuffer() != null && input.getBuffer() > 0.0)
	            file.setAssetBoundary(HSS_Double.of(input.getBuffer()));
	    }

        return builder;
	}
    
    /**
     * Create a target for the protobuf message.
     * @param input Details of the target from the API.
     * @return An asset.
     */
    private ProjectTarget.Builder createTargetFile(PrometheusData.Builder data, TargetFile input) {
        ProjectTarget.Builder builder = ProjectTarget.newBuilder()
                .setVersion(1)
                .setName(input.getName())
                .setComments(input.getComments())
                .setColor(0x0f0f00)
                .setSymbol(54)
                .setDisplaySize(3)
                .setImported(true);
        
        if (input.getType() == AssetShapeType.FILE) {
            builder.getFileBuilder()
                    .setFilename(readyFile(data, jobDir, input.getFilename(), true));
        }
        else {
            CwfgmTarget.Builder file = builder.getFilterBuilder();
            for (LatLon ll : input.getFeature())
                file.getTargetsBuilder()
                    .getPolygonBuilder()
                        .addPointsBuilder()
                            .setX(HSS_Double.of(ll.getLongitude()))
                            .setY(HSS_Double.of(ll.getLatitude()));
        }

        return builder;
    }

	/**
	 * Populate the details of a scenario.
	 * @param builder The scenario that is being populated.
	 * @param input The details of the scenario from the API.
	 * @param index The index of the scenario for looking up the number of cores it's allowed to use.
	 */
	private void populateScenario(ProjectScenario.Builder builder, Scenario input, int index) {
		builder.setName(input.getId())
			.setComments(input.getComments())
			.getScenarioBuilder()
			.setVersion(3)
			.setStartTime(TimeSerializer.serializeTime(createDateTime(input.getStartTime())))
			.setEndTime(TimeSerializer.serializeTime(createDateTime(input.getEndTime())))
			.setDisplayInterval(TimeSerializer.serializeTimeSpan(createDuration(input.getDisplayInterval())));
		TemporalCondition.Builder conds = builder.getTemporalConditionsBuilder()
				.setVersion(1);

        builder.setGlobalAssetOperationValue(input.getFgmOptions().getGlobalAssetOperation().value);
        if (input.getFgmOptions().getAssetCollisionCount() >= 0)
            builder.setGlobalCollisionCount(ProtoWrapper.ofInt(input.getFgmOptions().getAssetCollisionCount()));
		
		for (BurningConditions bc : input.getBurningConditions()) {
			DailyAttribute.Builder burn = conds.addDailyBuilder()
				.setVersion(1)
				.setLocalStartTime(ca.hss.times.proto.WTime.newBuilder().setTime(bc.getDate() + "T00:00:00"))
				.setMinFwi(HSS_Double.of(bc.getFwiGreater()))
				.setMaxWs(HSS_Double.of(bc.getWsGreater()))
				.setMinRh(HSS_Double.of(bc.getRhLess() / 100.0))
				.setMinIsi(HSS_Double.of(bc.getIsiGreater()));
			if (bc.getStartTimeOffset() != BurningConditionRelative.UNKNOWN)
			    burn.setLocalStartTimeRelativeValue(bc.getStartTimeOffset().value);
            if (bc.getEndTimeOffset() != BurningConditionRelative.UNKNOWN)
                burn.setLocalEndTimeRelativeValue(bc.getEndTimeOffset().value);
			if (bc.getStartTime().length() > 0)
				burn.setStartTime(TimeSerializer.serializeTimeSpan(createDuration(bc.getStartTime())));
			if (bc.getEndTime().length() > 0) {
			    WTimeSpan end = createDuration(bc.getEndTime());
			    //don't allow the end time to be greater than 23 hours, 59 minutes, and 59 seconds
			    if (WTimeSpan.greaterThanEqualTo(end, new WTimeSpan(0, 24, 0, 0)))
			        end = new WTimeSpan(0, 23, 59, 59);
				burn.setEndTime(TimeSerializer.serializeTimeSpan(end));
			}
		}
		
		for (SeasonalCondition sc : input.getSeasonalConditions()) {
		    SeasonalAttribute.Builder burn = conds.addSeasonalBuilder()
		            .setVersion(1);
		    if (!Strings.isNullOrEmpty(sc.getStartTime()))
		        burn.setLocalStartTime(TimeSerializer.serializeTimeSpan(createDuration(sc.getStartTime())));
		    EffectiveAttribute.Builder effective = burn.addAttributesBuilder()
		            .setTypeValue(sc.getType().value);
		    effective.setActive(sc.isActive());
		    if (sc.getValue() != null)
		        effective.setValue(HSS_Double.of(sc.getValue()));
		}
		
		for (String vector : input.getVectorInfo()) {
			builder.addVectorIndexBuilder()
				.setName(vector);
		}
		
		for (StationStream st : input.getStationStreams()) {
			WeatherIndex.Builder station = builder.addWeatherIndexBuilder();
			if (st.isPrimaryStream())
				station.setIsPrimary(ProtoWrapper.ofBool(true));
			station.getStationIndexBuilder()
				.setName(st.getStation());
			station.getStreamIndexBuilder()
				.setName(st.getStream());
			if (st.getStreamOptions() != null) {
			    if (!Strings.isNullOrEmpty(st.getStreamOptions().getName()))
    			    station.getWeatherOptionsBuilder()
    			        .setSubName(st.getStreamOptions().getName());
			    if (!Strings.isNullOrEmpty(st.getStreamOptions().getStartTime()))
			        station.getWeatherOptionsBuilder()
			            .setStartTime(TimeSerializer.serializeTime(createDateTime(st.getStreamOptions().getStartTime())));
			    if (!Strings.isNullOrEmpty(st.getStreamOptions().getEndTime()))
			        station.getWeatherOptionsBuilder()
			            .setEndTime(TimeSerializer.serializeTime(createDateTime(st.getStreamOptions().getEndTime())));
			    if (!Strings.isNullOrEmpty(st.getStreamOptions().getIgnitionTime()))
			        station.getWeatherOptionsBuilder()
			            .setIgnitionTime(TimeSerializer.serializeTime(createDateTime(st.getStreamOptions().getIgnitionTime())));
			    if (st.getStreamOptions().getWindDirection() != null)
			        station.getWeatherOptionsBuilder()
			            .setWindDirection(HSS_Double.of(st.getStreamOptions().getWindDirection()));
			    if (st.getStreamOptions().getDeltaWindDirection() != null)
			        station.getWeatherOptionsBuilder()
			            .setDeltaWindDirection(HSS_Double.of(st.getStreamOptions().getDeltaWindDirection()));
			}
		}
		
		for (IgnitionReference ignition : input.getIgnitionInfo()) {
			Reference.Builder bldr = builder.addFireIndexBuilder()
				.setName(ignition.getIgnition());
			//options for multi-point ignitions
			if (ignition.getMultiPointIgnitionOptions() != null) {
			    if (ignition.getMultiPointIgnitionOptions().getPointIndex() != null)
			        bldr.getPointIgnitionOptionsBuilder()
			            .setPointIndex(ProtoWrapper.ofInt(ignition.getMultiPointIgnitionOptions().getPointIndex()));
			    if (!Strings.isNullOrEmpty(ignition.getMultiPointIgnitionOptions().getName()))
			        bldr.getPointIgnitionOptionsBuilder()
			            .setSubName(ignition.getMultiPointIgnitionOptions().getName());
			}
			//options for polyline ignitions
			else if (ignition.getPolylineIgnitionOptions() != null) {
			    if (ignition.getPolylineIgnitionOptions().getPointIndex() != null)
			        bldr.getPolylineIgnitionOptionsBuilder()
			            .setPointIndex(ProtoWrapper.ofInt(ignition.getPolylineIgnitionOptions().getPointIndex()));
			    if (ignition.getPolylineIgnitionOptions().getPolyIndex() != null)
			        bldr.getPolylineIgnitionOptionsBuilder()
			            .setPolyIndex(ProtoWrapper.ofInt(ignition.getPolylineIgnitionOptions().getPolyIndex()));
			    if (ignition.getPolylineIgnitionOptions().getPointSpacing() != null)
			        bldr.getPolylineIgnitionOptionsBuilder()
			            .setPointSpacing(HSS_Double.of(ignition.getPolylineIgnitionOptions().getPointSpacing()));
			    if (!Strings.isNullOrEmpty(ignition.getPolylineIgnitionOptions().getName()))
			        bldr.getPolylineIgnitionOptionsBuilder()
			            .setSubName(ignition.getPolylineIgnitionOptions().getName());
			}
			//options for single point ignitions
			else if (ignition.getSinglePointIgnitionOptions() != null) {
			    bldr.getSinglePointIgnitionOptionsBuilder()
			        .setSubName(ignition.getSinglePointIgnitionOptions().getName());
			}
		}
		
		if (input.getLayerInfo().size() > 0) {
            input.setLayerInfo(input.getLayerInfo().stream().sorted(Comparator.comparingInt(x -> x.getIndex())).collect(Collectors.toList()));
    		for (LayerInfo info : input.getLayerInfo()) {
    		    ca.wise.project.proto.ProjectScenario.Reference.Builder b = builder.addFilterIndexBuilder()
    				.setName(info.getName());
    		    if (info.getOptions() != null) {
	    		    for (String s : info.getOptions().getSubNames()) {
	                    b.getFilterOptionsBuilder()
	                        .addSubName(s);
	    		    }
    		    }
    		}
		}
		
		for (AssetReference ref : input.getAssetFiles()) {
		    ca.wise.project.proto.ProjectScenario.Reference.Builder r = builder.addAssetIndexBuilder()
		        .setName(ref.getName());
		    if (ref.getOperation().value >= 0) {
		        ca.wise.project.proto.ProjectScenario.AssetOptions.Builder options = r.getAssetOptionsBuilder()
		                .setOperationValue(ref.getOperation().value);
		        if (ref.getCollisionCount() >= 0)
		            options.setCollisionCount(ProtoWrapper.ofInt(ref.getCollisionCount()));
		    }
		}
		
		if (input.getWindTargetFile() != null) {
		    builder.getWindTargetBuilder()
		            .setName(input.getWindTargetFile().getName())
		            .setGeometryIndex(input.getWindTargetFile().getGeometryIndex())
		            .setPointIndex(input.getWindTargetFile().getPointIndex());
		}
        
        if (input.getVectorTargetFile() != null) {
            builder.getVectorTargetBuilder()
                    .setName(input.getVectorTargetFile().getName())
                    .setGeometryIndex(input.getVectorTargetFile().getGeometryIndex())
                    .setPointIndex(input.getVectorTargetFile().getPointIndex());
        }
		
		if (input.getFgmOptions().getMaxAccTs() != null)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setMaxAccelTimestep(TimeSerializer.serializeTimeSpan(createDuration(input.getFgmOptions().getMaxAccTs())));

		if (input.getFgmOptions().getDistRes() >= 0)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setDistRes(HSS_Double.of(input.getFgmOptions().getDistRes()));

		if (input.getFgmOptions().getPerimRes() >= 0)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setPerimRes(HSS_Double.of(input.getFgmOptions().getPerimRes()));

		if (input.getFgmOptions().getMinimumSpreadingRos() != null)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setMinSpreadRos(HSS_Double.of(input.getFgmOptions().getMinimumSpreadingRos()));

		builder.getScenarioBuilder().getFgmOptionsBuilder().setStopAtGridEnd(ProtoWrapper.ofBool(input.getFgmOptions().isStopAtGridEnd()));

		builder.getScenarioBuilder().getFgmOptionsBuilder().setBreaching(ProtoWrapper.ofBool(input.getFgmOptions().isBreaching()));

		builder.getScenarioBuilder().getFgmOptionsBuilder().setDynamicSpatialThreshold(ProtoWrapper.ofBool(input.getFgmOptions().isDynamicSpatialThreshold()));

		builder.getScenarioBuilder().getFgmOptionsBuilder().setSpotting(ProtoWrapper.ofBool(input.getFgmOptions().isSpotting()));

		builder.getScenarioBuilder().getFgmOptionsBuilder().setPurgeNonDisplayable(ProtoWrapper.ofBool(input.getFgmOptions().isPurgeNonDisplayable()));

		if (input.getFgmOptions().getDx() != null && input.getFgmOptions().getDx() != 0)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setDx(HSS_Double.of(input.getFgmOptions().getDx()));

		if (input.getFgmOptions().getDy() != null && input.getFgmOptions().getDy() != 0)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setDy(HSS_Double.of(input.getFgmOptions().getDy()));

		if (input.getFgmOptions().getDt() != null && input.getFgmOptions().getDt().length() > 0)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setDt(TimeSerializer.serializeTimeSpan(createDuration(input.getFgmOptions().getDt())));
		
		if (input.getFgmOptions().getDwd() != null && input.getFgmOptions().getDwd() != 0)
		    builder.getScenarioBuilder().getFgmOptionsBuilder().setDWD(HSS_Double.of(input.getFgmOptions().getDwd()));
		
		if (input.getFgmOptions().getOwd() != null)
		    builder.getScenarioBuilder().getFgmOptionsBuilder().setOWD(HSS_Double.of(input.getFgmOptions().getOwd()));
        
        if (input.getFgmOptions().getDvd() != null)
            builder.getScenarioBuilder().getFgmOptionsBuilder().setDVD(HSS_Double.of(input.getFgmOptions().getDvd()));
        
        if (input.getFgmOptions().getOvd() != null)
            builder.getScenarioBuilder().getFgmOptionsBuilder().setOVD(HSS_Double.of(input.getFgmOptions().getOvd()));

		if (input.getFgmOptions().getGrowthPercentileApplied() != null)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setGrowthPercentileApplied(ProtoWrapper.ofBool(input.getFgmOptions().getGrowthPercentileApplied()));

		if (input.getFgmOptions().getGrowthPercentile() != null)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setGrowthPercentile(HSS_Double.of(input.getFgmOptions().getGrowthPercentile()));
		
		if (input.getFgmOptions().getSuppressTightConcave() != null)
		    builder.getScenarioBuilder().getFgmOptionsBuilder().setSuppressTightConcaveAdd(ProtoWrapper.ofBool(input.getFgmOptions().getSuppressTightConcave()));
		
		if (input.getFgmOptions().getNonFuelsAsVectorBreaks() != null)
		    builder.getScenarioBuilder().getFgmOptionsBuilder().setNonfuelsAsVectorBreaks(ProtoWrapper.ofBool(input.getFgmOptions().getNonFuelsAsVectorBreaks()));
		
		if (input.getFgmOptions().getNonFuelsToVectorBreaks() != null)
		    builder.getScenarioBuilder().getFgmOptionsBuilder().setNonfuelsToVectorBreaks(ProtoWrapper.ofBool(input.getFgmOptions().getNonFuelsToVectorBreaks()));
		
		builder.getScenarioBuilder().getFgmOptionsBuilder().setEnableFalseOrigin(ProtoWrapper.ofBool(input.getFgmOptions().isEnableFalseOrigin()));
		builder.getScenarioBuilder().getFgmOptionsBuilder().setEnableFalseScaling(ProtoWrapper.ofBool(input.getFgmOptions().isEnableFalseScaling()));
		
		if (input.getFgmOptions().getInitialVertexCount() != 16 && input.getFgmOptions().getInitialVertexCount() >= 4)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setInitialVertexCount(ProtoWrapper.ofUInt(input.getFgmOptions().getInitialVertexCount()));
		
		if (input.getFgmOptions().getIgnitionSize() > 0.25 && input.getFgmOptions().getIgnitionSize() != 0.5)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setIgnitionSize(HSS_Double.of(input.getFgmOptions().getIgnitionSize()));
		
		//if the user has specified a value for use independent timesteps add it to the scenario settings
		if (input.getFgmOptions().getUseIndependentTimesteps() != null)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setIndependentTimesteps(ProtoWrapper.ofBool(input.getFgmOptions().getUseIndependentTimesteps()));
		
		//if the user has specified a value for the perimeter spacing add it to the scenario settings
		if (input.getFgmOptions().getPerimeterSpacing() != null)
			builder.getScenarioBuilder().getFgmOptionsBuilder().setPerimSpacing(HSS_Double.of(input.getFgmOptions().getPerimeterSpacing()));

		if (input.getFbpOptions().getTerrainEffect() != null)
			builder.getScenarioBuilder().getFbpOptionsBuilder().setTerrainEffect(ProtoWrapper.ofBool(input.getFbpOptions().getTerrainEffect()));

		if (input.getFbpOptions().getWindEffect() != null)
			builder.getScenarioBuilder().getFbpOptionsBuilder().setWindEffect(ProtoWrapper.ofBool(input.getFbpOptions().getWindEffect()));

		if (input.getFmcOptions().getPerOverride() != null)
			builder.getScenarioBuilder().getFmcOptionsBuilder().setPerOverride(HSS_Double.of(input.getFmcOptions().getPerOverride()));
		else if (input.getFmcOptions().getPerOverride() != null && input.getFmcOptions().getPerOverride() == -2.0)
		    builder.getScenarioBuilder().getFmcOptionsBuilder().clearPerOverride();

		builder.getScenarioBuilder().getFmcOptionsBuilder().setNodataElev(HSS_Double.of(input.getFmcOptions().getNodataElev()));

		if (input.getFmcOptions().getTerrain() != null)
			builder.getScenarioBuilder().getFmcOptionsBuilder().setTerrain(ProtoWrapper.ofBool(input.getFmcOptions().getTerrain()));

		builder.getScenarioBuilder().getFmcOptionsBuilder().setAccurateLocation(ProtoWrapper.ofBool(true));

		if (input.getFwiOptions().getFwiSpacInterp() != null)
			builder.getScenarioBuilder().getFwiOptionsBuilder().setFwiSpacialInterp(ProtoWrapper.ofBool(input.getFwiOptions().getFwiSpacInterp()));

		if (input.getFwiOptions().getFwiFromSpacWeather() != null)
			builder.getScenarioBuilder().getFwiOptionsBuilder().setFwiFromSpacialWeather(ProtoWrapper.ofBool(input.getFwiOptions().getFwiFromSpacWeather()));

		if (input.getFwiOptions().getHistoryOnEffectedFWI() != null)
			builder.getScenarioBuilder().getFwiOptionsBuilder().setHistoryOnEffectedFwi(ProtoWrapper.ofBool(input.getFwiOptions().getHistoryOnEffectedFWI()));

		if (input.getFwiOptions().getBurningConditionsOn() != null)
			builder.getScenarioBuilder().getFwiOptionsBuilder().setBurningConditionsOn(ProtoWrapper.ofBool(input.getFwiOptions().getBurningConditionsOn()));

		if (input.getFwiOptions().getFwiTemporalInterp() != null)
			builder.getScenarioBuilder().getFwiOptionsBuilder().setFwiTemporalInterp(ProtoWrapper.ofBool(input.getFwiOptions().getFwiTemporalInterp()));
		
		if (input.getStopModellingOptions() != null) {
		    CwfgmScenario.StopModellingOptions.Builder options = builder.getScenarioBuilder().getStopOptionsBuilder();
		    if (input.getStopModellingOptions().getResponseTime() != null)
		        options.setResponseTime(TimeSerializer.serializeTimeSpan(createDuration(input.getStopModellingOptions().getResponseTime())));
            if (input.getStopModellingOptions().getFi90() != null) {
                options.getFI90PercentBuilder()
                    .setThreshold(HSS_Double.of(input.getStopModellingOptions().getFi90().getThreshold()))
                    .setDuration(TimeSerializer.serializeTimeSpan(createDuration(input.getStopModellingOptions().getFi90().getDuration())));
            }
		    if (input.getStopModellingOptions().getFi95() != null) {
		        options.getFI95PercentBuilder()
		            .setThreshold(HSS_Double.of(input.getStopModellingOptions().getFi95().getThreshold()))
		            .setDuration(TimeSerializer.serializeTimeSpan(createDuration(input.getStopModellingOptions().getFi95().getDuration())));
		    }
            if (input.getStopModellingOptions().getFi100() != null) {
                options.getFI100PercentBuilder()
                    .setThreshold(HSS_Double.of(input.getStopModellingOptions().getFi100().getThreshold()))
                    .setDuration(TimeSerializer.serializeTimeSpan(createDuration(input.getStopModellingOptions().getFi100().getDuration())));
            }
            if (input.getStopModellingOptions().getRh() != null) {
                options.getRHBuilder()
                    .setThreshold(HSS_Double.of(input.getStopModellingOptions().getRh().getThreshold()))
                    .setDuration(TimeSerializer.serializeTimeSpan(createDuration(input.getStopModellingOptions().getRh().getDuration())));
            }
            if (input.getStopModellingOptions().getPrecip() != null) {
                options.getPrecipBuilder()
                    .setThreshold(HSS_Double.of(input.getStopModellingOptions().getPrecip().getThreshold()))
                    .setDuration(TimeSerializer.serializeTimeSpan(createDuration(input.getStopModellingOptions().getPrecip().getDuration())));
            }
//            if (input.getStopModellingOptions().getArea() != null) {
//                options.getAreaBuilder()
//                    .setValue(input.getStopModellingOptions().getArea().getThreshold());
//            }
		}
        
        if (input.getGustingOptions() != null) {
            CwfgmScenario.GustingOptions.Builder options = builder.getScenarioBuilder().getGustingOptionsBuilder();
            options.setGustingValue(input.getGustingOptions().getGusting().value);
            if (input.getGustingOptions().getGustsPerHour() != null)
                options.setGustsPerHour(input.getGustingOptions().getGustsPerHour());
            if (input.getGustingOptions().getPercentGusting() != null)
                options.setPercentGusting(input.getGustingOptions().getPercentGusting());
            if (input.getGustingOptions().getGustBias() != null)
                options.setGustBiasValue(input.getGustingOptions().getGustBias().value);
        }
	}

	/**
	 * Create a scenario for the protobuf message.
	 * @param input The details of the scenario from the API.
	 * @param index The index of the scenario for looking up the number of cores it's allowed to use.
	 * @return A scenario.
	 */
	private ProjectScenario.Builder createScenario(Scenario input, int index) {
		ProjectScenario.Builder builder = ProjectScenario.newBuilder()
				.setVersion(1)
				.setName("Scenario");
		
		builder.getScenarioBuilder()
			.setVersion(2)
			.getFmcOptionsBuilder();
		
		populateScenario(builder, input, index);
		return builder;
	}

	/**
	 * Create a copy of an existing scenario for the protobuf message.
	 * @param input The details of the copied scenario from the API.
	 * @param index The index of the scenario for looking up the number of cores it's allowed to use.
	 * @return A scenario.
	 */
	private ProjectScenario.Builder createScenarioCopy(Scenario input, int index) {
		ProjectScenario.Builder builder = ProjectScenario.newBuilder()
				.setVersion(1)
				.setCopyName(ProtoWrapper.ofString(input.getScenToCopy()))
				.setName("Scenario");
		
		builder.getScenarioBuilder()
			.setVersion(2);
		
		populateScenario(builder, input, index);
		return builder;
	}

	/**
	 * Build the protobuf message from the job details received from the API.
	 * @param jobName The name of the job. Will also be used as the folder the job will be stored in.
	 * @return A writer that can export the job details to a file for Prometheus.
	 */
	@Override
	public IDataWriter combine(String jobName) {
		worldLocation = new WorldLocation();
		if (input.getInputs().getTimezone().isDst()) {
			worldLocation.setDSTAmount(new WTimeSpan(0, 1, 0, 0));
			worldLocation.setEndDST(new WTimeSpan(366, 0, 0, 0));
		}
		boolean found = false;
		if (input.getInputs().getTimezone().getValue() != null) {
		    TimeZoneInfo info = WorldLocation.getTimeZoneFromId(input.getInputs().getTimezone().getValue());
		    if (info != null) {
		        worldLocation.setTimezoneOffset(info);
		        found = true;
		    }
		}
		
		if (!found) {
			if (input.getInputs().getTimezone().getOffset() == null || input.getInputs().getTimezone().getOffset().length() == 0)
				worldLocation.setTimezoneOffset(new WTimeSpan(0));
			else
				worldLocation.setTimezoneOffset(new WTimeSpan(input.getInputs().getTimezone().getOffset()));
		}
		
		PrometheusData.Builder builder = PrometheusData.newBuilder();
		initParams(builder);
		initJob(builder, jobName);
		setGridFiles(builder);
		builder.getProjectBuilder().setLoadBalancing(LoadBalancingType.forNumber(input.getJobOptions().getLoadBalance().value));

		//add attachment files to the generated document
		if (options.singleFile && input.getAttachments().size() > 0) {
			for (ca.wise.api.FileAttachment att : input.getAttachments()) {
				String name = att.getFilename().substring(12);
				int index = name.indexOf("/");
				if (index >= 0 && index < (name.length() - 1))
					name = name.substring(index + 1);
				name = "Inputs/" + name;

				ByteString string;
				if (att.getData() == null)
				    string = ByteString.copyFrom(att.getContents().toString(), StandardCharsets.UTF_8);
				else
				    string = ByteString.copyFrom(att.getData());

				builder.addInputFilesBuilder()
					.setFilename(name)
					.setData(string);
			}
		}

		builder.getProjectBuilder().setComments(input.getComments());
		builder.getProjectBuilder().getVectorsBuilder().setVersion(1);
		for (FuelBreak fl : input.getInputs().getFiles().getFuelBreakFiles()) {
			builder.getProjectBuilder().getVectorsBuilder().addVectors(createFuelBreakFile(builder, fl));
		}
		builder.getProjectBuilder().getGridsBuilder().setVersion(1);
		for (FuelPatch fp : input.getInputs().getFiles().getFuelPatchFiles()) {
			builder.getProjectBuilder().getGridsBuilder().addFilters(createFuelPatchFile(builder, fp));
		}
		for (WeatherGrid fl : input.getInputs().getFiles().getWeatherGridFiles()) {
			builder.getProjectBuilder().getGridsBuilder().addFilters(createWeatherGrid(builder, fl));
		}
		for (WeatherPatch wp : input.getInputs().getFiles().getWeatherPatchFiles()) {
			builder.getProjectBuilder().getGridsBuilder().addFilters(createWeatherPatch(builder, wp));
		}
		for (GridFile fl : input.getInputs().getFiles().getGridFiles()) {
			builder.getProjectBuilder().getGridsBuilder().addFilters(createInputGridFile(builder, fl));
		}
		builder.getProjectBuilder().getStationsBuilder().setVersion(1);
		for (WeatherStation st : input.getInputs().getWeatherStations()) {
			builder.getProjectBuilder().getStationsBuilder().addStations(createWeatherStation(builder, st));
		}
		builder.getProjectBuilder().getIgnitionsBuilder().setVersion(1);
		for (Ignition ig : input.getInputs().getIgnitions()) {
			builder.getProjectBuilder().getIgnitionsBuilder().addIgnitions(createIgnition(builder, ig));
		}
		builder.getProjectBuilder().getAssetsBuilder().setVersion(1);
		for (AssetFile asset : input.getInputs().getAssetFiles()) {
		    builder.getProjectBuilder().getAssetsBuilder().addAssets(createAssetFile(builder, asset));
		}
		builder.getProjectBuilder().getTargetsBuilder().setVersion(1);
		for (TargetFile target : input.getInputs().getTargetFiles()) {
		    builder.getProjectBuilder().getTargetsBuilder().addTargets(createTargetFile(builder, target));
		}
		int index = 0;
		builder.getProjectBuilder().getScenariosBuilder().setVersion(1);
		for (Scenario sc : input.getInputs().getScenarios()) {
			if (Strings.isNullOrEmpty(sc.getScenToCopy())) {
				builder.getProjectBuilder().getScenariosBuilder().addScenarios(createScenario(sc, index));
				index++;
			}
		}
		for (Scenario sc : input.getInputs().getScenarios()) {
			if (!Strings.isNullOrEmpty(sc.getScenToCopy())) {
				builder.getProjectBuilder().getScenariosBuilder().addScenarios(createScenarioCopy(sc, index));
				index++;
			}
		}
        
        //if any of the units have been set output those settings to the FGM
        if (input.getExportUnits().anyValid()) {
            ca.wise.project.proto.PrometheusData.UnitExportSettings.Builder units = ca.wise.project.proto.PrometheusData.UnitExportSettings.newBuilder()
                    .setVersion(1);

            if (input.getExportUnits().getSmallMeasureOutput().value >= 0)
                units.setSmallDistanceOutputValue(input.getExportUnits().getSmallMeasureOutput().value);
            
            if (input.getExportUnits().getSmallDistanceOutput().value >= 0)
                units.setSmallDistanceOutputValue(input.getExportUnits().getSmallDistanceOutput().value);
            
            if (input.getExportUnits().getDistanceOutput().value >= 0)
                units.setDistanceOutputValue(input.getExportUnits().getDistanceOutput().value);
            
            if (input.getExportUnits().getAlternateDistanceOutput().value >= 0)
                units.setAlternateDistanceOutputValue(input.getExportUnits().getAlternateDistanceOutput().value);
            
            if (input.getExportUnits().getCoordinateOutput().value >= 0)
                units.setCoordinateOutputValue(input.getExportUnits().getCoordinateOutput().value);
            
            if (input.getExportUnits().getAreaOutput().value >= 0)
                units.setAreaOutputValue(input.getExportUnits().getAreaOutput().value);
            
            if (input.getExportUnits().getVolumeOutput().value >= 0)
                units.setVolumeOutputValue(input.getExportUnits().getVolumeOutput().value);
            
            if (input.getExportUnits().getTemperatureOutput().value >= 0)
                units.setTemperatureOutputValue(input.getExportUnits().getTemperatureOutput().value);
            
            if (input.getExportUnits().getMassOutput().value >= 0)
                units.setMassOutputValue(input.getExportUnits().getMassOutput().value);
            
            if (input.getExportUnits().getEnergyOutput().value >= 0)
                units.setEnergyOutputValue(input.getExportUnits().getEnergyOutput().value);
            
            if (input.getExportUnits().getAngleOutput().value >= 0)
                units.setAngleOutputValue(input.getExportUnits().getAngleOutput().value);
            
            if (input.getExportUnits().getVelocityOutput() != null &&
            		input.getExportUnits().getVelocityOutput().getDistance() != null &&
            		input.getExportUnits().getVelocityOutput().getTime() != null &&
            		input.getExportUnits().getVelocityOutput().getDistance().value >= 0 &&
            		input.getExportUnits().getVelocityOutput().getTime().value >= 0)
                units.getVelocityOutputBuilder()
                    .setDistanceValue(input.getExportUnits().getVelocityOutput().getDistance().value)
                    .setTimeValue(input.getExportUnits().getVelocityOutput().getTime().value);
            
            if (input.getExportUnits().getAlternateVelocityOutput() != null &&
            		input.getExportUnits().getAlternateVelocityOutput().getDistance() != null &&
            		input.getExportUnits().getAlternateVelocityOutput().getTime() != null &&
            		input.getExportUnits().getAlternateVelocityOutput().getDistance().value >= 0 &&
            		input.getExportUnits().getAlternateVelocityOutput().getTime().value >= 0)
                units.getAlternateVelocityOutputBuilder()
                    .setDistanceValue(input.getExportUnits().getAlternateVelocityOutput().getDistance().value)
                    .setTimeValue(input.getExportUnits().getAlternateVelocityOutput().getTime().value);
            
            if (input.getExportUnits().getIntensityOutput() != null &&
            		input.getExportUnits().getIntensityOutput().getEnergy() != null &&
            		input.getExportUnits().getIntensityOutput().getDistance() != null &&
            		input.getExportUnits().getIntensityOutput().getEnergy().value >= 0 &&
            		input.getExportUnits().getIntensityOutput().getDistance().value >= 0)
                units.getIntensityOutputBuilder()
                    .setEnergyValue(input.getExportUnits().getIntensityOutput().getEnergy().value)
                    .setDistanceValue(input.getExportUnits().getIntensityOutput().getDistance().value);
            
            if (input.getExportUnits().getMassAreaOutput() != null &&
            		input.getExportUnits().getMassAreaOutput().getMass() != null &&
            		input.getExportUnits().getMassAreaOutput().getArea() != null &&
            		input.getExportUnits().getMassAreaOutput().getMass().value >= 0 &&
            		input.getExportUnits().getMassAreaOutput().getArea().value >= 0)
                units.getMassAreaOutputBuilder()
                    .setMassValue(input.getExportUnits().getMassAreaOutput().getMass().value)
                    .setAreaValue(input.getExportUnits().getMassAreaOutput().getArea().value);
            
            builder.setUnitSettings(units);
        }
		
		LocalDateTime local = LocalDateTime.now();
		WTime now = new WTime(local.getYear(), local.getMonthValue(), local.getDayOfMonth(),
				local.getHour(), local.getMinute(), local.getSecond(), new WTimeManager(worldLocation));
		builder.getProjectBuilder().setProjectStartTime(TimeSerializer.serializeTime(now));
		builder.getProjectBuilder().setTimeZoneSettings(TimeSerializer.serializeTimezone(worldLocation));

		for (VectorFile vf : input.getOutputs().getVectorFiles()) {
			builder.getProjectBuilder().getOutputsBuilder().addVectors(createVectorFile(vf));
		}
		for (SummaryFile sf : input.getOutputs().getSummaryFiles()) {
			builder.getProjectBuilder().getOutputsBuilder().addSummaries(createSummaryFile(sf));
		}
		for (ca.wise.api.output.GridFile gf : input.getOutputs().getGridFiles()) {
			builder.getProjectBuilder().getOutputsBuilder().addGrids(createGridFile(gf));
		}
        for (FuelGridFile fgf : input.getOutputs().getFuelGridFiles()) {
            builder.getProjectBuilder().getOutputsBuilder().addFuelgrids(createFuelGridFile(fgf));
        }
		for (StatsFile sf : input.getOutputs().getStatsFiles()) {
		    builder.getProjectBuilder().getOutputsBuilder().addStats(createStatsFile(sf));
		}
		for (AssetStatsFile sf : input.getOutputs().getAssetStatsFiles()) {
		    builder.getProjectBuilder().getOutputsBuilder().addAssetStats(createAssetStatsFile(sf));
		}
		
		if (input.getTimestepSettings().getStatistics().size() > 0)
		    builder.getProjectBuilder().getTimestepSettingsBuilder().setVersion(1);
		for (GlobalStatistics i : input.getTimestepSettings().getStatistics()) {
            builder.getProjectBuilder().getTimestepSettingsBuilder().addMessageGlobalOutputsValue(i.value);
		}
		if (input.getTimestepSettings().getDiscretize() != null) {
		    builder.getProjectBuilder()
		        .getTimestepSettingsBuilder()
		            .getDiscretizedOptionsBuilder()
		                .setVersion(1)
		                .setDiscretize(input.getTimestepSettings().getDiscretize());
		}
		
		return new IPBCombiner.PBDataWriter(baseDir, builder.build(), options, input.getJobOptions().getPriority(), input.getJobOptions().isValidate());
	}
}
