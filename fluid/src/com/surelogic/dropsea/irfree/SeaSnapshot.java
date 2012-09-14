package com.surelogic.dropsea.irfree;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTEXT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FLAVOR_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FULL_TYPE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HASH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TYPE_ATTR;
import static com.surelogic.common.jsure.xml.JSureXMLReader.ID_ATTR;
import static com.surelogic.common.jsure.xml.JSureXMLReader.JAVA_DECL_INFO;
import static com.surelogic.common.jsure.xml.JSureXMLReader.PROPERTIES;
import static com.surelogic.common.jsure.xml.JSureXMLReader.ROOT;
import static com.surelogic.common.jsure.xml.JSureXMLReader.SUPPORTING_INFO;
import static com.surelogic.common.jsure.xml.JSureXMLReader.UID_ATTR;
import static com.surelogic.common.xml.XMLReader.PROJECT_ATTR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.xml.sax.Attributes;

import com.surelogic.common.jsure.xml.JSureXMLReader;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.refactor.JavaDeclInfo;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.xml.AbstractXMLResultListener;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ISupportingInformation;
import com.surelogic.dropsea.ir.AnalysisResultDrop;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.promises.threadroles.IThreadRoleDrop;
import com.surelogic.dropsea.irfree.drops.IRFreeDrop;
import com.surelogic.dropsea.irfree.drops.IRFreePromiseDrop;
import com.surelogic.dropsea.irfree.drops.IRFreeProposedPromiseDrop;
import com.surelogic.dropsea.irfree.drops.IRFreeResultDrop;
import com.surelogic.dropsea.irfree.drops.IRFreeResultFolderDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class SeaSnapshot extends AbstractSeaXmlCreator {
  public static final String SUFFIX = RegressionUtility.JSURE_SNAPSHOT_SUFFIX;
  public static final boolean useFullType = true;

  static final Map<String, Class<? extends Drop>> classMap = new HashMap<String, Class<? extends Drop>>();

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
      // pw = null;
      // JSureXMLReader.readSnapshot(location, null);
    }
  }

  private static void ensureClassMapping(Class<? extends Drop> cls) {
    if (classMap.containsKey(cls.getName())) {
      return;
    }
    String simple = Entity.internString(cls.getSimpleName());
    String qname = Entity.internString(cls.getName());
    classMap.put(simple, cls);
    classMap.put(qname, cls);
  }

  private static String[] packages = { "edu.cmu.cs.fluid.sea.drops.promises.", "edu.cmu.cs.fluid.sea.",
      "edu.cmu.cs.fluid.sea.drops.", "edu.cmu.cs.fluid.sea.drops.threadroles.", "edu.cmu.cs.fluid.sea.drops.modules.",
      "edu.cmu.cs.fluid.sea.drops.callgraph.", "edu.cmu.cs.fluid.sea.drops.layers.", };
  
  /**
   * A list of types that use to be in drop-sea and are in persisted scans, but
   * no longer are used. This just helps to avoid lots of warnings.
   */
  private static String[] obsoleteTypes = { "com.surelogic.analysis.AbstractWholeIRAnalysis$ResultsDepDrop" };

  @SuppressWarnings("unchecked")
  public static Class<?> findType(String type) {
    Class<?> thisType = classMap.get(type);
    if (thisType == null) {
      if (useFullType) {
        try {
          // System.out.println("Loading class "+type);
          thisType = Class.forName(type);
        } catch (ClassNotFoundException e) {
          // Keep going
        }
      } else {
        for (String prefix : packages) {

          try {
            thisType = Class.forName(prefix + type);
            break;
          } catch (ClassNotFoundException e) {
            // Keep going
          }
        }
      }
      if (thisType == null) {
        /*
         * Check if we know this type is no longer in the system.
         */
        if (!Arrays.asList(obsoleteTypes).contains(type))
          SLLogger.getLogger().warning("Unknown class type: " + type);
      } else {
        ensureClassMapping((Class<? extends Drop>) thisType);
      }
    }
    return thisType;
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
    if (IThreadRoleDrop.class.isInstance(d)) {
      // Ignoring these for now
      return;
    }
    final String id = computeId(d);
    if (preprocessRefs) {
      d.preprocessRefs(this);
    }

    final String name = d.getXMLElementName();
    final String type = d.getClass().getSimpleName();
    ensureClassMapping(d.getClass());
    final Builder db = b.nest(name);
    db.addAttribute(TYPE_ATTR, type);
    if (useFullType) {
      db.addAttribute(FULL_TYPE_ATTR, d.getClass().getName());
    }
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

  public void addSupportingInfo(Builder db, ISupportingInformation si) {
    Builder sib = db.nest(SUPPORTING_INFO);
    sib.addAttribute(Drop.MESSAGE, si.getMessage());
    addSrcRef(sib, si.getLocation(), si.getSrcRef(), 3, null);
    sib.end();
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

  /*
   * private void outputPromiseDropAttrs(StringBuilder b, PromiseDrop d) {
   * d.isAssumed(); d.isCheckedByAnalysis(); d.isFromSrc(); }
   */
  public static List<IDrop> loadSnapshot(File location) throws Exception {
    XMLListener l = new XMLListener();
    new JSureXMLReader(l).read(location);
    return l.getEntities();
  }

  static class XMLListener extends AbstractXMLResultListener {
    private final List<IRFreeDrop> entities = new ArrayList<IRFreeDrop>();

    List<IDrop> getEntities() {
      List<IDrop> rv = new ArrayList<IDrop>();
      for (IRFreeDrop i : entities) {
        if (i != null) {
          rv.add(i);
        }
      }
      return rv;
    }

    private void add(int id, IRFreeDrop info) {
      if (id >= entities.size()) {
        // Need to expand the array
        while (id > entities.size()) {
          entities.add(null);
        }
        entities.add(info);
      } else {
        IRFreeDrop old = entities.set(id, info);
        if (old != null) {
          throw new IllegalStateException("Replacing id: " + id);
        }
      }
    }

    public Entity makeEntity(String name, Attributes a) {
      if (JAVA_DECL_INFO.equals(name)) {
        return new JavaDeclInfo(name, a);
      }
      final String type = Entity.getValue(a, useFullType ? FULL_TYPE_ATTR : TYPE_ATTR);
      if (type != null) {
        final Class<?> thisType = findType(type);
        if (thisType != null) {
          if (ProposedPromiseDrop.class.isAssignableFrom(thisType)) {
            return new IRFreeProposedPromiseDrop(name, a);
          } else if (PromiseDrop.class.isAssignableFrom(thisType)) {
            return new IRFreePromiseDrop(name, a);
          } else if (ResultDrop.class.isAssignableFrom(thisType)) {
      	    return new IRFreeResultDrop(name, a);
          } else if (ResultFolderDrop.class.isAssignableFrom(thisType)) {
            return new IRFreeResultFolderDrop(name, a);
          } 
        }
      }
      return new IRFreeDrop(name, a);
    }

    @Override
    protected boolean define(final int id, Entity e) {
      if (!e.getName().endsWith("drop")) {
        System.out.println("Got " + e.getName());
      }
      final IRFreeDrop i = (IRFreeDrop) e;
      add(id, i);
      i.finishInit();
      return true;
    }

    @Override
    protected void handleRef(String fromLabel, int fromId, Entity to) {
      final String refType = to.getName();
      final IRFreeDrop fromE = entities.get(fromId);
      final int toId = Integer.valueOf(to.getId());
      final IRFreeDrop toE = entities.get(toId); // The entity above is really
                                                 // the ref
      // info
      if (Drop.DEPONENT.equals(refType)) {
        fromE.addDeponent(toE);
        toE.addDependent(fromE);
      } else if (ProposedPromiseDrop.PROPOSED_PROMISE.equals(refType)) {
        fromE.addProposal((IRFreeProposedPromiseDrop) toE);
      } else if (fromE instanceof IRFreePromiseDrop) {
          final IRFreePromiseDrop fromPI = (IRFreePromiseDrop) fromE;
          final IAnalysisResultDrop toPI = (IAnalysisResultDrop) toE;        
          if (PromiseDrop.CHECKED_BY_RESULTS.equals(refType)) {
              fromPI.addCheckedByResult(toPI);
          } else {
              throw new IllegalStateException("NOT Handled: " + refType + " ref from " + fromLabel + " to " + to.getId());
          }
      } else if (fromE instanceof IRFreeResultDrop) {
        final IRFreeResultDrop fromPI = (IRFreeResultDrop) fromE;
        if (toE instanceof IRFreeResultFolderDrop) {
        	if (ResultDrop.TRUSTED_FOLDER.equals(refType)) {
                fromPI.addTrustedFolder((IRFreeResultFolderDrop) toE);
            } else {
                throw new IllegalStateException("NOT Handled: " + refType + " ref from " + fromLabel + " to " + to.getId());
            }
        } else {
        	final IRFreePromiseDrop toPI = (IRFreePromiseDrop) toE;

        	if (AnalysisResultDrop.CHECKED_PROMISE.equals(refType)) {
        		fromPI.addCheckedPromise(toPI);
        	} else if (ResultDrop.TRUSTED_PROMISE.equals(refType)) {
        		fromPI.addTrustedPromise(toPI);
        	} else if (ResultDrop.OR_TRUSTED_PROMISE.equals(refType)) {
        		final String label = to.getAttribute(ResultDrop.OR_LABEL);
        		fromPI.addOrTrustedPromise(label, toPI);
            } else {
                throw new IllegalStateException("NOT Handled: " + refType + " ref from " + fromLabel + " to " + to.getId());
            }
        }
      } else if (fromE instanceof IRFreeResultFolderDrop) {
          final IRFreeResultFolderDrop fromPI = (IRFreeResultFolderDrop) fromE;    
          if (AnalysisResultDrop.CHECKED_PROMISE.equals(refType)) {
              final IRFreePromiseDrop toPI = (IRFreePromiseDrop) toE;    	              
              fromPI.addCheckedPromise(toPI);
          } else if (ResultFolderDrop.RESULT.equals(refType)) {
              final IRFreeResultDrop toPI = (IRFreeResultDrop) toE;    	        	  
    		  fromPI.addResult(toPI);
    	  } else if (ResultFolderDrop.SUB_FOLDER.equals(refType)) {
              final IRFreeResultFolderDrop toPI = (IRFreeResultFolderDrop) toE;    	      	  
    		  fromPI.addSubFolder(toPI);
          } else {
              throw new IllegalStateException("NOT Handled: " + refType + " ref from " + fromLabel + " to " + to.getId());
          }
      } else {
        throw new IllegalStateException("NOT Handled: " + refType + " ref from " + fromLabel + " to " + to.getId());
      }
    }
  }
}
