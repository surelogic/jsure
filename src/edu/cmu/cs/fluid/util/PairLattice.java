/*
 * Created on May 4, 2004
 *
 */
package edu.cmu.cs.fluid.util;

/**
 * @author dpgraves
 *
 * A lattice used to represent the functionality of two lattices at once
 * 
 */
public class PairLattice<T1,T2> implements Lattice<PairLattice.Type<T1,T2>> {
  public interface Type<T1,T2> {}

	private Lattice<T1> left = null;
	private Lattice<T2> right = null;
	
	private PairLattice<T1,T2> topLatt;
	private PairLattice<T1,T2> bottomLatt;
	
	protected PairLattice(Lattice<T1> l, Lattice<T2> r, PairLattice<T1,T2> t, PairLattice<T1,T2> b)
	{
		if(t == null){
		    topLatt = this;
		    left = l.top();
		    right = r.top();
		    return;
		}
		if(b == null){
		    bottomLatt = this;
		    left = l.bottom();
		    right = r.bottom();
		    return;
		}
		left = l;
		right = r;
		topLatt = t;
		bottomLatt = b;
	}
	
	public PairLattice(Lattice<T1> l, Lattice<T2> r) 
	{
		this.left = l;
		this.right = r;
		try
		{

		topLatt = newLattice(l,r);
		bottomLatt = newLattice(l,r);
		topLatt = bottomLatt.topLatt = topLatt.topLatt;
		bottomLatt = topLatt.bottomLatt = bottomLatt.bottomLatt;
		
		topLatt.left = l.top();
		topLatt.right = r.top();
		
		bottomLatt.left = l.bottom();
		bottomLatt.right = r.bottom();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	
	public PairLattice<T1,T2> newLattice(Lattice<T1> l, Lattice<T2> r){
		if(l.equals(l.top()) && r.equals(r.top())) return topLatt;
		if(l.equals(l.bottom()) && r.equals(r.bottom())) return bottomLatt;
		return new PairLattice<T1,T2>(l,r,topLatt,bottomLatt);
	}
  
	public Lattice<PairLattice.Type<T1,T2>> top() 
	{
		return topLatt;
	}
	
	public Lattice<PairLattice.Type<T1,T2>> bottom() 
	{
		return bottomLatt;
	}
  
	public Lattice<PairLattice.Type<T1,T2>> meet(Lattice<PairLattice.Type<T1,T2>> other) 
	{
		if(!(other instanceof PairLattice))
			return bottom();
	
    PairLattice<T1,T2> pair = (PairLattice<T1,T2>) other;
		if(this.equals(other))
			return this;
		if(includes(other)) return other;
		if(other.includes(this)) return this;
		Lattice<T1> leftVal = left.meet(pair.left);
		Lattice<T2> rightVal = right.meet(pair.right);
		return newLattice(leftVal,rightVal);
	}

	public boolean includes(Lattice<PairLattice.Type<T1,T2>> other) 
	{
		if(other instanceof PairLattice){
      PairLattice<T1,T2> o = (PairLattice<T1,T2>) other;
			return left.includes(o.left) && right.includes(o.right);
		}
		return false;
	}
  
	/**
	 * @return Returns the left lattice.
	 */
	public Lattice getLeft() {
		return left;
	}
	/**
	 * @return Returns the right lattice.
	 */
	public Lattice getRight() {
		return right;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
  public boolean equals(Object arg0) {
		if(arg0 instanceof PairLattice){
			PairLattice latt = (PairLattice)arg0;		
			return latt.left.equals(left) && latt.right.equals(right);
		}
		return false;
	}
  
  @Override
  public int hashCode() {
    return left.hashCode() + right.hashCode()*2;
  }
}
