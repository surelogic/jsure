package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

class MethodInfo {
	final IRNode mdecl;
	final IRNode formals;
	final IRNode typeFormals;
	final int numTypeFormals;
	final boolean isConstructor;
	final int numFormals;
	
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
    		isConstructor = false;
    	} else {
    		formals = ConstructorDeclaration.getParams(mdecl);
    		typeFormals = ConstructorDeclaration.getTypes(mdecl);
    		isConstructor = true;
    	}
    	numTypeFormals = AbstractJavaBinder.numChildrenOrZero(typeFormals);
    	numFormals = AbstractJavaBinder.numChildrenOrZero(formals);
	}
	
	int getNumFormals() {
		return numFormals;
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
    
    /**
     * @return true if the return type T is a type parameter for the method
     */
    boolean hasTypeParameterAsReturnType(IBinder b) {
    	if (isConstructor) {
    		return false;
    	}
    	IRNode rtype = MethodDeclaration.getReturnType(mdecl);
    	if (VoidType.prototype.includes(rtype)) {
    		return false;
    	}
    	IJavaType rt = b.getJavaType(rtype);
    	if (rt instanceof IJavaTypeFormal) {
    		IJavaTypeFormal tf = (IJavaTypeFormal) rt;
    		IRNode enclosingDecl = VisitUtil.getEnclosingClassBodyDecl(tf.getDeclaration());
    		return mdecl.equals(enclosingDecl);
    	}
		return false;
	}
    
    @Override
    public String toString() {
    	return JavaNames.genQualifiedMethodConstructorName(mdecl);
    }
}
