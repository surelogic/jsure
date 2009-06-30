/*
 * Created on Jul 14, 2003
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.Pair;
import edu.cmu.cs.fluid.util.CachedSet;
import edu.cmu.cs.fluid.util.AbstractCachedSet;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;

import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

import java.util.*;

@Deprecated
interface KeptFact{
	abstract public boolean contains(SimpleLocation l);
}

/**
 * A single alias fact.
 * isDoesAlias is true iff this fact is of the form a == b
 */
@Deprecated
class AliasFact extends Pair<SimpleLocation,SimpleLocation> implements KeptFact{

	public AliasFact(SimpleLocation a, SimpleLocation b, boolean c){
		super(a,b);
		isDoesAlias = c;
	}

	public final boolean isDoesAlias;

	public boolean contains(SimpleLocation l){
		return l.equals(first()) || l.equals(second());
	}

	@Override
  public boolean equals(Object other){
		try{
			AliasFact oth = (AliasFact)other;
			return isDoesAlias == oth.isDoesAlias 
					&& ( (first().equals(oth.first()) 
						&& second().equals(oth.second())) 
					||(first().equals(oth.second()) 
						&& second().equals(oth.first())) 
					);
		}catch(ClassCastException e){
			return false;
		}
	}
		
	@Override
  public int hashCode(){
		return first().hashCode() + second().hashCode() + (isDoesAlias?0:1);
	}
		
	@Override
  public String toString(){
		return first().toString() + (
			isDoesAlias?"==":"!=") + second().toString();
	}
}
@Deprecated
class AggregateFact extends Pair<LocationField,LocationField> implements KeptFact{
	
	public AggregateFact(LocationField in, LocationField around){
		super(in,around);
	}
	
	
	public boolean contains(SimpleLocation l){
		return l.equals(first()) || l.equals(second());
	}

	@Override
  public boolean equals(Object other){
		if(other instanceof AggregateFact){
			AggregateFact oth = (AggregateFact)other;
		  	final Object f = first(), s = second();
		  	final Object of = oth.first(), os = oth.second();
//		  	final boolean fn = f == null, sn = s == null;
		  	final boolean fb = (f == null)?of == null:f.equals(of);//(!fn && f.equals(of)) || (fn && of == null);
		  	final boolean sb = (s == null)?os == null:s.equals(os);//(!sn && s.equals(os)) || (fn && os == null);
		  	return fb && sb;
		}
		return false;
	}
  
	
  @Override
  public int hashCode() {
  	final Object f = first(), s = second();
  	return (f==null?0:f.hashCode() )+ (s==null?0:s.hashCode());  
  }
	
	@Override
  public String toString(){
	  	final Object f = first(), s = second();
		return (f==null?"??":f.toString() )+ "<" + (s==null?"??":s.toString());
	}
}

/**
 * Essentially a wrapper on a set of aliasing facts.  Each fact is either
 * a == b or a != b.  Logically represents the conjunction of all facts.
 * Lattice operations follow those of an IntersectionLattice.
 */
@SuppressWarnings("unchecked")
@Deprecated
public class ConjunctiveFactLattice implements AliasFactLattice{


	protected static KeptFact getSubstituteVal(SimpleLocation older, SimpleLocation newer, KeptFact f){
		if (f instanceof AliasFact){
			return getSubstituteVal(older,newer,(AliasFact)f);
		}else {
			return getSubstituteVal(older,newer,(AggregateFact)f);
		}
	}

	protected static AliasFact getSubstituteVal(SimpleLocation older, SimpleLocation newer, AliasFact f){
		SimpleLocation first = f.first();
		SimpleLocation second = f.second();
		if(older.equals(first))
			first = newer;
		if(older.equals(second))
			second = newer;
		return new AliasFact(first,second, f.isDoesAlias);
	}
	
