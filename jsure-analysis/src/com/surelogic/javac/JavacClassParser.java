package com.surelogic.javac;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.surelogic.Nullable;
import com.surelogic.analysis.IIRProject;
import com.surelogic.com.sun.source.tree.CompilationUnitTree;
import com.surelogic.com.sun.source.util.JavacTask;
import com.surelogic.com.sun.source.util.Trees;
import com.surelogic.com.sun.tools.javac.api.JavacTool;
import com.surelogic.com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.concurrent.ParallelArray;
import com.surelogic.common.concurrent.Procedure;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.java.Config;
import com.surelogic.common.java.Config.Type;
import com.surelogic.common.java.IClassPathEntry;
import com.surelogic.common.java.IJavaFile;
import com.surelogic.common.java.JavaClassPath;
import com.surelogic.common.java.JavaSourceFile;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.adapter.ClassAdapter;
import com.surelogic.javac.adapter.SourceAdapter;
import com.surelogic.javax.lang.model.element.Element;
import com.surelogic.javax.tools.Diagnostic;
import com.surelogic.javax.tools.DiagnosticListener;
import com.surelogic.javax.tools.JavaFileManager.Location;
import com.surelogic.javax.tools.JavaFileObject;
import com.surelogic.javax.tools.JavaFileObject.Kind;
import com.surelogic.javax.tools.StandardJavaFileManager;
import com.surelogic.javax.tools.StandardLocation;
import com.surelogic.xml.PackageAccessor;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeHashedMap;
import edu.cmu.cs.fluid.ir.MarkedIRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ICodeFile;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.Annotation;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.DemandName;
import edu.cmu.cs.fluid.java.operator.NameExpression;
import edu.cmu.cs.fluid.java.operator.NameType;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.QualifiedName;
import edu.cmu.cs.fluid.java.operator.SimpleName;
import edu.cmu.cs.fluid.java.operator.UnsupportedJava8Feature;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Triple;

public final class JavacClassParser extends JavaClassPath<Projects> {
  static Logger LOG = SLLogger.getLogger();

  /** Should we try to run things in parallel */
  static boolean wantToRunInParallel = true;

