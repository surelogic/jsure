/*
 * $header$
 * Created on Jan 10, 2005
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.DerivedSlotInfo;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.Version;

/**
 * Abstract class for modular analysis.
 * This class performs analysis for one procedure at a time.  A ``procedure''
 * corresponds to a 'flowUnit' and is one of the following
 * <ul>
 * <li> A method declaration.
 * <li> A constructor declaration.
 * <li> A class declaration (for the static initialization of a class).
 *   <p>
 *         (Instance initialization occurs in the context of a constructor.
 *          If the constructor being used is unclear, we choose the lexically first
 *          constructor.)
 *          
 * </ul>
 * <p>
 * The information stored with the results include analysis information in the form
 * of slot information, and also extra information associated with the analysis as a whole.
 * @param T the type of the slot information
 * @param E the element type for the iterable collection of information.
 * @author boyland
 */
public abstract class CachedProceduralAnalysis<T,R extends CachedProceduralAnalysis.Results<T>> extends DerivedSlotInfo<T> {
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis");
  public static final int DEFAULT_CACHE_SIZE = 3;
  protected final IBinder binder;
  protected final int maxVersionsCached;
  private final Version[] versions; // protected by this
  private final Map<IRNode,R>[] results; // protected by this, tables protected by themselves
  
  /**
   * Create a new anonymous untyped slot info for the analysis.
   * @param b binder to use for analysis
   */
  public CachedProceduralAnalysis(IBinder b) {
    this(b,DEFAULT_CACHE_SIZE);
  }
  
  @SuppressWarnings("unchecked")
  public CachedProceduralAnalysis(IBinder b, int cacheSize) {
    binder = b;
    maxVersionsCached = cacheSize;
    versions = new Version[cacheSize];
    results = new Map[cacheSize];
  }
  
  /**
   * Create a named registered slot info for this analysis.
   * @param b binder to use for analysis
   * @param name
   * @param type
   * @throws SlotAlreadyRegisteredException
   */
  public CachedProceduralAnalysis(String name, IRType type, IBinder b)
      throws SlotAlreadyRegisteredException {
    this(name,type,b,DEFAULT_CACHE_SIZE);
  }
  
  /**
   * Create a named registered slot info for this analysis.
   * @param b binder to use for analysis
   * @param name
   * @param type
   * @throws SlotAlreadyRegisteredException
   */
  @SuppressWarnings("unchecked")
  public CachedProceduralAnalysis(String name, IRType type, IBinder b, int cacheSize)
      throws SlotAlreadyRegisteredException {
    super(name, type);
    binder = b;
    maxVersionsCached = cacheSize;
    versions = new Version[cacheSize];
    results = new Map[cacheSize];
  }
  
  /**
   * Get all the results for the current version.  Return null if
   * nothing has been computed for this version yet.
   * @return all results, if this version is in the cache
   */
  protected synchronized Map<IRNode,R> findResults() {
    Version v = Version.getVersion();
    for (int i=0; i < maxVersionsCached; ++i) {
      if (versions[i] == v) {
        Map<IRNode,R> r = results[i];
        if (i != 0) {
          // we move it to the front, not for finding efficiency,
          // but to maintain a LRU cache.  I expect this action will happen
          // rarely, but it is cheap anyway (assume a small cache).
          while (i != 0) {
            versions[i] = versions[i-1];
            results[i] = results[i-1];
            i = i-1;
          }
          versions[0] = v;
          results[0] = r;
        }
        return r;
      }
    }
    return null;
  }
  
  /**
   * Return the map of all results of analysis for the current version.
   * @return map of results for the current version (never null).
   */
  protected synchronized Map<IRNode,R> getResults() {
    Map<IRNode,R> r = findResults();
    if (r != null) return r;
    for (int i=maxVersionsCached-1; i > 0; --i) {
      versions[i] = versions[i-1];
      results[i] = results[i-1];
    }
    versions[0] = Version.getVersion();
    return results[0] = new HashMap<IRNode,R>();
  }
  
