/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/AbstractJavaFileLocator.java,v 1.7 2008/08/25 19:07:36 chance Exp $*/
package edu.cmu.cs.fluid.java;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;

import com.surelogic.common.FileUtility;
import com.surelogic.common.FileUtility.TempFileFilter;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.*;
import com.surelogic.dropsea.ir.drops.BinaryCUDrop;
import com.surelogic.dropsea.ir.drops.SourceCUDrop;
import com.surelogic.dropsea.irfree.Entity;
import com.surelogic.dropsea.irfree.IXmlResultListener;
import com.surelogic.dropsea.irfree.NestedJSureXmlReader;

import edu.cmu.cs.fluid.ide.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.*;

public abstract class AbstractJavaFileLocator<T, P> implements IJavaFileLocator<T, P> {
  private static final Logger LOG = SLLogger.getLogger("java-file-locator");
  private static final DebugUnparser unparser = new DebugUnparser(3, JJNode.tree);
  private static final boolean useJavaCanonicalizer = false;

  protected final Map<T, JavaFileStatus<T, P>> resources = new HashMap<T, JavaFileStatus<T, P>>();
  final Map<P, JavaRewrite> canonicalizers = new HashMap<P, JavaRewrite>();
  final Map<P, JavaCanonicalizer> javaCanonicalizers = new HashMap<P, JavaCanonicalizer>();
  final IMultiMap<P, T> refs = new SetMultiMap<P, T>();
  final SlotHandler slotHandler = new SlotHandler();
  final MemoryHandler memHandler = new MemoryHandler();
  protected boolean okToCanonicalize = false;
  private boolean loadedFromArchive = true;

  /**
   * Path for locating archives
   */
  protected FileLocator flocPath;

  /**
   * Used to persist files before they get added to the path
   */
  ZipFileLocator currentArchive;
  int numWritten = 0;

  protected abstract long mapTimeStamp(long t);

  protected JavaRewrite getRewrite(P proj) {
    ITypeEnvironment te = getTypeEnvironment(proj);
    return new JavaRewrite(te);
  }

  protected abstract JavaCanonicalizer getCanonicalizer(P proj);

  protected abstract ITypeEnvironment getTypeEnvironment(P proj);

  protected abstract String getProjectHandle(P proj);

  protected abstract String getIdHandle(T id);

  protected abstract P getProjectFromHandle(String handle);

  protected abstract T getIdFromHandle(String handle);

  protected abstract File getDataDirectory();

  private void archiveFileLocator(FileLocator flocPath) {
    if (flocPath instanceof ZipFileLocator) {
      ZipFileLocator zip = (ZipFileLocator) flocPath;
      File target = new File(getDataDirectory(), "temp.zip"); // TODO fix to use
                                                              // project name
      FileUtility.copy(zip.getCorrespondingFile(), target);
      System.out.println("Final zip: " + (target.length() / 1024.0) + " KB");
    } else {
      throw new IllegalStateException("archiveFileLocator() called on " + flocPath);
    }

  }

  private static final TempFileFilter filter = new TempFileFilter("JSure", ".tmp");

  protected static File createTempFile() throws IOException {
    return filter.createTempFile();
  }

  /**
   * Deletes all temp files created by JSure
   */
  protected static void cleanupTempFiles() {
    FileUtility.deleteTempFiles(filter);
  }

  protected static <T, P> void finishInit(AbstractJavaFileLocator<T, P> l) {
    if (useIRPaging) {
      IDE.getInstance().getMemoryPolicy().addLowMemoryHandler(l.memHandler);
      IR.addUndefinedSlotHandler(l.slotHandler);
    }
  }

  public Iterator<IJavaFileStatus<T>> iterator() {
    return new FilterIterator<Map.Entry<T, JavaFileStatus<T, P>>, IJavaFileStatus<T>>(resources.entrySet().iterator()) {
      @Override
      protected Object select(Entry<T, JavaFileStatus<T, P>> e) {
        return e.getValue();
      }
    };
  }

  /**
   * Add to the existing set of FileLocator(s)
   */
  private void addToFlocPath(ZipFileLocator zfl) {
    if (flocPath == null) {
      flocPath = zfl;
    } else if (flocPath instanceof PathFileLocator) {
      PathFileLocator pfl = (PathFileLocator) flocPath;
      flocPath = pfl.prependToPath(zfl);
    } else {
      flocPath = new PathFileLocator(new FileLocator[] { flocPath, zfl });
    }
  }

