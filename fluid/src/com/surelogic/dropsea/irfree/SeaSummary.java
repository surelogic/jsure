package com.surelogic.dropsea.irfree;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTEXT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FILE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HASH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.LINE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ID_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OFFSET_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PATH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TYPE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.URI_ATTR;
import static com.surelogic.dropsea.irfree.JSureSummaryXMLReader.ROOT;
import static com.surelogic.dropsea.irfree.JSureSummaryXMLReader.TIME_ATTR;
import static com.surelogic.common.xml.XMLReader.PROJECT_ATTR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.FileUtility;
import com.surelogic.common.IViewable;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.irfree.JSureSummaryXMLReader;
import com.surelogic.dropsea.irfree.SeaSnapshotXMLReader;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.IXMLResultListener;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.ISupportingInformation;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ModelingProblemDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.threadroles.IThreadRoleDrop;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.MarkedIRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.util.Pair;

/**
 * Summarize the Sea (omits links)
 * 
 * @author Edwin
 */
public class SeaSummary extends AbstractSeaXmlCreator {
  private static final String COUNT = "count";
  private static boolean allowMissingSupportingInfos = true;

  private SeaSummary(File location) throws IOException {
    super(location);
  }

  public static File findSummary(String projectPath) {
    String xmlOracle = null;
    File xmlLocation = null;
    if (IDE.useJavac) {
      xmlOracle = RegressionUtility.getOracleName(projectPath, RegressionUtility.javacOracleFilter, "oracleJavac"
          + SeaSnapshot.SUFFIX);
      xmlLocation = new File(xmlOracle);
      System.out.println("Looking for " + xmlOracle);
    }
    if (true) {
      String tempOracle = RegressionUtility.getOracleName(projectPath, RegressionUtility.xmlOracleFilter, "oracle"
          + SeaSnapshot.SUFFIX);
      File tempLocation = new File(tempOracle);
      System.out.println("Looking for " + tempOracle);

      final boolean noOracleYet = xmlLocation == null || !xmlLocation.exists();
      boolean replace;
      if (noOracleYet) {
        replace = true;
      } else {
        System.out.println("Checking for newer oracle");
        replace = tempLocation.exists() && isNewer(tempOracle, xmlOracle);
      }
      if (replace) {
        xmlOracle = tempOracle;
        xmlLocation = tempLocation;
      }
      System.out.println("Using " + xmlOracle);
    }
    assert (xmlLocation.exists());
    return xmlLocation;
  }

  private static boolean isNewer(String oracle1, String oracle2) {
    String date1 = getDate(oracle1);
    String date2 = getDate(oracle2);
    boolean rv = date1.compareTo(date2) > 0;
    // if (XUtil.testing) {
    System.out.println(date1 + " ?= " + date2 + " => " + (rv ? "first" : "second"));
    // }
    return rv;
  }

  private static String getDate(String oracle) {
    // Start with last segment
    for (int i = oracle.lastIndexOf(File.separatorChar) + 1; i < oracle.length(); i++) {
      if (Character.isDigit(oracle.charAt(i))) {
        return oracle.substring(i);
      }
    }
    return oracle;
  }

  public static void summarize(String project, final Sea sea, File location) throws IOException {
    SeaSummary s = new SeaSummary(location);
    s.summarize(project, sea.getDrops());
  }

  public static void summarize(String project, Collection<? extends IDrop> drops, File location) throws IOException {
    SeaSummary s = new SeaSummary(location);
    s.summarize(project, drops);
  }

  private void summarize(String project, Collection<? extends IDrop> drops) {
    Date now = new Date(System.currentTimeMillis());
    b.start(ROOT);
    b.addAttribute(TIME_ATTR, DateFormat.getDateTimeInstance().format(now));
    b.addAttribute(PROJECT_ATTR, project);

    outputDropCounts(drops);

    for (IDrop d : drops) {
      final IDrop id = checkIfReady(d);
      if (id != null) {
        summarizeDrop(id);
      }
    }
    b.end();
    close();
  }

  private void outputDropCounts(Collection<? extends IDrop> drops) {
    final Map<String, Integer> counts = new HashMap<String, Integer>();
    for (IDrop d : drops) {
      final IDrop id = checkIfReady(d);
      if (id != null) {
        incr(counts, computeSimpleType(id));
      }
    }
    for (Map.Entry<String, Integer> e : counts.entrySet()) {
      Builder cb = b.nest(COUNT);
      cb.addAttribute(e.getKey(), e.getValue().toString());
      cb.end();
    }
  }

