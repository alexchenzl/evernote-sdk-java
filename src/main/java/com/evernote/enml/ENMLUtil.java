/*
 * Copyright 2015 Evernote Corporation
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.evernote.enml;

import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evernote.edam.type.Data;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.ResourceAttributes;

/**
 * Contains a set of static helper functions
 * 
 */
public class ENMLUtil {

  /**
   * All EDAM strings will be encoded as UTF-8.
   */
  public static final Charset UTF8 = Charset.forName("UTF-8");
  public static final Charset DEFAULT_CHARSET = UTF8;

  private static final ConcurrentHashMap<String, Charset> encodingToCharsetCache =
      new ConcurrentHashMap<String, Charset>();

  /**
   * One-way hashing function used for providing a checksum of EDAM data
   */
  public static final String EDAM_HASH_ALGORITHM = "MD5";

  public static final Pattern URL_PATTERN = Pattern.compile("([^:]*):(.*)");

  /**
   * Defines mime types used widely in ENML
   */
  public static final String MIME_PNG = "image/png";
  public static final String MIME_GIF = "image/gif";
  public static final String MIME_JPEG = "image/jpeg";
  public static final String MIME_TIFF = "image/tiff";
  public static final String MIME_JPG = "image/jpg";
  public static final String MIME_PJPEG = "image/pjpeg";
  public static final String MIME_BMP = "image/bmp";
  public static final String MIME_WAV = "audio/wav";
  public static final String MIME_VND_WAVE = "audio/vnd.wave";
  public static final String MIME_MP3 = "audio/mpeg";
  public static final String MIME_AMR = "audio/amr";
  public static final String MIME_PDF = "application/pdf";

  private static Map<String, String> mimeExtMap = new HashMap<String, String>();

  static {
    mimeExtMap.put(MIME_PNG, "png");
    mimeExtMap.put(MIME_GIF, "gif");
    mimeExtMap.put(MIME_JPEG, "jpg");
    mimeExtMap.put(MIME_JPG, "jpg");
    mimeExtMap.put(MIME_PJPEG, "jpg");
    mimeExtMap.put(MIME_TIFF, "tif");
    mimeExtMap.put(MIME_BMP, "bmp");

    mimeExtMap.put(MIME_WAV, "wav");
    mimeExtMap.put(MIME_VND_WAVE, "wav");
    mimeExtMap.put(MIME_MP3, "mp3");
    mimeExtMap.put(MIME_AMR, "amr");
    mimeExtMap.put(MIME_PDF, "pdf");
  }

  public static String getPossibleExtension(String mime) {
    return mimeExtMap.get(mime);
  }

  /**
   * A runtime exception that will be thrown when we hit an error that should "never"
   * occur ... e.g. if the JVM doesn't know about UTF-8 or MD5.
   */
  private static final class ENMLUtilException extends RuntimeException {

    private static final long serialVersionUID = -8099786694856724498L;

    public ENMLUtilException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Private constructor ... this class should not be instantiated, just use the static
   * helper functions.
   */
  private ENMLUtil() {
  }

