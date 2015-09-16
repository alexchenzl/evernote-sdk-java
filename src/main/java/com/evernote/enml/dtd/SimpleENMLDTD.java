/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml.dtd;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import com.evernote.enml.ENMLConstants;

/**
 * Provides an object model of the ENML DtD
 * 
 * 
 */
public class SimpleENMLDTD implements DeclHandler {

  private static SimpleENMLDTD instance;

  protected final Map<String, DTDElement> elementDefiniationMap =
      new HashMap<String, DTDElement>();

  /**
   * Obtains a singleton instance of the {@link SimpleENMLDTD}
   * 
   */
  public synchronized static final SimpleENMLDTD getInstance() {
    if (instance == null) {
      instance = new SimpleENMLDTD();
      instance.initialize();
    }
    return instance;
  }

  public void elementDecl(String name, String modelString) throws SAXException {
    DTDElement element = findOrCreate(name);
    element.setContentModel(modelString);
    elementDefiniationMap.put(name, element);

  }

  public void attributeDecl(String elementName, String attributeName, String type,
      String mode, String value) throws SAXException {

    DTDAttribute.AttributeType attributeType = null;
    Set<String> enumeratedValues = null;

    if (type.startsWith("(")) {
      enumeratedValues = new HashSet<String>();
      attributeType = DTDAttribute.AttributeType.SET;
      for (String possibleValue : type.substring(1, type.length() - 1).split("\\|")) {
        enumeratedValues.add(possibleValue);
      }
    } else {
      attributeType = DTDAttribute.AttributeType.valueOf(type);
    }
    DTDAttribute.DefaultValueModel defaultingModel = null;
    if (mode != null && mode.length() > 1) {
      defaultingModel = DTDAttribute.DefaultValueModel.valueOf(mode.substring(1));
    }
    DTDAttribute attribute = new DTDAttribute(attributeName, attributeType,
        enumeratedValues, defaultingModel, value);
    DTDElement element = findOrCreate(elementName);
    element.getAllAttributes().put(attribute.getName(), attribute);
    if (defaultingModel == DTDAttribute.DefaultValueModel.REQUIRED) {
      element.getRequiredAttributes().put(attribute.getName(), attribute);
    }
  }

  public void internalEntityDecl(String name, String value) throws SAXException {

  }

  public void externalEntityDecl(String name, String publicId, String systemId)
      throws SAXException {

  }

  public DTDElement getDTDElement(String elementName) {
    return elementDefiniationMap.get(elementName);
  }

  public boolean isElementAllowed(String elementName) {
    return (elementDefiniationMap.get(elementName) != null);
  }

  public DTDAttribute getDTDAttriute(String elementName, String attrName) {
    DTDElement element = elementDefiniationMap.get(elementName);
    if (element != null) {
      return element.getAllAttributes().get(attrName);
    }
    return null;
  }

  public final Map<String, DTDAttribute> getAllDTDAttributes(String elementName) {
    DTDElement element = elementDefiniationMap.get(elementName);
    if (element != null) {
      return element.getAllAttributes();
    }
    return null;
  }

  public final Map<String, DTDAttribute> getRequiredDTDAttriutes(String elementName) {
    DTDElement element = elementDefiniationMap.get(elementName);
    if (element != null) {
      return element.getRequiredAttributes();
    }
    return null;
  }

  public boolean isAttributeAllowed(String elementName, String attrName,
      String attrValue) {

    DTDElement element = elementDefiniationMap.get(elementName);
    if (element == null) {
      return false;
    }

    DTDAttribute attribute = element.getAllAttributes().get(attrName);
    if (attribute == null) {
      return false;
    }

    if (attribute.getType() == DTDAttribute.AttributeType.SET) {
      if (!attribute.getEnumeratedValues().contains(attrValue))
        return false;
    }

    if (attribute.getDefaultValueModel() == DTDAttribute.DefaultValueModel.REQUIRED) {
      if (attrValue == null) {
        return false;
      }
    }

    if (attribute.getDefaultValueModel() == DTDAttribute.DefaultValueModel.FIXED) {
      if (attrValue == null || !attrValue.equals(attribute.getValue())) {
        return false;
      }
    }

    return true;
  }

