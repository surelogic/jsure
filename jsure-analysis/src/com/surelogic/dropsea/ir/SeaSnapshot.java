package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DROP_TYPE_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FLAVOR_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FULL_TYPE_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ID_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.JAVA_REF;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROJECT_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROPERTIES;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ROOT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.TYPE_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.UID_ATTR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPOutputStream;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.dropsea.ISnapshotDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.MarkedIRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class SeaSnapshot extends XmlCreator {
  /**
   * For creation
   */
  private final Map<Drop, String> idMap = new HashMap<>();

  /**
   * For loading
   */
  private final ConcurrentMap<String, IJavaRef> refCache = new ConcurrentHashMap<>();
  
  public SeaSnapshot(File location) throws IOException {
    super(location != null ? 
    		location.getName().endsWith(FileUtility.GZIP_SUFFIX) ?
    				new GZIPOutputStream(new FileOutputStream(location)) :
        			new FileOutputStream(location) : 
    		null);
  }

  public static SeaSnapshot create() {
    try {
      return new SeaSnapshot(null);
    } catch (IOException e) {
      return null;
    }
  }

  public ConcurrentMap<String, IJavaRef> getRefCache() {
	  return refCache;
  }

  public void clear() {
    refCache.clear();
    idMap.clear();
  }

  private String computeId(Drop d) {
    String id = idMap.get(d);
    if (id == null) {
      int size = idMap.size();
      id = Integer.toString(size);
      idMap.put(d, id);
    }
    return id;
  }

  public void snapshot(String project, final Sea sea) throws IOException {
    try {
      b.start(ROOT);
      b.addAttribute(UID_ATTR, UUID.randomUUID().toString());
      b.addAttribute(PROJECT_ATTR, project);

      for (Drop d : sea.getDrops()) {
        snapshotDrop(d);
      }
    } finally {
      b.end();
      close();
    }
  }

  private static boolean preprocessRefs = false;

  public void snapshotDrop(Drop d) {
    if (preprocessRefs && idMap.containsKey(d)) {
      return;
    }

    if (!d.isValid() || d.getNode().identity() == IRNode.destroyedNode) {
      System.out.println("Ignoring invalid: " + d.getMessage());
      return; // ignore invalid drops
    }
    if (!ISnapshotDrop.class.isInstance(d)) {
      // Ignoring these for now
      return;
    }
    final String id = computeId(d);
    if (preprocessRefs) {
      d.preprocessRefs(this);
    }

    final String name = d.getXMLElementName();
    final Builder db = b.nest(name);
    final Class<?> c = d.getClass();
    db.addAttribute(TYPE_ATTR, c.getSimpleName());
    db.addAttribute(FULL_TYPE_ATTR, c.getName());
    db.addAttribute(DROP_TYPE_ATTR, d.getDropType().name());
    db.addAttribute(ID_ATTR, id);
    final IJavaRef javaRef = d.getJavaRef();
    if (javaRef != null) {
      final String encodedJavaRef = javaRef.encodeForPersistence();
      db.addAttribute(JAVA_REF, encodedJavaRef);
    } else {
      System.out.println("No java ref for "+d.getMessage()+" : "+DebugUnparser.toString(d.getNode())+" in "+JavaNames.getFullTypeName(VisitUtil.getEnclosingType(d.getNode())));
    }
    synchronized (d.getSeaLock()) {
    	d.snapshotAttrs(db);
    }
    d.snapshotRefs(this, db);
    db.end();
  }

  public void refDrop(Builder db, String name, Drop d) {
    refDrop(db, name, d, null, null);
  }

  public void refDrop(Builder db, String name, Drop d, String attr, String value) {
    Builder ref = db.nest(name);
    ref.addAttribute(ID_ATTR, computeId(d));
    if (attr != null) {
      ref.addAttribute(attr, value);
    }
    ref.end();
  }

  public void addProperties(Builder db, String flavor, Map<String, String> map) {
	if (map.isEmpty()) {
		return;
	}
    Builder pb = db.nest(PROPERTIES);
    pb.addAttribute(FLAVOR_ATTR, flavor);

    for (Map.Entry<String, String> e : map.entrySet()) {
      pb.addAttribute(e.getKey(), e.getValue());
    }
    pb.end();
  }

  public static long computeHash(IRNode node) {
    return computeHash(node, false);
  }

  public static long computeHash(IRNode node, boolean debug) {
    if (node instanceof MarkedIRNode) {
      return 0; // Not an AST node
    }
    final String unparse = DebugUnparser.unparseCode(node);
    if (debug) {
      System.out.println("Unparse: " + unparse);
    }
    return unparse.hashCode();
  }

  public static long computeContextHash(IRNode node) {
    return computeContextHash(node, false);
  }

  public static long computeContextHash(IRNode node, boolean debug) {
    if (node instanceof MarkedIRNode) {
      return 0; // Not an AST node
    }
    final String context = JavaNames.computeContextId(node);
    if (context != null) {
      if (debug) {
        System.out.println("Context: " + context);
      }
      /*
       * if (unparse.contains("@") { System.out.println("Found promise"); }
       */
      return context.hashCode();
    }
    return 0;
  }
}