  static final String[] sourceLevels = { "1.5" /* default */, "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "8" };

  static final boolean debug = Util.debug;
  final boolean loadAllLibraries = XUtil.loadAllLibs || IDE.getInstance().getBooleanPreference(IDEPreferences.LOAD_ALL_CLASSES);

  // ToolProvider.getSystemJavaCompiler();
  // private static final JavaCompiler compiler = JavacTool.create();

  static final Iterable<? extends File> tmpDir = Collections.singleton(new File(System.getProperty("java.io.tmpdir"))); // NOI18N

  public static final DiagnosticListener<JavaFileObject> nullListener = new DiagnosticListener<JavaFileObject>() {
    @Override
    public void report(Diagnostic<? extends JavaFileObject> d) {
      System.out.println("JCP: " + d);
    }
  };

  // Key: project
  private final Map<String, BatchParser> parsers = new HashMap<>();

  private final Map<File, File> mappedSources = new ConcurrentHashMap<>();

  private final Set<String> requiredRefs = new HashSet<>();

  // proj, qname, zip
  final ParallelArray<Triple<String, String, ZipFile>> jarRefs;

  public JavacClassParser(Projects p) throws IOException {
    super(p, false);
    jarRefs = new ParallelArray<>();

    for (JavacProject jp : p) {
      // System.out.println("Initializing "+jp.getName());
      parsers.put(jp.getName(), new BatchParser(jp, 100, jp.isAsBinary()));
      jp.collectMappedJars(mappedSources);
    }
  }

  public void ensureClassIsLoaded(String qname) {
    requiredRefs.add(qname);
  }

  private class BatchParser {
    final JavacProject jp;
    final JavacTypeEnvironment tEnv;
    final int max;
    final boolean asBinary;
    final ThreadLocal<SourceAdapter> adapter;
    final ParallelArray<CompilationUnitTree> cuts;
    final Queue<CodeInfo> cus;
    final References refs;
    final Map<JavaFileObject, JavaSourceFile> sources = new HashMap<>();

    // Use default charset
    final StandardJavaFileManager fileman = JavacTool.create().getStandardFileManager(nullListener, null, null);

    final boolean processJava8;

    public BatchParser(final JavacProject jp, int max, boolean asBinary) throws IOException {
      this.jp = jp;
      this.tEnv = jp.getTypeEnv();
      this.max = max;
      this.asBinary = asBinary;
      adapter = new ThreadLocal<SourceAdapter>() {
        @Override
        protected SourceAdapter initialValue() {
          return new SourceAdapter(projects, jp);
        }
      };
      cuts = new ParallelArray<>();
      cus = new ConcurrentLinkedQueue<>(); // Added to concurrently
      refs = new References(jp);

      fileman.setLocation(StandardLocation.CLASS_OUTPUT, tmpDir);
      fileman.setLocation(StandardLocation.CLASS_PATH, collectClasses(jp));
      // TODO What about other projects?
      processJava8 = jp.processJava8();
    }

    Iterable<File> collectClasses(JavacProject jp) {
      List<File> classpath = new ArrayList<>();
      for (IClassPathEntry cpe : jp.getConfig().getClassPath()) {
        File f = cpe.getFileForClassPath();
        if (f != null) {
          classpath.add(f);
        }
      }
      return classpath;
    }

    void parse(Iterable<JavaSourceFile> files, List<CodeInfo> results, boolean onDemand) throws IOException {
      // Eliminate duplicates
      // Issue w/ hashing on JaveFileObject - FIXED
      final Set<JavaFileObject> temp = new HashSet<>(max);
      for (JavaSourceFile p : files) {
        final CodeInfo info = jp.getTypeEnv().findCompUnit(p.qname);
        boolean load = !onDemand || info == null;
        if (load) {
          String path = p.file.toString();
          if (path.startsWith("jar:")) {
            final int bang = path.indexOf('!');
            final String zip = path.substring(path.startsWith("jar://") ? 5 : 4, bang);
            // To make it look like a qualified class name
            final String src = path.substring(bang + 1, path.length() - 5).replace('\\', '.');
            Location location = StandardLocation.locationFor(zip);
            fileman.setLocation(location, Collections.singletonList(new File(zip)));

            JavaFileObject jfo = fileman.getJavaFileForInput(location, src, Kind.SOURCE);
            if (jfo == null) {
              LOG.warning("Couldn't get Java file for " + path);
              continue;
            }
            temp.add(jfo);
            mapSource(jfo, p);
          } else if (p.file.exists() && p.file.length() > 0) {
            for (JavaFileObject jfo : fileman.getJavaFileObjects(p.file)) {
              if (jfo == null) {
                LOG.warning("Couldn't get Java file for " + path);
                continue;
              }
              temp.add(jfo);
              mapSource(jfo, p);
            }
          }
        }
      }
      // Handle in batches
      final Iterator<JavaFileObject> fileI = temp.iterator();
      final List<JavaFileObject> batch = new ArrayList<>(max);

      while (fileI.hasNext()) {
        if (tEnv.getProgressMonitor().isCanceled()) {
          throw new CancellationException();
        }
        batch.add(fileI.next());

        if (batch.size() >= max) {
          parseBatch(batch, results, asBinary);
          batch.clear();
        }
      }
      if (!batch.isEmpty()) {
        parseBatch(batch, results, asBinary);
      }
      for (CodeInfo cu : results) {
        if (usesUnsupportedJava8Features(cu.getNode())) {
          throw new CancellationException(I18N.msg("jsure.temp.java8.dialog", cu.getFile().getRelativePath()));
        }
      }
    }

    boolean usesUnsupportedJava8Features(IRNode cu) {
      if (processJava8) {
        return false;
      }
      for (IRNode n : JJNode.tree.topDown(cu)) {
        Operator op = JJNode.tree.getOperator(n);
        if (op instanceof UnsupportedJava8Feature) {
          return true;
        }
      }
      return false;
    }

    void mapSource(JavaFileObject w, JavaSourceFile p) {
      sources.put(w, p);
    }

    @SuppressWarnings("unused")
    private void printStream(String name, InputStream is) throws IOException {
      LineNumberReader r = new LineNumberReader(new InputStreamReader(is));
      String line = null;
      while ((line = r.readLine()) != null) {
        System.out.println(name + ": " + line);
      }
      r.close();
    }

//    void parseViaForkJoin(Iterable<JavaFileObject> files, List<CodeInfo> results, final boolean asBinary)
//        throws IOException, InterruptedException, ExecutionException {
//      final JavacTask javac = initJavac(files);
//      final Trees t = Trees.instance(javac);
//      // System.out.println("Parsing sources");
//      Stack<AdaptTask> tasks = new Stack<>();
//      final ForkJoinPool pool = new ForkJoinPool(ConcurrentAnalysis.getThreadCountToUse());
//      try {
//        for (final CompilationUnitTree cut : javac.parse()) {
//          if (debug) {
//            System.out.println("Parsing " + cut.getSourceFile().getName());
//          }
//          tEnv.addPackage(SourceAdapter.getPackage(cut), this.asBinary ? Config.Type.INTERFACE : Config.Type.SOURCE);
//          final AdaptTask task = new AdaptTask(t, cut);
//          pool.submit(task);
//          tasks.push(task);
//        }
//        while (!tasks.isEmpty()) {
//          final AdaptTask at = tasks.pop();
//          final CodeInfo info = at.get();
//          tEnv.addCompUnit(info, true);
//          results.add(info);
//        }
//        timeAnalysis(javac);
//      } finally {
//        pool.shutdown();
//      }
//    }

    private void timeAnalysis(final JavacTask javac) throws IOException {
      if (true) {
        return;
      }
      long start = System.currentTimeMillis();
      for (Element e : javac.analyze()) {
        System.out.println("Analyzed " + e.getSimpleName());
      }
      long end = System.currentTimeMillis();
      System.out.println("Analysis time: " + (end - start) + " ms");
    }

    class AdaptTask extends RecursiveTask<CodeInfo> {
      private static final long serialVersionUID = 1L;
      final Trees trees;
      @Nullable
      CompilationUnitTree cut;

      AdaptTask(Trees t, CompilationUnitTree c) {
        trees = t;
        cut = c;
      }

      @Override
      protected CodeInfo compute() {
        try {
          return adaptCompUnit(trees, cut);
        } finally {
          cut = null;
        }
      }
    }

    private JavacTask initJavac(Iterable<JavaFileObject> files) {
      /*
       * for(File f : files) { printStream(f.getName(), new FileInputStream(f));
       * }
       */
      // Iterable <? extends JavaFileObject> toCompile =
      // fileman.getJavaFileObjectsFromFiles(files);
      // FIX what do the arguments do?
      List<String> options = new ArrayList<>();
      options.add("-source");
      final int level = tEnv.getProject().getConfig().getIntOption(Config.SOURCE_LEVEL);
      // System.out.println("Parsing files from "+jp.getName()+" at level
      // "+level);
      if (level < sourceLevels.length) {
        options.add(sourceLevels[level]);
      } else {
        throw new IllegalArgumentException("Unknown source level: " + level);
      }
      // System.out.println(tEnv.getProject().getName()+" is set to source level
      // "+sourceLevels[level]+" -- "+level);
      options.add("-printsource");
      final JavacTask task = (JavacTask) JavacTool.create().getTask(null, // Output
                                                                          // to
                                                                          // System.err
          fileman, nullListener, options, null, // Classes for anno processing
          files);
      return task;
    }

    void parseBatch(Iterable<JavaFileObject> files, List<CodeInfo> results, final boolean asBinary) throws IOException {
      final JavacTask task = initJavac(files);
      try {
        // Requires mapping Tree to Element
        // task.getElements().getDocComment(e);

        cuts.asList().clear();
        // System.out.println("Parsing sources");
        for (CompilationUnitTree cut : task.parse()) {
          if (debug) {
            System.out.println("Parsing " + cut.getSourceFile().getName());
          }
          tEnv.addPackage(SourceAdapter.getPackage(cut), this.asBinary ? Config.Type.INTERFACE : Config.Type.SOURCE);

          // Scanning before adding these in
          // scanForReferencedTypes(refs, cut);
          cuts.asList().add(cut);
          tEnv.getProgressMonitor().worked(1);
        }
        timeAnalysis(task);

        final Trees t = Trees.instance(task);
        cus.clear();
        System.out.println("Adapting " + cuts.asList().size() + " CUTs");
        Procedure<CompilationUnitTree> proc = new Procedure<CompilationUnitTree>() {
          @Override
          public void op(CompilationUnitTree cut) {
            CodeInfo info = adaptCompUnit(t, cut);
            cus.add(info);
          }
        };
        if (wantToRunInParallel) {
          cuts.apply(proc);
        } else {
          for (CompilationUnitTree cut : cuts.asList()) {
            proc.op(cut);
          }
        }
        cuts.asList().clear();
        tEnv.addCompUnits(cus, true);
        results.addAll(cus);
        cus.clear();
      } catch (Error e) {
        e.printStackTrace(System.out);
        throw e;
      } finally {
        // task.finish();
      }
    }

    CodeInfo adaptCompUnit(final Trees t, final CompilationUnitTree cut) {
      if (debug) {
        System.out.println("Adapting " + cut.getSourceFile().getName());
      }
      // PlainIRNode.setCurrentRegion(new IRRegion());

      JCCompilationUnit jcu = (JCCompilationUnit) cut;
      JavaSourceFile file = sources.get(jcu.sourcefile);
      CodeInfo info = adapter.get().adapt(t, jcu, file, asBinary || file.asBinary);
      // cus.add(info);
      Projects.setProject(info.getNode(), jp);
      // System.out.println("Done adapting "+info.getFileName());
      return info;
    }

	void clearRefs() {
	  refs.clearRefs();
	}

	void addUnassociatedRefs(Iterable<String> qnames) {
	  if (qnames != null) {
		refs.addAll(qnames);
	  }
	}
  }

  public void parse(final List<CodeInfo> results) throws IOException {
    System.out.println("Assuming that the projects are run in dependency order");
    // TODO otherwise we could load something twice
    final long start = System.currentTimeMillis();
    final Multimap<String, CodeInfo> infos = ArrayListMultimap.create();
    for (JavacProject jp : projects) {
      final List<CodeInfo> temp = new ArrayList<>();
      // final BatchParser parser =
      parseSources(jp, temp);

      // Separate parsed files by project
      for (CodeInfo info : temp) {
        infos.put(info.getTypeEnv().getProject().getName(), info);
      }
    }
    final long parse = System.currentTimeMillis();
    final Multimap<String, String> qnames = ArrayListMultimap.create();
    if (loadAllLibraries) {
      loadAllLibraries(qnames);
    }
    for (JavacProject jp : projects) {
      final Collection<CodeInfo> info = infos.get(jp.getName());
      final BatchParser parser = parsers.get(jp.getName());
      final List<CodeInfo> temp = new ArrayList<>(info == null ? Collections.<CodeInfo> emptyList() : info);

      parser.addUnassociatedRefs(qnames.get(jp.getName()));
      handleReferences(parser, temp);
      parser.clearRefs();
      results.addAll(temp);
    }
    updateTypeEnvs(results);
    final long end = System.currentTimeMillis();
    System.out.println("Parsing ASTs  = " + (parse - start) + " ms");
    System.out.println("Handling refs = " + (end - parse) + " ms");
  }

  /**
   * Updates type environments if something defined in another project changed
   */
  private void updateTypeEnvs(final List<CodeInfo> results) {
    if (true /* projects.isDelta() */) {
      // Update refs to changed files
      for (JavacProject jp : projects) {
        int changed = 0;
        for (CodeInfo info : results) {
          /*
          if (info.getFileName().endsWith("-info.java")) {
            System.out.println("Updating " + info.getFileName());
          }
          */
          if (jp.getTypeEnv().addCompUnit(info, false)) {
            changed++;
          }
        }
        if (changed > 0) {
          // Note that this includes files that are already in the project
          System.out.println("Updated " + changed + " files in " + jp.getName());
        }
      }
    }
  }

  private BatchParser parseSources(JavacProject jp, final List<CodeInfo> results) throws IOException {
    final JavacTypeEnvironment tEnv = jp.getTypeEnv();
    Util.startSubTask(tEnv.getProgressMonitor(), "Parsing ...");
    final BatchParser parser = parsers.get(jp.getName());
    parser.parse(jp.getConfig().getFiles(), results, false);
    System.out.println("Done adapting " + results.size() + " files for " + jp.getName());
    Util.endSubTask(tEnv.getProgressMonitor());
    return parser;
  }

  /**
   * Load qnames with all the classes for each project
   */
  private void loadAllLibraries(final Multimap<String, String> qnames) {
	for (Pair<String, String> key : getMapKeys()) {
	  qnames.put(key.first(), key.second());
	}	  
  }
  
  /**
   * Refs are added to results
   */
  private void handleReferences(BatchParser parser, List<CodeInfo> results) throws IOException {
	final int initialSize = results.size();
	if (initialSize != 1) {
	  Util.startSubTask(parser.tEnv.getProgressMonitor(), "Handling references for " + parser.jp.getName());
	}
    final References refs = parser.refs;
    refs.add(SLUtility.JAVA_LANG_OBJECT);
    refs.add("java.lang.Class");
    refs.add("java.lang.String"); // For comparison purposes
    refs.add(PromiseConstants.ARRAY_CLASS_QNAME);
    refs.add("java.lang.Cloneable"); // array super types
    refs.add("java.io.Serializable"); // array super types
    refs.add("java.lang.Enum");
    refs.add("java.lang.annotation.Annotation");
    refs.add("java.util.Collection"); // used in for-each loops
    refs.add("java.util.Iterator"); // used in for-each loops
    refs.add("java.lang.Boolean");
    refs.add("java.lang.Byte");
    refs.add("java.lang.Character");
    refs.add("java.lang.Short");
    refs.add("java.lang.Integer");
    refs.add("java.lang.Long");
    refs.add("java.lang.Float");
    refs.add("java.lang.Double");
    refs.addAll(requiredRefs);
    if (XUtil.testingWorkspace) {
      refs.addAll(PackageAccessor.findPromiseXMLs());
    }

    if (false) {// wantToRunInParallel) {
      final ParallelArray<CodeInfo> temp = new ParallelArray<>();
      temp.asList().addAll(results);
      final Procedure<CodeInfo> proc = new Procedure<CodeInfo>() {
        @Override
        public void op(CodeInfo info) {
          // System.out.println("Scanning: "+info.getFileName());
          final boolean debug = false;
          if (refs.jp.getTypeEnv() == info.getTypeEnv()) {
            refs.scanForReferencedTypes(info.getNode(), debug);
          }
        }
      };
      temp.apply(proc);
    } else {
      for (CodeInfo info : results) {
        // System.out.println("Scanning: "+info.getFileName());
        final boolean debug = false;
        // info.getFileName().contains("EJBContext");

        if (refs.jp.getTypeEnv() == info.getTypeEnv()) {
          refs.scanForReferencedTypes(info.getNode(), debug);
        }
      }
    }
    // Check for AWT references
    // boolean usesAWT = false;
    for (String ref : refs.getAllRefs()) {
      if (ref.startsWith("java.awt.") || ref.startsWith("java.applet")) {
        refs.add("java.awt.event.ActionListener");
        break;
      }
    }
    final Collection<CodeInfo> newResults = handleDanglingRefs(parser.jp, refs.getAllRefs());
    results.addAll(newResults);

    refs.reorder(results);
	if (initialSize != 1) {
	  Util.endSubTask(parser.tEnv.getProgressMonitor());
	}
  }

  private Collection<CodeInfo> handleDanglingRefs(final JavacProject jp, Set<String> refs) throws IOException {
    if (jp.getTypeEnv().getProgressMonitor().isCanceled()) {
      throw new CancellationException();
    }
    if (refs.isEmpty()) {
      //System.out.println("No more dangling refs");
      return Collections.emptyList();
    }
    System.out.println("Handling " + refs.size() + " dangling refs for " + jp.getName());
    /*
     * if (refs.size() < 5) { for(String ref : refs) { System.out.println(
     * "Handling: "+ref); } }
     */
    Multimap<String, JavaSourceFile> asBinary = ArrayListMultimap.create();
    List<Triple<String, String, File>> classFiles = new ArrayList<>();
    ZipFile jar = null;
    File lastJar = null;
    for (String ref : refs) {
      // System.out.println("Got ref to "+ref+" from "+jp.getName());
      final IJavaFile p = getMapping(jp.getName(), ref);
      if (p == null) {
        SLLogger.getLogger().warning("Unable to find ref " + ref + " in " + jp.getName());
        continue;
      }
      switch (p.getType()) {
      case CLASS_FROM_JAR:
        // Jar file
        // System.out.println("Got ref to jarred class "+ref);
        if (!p.getFile().equals(lastJar)) {
          lastJar = p.getFile();
          jar = new ZipFile(p.getFile());
        }
        jarRefs.asList().add(new Triple<>(p.getProject(), ref, jar));
        break;
      case SOURCE:
        asBinary.put(p.getProject(), (JavaSourceFile) p);
        break;
      case CLASS:
        // Assume .class
        // System.out.println("Got ref to class "+ref);
        File f = p.getFile();
        String name = f.getName();
        int dollar = name.indexOf('$');
        if (dollar >= 0) {
          // Nested class, so we need to convert this to the outer class
          String outerName = name.substring(0, dollar);
          f = new File(f.getParentFile(), name.substring(0, dollar) + ".class");
          ref = ref.substring(0, ref.length() - name.length() + 6) + outerName;
        }
        classFiles.add(new Triple<>(p.getProject(), ref, f));
        break;
      default:
        System.out.println("Unknown ref: " + ref);
      }
    }

    // TODO thread safe?
    // Project -> binary CUs
    final Multimap<String, CodeInfo> cus = Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, CodeInfo>create());
    handleDanglingJarRefs(jp, cus);
    handleDanglingClassFileRefs(jp, classFiles, cus);
    final List<CodeInfo> newCUs = handleDanglingSourceRefs(jp, asBinary);

    // Needed to check for package-info classes
    // Project -> package names
    final Multimap<String, String> maybeNewPkgs = ArrayListMultimap.create();
    for (Map.Entry<String, Collection<CodeInfo>> e : cus.asMap().entrySet()) {
      @Nullable
      final String key = e.getKey();
      if (key != null) {
        @Nullable
        final Collection<CodeInfo> values = e.getValue();
        if (values != null) {
          newCUs.addAll(values);
          for (CodeInfo cu : values) {
            if (cu != null) {
              maybeNewPkgs.put(key, cu.getFile().getPackage());
            } else {
            	System.err.println("Got null CodeInfo for "+key);
            }
          }
        } else {
        	System.err.println("No packages for "+key);
        }
      } else {
    	  System.err.println("Null project for "+(e.getValue() == null ? "null packages" : e.getValue().iterator().next().getFileName()));
      }
    }

    // Project -> qnames
    final Multimap<String, String> moreRefs = ArrayListMultimap.create();
    // Check for package-info classes
    for (Map.Entry<String, Collection<String>> e : maybeNewPkgs.asMap().entrySet()) {
      for (String pkg : new HashSet<>(e.getValue())) {
        final String qname = pkg + '.' + SLUtility.PACKAGE_INFO;
        if (!refs.contains(qname) && isMapped(e.getKey(), qname)) {
          moreRefs.put(e.getKey(), qname);
        }
      }
    }
    for (CodeInfo cu : newCUs) {
      // System.out.println("Scanning: "+info.getFileName());
      final boolean debug = false; // cu.getFileName().contains("EJBContext");
      if (cu == null) {
    	  System.err.println("Got null CodeInfo");
      }
      if (cu.getFile() == null) {
    	  System.err.println("Got null file for "+cu.getFileName());
      }
      final String proj = cu.getFile().getProjectName();
      final BatchParser parser = parsers.get(proj);
      if (parser == null) {
        throw new NullPointerException();
      }
      final References r = parser.refs;
      final Set<String> refTypes = r.scanForReferencedTypes(cu.getNode(), debug);
      if (proj != null && !refTypes.isEmpty())
        moreRefs.putAll(proj, refTypes);
    }
    // See if there are still the same outstanding refs for this project
    if (checkForCycle(refs, moreRefs.get(jp.getName()))) {
      /*
       * //tEnv.addCompUnits(cus); for(String ref : moreRefs) { IRNode type =
       * tEnv.findNamedType(ref); if (type != null && couldBeUnknownType(ref) !=
       * null) { System.out.println("couldBeUnknownType doesn't match tEnv for "
       * +ref); } else { System.out.println("What is there to do with "+ref); }
       * }
       */
      throw new RuntimeException("Detected cycle");
    } else {
      refs.clear();
    }
    cus.clear();

    // TODO are these really for the same project as the first set?
    for (Map.Entry<String, Collection<String>> e : moreRefs.asMap().entrySet()) {
      @Nullable
      final String key = e.getKey();
      if (key != null) {
        @Nullable
        final Collection<String> values = e.getValue();
        final Set<String> setValues = values == null ? new HashSet<String>() : new HashSet<String>(values);
        final Collection<CodeInfo> moreCUs = handleDanglingRefs(projects.get(key), setValues);
        newCUs.addAll(moreCUs);
      } else {
        SLLogger.getLogger().log(Level.WARNING, "moreRefs returned a null key (code bug) for "+e.getValue(), new Exception());
      }
    }
    return newCUs;
  }

