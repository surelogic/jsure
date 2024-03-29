package com.surelogic.annotation.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.AASTStatus;
import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.java.DeclarationNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.ScrubberOrder;
import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.EmptyIterator;
import com.surelogic.common.util.FilterIterator;
import com.surelogic.common.util.Iteratable;
import com.surelogic.common.util.IteratorUtil;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;
import com.surelogic.promise.IBooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropSeqStorage;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.ISinglePromiseDropStorage;
import com.surelogic.promise.PromiseDropStorage;
import com.surelogic.task.CycleFoundException;
import com.surelogic.task.DuplicateTaskNameException;
import com.surelogic.task.TaskManager;
import com.surelogic.task.UndefinedDependencyException;
import com.surelogic.test.ITestOutput;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;

/**
 * A place for code common across rules packs
 * 
 * @author Edwin.Chan
 */
public abstract class AnnotationRules {
  protected static final Logger LOG = SLLogger.getLogger("annotation.rules");

  public static final ITestOutput XML_LOG = !IDE.hasInstance() ? null
      : IDE.getInstance().makeLog(System.getProperty(AnnotationConstants.XML_LOG_PROP, AnnotationConstants.XML_LOG_NAME));

  /*
   * Constants
   */

  protected static final SyntaxTreeInterface tree = JJNode.tree;

  /*
   * Utility code
   */

  public static <A extends DeclarationNode> String computeQualifiedName(A a) {
    return computeQualifiedName(a.getPromisedFor(), a.getId());
  }

  public static String computeQualifiedName(IRNode type, String id) {
    return JavaNames.getFullTypeName(type) + '.' + id;
  }

  public static Operator getOperator(IRNode n) {
    return tree.getOperator(n);
  }

  /*
   * Initialization code
   */

  private static boolean registered = false;

  /**
   * Registers the known rules with the promise framework
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
    // ThreadRoleRules.getInstance().register(fw);
    // ModuleRules.getInstance().register(fw);
    VouchRules.getInstance().register(fw);
    JcipRules.getInstance().register(fw);
    LayerRules.getInstance().register(fw);
    UtilityRules.getInstance().register(fw);
    NonNullRules.getInstance().register(fw);
    EqualityRules.getInstance().register(fw);
    StructureRules.getInstance().register(fw);
    // This should always be last after registering any rules
    PromiseDropStorage.init();
  }

  /**
   * Called to register any rules defined by subclasses
   */
  public abstract void register(PromiseFramework fw);

  /**
   * Convenience method for registering a parse rule and associated
   * storage/scrubber
   */
  protected void registerParseRuleStorage(PromiseFramework fw, IAnnotationParseRule<?, ?> r) {
    fw.registerParseDropRule(r);

    @SuppressWarnings({ "rawtypes" })
    IPromiseDropStorage<? extends PromiseDrop> stor = r.getStorage();
    if (stor != null) {
      fw.registerDropStorage(stor);
    }

    IAnnotationScrubber s = r.getScrubber();
    if (s != null) {
      registerScrubber(fw, s);
    }
  }

  /*
   * Scrubber code
   */

  private static final TaskManager mgr = makeManager();
  private static final TaskManager firstMgr = makeManager();
  private static final TaskManager lastMgr = makeManager();

