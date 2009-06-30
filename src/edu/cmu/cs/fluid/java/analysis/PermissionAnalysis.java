
/*
 * Created on Dec 16, 2003
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.EffectsAnnotation;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;

import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.RecordLattice;
import edu.cmu.cs.fluid.util.LesserStackLattice;
import edu.cmu.cs.fluid.util.ChainLattice;

import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
import edu.cmu.cs.fluid.java.analysis.AliasFactLattice.LocationClaim;
import edu.cmu.cs.fluid.java.analysis.AliasFactLattice.AggregatingClaim;

import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.EffectSpecification;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.java.bind.*;

/**
 *
 */
@Deprecated
class FullPermissionLattice extends RecordLattice {

	static final int LOC = 0;
	static final int PERM = 1;
	static final int FACT = 2;
	static final int STACK = 3;

	static final int NUMVALS = 4;

	static final Logger LOG = SLLogger.getLogger("PermissionAnalysis");
	
	public final LocationField shared;
	public final LocationField instanceRegion;
  
	protected  AssuranceLogger log;

	protected PermissionDropMediator pdm;

  protected final IBinder binder;
  
	protected final TrackingIntraproceduralAnalysis analysis;
	/**
	 */
	public FullPermissionLattice(IRNode md, IBinder binder,LocationGenerator gen, 
	    AssuranceLogger l, PermissionDropMediator pm,
		TrackingIntraproceduralAnalysis a) {
		super(new Lattice[]{
			new LocationMap(md,binder,gen),
			new PermissionSet(),
			new ConjunctiveFactLattice(),	
			new LesserStackLattice(gen.top())
		});
		log = l;
		pdm = pm;
    this.binder = binder;
		assert(a != null);
		analysis = gen.ourAnalysis;
    shared = new LocationField(gen.getNull(),LocationField.sharedRegion(binder));
		//LocationField.shared = shared = new SharedField(getLoc().nulLoc());
		((FullPermissionLattice)bottom()).log = l;
     IRNode object = binder.getTypeEnvironment()
                            .findNamedType("java.lang.Object");
     instanceRegion = new LocationField(
         gen.getNull(),
         binder.findRegionInType(object,PromiseConstants.REGION_INSTANCE_NAME));    

	}


	/**
	 * @param v
	 * @param t
	 * @param b
	 */
	protected FullPermissionLattice(Lattice[] v, RecordLattice t, RecordLattice b, 
			AssuranceLogger l, PermissionDropMediator pm,
			TrackingIntraproceduralAnalysis a, LocationField sh,LocationField all,IBinder binder) {
		super(v, t, b);
		pdm = pm;
		log = l;
    this.binder = binder;
		analysis = getLoc().gen.ourAnalysis;//(a==null)?((FullPermissionLattice)t).analysis:a;
		assert(analysis != null);
		shared = sh;
    instanceRegion = all;
	}

	/* 
	 * @see edu.cmu.cs.fluid.util.RecordLattice#newLattice(edu.cmu.cs.fluid.util.Lattice[])
	 */
	@Override
  protected RecordLattice newLattice(Lattice[] newValues) {
		return new FullPermissionLattice(newValues,top,bottom,log,pdm,analysis,
        shared,instanceRegion,binder);
	}

	public LocationMap getLoc(){
		return (LocationMap)getValue(LOC);
	}
	public ConjunctiveFactLattice getFacts(){
		return (ConjunctiveFactLattice) getValue(FACT);
	}
	public PermissionSet getPerms(){
		return (PermissionSet)getValue(PERM);
	}
	protected LesserStackLattice getStack(){
		return (LesserStackLattice)getValue(STACK);
	}
	
	protected FullPermissionLattice newLattice(LocationMap lm, PermissionSet ps, ConjunctiveFactLattice cfl, LesserStackLattice s){
		final FullPermissionLattice f =  (FullPermissionLattice)newLattice(new Lattice[]{lm,ps,cfl,s});
//		System.out.println(f);
		return f;
	}
	
	public FullPermissionLattice newLattice(LocationMap lm){
		return newLattice(lm,getPerms(),getFacts(),getStack());
	}

	public FullPermissionLattice newLattice(ILocationMap lm){
		return newLattice((LocationMap)lm,getPerms(),getFacts(),getStack());
	}
	
	public FullPermissionLattice newLattice(ConjunctiveFactLattice cfl){
		return newLattice(getLoc(),getPerms(),cfl,getStack());
	}

	public FullPermissionLattice newLattice(AliasFactLattice cfl){
		return newLattice(getLoc(),getPerms(),(ConjunctiveFactLattice)cfl,getStack());
	}
	
	public FullPermissionLattice newLattice (PermissionSet ps){
		return newLattice(getLoc(),ps,getFacts(),getStack());
	}
	
	public FullPermissionLattice push(SimpleLocation val){
		return newLattice(getLoc(),getPerms(),getFacts(),getStack().push(val));
	}
	
	public FullPermissionLattice push(){
		LocationMap lg = getLoc();
		return newLattice(lg,getPerms(),getFacts(),getStack().push(lg.gen.bottom()));
	}
	
	public FullPermissionLattice pop(){
		LesserStackLattice stack = getStack();
		if(stack.isEmpty()){
			log.reportNegativeAssurance("Stack Underflow",analysis.getLatestSink());
		}
		return newLattice(getLoc(),getPerms(),getFacts(),stack.pop());
	}

	public FullPermissionLattice popSecond(){
		SimpleLocation l = topElement();
		return pop().pop().push(l);
	}
	
	public SimpleLocation secondElement(){
		return pop().topElement();
	}
	
	public SimpleLocation topElement(){
		return (SimpleLocation)getStack().peek();
	}
	
  public void test(){
    
  }
  
	public FullPermissionLattice popAllPending(){
		final LesserStackLattice s = getStack();
		if(s.isEmpty()){
			return this;
		}
		return pop().popAllPending();
	}
	
	protected Iterator values(LesserStackLattice s){
	  return s.values();
	}
	
	/* 
	 * @see edu.cmu.cs.fluid.util.Lattice#meet(edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override
  public Lattice meet(Lattice otherL) {
		FullPermissionLattice other = (FullPermissionLattice) otherL;
		if (this.equals(bottom) || this.equals(other) || other.equals(top)) return this;
		if (this.equals(top)|| other.equals(bottom)) return other;
		if (includes(other)) return other;
		if (other.includes(this)) return this;

		//System.err.println("MEETING:\n"+this + "\nAND\n" + other + "\n");

        FullPermissionLattice self = this;
        final PermissionLocationAsserter pla_l = new PermissionLocationAsserter(self);
        final PermissionLocationAsserter pla_r = new PermissionLocationAsserter(other);
        /*  *
        getLoc().postLocationAsserters(pla_l,pla_r);
        /*  */
		Lattice[] newValues = new Lattice[NUMVALS];
		
		LocationMap leftMap = self.getLoc();
		LocationMap rightMap= other.getLoc();
		LocationMap lm = (LocationMap)leftMap.meet(rightMap);
		newValues[LOC] = lm;

		LesserStackLattice lstack = self.getStack();
		LesserStackLattice rstack = other.getStack();
		LesserStackLattice stack = (LesserStackLattice)lstack.meet(rstack);
		newValues[STACK] = stack; 

		LesserStackLattice b = (LesserStackLattice)stack.bottom();
		LesserStackLattice t = (LesserStackLattice)stack.top();

		/*		if(stack.equals(b)){
			System.out.println("BAD");
		}*/
		if(lstack.count() != rstack.count() /*&&
				!(rstack.equals(b) || lstack.equals(b))&&
				!(rstack.equals(t) || lstack.equals(t))*/){
			IRNode sink = analysis.getUserNode();//getLatestSink();
      
			if (JJNode.tree.isNode(sink)) {
				LOG.severe("STACKSIZE MISMATCH ERROR!\nJoining " + lstack + " and " + rstack
						+"\nAt " + DebugUnparser.toString(sink));
			} else {
				LOG.severe("STACKSIZE MISMATCH ERROR!\nJoining " + lstack + " and " + rstack
						+"\nAt <unknown>");
			}
			log.reportNegativeAssurance("Stacksize Mismatch Error",sink);
			return bottom();
		}

        self = pla_l.result();
        other = pla_r.result();
    
		ConjunctiveFactLattice left = self.getFacts();
		ConjunctiveFactLattice right = other.getFacts();

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
				
