package ca.wise.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Checkin {

	@JsonIgnore
	public static final int STATUS_RUNNING = 0;
	
	@JsonIgnore
	public static final int STATUS_STARTING_UP = 1;
	
	@JsonIgnore
	public static final int STATUS_SHUTTING_DOWN = 2;
	
	@JsonIgnore
	public static final int TYPE_BUILDER = 0;
	
	@JsonIgnore
	public static final int TYPE_MANAGER = 1;
	
	@JsonProperty("node_id")
	public String id;
	
	@JsonProperty("version")
	public String version;
	
	@JsonProperty("status")
	public int status;
	
	@JsonProperty("node_type")
	public int type;
    
    @JsonProperty("topic_string")
    public String topic;
}
