package com.surelogic.dropsea.irfree.drops;

import org.xml.sax.Attributes;

import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.refactor.IJavaDeclInfoClient;
import com.surelogic.common.refactor.JavaDeclInfo;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.MoreInfo;
import com.surelogic.common.xml.SourceRef;

public class SeaSnapshotXMLReader extends AbstractXMLReader {

  public static final String ROOT = "sea-snapshot";

  public static final String SUPPORTING_INFO = "supporting-info";
  public static final String JAVA_DECL_INFO = "java-decl-info";
  public static final String PROPERTIES = "properties";
  public static final String DEPONENT = "deponent";
  public static final String DEPENDENT = "dependent";

  public static final String UID_ATTR = "uid";
  public static final String ID_ATTR = "id";

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
    if ("source-ref".equals(lastName)) {
      String flavor = last.getAttribute(FLAVOR_ATTR);
      if (flavor != null) {
        // throw new UnsupportedOperationException();
        next.addRef(last);
      } else {
        next.setSource(new SourceRef(last));
      }
    } else if (SUPPORTING_INFO.equals(lastName)) {
      next.addInfo(new MoreInfo(last));
    } else if (JAVA_DECL_INFO.equals(lastName)) {
      JavaDeclInfo info = (JavaDeclInfo) last;
      if (next instanceof IJavaDeclInfoClient) {
        /*
         * Adds parents
         */
        ((IJavaDeclInfoClient) next).addInfo(info);
      } else {
        /*
         * We need to inform the associated drop (not the entity) so we ask the
         * listener for it. Likely this is a proposed promise we are trying to
         * stash java declaration information for refactoring onto.
         */
        f_seaSnapListener.handleJavaDecl(next, info);
      }
    } else {
      // System.out.println("Finished '"+name+"' inside of "+next);
      next.addRef(last);
    }
  }
}
