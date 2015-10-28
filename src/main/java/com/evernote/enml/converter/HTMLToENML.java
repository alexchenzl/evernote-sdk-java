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
package com.evernote.enml.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.evernote.edam.type.Resource;
import com.evernote.enml.ENMLConstants;
import com.evernote.enml.ENMLUtil;
import com.evernote.enml.ResourceData;
import com.evernote.enml.ResourceFetcher;
import com.evernote.enml.dtd.DTDAttribute;
import com.evernote.enml.dtd.DTDAttribute.AttributeType;
import com.evernote.enml.dtd.DTDAttribute.DefaultValueModel;
import com.evernote.enml.dtd.SimpleENMLDTD;

/**
 * A simple converter to transform HTML content into ENML content. The converter only
 * accepts in-line CSS style. It also ignores any Java scripts in the HTML content.
 * <p>
 * The instance is not thread-safe.
 * 
 * @author alexchenzl
 */
public class HTMLToENML {

  private static final Logger logger = Logger.getLogger(HTMLToENML.class.getName());

  // generated note resources
  private List<Resource> resourceList = null;
  // extracted HTML title
  private String title;
  // extracted keywords
  private String keywords;

  // generated ENML content
  private String content;

  private ResourceFetcher fetcher;
  private HTMLElementHandler customizedHandler;

  private static final Pattern PATTERN_DISPLAY_NONE = Pattern.compile(
      "display\\s*:\\s*none");

  // some tags are visible but not permitted in ENML, try to convert them to div tags
  // so that the content in these tags may be kept in the generated ENML content
  protected static final Set<String> TO_DIV_TAGS = new HashSet<String>();

  // some tags may need to be converted to span tags
  protected static final Set<String> TO_SPAN_TAGS = new HashSet<String>();

  static {
    TO_DIV_TAGS.add("section");
    TO_DIV_TAGS.add("fieldset");
    TO_DIV_TAGS.add("main");
    TO_DIV_TAGS.add("article");
    TO_DIV_TAGS.add("aside");
    TO_DIV_TAGS.add("summary");
    TO_DIV_TAGS.add("details");
    TO_DIV_TAGS.add("figcaption");
    TO_DIV_TAGS.add("figure");
    TO_DIV_TAGS.add("header");
    TO_DIV_TAGS.add("footer");
    TO_DIV_TAGS.add("nav");
    TO_DIV_TAGS.add("form");

    TO_SPAN_TAGS.add("mark");
    TO_SPAN_TAGS.add("label");
  }

  /**
   * Construct a HTMLToENML object
   * 
   * @param fetcher
   * @param customizedHandler
   */
  public HTMLToENML(ResourceFetcher fetcher, HTMLElementHandler customizedHandler) {
    this.fetcher = fetcher;
    this.customizedHandler = customizedHandler;
  }

  public ResourceFetcher getFetcher() {
    return fetcher;
  }