	protected static AggregateFact getSubstituteVal(SimpleLocation older, SimpleLocation newer, AggregateFact f){
		LocationField o = f.first();
		LocationField t =  f.second();
		SimpleLocation first = o.l;
		SimpleLocation second = t.l;
		if(older.equals(first))
			first = newer;
		if(older.equals(second))
			second = newer;
		return new AggregateFact(new LocationField(first,o.fdecl),new LocationField(second,t.fdecl));
	}
	
	protected static boolean isDoesAlias(AliasFact f){
		return f.isDoesAlias;
	}
	
	public ConjunctiveFactLattice(){
		l = CachedSet.getEmpty();
		//equivCache = new HashMap();
		subs = CachedSet.getEmpty();
	}
	/**/
	protected ConjunctiveFactLattice(Set l){
		this(l,CachedSet.getEmpty());
	}
	/*	
	protected ConjunctiveFactLattice(ConjunctiveFactLattice c, ConjunctiveFactLattice ls, ConjunctiveFactLattice rs){
		this(c.l,ls.l,rs.l);
	}
	*/
	protected ConjunctiveFactLattice(Set l, Set ls){
		this(l,ls,new HashSet()); // nopt Efficient!
	}
	
	protected ConjunctiveFactLattice(Set l, Set ls, Set rs){
		this.l = (AbstractCachedSet)CachedSet.getEmpty().union(l);
		//equivCache = new HashMap();
		subs = (AbstractCachedSet)CachedSet.getEmpty().union(ls).union(rs);
	}
	
	protected final AbstractCachedSet l;
	public static final ConjunctiveFactLattice bottom = 
																				new ConjunctiveFactLattice();
	public static final ConjunctiveFactLattice top = 
																				new ConjunctiveFactLattice();

	//protected final Map equivCache;

