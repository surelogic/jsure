package edu.cmu.cs.fluid.analysis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.annotation.parse.SLAnnotationsLexer;
import com.surelogic.annotation.parse.SLColorAnnotationsLexer;
import com.surelogic.annotation.parse.ScopedPromisesLexer;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

import edu.cmu.cs.fluid.dc.Majordomo;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.EclipseCodeFile;
import edu.cmu.cs.fluid.eclipse.EclipseFileLocator;
import edu.cmu.cs.fluid.eclipse.ISrcAdapterNotify;
import edu.cmu.cs.fluid.eclipse.adapter.AbstractJavaAdapter;
import edu.cmu.cs.fluid.eclipse.adapter.Binding;
import edu.cmu.cs.fluid.eclipse.adapter.CompUnitPattern;
import edu.cmu.cs.fluid.eclipse.adapter.ModuleUtil;
import edu.cmu.cs.fluid.eclipse.adapter.TypeBindings;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.AbstractIRNode;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.ICodeFile;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.analysis.AnalysisContext;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.ModulePromises;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.drops.BinaryCUDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.SimpleCallGraphDrop;
import edu.cmu.cs.fluid.tree.SyntaxTree;
import edu.cmu.cs.fluid.util.AbstractRunner;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.IteratorUtil;
import edu.cmu.cs.fluid.util.SimpleRemovelessIterator;
import edu.cmu.cs.fluid.version.Version;

/**
 * Analysis module to maintain Eclipse Java projects within the Fluid IR.
 */
public final class ConvertToIR extends AbstractFluidAnalysisModule<Void> {
	private static ConvertToIR INSTANCE;

	private static final Logger LOG = SLLogger
			.getLogger("analysis.util.ConvertToIR");

