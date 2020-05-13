package com.example.service;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.AccessTier;
import com.azure.storage.common.StorageSharedKeyCredential;

import java.time.Duration;
import java.util.Locale;

public final class AzureBlobStorageProvider {

  public static final int BLOCK_SIZE = 512 * 1024; //512 KB
  public static final int NUM_PARALLEL_BUFFERS = 4;
  public static final AccessTier BLOB_STORAGE_ACCESS_TIER = AccessTier.HOT;
  public static final int MAX_RETRY_DOWNLOAD_ATTEMPTS = 5;
  public static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(180);//3 minutes
  public static final Duration COPY_STATUS_POLL_DURATION = Duration.ofSeconds(2);

  private AzureBlobStorageProvider() {
  }

  public static BlobServiceAsyncClient getAysncClient(String accountName, String accountKey, String storageBaseUrl) {
    return getClientBuilder(accountName, accountKey, storageBaseUrl).buildAsyncClient();
  }

  public static BlobServiceClient getClient(String accountName, String accountKey, String storageBaseUrl) {
    return getClientBuilder(accountName, accountKey, storageBaseUrl).buildClient();
  }

  private static BlobServiceClientBuilder getClientBuilder(String accountName, String accountKey, String storageBaseUrl) {
    StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);
    String endPoint = String.format(Locale.ROOT, storageBaseUrl, accountName);
    return new BlobServiceClientBuilder().endpoint(endPoint).credential(credential);
  }
}
