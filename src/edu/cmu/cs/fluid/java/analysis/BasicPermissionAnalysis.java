/*
 * Created on Oct 27, 2003
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.control.ForwardAnalysis;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.control.EntryPort;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.promise.EffectSpecification;
import edu.cmu.cs.fluid.java.bind.EffectsAnnotation;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;


interface AssertionReporter{
	public void reportAssertion(IRNode expr, IRNode locale);
}
/**
 *
 */
@Deprecated
public class BasicPermissionAnalysis extends IntraproceduralAnalysis {

	protected final LocationFactAnalysis lfa;
	protected final AssuranceLogger asserted;
	
	public static Logger LOG = SLLogger.getLogger("PERMS");
	/**
	 * @param b
	 */
	public BasicPermissionAnalysis(IBinder b, LocationFactAnalysis lfa) {
		super(b);
		this.lfa = lfa;
		asserted = new AssuranceLogger("Basic Permission Analysis");
	}

	public Set failures(){
		return asserted.getNegativeAssurances();
	}
	public Set successes(){
		return asserted.getPositiveAssurances();
	}
	public /*final*/ AssuranceLogger log(){
		return asserted;
	}
	public void clearAssertions(){
		asserted.clearAssurances();
	}
	
	public AssuranceLogger getLogAfter(IRNode node, IRNode constructorContext){
//		clearAssertions();
		getAnalysisResultsAfter(node, constructorContext);
		return log();
	}
	
	public AssuranceLogger getLogBefore(IRNode node, IRNode constructorContext){
		getAnalysisResultsBefore(node, constructorContext);
		return log();
	}
	
	public AssuranceLogger getLogAbrupt(IRNode node, IRNode constructorContext){
		getAnalysisResultsAbrupt(node, constructorContext);
		return log();
	}
	
	public Set getNegativeFor(IRNode node, IRNode constructorContext){
		return getLogAfter(node, constructorContext).getNegativeAssurancesFor(node);
	}

	public Set getPositiveFor(IRNode node, IRNode constructorContext){
		return getLogAfter(node, constructorContext).getPositiveAssurancesFor(node);
	}

	static final int passed = 0;
	static final int failed = 1;
	
	static final int read = 0;
	static final int write = 1;
	static final int sameRead = 2;
	static final int sameWrite = 3;
	static final int meetRead = 4;
	static final int meetWrite = 5;

	protected final AssertionReporter reporters[][] = {
		{
			new AssertionReporter(){
				public void reportAssertion(IRNode read, IRNode locale){
					String message = "read permission for "+DebugUnparser.toString(read)+" was present";
					asserted.reportPositiveAssurance(message,locale);
				}
			},
			new AssertionReporter(){
				public void reportAssertion(IRNode read,IRNode locale){
					String message = "read permission for "+DebugUnparser.toString(read)+" missing";
					asserted.reportNegativeAssurance(message,locale);		
				}
			}
		},
		{
			new AssertionReporter(){
				public void reportAssertion(IRNode read, IRNode locale){
						String message = "write permission for "+DebugUnparser.toString(read)+" was present";
						asserted.reportPositiveAssurance(message,locale);
					}
			},
			new AssertionReporter(){
				 public void reportAssertion(IRNode read,IRNode locale){
					String message = "write permission for "+DebugUnparser.toString(read)+" missing";
					asserted.reportNegativeAssurance(message,locale);		
				}
			}
		},
		{
			new AssertionReporter(){
				public void reportAssertion(IRNode read, IRNode locale){
					String message = "the read permission for "+DebugUnparser.toString(read)+" is the same";
					asserted.reportPositiveAssurance(message,locale);
				}
			},
			new AssertionReporter(){
				public void reportAssertion(IRNode read,IRNode locale){
					String message = "the read permission for "+DebugUnparser.toString(read)+" shrank";
					asserted.reportNegativeAssurance(message,locale);		
				}
			}
		},
		{
			new AssertionReporter(){
				public void reportAssertion(IRNode read, IRNode locale){
					String message = "the write permission for "+DebugUnparser.toString(read)+" is the same";
					asserted.reportPositiveAssurance(message,locale);
				}
			},
			new AssertionReporter(){
				public void reportAssertion(IRNode read,IRNode locale){
					String message = "the write permission for "+DebugUnparser.toString(read)+" shrank";
					asserted.reportNegativeAssurance(message,locale);		
				}
			}
		},
		{
			new AssertionReporter(){
				public void reportAssertion(IRNode expr, IRNode locale){
					String message = "read annotation for " + DebugUnparser.toString(expr) + " met";
					asserted.reportPositiveAssurance(message,locale);
				}
			},
			new AssertionReporter(){
				public void reportAssertion(IRNode expr, IRNode locale){
					String message = "could not meet read annotation for " + DebugUnparser.toString(expr);
					asserted.reportNegativeAssurance(message,locale);
				}
			}
		},
		{
			new AssertionReporter(){
				public void reportAssertion(IRNode expr, IRNode locale){
					String message = "write annotation for " + DebugUnparser.toString(expr) + " met";
					asserted.reportPositiveAssurance(message,locale);
				}
			},
			new AssertionReporter(){
				public void reportAssertion(IRNode expr, IRNode locale){
					String message = "could not meet write annotation for " + DebugUnparser.toString(expr);
					asserted.reportNegativeAssurance(message,locale);
				}
			}
		}
	};
	
