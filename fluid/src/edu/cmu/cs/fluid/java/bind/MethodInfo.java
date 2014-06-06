package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

class MethodInfo {
	final IRNode mdecl;
	final IRNode formals;
	final IRNode typeFormals;
	final int numTypeFormals;
	
	MethodInfo(IRNode m) {
		mdecl = m;
    	final Operator op  = JJNode.tree.getOperator(mdecl);      
    	if (op instanceof MethodDeclaration) {
    		formals = MethodDeclaration.getParams(mdecl);
    		typeFormals = MethodDeclaration.getTypes(mdecl);
    		/*
	        if ("toArray".equals(MethodDeclaration.getId(mdecl))) {
	        	System.out.println(DebugUnparser.toString(mdecl));
	        	System.out.println();
	        }
    		 */
    	} else {
    		formals = ConstructorDeclaration.getParams(mdecl);
    		typeFormals = ConstructorDeclaration.getTypes(mdecl);
    	}
    	numTypeFormals = AbstractJavaBinder.numChildrenOrZero(typeFormals);
	}
	
	final IRNode getVarargsType() {
		IRNode varType;
    	IRLocation lastLoc = JJNode.tree.lastChildLocation(formals);
    	if (lastLoc != null) {
    		IRNode lastParam = JJNode.tree.getChild(formals,lastLoc);
    		IRNode ptype = ParameterDeclaration.getType(lastParam);
    		if (VarArgsType.prototype.includes(ptype)) {
    			/*
    			if (debug) {
    				LOG.finer("Handling variable numbers of parameters.");
    			}
    			*/
    			varType = ptype;
    		} else {
    			varType = null;
    		}
    	} else {
    		varType = null;
    	}
    	return varType;
	}
	
	final boolean isVariableArity() {
		return getVarargsType() != null;
    }
    
    final boolean isGeneric() {
    	return numTypeFormals > 0;
    }
    
    boolean hasTypeParameterAsReturnType() {
		// TODO Auto-generated method stub
		throw new NotImplemented();
	}
}
