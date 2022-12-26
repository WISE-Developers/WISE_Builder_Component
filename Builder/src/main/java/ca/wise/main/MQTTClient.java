package ca.wise.main;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import ca.wise.json.Checkin;
import ca.wise.json.Job;
import ca.wise.json.JobResponse;
import ca.wise.json.Manage;
import ca.wise.json.Shutdown;
import ca.wise.fgm.tools.WISELogger;
import ca.wise.rpc.FileServerClient;

public class MQTTClient implements MqttCallback, IMqttActionListener {

	private MqttAsyncClient client;
	private String myId;
	private Object locker = new Object();
	private String topic = "wise";
	private List<Job> requests = new ArrayList<>();
	private ObjectMapper mapper = new ObjectMapper()
	        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String myIpAddress;
    private ExecutorService executors = Executors.newFixedThreadPool(5);
	
	private List<Checkin> onlineManagers = new ArrayList<>();
	
	private long getPID() {
		Long pid = null;
		try {
			String processName = ManagementFactory.getRuntimeMXBean().getName();
			pid = Long.parseLong(processName.split("@")[0]);
		}
		catch (Exception ex) { }
		if (pid == null) {
			WISELogger.warn("Unable to find PID, using a random number instead");
			pid = new Random(System.currentTimeMillis()).nextLong();
		}
		return pid;
	}
	
