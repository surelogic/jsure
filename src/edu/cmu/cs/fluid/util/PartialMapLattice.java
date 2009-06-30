/*
 * Created on Aug 6, 2003
 */
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 *	Lattice representing a function from (any) objects
 *	to elements of a particular lattice.  Only represents
 *	functions for which all but finitely many objects map 
 *	to top, or all but finitely many objects map to bottom. 
 */
@SuppressWarnings("unchecked")
public class PartialMapLattice<K extends Object, L extends Lattice> implements Lattice{
	
	/**
	 * Maps the finite interesting objects.
	 */
	protected final HashMap<?,L> map;
	
	/**
	 *  The lattice the function maps into
	 */
	protected final Lattice range;
	
	/**
	 * Top value -- all objects map to top
	 */
	public final PartialMapLattice<K,L> topVal;
	/**
	 *	Bottom value -- all objects map to bottom
	 */
	public final PartialMapLattice<K,L> botVal;

	/**
	 * True iff the infinitely many elements not explicitly 
	 * represented map to top.
	 */
	public final boolean isTop;

	/**
	 * Primary constructor.  By default, all objects point to the 
	 * bottom of the range lattice.
	 * @param range  The lattice into which objects are mapped.
	 */
  public PartialMapLattice(Lattice range){
    this(range,false);
  }
  
  /**
   * Same as primary constructor, but can specify top or bottom
   * as default map value.
   * @param range
   * @param is_top
   */
  public PartialMapLattice(Lattice range, boolean is_top) {
		this.range = range.bottom();
		isTop = is_top;
		map = new HashMap<Object,L>();
		botVal = this;
		topVal = newLattice(new HashMap<Object,L>(), true);
	}

	/**
	 * Create a new lattice value in existing lattice
	 */
	protected PartialMapLattice(Lattice range, HashMap<?,L> m, PartialMapLattice t,
				PartialMapLattice b,boolean isTop) {
		this.range = isTop?range.top():range.bottom();
		topVal = t==null?this:t;
		botVal = b;
		map = m;
		this.isTop = isTop;
	}
	
	/**
	 * Routine to add values to existing lattice
	 * Should be overridden in child classes
	 */
	protected PartialMapLattice<K,L> newLattice(HashMap<?,L> newValues,boolean isTop) {
		return new PartialMapLattice(range,newValues,topVal,botVal,isTop);
	}

	public boolean isTop(){
		return isTop && map.isEmpty();
	}

	public Lattice top() {
		return topVal;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.util.Lattice#bottom()
	 */
	public Lattice bottom() {
		return botVal;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.util.MapLattice#get(java.lang.Object)
	 */
  public L get(Object key) {
		L l = map.get(key);
		if(l == null)
			return (L)range;//isTop?range.top():range.bottom();
		return l;
	}

	/**
	 * Does key map to an "interesting" value?
	 */
	public boolean containsKey(Object key){
		return map.containsKey(key);
	}

	/**
	 * @return An iterator through all keys that map to
	 * 		"interesting" values.
	 */
	public Iterator keys() {
		return map.keySet().iterator();
	}

	/**
	 * Return a new Partial Map element which maps every
	 * object the same as the current map except key, which
	 * the new function maps to val.
	 */
	@SuppressWarnings("unchecked")
  public PartialMapLattice<K,L> update(K key, L val) {
		HashMap<K,L> m = (HashMap<K, L>) map.clone();
		if(isTop){
			//ignore "uninteresting" updates
		 if( val.equals(range.top())) 
		 		m.remove(key);
		 	else
		 		m.put(key,val);
		 		//ignore "uninteresting" updates
		}else if(val.equals(range.bottom())){
				m.remove(key);
		} else{
			m.put(key,val);
		}
		return newLattice(m,isTop);
	}


	public Iterator values() {
		return map.values().iterator();
	}

	/**
	 * Pointwise inclusion
	 */
	public boolean includes(Lattice other) {
		if(other instanceof PartialMapLattice){
			if(isTop()) return true;

			PartialMapLattice mpl = (PartialMapLattice)other;
			if(mpl.isTop()) return false;
			
			if(isTop){
				for(Iterator i = keys(); i .hasNext();){
					Object k = i.next();
					Lattice l = this.get(k);
					Lattice l2 = mpl.get(k);
					if(!l.includes(l2)) return false;
				}
				return true;
			}else{
				for(Iterator i = mpl.keys(); i .hasNext();){
					Object k = i.next();
					Lattice l = this.get(k);
					Lattice l2 = mpl.get(k);
					if(!l.includes(l2)) return false;
				}
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.util.Lattice#meet(edu.cmu.cs.fluid.util.Lattice)
	 */
	public Lattice meet(Lattice other) {
		if(other instanceof PartialMapLattice){
			if(isTop()) return other;
			PartialMapLattice mpl =(PartialMapLattice)other;
			if(mpl.isTop()){
				return this;
			}
			HashMap<K,L> m = new HashMap<K,L>();
			if(isTop){
				if(mpl.isTop){
//TODO  There must be better ways of doing this...
					for(Iterator<K> i = keys(); i.hasNext();){
						K k = i.next();
						Lattice l = this.get(k);
						Lattice l2 = mpl.get(k);
						Lattice res = l.meet(l2);
						if(!res.equals(range.top())){
							m.put(k,(L)res);
						}
					}
					for(Iterator<K> i = mpl.keys(); i.hasNext();){
						K k = i.next();
						Lattice l = this.get(k);
						Lattice l2 = mpl.get(k);
						Lattice res = l.meet(l2);
						if(!res.equals(range.top())){
							m.put(k,(L)res);
						}
					}
				}else{
					for(Iterator<K> i = mpl.keys(); i.hasNext();){
						K k = i.next();
						Lattice l = this.get(k);
						Lattice l2 = mpl.get(k);
						Lattice res = l.meet(l2);
						if(!res.equals(range.bottom())){
							m.put(k,(L)res);
						}
					}					
				}
			} else {
				for(Iterator<K> i = keys(); i.hasNext();){
					K k = i.next();
					Lattice l = this.get(k);
					Lattice l2 = mpl.get(k);
					Lattice res = l.meet(l2);
					if(!res.equals(range.bottom())){
						m.put(k,(L)res);
					}
				}
			}
			return newLattice(m,isTop&&mpl.isTop);
		}
		return bottom();
	}


	@Override
  public boolean equals(Object other){
		if(other instanceof PartialMapLattice){
			PartialMapLattice l = (PartialMapLattice)other;
			return isTop == l.isTop && l.range.equals(range) && l.map.equals(map);
		}
		return false;
	}
	@Override
  public int hashCode(){
		return map.hashCode() + range.hashCode();
	}
	@Override
  public String toString(){
		return map.toString();
	}
}