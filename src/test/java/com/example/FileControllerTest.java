package com.example;

import static com.example.Constants.COPY_FILE_IDS;
import static com.example.Constants.FILE_NAME_HEADER;
import static com.example.Constants.FILE_REFERENCE_KEY_HEADER;
import static com.example.Constants.FILE_REFERENCE_VALUE_HEADER;
import static com.example.Constants.FILE_SOURCE_HEADER;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.example.service.AzureBlobStorageProvider;
import com.example.tenancy.TenancyContext;
import com.example.tenancy.TenancyContextHolder;
import com.example.tenancy.Tenant;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

//CSOFF: MagicNumber
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = Application.class)
@ActiveProfiles(Application.Profiles.NO_DEPENDENCIES)
public class FileControllerTest {

  private static final String TENANT_ID = "x-foo";
  private static final Logger LOGGER = LoggerFactory.getLogger(FileControllerTest.class);
  private static Process process;

  @Value("${azure.storage.accountName}")
  private String storageAccountName;

  @Value("${azure.storage.key}")
  private String storageAccountKey;

  @Value("${azure.storage.baseUrl}")
  private String storageBaseUrl;

  @BeforeClass
  public static void setUp() throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command("bash", "-c", "npm run azurite-start");
    LOGGER.info("*********Starting azurite emulator***********");
    process = processBuilder.start();
    setTenantContext();
  }

  @AfterClass
  public static void tearDown() throws InterruptedException {
    LOGGER.info("*********Stopping azurite emulator***********");
    process.destroy();                     // tell the process to stop
    process.waitFor(10, TimeUnit.SECONDS); // give it a chance to stop
    process.destroyForcibly();             // tell the OS to kill the process
    process.waitFor();// the process is now dead
  }

  @Test
  public void test_FileOperations() {
    LOGGER.info("Executing test_FileOperations ");
    String fileContent = "Hello from Integration Test!!";
    byte[] fileContentBytes = fileContent.getBytes(Charset.defaultCharset());
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
    headers.add(FILE_NAME_HEADER, "dummyfile");
    headers.add(FILE_SOURCE_HEADER, "dummysource");
    headers.add(FILE_REFERENCE_KEY_HEADER, "dummyrefkey");
    headers.add(FILE_REFERENCE_VALUE_HEADER, "dummrefval");

    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<String> uploadFileApiResponse = restTemplate.exchange(getFileUrl(),
        HttpMethod.POST,
        new HttpEntity<>(fileContentBytes, headers),
        String.class);

    Assert.assertEquals(HttpStatus.CREATED, uploadFileApiResponse.getStatusCode());
    String fileId = uploadFileApiResponse.getBody();

    ResponseEntity<String> downloadFileApiResponse = restTemplate.getForEntity(getFileIdUrl(fileId),
        String.class);

    Assert.assertEquals(HttpStatus.OK, downloadFileApiResponse.getStatusCode());
    Assert.assertEquals(fileContent, downloadFileApiResponse.getBody());

    HttpHeaders responseHeaders = downloadFileApiResponse.getHeaders();

    Assert.assertEquals(MediaType.TEXT_PLAIN_VALUE, responseHeaders.get(HttpHeaders.CONTENT_TYPE).get(0));
    Assert.assertEquals("dummyfile", responseHeaders.get(FILE_NAME_HEADER).get(0));
    Assert.assertEquals("dummysource", responseHeaders.get(FILE_SOURCE_HEADER).get(0));
    Assert.assertEquals("dummyrefkey", responseHeaders.get(FILE_REFERENCE_KEY_HEADER).get(0));
    Assert.assertEquals("dummrefval", responseHeaders.get(FILE_REFERENCE_VALUE_HEADER).get(0));

    ResponseEntity<FileCopyReference[]> fileCopyResponse =
        restTemplate.exchange(UriComponentsBuilder.fromUriString(getFileCopyUrl())
                .queryParam(COPY_FILE_IDS, fileId).toUriString(),
            HttpMethod.POST,
            null,
            FileCopyReference[].class);

    Assert.assertEquals(HttpStatus.OK, fileCopyResponse.getStatusCode());
    FileCopyReference[] fileCopyReferences = fileCopyResponse.getBody();
    Assert.assertEquals(1, fileCopyReferences.length);
    FileCopyReference fileCopyReference = fileCopyReferences[0];
    Assert.assertEquals(fileId, fileCopyReference.getActualFileId());

    String copiedFileId = fileCopyReference.getCopiedFileId();

    ResponseEntity<Void> deleteFileApiResponse1 = restTemplate.exchange(getFileIdUrl(fileId),
        HttpMethod.DELETE, null, Void.class);

    Assert.assertEquals(HttpStatus.NO_CONTENT, deleteFileApiResponse1.getStatusCode());

    ResponseEntity<Void> deleteFileApiResponse2 = restTemplate.exchange(getFileIdUrl(copiedFileId),
        HttpMethod.DELETE, null, Void.class);

    Assert.assertEquals(HttpStatus.NO_CONTENT, deleteFileApiResponse2.getStatusCode());

  }

  private String getFileUrl() {
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance().scheme("http")
        .host("localhost").port(8080);
    return
        uriComponentsBuilder.path(Routes.FILE_API_V1).buildAndExpand(TENANT_ID).toUriString();
  }

  private String getFileIdUrl(String fileId) {
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance().scheme("http")
        .host("localhost").port(8080);
    return uriComponentsBuilder.path(Routes.FILE_API_V1_ID).buildAndExpand(TENANT_ID, fileId).toUriString();
  }

  private String getFileCopyUrl() {
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance().scheme("http")
        .host("localhost").port(8080);
    return uriComponentsBuilder.path(Routes.FILE_API_V1_COPY).buildAndExpand(TENANT_ID).toUriString();
  }

  private void createBucket() {

    final BlobServiceClient blobServiceClient = AzureBlobStorageProvider.getClient(storageAccountName,
        storageAccountKey, storageBaseUrl);
    String bucketName = TenancyContextHolder.getContext().getTenantId();
    LOGGER.info("Trying to create bucket {}", bucketName);
    BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(bucketName);
    try {
      containerClient.create();
    } catch (BlobStorageException exception) {
      LOGGER.info("container exists, proceeding with test..");
    }
  }

  private static void setTenantContext() {
    TenancyContextHolder.setContext(TenancyContext.newContext(new Tenant(TENANT_ID, TENANT_ID, true)));
  }
}
//CSON: MagicNumber
