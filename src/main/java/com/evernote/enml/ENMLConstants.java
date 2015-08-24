/**
 * Copyright 2015 Evernote Corporation.
 */
package com.evernote.enml;

/**
 * Some constant strings that are frequently used in this library
 */
public class ENMLConstants {

  public static final String DTD_URL = "http://xml.evernote.com/pub/enml2.dtd";

  public static final String ENML_HEADER =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE en-note SYSTEM \"" + DTD_URL
          + "\">\n";

  // ENML special tags
  public static final String EN_ROOT_TAG = "en-note";

  public static final String EN_CRYPT_TAG = "en-crypt";

  public static final String EN_CRYPT_ATTR_CIPHER = "cipher";

  public static final String EN_TODO_TAG = "en-todo";

  public static final String EN_MEDIA_TAG = "en-media";
  public static final String EN_MEDIA_ATTR_HASH = "hash";
  public static final String EN_MEDIA_ATTR_TYPE = "type";
  public static final String EN_MEDIA_ATTR_WIDTH = "width";
  public static final String EN_MEDIA_ATTR_HEIGHT = "height";

  // some HTML tags and attributes
  public static final String HTML_IMG_TAG = "img";
  public static final String HTML_IMG_SRC_ATTR = "src";

  public static final String HTML_BODY = "body";

  public static final String HTML_DIV_TAG = "div";

  public static final String HTML_SPAN_TAG = "span";

  public static final String HTML_HR_TAG = "hr";

  public static final String HTML_BR_TAG = "br";

  public static final String HTML_INPUT_TAG = "input";

  public static final String HTML_LINK_TAG = "link";

  public static final String HTML_ANCHOR_TAG = "a";
  public static final String HTML_ANCHOR_HREF_ATTR = "href";
  public static final String HTML_ANCHOR_TARGET_ATTR = "target";
  public static final String HTML_ANCHOR_TARGET_ATTR_VALUE = "_blank";

  // some attributes
  public static final String HTML_STYLE_ATTR = "style";
  public static final String HTML_ID_ATTR = "id";
  public static final String HTML_CLASS_ATTR = "class";
  public static final String HTML_NAME_ATTR = "name";
  public static final String HTML_ALT_ATTR = "alt";
  public static final String HTML_MEDIA_ATTR = "media";

  public static final String CSS_MEDIA_TYPE_SCREEN = "screen";
  public static final String CSS_MEDIA_TYPE_ALL = "all";

}
