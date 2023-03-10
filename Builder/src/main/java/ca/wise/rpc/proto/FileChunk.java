// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: file_server.proto

package ca.wise.rpc.proto;

/**
 * <pre>
 * A part of a file.
 * </pre>
 *
 * Protobuf type {@code rpc.FileChunk}
 */
public  final class FileChunk extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rpc.FileChunk)
    FileChunkOrBuilder {
private static final long serialVersionUID = 0L;
  // Use FileChunk.newBuilder() to construct.
  private FileChunk(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private FileChunk() {
    name_ = "";
    data_ = com.google.protobuf.ByteString.EMPTY;
    type_ = 0;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private FileChunk(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            name_ = s;
            break;
          }
          case 18: {

            data_ = input.readBytes();
            break;
          }
          case 24: {
            int rawValue = input.readEnum();

            type_ = rawValue;
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return ca.wise.rpc.proto.FileServerProto.internal_static_rpc_FileChunk_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return ca.wise.rpc.proto.FileServerProto.internal_static_rpc_FileChunk_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            ca.wise.rpc.proto.FileChunk.class, ca.wise.rpc.proto.FileChunk.Builder.class);
  }

  /**
   * Protobuf enum {@code rpc.FileChunk.OutputType}
   */
  public enum OutputType
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>XML = 0;</code>
     */
    XML(0),
    /**
     * <code>JSON_PRETTY = 1;</code>
     */
    JSON_PRETTY(1),
    /**
     * <code>JSON_MINIMAL = 2;</code>
     */
    JSON_MINIMAL(2),
    /**
     * <code>BINARY = 3;</code>
     */
    BINARY(3),
    UNRECOGNIZED(-1),
    ;

    /**
     * <code>XML = 0;</code>
     */
    public static final int XML_VALUE = 0;
    /**
     * <code>JSON_PRETTY = 1;</code>
     */
    public static final int JSON_PRETTY_VALUE = 1;
    /**
     * <code>JSON_MINIMAL = 2;</code>
     */
    public static final int JSON_MINIMAL_VALUE = 2;
    /**
     * <code>BINARY = 3;</code>
     */
    public static final int BINARY_VALUE = 3;


    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static OutputType valueOf(int value) {
      return forNumber(value);
    }

    public static OutputType forNumber(int value) {
      switch (value) {
        case 0: return XML;
        case 1: return JSON_PRETTY;
        case 2: return JSON_MINIMAL;
        case 3: return BINARY;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<OutputType>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        OutputType> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<OutputType>() {
            public OutputType findValueByNumber(int number) {
              return OutputType.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return ca.wise.rpc.proto.FileChunk.getDescriptor().getEnumTypes().get(0);
    }

    private static final OutputType[] VALUES = values();

    public static OutputType valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private OutputType(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:rpc.FileChunk.OutputType)
  }

  public static final int NAME_FIELD_NUMBER = 1;
  private volatile java.lang.Object name_;
  /**
   * <pre>
   * The name of the job that the file is for.
   * </pre>
   *
   * <code>string name = 1;</code>
   */
  public java.lang.String getName() {
    java.lang.Object ref = name_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      name_ = s;
      return s;
    }
  }
  /**
   * <pre>
   * The name of the job that the file is for.
   * </pre>
   *
   * <code>string name = 1;</code>
   */
  public com.google.protobuf.ByteString
      getNameBytes() {
    java.lang.Object ref = name_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      name_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int DATA_FIELD_NUMBER = 2;
  private com.google.protobuf.ByteString data_;
  /**
   * <pre>
   * The bytes from the file.
   * </pre>
   *
   * <code>bytes data = 2;</code>
   */
  public com.google.protobuf.ByteString getData() {
    return data_;
  }

  public static final int TYPE_FIELD_NUMBER = 3;
  private int type_;
  /**
   * <pre>
   * The file format to save the data in.
   * </pre>
   *
   * <code>.rpc.FileChunk.OutputType type = 3;</code>
   */
  public int getTypeValue() {
    return type_;
  }
  /**
   * <pre>
   * The file format to save the data in.
   * </pre>
   *
   * <code>.rpc.FileChunk.OutputType type = 3;</code>
   */
  public ca.wise.rpc.proto.FileChunk.OutputType getType() {
    @SuppressWarnings("deprecation")
    ca.wise.rpc.proto.FileChunk.OutputType result = ca.wise.rpc.proto.FileChunk.OutputType.valueOf(type_);
    return result == null ? ca.wise.rpc.proto.FileChunk.OutputType.UNRECOGNIZED : result;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!getNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, name_);
    }
    if (!data_.isEmpty()) {
      output.writeBytes(2, data_);
    }
    if (type_ != ca.wise.rpc.proto.FileChunk.OutputType.XML.getNumber()) {
      output.writeEnum(3, type_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, name_);
    }
    if (!data_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream
        .computeBytesSize(2, data_);
    }
    if (type_ != ca.wise.rpc.proto.FileChunk.OutputType.XML.getNumber()) {
      size += com.google.protobuf.CodedOutputStream
        .computeEnumSize(3, type_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof ca.wise.rpc.proto.FileChunk)) {
      return super.equals(obj);
    }
    ca.wise.rpc.proto.FileChunk other = (ca.wise.rpc.proto.FileChunk) obj;

    if (!getName()
        .equals(other.getName())) return false;
    if (!getData()
        .equals(other.getData())) return false;
    if (type_ != other.type_) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + NAME_FIELD_NUMBER;
    hash = (53 * hash) + getName().hashCode();
    hash = (37 * hash) + DATA_FIELD_NUMBER;
    hash = (53 * hash) + getData().hashCode();
    hash = (37 * hash) + TYPE_FIELD_NUMBER;
    hash = (53 * hash) + type_;
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static ca.wise.rpc.proto.FileChunk parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static ca.wise.rpc.proto.FileChunk parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static ca.wise.rpc.proto.FileChunk parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static ca.wise.rpc.proto.FileChunk parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(ca.wise.rpc.proto.FileChunk prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * A part of a file.
   * </pre>
   *
   * Protobuf type {@code rpc.FileChunk}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rpc.FileChunk)
      ca.wise.rpc.proto.FileChunkOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return ca.wise.rpc.proto.FileServerProto.internal_static_rpc_FileChunk_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return ca.wise.rpc.proto.FileServerProto.internal_static_rpc_FileChunk_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              ca.wise.rpc.proto.FileChunk.class, ca.wise.rpc.proto.FileChunk.Builder.class);
    }

    // Construct using ca.wise.rpc.proto.FileChunk.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      name_ = "";

      data_ = com.google.protobuf.ByteString.EMPTY;

      type_ = 0;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return ca.wise.rpc.proto.FileServerProto.internal_static_rpc_FileChunk_descriptor;
    }

    @java.lang.Override
    public ca.wise.rpc.proto.FileChunk getDefaultInstanceForType() {
      return ca.wise.rpc.proto.FileChunk.getDefaultInstance();
    }

    @java.lang.Override
    public ca.wise.rpc.proto.FileChunk build() {
      ca.wise.rpc.proto.FileChunk result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public ca.wise.rpc.proto.FileChunk buildPartial() {
      ca.wise.rpc.proto.FileChunk result = new ca.wise.rpc.proto.FileChunk(this);
      result.name_ = name_;
      result.data_ = data_;
      result.type_ = type_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof ca.wise.rpc.proto.FileChunk) {
        return mergeFrom((ca.wise.rpc.proto.FileChunk)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(ca.wise.rpc.proto.FileChunk other) {
      if (other == ca.wise.rpc.proto.FileChunk.getDefaultInstance()) return this;
      if (!other.getName().isEmpty()) {
        name_ = other.name_;
        onChanged();
      }
      if (other.getData() != com.google.protobuf.ByteString.EMPTY) {
        setData(other.getData());
      }
      if (other.type_ != 0) {
        setTypeValue(other.getTypeValue());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      ca.wise.rpc.proto.FileChunk parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (ca.wise.rpc.proto.FileChunk) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object name_ = "";
    /**
     * <pre>
     * The name of the job that the file is for.
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public java.lang.String getName() {
      java.lang.Object ref = name_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        name_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <pre>
     * The name of the job that the file is for.
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public com.google.protobuf.ByteString
        getNameBytes() {
      java.lang.Object ref = name_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        name_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <pre>
     * The name of the job that the file is for.
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public Builder setName(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      name_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * The name of the job that the file is for.
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public Builder clearName() {
      
      name_ = getDefaultInstance().getName();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * The name of the job that the file is for.
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public Builder setNameBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      name_ = value;
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString data_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <pre>
     * The bytes from the file.
     * </pre>
     *
     * <code>bytes data = 2;</code>
     */
    public com.google.protobuf.ByteString getData() {
      return data_;
    }
    /**
     * <pre>
     * The bytes from the file.
     * </pre>
     *
     * <code>bytes data = 2;</code>
     */
    public Builder setData(com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      data_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * The bytes from the file.
     * </pre>
     *
     * <code>bytes data = 2;</code>
     */
    public Builder clearData() {
      
      data_ = getDefaultInstance().getData();
      onChanged();
      return this;
    }

    private int type_ = 0;
    /**
     * <pre>
     * The file format to save the data in.
     * </pre>
     *
     * <code>.rpc.FileChunk.OutputType type = 3;</code>
     */
    public int getTypeValue() {
      return type_;
    }
    /**
     * <pre>
     * The file format to save the data in.
     * </pre>
     *
     * <code>.rpc.FileChunk.OutputType type = 3;</code>
     */
    public Builder setTypeValue(int value) {
      type_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * The file format to save the data in.
     * </pre>
     *
     * <code>.rpc.FileChunk.OutputType type = 3;</code>
     */
    public ca.wise.rpc.proto.FileChunk.OutputType getType() {
      @SuppressWarnings("deprecation")
      ca.wise.rpc.proto.FileChunk.OutputType result = ca.wise.rpc.proto.FileChunk.OutputType.valueOf(type_);
      return result == null ? ca.wise.rpc.proto.FileChunk.OutputType.UNRECOGNIZED : result;
    }
    /**
     * <pre>
     * The file format to save the data in.
     * </pre>
     *
     * <code>.rpc.FileChunk.OutputType type = 3;</code>
     */
    public Builder setType(ca.wise.rpc.proto.FileChunk.OutputType value) {
      if (value == null) {
        throw new NullPointerException();
      }
      
      type_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * The file format to save the data in.
     * </pre>
     *
     * <code>.rpc.FileChunk.OutputType type = 3;</code>
     */
    public Builder clearType() {
      
      type_ = 0;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:rpc.FileChunk)
  }

  // @@protoc_insertion_point(class_scope:rpc.FileChunk)
  private static final ca.wise.rpc.proto.FileChunk DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new ca.wise.rpc.proto.FileChunk();
  }

  public static ca.wise.rpc.proto.FileChunk getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<FileChunk>
      PARSER = new com.google.protobuf.AbstractParser<FileChunk>() {
    @java.lang.Override
    public FileChunk parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new FileChunk(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<FileChunk> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<FileChunk> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public ca.wise.rpc.proto.FileChunk getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