  private void handleDanglingJarRefs(final JavacProject jp, final Multimap<String, CodeInfo> cus) {
    // Handle refs in jars
    Procedure<Triple<String, String, ZipFile>> proc = new Procedure<Triple<String, String, ZipFile>>() {
      @Override
      public void op(Triple<String, String, ZipFile> tri) {
        String project = tri.first();
        String ref = tri.second();
        ZipFile jar = tri.third();

        final JavacProject srcProject = projects.get(project);
        CodeInfo info = findClass(srcProject, ref, new File(jar.getName()));
        if (info == null || info.getNode().identity() == IRNode.destroyedNode) {
          // if (ref.startsWith("junit.framework.")) {
          // System.out.println("\tGot "+ref+" from "+project+":
          // "+jar.getName());
          // }
          /*
           * if (SLUtility.JAVA_LANG_OBJECT.equals(ref)) { System.out.println(
           * "Got Object from "+project+": "+jar.getName()); } if
           * ("java.lang.Enum".equals(ref)) { System.out.println(
           * "Got Enum from "+project+": "+jar.getName()); }
           */
          ClassAdapter p = new ClassAdapter(srcProject, jar, ref, false, 0, generateMD5Hash(ref));
          ICodeFile file = new JarResource(jar.getName(), ref, project);
          info = adaptClass(srcProject, p, file, ref, cus);
        } else {
          // System.out.println("\tCopied "+ref+" from
          // "+info.getTypeEnv().getProject().getName()+": "+jar.getName());
        }
        // System.out.println("Looking at "+ref+" ("+project+") via
        // "+jp.getName());
        if (info != null && jp.getTypeEnv() != srcProject.getTypeEnv()) {
          jp.getTypeEnv().addCompUnit(info, true);
          return;
        }
      }
    };
    if (wantToRunInParallel) {
      jarRefs.apply(proc);
    } else {
      for (Triple<String, String, ZipFile> triple : jarRefs.asList()) {
        proc.op(triple);
      }
    }
    jarRefs.asList().clear();
  }

