package com.surelogic.dropsea.irfree.drops;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.xml.sax.Attributes;

import com.surelogic.common.StringCache;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.irfree.NestedJSureXmlReader;

public class SeaSnapshotXMLReader extends NestedJSureXmlReader {

  final SeaSnapshotXMLReaderListener f_seaSnapListener;

  public SeaSnapshotXMLReader(SeaSnapshotXMLReaderListener l) {
    super(l);
    f_seaSnapListener = l;
  }

  public static List<IDrop> loadSnapshot(File location) throws Exception {
	  return loadSnapshot(null, location);
  }

  public static List<IDrop> loadSnapshot(ConcurrentMap<String, IJavaRef> cache, File location) throws Exception {
	  DeclUtil.setStringCache(new StringCache());
	  final SeaSnapshotXMLReaderListener l;
	  try {
		  l = new SeaSnapshotXMLReaderListener(cache);
		  new SeaSnapshotXMLReader(l).read(location);
	  } finally {
		  DeclUtil.setStringCache(null);
	  }
	  return l.getDrops();
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
