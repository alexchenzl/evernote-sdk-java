/**
 * Copyright 2015 Evernote Corporation.
 */
package com.evernote.enml.css;

import java.util.HashMap;
import java.util.Map;

public class CSSCascadingInfo {

  private Map<String, CSSSpecificity> specificityMap =
      new HashMap<String, CSSSpecificity>();

  private int inlineStylePos = 0;

  public Map<String, CSSSpecificity> getSpecificityMap() {
    return specificityMap;
  }

  public void setSpecificityMap(Map<String, CSSSpecificity> specificityMap) {
    this.specificityMap = specificityMap;
  }

  public int getInlineStylePos() {
    return inlineStylePos;
  }

  public void setInlineStylePos(int inlineStylePos) {
    this.inlineStylePos = inlineStylePos;
  }

}
