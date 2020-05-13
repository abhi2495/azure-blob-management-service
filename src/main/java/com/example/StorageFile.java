package com.example;

public final class StorageFile {

  private String id;
  private String name;
  private long size;
  private String type;
  private String bucketName;
  private String source;
  private String referenceKey;
  private String referenceValue;
  private byte[] content;

  private StorageFile(Builder builder) {
    id = builder.id;
    name = builder.name;
    size = builder.size;
    type = builder.type;
    bucketName = builder.bucketName;
    source = builder.source;
    referenceKey = builder.referenceKey;
    referenceValue = builder.referenceValue;
    content = builder.content;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public long getSize() {
    return size;
  }

  public String getType() {
    return type;
  }

  public String getBucketName() {
    return bucketName;
  }

  public String getSource() {
    return source;
  }

  public String getReferenceKey() {
    return referenceKey;
  }

  public String getReferenceValue() {
    return referenceValue;
  }

  public byte[] getContent() {
    return content.clone();
  }

  public static final class Builder {
    private String id;
    private String name;
    private long size;
    private String type;
    private String bucketName;
    private String source;
    private String referenceKey;
    private String referenceValue;
    private byte[] content;

    private Builder() {
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withSize(long size) {
      this.size = size;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withBucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder withSource(String source) {
      this.source = source;
      return this;
    }

    public Builder withReferenceKey(String referenceKey) {
      this.referenceKey = referenceKey;
      return this;
    }

    public Builder withReferenceValue(String referenceValue) {
      this.referenceValue = referenceValue;
      return this;
    }

    public Builder withContent(byte[] content) {
      this.content = content.clone();
      return this;
    }

    public StorageFile build() {
      return new StorageFile(this);
    }

  }
}
