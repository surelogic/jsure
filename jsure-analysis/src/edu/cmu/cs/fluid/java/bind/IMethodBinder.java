package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CallInterface.NoArgs;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.VarArgsExpression;
import edu.cmu.cs.fluid.parse.JJNode;

interface IMethodBinder {
	BindingInfo findBestMethod(final IJavaScope scope, final LookupContext context, final boolean needMethod, final IRNode from, final CallState call);
	
	enum InvocationKind {
		STRICT, LOOSE, VARARGS
	}
	
	interface ICallState extends IDebugable {
		IRNode getNode();	
		
		int numArgs();
		IRNode getArgOrNull(int i);
		IJavaType getArgType(int i);
		
		int getNumTypeArgs();
		IJavaType getTypeArg(int i);
		IJavaType[] getTypeArgs();
		
		IRNode getReceiverOrNull();
		IJavaType getReceiverType();
		boolean needsVarArgs();
		boolean needsExactInvocation();

		ICallState replaceReceiverType(IJavaType newRec);
	}
	
	class CallState implements ICallState {
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
    	final IRNode receiver; 
    	
    	static CallState create(IBinder b, IRNode call, IRNode targs, IRNode args, IJavaType recType) {
    		final IRNode receiver = MethodCall.prototype.includes(call) ? MethodCall.getObject(call) : null;
    		return new CallState(b, call, targs, args, recType, receiver);
    	}
    	
    	static CallState create(IBinder binder, IRNode call, IBinding b) {
    		final CallInterface op = (CallInterface) JJNode.tree.getOperator(call);
    		try {
    			return create(binder, call, op.get_TypeArgs(call), op.get_Args(call), b.getReceiverType());
    		} catch (NoArgs e) {
    			throw new IllegalStateException("No args");
    		}
    	}    	
    	
    	/**
    	 * For methods
    	 */
    	CallState(IBinder b, IRNode call, IRNode targs, IRNode args, IJavaType recType, IRNode rec) {
    		binder = b;
      		this.call = call;
      		this.targs = getNodes(targs);
      		this.args = getNodes(args);
      		receiverType = recType;
      		receiver = rec;
      		constructorType = null;
    	}
    	
    	CallState(IBinder b, IRNode call, IRNode targs, IRNode args, IRNode type) {    		
    		binder = b;
    		this.call = call;
			this.targs = getNodes(targs);
			this.args = getNodes(args);
			constructorType = type;			
	    	receiverType = type == null ? null : b.getTypeEnvironment().convertNodeTypeToIJavaType(type);
	    	receiver = null;
    	}
    	
    	CallState(CallState o, IJavaType recType) {
    		binder = o.binder;
      		call = o.call;
      		targs = o.targs;
      		args = o.args;
      		receiverType = recType;
      		receiver = o.receiver;
      		if (o.constructorType != null) {
      			throw new IllegalStateException("Nonnull constructorType : "+o.constructorType);
      		}
      		constructorType = null;
		}

    	public ICallState replaceReceiverType(IJavaType newRecT) {
    		return new CallState(this, newRecT);
    	}
    	
        public boolean needsExactInvocation() {
            return false;
        }
        
        @Override
        public boolean equals(Object o) {
        	if (o instanceof ICallState) {
        		ICallState c = (ICallState) o;
        		return call.equals(c.getNode());
        	}
        	return false;
        }
        
        @Override 
        public String toFullyQualifiedText() {
        	return toSourceText(); // TODO
        }
	     
		@Override
		public String toSourceText() {
			StringBuilder sb = new StringBuilder(JJNode.getInfoOrNull(call));
			sb.append('(');
			boolean first = true;
			for(IRNode arg : args) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(DebugUnparser.toString(arg, 3));
			}
			sb.append(')');			
			return sb.toString();
		}
		
		@Override
    	public String toString() {
    		return DebugUnparser.toString(call);
    	}
    	