  /**
   * Make sure all writes are complete
   * 
   * @param force
   *          If true, this archive replaces the existing set of FileLocator(s)
   */
  protected long commitCurrentArchive(boolean force) throws IOException {
    if (currentArchive == null || numWritten == 0) {
      return 0;
    }
    final File tempFile = currentArchive.getCorrespondingFile();
    currentArchive.commit();
    if (LOG.isLoggable(Level.FINE))
      LOG.fine("Committed " + numWritten + " resources (" + tempFile.length() + " bytes)");

    ZipFileLocator floc = new ZipFileLocator(tempFile, ZipFileLocator.READ);
    if (force) {
      // Replace path
      flocPath = floc;
    } else {
      addToFlocPath(floc);
    }
    if (LOG.isLoggable(Level.FINE))
      LOG.fine("Added to floc path");
    currentArchive = null;
    numWritten = 0;
    return tempFile.length();
  }

  private void ensureCurrentArchiveExists() throws IOException {
    if (currentArchive == null) {
      File tempFile = createTempFile();
      currentArchive = new ZipFileLocator(tempFile, ZipFileLocator.WRITE);
      tempFile.deleteOnExit();
    }
  }

  public synchronized void ensureAllCanonical() {
    for (IJavaFileStatus<T> s : this) {
      s.canonicalize();
    }
  }

  protected void canonicalize(JavaFileStatus<T, P> s) {
    if (useIRPaging && !okToCanonicalize) {
      try {
        beginCanonicalization();
        okToCanonicalize = true;
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Unable to canonicalize", e);
      }
    }
    final boolean changed;
    if (useJavaCanonicalizer) {
      changed = doJavaCanonicalization(s);
    } else {
      P p = s.project();
      JavaRewrite r = canonicalizers.get(p);
      if (r == null) {
        r = getRewrite(p);
        canonicalizers.put(p, r);
      }
      changed = r.ensureDefaultsExist(s.root());
    }
    if (changed) {
      IDE.getInstance().notifyASTChanged(s.root());
    }
  }

  /**
   * @return true if changed
   */
  private boolean doJavaCanonicalization(JavaFileStatus<T, P> s) {
    P p = s.project();
    JavaCanonicalizer jc = javaCanonicalizers.get(p);
    if (jc == null) {
      jc = getCanonicalizer(p);
      javaCanonicalizers.put(p, jc);
    }
    return jc.canonicalize(s.root());
  }

  public synchronized void beginCanonicalization() throws IOException {
    // Canonicalized files need to be in a separate archive
    commitCurrentArchive(false);
  }

  protected void persist(JavaFileStatus<T, P> s) {
    try {
      ensureCurrentArchiveExists();
      if (s.makePersistent(currentArchive, false)) {
        if (LOG.isLoggable(Level.FINE))
          LOG.fine("Persisted " + s.label());
        numWritten++;
      }
      // Check if low on memory
      memHandler.handleLowMemory(IDE.getInstance().getMemoryPolicy());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Unable to persist IR", e);
    }
  }

  /**
   * Persist all the resources that haven't been persisted
   * 
   * @throws IOException
   */
  public synchronized void persistNew() throws IOException {
    persistAll(false);
  }

  public synchronized void persistAll() throws IOException {
    if (loadedFromArchive) {
      return;
    }

    // To prevent duplicates
    commitCurrentArchive(false);

    final long start = System.currentTimeMillis();
    persistAll(true);
    final long end = System.currentTimeMillis();
    System.out.println("Final zip: " + (end - start) + " ms");

    archiveFileLocator(flocPath);
  }

