package ca.wise.main;

public class Admin {
	public boolean kill = false;
	public boolean reloadDefaults = false;
	public boolean listCompleteJobs = false;

	public Admin(String data) {
		String split[] = data.split(" ");
		if (split.length > 1) {
			if (split[1].equalsIgnoreCase("kill")) {
				kill = true;
			}
			else if (split[1].equalsIgnoreCase("reload")) {
				reloadDefaults = true;
			}
			else if (split[1].equalsIgnoreCase("finishedjobs")) {
				listCompleteJobs = true;
			}
		}
	}
}