  /**
   * Takes the provided byte array and converts it into a hexidecimal string with two
   * characters per byte.
   *
   */
  public static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte hashByte : bytes) {
      int intVal = 0xff & hashByte;
      if (intVal < 0x10) {
        sb.append('0');
      }
      sb.append(Integer.toHexString(intVal));
    }
    return sb.toString();
  }

  /**
   * Encodes a string as a byte array using the default encoding.
   */
  public static byte[] stringToBytes(String string) {
    return stringToBytes(string, DEFAULT_CHARSET);
  }

  /**
   * Encodes a string as a byte array with a specified character set encoding.
   */
  public static byte[] stringToBytes(String string, String encoding) {
    return stringToBytes(string, getCharset(encoding));
  }

  /**
   * Encodes a string as a byte array with a specified character set encoding.
   */
  private static byte[] stringToBytes(String string, Charset charset) {
    if (string == null) {
      return null;
    }
    ByteBuffer encoded = charset.encode(string);
    byte[] result = new byte[encoded.remaining()];
    encoded.get(result, 0, result.length);
    return result;
  }

  /**
   * Decodes a byte array as a string using the default encoding (UTF-8)
   */
  public static String bytesToString(byte[] bytes) {
    return bytesToString(bytes, DEFAULT_CHARSET);
  }

  /**
   * Decodes a byte array as a string using the specified character set encoding.
   */
  public static String bytesToString(byte[] bytes, String encoding) {
    return bytesToString(bytes, getCharset(encoding));
  }

  /**
   * Decodes a byte array as a string using the specified character set encoding.
   */
  public static String bytesToString(byte[] bytes, Charset charSet) {
    if (bytes == null) {
      return null;
    }
    return charSet.decode(ByteBuffer.wrap(bytes)).toString();
  }

  /**
   * Creates a Thrift Data object using the provided data blob.
   *
   * @param body the binary contents of the Data object to be created
   */
  public static Data bytesToData(byte[] body) {
    return bytesToData(body, true);
  }

  /**
   * Creates a Thrift Data object using the provided data blob.
   *
   * @param body the binary contents of the Data object to be created
   * @param includeBody if true, then the Data should contain the body bytes, otherwise it
   *          will just contain the metadata (hash, size) about the data.
   */
  public static Data bytesToData(byte[] body, boolean includeBody) {
    Data data = new Data();
    data.setSize(body.length);
    data.setBodyHash(hash(body));
    if (includeBody) {
      data.setBody(body);
    }
    return data;
  }

  /**
   * Returns an MD5 checksum of the provided array of bytes.
   */
  public static byte[] hash(byte[] body) {
    try {
      return MessageDigest.getInstance(EDAM_HASH_ALGORITHM).digest(body);
    } catch (NoSuchAlgorithmException e) {
      throw new ENMLUtilException(EDAM_HASH_ALGORITHM + " not supported", e);
    }
  }

  /**
   * Returns an MD5 checksum of the provided string, which is encoded into UTF-8 format
   * first for unambiguous hashing.
   */
  public static byte[] hash(String content) {
    return hash(stringToBytes(content));
  }

  /**
   * 
   * Creates a thrift Resource object using provided binary blob, mime type and optional
   * filename
   * 
   * 
   * @param bytes
   * @param mime
   * @param filename
   * @return A Resource object
   */
  public Resource buildResource(byte[] bytes, String mime, String filename) {

    if (bytes != null && mime != null) {
      Data data = ENMLUtil.bytesToData(bytes);
      Resource res = new Resource();
      res.setData(data);
      res.setMime(mime);
      if (filename != null) {
        ResourceAttributes attr = new ResourceAttributes();
        attr.setFileName(filename);
        res.setAttributes(attr);
      }
      return res;
    }
    return null;
  }

  /**
   * Returns the Java Charset that should be used for the provided encoding
   */
  public static Charset getCharset(String enc) {
    Charset charset = encodingToCharsetCache.get(enc);
    if (charset == null) {
      charset = Charset.forName(enc);
      encodingToCharsetCache.put(enc, charset);
    }
    return charset;
  }

  /**
   * Checks if a provided string is treated as blank string in ENML
   * 
   * @param str
   * @return {@code true} if it's a blank string
   */
  public static boolean isBlank(String str) {
    if (str == null) {
      return true;
    }
    return isBlank(new StringBuilder(str));
  }

  /**
   * Uses {@link Character#isSpaceChar} in addition to {@link Character#isWhitespace}.
   *
   * Character.isWhitespace covers things like \t \n and other Unicode control characters
   * that are treated as whitespace in ENML. Character.isSpaceChar covers things like
   * non-breaking spaces that Java doesn't directly consider whitespace.
   *
   * And for some reason, Character.isSpaceChar isn't true for tabs so we really do need
   * both. http://stackoverflow.com/a/1060759.
   *
   * @param sb
   * @return is the string composed of only whitespace or control characters
   */
  public static boolean isBlank(StringBuilder sb) {
    int length;
    if (sb == null || (length = sb.length()) == 0) {
      return true;
    }

    for (int offset = 0; offset < length;) {
      final int codepoint = sb.codePointAt(offset);

      if (!saneIsWhitespace(codepoint)) {
        return false;
      }
      offset += Character.charCount(codepoint);
    }
    return true;
  }

  private static boolean saneIsWhitespace(int codepoint) {
    return Character.isWhitespace(codepoint) || Character.isSpaceChar(codepoint);
  }

  /**
   * This will remove whitespace (as defined by isBlank) and control characters as defined
   * by Unicode from the beginning and end of a given string.
   *
   * 
   *
   * @param str
   * @return string cleaned of whitespace and control characters. Never null.
   */
  public static String cleanString(String str) {
    if (str == null) {
      return "";
    }
    int beginIndex = 0;
    int endIndex = str.length();

    // Find the first non-whitespace, non-control character's index
    while (beginIndex < endIndex) {
      final int codepoint = str.codePointAt(beginIndex);

      if (!saneIsWhitespace(codepoint) && !Character.isISOControl(codepoint)) {
        break;
      }
      beginIndex += Character.charCount(codepoint);
    }

    // Find the last non-whitespace, non-control character's index
    while (endIndex > beginIndex) {
      // Don't copy this loop if you want a generic "iterate from the end" loop.
      // This does not handle wide characters correctly. But since there are no wide
      // character spaces or control characters, it works here.
      final int codepoint = str.codePointAt(endIndex - 1);
      if (!saneIsWhitespace(codepoint) && !Character.isISOControl(codepoint)) {
        break;
      }
      endIndex -= 1;
    }

    return str.substring(beginIndex, endIndex);
  }

  /**
   * Check if the URL is supported in ENML.
   * 
   * Only http, https and file URL protocols are supported in ENML
   * 
   * @param urlString
   * @return {@code true} if it's a valid URL string
   */
  public static boolean isValidURL(String urlString) {
    urlString = cleanString(urlString);
    if (urlString == null || urlString.startsWith("data:text")) {
      return false;
    }
    Matcher m = URL_PATTERN.matcher(urlString);
    if (!m.matches()) {
      return false;
    }
    String scheme = m.group(1).toLowerCase();
    if (scheme.contains("script")) {
      return false;
    }

    try {
      new URL(urlString);
      return true;
    } catch (Exception badURLCheckWithProvidedScheme) {
      return false;
    }
  }

  /**
   * 
   * replaces unsafe ASCII characters with a "%" followed by hexadecimal digits in a URL's
   * query string.
   * 
   * For example: "https://www.evernote.com/hello word" will be converted to
   * "http://www.evernote.com/hello%20word"
   * 
   * 
   */
  public static String escapeURL(String urlString) {
    try {
      if (urlString != null) {
        URL url = new URL(urlString.trim());
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url
            .getPort(), url.getPath(), url.getQuery(), url.getRef());
        return uri.toASCIIString();
      }
    } catch (Exception e) {
      // It should be a wrong URL string
    }
    return null;
  }

}