  private void persistAll(boolean force) throws IOException {
    long start = System.nanoTime();
    int num = 0;

    if (force) {
      ensureAllCanonical();
    }
    complainIfNotAllCanonical();

    ensureCurrentArchiveExists();
    for (JavaFileStatus<T, P> s : resources.values()) {
      if (s.makePersistent(currentArchive, force)) {
        num++;
      }
    }
    numWritten += num;

    if (force == true) {
      createArchiveIndex();
    }
    long length = commitCurrentArchive(force);
    long end = System.nanoTime();
    if (LOG.isLoggable(Level.FINE)) {
      if (num != 0) {
        LOG.fine("Persisted " + num + " resources (" + length + " bytes) in " + ((end - start) / 1000000) + " ms");
      } else {
        LOG.fine("Persisted " + length + " bytes in " + ((end - start) / 1000000) + " ms");
      }
    }
    IDE.getInstance().getMemoryPolicy().addLowMemoryHandler(memHandler);
    IR.addUndefinedSlotHandler(slotHandler);
  }

  private void createArchiveIndex() {
    final OutputStream out = currentArchive.openFileWriteOrNull("archive.index");
    if (out != null) {
      final PrintWriter pw = new PrintWriter(out);
      final StringBuilder b = new StringBuilder();
      Entities.start("archive", b);
      Entities.addAttribute("size", resources.size(), b);
      b.append(">\n");
      flushBuffer(pw, b);

      for (JavaFileStatus<T, P> s : resources.values()) {
        s.indexXML(pw, b);
      }
      pw.println("</archive>");
      pw.close();
    }
  }