  private void handleDanglingClassFileRefs(final JavacProject jp, List<Triple<String, String, File>> classFiles,
      final Multimap<String, CodeInfo> cus) {
    // TODO parallelize?
    for (Triple<String, String, File> tri : classFiles) {
      String project = tri.first();
      String ref = tri.second();
      File classFile = tri.third();
      System.out.println("\tGot " + ref + " from " + project + ": " + classFile);

      final JavacProject srcProject = projects.get(project);
      CodeInfo info = findClass(srcProject, ref, classFile);
      if (info == null) {
        ClassAdapter p = new ClassAdapter(srcProject, classFile, ref, false, 0, generateMD5Hash(ref));
        ICodeFile file = new FileResource(projects, classFile, ref, project);
        info = adaptClass(srcProject, p, file, ref, cus);
      }
      if (info != null && jp.getTypeEnv() != srcProject.getTypeEnv()) {
        jp.getTypeEnv().addCompUnit(info, true);
      }
    }
  }

  private List<CodeInfo> handleDanglingSourceRefs(final JavacProject jp, Multimap<String, JavaSourceFile> asBinary)
      throws IOException {
    // Adapt files from other projects
    final List<CodeInfo> newCUs = new ArrayList<>();
    for (JavacProject jcp : projects) {
      final Collection<JavaSourceFile> files = asBinary.get(jcp.getName());
      if (files != null) {
        final List<JavaSourceFile> newFiles = new ArrayList<>();
        for (JavaSourceFile jsf : files) {
          /*
           * if (jsf.qname.startsWith("root") || jsf.qname.startsWith("common"))
           * { System.out.println("Looking at "+jsf.qname); }
           */
          final CodeInfo info = jcp.getTypeEnv().findCompUnit(jsf.qname);
          if (info == null) {
            /*
             * TODO Base64 System.out.println("Couldn't find "+jsf.qname+" in "
             * +jcp.getTypeEnv()); if (jcp.getTypeEnv().findNamedType(jsf.qname)
             * != null) { System.out.println(
             * "Found the type, not the CodeInfo for "+jsf.qname); }
             */
            newFiles.add(jsf);
          } else {
            newCUs.add(info);
          }
        }
        if (!newFiles.isEmpty()) {
          parsers.get(jcp.getName()).parse(newFiles, newCUs, jcp.isAsBinary());
        }
      }
    }
    asBinary.clear();
    // Import to the current project
    for (CodeInfo info : newCUs) {
      boolean changed = jp.getTypeEnv().addCompUnit(info, true);
      if (changed) {
    	System.out.println("Importing " + info.getFile().getRelativePath() + " to " + jp.getName());
      }
    }
    return newCUs;
  }

