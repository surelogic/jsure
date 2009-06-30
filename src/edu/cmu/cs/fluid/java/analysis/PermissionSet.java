/*
 * Created on Oct 25, 2003
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.ChainLattice;
import edu.cmu.cs.fluid.util.PartialMapLattice;
import edu.cmu.cs.fluid.util.UnionLattice;
import edu.cmu.cs.fluid.util.PairLattice;
import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.promise.EffectSpecification;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
/**
 *
 */
@SuppressWarnings("unchecked")
@Deprecated
public class PermissionSet extends PartialMapLattice<Object,Lattice> implements Lattice {

	public static final Logger LOG = SLLogger.getLogger("PermissionAnalysis");

	/*
	 * Establish ChainLattice for permissions:
	 * 	 top=write > read > "smaller read" > useless=bottom
	 */

	protected static final String names[] = {"useless","write","read","smaller"};

	public static final ChainLattice useless = new ChainLattice(4){
		@Override
    public String toString(){return names[this.hashCode()];}
	};
	public static final ChainLattice write = useless.below();
	public static final ChainLattice read = write.below();
	public static final ChainLattice smaller = read.below();

	public static String toString(ChainLattice perm){
		int idx = perm.hashCode();//exploiting indexing of ChainLattice--bad idea
		if(0 <= idx && idx <=3){
			return names[idx];
		}
		return "UNKNOWN";
	}
	
	public final PermissionSet top;


	/**
	 */
	public PermissionSet(){
		super(new PermDropLattice());
		top = (PermissionSet)newLattice(map,true);
	}

