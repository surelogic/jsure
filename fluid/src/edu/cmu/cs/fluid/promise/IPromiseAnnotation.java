/*
 * Created on Oct 8, 2003
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Represents a promise module.  
 * Typically encapsulating support for one category of related promises 
 * (possibly using multiple annotation tags)

 * Bundling rules dealing with 
 * -- storage (typically in a SlotInfo)
 * -- representation (typically as IR ASTs)
 * -- parsing
 * -- sanity checking
 * -- binding
 * 
 * What about re-parsing? diffing?
 * 
 * Accessors appear as static methods on the specific modules
 * 
 * @author chance
 */
public interface IPromiseAnnotation {
  String name();

  /** 
   * Usually needed for sanity checking and binding
   * @param bind The appropriate EclipseBinder
   */
  void setBinder(IBinder bind);

  /**
	 * The process: 
	 * -- Registering promise tags used and which parser rules
	 *    should be used 
	 * -- Registering promise Operators (if any) and which rules
	 *    to use for each
	 */
  boolean register(IPromiseFramework frame);
}

/*
interface Extension {
	// Not necessarily a 1-to-1 mapping
  SlotInfo getSlotInfo(String annoTag);

  boolean getBoolAnnotation(String annoTag, IRNode promisedFor);

  IRNode getNodeAnnotation(String annoTag, IRNode promisedFor);

  IRSequence getSeqAnnotation(String annoTag, IRNode promisedFor);
}
*/