package com.surelogic.dropsea.irfree;

import java.net.URI;
import java.util.*;

import com.surelogic.common.FileUtility;
import com.surelogic.dropsea.*;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.util.Pair;

/**
 * Diff code for IRFreeDrops
 * 
 * @author Edwin
 */
public class SeaSnapshotDiff<K> {
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

	/**
	 * @return true if non-empty
	 */
	public boolean build(List<IDrop> old, List<IDrop> newer) {
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

	private static List<IDrop> filter(IDropFilter f, List<IDrop> l) {
	    final List<IDrop> drops = new ArrayList<IDrop>();
	    // Collections.sort(oldDrops, EntityComparator.prototype);
	    for (IDrop d : l) {	    	    	
	      ISrcRef ref = d.getSrcRef();
	      if (ref != null) {
	    	  String path = ref.getRelativePath();
	    	  if (path == null || f.showResource(path)) {
	    		  drops.add(d);
	    	  }
	      } else {
	    	  drops.add(d);
	      }
	    }
	    return drops;
	}
	
	private void separateIntoCategories(List<IDrop> old, List<IDrop> newer) {
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
			
	public static SeaSnapshotDiff<Pair<String,String>> diff(IDropFilter f, List<IDrop> old, List<IDrop> newer) {
		SeaSnapshotDiff<Pair<String,String>> rv = new SeaSnapshotDiff<Pair<String,String>>();
		rv.setFilter(f);
		rv.setSeparator(new IDropSeparator<Pair<String,String>>() {
			@Override
			public Pair<String, String> makeKey(IDrop d) {
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
			    return Pair.getInstance(f, type.getName());
			}
		});
		rv.setMatcher(new DropMatcher("Exact  ", "Hashed ", "Hashed2", "Results") {
			@Override
			protected boolean match(int pass, IDrop n, IDrop o) {
				switch (pass) {
				case 0:
					return matchExact(n, o);
				case 1:
					return matchHashed(n, o);
				case 2:
					return matchHashed2(n, o);
				case 3:
					return matchResults(n, o);
				default:
					return false;
				}
			}
			private boolean matchExact(IDrop n, IDrop o) {
				return matchBasics(n, o) && getOffset(n) == getOffset(o);
			}
			private boolean matchHashed(IDrop n, IDrop o) {
				return matchBasics(n, o) && 
				       matchLong(n.getTreeHash(), o.getTreeHash()) && 
				       matchLong(n.getContextHash(), o.getContextHash());
			}
			private boolean matchHashed2(IDrop n, IDrop o) {
				return matchBasics(n, o) && matchLong(n.getTreeHash(), o.getTreeHash());				
			}
			private boolean matchResults(IDrop n, IDrop o) {
				return (n instanceof IResultDrop) && matchLong(n.getTreeHash(), o.getTreeHash());
			}
		});
		rv.build(old, newer);
		return rv;
	}
}