	void reportAssertion(IRNode expr, IRNode locale, int assertionCode, boolean success){
		int s = success?0:1;
		reporters[assertionCode][s].reportAssertion(expr,locale);
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis#createAnalysis(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  protected FlowAnalysis createAnalysis(IRNode flowUnit) {
		return new ForwardAnalysis("Oversimplified permission analysis", 
															new PermissionSet(), new BasicPermissionTransfer(this,binder, flowUnit),
                              DebugUnparser.viewer);
	}


//	ILocationMap mapBefore(IRNode node){
//		return (ILocationMap)lfa.getAnalysisResultsBefore(node);
//	}
//	ILocationMap mapAfter(IRNode node){
//		return (ILocationMap)lfa.getAnalysisResultsAfter(node);
//	}
//	AliasFactLattice aliasingBefore(IRNode node){
//		return (AliasFactLattice)lfa.getAnalysisResultsBefore(node);
//	}
//	AliasFactLattice aliasingAfter(IRNode node){
//		return (AliasFactLattice)lfa.getAnalysisResultsAfter(node);
//	}

/*	boolean assertRead(SimpleLocation loc, IRNode fdecl,IRNode expr, IRNode locale){
		PermissionSet ps = (PermissionSet)getAnalysisResultsBefore(locale);		
*/
	
}
@Deprecated
class BasicPermissionTransfer extends JavaForwardTransfer{
	private final FlowAnalysis focusedLFA;
	private final IRNode flowUnit;
	
	public BasicPermissionTransfer(
		IntraproceduralAnalysis base,
		IBinder binder,
		IRNode flowUnit) {
		super(base, binder);
		focusedLFA = lfa().getAnalysis(flowUnit);
		this.flowUnit = flowUnit;
	}

	LocationFactAnalysis lfa(){
		return ((BasicPermissionAnalysis)baseAnalysis).lfa;
	}

	BasicPermissionAnalysis base(){
		return (BasicPermissionAnalysis)baseAnalysis;
	}

	ILocationMap mapBefore(IRNode node){
	  return (ILocationMap) focusedLFA.getAfter(node, WhichPort.ENTRY);
//		return (ILocationMap)lfa().getAnalysisResultsBefore(node);
	}
//	ILocationMap mapAfter(IRNode node){
//		return (ILocationMap)lfa().getAnalysisResultsAfter(node);
//	}
	AliasFactLattice aliasingBefore(IRNode node){
    return (AliasFactLattice) focusedLFA.getAfter(node, WhichPort.ENTRY);
//		return (AliasFactLattice)lfa().getAnalysisResultsBefore(node);
	}
	AliasFactLattice aliasingAfter(IRNode node){
    return (AliasFactLattice) focusedLFA.getAfter(node, WhichPort.NORMAL_EXIT);
//		return (AliasFactLattice)lfa().getAnalysisResultsAfter(node);
	}

/**/

