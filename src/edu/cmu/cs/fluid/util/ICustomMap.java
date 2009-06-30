/*
 * Created on Aug 5, 2003
 *
 */
package edu.cmu.cs.fluid.util;

import java.util.Map;

/**
 * @author chance
 *
 */
public interface ICustomMap extends Map {
	/**
	 * @param key
	 * @return The entry matching the key
	 */
	Map.Entry getEntry(Object key);
	
	Map.Entry getEntryAlways(Object key);
	
  void putEntry(Map.Entry entry);  
	
	public interface EntryFactory {
		/**
		 * To be called when put() is used
		 * @param key
		 * @param value
		 * @return An entry initialized to the appropriate defaults
		 */
	  Map.Entry create(Object key, Object value);
	  
	  /**
	   * To be called by putEntry() to confirm that the entry 
	   * matches what this Map expects 
	   * 
	   * @param entry
	   * @return true if expected
	   */
    boolean isValid(Map.Entry entry);

    /**
     * Copies the contents (value and any other related fields)
     * @param from
     * @param to
     */
    void copy(Map.Entry from, Map.Entry to);
	}
}
