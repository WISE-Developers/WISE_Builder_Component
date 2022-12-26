package ca.wise.solar;

import ca.hss.general.OutVariable;
import ca.wise.fgm.output.Message;
import ca.wise.fgm.tools.WISELogger;
import ca.hss.times.TimeZoneInfo;
import ca.hss.times.WTime;
import ca.hss.times.WTimeManager;
import ca.hss.times.WTimeSpan;
import ca.hss.times.WorldLocation;

public class SolarCalculator {
	
	public static boolean canHandle(Message m) {
		if (m == Message.SOLAR_CALCULATOR) {
			return true;
		}
		return false;
	}

	public static String calculateSunrise(Message m, String data) {
		String retval = "";
		String[] split = data.split("\\|");
		double lat;
		double lon;
		int year;
		int month;
		int day;
		try {
			lat = Double.parseDouble(split[0]);
			lon = Double.parseDouble(split[1]);
			year = Integer.parseInt(split[3]);
			month = Integer.parseInt(split[4]);
			day = Integer.parseInt(split[5]);
		}
		catch (NumberFormatException ex) {
			WISELogger.warn("Number format exception: Failed to parse numbers in solar calculator (" + data + ")");
			return "COMPLETE";
		}
		
		WorldLocation loc = new WorldLocation();
		loc.setLatitude(ca.hss.math.General.DEGREE_TO_RADIAN(lat));
		loc.setLongitude(ca.hss.math.General.DEGREE_TO_RADIAN(lon));

		TimeZoneInfo info = null;
		try {
			int value = Integer.parseInt(split[2]);
			TimeZoneInfo[] tzlist = WorldLocation.getList();
			for (int i = 0; i < tzlist.length; i++) {
				if (i == value) {
					info = tzlist[i];
					break;
				}
			}
		}
		catch (NumberFormatException e) {
			WISELogger.warn("Invalid timezone " + split[2]);
		}
		if (info == null) {
			WISELogger.warn("Unable to find specified timezone " + split[2]);
			return "COMPLETE";
		}
		else {
			loc.setTimezoneOffset(info.getTimezoneOffset());
			loc.setStartDST(new WTimeSpan(0));
			if (info.getDSTAmount().getTotalSeconds() > 0)
				loc.setEndDST(new WTimeSpan(366, 0, 0, 0));
			else
				loc.setEndDST(new WTimeSpan(0));
			loc.setDSTAmount(info.getDSTAmount());
		}
		
		WTime time = WTime.fromLocal(year, month, day, 12, 0, 0, new WTimeManager(loc));
		OutVariable<WTime> rise = new OutVariable<>();
		rise.value = new WTime(time);
		OutVariable<WTime> set = new OutVariable<>();
		set.value = new WTime(time);
		OutVariable<WTime> noon = new OutVariable<>();
		noon.value = new WTime(time);
		loc.getSunRiseSetNoon(time, rise, set, noon);
		retval = rise.value.toString(WTime.FORMAT_AS_LOCAL | WTime.FORMAT_WITHDST | WTime.FORMAT_DATE | WTime.FORMAT_TIME);
		retval += "|";
		retval += set.value.toString(WTime.FORMAT_AS_LOCAL | WTime.FORMAT_WITHDST | WTime.FORMAT_DATE | WTime.FORMAT_TIME);
		retval += "|";
		retval += noon.value.toString(WTime.FORMAT_AS_LOCAL | WTime.FORMAT_WITHDST | WTime.FORMAT_DATE | WTime.FORMAT_TIME);
		retval += "\r\nCOMPLETE";
		return retval;
	}
}