  CodeInfo findClass(JavacProject srcProject, String ref, File source) {
    // System.out.println("Looking for class from "+source);
    File src = mappedSources.get(source);
    if (src == null) {
      mappedSources.put(source, source);
      src = source;
    }
    final JavacTypeEnvironment srcEnv = srcProject.getTypeEnv();
    CodeInfo info = projects.getLoadedClasses(ref, src);
    if (info == null) {
      info = srcEnv.findCompUnit(ref);
    } else {
      /*
       * if (!source.getPath().contains("jre")) { System.out.println(
       * "Reusing info for "+ref+", "+src); }
       */
      srcEnv.addCompUnit(info, true);
    }
    return info;
  }

  CodeInfo adaptClass(JavacProject srcProject, ClassAdapter p, ICodeFile file, String ref, Multimap<String, CodeInfo> cus) {
    final JavacTypeEnvironment srcEnv = srcProject.getTypeEnv();
    // PlainIRNode.setCurrentRegion(new IRRegion());
    try {
      IRNode cu = p.getRoot();
      CodeInfo info = new CodeInfo(srcEnv, file, cu, null, ref, null, Type.BINARY);
      srcEnv.addCompUnit(info, true);
      cus.put(srcProject.getName(), info);
      Projects.setProject(cu, srcProject);
      if (SLUtility.JAVA_LANG_OBJECT.equals(ref)) {
        System.out.println("Setting project for Object from " + srcProject.getName());
      }

      File src = mappedSources.get(p.getSource());
      if (src == null) {
        mappedSources.put(p.getSource(), p.getSource());
        src = p.getSource();
      }
      projects.addLoadedClass(ref, src, info);
      return info;
    } catch (IOException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Unable to parse " + ref, e);
    }
    return null;
  }

