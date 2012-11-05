package com.surelogic.analysis.concurrency.heldlocks;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.aast.promise.QualifiedLockNameNode;
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysis;
import com.surelogic.analysis.InstanceInitAction;
import com.surelogic.analysis.InstanceInitializationVisitor;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.heldlocks.LockUtils.HowToProcessLocks;
import com.surelogic.analysis.concurrency.heldlocks.MustHoldAnalysis.HeldLocks;
import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLock;
import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLockFactory;
import com.surelogic.analysis.concurrency.heldlocks.locks.NeededLock;
import com.surelogic.analysis.concurrency.heldlocks.locks.NeededLockFactory;
import com.surelogic.analysis.effects.ConflictChecker;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.targets.LocalTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.IDiffInfo;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.locks.LockModel;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.ReturnsLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.JavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.OpAssignExpression;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.PostDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PostIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PreDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PreIncrementExpression;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;

/**
 * LockAnalysis re-written as a visitor. This implements the lock analysis as a
 * top-down analysis, which more faithfully follows the type-rule presentation
 * of it. The previous analysis worked from the inside out, which meant you
 * could present it with a field-access and ask it if it was correct. This
 * feature was never used by our system though, and the inside out
 * implementation is overly verbose and hard to follow.
 * 
 * <p>
 * The initial method {@link #analyzeClass(IRNode, Drop)} takes a node whose
 * operator is a {@link edu.cmu.cs.fluid.java.operator.ClassBody}. Analysis
 * <em>does not</em> traverse inside of any nested type declarations.
 * 
 * <p>
 * <em>This class relies heavily on callbacks from the an 
 * {@link edu.cmu.cs.fluid.java.analysis.InstanceInitVisitor} instance,
 * and maintains a lot of state for communicating with itself through the
 * call backs, e.g., {@link #ctxtInsideConstructor}, 
 * {@link #ctxtOnBehalfOfConstructor}.  Therefore, this class must be considered
 * to be single-threaded.  That is only one thread should use any particular
 * instance of this class.</em>
 * 
 * <P>
 * TODO: Say more here!
 * 
 * <p>
 * TODO: Reorganize helper methods. Can some of them be eliminated?
 * 
 * @author aarong
 * 
 */
