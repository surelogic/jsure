package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.parse.JJNode;

interface IMethodBinder {
	BindingInfo findBestMethod(final IJavaScope scope, final LookupContext context, final boolean needMethod, final IRNode from, final CallState call);
	
	class CallState {
    	final IBinder binder;
    	final IRNode call;
    	final IRNode[] targs;		
    	final IRNode[] args; 
    	/**
    	 * Null for methods
    	 */
    	final IRNode constructorType;
    	/**
    	 * Computed from the receiver expression for methods
    	 * Derived from constructorType for constructors
    	 */
    	final IJavaType receiverType;
    	
    	/**
    	 * For methods
    	 */
    	CallState(IBinder b, IRNode call, IRNode targs, IRNode args, IJavaType recType) {
    		binder = b;
      		this.call = call;
      		this.targs = getNodes(targs);
      		this.args = getNodes(args);
      		receiverType = recType;
      		constructorType = null;
    	}
    	
    	CallState(IBinder b, IRNode call, IRNode targs, IRNode args, IRNode type) {    		
    		binder = b;
    		this.call = call;
			this.targs = getNodes(targs);
			this.args = getNodes(args);
			constructorType = type;			
	    	receiverType = type == null ? null : b.getTypeEnvironment().convertNodeTypeToIJavaType(type);
    	}
    	
    	private IRNode[] getNodes(IRNode n) {
    		if (n == null) {
    			return null;
    		}
            final int num = JJNode.tree.numChildren(n);
            final IRNode[] rv = new IRNode[num];
            Iterator<IRNode> children = JJNode.tree.children(n); 
            for (int i= 0; i < num; ++i) {
            	rv[i] = children.next();            
            }
            return rv;
    	}
    	
        // Convert the args to IJavaTypes
    	IJavaType[] getArgTypes() {
    	  if (args == null) {
    		  return JavaGlobals.noTypes;
    	  }
          if (args.length == 0) {
    		  return JavaGlobals.noTypes;
          }
          IJavaType[] argTypes = new IJavaType[args.length];     
          for (int i= 0; i < args.length; ++i) {
        	/* TODO
        	if (MethodBinder8.couldBePolyExpression(args[i])) {
        		argTypes[i] = null;
        		continue;
        	}
        	*/
            argTypes[i] = binder.getJavaType(args[i]);
            
			if (MethodBinder.captureTypes) {    		
				IJavaType temp = JavaTypeVisitor.captureWildcards(binder, argTypes[i]);    			
				if (temp != argTypes[i]) {
					/*
					String call = DebugUnparser.toString(this.call);					
					if ("Maps.transformValues(<implicit>.delegate.rowMap, wrapper)".equals(call)) {
						System.out.println("Binding call "+call);
						//System.out.println("Looking at "+JavaNames.getRelativeName(m.bind.getNode()));
						System.out.println("Captured wildcards for "+argTypes[i]);
						//debug = true;
					}
					*/
					argTypes[i] = temp;
				}
			}
          }
          return argTypes;
        }

		boolean usesDiamondOp() {
			if (constructorType != null && ParameterizedType.prototype.includes(constructorType)) { 
				IRNode typeArgs = ParameterizedType.getArgs(constructorType);
				if (JJNode.tree.numChildren(typeArgs) == 0) {
					return true;
				}
			}
			return false;
		}

		public int getNumTypeArgs() {
			return targs == null ? 0 : targs.length;
		}
    }	
}