  private static TaskManager makeManager() {
    return new TaskManager(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
  }

  private static TaskManager getManager(ScrubberOrder order) {
    switch (order) {
    case FIRST:
      return firstMgr;
    case NORMAL:
      return mgr;
    case LAST:
      return lastMgr;
    }
    return mgr;
  }

  private static final String CONFLICT_RESOLUTION = "Conflict Resolution";

  private static class ConflictResolver extends ArrayList<IAnnotationConflictResolver>implements Runnable {
    private static final long serialVersionUID = 5985330630719673098L;

    ConflictResolver() {
      // Nothing to do
    }

    @Override
    public void run() {
      if (!isEmpty()) {
        for (IRNode n : AASTStore.getPromisedForNodes()) {
          // System.out.println("Resolving conflicts
          // for"+JavaNames.getRelativeName(n));
          final IAnnotationConflictResolver.Context context = AASTStore.getConflictResolutionContext(n);
          for (IAnnotationConflictResolver r : this) {
            r.resolve(context);
          }
        }
      }
    }
  }

  public static final Comparator<IAASTRootNode> originComparator = new Comparator<IAASTRootNode>() {
    public int compare(IAASTRootNode a, IAASTRootNode b) {
      return b.getOrigin().ordinal() - a.getOrigin().ordinal();
    }
  };
  private static final ConflictResolver resolver = new ConflictResolver();

  static {
    try {
      firstMgr.addTask(CONFLICT_RESOLUTION, resolver);
      // Explicit strings to avoid dependency issues
      firstMgr.addDependencies(CONFLICT_RESOLUTION, "Assume", "Promise");
    } catch (Exception e) {
      e.printStackTrace();
    }
    // Add code to make @Promise do defaults
    resolver.add(new IAnnotationConflictResolver() {
      @Override
      public void resolve(Context context) {
        for (Class<? extends IAASTRootNode> cls : context.getAASTTypes()) {
          handle(context, cls);
        }
      }

      <T extends IAASTRootNode> void handle(Context context, Class<T> cls) {
        Collection<T> aasts = context.getAASTs(cls);
        /*
         * System.out.println("\t"+cls.getSimpleName()+": "+aasts.size()); if
         * (cls == NonNullNode.class) { System.out.println(
         * "Looking at NonNull for "
         * +JavaNames.getRelativeName(context.getNode())+": "+aasts.size()); }
         */
        if (aasts.size() <= 1) {
          return;
        }
        if (!aasts.iterator().next().needsConflictResolution()) {
          return;
        }
        List<T> sorted = new ArrayList<>(aasts);
        removeLowOriginAASTs(context, sorted);
      }
    });
  }

  /**
   * Only keep the AASTs with the "highest" origins (e.g. DECL vs SCOPED_ON_PKG)
   */
  protected static <T extends IAASTRootNode> void removeLowOriginAASTs(IAnnotationConflictResolver.Context context,
      List<T> candidates) {
    Collections.sort(candidates, originComparator);
    // Assumes that the origins are sorted in descending order
    AnnotationOrigin firstOrigin = null;
    for (T ast : candidates) {
      if (firstOrigin == null) {
        firstOrigin = ast.getOrigin();
      } else if (ast.getOrigin() != firstOrigin) {
        context.remove(ast);
      }
    }
  }

  protected void registerConflictResolution(IAnnotationConflictResolver r) {
    resolver.add(r);
  }

  /**
   * Registers a scrubber and its dependencies on other scrubbers
   */
  protected void registerScrubber(PromiseFramework fw, IAnnotationScrubber s) {
    final TaskManager manager = getManager(s.order());
    try {
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
        LOG.warning("Didn't process " + a + " on " + JavaNames.getFullName(a.getPromisedFor()));
      } else {
        it.remove();
      }
    }
    AASTStore.clearASTs();
  }

  /*
   * Accessors for IPromiseDropStorage
   */

  static boolean isBogus(PromiseDrop<?> p) {
    return !p.isValid();
  }

  /**
   * Store the drop on the IRNode only if it is not null
   * 
   * @return The drop passed in
   */
  public static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> P storeDropIfNotNull(IPromiseDropStorage<P> stor,
      P pd) {
    if (pd == null) {
      return null;
    }
    final IRNode n = pd.getPromisedFor();
    if (n == null) {
      return null;
    }
    final IRNode mapped = PromiseFramework.getInstance().mapToProxyNode(n);
    /*
     * if (mapped != n) { System.out.println(pd.getMessage()+" created on "
     * +DebugUnparser .toString(n)); }
     */
    return stor.add(mapped, pd);
  }

  /**
   * Attach the promise drop to a IRNode outside of the normal scrubber
   * architecture.
   * 
   * @param iPromiseDropStorage
   */
  public static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> void attachAsVirtual(IPromiseDropStorage<P> stor,
      P pd) {
    pd.setVirtual(true);
    // What else?
    storeDropIfNotNull(stor, pd);
  }

  private static <P extends PromiseDrop<?>> P getMappedValue(SlotInfo<P> si, final IRNode n) {
    final PromiseFramework frame = PromiseFramework.getInstance();
    final IRNode mapped = frame.getProxyNode(n);
    /*
     * if (mapped != n) { System.out.println("Using "+mapped+", instead of "+
     * DebugUnparser.toString(n)); }
     */
    return getMappedValue(si, n, mapped, frame);
  }