		for(java.util.Iterator i = values(stack),l = values(lstack),r=values(rstack);i.hasNext();){
			SimpleLocation end = (SimpleLocation)i.next();
			SimpleLocation lefty = (SimpleLocation)l.next();
			SimpleLocation righty = (SimpleLocation)r.next();
			if(!(lefty.equals(end))){
				lsub = lsub.addDoesAlias(lefty,end);
			}
			if(!righty.equals(end)){
				rsub = rsub.addDoesAlias(righty,end);
			}
		}
		newValues[FACT] = left.merge(lsub,right,rsub);

        
    
		newValues[PERM] = self.getPerms().meet(other.getPerms());
		
		Lattice nl = newLattice(newValues);
		
        //System.err.println("\nto get\n" + nl);
         
    return nl;
  }

	public LocationMap initializeLocations(IRNode root, IBinder b){
		LocationMap start = getLoc();

		Operator mdop = JJNode.tree.getOperator(root);
		IRNode params;
		if(mdop instanceof MethodDeclaration){
			params = MethodDeclaration.getParams(root);
		}else if(mdop instanceof ConstructorDeclaration){
			params = ConstructorDeclaration.getParams(root);
		}else{
			return start;
		}
		Iterator e = Parameters.getFormalIterator(params);
		while(e.hasNext()){
			IRNode paramDecl = (IRNode)e.next();
			if(JJNode.tree.getOperator(paramDecl) instanceof ParameterDeclaration)
				start = (LocationMap)start.replaceLocation(paramDecl,start.getLocation(paramDecl));	
		}
		final Iterator effects = EffectsAnnotation.methodEffects(root);
		final boolean isStatic = JavaNode.getModifier(root, JavaNode.STATIC);
		if(effects != null){
			while(effects.hasNext()){
				final IRNode eff = (IRNode)effects.next();
				IRNode context = EffectSpecification.getContext(eff);
				final IRNode reg = EffectSpecification.getRegion(eff);
				if(context == null || reg == null){
					continue;
				}
				if(ThisExpression.prototype.includes(JJNode.tree.getOperator(context))){
					if(isStatic) continue;
					context = JavaPromise.getReceiverNode(root);
				}else if(!TypeExpression.prototype.includes(JJNode.tree.getOperator(context))){
					context = b.getBinding(context);
				}
				SimpleLocation ctxt = start.getLocation(context);

				final IRNode regdecl = b.getBinding(reg);
				start = (LocationMap)start.replaceLocation(ctxt,regdecl,start.getLocation(eff));
			}
		}
		return start;
	}

	LocationField getAllField(SimpleLocation l,IBinder b){
    return LocationField.allField(l,b);
	}
	/*
	FullPermissionLattice makeUnique(LocationField lf){
	  return newLattice(updateUnique(lf));
	}
	*/
	PermissionSet updateUnique(LocationField lf){
	  return updateUnique(lf,getPerms());
	}
	
	private PermissionSet updateUnique(LocationField lf, PermissionSet ps){
	  LocationMap lm = getLoc();
	  LocationField llf = getAllField(lm.getLocation(lf),binder);
	  return ps.updatePerm(llf,PermissionSet.write);
	}

	FullPermissionLattice renameLocation(IRNode expr){
		return newLattice(getLoc().renameLocation(expr));
	}
	
	FullPermissionLattice renameLocation(LocationField lf, IRNode locale){
		FullPermissionLattice r = newLattice(getLoc().renameLocation(lf));
		return r;
	}
	
	
	private PermissionSet updateShared(LocationField lf, PermissionSet ps){
	  return ps;//.updateUseless(lf);
	}
	private ConjunctiveFactLattice updateShared(LocationField lf, ConjunctiveFactLattice cfl,LocationMap lm){
	  return cfl.addIsMappedInto(LocationField.allField(lm.getLocation(lf),binder),shared);
	}
	
	FullPermissionLattice makeShared(LocationField lf){
	  return newLattice(updateShared(lf,newLattice(updateShared(lf,getFacts(),getLoc())).getPerms()));
	}
	
    FullPermissionLattice instantiateUnique(SimpleLocation l,IRNode fd){
      LocationField lf = new LocationField(l,fd);
      return newLattice(getLoc().assignPseudoLocation(lf)).makeUnique(lf);
    }
    FullPermissionLattice instantiateShared(SimpleLocation l,IRNode fd){
      LocationField lf = new LocationField(l,fd);
      return newLattice(getLoc().assignPseudoLocation(lf)).makeShared(lf);
    }

    FullPermissionLattice replaceLocation(SimpleLocation l, IRNode fd, SimpleLocation v){
      return newLattice(getLoc().replaceLocation(l,fd,v));
    }
    
    FullPermissionLattice removePerm(LocationField lf){
      return newLattice(getPerms().updateUseless(lf));
    }
    FullPermissionLattice removeRead(LocationField lf){
      return newLattice(getPerms().updateSmaller(lf));
    }
  
    public FullPermissionLattice getInitialLattice(IRNode root, IBinder b){
	
      LocationMap lm = initializeLocations(root,b);
      final boolean isStatic = JavaNode.getModifier(root, JavaNode.STATIC);
      final IRNode ths = JavaPromise.getReceiverNodeOrNull(root);
      if(!isStatic){
		lm = (LocationMap)lm.replaceLocation(lm.nulLoc(),ths,lm.getLocation(ths));
      }
      PermissionSet ps = getPerms().initialize(root,lm,b);
      ConjunctiveFactLattice cfl = getFacts();
      for(Iterator i = ps.keys();i.hasNext();){
		LocationField lf = (LocationField)i.next();
		IRNode fdecl = lf.fdecl;
		if(fdecl == null) continue;
		if(UniquenessAnnotation.isUnique(fdecl)){
      /*  *
          lm = (LocationMap)lm.replaceLocation(lf.l,lf.fdecl,lf.l.getUnique());
      /*  */    
          SimpleLocation l = lm.getLocation(lf);
		  ps = ps.updatePerm(getAllField(l,binder), PermissionSet.write);
      /* */
		  cfl = cfl.addRegionMappings(l,b.getJavaType(fdecl),b);
		}else if (UniquenessAnnotation.isBorrowed(fdecl)){
		  PermissionDropMediator.getBorrowedOKDrop().addCheckedPromise(UniquenessAnnotation.getBorrowedDrop(fdecl));
    }else if (UniquenessAnnotation.isImmutable(fdecl)){
		  //do nothing (yet for immutable)
		}else {//if(isAnnotatedShared(fdecl)){//isShared!
      /* *
          lm = (LocationMap)lm.replaceLocation(lf.l,lf.fdecl,lf.l.getShared());
      /* */
          ps = updateShared(lf,ps);
		  cfl = updateShared(lf,cfl,lm);
      /* */
		}
      }
      //ps = ps.updateWrite(shared);
      return newLattice(lm,ps,cfl.initialize(root,lm,b),getStack());
    }

	@Override
  public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Map   = " + getLoc() + "\n");
	/*	*/
    sb.append("Facts = " + getFacts() + "\n");
		sb.append("Perms = " + getPerms() + "\n");
		sb.append("stack = " + getStack() + "\n");
	/*	*/
      return sb.toString();
	}

  protected boolean makeClaim(SimpleLocation loc, LocationClaim lc, 
    String goodRep, String badRep, IRNode locale){
    boolean b = getFacts().makeClaim(loc,lc);
    if(b){
      log.reportPositiveAssurance(goodRep,locale);
    }else{
      log.reportNegativeAssurance(badRep,locale);
    }
    return b;
  }
  protected boolean makeAssertionClaim(SimpleLocation loc, 
      AssertionClaim ac, IRNode locale, ResultDrop s){
    LocationMap lm = getLoc();
    IRNode expr = lm.getSource(loc);
    boolean b = makeClaim(loc,ac,ac.positiveMsg(expr),ac.negativeMsg(expr),locale);
    ac.getResultingDrop(locale,b,s);
    if(!b){
      pdm.inconsistentizeDrop(s);
      //pdm.inconsistentizeDrop(methodResults);
    }
    return b;
  }

  protected FullPermissionLattice assertShared(SimpleLocation target, IRNode locale, 
      IRNode checked, ResultDrop s){
    if(getFacts().makeClaim(target,new MustEqualClaim(getLoc().nulLoc()))){
      return this;
    }
    final PermissionSet ps = getPerms();
    final LocationField all = getAllField(target,binder);
    final boolean b = target.isShared() || (PermissionSet.useless.equals(ps.get(all)) ||
        PermissionSet.useless.equals(ps.get(shared)))&&
      getFacts().allAbove(all).contains(shared);
    if(b){
      log.reportPositiveAssurance(DebugUnparser.toString(checked) + " is shared.",locale);
    }else{
      log.reportNegativeAssurance(DebugUnparser.toString(checked) + " is not shared.",locale);
    }
    pdm.createSharedAssertionDrop(b,locale,checked,s,ps.getDrops(all));
    return this;
  }

  protected FullPermissionLattice checkUnique(SimpleLocation loc,  
      ChainLattice perm, IRNode locale, IRNode checked, ResultDrop s){
    final UniquenessClaim uC = new CheckUniquenessClaim(this,perm,checked);
    return checkUnique(loc,perm,locale,checked,uC,s);
  }
  protected FullPermissionLattice checkUnique(SimpleLocation loc, 
      ChainLattice perm, IRNode locale, 
      IRNode checked, UniquenessClaim uC, ResultDrop s){
    if(perm.equals(PermissionSet.useless)) {
      return this;
    }
    if(getFacts().makeClaim(loc,new MustEqualClaim(getLoc().nulLoc()))){
      return  this;
    }
    makeAssertionClaim(loc,uC,locale,s);
    return uC.getResultingLattice();
  }
  
  protected FullPermissionLattice checkUnique(SimpleLocation loc,  
      IRNode locale, IRNode checked, ResultDrop s){
    return checkUnique(loc,PermissionSet.write,locale,checked,s);
  }
  protected FullPermissionLattice assertUniqueC(SimpleLocation loc,  
      IRNode locale, IRNode checked, ResultDrop s){
    return checkUnique(loc,PermissionSet.write,locale,checked,
        new AssertUniquenessClaim(this,PermissionSet.write,checked),s);
  }
  
  protected FullPermissionLattice assertUnique(SimpleLocation loc, 
      IRNode decl, IRNode locale,
      ChainLattice perm, IRNode asserted, ResultDrop s){
    final LocationField lf = new LocationField(loc,decl);
    SimpleLocation target = getLoc().getLocation(lf);
    return assertUnique(target,lf,locale,perm, asserted,s);
  }
  protected FullPermissionLattice assertUnique(SimpleLocation target, 
      LocationField lf, IRNode locale, ChainLattice perm, 
      IRNode asserted, ResultDrop s){ 
    AssertUniquenessClaim uC = new AssertUniquenessClaim(this,perm,asserted);
    FullPermissionLattice l2 = checkUnique(target,perm,locale,asserted,uC,s)
                                .renameLocation(lf,locale).makeUnique(lf);
    return l2.newLattice(l2.getPerms().addDrops(LocationField.allField(l2.getLoc().getLocation(lf),binder),uC.sourceDrops()));
  }
  
  protected FullPermissionLattice assertUnique(SimpleLocation loc, 
      IRNode decl, IRNode locale, 
      IRNode asserted, ResultDrop s){
    return assertUnique(loc,decl,locale,
        /*getPerms().getPerm(new LocationField(loc,decl))*/PermissionSet.write,asserted,s);
  }

  protected FullPermissionLattice assertUnique(SimpleLocation loc, 
      IRNode decl, IRNode locale, ResultDrop s){
    return assertUnique(loc,decl,locale,decl,s);
  }
  
  protected FullPermissionLattice assertShared(SimpleLocation loc, IRNode decl, IRNode locale,
      IRNode checked, ResultDrop s){
    final LocationField lf = new LocationField(loc,decl);
    SimpleLocation target = getLoc().getLocation(lf);
    return assertShared(target,locale,checked,s);
  }

  protected boolean makeAggregatingClaim(SimpleLocation loc, 
    PermissionAssertionClaim pc, IRNode locale, ResultDrop s){
  LocationMap lm = getLoc();
  IRNode expr = lm.getSource(loc);
  boolean b = getFacts().makeAggregatingClaim(pc,
      new LocationField(loc,pc.getDecl()));
  if(b){
    log.reportPositiveAssurance(pc.positiveMsg(expr),locale);
  }else{
    log.reportNegativeAssurance(pc.negativeMsg(expr),locale);
  }
  pc.getResultingDrop(locale,b,s);
  if(!b){
    pdm.inconsistentizeDrop(s);
    //pdm.inconsistentizeDrop(methodResults);
  }
  return b;
}


  protected FullPermissionLattice assertRead(SimpleLocation loc, 
      IRNode decl, IRNode locale, ResultDrop s){
    makeAggregatingClaim(loc,new ReadAssertionClaim(getPerms(),decl),locale,s);
    return this;
  }
  protected FullPermissionLattice assertWrite(SimpleLocation loc, 
      IRNode decl, IRNode locale, ResultDrop s){
    makeAggregatingClaim(loc,new WriteAssertionClaim(getPerms(),decl),locale,s);
    return this;
  }

  protected boolean assertAnnotation(SimpleLocation loc, IRNode decl, ChainLattice perm,
    IRNode locale, ResultDrop s){
    return makeAggregatingClaim(loc,
      new AnnotatedPermissionClaim(getPerms(),decl,perm),locale,s);
  }

  
  //to make a field unique, we add its permission for the .All field of the object it points to.
  protected FullPermissionLattice makeUnique(LocationField lf){
    SimpleLocation pt = getLoc().getLocation(lf);
    LocationField llf = getAllField(pt,binder);
    PermissionSet ps = getPerms();
    IRNode fdecl = lf.fdecl;//hhh
    Set pds = new HashSet();
    if(UniquenessAnnotation.isUnique(fdecl))
    pds.add(UniquenessAnnotation.getUniqueDrop(fdecl));
    if(UniquenessAnnotation.isUnique(fdecl))
    pds.add(UniquenessAnnotation.getUniqueDrop(fdecl));
    ConjunctiveFactLattice cfl = getFacts().addRegionMappings(pt,binder.getJavaType(fdecl),binder);
    return newLattice(ps.updatePerm(llf,/**/PermissionSet.write/*/ps.getPerm(lf)/**/).addDrops(llf,pds)).newLattice(cfl);
  }

  
  class ReadAssertionClaim extends PermissionAssertionClaim{

    public ReadAssertionClaim(PermissionSet l, IRNode fdecl) {
      super(l,fdecl);
    } 
    public ReadAssertionClaim(PermissionSet l, IRNode fdecl, Set sup) {
      super(l,fdecl,sup);
    } 
  
    public String negativeMsg(IRNode expr) {
      return "The expression \"" + DebugUnparser.toString(expr) + 
        "\" lacks read permission for " + DebugUnparser.toString(fdecl);
    }
  
    public String positiveMsg(IRNode expr) {
      return "The expression \"" + DebugUnparser.toString(expr) + 
        "\" posesses the required read permission for " + DebugUnparser.toString(fdecl);
    }
  
    public boolean makeClaim(SimpleLocation l) {
      if(perms.assertRead(l,fdecl)){
        supported.addAll(perms.getDrops(new LocationField(l,fdecl)));
        return true;
      }
      return false;
    }
  
    @Override
    protected PermissionAssertionClaim newClaim(PermissionSet l, IRNode fd) {
      return new ReadAssertionClaim(l,fd,supported);
    }
  
    public ResultDrop getResultingDrop(IRNode expr, boolean passed,
        ResultDrop s) {
      return pdm.createReadDrop(passed,expr,fdecl,s,supported);
    }
  }
  class WriteAssertionClaim extends PermissionAssertionClaim{
    public WriteAssertionClaim(PermissionSet l, IRNode fd) {
      super(l, fd);
    }
    public WriteAssertionClaim(PermissionSet l, IRNode fd, Set sup) {
      super(l, fd, sup);
    }
  
    public String negativeMsg(IRNode expr) {
      return "The expression \"" + DebugUnparser.toString(expr) + 
        "\" lacks write permission for " + DebugUnparser.toString(fdecl);
    }
  
    public String positiveMsg(IRNode expr) {
      return "The expression \"" + DebugUnparser.toString(expr) + 
        "\" posesses the required write permission for " + DebugUnparser.toString(fdecl);
    }
    public boolean makeClaim(SimpleLocation l) {
      if(perms.assertWrite(l,fdecl)){
        supported.addAll(perms.getDrops(new LocationField(l,fdecl)));
        return true;
      }
      return false;
    }
  
    @Override
    protected PermissionAssertionClaim newClaim(PermissionSet l, IRNode fd) {
      return new WriteAssertionClaim(l,fd,supported);
    }
  
  public ResultDrop getResultingDrop(IRNode expr, boolean passed, ResultDrop s) {
    return pdm.createWriteDrop(passed,expr,fdecl,s,supported);
  }
  }
  class AnnotatedPermissionClaim extends PermissionAssertionClaim{
    
    protected final ChainLattice perm;
    
    public AnnotatedPermissionClaim(PermissionSet l, IRNode fdecl, ChainLattice perm){
      super(l,fdecl);
      this.perm = perm;
    }
    public AnnotatedPermissionClaim(PermissionSet l, IRNode fdecl, Set sup, ChainLattice perm){
      super(l,fdecl,sup);
      this.perm = perm;
    }
    
    public String negativeMsg(IRNode expr) {
      return "The expression " + DebugUnparser.toString(fdecl) + " does not have the annotated "
        + PermissionSet.toString(perm) + " permission for the field " + DebugUnparser.toString(fdecl);
    }
  
    public String positiveMsg(IRNode expr) {
      return "The expression " + DebugUnparser.toString(fdecl) + " meets the annotated "
        + PermissionSet.toString(perm) + " permission for the field " + DebugUnparser.toString(fdecl);
    }
  
    public boolean makeClaim(SimpleLocation l) {
      if(perms.assertPerm(l,fdecl,perm)){
        supported.addAll(perms.getDrops(new LocationField(l,fdecl)));
        return true;
      }
      return false;
    }
  
    @Override
    protected PermissionAssertionClaim newClaim(PermissionSet l, IRNode fd) {
      return new AnnotatedPermissionClaim(l,fd,supported,perm);
    }
  
  public ResultDrop getResultingDrop(IRNode expr, boolean passed, ResultDrop s) {
    return pdm.createAnnoDrop(passed,expr,fdecl,s,supported,perm);
  }
  }
  class SamePermissionClaim extends PermissionAssertionClaim{
    
    protected final ChainLattice perm;
    
    public SamePermissionClaim(PermissionSet l, IRNode fdecl, ChainLattice perm){
      super(l,fdecl);
      this.perm = perm;
    }
    public SamePermissionClaim(PermissionSet l, IRNode fdecl, Set sup, ChainLattice perm){
      super(l,fdecl,sup);
      this.perm = perm;
    }
    
    public String negativeMsg(IRNode expr) {
      return "The expression " + DebugUnparser.toString(fdecl) + " has lost its "
        + PermissionSet.toString(perm) + " permission for the field " + DebugUnparser.toString(fdecl);
    }
  
    public String positiveMsg(IRNode expr) {
      return "The expression " + DebugUnparser.toString(fdecl) + " still has "
        + PermissionSet.toString(perm) + " permission for the field " + DebugUnparser.toString(fdecl);
    }
  
    public boolean makeClaim(SimpleLocation l) {
      return perms.assertPerm(l,fdecl,perm);
    }
  
    @Override
    protected PermissionAssertionClaim newClaim(PermissionSet l, IRNode fd) {
      return new SamePermissionClaim(l,fd,supported,perm);
    }
  public ResultDrop getResultingDrop(IRNode expr, boolean passed, ResultDrop s) {
    return pdm.createAnnoDrop(passed,expr,fdecl,s,supported,perm);
  }
  
  }
  abstract class UniquenessClaim implements AssertionClaim{
    
    protected final ChainLattice perm;
    protected FullPermissionLattice ret;
    protected final IRNode asserted;
    protected final PermissionSet perms;
    protected final Set written;
    protected final Set drops;
    
    public UniquenessClaim(FullPermissionLattice l, LocationField lf, ChainLattice perm){
      this(l,perm,lf.fdecl);
    }
    
    public UniquenessClaim(FullPermissionLattice l, IRNode asserted){
      this(l,PermissionSet.write,asserted);
    }
    
    public UniquenessClaim(FullPermissionLattice l, ChainLattice perm, IRNode asserted){
      super();
      perms = l.getPerms();
      this.perm = perm;
      this.ret = l;
      this.asserted = asserted;
      this.written = new HashSet();
      drops = new HashSet();
    }
    
    public String negativeMsg(IRNode expr) {
      return DebugUnparser.toString(asserted) + " is not unique.";
  //      + PermissionSet.toString(perm) + " permission for the field " + DebugUnparser.toString(fdecl);
    }
  
    public String positiveMsg(IRNode expr) {
      return DebugUnparser.toString(asserted) + " is unique.";
    }
  
  
    FullPermissionLattice getResultingLattice(){ return ret; }
    protected Set sourceDrops() {
      return drops;
    }
  
    Iterator uniquePerms(){ return written.iterator(); }
  public ResultDrop getResultingDrop(IRNode expr, boolean passed, ResultDrop s) {
    return pdm.createUniqueAssertionDrop(passed,expr,asserted,s,sourceDrops());
  }
  }
  
  class AssertUniquenessClaim extends UniquenessClaim{
  
    public AssertUniquenessClaim(FullPermissionLattice l, ChainLattice perm,
        IRNode asserted) {
      super(l, perm, asserted);
    }
    public AssertUniquenessClaim(FullPermissionLattice l, IRNode asserted) {
      super(l, asserted);
    }
    public AssertUniquenessClaim(FullPermissionLattice l, LocationField lf,
        ChainLattice perm) {
      super(l, lf, perm);
    }
    public boolean makeClaim(SimpleLocation l) {
      final LocationField all = LocationField.allField(l,binder);
      final IRNode fdecl = all.fdecl;
      final boolean b =  l.isUnique() || perms.assertPerm(l,fdecl,perm);
      if(b){
        written.add(l);
        drops.addAll(perms.getDrops(all));
      }
      return b;
    }
  
    @Override
    FullPermissionLattice getResultingLattice() {
      for(Iterator i = written.iterator(); i.hasNext();){
        SimpleLocation sl = (SimpleLocation)i.next();
        PermissionSet ps = ret.getPerms();
        LocationField all = LocationField.allField(sl,binder);
        ret = ret.newLattice(ps.killAll(all));
      }
      return super.getResultingLattice();
    }
  }
  class CheckUniquenessClaim extends UniquenessClaim{
    public CheckUniquenessClaim(FullPermissionLattice l, ChainLattice perm,
        IRNode asserted) {
      super(l, perm, asserted);
    }
    public CheckUniquenessClaim(FullPermissionLattice l, IRNode asserted) {
      super(l, asserted);
    }
    public CheckUniquenessClaim(FullPermissionLattice l, LocationField lf,
        ChainLattice perm) {
      super(l, lf, perm);
    }
    public boolean makeClaim(SimpleLocation l) {
      final LocationField all = LocationField.allField(l,binder);
      final IRNode fdecl = all.fdecl;
      final boolean b =  l.isUnique() || perms.assertPerm(l,fdecl,perm);
      if(b){
        written.add(l);
        drops.addAll(perms.getDrops(all));
      }
      return b;
    }
  
  }
  @Deprecated
  class MustEqualClaim implements LocationClaim{
  
    private final SimpleLocation loc;
    
    public MustEqualClaim(SimpleLocation l){ loc = l;}
    
    public boolean makeClaim(SimpleLocation l) {
      return loc.equals(l);
    }
  }
  @Deprecated
  class PermissionLocationAsserter implements LocationGenerator.LocationAsserter{
    private FullPermissionLattice current;
    
    private final Set<SimpleLocation> unique;
    private final Set<SimpleLocation> shared;
    
    PermissionLocationAsserter(FullPermissionLattice init){
      current = init;
      unique = new HashSet<SimpleLocation>();
      shared = new HashSet<SimpleLocation>();
    }
    
    FullPermissionLattice result(){
      return current;
    }
    public boolean assertShared(SimpleLocation s) {
      if(shared.contains(s)) return true;
      IRNode l = analysis.getUserNode();
      ResultDrop pd = pdm.createMeetDrop(l);
      current = current.assertShared(s,l,l,pd);
      boolean b = pd.isConsistent();
      if(b) shared.add(s);
      return b;
    }

    public boolean assertUnique(SimpleLocation u) {
      if(unique.contains(u)) return true;
      IRNode l = analysis.getUserNode();
      ResultDrop pd = pdm.createMeetDrop(l);
      current = current.assertUnique(u,l,l,pd);
      boolean b = pd.isConsistent();
      if(b) unique.add(u);
      return b;
    }
    
  }
}
@Deprecated
final class SharedField extends LocationField{
  SharedField(SimpleLocation nul){
    super(nul,null);
  }
  @Override
  public String toString() {return "0.Shared";}
}


