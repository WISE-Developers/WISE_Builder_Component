package ca.wise.rpc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * Server methods for receiving fgm files.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.19.0)",
    comments = "Source: file_server.proto")
public final class FileServerGrpc {

  private FileServerGrpc() {}

  public static final String SERVICE_NAME = "rpc.FileServer";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<ca.wise.rpc.proto.FileChunk,
      ca.wise.rpc.proto.FileResult> getSendFileMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendFile",
      requestType = ca.wise.rpc.proto.FileChunk.class,
      responseType = ca.wise.rpc.proto.FileResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<ca.wise.rpc.proto.FileChunk,
      ca.wise.rpc.proto.FileResult> getSendFileMethod() {
    io.grpc.MethodDescriptor<ca.wise.rpc.proto.FileChunk, ca.wise.rpc.proto.FileResult> getSendFileMethod;
    if ((getSendFileMethod = FileServerGrpc.getSendFileMethod) == null) {
      synchronized (FileServerGrpc.class) {
        if ((getSendFileMethod = FileServerGrpc.getSendFileMethod) == null) {
          FileServerGrpc.getSendFileMethod = getSendFileMethod = 
              io.grpc.MethodDescriptor.<ca.wise.rpc.proto.FileChunk, ca.wise.rpc.proto.FileResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(
                  "rpc.FileServer", "SendFile"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ca.wise.rpc.proto.FileChunk.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ca.wise.rpc.proto.FileResult.getDefaultInstance()))
                  .setSchemaDescriptor(new FileServerMethodDescriptorSupplier("SendFile"))
                  .build();
          }
        }
     }
     return getSendFileMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static FileServerStub newStub(io.grpc.Channel channel) {
    return new FileServerStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static FileServerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new FileServerBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static FileServerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new FileServerFutureStub(channel);
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static abstract class FileServerImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Stream a file from the client to the server.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<ca.wise.rpc.proto.FileChunk> sendFile(
        io.grpc.stub.StreamObserver<ca.wise.rpc.proto.FileResult> responseObserver) {
      return asyncUnimplementedStreamingCall(getSendFileMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendFileMethod(),
            asyncClientStreamingCall(
              new MethodHandlers<
                ca.wise.rpc.proto.FileChunk,
                ca.wise.rpc.proto.FileResult>(
                  this, METHODID_SEND_FILE)))
          .build();
    }
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static final class FileServerStub extends io.grpc.stub.AbstractStub<FileServerStub> {
    private FileServerStub(io.grpc.Channel channel) {
      super(channel);
    }

    private FileServerStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FileServerStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new FileServerStub(channel, callOptions);
    }

    /**
     * <pre>
     * Stream a file from the client to the server.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<ca.wise.rpc.proto.FileChunk> sendFile(
        io.grpc.stub.StreamObserver<ca.wise.rpc.proto.FileResult> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(getSendFileMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static final class FileServerBlockingStub extends io.grpc.stub.AbstractStub<FileServerBlockingStub> {
    private FileServerBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private FileServerBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FileServerBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new FileServerBlockingStub(channel, callOptions);
    }
  }

  /**
   * <pre>
   * Server methods for receiving fgm files.
   * </pre>
   */
  public static final class FileServerFutureStub extends io.grpc.stub.AbstractStub<FileServerFutureStub> {
    private FileServerFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private FileServerFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FileServerFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new FileServerFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_SEND_FILE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final FileServerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(FileServerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_FILE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sendFile(
              (io.grpc.stub.StreamObserver<ca.wise.rpc.proto.FileResult>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class FileServerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    FileServerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ca.wise.rpc.proto.FileServerProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("FileServer");
    }
  }

  private static final class FileServerFileDescriptorSupplier
      extends FileServerBaseDescriptorSupplier {
    FileServerFileDescriptorSupplier() {}
  }

  private static final class FileServerMethodDescriptorSupplier
      extends FileServerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    FileServerMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (FileServerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new FileServerFileDescriptorSupplier())
              .addMethod(getSendFileMethod())
              .build();
        }
      }
    }
    return result;
  }
}
