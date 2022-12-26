package ca.wise.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class Shutdown {

	@JsonProperty("priority")
	public int priority;
	
	@JsonProperty("timeout")
	public Integer timeout;
}
