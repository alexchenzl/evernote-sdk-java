/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml;

/**
 * A simple representation of a downloaded resource object
 * 
 * @author alexchenzl
 */
public class BinaryResource {

  private byte[] bytes;
  private String mime;
  private String charset;
  private String filename;

  public BinaryResource(byte[] bytes, String mime, String charset, String filename) {
    this.bytes = bytes;
    this.mime = mime;
    this.charset = charset;
    this.filename = filename;
  }

  /**
   * convert binary bytes into a string object
   * 
   * @return A string object
   */
  public String asString() {
    if (charset != null) {
      return ENMLUtil.bytesToString(bytes, charset);
    }
    return ENMLUtil.bytesToString(bytes);
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

}