  private static <T> void incr(Map<T, Integer> counts, T key) {
    Integer i = counts.get(key);
    if (i == null) {
      i = 1;
    } else {
      i++;
    }
    counts.put(key, i);
  }

  private static IDrop checkIfReady(IDrop d) {
    if (d.instanceOf(PromiseDrop.class)) {
      IProofDrop pd = (IProofDrop) d;
      if (!pd.isFromSrc()) {
        // no need to do anything
        return null;
      }
    }
    // TODO skipping for now
    if (d.instanceOf(IThreadRoleDrop.class)) {
      return null;
    }
    if (d.instanceOf(ModelingProblemDrop.class) && d.getMessage().contains("ThreadRole")) {
      return null;
    }
    if (d.instanceOf(IRReferenceDrop.class)) {
      // Need a location to report
      ISrcRef ref = d.getSrcRef();
      if (ref == null) {
        if (!d.getMessage().contains("java.lang.Object")) {
          /*
           * if (d.getMessage().startsWith("ThreadRole")) {
           * System.out.println("Found ThreadRole"); }
           */
          System.out.println("No src ref for " + d.getMessage());
        } else {
          // System.currentTimeMillis();
        }
        return null;
      }
      return d;
    }
    return null;
  }

  private Builder summarizeDrop(IDrop id) {
    final String name = id.getXMLElementName();
    final Builder b = this.b.nest(name);
    addAttributes(b, id);
    for (ISupportingInformation si : id.getSupportingInformation()) {
      outputSupportingInfo(b, si);
    }
    b.end();
    return b;
  }

  // Note: not using addAttribute(), so we can't convert a Snapshot into a
  // Summary properly?
  private void outputSupportingInfo(Builder outer, ISupportingInformation si) {
    final Builder b = outer.nest(SeaSnapshotXMLReader.SUPPORTING_INFO);
    b.addAttribute(MESSAGE_ATTR, si.getMessage());
    if (si.getSrcRef() != null) {
      addLocation(b, si.getSrcRef());
    }
    b.end();
  }

  private static String computeSimpleType(IDrop id) {
    String type = id.getIRDropSeaClass().getName();
    int lastDot = type.lastIndexOf('.');
    if (lastDot > 0) {
      type = type.substring(lastDot + 1);
    }
    return type;
  }

  private void addAttributes(Builder b, IDrop id) {
    String type = computeSimpleType(id);
    if (type.endsWith("Info")) {
      System.out.println("Bad drop type: " + type);
    }
    /*
     * if (type.contains("Proposed")) { System.out.println("Found "+type); }
     */
    b.addAttribute(TYPE_ATTR, type);
    // addAttribute(MESSAGE_ATTR, id.getMessage());

    ISrcRef ref = id.getSrcRef();
    addLocation(b, ref);
    // addAttribute(OFFSET_ATTR, (long) ref.getOffset());
    b.addAttribute(HASH_ATTR, id.getTreeHash());
    b.addAttribute(CONTEXT_ATTR, id.getContextHash());

    // addAttribute("unparse", unparser.unparseString(id.getNode()));
    // Omitting supporting info
    /*
     * if (id.getMessage().contains("copyStat")) { computeHash(id.getNode(),
     * true); }
     */
    id.snapshotAttrs(b);
  }

  public static long computeHash(IRNode node) {
    return computeHash(node, false);
  }

  public static long computeHash(IRNode node, boolean debug) {
    if (node instanceof MarkedIRNode) {
      return 0; // Not an AST node
    }
    final String unparse = DebugUnparser.unparseCode(node);
    if (debug) {
      System.out.println("Unparse: " + unparse);
    }
    return unparse.hashCode();
  }

  public static long computeContext(IRNode node, boolean debug) {
    if (node instanceof MarkedIRNode) {
      return 0; // Not an AST node
    }
    final String context = JavaNames.computeContextId(node);
    if (context != null) {
      if (debug) {
        System.out.println("Context: " + context);
      }
      /*
       * if (unparse.contains("@") { System.out.println("Found promise"); }
       */
      return context.hashCode();
    }
    return 0;
  }

  public static Listener read(File location) throws Exception {
    // Load up current contents
    final Listener l = new Listener();
    new JSureSummaryXMLReader(l).read(location);
    return l;
  }