/**
 *
 */
@SuppressWarnings("unchecked")
@Deprecated
public class PermissionAnalysis extends TrackingIntraproceduralAnalysis implements INullAnalysis {

	//protected final AssuranceLogger log;
		
	public AssuranceLogger log(IRNode flowUnit){
		return ((FullPermissionLattice)getAnalysis(getFlowUnit(flowUnit)).getLattice()).log;
	}

	protected PermissionDropMediator getPDM(FlowAnalysis fa){
	  return ((FullPermissionLattice)fa.getLattice()).pdm;
	}
	protected PermissionDropMediator getPDM(IRNode flowUnit){
	  return getPDM(getAnalysis(getFlowUnit(flowUnit)));
	}
	
	
	public AssuranceLogger logAfter(IRNode node){
		getAnalysisResultsAfter(node);
		return log(getFlowUnit(node));
	}

	public AssuranceLogger resultsForMethod(IRNode mDecl){
		FlowAnalysis fa = getAnalysis(getFlowUnit(mDecl));
		AssuranceLogger log = ((FullPermissionLattice)fa.getLattice()).log;
		PermissionDropMediator pdm = getPDM(mDecl);
		log.stopCollection();
		pdm.stopReporting();
		getAnalysisResultsAfter(mDecl);
		pdm.startReporting();
		log.startCollection();
		fa.reworkAll();
		pdm.stopReporting();
		log.stopCollection();
		return log;
	}
	