  public synchronized List<CodeInfo> loadArchiveIndex() throws IOException {
    // Check for an archive
    final File archive = new File(getDataDirectory(), "temp.zip"); // TODO fix
                                                                   // to use
                                                                   // project
                                                                   // name
    if (!archive.isFile()) {
      return Collections.emptyList();
    }
    flocPath = new ZipFileLocator(archive, ZipFileLocator.READ);

    final long start = System.currentTimeMillis();
    final InputStream in = flocPath.openFileReadOrNull("archive.index");
    if (in != null) {
      IndexHandler h = new IndexHandler();
      try {
        h.read(in);
        final long end = System.currentTimeMillis();
        System.out.println("Reloaded index: " + (end - start) + " ms");

        loadedFromArchive = true;
        return h.infos;
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      flocPath = null;
    }
    return Collections.emptyList();
  }

  class IndexHandler extends NestedJSureXmlReader implements IXmlResultListener {
    final List<CodeInfo> infos = new ArrayList<CodeInfo>();

    @Override
    protected String checkForRoot(String name, Attributes attributes) {
      if ("archive".equals(name)) {
        return "archive"; // TODO fix to be unique
      }
      return null;
    }

    public Entity makeEntity(String name, Attributes a) {
      return new Entity(name, a);
    }

    public void start(String uid, String project) {
      // Nothing to do
    }

    public void notify(Entity e) {
      final JavaFileStatus<T, P> s = JavaFileStatus.recreate(AbstractJavaFileLocator.this, e);
      resources.put(s.id(), s);

      getTypeEnvironment(s.project()).addTypesInCU(s.root());
      setupCUDrop(s);
    }

    private void setupCUDrop(final JavaFileStatus<T, P> s) {
      final String javaOSFileName = s.label();
      final CodeInfo info = new CodeInfo(null, new AbstractCodeFile() {
        public String getPackage() {
          return VisitUtil.getPackageName(s.root());
        }

        public Object getHostEnvResource() {
          return s.id();
        }

        @Override
        public int hashCode() {
          return s.id().hashCode();
        }

        @Override
        public boolean equals(Object o) {
          if (o instanceof ICodeFile) {
            ICodeFile o2 = (ICodeFile) o;
            return s.id() == o2.getHostEnvResource();
          }
          return false;
        }
      }, s.root(), null, javaOSFileName, null, s.getType());
      switch (s.getType()) {
      case SOURCE:
        new SourceCUDrop(info);
        break;
      default:
        new BinaryCUDrop(info);
      }
      infos.add(info);
    }

    public void done() {
      // Nothing to do
    }
  }

  static void flushBuffer(PrintWriter pw, StringBuilder b) {
    pw.append(b);
    System.out.println(b.toString());
    b.setLength(0);
  }

  private void complainIfNotAllCanonical() {
    for (IJavaFileStatus<T> s : this) {
      if (!s.isCanonical()) {
        throw new Error("Not canonical: " + s.label());
      }
    }
  }

  private void complainIfNotAllPersistent() {
    for (IJavaFileStatus<T> s : this) {
      if (!s.isPersistent()) {
        throw new Error("Not persistent: " + s.label());
      }
    }
  }

  private void complainIfSomeLoaded() {
    for (IJavaFileStatus<T> s : this) {
      if (s.isLoaded()) {
        throw new Error("Not persistent: " + s.label());
      }
    }
  }

  public synchronized IJavaFileStatus<T> register(P project, T handle, String label, long modTime, IRNode root, Type type) {
    loadedFromArchive = false;

    JavaFileStatus<T, P> s = new JavaFileStatus<T, P>(this, project, handle, label, modTime, root, type);
    JavaFileStatus<T, P> s2 = resources.put(handle, s);
    if (s2 != null) {
      // Put it back ...
      resources.put(handle, s2);
      throw new IllegalArgumentException("Already registered: " + handle);
    }
    if (useIRPaging) {
      okToCanonicalize = false;
      persist(s);
    }
    return s;
  }

  public synchronized IJavaFileStatus<T> getStatusForAST(IRNode root) {
    if (root == null) {
      return null;
    }
    for (IJavaFileStatus<T> s : this) {
      if (root.equals(s.root())) {
        return s;
      }
    }
    throw new IllegalArgumentException("Couldn't find AST for " + DebugUnparser.toString(root));
  }

  protected JavaFileStatus<T, P> getStatusForRegionOrNull(IRRegion owner) {
    return getStatusForRegion(owner, false);
  }

  // private EclipseFileStatus getStatusForRegion(IRRegion owner) {
  // return getStatusForRegion(owner, true);
  // }

  private JavaFileStatus<T, P> getStatusForRegion(IRRegion owner, boolean failOnError) {
    if (owner == null) {
      return null;
    }
    for (JavaFileStatus<T, P> s : resources.values()) {
      if (s.includesRegion(owner)) {
        return s;
      }
    }
    /*
     * for(Map.Entry<Object,EclipseFileStatus> e : resources.entrySet()) {
     * EclipseFileStatus s = e.getValue(); s.dumpRegions(); }
     */
    if (failOnError) {
      throw new IllegalArgumentException("Couldn't find status for " + owner);
    }
    return null;
  }

  public synchronized boolean isLoaded(T handle) {
    return resources.containsKey(handle);
  }

  public synchronized IJavaFileStatus<T> getStatus(T handle) {
    return resources.get(handle);
  }

  public synchronized IJavaFileStatus<T> unregister(T handle) {
    JavaFileStatus<T, P> s = resources.remove(handle);
    if (s != null) {
      // TODO cleanup -- unload?
      destroy(s.astChunk1);
      destroy(s.astChunk2);
      destroy(s.astChunk3);
      destroy(s.canonChunk1);
      destroy(s.canonChunk2);
      destroy(s.canonChunk3);
    }
    return s;
  }

  private void destroy(IRChunk c) {
    if (c == null) {
      return;
    }
    IRPersistent r = c.getRegion();
    if (!r.isDestroyed()) {
      r.destroy();
    }
  }

  public IJavaFileStatus<T> isUpToDate(T id, long time, Type thisType) {
    IJavaFileStatus<T> status = getStatus(id);
    long modTime = mapTimeStamp(time);
    if (status != null && modTime == status.modTime() && thisType == status.getType()) {
      return status;
    }
    return null;
  }

  public void setProjectReference(P proj, T handle) {
    refs.map(proj, handle);
  }

  public P findProject(T id) {
    for (IMultiMap.Entry<P, T> e : refs.entrySet()) {
      for (T value : e.getValues()) {
        if (value.equals(id)) {
          return e.getKey();
        }
      }
    }
    return null;
  }

  public synchronized void printSummary(PrintWriter pw) {
    for (JavaFileStatus<T, P> s : resources.values()) {
      s.printSummary(pw);
    }
  }

  protected class SlotHandler implements IUndefinedSlotHandler {
    public boolean handleSlotUndefinedException(@SuppressWarnings("rawtypes") PersistentSlotInfo si, IRNode n) {
      IRRegion owner = IRRegion.getOwnerOrNull(n);
      if (owner == null) {
        // Nothing we can do here
        return false;
      }
      if (!JavaFileStatus.isInBundle(si)) {
        return false;
      }
      // Check if low on memory
      memHandler.handleLowMemory(IDE.getInstance().getMemoryPolicy());

      JavaFileStatus<T, P> s = getStatusForRegionOrNull(owner);
      if (s != null) {
        try {
          boolean rv = s.reload(flocPath, false);
          assert rv : "Should have been able to reload " + s.label();
          if (LOG.isLoggable(Level.FINE))
            LOG.fine("Reloaded " + s.label());
          return true;
        } catch (IOException e) {
          LOG.log(Level.SEVERE, "Problem while reloading IR", e);
        }
      }
      return false;
    }
  }

  protected class MemoryHandler implements ILowMemoryHandler {
    Random r = new Random(hashCode());

    public void handleLowMemory(IMemoryPolicy mp) {
      final double ratio = mp.percentToUnload();
      if (ratio < 0.01) {
        return;
      }
      if (r.nextDouble() < ratio) {
        IDE.getInstance().clearCaches();
      }
      final int num = (int) Math.round(1.0 / ratio);
      if (LOG.isLoggable(Level.FINE))
        LOG.fine("Need to unload: " + ratio + " or 1 out of " + num);

      int numEligible = 0;
      int numLoaded = 0;
      int numPersistent = 0;
      int numUnloaded = 0;

      // Change to always unloading 1 of N based on ratio
      final int indexToUnload = r.nextInt(num);
      int i = 0;
      //
      // Could kick them out based on resources.size()
      // but what if some are ineligible?
      for (JavaFileStatus<T, P> s : resources.values()) {
        if (s.okToUnload()) {
          // System.out.println("Considering ...
          // "+e.getValue().label());
          numEligible++;

          // if (r.nextDouble() < ratio) {
          if (i == indexToUnload) {
            try {
              commitCurrentArchive(false);
            } catch (IOException ioe) {
              LOG.log(Level.SEVERE, "Unable to commit current archive", ioe);
              return;
            }

            if (s.unload()) {
              if (LOG.isLoggable(Level.FINE))
                LOG.fine("Unloading " + s.label());
              numUnloaded++;
            }
          }
          i++;
          if (i == num) {
            // Start over
            i = 0;
          }
        } else {
          if (s.isPersistent()) {
            numPersistent++;
          }
          if (s.isLoaded()) {
            numLoaded++;
          }
        }
      }
      if (numUnloaded > 0) {
        if (LOG.isLoggable(Level.FINE))
          LOG.fine("Randomly unloaded ... " + numUnloaded + " out of " + numEligible);
        SlotInfo.gc();

        // Seems to do a full GC, which takes a while
        // System.gc();
      } else {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("None of these were unloaded: " + numEligible);
          LOG.fine("#persistent = " + numPersistent);
          LOG.fine("#loaded     = " + numLoaded);
        }
      }
    }
  }

