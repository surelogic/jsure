package com.surelogic.dropsea.irfree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.java.persistence.JSureScanInfo;

import edu.cmu.cs.fluid.util.CPair;

/**
 * Diff code for IRFreeDrops
 * 
 * @author Edwin
 */
public class SeaSnapshotDiff<K extends Comparable<K>> implements ISeaDiff {
  private final Map<K, DiffCategory<K>> categories = new HashMap<>();
  private IDropFilter filter;
  private IDropSeparator<K> separator;
  private CategoryMatcher matcher = defaultMatcher;
  private final PrintStream out; 
  
  public SeaSnapshotDiff() {
	  this(System.out);
  }
  
  public SeaSnapshotDiff(PrintStream o) {
	  if (o == null) {
	      throw new IllegalArgumentException();
	  }
	  out = o;
  }
  
  public void setFilter(IDropFilter f) {
    if (f == null) {
      throw new IllegalArgumentException();
    }
    if (filter != null) {
      throw new IllegalStateException();
    }
    filter = f;
  }

  public void setSeparator(IDropSeparator<K> s) {
    if (s == null) {
      throw new IllegalArgumentException();
    }
    if (separator != null) {
      throw new IllegalStateException();
    }
    separator = s;
  }

  public void setMatcher(CategoryMatcher c) {
    if (c == null) {
      throw new IllegalArgumentException();
    }
    if (matcher != defaultMatcher) {
      throw new IllegalStateException();
    }
    matcher = c;
  }

