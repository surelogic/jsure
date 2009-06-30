/*
 * Created on Jul 8, 2003
 */
package edu.cmu.cs.fluid.java.analysis;


import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.AbstractCachedSet;
import edu.cmu.cs.fluid.util.CachedSet;
import edu.cmu.cs.fluid.util.Hashtable2;
import edu.cmu.cs.fluid.util.PartialMapLattice;
import java.util.*;
import edu.cmu.cs.fluid.java.operator.NullLiteral;

/**
 * @author williamr
 *
 * A LocationGenerator provides SimpleLocations.  It ensures that
 * these locations are uniquely created.
 *
 * A SimpleLocation represents an abstract location in memory.  Each 
 * SimpleLocation maps to exactly one location in physical memory, but 
 * each physical location may be represented by more than one simple 
 * location.
 * 
 * For now each SimpleLocation is represented by the IRNode where that 
 * location is first referenced or by a pair of other locations from 
 * which it was created.  In addtion, each location is given a unique 
 * identification number (for this generator).
 */

@Deprecated
public class LocationGenerator{
 
	private int number_generator;
	
  
    protected final PartialMapLattice<IRNode,SimpleLocation> init_r;
  
	private final Hashtable2<Object, Object, Object> sources;
	private final Hashtable2<SimpleLocation, SimpleLocation, SimpleLocation> merged;

	//private final HashMap<SimpleLocation,Set<SimpleLocation>> renamings;
//	private final HashMap includes;
  
	private final Map<SimpleLocation,AbstractCachedSet<SimpleLocation>> includes;

	private final SimpleLocation topVal;
	private final SimpleLocation botVal;
  private final SimpleLocation nulVal;
  private final SimpleLocation unique;
  private final SimpleLocation shared;

  protected LocationAsserter leftLA;
  protected LocationAsserter rightLA;

	protected final TrackingIntraproceduralAnalysis ourAnalysis;

	
  public LocationGenerator(TrackingIntraproceduralAnalysis oA){
    number_generator = -4;
    //		sources = new Hashtable();
    merged = new Hashtable2<SimpleLocation, SimpleLocation, SimpleLocation>(); 
    sources = new Hashtable2<Object, Object, Object>();
    //renamings = new HashMap<SimpleLocation,Set<SimpleLocation>>();
    includes = new HashMap<SimpleLocation, AbstractCachedSet<SimpleLocation>>();
    botVal = new SimpleLocation();
    init_r = new PartialMapLattice<IRNode,SimpleLocation>(botVal,true);
    botVal.renames = init_r;
    topVal = new SimpleLocation();
    unique = new SimpleLocation();
    shared = new SimpleLocation();
    nulVal = new SimpleLocation();
    ourAnalysis = oA;
    leftLA = new DefaultLocationAsserter();
    rightLA = leftLA;
  }

  synchronized protected int next_number(){
    return number_generator++;
  }

	public class SimpleLocation implements Lattice{
	
		final private int subscript;
		private final SimpleLocation l_par;
		private final SimpleLocation r_par;
        public PartialMapLattice<IRNode,SimpleLocation> renames;
    /**
	 *  Give this location a unique subscript
	 */
		protected SimpleLocation() {
			super();
			subscript = next_number();
			/*
            HashSet<SimpleLocation> hS = new HashSet<SimpleLocation>();
            hS.add(this);
            renamings.put(this,hS);
            */
            renames = init_r;
            l_par = null;
			r_par = null;
			//System.err.println("Generating "+this);
      
    }
		
		protected SimpleLocation(SimpleLocation l, SimpleLocation r){
			super();
			subscript = next_number();
			/*	
		    HashSet<SimpleLocation> hS = new HashSet<SimpleLocation>();
            hS.add(this);
            renamings.put(this,hS);
*/			
			merged.put(l,r,this);
      merged.put(r,l,this);
/*/
          Pair p1 = new Pair(l,r);
          Pair p2 = new Pair(r,l);
      IRNode e = ourAnalysis.getUserNode();
      merged.put(p1,e,this);
      merged.put(p2,e,this);
      
  */
  renames = init_r;
            renames = (PartialMapLattice<IRNode,SimpleLocation>)
                          l.renames.meet(r.renames);
            l_par = l;
			r_par = r;
      //System.err.println("Generating "+this);

		}

