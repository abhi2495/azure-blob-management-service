package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = FileCopyReference.Builder.class)
public final class FileCopyReference {

  private String actualFileId;
  private String copiedFileId;

  private FileCopyReference(Builder builder) {
    actualFileId = builder.actualFileId;
    copiedFileId = builder.copiedFileId;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public String getCopiedFileId() {
    return copiedFileId;
  }

  public String getActualFileId() {
    return actualFileId;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Builder {
    private String actualFileId;
    private String copiedFileId;

    private Builder() {
    }

    public Builder withActualFileId(String actualFileId) {
      this.actualFileId = actualFileId;
      return this;
    }

    public Builder withCopiedFileId(String copiedFileId) {
      this.copiedFileId = copiedFileId;
      return this;
    }

    public FileCopyReference build() {
      return new FileCopyReference(this);
    }
  }
}