	protected void reworkAll(IRNode node){
		getAnalysis(getFlowUnit(node)).reworkAll();
	}

	/**
	 * @param b
	 */
	public PermissionAnalysis(IBinder b) {
		super(b);
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis#createAnalysis(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  protected FlowAnalysis createAnalysis(IRNode flowUnit) {
		final LocationGenerator lg = new LocationGenerator(this);
		final AssuranceLogger log = new AssuranceLogger("Permission Analysis");
		final PermissionDropMediator pdm = new PermissionDropMediator(flowUnit);
		
		final PermProcDrop mr = pdm.res;
		
		return new TrackingForwardAnalysis("Permission Analysis",
			new FullPermissionLattice(flowUnit,binder,lg,log, pdm,this),
			new PermissionTransfer(this, binder,lg,log, pdm, mr));
		}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.INullAnalysis#maybeNull(edu.cmu.cs.fluid.ir.IRNode)
	 */
	public boolean maybeNull(IRNode expr) {
		FullPermissionLattice results = (FullPermissionLattice)getAnalysisResultsBefore(expr);
		LocationMap lm = results.getLoc();
		SimpleLocation l = lm.getLocation(expr);
		SimpleLocation n = lm.nulLoc();
		ConjunctiveFactLattice f = results.getFacts();
		return f.doesAlias(l,n) || (!f.doesNotAlias(l,n));
	}

