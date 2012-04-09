/*
 * Created on Oct 10, 2003
 *  
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.annotation.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRBooleanType;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeType;
import edu.cmu.cs.fluid.ir.IRSequenceType;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.MarkedIRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.analysis.IWarningReport;
import edu.cmu.cs.fluid.java.analysis.SilentWarningReport;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * @author chance
 *  
 */
@SuppressWarnings("unchecked")
public class PromiseFramework implements IPromiseFramework, PromiseConstants {

  /**
   * Logger for this class
   */
  static final Logger LOG = SLLogger.getLogger("FLUID.bind");

  IWarningReport reporter = SilentWarningReport.prototype;

  IPromiseParser parser = null;

  final Collection<IPromiseAnnotation> annos = new ArrayList<IPromiseAnnotation>();

  final Map<String, IAnnotationParseRule> parseMap = new HashMap<String, IAnnotationParseRule>(); 
  final Map<String, IPromiseDropStorage> storageMap = new HashMap<String, IPromiseDropStorage>(); 
  
  final Map<Operator, IPromiseBindRule> bindMap = new HashMap<Operator, IPromiseBindRule>(); // Of op -> rules

  final ICustomHashMap checkMap = new CustomHashMap(); // Of op -> List<rules>

  final ICustomHashMap storMap = new CustomHashMap(); // Of op -> List<rules>
  final Set<IPromiseStorage> storSet = new HashSet<IPromiseStorage>(); 

  private PromiseFramework() {
    // check to see that all ops are being bound
  }

  private static final PromiseFramework instance = new PromiseFramework();

  public static PromiseFramework getInstance() {
    return instance;
  }

  protected static final <T extends PromiseDrop> 
  SlotInfo<T> makeDropSlotInfo(String promise) {
    SlotInfo<T> si = SimpleSlotFactory.prototype.newLabeledAttribute("@"+promise, null);
    return si;
  }
  
  protected static final <T extends PromiseDrop> 
  SlotInfo<List<T>> makeDropSeqSlotInfo(String promise) {
    SlotInfo<List<T>> si = SimpleSlotFactory.prototype.newLabeledAttribute("@"+promise, null);
    return si;
  }
  
  /**
   * 
   */
  @Deprecated
  public void finishInit(IBinder binder, IPromiseParser parser) {
    setParser(parser);

    setBinder(binder);
  }

  @Deprecated
  public void setBinder(IBinder binder) {
    Iterator<IPromiseAnnotation> it = annos.iterator();
    while (it.hasNext()) {
      IPromiseAnnotation anno = it.next();
      anno.setBinder(binder);
    }
  }

  public void setReporter(IWarningReport report) {
    reporter = report;
  }

  public void clearReporter() {
    reporter = SilentWarningReport.prototype;
  }

  public IWarningReport getReporter() {
    return reporter;
  }

  public void registerAnnotation(IPromiseAnnotation anno) {
    annos.add(anno);
    anno.register(this);
  }

  private final List<IPromiseParseRule> unregisteredParseRules = new ArrayList<IPromiseParseRule>();
  
  /**
   * Register the tag (e.g., @foo) as being parsed by this rule
   * 
   * @param tag
   * @param rule
   * @return true if completed successfully
   */
  public boolean registerParseRule(String tag, IPromiseParseRule rule) {
    if (parser == null) {
      unregisteredParseRules.add(rule); // XXX fix to do something later
      return true;
    }
    return registerParseRule_real(tag, rule);
  }
  
  private boolean registerParseRule_real(String tag, IPromiseParseRule rule) {
    IPromiseParseRule bumped = parser.addRule(rule);
    if (bumped != null
        && !bumped.getClass().getName().startsWith(
            "edu.cmu.cs.fluid.eclipse.promise.PromiseParser")) {
      LOG.warning("Bumped out parse rule: " + bumped);
    }
    return true;
  }

  /**
   * Register the Operator as being parsed by this rule
   * 
   * @param op
   * @param rule
   * @return true if completed successfully
   */
  public boolean registerParseDropRule(IAnnotationParseRule rule) {
	  return registerParseDropRule(rule, false);
  }

  public boolean registerParseDropRule(IAnnotationParseRule rule, boolean suppressWarning) {
    String tag = rule.name();
    if (tag.length() == 0) {
      return false;
    }
    tag = ensureCapitalizedTag(tag);
    IAnnotationParseRule oldRule = parseMap.put(tag, rule);
    if (!suppressWarning && oldRule != null) {
      LOG.severe("Bumped out parse rule: " + oldRule);
    } 
    return true;
  }

