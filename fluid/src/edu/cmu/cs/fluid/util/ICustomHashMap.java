/*
 * Created on Aug 5, 2003
 *
 */
package edu.cmu.cs.fluid.util;

/**
 * @author chance
 *
 */
public interface ICustomHashMap extends ICustomMap {	
	public interface HashEntryFactory extends EntryFactory {
		/**
		 * To be called when put() is used
		 * @param key
		 * @param value
     * @param hash
		 * @return An entry initialized to the appropriate defaults
		 */
	  HashEntry create(Object key, Object value, int hash);
	}
  
  public class HashEntry extends CopiedHashMap.Entry {
		/**
		 * @param hash
		 * @param key
		 * @param val
		 */
		public HashEntry(int hash, Object key, Object val) {
			super(hash, key, val);
		}

    public HashEntry(Object key, Object val) {
      super(key, val);
    }
  }
}
