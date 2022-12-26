package ca.wise.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class JobResponse {

	@JsonProperty("cores_available")
	public int coresAvailable;
	
	@JsonProperty("job_name")
	public String jobName;
	
	@JsonProperty("offset")
	public Integer offset;
	
	@JsonProperty("size")
	public Integer size;
	
	@JsonProperty("rpc_address")
	public String rpcAddress;
    
    @JsonProperty("rpc_internal_address")
    public String rpcInternalAddress;
}
