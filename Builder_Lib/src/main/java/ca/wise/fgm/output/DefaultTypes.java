package ca.wise.fgm.output;


public enum DefaultTypes {
	JOBLOCATION("JOBLOCATION"),
	JOBBASENAME("JOBBASENAME"),
	//FGM Option defaults
	MAXACCTS("MAXACCTS"),
	DISTRES("DISTRES"),
	PERIMRES("PERIMRES"),
	MINSPREADROS("fgmd_minspreadros"),
	STOPGRIDEND("STOPGRIDEND"),
	BREACHING("BREACHING"),
	DYNAMICTHRESHOLD("fgmd_dynamicthreshold"),
	SPOTTING("fgmd_spotting"),
    PURGENONDISPLAY("fgmd_purgenondisplay"),
	DX("fgmd_dx"),
	DY("fgmd_dy"),
	DT("fmgd_dt"),
	GROWTHAPPLIED("fgmd_growthPercApplied"),
	GROWTHPERC("fgmd_growthPercentile"),
	//FBP Option defaults
	TERRAINEFF("TERRAINEFFECT"),
	GREENUP("GREENUP"),
	WINDEFFECT("fgmd_windeffect"),
	//FMC Option defaults
	PEROVERVAL("PEROVERRIDEVAL"),
	NODATAELEV("NODATAELEV"),
	TERRAIN("fmcd_terrain"),
	ACCURATE_LOCATION("fmcd_accuratelocation"),
	//FWI Option defaults
	FWISPACINTERP("FWISPACINTERP"),
	FWIFROMSPACWEATH("FWIFROMSPACWEATH"),
	HISTORYONFWI("HISTORYONFWI"),
	BURNINGCONDITIONSON("fwid_burnconditions"),
	TEMPORALINTERP("fwid_tempinterp"),
	//Output file defaults
	TIMETOEXEC("TIMETOEXEC"),
	GRIDINFO("GRIDINFO"),
	LOCATION("LOCATION"),
	ELEVINFO("ELEVINFO"),
	INPUTSUM("INPUTSUMMARY"),
	VERSION("VERSION"),
	SCENNAME("SCENNAME"),
	JOBNAME("JOBNAME"),
	IGNAME("IGNAME"),
	SIMDATE("SIMDATE"),
	FIRESIZE("FIRESIZE"),
	PERIMTOTAL("PERIMTOTAL"),
	PERIMACTIVE("PERIMACTIVE"),
	AREAUNIT("AREAUNIT"),
	PERIMUNIT("PERIMUNIT");

	String text;

	DefaultTypes(String t) {
		text = t;
	}

	@Override
	public String toString() {
		return text;
	}
}
