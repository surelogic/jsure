package edu.cmu.cs.fluid.sea;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.persistence.JavaIdentifier;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.IHasPromisedFor;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.ScopedPromises;
import edu.cmu.cs.fluid.java.comment.IJavadocElement;
import edu.cmu.cs.fluid.sea.drops.promises.ModelDrop;
import edu.cmu.cs.fluid.sea.xml.*;

/**
 * Abstract base class for tracking all promises in the "sea" of knowledge.
 * Within the Fluid system, promises represent models of design intent or
 * cutpoints for the analyses.
 */
public abstract class PromiseDrop<A extends IAASTRootNode> extends ProofDrop
		implements ISrcRef, IHasPromisedFor {
	public static final String VIRTUAL = "virtual";

	public static final String FROM_SRC = "from-src";

	public static final String CHECKED_BY_ANALYSIS = "checked-by-analysis";
	
	public static final String TO_BE_CHECKED_BY_ANALYSIS = "to-be-checked-by-analysis";

	public static final String ASSUMED = "assumed";

	public static final boolean useCheckedByResults = true;
	public static final String CHECKED_BY_RESULTS = "checked-by-result";
	
	public PromiseDrop(A a) {
		// set up dependencies if this is being created within a ScopedPromise
		ScopedPromises.getInstance().initDrop(this);
		if (a != null) {
			setAST(a);
		} else {
			setMessage(this.getClass().getName());
		}
	}

	public PromiseDrop() {
		this(null);
	}
	
	public final IRNode getPromisedFor() {
		return getNode();
		//return getAST().getPromisedFor();
	}
	
	/**
	 * Used to persist references to promiseDrops
	 */
	public String getPromiseName() {
		final String name = getClass().getSimpleName();
		if (!name.endsWith("PromiseDrop")) {
			throw new UnsupportedOperationException("No impl for getPromiseName() for "+name);
		}
		return name.substring(0, name.length()-11);
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
	 * the user interface. Dependency information is <i>not</i> managed by
	 * these references, hence, these should all be within a single compilation
	 * unit. <br>
	 * A PromiseDrop may represent the composite of many physical promises
	 * within the program source code (e.g., method effects aggregation), hence,
	 * this method may be invoked many times. Often, however, only a single
	 * source promise needs to be referenced, so this method would not be
	 * invoked. Rather setNode() and setMessage() on the drop are used. If this
	 * PromiseDrop represents a virtual promise then this method will not be
	 * invoked at all, {@link #referenceScopedPromise(PromiseDrop)} should be
	 * used instead.
	 * 
	 * @param promise
	 *            the IRNode to get a source reference for the promise
	 *            annotation
	 * @param message
	 *            a single line version of the promise text (e.g., "borrowed
	 *            this")
	 * 
	 * @see #referenceScopedPromise(PromiseDrop)
	 */
	public final void referencePromiseAnnotation(IRNode promise, String message) {
		PromiseAnnotation pa = new PromiseAnnotation();
		pa.text = message;
		pa.location = promise;
		promiseAnnotationSet.add(pa);
	}

	/**
	 * Gets the (possibly empty) set of promise annotations referenced by this
	 * promise drop. All elements of this set are of type
	 * {@link PromiseAnnotation}.
	 * 
	 * @return a set containing PromiseAnnotations (non null)
	 */
	public Set<PromiseAnnotation> getReferencedPromiseAnnotations() {
		return promiseAnnotationSet;
	}

	/**
	 * Returns if this PromiseDrop is <i>intended</i> to be checked by analysis
	 * or not. Most promises are supported by analysis results (i.e., they have
	 * ResultDrops attached to them), however some are simply well-formed (e.g.,
	 * region models). If the promise is simply well-formed then it should
	 * override this method and return <code>false</code>.
	 * 
	 * @return <code>true</code> if the PromiseDrop is intended to be checked
	 *         by analysis, <code>false</code> otherwise.
	 */
	public boolean isIntendedToBeCheckedByAnalysis() {
		return true;
	}

	/**
	 * Returns if this PromiseDrop has been checked by analysis or not. If this
	 * PromiseDrop (or any deponent PromiseDrop of this PromiseDrop) has results
	 * it is considered checked, otherwise it is considered trusted. This
	 * approach is designed to allow the system to detect when a PromiseDrop
	 * that usually is checked by an analysis has not been checked (i.e., the
	 * analysis is turned off). This method is intended to be overridden by
	 * subclasses where the default behavior is wrong. That said, however,
	 * usually the subclass should override
	 * {@link #isIntendedToBeCheckedByAnalysis()} and note that the promise is
	 * not intended to be checked by analysis (which will cause this method to
	 * return <code>true</code>).
	 * <p>
	 * We currently trust XML promises as having been checked by analysis, or
	 * defined by the JLS.
	 * 
	 * @return <code>true</code> if the PromiseDrop is considered checked by
	 *         analysis, <code>false</code> otherwise.
	 */
	public boolean isCheckedByAnalysis() {
		if (!fromSrc) {
			// from XML, we trust this drop as part of the JLS
			return true;
		} else if (!isIntendedToBeCheckedByAnalysis()) {
			return true;
		} else {
			return isCheckedByAnalysis(new HashSet<PromiseDrop>());
		}
	}

	/**
	 * Utility routine that guards against cycles in the drop-sea graph.
	 * 
	 * @param examinedPromiseDrops
	 *            the set of promise drops examined.
	 * @return <code>true</code> if the PromiseDrop is considered checked by
	 *         analysis, <code>false</code> otherwise.
	 * 
	 * @see #isCheckedByAnalysis()
	 */
	private boolean isCheckedByAnalysis(Set<PromiseDrop> examinedPromiseDrops) {
		if (examinedPromiseDrops.contains(this)) {
			return false; // graph loop guard (and wasn't checked the first
							// time)
		} else {
			examinedPromiseDrops.add(this); // we've now looked at this promise

			// check if any dependent result drop checks this drop (trusts
			// doesn't
			// count)
			if (!getCheckedBy().isEmpty()) {
				return true;
			}

			// check if any of our dependent promise drops are checked
			Set<? extends PromiseDrop> p = Sea.filterDropsOfTypeMutate(
					PromiseDrop.class, getDependents());
			for (PromiseDrop promise : p) {
				// PromiseDrop promise = (PromiseDrop) i.next();
				if (promise.isCheckedByAnalysis(examinedPromiseDrops)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Returns the set of result drops which directly check this promise drop.
	 * 
	 * @return a set, all members of the type {@link ResultDrop}, which check
	 *         this promise drop
	 */
	public final Set<? extends ResultDrop> getCheckedBy() {
		Set<ResultDrop> result = new HashSet<ResultDrop>();
		// check if any dependent result drop checks this drop ("trusts" doesn't
		// count)
		Set<? extends ResultDrop> s = Sea.filterDropsOfTypeMutate(
				ResultDrop.class, getDependents());
		for (ResultDrop rd : s) {
			// ResultDrop rd = i.next();
			if (rd.getChecks().contains(this)) {
				result.add(rd);
			}
		}
		return result;
	}

	/**
	 * Returns the set of result drops which directly trust (as an "and" or an
	 * "or" precondition) this promise drop.
	 * 
	 * @return a set, all members of the type {@link ResultDrop}, which trust
	 *         this promise drop
	 */
	public final Set<? extends ResultDrop> getTrustedBy() {
		Set<ResultDrop> result = new HashSet<ResultDrop>();
		// check if any dependent result drop trusts this drop ("checks" doesn't
		// count)
		Set<? extends ResultDrop> s = Sea.filterDropsOfTypeMutate(
				ResultDrop.class, getDependents());
		for (ResultDrop rd : s) {
			// ResultDrop rd = (ResultDrop) i.next();
			if (rd.getTrustsComplete().contains(this)) {
				result.add(rd);
			}
		}
		return result;
	}

	/**
	 * @return <code>true</code> if the promise is assumed, <code>false</code>
	 *         otherwise.
	 */
	public final boolean isAssumed() {
		return assumed;
	}

	/**
	 * @param isAssumed
	 *            <code>true</code> if the promise is assumed,
	 *            <code>false</code> otherwise.
	 */
	public final void setAssumed(boolean isAssumed) {
		assumed = isAssumed;
	}

	/**
	 * @return <code>true</code> if the promise is virtual, <code>false</code>
	 *         otherwise.
	 */
	public final boolean isVirtual() {
		return virtual;
	}

	/**
	 * @param isVirtual
	 *            <code>true</code> if the promise is virtual,
	 *            <code>false</code> otherwise.
	 */
	public final void setVirtual(boolean isVirtual) {
		virtual = isVirtual;
	}

	/**
	 * Returns if this promise is from source code or from another location,
	 * such as XML. The default value for a promise drop is <code>true</code>.
	 * 
	 * @return <code>true</code> if the promise was created from an annotation
	 *         in source code, <code>false</code> otherwise
	 */
	public final boolean isFromSrc() {
		return fromSrc;
	}

	/**
	 * Sets if this promise is from source code or from another location, such
	 * as XML. The default value for a promise drop is <code>true</code>.
	 * 
	 * @param fromSrc
	 *            <code>true</code> if the promise was created from an
	 *            annotation in source code, <code>false</code> otherwise
	 */
	public final void setFromSrc(boolean fromSrc) {
		this.fromSrc = fromSrc;
	}

	/*
	 * TODO to be uncommented when all classes changed public final void
	 * setMessage(String message) { throw new Error("Only to be called
	 * internally"); }
	 * 
	 * public final void setMessage(String message, Object... args) { throw new
	 * Error("Only to be called internally"); }
	 */

	protected final void setMsg(String message, Object... args) {
		super.setMessage(message, args);
	}

	@Override
	protected void computeBasedOnAST() {
	}

	public A getAST() {
		return ast;
	}

	public void setAST(A a) {
		if (checkASTs(a, ast)) {
			return;
		}
		ast = a;
		// computeBasedOnAST() called below
		setNode(a.getPromisedFor());
		
		final ISrcRef orig = super.getSrcRef();
		if (orig != null) {
			ISrcRef ref = orig.createSrcRef(ast.getOffset());
			if (ref == null) {
				ref = orig; 
			}
			f_lineNumber = ref.getLineNumber();
			f_hash       = ref.getHash();		
		} else {
			f_lineNumber = -1;
			f_hash       = -1L;
		}
	}

	public void clearAST() {
		ast = null;
		// FIX need to do anything else?
	}

	@Override
	protected void invalidate_internal() {
		/*
        if (isFromSrc()) {
			System.out.println("Invalidating "+getMessage());
		}
		*/
		super.invalidate_internal();
		
		if (getAST() != null) {
			getAST().clearPromisedFor();
		}
		clearAST();
		clearNode();		
	}

	/**
	 * @param ast
	 *            The current value
	 * @param a
	 *            The new value
	 * @return true if already set
	 */
	protected boolean checkASTs(A a, A ast) {
		if (a == null) {
			throw new NullPointerException("null ast");
		}
		if (a.equals(ast)) {
			return true; // already set
		}
		if (ast != null) {
			if (ast.getPromisedFor().identity() == IRNode.destroyedNode) {
				return false;
			}
			if (this instanceof ModelDrop) {
				return false;
			}
			LOG.warning("ast already set for " + this + " : " + getMessage());
		}
		return false;
	}

	public void addAST(A a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISrcRef getSrcRef() {
		final ISrcRef ref = super.getSrcRef();
		if (ref != null) {
			if (ast != null) {
				// System.out.println("Getting ref for "+this.getMessage());
				return this;
			} else {
				return ref;
			}
		}
		return null;
	}

	/**
	 * @return the PromiseDrop that this one was created from, or null if this
	 *         is the source
	 */
	public PromiseDrop<?> getSourceDrop() {
		return source;
	}

	public void setSourceDrop(PromiseDrop<?> drop) {
		if (drop == null) {
			throw new IllegalArgumentException("source drop is null");
		}
		if (source != null && source.isValid()) {
			if (source == drop) {
				LOG.warning("Re-setting source drop: "+drop);
				return;
			}
			throw new IllegalArgumentException(
					"already have a valid source drop");
		}
		source = drop;
		drop.addDependent(this);
	}

	/***************************************************************************
	 * Stuff to implement ISrcRef
	 **************************************************************************/

	public String getJavaId() {
		final IRNode decl = getNode();
		return decl == null ? null : JavaIdentifier.encodeDecl(decl);
	}
	
	public ISrcRef createSrcRef(int offset) {
		throw new UnsupportedOperationException();
	}
	
	public void clearJavadoc() {
		throw new UnsupportedOperationException();
	}

	public String getComment() {
		throw new UnsupportedOperationException();
	}

	public Object getEnclosingFile() {
		if (super.getSrcRef() == null) {
			return null;
		}
		return super.getSrcRef().getEnclosingFile();
	}
	
	public URI getEnclosingURI() {
		if (super.getSrcRef() == null) {
			return null;
		}
		return super.getSrcRef().getEnclosingURI();
	}
	
	public String getRelativePath() {
		if (super.getSrcRef() == null) {
			return null;
		}
		return super.getSrcRef().getRelativePath();
	}

	public IJavadocElement getJavadoc() {
		throw new UnsupportedOperationException();
	}

	public int getLength() {
		return 0;
	}

	public int getLineNumber() {		
		return f_lineNumber;
	}

	public int getOffset() {
		return ast.getOffset();
	}

	public Long getHash() {
		return f_hash;
	}

	public String getCUName() {
		ISrcRef ref = super.getSrcRef();
		return ref == null ? null : ref.getCUName();
	}

	public String getPackage() {
		ISrcRef ref = super.getSrcRef();
		return ref == null ? null : ref.getPackage();
	}

	public String getProject() {
		ISrcRef ref = super.getSrcRef();
		return ref == null ? null : ref.getProject();
	}
	
	/**
	 * Set of promise annotations composing this promise drop.
	 */
	private Set<PromiseAnnotation> promiseAnnotationSet = new HashSet<PromiseAnnotation>();

	/**
	 * Flags if this promise drop represents an assumed promise.
	 */
	private boolean assumed = false;

	/**
	 * Flags if this promise drop represents a virtual promise.
	 */
	
	private boolean virtual = false;

	/**
	 * Flags if this promise is from source code or from another location, such
	 * as XML.
	 */
	private boolean fromSrc = true;

	/**
	 * Annotation AST for this drop
	 */
	private A ast;
	private Long f_hash;
	private int f_lineNumber;
	private PromiseDrop<?> source;

	@Override
	public String getEntityName() {
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
	
	@Override
	protected JavaSourceReference createSourceRef() {	  
		IRNode n = getNode();
		if (n == null) {
			n = getAST().getPromisedFor();
		}
		return createSourceRef(n, getSrcRef()); 
	}
	
//	@Override
//	public final int hashCode() {
//		return getClass().hashCode() + getMessage().hashCode() + getNode().identity().hashCode();
//	}
//	
//	/**
//	 * Checking if the class and message match
//	 */
//	@Override
//	public final boolean equals(Object o) {
//		if (o instanceof PromiseDrop<?>) {
//			PromiseDrop<?> d = (PromiseDrop<?>) o;
//			return getNode().equals(d.getNode()) &&
//			       getClass().equals(d.getClass()) && getMessage().equals(d.getMessage());
//		}
//		return false;
//	}
}