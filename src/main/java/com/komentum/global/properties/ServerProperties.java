package com.komentum.global.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "server")
public class ServerProperties {

  private final String port;
  private final String domain;
  private final String externalDomain;
}
