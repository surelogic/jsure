package com.surelogic.javac;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.lang3.SystemUtils;

import com.surelogic.analysis.Analyses;
import com.surelogic.analysis.AnalysisGroup;
import com.surelogic.analysis.ConcurrencyType;
import com.surelogic.analysis.ConcurrentAnalysis;
import com.surelogic.analysis.IAnalysisGroup;
import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.IIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.granules.FlowUnitGranule;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.analysis.visitors.FlowUnitVisitor;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.annotation.parse.ParseUtil;
import com.surelogic.annotation.parse.SLAnnotationsLexer;
import com.surelogic.annotation.parse.SLThreadRoleAnnotationsLexer;
import com.surelogic.annotation.parse.ScopedPromisesLexer;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.annotation.rules.ScopedPromiseRules;
import com.surelogic.annotation.rules.VouchProcessorConsistencyProofHook;
import com.surelogic.common.FileUtility;
import com.surelogic.common.NullOutputStream;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.Decl;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.tool.SureLogicToolsFilter;
import com.surelogic.common.tool.SureLogicToolsPropertiesUtility;
import com.surelogic.dropsea.IAnalysisOutputDrop;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.MetricDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.SeaConsistencyProofHook;
import com.surelogic.dropsea.ir.drops.BinaryCUDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.CUDropClearOutAfterAnalysisProofHook;
import com.surelogic.dropsea.ir.drops.ClearOutUnconnectedResultsProofHook;
import com.surelogic.dropsea.ir.drops.PackageDrop;
import com.surelogic.dropsea.ir.drops.PromisePromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModelClearOutUnusedStaticProofHook;
import com.surelogic.dropsea.ir.drops.SourceCUDrop;
import com.surelogic.dropsea.ir.utility.Dependencies;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.javac.persistence.JSureDataDirScanner;
import com.surelogic.javac.persistence.JSurePerformance;
import com.surelogic.javac.persistence.JSureSubtypeInfo;
import com.surelogic.persistence.JSureResultsXMLReader;
import com.surelogic.persistence.JSureResultsXMLRefScanner;
import com.surelogic.persistence.JavaIdentifier;
import com.surelogic.xml.PromisesXMLParser;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.AbstractIRNode;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.ICodeFile;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.IJavaFileLocator.Type;
import edu.cmu.cs.fluid.java.SkeletonJavaRefUtility;
import edu.cmu.cs.fluid.java.adapter.AdapterUtil;
import edu.cmu.cs.fluid.java.bind.AbstractJavaBinder;
import edu.cmu.cs.fluid.java.bind.AbstractTypeEnvironment;
import edu.cmu.cs.fluid.java.bind.ICompUnitListener;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaCanonicalizer;
import edu.cmu.cs.fluid.java.bind.JavaRewrite;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.bind.UnversionedJavaBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.ClassBodyDeclaration;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.IllegalCode;
import edu.cmu.cs.fluid.java.operator.NestedDeclInterface;
import edu.cmu.cs.fluid.java.operator.Statement;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.project.JavaMemberTable;
import edu.cmu.cs.fluid.java.util.DeclFactory;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import extra166y.ParallelArray;
import extra166y.Ops.Procedure;

public class Util {
  public static final boolean useNewDriver = false;
  /**
   * Splits and integrates the rewrite into the adapter/canonicalizer
   */
  public static final boolean useIntegratedRewrite = true;
  
  public static final String EXPECT_ANALYSIS = "expectAnalysis";
  public static final String RECORD_ANALYSIS = "recordAnalysis";

  /** Should we try to run things in parallel */
  private static boolean wantToRunInParallel = true;// false;

  private static final boolean batchAndCacheBindingsForCanon = false;
  private static final boolean profileMemoryAfterLoading = false;
  private static final boolean testPersistence = false;
  private static final boolean loadPartial = false;
  public static final boolean useResultsXML = false;

  public static final boolean debug = false;
  private static final String HOME = System.getProperty("user.home");

  private static final boolean isWindows = SystemUtils.IS_OS_WINDOWS;
  private static final boolean isMacOS = SystemUtils.IS_OS_MAC_OSX;

  private static final Demo DEFAULT_DEMO = Demo.COMMON;

  private static final String WORK = isWindows ? "C:/work" : HOME + "/work";

  private static final String[] POSSIBLE_WORKSPACES = { WORK + "/workspace", WORK + "/fluid-workspace", WORK + "/fl-test-workspace" };

  public static final String WORKSPACE = isMacOS ? "/Users/aarong/Work/Eclipse Workspaces/Eclipse 3.3/Fluid Workspace"
      : findFirstExistingDir(POSSIBLE_WORKSPACES);

  private static final String[] POSSIBLE_ECLIPSES = { "C:/eclipse", WORK + "/eclipse-3.5.1",
      HOME + "/My Documents/bin/eclipse-3.3.1.1", };

  private static final String ECLIPSE = isWindows ? findFirstExistingDir(POSSIBLE_ECLIPSES)
      : isMacOS ? "/Eclipses/Eclipse-3.3-Fluid" : HOME + "/eclipse";

  private static final String[] POSSIBLE_JDKS = { "C:/Program Files/Java/jdk1.6.0_17", "C:/Program Files/Java/jdk1.6.0_16", };

  private static Logger LOG = SLLogger.getLogger();

  private enum Demo {
    TEST, COMMON, FLUID, JDK6, JEDIT, SMALL_WORLD;

    public static Demo get(final String id) {
      if (TEST.name().equals(id)) {
        return TEST;
      } else if (COMMON.name().equals(id)) {
        return COMMON;
      } else if (FLUID.name().equals(id)) {
        return FLUID;
      } else if (JDK6.name().equals(id)) {
        return JDK6;
      } else if (JEDIT.name().equals(id)) {
        return JEDIT;
      } else if (SMALL_WORLD.name().equals(id)) {
        return SMALL_WORLD;
      } else {
        throw new IllegalArgumentException("Unknown demo \"" + id + "\"");
      }
    }
  }

  private static String findFirstExistingDir(String[] paths) {
    for (String path : paths) {
      File f = new File(path);
      if (f.isDirectory()) {
        return path;
      }
    }
    System.out.println("No path exists");
    return "";
  }

  private static class JavacAnalysisEnvironment extends HashMap<Object, Object> implements IIRAnalysisEnvironment, IAnalysisMonitor {
    JavacClassParser loader;
    private final ZipOutputStream out;
    private boolean hasResults = false;
    private final SLProgressMonitor monitor;

    JavacAnalysisEnvironment(JavacClassParser loader, OutputStream out, SLProgressMonitor mon) {
      this.loader = loader;
      monitor = mon;
      this.out = out == null ? null : new ZipOutputStream(out);
    }

    @Override
    public void ensureClassIsLoaded(String qname) {
      loader.ensureClassIsLoaded(qname);
    }

    void finishedInit() {
      loader = null;
    }

    @Override
    public OutputStream makeResultStream(CUDrop cud) throws IOException {
      if (out == null) {
        return null;
      }
      String pathName = cud.getRelativePath();
      if (pathName != null) {
        if (pathName.startsWith("/")) {
          pathName = pathName.substring(1);
        }
        ZipEntry e = new ZipEntry(pathName);
        out.putNextEntry(e);
        return out;
      }
      return null;
    }

    @Override
    public void closeResultStream() throws IOException {
      if (out != null) {
        hasResults = true;
        out.closeEntry();
      }
    }