  public static Diff diff(IDropFilter f, File location1, File location2) throws Exception {
    final Listener l1 = read(location1);
    final Listener l2 = read(location2);
    Diff d = new Diff(filter(f, l1), filter(f, l2));
    d.diff();
    return d;
  }

  public static Diff diff(final Sea sea, File location) throws Exception {
    List<Drop> drops = sea.getDrops();
    return diff(drops, location, IDropFilter.nullFilter);
  }

  private static List<Entity> filter(IDropFilter f, Listener l) {
    final List<Entity> drops = new ArrayList<Entity>();
    // Collections.sort(oldDrops, EntityComparator.prototype);
    for (Entity e : l.drops) {
      String path = e.getAttribute(PATH_ATTR);
      if (path == null || f.showResource(path)) {
        drops.add(e);
      }
    }
    return drops;
  }

  public static Diff diff(Collection<? extends IDrop> drops, File location, IDropFilter f) throws Exception {
    // Load up current contents
    final Listener l = read(location);

    final List<Entity> oldDrops = filter(f, l);
    final SeaSummary s = new SeaSummary(null);
    final List<Entity> newDrops = new ArrayList<Entity>();
    for (IDrop d : drops) {
      IDrop id = checkIfReady(d);
      if (id != null && f.showResource(id)) {
        Builder b = s.summarizeDrop(id);
        /*
         * if (id.getClass().equals(PromisePromiseDrop.class) &&
         * id.getMessage().contains("InRegion(TotalRegion)")) {
         * System.out.println("Found scoped promise: "+id.getMessage()); }
         */
        /*
         * if (id.getMessage().contains(
         * "ProposedPromiseDrop @RegionEffects(reads java.lang.Object:All)")) {
         * System.out.println("Found proposal"); }
         */
        Entity e = b.build();
        newDrops.add(e);
      } else {
        // System.out.println("Ignoring "+d.getMessage());
      }
    }
    // Collections.sort(newDrops, EntityComparator.prototype);
    Diff d = new Diff(oldDrops, newDrops);
    d.diff();
    return d;
  }

  static class Listener implements IXMLResultListener {
    final Map<String, String> officialCounts = new HashMap<String, String>();
    final Map<String, Integer> counts = new HashMap<String, Integer>();
    final List<Entity> drops = new ArrayList<Entity>();
    String project;
    Date time;

    public Entity makeEntity(String name, Attributes a) {
      return new Entity(name, a);
    }

    public void start(String time, String project) {
      try {
        this.time = DateFormat.getDateTimeInstance().parse(time);
        this.project = project;
      } catch (ParseException e) {
        SLLogger.getLogger().log(Level.SEVERE, "Could not parse " + time);
      }
    }

    public void notify(Entity e) {
      if (COUNT.equals(e.getName())) {
        for (Map.Entry<String, String> me : e.getAttributes().entrySet()) {
          String old = officialCounts.put(me.getKey(), me.getValue());
          if (old != null) {
            throw new IllegalStateException("Duplicate count for " + me.getKey());
          }
        }
      } else {
        drops.add(e);

        String type = e.getAttribute(TYPE_ATTR);
        if (type != null) {
          incr(counts, type);
        }
      }
    }

    public void done() {
      if (!officialCounts.isEmpty()) {
        boolean success = true;
        for (Map.Entry<String, String> e : officialCounts.entrySet()) {
          final Integer i = counts.get(e.getKey());
          if (i == null || !e.getValue().equals(i.toString())) {
            success = false;
            System.out.println(e.getKey() + ": " + e.getValue() + " != " + i);
          }
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
          final String value = officialCounts.get(e.getKey());
          if (value == null || !value.equals(e.getValue().toString())) {
            success = false;
            System.out.println(e.getKey() + ": " + value + " != " + e.getValue());
          }
        }
        if (!success) {
          throw new IllegalStateException("Counts don't match up");
        }
      }
    }
  }

  /**
   * Used to sort the entities by file/entity-name
   */
  static class EntityComparator implements Comparator<Entity> {
    static EntityComparator prototype = new EntityComparator();

    public int compare(Entity e1, Entity e2) {
      final String file1 = e1.getAttribute(FILE_ATTR);
      final String file2 = e2.getAttribute(FILE_ATTR);
      int rv = file1.compareTo(file2);
      if (rv == 0) {
        rv = e1.getName().compareTo(e2.getName());
      }
      return rv;
    }

  }