  public static String ensureCapitalizedTag(String tag) {
    char first = tag.charAt(0);
    if (Character.isLowerCase(first)) {
      String msg = "Tag is lowercase: "+tag;
      LOG.log(Level.SEVERE, msg, new Throwable(msg));
      tag = Character.toUpperCase(first) + tag.substring(1);
    }
    return tag;
  }
  
  public IAnnotationParseRule getParseDropRule(String tag) {
    return parseMap.get(tag);
  }
  
  public Iterable<IAnnotationParseRule> getParseDropRules() {
	  return parseMap.values();
  }
  
  public <D extends PromiseDrop>
  boolean registerDropStorage(IPromiseDropStorage<D> stor) {
    String tag = stor.name();
    if (tag.length() == 0) {
      return false;
    }
    tag = ensureCapitalizedTag(tag);
    IPromiseDropStorage oldStor = storageMap.put(tag, stor);
    if (oldStor != null) {
      LOG.severe("Bumped out storage: " + oldStor);
    } 
    
    switch (stor.type()) {
      case BOOLEAN:
        SlotInfo<BooleanPromiseDrop> bsi = makeDropSlotInfo(tag);
        IBooleanPromiseDropStorage<BooleanPromiseDrop> bstor = 
          (IBooleanPromiseDropStorage<BooleanPromiseDrop>) stor;
        bstor.init(bsi);
        break;
      case NODE:
        SlotInfo<D> si = makeDropSlotInfo(tag);
        ((ISinglePromiseDropStorage<D>) stor).init(si);
        break;
      case SEQ:
        SlotInfo<List<D>> ssi = makeDropSeqSlotInfo(tag);
        ((IPromiseDropSeqStorage<D>) stor).init(ssi);
        break;
      case NONE:
    }
    return true;
  }
  
  public IPromiseDropStorage findStorage(String tag) {
    IPromiseDropStorage stor = storageMap.get(tag);
    return stor;
  }

  private SlotInfo findStorage(String tag, StorageType t) {
    IPromiseDropStorage stor = findStorage(tag);
    if (stor == null) {
      throw new IllegalArgumentException("No storage for "+tag);
    }
    if (stor.type() != t) {
      throw new Error("Types don't match: "+stor.type());
    }    
    switch (stor.type()) {
      case BOOLEAN:
        return ((IBooleanPromiseDropStorage) stor).getSlotInfo();
      case NODE:
        return ((ISinglePromiseDropStorage) stor).getSlotInfo();
      case SEQ:
        return ((IPromiseDropSeqStorage) stor).getSeqSlotInfo();
      case NONE:
    }
    return null;
  }
  
  public <D extends PromiseDrop>
  ISinglePromiseDropStorage<D> findNodeStorage(String tag) {
    return (ISinglePromiseDropStorage<D>) findStorage(tag);
  }
  
  public <D extends PromiseDrop>
  IPromiseDropSeqStorage<D> findSeqStorage(String tag) {
    return (IPromiseDropSeqStorage<D>) findStorage(tag);
  }
  
  public <D extends BooleanPromiseDrop>
  IBooleanPromiseDropStorage<D> findBooleanStorage(String tag) {
    return (IBooleanPromiseDropStorage<D>) findStorage(tag);
  }

  public <D extends PromiseDrop>
  SlotInfo<D> findNodeSlotInfo(String tag) {
    return findStorage(tag, StorageType.NODE);
  }
  
  public <D extends PromiseDrop>
  SlotInfo<List<D>> findSeqSlotInfo(String tag) {
    return findStorage(tag, StorageType.SEQ);
  }
  
  public <D extends BooleanPromiseDrop>
  SlotInfo<D> findBooleanSlotInfo(String tag) {
    return findStorage(tag, StorageType.BOOLEAN);
  }
  
  public Iteratable<IPromiseDropStorage> getAllStorage() {
    final Iterator<Entry<String,IPromiseDropStorage>> entries = 
      storageMap.entrySet().iterator();
    
    return new SimpleRemovelessIterator<IPromiseDropStorage>() {
      @Override
      protected Object computeNext() {        
        return entries.hasNext() ? entries.next().getValue() : IteratorUtil.noElement;
      }
      
    };
  }
  