	protected final AbstractCachedSet subs;
	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#bottom()
	 */
	public Lattice bottom() {
		return bottom;
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#includes(edu.cmu.cs.fluid.util.Lattice)
	 */
	
	private static boolean includes(ConjunctiveFactLattice t, ConjunctiveFactLattice o){
		if(t == null){
			return o == null || o.l.isEmpty();
		}
		return o == null || t.includes(o);
	}
	private static ConjunctiveFactLattice union(ConjunctiveFactLattice t, ConjunctiveFactLattice o){
		if(t == null)
			return o;
		if(o == null){
			return t;
		}
		return t.union(o);
	}
	
	public boolean includes(Lattice other) {
		if(other instanceof ConjunctiveFactLattice){
			if(this == top) return true;
			if(other == bottom) return true;
			if(this == bottom) return false;
			if(other == top) return false;
			ConjunctiveFactLattice o = ((ConjunctiveFactLattice)other);
			AbstractCachedSet s = (o).l;
			if(subs.isEmpty()){
				return /*o.subs.isEmpty() &&*/ l.includes(s);
			}
			else{
				Object ss[] = subs.toArray();
				Object os[] = o.subs.toArray();
				for(int i = 0; i < ss.length; ++i){
					ss[i] = ((ImmutableHashOrderSet)ss[i]).union(l);
				}
				for(int i = 0; i < os.length; ++i){
					os[i] = ((ImmutableHashOrderSet)os[i]).union(s);
				}
				for(int i = 0; i < os.length; ++i){
					boolean not_included = true;
					for(int j = 0; not_included && j < ss.length; ++j){
					  not_included = !implies((ImmutableHashOrderSet)os[i],(ImmutableHashOrderSet)ss[j]);
          }
					if(not_included){
						return false;
					}
				}
				return true;
			}
		}
		
		return false;	
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#meet(edu.cmu.cs.fluid.util.Lattice)
	 */
	public Lattice meet(Lattice other) {
		if (this == bottom || this == other || other == top) return this;
		if (this == top || other == bottom) return other;
		if (includes(other)) return other;
		if (other.includes(this)) return this;
		if(!(other instanceof ConjunctiveFactLattice))
			return bottom();
		return new 
				ConjunctiveFactLattice(l.intersect(((ConjunctiveFactLattice)other).l));
		
	}
/*
	private ConjunctiveFactLattice combine(ConjunctiveFactLattice other){
		if (this == bottom || this == other || other == top) return this;
		if (this == top || other == bottom) return other;
		if (includes(other)) return other;
		if (other.includes(this)) return this;
		ConjunctiveFactLattice empty = new ConjunctiveFactLattice();
		return (ConjunctiveFactLattice)merge()
	}
	*/
	public AliasFactLattice merge(AliasFactLattice subs, AliasFactLattice other, 
			AliasFactLattice osubs){
		if (this == bottom  || other == top) return this;
		if (this == top || other == bottom ) return other;
//		if(this.equals(other)) return this;

       //System.err.println("\n\nMerging "+this+"\n( + "+subs+")\n and "
       //     +other+"\n( + "+osubs+")\n");
		
/*		
		if (includes(other)) return other;//new ConjunctiveFactLattice(o,ls,rs);
		if (other.includes(this)) return this;//new ConjunctiveFactLattice(this,ls,rs);
*/
		ConjunctiveFactLattice s1 = (ConjunctiveFactLattice)subs;
		ConjunctiveFactLattice s2 = (ConjunctiveFactLattice)osubs;

		ConjunctiveFactLattice o = (ConjunctiveFactLattice)other;

		ConjunctiveFactLattice c1 = this.suball(s1);
		ConjunctiveFactLattice c2 = o.suball(s2);

		HashSet<ImmutableSet> ns = new HashSet<ImmutableSet>(),nos = new HashSet<ImmutableSet>();
		
		if(s1.l.isEmpty()){
          ns.addAll(this.subs);
		}else if(this.subs.isEmpty()){
			ns.add(s1.l);
		}else{
			for(Iterator i = this.subs.iterator(); i.hasNext();){
				ns.add(s1.l.union((Set)i.next()));
			}
		}
		
       //System.err.println("\nns = "+ns);

		if(s2.l.isEmpty()){
            nos.addAll(o.subs);
		}else if(o.subs.isEmpty()){
			nos.add(s2.l);
		}else{
			//nos = new HashSet<ImmutableSet>();
			for(Iterator i = o.subs.iterator(); i.hasNext();){
				nos.add(s2.l.union((Set)i.next()));
			}
		}

        //System.err.println("\nnos= "+nos);

    
		ConjunctiveFactLattice ret =  new ConjunctiveFactLattice(c1.l.intersect(c2.l),ns,nos);

     //System.err.println("\n\nResult = " + ret);
    
		return ret;
	}
	
	public ConjunctiveFactLattice suball(ConjunctiveFactLattice subs){
		AliasFactLattice ret = this;
		for(Iterator i = subs.l.iterator(); i.hasNext();){
			KeptFact f = (KeptFact)i.next();
			if(f instanceof AliasFact){
				AliasFact f2 = (AliasFact)f;
				ret = ret.substitute(f2.first(), f2.second());
			}
		}
		return (ConjunctiveFactLattice)ret;
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#top()
	 */
	public Lattice top() {
		return top;
	}

	public AliasFactLattice addDoesAlias(SimpleLocation a, SimpleLocation b){
		if(a.equals(a.top()) || a.equals(a.bottom())|| b.equals(b.top()) || b.equals(b.bottom())) return this;
		AliasFactLattice f = new ConjunctiveFactLattice(l.addElement(new AliasFact(a,b,true)),subs);
		return f;
	}
	
	public AliasFactLattice addDoesNotAlias(SimpleLocation a, SimpleLocation b){
		if(a.equals(a.top()) || a.equals(a.bottom())|| b.equals(b.top()) || b.equals(b.bottom())) return this;
		return new ConjunctiveFactLattice(l.addElement(new AliasFact(a,b,false)),subs);
	}
	
	public ConjunctiveFactLattice addIsMappedInto(LocationField f1,LocationField f2){
		final SimpleLocation a = f1.l, b = f2.l;
		if(a.equals(a.top()) || a.equals(a.bottom())|| b.equals(b.top()) || b.equals(b.bottom())) return this;
		return new ConjunctiveFactLattice(l.addElement(new AggregateFact(f1,f2)),subs);
	}
	
	public AliasFactLattice substitute(SimpleLocation older, SimpleLocation newer){
		ImmutableHashOrderSet added = CachedSet.getEmpty();
		if(!l.isInfinite()){
			Iterator i = l.iterator();
			while(i.hasNext()){
				KeptFact f = (KeptFact)i.next();
				if(f.contains(older)){
					added = added.addElement(getSubstituteVal(older,newer,f));
				}
			}
		}
		Set res = l.union(added);
		return new ConjunctiveFactLattice(res,subs);
	}

	public boolean doesAlias(SimpleLocation a, SimpleLocation b){
		if(a.equals(b)) return true;
		ImmutableHashOrderSet equiv = getEquiv(a);
		return equiv.contains(b);
	}
	
	@SuppressWarnings("unchecked")
  public Set<SimpleLocation> doesAlias(SimpleLocation a){
		return getEquiv(a);
	}
	/*
	 * return a set representing the transitive closure of all == facts for a
	 * single location.
	 * @param a the aforementioned location.
	 */
	protected ImmutableHashOrderSet getEquiv(SimpleLocation a){
	  return getEquiv(a,l);
    }
    
    protected static ImmutableHashOrderSet getEquiv(SimpleLocation a, ImmutableHashOrderSet l){
      ImmutableHashOrderSet equiv = null;//CachedSet.getEmpty();//(ImmutableHashOrderSet)equivCache.get(a);
		if(equiv == null){
			equiv =CachedSet.getEmpty().addElement(a);
		}
		if (l.isInfinite())
			return equiv;

		for(ImmutableHashOrderSet temp = equiv;
					true; temp = temp.union(equiv)){
						Iterator i = l.iterator();
						while(i.hasNext()){
							KeptFact f1 = (KeptFact) i.next();
			
							if(!(f1 instanceof AliasFact)){ continue;	}
			
							AliasFact f = (AliasFact)f1;

							if(isDoesAlias(f)){
								if(temp.contains(f.first())){
									equiv = equiv.addElement(f.second());
								}
								if(temp.contains(f.second())){
									equiv = equiv.addElement(f.first());
								}
							}
						}
						if(temp.containsAll(equiv)){
							//equivCache.put(a,equiv);	
							return equiv;
						}
					}
		
//		return equiv;
	}

	public boolean doesNotAlias(SimpleLocation a, SimpleLocation b){
		return doesNotAlias(a).contains(b);
	}
	
	public Set doesNotAlias(SimpleLocation a){
	  return doesNotAlias(a,l);
  }
    protected static Set doesNotAlias(SimpleLocation a, ImmutableHashOrderSet l){
      ImmutableHashOrderSet n_eq = CachedSet.getEmpty();
		
		if(l.isInfinite()) return n_eq;
		ImmutableHashOrderSet eq = getEquiv(a,l);

		for(Iterator i = l.iterator();i.hasNext();){
			KeptFact f1 = (KeptFact) i.next();
			
			if(!(f1 instanceof AliasFact)){ continue;	}
			
			AliasFact f = (AliasFact)f1;
			if(!isDoesAlias(f)){
				Object ft = f.first();
				Object sd = f.second();
				if(eq.contains(ft)){
					n_eq = n_eq.addElement(sd);
				}
				if(eq.contains(sd)){
					n_eq = n_eq.addElement(ft);
				}
			}
		}
		ImmutableHashOrderSet s = CachedSet.getEmpty().union(n_eq);
		for(Iterator i = n_eq.iterator(); i.hasNext();){
			ImmutableHashOrderSet s2 = getEquiv((SimpleLocation)i.next(),l);
			s = s.union(s2);
		}
		return s;
	}
	
	public boolean isImmediatelyWithin(LocationField f1, LocationField f2){
		if(!l.isInfinite()){
			for(Iterator i = l.iterator(); i.hasNext();){
				KeptFact ff1 = (KeptFact) i.next();
			
				if(!(ff1 instanceof AggregateFact)){ continue;	}
			
				AggregateFact f = (AggregateFact)ff1;
				
				if(f.equals(new AggregateFact(f1,f2))){
					return true;
				}
			}
		}
		return false;
	}
	
	public Set immediateAbove(LocationField f1){
		ImmutableHashOrderSet //above = (ImmutableHashOrderSet)equivCache.get(f1);
		//if(above == null){
			above = CachedSet.getEmpty();
		//}
		
		if (l.isInfinite())
			return above;

		for(Iterator i = l.iterator(); i.hasNext();){
			KeptFact ff1 = (KeptFact) i.next();

			if(!(ff1 instanceof AggregateFact)){ continue;	}

			AggregateFact f = (AggregateFact)ff1;
	
			if(f.first().equals(f1)){
				above = above.addElement(f.second());
			}
		}
		//equivCache.put(f1,above);
		return above;
	}
	
	public Set immediateBelow(LocationField f1){
		ImmutableHashOrderSet below = CachedSet.getEmpty();
		
		if (l.isInfinite())
			return below;

		for(Iterator i = l.iterator(); i.hasNext();){
			KeptFact ff1 = (KeptFact) i.next();

			if(!(ff1 instanceof AggregateFact)){ continue;	}

			AggregateFact f = (AggregateFact)ff1;
	
			if(f.second().equals(f1)){
				below = below.addElement(f.first());
			}
		}
		return below;
	}

	public boolean isRecursivelyWithin(LocationField f1, LocationField f2){
		return allAbove(f1).contains(f2);
	}
	
	@SuppressWarnings("unchecked")
  public Set<LocationField> allAbove(LocationField f1){
		ImmutableHashOrderSet above;// = //(ImmutableHashOrderSet)equivCache.get(f1);
//		if(above == null){
			above = CachedSet.getEmpty().addElement(f1);
//		}
		
		if (l.isInfinite())
			return above.removeElement(f1);

		for(ImmutableHashOrderSet temp = above;
					true; temp = temp.union(above)){
						Iterator i = l.iterator();
						while(i.hasNext()){
							KeptFact ff1 = (KeptFact) i.next();
			
							if(!(ff1 instanceof AggregateFact)){ continue;	}
			
							AggregateFact f = (AggregateFact)ff1;
							if(temp.contains(f.first())){
								above = above.addElement(f.second());
							}
						}
						if(temp.containsAll(above)){
							//equivCache.put(f1,above);	
							return above.removeElement(f1);
						}
					}

	}

	public Set<LocationField> fullAbove(LocationField lf){
	  HashSet<LocationField> abv = new HashSet<LocationField>();
	  for(Iterator<SimpleLocation> i = doesAlias(lf.l).iterator(); i.hasNext();){
	    abv.addAll(allAbove(new LocationField(i.next(),lf.fdecl)));
	  }
	  return abv;
	}
	public Set<LocationField> fullBelow(LocationField lf){
	  HashSet<LocationField> abv = new HashSet<LocationField>();
	  for(Iterator<SimpleLocation> i = doesAlias(lf.l).iterator(); i.hasNext();){
	    abv.addAll(allBelow(new LocationField(i.next(),lf.fdecl)));
	  }
	  return abv;
	}
	
	@SuppressWarnings("unchecked")
  public Set<LocationField> allBelow(LocationField f1){
		ImmutableHashOrderSet below = CachedSet.getEmpty().addElement(f1);
		
		if (l.isInfinite())
			return below.removeElement(f1);
		
		for(ImmutableHashOrderSet temp = below;
					true; temp = temp.union(below)){
		  Iterator i = l.iterator();
			while(i.hasNext()){
			  KeptFact ff1 = (KeptFact) i.next();
			
			  if(!(ff1 instanceof AggregateFact)){ continue;	}
							
			  AggregateFact f = (AggregateFact)ff1;

			  if(temp.contains(f.second())){
			    below = below.addElement(f.first());
			  }
			}
			if(temp.containsAll(below)){	
			  return below.removeElement(f1);
			}
		}
	}

	
	@Override
  public String toString(){
		StringBuilder sb = new StringBuilder();
		if(!l.isInfinite()){
			Iterator i = l.iterator();
			for(int j = 0;i.hasNext();++j){
				KeptFact f = (KeptFact)i.next();
				sb.append(f.toString());
				if(j == 10){ sb.append("\n"); j = 0;}
				if(i.hasNext()) sb.append(" ^ ");
			}
		}		
		for(Iterator i = subs.iterator(); i.hasNext();){		
			sb.append("\n\t + [" + i.next() + "]");
		}
		return sb.toString();
	}

	@Override
  public boolean equals(Object Xother){
		if(Xother instanceof ConjunctiveFactLattice){
			ConjunctiveFactLattice o = (ConjunctiveFactLattice)Xother;
			 if(o.includes(this) && includes(o)) return true;
      ImmutableHashOrderSet s = o.l;
      
			if(subs.isEmpty()){
				return o.subs.isEmpty() && l.equals(s);
			}
			else{
				Object ss[] = subs.toArray();
				Object os[] = o.subs.toArray();
				if(ss.length != os.length) return false;
				for(int i = 0; i < ss.length; ++i){
					ss[i] = ((ImmutableHashOrderSet)ss[i]).union(l);
					os[i] = ((ImmutableHashOrderSet)os[i]).union(s);
				}
				for(int i = 0,k = ss.length; i < os.length; ++i){
					boolean not_included = true;
					for(int j = 0; not_included && j < k; ++j){
						if(ss[j].equals(os[i])){
							not_included = false;
							--k;
							ss[j] = ss[k];
						}
					}
					if(not_included){
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

  @Override
  public int hashCode() {
    return l.hashCode() + subs.hashCode();
  }      
  
	protected ConjunctiveFactLattice union(ConjunctiveFactLattice other){
		return new ConjunctiveFactLattice(l.union(other.l),subs.union(other.subs));
	}

	private ConjunctiveFactLattice union(Set s){
		return new ConjunctiveFactLattice(l.union(s),CachedSet.getEmpty());
	}
	
	public boolean makeClaim(SimpleLocation l, LocationClaim c) {
		if(c.makeClaim(l)){
			return true;
		}
		for(Iterator i = doesAlias(l).iterator(); i.hasNext();){
			SimpleLocation sl = (SimpleLocation)i.next();
			if(c.makeClaim(sl)){
				return true;
			}
		}

		if(subs.isEmpty()) return false;
		boolean all_ok = true;
		for(Iterator i = subs.iterator();all_ok && i.hasNext();){
			all_ok = all_ok && union((Set)i.next()).makeClaim(l,c);
		}
		return all_ok;
	}

	protected boolean makeAgClaimHelper(AggregatingClaim c, LocationField lf) {
			SimpleLocation l = lf.l;
			if(c.makeClaim(l)){
				return true;
			}
			for(Iterator i = doesAlias(l).iterator(); i.hasNext();){
				SimpleLocation loc = (SimpleLocation)i.next();
				if(l.equals(loc))
					continue;
				else if(c.makeClaim(loc))
					return true;
				else{
					LocationField llff = new LocationField(loc,lf.fdecl);
					for(Iterator j = allAbove(llff).iterator();j.hasNext();){
								LocationField llf = (LocationField)j.next();
								if(c.extend(llf.fdecl).makeClaim(llf.l)){
									return true;
								}
							}				
				}
			}

			if(subs.isEmpty()) return false;
			boolean all_ok = true;
			for(Iterator i = subs.iterator();all_ok && i.hasNext();){
				all_ok = all_ok && union((Set)i.next()).makeClaim(l,c);
			}
			return all_ok;
		}

	public boolean makeAggregatingClaim(AggregatingClaim c, LocationField lf){
		if(makeAgClaimHelper(c,lf)){
			return true;
		}
				
		for(Iterator i = allAbove(lf).iterator();i.hasNext();){
			LocationField llf = (LocationField)i.next();
			if(makeAgClaimHelper(c.extend(llf.fdecl),llf)){
				return true;
			}
		}
		return false;
	}

	public static ConjunctiveFactLattice getRegionMappings(SimpleLocation obj, IJavaType classDecl,IBinder b){
		final Set<AggregateFact> relation = new HashSet<AggregateFact>();

		final Set regions = RegionAnnotation.getInstance().getAllRegionsInClass(classDecl);

		final LocationField all= LocationField.allField(obj,b);

		for(final Iterator i = regions.iterator(); i.hasNext();){
			final IRNode rdecl = (IRNode)i.next();
      

			final IRNode par = b.getRegionParent(rdecl);
			final LocationField f = new LocationField(obj,rdecl);
			if(par != null){
				final LocationField t = new LocationField(obj,par);
				relation.add(new AggregateFact(f,t));
			}else{
				relation.add(new AggregateFact(f,all));
			}
 		}
    if(classDecl instanceof IJavaArrayType){
      final LocationField elem = new LocationField(obj,LocationField.arrayRegion(b));
      relation.add(new AggregateFact(elem,all));
    }
		/*
     * Need field aggregation/mapping 
		 */
    
		ImmutableHashOrderSet rel2 = new ImmutableHashOrderSet(relation);
		return new ConjunctiveFactLattice(rel2);
	}

	public ConjunctiveFactLattice addRegionMappings(SimpleLocation l, IJavaType t, IBinder b){
		if(t instanceof IJavaDeclaredType || t instanceof IJavaArrayType){
			final SimpleLocation loc = l;
			return union(getRegionMappings(loc,t,b));
		}
		return this;
	
	}
	
  public static ConjunctiveFactLattice getFieldAggregations(SimpleLocation loc, IJavaDeclaredType t, IBinder b, ILocationMap lm){
    final Set<AggregateFact> relation = new HashSet<AggregateFact>();
    final IRNode classd = t.getDeclaration();
    Operator op = JJNode.tree.getOperator(classd);
    if(ClassDeclaration.prototype.includes(op)){
      final IRNode classb = ClassDeclaration.getBody(classd);
      op = JJNode.tree.getOperator(classb);
      if(ClassBody.prototype.includes(op)){
        for(final Iterator<IRNode> i = ClassBody.getDeclIterator(classb); i.hasNext();){
          final IRNode fdecl = i.next();
          if(FieldDeclaration.prototype.includes(fdecl)){
            final IRNode field_ag = RegionAnnotation.getFieldAggregationOrNull(fdecl);
            if(field_ag != null){
              final Iterator<IRNode> maps = MappedRegionSpecification.getMappingIterator(field_ag);
              while(maps.hasNext()){
                final IRNode map = maps.next();
                final IRNode from = RegionMapping.getFrom(map);
                final IRNode to = RegionMapping.getTo(map);
                final IRNode fB = b.getBinding(from);
                final IRNode tB = b.getBinding(to);
                LocationField the_field = new LocationField(loc,fdecl);
                SimpleLocation floc = lm.getLocation(the_field);
                if(!LocationGenerator.isMeaningful(floc)){
                  
                }
                LocationField f = new LocationField(floc,fB);
                LocationField t2 = new LocationField(loc,tB);
        //System.out.println("Adding Fact: " + f + " < " + t);
                relation.add(new AggregateFact(f,t2));
              }
            }
          }
        }
      }
    }
    return new ConjunctiveFactLattice(relation);
  }
	public ConjunctiveFactLattice addRegionMappings(IRNode expr, ILocationMap lm, IBinder b){
		final IJavaType type = b.getJavaType(expr);
		if(type instanceof IJavaDeclaredType){
			final SimpleLocation loc = lm.getLocation(expr);
			return union(getRegionMappings(loc,type,b));
		}
		return this;
		}

	public ConjunctiveFactLattice initialize(IRNode root, ILocationMap lm, IBinder b){
		return union(getInitial(root,lm,b));
	}

	public static ConjunctiveFactLattice getInitial(IRNode root, ILocationMap lm, IBinder b){
		return getInitialMappings(root,lm,b).union(getInitialMethodAliasing(root,lm,b));
	}
  @Deprecated
	public static ConjunctiveFactLattice getInitialMethodAliasing(IRNode root, ILocationMap lm, IBinder b){
		//Currently the only known aliasing information at method entry is that this is not null
		//Ideally we will have annotations referencing aliasing for later.
		final boolean isStatic = JavaNode.getModifier(root, JavaNode.STATIC);
		if(isStatic) return new ConjunctiveFactLattice();
		return (ConjunctiveFactLattice)new ConjunctiveFactLattice()
			.addDoesNotAlias(lm.getLocation(JavaPromise.getReceiverNode(root)),lm.nulLoc());
	}

	@SuppressWarnings("deprecation")
  public static ConjunctiveFactLattice getInitialMappings(IRNode root, ILocationMap lm, IBinder b){
		IRNode receiver = JavaPromise.getReceiverNodeOrNull(root);
		IRNode mclass   = JJNode.tree.getParent(JJNode.tree.getParent(root));
    IJavaType mtype = JavaTypeFactory.convertIRTypeDeclToIJavaType(mclass);
		final boolean isStatic = JavaNode.getModifier(root, JavaNode.STATIC);
		ConjunctiveFactLattice res = isStatic?new ConjunctiveFactLattice():getRegionMappings(lm.getLocation(receiver),mtype,b);
		Operator mdop = JJNode.tree.getOperator(root);
		IRNode params;
		if(mdop instanceof MethodDeclaration){
			params = MethodDeclaration.getParams(root);
		}else if(mdop instanceof ConstructorDeclaration){
			params = ConstructorDeclaration.getParams(root);
		}else{
			return res;
		}
		Iterator e = Parameters.getFormalIterator(params);
		while(e.hasNext()){
			IRNode paramDecl = (IRNode)e.next();
			if(JJNode.tree.getOperator(paramDecl) instanceof ParameterDeclaration){
				IRNode typInt = ParameterDeclaration.getType(paramDecl);
				if(JJNode.tree.getOperator(typInt) instanceof PrimitiveType){
				}else{
					IJavaType paramClass = b.getJavaType(ParameterDeclaration.getType(paramDecl));
					res = res.union(getRegionMappings(lm.getLocation(paramDecl),paramClass, b));
				}
			}	
		}
		return res;
	}

	protected Set<LocationField> topLevel(){
		Set<LocationField> s = new HashSet<LocationField>();
		for(Iterator i = l.iterator();i.hasNext();){
			KeptFact ff1 = (KeptFact) i.next();
			
			if(!(ff1 instanceof AggregateFact)){ continue;	}
			
			AggregateFact f = (AggregateFact)ff1;

			s.add(f.second());
			s.remove(f.first());
		}
		return s;
	}
  /*
   * Does the truth of s2 imply the truth of s1?
   */
  protected static boolean implies(ImmutableHashOrderSet s1, ImmutableHashOrderSet s2){
    for(Iterator i = s1.iterator();i.hasNext();){
      KeptFact ffl = (KeptFact) i.next();
      if(ffl instanceof AggregateFact){
        if(!s2.contains(ffl)) return false;
      }
      if(ffl instanceof AliasFact){
        AliasFact f = (AliasFact)ffl;
        if(isDoesAlias(f)){
          if(!getEquiv(f.first(),s2).contains(f.second())){
            return false;
          }
        }else{
          if(!doesNotAlias(f.first(),s2).contains(f.second())){
            return false;
          }
        }
      }
    }
    return true;
  }
  
}