  public static class Diff {
    final List<Entity> oldDrops;
    final List<Entity> newDrops;
    final Categories categories = new Categories();

    Diff(List<Entity> old, List<Entity> newD) {
      oldDrops = old;
      newDrops = newD;
    }

    public Category[] getCategories() {
      if (categories.isEmpty()) {
        separate();
      }
      List<Category> l = new ArrayList<Category>();
      for (Category c : categories.values()) {
        if (c.isEmpty()) {
          continue;
        }
        l.add(c);
      }
      Collections.sort(l, new Comparator<Category>() {
        public int compare(Category o1, Category o2) {
          int rv = o1.file.compareTo(o2.file);
          if (rv == 0) {
            rv = o1.name.compareTo(o2.name);
          }
          return rv;
        }
      });
      return l.toArray(new Category[l.size()]);
    }

    // Separate into categories
    private void separate() {
      for (Entity e : oldDrops) {
        Category c = categories.getOrCreate(e);
        if (c != null) {
          c.addOld(e);
        } else {
          System.out.println("Couldn't categorize: " + e.getAttribute(MESSAGE_ATTR));
        }
      }
      for (Entity e : newDrops) {
        /*
         * if
         * ("ProposedPromiseDrop @RegionEffects(reads java.lang.Object:All)".equals
         * (e.getAttribute(MESSAGE_ATTR))) {
         * System.out.println("Found proposal"); }
         */
        Category c = categories.getOrCreate(e);
        if (c != null) {
          c.addNew(e);
        } else {
          System.out.println("Couldn't categorize: " + e.getAttribute(MESSAGE_ATTR));
        }
      }
    }

    void diff() {
      if (categories.isEmpty()) {
        separate();
      }

      for (Category c : categories.values()) {
        // System.out.println("Category: "+c.name+" in "+c.file);
        c.match(System.out);
      }
    }

    public boolean isEmpty() {
      if (categories.isEmpty()) {
        return true;
      }
      for (Category c : categories.values()) {
        if (!c.isEmpty()) {
          return false;
        }
      }
      return true;
    }

    public void write(File file) throws IOException {
      OutputStream os = new FileOutputStream(file);
      Writer w = new OutputStreamWriter(os, "UTF-8");
      PrintWriter pw = new PrintWriter(w);
      for (Category c : categories.values()) {
        c.write(pw);
      }
      pw.flush();
      pw.close();
    }
  }

  static class Categories extends HashMap<Pair<String, String>, Category> {
    private static final long serialVersionUID = -9045897254507985423L;

    Category getOrCreate(Entity e) {
      final String typeName = e.getAttribute(TYPE_ATTR);
      final Class<?> type = DropTypeUtility.findType(typeName);
      if (type == null) {
        return null;
      }
      String path = e.getAttribute(PATH_ATTR);
      String uri = e.getAttribute(URI_ATTR);
      String file = e.getAttribute(FILE_ATTR);
      String f = uri == null ? file : path;
      if (f != null) {
        f = FileUtility.normalizePath(f);
      }
      final Pair<String, String> key = Pair.getInstance(f, type.getName());
      Category c = this.get(key);
      if (c == null) {
        c = new Category(f, key.second());
        this.put(key, c);
      }
      return c;
    }
  }

  static final Comparator<Entity> entityComparator = new Comparator<Entity>() {
    public int compare(Entity o1, Entity o2) {
      int rv = o1.getAttribute(MESSAGE_ATTR).compareTo(o2.getAttribute(MESSAGE_ATTR));
      if (rv == 0) {
        return o1.getDiffStatus().compareTo(o2.getDiffStatus());
      }
      return rv;
    }
  };

  /**
   * Storage for old and new drops that might match
   */
  public static class Category implements IViewable {
    public final String file;
    public final String name;
    final Set<Entity> old = new HashSet<Entity>();
    final Set<Entity> newer = new HashSet<Entity>();
    final Set<ContentDiff> diffs = new HashSet<ContentDiff>();

    public Category(String file, String name) {
      this.file = file;
      this.name = name;
    }

