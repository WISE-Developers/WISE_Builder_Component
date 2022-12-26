package ca.wise.forecast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import ca.hss.general.DecimalUtils;
import ca.hss.general.DecimalUtils.DataType;
import ca.wise.fgm.output.Message;
import ca.wise.fgm.tools.WISELogger;
import ca.hss.text.StringExtensions;
import ca.hss.times.TimeZoneInfo;
import ca.hss.times.WorldLocation;
import ca.weather.acheron.Calculator;
import ca.weather.acheron.LocationWeather;
import ca.weather.acheron.Calculator.LocationSmall;
import ca.weather.acheron.Day;
import ca.weather.acheron.Hour;
import ca.weather.forecast.Model;
import ca.weather.forecast.Time;

public class ForecastCalculator {
	private static List<String> canadianCities = null;
	private static List<LocationSmall> locationData = null;
	private static Object locker = new Object();
	
	public boolean isForecast(Message m) {
		if (m == Message.FORECAST_LIST_CITIES) {
			return true;
		}
		else if (m == Message.FORECAST_GET) {
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public String handle(Message m, String data) {
		String retval = "";
		if (m == Message.FORECAST_LIST_CITIES) {
			List<String> cities = getCities(data);
			for (int i = 0; i < cities.size(); i++) {
				retval += cities.get(i);
				if (i < (cities.size() - 1))
					retval += "|";
			}
		}
		else if (m == Message.FORECAST_GET) {
			if (canadianCities == null) {
				locationData = Calculator.getLocations();
				filterForCanadianCities(locationData);
			}
			String[] list = data.split("\\|");
			if (list.length < 9)
				return "COMPLETE";
			String city = list[1].toUpperCase();
			Pattern cityPattern = Pattern.compile(city);
			for (LocationSmall loc : locationData) {
				if (cityPattern.matcher(loc.locationName).find()) {
					city = loc.locationName;
					break;
				}
			}
			Calculator calculator = new Calculator();
			calculator.setLocation(city);
			Model model = Model.fromString(list[2]);
			calculator.setModel(model);
			calculator.clearMembers();
			calculator.setIgnorePrecipitation(false);
			if (model == Model.CUSTOM) {
				String[] split = list[3].split(",");
				for (String s : split) {
					try {
						int i = Integer.parseInt(s);
						calculator.addMember(i);
					}
					catch (NumberFormatException ex) {
						WISELogger.warn("Error parsing member ID " + s);
					}
				}
			}
			TimeZoneInfo info = null;
			try {
				int value = Integer.parseInt(list[5]);
				TimeZoneInfo[] tzlist = WorldLocation.getList();
				for (int i = 0; i < tzlist.length; i++) {
					if (i == value) {
						info = tzlist[i];
						break;
					}
				}
			}
			catch (NumberFormatException e) {
				WISELogger.warn("Invalid timezone " + list[5]);
			}
			if (info == null)
			WISELogger.warn("Unable to find specified timezone " + list[5]);
			else
				calculator.setTimezone(info);
			SimpleDateFormat format = new SimpleDateFormat("y-M-d");
			Date dt = new Date();
			try {
				dt = format.parse(list[4]);
			}
			catch (ParseException ex) {
				WISELogger.warn("Invalid date object " + list[4]);
			}
			dt.setHours(0);
			dt.setMinutes(0);
			dt.setSeconds(0);
			Calendar cal = Calendar.getInstance();
			cal.setTime(dt);
			calculator.setDate(cal);
			Time time = Time.fromString(list[6]);
			calculator.setTime(time);
			int percentile = 50;
			if (model != Model.GEM_DETER) {
				try {
					percentile = Integer.parseInt(list[7]);
				}
				catch (NumberFormatException ex) {
					WISELogger.warn("Unable to parse percentile " + list[7]);
				}
			}
			calculator.setPercentile(percentile);
			calculator.calculate();
			boolean useDay = false;
			if (list[8].equals("1") || list[8].toLowerCase().equals("day"))
				useDay = true;
			LocationWeather weather = calculator.getLocationsWeatherData(0);
			SimpleDateFormat formatter = new SimpleDateFormat("MMM dd yyyy HH:mm");
			if (!useDay) {
				List<Hour> hours = weather.getHourData();
				for (Hour hour : hours) {
					String val = formatter.format(hour.getCalendarDate().getTime()) + "|";
					val += DecimalUtils.format(hour.getTemperature(), DataType.TEMPERATURE) + "|";
					val += DecimalUtils.format(hour.getRelativeHumidity(), DataType.RH) + "|";
					val += DecimalUtils.format(hour.getPrecipitation(), DataType.PRECIP) + "|";
					val += DecimalUtils.format(hour.getWindSpeed(), DataType.WIND_SPEED) + "|";
					val += DecimalUtils.format(hour.getWindDirection(), DataType.WIND_DIR);
					retval += val + "|";
				}
			}
			else {
				List<Day> days = weather.getDayData();
				for (Day day : days) {
					String val = day.getDate() + "|";
					val += DecimalUtils.format(day.getMinTemperature(), DataType.TEMPERATURE) + "|";
					val += DecimalUtils.format(day.getMaxTemperature(), DataType.TEMPERATURE) + "|";
					val += DecimalUtils.format(day.getRelativeHumidity(), DataType.RH) + "|";
					val += DecimalUtils.format(day.getPrecipitation(), DataType.PRECIP) + "|";
					val += DecimalUtils.format(day.getMinWindSpeed(), DataType.WIND_SPEED) + "|";
					val += DecimalUtils.format(day.getMaxWindSpeed(), DataType.WIND_SPEED) + "|";
					val += DecimalUtils.format(day.getWindDirection(), DataType.WIND_DIR);
					retval += val + "|";
				}
			}
			if (retval.endsWith("|"))
				retval = retval.substring(0, retval.length() - 1);
			retval += "\r\nCOMPLETE";
		}
		return retval;
	}
	
	public List<String> getCities(String province) {
		switch (province.toLowerCase()) {
		case "alberta":
		case "ab":
			return getCitiesFromProvince("AB");
		case "ontario":
		case "on":
			return getCitiesFromProvince("ON");
		case "british columbia":
		case "bc":
			return getCitiesFromProvince("BC");
		case "mb":
		case "manitoba":
			return getCitiesFromProvince("MB");
		case "new brunswick":
		case "nb":
			return getCitiesFromProvince("NB");
		case "newfoundland and labrador":
		case "nl":
			return getCitiesFromProvince("NL");
		case "northwest territories":
		case "nt":
			return getCitiesFromProvince("NT");
		case "nova scotia":
		case "ns":
			return getCitiesFromProvince("NS");
		case "nunavut":
		case "nu":
			return getCitiesFromProvince("NU");
		case "prince edward island":
		case "pe":
			return getCitiesFromProvince("PE");
		case "quebec":
		case "qc":
			return getCitiesFromProvince("QC");
		case "saskatchewan":
		case "sk":
			return getCitiesFromProvince("SK");
		case "yukon":
		case "yt":
			return getCitiesFromProvince("YT");
		default:
			return new ArrayList<>();
		}
	}

	private List<String> getCitiesFromProvince(String prov) {
		synchronized (locker) {
			if (canadianCities == null) {
				locationData = Calculator.getLocations();
				filterForCanadianCitiesUnsafe(locationData);
			}

			ArrayList<String> list = new ArrayList<String>();
			Iterator<String> it = canadianCities.iterator();
			while (it.hasNext()) {
				String place = it.next();
				String provinceID = place.substring(place.length() - 2,
						place.length());
				if (provinceID.compareToIgnoreCase(prov) == 0)
					list.add(place.substring(0, place.length() - 3));
			}
			return list;
		}
	}

	private void filterForCanadianCities(List<LocationSmall> locationData) {
		synchronized (locker) {
			filterForCanadianCitiesUnsafe(locationData);
		}
	}
	
	private void filterForCanadianCitiesUnsafe(List<LocationSmall> locationData) {
		// discard US and Mexico data for now
		if (locationData == null || canadianCities != null)
			return;
		canadianCities = new ArrayList<>();
		Iterator<LocationSmall> it = locationData.iterator();
		while (it.hasNext()) {
			LocationSmall place = it.next();
			int charMarker = place.locationName.length();
			char[] placeChar = place.locationName.toCharArray();
			if (placeChar[--charMarker] == 'A'
					|| placeChar[--charMarker] == 'a') {
				if (placeChar[--charMarker] == 'C'
						|| placeChar[--charMarker] == 'c') {
					canadianCities.add(StringExtensions
							.capitalizeFully(place.locationName.substring(0,
									--charMarker)));
				}
			}
		}
	}
}
