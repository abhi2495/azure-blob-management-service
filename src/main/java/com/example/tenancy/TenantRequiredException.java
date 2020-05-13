package com.example.tenancy;

public class TenantRequiredException extends RuntimeException {

  public TenantRequiredException() {
    super("Valid tenant is required");
  }
}