    public void write(PrintWriter w) {
      if (!isEmpty()) {
        w.println("Category: " + name + " in " + file);
        if (!diffs.isEmpty()) {
          for (ContentDiff d : diffs) {
            w.println("\tDiffs in details for " + d.n.getAttribute(MESSAGE_ATTR));
            final Map<String, Entity> oldDetails = extractDetails(d.o);
            final Map<String, Entity> newDetails = extractDetails(d.n);
            final List<String> temp = new ArrayList<String>();
            for (String old : sort(oldDetails.keySet(), temp)) {
              w.println("\t\tOld    : " + old);
            }
            for (String newMsg : sort(newDetails.keySet(), temp)) {
              w.println("\t\tNewer  : " + newMsg);
            }
          }
        }
        for (Entity o : old) {
          w.println("\tOld    : " + toString(o));
        }
        for (Entity o : newer) {
          w.println("\tNewer  : " + toString(o));
        }
        w.println();
      }
    }

    public boolean isEmpty() {
      if ("ProposedPromiseDrop".equals(name)) {
        // Ignore ones that disappeared -- most likely duplicates
        return XUtil.ignoreProposals || newer.isEmpty();
        // return true;
        /*
         * if (AbstractWholeIRAnalysis.useDependencies) { // Temporarily ignore
         * older for this, because they don't get cleaned out before each build
         * return newer.isEmpty(); } else { // Needed because we're now using a
         * separate JVM, so no extra drops from past runs return
         * newer.isEmpty(); //return old.isEmpty(); }
         */
      }
      return !hasChildren();
    }

    public void addOld(Entity e) {
      old.add(e);
    }

    public void addNew(Entity e) {
      newer.add(e);
    }

    private static boolean ignoreProposedEffectsPromises = false;

    public void match(PrintStream out) {
      String title = "Category: " + name + " in " + file;
      // Set<Entity> newCopy = new HashSet<Entity>(newer);
      // Set<Entity> oldCopy = new HashSet<Entity>(old);
      title = match(title, out, EXACT, "Exact  ");
      /*
       * if (name.endsWith("ProposedPromiseDrop") && !old.isEmpty()) { // Get
       * matched List<Entity> origOld = new ArrayList<Entity>(oldCopy);
       * oldCopy.removeAll(old); List<Entity> matched = new
       * ArrayList<Entity>(oldCopy); System.out.println("MAtching proposals"); }
       */
      title = match(title, out, HASHED, "Hashed ");
      title = match(title, out, HASHED2, "Hashed2");
      // title = match(title, out, SAME_LINE, "Line   ");
      if (name.endsWith(".ResultDrop")) {
        title = match(title, out, RESULT, "Results");
      }
      if (isEmpty()) {
        return;
      }

      if (title != null && hasChildren()) {
        out.println(title);
      }
      /*
       * for(Entity o : sortByOffset(old)) {
       * out.println("\tOld    : "+toString(o)); } for(Entity o :
       * sortByOffset(newer)) { out.println("\tNewer  : "+toString(o)); }
       */
      int ignored = 0;
      for (Entity o : sortByOffset(old)) {
        final String msg = toString(o);
        if (ignoreProposedEffectsPromises && msg.contains("ProposedPromiseDrop (EMPTY)")) {
          ignored++;
        } else {
          out.println("\tOld    : " + msg);
        }
      }
      if (ignored > 0) {
        out.println("\tOld    : " + ignored + " ProposedPromiseDrop(s)");
      }
      int numEffects = 0;
      int numProposals = 0;
      if (ignoreProposedEffectsPromises && (newer.size() == ignored || old.size() == 0)) {
        // Check to see that they're all ProposedPromiseDrops
        for (Entity o : newer) {
          final String msg = toString(o);
          if (msg.contains("ProposedPromiseDrop")) {
            numProposals++;
            if (msg.contains("ProposedPromiseDrop @RegionEffects(")) {
              numEffects++;
            }
          } else {
            break;
          }
        }
      }
      if (ignoreProposedEffectsPromises && numEffects > 0 && numEffects == newer.size()) {
        out.println("\tNewer  : " + numEffects + " ProposedPromiseDrop @RegionEffects");
      } else {
        if (numProposals > 0) {
          out.println("\tNewer  : " + newer.size() + " total, " + numProposals + " proposals, " + numEffects + " @RegionEffects");
        }
        for (Entity o : sortByOffset(newer)) {
          out.println("\tNewer  : " + toString(o));
        }
      }
    }

    Iterable<Entity> sortByOffset(Collection<Entity> c) {
      List<Entity> l = new ArrayList<Entity>(c);
      Collections.sort(l, new Comparator<Entity>() {
        public int compare(Entity o1, Entity o2) {
          return offset(o1) - offset(o2);
        }

        private int offset(Entity e) {
          String offset = e.getAttribute(OFFSET_ATTR);
          if (offset == null) {
            return -1;
          }
          return Integer.parseInt(offset);
        }
      });
      return l;
    }

