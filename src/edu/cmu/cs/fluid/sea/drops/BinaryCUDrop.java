/*
 * Created on Jun 22, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops;

import java.util.*;

import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.ICodeFile;
import edu.cmu.cs.fluid.sea.*;

/**
 * @author chance
 *
 */
public class BinaryCUDrop extends CUDrop {
  public BinaryCUDrop(CodeInfo info) {
    super(info);
  }
  
  /**
   * Looks up the drop corresponding to the given name.
   * 
   * @param javaName the name of the drop to lookup
   * @return the corresponding drop, or <code>null</code> if a drop does
   *   not exist.
   */
  static public CUDrop queryCU(String project, String javaName) {
    @SuppressWarnings("unchecked") 
    Set<BinaryCUDrop> drops = Sea.getDefault().getDropsOfExactType(BinaryCUDrop.class);
    for (BinaryCUDrop drop : drops) {
      ICodeFile file = drop.info.getFile();
      String proj = file == null ? null : file.getProjectName();
      if (drop.javaOSFileName.equals(javaName) && proj == project) {
        return drop;
      }
    }
    return null;
  }

  @Override
  public boolean isAsSource() {
    return false;
  }

  public static Collection<BinaryCUDrop> invalidateAll() {	  
	  //Sea.getDefault().invalidateMatching(DropPredicateFactory.matchType(BinaryCUDrop.class));
	  final Set<BinaryCUDrop> drops = Sea.getDefault().getDropsOfExactType(BinaryCUDrop.class);
	  for(BinaryCUDrop d : drops) {
		  d.invalidate();
	  }
	  return drops;
  }
}
