package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.target.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/**
 * @author chance
 */
@Deprecated
public final class ScopedPromises extends AbstractPromiseAnnotation {
  private ScopedPromises() {
  }

  private static final ScopedPromises instance = new ScopedPromises();

  public static final ScopedPromises getInstance() {
    return instance;
  }

  public IPromiseParser getParser() {
    return PromiseFramework.getInstance().getParser();
  }
  
  static SlotInfo<IRSequence<IRNode>> assureSI;
  static SlotInfo<IRSequence<IRNode>> assumeSI;
  static SlotInfo<Map<IRNode,PromisePromiseDrop>> promiseDropSI = SimpleSlotFactory.prototype.newAttribute(); 
  static SlotInfo<Map<IRNode,AssumePromiseDrop>> assumeDropSI = SimpleSlotFactory.prototype.newAttribute(); 
  private PromiseDrop currentDrop;
  
  public void setCurrentDrop(PromiseDrop drop) {
    if (drop != null) {
      currentDrop = drop;
    }
  }
  public void clearCurrentDrop() {
    currentDrop = null;
  }
  
  public void initDrop(PromiseDrop drop) {
    XML e = XML.getDefault();
    if (e == null) {
      drop.setFromSrc(false);
      return;
    }
    if (e.processingXML() && !e.processingXMLInSrc()) {
      drop.setFromSrc(false);
    }
    
    if (currentDrop == null) {
      //System.out.println("Ignoring call to init(): "+drop.getMessage());
      return; // nothing to do
    }
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Adding dependency from "+currentDrop.getMessage()+" to "+drop.getClass().getName());
    }
    currentDrop.addDependent(drop);
    if (currentDrop instanceof AssumePromiseDrop) {
      drop.setAssumed(true);
    }
    else if (currentDrop instanceof PromisePromiseDrop ||
             currentDrop instanceof MapFieldsPromiseDrop) {
      /*
      if (currentDrop.isFromSrc()) {
    	  System.out.println("Processing "+drop.getMessage());
      }
      */
      drop.setVirtual(true);
    }
  }
  
  public void initDrop(PhantomDrop drop) {
    if (XML.getDefault().processingXML()) {
      drop.setFromSrc(false);
    }
  }

  public static Iteratable<IRNode> assuredPromises(IRNode classNode) {
    return getEnum_filtered(assureSI, classNode);
  }

  /** 
   */
  public static void addAssuredPromise(IRNode declNode, IRNode colorNode) {
    addToSeq_mapped(assureSI, declNode, colorNode);
  }

  /** 
   */
  public static boolean removeAssuredPromise(
    IRNode classNode,
    IRNode promiseNode) {
    return removeFromEnum_mapped(assureSI, classNode, promiseNode);
  }

  public static Iteratable<IRNode> assumedPromises(IRNode n) {
    // filtered but without proxy elements
    final Iterator<IRNode> e  = getIterator(assumeSI, n);
        
    if (e.hasNext()) {
      return new FilterIterator<IRNode,IRNode>(e) {
        @Override protected Object select(IRNode o) {
          if (o == null) {
            return null;
          }
          return isBogus(o) ? IteratorUtil.noElement : o;
        }
      };
    }
    return new EmptyIterator<IRNode>();
  }  
  
  /** 
   */
  public static void addAssumedPromise(IRNode declNode, IRNode assumeNode) {
    addToSeq(assumeSI, declNode, assumeNode);
    JavaPromise.attachPromiseNode(declNode, assumeNode);
  }

  /** 
   */
  public static boolean removeAssumedPromise(
    IRNode classNode,
    IRNode promiseNode) {
    return removeFromEnum(assumeSI, classNode, promiseNode);
  }  

  static interface Factory<D extends ScopedPromiseDrop> {
    Iteratable<IRNode> getPromises(IRNode n);
    D create();
  }
  private static final Factory<AssumePromiseDrop> assumeFactory = new Factory<AssumePromiseDrop>() {
    public AssumePromiseDrop create() {
      AssumePromiseDrop result = new AssumePromiseDrop(null);
      result.setCategory(JavaGlobals.ASSUME_CAT);
      return result;
    }
    public Iteratable<IRNode> getPromises(IRNode n) {
      return assumedPromises(n);
    }    
  };
  
  private static final Factory<PromisePromiseDrop> promiseFactory = new Factory<PromisePromiseDrop>() {
    public PromisePromiseDrop create() {
      PromisePromiseDrop result = new PromisePromiseDrop(null);
      result.setCategory(JavaGlobals.PROMISE_CAT);
      return result;
    }
    public Iteratable<IRNode> getPromises(IRNode n) {
      return assuredPromises(n);
    }    
  };  
  
  public static Iterator<AssumePromiseDrop> getAssumeDrops(IRNode node) {
    return getDrops(node, assumeDropSI, assumeFactory);
  }
  
  public static Iterator<PromisePromiseDrop> getPromiseDrops(IRNode node) {
    return getDrops(node, promiseDropSI, promiseFactory);
  }
  
  private static <D extends ScopedPromiseDrop> D getDrop(IRNode node, final SlotInfo<Map<IRNode,D>> si, final Factory<D> factory, IRNode promise) {
    final Map<IRNode,D> drops = getDropMap(node, si);
    
    D d = factory.create();
    initDrop(d, node, promise);
    drops.put(promise, d);    
    return d;
  }
  
  private static <D extends ScopedPromiseDrop> Iterator<D> getDrops(IRNode node, final SlotInfo<Map<IRNode,D>> si, final Factory<D> factory) {
    final Iterator<IRNode> promises = factory.getPromises(node);
    Map<IRNode,D> drops = null;
    
    if (promises.hasNext()) {
      drops = getDropMap(node, si);

      while (promises.hasNext()) {
        IRNode promise = promises.next();

        if (drops.containsKey(promise)) {
          continue;
        }
        D d = factory.create();
        initDrop(d, node, promise);
        drops.put(promise, d);
      }
    }
    // TODO how do I delete drops?
    return getDrops(drops);
  }

  private static <D extends ScopedPromiseDrop> Map<IRNode,D> getDropMap(IRNode node, final SlotInfo<Map<IRNode,D>> si) {
    Map<IRNode,D> drops = null;
    if (node.valueExists(si)) {
      drops = node.getSlotValue(si);
    } 
    if (drops == null) {
      drops = new HashMap<IRNode,D>(); 
      node.setSlotValue(si, drops);
    }
    return drops;
  }
  
  private static void initDrop(ScopedPromiseDrop d, IRNode n, IRNode promise) {
    d.setNode(promise);
    d.dependUponCompilationUnitOf(n);
    d.setMessage((d instanceof AssumePromiseDrop ? "assume \"" : "promise \"")
        + ScopedPromise.getPromise(promise) + "\" for "
        + DebugUnparser.toString(ScopedPromise.getTargets(promise)) + " on "
        + JavaNames.getTypeName(n));
  }
  
  private static <D extends ScopedPromiseDrop> Iterator<D> getDrops(Map<IRNode,D> drops) {
    if (drops == null) {
      return new EmptyIterator<D>();
    }
    final Iterator<Map.Entry<IRNode,D>> it = drops.entrySet().iterator();
    return new SimpleRemovelessIterator<D>() {
      @Override
      protected Object computeNext() {
        if (!it.hasNext()) {
          return noElement;
        }
        Map.Entry<IRNode,D> e = it.next();
        return e.getValue();
      }
    };
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] { 
		  new ScopedPromise_ParseRule("Promise", packageTypeDeclOps) { 
		  	public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) { assureSI = si;
          return new TokenInfo<IRSequence<IRNode>>("Assured promises", si, "Promise");
        }
        @Override
        public boolean checkSanity(Operator op, IRNode promisedFor,
            IPromiseCheckReport report) {
          getPromiseDrops(promisedFor);
          return true;
        }
      }, 
      new ScopedPromise_ParseRule("Assume", declOrConstructorOps) { 
        public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) { assumeSI = si;
          return new TokenInfo<IRSequence<IRNode>>("Assumed promises", si, "Assume");
        }
        @Override
        protected boolean processResult(final IRNode n, 
                      						      final IRNode promise,
			                            			IPromiseParsedCallback cb) {
        	// promises to be added to declarations in a later pass
          if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Got @assume: "+ScopedPromise.getPromise(promise));
          }
          addAssumedPromise(n, promise);
          return true;
        }
        @Override
        public boolean checkSanity(Operator op, IRNode promisedFor,
            IPromiseCheckReport report) {
          getAssumeDrops(promisedFor);
          return true;
        }        
      },
      new CheckPackageDeclarationsRule(),
    };
  }

  /**
   * Only allows @module
   */
  static class PkgDeclChecker extends PromiseFramework.HasPromisesProcessor {
    @Override
    public String getIdentifier() {
      return "Package decl checker";
    }
    
    @Override
    public void processNodePromise(IRNode n, TokenInfo info, IRNode sub) {
      if (ModulePromises.isModuleSI(info.si)) {
        // ignore this
        return;
      } 
      super.processNodePromise(n, info, sub);     
    } 
  }
  
  public static final IPromiseProcessor invalidator = new AbstractPromiseProcessor() {
    public String getIdentifier() {
      return "Promise invalidator";
    }

    @Override
    public void processNodePromise(IRNode n, TokenInfo info, IRNode sub) {
      if (ModulePromises.isModuleSI(info.si)) {
        // ignore this
        return;
      } 
      setBogus(sub, true);
    }

    @Override
    public void processSequencePromise(IRNode n, TokenInfo info, Iteratable<IRNode> e) {
      for(IRNode sub : e) {
        setBogus(sub, true);
      }
    }    
  };
  
  static class CheckPackageDeclarationsRule implements IPromiseCheckRule {
    public boolean checkSanity(Operator op, IRNode promisedFor, IPromiseCheckReport report) {
      // This shouldn't be called on package-info.java
      if (PackageDeclaration.prototype.includes(op)) {
        PkgDeclChecker c = new PkgDeclChecker();
        PromiseFramework.getInstance().processPromises(promisedFor, c);
        if (c.found) {
          report.reportError("Package declaration should not have any promises", promisedFor);

          // invalidate all promises (except @module) on this node
          PromiseFramework.getInstance().processPromises(promisedFor, invalidator);
          return false;
        }
      }
      return true;
    }

    private static final Operator[] ops = { PackageDeclaration.prototype };
    public Operator[] getOps(Class type) {
      return ops;
    }    
  }
  
  abstract class ScopedPromise_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {
    ScopedPromise_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, false, ops, ops);
    }

    /**
     * Designed for @promise
     * Overridden for @assume above
     */
    @Override
    protected boolean processResult(final IRNode n, 
																		final IRNode result,
																		IPromiseParsedCallback cb) {
      if (true) {
        LOG.info("Ignoring @Promise with the old XML parser in the new system");
        return true;
      }
      if (n==null) {
        LOG.severe("promisedFor is null for " + DebugUnparser.toString(result));
        return false;
      }
      
      addAssuredPromise(n, result);
      // XXX hack to get src ref produced early
      cb.parsed(result);      
      final ISrcRef ref = JavaNode.getSrcRef(result);
      
      currentDrop = getDrop(n, promiseDropSI, promiseFactory, result);
      
      try {
        final Operator op = tree.getOperator(n);
        // handle  on type
        if (TypeDeclaration.prototype.includes(op)) {      	
          return processAssurance(result, n, ref);
        }
        else if (op instanceof PackageDeclaration) { //.prototype.includes(op)) {
          // Handled when the classes in the package are loaded.
          return true;
        }
        return false;
      }
      finally { 
        currentDrop = null;
      }
    }
  }

  /**
   * 
   * @param assurance
   * @param type
   * @param ref Source ref for the original @promise
   * @return
   */
  public boolean processAssurance(IRNode assurance, IRNode type, ISrcRef ref) {
    if (ref == null) {
      LOG.warning("Src ref is null for @promise"); 
      ref = DummySrcRef.prototype;
    }
    final boolean debug = LOG.isLoggable(Level.FINE);
    
    // search for matches
    final String promise = ScopedPromise.getPromise(assurance);
    final IRNode targets = ScopedPromise.getTargets(assurance);
    final Iterator<IRNode> it = findTargetsInType(type, targets);
    if (debug) {
      LOG.fine("Applying "+promise+" to targets: "+DebugUnparser.toString(targets));
    } 
    while (it.hasNext()) {
      IRNode target = it.next();
      if (debug) {
        LOG.fine("Parsing promise on target: "+ DebugUnparser.toString(target));          
      }
      callback.init(assurance, ref);
      if (!createVirtualPromise(target, promise, callback)) {
        return false; // stop at error since probably all wrong
      }
      if (debug) {
        LOG.fine("Target after: "+DebugUnparser.toString(target));      
      }
    }
    return true;    
  }
  
	// Actually, creates a new promise -- need to set the SrcRef to point to scoped promise
  public boolean createVirtualPromise(IRNode target, String promise, IPromiseParsedCallback callback) {				
    // should this be called again?
		return PromiseFramework.getInstance().getParser().parsePromise(target, promise, callback);
	}  
  
  private static class TargetIterator extends SimpleRemovelessIterator<IRNode> {
    private final Iterator<IRNode> nodes;
    private final ITargetMatcher matcher;
    
    public TargetIterator(IRNode type, Iterator<IRNode> nodes, ITargetMatcher matcher) {
      super(type);
      this.nodes = nodes;
      this.matcher = matcher;
    }

    public TargetIterator(Iterator<IRNode> nodes, ITargetMatcher matcher) {
      this.nodes = nodes;
      this.matcher = matcher;
    }

    @Override
    protected Object computeNext() {
      while (nodes.hasNext()) {
        IRNode n = nodes.next();
        Operator op = tree.getOperator(n);

        // TODO filter out non-decls
        /*
        if (!Declaration.prototype.includes(op)) {
          continue;
        }
        */
        if (ClassInitializer.prototype.includes(op)) {
          continue;
        }
        if (matcher.match(n, op)) {
          return n;
        }
      }
      return noElement;
    }
  }
  
	/**
	 * @return
	 */
	static Iteratable<IRNode> findTargetsInType(IRNode type, final IRNode targets) {
    final ITargetMatcher matcher = TargetMatcherFactory.prototype.create(targets);
    
		// TODO fix so it covers the type and nested classes
    // tree.topDown(type);
		final Iterator<IRNode> nodes = VisitUtil.getClassBodyMembers(type);		
    return matcher.match(type) ? new TargetIterator(type, nodes, matcher) : new TargetIterator(nodes, matcher);
	}  

  /**
   * Temporary
   */
  public void processCuAssumptions(IBinder binder, IRNode cu) {
    Set bindings = null;
    //final Iterator types = VisitUtil.getTypeDecls(cu);
    final Iterator nodes = tree.bottomUp(cu);

    frame.clearTypeContext(cu);
    
    while (nodes.hasNext()) { 
      final IRNode n    = (IRNode) nodes.next();   
      final Operator op = tree.getOperator(n);
      if (ClassBodyDeclaration.prototype.includes(op) || TypeDeclaration.prototype.includes(op)) {
        bindings = processDeclAssumptions(binder, cu, n, bindings);
      } 
    }
  }
  
  private Set processDeclAssumptions(IBinder binder, final IRNode cu, IRNode n, Set bindings) {
    final Iterator enm = assumedPromises(n); // Unmapped
    if (!enm.hasNext()) {
      return bindings; // nothing to do
    }
    if (bindings == null) {
      bindings = collectBoundDecls(binder, cu);
    }
    frame.pushTypeContext(cu, true, true); // create one if there isn't one
    processAssumptions(n, bindings, enm);    
    // already pop'd above
    
    return bindings;
  }
  
  /*
  private void processAssumptions(IRNode type) {
  	final Iterator enum = assumedPromises(type); // Unmapped
  	if (!enum.hasNext()) {
  		return; // nothing to do
  	}
  	final Set bindings = collectBoundDecls(type);

    frame.pushTypeContext(type, true, true); // create one if there isn't one
    processAssumptions(type, bindings, enum);
    //  already pop'd above
  }  
  */
  
  /**
   * Assuming context already set
   * @param bindings The decls to match scoped assumptions against
   * @param assumptions
   */
  private void processAssumptions(IRNode n, Set bindings, Iterator assumptions) {
    final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
    
  assumes:
  	while (assumptions.hasNext()) {
  		final IRNode assume  = (IRNode) assumptions.next();
  		final String promise = ScopedPromise.getPromise(assume); 
  		final IRNode targets = ScopedPromise.getTargets(assume); 
      final ISrcRef ref    = JavaNode.getSrcRef(assume);
      
      final ITargetMatcher matcher = TargetMatcherFactory.prototype.create(targets);      
      currentDrop = getDrop(n, assumeDropSI, assumeFactory, assume);      

      final Iterator it = bindings.iterator();
  		while (it.hasNext()) {
  			final IRNode decl = (IRNode) it.next();
  			final Operator op = tree.getOperator(decl);
    		if (matcher.match(decl, op)) {
          if (true) {
            IRNode type = VisitUtil.getEnclosingType(decl);       
            if (type == null) {
              // Probably because decl is a type
              type = decl;
            }
            if (fineIsLoggable) {
              LOG.fine("Matched for @"+promise+" in "+TypeDeclaration.getId(type)+" : "+DebugUnparser.toString(decl));
            }
          }
          callback.init(assume, ref);
          
  				if (!createVirtualPromise(decl, promise, callback)) { // Mapped   			
  					// the promise is probably bad
            currentDrop = null;
  				  continue assumes;
  				} 
    		}
  		}
      currentDrop = null;
  	}
    frame.popTypeContext();
  }
  
  /**
   * Collect up all unique bindings in the type
   * -- not including those in enclosed types (TODO)
   * -- compensating for those that aren't ClassBodyDecls
   * 
   * TODO what about bindings in promises themselves?
   * 
   * @param type
   * @return
   */
  Set collectBoundDecls(IBinder binder, IRNode type) {
    return collectBoundDecls(binder, type, false);
  }
  
  Set<IRNode> collectBoundDecls(IBinder binder, final IRNode type, final boolean includeOriginal) {
  	final Set<IRNode> decls = new HashSet<IRNode>();
    
  	final Iterator<IRNode> nodes = tree.bottomUp(type);
  	while (nodes.hasNext()) {
  		final IRNode n = nodes.next();
			final Operator op = tree.getOperator(n);

			if (op instanceof IHasBinding) {
        IRNode decl = binder.getBinding(n);        
        if (decl == null) {
          continue;
        }
        IRNode decl2 = findEnclosingDecl(decl);
        if (decl2 != null) {
          decl = decl2;          
        }
  		  decls.add(decl);
			}
  	}
    if (LOG.isLoggable(Level.FINE)) {
      final Iterator<IRNode> it = decls.iterator();
      while (it.hasNext()) {
        final IRNode decl = it.next();        
        LOG.fine("Collected binding: "+DebugUnparser.toString(decl));
      }
    }  
  	return decls; // Collections.EMPTY_SET;
  }
  
  public static IRNode findEnclosingDecl(IRNode decl) {
    final Operator dop = tree.getOperator(decl);
    IRNode decl2 = null;
    
    if (ReturnType.prototype.includes(dop) ||
        ClassBodyDeclaration.prototype.includes(dop) ||
        TypeDeclaration.prototype.includes(dop) ||
        PackageDeclaration.prototype.includes(dop)) {
      return null;
    } 
    decl2 = VisitUtil.getEnclosingClassBodyDecl(decl);
    if (decl2 == null) {
      final IRNode parent = JavaPromise.getParentOrPromisedFor(decl);
      if (parent == null) {
        return null;
      }
      final Operator pop  = tree.getOperator(parent);
      if (ReceiverDeclaration.prototype.includes(dop) && 
          InitDeclaration.prototype.includes(pop)) {
        LOG.info("Ignoring receiver node of init decl"); 
      } else {
        LOG.warning("Not inside a ClassBodyDecl: "+DebugUnparser.toString(decl)+
                    " inside parent = "+
                    DebugUnparser.toString(JavaPromise.getParentOrPromisedFor(decl)));
      }
      return null;
    }           
    return decl2;       
  }
  
  private VirtualPromiseCallback callback = new VirtualPromiseCallback();
  
  private static class VirtualPromiseCallback extends AbstractPromiseParsedCallback {
    protected Logger LOG = AbstractPromiseAnnotation.LOG;      

    // Both to be (re)-used
    protected IRNode here;    
    protected ISrcRef ref;
    
    void init(IRNode here, ISrcRef ref) {
      this.here = here;
      this.ref  = ref; 
    }
    
    @Override
    public void noteProblem(String description) {
      super.noteProblem(description);
      PromiseFramework.getInstance().getReporter().reportProblem(description, here);
    }

    public void noteWarning(String description) {
      super.noteProblem(description);
      PromiseFramework.getInstance().getReporter().reportWarning(description, here);
    }
    
    /**
     * Try to get the IResource from the declaration the promise is about
     */
    protected Object getEnclosingFile(IRNode promise) {
      Object file = null;
      IRNode promisedFor = JavaPromise.getPromisedFor(promise);
      if (promisedFor != null) {
        final ISrcRef rsr = JavaNode.getSrcRef(promisedFor);
        if (rsr != null) {
          file = rsr.getEnclosingFile();
        }
      }
      return file;
    }      

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.AbstractPromiseParsedCallback#finish()
     */
    @Override
    protected void finish() { 
    }

    @Override
    public void parsed(IRNode n) {
      if (n == null) {
        return;
      }
      // calls finish()
      super.parsed(n);

      if (ref != null) {
    	JavaNode.setSrcRef(n, ref);
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("ref = " + ref.toString());
        }
      } else {
        LOG.warning("No src ref for "+DebugUnparser.toString(n));
      }
    }    
  }  
}