  /*
  public <T extends IAnnotationDrop>
  void addSlotValue(IPromiseDropParseRule rule, IRNode declNode, T drop) {
    final IRNode proxy = mapToProxyNode(declNode);
    switch (rule.type()) {
      case BOOLEAN:
        if (!(drop instanceof IBooleanAnnotationDrop)) {
          throw new IllegalArgumentException("Not a boolean drop");
        }
        SlotInfo<IBooleanAnnotationDrop> bsi = findBooleanStorage(rule.name());
        // FIX for assumptions
        proxy.setSlotValue(bsi, (IBooleanAnnotationDrop) drop);
        break;
      case NODE:
        SlotInfo<T> nsi = findNodeStorage(rule.name());
        // FIX for assumptions
        proxy.setSlotValue(nsi, drop);
        break;
      case SEQ:
        SlotInfo<List<T>> ssi = findSeqStorage(rule.name());          
        // FIX for assumptions
        List<T> l = proxy.getSlotValue(ssi);
        if (l == null) {
          l = new ArrayList<T>(); 
          proxy.setSlotValue(ssi, l);
        }
        l.add(drop);
        break;
      case NONE:
    }
    //TODO drop.setAttachedTo(declNode, si);
    //TODO JavaPromise.attachPromiseNode(n, elt);
  }
  */
  /**
   * Register the Operator as being bound by this rule
   * 
   * @param op
   * @param rule
   * @return true if completed successfully
   */
  public boolean registerBindRule(Operator op, IPromiseBindRule rule) {
    registerBindRule_exact(op, rule);

    Iterator<Operator> it = ((JavaOperator) op).subOperators();
    while (it.hasNext()) {
      registerBindRule_exact(it.next(), rule);
    }
    return true;
  }

  private boolean registerBindRule_exact(Operator op, IPromiseBindRule rule) {
    if (!(op instanceof IHasCustomBinding)) {
      LOG.severe("Does not have a custom binding: "+op.name());
    }
    Object o = bindMap.put(op, rule);
    if (o != null) {
      LOG.warning("Bumped out bind rule: " + o);
    }
    return true;
  }

  /**
   * Register the Operator as being checked by this rule
   * 
   * @param op
   * @param rule
   * @return true if completed successfully
   */
  public boolean registerCheckRule(Operator op, IPromiseCheckRule rule) {
    return registerOpsInMap(checkMap, op, rule);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.promise.IPromiseFramework#registerStorage(edu.cmu.cs.fluid.tree.Operator, edu.cmu.cs.fluid.promise.IPromiseStorage)
   */
  public boolean registerStorage(Operator op, IPromiseStorage stor) {
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Registering: " + stor.name() + " for " + op.name());
    }
    registerStorage_exact(op, stor);
    storSet.add(stor);

    Iterator<Operator> it = ((JavaOperator) op).subOperators();
    while (it.hasNext()) {
      registerStorage_exact(it.next(), stor);
    }
    return true;
  }

  private boolean registerStorage_exact(Operator op, IPromiseStorage stor) {
    return registerOpsInMap(storMap, op, stor);
  }

  private boolean registerOpsInMap(ICustomHashMap map, Operator op,
      IPromiseRule rule) {
    boolean ok = registerInMap(map, op, rule);

    Iterator<Operator> it = ((JavaOperator) op).subOperators();
    while (it.hasNext()) {
      ok = ok && registerInMap(map, it.next(), rule);
    }
    return true;
  }

  private boolean registerInMap(ICustomHashMap map, Operator op,
      IPromiseRule rule) {
    Map.Entry e = map.getEntryAlways(op);
    List l;

    if (e.getValue() == null) {
      l = new ArrayList();
      e.setValue(l);
    } else {
      l = (List) e.getValue();
    }
    if (l.contains(rule)) {
      // System.out.println("Already contains "+rule+" for "+op.name());
    } else {
      l.add(rule);
    }
    return true;
  }

  public Iterator<IPromiseParseRule> getParseRules() {
    return parser.getRules();
  }
  
  public void printCheckOps() {
    if (!LOG.isLoggable(Level.FINER)) {
      return;
    }
    final Iterator it = checkMap.keySet().iterator();
    while (it.hasNext()) {
      Operator op = (Operator) it.next();
      LOG.finer("Got check rules for " + op.name());
      final Collection c = (Collection) checkMap.get(op);
      if (c != null) {
        final Iterator it2 = c.iterator();
        while (it2.hasNext()) {
          final IPromiseCheckRule rule = (IPromiseCheckRule) it2.next();
          LOG.finer("\tRule: " + rule.getClass().getName());
        }
      }
    }
  }

