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
  static final Map<String,BinaryCUDrop> cache = new HashMap<String, BinaryCUDrop>();
	
  public BinaryCUDrop(CodeInfo info) {
    super(info);
    
    final String id = computeId(info.getFile(), this);
    cache.put(id, this);
  }
  
  static String computeId(ICodeFile file, BinaryCUDrop drop) {
      String proj = file == null ? null : file.getProjectName();
      return computeId(proj, drop.javaOSFileName);
  }
  
  static String computeId(String project, String javaName) {
	  return project+':'+javaName;
  }
  
  /**
   * Looks up the drop corresponding to the given name.
   * 
   * @param javaName the name of the drop to lookup
   * @return the corresponding drop, or <code>null</code> if a drop does
   *   not exist.
   */
  static public CUDrop queryCU(String project, String javaName) {
	CUDrop d = cache.get(computeId(project, javaName));
	if (d != null && d.isValid()) {
		return d;
	}	
	/*
    @SuppressWarnings("unchecked") 
    Set<BinaryCUDrop> drops = Sea.getDefault().getDropsOfExactType(BinaryCUDrop.class);
    for (BinaryCUDrop drop : drops) {
      ICodeFile file = drop.info.getFile();
      String proj = file == null ? null : file.getProjectName();
      if (drop.javaOSFileName.equals(javaName) && proj == project) {
        return drop;
      }
    }
    */
    return null;
  }

  @Override
  public boolean isAsSource() {
    return false;
  }

  public static Collection<BinaryCUDrop> invalidateAll() {	  
	  //Sea.getDefault().invalidateMatching(DropPredicateFactory.matchType(BinaryCUDrop.class));
	  final List<BinaryCUDrop> drops = Sea.getDefault().getDropsOfExactType(BinaryCUDrop.class);
	  for(BinaryCUDrop d : drops) {
		  d.invalidate();
	  }
	  return drops;
  }
}