@SuppressWarnings("deprecation")
public final class LockVisitor extends VoidTreeWalkVisitor implements
		IBinderClient {
	/** Logger instance for debugging. */
	private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.lock"); //$NON-NLS-1$

	/**
	 * Name of the wait-queue lock defined for <code>java.lang.Object</code>
	 * used by the {@link java.lang.Object#wait()}method, etc.
	 */
	public static final String MUTEX_NAME = "MUTEX"; //$NON-NLS-1$

	// ////////////////////////////////////////////////////////////////////////////
	// D R O P - S E A L O C K R E S U L T C A T E G O R I E S

	private final class LVThisExpressionBinder extends
			AbstractThisExpressionBinder {
		public LVThisExpressionBinder(final IBinder b) {
			super(b);
		}

		@Override
		protected IRNode bindReceiver(final IRNode node) {
			if (ctxtInsideConstructor != null || ctxtInsideMethod != null) {
				return ctxtTheReceiverNode;
			} else {
				throw new UnsupportedOperationException(
						"Binding ThisExpression that is not inside a method or constructor");
			}
		}

		@Override
		protected IRNode bindQualifiedReceiver(final IRNode outerType,
				final IRNode node) {
			if (ctxtInsideConstructor != null) {
				return JavaPromise.getQualifiedReceiverNodeByName(
						ctxtInsideConstructor, outerType);
			} else if (ctxtInsideMethod != null) {
				return JavaPromise.getQualifiedReceiverNodeByName(
						ctxtInsideMethod, outerType);
			} else {
				throw new UnsupportedOperationException(
						"Binding ThisExpression that is not inside a method or constructor");
			}
		}
	}

	private final Effects effects;

	private final ThisExpressionBinder thisExprBinder;

	/**
	 * The binder to use.
	 */
	private final IBinder binder;

	private final BindingContextAnalysis bindingContextAnalysis;

	/**
	 * An indirect reference to the model of all the locks in the system. In the
	 * past this was a direct reference, but this proved hard to manage: the
	 * GlobalLockModel object changes with each run, and a new reference was
	 * provided to the LockVisitor using a setter method. But I routinely forgot
	 * to update the lock model references used by objects that LockVisitor
	 * delegates to, i.e., LockUtils and JUCLockUsagemanager. Now all these
	 * objects, plus the LockAssurance3 object, share a reference to a handle.
	 * The LockAssurance simply updates the handle and everyone is happy.
	 */
	private final AtomicReference<GlobalLockModel> sysLockModelHandle;

	// ---------------------------------
	// -- Context information
	// ---------------------------------

	/**
	 * The type declaration being analyzed. This node has an operator type that
	 * is a {@link TypeDeclInterface}. This field is set by
	 * {@link #analyzeClass(IRNode)}.
	 * 
	 * @see ctxtDeclaredType
	 */
	private IRNode ctxtTypeDecl;

	/**
	 * The type representation of the type declaration being analyzed. This
	 * field is set by {@link #analyzeClass(IRNode)}. This field is equivalent
	 * to {@code JavaTypeFactory.getMyThisType(ctxtTypeDecl)}.
	 * 
	 * @see #ctxtTypeDecl
	 */
	private IJavaDeclaredType ctxtJavaType;

	/**
	 * The named locks that are currently being held. Treated as a stack; locks
	 * are always added to the front of the list. A lock may be in the stack
	 * multiple times. This repetition isn't useful for the analysis, but it
	 * makes the implementation simpler because visitor methods can use
	 * block-structured push/pop operations to add and remove to the lock
	 * context with out having to worry about duplication. This stack only
	 * contains those locks held due to
	 * <ul>
	 * <li>A method being declared <code>synchronized</code>
	 * <li>A method being annotated with lock preconditions
	 * <li>A lock being acquired via a <code>synchronized</code> statement.
	 * </ul>
	 * 
	 * <p>
	 * Locks that we pretend are held because of a constructor being thread
	 * confined or because of the special semantics of class initialization are
	 * recorded in the fields {@link #ctxtThreadConfinedLocks} and
	 * {@link #ctxtClassInitializationLocks} respectively.
	 */
	private LockStack ctxtTheHeldLocks = null;

	/**
	 * The named locks that do not need to be explicitly acquired because the
	 * constructor being analyzed is thread-confined.
	 */
	private Set<HeldLock> ctxtThreadConfinedLocks = null;

	/**
	 * The named locks that do not need to be explicitly acquired because we are
	 * analyzing a static initializer.
	 */
	private Set<HeldLock> ctxtClassInitializationLocks = null;

	/**
	 * If we are analyzing the inside of a method declaration then this refers
	 * to the lock (if any) that the method is supposed to return. If we are not
	 * inside of a method or the method is not supposed to return any particular
	 * lock then this is <code>null</code>.
	 * 
	 * @see #ctxtReturnsLockDrop
	 * @see #ctxtInsideMethod
	 */
	private HeldLock ctxtReturnedLock = null;

	/**
	 * If we are analyzing the inside of a method declaration then this refers
	 * to the drop associated with the "returnsLock" annotation (if any) of that
	 * the method. If we are not inside of a method or the method does not have
	 * a "returnsLock" annotation then this is <code>null</code>.
	 * 
	 * @see #ctxtReturnedLock
	 * @see #ctxtInsideMethod
	 */
	private ReturnsLockPromiseDrop ctxtReturnsLockDrop = null;

	/**
	 * If we are analyzing the inside of a method declaration then this points
	 * to the method we are inside of. If we are not inside of a method then
	 * this is <code>null</code>. We use this when
	 * {@link #visitReturnStatement(IRNode) analyzing return statements} to know
	 * when method the return statement belongs to.
	 * 
	 * <p>
	 * This can refer to ClassInitDeclaration or, in the case of an anonymous
	 * class expression, and InitDeclaration. It will never be a
	 * ConstructorDeclaration; see {@link #ctxtInsideConstructor}.
	 * 
	 * @see #ctxtReturnedLock
	 * @see #ctxtReturnsLockDrop
	 */
	private IRNode ctxtInsideMethod = null;

	/**
	 * The binding context analysis query focused to the current flow unit being
	 * analyzed. This needs to be updated whenever {@link #ctxtInsideMethod} or
	 * {@link #ctxtInsideConstructor} is updated.
	 */
	private BindingContextAnalysis.Query ctxtBcaQuery = null;

	/**
	 * The must hold analysis query focused to the current flow unit being
	 * analyzed. This needs to be updated whenever {@link #ctxtInsideMethod} or
	 * {@link #ctxtInsideConstructor} is updated.
	 */
	private JavaFlowAnalysisQuery<HeldLocks> ctxtHeldLocksQuery = null;
	private MustHoldAnalysis.LocksForQuery ctxtLocksForQuery = null;

	/**
	 * The must release analysis query focused to the current flow unit being
	 * analyzed. This needs to be updated whenever {@link #ctxtInsideMethod} or
	 * {@link #ctxtInsideConstructor} is updated.
	 */
	private MustReleaseAnalysis.Query ctxtMustReleaseQuery = null;

	/**
	 * The conflict checker to use, focused on the current flow analysis being
	 * analyzed. This needs to be updated whenever {@link #ctxtInsideMethod} or
	 * {@link #ctxtInsideConstructor} is updated.
	 */
	private ConflictChecker ctxtConflicter = null;

	/**
	 * This field is checked on entry to an expression to determine if the
	 * effect should be a write effect. It is always immediately restored to
	 * <code>false</code> after being checked. This isn't the best way of doing
	 * things, but it works. The same thing is done in {@link EffectsVisitor}.
	 * 
	 * <p>
	 * This field is set when visiting the parent of the lhs node. It is thus
	 * important that the parent node set this flag immediately before visiting
	 * the node that represents the left-hand side of the assignment expression.
	 */
	private boolean ctxtIsLHS = false;

	/**
	 * This is <code>true</code> if visiting on behalf of a constructor, but not
	 * inside the constructor body itself. That is, we are inside a field
	 * declaration or instance initializer block and have gotten there via the
	 * initialization helper.
	 */
	private boolean ctxtOnBehalfOfConstructor = false;

	/**
	 * If non-<code>null</code> this refers to the constructor that is currently
	 * being analyzed. This is needed so that analysis of field declarations and
	 * instance initializers can properly report back on whose behalf they are
	 * being analyzed.
	 * 
	 * <p>
	 * It is intended that this field is testing against to determine if
	 * analysis is proceeding on behalf of a constructor.
	 */
	private IRNode ctxtInsideConstructor = null;

  /**
   * If {@link #ctxtInsideConstructor} is non-<code>null</code> then this refers
   * to the name of the constructor. This is used as a parameter to
   * {@link MessageFormat#format(java.lang.String, java.lang.Object[])}. We
   * create it once because it is silly to repeatedly recompute it for each
   * assurance result.
   * 
   * @see #ctxtInsideConstructor
   */
  private String ctxtConstructorName = null;

	/**
	 * When analyzing the body of a constructor (including any initialization
	 * blocks or field initializers), this value is non- null} and contains
	 * information about whether the constructor is single-thread
	 * (thread-confined) or not.
	 */
	private LockExpressions.SingleThreadedData ctxtSingleThreadedData = null;

	/**
	 * The receiver declaration node of the constructor/method/field
	 * initializer/class initializer currently being analyzed. Every expression
	 * we want to analyze should be inside one of these things. We need to keep
	 * track of this because the {@link #initHelper instance initialization
	 * helper} re-enters this analysis on behalf of constructor declarations,
	 * and we want any field declarations and instance initializers to report
	 * their receivers in terms of the current constructor; this makes life
	 * easier for consumers of the effect results.
	 * 
	 * <p>
	 * This field is updated whenever analysis enters a method declaration,
	 * constructor declaration, or <em>static</em> initializer. It is
	 * <em>not</em> updated when analysis enters a field declaration or instance
	 * initializer, because in those cases analysis is proceeding on behalf of a
	 * particular constructor, and we want to use the receiver node from that
	 * constructor. (This works because analysis <em>does not</em> proceed
	 * inside of nested classes.)
	 */
	private IRNode ctxtTheReceiverNode = null;

	/**
	 * When the initializers of an anonymous class expression are being
	 * analyzed, this reference is non-<code>null</code> and points to the
	 * object that manages the relevant immediately enclosing instance
	 * references to be used when back-mapping the lock references from the body
	 * of the anonymous class expression to the context in which the anonymous
	 * class expression appears.
	 */
	private MethodCallUtils.EnclosingRefs ctxtEnclosingRefs = null;

	/**
	 * True if the body of an anonymous class expression is being recursively
	 * visited.
	 */
	private boolean ctxtInsideAnonClassExpr = false;

	/**
	 * Cache used by {@link #isSafeType} to store the result. Avoids having to
	 * repeatedly climb the type hierarchy.
	 */
	private final Map<IJavaType, Boolean> isSafeTypeCache;

	/**
	 * The non null analysis
	 */
	private final SimpleNonnullAnalysis nonNullAnalylsis;

	/**
	 * The alias analysis to use.
	 */
	private final IMayAlias mayAlias;

	// /**
	// * The intrinsic lock flow analysis.
	// */
	// private final IntrinsicLockAnalysis intrinsicLock;

	/**
	 * The must-release analysis.
	 */
	private final MustReleaseAnalysis mustRelease;

	/**
	 * The must-release analysis.
	 */
	private final MustHoldAnalysis mustHold;

	/**
	 * LockUtils instance. This is shared with the mustHold and mustRelease
	 * analyses.
	 */
	private final LockUtils lockUtils;

	/**
	 * Cache of whether/how methods/constructors use JUC Locks.
	 */
	private final JUCLockUsageManager jucLockUsageManager;

	/**
	 * Factory for created held locks.
	 */
	private final HeldLockFactory heldLockFactory;

	/**
	 * Factory for creating needed locks.
	 */
	private final NeededLockFactory neededLockFactory;

	// ----------------------------------------------------------------------
	// -- Helpers for dealing with field initialization and instance
	// initializers
	// ----------------------------------------------------------------------

	/**
	 * Record that is pushed onto the {@link LockVisitor#ctxtTheHeldLocks lock
	 * context} that contains the {@link #lock held lock}.
	 */
	private static final class StackLock {
		public final String key;
		public final HeldLock lock;
		public final StackLock next;

		public StackLock(final HeldLock l, final StackLock nxt) {
			lock = l;
			key = CommonStrings.intern(l.getName());
			next = nxt;
		}

		@Override
		public String toString() {
			return lock.toString();
		}

		public boolean mustAlias(final HeldLock t,
				final ThisExpressionBinder teb, final IBinder b) {
			return lock.mustAlias(t, teb, b);
		}

		public boolean mustSatisfy(final NeededLock t,
				final ThisExpressionBinder teb, final IBinder b) {
			return lock.mustSatisfy(t, teb, b);
		}
	}

	private static final class LockStackFrame implements Iterable<StackLock> {
		public static final LockStackFrame EMPTY = new LockStackFrame();

		private boolean isNeeded;

		private StackLock locks;

		public final LockStackFrame nextFrame;

		public final StackLock lastLock;

		/**
		 * Create the initial stack frame that represents an empty stack.
		 */
		private LockStackFrame() {
			isNeeded = false;
			locks = null;
			nextFrame = null;
			lastLock = null;
		}

		public LockStackFrame(final LockStackFrame next) {
			isNeeded = false;
			nextFrame = next;
			locks = next.locks;
			lastLock = next.locks;
		}

		public void push(final HeldLock lock) {
			locks = new StackLock(lock, locks);
		}

		public void push(final Set<HeldLock> locks) {
			for (final HeldLock lock : locks) {
				push(lock);
			}
		}

		public boolean isNeeded() {
			return isNeeded;
		}

		public boolean containsLock(final HeldLock lock,
				final ThisExpressionBinder teb, final IBinder b,
				final boolean setNeeded) {
			StackLock current = locks;
			while (current != lastLock) {
				if (current.mustAlias(lock, teb, b)) {
					if (setNeeded) {
						isNeeded = true;
					}
					return true;
				}
				current = current.next;
			}
			return false;
		}

		public boolean satisfiesLock(final NeededLock lock,
				final ThisExpressionBinder teb, final IBinder b,
				final boolean setNeeded) {
			StackLock current = locks;
			while (current != lastLock) {
				if (current.mustSatisfy(lock, teb, b)) {
					if (setNeeded) {
						isNeeded = true;
					}
					return true;
				}
				current = current.next;
			}
			return false;
		}

		@Override
		public String toString() {
			final SortedSet<String> names = new TreeSet<String>();
			StackLock current = locks;
			while (current != lastLock) {
				names.add(current.toString());
				current = current.next;
			}

			final StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (final Iterator<String> namesIter = names.iterator(); namesIter
					.hasNext();) {
				final String name = namesIter.next();
				sb.append(name);
				if (namesIter.hasNext()) {
					sb.append(", ");
				}
			}
			sb.append("]");
			return sb.toString();
		}

		public Iterator<StackLock> iterator() {
			return new Iterator<StackLock>() {
				private StackLock current = locks;

				public boolean hasNext() {
					return current != lastLock;
				}

				public StackLock next() {
					if (current != lastLock) {
						final StackLock item = current;
						current = current.next;
						return item;
					} else {
						throw new NoSuchElementException();
					}
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	private static final class LockStack implements Iterable<StackLock> {
		private LockStackFrame head;

		public LockStack() {
			head = LockStackFrame.EMPTY;
		}

		public LockStackFrame pushNewFrame() {
			head = new LockStackFrame(head);
			return head;
		}

		public void popFrame() {
			head = head.nextFrame;
		}

		public boolean satisfiesLock(final NeededLock lock,
				final ThisExpressionBinder teb, final IBinder b,
				final boolean setNeeded) {
			LockStackFrame frame = head;
			while (frame != null) {
				if (frame.satisfiesLock(lock, teb, b, setNeeded)) {
					return true;
				}
				frame = frame.nextFrame;
			}
			return false;
		}

		public boolean oldFramesContainLock(final HeldLock lock,
				final ThisExpressionBinder teb, final IBinder b) {
			if (head == null) {
				return false;
			} else {
				LockStackFrame frame = head.nextFrame;
				while (frame != null) {
					if (frame.containsLock(lock, teb, b, false)) {
						return true;
					}
					frame = frame.nextFrame;
				}
				return false;
			}
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("[");
			StackLock current = head.locks;
			while (current != null) {
				sb.append(current.toString());
				current = current.next;
				if (current != null) {
					sb.append(", ");
				}
			}
			sb.append("]");
			return sb.toString();
		}

		public Iterator<StackLock> iterator() {
			return new Iterator<StackLock>() {
				private StackLock current = head.locks;

				public boolean hasNext() {
					return current != null;
				}

				public StackLock next() {
					if (current != null) {
						final StackLock item = current;
						current = current.next;
						return item;
					} else {
						throw new NoSuchElementException();
					}
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	// ----------------------------------------------------------------------

	/**
	 * Construct a new Lock Visitor.
	 * 
	 * @param b
	 *            The Binder to use to look up names.
	 * @throws SlotAlreadyRegisteredException
	 */
	public LockVisitor(final IIRAnalysis a, final IBinder b, final Effects e,
			final IMayAlias ma, final BindingContextAnalysis bca,
			final AtomicReference<GlobalLockModel> glmRef) {
		binder = b;
		effects = new Effects(b);
		bindingContextAnalysis = bca;
		mayAlias = ma;
		sysLockModelHandle = glmRef;
		ctxtTypeDecl = null; // this will be set by analyzeClass()
		ctxtJavaType = null; // this will be set by analyzeClass()
		isSafeTypeCache = new HashMap<IJavaType, Boolean>();

		thisExprBinder = new LVThisExpressionBinder(b);
		heldLockFactory = new HeldLockFactory(thisExprBinder);
		neededLockFactory = new NeededLockFactory(thisExprBinder);

		lockUtils = new LockUtils(glmRef, b, e, mayAlias, neededLockFactory,
				thisExprBinder);
		jucLockUsageManager = new JUCLockUsageManager(lockUtils, binder, bca);

		// Create the subsidiary flow analyses
		nonNullAnalylsis = new SimpleNonnullAnalysis(binder);
		// intrinsicLock = new IntrinsicLockAnalysis(b, lockUtils,
		// jucLockUsageManager, nonNullAnalylsis);
		mustRelease = new MustReleaseAnalysis(thisExprBinder, b, lockUtils,
				jucLockUsageManager, nonNullAnalylsis);
		mustHold = new MustHoldAnalysis(thisExprBinder, b, lockUtils,
				jucLockUsageManager, nonNullAnalylsis);
	}

	private void updateJUCAnalysisQueries(final IRNode flowUnit) {
		final LockExpressions lockExprs = jucLockUsageManager
				.getLockExpressionsFor(flowUnit);
		if (lockExprs.usesJUCLocks()) {
			ctxtHeldLocksQuery = mustHold.getHeldLocksQuery(flowUnit);
		} else {
			ctxtHeldLocksQuery = MustHoldAnalysis.EMPTY_HELD_LOCKS_QUERY;
		}
		ctxtLocksForQuery = mustHold.getLocksForQuery(flowUnit);
		ctxtMustReleaseQuery = mustRelease.getUnlocksForQuery(flowUnit);
	}

	public IBinder getBinder() {
		return this.binder;
	}

	public void clearCaches() {
		isSafeTypeCache.clear();
		jucLockUsageManager.clear();
		lockUtils.clear();
		bindingContextAnalysis.clear();
		mustRelease.clear();
		mustHold.clear();
		nonNullAnalylsis.clear();
		clear();
	}

	private void clear() {
		ctxtTypeDecl = null;
		ctxtJavaType = null;
		ctxtTheHeldLocks = null;
		ctxtClassInitializationLocks = null;
		ctxtThreadConfinedLocks = null;
		ctxtReturnedLock = null;
		ctxtReturnsLockDrop = null;
		ctxtIsLHS = false;
		ctxtInsideMethod = null;
		ctxtOnBehalfOfConstructor = false;
		ctxtInsideConstructor = null;
		ctxtSingleThreadedData = null;
		ctxtConstructorName = null;
		ctxtTheReceiverNode = null;
		ctxtEnclosingRefs = null;
		ctxtInsideAnonClassExpr = false;
	}

	/**
	 * THe main method
	 * 
	 * @param node
	 *            A ClassBody node
	 */
	public synchronized void analyzeClass(final IRNode node) {
		final Operator op = JJNode.tree.getOperator(node);
		if (ClassBody.prototype.includes(op)) {
			ctxtTypeDecl = JJNode.tree.getParentOrNull(node);
			ctxtJavaType = (IJavaDeclaredType) binder.getTypeEnvironment().getMyThisType(
					ctxtTypeDecl);
			ctxtTheHeldLocks = new LockStack();
			this.doAccept(node);
		} else {
			throw new IllegalArgumentException(
					"Node must have an operator of type "
							+ ClassBody.class.getName()
							+ "; given node has operator of type "
							+ op.getClass().getName());
		}
	}

	// ----------------------------------------------------------------------
	// Drop management methods
	// ----------------------------------------------------------------------

	private HintDrop makeInfoDrop(final int category,
			final IRNode context, final int msgTemplate,
			final Object... msgArgs) {
		final HintDrop info = HintDrop.newInformation(context);
		info.setMessage(msgTemplate, msgArgs);
		info.setCategorizingMessage(category);
		return info;
	}

	private HintDrop makeWarningDrop(final int category,
			final IRNode context, final int msgTemplate,
			final Object... msgArgs) {
		final HintDrop info = HintDrop.newWarning(context);
		info.setMessage(msgTemplate, msgArgs);
		info.setCategorizingMessage(category);
		return info;
	}

	private ResultDrop makeResultDrop(final IRNode context,
			final PromiseDrop<? extends IAASTRootNode> p,
			final boolean isConsistent, final int msgTemplate,
			final Object... msgArgs) {
		final ResultDrop result = new ResultDrop(context);
		result.setMessage(msgTemplate, msgArgs);
		result.addChecked(p);
		if (isConsistent) {
			result.setConsistent();
		} else {
			result.setInconsistent();
		}
		return result;
	}

	private void addSupportingInformation(final Drop drop,
			final IRNode link, final int msgTemplate, final Object... msgArgs) {
		drop.addInformationHint(link, msgTemplate, msgArgs);
	}

	/**
	 * Add links to the lock precondition promises that are used by the proof
	 * for the given result drop.
	 * 
	 * @param lockToDrop
	 *            A mapping from lock names to PromiseDrops
	 * @param needed
	 *            The lock to look up
	 * @param result
	 *            The drop to add a trusted promise to
	 */
	private void addTrustedLockDrop(final LockStack intrinsicLocks,
			final Set<HeldLock> jucLocks, final NeededLock needed,
			final ResultDrop result) {
		final String lockKey = CommonStrings.intern(needed.getName());
		for (final StackLock lock : intrinsicLocks) {
			if (lock.key == lockKey) {
				final PromiseDrop<?> supportingDrop = lock.lock
						.getSupportingDrop();
				if (supportingDrop != null) {
					result.addTrusted(supportingDrop);
				}
				/*
				 * Only one annotation can support the holding of any given
				 * lock, so we quit looking once we have found it.
				 */
				return;
			}
		}

		for (final HeldLock lock : jucLocks) {
			if (lock.getName().equals(lockKey)) {
				final PromiseDrop<?> supportingDrop = lock.getSupportingDrop();
				if (supportingDrop != null) {
					result.addTrusted(supportingDrop);
				}
				/*
				 * Only one annotation can support the holding of any given
				 * lock, so we quit looking once we have found it.
				 */
				return;
			}
		}
	}

	private void addLockAcquisitionInformation(final Drop drop,
			final LockStack intrinsicLocks, final Set<HeldLock> jucLocks) {
		for (final StackLock has : intrinsicLocks) {
			addSupportingInformation(drop, has.lock.getSource(),
					has.lock.isAssumed() ? Messages.LockAnalysis_ds_AssumedHeld
							: Messages.LockAnalysis_ds_HeldLock, has.lock);
		}
		for (final HeldLock lock : jucLocks) {
			addSupportingInformation(drop, lock.getSource(),
					lock.isAssumed() ? Messages.LockAnalysis_ds_AssumedHeld
							: Messages.LockAnalysis_ds_HeldJUCLock, lock);
		}
		// for (final HeldLock lock : il) {
		// addSupportingInformation(drop, lock.getSource(),
		// (lock.isAssumed() ? DS_ASSUMED_HELD_MSG : DS_LOCK_HELD_MSG), lock);
		//
		// }
	}

	// ----------------------------------------------------------------------
	// Annotation getter methods
	// ----------------------------------------------------------------------

	/**
	 * Query if an expression is known to represent a policy lock. Returns true
	 * if the expression is final and can be matched to a policy lock
	 * declaration.
	 * 
	 * @param lockExpr
	 *            The expression to interpret as a lock. Must be a final
	 *            expression.
	 */
	private boolean isPolicyLockExpr(final IRNode lockExpr) {
		final Operator op = JJNode.tree.getOperator(lockExpr);
		final GlobalLockModel sysLockModel = sysLockModelHandle.get();
		/*
		 * First see if the expression itself results an object that uses itself
		 * as a lock. ThisExpressions and VariableUseExpressions are trivially
		 * handled here. We do not do this for method calls because the returned
		 * object does not yet have a fixed name.
		 */
		// get the locks for the class of the expression's value
		final IJavaType lockExprType = binder.getJavaType(lockExpr);
		final Set<PolicyLockRecord> plocksForExprType;
		if (lockExprType instanceof IJavaDeclaredType) {
			plocksForExprType = sysLockModel.getPolicyLocksForLockImpl(
					lockExprType, GlobalLockModel.THIS);
		} else {
			plocksForExprType = Collections.<PolicyLockRecord> emptySet();
		}

		// is the receiver used as a lock for that type?
		if (!plocksForExprType.isEmpty()) {
			return true;
		} else {
			/*
			 * Now see if the expression is a FieldRef or ClassExpression (which
			 * for our purposes is a special kind of FieldRef). If so, see if
			 * the field is distinguished as a lock.
			 */
			if (ClassExpression.prototype.includes(op)) { // lockExpr ==
															// 'e.class'
				final IRNode cdecl = this.binder.getBinding(lockExpr); // get
																		// the
																		// class
				if (!sysLockModel.getPolicyLocksForLockImpl(
						JavaTypeFactory.getMyThisType(cdecl), cdecl).isEmpty()) {
					return true;
				}
			} else if (FieldRef.prototype.includes(op)) { // lockExpr == 'e.f'
				final IRNode obj = FieldRef.getObject(lockExpr);
				if (!sysLockModel.getPolicyLocksForLockImpl(
						binder.getJavaType(obj),
						this.binder.getBinding(lockExpr)).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Test if the synchronization of a method corresponds to a policy lock.
	 */
	private boolean isPolicyLockMethod(final IRNode mdecl) {
		if (TypeUtil.isStatic(mdecl)) {
			// Complain if .class is not declared to be a policy lock.
			return !sysLockModelHandle.get()
					.getPolicyLocksForLockImpl(ctxtJavaType, ctxtTypeDecl)
					.isEmpty();
		} else {
			return !sysLockModelHandle
					.get()
					.getPolicyLocksForLockImpl(ctxtJavaType,
							GlobalLockModel.THIS).isEmpty();
		}
	}

	private Set<HeldLock> convertLockExpr(final IRNode lockExpr,
			final BindingContextAnalysis.Query bcaQuery,
			final IRNode enclosingDecl, final IRNode src) {
		if (lockUtils.getFinalExpressionChecker(bcaQuery, enclosingDecl,
		    SomeFunctionDeclaration.getBody(enclosingDecl)).isFinal(lockExpr)) {
			final Set<HeldLock> result = new HashSet<HeldLock>();
			lockUtils.convertIntrinsicLockExpr(lockExpr, heldLockFactory,
					enclosingDecl, src, result);
			return result;
		}
		return Collections.emptySet();
	}

	/**
	 * Get the locks held by a static initializer block. These are all the
	 * static <em>state</em> locks for the class of the instance being created.
	 * 
	 * @param initBlock
	 *            The declaration node for a initializer block.
	 * @param clazz
	 *            The type representation of the containing class
	 */
	private Set<HeldLock> convertStaticInitializerBlock(final IRNode initBlock,
			final IJavaDeclaredType clazz) {
		final Set<HeldLock> assumedLocks = new HashSet<HeldLock>();
		lockUtils.getClassInitLocks(HowToProcessLocks.INTRINSIC, initBlock,
				heldLockFactory, clazz, assumedLocks);
		return assumedLocks;
	}

	/**
	 * Get the locks held by a "singleThreaded" constructor. These are all the
	 * non-static <em>state</em> locks for the class of the instance being
	 * created, except for the special MUTEX lock.
	 * 
	 * @param conDecl
	 *            The declaration node for a constructor known to have a
	 *            <code>singleThreaded</code> annotation. This method assumes it
	 *            is being called from a context in which the body of conDecl is
	 *            being analyzed, and thus the field
	 *            {@link #ctxtTheReceiverNode} refers to the canonical receiver
	 *            for this constructor.
	 * @param classDecl
	 *            The declaration node for the containing class
	 */
	private Set<HeldLock> convertSingleThreadedConstructor(
			final IRNode conDecl, final IJavaDeclaredType clazz) {
		final Set<HeldLock> assumedLocks = new HashSet<HeldLock>();
		lockUtils.getSingleThreadedLocks(HowToProcessLocks.INTRINSIC, conDecl,
				heldLockFactory, clazz, ctxtTheReceiverNode, assumedLocks);
		return assumedLocks;
	}

	/**
	 * Process the lock preconditions of a method/constructor declaration.
	 * 
	 * @param decl
	 *            A MethodDeclaration or a ConstructorDeclaration node. This
	 *            method assumes it is being called from a context in which the
	 *            body of decl is being analyzed, and thus the field
	 *            {@link #ctxtTheReceiverNode} refers to the canonical receiver
	 *            for this method/constructor.
	 * @param lockStack
	 *            A linked list of locks that that is modified as a result of
	 *            this method. The locks corresponding to required locks are
	 *            added to the front of the list.
	 */
	private void processLockPreconditions(
	    final IRNode decl, final LockStackFrame stackFrame,
			Set<LockSpecificationNode> locksOnParameters) {
		final Set<HeldLock> preconditions = new HashSet<HeldLock>();
		LockUtils.getLockPreconditions(HowToProcessLocks.INTRINSIC, decl,
				heldLockFactory, ctxtTheReceiverNode, preconditions, locksOnParameters);
		stackFrame.push(preconditions);
	}
	
	private void checkMutabilityOfFormalParameters(
	    final IRNode decl, final Set<LockSpecificationNode> locksOnParameters,
	    final Set<LockSpecificationNode> returnsLockOnParameters) {
		/* Check for requires lock preconditions of the form "p:Lock".  Need to
		 * check if 'p' is modified in the body of the method.  We used to use the
		 * scrubber to check that 'p' is final.  Now we check it here.  If 'p' 
		 * is final, continue without saying anything.  If 'p' is not final, we
		 * check to see if 'p' is written too in the method.  If so, this is an 
		 * error.  If not, we proceed, but warn that it would be better to 
		 * declare 'p' as final.
		 */
		if (!(locksOnParameters.isEmpty() && returnsLockOnParameters.isEmpty())) {
		  final Set<Effect> fx =
		      effects.getEffectsQuery(decl, ctxtBcaQuery).getResultFor(
		          SomeFunctionDeclaration.getBody(decl));
		  checkHelper(fx, decl, "precondition", LockRules.getRequiresLock(decl), locksOnParameters);
		  checkHelper(fx, decl, "postcondition", LockRules.getReturnsLock(JavaPromise.getReturnNodeOrNull(decl)), returnsLockOnParameters);
		}
	}
	
	private void checkHelper(final Set<Effect> fx, final IRNode decl,
	    final String label, final PromiseDrop<? extends IAASTRootNode> drop,
  final Set<LockSpecificationNode> locksOnParameters) {
	  for (final LockSpecificationNode lockSpec : locksOnParameters) {
      final ExpressionNode base = ((QualifiedLockNameNode) lockSpec.getLock()).getBase();
      final IRNode pDecl = ((VariableUseExpressionNode) base).resolveBinding().getNode();
      if (!TypeUtil.isFinal(pDecl)) {
        boolean bad = false;
        for (final Effect e : fx) {
          if (e.isWrite()) {
            final Target t = e.getTarget();
            if (t instanceof LocalTarget) {
              if (((LocalTarget) t).getVarDecl().equals(pDecl)) {
                bad = true;
              }
            }
          }
        }
        if (bad) {
          makeResultDrop(pDecl, drop, false,
              Messages.FORMAL_PARAMETER_WRITTEN_TO,
              ParameterDeclaration.getId(pDecl),
              label,
              lockSpec.unparse(false));
        } else {
          makeWarningDrop(Messages.DSC_FINAL_FIELDS, pDecl,
              Messages.SHOULD_BE_FINAL,
              ParameterDeclaration.getId(pDecl),
              label,
              lockSpec.unparse(false));
          // warning: should make parameter final
        }
      }
	  }
	}

	/**
	 * @param regionAccess
	 *            A FieldRef, ArrayRefExpression, or VariableDeclarator
	 * @param neededLocks
	 *            The locks needed by <code>ref</code>
	 */
	private void assureRegionRef(final IRNode regionAccess,
			final Set<NeededLock> neededLocks) {
		// Get the JUC locks that are held at the point of the region access
		final MustHoldAnalysis.HeldLocks heldJUCLocks = getHeldJUCLocks(regionAccess);
		// final Set<HeldLock> il = getHeldIntrinsicLocks(regionAccess);

		final LockChecker regionRefChecker = new LockChecker(regionAccess) {
			@Override
			protected LockModel getPromiseDrop(final NeededLock neededLock) {
				return neededLock.getLockPromise();
			}

			@Override
			protected void addAdditionalEvidence(
					final ResultDrop resultDrop) {
				// No additional evidence to add
			}
		};
		regionRefChecker
				.assureNeededLocks(
						neededLocks,
						heldJUCLocks,
						Messages.DSC_FIELD_ACCESS_ASSURED,
						Messages.DSC_FIELD_ACCESS_NOT_ASSURED,
						Messages.LockAnalysis_ds_FieldAccessAssured,
						Messages.LockAnalysis_ds_FieldAccessAssuredAlternative,
						Messages.LockAnalysis_ds_FieldAccessOkayClassInit,
						Messages.LockAnalysis_ds_FieldAccessOkayClassInitAlternative,
						Messages.LockAnalysis_ds_FieldAccessOkayThreadConfined,
						Messages.LockAnalysis_ds_FieldAccessOkayThreadConfinedAlternative,
						Messages.LockAnalysis_ds_FieldAccessNotAssured,
						Messages.LockAnalysis_ds_FieldAccessNotResolvable);
	}

	/**
	 * Report if a field reference is potentially misleading. Here we try to
	 * identify places where aggregation might be needed. Our primary concern is
	 * expressions of the form e.f1.f2, where f1 is protected by a lock. We want
	 * to complain that f2 is *not* protected by that lock. Don't complain if f1
	 * is unique and f2 is mapped into a protected region. Don't complain if f2
	 * is final? Right now want to avoid complaining about unprotected fields in
	 * general because it interferes with the incremental nature of annotations.
	 * 
	 * <P>
	 * NB. ArrayRef expressions e[...] are just special kinds of field access.
	 * 
	 * @param fieldRef
	 *            A FieldRef expression or ArrayRefExpression
	 */
	private void dereferencesSafeObject(final IRNode fieldRef) {
		/* fieldRef == e.f or e[...] */
		/*
		 * Only interested if there is a nested FieldRef Expression. TODO:
		 * (Ought to worry about referencing fields out of array elements too,
		 * but not now...)
		 */
		final boolean isArrayRef = ArrayRefExpression.prototype
				.includes(JJNode.tree.getOperator(fieldRef));
		final IRNode objExpr = isArrayRef ? ArrayRefExpression
				.getArray(fieldRef) : FieldRef.getObject(fieldRef);
		final Operator op2 = JJNode.tree.getOperator(objExpr);

		/* We only care if fieldRef is e'.f'.f or e'.f'[...] */
		if (FieldRef.prototype.includes(op2)) {
			/*
			 * Things are only interesting if the outer region f is not
			 * protected. So we don't proceed if f' is unique (and thus f is
			 * aggregated into the state of the referring object), f is
			 * protected by a lock or if f is volatile or final. Array reference
			 * is not protected.
			 */
			final boolean unprotected = !UniquenessUtils
					.isUnique(this.binder.getBinding(objExpr))
					&& (isArrayRef || !isFinalOrVolatile(fieldRef)
							&& lockUtils.getLockForFieldRef(fieldRef) == null);
			if (unprotected) {
				/*
				 * Now check if f' is in a protected region. There are three
				 * cases: (1) f' is a final or volatile field in a class that
				 * contains lock declarations. (2) f' is a field in a region
				 * associated with a lock. (3) Otherwise, we assume f' is not
				 * meant to be accessed concurrently, so we don't have to issue
				 * a warning.
				 * 
				 * In the first case we report the warning under EACH lock that
				 * is declared in the class. In the second case we report the
				 * warning under the lock that protects f'.
				 */
				if (isFinalOrVolatile(objExpr)) {
					// Field is final or volatile, see if the class contains
					// locks
					if (mayBeAccessedByManyThreads(objExpr)) {
						/*
						 * For each lock declared in the class of e'.f', attach
						 * a warning that it is not protecting the field f.
						 * 
						 * Propose that the field be aggregated into those
						 * regions. Really this needs to be an OR. The end user
						 * should only be allowed to choose one of these.
						 */
						final HintDrop info = makeWarningDrop(
								Messages.DSC_AGGREGATION_NEEDED, fieldRef,
								Messages.LockAnalysis_ds_AggregationNeeded,
								DebugUnparser.toString(fieldRef));
						// Propose the unique annotation
						final IRNode fieldDecl = binder.getBinding(objExpr);
						final IJavaType rcvrType = binder.getJavaType(FieldRef
								.getObject(objExpr));
						final Set<AbstractLockRecord> records = sysLockModelHandle
								.get().getRegionAndPolicyLocksInClass(rcvrType);
						for (final AbstractLockRecord lockRecord : records) {
							if (!lockRecord.lockDecl.equals(lockUtils
									.getMutex())) {
								lockRecord.lockDecl.addDependent(info);

								if (lockRecord instanceof RegionLockRecord) {
									// Propose the aggregate annotation
									final String simpleRegionName = ((RegionLockRecord) lockRecord).region.getName();
									if ("Instance".equals(simpleRegionName)) {
										info.addProposal(new ProposedPromiseDrop(
												"Unique", null, fieldDecl,
												fieldRef, Origin.MODEL));
									} else {
										info.addProposal(new ProposedPromiseDrop(
												"UniqueInRegion",
												simpleRegionName, fieldDecl,
												fieldRef, Origin.MODEL));
									}
								}
							}
						}
					}
				} else {
					// Field is non-final, non-volatile
					final RegionLockRecord innerLock = lockUtils
							.getLockForFieldRef(objExpr);
					if (innerLock != null) {
						// Field is non-final, non-volatile, and is associated
						// with a lock

						/*
						 * For the lock required for e'.f', attach a warning
						 * that it is not protecting the field f.
						 */
						final HintDrop info = makeWarningDrop(
								Messages.DSC_AGGREGATION_NEEDED, fieldRef,
								Messages.LockAnalysis_ds_AggregationNeeded,
								DebugUnparser.toString(fieldRef));
						innerLock.lockDecl.addDependent(info);

						/*
						 * Propose that the field be @Unique and aggregated.
						 */
						final IRNode fieldDecl = binder.getBinding(objExpr);
						final String simpleRegionName = innerLock.region.getName();
						if ("Instance".equals(simpleRegionName)) {
							info.addProposal(new ProposedPromiseDrop(
									"Unique", null, fieldDecl, fieldRef,
									Origin.MODEL));
						} else {
							info.addProposal(new ProposedPromiseDrop(
									"UniqueInRegion", simpleRegionName,
									fieldDecl, fieldRef, Origin.MODEL));
						}
					}
				}
			}
		}
	}

	/**
	 * Determine whether a class can be considered to protect itself. Returns
	 * true} if one of the following is true:
	 * <ul>
	 * <li>The class, or one of its ancestors, is annotated with
	 * <code>@ThreadSafe</code>
	 * <li>The class, or one of its ancestors, is annotated with
	 * <code>@Immutable</code>
	 * <li>The class, or one of its ancestors, declares at least one region or
	 * policy lock</code>
	 * </ul>
	 */
	private boolean isSafeType(final IJavaType type) {
		final Boolean isSafeCached = isSafeTypeCache.get(type);
		if (isSafeCached != null) {
			return isSafeCached;
		} else {
			boolean isSafe = false;
			if (type instanceof IJavaSourceRefType) {
				final IJavaSourceRefType srcRefType = (IJavaSourceRefType) type;
				final IRNode typeDeclarationNode = srcRefType.getDeclaration();
				final boolean isThreadSafe = LockRules
						.isThreadSafe(typeDeclarationNode);
				isSafe = isThreadSafe || classDeclaresLocks(type);
			} else if (type instanceof IJavaIntersectionType) {
				final IJavaIntersectionType iType = (IJavaIntersectionType) type;
				isSafe = isSafeType(iType.getPrimarySupertype())
						|| isSafeType(iType.getSecondarySupertype());
			}
			isSafeTypeCache.put(type, isSafe);
			return isSafe;
		}
	}

	/**
	 * Return whether a FieldRef expression references a volatile or final
	 * field.
	 */
	private boolean isFinalOrVolatile(final IRNode fieldRef) {
		final IRNode fieldDecl = binder.getBinding(fieldRef);
		return TypeUtil.isFinal(fieldDecl) || TypeUtil.isVolatile(fieldDecl);
	}

	/**
	 * <p>
	 * Check if the expression used as the receiver in a method call refers to a
	 * "thread-safe" object and the field used to refer to the object is
	 * protected by a lock. Current implementation returns true if the field is
	 * protected by a lock or the field is final or volatile and the class
	 * declaring the field contains lock declarations, and one of the following
	 * is true
	 * <ul>
	 * <li>The expression is a FieldRef and the field is unique
	 * <li>The type of the expression indicates via <code>ThreadSafe</code> that
	 * the object protects itself (or otherwise describes how it should be
	 * protected).
	 * </ul>
	 * <p>
	 * (Other safe objects ought to include immutable, but we don't do this
	 * yet.)
	 * 
	 * @param actualRcvr
	 *            The expression used as the receiver for a method call.
	 */
	private void receiverIsSafeObject(final IRNode actualRcvr) {
		// First see if the referenced type is safe
		if (!isSafeType(binder.getJavaType(actualRcvr))) { // not safe
			final Operator op = JJNode.tree.getOperator(actualRcvr);
			if (FieldRef.prototype.includes(op)) {
				// If the field is unique, it is a safe object
				final boolean isUnique = UniquenessUtils
						.isUnique(this.binder.getBinding(actualRcvr));
				if (!isUnique) {
					/*
					 * See if the field is protected: either directly, or
					 * because the the field is final or volatile and the class
					 * contains lock annotations.
					 */
					if (isFinalOrVolatile(actualRcvr)) {
						if (mayBeAccessedByManyThreads(actualRcvr)) {
							// final/volatile field in a lock protected class
							final HintDrop info = makeWarningDrop(
									Messages.DSC_AGGREGATION_NEEDED,
									actualRcvr,
									Messages.LockAnalysis_ds_AggregationNeeded2,
									DebugUnparser.toString(actualRcvr));

							final IJavaType rcvrType = binder
									.getJavaType(FieldRef.getObject(actualRcvr));
							final Set<AbstractLockRecord> records = sysLockModelHandle
									.get().getRegionAndPolicyLocksInClass(
											rcvrType);
							for (final AbstractLockRecord lockRecord : records) {
								if (!lockRecord.lockDecl.equals(lockUtils
										.getMutex())) {
									lockRecord.lockDecl.addDependent(info);
								}
							}
						}
					} else {
						final RegionLockRecord neededLock = lockUtils
								.getLockForFieldRef(actualRcvr);
						if (neededLock != null) {
							// Lock protected field
							final HintDrop info = makeWarningDrop(
									Messages.DSC_AGGREGATION_NEEDED,
									actualRcvr,
									Messages.LockAnalysis_ds_AggregationNeeded2,
									DebugUnparser.toString(actualRcvr));
							neededLock.lockDecl.addDependent(info);
						}
					}
				}
			}
		}
	}

	/**
	 * Check if a <code>final</code> or <code>volatile</code> field is contained
	 * in a class that contains locking design intent. If so, then the field may
	 * be accessed concurrently, although we consider the field "protected" by
	 * virtue of it's finality or volatility. This method is called when we are
	 * trying to figure out if the object referenced by such a field can be
	 * concurrently accessed.
	 */
	private boolean mayBeAccessedByManyThreads(final IRNode fieldRef) {
		/* We assume fieldRef is final or volatile */
		// now see if class has programmer-declared locks in it.
		final IJavaType rcvrType = binder.getJavaType(FieldRef
				.getObject(fieldRef));
		return classDeclaresLocks(rcvrType);
	}

	private boolean classDeclaresLocks(final IJavaType type) {
		final Set<AbstractLockRecord> records = sysLockModelHandle.get()
				.getRegionAndPolicyLocksInClass(type);
		final Iterator<AbstractLockRecord> recIter = records.iterator();
		if (recIter.hasNext()) { // we have at least one lock
			int numLocks = 0;
			boolean containsMutex = false;
			while (recIter.hasNext()) {
				final AbstractLockRecord lr = recIter.next();
				numLocks += 1;
				containsMutex |= lr.lockDecl.equals(lockUtils.getMutex());
			}
			return !containsMutex || numLocks > 1;
		}
		return false;
	}

	private enum LockHeldResult {
		NOT_HELD {
			@Override
			public ResultDrop getResult(final LockVisitor lv,
					final PromiseDrop<? extends IAASTRootNode> promise,
					final int badCategory, final int goodCategory,
					final int badMsg, final int goodMsg,
					final int classInitMsg, final int threadConfinedMsg,
					final NeededLock lock, final IRNode useSite,
					final NeededLock altLock) {
				final ResultDrop result = lv.makeResultDrop(useSite,
						promise, false, badMsg, lock,
						DebugUnparser.toString(useSite), altLock);
				result.setCategorizingMessage(badCategory);
				return result;
			}
		},

		HELD {
			@Override
			public ResultDrop getResult(final LockVisitor lv,
					final PromiseDrop<? extends IAASTRootNode> promise,
					final int badCategory, final int goodCategory,
					final int badMsg, final int goodMsg,
					final int classInitMsg, final int threadConfinedMsg,
					final NeededLock lock, final IRNode useSite,
					final NeededLock altLock) {
				final ResultDrop result = lv.makeResultDrop(useSite,
						promise, true, goodMsg, lock,
						DebugUnparser.toString(useSite), altLock);
				result.setCategorizingMessage(goodCategory);
				return result;
			}
		},

		CLASS_INIT {
			@Override
			public ResultDrop getResult(final LockVisitor lv,
					final PromiseDrop<? extends IAASTRootNode> promise,
					final int badCategory, final int goodCategory,
					final int badMsg, final int goodMsg,
					final int classInitMsg, final int threadConfinedMsg,
					final NeededLock lock, final IRNode useSite,
					final NeededLock altLock) {
				final ResultDrop result = lv.makeResultDrop(useSite,
						promise, true, classInitMsg, lock,
						DebugUnparser.toString(useSite), altLock);
				result.setCategorizingMessage(goodCategory);
				return result;
			}
		},

		THREAD_CONFINED {
			@Override
			public ResultDrop getResult(final LockVisitor lv,
					final PromiseDrop<? extends IAASTRootNode> promise,
					final int badCategory, final int goodCategory,
					final int badMsg, final int goodMsg,
					final int classInitMsg, final int threadConfinedMsg,
					final NeededLock lock, final IRNode useSite,
					final NeededLock altLock) {
				final ResultDrop result = lv.makeResultDrop(useSite,
						promise, true, threadConfinedMsg, lock,
						DebugUnparser.toString(useSite), altLock);
				result.setCategorizingMessage(goodCategory);
				return result;
			}
		};

		public abstract ResultDrop getResult(LockVisitor lv,
				PromiseDrop<? extends IAASTRootNode> promise,
				int badCategory, int goodCategory, int badMsg,
				int goodMsg, int classInitMsg, int threadConfinedMsg,
				NeededLock lock, IRNode useSite, NeededLock altLock);

	}

	private abstract class LockChecker {
		/** The code site that requires a lock that is being checked. */
		protected final IRNode useSite;

		public LockChecker(final IRNode useSite) {
			this.useSite = useSite;
		}

		private LockHeldResult isLockSatisfied(final NeededLock neededLock,
				final MustHoldAnalysis.HeldLocks heldJUCLocks) {
			// Check if the lock is explicitly held
			if (ctxtTheHeldLocks.satisfiesLock(neededLock, thisExprBinder,
					binder, true)) {
				return LockHeldResult.HELD;
			}
			if (neededLock.isSatisfiedByLockSet(heldJUCLocks.heldLocks,
					thisExprBinder, binder)) {
				return LockHeldResult.HELD;
			}

			// Check if the lock does not need to be held because we are in the
			// class initializer
			if (ctxtClassInitializationLocks != null
					&& neededLock.isSatisfiedByLockSet(
							ctxtClassInitializationLocks, thisExprBinder,
							binder)) {
				return LockHeldResult.CLASS_INIT;
			}
			if (neededLock.isSatisfiedByLockSet(heldJUCLocks.classInitLocks,
					thisExprBinder, binder)) {
				return LockHeldResult.CLASS_INIT;
			}

			// Check if the lock does not need to be held because we are in a
			// thread-confined constructor
			if (ctxtThreadConfinedLocks != null
					&& neededLock.isSatisfiedByLockSet(ctxtThreadConfinedLocks,
							thisExprBinder, binder)) {
				return LockHeldResult.THREAD_CONFINED;
			}
			if (neededLock.isSatisfiedByLockSet(
					heldJUCLocks.singleThreadedLocks, thisExprBinder, binder)) {
				return LockHeldResult.THREAD_CONFINED;
			}

			// Otherwise, the lock is not satisfied
			return LockHeldResult.NOT_HELD;
		}

		// private boolean isLockSatisfied(
		// final NeededLock neededLock, final Set<HeldLock> heldJUCLocks) { //,
		// final Set<HeldLock> heldIntrinsicLocks) {
		// return ctxtTheHeldLocks.satisfiesLock(neededLock, thisExprBinder,
		// binder, true)
		// || neededLock.isSatisfiedByLockSet(heldJUCLocks, thisExprBinder,
		// binder);
		// // return neededLock.isSatisfiedByLockSet(heldIntrinsicLocks,
		// thisExprBinder, binder)
		// // || neededLock.isSatisfiedByLockSet(heldJUCLocks, thisExprBinder,
		// binder);
		// }

		public final void assureNeededLocks(final Set<NeededLock> neededLocks,
				final MustHoldAnalysis.HeldLocks heldJUCLocks,
				final int goodCategory, final int badCategory,
				final int goodMsg, final int goodAltMsg,
				final int classInitMsg, final int classInitAltMsg,
				final int threadConfinedMsg, final int threadConfinedAltMsg,
				final int badMsg, final int unresolvableMsg) {
			for (final NeededLock neededLock : neededLocks) {
				/*
				 * See if this lock might be available under a different name in
				 * an outer context. This is only possible when we are inside an
				 * anonymous class and the lock object is a qualified receiver.
				 */
				final boolean mayHaveAlternativeLock;
				final NeededLock alternativeLock;
				if (ctxtEnclosingRefs != null
						&& neededLock.mayHaveAliasInCallingContext()) {
					mayHaveAlternativeLock = true;
					alternativeLock = neededLock.getAliasInCallingContext(
							ctxtEnclosingRefs, neededLockFactory);
				} else {
					mayHaveAlternativeLock = false;
					alternativeLock = null;
				}

				/*
				 * Test for the needed lock. First try the lock, and if that
				 * doesn't work, try the alternative, if it exists.
				 */
				ResultDrop resultDrop = null;
				final PromiseDrop<? extends IAASTRootNode> promise = getPromiseDrop(neededLock);
				LockHeldResult lhr = isLockSatisfied(neededLock, heldJUCLocks);
				boolean isBad = false;
				if (lhr == LockHeldResult.NOT_HELD) {
					// Needed locks is not held. Might we have an alternative?
					if (mayHaveAlternativeLock) {
						if (alternativeLock != null) {
							lhr = isLockSatisfied(alternativeLock, heldJUCLocks);
							resultDrop = lhr.getResult(LockVisitor.this,
									promise, badCategory, goodCategory, badMsg,
									goodAltMsg, classInitAltMsg,
									threadConfinedAltMsg, neededLock, useSite,
									alternativeLock);
						} else {
							// We might, but don't, have an alternative, so we
							// have a not resolvable error
							resultDrop = makeResultDrop(useSite, promise,
									false, unresolvableMsg, neededLock,
									DebugUnparser.toString(useSite));
							resultDrop.setCategorizingMessage(badCategory);
						}
					} else {
						// No alternative, so normal lock is not held error
						isBad = true;
					}
				} else {
					isBad = true;
				}
				if (isBad) {
					resultDrop = lhr.getResult(LockVisitor.this, promise,
							badCategory, goodCategory, badMsg, goodMsg,
							classInitMsg, threadConfinedMsg, neededLock,
							useSite, alternativeLock);

					/*
					 * Add proposed promise if we are inside a constructor and
					 * the constructor is not thread-confined.
					 * ctxtSingleThreadedData may be non-null while
					 * ctxtInsideConstructor is null when we are inside the
					 * initializer of an anonymous class expression.
					 */
					if (ctxtSingleThreadedData != null
							&& !ctxtSingleThreadedData.isSingleThreaded
							&& ctxtInsideConstructor != null) {
						resultDrop.addProposal(new ProposedPromiseDrop(
								"Unique", "return", ctxtInsideConstructor,
								useSite, Origin.MODEL));
					}

					/*
					 * Add proposed promise if we are inside a method...the
					 * method might need a requires lock annotation.
					 */
					if (ctxtInsideMethod != null && !resultDrop.isConsistent()) {
						final String neededLockName = neededLock.getName();
						/*
						 * Don't propose a requires lock for MUTEX -- this seems
						 * rare.
						 */
						if (neededLockName != null
								&& !"MUTEX".equals(neededLockName)) {
							resultDrop.addProposal(new ProposedPromiseDrop(
									"RequiresLock", neededLockName,
									ctxtInsideMethod, useSite, Origin.MODEL));
						}
					}
				}

				addLockAcquisitionInformation(resultDrop, ctxtTheHeldLocks,
						heldJUCLocks.heldLocks);
        if (ctxtOnBehalfOfConstructor) {
          addSupportingInformation(resultDrop, ctxtInsideConstructor, Messages.LockAnalysis_ds_OnBehalfOfConstructor,
              ctxtConstructorName);
          final IKeyValue diffInfo = KeyValueUtility.getStringInstance(IDiffInfo.ANALYSIS_DIFF_HINT, ctxtConstructorName);
          resultDrop.addOrReplaceDiffInfo(diffInfo);
        }
				if (lhr == LockHeldResult.THREAD_CONFINED) {
					/*
					 * ctxtSingleThreadedData must be non-null and
					 * ctxtSingleThreadedData.isSingleThreaded must be true for
					 * this to be the case.
					 */
					ctxtSingleThreadedData.addSingleThreadedEvidence(resultDrop);
				}
				addTrustedLockDrop(ctxtTheHeldLocks, heldJUCLocks.heldLocks,
						neededLock, resultDrop);
				addAdditionalEvidence(resultDrop);
			}
		}

		protected abstract PromiseDrop<? extends IAASTRootNode> getPromiseDrop(
				NeededLock neededLock);

		protected abstract void addAdditionalEvidence(
				ResultDrop resultDrop);
	}

	/**
	 * @param call
	 *            An AnonClassExpression, ConstructorCall, MethodCall, or
	 *            NewExpression
	 */
	private void assureCall(final IRNode call) {
		final IRNode enclosingMethod = getEnclosingMethod();

		// Get the JUC locks that are held at entry to the method call
		final MustHoldAnalysis.HeldLocks heldJUCLocks = getHeldJUCLocks(call);
		// final Set<HeldLock> il = getHeldIntrinsicLocks(call);

		// Check that we hold the correct locks to call the method
		final LockUtils.GoodAndBadLocks<NeededLock> locks = lockUtils
				.getLocksForMethodCall(call, enclosingMethod);

		final IRNode mdecl = this.binder.getBinding(call);
		final RequiresLockPromiseDrop rlDrop = LockRules.getRequiresLock(mdecl);
		final LockChecker callChecker = new LockChecker(call) {
			@Override
			protected RequiresLockPromiseDrop getPromiseDrop(
					final NeededLock neededLock) {
				return rlDrop;
			}

			@Override
			protected void addAdditionalEvidence(
					final ResultDrop resultDrop) {
				// No additional evidence to add
			}
		};
		callChecker
				.assureNeededLocks(
						locks.goodLocks,
						heldJUCLocks,
						Messages.DSC_PRECONDITIONS_ASSURED,
						Messages.DSC_PRECONDITIONS_NOT_ASSURED,
						Messages.LockAnalysis_ds_PreconditionsAssured,
						Messages.LockAnalysis_ds_PreconditionsAssuredAlternative,
						Messages.LockAnalysis_ds_PreconditionsOkayClassInit,
						Messages.LockAnalysis_ds_PreconditionsOkayClassInitAlternative,
						Messages.LockAnalysis_ds_PreconditionsOkayThreadConfined,
						Messages.LockAnalysis_ds_PreconditionsOkayThreadConfinedAlternative,
						Messages.LockAnalysis_ds_PreconditionsNotAssured,
						Messages.LockAnalysis_ds_PreconditionsNotResolvable);

		// Locks that cannot be resolved lead to assurance failures
		for (final LockSpecificationNode lockSpec : locks.badLocks) {
			final ResultDrop result = makeResultDrop(call, rlDrop,
					false, Messages.LockAnalysis_ds_PreconditionNotResolvable,
					lockSpec.toString(), DebugUnparser.toString(call));
			result.setCategorizingMessage(Messages.DSC_PRECONDITIONS_NOT_ASSURED);
		}

		/*
		 * When state is aggregated using uniqueness, we must treat some method
		 * calls as being indirect accesses to shared state. The normal
		 * assumption that a method takes care of its own protection cannot be
		 * applied because the class implementing the method is not aware of the
		 * fact that it's state has been aggregated into another object.
		 * Consider the case of a method call <code>this.f.m()</code>, where
		 * <code>this</code> has type <code>C</code>, field <code>f</code> has
		 * type <code>D</code>, and method <code>m</code> may affect region
		 * <code>D.R</code>. Additionally suppose that <code>C </code> maps
		 * region <code>R</code> into region <code>C.Q</code>, and associates
		 * <code>Q</code> with lock <code>L</code>. We must determine that
		 * <code>m()</code> affects <code>this.Q</code> and that therefore the
		 * lock <code>L</code> must be held.
		 */
		final Set<NeededLock> neededLocks = lockUtils
				.getLocksForMethodAsRegionRef(effects, ctxtBcaQuery, ctxtTheReceiverNode,
						ctxtConflicter, call, enclosingMethod);
		final LockChecker indirectAccessChecker = new LockChecker(call) {
			@Override
			protected LockModel getPromiseDrop(final NeededLock neededLock) {
				return neededLock.getLockPromise();
			}

			@Override
			protected void addAdditionalEvidence(
					final ResultDrop resultDrop) {
				// TODO: Add rationale based on method effects and region
				// mapping
			}
		};
		indirectAccessChecker
				.assureNeededLocks(
						neededLocks,
						heldJUCLocks,
						Messages.DSC_INDIRECT_FIELD_ACCESS_ASSURED,
						Messages.DSC_INDIRECT_FIELD_ACCESS_NOT_ASSURED,
						Messages.LockAnalysis_ds_IndirectFieldAccessAssured,
						Messages.LockAnalysis_ds_IndirectFieldAccessAssuredAlternative,
						Messages.LockAnalysis_ds_IndirectFieldAccessOkayClassInit,
						Messages.LockAnalysis_ds_IndirectFieldAccessOkayClassInitAlternative,
						Messages.LockAnalysis_ds_IndirectFieldAccessOkayThreadConfined,
						Messages.LockAnalysis_ds_IndirectFieldAccessOkayThreadConfinedAlternative,
						Messages.LockAnalysis_ds_IndirectFieldAccessNotAssured,
						Messages.LockAnalysis_ds_IndirectFieldAccessNotResolvable);
	}

	/**
	 * This is preferable to using
	 * {@link PromiseUtil#getEnclosingMethod(IRNode)} because it understands the
	 * current context, i.e., whether we are analyzing field initializers or
	 * instance initializer blocks on behalf of a particular constructor. It
	 * returns that constructor as the enclosing method. Using
	 * {@code PromiseUtil.getEnclosingMethod} in such cases would return the
	 * special instance initializer method.
	 */
	private IRNode getEnclosingMethod() {
		if (ctxtInsideMethod != null) {
			// Method or class initializer
			return ctxtInsideMethod;
		} else if (ctxtInsideConstructor != null) {
			return ctxtInsideConstructor;
		} else {
			// XXX: this is not really the right thing to do, but it seems to
			// work
			// We have to deal with Enumerations correctly, but don't have time
			// for
			// that now
			return JavaPromise.getClassInitOrNull(ctxtTypeDecl);
			// throw new UnsupportedOperationException("No enclosing method");
		}
	}

	private MustHoldAnalysis.HeldLocks getHeldJUCLocks(final IRNode node) {
		return ctxtHeldLocksQuery.getResultFor(node);
	}

	// private Set<HeldLock> getHeldIntrinsicLocks(final IRNode node) {
	// final IRNode decl = getEnclosingMethod(node);
	// if (decl != null) {
	// final IRNode constructorContext =
	// ConstructorDeclaration.prototype.includes(decl) ? decl : null;
	// if (jucLockUsageManager.usesIntrinsicLocks(decl)) {
	// return intrinsicLock.getHeldLocks(node, constructorContext);
	// } else {
	// return Collections.emptySet();
	// }
	// }
	// // Shouldn't get here?
	// throw new IllegalStateException("Shouldn't get here");
	// }

	private List<IRNode> getJUCLockFields(final IRNode lockExpr) {
		/*
		 * We build a set of fields whose type is
		 * java.util.concurrent.locks.Lock or
		 * java.util.concurrent.locks.ReadWriteLock
		 */
		final IJavaType type = binder.getJavaType(lockExpr);
		if (type instanceof IJavaDeclaredType) {
			final List<IRNode> lockFields = new LinkedList<IRNode>();
			IJavaDeclaredType currentType = (IJavaDeclaredType) type;
			while (currentType != null) {
				final IRNode classDecl = currentType.getDeclaration();
				for (final IRNode vd : VisitUtil
						.getClassFieldDeclarators(classDecl)) {
					final IJavaType fieldType = binder.getJavaType(vd);
					if (lockUtils.implementsLock(fieldType)
							|| lockUtils.implementsReadWriteLock(fieldType)) {
						lockFields.add(vd);
					}
				}
				currentType = currentType.getSuperclass(binder
						.getTypeEnvironment());
			}
			return lockFields;
		} else { // IJavaArrayType
			return Collections.<IRNode> emptyList();
		}
	}

	// ----------------------------------------------------------------------
	// Traversal/visitor methods
	// ----------------------------------------------------------------------

	// Don't go inside of annotations
	@Override
	public Void visitAnnotation(final IRNode anno) {
		return null;
	}

	@Override
	public Void visitAnonClassExpression(final IRNode expr) {
		assureCall(expr);
		// Traverse into the arguments, but *not* the body
		doAccept(AnonClassExpression.getArgs(expr));

		/*
		 * We are going to recursively re-enter this class via the use of an
		 * InstanceInitVisitor instance. Thus we need to make sure to back up
		 * and otherwise properly establish the global state before re-entry,
		 * and to restore the state after visiting the internals of the
		 * anonymous class.
		 * 
		 * In general, we need to update state that is used to look up locks,
		 * but preserve state that is used to determine locks that are currently
		 * held.
		 */
		final IRNode oldTypeDecl = ctxtTypeDecl;
		final IJavaDeclaredType oldJavaType = ctxtJavaType;
		final IRNode oldInsideMethod = ctxtInsideMethod;
		final BindingContextAnalysis.Query oldBcaQuery = ctxtBcaQuery;
		final JavaFlowAnalysisQuery<HeldLocks> oldHeldLocksQuery = ctxtHeldLocksQuery;
		final MustHoldAnalysis.LocksForQuery oldLocksForQuery = ctxtLocksForQuery;
		final MustReleaseAnalysis.Query oldMRQ = ctxtMustReleaseQuery;
		final ConflictChecker oldConflicter = ctxtConflicter;
		final boolean oldOnBehalfOfConstructor = ctxtOnBehalfOfConstructor;
		final IRNode oldInsideConstructor = ctxtInsideConstructor;
		/*
		 * If we had some way of marking the constructor of the anonymous class
		 * as being thread-confining, which we currently don't because we cannot
		 * but annotations on anonymous class expressions, then we would want to
		 * maintain a stack of the SingleThreadedData objects. But since the
		 * only constructor that could possibly be single threaded is the outer
		 * most one from a non-anonymous class, there is no point in doing that.
		 * We simply maintain the ctxtSingleThreadedData value while analyzing
		 * the initializer of the anonymous class expression.
		 */
		// final LockExpressions.SingleThreadedData oldSingleThreadedData =
		// ctxtSingleThreadedData;
		final String oldConstructorName = ctxtConstructorName;
		final IRNode oldTheReceiverNode = ctxtTheReceiverNode;
		final MethodCallUtils.EnclosingRefs oldEnclosingRefs = ctxtEnclosingRefs;
		final boolean oldCtxtInsideAnonClassExpr = ctxtInsideAnonClassExpr;
		InstanceInitializationVisitor.processAnonClassExpression(expr, this,
				new InstanceInitAction() {
					public void tryBefore() {
						ctxtInsideAnonClassExpr = true;
						// Create the substitution map
						ctxtEnclosingRefs = MethodCallUtils
								.getEnclosingInstanceReferences(binder,
										thisExprBinder, expr, binder
												.getBinding(AnonClassExpression
														.getType(expr)),
										oldTheReceiverNode,
										getEnclosingMethod());

						/*
						 * Update the type being analyzed to be the anonymous
						 * class expression
						 */
						ctxtTypeDecl = expr;
						ctxtJavaType = (IJavaDeclaredType) JavaTypeFactory
								.getMyThisType(ctxtTypeDecl);

						/*
						 * The information needed for checking returned locks
						 * can be preserved, it is not needed by the recursive
						 * visitation because we will only visit field
						 * initializers and instance initializers.
						 */
						/* left hand side is irrelevant here. */
						/*
						 * The recursive visit is analyzing as the implicit
						 * initialization method for the class. We set the
						 * receiver accordingly.
						 */
						ctxtInsideMethod = JavaPromise
								.getInitMethodOrNull(expr);

						/*
						 * MUST update receiver node before creating the flow
						 * analyses because they indirectly use this field via
						 * the this expression binder object used by the lock
						 * factories. They have access to this factories via the
						 * lock utils object and the juc lock usage manager.
						 */
						ctxtTheReceiverNode = JavaPromise
								.getReceiverNodeOrNull(ctxtInsideMethod);

						ctxtBcaQuery = ctxtBcaQuery.getSubAnalysisQuery(expr);
						ctxtHeldLocksQuery = ctxtHeldLocksQuery
								.getSubAnalysisQuery(expr);
						ctxtLocksForQuery = ctxtLocksForQuery
								.getSubAnalysisQuery(expr);
						ctxtMustReleaseQuery = ctxtMustReleaseQuery
								.getSubAnalysisQuery(expr);

						ctxtConflicter = new ConflictChecker(binder, mayAlias);
						ctxtOnBehalfOfConstructor = false;
						ctxtInsideConstructor = null;
						ctxtConstructorName = null;
					}

					public void finallyAfter() {
						// restore the global state
						ctxtInsideAnonClassExpr = oldCtxtInsideAnonClassExpr;
						ctxtEnclosingRefs = oldEnclosingRefs;
						ctxtTypeDecl = oldTypeDecl;
						ctxtJavaType = oldJavaType;
						ctxtInsideMethod = oldInsideMethod;
						ctxtBcaQuery = oldBcaQuery;
						ctxtHeldLocksQuery = oldHeldLocksQuery;
						ctxtLocksForQuery = oldLocksForQuery;
						ctxtMustReleaseQuery = oldMRQ;
						ctxtConflicter = oldConflicter;
						ctxtOnBehalfOfConstructor = oldOnBehalfOfConstructor;
						ctxtInsideConstructor = oldInsideConstructor;
						ctxtConstructorName = oldConstructorName;
						ctxtTheReceiverNode = oldTheReceiverNode;
					}

					public void afterVisit() {
						// nothing
					}
				});

		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitArrayRefExpression(final IRNode expr) {
		final boolean isWrite = this.ctxtIsLHS;
		this.ctxtIsLHS = false;

		dereferencesSafeObject(expr);
		assureRegionRef(expr, lockUtils.getLocksForDirectRegionAccess(
				effects,
				ctxtBcaQuery,
				ctxtTheReceiverNode,
				expr,
				!isWrite,
				lockUtils.createInstanceTarget(
						ArrayRefExpression.getArray(expr),
						RegionModel.getInstanceRegion(expr))));
		// continue into the expression
		doAcceptForChildren(expr);
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitAssignExpression(final IRNode expr) {
		this.ctxtIsLHS = true;
		this.doAccept(AssignExpression.getOp1(expr));
		this.doAccept(AssignExpression.getOp2(expr));
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitClassDeclaration(final IRNode expr) {
		// Do not traverse inside of type declarations
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitClassInitializer(final IRNode expr) {
		if (TypeUtil.isStatic(expr)) {
			// always go inside of static initializers
			final IRNode classDecl = VisitUtil.getClosestType(expr);
			ctxtInsideMethod = JavaPromise.getClassInitOrNull(classDecl);
			ctxtBcaQuery = bindingContextAnalysis
					.getExpressionObjectsQuery(ctxtInsideMethod);
			updateJUCAnalysisQueries(ctxtInsideMethod);
			ctxtConflicter = new ConflictChecker(binder, mayAlias);
			// The receiver is non-existent
			ctxtTheReceiverNode = null;
			// We the static locks are held
			ctxtClassInitializationLocks = convertStaticInitializerBlock(expr,
					ctxtJavaType);

			try {
				doAcceptForChildren(expr);
			} finally {
				ctxtClassInitializationLocks = null;
				ctxtInsideMethod = null;
				ctxtBcaQuery = null;
				ctxtHeldLocksQuery = null;
				ctxtLocksForQuery = null;
				ctxtMustReleaseQuery = null;
				ctxtConflicter = null;
			}
		} else {
			/*
			 * Only go inside of instance initializers if we are being called by
			 * the InstanceInitVisitor! In this case, the InstanceInitVisitor
			 * directly traverses into the children of the ClassInitializer, so
			 * we don't even get here. The receiver node is preserved, i.e., it
			 * remains the receiver node of the constructor on whose behalf the
			 * class initializer is being analyzed.
			 */
		}
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitConstructorCall(final IRNode expr) {
		// First process the constructor call and it's arguments
		assureCall(expr);
		doAcceptForChildren(expr);

		// visit initializers next
		final BindingContextAnalysis.Query oldBCAQuery = ctxtBcaQuery;
		final JavaFlowAnalysisQuery<HeldLocks> oldHeldLocksQuery = ctxtHeldLocksQuery;
		final MustHoldAnalysis.LocksForQuery oldLocksForQuery = ctxtLocksForQuery;
		final MustReleaseAnalysis.Query oldCtxtMustReleaseQuery = ctxtMustReleaseQuery;
		InstanceInitializationVisitor.processConstructorCall(expr,
				TypeDeclaration.getBody(ctxtTypeDecl), this,
				new InstanceInitAction() {
					public void tryBefore() {
						ctxtOnBehalfOfConstructor = true;
						ctxtBcaQuery = ctxtBcaQuery.getSubAnalysisQuery(expr);
						ctxtHeldLocksQuery = ctxtHeldLocksQuery
								.getSubAnalysisQuery(expr);
						ctxtLocksForQuery = ctxtLocksForQuery
								.getSubAnalysisQuery(expr);
						ctxtMustReleaseQuery = ctxtMustReleaseQuery
								.getSubAnalysisQuery(expr);
					}

					public void finallyAfter() {
						ctxtOnBehalfOfConstructor = false;
						ctxtBcaQuery = oldBCAQuery;
						ctxtHeldLocksQuery = oldHeldLocksQuery;
						ctxtLocksForQuery = oldLocksForQuery;
						ctxtMustReleaseQuery = oldCtxtMustReleaseQuery;
					}

					public void afterVisit() {
						// nothing
					}
				});

		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitConstructorDeclaration(final IRNode cdecl) {
    try {
			// First thing: update the receiver node
			ctxtTheReceiverNode = JavaPromise.getReceiverNodeOrNull(cdecl);
			ctxtInsideConstructor = cdecl;
			ctxtBcaQuery = bindingContextAnalysis
					.getExpressionObjectsQuery(cdecl);
			ctxtConflicter = new ConflictChecker(binder, mayAlias);
      ctxtConstructorName = JavaNames.genMethodConstructorName(cdecl);
			ctxtSingleThreadedData = jucLockUsageManager
					.getSingleThreadedData(cdecl);

			if (ctxtSingleThreadedData.isSingleThreaded) {
				ctxtThreadConfinedLocks = convertSingleThreadedConstructor(
						cdecl, ctxtJavaType);
			}

			// Add locks from lock preconditions to the lock context
			final LockStackFrame reqFrame = ctxtTheHeldLocks.pushNewFrame();
			try {
			  final Set<LockSpecificationNode> locksOnParameters = 
			      new HashSet<LockSpecificationNode>();
				processLockPreconditions(cdecl, reqFrame, locksOnParameters);
				// ConstructorCall handles visiting initializers now
				try {
					updateJUCAnalysisQueries(cdecl);
					checkMutabilityOfFormalParameters(cdecl, locksOnParameters,
					    Collections.<LockSpecificationNode>emptySet());
					doAcceptForChildren(cdecl);
				} finally {
					ctxtHeldLocksQuery = null;
					ctxtLocksForQuery = null;
					ctxtMustReleaseQuery = null;
				}
			} finally {
				// remove the the lock preconditions
				ctxtTheHeldLocks.popFrame();
			}
			// TODO: Check if any of the lock preconditions are useless (why?)
		} finally {
			ctxtTheReceiverNode = null;
			ctxtInsideConstructor = null;
			ctxtBcaQuery = null;
			ctxtConflicter = null;
			ctxtConstructorName = null;
			ctxtSingleThreadedData = null;
			ctxtThreadConfinedLocks = null;
		}

		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitEnumDeclaration(final IRNode expr) {
		// Do not traverse into type declarations
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitFieldRef(final IRNode fieldRef) {
		final boolean isWrite = this.ctxtIsLHS;
		this.ctxtIsLHS = false;

		dereferencesSafeObject(fieldRef);

		// Only non-final fields need to be protected
		final IRNode id = binder.getBinding(fieldRef);
		if (!TypeUtil.isFinal(id)) {
			final IRegion fieldAsRegion = RegionModel.getInstance(binder
					.getBinding(fieldRef));
			final Target target;
			if (fieldAsRegion.isStatic()) {
				target = lockUtils.createClassTarget(fieldAsRegion);
			} else {
				target = lockUtils.createInstanceTarget(
						FieldRef.getObject(fieldRef), fieldAsRegion);
			}
			assureRegionRef(fieldRef, lockUtils.getLocksForDirectRegionAccess(
					effects, ctxtBcaQuery, ctxtTheReceiverNode, fieldRef, !isWrite, target));
		}

		// continue into the expression
		doAcceptForChildren(fieldRef);
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitInterfaceDeclaration(final IRNode expr) {
		// Do not traverse into type declarations
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitMethodCall(final IRNode expr) {
		final IRNode methodDecl = this.binder.getBinding(expr);
		// Don't do anything if the method call is a getter method from a Java 5
		// annotation
		if (AnnotationElement.prototype.includes(methodDecl)) {
			return null;
		}

		final MethodCall call = (MethodCall) JJNode.tree.getOperator(expr);

		/*
		 * If the method call is to a method from
		 * java.util.concurrent.locks.Lock, then we must process it specially
		 * because it affects the set of held locks, and they have other
		 * constraints on them.
		 * 
		 * This use of the flow analyses does not need to be specially guarded
		 * because it already is guarded by whichLockMethod().
		 */
		final LockMethods lockMethod = lockUtils.whichLockMethod(expr);
		if (lockMethod == LockMethods.IDENTICALLY_NAMED_METHOD) {
			// Warn about the use of lock()/unlock() methods that aren't from
			// the lock class
			makeWarningDrop(Messages.DSC_NOT_A_LOCK_METHOD, expr,
					Messages.LockAnalysis_ds_MasqueradingCall,
					DebugUnparser.toString(expr));
		} else if (lockMethod != LockMethods.NOT_A_LOCK_METHOD) {
			final IRNode object = call.get_Object(expr);
			final IRNode enclosingMethod = getEnclosingMethod();
			if (lockUtils.getFinalExpressionChecker(ctxtBcaQuery,
					enclosingMethod, null).isFinal(object)) {
				final Set<HeldLock> lockSet = new HashSet<HeldLock>();
				lockUtils.convertJUCLockExpr(object, heldLockFactory,
						enclosingMethod, null, lockSet);
				if (lockSet.isEmpty()) {
					makeWarningDrop(Messages.DSC_UNIDENTIFIABLE_LOCK_WARNING,
							object,
							Messages.LockAnalysis_ds_UnidentifiableLock,
							DebugUnparser.toString(object));
				}

				/* If it is a lock() call, look for the matching unlock() calls. */
				if (lockMethod.isLock) {
					final Set<IRNode> unlocks = ctxtMustReleaseQuery
							.getResultFor(expr); // mustRelease.getUnlocksFor(expr);
					if (unlocks == null) { // POISONED!
						final HintDrop match = makeWarningDrop(
								Messages.DSC_MATCHING_CALLS, expr,
								Messages.LockAnalysis_ds_PoisonedLockCall,
								lockMethod.name);
						for (final HeldLock lock : lockSet) {
							lock.getLockPromise().addDependent(match);
						}
					} else {
						if (unlocks.isEmpty()) {
							final HintDrop match = makeWarningDrop(
									Messages.DSC_MATCHING_CALLS, expr,
									Messages.LockAnalysis_ds_NoMatchingUnlocks,
									lockMethod.name);
							for (final HeldLock lock : lockSet) {
								lock.getLockPromise().addDependent(match);
							}
						} else {
							for (final IRNode n : unlocks) {
							  int lineNumber = -1;
							  final IJavaRef javaRef = JavaNode.getJavaRef(n);
						    if (javaRef != null)
						      lineNumber = javaRef.getLineNumber();
								final HintDrop match = makeInfoDrop(
										Messages.DSC_MATCHING_CALLS,
										expr,
										Messages.LockAnalysis_ds_MatchingUnlock,
										lockMethod.name, lineNumber);
								for (final HeldLock lock : lockSet) {
									lock.getLockPromise().addDependent(match);
								}
							}
						}
					}
				}

				/*
				 * If it is an unlock() call, look for the matching lock()
				 * calls.
				 */
				if (lockMethod == LockMethods.UNLOCK) {
					final Set<IRNode> locks = ctxtLocksForQuery
							.getResultFor(expr); // mustHold.getLocksFor(expr);
					if (locks == null) { // POISONED!
						final HintDrop match = makeWarningDrop(
								Messages.DSC_MATCHING_CALLS, expr,
								Messages.LockAnalysis_ds_PoisonedUnlockCall);
						for (final HeldLock lock : lockSet) {
							lock.getLockPromise().addDependent(match);
						}
					} else {
						if (locks.isEmpty()) {
							final HintDrop match = makeWarningDrop(
									Messages.DSC_MATCHING_CALLS, expr,
									Messages.LockAnalysis_ds_NoMatchingLocks);
							for (final HeldLock lock : lockSet) {
								lock.getLockPromise().addDependent(match);
							}
						} else {
							for (final IRNode n : locks) {
							  int lineNumber = -1;
                final IJavaRef javaRef = JavaNode.getJavaRef(n);
                if (javaRef != null)
                  lineNumber = javaRef.getLineNumber();
								final HintDrop match = makeInfoDrop(
										Messages.DSC_MATCHING_CALLS, expr,
										Messages.LockAnalysis_ds_MatchingLock,
										MethodCall.getMethod(n), lineNumber);
								for (final HeldLock lock : lockSet) {
									lock.getLockPromise().addDependent(match);
								}
							}
						}
					}
				}
			} else {
				makeWarningDrop(Messages.DSC_NONFINAL_EXPRESSION_WARNING, expr,
						Messages.LockAnalysis_ds_NonfinalExpression,
						DebugUnparser.toString(expr));
			}
		}

		// Warn about the use of readLock()/writeLock() methods that aren't from
		// ReadWriteLock
		final ReadWriteLockMethods rwLockMethod = lockUtils
				.whichReadWriteLockMethod(expr);
		if (rwLockMethod == ReadWriteLockMethods.IDENTICALLY_NAMED_METHOD) {
			// Warn about the use of lock()/unlock() methods that aren't from
			// the lock class
			makeWarningDrop(Messages.DSC_NOT_A_LOCK_METHOD, expr,
					Messages.LockAnalysis_ds_MasqueradingCall2,
					DebugUnparser.toString(expr));
		}

		/* Proceed with normal assurance of the method call */
		assureCall(expr);
		if (!TypeUtil.isStatic(methodDecl)) {
			/*
			 * Check if the receiver is a "safe" object. This does not apply if
			 * the method call is to a Lock method.
			 */
			if (lockMethod == LockMethods.NOT_A_LOCK_METHOD
					&& rwLockMethod == ReadWriteLockMethods.NOT_A_READWRITELOCK_METHOD) {
				receiverIsSafeObject(call.get_Object(expr));
			}
		}
		// Continue into the expression
		doAcceptForChildren(expr);
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitMethodDeclaration(final IRNode mdecl) {
		try {
			// First thing: update the receiver node
			ctxtTheReceiverNode = JavaPromise.getReceiverNodeOrNull(mdecl);

			/*
			 * Push any locks acquired to the front of the lock context. Locks
			 * can come from being a synchronized method and from lock
			 * preconditions.
			 */
			final LockStackFrame syncFrame = ctxtTheHeldLocks.pushNewFrame();
			boolean syncLockIsIdentifiable = false;
			boolean syncLockIsPolicyLock = false;
			if (JavaNode.getModifier(mdecl, JavaNode.SYNCHRONIZED)) {
				final Set<HeldLock> syncMethodLocks = new HashSet<HeldLock>();
				lockUtils.convertSynchronizedMethod(mdecl, heldLockFactory,
						ctxtTheReceiverNode, ctxtJavaType, ctxtTypeDecl,
						syncMethodLocks);
				syncFrame.push(syncMethodLocks);
				// convertSynchronizedMethod(mdecl, ctxtJavaType, ctxtTypeDecl,
				// syncFrame);
				syncLockIsPolicyLock = isPolicyLockMethod(mdecl);

				/*
				 * Complain if expression doesn't match a named lock, or if the
				 * only named lock is the MUTEX. (Don't complain if the lock is
				 * a policy lock though.)
				 */
				boolean justMUTEX = true;
				for (final StackLock guard : syncFrame) {
					justMUTEX &= guard.lock.getLockPromise().equals(
							lockUtils.getMutex());
				}
				if (justMUTEX && !syncLockIsPolicyLock) {
					if (TypeUtil.isStatic(mdecl)) {
						makeWarningDrop(
								Messages.DSC_UNIDENTIFIABLE_LOCK_WARNING,
								mdecl,
								Messages.LockAnalysis_ds_SynchronizedStaticMethodWarningDetails,
								JavaNames.genMethodConstructorName(mdecl),
								JavaNames.getTypeName(ctxtTypeDecl));
					} else {
						makeWarningDrop(
								Messages.DSC_UNIDENTIFIABLE_LOCK_WARNING,
								mdecl,
								Messages.LockAnalysis_ds_SynchronizedMethodWarningDetails,
								JavaNames.genMethodConstructorName(mdecl));
					}
				} else {
					// Sync is a declared lock/policy lock
					syncLockIsIdentifiable = true;
				}
			}

      final Set<LockSpecificationNode> locksOnParameters = 
          new HashSet<LockSpecificationNode>();
	    final LockStackFrame reqFrame = ctxtTheHeldLocks.pushNewFrame();
			processLockPreconditions(mdecl, reqFrame, locksOnParameters);

			/*
			 * If the method is declared to return a particular lock, then set
			 * up context for checking return statements. We set the value of
			 * ctxtReturnedLock and ctxtReturnsLockDrop for use by
			 * visitReturnStatement().
			 */
			ctxtInsideMethod = mdecl;
			ctxtBcaQuery = bindingContextAnalysis
					.getExpressionObjectsQuery(mdecl);
			updateJUCAnalysisQueries(mdecl);
			ctxtConflicter = new ConflictChecker(binder, mayAlias);
			final ReturnsLockPromiseDrop returnedLockName = LockUtils
					.getReturnedLock(mdecl);
      final Set<LockSpecificationNode> returnsLocksOnParameters = 
          new HashSet<LockSpecificationNode>();
			if (returnedLockName != null) {
				ctxtReturnsLockDrop = returnedLockName;
				ctxtReturnedLock = LockUtils.convertLockNameToMethodContext(
						mdecl, heldLockFactory, returnedLockName.getAAST()
								.getLock(), false, null, ctxtTheReceiverNode,
								returnsLocksOnParameters);
			}
			// Analyze the children
			checkMutabilityOfFormalParameters(
			    mdecl, locksOnParameters, returnsLocksOnParameters);
			doAcceptForChildren(mdecl);

			/*
			 * Check to see if the synchronization was used for anything. This
			 * needs to be done last because it queries isNeeded(), which is a
			 * side-effect of calling containsLock() on the lock stack frame.
			 */
			if (syncLockIsIdentifiable && !syncLockIsPolicyLock
					&& !syncFrame.isNeeded()) {
				final HintDrop info = makeWarningDrop(
						Messages.DSC_SYNCHRONIZED_UNUSED_WARNING, mdecl,
						Messages.LockAnalysis_ds_SynchronizationUnused,
						syncFrame);
				for (final StackLock stackLock : syncFrame) {
					stackLock.lock.getLockPromise().addDependent(info);
				}
			}

			// TODO: Check to see if the lock preconditions were needed
		} finally {
			// Cleanup the state used for checking returns
			ctxtTheReceiverNode = null;
			ctxtInsideMethod = null;
			ctxtBcaQuery = null;
			ctxtHeldLocksQuery = null;
			ctxtLocksForQuery = null;
			ctxtMustReleaseQuery = null;
			ctxtConflicter = null;
			ctxtReturnsLockDrop = null;
			ctxtReturnedLock = null;

			ctxtTheHeldLocks.popFrame();
			ctxtTheHeldLocks.popFrame();
		}
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitNewExpression(final IRNode expr) {
		assureCall(expr);
		// Continue into the expression
		doAcceptForChildren(expr);
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitOpAssignExpression(final IRNode expr) {
		this.ctxtIsLHS = true;
		this.doAccept(OpAssignExpression.getOp1(expr));
		this.doAccept(OpAssignExpression.getOp2(expr));
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitPostDecrementExpression(final IRNode expr) {
		this.ctxtIsLHS = true;
		this.doAccept(PostDecrementExpression.getOp(expr));
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitPostIncrementExpression(final IRNode expr) {
		this.ctxtIsLHS = true;
		this.doAccept(PostIncrementExpression.getOp(expr));
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitPreDecrementExpression(final IRNode expr) {
		this.ctxtIsLHS = true;
		this.doAccept(PreDecrementExpression.getOp(expr));
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitPreIncrementExpression(final IRNode expr) {
		this.ctxtIsLHS = true;
		this.doAccept(PreIncrementExpression.getOp(expr));
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitReturnStatement(final IRNode rstmt) {
		/*
		 * If the method has a @returnsLock annotation, check the return value
		 * against the declared lock.
		 */
		if (ctxtReturnedLock != null) {
			final IRNode expr = ReturnStatement.getValue(rstmt);
			boolean correct = false;
			/*
			 * XXX: This is not entirely correct for ReadWriteLocks, although it
			 * works.
			 */
			/*
			 * ctxtInsideMethod must be non-null, and will not refer to a
			 * ClassInitDeclaration because "return" can only be inside of a
			 * method. Furthermore, we can pass null to as the constructor
			 * context to convertLockExpr(), because we must be inside a method.
			 */
			for (final HeldLock lock : convertLockExpr(expr, ctxtBcaQuery,
					ctxtInsideMethod, rstmt)) {
				correct |= ctxtReturnedLock.mustAlias(lock, thisExprBinder,
						binder);
			}

			if (correct) {
				if (ctxtReturnsLockDrop != null) {
					final ResultDrop result = makeResultDrop(rstmt,
							ctxtReturnsLockDrop, true,
							Messages.LockAnalysis_ds_ReturnAssured,
							ctxtReturnedLock);
					result.setCategorizingMessage(Messages.DSC_RETURN_ASSURED);
				} else {
					LOG.log(Level.SEVERE,
							"null returnLock drop in checkReturnsLock");
				}
			} else {
				if (ctxtReturnsLockDrop != null) {
					final ResultDrop result = makeResultDrop(rstmt,
							ctxtReturnsLockDrop, false,
							Messages.LockAnalysis_ds_ReturnNotAssured,
							ctxtReturnedLock);
					result.setCategorizingMessage(Messages.DSC_RETURN_NOT_ASSURED);
				} else {
					LOG.log(Level.SEVERE,
							"null returnLock drop in checkReturnsLock");
				}
			}
		}

		// Analyze the children
		doAcceptForChildren(rstmt);
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitSynchronizedStatement(final IRNode syncBlock) {
		try {
			final IRNode lockExpr = SynchronizedStatement.getLock(syncBlock);
			final LockStackFrame syncFrame = ctxtTheHeldLocks.pushNewFrame();
			boolean lockIsIdentifiable = true;
			// XXX: This field may be obsolete --- 8 OCt 2007
			boolean lockIsPolicyLock = false;

			/*
			 * Test for mixed usage: warn about synchronizing on JUC locks! Only
			 * convert lock expression for intrinsic locks.
			 */
			final IJavaType typeOfLockExpr = binder.getJavaType(lockExpr);
			if (lockUtils.implementsLock(typeOfLockExpr)
					|| lockUtils.implementsReadWriteLock(typeOfLockExpr)) {
				makeWarningDrop(Messages.DSC_MIXED_PARADIGM, lockExpr,
						Messages.LockAnalysis_ds_SyncedJUCLock,
						DebugUnparser.toString(lockExpr));
				lockIsIdentifiable = false;
			} else { // possible intrinsic lock
				// Only decode the lock if it is a final expression
				final IRNode enclosingMethod = getEnclosingMethod();
				if (lockUtils.getFinalExpressionChecker(ctxtBcaQuery,
						enclosingMethod, syncBlock).isFinal(lockExpr)) {
					// Push the acquired locks into the lock context
					// convertLockExpr(lockExpr, getEnclosingMethod(lockExpr),
					// syncBlock, syncFrame);
					final Set<HeldLock> heldLocks = new HashSet<HeldLock>();
					lockUtils.convertIntrinsicLockExpr(lockExpr,
							heldLockFactory, enclosingMethod, syncBlock,
							heldLocks);
					syncFrame.push(heldLocks);

					lockIsPolicyLock = isPolicyLockExpr(lockExpr);

					/*
					 * Complain if expression doesn't match a named lock, or if
					 * the only named lock is the MUTEX. (Don't complain if the
					 * lock is a policy lock though.)
					 */
					boolean justMUTEX = true;
					for (final StackLock guard : syncFrame) {
						justMUTEX &= guard.lock.getLockPromise().equals(
								lockUtils.getMutex());

						// Complain if the lock acquisition is potentially
						// redundant
						if (ctxtTheHeldLocks.oldFramesContainLock(guard.lock,
								thisExprBinder, binder)) {
							final HintDrop info = makeWarningDrop(
									Messages.DSC_REDUNDANT_SYNCHRONIZED,
									syncBlock,
									Messages.LockAnalysis_ds_RedundantSynchronized,
									guard.lock);
							guard.lock.getLockPromise().addDependent(info);
						}
					}
					if (justMUTEX && !lockIsPolicyLock) {
						/*
						 * The expression cannot be resolved to any known
						 * intrinsic Java lock. Does the class of the object
						 * have JUC locks declared for it? If so, we issue a
						 * special warning.
						 * 
						 * In fact, we do two seemingly redundant searches:
						 * 
						 * (1) We search the class declaration of the type of
						 * the lock expression and its ancestors for fields that
						 * are Lock or ReadWriteLock.
						 * 
						 * (2) We look at all the declared locks in the class,
						 * and see which ones are Lock or ReadWriteLock.
						 * 
						 * The reason we do this twice is that (2) allows us to
						 * put the warnings under the headings of the particular
						 * locks that might actually be the correct ones to use.
						 * But (1) is more comprehensive.
						 */
						final List<IRNode> lockFields = getJUCLockFields(lockExpr);
						if (!lockFields.isEmpty()) {
							// (1)
							makeWarningDrop(
									Messages.DSC_MIXED_PARADIGM,
									lockExpr,
									lockFields.size() > 1 ? Messages.LockAnalysis_ds_JUCLockFields
											: Messages.LockAnalysis_ds_JUCLockFields,
									DebugUnparser.toString(lockExpr),
									fieldsToString(lockFields));

							/*
							 * Now (2) --- we already know from (1) there are
							 * JUC lock fields in the class.
							 */
							if (typeOfLockExpr instanceof IJavaDeclaredType) {
								final GlobalLockModel sysLockModel = sysLockModelHandle
										.get();
								for (final IRNode varDecl : lockFields) {
									for (final AbstractLockRecord lockRecord : sysLockModel
											.getRegionAndPolicyLocksForLockImpl(
													typeOfLockExpr, varDecl)) {
										final HintDrop warning = makeWarningDrop(
												Messages.DSC_MIXED_PARADIGM,
												lockExpr,
												Messages.LockAnalysis_ds_DeclaredJUCLockField,
												DebugUnparser
														.toString(lockExpr),
												VariableDeclarator
														.getId(lockRecord.lockImpl),
												lockRecord.name);
										lockRecord.lockDecl.addDependent(warning);
									}
								}
							}
						} else {
							makeWarningDrop(
									Messages.DSC_UNIDENTIFIABLE_LOCK_WARNING,
									lockExpr,
									Messages.LockAnalysis_ds_UnidentifiableLock,
									DebugUnparser.toString(lockExpr));
						}
						lockIsIdentifiable = false;
					}
				} else { // Non-final lock expression -> warning!
					// If the expression were final, would we be able to resolve
					// the lock?
					final Set<HeldLock> heldLocks = new HashSet<HeldLock>();
					lockUtils.convertIntrinsicLockExpr(lockExpr,
							heldLockFactory, enclosingMethod, syncBlock,
							heldLocks);

					final HintDrop warning = makeWarningDrop(
							Messages.DSC_NONFINAL_EXPRESSION_WARNING, lockExpr,
							Messages.LockAnalysis_ds_NonfinalExpression,
							DebugUnparser.toString(lockExpr));
					for (final HeldLock l : heldLocks) {
						l.getLockPromise().addDependent(warning);
					}
					lockIsIdentifiable = false;
				}
			}

			// continue the analysis
			doAcceptForChildren(syncBlock);

			// check to see if the synchronization was used for anything
			if (lockIsIdentifiable && !lockIsPolicyLock
					&& !syncFrame.isNeeded()) {
				final HintDrop info = makeWarningDrop(
						Messages.DSC_SYNCHRONIZED_UNUSED_WARNING, syncBlock,
						Messages.LockAnalysis_ds_SynchronizationUnused,
						syncFrame);
				for (final StackLock stackLock : syncFrame) {
					stackLock.lock.getLockPromise().addDependent(info);
				}
			}
		} finally {
			// remove the lock from the context
			ctxtTheHeldLocks.popFrame();
		}
		return null;
	}

	private static String fieldsToString(final List<IRNode> lockFields) {
		final StringBuilder sb = new StringBuilder();
		int count = 1;
		for (final Iterator<IRNode> i = lockFields.iterator(); i.hasNext();) {
			final IRNode vd = i.next();
			if (count == 2) {
				if (i.hasNext()) {
					sb.append(", ");
				} else {
					sb.append(" and ");
				}
			} else if (count > 2) {
				if (i.hasNext()) {
					sb.append(", ");
				} else {
					sb.append(", and ");
				}
			}
			sb.append(VariableDeclarator.getId(vd));
			count += 1;
		}
		return sb.toString();
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitTypeDeclarationStatement(final IRNode expr) {
		// Don't look inside classes/interfaces declared inside a method
		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitVariableDeclarator(final IRNode varDecl) {
		/*
		 * If this is inside a FieldDeclaration, then we only want to run if we
		 * are being executed on behalf of the InstanceInitHelper or if we are
		 * part of a static field declaration.
		 * 
		 * If this inside a DeclStatement, then we always want to run, and we
		 * don't do anything special at all. (I would like to avoid having to
		 * climb up the parse tree, but I don't have a choice because
		 * InstanceInitHelper does not call back into FieldDeclaration, but into
		 * the children of FieldDeclaration.)
		 * 
		 * The point of all this is we need to consider a field initialization
		 * as accessing the field.
		 */
		if (FieldDeclaration.prototype.includes(JJNode.tree
				.getOperator(JJNode.tree.getParentOrNull(JJNode.tree
						.getParentOrNull(varDecl))))) {
			/*
			 * Analyze the field initialization if we are inside a constructor,
			 * inside an anonymous class expression or visiting a static field.
			 */
			final boolean isStaticDeclaration = TypeUtil.isStatic(varDecl);
			if (ctxtInsideAnonClassExpr || ctxtInsideConstructor != null
					|| isStaticDeclaration) {
				// okay, at this point we know we are inside a field declaration
				// that is being analyzed on behalf of a constructor or a static
				// initializer.
				final IRNode init = VariableDeclarator.getInit(varDecl);
				// Don't worry about uninitialized fields
				if (!NoInitialization.prototype.includes(JJNode.tree
						.getOperator(init))) {
					/*
					 * If the initialization is static, we have to update the
					 * enclosing method to the class init declaration.
					 */
					if (isStaticDeclaration) {
						final IRNode classDecl = VisitUtil
								.getClosestType(varDecl);
						ctxtInsideMethod = JavaPromise
								.getClassInitOrNull(classDecl);
						ctxtBcaQuery = bindingContextAnalysis
								.getExpressionObjectsQuery(ctxtInsideMethod);
						updateJUCAnalysisQueries(ctxtInsideMethod);
						ctxtConflicter = new ConflictChecker(binder, mayAlias);
						ctxtClassInitializationLocks = convertStaticInitializerBlock(
								varDecl, ctxtJavaType);
					}
					// Don't worry about initialization of final
					// variables/fields
					// final LockStackFrame syncFrame =
					// ctxtTheHeldLocks.pushNewFrame();
					try {
						// if (isStaticDeclaration) {
						// convertStaticInitializerBlock(varDecl, ctxtJavaType,
						// syncFrame);
						// }
						// Only non-final fields need to be protected
						if (!TypeUtil.isFinal(varDecl)) {
							final IRegion fieldAsRegion = RegionModel
									.getInstance(varDecl);
							final Target target;
							if (fieldAsRegion.isStatic()) {
								target = lockUtils
										.createClassTarget(fieldAsRegion);
							} else {
								target = lockUtils.createInstanceTarget(
										ctxtTheReceiverNode, fieldAsRegion);
							}
							assureRegionRef(varDecl,
									lockUtils.getLocksForDirectRegionAccess(
											effects, ctxtBcaQuery, ctxtTheReceiverNode, varDecl,
											false, target));
						}
						// analyze the the RHS of the initialization
						doAcceptForChildren(varDecl);
					} finally {
						// ctxtTheHeldLocks.popFrame();
						if (isStaticDeclaration) {
							ctxtClassInitializationLocks = null;
							ctxtInsideMethod = null;
							ctxtBcaQuery = null;
							ctxtHeldLocksQuery = null;
							ctxtLocksForQuery = null;
							ctxtMustReleaseQuery = null;
							ctxtConflicter = null;
						}
					}
				}
			}
		} else {
			/*
			 * not a field declaration, so we are in a local variable
			 * declaration; analyze its contents.
			 */
			doAcceptForChildren(varDecl);
		}
		return null;
	}

	@Override
	public Void visitEnumConstantDeclaration(final IRNode constDecl) {
		/*
		 * Enumeration constant declarations are sort of like static field
		 * declarations, so we base this off of visitVariableDeclarator assuming
		 * a static field.
		 */
		final IRNode classDecl = VisitUtil.getClosestType(constDecl);
		ctxtInsideMethod = JavaPromise.getClassInitOrNull(classDecl);
		ctxtBcaQuery = bindingContextAnalysis
				.getExpressionObjectsQuery(ctxtInsideMethod);
		updateJUCAnalysisQueries(ctxtInsideMethod);
		ctxtConflicter = new ConflictChecker(binder, mayAlias);
		ctxtClassInitializationLocks = convertStaticInitializerBlock(constDecl,
				ctxtJavaType);

		try {
			/*
			 * Enumeration constant declarations are also like new expressions,
			 * so we assure the call.
			 */
			assureCall(constDecl);
			/* Assure the arguments (if any) */
			doAcceptForChildren(constDecl);
		} finally {
			ctxtClassInitializationLocks = null;
			ctxtInsideMethod = null;
			ctxtBcaQuery = null;
			ctxtHeldLocksQuery = null;
			ctxtLocksForQuery = null;
			ctxtMustReleaseQuery = null;
			ctxtConflicter = null;
		}
		return null;
	}

	@Override
	public Void visitEnumConstantClassDeclaration(final IRNode constDecl) {
		/*
		 * Enumeration constant declarations are sort of like static field
		 * declarations, so we (1) base this off of visitVariableDeclarator
		 * assuming a static field. (2) This is also like an anonymous class
		 * expression.
		 */

		/* (1) Set up context as if we are entering a static field declaration */
		final IRNode classDecl = VisitUtil.getEnclosingType(constDecl);
		ctxtInsideMethod = JavaPromise.getClassInitOrNull(classDecl);
		ctxtBcaQuery = bindingContextAnalysis
				.getExpressionObjectsQuery(ctxtInsideMethod);
		updateJUCAnalysisQueries(ctxtInsideMethod);
		ctxtConflicter = new ConflictChecker(binder, mayAlias);
		ctxtClassInitializationLocks = convertStaticInitializerBlock(constDecl,
				ctxtJavaType);

		try {
			// Assure the constructor call
			assureCall(constDecl);
			// Visit the arguments
			doAccept(EnumConstantClassDeclaration.getArgs(constDecl));

			/*
			 * (2) Now set up context as if we are entering an anonymous class
			 * expression that is being assigned to the field.
			 * 
			 * This is taken from vistAnonClassExpression(), except that we call
			 * processEnumConstantClassDeclaration().
			 */
			final IRNode oldTypeDecl = ctxtTypeDecl;
			final IJavaDeclaredType oldJavaType = ctxtJavaType;
			final IRNode oldInsideMethod = ctxtInsideMethod;
			final BindingContextAnalysis.Query oldBcaQuery = ctxtBcaQuery;
			final JavaFlowAnalysisQuery<HeldLocks> oldHeldLocksQuery = ctxtHeldLocksQuery;
			final MustHoldAnalysis.LocksForQuery oldLocksForQuery = ctxtLocksForQuery;
			final MustReleaseAnalysis.Query oldMRQ = ctxtMustReleaseQuery;
			final ConflictChecker oldConflicter = ctxtConflicter;
			final boolean oldOnBehalfOfConstructor = ctxtOnBehalfOfConstructor;
			final IRNode oldInsideConstructor = ctxtInsideConstructor;
			final String oldConstructorName = ctxtConstructorName;
			final IRNode oldTheReceiverNode = ctxtTheReceiverNode;
			final MethodCallUtils.EnclosingRefs oldEnclosingRefs = ctxtEnclosingRefs;
			final boolean oldCtxtInsideAnonClassExpr = ctxtInsideAnonClassExpr;
			InstanceInitializationVisitor.processEnumConstantClassDeclaration(
					constDecl, this, new InstanceInitAction() {
						public void tryBefore() {
							ctxtInsideAnonClassExpr = true;
							// Create the substitution map
							ctxtEnclosingRefs = MethodCallUtils
									.getEnclosingInstanceReferences(binder,
											thisExprBinder, constDecl,
											oldTypeDecl, oldTheReceiverNode,
											getEnclosingMethod());

							/*
							 * Update the type being analyzed to be the
							 * anonymous class expression
							 */
							ctxtTypeDecl = constDecl;
							ctxtJavaType = (IJavaDeclaredType) JavaTypeFactory
									.getMyThisType(ctxtTypeDecl);

							/*
							 * The information needed for checking returned
							 * locks can be preserved, it is not needed by the
							 * recursive visitation because we will only visit
							 * field initializers and instance initializers.
							 */
							/* left hand side is irrelevant here. */
							/*
							 * The recursive visit is analyzing as the implicit
							 * initialization method for the class. We set the
							 * receiver accordingly.
							 */
							ctxtInsideMethod = JavaPromise
									.getInitMethodOrNull(constDecl);

							/*
							 * MUST update receiver node before creating the
							 * flow analyses because they indirectly use this
							 * field via the this expression binder object used
							 * by the lock factories. They have access to this
							 * factories via the lock utils object and the juc
							 * lock usage manager.
							 */
							ctxtTheReceiverNode = JavaPromise
									.getReceiverNodeOrNull(ctxtInsideMethod);

							ctxtBcaQuery = ctxtBcaQuery
									.getSubAnalysisQuery(constDecl);
							ctxtHeldLocksQuery = ctxtHeldLocksQuery
									.getSubAnalysisQuery(constDecl);
							ctxtLocksForQuery = ctxtLocksForQuery
									.getSubAnalysisQuery(constDecl);
							ctxtMustReleaseQuery = ctxtMustReleaseQuery
									.getSubAnalysisQuery(constDecl);

							ctxtConflicter = new ConflictChecker(binder,
									mayAlias);
							ctxtOnBehalfOfConstructor = false;
							ctxtInsideConstructor = null;
							ctxtConstructorName = null;
						}

						public void finallyAfter() {
							// restore the global state
							ctxtInsideAnonClassExpr = oldCtxtInsideAnonClassExpr;
							ctxtEnclosingRefs = oldEnclosingRefs;
							ctxtTypeDecl = oldTypeDecl;
							ctxtJavaType = oldJavaType;
							ctxtInsideMethod = oldInsideMethod;
							ctxtBcaQuery = oldBcaQuery;
							ctxtHeldLocksQuery = oldHeldLocksQuery;
							ctxtLocksForQuery = oldLocksForQuery;
							ctxtMustReleaseQuery = oldMRQ;
							ctxtConflicter = oldConflicter;
							ctxtOnBehalfOfConstructor = oldOnBehalfOfConstructor;
							ctxtInsideConstructor = oldInsideConstructor;
							ctxtConstructorName = oldConstructorName;
							ctxtTheReceiverNode = oldTheReceiverNode;
						}

						public void afterVisit() {
							// nothing
						}
					});

		} finally {
			// End of static field declaration
			ctxtClassInitializationLocks = null;
			ctxtInsideMethod = null;
			ctxtBcaQuery = null;
			ctxtHeldLocksQuery = null;
			ctxtLocksForQuery = null;
			ctxtMustReleaseQuery = null;
			ctxtConflicter = null;
		}

		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	public Void visitVariableUseExpression(final IRNode expr) {
		/*
		 * We don't care about uses of local variables, but we need to reset the
		 * LHS flag.
		 */
		this.ctxtIsLHS = false;
		return null;
	}
}
