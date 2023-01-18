package ca.wise.fwi;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import ca.wise.fgm.output.Message;

public class FwiCalculator {
	private Duration CreateDuration(String input) {
		Duration retval = null;
		try {
			retval = DatatypeFactory.newInstance().newDuration(input);
		} catch (DatatypeConfigurationException e) {
		}
		return retval;
	}

	private long DurationToSeconds(Duration dur) {
		return (dur.getDays() * 86400) + (dur.getHours() * 3600) + (dur.getMinutes() * 60) + dur.getSeconds();
	}

	public boolean isFwi(Message m) {
		if (m == Message.HOURLY_FFMC_VAN_WAGNER) {
			return true;
		}
		else if (m == Message.HOURLY_FFMC_LAWSON) {
			return true;
		}
		else if (m == Message.HOURLY_FFMC_VAN_WAGNER_PREVIOUS) {
			return true;
		}
		else if (m == Message.HOURLY_FFMC_LAWSON_CONTIGUOUS) {
			return true;
		}
		else if (m == Message.DAILY_FFMC_VAN_WAGNER) {
			return true;
		}
		else if (m == Message.DMC) {
			return true;
		}
		else if (m == Message.DC) {
			return true;
		}
		else if (m == Message.FF) {
			return true;
		}
		else if (m == Message.ISI_FWI) {
			return true;
		}
		else if (m == Message.ISI_FBP) {
			return true;
		}
		else if (m == Message.BUI) {
			return true;
		}
		else if (m == Message.FWI) {
			return true;
		}
		else if (m == Message.DSR) {
			return true;
		}
		return false;
	}

	public double calculate(Message m, String data) {
		switch (m) {
		case HOURLY_FFMC_VAN_WAGNER:
			return hourlyFFMCVanWagner(data);
		case HOURLY_FFMC_LAWSON:
			return hourlyFFMCLawson(data);
		case HOURLY_FFMC_VAN_WAGNER_PREVIOUS:
			return hourlyFFMCVanWagnerPrevious(data);
		case HOURLY_FFMC_LAWSON_CONTIGUOUS:
			return hourlyFFMCLawsonContiguous(data);
		case DAILY_FFMC_VAN_WAGNER:
			return dailyFFMCVanWagner(data);
		case DMC:
			return dMC(data);
		case DC:
			return dC(data);
		case FF:
			return ff(data);
		case ISI_FWI:
			return isiFWI(data);
		case ISI_FBP:
			return isiFBP(data);
		case BUI:
			return bui(data);
		case FWI:
			return fwi(data);
		case DSR:
			return dsr(data);
		default:
			return -9999.0;
		}
	}

	private double hourlyFFMCVanWagner(String data) {
		String list[] = data.split("\\|");
		if (list.length != 6)
			return -9999.0;
		try {
			double inFFMC = Double.parseDouble(list[0]);
			double rain = Double.parseDouble(list[1]);
			double temperature = Double.parseDouble(list[2]);
			double rh = Double.parseDouble(list[3]);
			double ws = Double.parseDouble(list[4]);
			Duration dur = CreateDuration(list[5]);
			return Fwi.hourlyFFMCVanWagner(inFFMC, rain, temperature, rh, ws, DurationToSeconds(dur));
		}
		catch (NumberFormatException ex) {
		}
		return -9999.0;
	}

