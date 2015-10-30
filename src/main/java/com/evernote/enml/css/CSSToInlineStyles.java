/**
 * Copyright 2015 Evernote Corporation. All rights reserved.
 */
package com.evernote.enml.css;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector.SelectorParseException;

import com.evernote.enml.ENMLConstants;
import com.evernote.enml.ENMLUtil;
import com.evernote.enml.ResourceData;
import com.evernote.enml.ResourceFetcher;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.decl.CSSExpressionMemberTermURI;
import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.CSSMediaQuery;
import com.helger.css.decl.CSSMediaRule;
import com.helger.css.decl.CSSSelector;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.ICSSExpressionMember;
import com.helger.css.decl.ICSSTopLevelRule;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderDeclarationList;
import com.helger.css.reader.errorhandler.DoNothingCSSParseErrorHandler;
import com.helger.css.writer.CSSWriterSettings;

/**
 * Converts an HTML document with external and embedded styles into an HTML document with
 * inline styles. Pseudo class and pseudo element can not be handled with this tool.
 * 
 * The instance is NOT thread safe.
 * 
 */
public class CSSToInlineStyles {

  private static final Logger logger = Logger.getLogger(CSSToInlineStyles.class
      .getName());

  // fetcher is used to download CSS files
  private final ResourceFetcher fetcher;

  private List<CascadingStyleSheet> styleSheetList;
  // Every style sheet has its own base URL
  private List<URL> styleSheetBaseURLList;

  // For temporary use when processing HTML, key is the value of the attribute
  // DATA_EN_TAGIDX
  private Map<Integer, CSSCascadingInfo> cascadingInfos =
      new HashMap<Integer, CSSCascadingInfo>();

  // Add a temporary attribute to the HTML tag element when processing HTML
  private static final String DATA_EN_TAGIDX = "data-en-tagidx";
  private int tagIdx = 0;

  private static final CSSWriterSettings CSS_WRITER_SETTINGS = new CSSWriterSettings(
      ECSSVersion.CSS30, true);

  private static final DoNothingCSSParseErrorHandler CSS_PARSE_ERROR_HANDLER =
      new DoNothingCSSParseErrorHandler();

  /**
   * 
   * A ResourceFetcher is required if it needs to download external CSS files or import
   * CSS rules. Otherwise the fetcher parameter can be null.
   * 
   * 
   */
  public CSSToInlineStyles(ResourceFetcher fetcher) {
    this.fetcher = fetcher;
  }

  /**
   * Extracts CSS file links from HTML content, then downloads them and parses them as
   * CascadingStyleSheet objects. If there are embedded styles, they will also be parsed.
   * 
   * @param doc
   * @param baseURLStr
   */
  private void addExternalAndEmbeddedStyles(Document doc, String baseURLStr) {

    Elements elts = doc.select("link[rel=\"stylesheet\"], style");
    for (Element elt : elts) {
      if (elt.tagName().equalsIgnoreCase("link")) {
        String href = elt.absUrl(ENMLConstants.HTML_ANCHOR_HREF_ATTR);
        downloadStyleSheet(elt.attr(ENMLConstants.HTML_MEDIA_ATTR), href);
      } else {
        addStyleSheet(elt.html(), baseURLStr);
      }
    }

  }

  /**
   * Downloads external style sheets in this HTML content, then converts them and embedded
   * styles into inline styles
   *
   * @param html
   * @param baseURLStr
   * @return The result HTML string
   */
  public String processHTML(String html, String baseURLStr) {
    return processHTML(html, baseURLStr, null);
  }