	boolean assertRead(IRNode expr,IRNode locale, PermissionSet ps){
		return makeAssertion(expr,locale,ps,BasicPermissionAnalysis.read);
	}
	boolean assertWrite(IRNode expr,IRNode locale, PermissionSet ps){
		return makeAssertion(expr,locale,ps,BasicPermissionAnalysis.write);
	}

	boolean assertRead(SimpleLocation loc, IRNode fdecl,IRNode locale, IRNode expr,PermissionSet ps){
		return makeAssertion(loc,fdecl,locale,expr,ps,BasicPermissionAnalysis.read);
	}


	boolean assertWrite(SimpleLocation loc, IRNode fdecl,IRNode locale, IRNode expr,PermissionSet ps){
		return makeAssertion(loc,fdecl,locale,expr,ps,BasicPermissionAnalysis.write);
	}

	boolean makeAssertion(IRNode expr, IRNode locale,PermissionSet ps, int assertCode){
		ILocationMap lm = mapBefore(locale);
		Operator op = tree.getOperator(expr);
		if(VariableDeclaration.prototype.includes(op)){
			return makeAssertion(lm.nulLoc(),expr,locale,expr,ps,assertCode);
		}else if(VariableUseExpression.prototype.includes(op)){
			return makeAssertion(binder.getBinding(expr),locale,ps,assertCode);
		}else if(FieldRef.prototype.includes(op) ){
			SimpleLocation loc = lm.getLocation(FieldRef.getObject(expr));
			IRNode fdecl = binder.getBinding(expr);
			return makeAssertion(loc,fdecl,locale,expr,ps,assertCode);
		}else if(ThisExpression.prototype.includes(op)){
			IRNode d = JavaPromise.getReceiverNode(flowUnit);
			return makeAssertion(lm.nulLoc(),d,locale,expr,ps,assertCode);
		}else{
			return makeAssertion(lm.nulLoc(),expr,locale,expr,ps,assertCode);
		}
	}

	String toString(int ac){
		String names[] =  new String[]{"read","write","sameRead","sameWrite","meetRead","meetWrite"};
		if(0 <= ac && ac < names.length){
			return names[ac];
		}
		return "UNKNOWN";
	}


	boolean makeAssertion(final SimpleLocation loc, final IRNode fdecl, final IRNode locale, 
	                                              final IRNode expr, final PermissionSet ps, final int aCode){
		final AliasFactLattice afl = aliasingBefore(locale);

	final BasicPermissionTransfer t = this;

		final AliasFactLattice.LocationClaim lc = new AliasFactLattice.LocationClaim(){
			public boolean makeClaim(SimpleLocation l){
				return makeAssertion(l,fdecl,ps,aCode);
			}
			@Override
      public String toString(){
				return "Asserting " + t.toString(aCode) + " in " + ps;
			}
		};

		boolean b = afl.makeClaim(loc,lc);
		base().reportAssertion(expr,locale,aCode,b);
		return b;
	}

