package com.example.service;

import com.example.FileCopyReference;
import com.example.NotFoundException;
import com.example.StorageFile;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface StorageService {

  void checkIfFileExists(String bucketName, String fileId) throws NotFoundException;

  Map<String, String> getFileProperties(String bucketName, String fileId);

  void delete(String bucketName, String fileName) throws NotFoundException;

  void deleteAsync(String bucketName, String fileName);

  void downloadToOutputStream(String bucketName, String fileName, OutputStream outputStream);

  void upload(StorageFile storageFile) throws NotFoundException;

  void uploadAsync(StorageFile storageFile);

  FileCopyReference[] copy(String bucketName, List<String> fileIds) throws NotFoundException;
}
