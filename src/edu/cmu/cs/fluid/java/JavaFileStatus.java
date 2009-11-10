package edu.cmu.cs.fluid.java;

import java.io.*;

import com.surelogic.tree.SyntaxTreeRegion;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.IJavaFileLocator.Type;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.*;

public class JavaFileStatus<T,P> extends AbstractJavaFileStatus<T> {  
  private static final Bundle javaBundle = JavaNode.getBundle();
  private static final Bundle parseBundle = JJNode.getBundle();
  private final IRRegion astRegion   = JJNode.specializeForSyntaxTree ? new SyntaxTreeRegion() : new IRRegion();
  private final IRRegion canonRegion = JJNode.specializeForSyntaxTree ? new SyntaxTreeRegion() : new IRRegion();
  private final AbstractJavaFileLocator<T,P> locator;
  private final P project;
  private final T id;
  private final String label;
  private final long modTime;
  private final IRNode root;
  private final Type type;
  boolean loaded = true;
  boolean canonical = false;
  boolean persistent = false;
  boolean isCanonicalizing = false;
  IRChunk astChunk1 = null;
  IRChunk canonChunk1 = null;
  IRChunk astChunk2 = null;
  IRChunk canonChunk2 = null;
  int unloadCount = 0;

  public JavaFileStatus(AbstractJavaFileLocator<T,P> loc, P proj, T id, 
                           String label, long time, IRNode root, Type type) {    
    if (id == null) {
      throw new IllegalArgumentException("null resource");
    }
    if (label == null) {
      label = root.toString();
    }
    if (root == null) {
      throw new IllegalArgumentException("null AST for "+id);
    }
    if (type == null) {
      throw new IllegalArgumentException("no type for resource "+id);
    }
    locator    = loc;
    project    = proj;
    this.id    = id;
    this.label = label;
    modTime    = loc.mapTimeStamp(time);
    this.root  = root;
    this.type  = type;
    
    // This is really the earliest we can do this
    putInRegion(astRegion, root);
  }

  public static <T> boolean isPersisted(SlotInfo<T> si ) {
	  return parseBundle.isInBundle(si) || javaBundle.isInBundle(si);
  }
  
  private static class PromiseSaver extends AbstractNodePromiseProcessor {
    final IRRegion region;
    
    PromiseSaver(IRRegion r, boolean include) {
      region = r;
    }
    public String getIdentifier() {
      return "Promise Saver";
    }    

    @Override
    protected void process(IRNode root) {
      for(IRNode n : JavaPromise.bottomUp(root)) {
        region.saveNode(n);
      }
    }
  }
  
  private void putInRegion(IRRegion region, IRNode root) {
    putInRegion(region, root, false);
  }
  
  private void putInRegion(IRRegion region, IRNode root, boolean includePromises) {
    PromiseSaver p = new PromiseSaver(region, includePromises);    
    p.process(root);
  }



  /**
   * @return true if completed
   */
  boolean makePersistent(FileLocator floc, boolean force) throws IOException {
    if (!force && persistent) {
      return false;
    }
    if (astChunk1 == null) { 
      astChunk1 = astRegion.createChunk(parseBundle);
      astChunk2 = astRegion.createChunk(javaBundle);
    }
    // Needs to be stored again if canonicalized
    astChunk1.store(floc);
    astChunk2.store(floc);
  
    if (canonical) {
      if (canonChunk1 == null) { 
        canonChunk1 = canonRegion.createChunk(parseBundle);
        canonChunk2 = canonRegion.createChunk(javaBundle);
      }
      canonChunk1.store(floc);
      canonChunk2.store(floc);
    }
    persistent = true;
    return true;
  }
  
  boolean okToUnload() {
    return persistent && loaded;
  }
  
  /**
   * @return true if completed
   */
  boolean unload() {
    if (!okToUnload()) {
      return false;
    }
    astChunk1.unload();
    astChunk2.unload();
    if (canonChunk1 != null) {
      canonChunk1.unload();
      canonChunk2.unload();
    }
    //System.out.println("Unloaded "+label);
    unloadCount++;
    loaded = false;
    return true;
  }
  
  /**
   * @return true if completed
   * @throws IOException 
   */
  boolean reload(FileLocator floc, boolean force) throws IOException {
    if (!persistent || (loaded && !force)) {
      return false;
    }
    //System.out.println("Reloading "+label);
    astChunk1.load(floc);
    astChunk2.load(floc);
    if (canonChunk1 != null) {
      canonChunk1.load(floc);
      canonChunk2.load(floc);
    }
    loaded = true;
    return true;
  }
  
  public P project() {
    return project;
  }
  
  public T id() {
    return id;
  }

  public String label() {
    return label;
  }
   
  public long modTime() {
    return modTime;
  }
  
  public IRNode root() {
    return root;
  }
  
  public boolean asSource() {
    return type == Type.SOURCE;
  }
  
  public Type getType() {
    return type;
  }
  
  public boolean isLoaded() {
    return loaded;
  }

  public boolean isPersistent() {
    return persistent;
  }

  public void canonicalize() {
    if (canonical) {
      return;
    }
    isCanonicalizing = true;
    
    // Ensure that the super class(es) is canonicalized first
    final ITypeEnvironment env = locator.getTypeEnvironment(project);
    for(IRNode t : VisitUtil.getAllTypeDecls(root)) {
    	IJavaType jt = JavaTypeFactory.convertNodeTypeToIJavaType(t, env.getBinder());
    	for(IJavaType st : jt.getSupertypes(env)) {
    		if (st == jt) {
    			continue; // Check for java.lang.Object?
    		}
    		if (st instanceof IJavaDeclaredType) {
    			IJavaDeclaredType ds   = (IJavaDeclaredType) st;
    			IRNode root            = VisitUtil.findRoot(ds.getDeclaration());    			
    			IJavaFileStatus<T> jfs = locator.getStatusForAST(root);
    			if (jfs != null && !jfs.isCanonicalizing()) {
    				//System.out.println("Canonicalizing "+jfs.label());
    				jfs.canonicalize();
    			}
    		}
    	}
    }
    if (!loaded) {
    	try {
    		reload(locator.flocPath, false);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
    persistent = false;
    locator.canonicalize(this);
    canonical = true;    
    
    // We have to do this (again) to make sure
    // that the new nodes are in the region
    if (IJavaFileLocator.useIRPaging) {
      putInRegion(canonRegion, root, true);
      persistent = false;
      locator.persist(this);
      persistent = true;
    }
    isCanonicalizing = false;
  }
  
  public boolean isCanonical() {
    return canonical;
  }

  public boolean isCanonicalizing() {
	return isCanonicalizing;
  }
  
  @SuppressWarnings("unchecked")
  protected static boolean isInBundle(PersistentSlotInfo psi) {
    SlotInfo si = (SlotInfo) psi;
    return parseBundle.isInBundle(si) || javaBundle.isInBundle(si);
  }
  
  boolean includesRegion(IRRegion r) {
    return r == astRegion || r == canonRegion;
  }
  
  void printSummary(PrintWriter pw) {
    pw.print(label);
    pw.print(": ");
    if (loaded) {
      pw.print("loaded, ");
    }
    if (canonical) {
      pw.print("canonical, ");
    }
    if (persistent) {
      pw.print("persistent, ");
    }
    pw.println(unloadCount);
  }

  void dumpRegions() {
    System.out.println(label+" : "+astRegion+", "+canonRegion);
  }
}

