package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.jsure.xml.JSureSummaryXMLReader.*;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.Entities;
import com.surelogic.jsure.xml.Entity;
import com.surelogic.jsure.xml.IXMLResultListener;
import com.surelogic.jsure.xml.JSureSummaryXMLReader;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaUnparseStyle;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.util.Hashtable2;

public class SeaSummary extends AbstractSeaXmlCreator {
	private SeaSummary(File location) throws IOException {
		super(location);
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
		
		for(Drop d : sea.getDrops()) {
			IRReferenceDrop id = checkIfReady(d);
			if (id != null) {
				summarizeDrop(id);
			}
		}
		pw.println("</"+ROOT+">\n");
		pw.close();
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
		if (d instanceof IRReferenceDrop) {
			// Need a location to report
			IRReferenceDrop id = (IRReferenceDrop) d;
			ISrcRef ref = id.getSrcRef();
			if (ref == null) {
				if (!d.getMessage().contains("java.lang.Object")) {
					System.out.println("No src ref for "+d.getMessage());
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
		addAttribute(OFFSET_ATTR, (long) ref.getOffset());
		addAttribute(HASH_ATTR, computeHash(id.getNode(), false));
		addAttribute(CONTEXT_ATTR, computeContext(id.getNode(), false));
		// Omitting supporting info
		/*
		if (id.getMessage().contains("copyStat")) {
			computeHash(id.getNode(), true);
		}
        */		
		id.snapshotAttrs(this);
	}

	private static final JavaUnparseStyle noPromisesStyle = new JavaUnparseStyle(false);
	private static final DebugUnparser unparser = new DebugUnparser(5, JJNode.tree) {
		@Override
		public JavaUnparseStyle getStyle() {
			return noPromisesStyle;
		}
	};
	
	private long computeHash(IRNode node, boolean debug) {			
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
	
	public static Diff diff(String project, final Sea sea, File location)
	throws Exception {
		// Load up current contents
		final Listener l = new Listener();
		new JSureSummaryXMLReader(l).read(location);
		
		final List<Entity> oldDrops = l.drops;
		//Collections.sort(oldDrops, EntityComparator.prototype);
	
		final SeaSummary s = new SeaSummary(null);
		final List<Entity> newDrops = new ArrayList<Entity>();
		for(Drop d : sea.getDrops()) {
			IRReferenceDrop id = checkIfReady(d);
			if (id != null) {
				s.reset();
				s.summarizeDrop(id);
				
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
		final List<Entity> drops = new ArrayList<Entity>();
		String project;
		Date time;
		
		public void start(String time, String project) {
			try {
				this.time = DateFormat.getDateTimeInstance().parse(time);
				this.project = project;
			} catch (ParseException e) {
				SLLogger.getLogger().log(Level.SEVERE, "Could not parse "+time);
			}
		}

		public void notify(Entity e) {
			drops.add(e);
		}
		
		public void done() {
			// TODO Auto-generated method stub
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
	}
	
	static class Categories extends Hashtable2<String,String,Category> {
		Category getOrCreate(Entity e) {
			final String file = e.getAttribute(FILE_ATTR);
			final String type = e.getAttribute(TYPE_ATTR);
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

		public boolean isEmpty() {
			return old.isEmpty() && newer.isEmpty();
		}

		public void addOld(Entity e) {
			old.add(e);
		}

		public void addNew(Entity e) {
			newer.add(e);
		}
		
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
			for(Entity o : old) {
				out.println("\tOld    : "+toString(o));
			}
			for(Entity o : newer) {
				out.println("\tNewer  : "+toString(o));
			}
		}
		
		private String match(String title, PrintStream out, Matcher m, String label) {
			Iterator<Entity> it = newer.iterator();
			while (it.hasNext()) {
				Entity n = it.next();
				for(Entity o : old) {
					if (m.match(n, o)) {
						if (!"Exact  ".equals(label)) {
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
			       e.getAttribute(HASH_ATTR)+" - "+e.getAttribute(MESSAGE_ATTR);
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
		static boolean match(Entity n, Entity o, String attr) {
			String a_n = n.getAttribute(attr);
			String a_o = o.getAttribute(attr);
			if (a_n == null) {
				return a_o == null;
			}
			return a_n.equals(a_o);
		}
		
		boolean match(Entity n, Entity o) {
			return match(n, o, CATEGORY_ATTR) && match(n, o, MESSAGE_ATTR);
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