  public IPromiseBindRule getBindRule(Operator op) {
    IPromiseBindRule rule = bindMap.get(op);
    if (rule == null) {
      LOG.severe("Couldn't find bind rule for " + op);
    }
    return rule;
  }

  public IRNode getBinding(Operator op, IRNode n) {
    // Operator op = tree.getOperator(n);
    IPromiseBindRule rule = bindMap.get(op);
    if (rule == null) {
      LOG.severe("Couldn't find bind rule for " + op);
      return null;
    }
    return rule.getBinding(op, n);
  }

  static class CheckReport implements IPromiseCheckReport {
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseCheckReport#reportWarning(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode)
     */
    public void reportWarning(String description, IRNode promise) {
      PromiseFramework.getInstance().getReporter().reportWarning(description, promise);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseCheckReport#reportError(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode)
     */
    public void reportError(String description, IRNode promise) {
      PromiseFramework.getInstance().getReporter().reportProblem(description, promise);
      if (promise != null) {
        AbstractPromiseAnnotation.setBogus(promise, true);
        LOG.info("Setting as BOGUS due to "+description+": "+DebugUnparser.toString(promise));
      }
    }
  }

  final IPromiseCheckReport checkReporter = new CheckReport();

  
  /**
   * Iterates over the AST and checks the promises defined on each node
   * @param report
   * @param node
   */
  @Deprecated
  public void checkAST(IWarningReport report, IRNode node) {
    setReporter(report);

    final Iterator<IRNode> enm = tree.topDown(node);
    while (enm.hasNext()) {
      final IRNode n = enm.next();
      applyCheckRules(n);
    }
  }

  @SuppressWarnings("deprecation")
  public void checkAssumptionsOnAST(IWarningReport report, IRNode node) {
    final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
    
    final Map context = getCurrentTypeContext();
    if (context.isEmpty()) {      
      LOG.fine("No scrubbing to do for this context");
      return;
    }
    final Iterator it = context.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry e = (Map.Entry) it.next();
      final IRNode n = (IRNode) e.getKey();
      // proxy = e.getValue(); 
      final IRNode decl = ScopedPromises.findEnclosingDecl(n);
      if (decl != null && !context.containsKey(decl)) {
        if (fineIsLoggable) {
          LOG.fine("Scrubbing enclosing decl " + DebugUnparser.toString(decl));
        }
        applyCheckRules(decl);
      }
      if (fineIsLoggable) {
        LOG.fine("Scrubbing decl " + DebugUnparser.toString(n));
      }
      applyCheckRules(n);
    }
  }

  private void applyCheckRules(IRNode n) {
    final Operator op = tree.getOperator(n);
    final Collection<IPromiseCheckRule> c = (Collection<IPromiseCheckRule>) checkMap.get(op);
    if (c != null) {
      Iterator<IPromiseCheckRule> it = c.iterator();
      if (!it.hasNext()) {
        return;
      }
      //System.out.println("Checking "+op.name()+": "+DebugUnparser.toString(VisitUtil.getEnclosingCompilationUnit(n)));
      
      while (it.hasNext()) {
        final IPromiseCheckRule rule = it.next();
        //System.out.println("Using rule "+rule.getClass().getName()+": "+DebugUnparser.toString(VisitUtil.getEnclosingCompilationUnit(n)));
        //System.out.println("OBJECT = "+DebugUnparser.toString(object));
        rule.checkSanity(op, n, checkReporter);
      }
    }
  }

  /**
   * Returns info about what promises could appear on this node
   * @param op
   * @return
   */
  public Iterator<TokenInfo> getTokenInfos(Operator op) {
    List<IPromiseStorage> l = (List<IPromiseStorage>) storMap.get(op);
    if (l == null) {
      return new EmptyIterator<TokenInfo>();
    }
    return new FilterIterator<IPromiseStorage,TokenInfo>(l.iterator()) {
      @Override
      protected Object select(IPromiseStorage o) {
        return AbstractPromiseAnnotation.getInfo(o);
      }
    };
  }

