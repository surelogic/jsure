/*
 * Created on Jul 14, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Set;
import java.util.Iterator;
import edu.cmu.cs.fluid.util.Pair;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.RecordLattice;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;

/**
 * Lattice jointly representing both a map from variables (and other expressions)
 * to SimpleLocations (ILocationMap) and a set of facts about those locations'
 * aliasing (AliasFactLattice).  Both interfaces are implemented as pass-through
 * for convenience.
 */
@Deprecated
public class LocationFactLattice extends RecordLattice implements ILocationFactLattice {

	public static final int LOCMAP = 0;
	public static final int FACTS = 1;

	protected LocationFactLattice(Lattice[] v, RecordLattice t, RecordLattice b) {
		super(v, t, b);
	}

	/**
	 */
	public LocationFactLattice(IRNode md, IBinder binder,LocationGenerator gen) {
		super(new Lattice[] {
			new LocationMap(md,binder,gen),
			new ConjunctiveFactLattice()
		});
	}
	
	/**
	 * @return the ILocationMap lattice
	 */
	public ILocationMap getMap(){
		return (ILocationMap)getValue(LOCMAP);
	}
	
	/**
	 * @return the AliasFactLattice
	 */
	public AliasFactLattice getFacts(){
		return (AliasFactLattice)getValue(FACTS);
	}
	
	public SimpleLocation getLocation(IRNode expr){
		return getMap().getLocation(expr);
	}
	public SimpleLocation nulLoc(){
		return getMap().nulLoc();
	}
	public ILocationMap replaceLocation(IRNode decl, SimpleLocation loc){
		return newLattice(getMap().replaceLocation(decl,loc));
	}
	
	@Override
  protected RecordLattice newLattice(Lattice l []){
		return new LocationFactLattice(l,top,bottom);
	}
	
	protected LocationFactLattice newLattice(ILocationMap lm){
		return (LocationFactLattice)newLattice( new Lattice[]{
			lm,
			getFacts()
		});
	}
	
	protected LocationFactLattice newLattice(AliasFactLattice fl){
		return (LocationFactLattice)newLattice( new Lattice[]{
					getMap(),
					fl
				});
	}
	
	/*
	 * @see edu.cmu.cs.fluid.util.Lattice#meet(edu.cmu.cs.fluid.util.Lattice)
	 * 
	 * Ugly. 
	 * <ol>
	 * <li> Meet the <tt>ILocationMap</tt>s</li>
	 * <li> For each location that changed in the merge, substitute the result
	 * in the appropriate (pre-meet) <tt>AliasFactLattice</tt></li>
	 * <li> Meet the post-substitution <tt>AliasFactLattices</tt></li>
	 * <li> Return a new lattice from the results</li>
	 * </ol>
	 */
	@Override
  public Lattice meet(Lattice otherL) {
		LocationFactLattice other = (LocationFactLattice) otherL;
		if (this == bottom || this == other || other == top) return this;
		if (this == top || other == bottom) return other;
		if (includes(other)) return other;
		if (other.includes(this)) return this;
		Lattice[] newValues = new Lattice[2];
		
		LocationMap leftMap = (LocationMap)getMap();
		LocationMap rightMap= (LocationMap)other.getMap();
		LocationMap lm = (LocationMap)leftMap.meet(rightMap);
		newValues[LOCMAP] = lm;

		AliasFactLattice left = getFacts();
		AliasFactLattice right = other.getFacts();

		if(left instanceof ConjunctiveFactLattice && right instanceof ConjunctiveFactLattice){
			AliasFactLattice lsub = new ConjunctiveFactLattice();
			AliasFactLattice rsub = new ConjunctiveFactLattice();
			for(java.util.Iterator i = lm.keys(); i.hasNext();){
				Object k = i.next();
				SimpleLocation lefty = leftMap.get(k);
				SimpleLocation righty = rightMap.get(k);
				SimpleLocation end = lm.get(k);
				if(!(lefty.equals(end))){
					lsub = lsub.addDoesAlias(lefty,end);
				}
				if(!righty.equals(end)){
					rsub = rsub.addDoesAlias(righty,end);
				}
			}
			newValues[FACTS] = ((ConjunctiveFactLattice)left).merge(lsub,right,rsub);
		}else{
			for(java.util.Iterator i = lm.keys(); i.hasNext(); ){
				Object k = i.next();
				SimpleLocation lefty = leftMap.get(k);
				SimpleLocation righty = rightMap.get(k);
				SimpleLocation end = lm.get(k);
				if(!(lefty.equals(end))){
					left = left.substitute(lefty,end);
				}
				if(!righty.equals(end)){
					right = right.substitute(righty,end);
				}	
			}
			newValues[FACTS] = left.meet(right);
		}
		if (bottom.equals(newValues)) return bottom;

		return newLattice(newValues);
	}

	
	public AliasFactLattice addDoesAlias(SimpleLocation a, SimpleLocation b) {
		if(LocationGenerator.isMeaningful(a) && LocationGenerator.isMeaningful(b)){
			AliasFactLattice f =  newLattice(getFacts().addDoesAlias(a,b));
			return f;
		}
		return this;
	}

