package com.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class Application {

  public static final class Profiles {
    public static final String NO_DEPENDENCIES = "no-dependencies";
  }
}
