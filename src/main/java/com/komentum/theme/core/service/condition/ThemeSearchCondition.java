package com.komentum.theme.core.service.condition;

import lombok.Getter;

@Getter
public class ThemeSearchCondition {

  private Boolean bookmarked = false;
  private String publicUserId = null;

  public void withBookmarked(Boolean bookmarked) {
    this.bookmarked = bookmarked;
  }

  public void withPublicUserId(String publicUserId) {
    this.publicUserId = publicUserId;
  }
}
