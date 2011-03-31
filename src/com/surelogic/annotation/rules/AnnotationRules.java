/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/AnnotationRules.java,v 1.48 2008/10/29 14:17:16 chance Exp $*/
package com.surelogic.annotation.rules;

import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.AASTStatus;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.java.DeclarationNode;
import com.surelogic.annotation.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.promise.*;
import com.surelogic.task.*;
import com.surelogic.test.*;


import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IDropFactory;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.util.*;

/**
 * A place for code common across rules packs
 * 
 * @author Edwin.Chan
 */
public abstract class AnnotationRules {
  protected static final Logger LOG = SLLogger.getLogger("annotation.rules");
  
  public static final String XML_LOG_NAME = "AnnotationRules";
  public static final String XML_LOG_PROP = XML_LOG_NAME+".label";  
  
  public static final ITestOutput XML_LOG = IDE.getInstance().makeLog(System.getProperty(XML_LOG_PROP, XML_LOG_NAME));
  
  /* *************************************************
   *  Constants
   * *************************************************/
 
  protected static final String[] noStrings = new String[0];
  protected static final SyntaxTreeInterface tree = JJNode.tree;
  
  /* *************************************************
   *  Utility code
   * *************************************************/

  public static <A extends DeclarationNode> 
  String computeQualifiedName(A a) {
    return computeQualifiedName(a.getPromisedFor(), a.getId());
  }
  
  public static String computeQualifiedName(IRNode type, String id) {
    return JavaNames.getFullTypeName(type)+'.'+id;
  }
  
  public static Operator getOperator(IRNode n) {
    return tree.getOperator(n);
  }
  
  /* *************************************************
   *  Initialization code
   * *************************************************/
  
  private static boolean registered = false;
  
  /**
   * Registers the known rules with the promise
   * framework   
   */
  public static synchronized void initialize() {
    if (registered) {
      return;
    }
    registered = true;
    
    PromiseFramework fw = PromiseFramework.getInstance();
    StandardRules.getInstance().register(fw);
    UniquenessRules.getInstance().register(fw);
    RegionRules.getInstance().register(fw);
    LockRules.getInstance().register(fw);
    ThreadEffectsRules.getInstance().register(fw);
    MethodEffectsRules.getInstance().register(fw);
    TestRules.getInstance().register(fw);
    ScopedPromiseRules.getInstance().register(fw);
    ThreadRoleRules.getInstance().register(fw);
    ModuleRules.getInstance().register(fw);
    VouchRules.getInstance().register(fw);
    JcipRules.getInstance().register(fw);
    LayerRules.getInstance().register(fw);
    
    PromiseDropStorage.init();
  }
  
  /**
   * Called to register any rules defined by subclasses
   */
  public abstract void register(PromiseFramework fw);
  
  /**
   * Convenience method for registering a parse rule and associated storage/scrubber
   */
  protected void registerParseRuleStorage(PromiseFramework fw, IAnnotationParseRule r) {
    fw.registerParseDropRule(r);
    
    @SuppressWarnings("unchecked")
    IPromiseDropStorage<? extends PromiseDrop> stor = r.getStorage(); 
    if (stor != null) {
      fw.registerDropStorage(stor);
    }
    
    IAnnotationScrubber s = r.getScrubber();
    if (s != null) {
      registerScrubber(fw, s);
    }
  }
  
  /* *************************************************
   *  Scrubber code
   * *************************************************/

  private static final TaskManager mgr = makeManager();
  private static final TaskManager firstMgr = makeManager();
  private static final TaskManager lastMgr = makeManager();
  
  private static TaskManager makeManager() {
    return new TaskManager(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());   
  }
  
  private static TaskManager getManager(ScrubberOrder order) {
    switch(order) {
      case FIRST:
        return firstMgr;
      case NORMAL:
        return mgr;
      case LAST:
        return lastMgr;
    }
    return mgr;
  }
  
