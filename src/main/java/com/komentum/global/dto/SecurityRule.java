package com.komentum.global.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SecurityRule {

  private String pattern;
  private MatcherType matcher = MatcherType.ANT;

  public enum MatcherType {
    ANT,
    REGEX
  }
}