  class IsWrittenVisitor extends VoidTreeWalkVisitor{

    boolean isWritten(IRNode block){
      return PermissionAnalysis.this.isWritten(expr,block) == 1;
    }
    
    final IRNode expr;
    final Set<IRNode> writes;
    boolean localWrite;
    
    IsWrittenVisitor(IRNode expr){
      this.expr = expr;
      writes = new HashSet<IRNode>();
    }
    
    Set<IRNode> writtenIn(IRNode root){
      writes.clear();
      localWrite = false;
      this.doAccept(root);
      if(localWrite) return null;
      return writes;
    }
    
    void visitSomeCall(IRNode call){
      if(isWritten(call))
        writes.add(binder.getBinding(call));
    }

    @Override
    public Void visitAssignExpression(IRNode node) {
      final IRNode lhs = AssignExpression.getOp1(node);
      final IRNode decl = binder.getBinding(lhs);
      localWrite |= (decl == binder.getBinding(expr));
      return super.visitAssignExpression(node);
    }

    @Override
    public Void visitConstructorCall(IRNode node) {
      visitSomeCall(node);
      return super.visitConstructorCall(node);
    }

    @Override
    public Void visitMethodCall(IRNode node) {
      visitSomeCall(node);
      return super.visitMethodCall(node);
    }

    @Override
    public Void visitNonPolymorphicConstructorCall(IRNode node) {
      visitSomeCall(node);
      return super.visitNonPolymorphicConstructorCall(node);
    }

    @Override
    public Void visitNonPolymorphicMethodCall(IRNode node) {
      visitSomeCall(node);
      return super.visitNonPolymorphicMethodCall(node);
    }

    @Override
    public Void visitPolymorphicConstructorCall(IRNode node) {
      visitSomeCall(node);
      return super.visitPolymorphicConstructorCall(node);
    }
    
  }
  
