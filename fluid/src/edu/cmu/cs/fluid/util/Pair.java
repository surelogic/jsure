/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/Pair.java,v 1.5 2005/06/30 20:36:48 chance Exp $ */
package edu.cmu.cs.fluid.util;

/** A simple pair, defined so we can define equality and hashCode. */
public class Pair<T1,T2> {
  private final T1 elem1;
  private final T2 elem2;
  public Pair(T1 o1, T2 o2) {
    elem1 = o1;
    elem2 = o2;
  }
  public T1 first() { return elem1; }
  public T2 second() { return elem2; }
  
  @Override
  public boolean equals( Object other ) {
  	if( !( other instanceof Pair ) ) {
  		return false;
  	}
  	Pair otherPair = (Pair)other;
  	if( elem1 == null && elem2 == null ) {
  		return ( otherPair.elem1 == null && otherPair.elem2 == null );
  	}else if( elem1 == null ) {
  		return ( otherPair.elem1 == null && elem2.equals( otherPair.elem2 ) );
  	}else if( elem2 == null ) {
  		return ( otherPair.elem2 == null && elem1.equals( otherPair.elem1 ) );
  	}else {
  		return ( elem1.equals( otherPair.elem1 ) && elem2.equals( otherPair.elem2 ) );
  	}
  }
  
  @Override
  public int hashCode() {
    if( elem1 == null && elem2 == null ) {
    	return 0;
    }else if( elem1 == null ) {
    	return elem2.hashCode();
    }else if( elem2 == null ) {
    	return elem1.hashCode();
    }else {
    	return elem1.hashCode() + elem2.hashCode();
    }
  }
  
  @Override
  public String toString() {
	  return "<"+elem1+", "+elem2+'>';
  }
}