		protected SimpleLocation(SimpleLocation orig){
			super();
			subscript = next_number();
			/*HashSet<SimpleLocation> hS = new HashSet<SimpleLocation>();
            hS.add(this);
            renamings.put(this,hS);
            */
            renames = orig.renames;
            l_par = orig;//.l_par;
			r_par = orig;//.r_par;
      //System.err.println("Generating "+this);
      
		}

        protected void addRenaming(IRNode n, SimpleLocation nname){
          renames = renames.update(n,nname);
        }
    
        protected SimpleLocation getRenaming(IRNode n){
          return renames.get(n);
        }
        
		public SimpleLocation[] parentsOrNull(){
			if(l_par == r_par){
				if(l_par == null)
					return null;
				return new SimpleLocation[]{l_par};
			}
			return new SimpleLocation[]{l_par,r_par};
		}

		public int subscript(){
			return subscript;
		}

		@Override
    public String toString(){
			if(this == top()) return "TOP";
			if(this == bottom()) return "BOT";
			if(this == nulVal) return "0";
          if(this == unique) return "unique";
          if(this == shared) return "shared";
			return "L_"+subscript();
		}
		/* 
		 * @see edu.cmu.cs.fluid.util.Lattice#bottom()
		 */
		public Lattice bottom() {
			return botVal;
		}

		/* 
		 * @see edu.cmu.cs.fluid.util.Lattice#includes
		 * (edu.cmu.cs.fluid.util.Lattice)
		 */
		public boolean includes(Lattice other) {
			if (!(other instanceof SimpleLocation)) return false;
			if(this.equals(top()) || other.equals(bottom())) return true;
			if(this.equals(bottom()) || other.equals(top())) return false;
			if(this.equals(shared))return other.equals(shared);
      if(this.equals(unique)) return other.equals(unique);
      SimpleLocation sother = (SimpleLocation)other;

			//if(includes(this,sother)) return true;
			//if(!(sother.loc == ourAnalysis.getLatestEdge())) return false;
			
			if (sother.equals(this) 
				|| this.includes(sother.l_par)
				|| this.includes(sother.r_par)){
					addInclusion(this,sother);
					return true;
				}
			return false;
		}

		/* 
		 * Slow,Slow,Slow
		 */

    public Lattice meet(Lattice other) {  

      if(this.includes(other)) return other;
		
      if(other.includes(this)) return this;

	/*		 */
		
      if (equals(other)) return other;
      SimpleLocation o = (SimpleLocation)other;
      if(equals(unique)){
        /*if(leftLA.assertUnique(o))
          return unique;        
          */
        //System.err.println("Meeting " + this + " & " + other+" at bottom");
        return this;
      }
      if( other.equals(unique)){
        /*if(rightLA.assertUnique(this))
          return unique;
        */
        //System.err.println("Meeting " + this + " & " + other+" at bottom");
return other;
      }

      if(equals(shared)){
        /*if(leftLA.assertShared(o))
          return shared;
        */
        //System.err.println("Meeting " + this + " & " + other+" at bottom");
return this;
      }
      if(other.equals(shared)){
        /*if(rightLA.assertShared(this))
          return shared;
        */
        //System.err.println("Meeting " + this + " & " + other+" at bottom");
return other;
      }

      final SimpleLocation ol = getOriginal(this);
		
      final SimpleLocation or = getOriginal(o);
		
      final SimpleLocation merge = mergeLocations(ol,or);//(this,(SimpleLocation)other);
		
      //System.out.println("Meeting " + this + " and " + other + " as " + merge + " at " + ourAnalysis.getLatestEdge());
		
      return merge;
		
    }

