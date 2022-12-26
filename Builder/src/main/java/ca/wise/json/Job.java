package ca.wise.json;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import ca.wise.fgm.output.OutputType;
import ca.wise.rpc.FileServerClient;

@JsonInclude(Include.NON_NULL)
public class Job {
    
    @JsonIgnore
    public static final int VALIDATE_NONE = 0;
    
    @JsonIgnore
    public static final int VALIDATE_CURRENT = 1;
    
    @JsonIgnore
    public static final int VALIDATE_COMPLETE = 2;

	@JsonProperty("job_name")
	public String name;
	
	@JsonProperty("use_cores")
	public int cores;
	
	@JsonGetter("file_size")
	public int getFileSize() {
		return filedata == null ? 0 : filedata.length;
	}
	
	@JsonGetter("file_extension")
	public String getExtension() {
		switch (type) {
		case BINARY:
		case BINARY_V2:
			return "fgmb";
		case MINIMAL_PROTO:
		case PROTO:
		case MINIMAL_PROTO_V2:
		case PROTO_V2:
			return "fgmj";
		case XML:
			return "xml";
		}
		return "";
	}
    
    @JsonProperty("job_priority")
    public int priority = 0;
    
    @JsonProperty("validation_state")
    public int validationState = VALIDATE_NONE;
	
	@JsonIgnore
	public byte[] filedata;
	
	@JsonIgnore
	public String owner = null;
	
	@JsonIgnore
	public OutputType type;
	
	@JsonIgnore
	public FileServerClient client = null;
}