    private String match(String title, PrintStream out, Matcher m, String label) {
      Iterator<Entity> it = newer.iterator();
      while (it.hasNext()) {
        Entity n = it.next();
        for (Entity o : old) {
          if (m.match(n, o)) {
            if (!"Exact  ".equals(label) && !"Hashed ".equals(label)) {
              if (title != null) {
                out.println(title);
                title = null;
              }
              out.println("\t" + label + ": " + toString(n));
            }
            old.remove(o);
            it.remove();
            ContentDiff d = ContentDiff.compute(out, n, o);
            if (d != null) {
              diffs.add(d);
            }
            break;
          }
        }
      }
      return title;
    }

    public static String toString(Entity e) {
      return e.getAttribute(OFFSET_ATTR) + " - " + e.getAttribute(HASH_ATTR) + " - " + e.getAttribute(CONTEXT_ATTR) + " - "
          + e.getAttribute(PROVED_ATTR) + " - " + e.getAttribute(MESSAGE_ID_ATTR) + " - " + e.getAttribute(MESSAGE_ATTR);
    }

    public boolean hasChildren() {
      return !old.isEmpty() || !newer.isEmpty() || !diffs.isEmpty();
    }

    public Object[] getChildren() {
      Entity[] a = new Entity[old.size() + newer.size()];
      int i = 0;
      for (Entity o : old) {
        a[i] = o;
        o.setAsOld();
        i++;
      }
      for (Entity o : newer) {
        a[i] = o;
        o.setAsNewer();
        i++;
      }
      Arrays.sort(a, entityComparator);
      if (diffs.isEmpty()) {
        return a;
      }
      List<Object> temp = new ArrayList<Object>(diffs.size() + a.length);
      temp.addAll(diffs);
      Collections.sort(temp, new Comparator<Object>() {
        public int compare(Object o1, Object o2) {
          return o1.toString().compareTo(o2.toString());
        }
      });
      for (Entity e : a) {
        temp.add(e);
      }
      return temp.toArray();
    }

    public String getText() {
      if (file == null) {
        return name;
      }
      return name + "  in  " + file;
    }
  }

  public static class ContentDiff implements IViewable {
    final Entity n, o;
    final Object[] children;

    public ContentDiff(Entity n, Entity o, Object[] children) {
      this.children = children;
      this.n = n;
      this.o = o;
    }

    @Override
    public String toString() {
      return n.getAttribute(MESSAGE_ATTR);
    }

    public static ContentDiff compute(PrintStream out, Entity n, Entity o) {
      if (!o.hasRefs()) {
        if (!n.hasRefs()) {
          return null;
        }
        if (allowMissingSupportingInfos) {
          // System.out.println("Temporarily ignoring missing details in old oracles");
          return null;
        }
      }
      final Map<String, Entity> oldDetails = extractDetails(o);
      final Map<String, Entity> newDetails = extractDetails(n);
      final List<String> temp = new ArrayList<String>();
      // Remove matching ones
      for (String ns : newDetails.keySet()) {
        Entity oe = oldDetails.remove(ns);
        if (oe != null) {
          temp.add(ns);
        }
      }
      for (String match : temp) {
        newDetails.remove(match);
      }

      if (oldDetails.isEmpty() && newDetails.isEmpty()) {
        return null;
      }
      out.println("\tDiffs in details for " + n.getAttribute(MESSAGE_ATTR));
      for (String old : sort(oldDetails.keySet(), temp)) {
        out.println("\t\tOld    : " + old);
        Entity e = oldDetails.get(old);
        e.setAsOld();
      }
      for (String newMsg : sort(newDetails.keySet(), temp)) {
        out.println("\t\tNewer  : " + newMsg);
        Entity e = newDetails.get(newMsg);
        e.setAsNewer();
      }
      List<Entity> remaining = new ArrayList<Entity>(oldDetails.size() + newDetails.size());
      remaining.addAll(oldDetails.values());
      remaining.addAll(newDetails.values());
      Collections.sort(remaining, entityComparator);
      return new ContentDiff(n, o, remaining.toArray());
    }

    // @Override
    public Object[] getChildren() {
      return children;
    }