  public static IRNode getProcedure(IRNode node) {
    Operator op;
    for (;;) {
      if (node == null) return node;
      op = JJNode.tree.getOperator(node);
      //!! What about AnonClassExpression ?
      if (op instanceof MethodDeclaration || op instanceof ConstructorDeclaration) {
        return node;
      } else if (op instanceof InterfaceDeclaration) {
        return node;
      } else if (op instanceof NestedClassDeclaration) {
        if (JavaNode.getModifier(node,JavaNode.STATIC)) return node;
      } else if (op instanceof ClassDeclaration) {
        return node;
      } else if (op instanceof ClassInitializer) {
        if (!JavaNode.getModifier(node,JavaNode.STATIC)) {
          IRNode classBody = JJNode.tree.getParent(node);
          for (Iterator<IRNode> enm = JJNode.tree.children(classBody); enm.hasNext();) {
            IRNode d = enm.next();
            if (JJNode.tree.getOperator(d) instanceof ConstructorDeclaration) return d;
          }
          LOG.severe("Couldn't find constructor for " + node);
        }
      }
      node = JJNode.tree.getParent(node);
    }
  }

  /**
   * Return any cached results for this module for the current version.
   * If there are no cached results, return null
   * @param procedure procedure on which to run analysis.
   *   (Usually a method declaration)
   * @return cached results of running analysis on procedure in current version.
   */
  public R findResults(IRNode procedure) {
    Map<IRNode,R> table = findResults();
    if (table == null) return null;
    R results;
    synchronized (table) {
      results  = table.get(procedure);
    }
    if (results == placeholder) results = null;
    return results;
  }
  
  /**
   * Get the results of the analysis for this procedure.  If they haven't
   * been computed yet, compute them.  If they are being computed
   * in another thread, wait for this thread to finish computation.
   * If interrupted in the wait or if no results are computed (an error!) , return null.
   * @param procedure procedure to run analysis on.
   * @return results of running analysis on procedure in current version
   */
  public R getResults(IRNode procedure) {
    Map<IRNode,R> table = getResults();
    R results;
    synchronized (table) {
      while ((results = table.get(procedure)) == placeholder) {
        try {
          table.wait();
        } catch (InterruptedException e) {
          return null;
        }
      }
      if (results != null) return results;
      table.put(procedure,placeholder);
    }
    try {
      results = computeResults(procedure);
    } finally {
      synchronized (table) {
        table.put(procedure,results);
        table.notifyAll();
       }
    }
    return results;
  }
  
  protected abstract R computeResults(IRNode procedure);
    
  @Override
  protected T getSlotValue(IRNode node) {
    IRNode proc = getProcedure(node);
    return getResults(proc).getSlotValue(node);
  }
  @Override
  protected boolean valueExists(IRNode node) {
    IRNode proc = getProcedure(node);
    return getResults(proc).valueExists(node);
  }
  
  /**
   * Analysis results for the procedural analysis.  We keep one
   * of these for each procedure we have run analysis on.
   * @author boyland
   */
  public static interface Results<T> {
     public IRNode getProcedure();
     // direct acces to results
     public boolean valueExists(IRNode node);
     public T getSlotValue(IRNode node);
  }
  
  protected final R placeholder = makePlaceholder();
  protected abstract R makePlaceholder();
  
  
  /**
   * A simple container for analysis results.  We do nothing interesting.
   * @author boyland
   */
  public static class SimpleResults<T> implements Results<T> {
    protected final IRNode procedure;
    protected final Map<IRNode,T> results;
    
    public SimpleResults(IRNode p, Map<IRNode,T> values) {
      procedure = p;
      results = values;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.CachedProceduralAnalysis.Results#getProcedure()
     */
    @Override
    public IRNode getProcedure() {
      return procedure;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.CachedProceduralAnalysis.Results#valueExists(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public boolean valueExists(IRNode node) {
      return results != null && results.keySet().contains(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.CachedProceduralAnalysis.Results#getSlotValue()
     */
    @Override
    public T getSlotValue(IRNode node) {
      if (results == null) return null;
      return results.get(node);
    }

  }
  
  /**
   * Results including an assusrance logger
   * @see AssusranceLogger
   * @author boyland
   */
  public static class AssuranceLoggerResults<T> extends SimpleResults<T> {
    final AssuranceLogger logger;
    
    public AssuranceLoggerResults(IRNode proc, Map<IRNode,T> info, AssuranceLogger log) {
      super(proc,info);
      logger = log;
    }

    public AssuranceLogger getLog() {
      return logger;
    }
  }
}
