package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.control.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ASTDropGenerator;

import edu.cmu.cs.fluid.java.bind.NotNullAnnotation;
import java.text.MessageFormat;
import java.util.*;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;

/**
 * 
 * Simple analysis to determine whether local variables are provably not Null.
 */
public class NonNull extends IntraproceduralAnalysis implements INullAnalysis{

	private FlowAnalysis analysis;

  protected final ASTDropGenerator a;
  private final MaybeNullDropGenerator m;
  
  private boolean reporting_off;
  
	public NonNull(IBinder b) { 
    super(b); 
    a = new ASTDropGenerator();
    m = new MaybeNullDropGenerator();
    reporting_off = true;
  }

	@Override
  public FlowAnalysis createAnalysis(IRNode methodDecl) {
    	Lattice s = new NonNullLattice(a);
    	analysis =
        	new ForwardAnalysis("Null Pointer analysis",
				     s,new NonNullTransfer(this,binder), DebugUnparser.viewer);
    	return analysis;
   }

	/**
	 * returns true iff this analysis can prove the expression in not null. 
	 * 
	 * Only applies to array and class types.	 
	 */
  
  public boolean isNonNullExpr(IRNode expr, IRNode constructorContext){
    NonNullLattice nn = (NonNullLattice)getAnalysisResultsBefore(expr, constructorContext);
    return is_nn_expr(expr,nn);
  }

  
   public boolean isNonNullExprWithChecks(IRNode expr, IRNode constructorContext){
		NonNullLattice nn = (NonNullLattice)getAnalysisResultsBefore(expr, constructorContext);
    reporting_off = false;
    this.getAnalysis(getFlowUnit(expr)).reworkAll();
    reporting_off = true;
		return is_nn_expr(expr,nn);
	}

   public void checkNotNull(IRNode flowUnit){
     // we already have the flow unit
     getAnalysisResultsBefore(flowUnit, null);
     reporting_off = false;
     this.getAnalysis(getFlowUnit(flowUnit)).reworkAll();
     reporting_off = true;
   }
   
   boolean reporting(){
     return !reporting_off;
   }
   
	boolean is_nn_expr(IRNode expr, NonNullLattice nn){
		Operator expr_op = tree.getOperator(expr);
		return (expr_op instanceof AllocationExpression || 
				expr_op instanceof ThisExpression ||
				(VariableUseExpression.prototype.includes(expr_op) && 
					nn.contains(binder.getBinding(expr)))
          || isAnnoNonNull(expr));
	}
  
  IsAssignedNNDrop getAssignDrop(IRNode tExpr, IRNode aExpr, NonNullLattice nn){
    if(reporting_off) return null;
    boolean consistent = is_nn_expr(aExpr,nn);
    IsAssignedNNDrop nnd = new IsAssignedNNDrop(tExpr,aExpr,getNNdrops(aExpr,nn),getRawFlowUnit(tExpr),consistent);
    return nnd;
  }
  
  Collection<PromiseDrop> getNNdrops(IRNode expr, NonNullLattice nn){
    Collection<PromiseDrop> supporting = new HashSet<PromiseDrop>();
    if(is_nn_expr(expr,nn)){
      Operator expr_op = tree.getOperator(expr);
      if(VariableUseExpression.prototype.includes(expr_op) && 
          nn.contains(binder.getBinding(expr))){
        supporting.addAll(nn.supporting(expr));
      }
      else if(isAnnoNonNull(expr)){
        supporting.add(this.getNotNullDrop(expr));
      }
      else { //inherent to the node
        supporting.add(a.dropForNode(expr));
      }
    }else{//not nonNull
      supporting.add(m.getMNDrop(expr));
    }
    return supporting;
  }

  
  
  boolean isAnnoNonNull(IRNode n){
    return NotNullAnnotation.isNotNull(n) 
      || ((n=binder.getBinding(n))==null)?false:NotNullAnnotation.isNotNull(n);
  }
  
  PromiseDrop getNotNullDrop(IRNode n){
    return NotNullAnnotation.isNotNull(n)?NotNullAnnotation.getNotNullDrop(n) 
      :((n=binder.getBinding(n))==null)?null:NotNullAnnotation.getNotNullDrop(n);
}
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.INullAnalysis#maybeNull(edu.cmu.cs.fluid.ir.IRNode)
	 */
	public boolean maybeNull(IRNode expr, IRNode constructorContext) {
		return !isNonNullExpr(expr, constructorContext);
	}

}
	
	
class NonNullTransfer extends JavaForwardTransfer{

  public NonNullTransfer(NonNull nn, IBinder b){
		super(nn,b);
	}

