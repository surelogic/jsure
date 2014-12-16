/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/CachingBinder.java,v 1.8 2008/09/05 19:39:56 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class CachingBinder extends AbstractBinder implements JavaCanonicalizer.IBinderCache {
	protected static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");

	private final IBinder orig;
	private final Map<IRNode,IBinding> bindingCache = new ConcurrentHashMap<IRNode,IBinding>();
	private final Map<IRNode,IJavaType> typeCache = new ConcurrentHashMap<IRNode,IJavaType>();
	private final Set<IRNode> activeCUs           = new HashSet<IRNode>();
	
	private CachingBinder(IBinder b) {
		super(b);
		orig = b;
	}

	public static IBinder create(IBinder b) {
	    if (b instanceof CachingBinder) return b;
	    return new CachingBinder(b);
	}
	
	@Override
	public ITypeEnvironment getTypeEnvironment() {
		return orig.getTypeEnvironment();
	}
	
	@Override
  protected IBinding getIBinding_impl(IRNode node) {
		/*
		IBinding b = bindingCache.get(node);
		if (b != null) {
			return b;
		}
		*/
		return orig.getIBinding(node);
	}

	@Override
	public IJavaType getJavaType(IRNode node) {
		/*
		IJavaType t = typeCache.get(node);
		if (t != null) {
			return t;
		}
		*/
		return orig.getJavaType(node);
	}
	
	@Override
  public void init(final IRNode tree) {		
		final boolean isUJB = orig instanceof UnversionedJavaBinder;
		synchronized (activeCUs) {
			if (activeCUs.contains(tree)) {
				// Already initialized?
				return;
			}
			activeCUs.add(tree);
		}
		for(IRNode n : JJNode.tree.bottomUp(tree)) {
			Operator op = JJNode.tree.getOperator(n);
			//if (!isUJB) { 
			if (true) {
				if (op instanceof IHasBinding) {
				    boolean goAhead = true;
				    if (EnumConstantDeclaration.prototype.includes(op)) {
				        goAhead &= !AbstractJavaBinder.isBinary(n);
				    }
				    if (goAhead) {
				        IBinding b = orig.getIBinding(n);
				        if (b != null) {
				            bindingCache.put(n, b);					
				        }					
				    }
				}
			}
			if (op instanceof IHasType) {
				IJavaType t = orig.getJavaType(n);
				if (t != null) {
					typeCache.put(n, t);
				}
			}
		}
		
		if (isUJB) {
			UnversionedJavaBinder ub = (UnversionedJavaBinder) orig;
			//ub.getBindings(bindingCache, tree);
			ub.setBinderCache(this);
		}
	}

	@Override
  public IBinding checkForBinding(IRNode node) {
		return bindingCache.get(node);
	}
	
	@Override
  public IJavaType checkForType(IRNode node) {
		return typeCache.get(node);
	}
	
	@Override
  public void map(IRNode old, IRNode now) {
		if (old.equals(now)) {
			return;
		}
		//String label = "nothing";
		IBinding b = bindingCache.get(old);
		if (b != null) {
			bindingCache.put(now, b);
			//label = "binding";
		}
		IJavaType t = typeCache.get(old);
		if (t != null) {
			typeCache.put(now, t);
			//label = label == "nothing" ? "type   " : "both   ";
		}
		//System.out.println("Mapped "+label+" for "+now);
	}
	
	@Override
  public void addBinding(IRNode node, IBinding b) {
		// FIX check if null / already there?		
		/*
		if (b != null) {
			System.out.println("Added binding  for "+node);
		}
		*/
		bindingCache.put(node, b);
	}
	
	@Override
  public void finish(IRNode tree) {
		synchronized (activeCUs) {
			activeCUs.remove(tree);
			if (activeCUs.isEmpty()) {
				bindingCache.clear();
				typeCache.clear();
				if (orig instanceof UnversionedJavaBinder) {
					UnversionedJavaBinder ub = (UnversionedJavaBinder) orig;
					ub.setBinderCache(null);
				}
			}
		}
	}
}
