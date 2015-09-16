/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.clients;

import java.io.IOException;
import java.util.List;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteAttributes;
import com.evernote.enml.BinaryResource;
import com.evernote.enml.ENMLConstants;
import com.evernote.enml.ENMLUtil;
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
   * inline styles, then builds a note based on this web page
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
    BinaryResource resourceData = fetcher.fetchResource(url.toString(), null);
    if (resourceData != null) {
      Note note = buildNoteFromHtml(resourceData.asString(), selector, url, null);
      if (note != null) {
        // set source URL of the note
        NoteAttributes attr = note.getAttributes();
        if (attr == null) {
          attr = new NoteAttributes();
        }
        String sourceURL = ENMLUtil.escapeURL(url.toString());
        attr.setSourceURL(sourceURL);
        note.setAttributes(attr);
        return note;
      }
    }
    return null;
  }

  /**
   * Applies specified CSS to the HTML content as inline styles, then builds a note based
   * on the generated ENML content
   * 
   * @param html HTML content to be saved into a Note object
   * @param selector A jquery-like selector string to find the elements that will be saved
   *          into the Note object. If you want to save the whole web page, set selector
   *          to null. For selector syntax, please refer to
   *          http://jsoup.org/cookbook/extracting-data/selector-syntax
   * @param htmlBaseUrl The base URL of this HTML content
   * @param cssPariList A list of {@link CSSPair}, every {@link CSSPair} object includes
   *          CSS content and its base URL
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
    if (cssPariList == null || cssPariList.isEmpty()) {
      html = cssHandler.processHTML(html, htmlBaseUrl);
    } else {
      cssHandler.clearStyleSheet();
      for (CSSPair item : cssPariList) {
        cssHandler.addStyleSheet(item.getCss(), item.getUrl());
      }
      html = cssHandler.processHTMLWithSpecifiedCSS(html, htmlBaseUrl);
    }

    HTMLToENML converter = new HTMLToENML(fetcher, handler);
    if (converter.convert(html, selector, htmlBaseUrl)) {
      Note note = new Note();
      note.setContent(ENMLConstants.ENML_HEADER + "<en-note>" + converter.getContent()
          + "</en-note>");
      note.setTitle(converter.getTitle());
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