	@Override protected Lattice transferAssignment(IRNode assign, Lattice before) {
    AssignmentInterface aop = ((AssignmentInterface)tree.getOperator(assign));
		IRNode target = aop.getTarget(assign);
    	if (VariableUseExpression.prototype.includes(tree.getOperator(target))) {
			IRNode source = aop.getSource(assign);
      		IRNode vardecl = binder.getBinding(target);
      		NonNullLattice sbefore = (NonNullLattice)before;
      		final NonNull base = (NonNull)baseAnalysis;
        if(base.is_nn_expr(source,sbefore)){
          if(base.reporting()){
            ResultDrop nnd = base.getAssignDrop(target,source,sbefore);
      		    nnd.addCheckedPromise(base.a.dropForNode(assign));
          }
          return sbefore.add(vardecl,assign);
      		}
    		else{
      			return sbefore.remove(vardecl);
      		}
    	}
    if(FieldRef.prototype.includes(tree.getOperator(target))){
      final NonNull base = (NonNull)baseAnalysis;
      final IRNode fdecl = binder.getBinding(target); 
      if(base.reporting() && base.isAnnoNonNull(fdecl)){
        final IRNode source = aop.getSource(assign);
        final NonNullLattice sbefore = (NonNullLattice)before;
        ResultDrop nnd = base.getAssignDrop(target,source,sbefore);
        PromiseDrop ann = base.getNotNullDrop(fdecl);
        nnd.addCheckedPromise(ann);
      }
    }
    	return before;
	}

	@Override protected Lattice transferInitialization(IRNode init, Lattice before) {
    	if (tree.getOperator(init) instanceof VariableDeclarator) {
			IRNode source = VariableDeclarator.getInit(init);
			if(tree.getOperator(source) instanceof Initialization){
				source = Initialization.getValue(source);
        final NonNull base = (NonNull)baseAnalysis;
        final NonNullLattice sbefore = (NonNullLattice)before;
        if(base.is_nn_expr(source,sbefore)){
          if(base.reporting()){
            ResultDrop nnd = base.getAssignDrop(init,source,sbefore);
            nnd.addCheckedPromise(base.a.dropForNode(init));
          }
          return sbefore.add(init,init);
    			} 
			}
    	}
    	return before;    
  	}

	@Override protected Lattice transferRelop(IRNode node, Operator op, boolean flag, Lattice before)
	{
		if(op instanceof InstanceOfExpression){
			IRNode var = InstanceOfExpression.getValue(node);
			if(tree.getOperator(var) instanceof VariableUseExpression){
				return flag?
					(Lattice)(((NonNullLattice)before).add(binder.getBinding(var),node)):
					before;
			}
		}
		else if(op instanceof NotEqExpression){
			IRNode lhe = NotEqExpression.getOp1(node);
			IRNode rhe = NotEqExpression.getOp2(node);
			Operator lhs = tree.getOperator(lhe);
			Operator rhs = tree.getOperator(rhe);

			if(lhs instanceof VariableUseExpression && rhs instanceof NullLiteral){
				NonNullLattice sbefore = (NonNullLattice) before;
				IRNode vardecl = binder.getBinding(lhe);
				return flag?
					(Lattice)(sbefore.add(vardecl,node)):
					(sbefore.contains(vardecl)?
						sbefore.top():
						before);
			}else if(rhs instanceof VariableUseExpression && lhs instanceof NullLiteral){
				NonNullLattice sbefore = (NonNullLattice) before;
				IRNode vardecl = binder.getBinding(rhe);
				return flag?
					(Lattice)(sbefore.add(vardecl,node)):
					(sbefore.contains(vardecl)?
						sbefore.top():
						before);
			}
		}
		else if(op instanceof EqExpression){
			IRNode lhe = EqExpression.getOp1(node);
			IRNode rhe = EqExpression.getOp2(node);
			Operator lhs = tree.getOperator(lhe);
			Operator rhs = tree.getOperator(rhe);
			if(lhs instanceof VariableUseExpression && rhs instanceof NullLiteral){
				NonNullLattice sbefore = (NonNullLattice) before;
				IRNode vardecl = binder.getBinding(lhe);
				return flag?
					(sbefore.contains(vardecl)?
						sbefore.top():
						before):
					(Lattice)sbefore.add(vardecl,node);
			}
			else if(rhs instanceof VariableUseExpression && lhs instanceof NullLiteral){
				NonNullLattice sbefore = (NonNullLattice) before;
				IRNode vardecl = binder.getBinding(rhe);
				return flag?
					(sbefore.contains(vardecl)?
						sbefore.top():
						before):
					(Lattice)sbefore.add(vardecl,node);
			}
			else if(lhs instanceof VariableUseExpression && rhs instanceof VariableUseExpression){
				NonNullLattice sbefore = (NonNullLattice)before;
				IRNode lhv = binder.getBinding(EqExpression.getOp1(node));
				IRNode rhv = binder.getBinding(EqExpression.getOp2(node));
				if(sbefore.contains(lhv)){
					return flag?
						(Lattice)sbefore.add(rhv,node):
						before;
				} else if(sbefore.contains(rhv)){
					return flag?
						(Lattice)sbefore.add(lhv,node):
						before;
				}
			}
		}
		return before;
	}