  Set<IRNode> methodsThatSouldntWrite(IRNode expr, IRNode block){
    return (new IsWrittenVisitor(expr)).writtenIn(block);
  }
  int isWritten(IRNode expr, IRNode block){
    final FullPermissionLattice lat =
      ((FullPermissionLattice)getAnalysisResultsBefore(block));
    final LocationMap lm = lat.getLoc();
    final FullPermissionLattice after =
      (FullPermissionLattice)getAnalysisResultsAfter(block);
    final LocationMap lma = after.getLoc();
    final ConjunctiveFactLattice cfl = after.getFacts();
    if(cfl.getEquiv(lm.getLocation(expr)).contains(lma.getLocation(expr))){
      return 0;
    }
    return 1;
  }
}
@Deprecated
class PermissionTransfer extends JavaEvaluationTransfer{
	protected final AssuranceLogger log;
	protected final PermissionDropMediator pdm;
	protected final PermProcDrop methodResults;
	/**
	 * @param ba
	 * @param binder
	 */
	public PermissionTransfer(IntraproceduralAnalysis ba, IBinder binder, 
	    LocationGenerator lg, AssuranceLogger lo, 
	    PermissionDropMediator pm, PermProcDrop rd) {
		super(ba, binder);
		log = lo;
		pdm = pm;
		methodResults = rd;
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#pop(edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override
  protected Lattice pop(Lattice val) {
		return ((FullPermissionLattice)val).pop();
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#popAllPending(edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override
  protected Lattice popAllPending(Lattice val) {
		return ((FullPermissionLattice)val).popAllPending();
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#push(edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override
  protected Lattice push(Lattice val) {
		return ((FullPermissionLattice)val).push();
	}

	protected AssuranceLogger log(){
		return log;
	}

	protected SimpleLocation nulLoc(FullPermissionLattice l){
		return l.getLoc().nulLoc();
	}

	protected SimpleLocation getLocation(FullPermissionLattice l, IRNode expr){
		return l.getLoc().getLocation(expr);
	}
	protected SimpleLocation getLocation(FullPermissionLattice l, LocationField lf){
		return l.getLoc().getLocation(lf);
	}
	protected FullPermissionLattice assignPseudoLocation(FullPermissionLattice l, LocationField lf, IRNode expr){
		return l.newLattice(l.getLoc().assignPseudoLocation(lf,expr));
	}

	protected FullPermissionLattice addDoesAlias(FullPermissionLattice l, SimpleLocation l1, SimpleLocation l2){
		return l.newLattice(l.getFacts().addDoesAlias(l1,l2));
	}
	protected FullPermissionLattice addDoesNotAlias(FullPermissionLattice l, SimpleLocation l1, SimpleLocation l2){
		return l.newLattice(l.getFacts().addDoesNotAlias(l1,l2));
	}
	protected FullPermissionLattice addRegionMappings(FullPermissionLattice l, IRNode expr){
		return l.newLattice(l.getFacts().addRegionMappings(expr,l.getLoc(),binder));
	}
	protected FullPermissionLattice replaceLocation(FullPermissionLattice l, IRNode decl, SimpleLocation loc){
		return l.newLattice(l.getLoc().replaceLocation(decl,loc));
	}
	protected FullPermissionLattice replaceLocation(FullPermissionLattice l, SimpleLocation o, IRNode decl, 
			SimpleLocation loc){
		return l.newLattice(l.getLoc().replaceLocation(o,decl,loc));
	}

	protected LocationField getAllField(SimpleLocation l){
	  return LocationField.allField(l,binder);
	}

	protected FullPermissionLattice killPermission(FullPermissionLattice l, LocationField lf){
	  PermissionSet ps = l.getPerms();
	  for(Iterator i = l.getFacts().doesAlias(lf.l).iterator(); i.hasNext();){
	    ps = ps.updateUseless((SimpleLocation)i.next(),lf.fdecl);
	  }
	  return l.newLattice(ps);
	}
	protected FullPermissionLattice updateMustEqual2(FullPermissionLattice l, LocationField lf,ChainLattice perm){
	  PermissionSet ps = l.getPerms();
	  for(Iterator i = l.getFacts().doesAlias(lf.l).iterator(); i.hasNext();){
	    ps = ps.updatePerm(new LocationField((SimpleLocation)i.next(),lf.fdecl),perm);
	  }
	  return l.newLattice(ps);
	}

	protected FullPermissionLattice updateMayEqual(FullPermissionLattice l, LocationField lf, ChainLattice perm){
	  PermissionSet ps = l.getPerms();
	  Set dna = l.getFacts().doesNotAlias(lf.l);
	  for(Iterator i = ps.keys();i.hasNext();){
	    LocationField lf2 = (LocationField)i.next();
	    if(lf2.l != null && lf2.fdecl.equals(lf.fdecl)){
	      if(dna.contains(lf2.l)){
	      }else{
	        ps = ps.updatePerm(lf,perm);
	      }
	    }
	  }
	  return l.newLattice(ps);
	}
	
	protected FullPermissionLattice updateMustEqual(FullPermissionLattice l, LocationField lf, ChainLattice perm){
	  SimpleLocation nul = l.getLoc().nulLoc();
	  if(lf.l.equals(nul)){return l;}
	  for(Iterator i = l.getFacts().doesAlias(lf.l).iterator(); i.hasNext();){
	    SimpleLocation sl = (SimpleLocation)i.next();
	    if(!sl.equals(nul)){
	      l = updateMayEqual(l,(new LocationField(sl,lf.fdecl)),perm);
	    }
	  }
	  return l;
	}
	
	protected FullPermissionLattice updateUniquePerm(FullPermissionLattice l, LocationField lf, ChainLattice perm){
	  if(perm.equals(PermissionSet.useless)){ return l; }
	  ChainLattice newPerm = perm.equals(PermissionSet.write)?PermissionSet.useless:PermissionSet.smaller;
	  return l.newLattice(l.getPerms().updatePerm(lf,newPerm));
	}


	protected FullPermissionLattice makeShared(FullPermissionLattice l, LocationField lf){
	  IRNode fdecl = lf.fdecl;
  PromiseDrop pd = pdm.getImplicitSharedDrop(fdecl);
  //	  if(pd == null)
//    return /*l.newLattice(*/l.newLattice(l.getFacts().addIsMappedInto(lf,l.shared))/*.getPerms().updateUseless(lf.l,lf.fdecl))*/;
//	  else
    return l.newLattice(l.newLattice(l.getFacts().addIsMappedInto(lf,l.shared))
	        .getPerms().addDrop(lf,pd));
//*/   
	}
	
	
	protected FullPermissionLattice killField(FullPermissionLattice l, LocationField lf){
	  return l.newLattice(l.getLoc().replaceLocation(lf.l,lf.fdecl,(SimpleLocation)lf.l.top()));
	}
	
	protected boolean isTopLevel(FullPermissionLattice l, LocationField lf){
	  return l.getFacts().allAbove(lf).isEmpty();
	}
	
	protected boolean isAnnotatedShared(IRNode decl){
    Operator op = JJNode.tree.getOperator(decl);
    return decl != null && !(isAnnotatedBorrowed(decl) || isAnnotatedUnique(decl) || isAnnotatedImmutable(decl))
        && op instanceof edu.cmu.cs.fluid.java.bind.IHasType &&
        !(binder.getJavaType(decl) instanceof edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType);
	}
	
	protected boolean isAnnotatedUnique(IRNode decl){
	  return decl != null && (UniquenessAnnotation.isUnique(decl));
	}
	
	protected boolean isAnnotatedBorrowed(IRNode decl){
	  return UniquenessAnnotation.isBorrowed(decl);
	}
	protected boolean isAnnotatedImmutable(IRNode decl){
	  return UniquenessAnnotation.isImmutable(decl);
	}

	protected boolean isAnnotatedShared(LocationField lf){
	  return isAnnotatedShared(lf.fdecl);
	}
	protected boolean isAnnotatedUnique(LocationField lf){
	  return isAnnotatedUnique(lf.fdecl);
	}
	protected boolean isAnnotatedBorrowed(LocationField lf){
	  return isAnnotatedBorrowed(lf.fdecl);
	}
	protected boolean isAnnotatedImmutable(LocationField lf){
	  return isAnnotatedImmutable(lf.fdecl);
	}
	
	protected PermissionSet getAnnotatedPerms(FullPermissionLattice l, IRNode mdecl){
		return l.getPerms().annotatedPermissions(mdecl,l.getLoc(),binder);
	}
	
	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferAllocation(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferAllocation(IRNode node, Lattice value) {
		FullPermissionLattice val = (FullPermissionLattice)value;
		SimpleLocation loc = getLocation(val,node);
		val = addRegionMappings(addDoesNotAlias(val.push(loc),loc,nulLoc(val)),node);
		val = val.newLattice(val.getPerms().updateWrite(LocationField.allField(loc,binder)));
		return val;
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferAssignArray(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferAssignArray(IRNode aref, Lattice val) {
		FullPermissionLattice bef = (FullPermissionLattice)val;
		SimpleLocation aloc = getLocation(bef,ArrayRefExpression.getArray(aref));
		//For now, using null as stand in for array's [] field.
		ResultDrop rd = pdm.createArrayDrop(aref,methodResults);
		bef.assertWrite(aloc,LocationField.arrayRegion(binder),aref,rd);
		return replaceLocation(bef.popSecond().popSecond(),aloc,
        LocationField.arrayRegion(binder),bef.topElement());
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferAssignField(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferAssignField(IRNode fref, Lattice val) {
		FullPermissionLattice bef = (FullPermissionLattice)val;
		ResultDrop rd = pdm.createFieldDrop(fref,methodResults);
		IRNode fdecl = binder.getBinding(fref);
		SimpleLocation floc = bef.secondElement();//getLocation(bef,FieldRef.getObject(fref));
		bef.assertWrite(floc,fdecl,fref,rd);
		bef =  replaceLocation(bef.popSecond(),floc,fdecl,bef.topElement());
		if(isAnnotatedUnique(fdecl)){
			final IRNode assignment = JJNode.tree.getParent(fref);
			final IRNode assigned = AssignExpression.getOp1(assignment);
			bef = bef.checkUnique(bef.topElement(),assignment,assigned,rd);
		}
		return bef;
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferAssignVar(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferAssignVar(IRNode var, Lattice val) {
		FullPermissionLattice bef = (FullPermissionLattice)val;
		IRNode decl = binder.getBinding(var);
		//Should we check write permission for the local? we should always have it anyways...
		return replaceLocation(bef,decl,bef.topElement());
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferEq(edu.cmu.cs.fluid.ir.IRNode, boolean, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferEq(IRNode node, boolean flag, Lattice value) {
		FullPermissionLattice bef = (FullPermissionLattice)value;
		SimpleLocation l1 = getLocation(bef,BinopExpression.getOp1(node));
		SimpleLocation l2 = getLocation(bef,BinopExpression.getOp2(node));
		return flag?addDoesAlias(bef.pop().pop().push(),l1,l2):addDoesNotAlias(bef.pop().pop().push(),l1,l2);
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferInstanceOf(edu.cmu.cs.fluid.ir.IRNode, boolean, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferInstanceOf(
		IRNode node,
		boolean flag,
		Lattice val) {
		FullPermissionLattice bef = (FullPermissionLattice)val;
		SimpleLocation loc =bef.topElement();
		return push(pop(flag?addDoesNotAlias(bef,loc,nulLoc(bef)):bef));
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferIsObject(edu.cmu.cs.fluid.ir.IRNode, boolean, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferIsObject(
		IRNode node,
		boolean flag,
		Lattice value) {
		FullPermissionLattice bef = (FullPermissionLattice)value;
		IRNode obj;
		IRNode pnode = tree.getParent(node);
		Operator op = tree.getOperator(pnode);
		//for now, pull the location tested off the tree instead of the stack.
		if(op instanceof MethodCall){      
			obj = ((MethodCall) op).get_Object(pnode);
		}else if (op instanceof FieldRef){
			obj = FieldRef.getObject(pnode);
		}else if (op instanceof ArrayRefExpression){
			obj = ArrayRefExpression.getArray(pnode);
		}else if (op instanceof SynchronizedStatement){
			obj = SynchronizedStatement.getLock(pnode);
		}else{
			return super.transferIsObject(node, flag, value);
		}
		SimpleLocation loc = getLocation(bef,obj);
		//assertNullCaught(loc,node,bef);
		return flag?addDoesNotAlias(bef,loc,nulLoc(bef)):popAllPending(addDoesAlias(bef,loc,nulLoc(bef)));
	}
		
	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferUseArray(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferUseArray(IRNode aref, Lattice val) {
		FullPermissionLattice bef = (FullPermissionLattice)val;
		SimpleLocation loc  = getLocation(bef,ArrayRefExpression.getArray(aref));
		ResultDrop rd = pdm.createArrayDrop(aref,methodResults);
		bef.assertRead(loc,LocationField.arrayRegion(binder),aref,rd);
		if (isBothLhsRhs(aref))
			bef = bef.push().push();
		return bef.pop().pop().push(getLocation(bef,aref));
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferUseField(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	
  
  @Override protected Lattice transferUseField(IRNode fref, Lattice val) {
    FullPermissionLattice bef = (FullPermissionLattice)val;
	ResultDrop rd = pdm.createFieldDrop(fref,methodResults);
	SimpleLocation loc = getLocation(bef,FieldRef.getObject(fref));

    IRNode fdecl = binder.getBinding(fref);
	bef.assertRead(loc,fdecl,fref,rd);
	if(isBothLhsRhs(fref)){
	  bef = bef.push();
	}
	SimpleLocation floc = getLocation(bef,fref); 
	if(floc.isUnique()){
      bef = bef.instantiateUnique(loc,fdecl);
    }else if(floc.isShared()){
      bef = bef.instantiateShared(loc,fdecl);
    }
    else if(!bef.getLoc().contains(loc,fdecl)){
	  bef = replaceLocation(bef,loc,fdecl,floc);
	  if(isAnnotatedUnique(fdecl)){
	 	bef = bef.makeUnique(new LocationField(loc,fdecl));
	  }else if (isAnnotatedShared(fdecl)){
	   	bef = makeShared(bef,new LocationField(loc,fdecl));
	  }
	}
	return bef.pop().push(floc);
  }

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferUseVar(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferUseVar(IRNode var, Lattice val) {
		FullPermissionLattice bef = (FullPermissionLattice)val;
		SimpleLocation l = getLocation(bef,var);
    if(l.isUnique()){
        bef = bef.instantiateUnique(nulLoc(bef),binder.getBinding(var));
      }else if(l.isShared()){
        bef = bef.instantiateShared(nulLoc(bef),binder.getBinding(var));
      }
        return bef.push(l);
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferCall(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferCall(IRNode node, Lattice value) {
		FullPermissionLattice bef = (FullPermissionLattice)value;
		ResultDrop rd = pdm.createCallDrop(node,methodResults);
		
		Operator op = tree.getOperator(node);
		boolean mcall = MethodCall.prototype.includes(op);
		// Get the node of the method/constructor declaration
		final IRNode mdecl = binder.getBinding(node);

		IRNode receiver,params;
		Iterator argEnum;
		if(mcall){
      MethodCall call = (MethodCall) op;
			receiver  = call.get_Object(node);
			params = MethodDeclaration.getParams(mdecl);
			argEnum = Arguments.getArgIterator(call.get_Args(node));
		}else if (ConstructorCall.prototype.includes(op)){
			receiver = ConstructorCall.getObject(node);
			params = ConstructorDeclaration.getParams(mdecl);
			argEnum = Arguments.getArgIterator(ConstructorCall.getArgs(node));
		}else{
			receiver = node;
			params = ConstructorDeclaration.getParams(mdecl);
			argEnum = Parameters.getFormalIterator(params);
		}
		int nf = JJNode.tree.numChildren(params);
	

		final Iterator formalEnum = Parameters.getFormalIterator(params);
		final SimpleLocation[] actuals = new SimpleLocation[nf];

		for(int i = nf - 1; i >= 0; --i){
			actuals[i] = bef.topElement();
			bef = bef.pop();
		}
		
		// build a table mapping formal parameters locations to actual parameters
		final java.util.HashMap table = new java.util.HashMap();
		for(int i = 0; formalEnum.hasNext();++i) {
			final IRNode form = (IRNode)formalEnum.next();
			final IRNode arg = (IRNode)argEnum.next();
			if(isAnnotatedUnique(form)){
				bef = bef.assertUnique(actuals[i],node,arg,rd);
			}
			if(isAnnotatedShared(form)){
			  bef = bef.assertShared(actuals[i],node,arg,rd);
			}
			table.put(getLocation(bef,form),actuals[i]);
		}
		// if the method is not static, add replacement receiver to mapping
		final boolean isStatic = JavaNode.getModifier(mdecl, JavaNode.STATIC);
		if (!isStatic) {
			final IRNode rec = JavaPromise.getReceiverNode(mdecl);
			if(isAnnotatedUnique(rec)){
			  bef = bef.assertUniqueC(bef.topElement(),node,receiver,rd);
			}
			if(isAnnotatedShared(rec)){
			  bef.assertShared(bef.topElement(), node, receiver, rd);
			}
			table.put(getLocation(bef,rec),bef.topElement());
		}
		if(mcall){
			bef = bef.pop();
		}
		//make assertions
		PermissionSet methodPerms = getAnnotatedPerms(bef,mdecl);

		Set<LocationField> written = new HashSet<LocationField>();
    FullPermissionLattice par_not_eff = bef;

    for(java.util.Iterator annos = methodPerms.keys(); annos.hasNext();){
			LocationField lf = (LocationField)annos.next();
			Object o = table.get(lf.l);
			boolean iU = isAnnotatedUnique(lf), iS = isAnnotatedShared(lf);
			SimpleLocation sl = (o instanceof SimpleLocation)?(SimpleLocation)o:lf.l;
			ChainLattice perm = methodPerms.getPerm(lf);
			
      if(iS){
				bef = bef.assertShared(sl,lf.fdecl,node,rd);
			}
			if(iU){
				bef = bef.assertUnique(sl,lf.fdecl,node,rd);
				written.add(new LocationField(sl,lf.fdecl));
      }
      else if(perm.equals(PermissionSet.write)){
				written.add(new LocationField(sl,lf.fdecl));
			}
			for(Iterator under = bef.getFacts().allBelow(lf).iterator();
						under.hasNext();){

			  LocationField lf2 = (LocationField)under.next();
			  Object o2 = table.get(lf2.l);
				SimpleLocation sl2 = (o2 instanceof SimpleLocation)?(SimpleLocation)o2:lf2.l;
			  boolean iU2 = isAnnotatedUnique(lf2);
			  if(iU2){
				  if(LocationGenerator.isMeaningful(getLocation(bef,lf2)))
				    bef = bef.assertUnique(sl2,lf2.fdecl,node,lf2.fdecl,rd);
			  }

			}
			bef.assertAnnotation(sl,lf.fdecl,perm,node,rd);
			if(PermissionSet.write.equals(perm)){
			  bef = bef.removePerm(new LocationField(sl,lf.fdecl));
      }else{
        bef = bef.removeRead(new LocationField(sl,lf.fdecl));
      }
    }
		bef = par_not_eff;
    
		ConjunctiveFactLattice fcts = bef.getFacts();
		Set<LocationField> temp = new HashSet<LocationField>();
		for(Iterator writ = temp.iterator();writ.hasNext();){
			temp.addAll(bef.getFacts().allBelow((LocationField)writ.next()));
		}
		written.addAll(temp);
		
		for(Iterator wfds = written.iterator();wfds.hasNext();){
			Object o = wfds.next();
			if(o instanceof LocationField){
				final LocationField lf = (LocationField)o;
				Set does = fcts.doesAlias(lf.l);
				Set doesNot = fcts.doesNotAlias(lf.l);
				for(java.util.Iterator fds = bef.getLoc().keys(); fds.hasNext();){
					Object o1 = fds.next();
					if(o1 instanceof LocationField){
						LocationField p = (LocationField)o1;
						if(p.fdecl != null && p.fdecl.equals(lf.fdecl) &&  (does.contains(p.l) || !doesNot.contains(p.l))){	
						    if(isAnnotatedUnique(p)){
						      bef = bef.removePerm(bef.getAllField(getLocation(bef,p),binder));
                }
              bef = bef.renameLocation(p,node);
               if(isAnnotatedUnique(p)){
                 bef = bef.makeUnique(p);
               }
						}
					}
				}
			}
		}
		// if constructor, pop qualifications
		// while leaving receiver in place:
		if (hasOuterObject(node)) {
			if (mcall) {
				throw new FluidError("MethodCall's can't have qualifiers!");
                        }
	                bef = bef.pop();
		}
		// now if a method call, pop receiver and push return value
		if (mcall) {
		    IRNode retDecl = ReturnValueDeclaration.getReturnNode(mdecl);
			bef = bef.push(getLocation(bef,node));
			boolean uniqueRet = isAnnotatedUnique(retDecl) 
				|| NewExpression.prototype.includes(op)
				|| AnonClassExpression.prototype.includes(op);
			SimpleLocation loc = bef.topElement();
			if(uniqueRet){
				bef = bef.newLattice(bef.getPerms().updateWrite(LocationField.allField(loc,binder)));
			}else if(isAnnotatedShared(retDecl)){
				bef = bef.newLattice(bef.getFacts().
							addIsMappedInto(LocationField.allField(loc,binder),
									new LocationField( bef.getLoc().nulLoc(),LocationField.sharedRegion(binder))));
			}//FIXME use shared correctly!
		}
		return bef;
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaEvaluationTransfer#transferInitializationOfVar(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferInitializationOfVar(IRNode node, Lattice value) {
		FullPermissionLattice bef = (FullPermissionLattice)value;
		if (tree.getOperator(node) instanceof VariableDeclarator) {
			IRNode source = VariableDeclarator.getInit(node);
			if(tree.getOperator(source) instanceof Initialization){
				source = Initialization.getValue(source);
			}
			SimpleLocation l = getLocation(bef,source);
			return replaceLocation(bef.pop(),node,l);
		}
		return super.transferInitializationOfVar(node, value);
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferMethodBody(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.control.Port, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferMethodBody(IRNode node, Port kind, Lattice value) {
		FullPermissionLattice before = (FullPermissionLattice)value;
		final IRNode	methodDecl = tree.getParent(node);
		final PermissionSet methodPerms = getAnnotatedPerms(before,methodDecl);
		if (kind instanceof EntryPort) {
			FullPermissionLattice start = before.getInitialLattice(methodDecl,binder);
			for(java.util.Iterator annos = methodPerms.keys(); annos.hasNext();){
				LocationField lf = (LocationField)annos.next();
        if(lf.fdecl == null) continue;
        boolean iU = isAnnotatedUnique(lf), iS = isAnnotatedShared(lf);
				if(iU){
 					start = start.makeUnique(lf);
				}
				if(iS){
					start = makeShared(start,lf);
				}
        
				for(Iterator under = start.getFacts().allBelow(lf).iterator();
					under.hasNext();){
					LocationField lf2 = (LocationField)under.next();
					
					boolean iU2 = isAnnotatedUnique(lf2);
          
          if(iU2){			  
					  if(LocationGenerator.isMeaningful(getLocation(start,lf2))){
				    
					  }else{
					    start = assignPseudoLocation(start,lf2,node);
					  }
						start = start.makeUnique(lf2);//hhh
					}
				}
			}
      return start;
		} else if (kind instanceof ExitPort){
			
      
		  ResultDrop rd = pdm.createRetDrop(methodDecl,methodResults);
		  
			for(java.util.Iterator annos = methodPerms.keys(); annos.hasNext();){
				LocationField lf = (LocationField)annos.next();
        if(lf.fdecl == null){ continue; }
				boolean iU = isAnnotatedUnique(lf), iS = isAnnotatedShared(lf);
				ChainLattice perm = methodPerms.getPerm(lf);
				if(iU){
				  before = before.assertUnique(lf.l,lf.fdecl,node,rd);
				}			
				else if(iS){
					before = before.assertShared(lf.l,lf.fdecl,node,lf.fdecl,rd);
				}
				for(Iterator under = before.getFacts().allBelow(lf).iterator();
							under.hasNext();){
				  LocationField lf2 = (LocationField)under.next();
				  boolean iU2 = isAnnotatedUnique(lf2);
          if(LocationGenerator.isMeaningful(getLocation(before,lf2)))
            if(iU2){
               before = before.assertUnique(lf2.l,lf2.fdecl,node,rd);
            }else if(isAnnotatedShared(lf2)){
              before = before.assertShared(lf2.l, lf2.fdecl, node, lf2.fdecl, rd);
            }
				}

				before.assertAnnotation(lf.l,lf.fdecl,perm,node,rd);
			}

    }
		return super.transferMethodBody(node, kind, value);
	}

	@Override
  protected Lattice transferReturn(IRNode node, Lattice val) {
    FullPermissionLattice before = (FullPermissionLattice)val;
    final IRNode  methodDecl = /*tree.getParent*/(IntraproceduralAnalysis.getFlowUnit(node));
    ResultDrop rd = pdm.createRetDrop(methodDecl,methodResults);

    if(MethodDeclaration.prototype.includes(JJNode.tree.getOperator(methodDecl))
        && !JavaNode.getModifier(methodDecl,JavaNode.STATIC)
        && !(tree.getOperator(MethodDeclaration.getReturnType(methodDecl)) 
            instanceof VoidType)){
      IRNode ret = ReturnValueDeclaration.getReturnNode(methodDecl);
      SimpleLocation rl = before.topElement();
      if(isAnnotatedUnique(ret)){
        before = before.checkUnique(rl,node,ret,rd);
      }else if(isAnnotatedShared(ret)){
        before = before.assertShared(rl,node,ret,rd);
      }

    }
    return before;//super.transferReturn(node, val);
  }

  @Override protected Lattice transferLiteral(IRNode node, Lattice val) {
		Operator op = tree.getOperator(node);
		FullPermissionLattice v = (FullPermissionLattice)val; 
		if(NullLiteral.prototype.includes(op)){
			return v.push(nulLoc(v));
		}
		
		return super.transferLiteral(node, val);
	}

	
	
	

  /* */
  @Override
  public Lattice transferComponentChoice(IRNode node, Object info, boolean flag, Lattice value) {
  /* *
    System.err.println("\n\nAt " + DebugUnparser.toString(node));
    System.err.println("In = " + value); 
/*  */
    ((TrackingIntraproceduralAnalysis)baseAnalysis).setUserNode(node);
    Lattice out =  super.transferComponentChoice(node, info, flag, value);
/* *
    System.err.println("out = " + out); 
/* */    
    return out;
  }

  @Override
  public Lattice transferComponentFlow(IRNode node, Object info, Lattice value) {
    // TODO Auto-generated method stub
/*  *
    System.err.println("\n\nAt " + DebugUnparser.toString(node));
    System.err.println("In = " + value); 
/*  */
    ((TrackingIntraproceduralAnalysis)baseAnalysis).setUserNode(node);
    Lattice out = super.transferComponentFlow(node, info, value);
/* *
    System.err.println("out = " + out); 
/* */
    return out;
 }

}
@Deprecated
interface AssertionClaim extends LocationClaim{
	String positiveMsg(IRNode expr);
	String negativeMsg(IRNode expr);
	ResultDrop getResultingDrop(IRNode expr, boolean passed, ResultDrop s);
}

@Deprecated
abstract class PermissionAssertionClaim implements AssertionClaim, AggregatingClaim{
	protected final PermissionSet perms;
	protected final IRNode fdecl;
	protected final Set supported;
	public IRNode getDecl(){
		return fdecl;
	}
	PermissionAssertionClaim(PermissionSet l, IRNode fd){
	  this(l,fd,new HashSet());
  }
  protected PermissionAssertionClaim(PermissionSet l, IRNode fd, Set sup){
		perms = l;
		fdecl = fd;
		supported = sup;
	}
    
	protected abstract PermissionAssertionClaim newClaim(PermissionSet l, IRNode fd);
	
	public AggregatingClaim extend(IRNode ndecl){
		return newClaim(perms,ndecl);
	}
}
