/*
 * Created on Sep 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.util;

public interface IHashEntry<K,V> extends java.util.Map.Entry<K,V> {
  int getHash();
}
