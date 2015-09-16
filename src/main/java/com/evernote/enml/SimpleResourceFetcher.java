/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */

package com.evernote.enml;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a sample implementation of ResourceFetcher.
 * <p>
 * If you are running your application on server, you should use a connection pool to
 * implement your own ResourceFetcher. If you are running your application on Android,
 * OkHttpClient maybe is a good choice to implement ResrouceFetcher.
 * 
 * @author alexchenzl
 */
public class SimpleResourceFetcher implements ResourceFetcher {

  private static final Logger logger = Logger.getLogger(SimpleResourceFetcher.class
      .getName());

  private static final int DEFAULT_CONNECT_TIMEOUT = 3000;
  private static final int DEFAULT_READ_TIMEOUT = 10000;

  private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
  private int readTimeout = DEFAULT_READ_TIMEOUT;

  private Map<String, String> defaultHeaders = new HashMap<String, String>();

  public Map<String, String> getDefaultHeaders() {
    return defaultHeaders;
  }

  public void setDefaultHeaders(Map<String, String> defaultHeaders) {
    this.defaultHeaders = defaultHeaders;
  }

  /**
   * 
   * Check a URL for a prohibited host address. The list currently includes the following
   * IP address ranges:
   * 
   * <ul>
   * <li>10.*</li>
   * <li>127.*</li>
   * <li>192.168.*</li>
   * <li>172.16.0.0 - 172.31.255.255</li>
   * </ul>
   * 
   * @param urlString the URL String to check
   * 
   */
  public boolean isAllowedURL(String urlString) {
    try {
      URL url = new URL(urlString);
      String protocol = url.getProtocol();
      if (!protocol.startsWith("http"))
        return false;
      String host = url.getHost();
      InetAddress address = InetAddress.getByName(host);
      byte ip1 = address.getAddress()[0];
      byte ip2 = address.getAddress()[1];
      if (address.isLoopbackAddress() || ip1 == (byte) 10 || ip1 == (byte) 127
          || (ip1 == (byte) 192 && ip2 == (byte) 168) || (ip1 == (byte) 172
              && ip2 >= (byte) 16 && ip2 <= (byte) 31)) {
        return false;
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /*
   * method is either GET of POST
   */
  private HttpURLConnection openURLConnection(URL urlToOpen, String method)
      throws IOException {
    HttpURLConnection conn = (HttpURLConnection) urlToOpen.openConnection();
    conn.setConnectTimeout(connectTimeout);
    conn.setReadTimeout(readTimeout);
    conn.setRequestMethod(method);
    return conn;
  }

  public BinaryResource fetchResource(String resourceURL,
      Map<String, String> customHeaders) throws IOException {

    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = openURLConnection(new URL(resourceURL), "GET");
      Map<String, String> headers = customHeaders == null ? defaultHeaders
          : customHeaders;

      if (headers != null) {
        for (Entry<String, String> entry : headers.entrySet()) {
          conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }

      int code = conn.getResponseCode();
      if (code == HttpURLConnection.HTTP_OK) {
        int length = conn.getContentLength();
        in = new BufferedInputStream(conn.getInputStream());
        byte[] result = readInputBytes(resourceURL, in, length);

        String contentType = conn.getContentType();
        String mime = parseMime(contentType);
        String charset = parseCharset(contentType);

        String disposition = conn.getHeaderField("Content-Disposition");
        String filename = getFileName(resourceURL, disposition, mime);

        return new BinaryResource(result, mime, charset, filename);
      } else {
        logger.log(Level.WARNING, "Error " + code + " Failed to fetch resource "
            + resourceURL);
      }

    } finally {
      if (in != null) {
        in.close();
      }
    }
    return null;
  }

  private String parseMime(String contentType) {
    String result = contentType;
    if (result != null) {
      int semicolonAt = contentType.indexOf(';');
      if (semicolonAt > 0) {
        contentType = contentType.substring(0, semicolonAt);
      }
      result = contentType.trim();
    }
    return result;
  }

  private String parseCharset(String contentType) {
    if (contentType != null) {
      String result = contentType.toLowerCase();
      int idx = result.indexOf("charset=");
      if (idx > 0) {
        return result.substring(idx + 8).toLowerCase();
      }
    }
    return null;
  }

  /**
   * Attempt to get possible filename of resource to download, it will also try to append
   * a file extension to the filename if necessary
   * 
   * @param urlString
   * @param disposition
   * @param mime
   * 
   */
  private String getFileName(String urlString, String disposition, String mime) {
    // extracts file name from header field
    if (disposition != null) {
      int index = disposition.indexOf("filename=");
      if (index > 0) {
        String filename = disposition.substring(index + 10, disposition.length() - 1);
        return filename;
      }
    }

    // try to compose a filename according to the urlString
    try {
      URL url = new URL(urlString);
      String filename = url.getPath();
      int begin = filename.lastIndexOf("/");
      int end = filename.indexOf("?");
      if (begin > 0) {
        if (end > 0 && end > begin) {
          filename = filename.substring(begin + 1, end);
        } else {
          filename = filename.substring(begin + 1);
        }
      }

      if (filename.indexOf(".") < 0) {
        String ext = ENMLUtil.getPossibleExtension(mime);
        if (ext != null) {
          filename += "." + ext;
        }
      }
      return filename;
    } catch (MalformedURLException e) {
      // it should not happen
    }
    return null;
  }

  public boolean fetchResource(String resourceURL, Map<String, String> customHeaders,
      String filename) throws IOException {

    if (resourceURL == null || filename == null) {
      return false;
    }

    HttpURLConnection conn = null;
    InputStream in = null;
    FileOutputStream out = null;

    try {
      conn = openURLConnection(new URL(resourceURL), "GET");
      Map<String, String> headers = customHeaders == null ? defaultHeaders
          : customHeaders;

      if (headers != null) {
        for (Entry<String, String> entry : headers.entrySet()) {
          conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }

      int code = conn.getResponseCode();
      if (code == HttpURLConnection.HTTP_OK) {
        in = new BufferedInputStream(conn.getInputStream());
        out = new FileOutputStream(filename);
        int count;
        byte data[] = new byte[4096];
        while ((count = in.read(data, 0, 4096)) != -1) {
          out.write(data, 0, count);
        }
        out.flush();
        return true;
      } else {
        logger.log(Level.WARNING, "Error " + code + " Failed to fetch resource "
            + resourceURL);
      }

    } finally {
      if (out != null) {
        out.close();
      }
      if (in != null) {
        in.close();
      }
    }
    return false;
  }

  /**
   * Attempts to read up to getBodyMaxSize() bytes of input from the provided input
   * stream.
   *
   * This method should only be used when the Content-Length is not known.
   */
  private byte[] readInputBytes(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    long max = getBodyMaxSize();
    long readMax = max + 1;

    byte[] buf = new byte[readMax > 4096 ? 4096 : (int) readMax];

    long copied = 0;
    int toRead = buf.length;
    int bytesRead;
    while (toRead > 0 && (bytesRead = in.read(buf, 0, toRead)) >= 0) {
      if (bytesRead > 0) {
        out.write(buf, 0, bytesRead);

        copied += bytesRead;
        readMax -= bytesRead;
        if (readMax < toRead) {
          toRead = (int) readMax;
        }
      }
    }

    if (copied > max) {
      throw new IOException("Content stream is too long, maximum permitted length=" + max
          + " bytes");
    }
    return out.toByteArray();
  }

  /**
   * Attempts to read exactly 'length' bytes of input from the provided input stream. If
   * length is not known (that is -ve) it calls readInputBytes(InputStream) to read up to
   * getBodyMaxSize() bytes of input from the provided input stream.
   */
  private byte[] readInputBytes(String url, InputStream in, int length)
      throws IOException {
    if (length > getBodyMaxSize()) {
      throw new IOException("Content stream is too long, maximum permitted length="
          + length + " bytes");
    }

    if (length < 0) {
      // Content-Length is not known, read and return up to getBodyMaxSize() bytes
      return readInputBytes(in);
    }

    byte[] result = new byte[length];
    int offset = 0;
    int readLength;
    do {
      readLength = in.read(result, offset, length - offset);
      offset += readLength;
    } while (offset < length && readLength > 0);

    if (offset < length) {
      throw new IOException("Premature end of input from " + url);
    }
    return result;
  }

  protected int getBodyMaxSize() {
    return Integer.MAX_VALUE;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }

}
