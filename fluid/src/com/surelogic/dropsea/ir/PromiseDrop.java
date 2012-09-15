package com.surelogic.dropsea.ir;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.InRegion;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.persistence.JavaIdentifier;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.IHasPromisedFor;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.WrappedSrcRef;

/**
 * Abstract base class for tracking all promises in the "sea" of knowledge.
 * Within the Fluid system, promises represent models of design intent or
 * cutpoints for the analyses.
 */
public abstract class PromiseDrop<A extends IAASTRootNode> extends ProofDrop implements IPromiseDrop, IHasPromisedFor {

  public static final String VIRTUAL = "virtual";

  public static final String FROM_SRC = "from-src";

  public static final String CHECKED_BY_ANALYSIS = "checked-by-analysis";

  public static final String TO_BE_CHECKED_BY_ANALYSIS = "to-be-checked-by-analysis";

  public static final String ASSUMED = "assumed";

  public static final boolean useCheckedByResults = true;
  public static final String CHECKED_BY_RESULTS = "checked-by-result";

  /**
   * Constructs a promise drop with the root of the associated annotation
   * AST&mdash;an {@link IAASTRootNode}. The passed node must be non-null.
   * 
   * @param a
   *          a non-null annotation AST node.
   * 
   * @throws IllegalArgumentException
   *           if <tt>a</tt> is null;
   * @throws IllegalStateException
   *           if the {@link IRNode} associated with <tt>a</tt> is a
   *           {@link IRNode#destroyedNode}.
   */
  public PromiseDrop(A a) {
    super(a.getPromisedFor());

    setDerivedFromSrc(true);

    if (a.getPromisedFor().identity() == IRNode.destroyedNode) {
      throw new IllegalStateException("Destroyed node for: " + a);
    }
    f_aast = a;

    final ISrcRef orig = super.getSrcRef();
    if (orig != null) {
      ISrcRef ref = orig.createSrcRef(f_aast.getOffset());
      if (ref == null) {
        ref = orig;
      }
      f_lineNumber = ref.getLineNumber();
      f_hash = ref.getHash();
    } else {
      f_lineNumber = -1;
      f_hash = -1L;
    }
  }

  public final IRNode getPromisedFor() {
    return getNode();
  }

  /**
   * Used to persist references to promiseDrops
   */
  public final String getPromiseName() {
    final String name = getClass().getSimpleName();
    if (!name.endsWith("PromiseDrop")) {
      throw new UnsupportedOperationException("No implementation for getPromiseName() for " + name);
    }
    return name.substring(0, name.length() - 11);
  }

  /**
   * Class to hold information about referenced promise annotations.
   * 
   * @see PromiseDrop.referenceRealPromise(edu.cmu.cs.fluid.ir.IRNode,String)
   */
  public static class PromiseAnnotation {

    /**
     * fAST node where the promise annotation exists.
     */
    public IRNode location;

    /**
     * Text describing the promise annotation.
     */
    public String text;
  }

  /**
   * Adds a reference to a promise annotation which partially defines this
   * promise drop. The references collected are primarily intended for use by
   * the user interface. Dependency information is <i>not</i> managed by these
   * references, hence, these should all be within a single compilation unit. <br>
   * A PromiseDrop may represent the composite of many physical promises within
   * the program source code (e.g., method effects aggregation), hence, this
   * method may be invoked many times. Often, however, only a single source
   * promise needs to be referenced, so this method would not be invoked. Rather
   * setNode() and setMessage() on the drop are used. If this PromiseDrop
   * represents a virtual promise then this method will not be invoked at all,
   * {@link #referenceScopedPromise(PromiseDrop)} should be used instead.
   * 
   * @param promise
   *          the IRNode to get a source reference for the promise annotation
   * @param message
   *          a single line version of the promise text (e.g., "borrowed this")
   * 
   * @see #referenceScopedPromise(PromiseDrop)
   */
  public final void referencePromiseAnnotation(IRNode promise, String message) {
    synchronized (f_seaLock) {
      PromiseAnnotation pa = new PromiseAnnotation();
      pa.text = message;
      pa.location = promise;
      f_promiseAnnotationSet.add(pa);
    }
  }

