/**
 * Copyright 2015 Evernote Corporation.
 */
package com.evernote.enml.css;

import java.util.Objects;

public class CSSPair {

  private String url;
  private String css;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getCss() {
    return css;
  }

  public void setCss(String css) {
    this.css = css;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CSSPair)) {
      return false;
    }
    CSSPair p = (CSSPair) o;
    return Objects.equals(p.url, url) && Objects.equals(p.css, css);
  }

  @Override
  public int hashCode() {
    return (url == null ? 0 : url.hashCode()) ^ (css == null ? 0 : css.hashCode());
  }

}
