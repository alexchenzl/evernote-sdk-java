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
import org.jsoup.select.Elements;

import com.evernote.edam.type.Data;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.ResourceAttributes;
import com.evernote.enml.BinaryResource;
import com.evernote.enml.ENMLConstants;
import com.evernote.enml.ENMLUtil;
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
  // generated ENML content
  private String content;

  private ResourceFetcher fetcher;
  private HTMLElementHandler customizedHandler;

  private static final Pattern PATTERN_DISPLAY_NONE = Pattern.compile(
      "display\\s*:\\s*none");

  // some tags are visible but not permitted in ENML, try to convert them to DIV tags
  // so that the content in these tags may be kept in the generated ENML content
  protected static Set<String> toDIVTagSet = new HashSet<String>();

  // some tags may need to be converted to span tags
  protected static Set<String> toSpanTagSet = new HashSet<String>();

  static {
    toDIVTagSet.add("section");
    toDIVTagSet.add("fieldset");
    toDIVTagSet.add("main");
    toDIVTagSet.add("article");
    toDIVTagSet.add("aside");
    toDIVTagSet.add("summary");
    toDIVTagSet.add("details");
    toDIVTagSet.add("figcaption");
    toDIVTagSet.add("figure");
    toDIVTagSet.add("header");
    toDIVTagSet.add("footer");
    toDIVTagSet.add("nav");
    toDIVTagSet.add("form");

    toSpanTagSet.add("mark");
    toSpanTagSet.add("label");
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
   * Get extracted title from the HTML head tag. It may be null.
   *
   * @return The title string
   */
  public String getTitle() {
    return title;
  }

  /**
   * Get generated ENML content.
   * 
   * This content doesn't include ENML header and ENML root tag en-note.
   * 
   * @return The ENML string content
   */
  public String getContent() {
    return content;
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
    doc.outputSettings().prettyPrint(false);

    title = doc.title();
    if (title != null) {
      title = ENMLUtil.cleanString(title);
    }

    if (customizedHandler != null) {
      customizedHandler.initialize();
    }

    if (selector == null || selector.isEmpty()) {
      Element bodyElement = doc.body();
      if (bodyElement != null) {
        processElement(bodyElement);
        content = bodyElement.toString();
        return true;
      }
      return false;
    }

    Elements elts = doc.select(selector);
    if (elts != null) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < elts.size(); i++) {
        Element elt = elts.get(i);
        processElement(elt);
        builder.append(elt.toString());
      }
      content = builder.toString();
      return true;
    }
    return false;
  }

  protected void processElement(Element element) {
    if (element == null) {
      return;
    }
    // tag names in ENML must be all lowercase
    element.tagName(element.tagName().toLowerCase().trim());

    // call user defined element handler here
    if (customizedHandler != null) {
      if (!customizedHandler.process(element, fetcher)) {
        return;
      }
    }

    // convert some special tags to ENML tags
    // for example: body --> div, img --> en-media
    if (!transformSpecialTags(element)) {
      return;
    }

    // remove disallowed elements
    SimpleENMLDTD dtd = SimpleENMLDTD.getInstance();
    if (!dtd.isElementAllowed(element.tagName())) {
      element.remove();
      return;
    }

    // clean attributes
    cleanAttributes(dtd, element);

    // process children
    Elements children = element.children();
    if (children != null) {
      Iterator<Element> it = children.iterator();
      while (it.hasNext()) {
        Element element2 = it.next();
        if (element2 != null) {
          processElement(element2);
        }
      }
    }
  }

  /**
   * 
   * @param element
   * @return {@code true} if this element needs further processing, false means the
   *         element is removed
   */
  protected boolean transformSpecialTags(Element element) {

    // remove hidden elements
    String style = element.attr(ENMLConstants.HTML_STYLE_ATTR);
    if (style != null) {
      style = style.toLowerCase();
      Matcher matcher = PATTERN_DISPLAY_NONE.matcher(style);
      if (matcher.find()) {
        element.remove();
        return false;
      }
    }

    String elementName = element.tagName();
    // convert some tags to div or span
    if (toDIVTagSet.contains(elementName)) {
      element.tagName(ENMLConstants.HTML_DIV_TAG);
    } else if (toSpanTagSet.contains(elementName)) {
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
      if (ENMLUtil.isValidURL(imgUrl)) {
        imgUrl = ENMLUtil.escapeURL(imgUrl);
        if (resourceList == null) {
          element.attr(ENMLConstants.HTML_IMG_SRC_ATTR, imgUrl);
          return true;
        }

        if (!fetcher.isAllowedURL(imgUrl)) {
          logger.log(Level.INFO, "Prohibited image url " + imgUrl);
          element.remove();
          return false;
        }

        BinaryResource binaryResource;
        try {
          binaryResource = fetcher.fetchResource(imgUrl, null);
        } catch (Exception e) {
          // If it fails, remove this element
          binaryResource = null;
          logger.log(Level.WARNING, "Failed to get resource " + imgUrl + " for reason: "
              + e.getMessage());
        }

        if (binaryResource != null) {
          Resource res = buildResource(binaryResource.getBytes(), binaryResource
              .getMime(), binaryResource.getFilename());
          element.tagName(ENMLConstants.EN_MEDIA_TAG);
          element.attr(ENMLConstants.EN_MEDIA_ATTR_TYPE, res.getMime());
          element.attr(ENMLConstants.EN_MEDIA_ATTR_HASH, ENMLUtil.bytesToHex(res.getData()
              .getBodyHash()));
          element.removeAttr(ENMLConstants.HTML_IMG_SRC_ATTR);
          resourceList.add(res);
        } else {
          element.remove();
          return false;
        }
      } else {
        element.remove();
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

        if (attrName.equalsIgnoreCase(ENMLConstants.HTML_ANCHOR_HREF_ATTR) || attrName
            .equalsIgnoreCase(ENMLConstants.HTML_IMG_SRC_ATTR)) {
          if (ENMLUtil.isValidURL(attrValue)) {
            attr.setValue(ENMLUtil.escapeURL(attrValue));
          } else {
            attrs.remove(attrName);
            continue;
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

  private Resource buildResource(byte[] bytes, String mime, String filename) {

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

}
