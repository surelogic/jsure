package com.surelogic.dropsea.irfree;

import org.xml.sax.Attributes;

import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.IXMLResultListener;
import com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader;

public class JSureSummaryXMLReader extends AbstractXMLReader {
  public static final String ROOT = "sea-summary";

  public static final String TIME_ATTR = "time";
  public static final String OFFSET_ATTR = "offset";

  public JSureSummaryXMLReader(IXMLResultListener l) {
    super(l);
  }

  @Override
  protected String checkForRoot(String name, Attributes attributes) {
    if (ROOT.equals(name)) {
      if (attributes == null) {
        return "";
      }
      return attributes.getValue(TIME_ATTR);
    }
    else if (SeaSnapshotXMLReader.ROOT.equals(name)) {
    	throw new IllegalStateException();
    }
    return null;
  }

  @Override
  protected void handleNestedEntity(Entity next, Entity last, String lastName) {
    // System.out.println("Looking at "+last.getName()+" in "+next.getName());
    next.addRef(last);
  }
}
