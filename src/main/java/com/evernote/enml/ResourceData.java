/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml;

/**
 * A simple representation of a downloaded resource object. It may contains a binary array
 * or a String content.
 * 
 * @author alexchenzl
 */
public class ResourceData implements java.io.Serializable {

  private static final long serialVersionUID = 8059665577261650256L;

  private byte[] bytes;
  private String content;
  private String mime;
  private String charset;
  private String filename;
  // original url of a resource sometimes may be redrirected to another url, let's store
  // the final redirected url here.
  private String finalUrl;

  public ResourceData(byte[] bytes, String mime, String filename) {
    this.bytes = bytes;
    this.mime = mime;
    this.filename = filename;
  }

  public ResourceData(String content, String mime, String charset) {
    this.content = content;
    this.mime = mime;
    this.charset = charset;
  }

  /**
   * If the content member of this object is not null, the content String will be
   * returned. If it's null, the bytes member of this object will be converted as a String
   * to return.
   * 
   * @return String
   */
  public String asString() {

    if (content != null) {
      return content;
    }
    if (bytes != null) {
      if (charset != null) {
        return ENMLUtil.bytesToString(bytes, charset);
      } else {
        return ENMLUtil.bytesToString(bytes);
      }
    }
    return null;
  }

  /**
   * If the bytes member of this object is not null, the bytes will be returned. If it's
   * null, the content member of this object will be converted as a byte array to return.
   * 
   * @return byte[]
   */
  public byte[] asBinary() {
    if (bytes != null) {
      return bytes;
    }
    if (content != null) {
      if (charset != null) {
        return ENMLUtil.stringToBytes(content, charset);
      } else {
        return ENMLUtil.stringToBytes(content);
      }
    }
    return null;
  }

  public String getContent() {
    return content;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public void setBytes(byte[] bytes) {
    this.bytes = bytes;
  }

  public String getMime() {
    return mime;
  }

  public void setMime(String mime) {
    this.mime = mime;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getFinalUrl() {
    return finalUrl;
  }

  public void setFinalUrl(String finalUrl) {
    this.finalUrl = finalUrl;
  }

}