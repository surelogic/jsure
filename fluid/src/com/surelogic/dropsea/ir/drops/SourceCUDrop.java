package com.surelogic.dropsea.ir.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic.RequiresLock;
import com.surelogic.analysis.IIRProject;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.ICodeFile;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

/**
 * @lock CacheLock is class protects cachedDrops
 */
public final class SourceCUDrop extends CUDrop {

  /**
   * Map from files to source CU drop. A value of {@code null} means the map
   * needs to be rebuilt via a call to
   * {@link #fillFileToInstanceMapFromSeaQuery()}.
   * <p>
   * Use {@link Sea#getSeaLock()} to protect mutations to the field.
   */
  private static ConcurrentHashMap<ICodeFile, SourceCUDrop> FILE_TO_INSTANCE;

  public SourceCUDrop(CodeInfo info) {
    super(info, info.isAsSource());
    // System.out.println("Creating SourceCUDrop for "+info.getFileName());

    synchronized (Sea.getDefault().getSeaLock()) {
      // clear cached drops, since we're creating new ones
      FILE_TO_INSTANCE = null;
    }
  }

  /**
   * This is needed because we need to clean up some drops that we place in the
   * sea but are destroyed after construction of the compilation units.
 * @param sea 
   */
  @RequiresLock("sea:SeaLock")
  private static void fillFileToInstanceMapFromSeaQuery(Sea sea) {
    final List<SourceCUDrop> drops = sea.getDropsOfExactType(SourceCUDrop.class);
    FILE_TO_INSTANCE = new ConcurrentHashMap<ICodeFile, SourceCUDrop>(drops.size());
    for (SourceCUDrop drop : drops) {
      if (drop.isValid() && drop.getCompilationUnitIRNode().equals(IRNode.destroyedNode)) {
        drop.invalidate();
        continue;
      }
      if (drop.f_codeInfo == null) {
    	System.out.println("Null codeInfo for "+drop.getMessage());
    	drop.invalidate();
    	continue;
      }
      final ICodeFile javaFile = drop.f_codeInfo.getFile();
      if (javaFile != null) {
        SourceCUDrop d = FILE_TO_INSTANCE.put(javaFile, drop);
        if (d != null) {
          // duplicate drop?
          SLLogger.getLogger().severe("Got 2+ drops with same javaFile: " + javaFile);
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
  public static SourceCUDrop queryCU(ICodeFile javaFile) {
	final Sea sea = Sea.getDefault();
    synchronized (sea.getSeaLock()) {
      if (FILE_TO_INSTANCE == null) {
    	  fillFileToInstanceMapFromSeaQuery(sea);
      }
      return FILE_TO_INSTANCE.get(javaFile);
    }
  }

  /**
   * Invalidates all SourceCUDrops contained within this sea. Can be used as a
   * reset method when closing a project.
   */
  public static Collection<IRNode> invalidateAll(final Collection<IIRProject> projects) {
    final ArrayList<IRNode> result = new ArrayList<IRNode>();
    synchronized (Sea.getDefault().getSeaLock()) {
      final List<SourceCUDrop> drops = Sea.getDefault().getDropsOfExactType(SourceCUDrop.class);
      for (SourceCUDrop d : drops) {
        boolean invalidate = projects == null;

        if (!invalidate) {
          // TODO use hash map?
          final ITypeEnvironment dTEnv = d.getTypeEnv();
          for (IIRProject p : projects) {
            if (dTEnv == p.getTypeEnv()) {
              invalidate = true;
              break;
            }
          }
        }
        if (invalidate) {
          d.invalidate();
          result.add(d.getCompilationUnitIRNode());
        }
      }
      FILE_TO_INSTANCE = null;
    }
    return result;
  }
}