	@Override protected Lattice transferIsObject(IRNode node, boolean flag, Lattice before){

		IRNode pnode = tree.getParent(node);
		Operator op = tree.getOperator(pnode);
		NonNullLattice sbefore = (NonNullLattice)before;

		if(op instanceof MethodCall){
      MethodCall call = (MethodCall) op;
			IRNode receiver = call.get_Object(pnode);
			if(VariableUseExpression.prototype.includes(tree.getOperator(receiver))){
				IRNode vardecl = binder.getBinding(receiver);
				if (is_assigned(vardecl, call.get_Args(pnode), sbefore)){
					return (sbefore.remove(vardecl));				
				}else{
					return flag?((Lattice)sbefore.add(vardecl,node)):
						(sbefore.contains(vardecl)?before.top():before);
				}
			}
		}else if (op instanceof FieldRef){
			IRNode obj = FieldRef.getObject(pnode);
			if(VariableUseExpression.prototype.includes(tree.getOperator(obj))){
				IRNode vardecl = binder.getBinding(obj);
				return flag?((Lattice)sbefore.add(vardecl,node)):
						(sbefore.contains(vardecl)?before.top():before);
			}
		}else if (op instanceof ArrayRefExpression){
			IRNode ar = ArrayRefExpression.getArray(pnode);
			if(VariableUseExpression.prototype.includes(tree.getOperator(ar))){
				
				IRNode vardecl = binder.getBinding(ar);
				if (is_assigned(vardecl, ArrayRefExpression.getIndex(pnode), sbefore)){
					return (sbefore.remove(vardecl));				
				}else{
					return flag?((Lattice)sbefore.add(vardecl,node)):
						(sbefore.contains(vardecl)?before.top():before);
				}	
			}
		}else if (op instanceof SynchronizedStatement){
			IRNode lock = SynchronizedStatement.getLock(pnode);
			if(VariableUseExpression.prototype.includes(tree.getOperator(lock))){
				IRNode vardecl = binder.getBinding(lock);
				return flag?((Lattice)sbefore.add(vardecl,node)):
				(sbefore.contains(vardecl)?before.top():before);
			}
		}
		return before;
	}

	/* */
	@Override protected Lattice transferMethodBody(IRNode node, Port kind, Lattice value) {
		if (kind instanceof EntryPort) {
			  return value.bottom();
		} 
		return super.transferMethodBody(node, kind, value);
	}


	private boolean is_nn_expr(IRNode expr, NonNullLattice nn){
/*		Operator expr_op = tree.getOperator(expr);
		return (expr_op instanceof AllocationExpression || 
				(UseExpression.prototype.includes(expr_op) && 
					nn.contains(binder.getBinding(expr))));
*/
    return ((NonNull)baseAnalysis).is_nn_expr(expr,nn);
	}
	
	
	private boolean is_assigned(IRNode vardecl, IRNode expr, NonNullLattice nn){
		Operator op = tree.getOperator(expr);
		if(op instanceof AssignmentInterface){
			IRNode target = ((AssignmentInterface)op).getTarget(expr);
			if(VariableUseExpression.prototype.includes(tree.getOperator(target)) &&
				(vardecl == binder.getBinding(target)) && 
				!((NonNull)baseAnalysis).is_nn_expr(((AssignmentInterface)op).getSource(expr),nn)){
					return true;
				}
		}
		int nk = tree.numChildren(expr);
		boolean isa = false;
		for (int i = 0; !isa && i < nk; ++i){
			isa |= is_assigned(vardecl, tree.getChild(expr,i),nn);
		}
		return isa;
	}
	
}

final class NonNullLattice extends PartialMapLattice<IRNode, UnionLattice> {

  private final ASTDropGenerator a;
  
  protected NonNullLattice(Lattice range, HashMap<?, UnionLattice> m, PartialMapLattice t, PartialMapLattice b, boolean isTop, ASTDropGenerator ad) {
    super(range, m, t, b, isTop);
    a = ad;
  }

