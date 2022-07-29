// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: internal.proto

package org.kegbot.proto;

public final class Internal {
  private Internal() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface PendingPourOrBuilder extends
      // @@protoc_insertion_point(interface_extends:PendingPour)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required .RecordDrinkRequest drink_request = 1;</code>
     * @return Whether the drinkRequest field is set.
     */
    boolean hasDrinkRequest();
    /**
     * <code>required .RecordDrinkRequest drink_request = 1;</code>
     * @return The drinkRequest.
     */
    org.kegbot.proto.Api.RecordDrinkRequest getDrinkRequest();
    /**
     * <code>required .RecordDrinkRequest drink_request = 1;</code>
     */
    org.kegbot.proto.Api.RecordDrinkRequestOrBuilder getDrinkRequestOrBuilder();

    /**
     * <code>repeated string images = 2;</code>
     * @return A list containing the images.
     */
    java.util.List<java.lang.String>
        getImagesList();
    /**
     * <code>repeated string images = 2;</code>
     * @return The count of images.
     */
    int getImagesCount();
    /**
     * <code>repeated string images = 2;</code>
     * @param index The index of the element to return.
     * @return The images at the given index.
     */
    java.lang.String getImages(int index);
    /**
     * <code>repeated string images = 2;</code>
     * @param index The index of the value to return.
     * @return The bytes of the images at the given index.
     */
    com.google.protobuf.ByteString
        getImagesBytes(int index);
  }
  /**
   * Protobuf type {@code PendingPour}
   */
  public static final class PendingPour extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:PendingPour)
      PendingPourOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use PendingPour.newBuilder() to construct.
    private PendingPour(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private PendingPour() {
      images_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
      return new PendingPour();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private PendingPour(
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
              org.kegbot.proto.Api.RecordDrinkRequest.Builder subBuilder = null;
              if (((bitField0_ & 0x00000001) != 0)) {
                subBuilder = drinkRequest_.toBuilder();
              }
              drinkRequest_ = input.readMessage(org.kegbot.proto.Api.RecordDrinkRequest.PARSER, extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(drinkRequest_);
                drinkRequest_ = subBuilder.buildPartial();
              }
              bitField0_ |= 0x00000001;
              break;
            }
            case 18: {
              com.google.protobuf.ByteString bs = input.readBytes();
              if (!((mutable_bitField0_ & 0x00000002) != 0)) {
                images_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000002;
              }
              images_.add(bs);
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
        if (((mutable_bitField0_ & 0x00000002) != 0)) {
          images_ = images_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.kegbot.proto.Internal.internal_static_PendingPour_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.kegbot.proto.Internal.internal_static_PendingPour_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.kegbot.proto.Internal.PendingPour.class, org.kegbot.proto.Internal.PendingPour.Builder.class);
    }

    private int bitField0_;
    public static final int DRINK_REQUEST_FIELD_NUMBER = 1;
    private org.kegbot.proto.Api.RecordDrinkRequest drinkRequest_;
    /**
     * <code>required .RecordDrinkRequest drink_request = 1;</code>
     * @return Whether the drinkRequest field is set.
     */
    @java.lang.Override
    public boolean hasDrinkRequest() {
      return ((bitField0_ & 0x00000001) != 0);
    }
    /**
     * <code>required .RecordDrinkRequest drink_request = 1;</code>
     * @return The drinkRequest.
     */
    @java.lang.Override
    public org.kegbot.proto.Api.RecordDrinkRequest getDrinkRequest() {
      return drinkRequest_ == null ? org.kegbot.proto.Api.RecordDrinkRequest.getDefaultInstance() : drinkRequest_;
    }
    /**
     * <code>required .RecordDrinkRequest drink_request = 1;</code>
     */
    @java.lang.Override
    public org.kegbot.proto.Api.RecordDrinkRequestOrBuilder getDrinkRequestOrBuilder() {
      return drinkRequest_ == null ? org.kegbot.proto.Api.RecordDrinkRequest.getDefaultInstance() : drinkRequest_;
    }

    public static final int IMAGES_FIELD_NUMBER = 2;
    private com.google.protobuf.LazyStringList images_;
    /**
     * <code>repeated string images = 2;</code>
     * @return A list containing the images.
     */
    public com.google.protobuf.ProtocolStringList
        getImagesList() {
      return images_;
    }
    /**
     * <code>repeated string images = 2;</code>
     * @return The count of images.
     */
    public int getImagesCount() {
      return images_.size();
    }
    /**
     * <code>repeated string images = 2;</code>
     * @param index The index of the element to return.
     * @return The images at the given index.
     */
    public java.lang.String getImages(int index) {
      return images_.get(index);
    }
    /**
     * <code>repeated string images = 2;</code>
     * @param index The index of the value to return.
     * @return The bytes of the images at the given index.
     */
    public com.google.protobuf.ByteString
        getImagesBytes(int index) {
      return images_.getByteString(index);
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      if (!hasDrinkRequest()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!getDrinkRequest().isInitialized()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (((bitField0_ & 0x00000001) != 0)) {
        output.writeMessage(1, getDrinkRequest());
      }
      for (int i = 0; i < images_.size(); i++) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, images_.getRaw(i));
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) != 0)) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(1, getDrinkRequest());
      }
      {
        int dataSize = 0;
        for (int i = 0; i < images_.size(); i++) {
          dataSize += computeStringSizeNoTag(images_.getRaw(i));
        }
        size += dataSize;
        size += 1 * getImagesList().size();
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
      if (!(obj instanceof org.kegbot.proto.Internal.PendingPour)) {
        return super.equals(obj);
      }
      org.kegbot.proto.Internal.PendingPour other = (org.kegbot.proto.Internal.PendingPour) obj;

      if (hasDrinkRequest() != other.hasDrinkRequest()) return false;
      if (hasDrinkRequest()) {
        if (!getDrinkRequest()
            .equals(other.getDrinkRequest())) return false;
      }
      if (!getImagesList()
          .equals(other.getImagesList())) return false;
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
      if (hasDrinkRequest()) {
        hash = (37 * hash) + DRINK_REQUEST_FIELD_NUMBER;
        hash = (53 * hash) + getDrinkRequest().hashCode();
      }
      if (getImagesCount() > 0) {
        hash = (37 * hash) + IMAGES_FIELD_NUMBER;
        hash = (53 * hash) + getImagesList().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static org.kegbot.proto.Internal.PendingPour parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static org.kegbot.proto.Internal.PendingPour parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static org.kegbot.proto.Internal.PendingPour parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static org.kegbot.proto.Internal.PendingPour parseFrom(
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
    public static Builder newBuilder(org.kegbot.proto.Internal.PendingPour prototype) {
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
     * Protobuf type {@code PendingPour}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:PendingPour)
        org.kegbot.proto.Internal.PendingPourOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return org.kegbot.proto.Internal.internal_static_PendingPour_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return org.kegbot.proto.Internal.internal_static_PendingPour_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                org.kegbot.proto.Internal.PendingPour.class, org.kegbot.proto.Internal.PendingPour.Builder.class);
      }

      // Construct using org.kegbot.proto.Internal.PendingPour.newBuilder()
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
          getDrinkRequestFieldBuilder();
        }
      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        if (drinkRequestBuilder_ == null) {
          drinkRequest_ = null;
        } else {
          drinkRequestBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        images_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return org.kegbot.proto.Internal.internal_static_PendingPour_descriptor;
      }

      @java.lang.Override
      public org.kegbot.proto.Internal.PendingPour getDefaultInstanceForType() {
        return org.kegbot.proto.Internal.PendingPour.getDefaultInstance();
      }

      @java.lang.Override
      public org.kegbot.proto.Internal.PendingPour build() {
        org.kegbot.proto.Internal.PendingPour result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public org.kegbot.proto.Internal.PendingPour buildPartial() {
        org.kegbot.proto.Internal.PendingPour result = new org.kegbot.proto.Internal.PendingPour(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) != 0)) {
          if (drinkRequestBuilder_ == null) {
            result.drinkRequest_ = drinkRequest_;
          } else {
            result.drinkRequest_ = drinkRequestBuilder_.build();
          }
          to_bitField0_ |= 0x00000001;
        }
        if (((bitField0_ & 0x00000002) != 0)) {
          images_ = images_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.images_ = images_;
        result.bitField0_ = to_bitField0_;
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
        if (other instanceof org.kegbot.proto.Internal.PendingPour) {
          return mergeFrom((org.kegbot.proto.Internal.PendingPour)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(org.kegbot.proto.Internal.PendingPour other) {
        if (other == org.kegbot.proto.Internal.PendingPour.getDefaultInstance()) return this;
        if (other.hasDrinkRequest()) {
          mergeDrinkRequest(other.getDrinkRequest());
        }
        if (!other.images_.isEmpty()) {
          if (images_.isEmpty()) {
            images_ = other.images_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureImagesIsMutable();
            images_.addAll(other.images_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        if (!hasDrinkRequest()) {
          return false;
        }
        if (!getDrinkRequest().isInitialized()) {
          return false;
        }
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        org.kegbot.proto.Internal.PendingPour parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (org.kegbot.proto.Internal.PendingPour) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private org.kegbot.proto.Api.RecordDrinkRequest drinkRequest_;
      private com.google.protobuf.SingleFieldBuilderV3<
          org.kegbot.proto.Api.RecordDrinkRequest, org.kegbot.proto.Api.RecordDrinkRequest.Builder, org.kegbot.proto.Api.RecordDrinkRequestOrBuilder> drinkRequestBuilder_;
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       * @return Whether the drinkRequest field is set.
       */
      public boolean hasDrinkRequest() {
        return ((bitField0_ & 0x00000001) != 0);
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       * @return The drinkRequest.
       */
      public org.kegbot.proto.Api.RecordDrinkRequest getDrinkRequest() {
        if (drinkRequestBuilder_ == null) {
          return drinkRequest_ == null ? org.kegbot.proto.Api.RecordDrinkRequest.getDefaultInstance() : drinkRequest_;
        } else {
          return drinkRequestBuilder_.getMessage();
        }
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       */
      public Builder setDrinkRequest(org.kegbot.proto.Api.RecordDrinkRequest value) {
        if (drinkRequestBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          drinkRequest_ = value;
          onChanged();
        } else {
          drinkRequestBuilder_.setMessage(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       */
      public Builder setDrinkRequest(
          org.kegbot.proto.Api.RecordDrinkRequest.Builder builderForValue) {
        if (drinkRequestBuilder_ == null) {
          drinkRequest_ = builderForValue.build();
          onChanged();
        } else {
          drinkRequestBuilder_.setMessage(builderForValue.build());
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       */
      public Builder mergeDrinkRequest(org.kegbot.proto.Api.RecordDrinkRequest value) {
        if (drinkRequestBuilder_ == null) {
          if (((bitField0_ & 0x00000001) != 0) &&
              drinkRequest_ != null &&
              drinkRequest_ != org.kegbot.proto.Api.RecordDrinkRequest.getDefaultInstance()) {
            drinkRequest_ =
              org.kegbot.proto.Api.RecordDrinkRequest.newBuilder(drinkRequest_).mergeFrom(value).buildPartial();
          } else {
            drinkRequest_ = value;
          }
          onChanged();
        } else {
          drinkRequestBuilder_.mergeFrom(value);
        }
        bitField0_ |= 0x00000001;
        return this;
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       */
      public Builder clearDrinkRequest() {
        if (drinkRequestBuilder_ == null) {
          drinkRequest_ = null;
          onChanged();
        } else {
          drinkRequestBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       */
      public org.kegbot.proto.Api.RecordDrinkRequest.Builder getDrinkRequestBuilder() {
        bitField0_ |= 0x00000001;
        onChanged();
        return getDrinkRequestFieldBuilder().getBuilder();
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       */
      public org.kegbot.proto.Api.RecordDrinkRequestOrBuilder getDrinkRequestOrBuilder() {
        if (drinkRequestBuilder_ != null) {
          return drinkRequestBuilder_.getMessageOrBuilder();
        } else {
          return drinkRequest_ == null ?
              org.kegbot.proto.Api.RecordDrinkRequest.getDefaultInstance() : drinkRequest_;
        }
      }
      /**
       * <code>required .RecordDrinkRequest drink_request = 1;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<
          org.kegbot.proto.Api.RecordDrinkRequest, org.kegbot.proto.Api.RecordDrinkRequest.Builder, org.kegbot.proto.Api.RecordDrinkRequestOrBuilder> 
          getDrinkRequestFieldBuilder() {
        if (drinkRequestBuilder_ == null) {
          drinkRequestBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
              org.kegbot.proto.Api.RecordDrinkRequest, org.kegbot.proto.Api.RecordDrinkRequest.Builder, org.kegbot.proto.Api.RecordDrinkRequestOrBuilder>(
                  getDrinkRequest(),
                  getParentForChildren(),
                  isClean());
          drinkRequest_ = null;
        }
        return drinkRequestBuilder_;
      }

      private com.google.protobuf.LazyStringList images_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      private void ensureImagesIsMutable() {
        if (!((bitField0_ & 0x00000002) != 0)) {
          images_ = new com.google.protobuf.LazyStringArrayList(images_);
          bitField0_ |= 0x00000002;
         }
      }
      /**
       * <code>repeated string images = 2;</code>
       * @return A list containing the images.
       */
      public com.google.protobuf.ProtocolStringList
          getImagesList() {
        return images_.getUnmodifiableView();
      }
      /**
       * <code>repeated string images = 2;</code>
       * @return The count of images.
       */
      public int getImagesCount() {
        return images_.size();
      }
      /**
       * <code>repeated string images = 2;</code>
       * @param index The index of the element to return.
       * @return The images at the given index.
       */
      public java.lang.String getImages(int index) {
        return images_.get(index);
      }
      /**
       * <code>repeated string images = 2;</code>
       * @param index The index of the value to return.
       * @return The bytes of the images at the given index.
       */
      public com.google.protobuf.ByteString
          getImagesBytes(int index) {
        return images_.getByteString(index);
      }
      /**
       * <code>repeated string images = 2;</code>
       * @param index The index to set the value at.
       * @param value The images to set.
       * @return This builder for chaining.
       */
      public Builder setImages(
          int index, java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureImagesIsMutable();
        images_.set(index, value);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string images = 2;</code>
       * @param value The images to add.
       * @return This builder for chaining.
       */
      public Builder addImages(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureImagesIsMutable();
        images_.add(value);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string images = 2;</code>
       * @param values The images to add.
       * @return This builder for chaining.
       */
      public Builder addAllImages(
          java.lang.Iterable<java.lang.String> values) {
        ensureImagesIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, images_);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string images = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearImages() {
        images_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string images = 2;</code>
       * @param value The bytes of the images to add.
       * @return This builder for chaining.
       */
      public Builder addImagesBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureImagesIsMutable();
        images_.add(value);
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


      // @@protoc_insertion_point(builder_scope:PendingPour)
    }

    // @@protoc_insertion_point(class_scope:PendingPour)
    private static final org.kegbot.proto.Internal.PendingPour DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new org.kegbot.proto.Internal.PendingPour();
    }

    public static org.kegbot.proto.Internal.PendingPour getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    @java.lang.Deprecated public static final com.google.protobuf.Parser<PendingPour>
        PARSER = new com.google.protobuf.AbstractParser<PendingPour>() {
      @java.lang.Override
      public PendingPour parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new PendingPour(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<PendingPour> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PendingPour> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public org.kegbot.proto.Internal.PendingPour getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_PendingPour_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_PendingPour_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\016internal.proto\032\tapi.proto\032\014models.prot" +
      "o\"I\n\013PendingPour\022*\n\rdrink_request\030\001 \002(\0132" +
      "\023.RecordDrinkRequest\022\016\n\006images\030\002 \003(\tB\022\n\020" +
      "org.kegbot.proto"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          org.kegbot.proto.Api.getDescriptor(),
          org.kegbot.proto.Models.getDescriptor(),
        });
    internal_static_PendingPour_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_PendingPour_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_PendingPour_descriptor,
        new java.lang.String[] { "DrinkRequest", "Images", });
    org.kegbot.proto.Api.getDescriptor();
    org.kegbot.proto.Models.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
