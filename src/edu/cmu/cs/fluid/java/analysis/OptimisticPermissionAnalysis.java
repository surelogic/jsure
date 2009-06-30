/*
 * $header$
 * Created on Jan 11, 2005
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.CachedProceduralAnalysis.AssuranceLoggerResults;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A simple tree-walking version of optimistic permission analysis. This is a
 * "level 1" optimistic analysis: we don't use the CFG.
 * <P>
 * This class is not implemented yet. What is here is only a sketch.
 * @author boyland
 */
@Deprecated
public class OptimisticPermissionAnalysis extends CachedProceduralAnalysis<CoarsePermissionLattice,AssuranceLoggerResults<CoarsePermissionLattice>> {

  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.permissions.opt1");
  private final UnassignedVariables unassigned;
  
  /**
   * Create an unassigned variables analysis.
   * 
   * @param b
   */
  public OptimisticPermissionAnalysis(IBinder b)
       throws SlotAlreadyRegisteredException {
    super("fluid.java.permission.opt1", null, b);
    unassigned = UnassignedVariables.getInstance(b);
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.java.analysis.CachedModularAnalysis#computeResults(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected AssuranceLoggerResults<CoarsePermissionLattice> computeResults(IRNode proc) {
    UnassignedVariables.Results unassignedResults = unassigned.getResults(proc);
    AssuranceLogger logger = new AssuranceLogger("Permissions(opt 1)");
    Map<IRNode,CoarsePermissionLattice> exprResults = new HashMap<IRNode,CoarsePermissionLattice>();
    Visitor v = new Visitor(logger,unassignedResults,exprResults);
    v.doAccept(proc);
    return new AssuranceLoggerResults<CoarsePermissionLattice>(proc,null, logger);
  }

  public static boolean isLValue(IRNode node) {
    //! TODO: find this function somewhere.  Should be easy.
    return false;
  }
  private static final IRNode arrayElementRegion = null; //TODO: initialize
  @Deprecated
  class Visitor extends ProcedureVisitor<CoarsePermissionLattice> {
    private final AssuranceLogger logger;
    private final UnassignedVariables.Results unassignedResults;
    private final Map<IRNode,CoarsePermissionLattice> initialValues = new HashMap<IRNode,CoarsePermissionLattice>();
    private final Map<IRNode,CoarsePermissionLattice> exprResults;
    
    // TODO: pass in the permissions for this procedure too: used in checkEffect
    
    public Visitor(AssuranceLogger l, UnassignedVariables.Results ur,Map<IRNode,CoarsePermissionLattice> er) {
      logger = l;
      unassignedResults = ur;
      exprResults = er;
    }
    
    @Override
    public CoarsePermissionLattice doAccept(IRNode node) {
      CoarsePermissionLattice result = super.doAccept(node);
      exprResults.put(node, result);
      return result;
    }



    private void checkEffect(IRNode locus, IRNode object, IRNode region, boolean isWrite) {
      // TODO:implement this method
    }
    
   @Override
  public CoarsePermissionLattice visitVariableDeclarator(IRNode node) {
      CoarsePermissionLattice result = super.visit(node);
      if (unassignedResults.getSlotValue(node) == Boolean.TRUE) {
        initialValues.put(node,result);
      }
      return null;
    }
        
