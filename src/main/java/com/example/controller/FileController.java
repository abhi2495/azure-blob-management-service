package com.example.controller;

import static com.example.Constants.ASYNC_HEADER;
import static com.example.Constants.CONTENT_TYPE_TO_FILE_TYPE_MAP;
import static com.example.Constants.COPY_FILE_IDS;
import static com.example.Constants.FILE_NAME_HEADER;
import static com.example.Constants.FILE_NAME_METADATA;
import static com.example.Constants.FILE_REFERENCE_KEY_HEADER;
import static com.example.Constants.FILE_REFERENCE_VALUE_HEADER;
import static com.example.Constants.FILE_REF_KEY_METADATA;
import static com.example.Constants.FILE_REF_VAL_METADATA;
import static com.example.Constants.FILE_SIZE_METADATA;
import static com.example.Constants.FILE_SOURCE_HEADER;
import static com.example.Constants.FILE_SOURCE_METADATA;
import static com.example.Constants.FILE_TYPE_METADATA;

import com.example.FileCopyReference;
import com.example.NotFoundException;
import com.example.Routes;
import com.example.StorageFile;
import com.example.service.StorageService;
import com.example.tenancy.TenancyContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class FileController {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);
  private final StorageService storageService;

  @Autowired
  public FileController(StorageService storageService) {
    this.storageService = storageService;
  }

  @PostMapping(Routes.FILE_API_V1)
  public ResponseEntity<String> saveFile(
      @RequestBody byte[] fileContents,
      @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
      @RequestHeader(value = ASYNC_HEADER, required = false) boolean isAsync,
      @NotEmpty @RequestHeader(value = FILE_NAME_HEADER) String displayName,
      @RequestHeader(value = FILE_SOURCE_HEADER, required = false) String fileSource,
      @RequestHeader(value = FILE_REFERENCE_KEY_HEADER, required = false) String referenceKey,
      @RequestHeader(value = FILE_REFERENCE_VALUE_HEADER, required = false) String referenceValue) throws NotFoundException {

    String fileId = UUID.randomUUID().toString();
    StorageFile file = StorageFile.builder()
        .withId(fileId)
        .withName(displayName)
        .withSize(fileContents.length)
        .withType(contentType)
        .withBucketName(getBucketName())
        .withSource(fileSource)
        .withReferenceKey(referenceKey)
        .withReferenceValue(referenceValue)
        .withContent(fileContents)
        .build();
    if (isAsync) {
      storageService.uploadAsync(file);
    } else {
      storageService.upload(file);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(fileId);
  }

  @GetMapping(Routes.FILE_API_V1_ID)
  public ResponseEntity<StreamingResponseBody> getFile(@NotEmpty @PathVariable("id") String fileId) throws NotFoundException {
    String bucketName = getBucketName();
    storageService.checkIfFileExists(bucketName, fileId);
    LOGGER.info("File {} found, downloading it's metadata", fileId);
    Map<String, String> fileProperties = storageService.getFileProperties(bucketName, fileId);
    HttpHeaders httpHeaders = new HttpHeaders();
    fileProperties.forEach((key, value) -> {
      switch (key) {
        case FILE_TYPE_METADATA:
          httpHeaders.add(HttpHeaders.CONTENT_TYPE, value);
          break;
        case FILE_SIZE_METADATA:
          httpHeaders.add(HttpHeaders.CONTENT_LENGTH, value);
          break;
        case FILE_SOURCE_METADATA:
          httpHeaders.add(FILE_SOURCE_HEADER, value);
          break;
        case FILE_NAME_METADATA:
          httpHeaders.add(FILE_NAME_HEADER, value);
          break;
        case FILE_REF_KEY_METADATA:
          httpHeaders.add(FILE_REFERENCE_KEY_HEADER, value);
          break;
        case FILE_REF_VAL_METADATA:
          httpHeaders.add(FILE_REFERENCE_VALUE_HEADER, value);
          break;
        default:
          break;
      }
    });
    String contentDispositionHeader =
        "attachment; filename=" + fileProperties.get(FILE_NAME_METADATA) + CONTENT_TYPE_TO_FILE_TYPE_MAP.get(fileProperties.get(FILE_TYPE_METADATA));
    httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, contentDispositionHeader);
    LOGGER.info("Now downloading actual content of file {}", fileId);
    StreamingResponseBody streamingResponseBody = outputStream -> {
      storageService.downloadToOutputStream(bucketName, fileId, outputStream);
    };

    return new ResponseEntity<>(streamingResponseBody, httpHeaders, HttpStatus.OK);
  }

  @DeleteMapping(Routes.FILE_API_V1_ID)
  public ResponseEntity deleteFile(
      @NotEmpty @PathVariable("id") String fileId,
      @RequestHeader(value = ASYNC_HEADER, required = false) boolean isAsync) throws NotFoundException {
    if (isAsync) {
      storageService.deleteAsync(getBucketName(), fileId);
    } else {
      storageService.delete(getBucketName(), fileId);
    }
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PostMapping(Routes.FILE_API_V1_COPY)
  public ResponseEntity<FileCopyReference[]> copyFile(
      @RequestParam(value = COPY_FILE_IDS) List<String> fileIds) throws NotFoundException {
    return ResponseEntity.ok(storageService.copy(getBucketName(), fileIds));
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity handleIoException(IOException ioException) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ioException.getMessage());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity handleNotFoundException(NotFoundException notFoundException) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundException.getMessage());
  }

  private String getBucketName() {
    String bucketName = TenancyContextHolder.getContext().getTenantId();
    LOGGER.info("Bucket name is {}", bucketName);
    return bucketName;
  }
}
