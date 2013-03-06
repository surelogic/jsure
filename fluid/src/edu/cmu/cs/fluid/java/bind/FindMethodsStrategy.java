package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A Strategy for matching the method in a type
 */
public class FindMethodsStrategy 
  extends AbstractSuperTypeSearchStrategy implements ISubTypeSearchStrategy, JavaGlobals 
{	
	/*
	 * Types of the parameters (not just the names)
	 */
  private final IJavaType[] paramTypes;
  
  /*
   * The set of methods to be returned
   */
  protected final Set<IRNode> methods = new HashSet<IRNode>();
  
  public FindMethodsStrategy(IBinder bind, String mname, IJavaType[] types) {
    super(bind, "method", mname);
    paramTypes = types;
  }

  /*
   * Returns true if found
   */
  private boolean visitType(final IRNode type) {
    boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
    Iterator<IRNode> methods = VisitUtil.getClassMethods(type);

    loop : while (methods.hasNext()) {
    	final IRNode method = methods.next();
      final Operator op   = jtree.getOperator(method);
      
      if (ConstructorDeclaration.prototype.includes(op)) {
        // LOG.warn("Ignoring ConstructorDecl
				// "+ConstructorDeclaration.getName(methods[i]));
        continue;
      }
      if (AnnotationElement.prototype.includes(op)) {
        if (paramTypes.length == 0 && name.equals(AnnotationElement.getId(method))) {
          this.methods.add(method);
          return true;
        } else {
          continue;
        }
      }
      // Should be a method
      else if (name.equals(MethodDeclaration.getId(method))) {
        if (fineIsLoggable) {
          LOG.fine("Matched name against "+DebugUnparser.toString(method));
        }
        final IRNode params = MethodDeclaration.getParams(method);
        final Iterator<IRNode> p = Parameters.getFormalIterator(params);

        for (int j = 0; j < paramTypes.length; j++) {
          if (!p.hasNext()) {
            continue loop; // no match
          }
          final IRNode pd = p.next();
          // final IRNode pt = binder.mapTypeBinding(binder.getFluidBinding(pd));
          final IJavaType pt = binder.getJavaType(pd);

          if (!paramTypes[j].equals(pt)) { // FIXME is this right?
            continue loop;
          }
        }
        if (p.hasNext()) { // this method has more params than what I'm trying to match
          continue loop;
        }
        /*
		String name = JJNode.getInfoOrNull(type);
		if ("CopyOnWriteArraySet".equals(name)) {
			System.out.println("Looking at CopyOnWriteArraySet."+this.name+"["+paramTypes.length+"]");
		}
		*/
        
        //System.out.println("Added "+DebugUnparser.toString(method));
        this.methods.add(method);
        if (fineIsLoggable) {
          LOG.fine("Matched method " + name);
        }
        return true;
      }
    }
    return false;
  }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISubTypeSearchStrategy#visitClass_internal(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  public void visitClass_internal(IRNode type) {
		searchAfterLastType = !visitType(type);		
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISubTypeSearchStrategy#visitSubclasses()
	 */
	@Override
  public boolean visitSubclasses() {
		return searchAfterLastType;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ITypeSearchStrategy#getResult()
	 */
	@Override
  public Object getResult() {
		return methods.iterator();
	}
}
