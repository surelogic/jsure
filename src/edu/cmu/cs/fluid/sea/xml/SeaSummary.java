package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.jsure.xml.JSureSummaryXMLReader.*;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.common.FileUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.xml.*;
import com.surelogic.jsure.xml.JSureSummaryXMLReader;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaUnparseStyle;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.threadroles.IThreadRoleDrop;
import edu.cmu.cs.fluid.util.Hashtable2;

/**
 * Summarize the Sea (omits links)
 * 
 * @author Edwin
 */
public class SeaSummary extends AbstractSeaXmlCreator {
	private static final String COUNT = "count";
	
	private SeaSummary(File location) throws IOException {
		super(location);
	}
	
	public static File findSummary(String projectPath) {
		String xmlOracle = null;
		File xmlLocation = null;
		if (IDE.useJavac) {
			xmlOracle = RegressionUtility.getOracleName(projectPath, RegressionUtility.javacOracleFilter, "oracleJavac"+SeaSnapshot.SUFFIX);
			xmlLocation = new File(xmlOracle);    			  
			System.out.println("Looking for " + xmlOracle);
		}
		if (true) { 
			String tempOracle = RegressionUtility.getOracleName(projectPath, RegressionUtility.xmlOracleFilter, "oracle"+SeaSnapshot.SUFFIX);
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
		//if (XUtil.testing) {
			System.out.println(date1+" ?= "+date2+" => "+(rv ? "first" : "second"));
		//}
		return rv;
	}

	private static String getDate(String oracle) {		
		// Start with last segment 
		for(int i=oracle.lastIndexOf(File.separatorChar)+1; i<oracle.length(); i++) {
			if (Character.isDigit(oracle.charAt(i))) {
				return oracle.substring(i);
			}
		}
		return oracle;
	}
	
	public static void summarize(String project, final Sea sea, File location) 
	throws IOException {
		SeaSummary s = new SeaSummary(location);
		s.summarize(project, sea);
	}

	private void summarize(String project, Sea sea) {
		Date now = new Date(System.currentTimeMillis());
		Entities.start(ROOT, b);
		addAttribute(TIME_ATTR, DateFormat.getDateTimeInstance().format(now));
		addAttribute(PROJECT_ATTR, project);
		b.append(">\n");
		flushBuffer(pw);
		
		final Set<Drop> drops = sea.getDrops();
		outputDropCounts(drops);
		
		for(Drop d : drops) {
			final IRReferenceDrop id = checkIfReady(d);
			if (id != null) {
				summarizeDrop(id);
			}
		}
		pw.println("</"+ROOT+">\n");
		pw.close();
	}
		
	private void outputDropCounts(Set<Drop> drops) {
		final Map<Class<?>,Integer> counts = new HashMap<Class<?>, Integer>();
		for(Drop d : drops) {
			final IRReferenceDrop id = checkIfReady(d);
			if (id != null) {
				incr(counts, d.getClass());
			}
		}		
		for(Map.Entry<Class<?>, Integer> e : counts.entrySet()) {
			Entities.start(COUNT, b);
			addAttribute(e.getKey().getSimpleName(), e.getValue().toString());
			b.append("/>");
			flushBuffer(pw);
		}
	}

	private static <T> void incr(Map<T,Integer> counts, T key) {
		Integer i = counts.get(key);
		if (i == null) {
			i = 1;
		} else {
			i++;
		}
		counts.put(key, i);
	}
	
	@SuppressWarnings("unchecked")
	private static IRReferenceDrop checkIfReady(Drop d) {
		if (d instanceof PromiseDrop) {
			PromiseDrop pd = (PromiseDrop) d;
			if (!pd.isFromSrc()) {
				// no need to do anything
				return null;
			} 
		}
		// TODO skipping for now
		if (d instanceof IThreadRoleDrop) {
			return null;
		}
		if (d instanceof PromiseWarningDrop && d.getMessage().contains("ThreadRole")) {
			return null;
		}
		if (d instanceof IRReferenceDrop) {
			// Need a location to report
			IRReferenceDrop id = (IRReferenceDrop) d;
			ISrcRef ref = id.getSrcRef();
			if (ref == null) {
				if (id.getNode() != null && !d.getMessage().contains("java.lang.Object")) {
					/*
                    if (d.getMessage().startsWith("ThreadRole")) {
						System.out.println("Found ThreadRole");
					}
					*/
					System.out.println("No src ref for "+d.getMessage());
				} else {
					//System.currentTimeMillis();
				}
				return null;				
			}	
			return id;
		}
		return null;
	}

	private void summarizeDrop(IRReferenceDrop id) {					
		reset();
		
		final String name = id.getEntityName();	
		Entities.start(name, b);

		addAttributes(id);
		b.append("/>\n");
		//b.append("</"+name+">\n");
		flushBuffer(pw);
	}

	private void addAttributes(IRReferenceDrop id) {
		final String type = id.getClass().getSimpleName();
		addAttribute(TYPE_ATTR, type);
		//addAttribute(MESSAGE_ATTR, id.getMessage());
		
		ISrcRef ref = id.getSrcRef();
		addLocation(ref);
		//addAttribute(OFFSET_ATTR, (long) ref.getOffset());
		addAttribute(HASH_ATTR, computeHash(id.getNode(), false));
		addAttribute(CONTEXT_ATTR, computeContext(id.getNode(), false));
		//addAttribute("unparse", unparser.unparseString(id.getNode()));
		// Omitting supporting info
		/*
		if (id.getMessage().contains("copyStat")) {
			computeHash(id.getNode(), true);
		}
        */		
		id.snapshotAttrs(this);
	}

	private static final JavaUnparseStyle noPromisesStyle = new JavaUnparseStyle(false, false);
	private static final DebugUnparser unparser = new DebugUnparser(5, JJNode.tree) {
		@Override
		public JavaUnparseStyle getStyle() {
			return noPromisesStyle;
		}
	};
	
	public static long computeHash(IRNode node) {
		return computeHash(node, false);
	}
	
	private static long computeHash(IRNode node, boolean debug) {			
		final String unparse = unparser.unparseString(node);
		if (debug) {
			System.out.println("Unparse: "+unparse);
		}
		return unparse.hashCode();
	}
	
	private long computeContext(IRNode node, boolean debug) {			
		final String context = JavaNames.computeContextId(node);
		if (context != null) {
			if (debug) {
				System.out.println("Context: "+context);
			}
			/*
			if (unparse.contains("@") {
				System.out.println("Found promise");
			}
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
	
	public static Diff diff(File location1, File location2) throws Exception {
		final Listener l1 = read(location1);
		final Listener l2 = read(location2);
		Diff d = new Diff(l1.drops, l2.drops);
		d.diff();
		return d;
	}
	
	public static Diff diff(String project, final Sea sea, File location)
	throws Exception {
		// Load up current contents
		final Listener l = read(location);
		
		final List<Entity> oldDrops = l.drops;
		//Collections.sort(oldDrops, EntityComparator.prototype);
	
		final SeaSummary s = new SeaSummary(null);
		final List<Entity> newDrops = new ArrayList<Entity>();
		for(Drop d : sea.getDrops()) {
			IRReferenceDrop id = checkIfReady(d);
			if (id != null) {
				s.reset();
				s.summarizeDrop(id);
				/*
				if (id.getClass().equals(PromisePromiseDrop.class) && id.getMessage().contains("InRegion(TotalRegion)")) {
					System.out.println("Found scoped promise: "+id.getMessage());
				}
*/
				Entity e = new Entity(id.getEntityName(), s.attributes);
				newDrops.add(e);
			}
		}
		//Collections.sort(newDrops, EntityComparator.prototype);
		Diff d = new Diff(oldDrops, newDrops);
		d.diff();
		return d;
	}
	
