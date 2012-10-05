package com.surelogic.dropsea.irfree;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTEXT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FLAVOR_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FULL_TYPE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HASH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TYPE_ATTR;
import static com.surelogic.common.xml.XMLReader.PROJECT_ATTR;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.ID_ATTR;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.JAVA_DECL_INFO;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.PROPERTIES;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.ROOT;
import static com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader.UID_ATTR;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.surelogic.common.refactor.JavaDeclInfo;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ISnapshotDrop;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader;
import com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReaderListener;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.MarkedIRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;

public class SeaSnapshot extends AbstractSeaXmlCreator {	
  private final Map<Drop, String> idMap = new HashMap<Drop, String>();

  public SeaSnapshot(File location) throws IOException {
    super(location);
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

    if (!d.isValid()) {
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
    db.addAttribute(ID_ATTR, id);
    if (d instanceof IRReferenceDrop) {
      db.addAttribute(HASH_ATTR, d.getTreeHash());
      db.addAttribute(CONTEXT_ATTR, d.getContextHash());
    }
    d.snapshotAttrs(db);
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

  public void addSrcRef(Builder outer, IRNode context, ISrcRef srcRef) {
    addSrcRef(outer, context, srcRef, 2, null);
  }

  public void addSrcRef(Builder outer, IRNode context, ISrcRef s, String flavor) {
    addSrcRef(outer, context, s, 2, flavor);
  }

  public void addJavaDeclInfo(Builder b, final String flavor, final JavaDeclInfo info) {
    Builder db = b.nest(JAVA_DECL_INFO);
    db.addAttribute(FLAVOR_ATTR, flavor);
    db.addAttribute(JavaDeclInfo.INFO_KIND, info.getKind().toString());

    for (Map.Entry<String, String> e : info.getAttributes().entrySet()) {
      db.addAttribute(e.getKey(), e.getValue());
    }
    if (info.getParent() != null) {
      addJavaDeclInfo(db, JavaDeclInfo.PARENT, info.getParent());
    }
    db.end();
  }

  public void addProperties(Builder db, String flavor, Map<String, String> map) {
    Builder pb = db.nest(PROPERTIES);
    pb.addAttribute(FLAVOR_ATTR, flavor);

    for (Map.Entry<String, String> e : map.entrySet()) {
      pb.addAttribute(e.getKey(), e.getValue());
    }
    pb.end();
  }

  public static List<IDrop> loadSnapshot(File location) throws Exception {
    SeaSnapshotXMLReaderListener l = new SeaSnapshotXMLReaderListener();
    new SeaSnapshotXMLReader(l).read(location);
    return l.getDrops();
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

  public static long computeContext(IRNode node, boolean debug) {
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