  /**
   * Apply specified style sheets to the HTML content, converting them into inline styles.
   * The external styles and embedded styles within the head section of this HTML content
   * will be ignored, but original inline styles will be kept.
   * <p>
   * If there's no style sheets specified, the external style sheets and embedded styles
   * included in this HTML content will be applied to the HTML content.
   * 
   *
   * @param html The html content to be processed
   * @param baseURLStr The base URL of this html content
   * @param cssPariList A list of {@link CSSPair} object, each item includes both CSS
   *          content and its base URL.
   * @return The result HTML string
   */
  public String processHTML(String html, String baseURLStr, List<CSSPair> cssPariList) {

    resetTempData();
    clearStyleSheet();

    Document doc = parseHTML(html, baseURLStr);
    if (doc == null) {
      return null;
    }

    if (cssPariList == null || cssPariList.isEmpty()) {
      addExternalAndEmbeddedStyles(doc, baseURLStr);
    } else {
      for (CSSPair item : cssPariList) {
        addStyleSheet(item.getCss(), item.getUrl());
      }
    }

    if (styleSheetList != null) {
      for (int i = 0; i < styleSheetList.size(); i++) {
        applyStylesFromCascadingStyleSheet(doc, i);
      }
    }
    return doc.toString();
  }

  /**
   * clear all added style sheets
   *
   */
  public void clearStyleSheet() {
    styleSheetList = null;
    styleSheetBaseURLList = null;
  }

  protected void resetTempData() {
    cascadingInfos.clear();
    tagIdx = 0;
  }

  protected Document parseHTML(String html, String baseURLStr) {

    if (html == null) {
      return null;
    }
    Document doc = Jsoup.parse(html, baseURLStr);
    doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
    doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
    doc.outputSettings().prettyPrint(false);
    return doc;
  }

  /**
   * Add style sheet together with its base URL to this object
   *
   * @param styleSheetContent
   * @param baseUrlStr
   * @return {@code true} if it's successful to add the style sheet content
   */
  protected boolean addStyleSheet(String styleSheetContent, String baseUrlStr) {

    if (styleSheetContent == null || styleSheetContent.isEmpty()) {
      return false;
    }

    if (styleSheetList == null) {
      styleSheetList = new ArrayList<CascadingStyleSheet>();
    }

    if (styleSheetBaseURLList == null) {
      styleSheetBaseURLList = new ArrayList<URL>();
    }

    CascadingStyleSheet sheet = CSSReader.readFromString(styleSheetContent, ENMLUtil.UTF8,
        ECSSVersion.CSS30, CSS_PARSE_ERROR_HANDLER);

    if (sheet != null) {
      URL baseURL = null;
      if (baseUrlStr != null) {
        try {
          baseURL = new URL(baseUrlStr);
        } catch (MalformedURLException e) {
          logger.log(Level.INFO,
              "Faild to add style sheet because of MalformedURLException " + baseUrlStr);
          return false;
        }
      }

      // download import rules and put their stylesheets before the current sheet
      List<CSSImportRule> importRules = sheet.getAllImportRules();
      for (int i = 0; i < importRules.size(); i++) {
        CSSImportRule ir = importRules.get(i);
        if (isValidMedia(ir)) {
          try {
            String irLocation = ir.getLocationString();
            if (baseURL != null) {
              URL irLocationURL = new URL(baseURL, irLocation);
              irLocation = irLocationURL.toString();
            }
            downloadStyleSheet(null, ir.getLocationString());
          } catch (MalformedURLException e) {
            // if it fails, just skip to next one
          }
        }
      }
      styleSheetList.add(sheet);
      styleSheetBaseURLList.add(baseURL);
      return true;
    }
    return false;
  }

  /**
   * Determines if rule is usable (i.e. no media specified, or it's "screen" or "all").
   *
   * @param rule
   * @return {@code true} if rule is usable
   */
  private boolean isValidMedia(CSSImportRule rule) {
    if (rule.hasMediaQueries()) {
      return isValidMedia(rule.getAllMediaQueries());
    } else {
      return true;
    }
  }

  private boolean isValidMedia(CSSMediaRule rule) {
    if (rule.hasMediaQueries()) {
      return isValidMedia(rule.getAllMediaQueries());
    } else {
      return true;
    }
  }

