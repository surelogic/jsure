/*
 * Created on Dec 16, 2003
 */
package edu.cmu.cs.fluid.util;

import java.util.Iterator;
/**
 *
 */
public class LesserStackLattice<T> implements Lattice<T> {


	//protected final Lattice[] store;
	protected final int count;
	
	protected final Lattice<T> val;
	
	protected final LesserStackLattice<T> prev;
	
	protected final LesserStackLattice<T> top;
	protected final LesserStackLattice<T> bottom;

	protected final Hashtable2<Lattice<T>, LesserStackLattice<T>, LesserStackLattice<T>> pushes;

	public LesserStackLattice(Lattice<T> ct){
		count = 0;
		prev = null;
		val = ct.top();
		top = this;
		pushes = new Hashtable2<Lattice<T>, LesserStackLattice<T>, LesserStackLattice<T>>();
		bottom = newLattice(val.bottom(),null,count);
	}


	protected LesserStackLattice(int c,Lattice<T> v,LesserStackLattice<T> p, Hashtable2<Lattice<T>, LesserStackLattice<T>, LesserStackLattice<T>> ps, LesserStackLattice<T> t, LesserStackLattice<T> b){
		count = c;
		val = v;
		prev = p;
		pushes = ps;
		top = t;
		bottom = (b==null)?this:b;
	}

	protected LesserStackLattice<T> newLattice(Lattice<T> v,LesserStackLattice<T> p, int c){
		return new LesserStackLattice<T>(c,v,p,pushes,top,bottom);
	}
	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#top()
	 */
	public Lattice<T> top() {
		return top;
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#bottom()
	 */
	public Lattice<T> bottom() {
		return bottom;
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#meet(edu.cmu.cs.fluid.util.Lattice)
	 */
	public Lattice<T> meet(Lattice<T> other) {
//		if(!(other instanceof StackLattice)){
//			return bottom();
//		}
		LesserStackLattice<T> o = (LesserStackLattice<T>)other;

//System.out.println("Meeting " + this +" with " + o);

		if(equals(o)){
			return this;
		}
		if(includes(o)){
			//System.out.println("Returning o");
			return o;
		}
		if(o.includes(this)){
			//System.out.println("Returning this");
			return this;
		}
		if(count != o.count){
			//System.out.println("returning bot");
			return bottom();
		}
		Lattice<T> m = val.meet(o.val);
		LesserStackLattice<T> mp = (LesserStackLattice<T>)prev.meet(o.prev);
		return newLattice(m,mp,count);
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#includes(edu.cmu.cs.fluid.util.Lattice)
	 */
	public boolean includes(Lattice<T> other) {
		if(!(other instanceof LesserStackLattice)){
			return false;
		}
		LesserStackLattice<T> o = (LesserStackLattice<T>)other;
		if(this == top || other == bottom){
			return true;
		}
		if(this == bottom || other == top){
			return false;
		}
		if(o.count != count){
			return false;
		}
		return val.includes(o.val) &&
			( (prev == o.prev) || (prev != null && prev.includes(o.prev)));
	}

	public boolean equals(LesserStackLattice<T> other){
		return this == other;
/*		if(this == other) return true;
		if(other.count != count){
			return false;
		}
		return val.equals(other.val) &&
			( (prev == other.prev) || (prev != null && prev.equals(other.prev)));
	*/}

	@Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object other){
		if(other instanceof LesserStackLattice){
			return equals((LesserStackLattice<T>)other);
		}
		return false;
	}

	@Override
  public int hashCode(){
		return prev==null?0:prev.hashCode() + val.hashCode();
	}
	
	public LesserStackLattice<T> push(Lattice<T> val){
		LesserStackLattice<T> ret = pushes.get(val,this);
		if(ret == null){ 
			ret =  newLattice(val,this,count+1);
			pushes.put(val,this,ret);
		}
		return ret;
	}
	
	public LesserStackLattice<T> pop(){
		return prev!=null?prev:bottom;
	}
	
	public LesserStackLattice<T> push(){
		return push(val.bottom());
	}
	
	public Lattice<T> topElement(){
		return val;
	}
	
	public Lattice<T> peek(){
		return val;
	}
	
	public boolean isEmpty(){
		return prev == null;
	}
	
	
	
	public int count(){
		return count;
	}
	
	public Iterator<Lattice<T>> values(){
		return new StackLatticeIterator<T>(this);
	}

  static class StackLatticeIterator<T> extends AbstractRemovelessIterator<Lattice<T>> {
		LesserStackLattice<T> curr;
		
		StackLatticeIterator(LesserStackLattice<T> a){
			curr = a;
		}
		public boolean hasNext(){
			return curr.prev != null;
		}
		public Lattice<T> next(){
			LesserStackLattice<T> l = curr;
			curr = curr.prev;
			return l.val;
		}
	}
	
	@Override
  public String toString(){
		if(this == top) return "_TOP_";
		if(this == bottom) return "_BOT_";
		StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		for(LesserStackLattice i = this; i.prev != null; i = i.prev){
			sb.append(i.val.toString()).append(".");
		}
		sb.append("nil }");
		return sb.toString();
	}
}
