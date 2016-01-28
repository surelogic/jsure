package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Visits the body of a lambda
 * 
 * @author edwin
 */
// Should this really use JavaSemanticsVisitor?
public abstract class AbstractLambdaVisitor<T> extends TreeWalkVisitor<T> {
	private final T ignoreValue;
	
	AbstractLambdaVisitor(T ignore) {
		ignoreValue = ignore;
	}
	
	abstract T merge(T v1, T v2);
	
	final T ignore() {
		return ignoreValue;
	}
	
    @Override
    public final T visitAnonClassExpression(IRNode ace) {
        T rv = doAccept(AnonClassExpression.getAlloc(ace));
        for (IRNode m : VisitUtil.getClassBodyMembers(ace)) {
          final Operator op = JJNode.tree.getOperator(m);
          if (!TypeUtil.isStatic(m)) {
        	// TODO is this right?
            continue;
          }
          if (FieldDeclaration.prototype.includes(op)) {
            rv = merge(rv, doAccept(FieldDeclaration.getVars(m)));
          } else if (ClassInitializer.prototype.includes(op)) {
            rv = merge(rv, doAccept(ClassInitializer.getBlock(m)));
          }
          // Otherwise, ignore it
        }
        return rv;
      }
    
    @Override
    public final T visitLambdaExpression(IRNode e) {
      return ignore();
    }
    
    @Override
    public final T visitTypeDeclarationStatement(IRNode s) {
      return ignore();
    }
}
