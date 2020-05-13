package com.example.service;

import static com.example.Constants.FILE_NAME_METADATA;
import static com.example.Constants.FILE_REF_KEY_METADATA;
import static com.example.Constants.FILE_REF_VAL_METADATA;
import static com.example.Constants.FILE_SIZE_METADATA;
import static com.example.Constants.FILE_SOURCE_METADATA;
import static com.example.Constants.FILE_TYPE_METADATA;
import static com.example.service.AzureBlobStorageProvider.BLOB_STORAGE_ACCESS_TIER;
import static com.example.service.AzureBlobStorageProvider.BLOCK_SIZE;
import static com.example.service.AzureBlobStorageProvider.COPY_STATUS_POLL_DURATION;
import static com.example.service.AzureBlobStorageProvider.DOWNLOAD_TIMEOUT;
import static com.example.service.AzureBlobStorageProvider.MAX_RETRY_DOWNLOAD_ATTEMPTS;
import static com.example.service.AzureBlobStorageProvider.NUM_PARALLEL_BUFFERS;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.AccessTier;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobDownloadResponse;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.example.FileCopyReference;
import com.example.NotFoundException;
import com.example.StorageFile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AzureBlobStorageService implements StorageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorageService.class);

  @Value("${azure.storage.accountName}")
  private String storageAccountName;

  @Value("${azure.storage.key}")
  private String storageAccountKey;

  @Value("${azure.storage.baseUrl}")
  private String storageBaseUrl;

  @Override
  public void checkIfFileExists(String bucketName, String fileId) throws NotFoundException {
    LOGGER.info("Inside AzureBlobStorageService checkIfFileExists");
    BlobServiceClient blobServiceClient = AzureBlobStorageProvider
        .getClient(storageAccountName, storageAccountKey, storageBaseUrl);
    if (StringUtils.isBlank(bucketName)) {
      LOGGER.error("storage container name is empty");
      throw new NotFoundException("storage container name is empty");
    }
    BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(bucketName);
    LOGGER.info("Checking if storage container {} already exists", bucketName);
    if (containerClient.exists()) {
      LOGGER.info("Container {} found", bucketName);
      if (StringUtils.isBlank(fileId)) {
        LOGGER.error("file id is empty");
        throw new NotFoundException("file id is empty");
      }
      BlobClient blobClient = containerClient.getBlobClient(fileId);
      LOGGER.info("Checking if file {} already exists", fileId);
      if (blobClient.exists()) {
        return;
      } else {
        LOGGER.error("File {} was not found.", fileId);
        throw new NotFoundException("File " + fileId + " not found");
      }
    } else {
      LOGGER.error("Container {} doesn't exist.", bucketName);
      throw new NotFoundException("Container " + bucketName + " not found");

    }
  }

  @Override
  /**
   * This method assumes bucket and file already exists in Azure Blob Storage Account (no 'exists' checks are
   * performed).
   */
  public Map<String, String> getFileProperties(String bucketName, String fileId) {

    BlobServiceClient blobServiceClient = AzureBlobStorageProvider.getClient(storageAccountName, storageAccountKey,
        storageBaseUrl);
    BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(bucketName);
    BlobClient blobClient = containerClient.getBlobClient(fileId);
    BlobProperties blobProperties = blobClient.getProperties();
    Map<String, String> blobMetadata = new HashMap<>();
    blobMetadata.put(FILE_SIZE_METADATA, String.valueOf(blobProperties.getBlobSize()));
    blobMetadata.putAll(blobProperties.getMetadata());
    return blobMetadata;

  }

  @Override
  public void delete(String bucketName, String fileId) throws NotFoundException {
    LOGGER.info("Inside AzureBlobStorageService delete");
    BlobServiceClient blobServiceClient = AzureBlobStorageProvider.getClient(storageAccountName, storageAccountKey,
        storageBaseUrl);
    if (StringUtils.isBlank(bucketName)) {
      LOGGER.error("storage container name is empty");
      throw new NotFoundException("storage container name is empty");
    }
    BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(bucketName);
    LOGGER.info("Checking if storage container {} already exists", bucketName);
    if (containerClient.exists()) {
      LOGGER.info("Container {} found", bucketName);
      if (StringUtils.isBlank(fileId)) {
        LOGGER.error("file id is empty");
        throw new NotFoundException("file id is empty");
      }
      BlobClient blobClient = containerClient.getBlobClient(fileId);
      LOGGER.info("Checking if file {} already exists", fileId);
      if (blobClient.exists()) {
        Response<Void> response = blobClient.deleteWithResponse(null, null,
            null, Context.NONE);
        if (response.getStatusCode() == HttpStatus.ACCEPTED.value()) {
          LOGGER.info("File {} was deleted successfully.", fileId);
        } else {
          LOGGER.error("Response from azure blob storage service:" + response.getStatusCode());
        }

      } else {
        LOGGER.error("File {} was not found. So ignoring delete operation.", fileId);
        throw new NotFoundException("File " + fileId + " not found during delete blob operation");
      }
    } else {
      LOGGER.error("Container {} doesn't exist. So ignoring delete blob operation.", bucketName);
      throw new NotFoundException("Container " + bucketName + " not found during delete blob operation");
    }
  }

  @Override
  public void deleteAsync(String bucketName, String fileId) {
    LOGGER.info("Inside AzureBlobStorageService delete");
    BlobServiceAsyncClient blobServiceAsyncClient = AzureBlobStorageProvider.getAysncClient(storageAccountName,
        storageAccountKey, storageBaseUrl);
    BlobContainerAsyncClient blobContainerAsyncClient = blobServiceAsyncClient.getBlobContainerAsyncClient(bucketName);
    LOGGER.info("Checking if storage container {} already exists", bucketName);
    blobContainerAsyncClient.exists().subscribe(doesContainerExist -> {
      if (doesContainerExist) {
        LOGGER.info("Container exists");
        BlobAsyncClient blobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(fileId);
        LOGGER.info("Checking if file {} already exists", fileId);
        blobAsyncClient.exists().subscribe(doesBlobExist -> {
          if (doesBlobExist) {
            LOGGER.info("File {} exists, deleting asynchronously", fileId);
            blobAsyncClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null)
                .subscribe(
                    response -> LOGGER.info("Delete completed with status {}", response.getStatusCode()),
                    error -> LOGGER.error(error.getMessage(), error));
          } else {
            LOGGER.error("File {} doesn't exist, so ignoring delete operation", fileId);
          }
        });
      } else {
        LOGGER.error("Container {} does not exist, so ignoring delete blob operation", bucketName);
      }
    });
  }

  //CSOFF: MagicNumber

  @Override
  /**
   * This method assumes bucket and file already exists in Azure Blob Storage Account (no 'exists' checks are
   * performed).
   */
  public void downloadToOutputStream(String bucketName, String fileId, OutputStream outputStream) {
    LOGGER.info("Inside AzureBlobStorageService download by fileId");
    BlobServiceClient blobServiceClient = AzureBlobStorageProvider.getClient(storageAccountName, storageAccountKey,
        storageBaseUrl);
    BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(bucketName);
    BlobClient blobClient = containerClient.getBlobClient(fileId);
    DownloadRetryOptions options = new DownloadRetryOptions().setMaxRetryRequests(MAX_RETRY_DOWNLOAD_ATTEMPTS);
    BlobDownloadResponse response = blobClient.downloadWithResponse(outputStream, null, options,
        null, false, DOWNLOAD_TIMEOUT, Context.NONE);
    LOGGER.info("Completed download to output stream with status {}", response.getStatusCode());

  }
  //CSON: MagicNumber

  @Override
  public void upload(StorageFile storageFile) throws NotFoundException {
    LOGGER.info("Inside AzureBlobStorageService upload storageFile");

    BlobServiceClient blobServiceClient = AzureBlobStorageProvider.getClient(storageAccountName, storageAccountKey,
        storageBaseUrl);
    if (StringUtils.isBlank(storageFile.getBucketName())) {
      LOGGER.error("storage container name is empty");
      throw new NotFoundException("storage container name is empty");
    }
    LOGGER.info("Checking if container {} already exists", storageFile.getBucketName());
    BlobContainerClient container = blobServiceClient.getBlobContainerClient(storageFile.getBucketName());
    boolean doesContainerExist = container.exists();
    if (!doesContainerExist) {
      LOGGER.info("Container {} does not exist, so creating it.", storageFile.getBucketName());
      container.create();
    } else {
      LOGGER.info("Container exists.");
    }
    BlobClient blobClient = container.getBlobClient(storageFile.getId());
    LOGGER.info("Checking if file {} already exists", storageFile.getId());
    if (!blobClient.exists()) {
      LOGGER.info("Starting file upload..");
      BlockBlobClient blockBlobClient = blobClient.getBlockBlobClient();
      byte[] fileContent = storageFile.getContent();
      Map<String, String> metadata = constructMetdataMapFromStorageFile(storageFile);
      long startTime = System.currentTimeMillis();
      Response<BlockBlobItem> response = blockBlobClient.uploadWithResponse(new ByteArrayInputStream(fileContent),
          fileContent.length, null, metadata, AccessTier.HOT, null, null, null, null);
      long endTime = System.currentTimeMillis();
      if (response.getStatusCode() == HttpStatus.CREATED.value()) {
        LOGGER.info("File Successfully uploaded. Size {} bytes", storageFile.getSize());
        LOGGER.info("Time taken: {}ms", (endTime - startTime));
      } else {
        LOGGER.error("Response from azure blob storage service:" + response.getStatusCode());
      }
    } else {
      LOGGER.error("File already exists, so ignoring any upload.");
    }

  }

  @Override
  public void uploadAsync(StorageFile storageFile) {
    LOGGER.info("Inside AzureBlobStorageService upload async storageFile");
    BlobServiceAsyncClient blobServiceAsyncClient = AzureBlobStorageProvider.getAysncClient(storageAccountName,
        storageAccountKey, storageBaseUrl);
    BlobContainerAsyncClient blobContainerClient = blobServiceAsyncClient
        .getBlobContainerAsyncClient(storageFile.getBucketName());
    LOGGER.info("Checking if storage container {} already exists", storageFile.getBucketName());
    blobContainerClient.exists().subscribe(doesContainerExist -> {
      if (!doesContainerExist) {
        LOGGER.info("Container does not exist, so creating it before uploading file");
        blobContainerClient.create().subscribe(null, error -> {
          LOGGER.error(error.getMessage(), error);
        }, new Runnable() {
          @Override
          public void run() {
            LOGGER.info("Container created successfully");
            uploadFileAsync(blobContainerClient, storageFile);
          }

        });
      } else {
        LOGGER.info("Container exists.");
        uploadFileAsync(blobContainerClient, storageFile);
      }
    });
  }

  //CSOFF: MagicNumber
  private void uploadFileAsync(BlobContainerAsyncClient blobContainerClient, StorageFile storageFile) {
    BlobAsyncClient blobClient = blobContainerClient.getBlobAsyncClient(storageFile.getId());
    LOGGER.info("Checking if file {} already exists", storageFile.getId());
    blobClient.exists().subscribe(doesBlobExist -> {
      if (!doesBlobExist) {
        LOGGER.info("File doesnt exist, so uploading asynchronously..");
        long totalFileSize = storageFile.getSize();
        ParallelTransferOptions parallelTransferOptions = new ParallelTransferOptions(BLOCK_SIZE, NUM_PARALLEL_BUFFERS,
            bytesTransferred -> {
              float percentage = (((float) bytesTransferred) / ((float) totalFileSize)) * 100;
              LOGGER.info("Upload progress: {}%", percentage);
            });
        Flux<ByteBuffer> data = Flux.just(ByteBuffer.wrap(storageFile.getContent()));
        Map<String, String> metadata = constructMetdataMapFromStorageFile(storageFile);
        LOGGER.info("File upload starting...");
        long startTime = System.currentTimeMillis();
        blobClient.uploadWithResponse(data, parallelTransferOptions, null, metadata,
            BLOB_STORAGE_ACCESS_TIER, null)
            .subscribe(
                response -> {
                  long endTime = System.currentTimeMillis();
                  LOGGER.info("File Successfully uploaded. Size {} bytes.", storageFile.getSize());
                  LOGGER.info("Time taken: {}ms", (endTime - startTime));
                },
                error -> LOGGER.error(error.getMessage(), error));

      } else {
        LOGGER.error("File already exists, so ignoring any upload.");
      }
    });

  }
  //CSON: MagicNumber

  @Override
  public FileCopyReference[] copy(String bucketName, List<String> fileIds) throws NotFoundException {
    LOGGER.info("Inside AzureBlobStorageService copy");
    final BlobServiceClient blobServiceClient = AzureBlobStorageProvider.getClient(storageAccountName,
        storageAccountKey, storageBaseUrl);
    if (StringUtils.isBlank(bucketName)) {
      LOGGER.error("storage container name is empty");
      throw new NotFoundException("storage container name is empty");
    }
    final BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(bucketName);
    LOGGER.info("Checking if storage container {} already exists", bucketName);
    if (containerClient.exists()) {
      LOGGER.info("Container {} found", bucketName);
      List<FileCopyReference> fileCopyReferenceList = new ArrayList<>();
      for (String fileId : fileIds) {
        String urlDecodedFileId = "";
        try {
          urlDecodedFileId = URLDecoder.decode(fileId, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
          urlDecodedFileId = fileId;
        }
        if (StringUtils.isBlank(urlDecodedFileId)) {
          LOGGER.error("File id is empty");
        }
        BlobClient sourceBlobClient = containerClient.getBlobClient(urlDecodedFileId);
        LOGGER.info("Checking if file {} already exists", urlDecodedFileId);
        if (sourceBlobClient.exists()) {
          LOGGER.info("File {} exists, starting copy operation", urlDecodedFileId);
          String copiedFileId = UUID.randomUUID().toString();
          BlobClient copyBlobClient = containerClient.getBlobClient(copiedFileId);
          LOGGER.info("Source file url: {}", sourceBlobClient.getBlobUrl());
          final SyncPoller<BlobCopyInfo, Void> poller = copyBlobClient.beginCopy(sourceBlobClient.getBlobUrl(),
              COPY_STATUS_POLL_DURATION);
          PollResponse<BlobCopyInfo> pollResponse = poller.poll();

          LOGGER.info("Copy id: {} ,status: {} for source file: {} and destination file: {}",
              pollResponse.getValue().getCopyId(),
              pollResponse.getValue().getCopyStatus().toString(),
              urlDecodedFileId,
              copiedFileId);

          fileCopyReferenceList.add(FileCopyReference.newBuilder().withActualFileId(urlDecodedFileId)
              .withCopiedFileId(copiedFileId).build());

        } else {
          LOGGER.error("File {} was not found. So ignoring its copy operation.", urlDecodedFileId);
        }
      }
      if (fileCopyReferenceList.isEmpty()) {
        throw new NotFoundException("None of the files were found. So no copy happened.");
      } else {
        FileCopyReference[] fileCopyReferences = new FileCopyReference[fileCopyReferenceList.size()];
        return fileCopyReferenceList.toArray(fileCopyReferences);
      }
    } else {
      LOGGER.error("Container {} doesn't exist. So ignoring copy blob operation.", bucketName);
      throw new NotFoundException("Container " + bucketName + " not found during copy blob operation");

    }
  }

  private Map<String, String> constructMetdataMapFromStorageFile(StorageFile storageFile) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(FILE_NAME_METADATA, storageFile.getName());
    metadata.put(FILE_TYPE_METADATA, storageFile.getType());
    if (storageFile.getSource() != null) {
      metadata.put(FILE_SOURCE_METADATA, storageFile.getSource());
    }
    if (storageFile.getReferenceKey() != null) {
      metadata.put(FILE_REF_KEY_METADATA, storageFile.getReferenceKey());
    }
    if (storageFile.getReferenceValue() != null) {
      metadata.put(FILE_REF_VAL_METADATA, storageFile.getReferenceValue());
    }
    return metadata;
  }
}