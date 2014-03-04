package edu.cmu.cs.fluid.java;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.ComponentFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.parse.JJOperator;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.util.IterableThreadLocal;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;

public final class JavaComponentFactory implements ComponentFactory {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("FLUID.java.control");
  
  /*
   * TODO Could this be split up into thread-local maps?
   * (if it doesn't matter that they use the same thing and they mostly don't overlap anyways)
   */
  private static final JavaComponentFactory prototype = new JavaComponentFactory();

  /**
	 * Each syntax node potentially has a control component. This information is
	 * stored in the following table. The structures are transient, and so there
	 * is no need to use slots. @type Hashtable[IRNode,Component]
	 */
  private static final ConcurrentMap<IRNode, Component> components = new ConcurrentHashMap<IRNode, Component>();

  /**
   * Lock that determines whether I can clear 'components'
   */
  private static final ReentrantReadWriteLock activeLock = new ReentrantReadWriteLock();

  private static final CopyOnWriteArrayList<IntraproceduralAnalysis<?, ?, ?>> analyses = 
		  new CopyOnWriteArrayList<IntraproceduralAnalysis<?,?,?>>();
  
  public static void registerAnalysis(IntraproceduralAnalysis<?, ?, ?> a) {
	  analyses.add(a);
  }
  
  // TODO remove!
  
  public static void clearCache() {
	  try {
		  activeLock.writeLock().lock();
		  clear();			  
	  } finally {
		  activeLock.writeLock().unlock(); 
	  }		  
  }
  
  private static void clear() {
	  components.clear();  
	  
	  // Otherwise, they'll hang onto the info
	  for(IntraproceduralAnalysis<?, ?, ?> a : analyses) {
		  a.clear();
	  }
  }

  private static boolean isLowOnMemory() {
	  // TODO
	  return components.size() > 100000;
  }
  
  /**
   * Marks the beginning of a given thread's use of components,
   * so we can't GC while there are outstanding use
   */
  public static JavaComponentFactory startUse() {
	  // Low and not already holding the read lock
	  if (isLowOnMemory() && activeLock.getReadHoldCount() == 0) {		  		  
		  try {
			  activeLock.writeLock().lock();
			  if (isLowOnMemory()) {
			    System.out.println("~~~~~~ Clearing component cache ~~~~~~");
				  clear();			  
			  }
		  } finally {
			  // This should unblock everyone		  
			  activeLock.readLock().lock();
			  activeLock.writeLock().unlock(); // Unlock write, still hold read
		  }		  
	  } else {
		  activeLock.readLock().lock();
	  }
	  return prototype;
  }
  
  /**
   * Marks the end of a given thread's use of components,
   * specifically so they can be garbage collected
   */
  public static void finishUse(JavaComponentFactory factory) {	  
	  activeLock.readLock().unlock();
  }
  
  @Override
  public Component getComponent(IRNode node) {
    return getComponent(node, false);
  }
  
  public Component getComponent(IRNode node, boolean quiet) {
    final Component comp = components.get(node);
    if (comp == null)
      return prototype.createComponent(node, quiet);
    else
      return comp;
  }
  
  // No sync needed due to concurrent hash map
  private Component createComponent(final IRNode node, boolean quiet) {
    JavaOperator op = (JavaOperator) JJNode.tree.getOperator(node);
    Component comp = op.createComponent(node);
    if (comp != null) {
      if (LOG.isLoggable(Level.WARNING) && !comp.isValid()) {
        LOG.warning("invalid component for");
        JavaNode.dumpTree(System.out, node, 1);
      }
      comp.registerFactory(this);
	  Component old = components.putIfAbsent(node, comp);
	  if (old != null) {
		  // Already created by someone else
		  return old;
	  }
    } else if (!quiet) {
      LOG.warning(
        "Null control-flow component for " + DebugUnparser.toString(node) + " (OP = " + JJNode.tree.getOperator(node) + ")");
      IRNode here = node;
      while ((here = JJNode.tree.getParentOrNull(here)) != null) {
        LOG.warning(
          "  child of " + JJNode.tree.getOperator(here).name() + " node");
      }
      LOG.log(Level.WARNING, "Just want the trace", new FluidError("dummy"));
    }
    return comp;
  }

  @Override
  public SyntaxTreeInterface tree() {
    return JJOperator.tree;
  }
}

final class ThreadLocalComponentFactory implements ComponentFactory {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("FLUID.java.control");	
	
	private static final IterableThreadLocal<JavaComponentFactory> factories = new IterableThreadLocal<JavaComponentFactory>() {	
		@Override
		protected JavaComponentFactory makeInitialValue() {
			return new JavaComponentFactory();
		}
	};	

	// Thread-local components
	private final Map<IRNode, Component> components = new HashMap<IRNode, Component>();

	@Override
	public Component getComponent(IRNode node) {
		return getComponent(node, false);
	}

	public Component getComponent(IRNode node, boolean quiet) {
		final Component comp = components.get(node);
		if (comp == null)
			return createComponent(node, quiet);
		else
			return comp;
	}

	// No sync needed due to thread-local hash map
	private Component createComponent(final IRNode node, boolean quiet) {
		JavaOperator op = (JavaOperator) JJNode.tree.getOperator(node);
		Component comp = op.createComponent(node);
		if (comp != null) {
			if (LOG.isLoggable(Level.WARNING) && !comp.isValid()) {
				LOG.warning("invalid component for");
				JavaNode.dumpTree(System.out, node, 1);
			}
			comp.registerFactory(this);
			components.put(node, comp);
		} else if (!quiet) {
			LOG.warning(
					"Null control-flow component for " + DebugUnparser.toString(node) + " (OP = " + JJNode.tree.getOperator(node) + ")");
			IRNode here = node;
			while ((here = JJNode.tree.getParentOrNull(here)) != null) {
				LOG.warning(
						"  child of " + JJNode.tree.getOperator(here).name() + " node");
			}
			LOG.log(Level.WARNING, "Just want the trace", new FluidError("dummy"));
		}
		return comp;
	}	
	
	@Override
	public SyntaxTreeInterface tree() {
		return JJOperator.tree;
	}
}