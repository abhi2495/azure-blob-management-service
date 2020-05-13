package com.example.tenancy;

public final class Tenant {

  public static final String DEFAULT_TENANT_ID = "public";
  private final String id;

  private final String alias;

  private final boolean active;

  public Tenant(String alias) {
    this(null, alias, false);
  }

  public Tenant(String id, String alias, boolean active) {
    this.id = id;
    this.alias = alias;
    this.active = active;
  }

  public String getId() {
    return id;
  }

  public String getAlias() {
    return alias;
  }

  public boolean isActive() {
    return active;
  }
}
