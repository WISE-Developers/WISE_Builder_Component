package ca.wise.fbp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ca.hss.general.DecimalUtils;
import ca.wise.fgm.output.Message;
import ca.wise.fgm.tools.WISELogger;

public class FbpCalculator {

	public String calculate(Message m, String data) {
		switch (m) {
		case FBP_GET_FUELS:
			return getFuels();
		case FBP_GET_FUELS_V2:
			return getFuelsV2();
		case FBP_CALCULATE:
			return calculate(data);
		default:
			break;
		}
		return "";
	}
	
	private String calculate(String data) {
		String list[] = data.split("\\|");
		WISELogger.info("FBP calculate " + data);
		if (list.length != 22) {
			WISELogger.warn("FBP calculate should have 22 parameters, received " + list.length + ". String: " + data);
			return "ERROR: incorrect number of parameters";
		}
		String fuelType = list[0];
		double crownBase = Double.valueOf(list[1]);
		double percentConifer = Double.valueOf(list[2]);
		double percentDeadFir = Double.valueOf(list[3]);
		double grassCuring = Double.valueOf(list[4]);
		double grassFuelLoad = Double.valueOf(list[5]);
		double ffmc = Double.valueOf(list[6]);
		double dmc = Double.valueOf(list[7]);
		double dc = Double.valueOf(list[8]);
		double bui = Double.valueOf(list[9]);
		boolean useBui = Boolean.valueOf(list[10]);
		double windSpeed = Double.valueOf(list[11]);
		double windDirection = Double.valueOf(list[12]);
		double elevation = Double.valueOf(list[13]);
		double slopeValue = Double.valueOf(list[14]);
		boolean useSlope = Boolean.valueOf(list[15]);
		double aspect = Double.valueOf(list[16]);
		boolean useLine = Boolean.valueOf(list[17]);
		String startTime = list[18];
		double elapsedTime = Double.valueOf(list[19]);
		double latitude = Double.valueOf(list[20]);
		double longitude = Double.valueOf(list[21]);

		FBPCalculations fbpCalculations = new FBPCalculations();
		fbpCalculations.latitude = latitude;
		fbpCalculations.longitude = longitude;
		fbpCalculations.elevation = elevation;
		if (useSlope) {
			fbpCalculations.useSlope = true;
			fbpCalculations.slopeValue = slopeValue;
			fbpCalculations.aspect = aspect;
		}
		else {
			fbpCalculations.useSlope = false;
		}
		fbpCalculations.ffmc = ffmc;
		fbpCalculations.useBui = useBui;
		if (fbpCalculations.useBui) {
			fbpCalculations.bui = bui;
		}
		else {
			fbpCalculations.dmc = dmc;
			fbpCalculations.dc = dc;
		}
		fbpCalculations.windSpeed = windSpeed;
		fbpCalculations.windDirection = windDirection;
		if (fuelType.startsWith("M-1") || fuelType.startsWith("M-2")) {
			fbpCalculations.conifMixedWood = percentConifer;
		}
		else if (fuelType.startsWith("M-3") || fuelType.startsWith("M-4")) {
			fbpCalculations.deadBalsam = percentDeadFir;
		}
		else if (fuelType.startsWith("O-1a") || fuelType.startsWith("O-1b")) {
			fbpCalculations.grassCuring = grassCuring;
			fbpCalculations.grassFuelLoad = grassFuelLoad;
		}
		else if (fuelType.startsWith("C-6")) {
			fbpCalculations.crownBase = crownBase;
		}
		fbpCalculations.elapsedTime = elapsedTime;
		fbpCalculations.acceleration = useLine;
		fbpCalculations.useBuildup = true;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		Date dt;
		try {
			dt = format.parse(startTime);
		}
		catch (ParseException e) {
			WISELogger.warn("Error: Invalid time string (" + startTime + "). Expected format yyyy-MM-ddTHH:mm");
			return "Error: invalid date format";
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		fbpCalculations.m_date.set(Calendar.YEAR, cal.get(Calendar.YEAR));
		fbpCalculations.m_date.set(Calendar.MONTH, cal.get(Calendar.MONTH));
		fbpCalculations.m_date.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH));
		fbpCalculations.m_date.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
		fbpCalculations.m_date.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
		fbpCalculations.m_date.set(Calendar.SECOND, cal.get(Calendar.SECOND));
		try {
			fbpCalculations.FBPCalculateStatisticsCOM();
		}
		catch (CloneNotSupportedException e) {
			return "Internal Error";
		}
		catch (Exception e) {
			return "Error calculating (" + e.getMessage() + ").";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(DecimalUtils.format(fbpCalculations.ros_t));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.ros_eq));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.fros));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.lb));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.bros));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.rso));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.hfi));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.ffi));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.bfi));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.area));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.perimeter));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.distanceHead));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.distanceBack));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.distanceFlank));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.csi));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.cfb));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.sfc));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.tfc));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.cfc));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.isi, DecimalUtils.DataType.ISI));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.fmc, DecimalUtils.DataType.FFMC));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.wsv));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.raz));
		builder.append("|");
		builder.append(fbpCalculations.fireDescription.replace("\n", "^").replace("\r", ""));
		builder.append("|");
		builder.append(DecimalUtils.format(fbpCalculations.bui));
		return builder.toString();
	}
	
	private String getFuels() {
		return "C-1|Spruce-Lichen Woodland|" +
			   "C-2|Boreal Spruce|" +
			   "C-3|Mature Jack or Lodgepole Pine|" +
			   "C-4|Immature Jack or Lodgepole Pine|" +
			   "C-5|Red and White Pine|" +
			   "C-6|Conifer Plantation|" +
			   "C-7|Ponderosa Pine / Douglas Fir|" +
			   "D-1|Leafless Aspen|" +
			   "D-2|Green Aspen (w/ BUI Thresholding)|" +
			   "M-1|Boreal Mixedwood - Leafless|" +
			   "M-2|Boreal Mixedwood - Green|" +
			   "M-3|Dead Balsam Fir / Mixedwood - Leafless|" +
			   "M-4|Dead Balsam Fir / Mixedwood - Green|" +
			   "O-1a|Matted Grass|" +
			   "O-1b|Standing Grass|" +
			   "S-1|Jack or Lodgepole Pine Slash|" +
			   "S-2|White Spruce / Balsam Slash|" +
			   "S-3|Coastal Cedar / Hemlock / Douglas-Fir Slash";
	}
	
	private String getFuelsV2() {
		return "C-1|Spruce-Lichen Woodland||||||" + 0b0 + "|" +
				   "C-2|Boreal Spruce||||||" + 0b0 + "|" +
				   "C-3|Mature Jack or Lodgepole Pine||||||" + 0b0 + "|" +
				   "C-4|Immature Jack or Lodgepole Pine||||||" + 0b0 + "|" +
				   "C-5|Red and White Pine||||||" + 0b0 + "|" +
				   "C-6|Conifer Plantation|7|||||" + 0b1 + "|" +
				   "C-7|Ponderosa Pine / Douglas Fir||||||" + 0b0 + "|" +
				   "D-1|Leafless Aspen||||||" + 0b0 + "|" +
				   "D-2|Green Aspen (w/ BUI Thresholding)||||||" + 0b0 + "|" +
				   "M-1|Boreal Mixedwood - Leafless||50||||" + 0b10 + "|" +
				   "M-2|Boreal Mixedwood - Green||50||||" + 0b10 + "|" +
				   "M-3|Dead Balsam Fir / Mixedwood - Leafless|||50|||" + 0b100 + "|" +
				   "M-4|Dead Balsam Fir / Mixedwood - Green|||50|||" + 0b100 + "|" +
				   "O-1a|Matted Grass||||60|0.35|" + 0b11000 + "|" +
				   "O-1b|Standing Grass||||60|0.35|" + 0b11000 + "|" +
				   "S-1|Jack or Lodgepole Pine Slash||||||" + 0b0 + "|" +
				   "S-2|White Spruce / Balsam Slash||||||" + 0b0 + "|" +
				   "S-3|Coastal Cedar / Hemlock / Douglas-Fir Slash||||||" + 0b0;
	}

	public boolean isFbp(Message m) {
		if (m == Message.FBP_GET_FUELS)
			return true;
		if (m == Message.FBP_GET_FUELS_V2)
			return true;
		if (m == Message.FBP_CALCULATE)
			return true;
		return false;
	}
}
