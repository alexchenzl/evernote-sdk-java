/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml.dtd;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple representation of a XML element declaration including the name, attributes (
 * {@link DTDAttribute} ) and element content model string components.
 * 
 */
public class DTDElement {

  private final String name;

  private String contentModel;

  private final Map<String, DTDAttribute> allAttributes =
      new HashMap<String, DTDAttribute>();
  private final Map<String, DTDAttribute> requiredAttributes =
      new HashMap<String, DTDAttribute>();

  public DTDElement(String name, String contentModel) {
    this.name = name;
    this.contentModel = contentModel;
  }

  public DTDElement(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name + ": attr[" + allAttributes.values() + "]  model: [" + contentModel + "]";
  }

  public String getName() {
    return name;
  }

  public String getContentModel() {
    return contentModel;
  }

  public void setContentModel(String contentModel) {
    this.contentModel = contentModel;
  }

  public Map<String, DTDAttribute> getAllAttributes() {
    return allAttributes;
  }

  public Map<String, DTDAttribute> getRequiredAttributes() {
    return requiredAttributes;
  }
}