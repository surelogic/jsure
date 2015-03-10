/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/ScrubberType.java,v 1.2 2007/07/02 16:40:42 chance Exp $*/
package com.surelogic.annotation.scrub;

/**
 * Describes in what order a scrubber will look at AASTs 
 * 
 * @author Edwin.Chan
 */
public enum ScrubberType {
  UNORDERED,
  /**
   * All the AASTs on a given type will be processed before those of another type
   */
  BY_TYPE,
  /**
   * AASTs on a superclass will be examined before those on a subclass
   * (no particular order within a class)
   */
  BY_HIERARCHY,
  /**
   * As above, but also process immediate subclasses of types with annotations
   */
  INCLUDE_SUBTYPES_BY_HIERARCHY,
  /**
   * Similar to the above, but operates on method/constructor declarations
   * and processes the immediately overriden methods
   */
  INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY,
  /**
   * All drops will be given to the scrubber at once
   */
  DIY,
  OTHER
}
