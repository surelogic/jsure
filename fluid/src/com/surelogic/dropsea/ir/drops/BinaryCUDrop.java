package com.surelogic.dropsea.ir.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic.dropsea.ir.Sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.ICodeFile;

public final class BinaryCUDrop extends CUDrop {

  private static final ConcurrentHashMap<String, BinaryCUDrop> ID_TO_INSTANCE = new ConcurrentHashMap<String, BinaryCUDrop>();

  public BinaryCUDrop(CodeInfo info) {
    super(info, false);

    final String id = computeId(info.getFile(), this);
    ID_TO_INSTANCE.put(id, this);
  }

  private static String computeId(ICodeFile file, BinaryCUDrop drop) {
    final String proj = file == null ? null : file.getProjectName();
    return computeId(proj, drop.getJavaOSFileName());
  }

  private static String computeId(String project, String javaName) {
    return project + ':' + javaName;
  }

  /**
   * Looks up the drop corresponding to the given name if such a drop has been
   * previously created.
   * 
   * @param javaName
   *          the name of the drop to lookup
   * @return the corresponding drop, or <code>null</code> if a drop does not
   *         exist.
   */
  static public BinaryCUDrop queryCU(String project, String javaName) {
    BinaryCUDrop d = ID_TO_INSTANCE.get(computeId(project, javaName));
    if (d != null && d.isValid()) {
      return d;
    }
    return null;
  }

  public static Collection<IRNode> invalidateAll() {
    final ArrayList<IRNode> result = new ArrayList<IRNode>();
    synchronized (Sea.getDefault().getSeaLock()) {
      final List<BinaryCUDrop> drops = Sea.getDefault().getDropsOfExactType(BinaryCUDrop.class);
      for (BinaryCUDrop d : drops) {
        result.add(d.getCompilationUnitIRNode());
        d.invalidate();
      }
      ID_TO_INSTANCE.clear();
    }
    return result;
  }
}
