/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml.converter;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.evernote.enml.ResourceFetcher;

/**
 * Provides an hook for developers to implement their owns element hander for HTML to ENML
 * transform. This hook will be called before any other built-in transforming process.
 * 
 * @author alexchenzl
 */
public interface HTMLElementHandler {

  /**
   * Initializes the handler before converting a HTML string to an ENML string
   * 
   */
  public void initialize();

  /**
   * This interface is called before an element is processed by HTMLToENML built-in
   * transforming methods. Do NOT call element.remove() in this function, it may cause
   * unexpected exceptions.
   * 
   * The tag name of this element has already been converted to all lower case
   * 
   * @param element A Jsoup Element object to be processed
   * @param fetcher Resource fetcher
   * @return {@code true} means this element should be converted to an ENML element, the
   *         application will execute subsequent built-in processes on this element after
   *         this function returns; {@code false} means this element should not be
   *         converted to an ENML element, it will be removed from the Jsoup DOM tree
   *         after this function returns.
   */
  public boolean process(Element element, ResourceFetcher fetcher);

  /**
   * User defined logic to extract keywords from the HTML documents. The keywords can be
   * used to set note tags. If a null object or empty string is returned, the
   * {@link HTMLToENML} object will extract keywords from HTML meta tag.
   * 
   * @param doc Jsoup document object
   * @return extracted keywords from the HTML document
   * 
   */
  public String extractKeywords(Document doc);

}