  private IAnnotationScrubberContext context = new IAnnotationScrubberContext() {
    public void reportError(
        final IAASTNode n, final String msgTemplate, final Object... args) {
      reportError(MessageFormat.format(msgTemplate, args), n);
    }

    public void reportError(String msg, IAASTNode n) {
    	reportError(msg+" on "+n, n.getPromisedFor(), n.getOffset());
    }
    
    public void reportError(IRNode n, String msgTemplate, Object... args) {
    	final ISrcRef ref = JavaNode.getSrcRef(n);
    	final int offset;
    	if (ref == null) {
    		offset = 0;    	
    	} else {
    		offset = ref.getOffset();
    	}
        String txt = MessageFormat.format(msgTemplate, args)+" on "+DebugUnparser.toString(n);
    	reportError(txt, n, offset);
    }
    
    private void reportError(String txt, IRNode n, int offset) {      
//      System.out.println("SCRUBBER: "+txt);
      PromiseWarningDrop d = new PromiseWarningDrop(offset);
      d.setMessage(txt);
      d.setCategory(JavaGlobals.PROMISE_SCRUBBER);
      d.setNodeAndCompilationUnitDependency(n);
    }

    public void reportWarning(IAASTNode n, String msgTemplate, Object... args) {
      reportWarning(MessageFormat.format(msgTemplate, args), n);
    }

    public void reportWarning(String msg, IAASTNode n) {
      reportError(msg, n);
    }  
    
    public IBinder getBinder() {
      return IDE.getInstance().getTypeEnv().getBinder();
    }
  };
  
  /**
   * Registers a scrubber and its dependencies on other scrubbers
   */
  protected void registerScrubber(PromiseFramework fw, IAnnotationScrubber s) {
    final TaskManager manager = getManager(s.order());
    try {      
      s.setContext(context);
      
      manager.addTask(s.name(), s);
      manager.addDependencies(s.name(), s.dependsOn());
      for (String before : s.shouldRunBefore()) {
        manager.addDependency(before, s.name());
      }
    } catch (DuplicateTaskNameException e) {
      LOG.log(Level.SEVERE, "Unable to register scrubber", e);
    } catch (IllegalStateException e) {
      LOG.log(Level.SEVERE, "Unable to register scrubber", e);
    }
  }

  /**
   * Executes all the scrubbers
   */
  public static void scrub() {
    try {
      firstMgr.execute(true);
      mgr.execute(true, 1000, TimeUnit.SECONDS);
      lastMgr.execute(true);
    } catch (UndefinedDependencyException e) {
    	LOG.log(Level.SEVERE, "Problem while running scrubber", e);
    } catch (CycleFoundException e) {
    	LOG.log(Level.SEVERE, "Problem while running scrubber", e);
    } catch (InterruptedException e) {
    	LOG.log(Level.SEVERE, "Problem while running scrubber", e);
    } catch (BrokenBarrierException e) {
    	LOG.log(Level.SEVERE, "Problem while running scrubber", e);
    } catch (TimeoutException e) {
    	LOG.log(Level.SEVERE, "Problem while running scrubber", e);
    }
    final Iterator<IAASTRootNode> it = AASTStore.getASTs().iterator();
    while (it.hasNext()) {
      final IAASTRootNode a = it.next();
      if (a.getStatus() == AASTStatus.UNPROCESSED) {
        LOG.severe("Didn't process "+a+" on "+JavaNames.getFullName(a.getPromisedFor()));
      } else {
        it.remove();
      }
    }
    AASTStore.clearASTs();
  }
  
  /* *************************************************
   *  Accessors for IPromiseDropStorage
   * *************************************************/
  
  private static boolean isBogus(PromiseDrop p) {
    return !p.isValid();
  }
  
  /**
   * Store the drop on the IRNode only if it is not null
   * @return The drop passed in
   */
  public static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> 
  P storeDropIfNotNull(IPromiseDropStorage<P> stor, A a, P pd) {
    if (pd == null) {
      return null;
    }
    final IRNode n      = a.getPromisedFor();
    final IRNode mapped = PromiseFramework.getInstance().mapToProxyNode(n);
    /*
    if (mapped != n) {
    	System.out.println(pd.getMessage()+" created on "+DebugUnparser.toString(n));
    }
    */
    return stor.add(mapped, pd);
  }
  
  
  private static <P extends PromiseDrop> 
  P getMappedValue(SlotInfo<P> si, final IRNode n) {
    final PromiseFramework frame = PromiseFramework.getInstance();
    final IRNode mapped          = frame.getProxyNode(n);
    /*
    if (mapped != n) {
    	System.out.println("Using "+mapped+", instead of "+DebugUnparser.toString(n));
    }
    */
    return getMappedValue(si, n, mapped, frame);
  } 