  private static <P extends PromiseDrop<?>> P getMappedValue(SlotInfo<P> si, final IRNode n, final IRNode mapped,
      PromiseFramework frame) {
    P rv;

    // If there is a proxy node
    final boolean tryOrig;
    if (!n.equals(mapped)) {
      // Try to use the value from there
      rv = mapped.getSlotValue(si);
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
   * Remove from associated IRNode
   */
  protected static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> void removeDrop(IPromiseDropStorage<P> stor, P pd) {
    final IRNode n = pd.getAAST().getPromisedFor();
    final IRNode mapped = PromiseFramework.getInstance().getProxyNode(n);
    stor.remove(mapped, pd);
    // pd.invalidate();
  }

  /**
   * Getter for BooleanPromiseDrops
   */
  protected static <D extends BooleanPromiseDrop<?>> D getBooleanDrop(IPromiseDropStorage<D> s, IRNode n) {
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
  protected static <D extends PromiseDrop<?>> D getDrop(IPromiseDropStorage<D> s, IRNode n) {
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

  private static <D extends PromiseDrop<?>> Iterator<D> getIterator(SlotInfo<List<D>> si, IRNode n) {
    if (n == null) {
      return new EmptyIterator<>();
    }
    List<D> s = n.getSlotValue(si);
    if (s != null) {
      return s.iterator();
    }
    return new EmptyIterator<>();
  }

  /**
   * Getter for lists of PromiseDrops
   */
  protected static <D extends PromiseDrop<?>> Iterable<D> getDrops(IPromiseDropStorage<D> s, IRNode n) {
    if (n == null) {
      return new EmptyIterator<>();
    }
    IPromiseDropSeqStorage<D> storage = (IPromiseDropSeqStorage<D>) s;

    final PromiseFramework frame = PromiseFramework.getInstance();
    final IRNode mapped = frame.getProxyNode(n);
    final SlotInfo<List<D>> si = storage.getSeqSlotInfo();

    // Need to merge values if both available
    Iterator<D> e = new EmptyIterator<>();

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
      return new EmptyIterator<>();
    }
    return new FilterIterator<D, D>(e) {
      @Override
      protected Object select(D o) {
        if (o == null) {
          return null;
        }
        // return isBogus((IRNode) o) ? notSelected : o;
        if (isBogus(o)) {
          return IteratorUtil.noElement;
        } else {
          return o;
        }
      }
    };
  }

  public static final class ParameterMap {
    private final Map<IRNode, Integer> argPosition;
    private final List<IRNode> parentArgs;
    private final List<IRNode> childArgs;

    public ParameterMap(final IRNode parent, final IRNode child) {
      argPosition = new HashMap<>();
      parentArgs = new ArrayList<>();
      childArgs = new ArrayList<>();

      final Iteratable<IRNode> parentParams = Parameters.getFormalIterator(MethodDeclaration.getParams(parent));
      final Iteratable<IRNode> childParams = Parameters.getFormalIterator(MethodDeclaration.getParams(child));
      int count = 0;
      for (final IRNode parentArg : parentParams) {
        final IRNode childArg = childParams.next();
        final Integer idx = Integer.valueOf(count);
        argPosition.put(parentArg, idx);
        argPosition.put(childArg, idx);
        parentArgs.add(parentArg);
        childArgs.add(childArg);
        count += 1;
      }
    }

    // Returns -1 if param is not found
    public int getPositionOf(final IRNode param) {
      final Integer v = argPosition.get(param);
      return v == null ? -1 : v.intValue();
    }

    // Return null if there is a look up failure
    private IRNode getParallelArgument(final IRNode arg, final List<IRNode> otherArgs) {
      final Integer idx = argPosition.get(arg);
      return idx == null ? null : otherArgs.get(idx.intValue());
    }

    public IRNode getCorrespondingChildArg(final IRNode parentArg) {
      return getParallelArgument(parentArg, childArgs);
    }

    public IRNode getCorrespondingParentArg(final IRNode childArg) {
      return getParallelArgument(childArg, parentArgs);
    }
  }

  @Deprecated
  // use ParameterMap class instead
  protected static Map<IRNode, Integer> buildParameterMap(final IRNode annotatedMethod, final IRNode parent) {
    // Should have the same number of arguments
    final Iteratable<IRNode> p1 = Parameters.getFormalIterator(MethodDeclaration.getParams(annotatedMethod));
    final Iteratable<IRNode> p2 = Parameters.getFormalIterator(MethodDeclaration.getParams(parent));
    int count = 0;
    final Map<IRNode, Integer> positionMap = new HashMap<>();
    for (final IRNode arg1 : p1) {
      positionMap.put(arg1, count);
      positionMap.put(p2.next(), count);
      count += 1;
    }
    return positionMap;
  }

  public static final NamedTypeNode[] noTypes = new NamedTypeNode[0];

  protected static NamedTypeNode[] createNamedType(int offset, String val) {
    if (val == null || val.length() == 0) {
      return noTypes;
    }
    String[] values = val.split(",");
    NamedTypeNode[] rv = new NamedTypeNode[values.length];
    int i = 0;
    for (String s : values) {
      // FIX can we get the exact offset?
      rv[i] = new NamedTypeNode(offset, s.trim());
      i++;
    }
    return rv;
  }
}