	public AliasFactLattice addDoesNotAlias(SimpleLocation a, SimpleLocation b) {
		if(LocationGenerator.isMeaningful(a) && LocationGenerator.isMeaningful(b)){
					return newLattice(getFacts().addDoesNotAlias(a,b));
		}
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.AliasFactLattice
	 */
	public boolean doesAlias(SimpleLocation a, SimpleLocation b) {
		return getFacts().doesAlias(a,b);
	}

	public boolean doesNotAlias(SimpleLocation a, SimpleLocation b) {
		return getFacts().doesNotAlias(a,b);
	}

	public AliasFactLattice substitute(
		SimpleLocation older,
		SimpleLocation newer) {
		return newLattice(getFacts().substitute(older,newer));
	}

	@Override
  public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getMap().toString()).append("\n").
			append(getFacts().toString());
		return sb.toString(); 
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap
	 */
	public ILocationMap replaceLocation(
		SimpleLocation obj,
		IRNode fieldDecl,
		SimpleLocation loc) {
		Set eq = doesAlias(obj);
		LocationMap lm = (LocationMap)getMap();
		Set neq = doesNotAlias(obj);
		for(Iterator i = lm.keys();i.hasNext();){
			Object o = i.next();
			if(o instanceof Pair){
				Pair p = (Pair)o;
				if(p.second().equals(fieldDecl)){
					SimpleLocation lc = (SimpleLocation)p.first();
					if(! lc.equals(obj) && !neq.contains(lc)){
						lm = (LocationMap)lm.replaceLocation(lc,fieldDecl,(SimpleLocation)lc.bottom());
					}
				}
			}
		}
		for(Iterator i = eq.iterator();i.hasNext();){
			SimpleLocation lc = (SimpleLocation)i.next();
			if(LocationGenerator.isMeaningful(lc) && !LocationGenerator.isNull(lc))
				lm = (LocationMap)lm.replaceLocation(lc,fieldDecl,loc);
		}
		return newLattice(lm);
	}
	
	public boolean contains(SimpleLocation obj, IRNode fdecl){
		return ((LocationMap)getMap()).contains(obj,fdecl);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.AliasFactLattice
	 */
	public Set doesAlias(SimpleLocation a) {
		return getFacts().doesAlias(a);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.AliasFactLattice
	 */
	public Set doesNotAlias(SimpleLocation a) {
		return getFacts().doesNotAlias(a);
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.AliasFactLattice
	 */
	public boolean makeClaim(SimpleLocation l, LocationClaim c) {
		return getFacts().makeClaim(l,c);
	}
  /* 
   * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#getLocation(edu.cmu.cs.fluid.java.analysis.LocationField)
   */
  public SimpleLocation getLocation(LocationField lf) {
    // TODO Auto-generated method stub
    return getMap().getLocation(lf);
  }
	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#renameLocation(edu.cmu.cs.fluid.ir.IRNode)
	 */
	public ILocationMap renameLocation(IRNode expr) {
		return newLattice(getMap().renameLocation(expr));
	}
	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.ILocationMap#renameLocation(edu.cmu.cs.fluid.java.analysis.LocationField)
	 */
	public ILocationMap renameLocation(LocationField lf) {
		return newLattice(getMap().renameLocation(lf));
	}
}
