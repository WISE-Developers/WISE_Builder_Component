package ca.wise.fgm.output;

public enum Message {
	UNKNOWN(""),
	ACK("ACK"),
	STARTUP("STARTUP"),
	SHUTDOWN("SHUTDOWN"),
	ADMIN("ADMIN"),
	BEGINDATA("BEGINDATA"),
	ENDDATA("ENDDATA"),
	STARTJOB("STARTJOB"),
	STARTJOB_PB("START_JOB_PB"),
	STARTJOB_PB_V2("START_JOB_PB_V2"),
	GETDEFAULTS("GETDEFAULTS"),
	HOURLY_FFMC_VAN_WAGNER("HOURLY_FFMC_VAN_WAGNER"),
	HOURLY_FFMC_EQUILIBRIUM("HOURLY_FFMC_EQUILIBRIUM"),
	HOURLY_FFMC_LAWSON("HOURLY_FFMC_LAWSON"),
	HOURLY_FFMC_VAN_WAGNER_PREVIOUS("HOURLY_FFMC_VAN_WAGNER_PREVIOUS"),
	HOURLY_FFMC_EQUILIBRIUM_PREVIOUS("HOURLY_FFMC_EQUILIBRIUM_PREVIOUS"),
	HOURLY_FFMC_LAWSON_CONTIGUOUS("HOURLY_FFMC_LAWSON_CONTIGUOUS"),
	DAILY_FFMC_VAN_WAGNER("DAILY_FFMC_VAN_WAGNER"),
	DMC("DMC"),
	DC("DC"),
	FF("FF"),
	ISI_FWI("ISI_FWI"),
	ISI_FBP("ISI_FBP"),
	BUI("BUI"),
	FWI("FWI"),
	DSR("DSR"),
	LIST_TIMEZONES("LIST_TIMEZONES"),
	ARCHIVE_ZIP("ZIP"),
	ARCHIVE_TAR("TAR"),
	ARCHIVE_DELETE("DELETE"),
	JOB_OPTIONS("LIST_OPTIONS_COMPLETE"),
	JOB_OPTIONS_RUNNING("LIST_OPTIONS_RUNNING"),
	JOB_OPTIONS_QUEUED("LIST_OPTIONS_QUEUED"),
	FBP_GET_FUELS("FBP_GET_FUELS"),
	FBP_GET_FUELS_V2("FBP_GET_FUELS_V2"),
	FBP_CALCULATE("FBP_CALCULATE"),
	//forecast messages
	FORECAST_LIST_CITIES("FORECAST_LIST_CITIES"),
	FORECAST_GET("FORECAST_GET"),
	//weather messages
	WEATHER_LIST_CITIES("WEATHER_LIST_CITIES"),
	WEATHER_GET("WEATHER_GET"),
	//solar calculator
	SOLAR_CALCULATOR("SOLAR_CALCULATOR"),
	STOP_JOB("STOP_JOB"),
	LICENSES("GET_LICENSES");

	String name;
	public String data;

	Message(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	private static String concatenateList(String list[]) {
		StringBuilder builder = new StringBuilder();
		String sep = "";
		for (int i = 1; i < list.length; i++) {
			builder.append(sep).append(list[i]);
			sep = " ";
		}
		return builder.toString();
	}

	public static Message fromString(String mess) {
		Message ms[] = values();
		if (mess == null)
			return UNKNOWN;
		String split[] = mess.split(" ");
		if (split == null || split.length == 0)
			return UNKNOWN;
		String message = split[0].toUpperCase();
		for (Message m : ms) {
			if (m.name.equals(message)) {
				if (split.length > 1) {
					m.data = concatenateList(split);
				}
				return m;
			}
		}
		return UNKNOWN;
	}
}
