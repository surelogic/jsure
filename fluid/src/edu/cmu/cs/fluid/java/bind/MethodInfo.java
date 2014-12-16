package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import com.surelogic.common.util.FilterIterator;

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
	
	IJavaType getJavaType(IBinder b, IRNode f, boolean withSubst) {
		return b.getJavaType(f);
	}
	
	/**
	 * @return the parameter types that should match the call arguments;
	 */
	IJavaType[] getParamTypes(IBinder b, int callArgs, boolean varArity) {
		return getParamTypes(b, callArgs, varArity, true);
	}
	
    IJavaType[] getParamTypes(IBinder b, int callArgs, boolean varArity, boolean withSubst) {
    	IJavaType[] rv = new IJavaType[callArgs];
    	int i=0;
		for(IRNode f : Parameters.getFormalIterator(formals)) {
			if (varArity && i == callArgs) {
				// Empty varargs
				break; 
			}
			rv[i] = getJavaType(b, f, withSubst);
			i++;
		}
    	if (varArity && callArgs >/*=*/ numFormals) {
    		// Fill-in the rest of the types needed
    		IJavaArrayType origType = (IJavaArrayType) rv[numFormals-1];
    		IJavaType varargsType = origType.getElementType();
    		for(i=numFormals-1; i<callArgs; i++) {
    			rv[i] = varargsType;
    		}
    	}
    	// Check for nulls
    	if (false) {
    		int numNulls = 0;    	
    		for(IJavaType t : rv) {
    			if (t == null) {
    				numNulls++;
    			}
    		}
    		if (numNulls > 1) {
    			throw new IllegalStateException();    		
    		}
    	}
		return rv;
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
    
    Iterable<IJavaType> getThrownExceptions(final IBinder b) {
		IRNode thrown;
		if (isConstructor) {
			thrown = ConstructorDeclaration.getExceptions(mdecl);
		} else {
			thrown = MethodDeclaration.getExceptions(mdecl);
		}
		return new FilterIterator<IRNode,IJavaType>(Throws.getTypeIterator(thrown)) {
			@Override
			protected Object select(IRNode o) {				
				return b.getJavaType(o);
			}
			
		};
	}
    
    List<IJavaTypeFormal> getTypeFormals() {
    	List<IJavaTypeFormal> rv = new ArrayList<IJavaTypeFormal>(numFormals);
    	for(IRNode tf : JJNode.tree.children(typeFormals)) {
    		rv.add(JavaTypeFactory.getTypeFormal(tf));
    	}
		return rv;
	}
}
