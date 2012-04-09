package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A Strategy for matching a single method in a type
 */
public class FindOverriddenMethodsStrategy extends FindMethodsStrategy implements JavaGlobals
{	
	/*
	 * Find all matching methods in ancestors, or just immediately overridden ones
	 */
	private final boolean findAll;
	
	/*
	 * Used to skip the first type to find the overridden methods
	 */
	private boolean atFirstType = true;
	
	public FindOverriddenMethodsStrategy(IBinder bind, IRNode method, boolean all) {
		super(bind, getName(method), getParamTypes(bind, method));
    //System.out.println("Looking to match "+DebugUnparser.toString(method));
		findAll = all;
	}

	static String getName(IRNode method) {
		Operator op = jtree.getOperator(method);
		if (MethodDeclaration.prototype.includes(op)) {
  		return MethodDeclaration.getId(method);
		}
		// LOG.info("Trying to find the methods overridden by a constructor");
		return "<n/a>";
	}
	
	/** Return the types corresponding to the various parameters */
	static IJavaType[] getParamTypes(IBinder bind, IRNode method) {
		Operator op = JJNode.tree.getOperator(method);
		if (!MethodDeclaration.prototype.includes(op)) {
		  return noTypes;
		}
		List<IJavaType> types    = new ArrayList<IJavaType>();
		Iterator<IRNode> e = Parameters.getFormalIterator(MethodDeclaration.getParams(method));
		while (e.hasNext()) {
			// FIX dims?
			// ParameterDeclaration.getType();
			types.add(bind.getJavaType(e.next()));
		}
		return types.toArray(new IJavaType[types.size()]);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISubTypeSearchStrategy#visitClass_internal(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  public void visitClass_internal(IRNode type) {
		if (atFirstType) {
			atFirstType = false;
			searchAfterLastType = true;
			return;
		}
        /*
		String name = JJNode.getInfoOrNull(type);
		if ("CopyOnWriteArraySet".equals(name)) {
			System.out.println("Looking at method for CopyOnWriteArraySet");
		}
		*/
		super.visitClass_internal(type);
		if (findAll) {
      LOG.fine("Still searching after "+JavaNames.getTypeName(type));
			searchAfterLastType = true;
		}
	}
}
