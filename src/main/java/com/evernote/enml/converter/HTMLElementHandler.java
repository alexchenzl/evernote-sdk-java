/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml.converter;

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
   * transforming methods
   * 
   * The tag name of this element has already been converted to all lower case
   * 
   * @param element A Jsoup Element object to be processed
   * @param fetcher Resource fetcher
   * @return true means the application will execute following built-in processes on this
   *         element after this function returns; false means there is no need to execute
   *         following built-in processes on this element
   */
  public boolean process(Element element, ResourceFetcher fetcher);

}
