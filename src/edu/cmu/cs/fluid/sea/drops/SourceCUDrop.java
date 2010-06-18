/*
 * Created on Jun 22, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.ICodeFile;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.Sea;

/**
 * @lock CacheLock is class protects cachedDrops
 */
public class SourceCUDrop extends CUDrop {
  private static Map<ICodeFile, SourceCUDrop> cachedDrops = null;
  
  public SourceCUDrop(CodeInfo info, ProjectsDrop p) {
    super(info);
    //System.out.println("Creating SourceCUDrop for "+info.getFileName());
    javaFile = info.getFile();
    source   = info.getSource();
    adaptedAsSource = info.isAsSource();
    this.projects = p;
    
    synchronized (SourceCUDrop.class) {
      // clear cached drops, since we're creating new ones
      cachedDrops = null;
    }
  }

  public final ProjectsDrop projects;
  public final ICodeFile javaFile;
  public final String source;
  public final boolean adaptedAsSource;

  /**
   * @requiresLock CacheLock
   */
  private static void initCachedDrops() {
    Set<SourceCUDrop> drops = Sea.getDefault().getDropsOfExactType(SourceCUDrop.class);
    cachedDrops = new HashMap<ICodeFile, SourceCUDrop>(drops.size());
    for (SourceCUDrop drop : drops) {
      if (drop.isValid() && drop.cu.equals(IRNode.destroyedNode)) {
        drop.invalidate();
        continue;
      }
      if (drop.javaFile != null) {
        SourceCUDrop d = cachedDrops.put(drop.javaFile, drop);
        if (d != null) {
          // duplicate drop?
          LOG.severe("Got 2+ drops with same javaFile: "+drop.javaFile);
        }
      }
    }
  }
  
  /**
   * Looks up the drop corresponding to the given ICompilationUnit.
   * 
   * @param javaFile
   *          the Eclipse compilation to lookup the drop for
   * @return the corresponding drop, or <code>null</code> if a drop does not
   *         exist.
   */
  static public SourceCUDrop queryCU(ICodeFile javaFile) {
    /*
    Set<SourceCUDrop> drops = Sea.getDefault().getDropsOfExactType(SourceCUDrop.class);
    for (SourceCUDrop drop : drops) {
      if (drop.javaFile == null) {
        System.out.println("javaFile is null");  
      }
      else if (drop.javaFile.equals(javaFile)) return drop;
    }
    return null;
    */
    synchronized (SourceCUDrop.class) {
      if (cachedDrops == null) {
        initCachedDrops();
      }
      return cachedDrops.get(javaFile);
    }
  }

  /**
   * Invalidates all SourceCUDrops contained within this sea. 
   * Can be used as a reset method when closing a project.
   * 
   */
  public static Collection<SourceCUDrop> invalidateAll() {
    synchronized (SourceCUDrop.class) {
      cachedDrops = null;
    }
    //Sea.getDefault().invalidateMatching(DropPredicateFactory.matchType(SourceCUDrop.class));
    final Set<SourceCUDrop> cuds = Sea.getDefault().getDropsOfExactType(SourceCUDrop.class);
    for(SourceCUDrop d : Sea.getDefault().getDropsOfExactType(SourceCUDrop.class)) {
    	d.invalidate();    	
    }
    return cuds;
  }

  @Override
  public boolean isAsSource() {
    return adaptedAsSource;
  }
  /*
  @Override
  protected void invalidate_internal() {
	  System.out.println("Invalidating "+javaOSFileName);
  }
*/
}
