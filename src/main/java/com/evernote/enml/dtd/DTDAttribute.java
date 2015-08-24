/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml.dtd;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A DTD's representation of a XML element attribute.s
 * 
 */
public class DTDAttribute {
  public enum AttributeType {
    CDATA, ID, IDREF, IDREFS, NMTOKEN, NMTOKENS, ENTITY, ENTITIES, NOTATION, SET
  }

  public enum DefaultValueModel {
    IMPLIED, REQUIRED, FIXED
  }

  /**
   * {@link http://www.w3.org/TR/1998/REC-xml-19980210#NT-Nmtoken}
   */
  public static String NMTOKEN_PATTERN_STR = "[\\w\\.\\-\\:]+";
  public static String NMTOKENS_PATTERN_STR = "[\\w\\.\\-\\:]|";

  public static final Pattern NMTOKEN_PATTERN = Pattern.compile(NMTOKEN_PATTERN_STR);
  public static final Pattern NMTOKENS_PATTERN = Pattern.compile(NMTOKENS_PATTERN_STR);

  private String name;
  private AttributeType type;

  private DefaultValueModel defaultValueModel;
  private String value;
  private Set<String> enumeratedValues;

  public DTDAttribute(String name, AttributeType type, Set<String> enumeratedValues,
      DefaultValueModel defaultValueModel, String value) {
    this.name = name;
    this.type = type;
    this.enumeratedValues = enumeratedValues;
    this.defaultValueModel = defaultValueModel;
    this.value = value;
  }

  public DTDAttribute(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String toString() {
    return name
        + ":"
        + type
        + ":"
        + (enumeratedValues != null ? Arrays.deepToString(enumeratedValues.toArray())
            : "") + ":" + defaultValueModel;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AttributeType getType() {
    return type;
  }

  public void setType(AttributeType type) {
    this.type = type;
  }

  public DefaultValueModel getDefaultValueModel() {
    return defaultValueModel;
  }

  public void setDefaultValueModel(DefaultValueModel defaultValueModel) {
    this.defaultValueModel = defaultValueModel;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Set<String> getEnumeratedValues() {
    return enumeratedValues;
  }

  public void setEnumeratedValues(Set<String> enumeratedValues) {
    this.enumeratedValues = enumeratedValues;
  }

}