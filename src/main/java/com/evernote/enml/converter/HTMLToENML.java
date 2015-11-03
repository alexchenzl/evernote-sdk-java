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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
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
import org.jsoup.nodes.TextNode;
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
  private HTMLNodeHandler customizedHandler;

  private static final Pattern PATTERN_DISPLAY_NONE = Pattern.compile(
      "display\\s*:\\s*none");

  // This pattern is used to find out not-allowed charachers in ENML content, Please refer
  // to XML character range http://www.w3.org/TR/xml/#charsets
  private static final Pattern INVALID_CONTENT_TEXT_PATTERN = Pattern
      .compile("[^\\x09\\x0A\\x0D\\u0020-\\uD7FF\\uE000-\\uFFFD\\x{10000}-\\x{10FFFF}]");
  // Control chars or line/paragraph separators are not allowed in title. This pattern is
  // also used to clean keywords
  private static final Pattern INVALID_TITLE_TEXT_PATTERN =
      Pattern.compile("[\\p{Cc}\\p{Zl}\\p{Zp}]");

  protected static final Map<String, String> TAG_TRANSFORM_MAP =
      new HashMap<String, String>();
  protected static final Set<String> TAG_TO_REMOVE_SET = new HashSet<String>();

  static {
    // tags to div
    TAG_TRANSFORM_MAP.put("html", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("body", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("form", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("main", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("fieldset", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("iframe", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("embed", ENMLConstants.HTML_DIV_TAG);
    // HTML5 tags
    TAG_TRANSFORM_MAP.put("article", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("aside", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("detailes", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("footer", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("header", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("figure", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("figcaption", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("hgroup", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("nav", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("section", ENMLConstants.HTML_DIV_TAG);
    TAG_TRANSFORM_MAP.put("summary", ENMLConstants.HTML_DIV_TAG);
    // tags to span
    TAG_TRANSFORM_MAP.put("legend", ENMLConstants.HTML_SPAN_TAG);
    TAG_TRANSFORM_MAP.put("label", ENMLConstants.HTML_SPAN_TAG);
    TAG_TRANSFORM_MAP.put("highlight", ENMLConstants.HTML_SPAN_TAG);
    TAG_TRANSFORM_MAP.put("mark", ENMLConstants.HTML_SPAN_TAG);
    // tags to img
    TAG_TRANSFORM_MAP.put("canvas", ENMLConstants.HTML_IMG_TAG);
    TAG_TRANSFORM_MAP.put("video", ENMLConstants.HTML_IMG_TAG);

    // tags to remove
    TAG_TO_REMOVE_SET.add("script");
    TAG_TO_REMOVE_SET.add("noscript");
    TAG_TO_REMOVE_SET.add("ruby");
    TAG_TO_REMOVE_SET.add("link");
    TAG_TO_REMOVE_SET.add("style");
  }

  /**
   * Construct a HTMLToENML object
   * 
   * @param fetcher
   * @param customizedHandler
   */
  public HTMLToENML(ResourceFetcher fetcher, HTMLNodeHandler customizedHandler) {
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
   * {@link HTMLNodeHandler}
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
      } else {
        title = removeInvalidChar(title, INVALID_TITLE_TEXT_PATTERN);
      }
    }

    if (customizedHandler != null) {
      customizedHandler.initialize();
      keywords = customizedHandler.extractKeywords(doc);
    }

    if (keywords == null || keywords.isEmpty()) {
      extractKeywords(doc);
    }

    if (keywords != null) {
      keywords = removeInvalidChar(keywords, INVALID_TITLE_TEXT_PATTERN);
    }

    if (selector == null || selector.isEmpty()) {
      Element bodyElement = doc.body();
      if (bodyElement != null) {
        if (traverse(bodyElement)) {
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
        if (traverse(elt)) {
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

  /**
   * 
   * Depth-first traverse
   * 
   */
  protected boolean traverse(Node root) {
    Node node = root;
    int depth = 0;
    boolean result = false;
    Stack<Boolean> resultStack = new Stack<Boolean>();
    while (node != null) {
      result = head(node, depth);
      resultStack.push(result);
      if (node.childNodeSize() > 0) {
        node = node.childNode(0);
        depth++;
      } else {
        Node nextSibling = node.nextSibling();
        while (nextSibling == null && depth > 0) {
          Node parent = node.parentNode();
          result = resultStack.pop();
          tail(node, depth, result);
          node = parent;
          nextSibling = node.nextSibling();
          depth--;
        }

        result = resultStack.pop();
        tail(node, depth, result);
        if (node == root)
          break;
        node = nextSibling;
      }
    }
    return result;
  }

  protected boolean head(Node node, int depth) {

    if (!(node instanceof Element) && !(node instanceof TextNode)) {
      return false;
    }

    // call user defined element handler here
    if (customizedHandler != null) {
      if (!customizedHandler.process(node, fetcher)) {
        return false;
      }
    }

    if (node instanceof Element) {
      Element element = (Element) node;
      // tag names in ENML must be all lowercase
      element.tagName(element.tagName().toLowerCase());

      // convert some special tags to ENML tags
      // for example: body --> div, img --> en-media
      if (!transformSpecialTags(element)) {
        return false;
      }

      // If it's not specifically allowed, either, then we'll turn it into a span, this
      // preserves the content of special node types from HTML5 and the future.
      SimpleENMLDTD dtd = SimpleENMLDTD.getInstance();
      if (!dtd.isElementAllowed(element.tagName())) {
        element.tagName(ENMLConstants.HTML_SPAN_TAG);
      }

      // clean attributes
      cleanAttributes(dtd, element);
    }

    return true;

  }

  public long replaceTime = 0;

  protected void tail(Node node, int depth, boolean result) {
    if (!result) {
      node.remove();
      return;
    }

    if (node instanceof TextNode) {
      TextNode textNode = (TextNode) node;
      String text = removeInvalidChar(textNode.text(), INVALID_CONTENT_TEXT_PATTERN);
      textNode.text(text);
    }

  }

  private String removeInvalidChar(String text, Pattern pattern) {
    Matcher m = pattern.matcher(text);
    StringBuffer sb = new StringBuffer(text.length());
    while (m.find()) {
      m.appendReplacement(sb, Matcher.quoteReplacement(""));
    }
    m.appendTail(sb);
    return sb.toString();
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

    String tagName = element.tagName();
    if (TAG_TO_REMOVE_SET.contains(tagName)) {
      return false;
    }

    String toTagName = null;
    if (tagName.equals(ENMLConstants.HTML_INPUT_TAG)) {
      String type = element.attr("type");
      if ("checkbox".equalsIgnoreCase(type)) {
        toTagName = ENMLConstants.EN_TODO_TAG;
      } else if ("image".equalsIgnoreCase(type)) {
        toTagName = ENMLConstants.HTML_IMG_TAG;
      } else {
        toTagName = ENMLConstants.HTML_SPAN_TAG;
      }
    } else {
      toTagName = TAG_TRANSFORM_MAP.get(tagName);
    }

    if (toTagName != null) {
      tagName = toTagName;
      element.tagName(tagName);
    }

    // img to en-media
    if (tagName.equals(ENMLConstants.HTML_IMG_TAG)) {
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
    } else if (tagName.equals(ENMLConstants.HTML_ANCHOR_TAG)) {
      // url to absolute url
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
