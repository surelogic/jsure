/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/AnnotationRules.java,v 1.48 2008/10/29 14:17:16 chance Exp $*/
package com.surelogic.annotation.rules;

import com.surelogic.common.util.*;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.AASTStatus;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.java.DeclarationNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberOrder;
import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.XUtil;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.ModelingProblemDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
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
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
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

	public static final String XML_LOG_NAME = "AnnotationRules";
	public static final String XML_LOG_PROP = XML_LOG_NAME + ".label";

	public static final ITestOutput XML_LOG = !IDE.hasInstance() ? null : IDE
			.getInstance().makeLog(
					System.getProperty(XML_LOG_PROP, XML_LOG_NAME));

	/* *************************************************
	 * Constants ************************************************
	 */

	protected static final SyntaxTreeInterface tree = JJNode.tree;

	/* *************************************************
	 * Utility code ************************************************
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

	private static final String[] noArgMethods = new String[] { "toString",
			"hashCode", "annotationType", };

	private static final ConcurrentMap<String, Map<String, Attribute>> attrsByPromise = new ConcurrentHashMap<String, Map<String, Attribute>>();

	public static final class Attribute implements Comparable<Attribute> {
		private final String f_name;
		private final Class<?> f_type;
		/**
		 * May be null.
		 */
		private final String f_defaultValue;

		public String getName() {
			return f_name;
		}

		public Class<?> getType() {
			return f_type;
		}
		
		public boolean isTypeString() {
			return String.class.equals(f_type);
		}

		public String getDefaultValueOrNull() {
			return f_defaultValue;
		}

		private Attribute(String name, Class<?> type, String defaultValue) {
			if (name == null)
				throw new IllegalArgumentException(I18N.err(44, "name"));
			if (type == null)
				throw new IllegalArgumentException(I18N.err(44, "type"));

			f_name = name;
			f_type = type;
			f_defaultValue = defaultValue;
		}

		@Override
    public int compareTo(Attribute o) {
			return f_name.compareTo(o.f_name);
		}

		@Override
		public String toString() {
			return "[Attribute " + f_name + " : " + f_type.getName() + " def "
					+ f_defaultValue + "]";
		}
	}

	/**
	 * @return a map of attributes and default values
	 */
	public static Map<String, Attribute> getAttributes(String tag) {
		Map<String, Attribute> m = attrsByPromise.get(tag);
		if (m == null) {
			// This is idempotent, so it doesn't really matter which gets used
			m = computeAttributes(tag);
			attrsByPromise.putIfAbsent(tag, m);
		}
		return m;
	}

	private static Map<String, Attribute> computeAttributes(String tag) {
		final String qname = AnnotationConstants.PROMISE_PREFIX + tag;
		try {
			Class<?> cls = Class.forName(qname);
			Map<String, Attribute> l = new HashMap<String, Attribute>();

			outer: for (Method m : cls.getMethods()) {
				if (m.getParameterTypes().length > 0) {
					// Not an attribute
					continue outer;
				}
				for (String name : noArgMethods) {
					if (name.equals(m.getName())) {
						continue outer;
					}
				}
				/*
				 * if (AnnotationConstants.VALUE_ATTR.equals(m.getName())) {
				 * continue outer; }
				 */
				// System.out.println("Attribute '"+m.getName()+"' can appear on "+tag);
				Object value = m.getDefaultValue();
				Class<?> type = m.getReturnType();
				if (m.getReturnType().equals(String[].class)) {
					String[] values = (String[]) value;
					type = String.class;
					value = flattenStringArray(values);
				}
				l.put(m.getName(),
						new Attribute(m.getName(), type,
								value == null ? null : value.toString()));
			}
			return Collections.unmodifiableMap(l);
		} catch (ClassNotFoundException e) {
			// Ignore it
		}
		return Collections.emptyMap();
	}

	public static String flattenStringArray(String[] values) {
		if (values == null || values.length == 0) {
			return "";
		} else {
			StringBuilder b = new StringBuilder();
			for(String v : values) {
				if (b.length() != 0) {
					b.append(", ");
				}
				b.append(v);
				}
			return b.toString();
		}
	}
	
	/* *************************************************
	 * Initialization code ************************************************
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
	protected void registerParseRuleStorage(PromiseFramework fw,
			IAnnotationParseRule<?,?> r) {
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

	/* *************************************************
	 * Scrubber code ************************************************
	 */

	private static final TaskManager mgr = makeManager();
	private static final TaskManager firstMgr = makeManager();
	private static final TaskManager lastMgr = makeManager();

	private static TaskManager makeManager() {
		return new TaskManager(1, 1,//ConcurrentAnalysis.threadCount, ConcurrentAnalysis.threadCount, 
				60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
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

	public static boolean ignoreNode(IAASTNode n) {
		if (XUtil.useExperimental) {
			return false;
		}
		final IAASTRootNode root = n.getRoot();
		return root.isAutoGenerated();
	}
	
	private IAnnotationScrubberContext context = new IAnnotationScrubberContext() {		
		@Override
    public void reportError(IAASTNode n, int number, Object... args) {			
			if (ignoreNode(n)) {
				return;
			}
			makeProblemDrop(n.getPromisedFor(),	n.getOffset()).setMessage(number, args);
		}
		
		@Override
    public void reportError(final IAASTNode n, final String msgTemplate,
				final Object... args) {
			reportError(MessageFormat.format(msgTemplate, args), n);
		}

		@Override
    public void reportError(String msg, IAASTNode n) {
			if (ignoreNode(n)) {
				return;
			}
			if (!msg.contains(" on ")) {
				//IRNode here = n.getPromisedFor();
				msg = msg + " on " + n;//JavaNames.getFullName(here);
			}
			makeProblemDrop(n.getPromisedFor(),	n.getOffset()).setMessage(msg);
		}

		@Override
    public void reportError(IRNode n, String msgTemplate, Object... args) {
			reportError_private(n, msgTemplate, args);
		}

		@Override
    public void reportError(IRNode n, int number, Object... args) {
			makeProblemDrop(n, UNKNOWN).setMessage(number, args);
		}
		
		@Override
    public void reportErrorAndProposal(ProposedPromiseDrop p, int number, Object... args) {
			ModelingProblemDrop d = makeProblemDrop(p.getNode(), UNKNOWN);
			d.setMessage(number, args);
			d.addProposal(p);
		}
		
		@Override
    public void reportErrorAndProposal(ProposedPromiseDrop p,
				String msgTemplate, Object... args) {
			ModelingProblemDrop d = reportError_private(p.getNode(),
					msgTemplate, args);
			d.addProposal(p);
		}

		private ModelingProblemDrop reportError_private(IRNode n,
				String msgTemplate, Object... args) {
			String txt = MessageFormat.format(msgTemplate, args) + " on "
					+ DebugUnparser.toString(n);
			ModelingProblemDrop d = makeProblemDrop(n, UNKNOWN);
			d.setMessage(txt);
			return d;
		}

		private ModelingProblemDrop makeProblemDrop(IRNode node, int offset) {			
			return new ModelingProblemDrop(node, offset);
		}

		@Override
    public void reportWarning(IAASTNode n, int number, Object... args) {
			reportError(n, number, args);
		}
		
		@Override
    public void reportWarning(IAASTNode n, String msgTemplate,
				Object... args) {
			reportWarning(MessageFormat.format(msgTemplate, args), n);
		}

		@Override
    public void reportWarning(String msg, IAASTNode n) {
			reportError(msg, n);
		}

		@Override
    public IBinder getBinder(IRNode context) {
			final IIRProject p = JavaProjects.getEnclosingProject(context);
			return p.getTypeEnv().getBinder();
			//return IDE.getInstance().getTypeEnv().getBinder();
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
				LOG.warning("Didn't process " + a + " on "
						+ JavaNames.getFullName(a.getPromisedFor()));
			} else {
				it.remove();
			}
		}
		AASTStore.clearASTs();
	}

	/* *************************************************
	 * Accessors for IPromiseDropStorage
	 * ************************************************
	 */

	private static boolean isBogus(PromiseDrop<?> p) {
		return !p.isValid();
	}

	/**
	 * Store the drop on the IRNode only if it is not null
	 * 
	 * @return The drop passed in
	 */
	public static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> P storeDropIfNotNull(
			IPromiseDropStorage<P> stor, A a, P pd) {
		if (pd == null) {
			return null;
		}
		final IRNode n = a.getPromisedFor();
		final IRNode mapped = PromiseFramework.getInstance().mapToProxyNode(n);
		/*
		 * if (mapped != n) {
		 * System.out.println(pd.getMessage()+" created on "+DebugUnparser
		 * .toString(n)); }
		 */
		return stor.add(mapped, pd);
	}

	private static <P extends PromiseDrop<?>> P getMappedValue(SlotInfo<P> si,
			final IRNode n) {
		final PromiseFramework frame = PromiseFramework.getInstance();
		final IRNode mapped = frame.getProxyNode(n);
		/*
		 * if (mapped != n) {
		 * System.out.println("Using "+mapped+", instead of "+
		 * DebugUnparser.toString(n)); }
		 */
		return getMappedValue(si, n, mapped, frame);
	}

	private static <P extends PromiseDrop<?>> P getMappedValue(SlotInfo<P> si,
			final IRNode n, final IRNode mapped, PromiseFramework frame) {
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
	 * Add the AST to the PromiseDrop associated with the promisedFor node,
	 * creating a drop if there isn't already one
	 * 
	 * @return The drop that the AST was added to
	 */
//	protected static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> P ensureDropForAST(
//			IPromiseDropStorage<P> stor, IDropFactory<P, ? super A> factory, A a) {
//		final PromiseFramework frame = PromiseFramework.getInstance();
//		final IRNode n = a.getPromisedFor();
//		final IRNode mapped = frame.mapToProxyNode(n);
//		final SlotInfo<P> si = stor.getSlotInfo();
//
//		P pd = getMappedValue(si, n, mapped, frame);
//		if (pd == null) {
//			pd = factory.createDrop(n, a);
//			return pd == null ? null : stor.add(mapped, pd);
//		} else {
//			pd.setAAST(a);			
//		}
//		return pd;
//	}

	/**
	 * Remove from associated IRNode
	 */
	protected static <A extends IAASTRootNode, P extends PromiseDrop<? super A>> void removeDrop(
			IPromiseDropStorage<P> stor, P pd) {
		final IRNode n = pd.getAAST().getPromisedFor();
		final IRNode mapped = PromiseFramework.getInstance().getProxyNode(n);
		stor.remove(mapped, pd);
		// pd.invalidate();
	}

	/**
	 * Getter for BooleanPromiseDrops
	 */
	protected static <D extends BooleanPromiseDrop<?>> D getBooleanDrop(
			IPromiseDropStorage<D> s, IRNode n) {
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
	protected static <D extends PromiseDrop<?>> D getDrop(
			IPromiseDropStorage<D> s, IRNode n) {
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

	private static <D extends PromiseDrop<?>> Iterator<D> getIterator(
			SlotInfo<List<D>> si, IRNode n) {
		if (n == null) {
			return new EmptyIterator<D>();
		}
		List<D> s = n.getSlotValue(si);
		if (s != null) {
			return s.iterator();
		}
		return new EmptyIterator<D>();
	}

	/**
	 * Getter for lists of PromiseDrops
	 */
	protected static <D extends PromiseDrop<?>> Iterable<D> getDrops(
			IPromiseDropStorage<D> s, IRNode n) {
		if (n == null) {
			return new EmptyIterator<D>();
		}
		IPromiseDropSeqStorage<D> storage = (IPromiseDropSeqStorage<D>) s;

		final PromiseFramework frame = PromiseFramework.getInstance();
		final IRNode mapped = frame.getProxyNode(n);
		final SlotInfo<List<D>> si = storage.getSeqSlotInfo();

		// Need to merge values if both available
		Iterator<D> e = new EmptyIterator<D>();

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
			return new EmptyIterator<D>();
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
			argPosition = new HashMap<IRNode, Integer>();
			parentArgs = new ArrayList<IRNode>();
			childArgs = new ArrayList<IRNode>();

			final Iteratable<IRNode> parentParams = Parameters
					.getFormalIterator(MethodDeclaration.getParams(parent));
			final Iteratable<IRNode> childParams = Parameters
					.getFormalIterator(MethodDeclaration.getParams(child));
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
		private IRNode getParallelArgument(final IRNode arg,
				final List<IRNode> otherArgs) {
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
	protected static Map<IRNode, Integer> buildParameterMap(
			final IRNode annotatedMethod, final IRNode parent) {
		// Should have the same number of arguments
		final Iteratable<IRNode> p1 = Parameters
				.getFormalIterator(MethodDeclaration.getParams(annotatedMethod));
		final Iteratable<IRNode> p2 = Parameters
				.getFormalIterator(MethodDeclaration.getParams(parent));
		int count = 0;
		final Map<IRNode, Integer> positionMap = new HashMap<IRNode, Integer>();
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
		int i=0;
		for(String s : values) {
			// FIX can we get the exact offset?
			rv[i] = new NamedTypeNode(offset, s.trim());
			i++;
		}
		return rv;
	}
}
