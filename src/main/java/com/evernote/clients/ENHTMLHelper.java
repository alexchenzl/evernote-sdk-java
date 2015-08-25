/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.clients;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evernote.auth.EvernoteAuth;
import com.evernote.enml.BinaryResource;
import com.evernote.enml.ResourceFetcher;

/**
 * Provides methods to retrieve Note content as HTML instead of ENML, and also to download
 * ENML Resource as local files
 * 
 * @author alexchenzl
 */
public class ENHTMLHelper {

  private ResourceFetcher fetcher;
  private String noteBaseUrl;
  private String resBaseUrl;
  private Map<String, String> customHeader;
  private Pattern resUrlPattern;

  /**
   * 
   * @param token The authentication token to access the resources
   * @param noteStoreUrl URL of the note store in where those resources reside
   * @fetcher fetcher ResourceFetcher to download files from remote servers
   * 
   */
  public ENHTMLHelper(EvernoteAuth auth, ResourceFetcher fetcher) {
    if (auth == null || fetcher == null) {
      throw new IllegalArgumentException("All arguments must not be null!");
    }

    if (auth.getToken() == null || auth.getNoteStoreUrl() == null) {
      throw new IllegalArgumentException("OAuth token and noteStoreUrl must not be null!");
    }

    this.customHeader = new HashMap<String, String>();
    this.customHeader.put("Cookie", "auth=" + auth.getToken());
    this.fetcher = fetcher;

    String noteStoreUrl = auth.getNoteStoreUrl();
    int idx = noteStoreUrl.indexOf("shard");
    this.noteBaseUrl = noteStoreUrl.substring(0, idx) + "note/";

    idx = noteStoreUrl.indexOf("notestore");
    this.resBaseUrl = noteStoreUrl.substring(0, idx) + "res/";

    this.resUrlPattern =
        Pattern.compile("(" + this.resBaseUrl
            + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).*?)\"",
            Pattern.MULTILINE);
  }

  /**
   * Directly downloads a Note resource as a local file via HTTP Get request instead of
   * thrift API. For more details, please refer to
   * https://dev.evernote.com/doc/articles/resources.php
   * 
   * @param resourceGuid Guid of the resource to be downloaded
   * @param filename Full output filename
   * @return {@code true} if success
   * @throws IOException
   */
  public boolean downloadResourceAsFile(String resourceGuid, String filename)
      throws IOException {
    String url = this.resBaseUrl + resourceGuid;
    return fetcher.fetchResource(url, customHeader, filename);
  }

  /**
   * Downloads specified note as HTML.The result is block of HTML code snippet quoted by a
   * div tag.
   * 
   * Please be careful that all links of resources in this HTML content point to Evernote
   * web service if webResourcePath is not provided.
   * 
   * 
   * @param guid GUID of the note to be retrieved
   * @param localResourcePath The path where resource files to be saved on your local
   *          machine
   * @param webResourcePath It's used to generate full paths of resource links in the HTML
   *          content
   * @return
   * @throws IOException
   */
  public String downloadNoteAsHtml(String guid, String localResourcePath,
      String webResourcePath) throws IOException {
    String noteUrl = noteBaseUrl + guid;
    BinaryResource binaryData = fetcher.fetchResource(noteUrl, customHeader);
    if (binaryData != null) {
      String html = binaryData.asString();
      if (localResourcePath != null && webResourcePath != null) {
        html = downloadAllResourcesInHTML(html, localResourcePath, webResourcePath);
      }
      return html;
    }
    return null;
  }

  private String downloadAllResourcesInHTML(String html, String localResourcePath,
      String webResourcePath) throws IOException {
    if (html != null && localResourcePath != null && localResourcePath != null) {
      String result = html;
      Matcher matcher = resUrlPattern.matcher(html);
      while (matcher.find()) {
        String resUrl = matcher.group(1);
        String guid = matcher.group(2);
        String filename = guid;

        int idx = resUrl.indexOf(guid);
        String extension = getPossibleFileExtenstion(resUrl.substring(idx));
        if (extension != null) {
          filename += extension;
        }

        String absoluteFilename = localResourcePath + filename;
        if (downloadResource(resUrl, absoluteFilename)) {
          String newResUrl = webResourcePath + filename;
          result = result.replace(resUrl, newResUrl);
        }
      }
      return result;
    }
    return html;
  }

  private String getPossibleFileExtenstion(String partialUrl) {
    int end = partialUrl.indexOf("?");
    if (end > 0) {
      partialUrl = partialUrl.substring(0, end);
    }
    int begin = partialUrl.lastIndexOf(".");
    if (begin >= 0) {
      return partialUrl.substring(begin);
    }
    return null;
  }

  private boolean downloadResource(String url, String filename) throws IOException {
    return fetcher.fetchResource(url, customHeader, filename);
  }

  public ResourceFetcher getFetcher() {
    return fetcher;
  }

  public void setFetcher(ResourceFetcher fetcher) {
    this.fetcher = fetcher;
  }

}