  /**
   * Checking to see if we didn't eliminate any refs
   */
  private boolean checkForCycle(Set<String> refs, Collection<String> moreRefs) {
    if (moreRefs == null) {
      return false;
    }
    for (String r : refs) {
      if (!moreRefs.contains(r)) {
        return false;
      }
    }
    return true;
  }

  private class ImportHandler {
    final JavacProject jp;
    final Set<String> demandImports = new TreeSet<>();
    // final Map<String,String> namedImports = new HashMap<String,String>();

    ImportHandler(JavacProject p) {
      jp = p;
    }

    void add(String pkg) {
      demandImports.add(pkg);
    }

    boolean checkTypeReference(Set<String> refs, String id) {
      /*
       * This should not be necessary since these type names should be loaded up
       * by the reference in the import
       * 
       * String qname = namedImports.get(id); if (qname != null) { String key =
       * couldBeUnknownType(jp, qname); if (key != null) { refs.add(key); return
       * true; } }
       */
      /*
       * if ("SLFormatter".equals(id)) { System.out.println("Found: "+id); }
       */
      for (String prefix : demandImports) {
        /*
         * if ("com.surelogic.common.ref.Decl".equals(prefix) &&
         * id.endsWith("Builder")) { System.out.println("Looking at "+id); }
         */
        String qname = prefix + '.' + id;
        String key = couldBeUnknownType(jp, qname);
        if (key != null) {
          refs.add(key);
          return true;
        }
      }
      return false;
    }
    /*
     * void addByName(String name, String qname) { namedImports.put(name,
     * qname); }
     */
  }

