package com.surelogic.dropsea.irfree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.*;

import com.surelogic.common.FileUtility;
import com.surelogic.dropsea.*;
import com.surelogic.dropsea.ir.IRReferenceDrop;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.util.CPair;

/**
 * Diff code for IRFreeDrops
 * 
 * @author Edwin
 */
public class SeaSnapshotDiff<K extends Comparable<K>> implements ISeaDiff {
	private final Map<K, DiffCategory<K>> categories = new HashMap<K, DiffCategory<K>>();
	private IDropFilter filter;
	private IDropSeparator<K> separator;
	private DropMatcher matcher;

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
	
	public void setMatcher(DropMatcher c) {
		if (c == null) {
			throw new IllegalArgumentException();
		}
		if (matcher != null) {
			throw new IllegalStateException();
		}
		matcher = c;
	}

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
	private static final DiffCategory[] noCategories = new DiffCategory[0];
	
	@SuppressWarnings("unchecked")
	public DiffCategory<K>[] getCategories() {
		if (categories.isEmpty()) {
			return noCategories;
		}
		List<DiffCategory<K>> l = new ArrayList<DiffCategory<K>>();
		for (DiffCategory<K> c : categories.values()) {
			if (c.isEmpty()) {
				continue;
			}
			l.add(c);
		}
		Collections.sort(l);
		return l.toArray(new DiffCategory[l.size()]);
	}
	
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
			// do I need to keep it around?
			old = filter(filter, old);			
			newer = filter(filter, newer);
		}
		if (separator != null) {
			separateIntoCategories(old, newer);
		} else {
			DiffCategory<K> all = new DiffCategory<K>(null);
			all.addAllOld(old);
			all.addAllNew(newer);
			categories.put(null, all);			
		}
		if (matcher != null) {
			// TODO do in parallel?
			for(DiffCategory<K> c : categories.values()) {
				c.diff(System.out, matcher);
			}
		}
		return true;
	}

	private static List<IDrop> filter(IDropFilter f, Collection<? extends IDrop> l) {
	    final List<IDrop> drops = new ArrayList<IDrop>();
	    // Collections.sort(oldDrops, EntityComparator.prototype);
	    for (IDrop d : l) {	    	    	
	      ISrcRef ref = d.getSrcRef();
	      if (ref != null) {
	    	  String path = ref.getRelativePath();
	    	  if (path == null || f.showResource(d)) {
	    		  drops.add(d);
	    	  }
	      } else {
	    	  drops.add(d);
	      }
	    }
	    return drops;
	}
	
	private void separateIntoCategories(Collection<? extends IDrop> old, Collection<? extends IDrop> newer) {
		for(IDrop d : old) {
			DiffCategory<K> category = getOrCreateCategory(d);
			if (category != null) {
				category.addOld(d);
			}
		}
		for(IDrop d : newer) {
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
			c = new DiffCategory<K>(key);
			categories.put(key, c);
		}
		return c;
	}
			
	/**
	 * @return true if the drop is derived from source
	 */
	private static boolean select(IDrop d) {
		if (d instanceof IProofDrop) {
			IProofDrop pd = (IProofDrop) d;
			return pd.derivedFromSrc();
		}
		if (d.instanceOfIRDropSea(IRReferenceDrop.class)) {
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
		        return false;
		      }
		      return true;
		}
		return false;
	}
	
	public static SeaSnapshotDiff<CPair<String,String>> diff(final IDropFilter f, File old, Collection<? extends IDrop> newer) 
	throws Exception {
		Collection<IDrop> oldResults = SeaSnapshot.loadSnapshot(old);
		return diff(f, oldResults, newer);
	}
	
	public static SeaSnapshotDiff<CPair<String,String>> diff(final IDropFilter f, Collection<IDrop> old, Collection<? extends IDrop> newer) {
		SeaSnapshotDiff<CPair<String,String>> rv = new SeaSnapshotDiff<CPair<String,String>>();
		rv.setFilter(new IDropFilter() {
//			@Override
			public boolean showResource(IDrop d) {
				return select(d) && f.showResource(d);
			}

//			@Override
			public boolean showResource(String path) {
				return f.showResource(path);
			}
			
		});
		rv.setSeparator(new IDropSeparator<CPair<String,String>>() {
//			@Override
			public CPair<String, String> makeKey(IDrop d) {
				final Class<?> type = d.getIRDropSeaClass();
			    if (type == null) {
			    	return null;
			    }
			    ISrcRef ref = d.getSrcRef();
			    String f = null;
			    if (ref != null) {
			    	String path = ref.getRelativePath();
			    	URI uri = ref.getEnclosingURI();
			    	String file = ref.getEnclosingFile();
			    	f = uri == null ? file : path;
			    	if (f != null) {
			    		f = FileUtility.normalizePath(f);
			    	}
			    }
			    return new CPair<String,String>(f, type.getName());
			}
		});
		rv.setMatcher(new DropMatcher("Exact  ", "Core   ", "Hashed ", "Hashed2", "Results") {
			@Override
			protected boolean warnIfMatched(int pass) {				
				return pass >= 4;
			}
			
			@Override
			protected boolean match(int pass, IDrop n, IDrop o) {
				switch (pass) {
				case 0:
					return matchExact(n, o);
				case 1:				
					return matchCore(n, o);
				case 2:					
					return matchHashedAndHints(n, o);
				case 3:
					return matchHashed(n, o);
				case 4:
					return matchResults(n, o);
				default:
					return false;
				}
			}
			private boolean matchExact(IDrop n, IDrop o) {
				return matchCore(n, o) && matchSupportingInfo(n, o);
			}
			
			private boolean matchCore(IDrop n, IDrop o) {
				return matchBasics(n, o) && matchLong(getOffset(n), getOffset(o));
			}
			
			private boolean matchHashedAndHints(IDrop n, IDrop o) {
				return matchHashed(n, o) && matchSupportingInfo(n, o);
			}
			
			private boolean matchHashed(IDrop n, IDrop o) {
				return matchBasics(n, o) && 
				       matchLong(n.getTreeHash(), o.getTreeHash()) && 
				       matchLong(n.getContextHash(), o.getContextHash());
			}
			/*
			private boolean matchHashed2(IDrop n, IDrop o) {
				return matchBasics(n, o) && matchLong(n.getTreeHash(), o.getTreeHash());				
			}
			*/
			private boolean matchResults(IDrop n, IDrop o) {
				return (n instanceof IResultDrop) && matchLong(n.getTreeHash(), o.getTreeHash());
			}
		});
		rv.build(old, newer);
		return rv;
	}
}