  private DTDElement findOrCreate(String name) {
    DTDElement element = elementDefiniationMap.get(name);
    if (element == null) {
      element = new DTDElement(name);
    }
    return element;
  }

  /**
   * This function is used to parse the ENML DTD file
   * 
   * @return {@code true} if it's successful to the parseENML DTD file
   */
  @SuppressWarnings("unused")
  private boolean parseENMLDTD() {
    try {
      XMLReader xr = XMLReaderFactory.createXMLReader();
      StringBuilder builder = new StringBuilder();
      builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      builder.append("<!DOCTYPE en-note SYSTEM \"" + ENMLConstants.DTD_URL + "\">\n");
      builder.append("<en-note><img/><br/></en-note>\n");
      InputSource source = new InputSource(new StringReader(builder.toString()));
      XMLFilterImpl defaultHandler = new XMLFilterImpl(xr);
      defaultHandler.setProperty("http://xml.org/sax/properties/declaration-handler",
          this);
      defaultHandler.parse(source);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * The function call sequence in this function are generated through calling
   * parseENMLDTD to parse the ENML DTD file. Please DO NOT change it manually.
   * 
   * Two reasons to do this: parseENMLDTD may fail on Android because the DeclHandler is
   * not supported by org.xmlpull.v1.sax2.Driver
   * 
   */
  private void initialize() {
    try {
      elementDecl("en-note",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("en-note", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("en-note", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("en-note", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("en-note", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("en-note", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("en-note", "bgcolor", "CDATA", "#IMPLIED", null);
      attributeDecl("en-note", "text", "CDATA", "#IMPLIED", null);
      attributeDecl("en-note", "xmlns", "CDATA", "#FIXED",
          "http://xml.evernote.com/pub/enml2.dtd");
      elementDecl("en-crypt", "(#PCDATA)");
      attributeDecl("en-crypt", "hint", "CDATA", "#IMPLIED", null);
      attributeDecl("en-crypt", "cipher", "CDATA", null, "RC2");
      attributeDecl("en-crypt", "length", "CDATA", null, "64");
      elementDecl("en-todo", "EMPTY");
      attributeDecl("en-todo", "checked", "(true|false)", null, "false");
      elementDecl("en-media", "EMPTY");
      attributeDecl("en-media", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "type", "CDATA", "#REQUIRED", null);
      attributeDecl("en-media", "hash", "CDATA", "#REQUIRED", null);
      attributeDecl("en-media", "height", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "width", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "usemap", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "border", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "hspace", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "vspace", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "longdesc", "CDATA", "#IMPLIED", null);
      attributeDecl("en-media", "alt", "CDATA", "#IMPLIED", null);
      elementDecl("a",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("a", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "accesskey", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "tabindex", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "charset", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "type", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "name", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "href", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "hreflang", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "rel", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "rev", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "shape", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "coords", "CDATA", "#IMPLIED", null);
      attributeDecl("a", "target", "CDATA", "#IMPLIED", null);
      elementDecl("abbr",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("abbr", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("abbr", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("abbr", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("abbr", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("abbr", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("acronym",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("acronym", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("acronym", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("acronym", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("acronym", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("acronym", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("address",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("address", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("address", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("address", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("address", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("address", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("area",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("area", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "accesskey", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "tabindex", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "shape", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "coords", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "href", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "nohref", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "alt", "CDATA", "#IMPLIED", null);
      attributeDecl("area", "target", "CDATA", "#IMPLIED", null);
      elementDecl("b",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("b", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("b", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("b", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("b", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("b", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("bdo",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("bdo", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("bdo", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("bdo", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("bdo", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("bdo", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("big",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("big", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("big", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("big", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("big", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("big", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("blockquote",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("blockquote", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("blockquote", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("blockquote", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("blockquote", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("blockquote", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("blockquote", "cite", "CDATA", "#IMPLIED", null);
      elementDecl("br",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("br", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("br", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("br", "clear", "CDATA", "#IMPLIED", null);
      elementDecl("caption",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("caption", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("caption", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("caption", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("caption", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("caption", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("caption", "align", "CDATA", "#IMPLIED", null);
      elementDecl("center",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("center", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("center", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("center", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("center", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("center", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("cite",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("cite", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("cite", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("cite", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("cite", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("cite", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("code",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("code", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("code", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("code", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("code", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("code", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("col",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("col", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "valign", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "span", "CDATA", "#IMPLIED", null);
      attributeDecl("col", "width", "CDATA", "#IMPLIED", null);
      elementDecl("colgroup",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("colgroup", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "valign", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "span", "CDATA", "#IMPLIED", null);
      attributeDecl("colgroup", "width", "CDATA", "#IMPLIED", null);
      elementDecl("dd",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("dd", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("dd", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("dd", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dd", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dd", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("del",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("del", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("del", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("del", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("del", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("del", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("del", "cite", "CDATA", "#IMPLIED", null);
      attributeDecl("del", "datetime", "CDATA", "#IMPLIED", null);
      elementDecl("dfn",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("dfn", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("dfn", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("dfn", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dfn", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dfn", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("div",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("div", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("div", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("div", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("div", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("div", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("div", "align", "CDATA", "#IMPLIED", null);
      elementDecl("dl",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("dl", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("dl", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("dl", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dl", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dl", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("dl", "compact", "CDATA", "#IMPLIED", null);
      elementDecl("dt",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("dt", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("dt", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("dt", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dt", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("dt", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("em",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("em", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("em", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("em", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("em", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("em", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("font",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("font", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("font", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("font", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("font", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("font", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("font", "size", "CDATA", "#IMPLIED", null);
      attributeDecl("font", "color", "CDATA", "#IMPLIED", null);
      attributeDecl("font", "face", "CDATA", "#IMPLIED", null);
      elementDecl("h1",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("h1", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("h1", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("h1", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h1", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h1", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("h1", "align", "CDATA", "#IMPLIED", null);
      elementDecl("h2",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("h2", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("h2", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("h2", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h2", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h2", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("h2", "align", "CDATA", "#IMPLIED", null);
      elementDecl("h3",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("h3", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("h3", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("h3", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h3", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h3", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("h3", "align", "CDATA", "#IMPLIED", null);
      elementDecl("h4",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("h4", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("h4", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("h4", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h4", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h4", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("h4", "align", "CDATA", "#IMPLIED", null);
      elementDecl("h5",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("h5", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("h5", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("h5", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h5", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h5", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("h5", "align", "CDATA", "#IMPLIED", null);
      elementDecl("h6",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("h6", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("h6", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("h6", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h6", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("h6", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("h6", "align", "CDATA", "#IMPLIED", null);
      elementDecl("hr",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("hr", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "noshade", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "size", "CDATA", "#IMPLIED", null);
      attributeDecl("hr", "width", "CDATA", "#IMPLIED", null);
      elementDecl("i",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("i", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("i", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("i", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("i", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("i", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("img",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("img", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "src", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "alt", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "name", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "longdesc", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "height", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "width", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "usemap", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "ismap", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "border", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "hspace", "CDATA", "#IMPLIED", null);
      attributeDecl("img", "vspace", "CDATA", "#IMPLIED", null);
      elementDecl("ins",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("ins", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("ins", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("ins", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("ins", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("ins", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("ins", "cite", "CDATA", "#IMPLIED", null);
      attributeDecl("ins", "datetime", "CDATA", "#IMPLIED", null);
      elementDecl("kbd",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("kbd", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("kbd", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("kbd", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("kbd", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("kbd", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("li",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("li", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("li", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("li", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("li", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("li", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("li", "type", "CDATA", "#IMPLIED", null);
      attributeDecl("li", "value", "CDATA", "#IMPLIED", null);
      elementDecl("map",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("map", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("map", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("map", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("map", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("map", "name", "CDATA", "#IMPLIED", null);
      elementDecl("ol",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("ol", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("ol", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("ol", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("ol", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("ol", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("ol", "type", "CDATA", "#IMPLIED", null);
      attributeDecl("ol", "compact", "CDATA", "#IMPLIED", null);
      attributeDecl("ol", "start", "CDATA", "#IMPLIED", null);
      elementDecl("p",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("p", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("p", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("p", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("p", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("p", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("p", "align", "CDATA", "#IMPLIED", null);
      elementDecl("pre",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("pre", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("pre", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("pre", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("pre", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("pre", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("pre", "width", "CDATA", "#IMPLIED", null);
      attributeDecl("pre", "xml:space", "(preserve)", "#FIXED", "preserve");
      elementDecl("q",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("q", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("q", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("q", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("q", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("q", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("q", "cite", "CDATA", "#IMPLIED", null);
      elementDecl("s",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("s", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("s", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("s", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("s", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("s", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("samp",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("samp", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("samp", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("samp", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("samp", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("samp", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("small",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("small", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("small", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("small", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("small", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("small", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("span",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("span", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("span", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("span", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("span", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("span", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("strike",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("strike", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("strike", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("strike", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("strike", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("strike", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("strong",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("strong", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("strong", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("strong", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("strong", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("strong", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("sub",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("sub", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("sub", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("sub", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("sub", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("sub", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("sup",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("sup", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("sup", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("sup", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("sup", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("sup", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("table",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("table", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "summary", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "width", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "border", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "cellspacing", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "cellpadding", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("table", "bgcolor", "CDATA", "#IMPLIED", null);
      elementDecl("tbody",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("tbody", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("tbody", "valign", "CDATA", "#IMPLIED", null);
      elementDecl("td",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("td", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "valign", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "abbr", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "rowspan", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "colspan", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "nowrap", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "bgcolor", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "width", "CDATA", "#IMPLIED", null);
      attributeDecl("td", "height", "CDATA", "#IMPLIED", null);
      elementDecl("tfoot",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("tfoot", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("tfoot", "valign", "CDATA", "#IMPLIED", null);
      elementDecl("th",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("th", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "valign", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "abbr", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "rowspan", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "colspan", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "nowrap", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "bgcolor", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "width", "CDATA", "#IMPLIED", null);
      attributeDecl("th", "height", "CDATA", "#IMPLIED", null);
      elementDecl("thead",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("thead", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("thead", "valign", "CDATA", "#IMPLIED", null);
      elementDecl("tr",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("tr", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "align", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "char", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "charoff", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "valign", "CDATA", "#IMPLIED", null);
      attributeDecl("tr", "bgcolor", "CDATA", "#IMPLIED", null);
      elementDecl("tt",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("tt", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("tt", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("tt", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tt", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("tt", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("u",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("u", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("u", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("u", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("u", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("u", "dir", "CDATA", "#IMPLIED", null);
      elementDecl("ul",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("ul", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("ul", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("ul", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("ul", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("ul", "dir", "CDATA", "#IMPLIED", null);
      attributeDecl("ul", "type", "CDATA", "#IMPLIED", null);
      attributeDecl("ul", "compact", "CDATA", "#IMPLIED", null);
      elementDecl("var",
          "(#PCDATA|a|abbr|acronym|address|area|b|bdo|big|blockquote|br|caption|center|cite|code|col|colgroup|dd|del|dfn|div|dl|dt|em|en-crypt|en-media|en-todo|font|h1|h2|h3|h4|h5|h6|hr|i|img|ins|kbd|li|map|ol|p|pre|q|s|samp|small|span|strike|strong|sub|sup|table|tbody|td|tfoot|th|thead|tr|tt|u|ul|var)*");
      attributeDecl("var", "style", "CDATA", "#IMPLIED", null);
      attributeDecl("var", "title", "CDATA", "#IMPLIED", null);
      attributeDecl("var", "lang", "CDATA", "#IMPLIED", null);
      attributeDecl("var", "xml:lang", "CDATA", "#IMPLIED", null);
      attributeDecl("var", "dir", "CDATA", "#IMPLIED", null);
    } catch (SAXException e) {
      // should never happen
    }
  }
}