  public synchronized void testReload() throws IOException {
    long start = System.nanoTime();
    int num = 0;
    complainIfSomeLoaded();

    for (JavaFileStatus<T, P> s : resources.values()) {
      if (s.reload(flocPath, false)) {
        try {
          unparser.unparse(s.root());
        } catch (SlotUndefinedException sue) {
          throw new Error(sue);
        }
        num++;
      }
    }
    long end = System.nanoTime();
    if (LOG.isLoggable(Level.FINE))
      LOG.fine("Reloaded " + num + " resources in " + ((end - start) / 1000000) + " ms");
  }

  public synchronized void testUnload(boolean test, boolean expectExceptions) {
    long start = System.nanoTime();
    int num = 0;
    complainIfNotAllPersistent();

    for (Map.Entry<T, JavaFileStatus<T, P>> e : resources.entrySet()) {
      if (e.getValue().unload()) {
        if (test) {
          try {
            unparser.unparse(e.getValue().root());
            assert expectExceptions : "No exception on " + e.getKey();
          } catch (SlotUndefinedException sue) {
            assert !expectExceptions : "Unexpected SlotUndefinedEx on " + e.getKey();
          }
        }
        num++;
      }
    }
    long end = System.nanoTime();
    if (LOG.isLoggable(Level.FINE))
      LOG.fine("Unloaded " + num + " resources in " + ((end - start) / 1000000) + " ms");
  }
}
