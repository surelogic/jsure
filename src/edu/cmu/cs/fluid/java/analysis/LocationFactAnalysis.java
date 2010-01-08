package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.control.EntryPort;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.*;

import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;

import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
import edu.cmu.cs.fluid.java.analysis.AliasFactLattice.LocationClaim;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import edu.cmu.cs.fluid.FluidRuntimeException;

/**
 * Initial analysis implementing the "baby IWACO".  Answers a wide variety
 * of aliasing questions fairly conservatively.
 */
@Deprecated
public class LocationFactAnalysis extends TrackingIntraproceduralAnalysis 
																	implements IAliasAnalysis,
																	IEqualAnalysis,
																	INullAnalysis{

	public LocationFactAnalysis(IBinder b) {
		super(b);
	}

	public MethodFactory getMethodFactory(final IRNode flowUnit) {
	  return new MethodFactory() {
      private final FlowAnalysis a = getAnalysis(flowUnit);
      
      public Method getMustAliasMethod(final IRNode before) {
        return new Method() {
          public boolean aliases(final IRNode expr1, final IRNode expr2) {
            return mustAliasInternal(a, expr1, expr2, before);
          }
        };
      }
      
      public Method getMayAliasMethod(final IRNode before) {
        return new Method() {
          public boolean aliases(final IRNode expr1, final IRNode expr2) {
            return mayAliasInternal(a, expr1, expr2, before);
          }
        };
      }
    };
	}

	
	
	@Override
  protected FlowAnalysis createAnalysis(IRNode flowUnit) {
		LocationGenerator gen = new LocationGenerator(this);
		IRNode methodDecl = getRawFlowUnit(flowUnit);

		LocationFactLattice dbi = new LocationFactLattice(methodDecl,binder,gen);
			FlowAnalysis analysis =
				new TrackingForwardAnalysis("draft IWACO analysis",
						dbi,new LocationFactTransfer(this, binder));
			return analysis;
	}
  /*
	private boolean isInterface(IRNode t1){
		return true;
	}
  */
  
	/*
	 * Is t2 a supertype of t1?
	 */
  /*
	public boolean isSupertype(IRNode t1, IRNode t2){
			if(isInterface(t2))
				return true;
			while(t1 != null){
				if(t1.equals(t2)){
					return true;
				}
				t1 = binder.getSuperclass(t1);
			}
			return false;
	}
  */

	/*
	 * Early stab at adding type compatibility as an extra aliasing screen
	 */
	public boolean compatibleTypes(IRNode expr1, IRNode expr2){
		IJavaType j1 = binder.getJavaType(expr1);
		IJavaType j2 = binder.getJavaType(expr2);
		if(j1 instanceof IJavaDeclaredType){
			if(j2 instanceof IJavaDeclaredType){
        /*
			  IRNode t1 = ((IJavaDeclaredType)j1).getDeclaration();
				IRNode t2 = ((IJavaDeclaredType)j1).getDeclaration();
        return isSupertype(t1,t2) || isSupertype(t2,t1);
        */
        ITypeEnvironment te = binder.getTypeEnvironment();
        return te.isSubType(j1,j2) || te.isSubType(j2,j1);

			}
			return false;
		}
		return j1.equals(j2);
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.IAliasAnalysis
	 */
	public boolean mayAlias(IRNode expr1, IRNode expr2, IRNode before, IRNode constructorContext) {
	  return mayAliasInternal(getAnalysis(getFlowUnit(before, constructorContext)), expr1, expr2, before);
	}

	 private boolean mayAliasInternal(FlowAnalysis a, IRNode expr1, IRNode expr2, IRNode before) {
	    ILocationFactLattice results = 
	              (ILocationFactLattice) a.getAfter(before, WhichPort.NORMAL_EXIT);
	    SimpleLocation l1 = results.getLocation(expr1);
	    SimpleLocation l2 = results.getLocation(expr2);
	    return !(results.doesNotAlias(l1,l2));// && (compatibleTypes(expr1,expr2));
	  }

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.IAliasAnalysis
	 */
	public boolean mustAlias(IRNode expr1, IRNode expr2, IRNode before, IRNode constructorContext) {
    return mustAliasInternal(getAnalysis(getFlowUnit(before, constructorContext)), expr1, expr2, before);
	}

  private boolean mustAliasInternal(FlowAnalysis a, IRNode expr1, IRNode expr2, IRNode before) {
     ILocationFactLattice results = 
               (ILocationFactLattice) a.getAfter(before, WhichPort.NORMAL_EXIT);
     SimpleLocation l1 = results.getLocation(expr1);
     SimpleLocation l2 = results.getLocation(expr2);
     return results.doesAlias(l1,l2);
   }

	public boolean mustAliasOneOf(IRNode before, IRNode var, Set<IRNode> bunchOfVars, IRNode constructorContext){
		ILocationFactLattice results = (ILocationFactLattice)getAnalysisResultsAfter(before, constructorContext);
		SimpleLocation l = results.getLocation(var);
		final Set<SimpleLocation> locs = new HashSet<SimpleLocation>();
		for(Iterator<IRNode> i = bunchOfVars.iterator();i.hasNext();){
			locs.add(results.getLocation(i.next()));
		}
		LocationClaim c = new LocationClaim(){
			public boolean makeClaim(SimpleLocation loc){
				return locs.contains(loc);
			}
		};
		return results.makeClaim(l,c);
	}

	protected boolean isLoop(IRNode expr){
		Operator op = tree.getOperator(expr);
		return (op instanceof WhileStatement 
					|| op instanceof ForStatement
					|| op instanceof DoStatement );
	}

	protected boolean isBlockSafe(IRNode expr, IRNode block){
		IRNode e = expr;
		while(!e.equals(block)){
			if(isLoop(e)){
				return false;
			}
			e = tree.getParent(e);
			if(expr == null){
				throw new IllFormedBlockException("Block " + 
											DebugUnparser.toString(block)+ 
											" does not contain "+ 
											DebugUnparser.toString(expr));
						}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.IEqualAnalysis
	 */
	public boolean mayEqual(IRNode expr1, IRNode expr2, IRNode block, IRNode constructorContext) {
		if(isBlockSafe(expr1,block) && isBlockSafe(expr2,block)){
			ILocationFactLattice res1 = (ILocationFactLattice)getAnalysisResultsBefore(expr1, constructorContext);
			SimpleLocation loc1 = res1.getLocation(expr1);
			SimpleLocation loc2 = res1.getLocation(expr2);
			if(res1.doesNotAlias(loc1,loc2)){
				return false;
			}
      // XXX: SHouldn't this be expr2???  (noted by Aaron Greenhouse 2010-01-04)
			ILocationFactLattice res2 = (ILocationFactLattice)getAnalysisResultsBefore(expr1, constructorContext);
			SimpleLocation l1 = res2.getLocation(expr1);
			SimpleLocation l2 = res2.getLocation(expr2);
			if(res2.doesNotAlias(l1,l2)){
				return false;
			}
		}
		return true;//compatibleTypes(expr1,expr2);
	}

	/*
	 * @see edu.cmu.cs.fluid.java.analysis.IEqualAnalysis
	 *
	 *	Ultraconservative implementation
	 */
	public boolean mustEqual(IRNode expr1, IRNode expr2, IRNode block, IRNode constructorContext) {
		if(isBlockSafe(expr1,block) && isBlockSafe(expr2,block)){
			ILocationFactLattice res1 = (ILocationFactLattice)getAnalysisResultsBefore(expr1, constructorContext);
			SimpleLocation loc1 = res1.getLocation(expr1);
			SimpleLocation loc2 = res1.getLocation(expr2);
			if(!loc1.equals(loc2)){
				return false;
			}
			// XXX: SHouldn't this be expr2???  (noted by Aaron Greenhouse 2010-01-04)
			ILocationFactLattice res2 = (ILocationFactLattice)getAnalysisResultsBefore(expr1, constructorContext);
			SimpleLocation l1 = res2.getLocation(expr1);
			SimpleLocation l2 = res2.getLocation(expr2);
			if(!l1.equals(l2)){
				return false;
			}
			return true;
		}
		return false;
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.INullAnalysis#maybeNull(edu.cmu.cs.fluid.ir.IRNode)
	 */
	public boolean maybeNull(IRNode expr, IRNode constructorContext) {
		ILocationFactLattice results = (ILocationFactLattice)getAnalysisResultsAfter(expr, constructorContext);
		SimpleLocation l = results.getLocation(expr);
		SimpleLocation n = results.nulLoc();
		return results.doesAlias(l,n) || (!results.doesNotAlias(l,n));
	}

}

/**
 * Naive but (I believe) sound transfer functions for the above analysis.
 * for details on the individual functions @see edu.cmu.cs.fluid.java.analysis.JavaTransfer
 */
@Deprecated
class LocationFactTransfer extends JavaForwardTransfer{
	
	public LocationFactTransfer(IntraproceduralAnalysis base, IBinder binder) {
		super(base, binder);
	}

	/*Better setup*/
	@Override protected Lattice transferMethodBody(IRNode node, Port kind, Lattice value) {
		if (kind instanceof EntryPort) {
			LocationFactLattice before = (LocationFactLattice)value;
			ILocationMap start = (ILocationMap)before.bottom();
			IRNode	methodDecl = tree.getParent(node);
			Operator mdop = tree.getOperator(methodDecl);
			IRNode params;
			if(mdop instanceof MethodDeclaration){
				params = MethodDeclaration.getParams(methodDecl);
			}else if(mdop instanceof ConstructorDeclaration){
				params = ConstructorDeclaration.getParams(methodDecl);
			}else{
				return start;//((LocationFactLattice)start).replaceValue(LocationFactLattice.FACTS,ConjunctiveFactLattice.getInitialMethodAliasing(tree.getParent(node),start,binder));
			}
			Iterator e = Parameters.getFormalIterator(params);
			while(e.hasNext()){
				IRNode paramDecl = (IRNode)e.next();
				if(tree.getOperator(paramDecl) instanceof ParameterDeclaration)
					start = start.replaceLocation(paramDecl,start.getLocation(paramDecl));	
			}
			return ((LocationFactLattice)start).replaceValue(LocationFactLattice.FACTS,ConjunctiveFactLattice.getInitialMethodAliasing(tree.getParent(node),start,binder));
		} 
		return super.transferMethodBody(node, kind, value);
	}
/**/	


	@Override
  public Lattice transferAssignment(IRNode node, Lattice value) {
    AssignmentInterface op = (AssignmentInterface)tree.getOperator(node);
		IRNode lhs = op.getTarget(node);
		Operator lhsOp = tree.getOperator(lhs);
		ILocationMap lm = (ILocationMap)value;
		if (lhsOp instanceof VariableUseExpression) {
			IRNode decl = binder.getBinding(lhs);
			SimpleLocation l = lm.getLocation(op.getSource(node));
			return lm.replaceLocation(decl,l);
		} else if (lhsOp instanceof FieldRef){
			IRNode fdecl = binder.getBinding(lhs);
			SimpleLocation obj = lm.getLocation(FieldRef.getObject(lhs));
			SimpleLocation l = lm.getLocation(op.getSource(node));
			return lm.replaceLocation(obj,fdecl,l);
		}
		return value;
	}
	
	@Override
  public Lattice transferInitialization(IRNode node, Lattice value) {
		ILocationMap lm = (ILocationMap)value;
		if (tree.getOperator(node) instanceof VariableDeclarator) {
			IRNode source = VariableDeclarator.getInit(node);
			if(tree.getOperator(source) instanceof Initialization){
				source = Initialization.getValue(source);
			}
			SimpleLocation l = lm.getLocation(source);
			return lm.replaceLocation(node,l);
		}
		return value;
	}
		
	@Override protected Lattice transferRelop(IRNode node, Operator op, boolean flag, Lattice before)
	{
		ILocationFactLattice lbefore = (ILocationFactLattice)before;
		if(op instanceof InstanceOfExpression){
			IRNode var = InstanceOfExpression.getValue(node);
			return flag?
					lbefore.addDoesNotAlias(lbefore.getLocation(var),lbefore.nulLoc()):
					before;
		}
		else if(op instanceof NotEqExpression){
			IRNode lhe = NotEqExpression.getOp1(node);
			IRNode rhe = NotEqExpression.getOp2(node);
			return flag?
				lbefore.addDoesNotAlias(lbefore.getLocation(lhe),
																lbefore.getLocation(rhe)):
				lbefore.addDoesAlias(lbefore.getLocation(lhe),
															lbefore.getLocation(rhe));
		}
		else if(op instanceof EqExpression){
			IRNode lhe = EqExpression.getOp1(node);
			IRNode rhe = EqExpression.getOp2(node);
			return flag?
				lbefore.addDoesAlias(lbefore.getLocation(lhe),
															lbefore.getLocation(rhe)):
				lbefore.addDoesNotAlias(lbefore.getLocation(lhe),
																lbefore.getLocation(rhe));
		}		
		return before;
	}

	@Override protected Lattice transferIsObject(IRNode node, boolean flag, 
																			Lattice before){

		IRNode pnode = tree.getParent(node);
		Operator op = tree.getOperator(pnode);
		ILocationFactLattice bef = (ILocationFactLattice)before;

		if(op instanceof MethodCall){
      MethodCall mcall = (MethodCall) op;
			IRNode receiver = mcall.get_Object(pnode);
			SimpleLocation rloc = bef.getLocation(receiver);
			if (is_assigned(rloc, mcall.get_Args(pnode), bef)){
				return before;				
			}else{
				return flag?(bef.addDoesNotAlias(rloc,bef.nulLoc())):
//						(bef.doesNotAlias(rloc,bef.nulLoc())?before.top():before);
							bef.addDoesAlias(rloc,bef.nulLoc());
			}
		}else if (op instanceof FieldRef){
			IRNode obj = FieldRef.getObject(pnode);
			if(ThisExpression.prototype.includes(tree.getOperator(obj))){
				bef = (ILocationFactLattice)bef.addDoesNotAlias(bef.getLocation(obj),bef.nulLoc());
			}
			SimpleLocation loc = bef.getLocation(obj);	
			return flag?(bef.addDoesNotAlias(loc,bef.nulLoc())):
//				(bef.doesNotAlias(loc,bef.nulLoc())?before.top():before);
			bef.addDoesAlias(loc,bef.nulLoc());
		}else if (op instanceof ArrayRefExpression){
			IRNode ar = ArrayRefExpression.getArray(pnode);
			SimpleLocation arloc = bef.getLocation(ar);
			if (is_assigned(arloc, ArrayRefExpression.getIndex(pnode), bef)){
				return before;				
			}else{
				return flag?(bef.addDoesNotAlias(arloc,bef.nulLoc())):
//						(bef.doesNotAlias(arloc,bef.nulLoc())?before.top():before);
				bef.addDoesAlias(arloc,bef.nulLoc());
			}
		}else if (op instanceof SynchronizedStatement){
			IRNode lock  = SynchronizedStatement.getLock(pnode);
			SimpleLocation loc = bef.getLocation(lock);
			return flag?(bef.addDoesNotAlias(loc,bef.nulLoc())):
//				(bef.doesNotAlias(loc,bef.nulLoc())?before.top():before);
				bef.addDoesAlias(loc,bef.nulLoc()); //??
		}
		return before;
	}

	/*
	 * from NonNull
	 */
	private boolean is_assigned(SimpleLocation var, IRNode expr, 
															ILocationFactLattice nn){
			Operator op = tree.getOperator(expr);
			if(op instanceof AssignmentInterface){
				IRNode target = ((AssignmentInterface)op).getTarget(expr);
				SimpleLocation lt = nn.getLocation(target);
				if(lt.equals(var)){
					IRNode source = ((AssignmentInterface)op).getSource(expr);
					SimpleLocation ls = nn.getLocation(source);
					if(!nn.doesNotAlias(ls,nn.nulLoc()))
						return true;
				}
			}
			int nk = tree.numChildren(expr);
			boolean isa = false;
			for (int i = 0; !isa && i < nk; ++i){
				isa |= is_assigned(var, tree.getChild(expr,i),nn);
			}
			return isa;
		}

	@Override protected Lattice transferOperation(
		IRNode node,
		Operator op,
		Object info,
		Lattice value) {
		Operator op2 = tree.getOperator(node);
		if(op2 instanceof AllocationExpression){
			ILocationFactLattice before = (ILocationFactLattice)value;
			return before.addDoesNotAlias(before.getLocation(node),before.nulLoc());
			//TODO we can do better, as a new expr also does not equal any prexisting location.
		}
		return super.transferOperation(node, op, info, value);
	}



	@Override protected Lattice transferUse(IRNode node, Operator op, Lattice value) {
		Operator op2 = tree.getOperator(node);
		LocationFactLattice bef = (LocationFactLattice)value;
		if(op2 instanceof FieldRef){
			///////////////
			//ick.  But, for now...
			//if the field maps to an interesting location, continue to use that location,
			//otherwise map the field to a new location
			SimpleLocation loc = bef.getLocation(FieldRef.getObject(node));
			IRNode fdecl = binder.getBinding(node);
			if(fdecl != null && !bef.contains(loc,fdecl)){
				SimpleLocation l = bef.getLocation(node);
				return bef.replaceLocation(loc,fdecl,l);
			}
		}
		else if(op2 instanceof ThisExpression){
			return bef.addDoesNotAlias(bef.getLocation(node),bef.nulLoc());
		}
		return super.transferUse(node, op, value);
	}

	/* 
	 * For the moment, this assumes that any field is trashed by the call
	 * Effects can be used as a trusted annotation to improve the precision
	 * (Of course, everything will change with permissions)
	 */
	@Override protected Lattice transferCall(IRNode call, boolean flag, Lattice value) {
		LocationFactLattice before = (LocationFactLattice)value;
		LocationMap lm = (LocationMap)before.getMap();
		for(java.util.Iterator i = lm.keys(); i.hasNext();){
			Object o = i.next();
			if(o instanceof LocationField){
				LocationField p = (LocationField)o;
				
				lm = (LocationMap)lm.replaceLocation(p.l,p.fdecl,(SimpleLocation)p.l.bottom());
			}
		}
		return flag?before.replaceValue(LocationFactLattice.LOCMAP,lm)
											:super.transferCall(call,flag,value);
		// TODO use effects
	}

}

/*
 */
class IllFormedBlockException extends FluidRuntimeException{
	
	public IllFormedBlockException() {
		super();
	}

	public IllFormedBlockException(String s) {
		super(s);
	}

}
	