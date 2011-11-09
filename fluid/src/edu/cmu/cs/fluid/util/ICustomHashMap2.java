/*
 * Created on Aug 5, 2003
 *
 */
package edu.cmu.cs.fluid.util;

import edu.cmu.cs.fluid.util.CopiedHashMap2.IHashEntry;

/**
 * @author chance
 *
 */
public interface ICustomHashMap2 extends ICustomMap {	
	public interface HashEntryFactory extends EntryFactory {
		/**
		 * To be called when put() is used
		 * @param key
		 * @param value
     * @param hash
		 * @return An entry initialized to the appropriate defaults
		 */
	  IHashEntry create(Object key, Object value, int hash);
	}
}