		/* 
		 * @see edu.cmu.cs.fluid.util.Lattice#top()
		 */
		public Lattice top() {
			return topVal;
		}

		@Override
    public boolean equals(Object other){
			if(other instanceof SimpleLocation){
				return subscript() == ((SimpleLocation)other).subscript();
			}
/*			*/
			return other == this;
		}

		@Override
    public int hashCode(){
			return subscript();
		}

		protected boolean includes(SimpleLocation l1, SimpleLocation l2){
			try{
				return includes.get(l1).contains(l2);
			}catch (NullPointerException e){
				//assume lookup failed
				return false;
			}
		}

		@SuppressWarnings("unchecked")
    protected void addInclusion(SimpleLocation l1,SimpleLocation l2){
			Object o = includes.get(l1);
			if(o instanceof AbstractCachedSet){
				((AbstractCachedSet<SimpleLocation>)o).addElement(l2);
			}else{
				includes.put(l1,CachedSet.<SimpleLocation>getEmpty().addElement(l2));
			}
		}

       public boolean isUnique(){
            return equals(unique);
          }
          public SimpleLocation getUnique(){
            return unique;
          }
          public boolean isShared(){
            return equals(shared);
          }
          public SimpleLocation getShared(){
            return shared;
          }
    }

    public interface LocationAsserter{
      public abstract boolean assertUnique(SimpleLocation u);
      public abstract boolean assertShared(SimpleLocation s);
    }

    public static class DefaultLocationAsserter implements LocationAsserter{
      public boolean assertShared(SimpleLocation s) {
        return false;
      }
      public boolean assertUnique(SimpleLocation u) {
        return false;
      }      
    }
    
    public void postLocationAsserterLeft(LocationAsserter la){
      leftLA = la;
    }
    public void postLocationAsserterRight(LocationAsserter la){
      rightLA = la;
    }

    public void clearLocationAsserter(){
      leftLA = new DefaultLocationAsserter();
      rightLA = leftLA;
    }
    
	public SimpleLocation top(){
		return topVal;
	}
	
	public SimpleLocation bottom(){
		return botVal;
	}
	
	/**
	 * @return the <tt>null</tt> location.
	 */
	public SimpleLocation getNull(){
		return nulVal;
	}
	
	/**
	 * Return a unique (new) location at which the result of (all executions of)
	 * source is stored.
	 * @param source An IR expression.
	 */	
	public SimpleLocation getLocation(IRNode source){
		if(JJNode.tree.getOperator(source) instanceof NullLiteral)
			return getNull();
		return getLocation(source,source);
	}

	SimpleLocation getLocation(Object s1, Object s2){
		Object o = sources.get(s1,s2);
		if (o instanceof SimpleLocation){
			return (SimpleLocation)o;
	 	}else{
			SimpleLocation l = new SimpleLocation();
			sources.put(s1,s2,l);
			//l = (SimpleLocation)sources.get(source,source);
			sources.put(l,l,s2);
			return l;
	 }
	 //TODO should throw exception ?
	 //return botVal;
	}

	public SimpleLocation renameLocation(IRNode source){
		Object o = sources.get(source,source);
		SimpleLocation l;
		if(o instanceof SimpleLocation){
			l = duplicateLocation((SimpleLocation)o);
		}else{
			l = new SimpleLocation();
		}
		sources.put(source,source,l);
		sources.put(l,l,source);
		return l;
	}

	public SimpleLocation renameLocation(SimpleLocation l){
		IRNode n = ourAnalysis.getUserNode();
		SimpleLocation l2 = duplicateLocation(l);
		sources.put(n,n,l2);
		return l2;
	}
	
	public IRNode getSource(SimpleLocation loc){
		Object o =  sources.get(loc,loc);
		if(o instanceof IRNode){
			return (IRNode)o;
		}
		//BAD
		return null;
	}
	
