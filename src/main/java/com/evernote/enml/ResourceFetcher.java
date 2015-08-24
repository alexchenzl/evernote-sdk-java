/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml;

import java.io.IOException;
import java.util.Map;

/**
 * An interface to fetch resource from remote servers or even local disks.
 * 
 * Developers may use HttpConnection, HttpClient or OkHttpClient on Android to implement
 * this interface
 * 
 * @author alexchenzl
 */
public interface ResourceFetcher {

  /**
   * To implement this interface to prohibit malicious downloading attempts
   * 
   * @param urlString
   * @return
   */
  public boolean isAllowedURL(String urlString);

  /**
   * Downloads the resource as in memory BinaryResource object for late use
   * 
   * @param resourceURL
   * @param customHeaders
   * @return
   * @throws IOException
   * @throws ClientProtocolException
   */
  public BinaryResource fetchResource(String resourceURL,
      Map<String, String> customHeaders) throws IOException;

  /**
   * Downloads the resource and saves it as a local file.
   * 
   * @param resourceURL
   * @param customHeaders
   * @param filename
   * @return
   * @throws IOException
   * @throws ClientProtocolException
   */
  public boolean fetchResource(String resourceURL, Map<String, String> customHeaders,
      String filename) throws IOException;

}
