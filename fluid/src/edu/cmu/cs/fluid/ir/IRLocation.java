/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRLocation.java,v 1.14 2008/09/05 18:01:04 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** An abstract point within a sequence.
 * A location has no meaning apart from its sequence.
 * In particular do not suppose the ID has anything to do
 * with the location's interpretation as a numeric place
 * in the sequence.
 */
public class IRLocation {
  private final int id;
  protected IRLocation(int i) {
    id = i;
  }
  public int getID() {
    return id;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof IRLocation) {
      IRLocation loc = (IRLocation) other;
      return loc.id == id;
    } else {
      return false;
    }
  }
  @Override
  public int hashCode() {
    return id;
  }

  private static final List<IRLocation> locations = new CopyOnWriteArrayList<IRLocation>();
  private static final List<IRLocation> tempLocations = new ArrayList<IRLocation>();
  private static final IRLocation sentinel = new IRLocation(-2);

  @SuppressWarnings("unchecked")
  public static IRLocation get(int i) {
    if (i == -1) return null;
	if (i == -2) return sentinel;
	
	try {
		return locations.get(i);
	} catch(IndexOutOfBoundsException e) {
		// Make sure only one thread changes 'locations' at a time	
		synchronized (locations) {
			tempLocations.clear();
			
			// Add locations for the next location up to i
			for(int j=locations.size(); j<=i; j++) {
				tempLocations.add(new IRLocation(j));
			}
			locations.addAll(tempLocations);
			tempLocations.clear();
		}
		return locations.get(i);
	}
  }
  
  public static final IRLocation zeroPrototype = get(0);
  
  /**
   * Return a special IRLocation (non-null) that will never occur normally.
   * This sentinel can be persisted as normal.
   * @return sentinel IRLocation (not null)
   */
  public static IRLocation getSentinel() {
    return sentinel;
  }

  @Override
  public String toString() {
    return String.valueOf(id);
  }

  public static IRLocation valueOf(String s) {
    if (s.compareTo("") == 0) {
      return null;
    } else {
      return get(Integer.parseInt(s));
    }
  }
}