	public double hourlyFFMCLawson(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 4)
			return -9999.0;
		try {
			double prevFFMC = Double.parseDouble(list[0]);
			double currFFMC = Double.parseDouble(list[1]);
			double rh = Double.parseDouble(list[2]);
			Duration dur = CreateDuration(list[3]);
			retval = Fwi.hourlyFFMCLawson(prevFFMC, currFFMC, rh, DurationToSeconds(dur));
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double hourlyFFMCVanWagnerPrevious(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 5)
			return retval;
		try {
			double currFFMC = Double.parseDouble(list[0]);
			double rain = Double.parseDouble(list[1]);
			double temperature = Double.parseDouble(list[2]);
			double rh = Double.parseDouble(list[3]);
			double ws = Double.parseDouble(list[4]);
			retval = Fwi.hourlyFFMCVanWagnerPrevious(currFFMC, rain, temperature, rh, ws);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double hourlyFFMCLawsonContiguous(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 6)
			return retval;
		try {
			double prevFFMC = Double.parseDouble(list[0]);
			double currFFMC = Double.parseDouble(list[1]);
			double rh0 = Double.parseDouble(list[2]);
			double rh = Double.parseDouble(list[3]);
			double rh1 = Double.parseDouble(list[4]);
			Duration dur = CreateDuration(list[5]);
			retval = Fwi.hourlyFFMCLawsonContiguous(prevFFMC, currFFMC, rh0, rh, rh1, DurationToSeconds(dur));
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double dailyFFMCVanWagner(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 5)
			return retval;
		try {
			double inFFMC = Double.parseDouble(list[0]);
			double rain = Double.parseDouble(list[1]);
			double temperature = Double.parseDouble(list[2]);
			double rh = Double.parseDouble(list[3]);
			double ws = Double.parseDouble(list[4]);
			retval = Fwi.dailyFFMCVanWagner(inFFMC, rain, temperature, rh, ws);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double dMC(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 7)
			return retval;
		try {
			double inDMC = Double.parseDouble(list[0]);
			double rain = Double.parseDouble(list[1]);
			double temperature = Double.parseDouble(list[2]);
			double latitude = Double.parseDouble(list[3]);
			double longitude = Double.parseDouble(list[4]);
			int month = Integer.parseInt(list[5]);
			double rh = Double.parseDouble(list[6]);
			retval = Fwi.dMC(inDMC, rain, temperature, latitude, longitude, month, rh);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double dC(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 6)
			return retval;
		try {
			double inDC = Double.parseDouble(list[0]);
			double rain = Double.parseDouble(list[1]);
			double temperature = Double.parseDouble(list[2]);
			double latitude = Double.parseDouble(list[3]);
			double longitude = Double.parseDouble(list[4]);
			int month = Integer.parseInt(list[5]);
			retval = Fwi.dC(inDC, rain, temperature, latitude, longitude, month);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double ff(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 2)
			return retval;
		try {
			double ffmc = Double.parseDouble(list[0]);
			double seconds = Double.parseDouble(list[1]);
			retval = Fwi.ff(ffmc, seconds);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double isiFWI(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 3)
			return retval;
		try {
			double ffmc = Double.parseDouble(list[0]);
			double ws = Double.parseDouble(list[1]);
			Duration seconds = CreateDuration(list[2]);
			retval = Fwi.isiFWI(ffmc, ws, DurationToSeconds(seconds));
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double isiFBP(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 3)
			return retval;
		try {
			double ffmc = Double.parseDouble(list[0]);
			double ws = Double.parseDouble(list[1]);
			Duration seconds = CreateDuration(list[2]);
			retval = Fwi.isiFBP(ffmc, ws, DurationToSeconds(seconds));
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double bui(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 2)
			return retval;
		try {
			double dc = Double.parseDouble(list[0]);
			double dmc = Double.parseDouble(list[1]);
			retval = Fwi.bui(dc, dmc);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double fwi(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 2)
			return retval;
		try {
			double isi = Double.parseDouble(list[0]);
			double bui = Double.parseDouble(list[1]);
			retval = Fwi.fwi(isi, bui);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}

	public double dsr(String data) {
		String list[] = data.split("\\|");
		double retval = -9999.0;
		if (list.length != 1)
			return retval;
		try {
			double fwi = Double.parseDouble(list[0]);
			retval = Fwi.dsr(fwi);
		}
		catch (NumberFormatException ex) {
		}
		return retval;
	}
}