	/**
	 * Returns the unique (new) location resulting from merging l1 and l2
	 * (in that order).	 
	 * @param l1
	 * @param l2
	 */
	/*
	 * Deliberately assymetric on l1,l2.
	 */
	public SimpleLocation mergeLocations(SimpleLocation l1, SimpleLocation l2){
/*		Pair p = new Pair(l1,l2);
		IRNode e = ourAnalysis.getUserNode();
		Object o = merged.get(p,e);
*/		Object o = merged.get(l1,l2);
		if(o instanceof SimpleLocation){
			return (SimpleLocation)o;
		}else{
		
			SimpleLocation l = new SimpleLocation(l1,l2);
//System.out.println("Merging " + l1 + " and " + l2 + " to get "+ l + " at "+e);
//			merged.put(p,e,l);
//			l = (SimpleLocation)merged.get(p,e);
//			merged.put(l1,l2,l);
			sources.put(l,l,ourAnalysis.getUserNode());
			return l;
		}
	}
	/**/
	public SimpleLocation duplicateLocation(SimpleLocation l){
		if(isNull(l)){
			return l;
		}
    if(!isMeaningful(l)){
      SimpleLocation l2 = new SimpleLocation();
      return l2;
    }
    
		SimpleLocation lo = l;
		IRNode n = ourAnalysis.getUserNode();//Renaming only once per locale
		
		SimpleLocation o = l.getRenaming(n);
		if(o != null && isMeaningful(o))return o;
/*		while(l.l_par!=null && l.l_par.equals(l.r_par)){
			System.out.println("Really " + l + " is " + l.l_par);
			l = l.l_par;
			o = sources.get(l,n);
			if(o instanceof SimpleLocation)return (SimpleLocation)o;
			//return duplicateLocation(l.l_par);
		}
	/*	
		l = getDuplicate(l);
		if(l != null) return l;
    /*  */
		SimpleLocation l2 =  new SimpleLocation();
		lo.addRenaming(n,l2);
        l2.addRenaming(n,l2);
        //ControlEdge n = ourAnalysis.getLatestEdge();
		//System.out.println("Duplicating "+ lo + " as " + l2 + " at " + n);
		/*
		Set<SimpleLocation> rns = renamings.get(l);
		rns.add(l2);
        renamings.put(l2,rns);
        */
        IRNode s = getSource(lo);
		sources.put(l2,l2,s == null?n:s);
		//sources.put(lo,n,l2);
		
		return l2;
	}
	/*
	private SimpleLocation getDuplicate(SimpleLocation l){
	  if(l == null) return l;
		ControlEdge n = ourAnalysis.getLatestEdge();//Renaming only once per locale
		
		//Object o = sources.get(l,n);
		SimpleLocation o = l.getRenaming(n);
		if(o != null && isMeaningful(o))return o;
//		if(o instanceof SimpleLocation)return (SimpleLocation)o;
		SimpleLocation lp = getDuplicate(l.l_par);
		SimpleLocation rp = getDuplicate(l.r_par);
		if(lp != null && lp.equals(rp)) return lp;
		return null;
	}
  */
  
	private SimpleLocation getOriginal(SimpleLocation l){
	  if(l == null) return l;
	  if(l.l_par == null || l.r_par == null)
	    return l;
	  SimpleLocation lp = getOriginal(l.l_par);
	  SimpleLocation rp = getOriginal(l.r_par);
		if(lp != null && lp.equals(rp)) return lp;
		return l;
	}
	/**/
	/**
	 * @param l
	 * @return true iff l != top and l != bottom
	 */
	public static boolean isMeaningful(SimpleLocation l){
		return !(l.equals(l.top()) || l.equals(l.bottom()));
	}
	public static boolean isNull(SimpleLocation l){
		return l.subscript() == 0;
	}
  public SimpleLocation getUnique(){
    return unique;
  }
  public boolean isUnique(SimpleLocation l){
    return l.isUnique();
  }
}
