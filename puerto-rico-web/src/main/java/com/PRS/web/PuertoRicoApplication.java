package com.PRS.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling is enabled for exactly one job: {@link GameTableSweeper}. */
@SpringBootApplication
@EnableScheduling
public class PuertoRicoApplication {

  public static void main(String[] args) {
    SpringApplication.run(PuertoRicoApplication.class, args);
  }
}