  String couldBeUnknownType(JavacProject jp, String origName) {
    /*
     * if ("ConcurrentHashSet".equals(origName)) { System.out.println(
     * "Got ConcurrentHashSet"); }
     */
    // int dollar = origName.indexOf('$');
    String qname;
    // if (false && dollar >= 0) {
    // TODO do I really ever use this?
    // qname = origName.substring(0, dollar);
    // } else {
    qname = origName;
    // }
    /*
     * if (doneWithInitialParse && "java.util.LinkedList".equals(qname)) {
     * System.out.println(qname); }
     */
    /*
     * if (jp.getName().equals("common") &&
     * qname.endsWith("junit.framework.Assert")) { System.out.println(
     * "Checking: "+qname); }
     */
    if (jp.getTypeEnv().findNamedType(qname) != null) {
      return null;
    }
    if (jp.getTypeEnv().findPackage(qname, null) != null) {
      return null;
    }
    if (isMapped(jp.getName(), qname)) {
      return qname;
      /*
       * } else if (qname.startsWith("org.eclipse") ||
       * qname.startsWith("org.osgi")) { System.out.println("Missing: "+qname);
       */
    }
    return null;
  }

  /**
   * Used to get the set of qualified names to load up
   */
  private class References {
    final JavacProject jp;
    final Map<IRNode, Set<String>> refs = new IRNodeHashedMap<>();
    final IRNode unassociated = new MarkedIRNode("Unassociated");

    References(JavacProject p) {
      jp = p;
    }

	private Set<String> ensureInitUnassociated() {
      Set<String> r = refs.get(unassociated);
      if (r == null) {
        r = new HashSet<>();
        refs.put(unassociated, r);
      }
      return r;
    }

    private void add(Set<String> refs, String ref) {
      if (couldBeUnknownType(jp, ref) != null) {
        refs.add(ref);
      }
    }

    /**
     * Add qualified names not associated with a particular CU
     */
    void add(String ref) {
      Set<String> refs = ensureInitUnassociated();
      add(refs, ref);
    }

    void addAll(Iterable<String> moreRefs) {
      Set<String> refs = ensureInitUnassociated();
      for (String ref : moreRefs) {
        add(refs, ref);
      }
    }

    void clearRefs() {
      //Set<String> refs = ensureInitUnassociated();	
      refs.clear();
	}
    
    Set<String> scanForReferencedTypes(IRNode cu, boolean debug) {
      Set<String> r = new HashSet<>();
      FASTScanner s = new FASTScanner(jp, r, debug);
      IRNode t = VisitUtil.getPrimaryType(cu);

      /*
       * String qname = JavaNames.getFullTypeName(t); if
       * (qname.endsWith("DefaultSynthStyle")) { System.out.println(
       * "FAST Scanning "+qname); }
       */
      s.doAccept(cu);
      s.finish();

      synchronized (refs) {
        refs.put(cu, r);
      }
      /*
       * if (r.contains("javax.swing.WindowConstants") &&
       * "jEdit-4.1".equals(jp.getName())) { IRNode type =
       * VisitUtil.getPrimaryType(cu); String unparse =
       * JavaNames.getFullTypeName(type); System.out.println("moreRefs: "
       * +unparse); }
       */
      return r;
    }

    Set<String> getAllRefs() {
      Set<String> results = new HashSet<>();
      for (Set<String> s : refs.values()) {
        results.addAll(s);
      }
      return results;
    }

    Set<String> getRefs(CodeInfo cu) {
      Set<String> r = refs.get(cu.getNode());
      return r == null ? Collections.<String> emptySet() : r;
    }

    void reorder(List<CodeInfo> results) {
      Map<IRNode, CodeInfo> map = new HashMap<>(results.size());
      for (CodeInfo info : results) {
        map.put(info.getNode(), info);
      }
      List<CodeInfo> stack = new ArrayList<>(results.size());
      Set<Object> visited = new HashSet<>();
      for (CodeInfo cu : results) {
        depthFirstSearch(map, stack, visited, cu);
      }
      // Pop stack contents to results
      results.clear();
      for (int i = stack.size() - 1; i >= 0; i--) {
        results.add(stack.get(i));
      }
    }

    private void depthFirstSearch(Map<IRNode, CodeInfo> map, List<CodeInfo> stack, Set<Object> visited, CodeInfo cu) {
      if (cu == null) {
        return;
      }
      if (visited.contains(cu)) {
        return;
      }
      visited.add(cu);
      stack.add(cu);

      for (String ref : getRefs(cu)) {
        if (visited.contains(ref)) {
          continue;
        }
        visited.add(ref);
        IRNode tdecl = jp.getTypeEnv().findNamedType(ref);
        IRNode refCU = VisitUtil.getEnclosingCompilationUnit(tdecl);
        depthFirstSearch(map, stack, visited, map.get(refCU));
      }
    }
  }

  /**
   * Built to scan for references to unknown types
   */
  private class FASTScanner extends Visitor<Void> {
    final JavacProject jp;
    @SuppressWarnings("unused")
    final boolean debug;
    final Set<String> refs;
    final ImportHandler demandImports;

    public FASTScanner(JavacProject p, Set<String> refs, boolean debug) {
      jp = p;
      this.refs = refs;
      this.debug = debug;
      demandImports = new ImportHandler(p);
    }

    void finish() {
      // In case any of the imports are of outer types
      for (String demand : demandImports.demandImports) {
        if (jp.getTypeEnv().findPackage(demand, null) == null && jp.getTypeEnv().findNamedType(demand) == null) {
          refs.add(demand);
        }
      }
    }

    @Override
    public Void visit(IRNode node) {
      super.doAcceptForChildren(node);
      return null;
    }

    @Override
    public Void visitAnnotation(IRNode node) {
      String name = Annotation.getId(node);
      if (name.indexOf('.') > 0) {
        checkTypeReference(name);
      } else {
        /*
         * if (!name.equals("Override")) { System.out.println("Looking at: "
         * +name); }
         */
        checkSimpleName(name);
      }
      doAcceptForChildren(node);
      return null;
    }