  private static <P extends PromiseDrop> 
  P getMappedValue(SlotInfo<P> si, final IRNode n, 
                   final IRNode mapped, PromiseFramework frame) {
    P rv;
    
    // If there is a proxy node
    final boolean tryOrig;
    if (!n.equals(mapped)) {
      // Try to use the value from there
      rv =  mapped.getSlotValue(si); 
      if (rv != null && !isBogus(rv)) {
        return rv;
      }      
      tryOrig = !frame.useAssumptionsOnly();
    } else {
      tryOrig = true;
    }
    // Otherwise
    return tryOrig ? n.getSlotValue(si) : null;
  }
  
  /**
   * Add the AST to the PromiseDrop associated with the promisedFor node,
   * creating a drop if there isn't already one
   * 
   * @return The drop that the AST was added to
   */
  protected static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> 
  P ensureDropForAST(IPromiseDropStorage<P> stor, IDropFactory<P,? super A> factory, A a) {
    final PromiseFramework frame = PromiseFramework.getInstance();
    final IRNode n               = a.getPromisedFor();
    final IRNode mapped          = frame.mapToProxyNode(n);
    final SlotInfo<P> si         = stor.getSlotInfo();
    
    P pd = getMappedValue(si, n, mapped, frame);
    if (pd == null) {
      pd = factory.createDrop(n, a);
      return pd == null ? null : stor.add(mapped, pd);
    } else {
      pd.addAST(a);
    }
    return pd;
  }
  
  /**
   * Remove from associated IRNode 
   */
  protected static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> 
  void removeDrop(IPromiseDropStorage<P> stor, P pd) {
    final IRNode n      = pd.getAST().getPromisedFor();
    final IRNode mapped = PromiseFramework.getInstance().getProxyNode(n);
    stor.remove(mapped, pd);
    // pd.invalidate();
  }
  
  /**
   * Getter for BooleanPromiseDrops
   */
  protected static <D extends BooleanPromiseDrop> 
  D getBooleanDrop(IPromiseDropStorage<D> s, IRNode n) {
    IBooleanPromiseDropStorage<D> storage = (IBooleanPromiseDropStorage<D>) s;
    if (n == null) {
      return null;
    }
    D d = getMappedValue(storage.getSlotInfo(), n);
    if (d == null || !d.isValid()) {
    	return null;
    }
    return d;
  }
  
  /**
   * Getter for single PromiseDrops
   */
  protected static <D extends PromiseDrop> 
  D getDrop(IPromiseDropStorage<D> s, IRNode n) {
    ISinglePromiseDropStorage<D> storage = (ISinglePromiseDropStorage<D>) s;
    if (n == null) {
      return null;
    }
    D d = getMappedValue(storage.getSlotInfo(), n);
    if (d == null || !d.isValid()) {
    	return null;
    }
    return d;
  }
  
  private static <D extends PromiseDrop>
  Iterator<D> getIterator(SlotInfo<List<D>> si, IRNode n) {
    if (n == null) {
      return EmptyIterator.prototype();
    }
    List<D> s = n.getSlotValue(si);
    if (s != null) {
      return s.iterator();
    }
    return EmptyIterator.prototype();
  }
  
  /**
   * Getter for lists of PromiseDrops
   */
  protected static <D extends PromiseDrop> 
  Iterable<D> getDrops(IPromiseDropStorage<D> s, IRNode n) {
	if (n == null) {
		return EmptyIterator.prototype();
	}
    IPromiseDropSeqStorage<D> storage = (IPromiseDropSeqStorage<D>) s;
    
    final PromiseFramework frame = PromiseFramework.getInstance();
    final IRNode mapped          = frame.getProxyNode(n);
    final SlotInfo<List<D>> si   = storage.getSeqSlotInfo();
    
    // Need to merge values if both available    
    Iterator<D> e = EmptyIterator.prototype();
    
    final boolean tryOrig;
    // If there's a proxy node
    if (!n.equals(mapped)) {
      e = getIterator(si, mapped);
      
      tryOrig = !frame.useAssumptionsOnly();
    } else {
      // no proxy node
      tryOrig = true;
    }
    if (!e.hasNext() && tryOrig) {
      e = getIterator(si, n);
    }
    if (!e.hasNext()) {
      return EmptyIterator.prototype();
    }
    return new FilterIterator<D,D>(e) {
      @Override
      protected Object select(D o) {
        if (o == null) {
          return null;
        }
        // return isBogus((IRNode) o) ? notSelected : o;
        if (isBogus(o)) {
          return noElement;
        } else {
          return o;
        }
      }
    };       
  }
}
