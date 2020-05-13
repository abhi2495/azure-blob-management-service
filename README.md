# azure-blob-management-service

This application exposes REST APIs to perform the basic file operations (upload,download,delete,copy) 
in Azure Blob Storage Account. We have followed **container per tenant** principal here i.e for each tenant,
there is a dedicated container inside which blobs related to that tenant are stored.

The [**v12 azure sdk for java**](https://github.com/Azure/azure-sdk-for-java/tree/master/sdk/storage/azure-storage-blob) is being used is here.
 
For integration testing, the storage emulator [**Azurite**](https://github.com/Azure/Azurite) has been leveraged.


<br>
<br>

## API documentation

- [Upload](#Upload)
- [Download](#Download)
- [Delete](#Delete)
- [Copy](#Copy)


#### Uploading file <a name="Upload"/>

##### Endpoint

    POST {tenantId}/api/v1/file
    Host: http://example.com

##### Request Body

    Byte array of the file content
    
##### Request Headers

    - Content-Type      : [Required](String) File type. eg. application/json, text/plain, text/xml, text/csv, application/pdf, image/gif, image/jpeg, image/png, application/xml, application/octet-stream
    - isAsync           : [Optional](Boolean) Whether to use async api for asynchronous upload
    - x-file-name       : [Required](String) Non-empty string representing name of the file
    - x-file-source     : [Optional](String) Source of the file
    - x-reference-key   : [Optional](String) Reference key for the file
    - x-reference-value : [Optional](String) Reference value for the file

##### Corresponding response

    HTTP/1.1 201 Created
    Content-Type: text/plain

    <file-identifier>
    
    
**Note -**

    - The file identifier returned in response can be used to download, delete or copy the blob
    - If storage container for the specified tenant is not present, then it will be created before blob is created inside it
    
<br>
<br>

#### Downloading file <a name="Download"/>

##### Endpoint

    GET {tenantId}/api/v1/file/{id}
    Host: http://example.com
    

##### Corresponding response

    HTTP/1.1 200 OK
    Content-Disposition: <file-type>

    <streaming response>
    
**Note -**

    - Errors out if there is no storage container corresponding to the tenant in azure
    
<br>
<br>

#### Deleting file <a name="Delete"/>

##### Endpoint

    DELETE {tenantId}/api/v1/file/{id}
    Host: http://example.com
    
##### Request Headers

    - isAsync : [Optional](Boolean) Whether to use async api for asynchronous delete

##### Corresponding response

    HTTP/1.1 204 No Content

**Note -**

    - Errors out if there is no storage container corresponding to the tenant in azure
    
<br>
<br>


#### Copying file(s) <a name="Copy"/>

##### Endpoint

    POST {tenantId}/api/v1/copyFile?ids=file1,file2
    Host: http://example.com
    
##### Request Params

    - ids : [Required](String) Comma separated list of one or more file ids which have to be copied

##### Corresponding response

    HTTP/1.1 200 OK
    Content-Type: application/json
    
    [
      {
        "actualFileId" : "file-1",
        "copiedFileId" : "file-1-copy"
      },
      ...
    ]  
    
**Note -**

    - Errors out if there is no storage container corresponding to the tenant in azure