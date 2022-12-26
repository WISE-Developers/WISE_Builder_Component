package ca.wise.fgm.output;


public interface IDataWriter {
	
	boolean write(String jobname);
	
	byte[] stream(String jobname);
	
	OutputType getType();
	
	int getCoreCount();
	
	int getPriority();
	
	boolean isValidate();
}
