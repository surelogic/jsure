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
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.analysis.IWarningReport;
import edu.cmu.cs.fluid.java.analysis.SilentWarningReport;
import edu.cmu.cs.fluid.java.promise.*;
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

  final Collection<IPromiseAnnotation> annos = new ArrayList<IPromiseAnnotation>();

  final Map<String, IAnnotationParseRule> parseMap = new HashMap<String, IAnnotationParseRule>(); 
  final Map<String, IPromiseDropStorage> storageMap = new HashMap<String, IPromiseDropStorage>(); 
  
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