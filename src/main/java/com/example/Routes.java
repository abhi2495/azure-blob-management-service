package com.example;


public final class Routes {
  private static final String TENANT = "/{tenantId}";

  private static final String API = "/api";
  private static final String API_V1 = API + "/v1";
  private static final String TENANT_API_V1 = TENANT + API_V1;
  public static final String FILE_API_V1 = TENANT_API_V1 + "/file";
  public static final String FILE_API_V1_ID = TENANT_API_V1 + "/file/{id}";
  public static final String FILE_API_V1_COPY = TENANT_API_V1 + "/copyFile";
  public static final String GREET_API = TENANT_API_V1 + "/greet";
  private Routes() {
  }
}