  private boolean isValidMedia(List<CSSMediaQuery> mediaQueries) {
    for (int i = 0; i < mediaQueries.size(); i++) {
      CSSMediaQuery mq = mediaQueries.get(i);
      // only want "screen" and "all"
      if (!mq.hasMediaExpressions()) {
        String medium = mq.getMedium();
        if (medium != null && (medium.equalsIgnoreCase(
            ENMLConstants.CSS_MEDIA_TYPE_SCREEN) || medium.equalsIgnoreCase(
                ENMLConstants.CSS_MEDIA_TYPE_ALL))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Iterates through all rules in a stylesheet and applies them to the DOM tree nodes.
   * The rules are added to the node's style attribute. For media rules, its inner style
   * rules are inserted at the location of the media rule. Those inner style rules are
   * processed the same way as normal style rules.
   *
   * @param doc DOM tree
   * @param sheetIndex
   */

  private void applyStylesFromCascadingStyleSheet(Document doc, int sheetIndex) {

    CascadingStyleSheet sheet = styleSheetList.get(sheetIndex);
    URL baseURL = styleSheetBaseURLList.get(sheetIndex);

    List<ICSSTopLevelRule> rules = sheet.getAllRules();
    int ruleIndex = 0;
    while (ruleIndex < rules.size()) {
      ICSSTopLevelRule topLevelRule = rules.get(ruleIndex);
      if (topLevelRule instanceof CSSStyleRule) {
        CSSStyleRule rule = (CSSStyleRule) topLevelRule;
        // a rule can have multiple selectors separated by commas. their specificity are
        // calculated individually, so they must be separated here
        List<CSSSelector> selectors = rule.getAllSelectors();
        for (CSSSelector selector : selectors) {
          List<CSSDeclaration> ruleDeclarations = rule.getAllDeclarations();
          try {

            String selString = selector.getAsCSSString(CSS_WRITER_SETTINGS, 0);
            if (selString.indexOf(":") > -1) {
              // the jsoup selector doesn't understand some pseudo elements or pseudo
              // classes, so convert the ones that can be converted (e.g. :link to
              // [href]) but don't modify the original selector object since that affects
              // how specificity is calculated (e.g. [href] has a higher specificity than
              // :link)
              if (selString.indexOf(":link") > -1) {
                selString = selString.replaceAll(":link", "[href]");
              } else {
                continue;
              }
            }

            Elements elts = null;
            try {
              elts = doc.select(selString);
            } catch (Exception e) {
              // Jsoup failed to execute this selector, just jump to next one;
              logger.log(Level.WARNING, "Jsoup failed to execute this selector \""
                  + selString + "\"");
              continue;
            }

            for (int j = 0; j < elts.size(); j++) {
              Element elt = elts.get(j);
              String style = elt.attr(ENMLConstants.HTML_STYLE_ATTR);

              Integer key = 0;
              String keyStr = elt.attr(DATA_EN_TAGIDX);
              if (keyStr != null && !keyStr.isEmpty()) {
                key = Integer.parseInt(keyStr);
              } else {
                key = tagIdx++;
                elt.attr(DATA_EN_TAGIDX, key.toString());
              }

              CSSDeclarationList existingDeclarationList = null;
              if (style != null && !style.isEmpty()) {
                existingDeclarationList = CSSReaderDeclarationList.readFromString(style,
                    ECSSVersion.CSS30, CSS_PARSE_ERROR_HANDLER);
              }
              if (existingDeclarationList == null) {
                existingDeclarationList = new CSSDeclarationList();
              }

              CSSCascadingInfo cascadingInfo = cascadingInfos.get(key);
              if (cascadingInfo == null) {
                cascadingInfo = new CSSCascadingInfo();
              }

              // records start position of original inline styles to make sure
              // external styles will be inserted before original inline styles
              int inlineStylePos = cascadingInfo.getInlineStylePos();

              for (CSSDeclaration declaration : ruleDeclarations) {
                if (baseURL != null) {
                  try {
                    normalizeUrl(baseURL, declaration);
                  } catch (MalformedURLException e) {
                    // if it fails just leave it alone
                  }
                }

                CSSSpecificity newSpecificity = new CSSSpecificity(selector, declaration);
                String property = declaration.getProperty();
                CSSDeclaration existingDeclaration = existingDeclarationList
                    .getDeclarationOfPropertyName(property);

                if (existingDeclaration == null) {
                  existingDeclarationList.addDeclaration(inlineStylePos++, declaration);
                  cascadingInfo.getSpecificityMap().put(property, newSpecificity);
                } else {
                  CSSSpecificity existingSpecificity = cascadingInfo.getSpecificityMap()
                      .get(property);
                  if (existingSpecificity == null) {
                    // means the existing declaration is an original inline declaration
                    existingSpecificity = new CSSSpecificity(existingDeclaration);
                    cascadingInfo.getSpecificityMap().put(property, existingSpecificity);
                  }

                  if (newSpecificity.compareTo(existingSpecificity) >= 0) {
                    existingDeclarationList.addDeclaration(inlineStylePos, declaration);
                    existingDeclarationList.removeDeclaration(existingDeclaration);
                    if (existingSpecificity.isInlineStyle()) {
                      inlineStylePos++;
                    }
                    // mark the specificity
                    cascadingInfo.getSpecificityMap().put(property, newSpecificity);
                  }
                }
              }
              style = existingDeclarationList.getAsCSSString(CSS_WRITER_SETTINGS, 0);
              elt.attr(ENMLConstants.HTML_STYLE_ATTR, style);

              cascadingInfo.setInlineStylePos(inlineStylePos);
              cascadingInfos.put(key, cascadingInfo);
            }
          } catch (SelectorParseException e) {
            logger.log(Level.WARNING, "Error selecting CSS selector", e);
          }
        }
      } else if (topLevelRule instanceof CSSMediaRule) {
        CSSMediaRule rule = (CSSMediaRule) topLevelRule;
        if (isValidMedia(rule)) {
          // remove media rule and add its style rules
          rules.remove(ruleIndex);
          List<ICSSTopLevelRule> innerRules = rule.getAllRules();
          rules.addAll(ruleIndex, innerRules);
          ruleIndex--;
        }
      }
      ruleIndex++;
    }
  }

  /**
   * Converts relative URL in CSS declarations to absolute URL. For example, if there is a
   * style rule "background-image:url(foo.png);", it will try to convert "foo.png" to an
   * absolute URL.
   *
   * @param baseURL
   * @param decl
   * @throws MalformedURLException
   */
  private void normalizeUrl(URL baseURL, CSSDeclaration decl)
      throws MalformedURLException {
    List<ICSSExpressionMember> members = decl.getExpression().getAllMembers();
    for (ICSSExpressionMember member : members) {
      if (member instanceof CSSExpressionMemberTermURI) {
        CSSExpressionMemberTermURI uriMember = (CSSExpressionMemberTermURI) member;
        uriMember.setURIString(new URL(baseURL, uriMember.getURIString()).toString());
      }
    }
  }

  private boolean downloadStyleSheet(String mediaAttr, String styleSheetUrlStr) {
    if (fetcher == null) {
      return false;
    }
    if (mediaAttr == null || mediaAttr.isEmpty() || mediaAttr.equalsIgnoreCase("screen")
        || mediaAttr.equalsIgnoreCase("all")) {

      if (!fetcher.isAllowedURL(styleSheetUrlStr)) {
        return false;
      }

      try {
        ResourceData resData = fetcher.fetchResource(styleSheetUrlStr, null);
        if (resData == null) {
          return false;
        }

        String baseUrl = resData.getFinalUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
          baseUrl = styleSheetUrlStr;
        }
        return addStyleSheet(resData.asString(), baseUrl);

      } catch (Exception e) {
        logger.log(Level.WARNING, "Faild to download style sheet " + styleSheetUrlStr
            + " for reason: " + e.getMessage());
      }

    }
    return false;
  }

}