  /**
   * Get all the infos available
   * @return
   */
  public Iterator<TokenInfo> getTokenInfos() {
    Iterator<IPromiseStorage> it = storSet.iterator();
    if (!it.hasNext()) {
      return new EmptyIterator<TokenInfo>();
    }
    return new FilterIterator<IPromiseStorage,TokenInfo>(it) {
      @Override protected Object select(IPromiseStorage o) {
        return AbstractPromiseAnnotation.getInfo(o);
      }
    };
  }
  
  public <T extends PromiseDrop> 
  SlotInfo<T> findSlotInfo(String promise, Class<T> cls) {
    for(IPromiseStorage s : storSet) {
      if (promise.equals(s.name())) {
        return AbstractPromiseAnnotation.getInfo(s).si;
      }
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.promise.IPromiseFramework#getParser()
   */
  public final IPromiseParser getParser() {
    return parser;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.promise.IPromiseFramework#setParser(edu.cmu.cs.fluid.promise.IPromiseParser)
   */
  public void setParser(IPromiseParser parser) {
    if (this.parser != null && this.parser != parser) {
      LOG.severe("Promise parser already set to "+this.parser);
    }
    this.parser = parser;
    
    for(IPromiseParseRule r : unregisteredParseRules) {
      parser.addRule(r);
    }
  }

  /**
   * set to create promises on proxy nodes, instead of usual    
   * @author chance
   */
  public Map pushTypeContext(IRNode type) {
    return pushTypeContext(type, false, false); // Don't create anything
  }

  /**
   * HashMap, but modified to note whether we should create IRNodes if none
   */
  private static class MyMap extends ConcurrentHashMap<IRNode,IRNode> {
	private static final long serialVersionUID = 1L;
	
	boolean f_createIfNone = false;
	boolean f_onlyAssume = false;
	final IRNode compUnit;

	MyMap(IRNode cu) {
		compUnit = cu;
	}
	  
	  synchronized boolean createIfNone() {
		  return f_createIfNone;
	  }
	  synchronized boolean onlyAssume() {
		  return f_onlyAssume;
	  }
	  synchronized void setCreateIfNone(boolean createIfNone) {
		  f_createIfNone = createIfNone;
	  }
	  synchronized void setOnlyAssume(boolean onlyAssume) {
		  f_onlyAssume = onlyAssume;
	  }
  }

  private static final MyMap EMPTY = new MyMap(null) {
	private static final long serialVersionUID = 1L;
	
	@Override
    public IRNode get(Object k) {
      return null; // Always empty
    }

    @Override
    public IRNode put(IRNode k, IRNode v) {
      throw new NotImplemented();
    }
    @Override
    public IRNode putIfAbsent(IRNode k, IRNode v) {
      throw new NotImplemented();
    }
  };

  /**
   * set to create promises on proxy nodes, instead of usual    
   * @author chance
   */
  public Map pushTypeContext(IRNode type, boolean createIfNone,
      boolean onlyAssume) {
    MyMap context = contextMap.get(type);
    if (context == null) {
      if (createIfNone) {
        LOG.info("Creating new type context for " + type);
        context = new MyMap(type);
        MyMap existing = contextMap.putIfAbsent(type, context);
        if (existing != null) {
        	// Use whatever's present
        	context = existing;
        }
      } else {
        context = EMPTY;
      }
    } else {
    	//System.out.println("Pushing type context for "+DebugUnparser.toString(type));
    }    
    if (LOG.isLoggable(Level.FINE) && context.size() > 0) {
      LOG.fine("Pushing non-empty @assume context");
      /*
      Iterator it = context.keySet().iterator();
      while (it.hasNext()) {
        IRNode n = (IRNode) it.next();
        System.out.println("proxy for: "+DebugUnparser.toString(n));
      }
      */
    }
    context.setCreateIfNone(createIfNone);
    context.setOnlyAssume(onlyAssume);
    typeContexts.get().push(context);
    return context;
  }

  /**
   * 
   * reset to not create proxy nodes
   */
  public Map popTypeContext() {
    MyMap map = typeContexts.get().pop();
    map.setCreateIfNone(false);
    map.setOnlyAssume(false);
    /*
    if (map.size() > 0) {
      System.out.println("Popping non-empty @assume context");
    }
    */
    return map;
  }

  /**
   * 
   * @return
   */
  public boolean clearTypeContext(IRNode type) {
    MyMap context = contextMap.remove(type);
    if (context != null) {
      return true;
    }
    return false;
  }

  /**
   * 
   * @return Non-null
   */
  private MyMap getCurrentTypeContext() {
	final Stack<MyMap> contexts = typeContexts.get();
    if (contexts.isEmpty()) {
      return EMPTY;
    }
    MyMap m = contexts.peek();
    return m;
  }

  /**
   * Get the CompUnit node for the current type context
   */
  public IRNode getCurrentContextType() {
	  return getCurrentTypeContext().compUnit;
  }
  
  /**
   * Get a proxy node if any
   * 
   * @param n
   * @return A node if any available, the original node otherwise
   */
  public IRNode getProxyNode(IRNode n) {
    Object o = getCurrentTypeContext().get(n);
    if (o != null) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Using proxy node for " + n);
      }
      return (IRNode) o;
    }
    return n;
  }

  /**
   * Get a proxy node, 
   * but create one if none already and context says to do so
   * 
   * @param n
   * @return
   */
  public IRNode mapToProxyNode(final IRNode n) {
    MyMap m = getCurrentTypeContext();
    Object o = m.get(n);
    if (o == null) {
      if (m.createIfNone()) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Creating proxy node for " + n);
        }
        IRNode proxy = new MarkedIRNode("Proxy node");
        IRNode present = m.putIfAbsent(n, proxy);
        //System.out.println("Creating proxy for "+DebugUnparser.toString(n));
        if (present != null) {
        	return present;
        }
        return proxy;
      }
      return n;
    }
    return (IRNode) o;
  }

  /**
   * A flag set to keep the promise scrubber from looking at the 
   * underlying promises (not assumpt
   * ions)
   * @return
   */
  public boolean useAssumptionsOnly() {
    return getCurrentTypeContext().onlyAssume();
  }

  private ThreadLocal<Stack<MyMap>> typeContexts = new ThreadLocal<Stack<MyMap>>() {
	  @Override
	  protected Stack<MyMap> initialValue() {
		  return new Stack<MyMap>();
	  }
  }; 

  private ConcurrentMap<IRNode, MyMap> contextMap = new ConcurrentHashMap<IRNode, MyMap>(); 
  
  /**
   * @return true if the node has assumptions stored for it
   *         in the current context
   */
  public boolean hasAssumptions(IRNode n) {
    return getCurrentTypeContext().containsKey(n);
  }
  
  /**
   * @return true if the current context has any assumptions
   */
  public boolean contextHasAssumptions() {
    return !getCurrentTypeContext().isEmpty();    
  }
  
  /**
   * @param type The type to be asked about
   * @return true if the context for the given type has any assumptions
   */
  public boolean contextHasAssumptions(IRNode type) {
    MyMap context = contextMap.get(type);
    return context != null && !context.isEmpty();    
  }
  
  /**
   * Examines the given node and delegates to the IPromiseProcessor to process the
   * promises that exist on the node.
   * 
   * @param n The IRNode we want to ask about
   * @param p The policy object that decides what to do with the promises found
   */
  @Deprecated
  public void processPromises(IRNode n, IPromiseProcessor p) {
    if (n == null || !tree.isNode(n)) {
      return;
    }
    final Operator op = tree.getOperator(n);
    final Iterator<TokenInfo> it = getTokenInfos(op);
    while (it.hasNext() && p.continueProcessing()) {
      TokenInfo info = it.next();
      final IRType type = info.si.getType();
        
      if (type instanceof IRBooleanType) {        
        if (AbstractPromiseAnnotation.isX_filtered(info.si, n)) { 
          p.processBooleanPromise(n, info);
        }     
      }
      /*
      else if (type instanceof IRIntegerType) {       
      }
      */
      else if (type instanceof IRNodeType) {        
        IRNode sub = AbstractPromiseAnnotation.getXorNull_filtered(info.si, n);
        if (sub != null) {
          p.processNodePromise(n, info, sub);
        }
      }
      else if (type instanceof IRSequenceType) {
       final Iteratable<IRNode> e = AbstractPromiseAnnotation.getEnum_filtered(info.si, n);        
        if (e.hasNext()) {
          p.processSequencePromise(n, info, e);
        }
      }
    }
    if (ConstructorDeclaration.prototype.includes(op)) {
      processReceiverDecl(n, p);
    }
    else if (MethodDeclaration.prototype.includes(op)) {
      processReceiverDecl(n, p);
      
      IRNode retnode  = JavaPromise.getReturnNodeOrNull(n);
      if (retnode != null) {
        processPromises(retnode, p.getProcessorForReturnNode(n, retnode));
      }
    }     
  }

  private void processReceiverDecl(IRNode n, IPromiseProcessor p) {
    IRNode receiver = JavaPromise.getReceiverNodeOrNull(n);
    if (receiver != null) {
      processPromises(receiver, p.getProcessorForReceiver(n, receiver));
    }
    IRNode qr = JavaPromise.getQualifiedReceiverNodeOrNull(n);
    if (qr != null) {
      /*
      if ("Promise Visitor".equals(p.getIdentifier())) {
        System.out.println(qr+": "+DebugUnparser.toString(qr));
        System.out.println(QualifiedReceiverDeclaration.getBase(qr));
      }
      */
      processPromises(qr, p.getProcessorForReceiver(n, qr));
    }
  }

  public static class HasPromisesProcessor implements IPromiseProcessor {
    public String getIdentifier() {
      return "Has promises";
    }
    
    public boolean found = false;
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseProcessor#processBooleanPromise(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo)
     */
    public void processBooleanPromise(IRNode n, TokenInfo info) {
      found = true;      
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseProcessor#processNodePromise(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo, edu.cmu.cs.fluid.ir.IRNode)
     */
    public void processNodePromise(IRNode n, TokenInfo info, IRNode sub) {
      found = true;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseProcessor#processSequencePromise(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo, java.util.Iterator)
     */
    public void processSequencePromise(IRNode n, TokenInfo info, Iteratable e) {
      found = true;      
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseProcessor#getProcessorForReceiver(edu.cmu.cs.fluid.ir.IRNode)
     */
    public IPromiseProcessor getProcessorForReceiver(IRNode n, IRNode receiver) {
      return this;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseProcessor#getProcessorForReturnNode(edu.cmu.cs.fluid.ir.IRNode)
     */
    public IPromiseProcessor getProcessorForReturnNode(IRNode n, IRNode retnode) {
      return this;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.IPromiseProcessor#continueProcessing()
     */
    public boolean continueProcessing() {
      return !found;
    }
    
  }
  
  /**
   * @return true if the node has promises stored on it
   */
  public boolean hasPromises(IRNode n) {
    if (n == null) {
      return false;
    }
    HasPromisesProcessor p = new HasPromisesProcessor();
    processPromises(n, p);
    return p.found;
  }
/*
  public boolean hasPromises(IRNode n) {
    if (n == null) {
      return false;
    }
    else if (tree.isNode(n)) {    
      final Operator op = tree.getOperator(n);
      final Iterator it = getTokenInfos(op);
      while (it.hasNext()) {
        TokenInfo info = (TokenInfo) it.next();
        final IRType type = info.si.getType();
        
        if (type instanceof IRBooleanType) {        
          if (AbstractPromiseAnnotation.isX_filtered(info.si, n)) { 
            return true;
          }     
        }
        /*
        else if (type instanceof IRIntegerType) {       
        }
        */
  /*
        else if (type instanceof IRNodeType) {        
          IRNode sub = AbstractPromiseAnnotation.getXorNull_filtered(info.si, n);
          if (sub != null) {
            return true;
          }
        }
        else if (type instanceof IRSequenceType) {
          final Iterator e = AbstractPromiseAnnotation.getEnum_filtered(info.si, n);        
          if (e.hasNext()) {
            return true;
          }
        }
      }
      if (ConstructorDeclaration.prototype.includes(op)) {
        return hasPromises(JavaPromise.getReceiverNodeOrNull(n));
      }
      else if (MethodDeclaration.prototype.includes(op)) {
        return hasPromises(JavaPromise.getReceiverNodeOrNull(n)) ||
               hasPromises(JavaPromise.getReturnNodeOrNull(n));
      }
    }
    return false;
  }
  */
  public boolean subtreeHasPromises(IRNode n) {
    final Iterator<IRNode> nodes = JJNode.tree.bottomUp(n);
    while (nodes.hasNext()) {
      IRNode node = nodes.next();
      if (PromiseFramework.getInstance().hasPromises(node)) return true;
    }
    return false;
  }

  /**
   * @return The set of annotation names that allow multiple annotations on a given declaration
   */
  public Set<String> getAllowsMultipleAnnosSet() {
	  final Set<String> rv = new HashSet<String>();
	  for(Map.Entry<String, IPromiseDropStorage> e : storageMap.entrySet()) {
		  if (e.getValue().type() == StorageType.SEQ) {
			  rv.add(e.getKey());
		  }
	  }
	  return rv;
  }
}