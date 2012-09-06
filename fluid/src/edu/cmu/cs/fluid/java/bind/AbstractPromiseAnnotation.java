/*
 * Created on Oct 10, 2003
 *  
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/**
 * @author chance
 *  
 */
public abstract class AbstractPromiseAnnotation
  implements IPromiseAnnotation, PromiseConstants {
  protected static final Logger LOG        = SLLogger.getLogger("FLUID.bind.assume");
  private static final String STRICT_PROP  = "jsure.strict";
  public static final boolean inStrictMode = inStrictMode();  

  static Bundle bundle = new Bundle();
  static PromiseFramework frame = null;
  
  @Deprecated
  protected IBinder binder;

  protected AbstractPromiseAnnotation() {
    frame = PromiseFramework.getInstance();
    try {
      frame.registerAnnotation(this);
    } catch (Throwable t) {
      LOG.log(Level.SEVERE, "Unable to register annotations", t);
    }
  }

  private static boolean inStrictMode() {
    String prop = QuickProperties.getInstance().getProperties().getProperty(STRICT_PROP, null);
    return prop != null;
  }

  protected static final SlotFactory sf = JJNode.treeSlotFactory;
  protected static final IPromiseBindRule[] noBindRules =
    new IPromiseBindRule[0];

  static final List<SlotInfo> boolSIs = new ArrayList<SlotInfo>();
  static final List<SlotInfo> intSIs = new ArrayList<SlotInfo>();
  static final List<SlotInfo> nodeSIs = new ArrayList<SlotInfo>();
  static final List<SlotInfo> seqSIs = new ArrayList<SlotInfo>();
  static final boolean boolDefault = false;
  static final int intDefault = 0;
  static final IRNode nodeDefault = null;
  static final IRSequence<IRNode> seqDefault = null;

  private static final SlotInfo<Boolean> bogusSI =
    createBooleanSI("AbstractPromiseAnnotation.bogus");

  private static final Map<IPromiseStorage, TokenInfo> storage = new HashMap<IPromiseStorage, TokenInfo>(); 

  private static final Map<String,SlotInfo<? extends PromiseDrop>> promiseDropSIs =
    new HashMap<String,SlotInfo<? extends PromiseDrop>>();
  
  protected static final <T extends PromiseDrop> 
  SlotInfo<T> makeDropSlotInfo(String promise) {
    SlotInfo<T> si = SimpleSlotFactory.prototype.newAttribute(null);
    promiseDropSIs.put(promise, si);
    return si;
  }
  
  @SuppressWarnings("unchecked")
  public static final <T extends PromiseDrop> 
  SlotInfo<T> findDropSlotInfo(String promise, Class<T> cls) {
    return (SlotInfo<T>) promiseDropSIs.get(promise);
  }
  
  protected static boolean isBogus(IRNode promise) {
    return isX(bogusSI, promise);
  }

  protected static void setBogus(IRNode promise, boolean bogus) {
    setX_private(bogusSI, promise, bogus);
  }
  /*
	 * Methods needed to implement IPromiseAnnotation
	 */
  public final String name() {
    return this.getClass().getName(); // is this right?
  }
  @Deprecated
  public final void setBinder(IBinder bind) {
    binder = bind;
  }

  protected abstract IPromiseRule[] getRules();

  private boolean registerParseRule(
    IPromiseFramework frame,
    IPromiseParseRule rule) {
    return false;//frame.registerParseRule(rule.name(), rule);
  }

  private boolean registerBindRule(
    IPromiseFramework frame,
    IPromiseBindRule rule) {
    boolean ok = true;
    Operator[] ops = rule.getOps(IPromiseBindRule.class);
    for (int j = 0; j < ops.length; j++) {
      Operator op = ops[j];
      if (!(op instanceof IHasBinding)) {
        LOG.severe(
          "Trying to register a bind rule for something not IBindable: " + op);
      } else {
        //ok = ok && frame.registerBindRule(op, rule);
      }
    }
    return ok;
  }

  private boolean registerCheckRule(
    IPromiseFramework frame,
    IPromiseCheckRule rule) {
    boolean ok = true;
    Operator[] ops = rule.getOps(IPromiseCheckRule.class);
    for (int j = 0; j < ops.length; j++) {
      //ok = ok && frame.registerCheckRule(ops[j], rule);
    }
    return ok;
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.fluid.java.bind.IPromiseAnnotation#register(edu.cmu.cs.fluid.java.bind.PromiseFramework)
	 */
  public final boolean register(IPromiseFramework frame) {
    boolean ok = true;

    final IPromiseRule[] rules = getRules();
    for (int i = 0; i < rules.length; i++) {
      boolean handled = false;
      if (rules[i] instanceof IPromiseParseRule) {
        handled = true;
        ok = ok & registerParseRule(frame, (IPromiseParseRule) rules[i]);
      }
      if (rules[i] instanceof IPromiseBindRule) {
        handled = true;
        ok = ok & registerBindRule(frame, (IPromiseBindRule) rules[i]);
      }
      if (rules[i] instanceof IPromiseCheckRule) {
        handled = true;
        ok = ok & registerCheckRule(frame, (IPromiseCheckRule) rules[i]);
      }
      if (rules[i] instanceof IPromiseStorage) {
        handled = true;
        ok = ok & registerStorage(frame, (IPromiseStorage) rules[i]);
      }
      
      if (!handled) {
        LOG.severe("Unregistered rule " + rules[i]);
        ok = false;
      }

    }
    return ok;
  }

  @SuppressWarnings("unchecked")
  boolean registerStorage(IPromiseFramework frame, IPromiseStorage stor) {
		Operator[] ops = stor.getOps(IPromiseStorage.class);
    
		if (ops.length == 0) {
		  LOG.warning("Ignoring IPromiseStorage - no ops: "+stor.name());
		  return true;
		}
    SlotInfo si;
    switch (stor.type()) {
      case IPromiseStorage.BOOL :
        // si = getOpenBitSI(stor.name());
        si = createBooleanSI(stor.name());
        break;
      case IPromiseStorage.INT :
        si = createIntSI(stor.name());
        break;
      case IPromiseStorage.NODE :
        si = createNodeSI(stor.name());
        break;
      case IPromiseStorage.SEQ :
        si = createNodeSequenceSI(stor.name());
        break;
      default :
        si = null;
    }
    storage.put(stor, stor.set(si));
    
    // Check and register operators that it shows up on
    boolean ok = true;
		for (int j = 0; j < ops.length; j++) {
			//ok = ok && frame.registerStorage(ops[j], stor);
		}
		return ok;
  }
  
  public static TokenInfo getInfo(IPromiseStorage stor) {
    return storage.get(stor);
  }

  /*
	 * Methods needed for default IR storage
	 */
  static final IRType<IRSequence<IRNode>> seqType = IRSequenceType.nodeSequenceType;
  
  private static <T> SlotInfo<T> createSI(
    SlotFactory sf,
    String name,
    IRType<T> type,
    T defaultValue) {
    final String slotName = name; // name()+"-"+name;
    try {
      SlotInfo<T> si = sf.newAttribute(slotName, type, defaultValue);
      bundle.saveAttribute(si);
      return si;
    } catch (SlotAlreadyRegisteredException e) {
      throw new FluidRuntimeException(slotName + " slot already allocated");
    }
  }

  protected static final SlotInfo<Boolean> createBooleanSI(String name) {
    SlotInfo<Boolean> si =
      createSI(
        sf,
        name,
        IRBooleanType.prototype,
        boolDefault ? Boolean.TRUE : Boolean.FALSE);
    boolSIs.add(si);
    return si;
  }

  protected static final SlotInfo<Integer> createIntSI(String name) {
    SlotInfo<Integer> si =
      createSI(
        sf,
        name,
        IRIntegerType.prototype,
        IntegerTable.newInteger(intDefault));
    intSIs.add(si);
    return si;
  }

  protected static final SlotInfo<IRNode> createNodeSI(String name) {
    SlotInfo<IRNode> si = createSI(sf, name, IRNodeType.prototype, nodeDefault);
    nodeSIs.add(si);
    return si;
  }

  protected static final SlotInfo<IRSequence<IRNode>> createNodeSequenceSI(String name) {
    SlotInfo<IRSequence<IRNode>> si =
      createSI(SimpleSlotFactory.prototype, name, seqType, seqDefault);
    seqSIs.add(si);
    return si;
  }

  static int vectorCount = 0;
  static SlotInfo<Integer> currBitVectorSI = null;
  static int nextBit = 0;
  static final int MAXBITS = 32;

  static BitSI getOpenBitSI(String name) {
    if (nextBit >= MAXBITS) {
      nextBit = 0;
      currBitVectorSI = null;
    }
    if (currBitVectorSI == null) {
      currBitVectorSI =
        createSI(
          sf,
          "bitvector" + vectorCount,
          IRIntegerType.prototype,
          IntegerTable.newInteger(0));
    }
    final int bit = nextBit;
    nextBit++;
    BitSI si = new BitSI(name, currBitVectorSI, (1 << bit));
    boolSIs.add(si);
    return si;
  }

  static class BitSI extends SlotInfo {
    protected final String name;
    protected final SlotInfo<Integer> info;
    protected final int bitmask;

    BitSI(String n, SlotInfo<Integer> si, int mask) {
      name = n;
      info = si;
      bitmask = mask;
    }

    @Override
    public int size() {
      return info.size();
    }
    
    public boolean get(IRNode node) {
      int mods = node.getIntSlotValue(info);
      return ((mods & bitmask) > 0);
    }

    private int setModifier(int mods, int mod, boolean accumulate) {
      if (accumulate) {
        mods |= mod;
      } else {
        mods &= mod;
      }
      return mods;
    }

    public void set(IRNode node, boolean value) {
      int mods = setModifier(node.getIntSlotValue(info), bitmask, value);
      node.setSlotValue(info, mods);
    }

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.ir.SlotInfo#valueExists(edu.cmu.cs.fluid.ir.IRNode)
		 */
    @Override
    protected boolean valueExists(IRNode node) {
      return true;
    }

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.ir.SlotInfo#getSlotValue(edu.cmu.cs.fluid.ir.IRNode)
		 */
    @Override
    protected Object getSlotValue(IRNode node) throws SlotUndefinedException {
      return get(node) ? Boolean.TRUE : Boolean.FALSE;
    }

    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.ir.SlotInfo#setSlotValue(edu.cmu.cs.fluid.ir.IRNode,
		 *      java.lang.Object)
		 */
    @Override
    protected void setSlotValue(IRNode node, Object newValue)
      throws SlotImmutableException {
      if (newValue == Boolean.TRUE) {
        set(node, true);
      } else if (newValue == Boolean.FALSE) {
        set(node, false);
      }
    }
  }
  /*****************************************************************************
	 * Boolean SlotInfos for promises
	 ****************************************************************************/

  protected static final boolean isX(SlotInfo<Boolean> si, IRNode n) {
    return (n.getSlotValue(si)).booleanValue();
  } 
  
  private static final void setX_private(SlotInfo<Boolean> si, IRNode n, boolean val) {
    if (n.valueExists(si)) {
      Boolean last = n.getSlotValue(si);
      if (last == null) {
        LOG.warning("Got a null from a boolean SI, so assuming to be false");
        last = Boolean.FALSE;
      }
      if (last.booleanValue() && !val) {
        // FIX check for conflicts instead
        frame.getReporter().reportProblem("Resetting boolean promise to false: "+DebugUnparser.toString(n), n);        
      }
    }    
    n.setSlotValue(si, val ? Boolean.TRUE : Boolean.FALSE);
  }

  protected static final boolean isXorFalse(SlotInfo<Boolean> si, IRNode n) {  
    if (n == null) {
      return false;
    }
    if (n.valueExists(si)) {
      Boolean res = n.getSlotValue(si);
      if (res != null)
        return res.booleanValue();
    }
    return false;
  }

  /*****************************************************************************
	 * IRNode SlotInfos for promises
	 ****************************************************************************/

  protected static final IRNode getXorNull(SlotInfo<IRNode> si, IRNode n) {
    return n.getSlotValue(si);
    /*
		 * if (n == null) { return null; } if (n.valueExists(si)) { return (IRNode)
		 * n.getSlotValue(si); } return null;
		 */
  }

  private static final void setX_private(SlotInfo<IRNode> si, IRNode n, IRNode val) {
    if (val == null) {
      return;
    }
    final IRNode last = getXorNull(si, n);
    if (last != null) {
      // FIX check for conflicts instead
      frame.getReporter().reportProblem("Duplicate promise '"+DebugUnparser.toString(val)+
          "' on "+DebugUnparser.toString(n), n);
      JavaPromise.detachPromiseNode(n, last);
    }
    n.setSlotValue(si, val);
    // JavaPromise.attachPromiseNode(n, val);
  }

  /*****************************************************************************
	 * IRSequence(IRNode) SlotInfos for promises
	 ****************************************************************************/

  @SuppressWarnings("unchecked")
  protected static Iterator<IRNode> getIterator(SlotInfo<IRSequence<IRNode>> si, IRNode n) {
    if (n == null) {
      return new EmptyIterator<IRNode>();
    }
    IRSequence<IRNode> s = n.getSlotValue(si);
    if (s != null) {
      return s.elements();
    }
    return new EmptyIterator<IRNode>();
  }

  protected static void addToSeq(SlotInfo<IRSequence<IRNode>> si, IRNode n, IRNode elt) {
    IRSequence<IRNode> s = n.getSlotValue(si);
    if (s == null) {
      s = sf.newSequence(~1);
      n.setSlotValue(si, s);
      s.setElementAt(elt, 0);
    } else {
      // FIX check for conflicts
      /*
      final String eltS = DebugUnparser.toString(elt);
      for (IRNode existing : s.elements()) {
        String existingS = DebugUnparser.toString(existing);
        if (eltS.equals(existingS)) {
          frame.getReporter().reportProblem("Duplicate promise '"+eltS+
                                            "' on "+DebugUnparser.toString(n), elt);
        }
      }
      */
      s.appendElement(elt);
    }
    // treeChanged.noteChange(n);
  }

  protected static boolean removeFromEnum(SlotInfo<IRSequence<IRNode>> si, IRNode n, IRNode elt) {
    final IRSequence<IRNode> s = n.getSlotValue(si);
    if (s == null) {
      return false;
    }
    try {
      IRLocation loc = s.firstLocation();
      while (true) {
        if (s.elementAt(loc).equals(elt)) {
          s.removeElementAt(loc);
          JavaPromise.detachPromiseNode(n, elt);
          // treeChanged.noteChange(n);
          return true;
        }
        loc = s.nextLocation(loc);
      }
    } catch (IRSequenceException e) {
      return false;
    }
  }

  /*
	 * Filtered versions of the accessors above
   * -- incorporating proxy node support
	 */
  /*****************************************************************************
	 * Boolean SlotInfos for promises
	 ****************************************************************************/

  public static final boolean isX_filtered(SlotInfo<Boolean> si, IRNode n) {
    final IRNode n2 = frame.getProxyNode(n);

    // If there is a proxy node, and it's set to true
    if (n != n2 && isX(si, n2)) {
      return true;
    }
    // otherwise, return the original value
    if (frame.useAssumptionsOnly()) {
      return false;
    } else {
      return isX(si, n);
    }
  }

  public static final void setX_mapped(SlotInfo<Boolean> si, IRNode n, boolean val) {
    setX_private(si, frame.mapToProxyNode(n), val);
  }
  
  protected static final boolean isXorFalse_filtered(SlotInfo<Boolean> si, IRNode n) {
    return isXorFalse(si, frame.getProxyNode(n));
  }

  /*****************************************************************************
   * IRNode SlotInfos for promises
   ****************************************************************************/  
  
  public static final IRNode getXorNull_filtered(SlotInfo<IRNode> si, IRNode n) {
    final IRNode n2 = frame.getProxyNode(n);
    IRNode rv;
    
    // If there is a proxy node
    final boolean tryOrig;
    if (!n.equals(n2)) {
      // Try to use the value from there
      rv =  n2.getSlotValue(si); 
      if (rv != null && !isBogus(rv)) {
        return rv;
      }      
      tryOrig = !frame.useAssumptionsOnly();
    } else {
      tryOrig = true;
    }
    // Otherwise
    return tryOrig ? getXorNull_checked(si, n) : null;
  }
  
  private static final IRNode getXorNull_checked(SlotInfo<IRNode> si, IRNode n) {
    IRNode rv = n.getSlotValue(si);
    if (rv == null || isBogus(rv)) {
      return null;
    }
    return rv;    
  }

  protected static final void setX_mapped(SlotInfo<IRNode> si, IRNode n, IRNode val) {
    final IRNode proxy = frame.mapToProxyNode(n);
    setX_private(si, proxy, val);
    JavaPromise.attachPromiseNode(n, val);    
    // JavaPromise.attachPromiseNode(proxy, val);
  }

  /*****************************************************************************
	 * IRSequence(IRNode) SlotInfos for promises
   * 
   * Note: effects declarations have their own code for adding/removing
	 ****************************************************************************/

  public static Iteratable<IRNode> getEnum_filtered(SlotInfo<IRSequence<IRNode>> si, IRNode n) {
    final IRNode n2 = frame.getProxyNode(n);
    Iterator<IRNode> e = new EmptyIterator<IRNode>();
    
    final boolean tryOrig;
    // If there's a proxy node
    if (!n.equals(n2)) {
      e = getIterator(si, n2);
      
      tryOrig = !frame.useAssumptionsOnly();
    } else {
      // no proxy node
      tryOrig = true;
    }
    if (!e.hasNext() && tryOrig) {
      e = getIterator(si, n);
    }
    if (!e.hasNext()) {
      return new EmptyIterator<IRNode>();
    }
    return new FilterIterator<IRNode,IRNode>(e) {
      @Override
      protected Object select(IRNode o) {
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
  
  protected static void addToSeq_mapped(SlotInfo<IRSequence<IRNode>> si, IRNode n, IRNode elt) {
    final IRNode proxy = frame.mapToProxyNode(n);
    addToSeq(si, proxy, elt);
    JavaPromise.attachPromiseNode(n, elt);
    // JavaPromise.attachPromiseNode(proxy, elt);
  }
  protected static boolean removeFromEnum_mapped(SlotInfo<IRSequence<IRNode>> si, IRNode n, IRNode elt) {
    return removeFromEnum(si, frame.getProxyNode(n), elt);
  }

  /*****************************************************************************
   * Support code for handling boolean promise drops
   ****************************************************************************/

  protected static <D extends PromiseDrop> D getDrop(IDropFactory<D,Object> factory, IRNode n) {
    SlotInfo<D> dropSI = factory.getSI();
    
    final IRNode proxy = frame.getProxyNode(n);
    D drop = proxy.getSlotValue(dropSI);
    if (drop != null) {
      return drop;
    } else {
      // if it should exist, create it
      drop = factory.createDrop(n, null);
      if (drop != null) {
        proxy.setSlotValue(dropSI, drop);
        return drop;
      }
      return null;
    }
  }
}
