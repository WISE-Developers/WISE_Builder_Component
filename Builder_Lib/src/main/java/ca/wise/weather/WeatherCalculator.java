package ca.wise.weather;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ca.hss.general.DecimalUtils;
import ca.wise.fgm.output.Message;
import ca.weather.current.CurrentWeather;
import ca.weather.current.Cities.AlbertaCities;
import ca.weather.current.Cities.BritishColumbiaCities;
import ca.weather.current.Cities.Cities;
import ca.weather.current.Cities.CitiesHelper;
import ca.weather.current.Cities.ManitobaCities;
import ca.weather.current.Cities.NewBrunswickCities;
import ca.weather.current.Cities.NewfoundLandLabradorCities;
import ca.weather.current.Cities.NorthwestTerritoriesCities;
import ca.weather.current.Cities.NovaScotiaCities;
import ca.weather.current.Cities.NunavutCities;
import ca.weather.current.Cities.OntarioCities;
import ca.weather.current.Cities.PrinceEdwardIslandCities;
import ca.weather.current.Cities.QuebecCities;
import ca.weather.current.Cities.SaskatchewanCities;
import ca.weather.current.Cities.YukonTerritoryCities;
import ca.weather.forecast.InvalidXMLException;
import ca.weather.forecast.Province;

public class WeatherCalculator {
	
	public boolean isWeather(Message m) {
		if (m == Message.WEATHER_LIST_CITIES) {
			return true;
		}
		else if (m == Message.WEATHER_GET) {
			return true;
		}
		return false;
	}

	public String handle(Message m, String data) {
		String retval = "";
		if (m == Message.WEATHER_LIST_CITIES) {
			List<String> cities = getListOfCitiesObservedWx(data);
			for (int i = 0; i < cities.size(); i++) {
				retval += cities.get(i);
				if (i < (cities.size() - 1))
					retval += "|";
			}
		}
		else if (m == Message.WEATHER_GET) {
			String[] list = data.split("\\|");
			if (list.length < 2)
				return "COMPLETE";
			String province = list[0];
			String dropBoxCity = list[1];
			int index = provinceIndex(province);
			if (index < 0)
				return "COMPLETE";
			Cities[] cities = CitiesHelper.getCities(Province.values()[index]);
			Cities chosenCity = null;
			for (Cities city : cities) {
				if (city.getName().equalsIgnoreCase(dropBoxCity)) {
					chosenCity = city;
					break;
				}
			}
			if (chosenCity == null)
				return "COMPLETE";
			
			retval = Province.values()[index].abbreviation().toLowerCase();
			retval += "|";
			retval += chosenCity.getName();
			retval += "|";

			CurrentWeather currentWeather;
			try {
				currentWeather = new CurrentWeather(chosenCity, "");
			} catch (InterruptedException e) {
				e.printStackTrace();
				return "COMPLETE";
			}

			try {
				String notReported = "Not Reported";
				Double temp = currentWeather.getTemperature();
				if (temp == null)
					retval += notReported;
				else {
					retval += DecimalUtils.format(temp, DecimalUtils.DataType.TEMPERATURE);
				}
				retval += "|";
				Date time = currentWeather.getObservedDateTime();
				if (time == null)
					retval += currentWeather.getObserved();
				else {
					SimpleDateFormat sdf = new SimpleDateFormat("h:mm, d MMM yyyy");
					retval += sdf.format(time);
				}
				retval += "|";
				
				Double humidity = currentWeather.getHumidity();
				if (humidity == null)
					retval += notReported;
				else
					retval += DecimalUtils.format(humidity, DecimalUtils.DataType.RH);
				retval += "|";
				
				Double speed = currentWeather.getWindSpeed();
				if (speed == null)
					retval += notReported;
				else {
					retval += DecimalUtils.format(speed, DecimalUtils.DataType.WIND_SPEED);
				}
				retval += "|";
				
				Double wdir = currentWeather.getWindDirectionAngle();
				if (wdir == null)
					retval += notReported;
				else
					retval += DecimalUtils.format(wdir, DecimalUtils.DataType.WIND_DIR);
				retval += "\r\nCOMPLETE";
			}
			catch (InterruptedException|InvalidXMLException e) {
				e.printStackTrace();
				return "COMPLETE";
			}
		}
		return retval;
	}
	
	private static int provinceIndex(String province) {
		int i = -1;
		switch (province.toLowerCase()) {
		case "ab":
		case "alberta":
			i = 0;
			break;
		case "bc":
		case "british columbia":
			i = 1;
			break;
		case "mb":
		case "manitoba":
			i = 2;
			break;
		case "nb":
		case "new brunswick":
			i = 3;
			break;
		case "nl":
		case "newfoundland and labrador":
			i = 4;
			break;
		case "nt":
		case "northwest territories":
			i = 5;
			break;
		case "ns":
		case "nova scotia":
			i = 6;
			break;
		case "nu":
		case "nunavut":
			i = 7;
			break;
		case "on":
		case "ontario":
			i = 8;
			break;
		case "pe":
		case "prince edward island":
			i = 9;
			break;
		case "qc":
		case "quebec":
			i = 10;
			break;
		case "sk":
		case "saskatchewan":
			i = 11;
			break;
		case "yt":
		case "yukon":
			i = 12;
			break;
		}
		return i;
	}

	private static List<String> getListOfCitiesObservedWx(String province) {
		int i = provinceIndex(province);
		if (i < 0)
			return new ArrayList<String>();
		List<String> list;
		switch (Province.values()[i]) {
		case ALBERTA:
			list = AlbertaCities.valuesAsStrings();
			break;
		case ONTARIO:
			list = OntarioCities.valuesAsStrings();
			break;
		case BRITISH_COLUMBIA:
			list = BritishColumbiaCities.valuesAsStrings();
			break;
		case MANITOBA:
			list = ManitobaCities.valuesAsStrings();
			break;
		case NEW_BRUNSWICK:
			list = NewBrunswickCities.valuesAsStrings();
			break;
		case NEWFOUNDLAND_AND_LABRADOR:
			list = NewfoundLandLabradorCities.valuesAsStrings();
			break;
		case NORTHWEST_TERRITORIES:
			list = NorthwestTerritoriesCities.valuesAsStrings();
			break;
		case NOVA_SCOTIA:
			list = NovaScotiaCities.valuesAsStrings();
			break;
		case NUNAVUT:
			list = NunavutCities.valuesAsStrings();
			break;
		case PRINCE_EDWARD_ISLAND:
			list = PrinceEdwardIslandCities.valuesAsStrings();
			break;
		case QUEBEC:
			list = QuebecCities.valuesAsStrings();
			break;
		case SASKATCHEWAN:
			list = SaskatchewanCities.valuesAsStrings();
			break;
		default:
			list = YukonTerritoryCities.valuesAsStrings();
			break;
		}
		return list;
	}
}
