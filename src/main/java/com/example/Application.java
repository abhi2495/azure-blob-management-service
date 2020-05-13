/*
 * // ==========================================================================
 * //                  Copyright 2019, JDA Software Group, Inc.
 * //                            All Rights Reserved
 * //
 * //               THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF
 * //                          JDA SOFTWARE GROUP, INC.
 * //
 * //
 * //          The copyright notice above does not evidence any actual
 * //               or intended publication of such source code.
 * //
 * // ==========================================================================
 */

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
