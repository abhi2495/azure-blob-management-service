package com.example.tenancy;

import java.util.Objects;

public final class TenancyContext {

  private static final TenancyContext EMPTY_CONTEXT = new TenancyContext(null, true);

  private final Tenant tenant;

  private final boolean valid;

  private TenancyContext(Tenant tenant, boolean valid) {
    this.tenant = tenant;
    this.valid = valid;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public boolean hasTenant() {
    return tenant != null;
  }

  public boolean isValid() {
    return valid && (tenant == null || tenant.isActive());
  }

  public String getTenantId() throws TenantRequiredException {
    if (hasTenant()) {
      if (isValid()) {
        return tenant.getId();
      } else {
        throw new TenantRequiredException();
      }
    } else {
      return null;
    }
  }

  public boolean isTenantIdEqualTo(String otherTenantId) {
    try {
      return Objects.equals(getTenantId(), otherTenantId);
    } catch (TenantRequiredException e) {
      return false;
    }
  }

  public static TenancyContext newContext(Tenant tenant) {
    if (tenant == null) {
      throw new IllegalArgumentException("Tenant must not be null.");
    }
    return new TenancyContext(tenant, true);
  }

  public static TenancyContext invalidContext(String tenantAlias) {
    return new TenancyContext(new Tenant(tenantAlias), false);
  }

  public static TenancyContext emptyContext() {
    return EMPTY_CONTEXT;
  }

  @Override
  public String toString() {
    if (tenant == null) {
      return "No Tenant";
    } else {
      if (isValid()) {
        return "Tenant " + tenant.getAlias() + " (" + tenant.getId() + ")";
      } else {
        return "Tenant " + tenant.getAlias() + " (unknown)";
      }
    }
  }
}