	static class Listener implements IXMLResultListener {
		final Map<String,String> officialCounts = new HashMap<String, String>();
		final Map<String,Integer> counts = new HashMap<String, Integer>();
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
				SLLogger.getLogger().log(Level.SEVERE, "Could not parse "+time);
			}
		}

		public void notify(Entity e) {
			if (COUNT.equals(e.getName())) {
				for(Map.Entry<String, String> me : e.getAttributes().entrySet()) {
					String old = officialCounts.put(me.getKey(), me.getValue());
					if (old != null) {
						throw new IllegalStateException("Duplicate count for "+me.getKey());
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
				for(Map.Entry<String, String> e : officialCounts.entrySet()) {
					final Integer i = counts.get(e.getKey());
					if (i == null || !e.getValue().equals(i.toString())) {
						success = false;
						System.out.println(e.getKey()+": "+e.getValue()+" != "+i);
					}
				}
				for(Map.Entry<String, Integer> e : counts.entrySet()) {
					final String value = officialCounts.get(e.getKey());
					if (value == null || !value.equals(e.getValue().toString())) {
						success = false;
						System.out.println(e.getKey()+": "+value+" != "+e.getValue());
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
			for(Category c : categories.elements()) {
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
			for(Entity e : oldDrops) {
				Category c = categories.getOrCreate(e);
				c.addOld(e);
			}
			for(Entity e : newDrops) {
				Category c = categories.getOrCreate(e);
				c.addNew(e);
			}
		}
		
		void diff() {
			if (categories.isEmpty()) {
				separate();
			}
			
			for(Category c : categories.elements()) {
				//System.out.println("Category: "+c.name+" in "+c.file);
				c.match(System.out);
			}
		}

		public boolean isEmpty() {
			if (categories.isEmpty()) {
				return true;
			}
			for(Category c : categories.elements()) {
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
			for(Category c : categories.elements()) {
				c.write(pw);
			}
			pw.flush();
			pw.close();
		}
	}
	
	static class Categories extends Hashtable2<String,String,Category> {
		Category getOrCreate(Entity e) {	
			final String type = e.getAttribute(TYPE_ATTR);
			String file = e.getAttribute(PATH_ATTR);
			if (file == null) {
				file = e.getAttribute(URI_ATTR);
			}
			if (file == null) {
				file = e.getAttribute(FILE_ATTR);
			}
			if (file != null) {
				file = FileUtility.normalizePath(file);
			}
			Category c = this.get(file, type);
			if (c == null) {
				c = new Category(file, type);
				this.put(file, type, c);
			}
			return c;
		}
	}
	
	/**
	 * Storage for old and new drops that might match
	 */
	public static class Category {
		public final String file;
		public final String name;		
		final Set<Entity> old = new HashSet<Entity>();
		final Set<Entity> newer = new HashSet<Entity>();
		
		public Category(String file, String name) {
			this.file = file;
			this.name = name;
		}

		public void write(PrintWriter w) {
			if (!isEmpty()) {
				w.println("Category: "+name+" in "+file);
				for(Entity o : old) {
					w.println("\tOld    : "+toString(o));
				}
				for(Entity o : newer) {
					w.println("\tNewer  : "+toString(o));
				}
				w.println();
			}
		}

		public boolean isEmpty() {
			if ("ProposedPromiseDrop".equals(name)) {
				if (AbstractWholeIRAnalysis.useDependencies) {
					// Temporarily ignore older for this, because they don't get cleaned out before each build
					return newer.isEmpty(); 
				} else {
					// No longer needed?
					//return old.isEmpty();
				}
			}
			return old.isEmpty() && newer.isEmpty();
		}

		public void addOld(Entity e) {
			old.add(e);
		}

		public void addNew(Entity e) {
			newer.add(e);
		}
		
		private static boolean ignoreProposedEffectsPromises = false;
		
		public void match(PrintStream out) {
			String title = "Category: "+name+" in "+file;
			title = match(title, out, EXACT,  "Exact  ");
			title = match(title, out, HASHED, "Hashed ");
			title = match(title, out, HASHED2, "Hashed2");
			//title = match(title, out, SAME_LINE,  "Line   ");
			if ("ResultDrop".equals(name)) {
				title = match(title, out, RESULT, "Results");
			}
			if (title != null && hasChildren()) {
				out.println(title);
			}			
			/*
			for(Entity o : sortByOffset(old)) {
				out.println("\tOld    : "+toString(o));
			}
			for(Entity o : sortByOffset(newer)) {
				out.println("\tNewer  : "+toString(o));
			}
			*/
			int ignored = 0;
			for(Entity o : sortByOffset(old)) {					
				final String msg = toString(o);
				if (ignoreProposedEffectsPromises && msg.contains("ProposedPromiseDrop (EMPTY)")) {
					ignored++;
				} else {
					out.println("\tOld    : "+msg);
				}
			}
			if (ignored > 0) {
				out.println("\tOld    : "+ignored+" ProposedPromiseDrop(s)");
			}			
			int numEffects = 0;
			if (ignoreProposedEffectsPromises && (newer.size() == ignored || old.size() == 0)) { 
				// Check to see that they're all ProposedPromiseDrops
				for(Entity o : newer) {
					final String msg = toString(o);
					if (msg.contains("ProposedPromiseDrop @RegionEffects(")) {
						numEffects++;
					} else {
						break;				
					}
				}
			} 
			if (ignoreProposedEffectsPromises && numEffects > 0 && numEffects == newer.size()) {
				out.println("\tNewer  : "+numEffects+" ProposedPromiseDrop @RegionEffects");
			} else {
				for(Entity o : sortByOffset(newer)) {
					out.println("\tNewer  : "+toString(o));
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
				for(Entity o : old) {
					if (m.match(n, o)) {
						if (!"Exact  ".equals(label) && !"Hashed ".equals(label)) {
							if (title != null) {
								out.println(title);
								title = null;
							}
							out.println("\t"+label+": "+toString(n));
						}
						old.remove(o);
						it.remove();
						break;
					}
				}
			}
			return title;
		}
		
		public static String toString(Entity e) {
			return e.getAttribute(OFFSET_ATTR)+" - "+
			       e.getAttribute(HASH_ATTR)+" - "+
			       e.getAttribute(CONTEXT_ATTR)+" - "+
			       e.getAttribute(PROVED_ATTR)+" - "+
			       e.getAttribute(MESSAGE_ID_ATTR)+" - "+
			       e.getAttribute(MESSAGE_ATTR);
		}

		public boolean hasChildren() {
			return !old.isEmpty() || !newer.isEmpty();
		}

		public Entity[] getChildren() {
			Entity[] a = new Entity[old.size() + newer.size()];
			int i=0;
			for(Entity o : old) {
				a[i] = o;
				o.setAsOld();
				i++;
			}
			for(Entity o : newer) {
				a[i] = o;
				o.setAsNewer();
				i++;
			}
			Arrays.sort(a, new Comparator<Entity>() {
				public int compare(Entity o1, Entity o2) {
					int rv = o1.getAttribute(MESSAGE_ATTR).compareTo(o2.getAttribute(MESSAGE_ATTR));
					if (rv == 0) {
						return o1.getDiffStatus().compareTo(o2.getDiffStatus());
					}
					return rv;
				}				
			});
			return a;
		}
	}
	
	static abstract class Matcher {
		static boolean match(Entity n, Entity o, String... attrs) {
			return match(n, o, false, attrs);
		}
		
		static boolean match(Entity n, Entity o, boolean defaultValue, String... attrs) {
			return match(false, n, o, defaultValue, attrs);
		}
		
		static boolean match(boolean nullMeansFalse, Entity n, Entity o, boolean defaultValue, String... attrs) {
			for(String attr : attrs) {
				String a_n = n.getAttribute(attr);
				String a_o = o.getAttribute(attr);
				if (a_n == null) {
					if (nullMeansFalse) {
						a_n = "false";
					} else {
						continue; // Skip this attribute
					}
				}
			    if (a_o == null) {				
					if (nullMeansFalse) {
						a_o = "false";
					} else {
						continue; // Skip this attribute
					}
				}		
				/*
				// Temporary
				a_n = a_n.replaceAll("  on  ", " on ");
				a_o = a_o.replaceAll("  on  ", " on ");
                */
			    /*
				if (PROVED_ATTR.equals(attr)) {
					System.out.println("Comparing "+a_n+" to "+a_o+" for "+attr);
				}
				*/
				return a_n.equals(a_o);
			}
			return defaultValue;
		}
		
		boolean match(Entity n, Entity o) {
			return match(n, o, true, CATEGORY_ATTR) && match(n, o, MESSAGE_ID_ATTR, MESSAGE_ATTR) &&
   			       match(true, n, o, true, PROVED_ATTR);
		}
	}
	
	static final Matcher EXACT = new Matcher() {
		@Override
		boolean match(Entity n, Entity o) {
			return super.match(n, o) && match(n, o, OFFSET_ATTR);
		}
	};
	
	static final Matcher HASHED = new Matcher() {
		@Override
		boolean match(Entity n, Entity o) {
			return super.match(n, o) && match(n, o, HASH_ATTR) &&
			       match(n, o, CONTEXT_ATTR);
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