    	private IRNode[] getNodes(IRNode n) {
    		if (n == null) {
    			return JavaGlobals.noNodes;
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
        	  argTypes[i] = computeArgType(args[i]);
          }
          return argTypes;
        }

    	private IJavaType computeArgType(IRNode arg) {
        	IJavaType rv = binder.getJavaType(arg);
            
			if (MethodBinder.captureTypes) {    		
				IJavaType temp = JavaTypeVisitor.captureWildcards(binder, rv);    			
				if (temp != rv) {
					/*
					String call = DebugUnparser.toString(this.call);					
					if ("Maps.transformValues(<implicit>.delegate.rowMap, wrapper)".equals(call)) {
						System.out.println("Binding call "+call);
						//System.out.println("Looking at "+JavaNames.getRelativeName(m.bind.getNode()));
						System.out.println("Captured wildcards for "+argTypes[i]);
						//debug = true;
					}
					*/
					rv = temp;
				}
			}
			return rv;
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
    	
    	public IRNode getNode() {
    		return call;
    	}
		
		public int numArgs() {
			return args == null ? 0 : args.length;
		}

		public IRNode getArgOrNull(int i) {
			return args == null ? null : args[i];
		}

		public IJavaType getArgType(int i) {
			return computeArgType(args[i]);
		}

		public IJavaType getTypeArg(int i) {
			return binder.getJavaType(targs[i]);
		}

		public IJavaType[] getTypeArgs() {
			if (targs == null || targs.length == 0) {
				return JavaGlobals.noTypes;
			}
			IJavaType[] rv = new IJavaType[targs.length];
			for(int i=0; i<targs.length; i++) {
				rv[i] = getTypeArg(i);
			}
			return rv;
		}

		public IRNode getReceiverOrNull() {
			return receiver;
		}
		
		public IJavaType getReceiverType() {
			return receiverType;
		}

		public boolean needsVarArgs() {
			if (args.length == 0) {
				return false;
			}
			IRNode lastArg = args[args.length-1];			
			return VarArgsExpression.prototype.includes(lastArg);
		}
    }	
	
	class MethodBinding extends MethodInfo implements IBinding {
    	final IBinding bind;
    	
    	MethodBinding(IBinding mb) {
    		super(mb.getNode());
    		bind = mb;
    	}
    	
    	@Override
    	public int hashCode() {
    		return mdecl.hashCode() + bind.getContextType().hashCode();
    	}
    	
    	@Override 
    	public boolean equals(Object o) {
    		if (o instanceof MethodBinding) {
    			final MethodBinding mo = (MethodBinding) o;
    			return mdecl.equals(mo.mdecl) && bind.getContextType().equals(mo.bind.getContextType());
    		}
    		return false;
    	}
    	/*
        @Override
        public String toString() {
        	return super.toString()+" from "+mb.getContextType();
        }
        */
    	@Override
    	IJavaType getJavaType(IBinder b, IRNode f, boolean withSubst) {
    		IJavaType t = super.getJavaType(b, f, withSubst);
    		if (withSubst) {
    			return bind.convertType(b, t);
    		}
    		return t;
    	}
    	    	
        final IJavaType getReturnType(ITypeEnvironment tEnv) {
        	return getReturnType(tEnv, true);
        }
        
        final IJavaType getReturnType(ITypeEnvironment tEnv, boolean withSubst) {
    		IJavaType base = JavaTypeVisitor.getJavaType(mdecl, tEnv.getBinder());
    		if (withSubst) {
    			return bind.convertType(tEnv.getBinder(), base);
    		}
    		return base;
    	}

		@Override
		public IRNode getNode() {
			return bind.getNode();
		}

		@Override
		public IJavaDeclaredType getContextType() {
			return bind.getContextType();
		}

		@Override
		public IJavaReferenceType getReceiverType() {
			return bind.getReceiverType();
		}

		@Override
		public IJavaType convertType(IBinder binder, IJavaType ty) {
			return bind.convertType(binder, ty);
		}

		@Override
		public IJavaTypeSubstitution getSubst() {
			return bind.getSubst();
		}
	}
}