    // @Override
    public String getText() {
      return toString();
    }

    // @Override
    public boolean hasChildren() {
      return true;
    }
  }

  private static Collection<String> sort(Collection<String> s, List<String> temp) {
    temp.clear();
    temp.addAll(s);
    Collections.sort(temp);
    return temp;
  }

  // Assume that we only have supporting info
  private static Map<String, Entity> extractDetails(Entity e) {
    if (!e.hasRefs()) {
      return Collections.emptyMap();
    }
    final Map<String, Entity> rv = new TreeMap<String, Entity>();
    for (Entity i : e.getReferences()) {
      String msg = i.getAttribute(MESSAGE_ATTR);
      if (msg != null) {
        rv.put(msg, i);
      } else {
        System.out.println("No message for " + i.getEntityName());
      }
    }
    return rv;
  }

  static abstract class Matcher {
    static boolean match(Entity n, Entity o, String... attrs) {
      return match(n, o, false, attrs);
    }

    static boolean match(Entity n, Entity o, boolean defaultValue, String... attrs) {
      return match(false, n, o, defaultValue, attrs);
    }

    static boolean match(boolean nullMeansFalse, Entity n, Entity o, boolean defaultValue, String... attrs) {
      for (String attr : attrs) {
        String a_n = n.getAttribute(attr);
        String a_o = o.getAttribute(attr);
        if (a_n == null) {
          if (nullMeansFalse) {
            a_n = "false";
          } else {
            continue; // Skip this attribute
          }
        } else {
          a_n = a_n.trim();
        }
        if (a_o == null) {
          if (nullMeansFalse) {
            a_o = "false";
          } else {
            continue; // Skip this attribute
          }
        } else {
          a_o = a_o.trim();
        }
        /*
         * // Temporary a_n = a_n.replaceAll("  on  ", " on "); a_o =
         * a_o.replaceAll("  on  ", " on ");
         */
        /*
         * if (PROVED_ATTR.equals(attr)) {
         * System.out.println("Comparing "+a_n+" to "+a_o+" for "+attr); }
         */
        // return a_n.equalsIgnoreCase(a_o);
        return a_n.equals(a_o);
      }
      return defaultValue;
    }

    boolean match(Entity n, Entity o) {
      return match(n, o, true, CATEGORY_ATTR) && match(n, o, MESSAGE_ID_ATTR, MESSAGE_ATTR) && match(true, n, o, true, PROVED_ATTR)
          && matchSupportingInfo(n, o);
    }

    boolean matchSupportingInfo(Entity n, Entity o) {
      final long oh = computeSIHash(o);
      if (allowMissingSupportingInfos && oh == 0) {
        return true;
      }
      final long nh = computeSIHash(n);
      return nh == oh;
    }

    long computeSIHash(Entity e) {
      long rv = 0;
      for (Entity i : e.getReferences()) {
        String msg = i.getAttribute(MESSAGE_ATTR);
        if (msg == null) {
          continue;
        }
        rv += msg.hashCode();
      }
      return rv;
    }
  }

  static final Matcher EXACT = new Matcher() {
    @Override
    boolean match(Entity n, Entity o) {
      boolean rv = super.match(n, o) && match(n, o, OFFSET_ATTR);
      /*
       * if (!rv) { boolean m1 = match(n, o, true, CATEGORY_ATTR); boolean m2 =
       * match(n, o, MESSAGE_ID_ATTR, MESSAGE_ATTR); boolean m3 = match(true, n,
       * o, true, PROVED_ATTR); boolean m4 = match(n, o, OFFSET_ATTR); }
       */
      return rv;
    }
  };

  static final Matcher HASHED = new Matcher() {
    @Override
    boolean match(Entity n, Entity o) {
      return super.match(n, o) && match(n, o, HASH_ATTR) && match(n, o, CONTEXT_ATTR);
    }
  };

  static final Matcher HASHED2 = new Matcher() {
    @Override
    boolean match(Entity n, Entity o) {
      return super.match(n, o) && match(n, o, HASH_ATTR);
    }
  };

  static final Matcher SAME_LINE = new Matcher() {
    @Override
    boolean match(Entity n, Entity o) {
      return super.match(n, o) && match(n, o, LINE_ATTR);
    }
  };

  static final Matcher RESULT = new Matcher() {
    @Override
    boolean match(Entity n, Entity o) {
      return match(n, o, HASH_ATTR) && false;
    }
  };
}
