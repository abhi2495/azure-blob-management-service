package com.example.tenancy;

public class TenantNotFoundException extends RuntimeException {

  public TenantNotFoundException(String tenantId) {
    super("No database yet configured for tenant " + tenantId);
  }

}
