/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.clients;

import java.io.IOException;
import java.util.List;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;
import com.evernote.enml.ENMLConstants;
import com.evernote.enml.ENMLUtil;
import com.evernote.enml.ResourceData;
import com.evernote.enml.ResourceFetcher;
import com.evernote.enml.converter.HTMLElementHandler;
import com.evernote.enml.converter.HTMLToENML;
import com.evernote.enml.css.CSSPair;
import com.evernote.enml.css.CSSToInlineStyles;

/**
 * This helper class provides helper methods to build a Note according to html and css
 * content, even to build a note according to a specified web page url.
 * <p>
 * It's NOT thread safe.
 * 
 * @author alexchenzl
 */

public class ENHTMLToENMLHelper {

  private ResourceFetcher fetcher;
  private HTMLElementHandler handler;

  /**
   * Constructs a ENHTMLToENMLHelper object.
   * 
   * 
   * @param fetcher This {@link ResourceFetcher} object is used to download files from
   *          remote servers.
   * @param handler If you want to add some customized process step before the built-in
   *          HTML to ENML transform steps, you can implement your own
   *          {@link HTMLElementHandler}. This parameter can be null.
   * 
   */
  public ENHTMLToENMLHelper(ResourceFetcher fetcher, HTMLElementHandler handler) {
    if (fetcher == null) {
      throw new IllegalArgumentException("The argument fetcher must not be null!");
    }
    this.fetcher = fetcher;
    this.handler = handler;
  }

  /**
   * Downloads the web page specified by URL, and converts all external CSS styles into
   * inline styles, then builds a note based on this web page. The URL may also points to
   * a binary resource, such as an image
   * 
   * @param url The URL string of the web page
   * @param selector A jquery-like selector string to find the elements that will be saved
   *          into the Note object. If you want to save the whole web page, set selector
   *          to null. For selector syntax, please refer to
   *          http://jsoup.org/cookbook/extracting-data/selector-syntax
   * @return A Note object built from this web page content. Please notice that it is not
   *         saved into Evernote service yet.
   * @throws IOException
   */

  public Note buildNoteFromURL(String url, String selector) throws IOException {
    ResourceData resourceData = fetcher.fetchResource(url.toString(), null);
    if (resourceData != null) {
      if ("text/html".equalsIgnoreCase(resourceData.getMime())) {
        return buildNoteFromHtml(resourceData.asString(), selector, url, null);
      }

      // If the resource is not html, it will be saved as resource in a new Note
      byte[] bytes = resourceData.asBinary();
      if (bytes != null) {
        Resource res = ENMLUtil.buildResource(bytes,
            resourceData.getMime(), resourceData.getFilename());
        String hash = ENMLUtil.bytesToHex(res.getData()
            .getBodyHash());

        StringBuilder builder = new StringBuilder();
        builder.append(ENMLConstants.ENML_HEADER);
        builder.append("<en-note>");
        builder
            .append("<en-media type=\"" + res.getMime() + "\" hash=\"" + hash + "\"/>");
        builder.append("/<en-note>");

        String title = ENMLUtil.cleanString(resourceData.getFilename());
        if (title.isEmpty()) {
          title = "New Note";
        }

        Note note = new Note();
        note.setContent(builder.toString());
        note.setTitle(title);
        note.addToResources(res);
        return note;
      }
    }
    return null;
  }

  /**
   * Applies specified CSS to the HTML content and converts them into inline styles, then
   * builds a note based on the generated ENML content.
   * <p>
   * If there's no style sheets specified, the external style sheets and embedded styles
   * included in this HTML content will be applied to the HTML content.
   * 
   * @param html HTML content to be saved into a Note object
   * @param selector A jquery-like selector string to find the elements that will be saved
   *          into the Note object. If you want to save the whole web page, set selector
   *          to null. For selector syntax, please refer to
   *          http://jsoup.org/cookbook/extracting-data/selector-syntax
   * @param htmlBaseUrl The base URL of this HTML content
   * @param cssPariList A list of {@link CSSPair} object, each item includes both CSS
   *          content and its base URL.
   * @return A Note object built from this web page content. Please notice that it is not
   *         saved into Evernote service yet.
   * 
   */
  public Note buildNoteFromHtml(String html, String selector, String htmlBaseUrl,
      List<CSSPair> cssPariList) {
    if (html == null || html.isEmpty()) {
      return null;
    }

    CSSToInlineStyles cssHandler = new CSSToInlineStyles(fetcher);
    HTMLToENML converter = new HTMLToENML(fetcher, handler);
    html = cssHandler.processHTML(html, htmlBaseUrl, cssPariList);
    if (converter.convert(html, selector, htmlBaseUrl)) {
      Note note = new Note();
      note.setContent(ENMLConstants.ENML_HEADER + "<en-note>" + converter.getContent()
          + "</en-note>");
      note.setTitle(converter.getTitle());
      if (note.getTitle() == null || note.getTitle().isEmpty()) {
        note.setTitle("New Note");
      }
      note.setResources(converter.getResources());
      return note;
    }
    return null;
  }

  public ResourceFetcher getFetcher() {
    return fetcher;
  }

  public void setFetcher(ResourceFetcher fetcher) {
    this.fetcher = fetcher;
  }

  public HTMLElementHandler getHandler() {
    return handler;
  }

  public void setHandler(HTMLElementHandler handler) {
    this.handler = handler;
  }

}