  @Override
  protected PartialMapLattice<IRNode, UnionLattice> newLattice(HashMap<?, UnionLattice> newValues, boolean isTop) {
    return new NonNullLattice(range,newValues,topVal,botVal,isTop,a);
  }

  public NonNullLattice(ASTDropGenerator ast) {
    super(new UnionLattice());
    a = ast;
  }

  public boolean contains(IRNode n){
    return this.containsKey(n);
  }
  
  public Collection<PromiseDrop> supporting(IRNode n){
    Collection<PromiseDrop> c = new HashSet<PromiseDrop>();
    UnionLattice u = get(n);
    for(Object o:u){
      if(o instanceof PromiseDrop){
        PromiseDrop pd = (PromiseDrop)o;
        c.add(pd);
      }
    }
    return c;
  }
  
  UnionLattice empty(){
    return (UnionLattice)range.top();
  }
  UnionLattice unsupported(){
    return (UnionLattice)range.bottom();
  }
  
  NonNullLattice remove(IRNode decl)
  {
    return (NonNullLattice)update(decl,unsupported());
  }
  UnionLattice getDropSet(IRNode n){
    return (UnionLattice)empty().addElement(a.dropForNode(n));
  }
  
  NonNullLattice add(IRNode decl, IRNode locale){
    return (NonNullLattice)update(decl,getDropSet(locale));
  }
    
  
}

final class MaybeNullDropGenerator{
  
  private static final String TEMPLATE = "{0} lacks information to prove {1} is nonNull";
  
  private final Map<IRNode,MaybeNullDrop> cache = new HashMap<IRNode,MaybeNullDrop>();
  
  public MaybeNullDrop getMNDrop(IRNode use){
    MaybeNullDrop m = cache.get(use);
    if(m == null){
      IRNode flow = IntraproceduralAnalysis.getFlowUnit(use);
      m = new MaybeNullDrop(use,flow);
    }
    return m;
  }
  
  class MaybeNullDrop extends PromiseDrop{
    private MaybeNullDrop(IRNode use, IRNode flowUnit){
      this.setNode(flowUnit);
      this.dependUponCompilationUnitOf(flowUnit);
      this.setCategory(JavaGlobals.NULL_CAT);
      String label = getMsg(use,flowUnit);
      this.setMessage(label);
      
    }
    private String getMsg(IRNode use, IRNode flowUnit){
//Code for getting string name of flow unit copied from MethodControlFlow
      final MessageFormat form = new MessageFormat(TEMPLATE);
      final Object[] args = new Object[2];

      if (flowUnit != null) {
        final Operator op = JJNode.tree.getOperator(flowUnit);
        if (ConstructorDeclaration.prototype.includes(op)
            || MethodDeclaration.prototype.includes(op)) {
          args[0] = JavaNames.genMethodConstructorName(flowUnit);

        } else {
          args[0] = DebugUnparser.toString(flowUnit);
        }
      } else {
        args[0] = "[null]";
      }
      if(use == null){
        args[1] = "[null]";
      }else{
        args[1] = DebugUnparser.toString(use);
      }
      final String label = form.format(args);
      return label;
    }
    @Override
    public boolean isIntendedToBeCheckedByAnalysis() {
      return false;
    }
  }
}
/*final class isAssignedNNDropGenerator{
  
  private final Map<IRNode,isAssignedNNDrop> cache = new HashMap<IRNode,isAssignedNNDrop>();
  private final NonNull a;
  
  isAssignedNNDropGenerator(NonNull nn){
    a = nn;
  }
  
  public isAssignedNNDrop getMNDrop(IRNode use){
    isAssignedNNDrop m = cache.get(use);
    if(m == null){
      if(a.isAnnoNonNull(use)){
        IRNode flowUnit = IntraproceduralAnalysis.getFlowUnit(use);
        m = new isAssignedNNDrop(use,flowUnit);
      }else{
        //severe problem, shoulde never happen, but...
      }
    } 
    return m;
  }
  */
  class IsAssignedNNDrop extends ResultDrop{
    IsAssignedNNDrop(IRNode use, IRNode aExpr, 
        Collection<PromiseDrop> supporting, IRNode flowUnit, boolean consistent){
      this.setNode(flowUnit);
      this.dependUponCompilationUnitOf(flowUnit);
      this.setCategory(JavaGlobals.NULL_CAT);
      String label = getMsg(use, aExpr);
      this.setMessage(label);
      this.addTrustedPromises(supporting);
      this.setConsistent(consistent);
    }
    private String getMsg(IRNode use, IRNode aExpr){
      return DebugUnparser.toString(use) + " is assigned from " + DebugUnparser.toString(aExpr);
    }
  }
//}
