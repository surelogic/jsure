package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * A basic implementation of Set that preserves the 
 * order of the elements to support ListMap
 * 
 * @author Edwin
 */
public class ListSet<E> extends AbstractSet<E> {
	private final List<E> contents;
	
	public ListSet(List<E> l) {
		contents = new ArrayList<E>(l);
	}

	@Override
	public Iterator<E> iterator() {
		return contents.iterator();
	}

	@Override
	public int size() {
		return contents.size();
	}

}
