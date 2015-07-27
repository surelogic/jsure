/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/SAXElement.java,v 1.7 2007/07/30 20:15:08 swhitman Exp $*/
package com.surelogic.xml;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;

/**
 * Simple class to keep track of xml elements, their attributes, and the tagged
 * data.
 * 
 * @author Spencer.Whitman
 */
public class SAXElement {
  private final String name;
  private HashMap<String, String> attMap = new HashMap<>(TestXMLParserConstants.HASH_MAP_LOAD);
  private String cdata = "";

  SAXElement(String name) {
    this.name = name;
  }

  SAXElement(String name, Attributes atts) {
    this.name = name;

    if (atts != null) {
      for (int i = 0; i < atts.getLength(); i++) {
        if ("".equals(atts.getValue(i)))
          continue;

        String aName = atts.getLocalName(i);
        if ("".equals(aName))
          aName = atts.getQName(i);
        attMap.put(aName, atts.getValue(i));
      }
    }

  }

  public String getName() {
    return name;
  }

  public String getCdata() {
    return cdata;
  }

  public String getAttribute(String attrName) {
    return attMap.get(attrName);
  }

  public Map<String, String> getAttributes() {
    return attMap;
  }

  /**
   * addToCdata concats onto cdata, because the parser may generate several
   * events for the same cdata string
   */
  public void addToCdata(String s) {
    if ("".equals(cdata))
      cdata = s;
    else {
      cdata = cdata.concat(s);
    }
  }

  @Override
  public String toString() {
    String str = ("".equals(name) ? "" : name);

    for (String k : attMap.keySet()) {
      str += " ATTR: " + k + "=" + attMap.get(k);
    }

    if (!"".equals(cdata))
      str += (" <" + cdata + ">");
    return str;
  }
}
