package ca.wise.rpc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;

import ca.wise.json.Job;
import ca.wise.json.JobResponse;
import ca.wise.main.MQTTClient;
import ca.wise.fgm.tools.WISELogger;
import ca.wise.rpc.proto.FileChunk;
import ca.wise.rpc.proto.FileResult;
import ca.wise.rpc.proto.FileServerGrpc;
import ca.wise.rpc.proto.FileServerGrpc.FileServerStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;

public class FileServerClient {

	private final MQTTClient client;
	private final JobResponse response;
	private final String managerId;
    private final String myAddress;
	private final Job job;
	private ManagedChannel channel;
	private FileServerStub asyncStub;
	
	public FileServerClient(MQTTClient client, JobResponse response, Job job, String managerId, String myAddress) {
		this.client = client;
		this.response = response;
		this.managerId = managerId;
		this.job = job;
		this.myAddress = myAddress;
	}
	
	public void runAsync() {
		new Thread(() -> {
			try {
				start();
				sendFgmFileAsync();
			}
			catch (Exception e) {
				WISELogger.error("Error submitting job via RPC", e);
			}
            finally {
                try {
                    shutdown();
                }
                catch (Exception e) {
                    WISELogger.error("Error shutting down RPC", e);
                }
            }
		}).start();;
	}
	
	public void start() {
        String rpcAddress;
        //if the remote server specified both an internal and external address
        if (!Strings.isNullOrEmpty(response.rpcInternalAddress)) {
            //if I know my address
            if (!Strings.isNullOrEmpty(myAddress)) {
                //remove the port from the end of the remote external address
                String externalAddress;
                int index = response.rpcAddress.lastIndexOf(':');
                if (index > 0)
                    externalAddress = response.rpcAddress.substring(0, index);
                else
                    externalAddress = response.rpcAddress;
                //if my external address is the same as the remote external address use the internal address
                if (myAddress.equals(externalAddress))
                    rpcAddress = response.rpcInternalAddress;
                    //if my external address is different than the remote external address use the external address
                else
                    rpcAddress = response.rpcAddress;
            }
            //if I don't know my address just use the remote external address
            else
                rpcAddress = response.rpcAddress;
        }
        //if the remote server only has an external address use that
        else
            rpcAddress = response.rpcAddress;

		channel = ManagedChannelBuilder
					.forTarget(rpcAddress)
					.usePlaintext()
					.build();
		asyncStub = FileServerGrpc.newStub(channel);
	}
	
	public void shutdown() throws InterruptedException {
	    if (channel != null && !channel.isShutdown())
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}
	
	public void sendFgmFileAsync() throws InterruptedException {
		CountDownLatch finishedLatch = new CountDownLatch(1);
		AtomicBoolean wasError = new AtomicBoolean(false);
		
		FileChunk.OutputType type = FileChunk.OutputType.UNRECOGNIZED;
		switch (job.type) {
		case BINARY:
        case BINARY_V2:
			type = FileChunk.OutputType.BINARY;
			break;
		case MINIMAL_PROTO:
        case MINIMAL_PROTO_V2:
			type = FileChunk.OutputType.JSON_MINIMAL;
			break;
		case PROTO:
        case PROTO_V2:
			type = FileChunk.OutputType.JSON_PRETTY;
			break;
		case XML:
			type = FileChunk.OutputType.XML;
			break;
		}
        final FileChunk.OutputType typ = type;

        ClientResponseObserver<FileChunk, FileResult> responseObserver = new ClientResponseObserver<FileChunk, FileResult>() {

            @Override
            public void onNext(FileResult value) { }

            @Override
            public void onError(Throwable t) {
                WISELogger.error("Failed to send FGM file data.", t);
                wasError.set(true);
                finishedLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishedLatch.countDown();
            }

            @Override
            public void beforeStart(final ClientCallStreamObserver<FileChunk> requestStream) {
                Runnable drain = new Runnable() {
                    int offset = 0;
                    boolean isComplete = false;

                    @Override
                    public void run() {
                        try {
                            final int size = 2097152;//2MiB
                            for (; offset < job.filedata.length && requestStream.isReady(); offset += size) {
                                int length = job.filedata.length - offset;
                                if (length > size)
                                    length = size;
                                ByteString buffer = ByteString.copyFrom(job.filedata, offset, length);
                                FileChunk.Builder builder = FileChunk.newBuilder()
                                        .setType(typ)
                                        .setName(job.name)
                                        .setData(buffer);
                                requestStream.onNext(builder.build());
                                //an error has occurred
                                if (finishedLatch.getCount() == 0)
                                    break;
                            }

                            if (offset >= job.getFileSize() && !isComplete) {
                                isComplete = true;
                                requestStream.onCompleted();
                            }
                        }
                        catch (RuntimeException e) {
                            WISELogger.error("Error transfering FGM file data.", e);
                            throw e;
                        }
                    }
                };
                requestStream.setOnReadyHandler(drain);
            }
        };

        @SuppressWarnings("unused") StreamObserver<FileChunk> requestObserver = asyncStub.withCompression("gzip").sendFile(responseObserver);

		if (!finishedLatch.await(5, TimeUnit.MINUTES)) {
			WISELogger.warn("An error may have occurred while transferring FGM data, the connection is taking a long time");
		}
		
		if (wasError.get())
			client.onRPCSendError(job.name, managerId, response);
		else {
			try {
				client.onRPCSendComplete(job.name, managerId);
			}
			catch (Exception e) {
				WISELogger.error("Failed to send job start signal after RPC transfer", e);
			}
		}
	}
}