  public void setFetcher(ResourceFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /**
   * Get generated ENML resource objects by images or other binary files in this HTML
   * content.
   * 
   * @return A list of Resource objects
   */
  public List<Resource> getResources() {
    return resourceList;
  }

  /**
   * Get extracted title from the HTML head tag. It may be null, but will not be an empty
   * string
   *
   * @return The title string
   */
  public String getTitle() {
    return title;
  }

  /**
   * Get extracted keywords from the HTML document. It may be null. Multiple keywords are
   * separated by a comma.
   * <p>
   * By default, the keywords are extracted from the meta tag with the "keywords"
   * property. Developers can use their own methods to extract keywords by implementing
   * {@link HTMLElementHandler}
   * 
   * @return The keywords string
   */
  public String getKeywords() {
    return keywords;
  }

  /**
   * Get generated ENML content. This content doesn't include ENML header and ENML root
   * tag en-note.
   * 
   * @return The ENML string content
   */
  public String getContent() {
    return content;
  }

  private void extractKeywords(Document doc) {
    Elements metaKeywords = doc.select("meta[property=keywords]");
    if (metaKeywords != null) {
      keywords = metaKeywords.attr("content");
      keywords = ENMLUtil.cleanString(keywords);
    }
  }

  /**
   * Convert HTML string into ENML string.
   * 
   * If only some part of this HTML string needs to be converted, a CSS-like query
   * selector can be specified, for more information, please refer to
   * http://jsoup.org/cookbook/extracting-data/selector-syntax
   * 
   * 
   * @param html
   * @param selector
   * @param baseURLStr
   * @return {@code true} if the process is successful
   */
  public boolean convert(String html, String selector, String baseURLStr) {
    if (html == null) {
      return false;
    }

    title = null;
    content = null;
    resourceList = new ArrayList<Resource>();

    Document doc = Jsoup.parse(html, baseURLStr);
    doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
    doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
    doc.outputSettings().prettyPrint(false);

    title = doc.title();
    if (title != null) {
      title = ENMLUtil.cleanString(title);
      if (title.isEmpty()) {
        title = null;
      }
    }

    if (customizedHandler != null) {
      customizedHandler.initialize();
      keywords = customizedHandler.extractKeywords(doc);
    }

    if (keywords == null || keywords.isEmpty()) {
      extractKeywords(doc);
    }

    if (selector == null || selector.isEmpty()) {
      Element bodyElement = doc.body();
      if (bodyElement != null) {
        if (processElement(bodyElement)) {
          removeComments(bodyElement);
          content = bodyElement.toString();
          return true;
        }
      }
      return false;
    }

    Elements elts = doc.select(selector);
    if (elts != null && elts.size() > 0) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < elts.size(); i++) {
        Element elt = elts.get(i);
        if (processElement(elt)) {
          removeComments(elt);
          builder.append(elt.toString());
        }
      }
      if (builder.length() > 0) {
        content = builder.toString();
        return true;
      }
    }
    return false;
  }

  private boolean handleFailure(Element element) {
    element.remove();
    return false;
  }

  protected boolean processElement(Element element) {
    if (element == null) {
      return false;
    }
    // tag names in ENML must be all lowercase
    element.tagName(element.tagName().toLowerCase().trim());

    // call user defined element handler here
    if (customizedHandler != null) {
      if (!customizedHandler.process(element, fetcher)) {
        return handleFailure(element);
      }
    }

    // convert some special tags to ENML tags
    // for example: body --> div, img --> en-media
    if (!transformSpecialTags(element)) {
      return handleFailure(element);
    }

    // remove disallowed elements
    SimpleENMLDTD dtd = SimpleENMLDTD.getInstance();
    if (!dtd.isElementAllowed(element.tagName())) {
      return handleFailure(element);
    }

    // clean attributes
    cleanAttributes(dtd, element);

    // process children
    Elements children = element.children();
    if (children.size() > 0) {
      int remainedChildren = 0;
      Iterator<Element> it = children.iterator();
      while (it.hasNext()) {
        Element element2 = it.next();
        if (element2 != null) {
          if (processElement(element2)) {
            remainedChildren++;
          }
        }
      }

      // If all children are removed, and the content of this element is empty, then
      // remove this element and return false;
      if (remainedChildren == 0) {
        String text = element.text();
        if (text == null || text.trim().isEmpty()) {
          return handleFailure(element);
        }
      }
    }
    return true;
  }

  private static void removeComments(Node node) {
    for (int i = 0; i < node.childNodes().size();) {
      Node child = node.childNode(i);
      if (child.nodeName().equals("#comment")) {
        child.remove();
      } else {
        removeComments(child);
        i++;
      }
    }
  }

  /**
   * 
   * @param element
   * @return {@code true} if this element needs further processing, false means this
   *         element should be removed from ENML content
   */
  protected boolean transformSpecialTags(Element element) {

    // remove hidden elements
    String style = element.attr(ENMLConstants.HTML_STYLE_ATTR);
    if (style != null) {
      style = style.toLowerCase();
      Matcher matcher = PATTERN_DISPLAY_NONE.matcher(style);
      if (matcher.find()) {
        return false;
      }
    }

    String elementName = element.tagName();

    // convert some tags to div or span
    if (TO_DIV_TAGS.contains(elementName)) {
      element.tagName(ENMLConstants.HTML_DIV_TAG);
    } else if (TO_SPAN_TAGS.contains(elementName)) {
      element.tagName(ENMLConstants.HTML_SPAN_TAG);
    } else if (elementName.equals(ENMLConstants.HTML_BODY)) {
      element.tagName(ENMLConstants.HTML_DIV_TAG);
    } else
      if (elementName.equals(ENMLConstants.HTML_INPUT_TAG) && "checkbox".equalsIgnoreCase(
          element.attr("type"))) {
      element.tagName(ENMLConstants.EN_TODO_TAG);
      element.removeAttr("type");
    } else if (elementName.equals(ENMLConstants.HTML_IMG_TAG)) {
      String imgUrl = element.absUrl(ENMLConstants.HTML_IMG_SRC_ATTR);
      if (ENMLUtil.isAcceptableURL(imgUrl)) {
        if (resourceList == null) {
          element.attr(ENMLConstants.HTML_IMG_SRC_ATTR, imgUrl);
          return true;
        }

        if (!fetcher.isAllowedURL(imgUrl)) {
          logger.log(Level.INFO, "Prohibited image url " + imgUrl);
          return false;
        }

        ResourceData resData;
        try {
          resData = fetcher.fetchResource(imgUrl, null);
        } catch (Exception e) {
          // If it fails, remove this element
          resData = null;
          logger.log(Level.WARNING, "Failed to get resource " + imgUrl + " for reason: ",
              e);
        }

        if (resData != null && resData.getBytes() != null && resData.getMime() != null) {
          Resource res = ENMLUtil.buildResource(resData.getBytes(), resData
              .getMime(), resData.getFilename());
          element.tagName(ENMLConstants.EN_MEDIA_TAG);
          element.attr(ENMLConstants.EN_MEDIA_ATTR_TYPE, res.getMime());
          element.attr(ENMLConstants.EN_MEDIA_ATTR_HASH, ENMLUtil.bytesToHex(res.getData()
              .getBodyHash()));
          element.removeAttr(ENMLConstants.HTML_IMG_SRC_ATTR);
          resourceList.add(res);
        } else {
          return false;
        }
      } else {
        return false;
      }
    } else if (elementName.equals(ENMLConstants.HTML_ANCHOR_TAG)) {
      String href = element.absUrl(ENMLConstants.HTML_ANCHOR_HREF_ATTR);
      if (ENMLUtil.isAcceptableURL(href)) {
        element.attr(ENMLConstants.HTML_ANCHOR_HREF_ATTR, href);
      } else {
        return false;
      }

    }
    return true;
  }

  /**
   * Removes disallowed attributes and adds must-have attributes if possible
   * 
   * @param dtd
   * @param element
   */
  protected void cleanAttributes(SimpleENMLDTD dtd, Element element) {
    Attributes attrs = element.attributes();
    String elementName = element.tagName();

    Set<String> attrNameSet = new HashSet<String>();

    if (attrs != null) {
      Iterator<Attribute> it = attrs.iterator();
      while (it.hasNext()) {

        Attribute attr = it.next();
        String attrName = attr.getKey();
        String attrValue = attr.getValue();

        if (!dtd.isAttributeAllowed(elementName, attrName, attrValue)) {
          attrs.remove(attrName);
          continue;
        }

        DTDAttribute dtdAttr = dtd.getDTDAttriute(elementName, attrName);
        if (dtdAttr.getType() == AttributeType.NMTOKEN) {
          if (!DTDAttribute.NMTOKEN_PATTERN.matcher(attrValue).matches()) {
            attr.setValue(attrValue.replaceAll("[^\\d\\w\\-\\:]", "_"));
          } else if (dtdAttr.getType() == AttributeType.NMTOKENS) {
            if (DTDAttribute.NMTOKENS_PATTERN.matcher(attrValue).matches()) {
              attr.setValue(attrValue.replaceAll("[^\\d\\w\\-\\: ]", "_").trim());
            }
          }
        }

        attrNameSet.add(attrName);
      }
    }

    // add absent attributes
    Map<String, DTDAttribute> requiredAttrMap = dtd.getRequiredDTDAttriutes(elementName);
    if (requiredAttrMap != null && requiredAttrMap.size() > 0) {
      for (DTDAttribute dtdAttr : requiredAttrMap.values()) {
        if (!attrNameSet.contains(dtdAttr.getName())) {
          String value = dtdAttr.getValue();
          if (value == null && dtdAttr
              .getDefaultValueModel() == DefaultValueModel.REQUIRED) {
            if (dtdAttr.getType() == AttributeType.SET) {
              value = (String) dtdAttr.getEnumeratedValues().toArray()[0];
            } else {
              value = "unknown";
            }
          }
          element.attr(dtdAttr.getName(), value);
        }
      }
    }

  }

}