	public String getHost() {
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostName();
		}
		catch (Exception ex) { }
		if (host == null) {
			WISELogger.warn("Unable to find host name, using a random number instead");
			host = Long.toHexString(new Random(System.currentTimeMillis()).nextLong());
		}
		return host;
	}
	
	public String buildTopic(String to, String message) {
		String topic = this.topic;
		topic += "/";
		topic += myId;
		topic += "/";
		topic += to;
		topic += "/";
		topic += message;
		return topic;
	}
	
	public MQTTClient(URI url, String username, String password, String topic, String myIpAddress) {
		this.topic = topic;
		this.myIpAddress = myIpAddress;
		//TODO background connection
		myId = "builder_" + getPID() + "-" + getHost().toUpperCase();
		String address = url.toString();
        MqttClientPersistence persistence;
        if (CommandLine.get().getMqttDirectory() != null) {
        	if (CommandLine.get().getMqttDirectory().equalsIgnoreCase(":memory"))
        		persistence = new MemoryPersistence();
        	else
        		persistence = new MqttDefaultFilePersistence(CommandLine.get().getMqttDirectory());
        }
        else
        	persistence = new MqttDefaultFilePersistence();
		try {
			client = new MqttAsyncClient(address, myId, persistence);
			DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
			bufferOptions.setBufferSize(4194304);
			client.setBufferOpts(bufferOptions);
			MqttConnectOptions options = new MqttConnectOptions();
			if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)) {
				options.setUserName(username);
				options.setPassword(password.toCharArray());
			}
			client.setCallback(this);
			client.connect(options, null, this);
		}
		catch (MqttException e) {
			WISELogger.error("Unable to connect to MQTT broker", e);
			throw new RuntimeException(e);
		}
	}
	
	public void shutdown() {
		synchronized (locker) {
			if (client != null && client.isConnected()) {
				try {
					client.disconnect().waitForCompletion();
					client.close();
				}
				catch (MqttException e) {
					WISELogger.error("Unable to disconnect from the MQTT broker", e);
				}
			}
		}
	}
	
	public boolean sendMessage(String topic, String message) {
		boolean retval = false;
		synchronized (locker) {
			try {
				client.publish(topic, message.getBytes(StandardCharsets.UTF_8), 0, false);
			}
			catch (Exception e) {
				WISELogger.error("Unable to send MQTT message to topic " + topic, e);
			}
		}
		return retval;
	}
	
	public boolean sendMessage(String topic, byte[] payload) {
		boolean retval = false;
		synchronized (locker) {
			try {
				client.publish(topic, payload, 0, false);
			}
			catch (Exception e) {
				WISELogger.error("Unable to send MQTT message to topic " + topic, e);
			}
		}
		return retval;
	}
	
	public void sendJobRequest(Job request) {
		synchronized (locker) {
			requests.add(request);
			try {
				String payload = mapper.writeValueAsString(request);
				String topic;
				if (request.filedata == null)
					topic = buildTopic("manager", "request");
				else
					topic = buildTopic("manager", "remoterequest");
				client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 0, false);
			}
			catch (JsonProcessingException | MqttException e) {
				WISELogger.error("Unable to send job request", e);
			}
		}
	}
	
	public void sendStopJobRequest(String jobName, Shutdown request) {
		synchronized (locker) {
			try {
				String payload = mapper.writeValueAsString(request);
				String topic = buildTopic(jobName, "shutdown");
				client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 0, false);
			}
			catch (JsonProcessingException | MqttException e) {
				WISELogger.error("Unable to send job shutdown request", e);
			}
		}
	}
	
	public void sendCheckinRequest() {
		synchronized (locker) {
			try {
				String topic = buildTopic("broadcast", "reportin");
				client.publish(topic, new byte[0], 0, false);
			}
			catch (MqttException e) {
				WISELogger.error("Unable to send checkin request", e);
			}
		}
	}
	
	public void sendCheckinResponse(String requester, Checkin checkin) {
		synchronized (locker) {
			try {
				String payload = mapper.writeValueAsString(checkin);
				String topic = buildTopic(requester, "checkin");
				client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 0, false);
			}
			catch (JsonProcessingException | MqttException e) {
				WISELogger.error("Unable to send checkin response", e);
			}
		}
	}
	
	/**
	 * Attempted to send a job to manager using RPC but it failed,
	 * fall back to sending it via MQTT.
	 * @param job The name of the job that failed.
	 */
	public void onRPCSendError(String job, String managerId, JobResponse response) {
		Optional<Job> request = Optional.empty();
		synchronized (locker) {
			request = requests.stream().filter(x -> x.name.equals(job)).findFirst();
			if (request.isPresent()) {
				//if the job has not yet been claimed, assign it to this manager
				if (request.get().owner == null)
					request.get().owner = managerId;
				//if the job has already been assigned to a different manager, don't send data
				else if (!request.get().owner.equals(managerId))
					request = null;
			}
		}
		if (request.isPresent()) {
			request.get().client = null;
			if (response.size < 0) {
				//done transferring the job
				synchronized (locker) {
					requests.remove(request.get());
				}
			}
			//send the data over MQTT
			else {
				String topic = buildTopic(managerId, "remotestart/" + request.get().name);
				int to = response.offset + response.size;
				if (to > request.get().filedata.length)
					to = request.get().filedata.length;
				WISELogger.info("Send bytes " + response.offset + " to " + to + " of " + request.get().filedata.length + " to " + managerId);
				sendMessage(topic, Arrays.copyOfRange(request.get().filedata, response.offset, to));
			}
		}
	}
	
	/**
	 * Sent a job to manager using RPC, remove the job from the queue
	 * and send the start signal.
	 * @param job The name of the job that successfully sent.
	 */
	public void onRPCSendComplete(String job, String managerId) throws Exception {
		Optional<Job> request = Optional.empty();
		synchronized (locker) {
			request = requests.stream().filter(x -> x.name.equals(job)).findFirst();
			if (request.isPresent())
				requests.remove(request.get());
		}
		if (request.isPresent()) {
			request.get().client = null;
			String topic = buildTopic(managerId, "start");
			sendMessage(topic, mapper.writeValueAsString(request.get()));
		}
	}

	@Override
	public void connectionLost(Throwable arg0) { }

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) { }

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		String[] parts = arg0.split("/");
		WISELogger.info("Receive message " + arg0);
		executors.execute(() -> {
	        boolean handled = false;
	        try {
        		if (parts.length == 4) {
        			if (parts[3].equalsIgnoreCase("available")) {
        				handled = true;
        				Optional<Job> request = Optional.empty();
        				synchronized (locker) {
        					JobResponse response = mapper.readValue(arg1.getPayload(), JobResponse.class);
        					request = requests.stream().filter(x -> x.name.equals(response.jobName)).findFirst();
        					if (request.isPresent())
        						requests.remove(request.get());
        				}
        				if (request.isPresent()) {
        					String topic = buildTopic(parts[1], "start");
        					sendMessage(topic, mapper.writeValueAsString(request.get()));
        				}
        			}
        			else if (parts[3].equalsIgnoreCase("remoteavailable")) {
        				handled = true;
        				Optional<Job> request = Optional.empty();
        				JobResponse response = mapper.readValue(arg1.getPayload(), JobResponse.class);
        				synchronized (locker) {
        					request = requests.stream().filter(x -> x.name.equals(response.jobName)).findFirst();
        					if (request.isPresent()) {
        						//if the job has not yet been claimed, assign it to this manager
        						if (request.get().owner == null)
        							request.get().owner = parts[1];
        						//if the job has already been assigned to a different manager, don't send data
        						else if (!request.get().owner.equals(parts[1]))
                                    request = Optional.empty();
        					}
        				}
        				if (request.isPresent()) {
        					//the data can be transferred by RPC
        					if (!Strings.isNullOrEmpty(response.rpcAddress)) {
        						request.get().client = new FileServerClient(this, response, request.get(), parts[1], myIpAddress);
        						request.get().client.runAsync();
        					}
        					else if (response.size < 0) {
        						//done transferring the job
        						synchronized (locker) {
        							requests.remove(request.get());
        						}
        					}
        					//send the data over MQTT
        					else {
        						String topic = buildTopic(parts[1], "remotestart/" + request.get().name);
        						int to = response.offset + response.size;
        						if (to > request.get().filedata.length)
        							to = request.get().filedata.length;
										WISELogger.info("Send bytes " + response.offset + " to " + to + " of " + request.get().filedata.length + " to " + parts[1]);
        						sendMessage(topic, Arrays.copyOfRange(request.get().filedata, response.offset, to));
        					}
        				}
        			}
        			else if (parts[3].equalsIgnoreCase("checkin")) {
        				handled = true;
        				synchronized (onlineManagers) {
        					Checkin checkin = mapper.readValue(arg1.getPayload(), Checkin.class);
        					if (checkin.type == Checkin.TYPE_MANAGER) {
        						Optional<Checkin> old = onlineManagers.stream().filter(x -> x.id.equals(checkin.id)).findFirst();
        						if (old.isPresent()) {
        							if (checkin.status == Checkin.STATUS_SHUTTING_DOWN)
        								onlineManagers.remove(old.get());
        							else {
        								old.get().status = checkin.status;
        								old.get().type = checkin.type;
        								old.get().version = checkin.version;
        							}
        						}
        						else if (checkin.status != Checkin.STATUS_SHUTTING_DOWN) {
        							onlineManagers.add(checkin);
        						}
        					}
        				}
        			}
        			else if (parts[3].equalsIgnoreCase("manage")) {
        				handled = true;
        				Manage manage = mapper.readValue(arg1.getPayload(), Manage.class);
        				if (manage.request != null) {
        					if (manage.request.equals("shutdown")) {
        						Admin admin = new Admin("please kill");
        						new Thread(() -> shutdown()).start();
        						Main.getLooper().adminMessage(admin);
        					}
        					else if (manage.request.equals("reboot")) {
        						if (Main.canRestart()) {
        							Admin admin = new Admin("please kill");
        							new Thread(() -> shutdown()).start();
        							Main.setRestartOnExit(true);
        							Main.getLooper().adminMessage(admin);
        						}
        					}
        				}
        			}
        			else if (parts[3].equalsIgnoreCase("reportin")) {
        				if (parts[2].equalsIgnoreCase(myId) || parts[2].equalsIgnoreCase("builder") || parts[2].equalsIgnoreCase("broadcast")) {
        					handled = true;
        					Checkin checkin = new Checkin();
        					checkin.id = myId;
        					checkin.status = Checkin.STATUS_RUNNING;
        					checkin.version = Main.getVersion();
        					checkin.type = Checkin.TYPE_BUILDER;
        					checkin.topic = topic;
        					sendCheckinResponse(parts[1], checkin);
        				}
        			}
        		}
	        }
	        catch (IOException e) {
	            WISELogger.info("Unable to handle MQTT message", e);
	        }
    		
    		if (!handled)
    			WISELogger.info("Received message at topic " + arg0 + ": " + new String(arg1.getPayload(), StandardCharsets.UTF_8));
		});
	}

	@Override
	public void onFailure(IMqttToken arg0, Throwable arg1) {
		WISELogger.error("MQTT connection lost: ", arg1);
	}

	@Override
	public void onSuccess(IMqttToken arg0) {
		if (arg0.getUserContext() != null && arg0.getUserContext() instanceof String && ((String)arg0.getUserContext()).equals("shutdown")) {
			try {
				client.close();
				WISELogger.info("MQTT shutdown");
			}
			catch (MqttException e) {
				e.printStackTrace();
			}
		}
		else {
			WISELogger.info("MQTT connected as " + myId);
			try {
				client.subscribe(topic + "/+/" + myId + "/available", 0);
				client.subscribe(topic + "/+/" + myId + "/remoteavailable", 0);
				client.subscribe(topic + "/+/broadcast/checkin", 0);
				client.subscribe(topic + "/+/" + myId + "/checkin", 0);
				client.subscribe(topic + "/+/broadcast/reportin", 0);
				client.subscribe(topic + "/+/builder/manage", 0);
				client.subscribe(topic + "/+/" + myId + "/manage", 0);
				
				sendCheckinRequest();
			}
			catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