	/**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
	public static ConvertToIR getInstance() {
		return INSTANCE;
	}

	private static void setInstance(ConvertToIR me) {
		INSTANCE = me;
	}

	/**
	 * Public constructor that will be called by Eclipse when this analysis
	 * module is created.
	 */
	public ConvertToIR() {
		setInstance(this);
		prefetch("java.lang.Object");
		// Needed because the scrubber calls
		// AbstractLockDeclarationNode.isReadWriteLock
		prefetch("java.util.concurrent.locks.ReadWriteLock");
		for (IJavaPrimitiveType pt : JavaTypeFactory.primTypes) {
			prefetch(pt.getCorrespondingTypeName());
		}

		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				new ClearProjectListener(), IResourceChangeEvent.PRE_CLOSE);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				new ClearProjectListener(), IResourceChangeEvent.PRE_DELETE);
		if (ClearProjectListener.clearAfterChange) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(
					new ClearProjectListener(), IResourceChangeEvent.POST_CHANGE);
		}
		Eclipse.initialize();
		Eclipse.register(new ITypeBindingHandler());
	}

	static public final class Contents {

		// Holds information about the current Java source file being examined
		public ICompilationUnit javaFile;

		String javaOSFileName;

		String javaFileSource;

		public AnalysisContext analysisContext;

		public IRNode cu;

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return javaOSFileName + " successfully converted to IR";
		}
	}

	@SuppressWarnings("unused")
	private String javaFileSource;

	private String javaOSFileName;

	private IRNode cu;

	private long used;

	private int nodes;

	private int compUnits;

	private boolean jloChanged = false;

	private static final String MODULE_PREFIX = "Module.";
	private static final String PROJECT_KEY = "Project";

	/**
	 * 
	 * Did java.lang.Object change?
	 */
	public boolean didJavaLangObjectChange() {
		return jloChanged;
	}

	@Override
	public void preBuild(IProject p) {
		super.preBuild(p);
		jloChanged = false;

		SlotInfo.gc();
		/*
		 * final Runtime rt = Runtime.getRuntime(); rt.maxMemory();
		 * rt.totalMemory(); rt.freeMemory();
		 */

		// setup node for .project
		IResource f = p.getFile(".project");
		Eclipse.getDefault().confirmResourceNode(f, ".project");

		ModulePromises.clearSettings();
		Binding.clearAsSourcePatterns();
		Binding.clearAsNeededPatterns();

		IResource fp = p.getFile(JSURE_PROPERTIES);
		Eclipse.getDefault().confirmResourceNode(fp, JSURE_PROPERTIES);
		Properties props = IDE.getInstance().getProperties();

		// If no properties, try to read them in
		if (props == null || props.get(PROJECT_KEY) != p) {
			if (fp.exists()) {
				String location = fp.getLocation().toOSString();
				props = new Properties();
				props.put(PROJECT_KEY, p);

				try {
					InputStream is = new FileInputStream(new File(location));
					props.load(is);
					is.close();
				} catch (IOException e) {
					String msg = "Problem while loading "+JSURE_PROPERTIES+": "
							+ e.getMessage();
					reportProblem(msg, null);
					LOG.log(Level.SEVERE, msg, e);
				} finally {
					// Nothing to do
				}
				Eclipse.getDefault().setProperties(props);
			} else {
				props = Eclipse.getDefault().setProperties(null);
			}
		}
		if (props.size() > 0) {
			// process
			for (String defaults : getValues(props, IDE.MODULE_DEFAULTS)) {
				if (defaults.equals(IDE.AS_CLASS)) {
					ModulePromises.defaultAsSource(false);
				} else if (defaults.equals(IDE.AS_NEEDED)) {
					ModulePromises.defaultAsSource(false);
					ModulePromises.defaultAsNeeded(true);
				} else if (defaults.equals(IDE.AS_SOURCE)) {
					ModulePromises.defaultAsSource(true);
				} else {
					LOG.severe("Unknown value for " + IDE.MODULE_DEFAULTS
							+ ": " + defaults);
				}
			}
			for (String modulePattern : getValues(props, IDE.MODULE_REQUIRED)) {
				ModulePromises.setAsNeeded(modulePattern, false);
			}
			for (String modulePattern : getValues(props, IDE.MODULE_AS_NEEDED)) {
				ModulePromises.setAsNeeded(modulePattern, true);
				ModulePromises.setAsSource(modulePattern, false);
			}
			for (String modulePattern : getValues(props, IDE.MODULE_AS_CLASS)) {
				ModulePromises.setAsSource(modulePattern, false);
			}
			for (String modulePattern : getValues(props, IDE.MODULE_AS_SOURCE)) {
				ModulePromises.setAsNeeded(modulePattern, false);
				ModulePromises.setAsSource(modulePattern, true);
			}
			for (String moduleKey : getModules(props)) {
				String pattern = props.getProperty(moduleKey, null);
				if (pattern != null) {
					createModuleFromKeyAndPattern(IDE.MODULE_DECL_PREFIX,
							moduleKey, pattern);
				}
			}
			UpdateSuperRootStorage.clearLibraryPath();
			for (String excludePath : getValues(props, IDE.LIB_EXCLUDES)) {
				UpdateSuperRootStorage.excludeLibraryPath(excludePath);
			}
		}
	}

	/**
	 * 
	 * @param props
	 *            The set of properties to search for module definitions
	 * @return The keys for all the modules found
	 */
	private static Iteratable<String> getModules(Properties props) {
		if (props.isEmpty()) {
			return EmptyIterator.prototype();
		}
		final Set<Object> keys = props.keySet();
		return new FilterIterator<Object, String>(keys.iterator()) {
			@Override
			protected Object select(Object o) {
				if (o instanceof String) {
					String key = (String) o;
					if (key.startsWith(IDE.MODULE_DECL_PREFIX)) {
						return key;
					}
				}
				return IteratorUtil.noElement;
			}
		};
	}

	/**
	 * Gets the comma-separated values for the given key
	 */
	private static Iteratable<String> getValues(Properties props, String key) {
		String prop = props.getProperty(key, "");
		if (prop.equals("")) {
			return EmptyIterator.prototype();
		}
		final StringTokenizer st = new StringTokenizer(prop, ",");
		if (!st.hasMoreTokens()) {
			return EmptyIterator.prototype();
		}
		return new SimpleRemovelessIterator<String>() {
			@Override
			protected Object computeNext() {
				if (st.hasMoreTokens()) {
					return st.nextToken().trim();
				}
				return IteratorUtil.noElement;
			}
		};
	}

	@Override
	public void postBuild(IProject project) {
		final Logger log = SLLogger.getLogger();
		if (JJNode.tree instanceof SyntaxTree) {
			if (log.isLoggable(Level.FINE)) {
				SyntaxTree t = (SyntaxTree) JJNode.tree;
				StringWriter out = new StringWriter();
				t.printCounts(new PrintWriter(out), t.getTotal() / 100);
				final String msg = String.format("SyntaxTree Counts:%n%s  ",
						out.toString());
				log.fine(msg);
			}
		} else {
			if (LOG.isLoggable(Level.FINE))
				log.fine("JavaNode.tree = " + JJNode.tree);
		}
		printUsage();

		final Runtime rt = Runtime.getRuntime();
		final long maxMem = rt.maxMemory();
		if (rt.totalMemory() >= maxMem && rt.freeMemory() < 0.05 * maxMem) {
			IDE.getInstance().popupWarning("Low on memory");
		}
	}

	private void printUsage() {
		printUsage("ConvertToIR");
	}

	private void printUsage(String label) {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine(label + " #Modifiers = "
					+ AbstractJavaAdapter.getModsCount());
			LOG.fine(label + " #Nodes = " + AbstractIRNode.getTotalNodesCreated());
			LOG.fine(label + " #Slots = " + SlotInfo.totalSize());
			// AssocList.printTotal();
			// NodeFactories.printUsage();

			int empty = 0, one = 0, more = 0;
			for (SimpleCallGraphDrop d : SimpleCallGraphDrop.getAllCGDrops()) {
				switch (d.getCallees().size()) {
				case 0:
					empty++;
					break;
				case 1:
					one++;
					break;
				default:
					more++;
				}
				switch (d.getCallers().size()) {
				case 0:
					empty++;
					break;
				case 1:
					one++;
					break;
				default:
					more++;
				}
			}
			LOG
					.fine("empty = " + empty + ", one = " + one + ", more = "
							+ more);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setArguments(Map args) {
		// Setup patterns in ???
		if (args == null) {
			// System.out.println("No arguments to set asSource");
			return;
		}
		String kind = (String) args.get(Majordomo.BUILD_KIND);
		if (kind != null) {
			final int k = Integer.parseInt(kind);
			if (k == IncrementalProjectBuilder.CLEAN_BUILD
					|| k == IncrementalProjectBuilder.FULL_BUILD) {
				ClearProjectListener.clearJSureState();
				IDE.getInstance().getProperties().put(Majordomo.BUILD_KIND, kind);
			} else {
				IDE.getInstance().getProperties().remove(Majordomo.BUILD_KIND);
			}
		}
		ProjectDrop.ensureDrop(getProject().getName(), 
				               Eclipse.getDefault().makeClassPath(getProject()));

		final Iterator<Map.Entry<String, String>> it = args.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> e = it.next();
			String key = e.getKey();
			String pattern = e.getValue();
			if (key.equals(Majordomo.BUILD_KIND)) {
				continue; // Nothing to do here
			} else if (key.startsWith("ConvertToIR.asClass")) {
				// Binding.setAsSource(CompUnitPattern.create(project, pattern),
				// false);
				for (Iterator<CompUnitPattern> p = parsePatterns(getProject(),
						pattern); p.hasNext();) {
					CompUnitPattern pat = p.next();
					LOG.info("Adding pattern to convert as .class: '" + pat
							+ "'");
					Binding.setAsSource(pat, false);
				}
			} else if (key.startsWith("ConvertToIR.asSource")) {
				// Binding.setAsSource(CompUnitPattern.create(project, pattern),
				// true);
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// false);
				for (Iterator<CompUnitPattern> p = parsePatterns(getProject(),
						pattern); p.hasNext();) {
					CompUnitPattern pat = p.next();
					LOG.info("Adding pattern to convert as source: '" + pat
							+ "'");
					Binding.setAsSource(pat, true);
					Binding.setAsNeeded(pat, false);
				}
			} else if (key.equals("ConvertToIR.defaultAsSource")) {
				ModulePromises.defaultAsSource(true);
			} else if (key.equals("ConvertToIR.defaultAsClass")) {
				ModulePromises.defaultAsSource(false);
			} else if (key.equals("ConvertToIR.defaultAsNeeded")) {
				ModulePromises.defaultAsSource(false);
				ModulePromises.defaultAsNeeded(true);
			} else if (key.startsWith("ConvertToIR.asNeeded")) {
				// Binding.setAsSource(CompUnitPattern.create(project, pattern),
				// false);
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// true);
				for (Iterator<CompUnitPattern> p = parsePatterns(getProject(),
						pattern); p.hasNext();) {
					CompUnitPattern pat = p.next();
					Binding.setAsSource(pat, false);
					Binding.setAsNeeded(pat, true);
				}
			} else if (key.startsWith("ConvertToIR.required")) {
				// Binding.setAsNeeded(CompUnitPattern.create(project, pattern),
				// false);
				for (Iterator<CompUnitPattern> p = parsePatterns(getProject(),
						pattern); p.hasNext();) {
					Binding.setAsNeeded(p.next(), false);
				}
			} else if (key.startsWith(MODULE_PREFIX)) {
				createModuleFromKeyAndPattern(MODULE_PREFIX, key, pattern);
			} else {
				String warn = "Got an unrecognized key in .project: " + key;
				reportWarning(warn, Eclipse.getDefault().getResourceNode(
						".project"));
				LOG.warning(warn);
				continue;
			}
		}
	}

	private void createModuleFromKeyAndPattern(String prefix, String key,
			String pattern) {
		Binding.createModule(getProject(), key.substring(prefix.length()),
				pattern);
	}

	private Iterator<CompUnitPattern> parsePatterns(final IProject proj,
			String patterns) {
		final StringTokenizer st = new StringTokenizer(patterns, ",");
		return new SimpleRemovelessIterator<CompUnitPattern>() {
			@Override
			protected Object computeNext() {
				if (st.hasMoreElements()) {
					String pat = st.nextToken().trim();
					return CompUnitPattern.create(proj, pat);
				}
				return IteratorUtil.noElement;
			}
		};
	}

	@Override
	public void analyzeBegin(final IProject p) {
		super.analyzeBegin(p);
		IDE.getInstance().setAdapting();
		used = edu.cmu.cs.fluid.dc.Plugin.memoryUsed();
		nodes = AbstractIRNode.getTotalNodesCreated();
		compUnits = 0;
		justLoaded.clear();

		ScopedPromisesLexer.init();
		SLAnnotationsLexer.init();
		SLColorAnnotationsLexer.init();
	
		if (IJavaFileLocator.useIRPaging &&
			IDE.getInstance().getProperties().containsKey(Majordomo.BUILD_KIND)) {
			try {
				List<CodeInfo> infos = 
					IDE.getInstance().getJavaFileLocator().loadArchiveIndex();
				for(CodeInfo info : infos) {
					if (info.getType() != IJavaFileLocator.Type.SOURCE) {
						javaFileLoaded(info);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		IDE.runVersioned(new AbstractRunner() {
			public void run() {
				// pre-load Object
				Eclipse.getDefault().getTypeEnv(p).findNamedType(
						"java.lang.Object");
			}
		});
	}

	/*
	 * static final IRNode jlo; static String jloUnparsed = null;
	 */

	@Override
	public boolean analyzeResource(IResource resource, int kind) {
		// FIX how to distinguish from one for a package?
		if (AbstractFluidAnalysisModule.isPromisesXML(resource)
				&& UpdateSuperRootStorage.onLibPath(resource)
				&& !isOnOutputPath(resource)) {
			// (re-)load corresponding class file
			String qname = getCorrespondingTypeName(resource);
			queueForLaterProcessing(qname);
			Eclipse.getDefault().confirmResourceNode(resource, qname);

			if (isRemoved(kind)) {
				throw new UnsupportedOperationException();
			}
		} else if (AbstractFluidAnalysisModule.isPackageJava(resource)) {
			return true;
		} else if (AbstractFluidAnalysisModule.isDotProject(resource)) {
			Eclipse.getDefault().confirmResourceNode(resource,
					resource.getFullPath().toPortableString());

			if (isRemoved(kind)) {
				throw new UnsupportedOperationException();
			}
		}
		return super.analyzeResource(resource, kind);
	}

	@Override
	protected void removeResource(IResource resource) {
		super.removeResource(resource);

		final EclipseFileLocator jfl = Eclipse.getDefault()
				.getJavaFileLocator();
		// How to invalidate the old info?
		String handle = resourceHandles.get(resource.getLocation()
				.toPortableString());
		if (handle != null) {
			Binding.clearOldCU(handle);
			jfl.unregister(handle);
		}
	}

	private final Map<String, String> resourceHandles = new HashMap<String, String>();

	private void updateResourceHandle(ICompilationUnit cu) {
		try {
			resourceHandles.put(cu.getCorrespondingResource().getLocation()
					.toPortableString(), cu.getHandleIdentifier());
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return true if already loaded, or if not loaded on demand
	 */
	private boolean toBeLoaded(final ICompilationUnit file) {
		if (Eclipse.getDefault().getJavaFileLocator().isLoaded(
				file.getHandleIdentifier())
				|| !ModuleUtil.loadedAsNeeded(file)) {
			ICodeFile cf = new EclipseCodeFile(file);
			return !justLoaded.contains(cf);
		}
		return false;
	}

	private static final int BATCH_SIZE = 100;
	private static final boolean batch = (BATCH_SIZE > 1);
	private final List<ICompilationUnit> batchQ = new ArrayList<ICompilationUnit>(
			BATCH_SIZE);
	private final Set<ICodeFile> justLoaded = new HashSet<ICodeFile>();

	// private ICompilationUnit[] temp = new ICompilationUnit[BATCH_SIZE];

	/**
	 * @see edu.cmu.cs.fluid.javaassure.IAnalysis#analyzeCompilationUnit(org.eclipse.jdt.core.ICompilationUnit,
	 *      org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public boolean analyzeCompilationUnit(final ICompilationUnit file,
			CompilationUnit ast, 
            final IAnalysisMonitor monitor) {

		final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
		final boolean load = toBeLoaded(file);

		updateResourceHandle(file);

		if (load) {
			if (fineIsLoggable) {
				LOG.fine("adapting " + file.getHandleIdentifier()
						+ " into the JSure IR");
			}
			invalidateOldDrops(file);
		} else {
			if (fineIsLoggable)
				LOG.fine("Ignoring for now: " + file.getHandleIdentifier());
		}

		// Do the conversion to IR, and create a drop to hold the results.
		try {
			javaFileSource = file.getSource();
		} catch (JavaModelException e) {
			LOG.log(Level.SEVERE, "couldn't get source code for "
					+ file.getElementName(), e);
		}
		if (javaFileSource.length() == 0) {
			return false;
		}
		
		javaFile = file;
		javaOSFileName = javaFile.getResource().getLocation().makeAbsolute()
				.toOSString();

		Object rv = IDE.runVersioned(new AbstractRunner() {
			public void run() {
				result = null;
				if (load) {
					// Already loaded, but with private members omitted, yet it
					// was changed?
					final boolean loaded = Eclipse.getDefault()
							.getJavaFileLocator().isLoaded(
									file.getHandleIdentifier());
					final boolean source = ModuleUtil.treatedAsSource(file);
					final boolean warn = loaded && !source;
					//System.out.println("Adapting: "+javaOSFileName);
					if (batch && !warn) {
						cu = null;
						batchQ.add(javaFile);
						batchIfReady(monitor);												
					} else {
						cu = Eclipse.adaptIR(getJavaProject(), javaFile);
						if (IDE.testReloadMemoryLeak) {
							cu = Eclipse.adaptIR(getJavaProject(), javaFile);
						}
						if (warn) {
							String msg = "Possibly edited a file that has members omitted (not treated as source)";
							reportProblem(msg, cu);
							LOG.warning(msg);
						}
						result = true;
					}
					// Binding.ensureNonlocalBindingsLoaded(getProject());
				} else {
					cu = null;
				}
			}
		});

		Version current = IDE.getVersion();
		Version.setVersion(current);
		if (fineIsLoggable && cu != null) {
			LOG.fine("Got IR for " + javaOSFileName + " = " + cu);
			// createNewDrop(file);
		}
		return rv == Boolean.TRUE;
	}

	protected int batchIfReady(IAnalysisMonitor monitor) {
		return batchIfReady(monitor, false);
	}

	protected int batchIfReady(IAnalysisMonitor monitor, boolean force) {
		final int size = batchQ.size();
		if (force || size >= BATCH_SIZE) {
			ICompilationUnit[] cus = batchQ.toArray(new ICompilationUnit[size]);
			IRNode[] asts = new IRNode[size];
			Eclipse.adaptIR(getJavaProject(), cus, asts, monitor);
			batchQ.clear();

			Binding.ensureBindingsLoaded(monitor);

			if (IDE.testReloadMemoryLeak) {
				printUsage("BEFORE");
				Eclipse.adaptIR(getJavaProject(), cus, asts, null);
				printUsage("AFTER");
			}
			return size;
		}
		return 0;
	}

	private void invalidateOldDrops(final ICompilationUnit file) {
		// invalidate past results on this java file
		CUDrop outOfDate = SourceCUDrop.queryCU(new EclipseCodeFile(file));
		if (outOfDate != null)
			outOfDate.invalidate();
	}

	private void createNewDrop(CodeInfo info) {
		new SourceCUDrop(info);
		justLoaded.add(info.getFile());
		compUnits++;
	}

	// private IDrop createDrop(ICategory category, IResource resource,
	// final Contents dropContents) {
	// IDrop report = seaFacade.newDrop(this, resource.getProject(), resource,
	// category, dropContents);
	// dropContents.dependentDrop = report;
	// Eclipse.runAtMarker(new AbstractRunner() {
	//
	// public void run() {
	// dumpTree(LOG, System.out, dropContents.cu, 1);
	// }
	// });
	// bindingMap.put(dropContents.cu, report);
	// return report;
	// }

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
	 */
	@Override
	public IResource[] analyzeEnd(final IProject p, final IAnalysisMonitor monitor) {
		final long used2 = edu.cmu.cs.fluid.dc.Plugin.memoryUsed();
		final int nodes2 = AbstractIRNode.getTotalNodesCreated();
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("BEFORE nodes = " + nodes + ", memory = " + used);
			LOG.fine("AFTER  nodes = " + nodes2 + ", memory = " + used2);
			LOG.fine("DELTA  nodes = " + (nodes2 - nodes) + ", memory = "
					+ (used2 - used));
			LOG.fine("Comp units   = " + compUnits);
			LOG.fine("Type bindings = " + TypeBindings.numTypesBound());
			LOG.fine("Binding key size = " + Binding.bindingStringSize());
		}

		IDE.runVersioned(new AbstractRunner() {
			public void run() {
				batchIfReady(monitor, true);

				// Check if Object got processed correctly (has a drop)
				IRNode object = Eclipse.getDefault().getTypeEnv(p)
						.findNamedType("java.lang.Object");
				IRNode cu = VisitUtil.getEnclosingCompilationUnit(object);
				CUDrop drop = CUDrop.queryCU(cu);				
				if (drop == null && object != null) {
					// Need to make sure it gets processed right
					CodeInfo info = CodeInfo.createMatchTemplate(cu,
							"java.lang.Object");
					registerClass(info);
				}
								
				// process pre-fetch list first (so their bindings also get
				// loaded)
				processPrefetchList();

				handleWaitQueue(new IQueueHandler() {
					public void handle(String qname) {
						// handleWaitQueue() checks if it's already been
						// processed normally,
						// and skips otherwise
						//
						IRNode t = Eclipse.getDefault().getETypeEnv(p)
								.reloadNamedType(qname);
						if (t == null) {
							LOG.warning("Couldn't load " + qname
									+ " as a type or a package");
							// Eclipse.getDefault().getETypeEnv(p).reloadNamedType(qname);
						}
					}
				});
				Binding.ensureBindingsLoaded(monitor);
			}
		});

		final long used3 = edu.cmu.cs.fluid.dc.Plugin.memoryUsed();
		final int nodes3 = AbstractIRNode.getTotalNodesCreated();
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("BEFORE nodes = " + nodes2 + ", memory = " + used2);
			LOG.fine("AFTER  nodes = " + nodes3 + ", memory = " + used3);
			LOG.fine("DELTA  nodes = " + (nodes3 - nodes2) + ", memory = "
					+ (used3 - used2));
			LOG.fine("Comp units   = " + compUnits);
			LOG.fine("Type bindings = " + TypeBindings.numTypesBound());
			LOG.fine("Binding key size = " + Binding.bindingStringSize());
		}
		IDE.getInstance().clearAdapting();
		justLoaded.clear();
		return super.analyzeEnd(p, monitor);
	}

	// Used by both source and binary files
	class ITypeBindingHandler implements ISrcAdapterNotify {
		public void run(CodeInfo info) {
			checkIfRunning(info.getFileName());
			doneProcessing(info.getNode());

			if (!jloChanged && isProcessed("java.lang.Object")) {
				jloChanged = true;
			}
			if (info.getFile() != null) {
				// Usually a source file from the current project
				/*
				 * if (info.getFileName().endsWith("Object.java")) {
				 * System.out.println("Found Object"); }
				 */
				EclipseCodeFile ecf = (EclipseCodeFile) info.getFile();
				if (info.getFileName().contains("PropertyRecord")) {
					// System.out.println("p1 = "+getProject());
					// System.out.println("p2 = "+ecf.getProject());
				}
				if (getProject() == ecf.getProject()) {
					createNewDrop(info);
					return;
				}
				// Otherwise we still need to process this more
			}
			/*
			 * if (!running || javaFile == null) { if
			 * (info.getFileName().equals("java.lang.Object")) {
			 * LOG.fine("ITypeBindingHandler called for Object"); } else {
			 * LOG.severe("Called ConvertToIR listener while not running: " +
			 * info.getFileName()); } }
			 * 
			 * else if (project == null) { project =
			 * Eclipse.getDefault().findFirstJavaProject(); } else if
			 * (LOG.isLoggable(Level.FINE)) { LOG.fine("Creating a drop for " +
			 * info.getFileName() + " under " + getProject().getName()); }
			 */
			registerClass(info);
		}

		public void gotNewPackage(IRNode pkg, String name) {
			/*
			 * checkIfRunning(name);
			 * 
			 * CodeInfo info = CodeInfo.createMatchTemplate(VisitUtil
			 * .getEnclosingCompilationUnit(pkg), name); createDrop(info);
			 */
		}

		private void checkIfRunning(String name) {
			if (!IDE.getInstance().isAdapting()) {
				String msg = name
						+ " is only referenced from a promise, not code.  Try adding an import";
				reportProblem(msg, null);
				LOG.log(Level.SEVERE, msg, new Throwable("For stack trace"));
			}
		}
	}

	/**
	 * Only called for .class files
	 */
	void registerClass(CodeInfo info) {
		createDrop(info);
		javaFileLoaded(info);
	}

	private void createDrop(CodeInfo info) {
		if (info.getSource() != null) {
			invalidateOldDrops((ICompilationUnit) info.getFile()
					.getHostEnvResource());
			new SourceCUDrop(info);
			// createNewDrop(info);
		} else {
			CUDrop outOfDate = BinaryCUDrop.queryCU(info.getFileName());
			if (outOfDate != null)
				outOfDate.invalidate();

			new BinaryCUDrop(info);
		}
	}

	@Override
	protected Category warningCategory() {
		return JavaGlobals.CONVERT_TO_IR;
	}

	@Override
	protected Category problemCategory() {
		return JavaGlobals.CONVERT_TO_IR;
	}

	public static String findModule(IRNode n) {
		IRNode cu = VisitUtil.getEnclosingCompilationUnit(n);
		if (cu == null) {
			cu = n;
		}
		CUDrop dd = CUDrop.queryCU(cu);

		if (!(dd instanceof SourceCUDrop)) {
			return Binding.REST_OF_THE_WORLD;
		}
		SourceCUDrop d = (SourceCUDrop) dd;
		ICodeFile file = d.javaFile;
		return Binding.getModule(file);
	}

	/**
	 * List that holds all registered objects to be called back when an Eclipse
	 * ICompilationUnit is adapted into the IR.
	 */
	private static List<ISrcAdapterNotify> sf_srcAdapterNotifyList = new CopyOnWriteArrayList<ISrcAdapterNotify>();

	public static void register(ISrcAdapterNotify item) {
		sf_srcAdapterNotifyList.add(0, item); // HACK
	}

	private static void javaFileLoaded(CodeInfo info) {
		if (!IDE.getInstance().isAdapting()) {
			throw new IllegalArgumentException(
					"Calling listeners when not adapting");
		}
		for (Iterator<ISrcAdapterNotify> i = sf_srcAdapterNotifyList.iterator(); i
				.hasNext();) {
			ISrcAdapterNotify item = i.next();
			if (item != null)
			item.run(info);
		}
	}

	/**
	 * The list of types to be pre-fetched
	 */
	private static List<String> prefetchList = new ArrayList<String>();

	/**
	 * Intended to be called when an analysis module is created by
	 * double-checker
	 * 
	 * @param qname
	 *            The name of the type to be pre-loaded (typically for use by
	 *            analysis)
	 */
	public static void prefetch(String qname) {
		prefetchList.add(qname);
	}

	protected void processPrefetchList() {
		Eclipse e = Eclipse.getDefault();
		for (String t : e.getQueuedTypes()) {
			//System.out.println("Prefetching "+t);
			prefetch(t);
		}
		e.clearTypeQueue();

		ITypeEnvironment te = e.getTypeEnv(getProject());
		Iterator<String> qnames = prefetchList.iterator();
		while (qnames.hasNext()) {
			String qname = qnames.next();
			//System.out.println("Prefetching "+qname);
			te.findNamedType(qname);
		}
	}
}