  /**
   * Gets the (possibly empty) set of promise annotations referenced by this
   * promise drop. All elements of this set are of type
   * {@link PromiseAnnotation}. Do <b>not</b> modify the returned set.
   * 
   * @return a non-null (possibly empty) set containing
   *         {@link PromiseAnnotation} instances.
   */
  public final HashSet<PromiseAnnotation> getReferencedPromiseAnnotations() {
    synchronized (f_seaLock) {
      return f_promiseAnnotationSet;
    }
  }

  /**
   * Returns if this PromiseDrop is <i>intended</i> to be checked by analysis or
   * not. Most promises are supported by analysis results (i.e., they have
   * ResultDrops attached to them), however some are simply well-formed (e.g.,
   * region models). If the promise is simply well-formed then it should
   * override this method and return <code>false</code>.
   * 
   * @return <code>true</code> if the PromiseDrop is intended to be checked by
   *         analysis, <code>false</code> otherwise.
   */
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }

  /**
   * Returns if this PromiseDrop has been checked by analysis or not. If this
   * PromiseDrop (or any deponent PromiseDrop of this PromiseDrop) has results
   * it is considered checked, otherwise it is considered trusted. This approach
   * is designed to allow the system to detect when a PromiseDrop that usually
   * is checked by an analysis has not been checked (i.e., the analysis is
   * turned off). This method is intended to be overridden by subclasses where
   * the default behavior is wrong. That said, however, usually the subclass
   * should override {@link #isIntendedToBeCheckedByAnalysis()} and note that
   * the promise is not intended to be checked by analysis (which will cause
   * this method to return <code>true</code>).
   * <p>
   * We currently trust XML promises as having been checked by analysis, or
   * defined by the JLS.
   * 
   * @return <code>true</code> if the PromiseDrop is considered checked by
   *         analysis, <code>false</code> otherwise.
   */
  public boolean isCheckedByAnalysis() {
    synchronized (f_seaLock) {
      if (!f_fromSrc) {
        // from XML, we trust this drop as part of the JLS
        return true;
      } else if (!isIntendedToBeCheckedByAnalysis()) {
        return true;
      } else {
        return isCheckedByAnalysis(new HashSet<PromiseDrop<?>>());
      }
    }
  }

  /**
   * Utility routine that guards against cycles in the drop-sea graph.
   * 
   * @param examinedPromiseDrops
   *          the set of promise drops examined.
   * @return <code>true</code> if the PromiseDrop is considered checked by
   *         analysis, <code>false</code> otherwise.
   * 
   * @see #isCheckedByAnalysis()
   */
  @RequiresLock("SeaLock")
  private boolean isCheckedByAnalysis(Collection<PromiseDrop<?>> examinedPromiseDrops) {
    if (examinedPromiseDrops.contains(this)) {
      /*
       * graph loop guard (and wasn't checked the first time)
       */
      return false;
    } else {
      examinedPromiseDrops.add(this); // we've now looked at this promise

      /*
       * check if any dependent result drop checks this drop (trusts doesn't
       * count)
       */
      if (!getCheckedBy().isEmpty()) {
        return true;
      }

      /*
       * check if any of our dependent promise drops are checked
       */
      @SuppressWarnings("rawtypes")
      final List<PromiseDrop> p = Sea.filterDropsOfType(PromiseDrop.class, getDependentsReference());
      for (PromiseDrop<?> promise : p) {
        if (promise.isCheckedByAnalysis(examinedPromiseDrops)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Returns a copy of the set of result drops which directly check this promise
   * drop.
   * 
   * @return a non-null (possibly empty) set which check this promise drop
   */
  public final HashSet<AnalysisResultDrop> getCheckedBy() {
    final HashSet<AnalysisResultDrop> result = new HashSet<AnalysisResultDrop>();
    /*
     * check if any dependent result drop checks this drop ("trusts" doesn't
     * count)
     */
    synchronized (f_seaLock) {
      final List<AnalysisResultDrop> ss = Sea.filterDropsOfType(AnalysisResultDrop.class, getDependentsReference());
      for (AnalysisResultDrop rd : ss) {
        if (rd.getChecksReference().contains(this)) {
          result.add(rd);
        }
      }
    }
    return result;
  }

  @Override
  @NonNull
  public final Set<ResultDrop> getTrustedBy() {
    /*
     * Only overridden to ensure that no subtype changes this call's behavior.
     */
    return super.getTrustedBy();
  }

  /**
   * Gets if this promise is assumed.
   * 
   * @return <code>true</code> if the promise is assumed, <code>false</code>
   *         otherwise.
   */
  public final boolean isAssumed() {
    synchronized (f_seaLock) {
      return f_assumed;
    }
  }

  /**
   * Sets if this promise is assumed.
   * 
   * @param value
   *          <code>true</code> if the promise is assumed, <code>false</code>
   *          otherwise.
   */
  public final void setAssumed(boolean value) {
    synchronized (f_seaLock) {
      f_assumed = value;
    }
  }

  /**
   * Gets if this promise is virtual.
   * 
   * @return <code>true</code> if the promise is virtual, <code>false</code>
   *         otherwise.
   */
  public final boolean isVirtual() {
    synchronized (f_seaLock) {
      return f_virtual;
    }
  }

  /**
   * Sets if this promise is virtual.
   * 
   * @param value
   *          <code>true</code> if the promise is virtual, <code>false</code>
   *          otherwise.
   */
  public final void setVirtual(boolean value) {
    synchronized (f_seaLock) {
      f_virtual = value;
    }
  }

  /**
   * Returns if this promise is from source code or from another location, such
   * as XML. The default value for a promise drop is <code>true</code>.
   * 
   * @return <code>true</code> if the promise was created from an annotation in
   *         source code, <code>false</code> otherwise
   */
  @Override
  public final boolean isFromSrc() {
    synchronized (f_seaLock) {
      return f_fromSrc;
    }
  }

  /**
   * Sets if this promise is from source code or from another location, such as
   * XML. The default value for a promise drop is <code>true</code>.
   * 
   * @param fromSrc
   *          <code>true</code> if the promise was created from an annotation in
   *          source code, <code>false</code> otherwise
   */
  public final void setFromSrc(boolean fromSrc) {
    synchronized (f_seaLock) {
      this.f_fromSrc = fromSrc;
      setDerivedFromSrc(fromSrc);
    }
  }

  /**
   * Gets the annotation AST for this promise. The value is a subtype of
   * {@link IAASTRootNode}.
   * 
   * @return the annotation AST for this promise.
   */
  @NonNull
  public final A getAAST() {
    return f_aast;
  }

  @Override
  @RequiresLock("SeaLock")
  protected void invalidate_internal() {
    super.invalidate_internal();

    if (getAAST() != null) {
      getAAST().clearPromisedFor();
    }
  }

  @Override
  public final ISrcRef getSrcRef() {
    final ISrcRef ref = super.getSrcRef();
    if (ref != null) {
      return new WrappedSrcRef(ref) {
        public String getJavaId() {
          final IRNode decl = getNode();
          return decl == null ? null : JavaIdentifier.encodeDecl(decl);
        }

        public int getLineNumber() {
          return f_lineNumber;
        }

        public int getOffset() {
          return f_aast.getOffset();
        }

        public Long getHash() {
          return f_hash;
        }
      };
    } else
      return null;
  }

  /**
   * @return the PromiseDrop that this one was created from, or null if this is
   *         the source
   */
  public PromiseDrop<? extends IAASTRootNode> getSourceDrop() {
    synchronized (f_seaLock) {
      return f_source;
    }
  }

  public void setSourceDrop(PromiseDrop<? extends IAASTRootNode> drop) {
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));

    synchronized (f_seaLock) {
      if (f_source != null && f_source.isValid()) {
        if (f_source == drop) {
          LOG.log(Level.WARNING, "Re-setting source drop to its current value: " + drop, new Exception("trace"));
          return;
        }
        throw new IllegalArgumentException("This PromiseDrop already has a source drop: set-to=" + f_source + " changing-to="
            + drop);
      }
      f_source = drop;
      drop.addDependent(this);
    }
  }

  /**
   * Set of promise annotations composing this promise drop.
   */
  @UniqueInRegion("DropState")
  private final HashSet<PromiseAnnotation> f_promiseAnnotationSet = new HashSet<PromiseAnnotation>();

  /**
   * {@code true} if this promise drop represents an assumed promise,
   * {@code false} otherwise.
   */
  @InRegion("DropState")
  private boolean f_assumed = false;

  /**
   * {@code true} if this promise drop represents a virtual promise,
   * {@code false} otherwise.
   */
  @InRegion("DropState")
  private boolean f_virtual = false;

  /**
   * {@code true} if this promise is from source code or {@code false} if this
   * promise is from another location, such as XML.
   */
  @InRegion("DropState")
  private boolean f_fromSrc = true;

  /**
   * Annotation AST for this drop
   */
  @NonNull
  private final A f_aast;
  private final Long f_hash;
  private final int f_lineNumber;

  @InRegion("DropState")
  private PromiseDrop<? extends IAASTRootNode> f_source;

  @Override
  @RequiresLock("SeaLock")
  protected JavaSourceReference createSourceRef() {
    IRNode n = getNode();
    if (n == null) {
      n = getAAST().getPromisedFor();
    }
    return DropSeaUtility.createJavaSourceReferenceFromOneOrTheOther(n, getSrcRef());
  }

  @Override
  @RequiresLock("SeaLock")
  final void proofInitialize() {
    // for a promise drop we flag a red dot if it is not checked by analysis
    setProofUsesRedDot(!isCheckedByAnalysis());
    if (isAssumed())
      setProofUsesRedDot(true);

    // if no immediate result drops are an "X" then we are consistent
    setProvedConsistent(true); // assume true
    setDerivedFromSrc(isFromSrc());

    Collection<AnalysisResultDrop> analysisResults = getCheckedBy();
    for (AnalysisResultDrop result : analysisResults) {
      /*
       * & in local result (only real results not folders)
       */
      if (result instanceof ResultDrop) {
        ResultDrop r = (ResultDrop) result;
        setProvedConsistent(provedConsistent() && (r.isConsistent() || r.isVouched()));
      }
      setDerivedFromSrc(derivedFromSrc() || result.isFromSrc());
    }

  }

  @Override
  @RequiresLock("SeaLock")
  final void proofTransfer() {
    // examine dependent analysis results and dependent promises
    final Set<ProofDrop> proofDrops = new HashSet<ProofDrop>(getCheckedBy());
    proofDrops.addAll(Sea.filterDropsOfType(PromiseDrop.class, getDependents()));
    for (ProofDrop result : proofDrops) {
      // all must be consistent for this promise to be consistent
      setProvedConsistent(provedConsistent() & result.provedConsistent());
      // any red dot means this promise depends upon a red dot
      if (result.proofUsesRedDot())
        setProofUsesRedDot(true);
      // push along if derived from source code
      setDerivedFromSrc(derivedFromSrc() | result.derivedFromSrc());
    }
  }

  @Override
  @RequiresLock("SeaLock")
  final void proofAddToWorklistOnChange(Collection<ProofDrop> mutableWorklist) {
    // add all result drops trusted by this promise
    mutableWorklist.addAll(getTrustedBy());
    // add all deponent promise drops of this promise
    mutableWorklist.addAll(Sea.filterDropsOfType(PromiseDrop.class, getDeponentsReference()));
  }

  /*
   * XML Methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return "promise-drop";
  }

  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(ASSUMED, isAssumed());
    s.addAttribute(CHECKED_BY_ANALYSIS, isCheckedByAnalysis());
    s.addAttribute(FROM_SRC, isFromSrc());
    s.addAttribute(VIRTUAL, isVirtual());
    s.addAttribute(TO_BE_CHECKED_BY_ANALYSIS, isIntendedToBeCheckedByAnalysis());
  }

  @Override
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    if (useCheckedByResults) {
      for (Drop c : getCheckedBy()) {
        s.refDrop(db, CHECKED_BY_RESULTS, c);
      }
    }
  }
}