	@Override
  public Lattice meet(Lattice other) {
		PermissionSet ps = (PermissionSet)other;
		if(!isTop) LOG.severe("I'm not TOP!");
		if(!ps.isTop) LOG.severe("They're not TOP!");
		return super.meet(other);
	}
	/**
	 * @param range
	 * @param m
	 * @param t
	 * @param b
	 * @param isTop
	 */
	protected PermissionSet(
		Lattice range,
		HashMap<?,Lattice> m,
		PartialMapLattice t,
		PartialMapLattice b,
		boolean isTop) {
		super(range, m, t, b, isTop);
		top = ((t == null)?this:(PermissionSet)t);
	}

	
	/* 
	 * @see edu.cmu.cs.fluid.util.PartialMapLattice#newLattice(java.util.HashMap, boolean)
	 */
	@Override
  protected PartialMapLattice newLattice(HashMap<?,Lattice> newValues, boolean isTop) {
		return new PermissionSet(range, newValues, top, botVal, isTop);
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#top()
	 */
	@Override
  public Lattice top() {
		return top;
	}
		
	public boolean assertRead(SimpleLocation sl,IRNode field){
		LocationField l = new LocationField(sl,field);
		return assertPerm(l,smaller);
	}
	public boolean assertWrite(SimpleLocation sl,IRNode field){
		LocationField l = new LocationField(sl,field);
		return assertPerm(l,write);
	}
	
	public boolean assertSameRead(SimpleLocation sl,IRNode field){
		LocationField l = new LocationField(sl,field);
		return assertPerm(l,read);
	}
	
	public boolean assertPerm(SimpleLocation sl, IRNode field, ChainLattice perm){
		LocationField l = new LocationField(sl,field);
		return assertPerm(l,perm);
	}
	
	protected boolean allParentsInclude(LocationField l, ChainLattice perm){
		SimpleLocation loc = l.l;
		SimpleLocation ls[] = loc.parentsOrNull();
		if(ls == null){
			return false;
		}
		for(int i = 0; i < ls.length; ++i){
			LocationField lf = new LocationField(ls[i],l.fdecl);
			if(!assertPerm(lf,perm)){
				return false;
			}
		}
		return true;
	}
	protected boolean assertPerm(LocationField l, ChainLattice perm){
		if (perm.equals(useless)) return true;
		Lattice lt = getPerm(l);
		boolean b = lt.includes(perm) && !lt.equals(useless);
		return b;
	}
	
	public PermissionSet updateWrite(SimpleLocation l, IRNode fdecl){
		return updatePerm(new LocationField(l,fdecl),write);
	}
	public PermissionSet updateRead(SimpleLocation l, IRNode fdecl){
		return updatePerm(new LocationField(l,fdecl),read);
	}
	public PermissionSet updateSmaller(SimpleLocation l, IRNode fdecl){
		return updatePerm(new LocationField(l,fdecl),smaller);
	}
	public PermissionSet updateUseless(SimpleLocation l, IRNode fdecl){
		return updatePerm(new LocationField(l,fdecl),useless);
	}
	
	public PermissionSet updateWrite(LocationField l){
		return updatePerm(l,write);
	}
	public PermissionSet updateRead(LocationField l){
		return updatePerm(l,read);
	}
	public PermissionSet updateSmaller(LocationField l){
		return updatePerm(l,smaller);
	}
	public PermissionSet updateUseless(LocationField l){
    	return updatePerm(l,useless);
	}

	public PermissionSet updatePerm(LocationField lf, ChainLattice perm){
	    final PermDropLattice pdl = (PermDropLattice)get(lf);
	    final Lattice dps = pdl.getRight();
	    return (PermissionSet)super.update(lf,pdl.newLattice(perm,dps));
	}

	public PermissionSet addDrop(LocationField lf, PromiseDrop pd){
	    final PermDropLattice pdl = (PermDropLattice)get(lf);
	    final UnionLattice dps = (UnionLattice)pdl.getRight();
	    return (PermissionSet)super.update(lf,
	            pdl.newLattice(pdl.getLeft(),(UnionLattice)dps.addElement(pd)));
	}
	public PermissionSet addDrops(LocationField lf, Collection pds){
	    final PermDropLattice pdl = (PermDropLattice)get(lf);
	    final UnionLattice dps = (UnionLattice)pdl.getRight();
	    return (PermissionSet)super.update(lf,
	            pdl.newLattice(pdl.getLeft(),(UnionLattice)dps.addElements(pds.iterator())));
	}
	
	public Collection getDrops(LocationField lf){
	    return (UnionLattice)((PermDropLattice)get(lf)).getRight();
	}
	
	public PermissionSet killAll(LocationField lf){
	    return (PermissionSet)super.update(lf,range().top());
	}
	
	public PermissionSet transferPerms(LocationField from, LocationField to){
	    Lattice old = get(from);
	    return (PermissionSet)super.update(from,range().top()).update(to,old);
	}
	
	
	Operator getOperator(IRNode i){
		return JJNode.tree.getOperator(i);
	}
	
	public ChainLattice getPerm(LocationField lf){
	  Lattice l1 = get(lf);
	  if(l1 == null) return useless;
	  if(!(l1 instanceof PermDropLattice)){
	    LOG.severe("Bad Pair " + l1 + " for " + lf);
	    return useless;
	  }
	  Lattice l = ((PermDropLattice)l1).getLeft();
	  if(l instanceof ChainLattice){
	    return (ChainLattice)l;
	  }
	  LOG.severe("Bad Permission " + l + " for " + lf);
	  return useless;
	}
	
	public PermissionSet annotatedPermissions(IRNode root,ILocationMap lm, IBinder b){
		return (PermissionSet)newLattice(getAnnoPerms(root,lm,b),true);//false);
	}
		
	protected HashMap<LocationField,Lattice> getAnnoPerms(IRNode root,ILocationMap lm, IBinder b){
		final HashMap<LocationField,Lattice> h = new HashMap<LocationField,Lattice>();
		final Iterator effects = EffectsAnnotation.methodEffects(root);
		final boolean isStatic = JavaNode.getModifier(root, JavaNode.STATIC);
		final PromiseDrop ed = EffectsAnnotation.getMethodEffectsDrop(root);
//System.out.println("Getting effects for " + DebugUnparser.toString(root));
    if(effects != null){
			while(effects.hasNext()){
				final IRNode eff = (IRNode)effects.next();
				IRNode context = EffectSpecification.getContext(eff);
				final IRNode reg = EffectSpecification.getRegion(eff);
				if(context == null || reg == null){
					LOG.warning("Bad Annotation: Null effect " + DebugUnparser.toString(eff));
					continue;
				}
				if(ThisExpression.prototype.includes(getOperator(context))){
					if(isStatic){
						LOG.warning("Bad Annotation: " + DebugUnparser.toString(eff) + " on static ");
						continue;
					}
					context = JavaPromise.getReceiverNode(root);
				}else if(!TypeExpression.prototype.includes(getOperator(context))){
					context = b.getBinding(context);
				}
        
				SimpleLocation ctxt = lm.getLocation(context);
				final IRNode regdecl = b.getBinding(reg);
				if(EffectSpecification.getIsWrite(eff)){
					h.put(new LocationField(ctxt,regdecl),write(ed));
				}else{
					h.put(new LocationField(ctxt,regdecl),read(ed));
				}
			}
		}else{

      h.put(new LocationField(lm.nulLoc(),LocationField.sharedRegion(b)),write((UnionLattice)range().getRight()));
		}
		return h;
	}
	
	public PermissionSet initialize(IRNode root, ILocationMap lm, IBinder b){
		final HashMap<LocationField,Lattice> h = getAnnoPerms(root,lm,b);
		final boolean isStatic = JavaNode.getModifier(root, JavaNode.STATIC);
		final IRNode ths = JavaPromise.getReceiverNodeOrNull(root);
		final LocationField tlf = new LocationField(lm.nulLoc(),ths);
		if (!isStatic) h.put(tlf,read);
		
		Operator mdop = getOperator(root);
		IRNode params;
		if(mdop instanceof MethodDeclaration){
			params = MethodDeclaration.getParams(root);
		}else if(mdop instanceof ConstructorDeclaration){
			params = ConstructorDeclaration.getParams(root);
			h.put(LocationField.allField(lm.getLocation(tlf),b),write((UnionLattice)range().getRight()));
		}else{
			h.put(LocationField.allField(lm.getLocation(tlf),b),write((UnionLattice)range().getRight()));
			return (PermissionSet) newLattice(h,true);//false);
		}
		Iterator e = Parameters.getFormalIterator(params);
		while(e.hasNext()){
			IRNode paramDecl = (IRNode)e.next();
			if(getOperator(paramDecl) instanceof ParameterDeclaration){
				h.put(new LocationField(lm.nulLoc(),paramDecl), write((UnionLattice)range().getRight()));
			}
		}
		return (PermissionSet) newLattice(h,true);//false);
	}

	private PermDropLattice range(){ return (PermDropLattice)range; }
	
	public PermDropLattice useless(UnionLattice u){
	    return (PermDropLattice)range().newLattice(useless,u);
	}
	public PermDropLattice write(UnionLattice u){
	    return (PermDropLattice)range().newLattice(write,u);
	}
	public PermDropLattice read(UnionLattice u){
	    return (PermDropLattice)range().newLattice(read,u);
	}
	public PermDropLattice smaller(UnionLattice u){
	    return (PermDropLattice)range().newLattice(smaller,u);
	}
	private UnionLattice add(Object o){
	    return (UnionLattice)((UnionLattice)range().getRight()).addElement(o);
	}
	public PermDropLattice useless(PromiseDrop pd){
	    return useless(add(pd));
	}
	public PermDropLattice write(PromiseDrop pd){
	    return write(add(pd));
	}
	public PermDropLattice read(PromiseDrop pd){
	    return read(add(pd));
	}
	public PermDropLattice smaller(PromiseDrop pd){
	    return smaller(add(pd));
	}
	
	
	/*
	public PermissionSet grantFullPermission(IRNode expr, ILocationMap lm, IBinder b){
		PermissionSet ret = this;
		final SimpleLocation loc = lm.getLocation(expr);
		final IJavaType type = b.getJavaType(expr);
		if(!(type instanceof IJavaDeclaredType)) return ret;
		final IRNode cdecl = ((IJavaDeclaredType)type).getDeclaration();
		final ConjunctiveFactLattice maps = ConjunctiveFactLattice.getRegionMappings(loc,cdecl,b);
		final Iterator regions = ((RegionAnnotation)RegionAnnotation.getInstance()).getAllRegionsInClass(cdecl).iterator();
		while(regions.hasNext()){
			final IRNode reg = (IRNode)regions.next();
			if(maps.allAbove(new LocationField(loc,reg)).isEmpty()){
				ret = ret.updateWrite(loc,reg);
			}
		}
		return ret;
	}
	*/
}
@Deprecated
class PermDropLattice extends PairLattice{
  /**
    * @param l
    * @param r
    * @param t
    * @param b
    */
   protected PermDropLattice(Lattice l, Lattice r, PairLattice t,
      PairLattice b) {
      super(l, r, t, b);
   }
   
   public PermDropLattice() {
     super(PermissionSet.useless,new UnionLattice());
   }
   
   @Override
  public PairLattice newLattice(Lattice l, Lattice r) {
   		return new PermDropLattice(l,r,(PairLattice)top(),(PairLattice)bottom());
   	}
   @Override
  public String toString(){
       return PermissionSet.toString((ChainLattice)getLeft());
   }
}
