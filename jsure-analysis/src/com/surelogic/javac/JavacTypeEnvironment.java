package com.surelogic.javac;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Multimap;
import com.surelogic.*;
import com.surelogic.analysis.IIRProject;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.PackageDrop;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;

@ThreadSafe
@Region("JTEState")
@RegionLock("JTELock is this protects JTEState")
public class JavacTypeEnvironment extends AbstractTypeEnvironment implements
		IOldTypeEnvironment {
	@ThreadSafe
	class Binder extends UnversionedJavaBinder {
		Binder(JavacTypeEnvironment te, boolean processJ8) {
			super(te, processJ8);
		}

		@Override
		public String toString() {
			return "Binder for " + typeEnvironment;
		}

		private IBinding getIBinding_javac(IRNode node, IRNode contextFlowUnit) {
			return super.getIBinding_impl(node);
		}

		private IJavaType getJavaType_javac(IRNode node) {
			return super.getJavaType_internal(node);
		}

		@Override
		public IBinding getIBinding(IRNode node) {
			return getTypeEnv_cached(node).binder.getIBinding_javac(node, null);
		}

		@Override
		public IBinding getIBinding(IRNode node, IRNode contextFlowUnit) {
			return getTypeEnv_cached(contextFlowUnit).binder.getIBinding_javac(node, contextFlowUnit);
		}
		
		@Override
		protected IJavaType getJavaType_internal(IRNode node) {
			return getTypeEnv_cached(node).binder.getJavaType_javac(node);
		}

		private <T> T findClassBodyMembers_javac(IRNode type,
				ISuperTypeSearchStrategy<T> tvs, boolean throwIfNotFound) {
			return super.findClassBodyMembers(type, tvs, throwIfNotFound);
		}

		@Override
		public <T> T findClassBodyMembers(IRNode type,
				ISuperTypeSearchStrategy<T> tvs, boolean throwIfNotFound) {
			return getTypeEnv_cached(type).binder.findClassBodyMembers_javac(type,
					tvs, throwIfNotFound);
		}
		
		private IJavaScope getImportTable_javac(IRNode node) {
		    return super.getImportTable(node);
		}
		
		@Override
		public IJavaScope getImportTable(IRNode node) {
			JavacTypeEnvironment tEnv = getTypeEnv_cached(node);
			return tEnv.binder.getImportTable_javac(node);
		}
		
		@Override
		public void reset() {
			super.reset();
		}
	}

	private static final boolean debug = Util.debug;
	final ClassTable classes = new ClassTable();
	final Binder binder;
	@InRegion("JTEState")
	private SLProgressMonitor monitor;
	@InRegion("JTEState")
	private JavacProject project;
	private final ConcurrentMap<IRNode, List<IRNode>> subtypeMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, CodeInfo> infos = new ConcurrentHashMap<>();
	
	@Unique("return")
	public JavacTypeEnvironment(Projects projs, JavacProject p,
			SLProgressMonitor monitor) {
		binder = new Binder(this, p.processJava8());
		project = p;
		//System.out.println("Creating "+this);
		
		this.monitor = monitor;
		AnnotationRules.initialize();

		if (p.getName().startsWith(Config.JRE_NAME) || p.containsJavaLangObject()) {
			initArrayClassDecl();
		} else {
			final String jre = IDE.getInstance().getStringPreference(
					IDEPreferences.DEFAULT_JRE);
			if (jre == "") {
				//System.out.println("No default JRE defined.");
				return;
			}
			final JavacProject jreP = projs.get(jre);
			if (jreP != null) {
				classes.addOuterClass(PromiseConstants.ARRAY_CLASS_QNAME, jreP
						.getTypeEnv().getArrayClassDeclaration());
			} else {
				//System.out.println("Couldn't add [] to " + p.getName());
			}
		}
	}

	// Only used by copy()
	private JavacTypeEnvironment(boolean processJ8) {		
		//System.out.println("Making copy()");
		binder = new Binder(this, processJ8);
	}

	@InRegion("JTEState")
	private IRNode arrayClassDeclaration = null;
	
	@RequiresLock("JTELock")
	private IRNode initArrayClassDecl() {
		final IRNode arrayType = DirtyTricksHelper.createArrayType(project.getName(), project, this);
		classes.addOuterClass(PromiseConstants.ARRAY_CLASS_QNAME, arrayType);
		return arrayType;
	}
	
	@Override
	public synchronized IRNode getArrayClassDeclaration() {
	    if (arrayClassDeclaration == null) {
	        arrayClassDeclaration = findNamedType(PromiseConstants.ARRAY_CLASS_QNAME);
	        if (arrayClassDeclaration == null) {
	        	// For the rare case that this is the source for the JDK itself
	        	arrayClassDeclaration = initArrayClassDecl();
	        }
	    }
	    return arrayClassDeclaration;
	}
	
	public synchronized JavacTypeEnvironment copy(JavacProject p) {
		JavacTypeEnvironment copy = new JavacTypeEnvironment(p.processJava8());
		copy.project = p;
		copy.infos.putAll(this.infos);
		copy.classes.copy(this.classes);
		for (Map.Entry<IRNode, List<IRNode>> e : this.subtypeMap.entrySet()) {
			copy.subtypeMap
					.put(e.getKey(), new ArrayList<>(e.getValue()));
		}
		return copy;
	}

	@Override
	public void clearCaches(boolean clearAll) {
		super.clearCaches(clearAll);
		binder.astsChanged();
		classes.clearCaches(clearAll);
		final Iterator<Map.Entry<String, CodeInfo>> it = infos.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, CodeInfo> e = it.next();
			if (e.getValue().getNode().identity() == IRNode.destroyedNode) {
//				System.out.println("Removed CodeInfo for "+e.getKey()+" from "+this);
				it.remove();
			}
		}
		typeEnvCache.clear();
	}

	@Override
	public String toString() {
		return "JavacTypeEnvironment "+hashCode()+": " + project.getName();
	}

	synchronized void setProgressMonitor(SLProgressMonitor m) {
		monitor = m;
	}

	public synchronized SLProgressMonitor getProgressMonitor() {
		return monitor;
	}

	@Override
  public synchronized JavacProject getProject() {
		return project;
	}

	synchronized void setProject(JavacProject newProject) {
		project = newProject;
	}

	@Override
  public UnversionedJavaBinder getBinder() {
		return binder;
	}

	@Override
	public synchronized int getMajorJavaVersion() {
		if (project != null) {
			int level = project.getConfig().getIntOption(Config.SOURCE_LEVEL);
			if (level != 0) {
				return level;
			}
		}
		return super.getMajorJavaVersion();
	}

	@Override
	public IJavaClassTable getClassTable() {
		return classes;
	}
	@ThreadSafe
	class ClassTable extends AbstractJavaClassTable {
		private final ConcurrentMap<String, IRNode> packages = new ConcurrentHashMap<>();
		private final ConcurrentMap<String, IRNode> outerClasses = new ConcurrentHashMap<>();

		public void copy(ClassTable orig) {
			this.packages.putAll(orig.packages);
			this.outerClasses.putAll(orig.outerClasses);
		}

		@Override
    public Set<String> allNames() {
			return outerClasses.keySet();
		}

		private int gc(Map<String, IRNode> map) {
			int cleaned = 0;
			final Iterator<Map.Entry<String, IRNode>> it = map.entrySet()
					.iterator();
			while (it.hasNext()) {
				Map.Entry<String, IRNode> e = it.next();
				if (e.getValue().identity() == IRNode.destroyedNode) {
					it.remove();
					cleaned++;
				}
			}
			return cleaned;
		}

		public int clearCaches(boolean clearAll) {
			return gc(packages) + gc(outerClasses);
		}

		@Override
    public Iterable<Pair<String, IRNode>> allPackages() {
			List<Pair<String, IRNode>> pairs = new ArrayList<>();
			for (Map.Entry<String, IRNode> e : packages.entrySet()) {
				pairs.add(new Pair<String, IRNode>(e.getKey(), e.getValue()));
			}
			return pairs;
		}

		@Override
    public IRNode getOuterClass(String qname, IRNode useSite) {
			/*
			 * if ("java.lang.Class".equals(qname)) { System.out.println(qname);
			 * }
			 */
			if (useSite != null) {
				JavacTypeEnvironment tEnv = getTypeEnv_cached(useSite);
				return tEnv.classes.getOuterClass(qname);
			}
			return getOuterClass(qname);
		}

		private IRNode getOuterClass(String qname) {
			IRNode result = outerClasses.get(qname);
			if (result == null) {
				result = packages.get(qname);
				if (result != null && result.identity() == IRNode.destroyedNode) {
					LOG.info("Removed old package "+qname);
					packages.remove(qname);
					return null;
				}
			}
			return result;
		}

		IRNode getPackage(String name, IRNode useSite) {
			if (useSite != null) {
				JavacTypeEnvironment tEnv = getTypeEnv_cached(useSite);
				return tEnv.classes.getPackage(name);
			}
			return getPackage(name);
		}

		IRNode getPackage(String name) {
			return packages.get(name);
		}

		@RequiresLock("JavacTypeEnvironment.this:JTELock")
		void addOuterClass(String name, IRNode decl) {			
			/*
			if (name.endsWith(SLUtility.JAVA_LANG_OBJECT)) {
				System.out.println("Adding: "+name+" to "+JavacTypeEnvironment.this); 
			}
			*/
			if (!name.endsWith(JJNode.getInfo(decl))) {
				throw new IllegalArgumentException(name + " doesn't match "
						+ DebugUnparser.toString(decl));
			}
			/*
			if (name.endsWith("EventType")) {
				System.out.println("Adding "+name+" = "+decl);
			}
			*/
			IRNode old = outerClasses.put(name, decl);
			/*
			 * if (name.equals(
			 * "com.acclamation.config.xml.ConfigurationStructureException")) {
			 * System.out.println("First time: "+name); }
			 */
			if (old != null && !old.equals(decl) /*
												 * && old.identity() !=
												 * IRNode.destroyedNode
												 */) {
				System.out.println("Warning: replacing " + name + " = " + decl
						+ " in " + project.getName());
			} else {
				// System.out.println("Added to "+project.getName()+": "+name+" = "+decl);
				/*
				 * if (name.startsWith("com.surelogic.common")) {
				 * System.out.println(); }
				 */
			}
		}

		/**
		 * @return true if modified
		 */
		boolean addPackage(JavacProject proj, String name, IRNode root,
				boolean replaceOnlyIfExists, Config.Type type) {
			if (proj == null) {
				throw new IllegalStateException("Null project");
			}
			IRNode pkg = packages.get(name);
			if (pkg == null) {
				if (replaceOnlyIfExists) {
					return false;
				}
				// System.out.println("Adding package: "+name);
				PackageDrop pd = PackageDrop.createPackage(proj, name, null,
						null, type);
				addPackage_private(name, pd);
				// Something's here
			} else if (root != null) {
				PackageDrop pd = PackageDrop.createPackage(proj, name, root,
						null, type); 
				addPackage_private(name, pd);
			} else {
				return false;
			}
			if (replaceOnlyIfExists) {
				System.out.println("Replaced "+name);
			}
			return true;
		}

		private void addPackage_private(String name, PackageDrop pd) {
			// TODO Use putIfAbsent?
			packages.put(name, pd.getPackageDeclarationNode());
		}
	}

	@Override
	public IRNode findPackage(String name, IRNode context) {
		IRNode pkg = classes.getPackage(name, context);
		if (pkg != null && pkg.identity() == IRNode.destroyedNode) {
			return null;
		}
		return pkg;
	}

	@Override
	public void addTypesInCU(IRNode cu) {
		for(IRNode t : VisitUtil.getTypeDecls(cu)) {
			final String name = JavaNames.getFullTypeName(t);
			classes.addOuterClass(name, t);
		}
	}
	
	/**
	 * @param addAlways
	 *            change no matter if it exists here or not; otherwise, just
	 *            replace
	 * @return true if modified
	 */
	public boolean addCompUnits(Iterable<CodeInfo> cus, boolean addAlways) {
		boolean changed = false;
		for (final CodeInfo info : cus) {
			/*
            if (info.getFileName().contains("Unmatched")) {
				System.out.println("Looking at "+info.getFileName());
			}
			*/
			changed |= addCompUnit(info, addAlways);
		}
		return changed;
	}

	/**
	 * @param addAlways
	 *            change no matter if it exists here or not; otherwise, just
	 *            replace if from the same project
	 * @return true if modified
	 */
	public boolean addCompUnit(final CodeInfo info, boolean addAlways) {
		boolean changed = false;
		final IIRProject newProj = info.getTypeEnv().getProject();
		final IRNode cu = info.getNode();
		final String pkg = VisitUtil.getPackageName(cu);
		IRNode pkgNode = null;
		if (addAlways || (pkgNode = classes.getPackage(pkg, cu)) != null) {
			if (pkgNode == null || Projects.getProject(pkgNode) == newProj) {
				final IRNode root = info.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA) ? info.getNode() : null;
				changed = classes.addPackage(project, pkg, root, !addAlways, info.getType());
				if (root != null) {
					infos.put(pkg+'.'+SLUtility.PACKAGE_INFO, info);
				}
			}
		}

		for (final IRNode td : VisitUtil.getTypeDecls(cu)) {
			String qname = pkg.length() == 0 ? JavaNames.getTypeName(td) : pkg
					+ "." + JavaNames.getTypeName(td);
			if (debug) {
				System.out.println("Found cu to add: " + qname);
			}
			if (!addAlways) {
				// Only replace if it's there
				CodeInfo old = infos.get(qname);
				if (old == null || old.getTypeEnv() != info.getTypeEnv() || !old.getFile().equals(info.getFile())) {
					continue;
				}
			}
			/* TODO Base64
            if (qname.endsWith("Base64")) {
				System.out.println("Adding CodeInfo for "+qname+" from "+this);
			}
			*/
			classes.addOuterClass(qname, td);
			infos.put(qname, info);
			changed = true;
		}
		return changed;
	}

	public CodeInfo findCompUnit(String qname) {
		return infos.get(qname);
	}

	/**
	 * type -> subtypes
	 */
	public void saveSubTypeInfo(Multimap<CUDrop, CUDrop> deps) {
		for (Map.Entry<IRNode, List<IRNode>> e : subtypeMap.entrySet()) {
			final IRNode tRoot = VisitUtil.findRoot(e.getKey());
			final CUDrop tCU = CUDrop.queryCU(tRoot);
			if (tCU == null) {
				CUDrop.queryCU(tRoot);
				throw new IllegalStateException();
			}
			for (IRNode s : e.getValue()) {
				final IRNode sRoot = VisitUtil.findRoot(s);
				if ("[]".equals(JJNode.getInfoOrNull(s))) {
					continue;
				}
				final CUDrop sCU = CUDrop.queryCU(sRoot);
				if (sCU == null) {
					CUDrop.queryCU(sRoot);
					throw new IllegalStateException();
				}
				deps.put(tCU, sCU);
			}
		}
	}

	public void postProcessCompUnits(final boolean debug) {
		Set<IRNode> roots = new HashSet<>();
		for (IRNode outer : classes.outerClasses.values()) {
			roots.add(VisitUtil.findRoot(outer));
		}
		//System.out.println("Clearing subtypeMap");
		subtypeMap.clear();

		for (IRNode root : roots) {
			processCompUnit(debug, root);
		}
	}

	@RequiresLock("JTELock")
	private void processCompUnit(final boolean debug, final IRNode top) {
		List<IRNode> newL = new ArrayList<IRNode>();
		for (IRNode n : VisitUtil.getAllTypeDecls(top)) {
			if (debug) {
				String name = JavaNames.getFullTypeName(n);
				System.out.println("Putting in " + name + " for " + project.getName());
				/*
				if (name.endsWith("AbstractSet")) {
					System.out.println("Looking at AbstractSet");
				}
				*/
			}			
			/*
            boolean print = false;
			if ("C1".equals(JJNode.getInfoOrNull(n))) {
				System.out.println("Found my class: "+JavaNames.getFullTypeName(n)+" -- "+n);
				System.out.println("TE = "+this);
				print = true;
			}
            */
			// Compute and store subtypes
			final IJavaType type = JavaTypeFactory.convertNodeTypeToIJavaType(
					n, binder);
			if (type instanceof IJavaDeclaredType) {
				final IJavaDeclaredType t = (IJavaDeclaredType) type;
				supers:
				for(IJavaType superT : t.getSupertypes(this)) {
					IJavaDeclaredType s = (IJavaDeclaredType) superT;
			

                    //if (print) {
					//System.out.println(getProject().getName()+" adding supertype: "+s+" <--- "+t);
					//}
					
					// Changed to put the info in supertype's type env, instead of the subtype's
					//
					// List<IRNode> oldL = subtypeMap.putIfAbsent(s.getDeclaration(), newL);
					final JavacTypeEnvironment tEnv = getTypeEnv_cached(s.getDeclaration());
					if (debug && tEnv != this) {
						if (tEnv.getProject().getName().contains(Config.JRE_NAME)) {
							System.out.println("JRE adding supertype: "+s+" <--- "+t);
						} else {
							System.out.println(tEnv.getProject().getName()+" adding supertype: "+s+" <--- "+t);
						}
					}
					List<IRNode> oldL = tEnv.subtypeMap.putIfAbsent(s.getDeclaration(), newL);
					if (oldL == null) {
						// Will be adding first mapping
						oldL = newL;
						newL = new ArrayList<IRNode>();
					}
					// TODO this could be really slow (n^2) for types like j.l.Object
					else if (oldL.contains(t.getDeclaration())) {
						// Skip, otherwise we would end up with duplicate mappings, due to 'type' being in both TypeEnvs
						if (debug) {
							System.out.println("Duplicate subtype info for "+t);
						}
						continue supers;
					}
					oldL.add(t.getDeclaration());
				}
			} else if (type instanceof IJavaTypeFormal) {
				// Ignoring these
			} else {
				LOG.warning("Unexpected type: " + type);
			}
		}
	}

	@Override
	public Iterable<IRNode> getRawSubclasses(IRNode type) {
		return getTypeEnv_cached(type).getRawSubclasses_javac(type);
	}
	
	private Iterable<IRNode> getRawSubclasses_javac(IRNode type) {
		Iterable<IRNode> subs = subtypeMap.get(type);
		if (subs == null) {
			return new EmptyIterator<IRNode>();
		}
		return subs;
	}

	public void addPackage(String pkg, Config.Type type) {
		// System.out.println("Added package: "+pkg);
		classes.addPackage(project, pkg, null, false, type);
	}

	public void addPackage(String pkg, IRNode root) {
		classes.addPackage(project, pkg, root, false, null);
	}

	// ---------------------------------------------------------------
	// Support for multiple projects
	// ---------------------------------------------------------------
	
	private final ConcurrentMap<IRNode,JavacTypeEnvironment> typeEnvCache =
		new ConcurrentHashMap<IRNode, JavacTypeEnvironment>();
	
	/*
	private JavacTypeEnvironment getTypeEnv(IRNode here) {
		if (here.identity() == IRNode.destroyedNode) {
			return null;
		}
		return computeTypeEnv(here);
	}
	*/
	
	JavacTypeEnvironment getTypeEnv_cached(IRNode here) {
		if (here == null) {
			return null;
		}
		if (here.identity() == IRNode.destroyedNode) {
			return null;
		}
		JavacTypeEnvironment tEnv = typeEnvCache.get(here);
		if (tEnv == null) {
			tEnv = computeTypeEnv(here);
			typeEnvCache.put(here, tEnv);
		}
		return tEnv;
	}
	
	private JavacTypeEnvironment computeTypeEnv(IRNode here) {
		final IRNode cu = VisitUtil.findCompilationUnit(here);
		final JavacProject p = Projects.getProject(cu);
		if (p == null) {
			System.out.println("No project for " + DebugUnparser.toString(cu));
		}
		return p.getTypeEnv();
	}

	private IJavaType convertNodeTypeToIJavaType_javac(IRNode node) {
		return super.convertNodeTypeToIJavaType(node);
	}

	private Iteratable<IJavaType> getSuperTypes_javac(IJavaType ty) {
		return super.getSuperTypes(ty);
	}

	@Override
	public Iteratable<IJavaType> getSuperTypes(IJavaType ty) {
		if (ty instanceof IJavaDeclaredType) {
			final IJavaDeclaredType dty = (IJavaDeclaredType) ty;
			return getTypeEnv_cached(dty.getDeclaration()).getSuperTypes_javac(dty);
		}
		return super.getSuperTypes(ty);
	}

	@Override
	public IJavaType convertNodeTypeToIJavaType(IRNode node) {
		return getTypeEnv_cached(node).convertNodeTypeToIJavaType_javac(node);
	}

	private IJavaDeclaredType getSuperclass_javac(IJavaDeclaredType ty) {
		return super.getSuperclass(ty);
	}

	@Override
	public IJavaDeclaredType getSuperclass(IJavaDeclaredType dty) {
		return getTypeEnv_cached(dty.getDeclaration()).getSuperclass_javac(dty);
	}

	@Override
  protected boolean isSubType(IJavaType s, IJavaType t,
			final boolean ignoreGenerics) {
		if (s == null || t == null) {
			return false;
		}
		/* Why is this here?	
		if (s.toString().equals(t.toString())) {
			return true;
		}
		*/
		return super.isSubType(s, t, ignoreGenerics);
	}
}
