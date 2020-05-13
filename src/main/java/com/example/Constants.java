package com.example;

import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Constants {
  static {
    Map<String, String> map = new HashMap<>();

    map.put(MediaType.APPLICATION_JSON_VALUE, ".json");
    map.put(MediaType.TEXT_PLAIN_VALUE, ".txt");
    map.put(MediaType.TEXT_XML_VALUE, ".xml");
    map.put("text/csv", ".csv");
    map.put(MediaType.APPLICATION_PDF_VALUE, ".pdf");
    map.put(MediaType.IMAGE_GIF_VALUE, ".gif");
    map.put(MediaType.IMAGE_JPEG_VALUE, ".jpg");
    map.put(MediaType.IMAGE_PNG_VALUE, ".png");
    map.put(MediaType.APPLICATION_XML_VALUE, ".xml");
    map.put(MediaType.APPLICATION_OCTET_STREAM_VALUE, ".bin");

    CONTENT_TYPE_TO_FILE_TYPE_MAP = Collections.unmodifiableMap(map);
  }

  public static final String FILE_TYPE_METADATA = "filetype";
  public static final String FILE_SOURCE_METADATA = "filesource";
  public static final String FILE_NAME_METADATA = "filename";
  public static final String FILE_REF_KEY_METADATA = "filereferencekey";
  public static final String FILE_REF_VAL_METADATA = "filereferencevalue";
  public static final String FILE_SIZE_METADATA = "filesize";

  public static final Map<String, String> CONTENT_TYPE_TO_FILE_TYPE_MAP;
  public static final String COPY_FILE_IDS = "ids";

  public static final String ASYNC_HEADER = "isAsync";
  public static final String FILE_NAME_HEADER = "x-file-name";
  public static final String FILE_SOURCE_HEADER = "x-file-source";
  public static final String FILE_REFERENCE_KEY_HEADER = "x-reference-key";
  public static final String FILE_REFERENCE_VALUE_HEADER = "x-reference-value";

  private Constants() {

  }
}