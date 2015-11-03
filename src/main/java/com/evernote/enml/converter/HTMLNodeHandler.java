/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml.converter;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

import com.evernote.enml.ResourceFetcher;

/**
 * Provides an hook for developers to implement their owns element hander for HTML to ENML
 * transform. This hook will be called before any other built-in transforming process.
 * 
 * @author alexchenzl
 */
public interface HTMLNodeHandler {

  /**
   * Initializes the handler before converting a HTML string to an ENML string
   * 
   */
  public void initialize();

  /**
   * This interface is called before a node is processed by HTMLToENML built-in
   * transforming methods. The node type can be either org.jsoup.nodes.TextNode or
   * org.jsoup.nodes.Element.
   * <p>
   * DO NOT remove the node in this function, it may cause unexpected exceptions. Be
   * careful to execute any other functions that may change the DOM tree's structure,
   * because it may cause the HTMLToENML object can not traverse all nodes. But it's free
   * to modify other attributes of the node.
   * 
   * @param node The Jsoup Node object to be processed.
   * @param fetcher Resource fetcher
   * @return {@code true} means this element should be converted to an ENML element, the
   *         application will execute subsequent built-in processes on this element after
   *         this function returns; {@code false} means this element should not be
   *         converted to an ENML element, it will be removed from the Jsoup DOM tree
   *         after this function returns.
   */
  public boolean process(Node node, ResourceFetcher fetcher);

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