	boolean makeAssertion(SimpleLocation loc, IRNode fdecl, PermissionSet ps, int assertionCode){
		if(assertionCode == BasicPermissionAnalysis.read || 
			assertionCode == BasicPermissionAnalysis.meetRead){
			return ps.assertRead(loc,fdecl);
		}else if (assertionCode == BasicPermissionAnalysis.sameRead){
			return ps.assertSameRead(loc,fdecl);
		}else{
			//Whatever we assert will pass if we have write permission
			return ps.assertWrite(loc,fdecl);
		}
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferMethodBody(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.control.Port, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferMethodBody(IRNode node, Port kind, Lattice value) {
		if(kind instanceof EntryPort){
			PermissionSet p = (PermissionSet)value;
			IRNode par = tree.getParent(node);
			return p.initialize(par,mapBefore(node),binder);
		}

		return super.transferMethodBody(node, kind, value);
	}
	
	

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferUse(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.tree.Operator, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferUse(IRNode node, Operator op, Lattice value) {
		op = tree.getOperator(node);
		if(SuperExpression.prototype.includes(op)){
			return value;
		}
		@SuppressWarnings("unused")
    boolean b = assertRead(node,node,(PermissionSet)value);
		return value;//super.transferUse(node, op, value);
	}
	

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferAssignment(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferAssignment(IRNode node, Lattice value) {
    AssignmentInterface op = (AssignmentInterface)tree.getOperator(node);
		IRNode lhs = op.getTarget(node);
		Operator lhsOp = tree.getOperator(lhs);
		ILocationMap lm = mapBefore(node);
		boolean b = true;
		if (lhsOp instanceof VariableUseExpression) {
			IRNode decl = binder.getBinding(lhs);
			b = assertWrite(decl,node,(PermissionSet)value);
		} else if (lhsOp instanceof FieldRef){
			IRNode fdecl = binder.getBinding(lhs);
			SimpleLocation obj = lm.getLocation(FieldRef.getObject(lhs));
			b = assertWrite(obj,fdecl,node,lhs,(PermissionSet)value);
		}
		return value;//super.transferAssignment(node, value);
	}
	
	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferCall(edu.cmu.cs.fluid.ir.IRNode, boolean, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferCall(IRNode call, boolean flag, Lattice value) {
		PermissionSet ps = (PermissionSet)value;

		// Get the node of the method/constructor declaration
		final IRNode mdecl = binder.getBinding(call);

		Operator op = tree.getOperator(call);
		IRNode receiver;
		if(MethodCall.prototype.includes(op)){
      MethodCall mcall = (MethodCall) op;
			receiver  = mcall.get_Object(call);
		}else if (ConstructorCall.prototype.includes(op)){
			receiver = ConstructorCall.getObject(call);
		}else{
			receiver = call;
		}
			// Get the formal parameters
		final IRNode params =
			(tree.getOperator(mdecl) == MethodDeclaration.prototype)
				? MethodDeclaration.getParams(mdecl)
				: ConstructorDeclaration.getParams(mdecl);

		final IRNode args = ((CallInterface)op).get_Args(call);

		final Iterator<IRNode> paramsEnum = Parameters.getFormalIterator(params);
		final Iterator<IRNode> argsEnum = Arguments.getArgIterator(args);

		// build a table mapping each formal parameter to its actual
		final Map<IRNode,IRNode> table = new HashMap<IRNode,IRNode>();
		while (paramsEnum.hasNext()) {
			table.put(paramsEnum.next(), argsEnum.next());
		}
		// if the method is not static, add replacement receiver to mapping
		if (!JavaNode.getModifier(mdecl, JavaNode.STATIC)) {
			table.put(JavaPromise.getReceiverNode(mdecl), receiver);
		}
		ILocationMap lm = mapBefore(call);
		final Iterator<IRNode> effects = EffectsAnnotation.methodEffects(mdecl);
		if(effects != null){
			while(effects.hasNext()){
				final IRNode eff = effects.next();
				IRNode context = EffectSpecification.getContext(eff);
				final IRNode reg = EffectSpecification.getRegion(eff);
				if(context == null || reg == null){
					LOG.warning("Bad Annotation: Null effect " + DebugUnparser.toString(eff));
					continue;
				}
				if(ThisExpression.prototype.includes(tree.getOperator(context))){
					context = JavaPromise.getReceiverNode(mdecl);
				}else{
					context = binder.getBinding(context);
					context = table.get(context);
				}
				SimpleLocation ctxt = lm.getLocation(context);

				final IRNode regdecl = binder.getBinding(reg);
/*
				if(EffectSpecification.getIsWrite(eff)){
					assertWrite(ctxt,regdecl,call,regdecl,ps);
				}else{
					assertRead(ctxt,regdecl,call,regdecl,ps);
				}
/**/
				int code = EffectSpecification.getIsWrite(eff)?BasicPermissionAnalysis.meetWrite:BasicPermissionAnalysis.meetRead;
				makeAssertion(ctxt,regdecl,call,regdecl,ps,code);
/**/
						}
		}

		return ps;
	}

	/* 
	 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer#transferInitialization(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.util.Lattice)
	 */
	@Override protected Lattice transferInitialization(IRNode node, Lattice value) {
		PermissionSet ps = (PermissionSet)value;
		if (tree.getOperator(node) instanceof VariableDeclarator) {
			return ps.updateWrite(mapBefore(node).nulLoc(),node);
		}
		return super.transferInitialization(node, value);
	}

}