package com.example.tenancy;

import java.util.ArrayList;
import java.util.List;

public final class TenancyContextHolder {

  private static final ThreadLocal<TenancyContext> CONTEXT_HOLDER = new ThreadLocal<>();

  private static final List<TenancyContextObserver> TENANCY_CONTEXT_OBSERVERS = new ArrayList<>();

  private TenancyContextHolder() {
  }

  public static TenancyContext getContext() {
    TenancyContext context = CONTEXT_HOLDER.get();

    if (context == null) {
      return TenancyContext.emptyContext();
    }

    return context;
  }

  public static void setContext(TenancyContext context) {
    if (context == null) {
      throw new IllegalArgumentException("Context must not be null.");
    }
    CONTEXT_HOLDER.set(context);
    for (TenancyContextObserver observer : TENANCY_CONTEXT_OBSERVERS) {
      observer.tenancyContextChanged();
    }
  }

  public static void clearContext() {
    CONTEXT_HOLDER.remove();
    for (TenancyContextObserver observer : TENANCY_CONTEXT_OBSERVERS) {
      observer.tenancyContextChanged();
    }
  }

  public static void registerTenancyContextObserver(TenancyContextObserver observer) {
    if (!TENANCY_CONTEXT_OBSERVERS.contains(observer)) {
      TENANCY_CONTEXT_OBSERVERS.add(observer);
    }
  }

  public static void unregisterTenancyContextObserver(TenancyContextObserver observer) {
    TENANCY_CONTEXT_OBSERVERS.remove(observer);
  }
}