  @Override
  public boolean isEmpty() {
    if (categories.isEmpty()) {
      return true;
    }
    for (DiffCategory<K> c : categories.values()) {
      if (!c.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings({ "rawtypes" })
  public static final DiffCategory[] noCategories = new DiffCategory[0];

  @Override
  @SuppressWarnings("unchecked")
  public DiffCategory<K>[] getCategories() {
    if (categories.isEmpty()) {
      return noCategories;
    }
    List<DiffCategory<K>> l = new ArrayList<>();
    for (DiffCategory<K> c : categories.values()) {
      if (c.isEmpty()) {
        continue;
      }
      l.add(c);
    }
    Collections.sort(l);
    return l.toArray(new DiffCategory[l.size()]);
  }

  @Override
  public void write(File file) throws IOException {
    OutputStream os = new FileOutputStream(file);
    Writer w = new OutputStreamWriter(os, "UTF-8");
    PrintWriter pw = new PrintWriter(w);
    for (DiffCategory<K> c : categories.values()) {
      c.write(pw);
    }
    pw.flush();
    pw.close();
  }

  /**
   * @return true if non-empty
   */
  public boolean build(Collection<? extends IDrop> old, Collection<? extends IDrop> newer) {
    if (!categories.isEmpty()) {
      throw new IllegalStateException("Already built");
    }
    if (filter != null) {
      //final int oldSize = old.size();
      //final int newerSize = newer.size();
      // do I need to keep it around?
      old = filter(filter, old);
      newer = filter(filter, newer);
      //out.println("Old  : "+oldSize+" -> "+old.size());
      //out.println("Newer: "+newerSize+" -> "+newer.size());
    }
    if (separator != null) {
      separateIntoCategories(old, newer);

      // Check if we lost any 
      int numOld = 0;
      int numNew = 0;
      for(DiffCategory<K> c : categories.values()) {
    	  numOld += c.old.size();
    	  numNew += c.newer.size();
      }
      if (old.size() != numOld) {
    	  throw new IllegalStateException("Lost some old drops");
      }
      if (newer.size() != numNew) {
    	  throw new IllegalStateException("Lost some new drops");
      }
    } else {
      DiffCategory<K> all = new DiffCategory<>(null);
      all.addAllOld(old);
      all.addAllNew(newer);
      categories.put(null, all);
    }
    if (matcher != null) {
      // TODO do in parallel?
      for (DiffCategory<K> c : categories.values()) {
        c.diff(out, matcher);
      }
    }
    return true;
  }

  private static List<IDrop> filter(IDropFilter f, Collection<? extends IDrop> l) {
    final List<IDrop> drops = new ArrayList<>();
    // Collections.sort(oldDrops, EntityComparator.prototype);
    for (IDrop d : l) {
      if (f.keep(d)) {
        drops.add(d);
      }
    }
    return drops;
  }

  private void separateIntoCategories(Collection<? extends IDrop> old, Collection<? extends IDrop> newer) {
    for (IDrop d : old) {
      DiffCategory<K> category = getOrCreateCategory(d);
      if (category != null) {
        category.addOld(d);
      }
    }
    for (IDrop d : newer) {
      DiffCategory<K> category = getOrCreateCategory(d);
      if (category != null) {
        category.addNew(d);
      }
    }
  }

  private DiffCategory<K> getOrCreateCategory(IDrop d) {
    K key = separator.makeKey(d);
    if (key == null) {
      return null;
    }
    DiffCategory<K> c = categories.get(key);
    if (c == null) {
      c = new DiffCategory<>(key);
      categories.put(key, c);
    }
    return c;
  }

  /**
   * @return true if the drop is derived from source
   */
  static boolean select(IDrop d) {
    /* No longer desirable, since it makes these drops 
     * show up as 'new'
     *
    if (d instanceof IProofDrop) {
      IProofDrop pd = (IProofDrop) d;
      return pd.derivedFromSrc();
    }
    */
	if (!d.includeInDiff()) {
	  // Ignore these
      return false;
	}
    // Need a location to report
    IJavaRef ref = d.getJavaRef();
    if (ref == null) {
      if (!(d instanceof IResultFolderDrop) && !d.getMessage().contains(SLUtility.JAVA_LANG_OBJECT) && 
    	  !d.getMessage().contains("Field length is final")) {
        System.out.println("No src ref for " + d.getMessage());
      }
      return false;
    }
    return true;
  }
  
  public static IDropFilter augmentDefaultFilter(final IDropFilter f) {
	  return new IDropFilter() {
		  // @Override
		  @Override
      public boolean keep(IDrop d) {
			  return select(d) && f.keep(d);
		  }
	  };
  }

  public static SeaSnapshotDiff<CPair<String, String>> diff(final IDropFilter f, File old, File newer) throws Exception {
	Collection<IDrop> newerResults = newer.isDirectory() ? new JSureScanInfo(new JSureScan(newer)).getDropInfo() :
		SeaSnapshotXMLReader.loadSnapshot(newer);
	Collection<IDrop> oldResults = old.isDirectory() ? new JSureScanInfo(new JSureScan(old)).getDropInfo() :
		SeaSnapshotXMLReader.loadSnapshot(old);
    return diff(f, oldResults, newerResults);
  }

  public static SeaSnapshotDiff<CPair<String, String>> diff(final IDropFilter f, File old, JSureScanInfo newer)
      throws Exception {
    Collection<IDrop> oldResults = old.isDirectory() ? new JSureScanInfo(new JSureScan(old)).getDropInfo() :
    	SeaSnapshotXMLReader.loadSnapshot(old);
    return diff(f, oldResults, newer.getDropInfo());
  }
	  
  /*
  private static SeaSnapshotDiff<CPair<String, String>> diff(final IDropFilter f, JSureScanInfo old, JSureScanInfo newer) {
	  
  }
  */
  
  private static SeaSnapshotDiff<CPair<String, String>> diff(final IDropFilter f, Collection<IDrop> old,
      Collection<? extends IDrop> newer) {
	System.out.println("old/newer: "+old.size()+" vs "+newer.size());
    SeaSnapshotDiff<CPair<String, String>> rv = new SeaSnapshotDiff<>();
    rv.setFilter(augmentDefaultFilter(f));
    rv.setSeparator(defaultSeparator);
    rv.build(old, newer);
    return rv;
  }
  
  public static final CategoryMatcher defaultMatcher = new DefaultCategoryMatcher();
  
  public static final IDropSeparator<CPair<String, String>> defaultSeparator = 
	  new IDropSeparator<CPair<String, String>>() {
	  // @Override
	  @Override
    public CPair<String, String> makeKey(IDrop d) {
		  final String type = DropTypeUtility.mapFullName(d.getFullClassName());
		  if (type == null) {
			  return null;
		  }
		  IJavaRef ref = d.getJavaRef();
		  // String f = ref == null ? "" : ref.getTypeNameFullyQualified();
		  String f = ref == null ? "" : ref.getPackageName() + '/' + ref.getSimpleFileNameWithNoExtension();
		  /*
		   * String f = ""; if (ref != null) { IDecl decl = ref.getDeclaration();
		   * if (decl != null) { // TODO not quite right if there's more than one
		   * top-level type in the file f =
		   * DeclUtil.getTypeNameFullyQualifiedOutermostTypeNameOnly(decl); } if
		   * (f == null) { f = ref.getTypeNameFullyQualified(); } }
		   */
		  return new CPair<>(f, type);
	  }
  };

//  @Override
  @Override
  public ScanDifferences build() {
	  ScanDifferences.Builder diffs = new ScanDifferences.Builder();
	  for(DiffCategory<?> c : categories.values()) {		  
		  diffs.addAllNewSameAsOld(c.newMatchingOld);
		  for(DropDiff d : c.diffs) {
			  diffs.addNewChangedFromOld(d.drop, d.old);
		  }
		  for(DiffNode n : c.newer) {
			  diffs.addNew(n.drop);
		  }
	  }
	  return diffs.build();
  }  
}