    @Override
    public CoarsePermissionLattice visitAnonClassExpression(IRNode node) {
      return visitCall(node);
    }
    @Override
    public CoarsePermissionLattice visitArguments(IRNode node) {
      // TODO Auto-generated method stub
      return super.visitArguments(node);
    }
    @Override
    public CoarsePermissionLattice visitArrayRefExpression(IRNode node) {
      IRNode anode = ArrayRefExpression.getArray(node);
      CoarsePermissionLattice arrayLat = doAccept(anode);
      doAccept(ArrayRefExpression.getIndex(node));
      boolean lvalue = isLValue(node);
      if (!arrayLat.equals(CoarsePermissionLattice.UNIQUE)) {
        checkEffect(node,anode,arrayElementRegion,lvalue);
      }
      //?  We are *very* conservative.
      //? Doing better requires CoarsePermissionLattice to have array typed stuff.
      if (lvalue) {
        return CoarsePermissionLattice.USELESS;
      } else {
        return CoarsePermissionLattice.UNIQUE;
      }
    }
    @Override
    public CoarsePermissionLattice visitAssignExpression(IRNode node) {
      CoarsePermissionLattice lhs = doAccept(AssignExpression.getOp1(node));
      CoarsePermissionLattice rhs = doAccept(AssignExpression.getOp2(node));

      if (!rhs.includes(lhs)) {
        logger.reportNegativeAssurance("Assignment does not meet reference annotation requirements: " +
            lhs + " := " + rhs, node);
      } else {
        logger.reportPositiveAssurance("Assignment may meet reference annotation requirements",node);
      }
      return rhs;
    }
    @Override
    public CoarsePermissionLattice visitBinopExpression(IRNode node) {
      super.visit(node);
      return CoarsePermissionLattice.IMMUTABLE;
    }
    @Override
    public CoarsePermissionLattice visitCall(IRNode node) {
      IRNode decl = binder.getBinding(node);
      // TODO: form formal -> actual mapping
      // TODO: Use it to map annotations (ownership!) from formals to actuals
      // TODO: Use also for effect mapping.
      // visit receiver and check lattice value
      // visit the arguments and check lattice values
      // TODO check effects too
      IRNode rdecl = JavaPromise.getReturnNode(decl);
      // Hmm. not right for constructors etc.
      return CoarsePermissionLattice.fromAnnotation(rdecl);
    }
    @Override
    public CoarsePermissionLattice visitFieldRef(IRNode node) {
      boolean lvalue = isLValue(node);
      IRNode field = binder.getBinding(node);
      IRNode object = FieldRef.getObject(node);
      CoarsePermissionLattice oval = doAccept(object);
      CoarsePermissionLattice fval = CoarsePermissionLattice.fromAnnotation(field);
      if (!oval.includes(CoarsePermissionLattice.UNIQUEWRITE)) {
        checkEffect(node,object,field,lvalue);
      }
      return fval; 
     }
    @Override
    public CoarsePermissionLattice visitLiteralExpression(IRNode node) {
      return CoarsePermissionLattice.IMMUTABLE;
    }
    @Override
    public CoarsePermissionLattice visitMethodCall(IRNode node) {
      return visitCall(node);
    }
    @Override
    public CoarsePermissionLattice visitNewExpression(IRNode node) {
      return visitCall(node);
    }
    @Override
    public CoarsePermissionLattice visitNullLiteral(IRNode node) {
      return CoarsePermissionLattice.UNIQUE;
    }
    @Override
    public CoarsePermissionLattice visitParenExpression(IRNode node) {
      return super.visit(node);
    }
    @Override
    public CoarsePermissionLattice visitQualifiedSuperExpression(IRNode node) {
      return visitUseExpression(node);
    }
    @Override
    public CoarsePermissionLattice visitQualifiedThisExpression(IRNode node) {
      return visitUseExpression(node);
    }
    @Override
    public CoarsePermissionLattice visitReturnStatement(IRNode node) {
      IRNode returnNode = ReturnStatement.getValue(node);
      CoarsePermissionLattice rval = doAccept(returnNode);
      IRNode returnDecl = binder.getBinding(node);
      CoarsePermissionLattice required = CoarsePermissionLattice.fromAnnotation(returnDecl);
      if (rval.includes(required)) {
        logger.reportPositiveAssurance("Return may meet reference annotation requirement",node);
      } else {
        logger.reportNegativeAssurance("Return does not meet reference annotation requirement" +
            "returning " + rval + " as " + required, node);
      }
      return null;
    }
    @Override
    public CoarsePermissionLattice visitSuperExpression(IRNode node) {
      return visitUseExpression(node);
    }
    @Override
    public CoarsePermissionLattice visitThisExpression(IRNode node) {
      return visitUseExpression(node);
    }
    @Override
    public CoarsePermissionLattice visitUnopExpression(IRNode node) {
      super.visit(node);
      return CoarsePermissionLattice.IMMUTABLE;
    }

    public CoarsePermissionLattice visitUseExpression(IRNode node) {
      IRNode decl = binder.getBinding(node);
      boolean lvalue = isLValue(node);
      if (unassignedResults.getSlotValue(node) == Boolean.TRUE) {
        Operator op = JJNode.tree.getOperator(decl);
        if (op instanceof ParameterDeclaration || op instanceof ReceiverDeclaration) {
          return CoarsePermissionLattice.fromAnnotation(node);
        } else if (op instanceof VariableDeclarator) {
          return initialValues.get(decl);
        } else {
          LOG.warning("Unknown declaration for use " + op);
        }
      }
      return lvalue ? CoarsePermissionLattice.USELESS : CoarsePermissionLattice.UNIQUE;
    }
  }

  @Override
  protected CachedProceduralAnalysis.AssuranceLoggerResults<CoarsePermissionLattice> makePlaceholder() {
    return new AssuranceLoggerResults<CoarsePermissionLattice>(null,null, null);
  }
}