    @Override
    public Void visitCompilationUnit(IRNode node) {
      demandImports.add("java.lang");
      visit(CompilationUnit.getImps(node));
      doAccept(CompilationUnit.getPkg(node));
      visit(CompilationUnit.getDecls(node));
      return null;
    }

    @Override
    public Void visitUnnamedPackageDeclaration(IRNode node) {
      demandImports.add("");
      return null;
    }

    @Override
    public Void visitNamedPackageDeclaration(IRNode node) {
      demandImports.add(NamedPackageDeclaration.getId(node));
      return visit(node);
    }

    @Override
    public Void visitDemandName(IRNode node) {
      demandImports.add(DemandName.getPkg(node));
      return null;
    }

    /*
     * This should not be necessary since these type names should be loaded up
     * by the reference in the import
     * 
     * @Override public Void visitImportDeclaration(IRNode node) { IRNode item =
     * ImportDeclaration.getItem(node); Operator op =
     * JJNode.tree.getOperator(item); if (ClassType.prototype.includes(op)) {
     * return handleClassType(item, op); } else if (op instanceof DemandName) {
     * return visitDemandName(item); } else if (op instanceof StaticImport) {
     * IRNode type = StaticImport.getType(item); return handleClassType(type,
     * JJNode.tree.getOperator(type)); } else if (op instanceof
     * StaticDemandName) { IRNode type = StaticDemandName.getType(item); return
     * handleClassType(type, JJNode.tree.getOperator(type)); } else { // TODO
     * what about static imports? return super.visitImportDeclaration(node); } }
     * 
     * private Void handleClassType(IRNode item, Operator op) { if
     * (TypeRef.prototype.includes(op)) { IRNode base = TypeRef.getBase(item);
     * return handleClassType(base, JJNode.tree.getOperator(base)); } String
     * unparse = DebugUnparser.toString(item); String name =
     * unparse.substring(unparse.lastIndexOf('.')+1); if
     * (name.equals("RegionEffects")) { System.out.println("Got class type: "
     * +unparse); } demandImports.addByName(name, unparse); return null; }
     */

    @Override
    public Void visitNameType(IRNode node) {
      processName(NameType.getName(node));
      return null;
    }

    @Override
    public Void visitNameExpression(IRNode node) {
      /*
       * String unparse = DebugUnparser.toString(node); if
       * (unparse.contains("RetentionPolicy")) { System.out.println("Got "
       * +unparse); }
       */
      processName(NameExpression.getName(node));
      return null;
    }

    /**
     * @return true if successful
     */
    private boolean processName(IRNode n) {
      if (QualifiedName.prototype.includes(n)) {
        if (processName(QualifiedName.getBase(n))) {
          return true;
        }
        String qname = DebugUnparser.toString(n);
        return checkTypeReference(qname);
      }
      String id = SimpleName.getId(n);
      /*
       * if ("CDRInputStream_1_0".equals(id)) { System.out.println("ID: "+id); }
       * if ("RetentionPolicy".equals(id)) { System.out.println(
       * "Got RetentionPolicy"); }
       */
      return checkSimpleName(id);
    }

    private boolean checkSimpleName(String id) {
      if (checkTypeReference(id)) {
        return true;
      }
      return demandImports.checkTypeReference(refs, id);
    }

    @Override
    public Void visitNamedType(IRNode node) {
      String qname = NamedType.getType(node);
      /*
       * if (qname.contains("javax.swing.plaf.synth.SynthStyle")) {
       * System.out.println("Checking NT: "+qname); }
       */
      checkTypeReference(qname);
      return null;
    }

    @Override
    public Void visitEnumDeclaration(IRNode node) {
      checkTypeReference("java.lang.Enum");
      return super.visitEnumDeclaration(node);
    }

    private boolean checkTypeReference(String qname) {
      /*
       * if (qname.contains("root.Root")) { System.out.println("Checking TR: "
       * +qname); }
       */
      // Check for substrings of the name
      String name = qname;
      while (name != null) {
        boolean rv = checkTypeReference_internal(name);
        if (rv) {
          return true;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
          name = null;
        } else {
          name = name.substring(0, lastDot);
        }
      }
      return false;
    }

    private boolean checkTypeReference_internal(String qname) {
      String key = couldBeUnknownType(jp, qname);
      if (key != null) {
        /*
         * String name = key.substring(0, key.length()-6).replace('/', '.'); if
         * (tEnv.findNamedType(name) != null) { throw new
         * RuntimeException(name); }
         */
        if (!refs.contains(key)) {
          // System.out.println("Dangling ref: "+key);
          refs.add(key);
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Check for newly referenced types
   */
  public void checkReferences(List<CodeInfo> cus) throws IOException {
    // Organize by project
    final Multimap<String, CodeInfo> byProject = ArrayListMultimap.create();
    for (CodeInfo info : cus) {
      final IIRProject p = Projects.getProject(info.getNode());
      byProject.put(p.getName(), info);
      // System.out.println(p.getName()+" : "+info.getFileName());
    }
    cus.clear();

    for (String proj : byProject.keySet()) {
      // System.out.println("Checking refs for "+proj);
      final Collection<CodeInfo> info = byProject.get(proj);
      final List<CodeInfo> results = new ArrayList<>(info);
      checkReferences(results, proj);
      cus.addAll(results);
    }
    updateTypeEnvs(cus);
  }

  private void checkReferences(final List<CodeInfo> results, final String proj) throws IOException {
    final BatchParser parser = parsers.get(proj);
    parser.clearRefs();
    handleReferences(parser, results);
  }

  public void checkReferences(CodeInfo info) throws IOException {
    final IIRProject p = Projects.getProject(info.getNode());
    final List<CodeInfo> results = new ArrayList<>();
    results.add(info);
    checkReferences(results, p.getName());
    updateTypeEnvs(results);
  }

}
