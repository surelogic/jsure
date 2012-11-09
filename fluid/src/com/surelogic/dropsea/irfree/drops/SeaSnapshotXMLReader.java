package com.surelogic.dropsea.irfree.drops;

import org.xml.sax.Attributes;

import com.surelogic.dropsea.irfree.Entity;
import com.surelogic.dropsea.irfree.NestedJSureXmlReader;

public class SeaSnapshotXMLReader extends NestedJSureXmlReader {

  final SeaSnapshotXMLReaderListener f_seaSnapListener;

  public SeaSnapshotXMLReader(SeaSnapshotXMLReaderListener l) {
    super(l);
    f_seaSnapListener = l;
  }

  @Override
  protected String checkForRoot(String name, Attributes attributes) {
    if (ROOT.equals(name)) {
      if (attributes == null) {
        return "";
      }
      return attributes.getValue(UID_ATTR);
    }
    return null;
  }

  @Override
  protected void handleNestedEntity(Entity next, Entity last, String lastName) {
    boolean obsoleteStuff = "source-ref".equals(lastName) || "java-decl-info".equals(lastName)
        || "supporting-info".equals(lastName);
    if (!obsoleteStuff) {
      // System.out.println("Finished '"+name+"' inside of "+next);
      next.addRef(last);
    }
  }
}