    @Override
    public void done() {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          if (hasResults) {
            e.printStackTrace();
          }
        }
      }
    }

    @Override
    public IAnalysisMonitor getMonitor() {
      return this;
    }

    @Override
    public void subTask(String name) {
      if (monitor != null) {
        monitor.subTask(name);
      }
    }

    @Override
    public void worked() {
      if (monitor != null) {
        monitor.worked(1);
      }
    }
  }

  private static void openFiles(Demo which, Config config) throws Exception {
    if (isMacOS) {
      config.addJar("/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes");
    } else {
      final String jdk;
      if (isWindows) {
        jdk = findFirstExistingDir(POSSIBLE_JDKS);
      } else {
        jdk = "/usr/lib/jvm/java-1.5.0/jre/lib";
      }
      if (!new File(jdk).exists()) {
        System.out.println("Does not exist: " + jdk);
        return;
      }
      config.addJar(jdk + "/jre/lib/ext");
      config.addJar(jdk + "/jre/lib");
    }
    config.addJar(WORKSPACE + "/common/lib/runtime");
    config.addJar(WORKSPACE + "/fluid/lib/jars");

    config.addJar(ECLIPSE + "/plugins/org.junit4_4.5.0.v20090824");
    config.addJar(ECLIPSE + "/plugins/org.junit4_4.3.1");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.core.resources_3.3.0.v20070604.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.core.contenttype_3.2.100.v20070319.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.text_3.3.0.v20070606-0010.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.core.runtime_3.3.100.v20070530.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.equinox.common_3.3.0.v20070426.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.equinox.preferences_3.2.100.v20070522.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.equinox.registry_3.3.1.R33x_v20070802.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.osgi_3.3.1.R33x_v20070828.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.core.jobs_3.3.1.R33x_v20070709.jar");
    config.addJar(ECLIPSE + "/plugins/org.eclipse.jdt.core_3.3.1.v_780_R33x.jar");
    if (which == Demo.JEDIT) {
      config.addJar(WORKSPACE + "/JEdit/promises.jar");
    } else if (which == Demo.SMALL_WORLD) {
      config.addJar(WORKSPACE + "/SmallWorld/jdom-1.0.jar");
    }
    final Projects projects = new Projects(config, new NullSLProgressMonitor());
    openFiles(projects, true);
  }

  static int estimateWork(Projects projects) {
    return 10 + projects.size() * (2 + Javac.numAnalyses());
  }

  /**
   * @param analyze
   *          Whether to analyze the loaded sources or not
   */
  public static File openFiles(Projects projects, boolean analyze) throws Exception {
    projects.getMonitor().begin(estimateWork(projects));
    startSubTask(projects.getMonitor(), "Initializing ...");
    Javac.initialize();

    File results = process(projects, analyze);
    if (analyze && useResultsXML && projects.getResultsFile() != null && projects.getResultsFile().exists()) {
      PromiseMatcher.load(projects.getResultsFile().getParentFile());
    }
    if (false) {
      JSureDataDirScanner.scan(new File("C:/work/jsure-test-workspace/test-data-dir"));
    }
    return results;
  }

  public static File openFiles(Projects oldProjects, final Projects projects, boolean analyze) throws Exception {
    projects.getMonitor().begin(estimateWork(projects));
    startSubTask(projects.getMonitor(), "Initializing ...");
    Javac.initialize();

    final boolean noConflict = !projects.conflictsWith(oldProjects);
    if (noConflict) {
      projects.init(oldProjects);
    } else {
      System.out.println("Detected a conflict between projects");
    }
    File result = process(projects, analyze);
    if (noConflict) {
      final Projects merged = projects.merge(oldProjects);
      // pd.setProjects(merged);
      // System.out.println("Merged projects: "+merged.getLabel());
    }
    return result;
  }

  private static <T> void eliminateDups(List<T> all, List<T> unique) {
    Set<T> temp = new HashSet<T>(all);
    all.clear();
    unique.clear();
    unique.addAll(temp);
  }

  /**
   * @return the location of the results
   */
  static File process(Projects projects, boolean analyze) throws Exception {
	analyze = analyze && !profileMemoryAfterLoading;
	
    System.out.println("monitor = " + projects.getMonitor());
    clearCaches(projects);
    if (loadPartial) {
      selectFilesToLoad(projects);
    }

    final boolean singleThreaded = !wantToRunInParallel || ConcurrentAnalysis.singleThreaded;
    System.out.println("singleThread = " + singleThreaded);
    final JSurePerformance perf = new JSurePerformance(projects, singleThreaded);
    
    ScopedPromisesLexer.init();
    SLAnnotationsLexer.init();
    SLThreadRoleAnnotationsLexer.init();
    JavacClassParser loader = new JavacClassParser(perf.pool, projects);

    // loader.ensureClassIsLoaded("java.util.concurrent.locks.ReadWriteLock");
    loader.ensureClassIsLoaded(SLUtility.JAVA_LANG_OBJECT);
    final OutputStream results = NullOutputStream.prototype;
        //projects.getResultsFile() == null ? null : new FileOutputStream(projects.getResultsFile());
    final JavacAnalysisEnvironment env = new JavacAnalysisEnvironment(loader, results, projects.getMonitor());

    final Analyses analyses = Javac.makeAnalyses();
    for(IIRAnalysis<?> a : analyses) {
		a.init(env);
	}
	env.finishedInit(); // To free up memory
    
    final ParallelArray<CodeInfo> cus = perf.createArray(CodeInfo.class);
    endSubTask(projects.getMonitor());

    for (Config config : projects.getConfigs()) {
      destroyOldCUs(config.getProject(), config.getRemovedFiles());
    }

    perf.startTiming();
    List<CodeInfo> temp = new ArrayList<CodeInfo>();
    loader.parse(temp);
    IDE.getInstance().setDefaultClassPath(projects.getFirstProjectOrNull());
    
    eliminateDups(temp, cus.asList());
    temp = null; // To free up memory

    perf.markTimeFor("Parsing");
    // checkForDups(cus.asList());
    if (!useIntegratedRewrite) {
    	rewriteCUs(projects, cus.asList(), projects.getMonitor(), loader);
    	// checkForDups(cus.asList());
    	// Really to check if we added type refs via default constructors
    	//    loader.checkReferences(cus.asList());

    	eliminateDups(cus.asList(), cus.asList());
    	// checkForDups(cus.asList());
    	clearCaches(projects); // To clear out old state invalidated by rewriting
    	//ClassSummarizer.printStats();

    	perf.markTimeFor("Rewriting");
    }
    canonicalizeCUs(perf, cus, projects);
    // Checking if we added type refs by canonicalizing implicit refs
    loader.checkReferences(cus.asList());
    loader = null; // To free up memory

    perf.markTimeFor("Canonicalization");
    eliminateDups(cus.asList(), cus.asList());
    clearCaches(projects);

    perf.markTimeFor("Cleanup");
    final boolean addRequired = false;
    if (addRequired) {
      addRequired(cus, projects.getMonitor());
    }
    perf.markTimeFor("Add.required");
    final Dependencies deps = checkDependencies(cus);

    // cus now include reprocessed dependencies
    createCUDrops(cus, projects.getMonitor());
    if (addRequired) {
      clearCaches(projects);
    }
    perf.markTimeFor("Drop.creation");
    parsePromises(cus, projects.getMonitor());
    /*
     * for(CodeInfo i : cus.asList()) { if (i.getFileName().endsWith(".java")) {
     * System.out.println("Found: "+i.getFileName()); } }
     */
    perf.markTimeFor("Promise.parsing");
    if (projects.getMonitor().isCanceled()) {
      return null;
    }
    // Needed by the scrubber
    computeSubtypeInfo(projects);
    scrubPromises(cus.asList(), projects.getMonitor());
    // RegionModel.printModels();

    perf.markTimeFor("Promise.scrubbing");
    perf.setLongProperty("Total.nodes", AbstractIRNode.getTotalNodesCreated());
    perf.setLongProperty("Total.destroyed", SlotInfo.numNodesDestroyed());
    perf.setLongProperty("Total.gced", SlotInfo.numGarbageCollected());
    long[] times;
    if (analyze) {
      // These are all the SourceCUDrops for this project
      final ParallelArray<SourceCUDrop> cuds = findSourceCUDrops(perf);
      final ParallelArray<SourceCUDrop> allCuds = cuds;// findSourceCUDrops(null,
                                                        // singleThreaded,
                                                        // pool);
      checkforCUs(cus, cuds);
      times = analyzeCUs(env, projects, analyses, cuds, allCuds, perf);
      env.done();
      matchResults(projects);
      perf.markTimeFor("All.analyses");
    } else {
      times = new long[analyses.size()];
    }
    File tmpLocation;
    if (!profileMemoryAfterLoading) {
    	System.out.println("Updating consistency proof");
    	final SeaConsistencyProofHook vouchHook = new VouchProcessorConsistencyProofHook();
    	final SeaConsistencyProofHook staticHook = new RegionModelClearOutUnusedStaticProofHook();
    	final SeaConsistencyProofHook cuDropHook = new CUDropClearOutAfterAnalysisProofHook();
    	final SeaConsistencyProofHook clearResultsHook = new ClearOutUnconnectedResultsProofHook();
    	Sea.getDefault().addConsistencyProofHook(vouchHook);
    	Sea.getDefault().addConsistencyProofHook(staticHook);
    	Sea.getDefault().addConsistencyProofHook(cuDropHook);
    	Sea.getDefault().addConsistencyProofHook(clearResultsHook);
    	Sea.getDefault().updateConsistencyProof();
    	Sea.getDefault().removeConsistencyProofHook(clearResultsHook);
    	Sea.getDefault().removeConsistencyProofHook(cuDropHook);
    	Sea.getDefault().removeConsistencyProofHook(staticHook);
    	Sea.getDefault().removeConsistencyProofHook(vouchHook);

    	filterResultsBySureLogicToolsPropertiesFile(projects);

    	perf.markTimeFor("Sea.update");

    	// This would clear things before I persist the info
    	//
    	// IDE.getInstance().clearCaches();

    	/*
    	 * if (false) { for(ProofDrop d :
    	 * Sea.getDefault().getDropsOfType(ProofDrop.class)) { if
    	 * (!d.provedConsistent()) { ISrcRef ref = d.getSrcRef(); if (ref != null) {
    	 * System
    	 * .out.print(ref.getCUName()+":"+ref.getLineNumber()+" - "+d.getMessage());
    	 * } } } } else { writeOutput(projects); }
    	 */
    	tmpLocation = RemoteJSureRun.snapshot(System.out, projects.getLabel(), projects.getRunDir());
    	perf.markTimeFor("Sea.export");
    	testExperimentalFeatures(projects, cus);
    } else {
    	tmpLocation = null;
    }
	final long total = perf.stopTiming("Total.JSure.time");
    System.out.println("Done in " + total + " ms.");
    if (analyze) {
      int i = 0;
      for (final IIRAnalysis<IAnalysisGranule> a : analyses) {
        System.out.println(a.name() + "\t: " + times[i] + " ms");
        perf.setLongProperty("analysis."+a.name(), times[i]);
        i++;
      }
    }
    perf.setIntProperty("Total.try.destroyed", destroyedNodes);
    //perf.setIntProperty("Total.not.destroyed", diffNodes);
    perf.setIntProperty("Total.canonical", canonicalNodes);
    perf.setIntProperty("Total.decls", decls);
    perf.setIntProperty("Total.stmts", stmts);
    perf.setIntProperty("Total.blocks", blocks);
    perf.setIntProperty("Total.loc", computeLOC());
    perf.setLongProperty("Find.canon.time", findTime);
    perf.setLongProperty("Destroy.time", destroyTime);
    // System.out.println("Binary rewrites : "+binaryRewrites);
    UnversionedJavaBinder.printStats(perf);
    AbstractTypeEnvironment.printStats();
    perf.store();
    perf.print(System.out);
    return tmpLocation;
  }

  private static void checkforCUs(ParallelArray<CodeInfo> cus, ParallelArray<SourceCUDrop> cuds) {
	Map<IRNode, SourceCUDrop> drops = new HashMap<IRNode, SourceCUDrop>(cuds.size());
	for(SourceCUDrop d : cuds) {
		drops.put(d.getNode(), d);
	}
	for(CodeInfo info : cus) {
		if (!info.isAsSource() || info.getFileName().endsWith("package-info.java")) {
			continue;
		}
		if (drops.get(info.getNode()) == null) {
			SLLogger.getLogger().log(Level.INFO, "Didn't find source drop for "+info.getFileName(), new Throwable());
		}
	}
  }
  
  private static int computeLOC() {
	int loc = 0;
	for(MetricDrop m : Sea.getDefault().getDropsOfExactType(MetricDrop.class)) {		
		if (m.getMetric() == IMetricDrop.Metric.SLOC) {
			loc += m.getMetricInfoAsInt(IMetricDrop.SLOC_LINE_COUNT, 0);		
		}
	}
	return loc;
  }

  private static void filterResultsBySureLogicToolsPropertiesFile(Projects projects) {
    /*
     * Do this per-project and only clear out excluded drops if they are in the
     * project's exclusions -- otherwise we could match things, and exclude
     * them, when we should not.
     */
    for (JavacProject p : projects) {
      String[] excludedSourceFolders = p.getConfig().getListOption(SureLogicToolsPropertiesUtility.SCAN_EXCLUDE_SOURCE_FOLDER);
      String[] excludedPackagePatterns = p.getConfig().getListOption(SureLogicToolsPropertiesUtility.SCAN_EXCLUDE_SOURCE_PACKAGE);
      if (excludedSourceFolders.length > 0 || excludedPackagePatterns.length > 0) {
        final SureLogicToolsFilter filter = SureLogicToolsPropertiesUtility.getFilterFor(excludedSourceFolders,
            excludedPackagePatterns);
        for (final Drop d : Sea.getDefault().getDrops()) {
          final IJavaRef ref = d.getJavaRef();
          if (ref != null) {
            if (p.getName().equals(ref.getEclipseProjectNameOrNull())) {
              final String pkg = ref.getPackageName();
              if ("Object".equals(ref.getTypeNameOrNull()) && "java.lang".equals(pkg)) {
            	  // Keep anything on java.lang.Object
            	  continue;
              }
              if (filter.matches(ref.getAbsolutePathOrNull(), pkg)) {
                System.out.println("surelogic-tools.properties in project " + p.getName() + " filtered out drop about "
                    + ref.getTypeNameFullyQualified());
                d.invalidate();
              }
            }
          }
        }
      }
    }
  }

  private static void checkForDups(List<CodeInfo> cus) {
    Map<IRNode, CodeInfo> seen = new HashMap<IRNode, CodeInfo>();
    for (CodeInfo cu : cus) {
      CodeInfo dup = seen.get(cu.getNode());
      if (dup != null) {
        System.out.println("Already contains " + cu.getFile().getRelativePath());
      } else {
        seen.put(cu.getNode(), cu);
      }
    }
  }

  private static void computeSubtypeInfo(Projects projects) throws IOException {
    // Compute/persist subtype info
    final boolean saveSubtypeInfo = useResultsXML && projects.getResultsFile() != null;
    final MultiMap<CUDrop, CUDrop> subtypeDependencies = saveSubtypeInfo ? new MultiHashMap<CUDrop, CUDrop>() : null;
    for (JavacProject p : projects) {
      // Compute subtype info
      p.getTypeEnv().postProcessCompUnits(false);
      if (saveSubtypeInfo) {
        // TODO do I need to separate out the info?
        p.getTypeEnv().saveSubTypeInfo(subtypeDependencies);
        JSureSubtypeInfo.save(projects.getResultsFile().getParentFile(), subtypeDependencies);
      }
    }
  }

  /**
   * Read the results and match them up to drops
   */
  private static void matchResults(Projects projects) throws Exception {
    if (true) {
      return;
    }
    final File results = useResultsXML ? projects.getResultsFile() : null;
    if (results == null || !results.exists()) {
      return;
    }
    JSureResultsXMLRefScanner scanner = new JSureResultsXMLRefScanner(projects);
    try {
      scanner.readXMLArchive(results);
    } catch (ZipException e) {
      System.out.println("Bad results zip -- nothing to match");
      return;
    }
    // Test code for JSureResultsXMLRefScanner
    final Map<String, SourceCUDrop> sources = new HashMap<String, SourceCUDrop>();
    final Map<String, IRNode> types = new HashMap<String, IRNode>();
    for (CUDrop cud : Sea.getDefault().getDropsOfType(CUDrop.class)) {
      if (cud instanceof SourceCUDrop) {
        String path = FileUtility.normalizePath(cud.getRelativePath());
        sources.put(path, (SourceCUDrop) cud);
      }
      for (IRNode type : VisitUtil.getTypeDecls(cud.getCompilationUnitIRNode())) {
        String loc = JavaIdentifier.encodeDecl(cud.getTypeEnv().getProject(), type);
        types.put(loc, type);
      }
    }
    scanner.selectByFilePath(sources);
    scanner.selectByTypeLocation(types);

    ParseUtil.init();
    try {
      new JSureResultsXMLReader(projects).readXMLArchive(results);
    } finally {
      ParseUtil.clear();
    }
  }

  /**
   * Select one file to load, and remove the rest
   */
  private static void selectFilesToLoad(Projects projects) {
    for (Config c : projects.getConfigs()) {
      if (c.getFiles().size() <= 0) {
        continue;
      }
      final int selected = new Random().nextInt(c.getFiles().size());
      int i = 0;
      for (JavaSourceFile f : c.getFiles()) {
        if (i == selected) {
          System.err.println("Selecting to analyze " + f.relativePath);
          c.intersectFiles(Collections.singletonList(f));
          break;
        }
        i++;
      }

    }
  }

  private static void testExperimentalFeatures(final Projects projects, ParallelArray<CodeInfo> cus) {
    if (testPersistence) {
      ParseUtil.init();
      try {
        for (CodeInfo cu : cus.asList()) {
          JavaIdentifier.testFindEncoding(projects, cu.getTypeEnv().getProject(), cu.getNode());
        }
      } finally {
        ParseUtil.clear();
      }
    }
  }

  /**
   * Gets every drop if pd is null
   */
  private static ParallelArray<SourceCUDrop> findSourceCUDrops(final JSurePerformance perf) {
    final ParallelArray<SourceCUDrop> cuds = perf.createArray(SourceCUDrop.class);
    for (SourceCUDrop scud : Sea.getDefault().getDropsOfExactType(SourceCUDrop.class)) {
      cuds.asList().add(scud);
    }
    return cuds;
  }

  private static void clearCaches(Projects projects) {
    // Clear binder cache
    // IDE.getInstance().notifyASTsChanged();
    // IDE.getInstance().clearAll();
    for (JavacProject jp : projects) {
      jp.getTypeEnv().clearCaches(true);
    }
  }

  private static void destroyOldCUs(String project, Iterable<File> removed) {
    for (File f : removed) {
      System.out.println("Removing " + f);
      SourceCUDrop cud = SourceCUDrop.queryCU(new FileResource(project, f));
      if (cud != null) {
        cud.invalidate();
        AdapterUtil.destroyOldCU(cud.getCompilationUnitIRNode());
      }
    }
  }

  private static void clearOldResults(CUDrop cud) {
    System.out.println("Clearing old results for " + cud);
    for (Drop d : cud.getDependents()) {
      if (d instanceof IAnalysisOutputDrop) {
        if (Dependencies.DROP_MESSAGE_DEBUG == null || d.getMessage().startsWith(Dependencies.DROP_MESSAGE_DEBUG)) {
          System.err.println("\t" + d.getMessage());
        }
        d.invalidate();
      }
    }
  }

  private static long[] analyzeCUs(final IIRAnalysisEnvironment env, final Projects projects, final Analyses analyses,
      ParallelArray<SourceCUDrop> cus, ParallelArray<SourceCUDrop> allCus, JSurePerformance perf) {
    if (XUtil.recordScript() != null) {
      final File log = (File) projects.getArg(RECORD_ANALYSIS);
      if (log != null) {
        recordFilesAnalyzed(allCus, log);
      }
      final File expected = (File) projects.getArg(EXPECT_ANALYSIS);
      if (expected != null && expected.exists()) {
        checkForExpectedSourceFiles(allCus, expected);
      }
    }
    System.out.println("Starting analyses");
    final AllTimings timings = new AllTimings(analyses);
    
    if (useNewDriver) {
    	System.out.println("Using new analysis framework");
    	boolean first = true;
    	    	
    	final MultiMap<IAnalysisGranulator<?>,IAnalysisGranule> granules = new MultiHashMap<IAnalysisGranulator<?>, IAnalysisGranule>();
    	for(AnalysisGroup<?> group : analyses.getGroups()) {
    		final boolean runOneThread = perf.singleThreaded || group.runSingleThreaded();    				
    		System.out.println("Starting group: "+group.getLabel());
    		if (group.getGranulator() == null) {
    			final AnalysisGroup<CUDrop> cuGroup = (AnalysisGroup<CUDrop>) group;
        		final AnalysisInfo<CUDrop> ai = new AnalysisInfo<CUDrop>(runOneThread, CUDrop.class, env, projects.getMonitor());	
        		ai.populate(allCus.asList());
        		ai.analyzeProjects(projects, cuGroup, timings);
    		} else {
				final AnalysisInfo ai = new AnalysisInfo(runOneThread, group.getGranuleType(), env, projects.getMonitor());
        		ai.populate(granules.get(group.getGranulator()));        		
        		ai.analyzeProjects(projects, group, timings);
    		}
    		if (first) {
    			first = false;
    			   
        		// TODO do this with each group above?
    			// TODO in parallel?
        		for(CUDrop d : allCus) {
        			for(IAnalysisGranulator<?> g : analyses.getGranulators()) {
        				// This may require some setup!
        				g.extractGranules(d.getTypeEnv(), d.getCompilationUnitIRNode());
        			}
        		}
     			for(IAnalysisGranulator<?> g : analyses.getGranulators()) {
     				Collection<? extends IAnalysisGranule> toAnalyze = g.getGranules();
     				System.out.println(g+": "+toAnalyze.size());
     				granules.putAll(g, toAnalyze);
     			}
    		}
    	}
    	
    	// TODO how to focus all the analyses on a few CUs, while dealing with individual analyses that might take a long time
    	// use fork-join for uniqueness and the like?
    	
    	// Finish all analyses
    	int i = 0;
    	for (final IIRAnalysis<?> a : analyses) {
    		final long start = System.nanoTime();
    		a.finish(env);
    		final long end = System.nanoTime();
    		timings.times[i] += end - start;
    		i++;
    	}
        return timings.summarize();
    }
    System.out.println("Using old analysis framework");
    final long[] times = new long[analyses.size()];
    int i = 0;
    for (final IIRAnalysis<IAnalysisGranule> a : analyses) {
      final long start = System.currentTimeMillis();
      final ParallelArray<SourceCUDrop> toAnalyze = a.analyzeAll() ? allCus : cus;
      //System.out.println(a.name()+" analyzing "+(a.analyzeAll() ? "all CUs" : "source CUs"));
      
      for (final JavacProject project : projects) {
        if (projects.getMonitor().isCanceled()) {
          throw new CancellationException();
        }
        final int num = toAnalyze.asList().size();
        if (num == 0) {
          continue;
        }
        String inParallel;
        switch (a.runInParallel()) {
        case INTERNALLY:
          inParallel = "PARALLEL ";
          break;
        case EXTERNALLY:
          inParallel = perf.singleThreaded ? "" : "parallel ";
          break;
        default:
          inParallel = "";
        }
        startSubTask(projects.getMonitor(),
            "Starting " + inParallel + a.name() + " [" + i + "]: " + num + " for " + project.getName());
        a.analyzeBegin(env, project);

        final int which = i;
        final PromiseFramework frame = PromiseFramework.getInstance();
        Procedure<SourceCUDrop> proc = new Procedure<SourceCUDrop>() {
          @Override
          public void op(SourceCUDrop cud) {
            if (!cud.isAsSource()) {
              // LOG.warning("No analysis on "+cud.javaOSFileName);
              return;
            }
            if (projects.getMonitor().isCanceled()) {
              throw new CancellationException();
            }
            if (project.getTypeEnv() == cud.getTypeEnv()) { // Same project!
              // System.out.println("Running "+a.name()+" on "+cud.javaOSFileName);
              try {
            	final AnalysisTimings timing = timings.timings.get();
                frame.pushTypeContext(cud.getCompilationUnitIRNode());
                final long start = System.nanoTime();
                try {
                  a.doAnalysisOnGranule(env, cud);
                } finally {
                	final long end = System.nanoTime();
                	timing.times[which] += (end - start);
                }
              } catch (RuntimeException e) {
                System.err.println("Error while processing " + cud.getJavaOSFileName());
                throw e;
              } finally {
                frame.popTypeContext();                
              }
            }
          }
        };
        switch (a.runInParallel()) {
        case INTERNALLY:
        default:
          // Handled by the analysis itself
          for (final SourceCUDrop cud : toAnalyze) {
            proc.op(cud);
          }
          break;
        case EXTERNALLY:
          toAnalyze.apply(proc);
        }
        final long startNano = System.nanoTime();
        handleAnalyzeEnd(a, env, project);

        a.postAnalysis(project);
        final long endNano = System.nanoTime();
        timings.times[which] += (endNano - startNano);
        
        endSubTask(projects.getMonitor());
      }
      times[i] += System.currentTimeMillis() - start;
      i++;
    }
    i = 0;

    // Finish
    for (final IIRAnalysis<IAnalysisGranule> a : analyses) {
      final long start = System.currentTimeMillis();
      a.finish(env);
      final long end = System.currentTimeMillis();
      times[i] += end - start;
      i++;
    }
    i = 0;
    
    final long[] allTimesNano = timings.summarize();
    for (final IIRAnalysis<IAnalysisGranule> a : analyses) {
    	perf.setLongProperty("analysis.all.nano."+a.name(), allTimesNano[i]);
    	i++;
    }
    return times;
  }

  static <Q extends IAnalysisGranule> 
  void handleAnalyzeEnd(IIRAnalysis<Q> a, IIRAnalysisEnvironment env, IIRProject project) {
	  boolean moreToAnalyze;
	  do {
		  moreToAnalyze = false;
		  for(Q granule : a.analyzeEnd(env, project)) {
			  moreToAnalyze = true;

			  // TODO parallelize?
			  a.doAnalysisOnGranule(env, granule);
		  }
	  } while (moreToAnalyze);
  }
  
  static class AllTimings {
	  final Analyses analyses;
	  final long[] times;
	  
	  final List<AnalysisTimings> allTimings = new CopyOnWriteArrayList<AnalysisTimings>();
	  final ThreadLocal<AnalysisTimings> timings = new ThreadLocal<AnalysisTimings>() {
		  @Override
		  protected AnalysisTimings initialValue() {
			  AnalysisTimings rv = new AnalysisTimings(analyses);
			  allTimings.add(rv);
			  return rv;
		  }
	  };
	  
	  AllTimings(Analyses analyses) {
		  this.analyses = analyses;
		  this.times = new long[analyses.size()];
	  }

	  long[] summarize() {
		  for(AnalysisTimings t : allTimings) {
			  for(int j=0; j<times.length; j++) {
				  times[j] += t.times[j]; 
			  }			  
		  }
		  
		  // Postprocess times to normalize to millis
		  for(int i = 0; i<times.length; i++) {
			  times[i] = times[i] / 1000000;
		  }
		  return times;
	  }	  
  }
  
  static class AnalysisTimings {
	  final Analyses analyses;
	  final long[] times;
	  
	  AnalysisTimings(Analyses analyses) {
		  this.analyses = analyses;
		  times = new long[analyses.size()];
	  }
  }  
  
  static class AnalysisInfo<Q extends IAnalysisGranule> extends ConcurrentAnalysis<Q> {
	  final IIRAnalysisEnvironment env;
	  final MultiMap<ITypeEnvironment, Q> granules = new MultiHashMap<ITypeEnvironment, Q>();
	  final SLProgressMonitor monitor;
	  
	  AnalysisInfo(boolean runOneThread, Class<Q> cls, IIRAnalysisEnvironment e, SLProgressMonitor mon) {
		  super(!runOneThread, cls);
		  env = e;
		  monitor = mon;
	  }
  
	  void populate(Collection<? extends Q> newGranules) {
		  if (newGranules == null) {
			  return;
		  }
		  for(Q granule : newGranules) {
			  granules.put(granule.getTypeEnv(), granule);
		  }
	  }
	  
	  private Procedure<Q> setupProcedure(final IAnalysisGroup<Q> analyses, final ThreadLocal<AnalysisTimings> timings) {
		  final PromiseFramework frame = PromiseFramework.getInstance();	  
		  final Procedure<Q> rv = new Procedure<Q>() {	  
			  @Override
			  public void op(Q granule) {
				  if (!granule.isAsSource()) {
					  // LOG.warning("No analysis on "+granule.javaOSFileName);
					  return;
				  }
				  if (monitor.isCanceled()) {
					  throw new CancellationException();
				  }				  
				  // System.out.println("Running "+a.name()+" on "+granule.javaOSFileName);
				  try {
					  final AnalysisTimings timing = timings.get();
					  frame.pushTypeContext(granule.getCompUnit());
					  int i = analyses.getOffset();
					  for (final IIRAnalysis<Q> a : analyses) {
						  if (monitor != null) {
							  monitor.subTask("Checking [ "+a.label()+" ] " + granule.getLabel());
						  }
						  final long start = System.nanoTime();
						  a.doAnalysisOnGranule(env, granule);
						  final long end = System.nanoTime();
						  final long time = end - start;
						  recordTime(granule, a, time);
						  timing.times[i] += time;
						  i++;	
					  }

				  } catch (RuntimeException e) {
					  System.err.println("Error while processing " + granule.getLabel());
					  throw e;
				  } finally {
					  frame.popTypeContext();
				  }
			  }				  			  
		  };
		  if (analyses.getGranulator() != null) {
			  setWorkProcedure(analyses.getGranulator().wrapAnalysis(rv));
		  } else {
			  setWorkProcedure(rv);
		  }
		  return getWorkProcedure();
	  }
	  
	  protected void recordTime(Q granule, IIRAnalysis<Q> a, long t_in_ns) {
		  final MetricDrop d = new MetricDrop(granule.getNode(), IMetricDrop.Metric.SCAN_TIME);
		  
		  final IKeyValue name = KeyValueUtility.getStringInstance(IMetricDrop.SCAN_TIME_ANALYSIS_NAME, a.label());
		  d.addOrReplaceMetricInfo(name);
		  
		  final IKeyValue time = KeyValueUtility.getLongInstance(IMetricDrop.SCAN_TIME_DURATION_NS, t_in_ns);
		  d.addOrReplaceMetricInfo(time);
	  }

	  void analyzeProjects(final Projects projects, IAnalysisGroup<Q> analyses, final AllTimings timing) {
		setupProcedure(analyses, timing.timings); 
  		for (final JavacProject project : projects) {
			if (projects.getMonitor().isCanceled()) {
				throw new CancellationException();
			}
			analyzeAProject(project, analyses, timing);            
		}
	  }
	  
	  private void analyzeAProject(final JavacProject project, IAnalysisGroup<Q> analyses, final AllTimings timing) {		  
		  final Collection<Q> fromProj = granules.get(project.getTypeEnv());
		  int i = analyses.getOffset();
		  for (final IIRAnalysis<Q> a : analyses) {
			  //System.out.println(a.name()+" analyzing "+(a.analyzeAll() ? "all CUs" : "source CUs"));
	  
			  if (monitor.isCanceled()) {
				  throw new CancellationException();
			  }
			  final String inParallel = singleThreaded ? "" : "parallel ";
			  startSubTask(monitor, "Starting " + inParallel + a.name() + " [" + i + "]: " + (fromProj == null ? 0 : fromProj.size()) + 
					                " for " + project.getName());
			  final long start = System.nanoTime();
			  try {
				  a.analyzeBegin(env, project);
			  } finally {
				  final long end = System.nanoTime();
				  timing.times[i] += end - start;
				  i++;
			  }
		  }
		  // Analyze granules
		  if (fromProj != null) {
			  analyzeGranules(fromProj);
			  flushWorkQueue();
		  }
		  
		  // Finishing up loose ends (if any)
		  i = analyses.getOffset();
		  for (final IIRAnalysis<Q> a : analyses) {
			  final long start = System.nanoTime();
			  handleAnalyzeEnd(a, env, project);
			  final long end = System.nanoTime();
			  timing.times[i] += end - start;
			  i++;	
		  }
		  
		  // All analysis is done for the project
		  i = analyses.getOffset();
		  for (final IIRAnalysis<Q> a : analyses) {
			  final long start = System.nanoTime();
			  a.postAnalysis(project);
			  final long end = System.nanoTime();
			  timing.times[i] += end - start;
			  i++;	
			  endSubTask(monitor);
		  }
	  }
	  
	  private void analyzeGranules(final Collection<Q> toAnalyze) {
		  if (runInParallel() == ConcurrencyType.EXTERNALLY) {
			  final Procedure<Q> proc = getWorkProcedure();
			  for (final Q granule : toAnalyze) {
				  proc.op(granule);
			  }
		  } else {			  			  
			  queueWork(toAnalyze);
		  }		  
	  }
	  		  
	  private void execute(List<FlowUnitGranule> granules, List<FlowUnitVisitor<?>> subVisitors) {
		  for(FlowUnitGranule g : granules) {		  
			  // TODO setup versioning, assumptions, component locking
			  for(FlowUnitVisitor<?> sv : subVisitors) {
				  // TODO timing
				  
				  // a.doAnalysisOnGranule(env, granule) ->
				  g.execute(sv);
			  }
		  }
	  }
  }
  
  private static void recordFilesAnalyzed(ParallelArray<SourceCUDrop> allCus, File log) {
    System.out.println("Recording which files actually got (re-)analyzed");
    try {
      final PrintWriter pw = new PrintWriter(log);
      for (SourceCUDrop cud : allCus.asList()) {
        final String primaryType = JavaNames.genPrimaryTypeName(cud.getCompilationUnitIRNode());
        pw.println(primaryType);
      }
      pw.close();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Check if we have all the files that we expect
   * 
   * @throws IOException
   */
  private static void checkForExpectedSourceFiles(ParallelArray<SourceCUDrop> allCus, File expected) {
    System.out.println("Checking source files expected for analysis");
    try {
      final Set<String> cus = RegressionUtility.readLinesAsSet(expected);
      for (SourceCUDrop cud : allCus.asList()) {
        final String primaryType = JavaNames.genPrimaryTypeName(cud.getCompilationUnitIRNode());
        if (!cus.remove(primaryType)) {
          throw new IllegalStateException("Building extra file: " + primaryType);
        }
      }
      if (!cus.isEmpty()) {
        throw new IllegalStateException("File not analyzed: " + cus.iterator().next());
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Adds default constructors, calls to super(), and implicit Enum methods
 * @param loader 
   */
  private static void rewriteCUs(Projects projects, final List<CodeInfo> cus, SLProgressMonitor monitor, final JavacClassParser loader) 
  throws IOException {
    final Map<ITypeEnvironment, JavaRewrite> rewrites = new HashMap<ITypeEnvironment, JavaRewrite>();
    // int binaryRewrites = 0;
    startSubTask(monitor, "Rewriting CUs");

    // Init the list of binders
    final List<JavacTypeEnvironment.Binder> binders = new ArrayList<JavacTypeEnvironment.Binder>();
    for (JavacProject p : projects) {
      final JavacTypeEnvironment tEnv = p.getTypeEnv();
      rewrites.put(tEnv, new JavaRewrite(tEnv));

      final UnversionedJavaBinder b = tEnv.getBinder();
      binders.add((JavacTypeEnvironment.Binder) b);
    }

    final Map<IRNode,CodeInfo> infoMap = new HashMap<IRNode,CodeInfo>(cus.size());
    for(CodeInfo info : cus) {
    	infoMap.put(info.getNode(), info);
    }
    final ICompUnitListener refHandler = new ICompUnitListener() {
		public void astsChanged() {
			try {
				loader.checkReferences(cus);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}		
		public void astChanged(IRNode cu) {
			try {
				CodeInfo info = infoMap.get(cu);
				if (info == null) {
					throw new NullPointerException("Couldn't find "+cu);
				}
				loader.checkReferences(info);	
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	};
    IDE.getInstance().addCompUnitListener(refHandler);
	
    for (CodeInfo info : cus) {
      if (monitor.isCanceled()) {
        throw new CancellationException();
      }
      if (info.getFile().getRelativePath() != null) {
        System.out.println("Rewriting " + info.getFile().getRelativePath());
      }
      final IRNode cu = info.getNode();
      IRNode type = VisitUtil.getPrimaryType(cu);
      if (type == null) {
        // package-info.java?
        continue;
      }      
      if (info.getFileName().endsWith("DrawApplet.java")) {
    	  System.out.println("Found DrawApplet");
      }      
      /*
       * if (JavaNode.getModifier(cu, JavaNode.AS_BINARY)) {
       * //System.out.println("Skipping  "+JavaNames.getFullTypeName(type));
       * //continue; } else { if
       * (info.getFileName().endsWith("NestedTest.java")) {
       * System.out.println("Rewriting "+info.getFileName()); } }
       */
      JavaRewrite rewrite = rewrites.get(info.getTypeEnv());
      boolean changed = rewrite.ensureDefaultsExist(cu);
      if (changed) {
        if (debug) {
          System.out.println("Rewriting     " + JavaNames.getFullTypeName(type));
        }
        // Need to clear out state from all the binders
        for (UnversionedJavaBinder b : binders) {
          b.astChanged(cu);
        }
        /*
         * Worst case for(JavacTypeEnvironment.Binder b : binders) { b.reset();
         * }
         */

        /*
         * if (JavaNode.getModifier(cu, JavaNode.AS_BINARY)) { binaryRewrites++;
         * }
         */
        loader.checkReferences(info);
      } else if (debug) {
        System.out.println("NOT rewriting " + JavaNames.getFullTypeName(type));
      }
    }
    IDE.getInstance().removeCompUnitListener(refHandler);
    JavaMemberTable.clearAll();
    endSubTask(monitor);
  }

  static abstract class MonitoredProcedure<T> implements Procedure<T> {
	  private SLProgressMonitor monitor;
	  private Projects projects;
	  void setMonitor(SLProgressMonitor mon) {
		  monitor = mon;
	  }	  
	  void setProjects(Projects p) {
		  projects = p;
	  }
	  Projects getProjects() {
		  return projects;
	  }
	  public void op(T info) {
		  if (monitor != null && monitor.isCanceled()) {
			     throw new CancellationException();
		  }
		  process(info);
	  }
	  protected abstract void process(T info);
  }
  
  static final ConcurrentMap<JavacTypeEnvironment, JavaCanonicalizer> canonicalizers = new ConcurrentHashMap<JavacTypeEnvironment, JavaCanonicalizer>();
  static JavaCanonicalizer getCanonicalizer(CodeInfo info) {
	  final JavacTypeEnvironment tEnv = (JavacTypeEnvironment) info.getTypeEnv();
	  JavaCanonicalizer rv = canonicalizers.get(tEnv);
	  if (rv == null) {	  
		  final UnversionedJavaBinder b = tEnv.getBinder();
		  final JavaCanonicalizer jcanon = new JavaCanonicalizer(b);
		  rv = canonicalizers.putIfAbsent(tEnv, jcanon);
		  if (rv == null) {
			  rv = jcanon;
		  }
	  }
	  return rv;
  }
  
  static final MonitoredProcedure<CodeInfo> bindProc = new MonitoredProcedure<CodeInfo>() {
	  @Override
	  protected void process(CodeInfo info) {
        if (!info.isAsSource()) {
          /*
           * IRNode type = VisitUtil.getPrimaryType(info.getNode()); String
           * unparse = DebugUnparser.toString(type); if
           * (unparse.contains("Deprecated")) {
           * System.out.println("Deprecated in "+JavaNames.getFullName(type)); }
           */
          return; // Nothing to do on class files
        }
        if (batchAndCacheBindingsForCanon) {
        	
        }
        final JavacTypeEnvironment tEnv = (JavacTypeEnvironment) info.getTypeEnv();
        final UnversionedJavaBinder b = tEnv.getBinder();
        b.bindCompUnit(info.getNode(), info.getFileName());
      }
  };

  static final MonitoredProcedure<CodeInfo> canonProc = new MonitoredProcedure<CodeInfo>() {
	  @Override
	  protected void process(CodeInfo info) {
		  if (!info.isAsSource()) {
			  return; // Nothing to do on class files
		  }
		  final IRNode cu = info.getNode();
		  final IRNode type = VisitUtil.getPrimaryType(cu);
		  final String typeName = info.getFileName();
		  try {
			  final long start = System.currentTimeMillis();
			  final Nodes nodes = findNoncanonical(cu);
			  final long find = System.currentTimeMillis();
			  /*
			   * Not quite right, since it will miss (un)boxing and the like if
			   * (noncanonical.isEmpty()) { return; // Nothing to do }
			   */
			  final JavacTypeEnvironment tEnv = (JavacTypeEnvironment) info.getTypeEnv();
			  final UnversionedJavaBinder b = tEnv.getBinder();
			  // TODO needs to be shared, so I can preload the caches
			  final JavaCanonicalizer jcanon = new JavaCanonicalizer(b);
			  boolean changed = jcanon.canonicalize(cu);
			  final long restart = System.currentTimeMillis();
			  if (changed) {
				  if (debug) {
					  System.out.println("Canonicalized     " + typeName);
				  }
				  //b.astChanged(cu);
				  // TODO will this work if run in parallel?
				  for (JavacProject jp : getProjects()) {
					  jp.getTypeEnv().getBinder().astChanged(cu);
				  }
			  } else if (debug) {
				  System.out.println("NOT canonicalized " + typeName);
			  }
			  destroyNoncanonical(nodes);

			  final long destroy = System.currentTimeMillis();
			  findTime += (find-start);
			  destroyTime += (destroy-restart);
		  } catch (Throwable t) {
			  LOG.log(Level.SEVERE, "Exception while processing " + type, t);
		  }
	  }
  };
  
  private static void canonicalizeCUs(JSurePerformance perf, final ParallelArray<CodeInfo> cus, final Projects projects) {
    final SLProgressMonitor monitor = projects.getMonitor();
    if (monitor.isCanceled()) {
      throw new CancellationException();
    }
    AbstractJavaBinder.printStats();
    startSubTask(monitor, "Canonicalizing ASTs");
    
    // Init procedures
    bindProc.setMonitor(monitor);
    canonProc.setMonitor(monitor);
    canonProc.setProjects(projects);
    long bindingTime = 0;
    if (batchAndCacheBindingsForCanon) {
    	final ParallelArray<CodeInfo> temp = perf.createArray(CodeInfo.class);    	
    	for(CodeInfo i : cus) {
    		temp.asList().add(i);
    		if (temp.size() > 100) {
    			bindingTime += doCanonicalize(temp, false);
    			temp.asList().clear();
    		}
    	}
    	if (!temp.isEmpty()) {
    		bindingTime += doCanonicalize(temp, false);
    	}
    } else {
    	bindingTime = doCanonicalize(cus, true);
    }
	perf.setLongProperty("Binding.before.canon", bindingTime);
	System.out.println("Binding = " + bindingTime + " ms");  	
    SlotInfo.gc();
    endSubTask(monitor);
  }

  /**
   * Assumes that init is all done
   * @return the time taken for binding
   */  
  private static long doCanonicalize(ParallelArray<CodeInfo> cus, boolean printBinderStats) {
	  // Precompute all the bindings first
	  final long start = System.currentTimeMillis();
	  cus.apply(bindProc);
	  final long end = System.currentTimeMillis();
	  if (printBinderStats) {
		  AbstractJavaBinder.printStats();
	  }
	  // cus.apply(proc);
	  for (final CodeInfo info : cus) {
	      if (info.getFile().getRelativePath() != null) {
	        System.out.println("Canonicalizing " + info.getFile().getRelativePath());
	      }
	      canonProc.op(info);
	  }   
	  return end-start;
  }
  
  static long destroyTime = 0, findTime = 0;
  static int destroyedNodes = 0, canonicalNodes = 0;//, diffNodes = 0;
  static int decls = 0, stmts = 0, blocks = 0;

  static class Nodes {
	  //final Set<IRNode> original = new HashSet<IRNode>();
	  final List<IRNode> noncanonical = new ArrayList<IRNode>();
	  final IRNode cu;
	  
	  Nodes(IRNode cu) {
		this.cu = cu;
	  }
  }
  
  private static Nodes findNoncanonical(IRNode cu) {
	Nodes rv = new Nodes(cu);
    for (IRNode n : JJNode.tree.topDown(cu)) {
      //rv.original.add(n);
      
      Operator op = JJNode.tree.getOperator(n);
      if (op instanceof IllegalCode) {
        rv.noncanonical.add(n);
      } else {
        // FIX these aren't all of them
        canonicalNodes++;
      }
      if (Declaration.prototype.includes(op) || ClassBodyDeclaration.prototype.includes(op)) {
        decls++;
      }
      if (Statement.prototype.includes(op)) {
        stmts++;
        if (BlockStatement.prototype.includes(op)) {
          blocks++;
        }
      }
    }
    return rv;
  }

  private static void destroyNoncanonical(Nodes nodes) {
	/*
	for (IRNode n : JJNode.tree.topDown(nodes.cu)) {
		nodes.original.remove(n);
	}
	final int origSize = nodes.original.size();
	*/
	final int noncanonSize = nodes.noncanonical.size();
	/*
	if (origSize != noncanonSize) {
		//System.out.println("Found "+origSize+" nodes vs. "+noncanonSize+" noncanonical");
		diffNodes += (origSize - noncanonSize);
	}
	*/
    destroyedNodes += noncanonSize;

    for (IRNode n : nodes.noncanonical) {
      SkeletonJavaRefUtility.removeInfo(n);	
      n.destroy();
    }
  }

  @SuppressWarnings("deprecation")
  private static void addRequired(ParallelArray<CodeInfo> cus, final SLProgressMonitor monitor) {
    startSubTask(monitor, "Adding required nodes");
    Procedure<CodeInfo> proc = new Procedure<CodeInfo>() {
      @Override
      public void op(CodeInfo info) {
        final ITypeEnvironment tEnv = info.getTypeEnv();
        if (monitor.isCanceled()) {
          throw new CancellationException();
        }
        final IRNode cu = info.getNode();
        PromiseUtil.activateRequiredCuPromises(tEnv.getBinder(), tEnv.getBindHelper(), cu);
      }
    };
    cus.apply(proc);
    /*
    for (final CodeInfo info : cus) {
      proc.op(info);
    }
    */
    endSubTask(monitor);
  }

  private static void parsePromises(ParallelArray<CodeInfo> cus, final SLProgressMonitor monitor) {
    ParseUtil.init();

    startSubTask(monitor, "Parsing promises");
    // final File root = new
    // File(IDE.getInstance().getStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY));
    Procedure<CodeInfo> proc = new Procedure<CodeInfo>() {
      @Override
      public void op(CodeInfo info) {
        if (monitor.isCanceled()) {
          throw new CancellationException();
        }
        final IRNode cu = info.getNode();
        if (cu.identity() == IRNode.destroyedNode) {
          System.out.println("No node for " + info.getFileName());
          return;
        }
        // final ISrcRef ref = JavaNode.getSrcRef(cu);
        String name = null;
        /*
         * if (ref != null) { ref.getRelativePath(); }
         */
        if (name == null) {
          name = JavaNames.genPrimaryTypeName(cu);
        }
        if (name == null) {
          if (!info.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA)) {
            return;
          }
          name = info.getFile().getPackage();
        }
        // testIDecls(info);

        // Add the Static region before anything else (even All?)
        final AnnotationVisitor v = new AnnotationVisitor(info.getTypeEnv(), name);
        for (IRNode type : VisitUtil.getAllTypeDecls(cu)) {
          final String qname = JavaNames.getFullTypeName(type);
          /*
           * if (
           * "region.accessibility.samePackage.DefaultSuper.Inner_ParentIsDefaultSuper"
           * .equals(qname)) {
           * System.out.println("Checking Inner_ParentIsDefaultSuper"); }
           */
          final Operator op = JJNode.tree.getOperator(type);
          if (op instanceof AnonClassExpression || op instanceof TypeFormal) {
            continue;
          }
          /*
           * Removed due to bug in Javac -- missing static modifier if (op
           * instanceof NestedDeclInterface && !JavaNode.getModifier(type,
           * JavaNode.STATIC)) { // These can't have static fields continue; }
           */
          if (op instanceof EnumConstantClassDeclaration) {
            continue;
          }
          if (insideOfMethod(type)) {
            continue;
          }
          if (SLUtility.JAVA_LANG_OBJECT.equals(name)) {
            v.handleImplicitPromise(type, RegionRules.REGION, "public static All", Collections.<String, String> emptyMap());
          }
          v.handleImplicitPromise(type, RegionRules.REGION, "public static Static extends All", Collections.<String, String> emptyMap());
        }

        // Process any pre-existing package-level scoped promises?
        // (If the package is reprocessed, there shouldn't be any promises on it
        // here)
        final PackageDrop pkg = PackageDrop.findPackage(info.getFile().getPackage());
        if (pkg != null) {
          final IRNode decl = CompilationUnit.getPkg(pkg.getCompilationUnitIRNode());
          for (PromisePromiseDrop sp : ScopedPromiseRules.getScopedPromises(decl)) {
            for (IRNode type : VisitUtil.getTypeDecls(cu)) {
              ScopedPromiseRules.applyPromiseOnType(type, sp);
            }
          }
        }

        // Visit the source, checking for annotations
        int num = v.doAccept(cu);
        final JavacProject p = Projects.getProject(cu);
        int fromXML = 0;
        /*
         * if (root != null) { // Try from the user-customizable location first
         * try { fromXML = TestXMLParser.process(p.getTypeEnv(), root, cu,
         * name+TestXMLParserConstants.SUFFIX); } catch (Exception e) {
         * handleException(name, e); } } if (fromXML == 0) { // Otherwise, use
         * our XML try { fromXML = TestXMLParser.process(p.getTypeEnv(), cu,
         * name+TestXMLParserConstants.SUFFIX); } catch (Exception e) {
         * handleException(name, e); } }
         */
        try {
          fromXML = PromisesXMLParser.process(p.getTypeEnv(), cu, name.replace('.', '/') + TestXMLParserConstants.SUFFIX);
        } catch (Exception e) {
          handleException(name, e);
        }
        num += fromXML;

        if (num > 0) {
          System.out.println("Added " + num + " promises for " + name + " in " + p.getName() + ": " + VisitUtil.getPrimaryType(cu));
          /*
           * } else if (info.getFileName().endsWith(".java")) {
           * System.out.println
           * ("No promises found for "+name+" in "+p.getName());
           */
        }
        /*
         * The model won't show up yet, since it hasn't been scrubbed yet if
         * (SLUtility.JAVA_LANG_OBJECT.equals(name)) { RegionModel m =
         * RegionModel.getInstance(name, p.getName()); if (m.getNode() == null)
         * { SLLogger.getLogger().severe(
         * "RegionModel for java.lang.Object has null node"); } }
         */
      }

      private void handleException(String name, Exception e) {
        if (!(e instanceof FileNotFoundException)) {
          LOG.log(Level.SEVERE, "Unexpected exception", e);
        } else if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Couldn't find file " + name + TestXMLParserConstants.SUFFIX);
        }
      }
    };
    cus.apply(proc);
    /*
     * for (final CodeInfo info : cus) { proc.op(info); }
     */
    endSubTask(monitor);
  }

  private static void testIDecls(CodeInfo info) {
    final DeclFactory factory = new DeclFactory(info.getTypeEnv().getBinder());
    for (IRNode n : JJNode.tree.topDown(info.getNode())) { // what about
                                                           // receivers/return
                                                           // values?
      Pair<IDecl, IJavaRef.Position> p = factory.getDeclAndPosition(n);
      if (p == null) {
        continue;
      }
      String encode = Decl.encodeForPersistence(p.first());
      IDecl decode = Decl.parseEncodedForPersistence(encode);
      if (!decode.getKind().equals(p.first().getKind()) || !decode.getName().equals(p.first().getName())) {
        throw new IllegalStateException();
      }
    }
  }

  /**
   * @return true if the type is declared somewhere inside of a method
   */
  protected static boolean insideOfMethod(IRNode type) {
    IRNode here = VisitUtil.getEnclosingClassBodyDecl(type);
    while (here != null) {
      Operator op = JJNode.tree.getOperator(here);
      if (!(op instanceof NestedDeclInterface)) {
        return true;
      }
      here = VisitUtil.getEnclosingClassBodyDecl(here);
    }
    return false;
  }

  static void startSubTask(SLProgressMonitor monitor, String msg) {
    if (monitor == null) {
      System.out.println("null monitor");
      return;
    }
    System.out.println(msg);
    monitor.subTask(msg);
  }

  static void endSubTask(SLProgressMonitor monitor) {
    monitor.subTaskDone();
    monitor.worked(1);
  }

  private static Dependencies checkDependencies(final ParallelArray<CodeInfo> cus) {
    final Dependencies deps = new Dependencies() {
      @Override
      protected void handlePackage(final PackageDrop pkg) {
        /*
         * runVersioned(new AbstractRunner() { public void run() {
         * parsePackagePromises(pkg); } });
         */
        CodeInfo info = pkg.makeCodeInfo();
        if (info != null) {
          System.err.println("Reprocessing " + pkg.getJavaOSFileName());
          cus.asList().add(pkg.makeCodeInfo());
        }
      }

      @Override
      protected void handleType(CUDrop d) {
        // ConvertToIR.getInstance().registerClass(d.makeCodeInfo());
        System.err.println("Reprocessing " + d.getMessage());
        cus.asList().add(d.makeCodeInfo());
      }
    };
    for (CodeInfo info : new ArrayList<CodeInfo>(cus.asList())) {
      // TODO Check for package-info files
      // Check for sources
      if (info.getType() == Type.SOURCE) { // TODO what about interfaces?
        CUDrop d;
        if (info.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA)) {
          d = PackageDrop.findPackage(info.getFile().getPackage());
        } else {
          d = SourceCUDrop.queryCU(info.getFile());
        }
        deps.markAsChanged(d);
        /*
         * } else { System.out.println("Ignoring "+info.getFileName());
         */
      }
    }
    deps.finishReprocessing();
    return deps;
  }

  private static void createCUDrops(ParallelArray<CodeInfo> cus, final SLProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new CancellationException();
    }
    startSubTask(monitor, "Creating drops");
    sortCodeInfos(cus);
    /*
     * for(SourceCUDrop cud :
     * Sea.getDefault().getDropsOfExactType(SourceCUDrop.class)) {
     * System.out.println("Source: "+cud.javaOSFileName); }
     */
    Procedure<CodeInfo> proc = new Procedure<CodeInfo>() {
      @Override
      public void op(CodeInfo info) {
        if (monitor.isCanceled()) {
          throw new CancellationException();
        }
        if (info.getNode().identity() == IRNode.destroyedNode) {
          LOG.info("WARNING Already destroyed: " + info.getFileName());
          return;
        }
        // invalidate past results on this java file
        final ICodeFile file = info.getFile();
        CUDrop outOfDate = null;
        switch (info.getType()) {
        case SOURCE:
        case INTERFACE:
          if (info.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA)) {
            // System.out.println("Found package: "+info.getFileName());
            outOfDate = PackageDrop.findPackage(file.getPackage());
          } else {
            // System.out.println("Found source:  "+info.getFileName());
            outOfDate = SourceCUDrop.queryCU(file);
          }
          break;
        case BINARY:
          outOfDate = BinaryCUDrop.queryCU(file.getProjectName(), info.getFileName());
        default:
        }

        if (outOfDate != null) {
          if (outOfDate.getCompilationUnitIRNode().identity() != IRNode.destroyedNode
              && outOfDate.getCompilationUnitIRNode().equals(info.getNode())) {
            // Same IRNode, so keep this drop
            System.out.println("Keeping the old drop for " + outOfDate.getJavaOSFileName());
            return;
          }
          // if (AbstractWholeIRAnalysis.debugDependencies) {
          // System.out.println("Invalidating "+outOfDate+": "+
          // Projects.getProject(outOfDate.getCompilationUnitIRNode())+" -> "+
          // Projects.getProject(info.getNode()));
          // if (!(outOfDate instanceof PackageDrop)) {
          // System.out.println("Found "+outOfDate);
          // }
          // }
          // System.out.println("Destroying "+outOfDate.javaOSFileName);
          AdapterUtil.destroyOldCU(outOfDate.getCompilationUnitIRNode());
          outOfDate.invalidate();
        } else {
          // System.out.println("Couldn't find: "+info.getFile());
        }
        // System.out.println("Creating drop: "+info.getFileName());

        if (info.getType().fromSourceFile()) {
          if (info.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA)) {
            final JavacTypeEnvironment tEnv = (JavacTypeEnvironment) info.getTypeEnv();
            tEnv.addPackage(info.getFile().getPackage(), info.getNode());
            // PackageDrop.createPackage(info.getFile().getPackage(),
            // info.getNode());
          } else {
            new SourceCUDrop(info);
            System.out.println("Created source drop for " + info.getFileName());
          }
        } else {
          new BinaryCUDrop(info);
          System.out.println("Created binary drop for " + info.getFileName());
        }
        if (debug) {
          System.out.println("Created drop for " + info.getFileName());
        }
      }
    };
    cus.apply(proc);
    /*
    for (final CodeInfo info : cus) {
      proc.op(info);
    }
    */
    endSubTask(monitor);
  }

  private static void sortCodeInfos(ParallelArray<CodeInfo> cus) {
    // Required to make sure that we process package-info files first
    Collections.sort(cus.asList(), new Comparator<CodeInfo>() {
      @Override
      public int compare(CodeInfo o1, CodeInfo o2) {
        if (o1.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA)) {
          if (o2.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA)) {
              return o1.getFileName().compareTo(o2.getFileName());
          }
          return Integer.MIN_VALUE;
        }
        if (o2.getFileName().endsWith(SLUtility.PACKAGE_INFO_JAVA)) {
          return Integer.MAX_VALUE;
        }
        return o1.getFileName().compareTo(o2.getFileName());
      }
    });
    System.out.println("Done sorting CodeInfos");
  }

  private static void scrubPromises(List<CodeInfo> cus, SLProgressMonitor monitor) {
    startSubTask(monitor, "Scrubbing promises");
    AnnotationRules.scrub();
    ParseUtil.clear();
    endSubTask(monitor);
  }

  public static void main(String[] args) {
    Demo which = DEFAULT_DEMO;
    if (args.length > 0) {
      which = Demo.get(args[0]);
    }
    final File location;
    switch (which) {
    case TEST:
      location = new File(WORKSPACE + "/test");
      break;
    case COMMON:
    default:
      location = new File(WORKSPACE + "/common");
      break;
    case FLUID:
      location = new File(WORKSPACE + "/fluid");
      break;
    case JDK6:
      location = new File(WORK + "/jdk6-workspace/jdk6");
      break;
    case JEDIT:
      location = new File(WORKSPACE + "/JEdit");
      break;
    case SMALL_WORLD:
      location = new File(WORKSPACE + "/SmallWorld");
      break;
    }
    final Config config = new Config(which.name(), location, false, which == Demo.JDK6);
    switch (which) {
    case TEST:
    case COMMON:
    case SMALL_WORLD:
    default:
      addJavaFiles(new File(location, "src"), config);
      break;
    case FLUID:
      addJavaFiles(new File(WORKSPACE + "/common"), config, "common");
      addJavaFiles(new File(WORKSPACE + "/jsure-message"), config, "jsure-message");
      addJavaFiles(new File(WORKSPACE + "/fluid/src"), config);
      addJavaFiles(new File(WORKSPACE + "/fluid/gensrc"), config);
      break;
    case JDK6:
      addJavaFiles(new File(WORK + "/jdk6-workspace/jdk6"), config);
      break;
    case JEDIT:
      addJavaFiles(new File(WORKSPACE + "/JEdit"), config, new File(WORKSPACE + "/JEdit/jars"), new File(WORKSPACE
          + "/JEdit/jeditshell"), new File(WORKSPACE + "/JEdit/doclet"));
      break;
    }
    try {
      openFiles(which, config);
    } catch (Throwable e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      System.exit(0);
    }
  }

  public static class ExcludeFilter implements FileFilter {
    final File[] excluded;

    public ExcludeFilter(File... excluded) {
      this.excluded = excluded;
    }

    @Override
    public boolean accept(File f) {
      if (f.isDirectory()) {
        for (File exclude : excluded) {
          if (f.equals(exclude)) {
            System.out.println("Excluded: " + f);
            return false;
          }
        }
      }
      return true;
    }
  }

  static class NullFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
      return true;
    }
  }

  static final NullFilter nullFilter = new NullFilter();

  public static void addJavaFiles(File loc, Config config, String dependentProject) {
    Config dep = new Config(dependentProject, loc, true, false);
    File dir = new File(loc, "src");
    addJavaFiles("", dir, dep, nullFilter);
    config.addToClassPath(dep);
  }

  public static void addJavaFiles(File dir, Config config, File... excluded) {
    if (excluded.length > 0) {
      addJavaFiles("", dir, config, new ExcludeFilter(excluded));
    } else {
      addJavaFiles("", dir, config, nullFilter);
    }
  }

  public static void addJavaFiles(File dir, Config config, FileFilter filter) {
    addJavaFiles("", dir, config, filter);
  }

  private static void addJavaFiles(String pkg, File dir, Config config, FileFilter filter) {
    if (!filter.accept(dir)) {
      return;
    }
    if (dir == null || !dir.exists()) {
      return;
    }
    // System.out.println("Scanning "+dir.getAbsolutePath());
    boolean added = false;
    for (File f : dir.listFiles()) {
      if (f.getName().endsWith(".java") && filter.accept(f)) {
        System.out.println("Found source file: " + f.getPath());
        String typeName = f.getName().substring(0, f.getName().length() - 5);
        String qname = pkg.length() == 0 ? typeName : pkg + '.' + typeName;
        config.addFile(new JavaSourceFile(qname, f, f.getAbsolutePath(), false, "(unknown)"));
        if (!added) {
          added = true;
          if (debug) {
            System.out.println("Found java files in " + pkg);
          }
          config.addPackage(pkg);
        }
      }
      if (f.isDirectory()) {
        final String newPkg = pkg == "" ? f.getName() : pkg + '.' + f.getName();
        addJavaFiles(newPkg, f, config, filter);
      }
    }
  }
  /*
   * static void browseAndOpen() { JFrame frame = new JFrame(); JFileChooser jfc
   * = new JFileChooser(); jfc.setFileSelectionMode(JFileChooser.FILES_ONLY); if
   * (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) { try {
   * openFiles(Collections.singletonList(jfc.getSelectedFile())); } catch
   * (Exception e) { // TODO Auto-generated catch block e.printStackTrace(); }
   * finally { System.exit(0); } } }
   */
}
