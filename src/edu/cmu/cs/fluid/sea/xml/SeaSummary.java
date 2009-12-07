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

	private static IRReferenceDrop checkIfReady(Drop d) {
		if (d instanceof PromiseDrop) {
			@SuppressWarnings("unchecked")
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
		addAttribute(HASH_ATTR, computeHash(id.getNode()));
		// Omitting supporting info

		id.snapshotAttrs(this);
	}

	private long computeHash(IRNode node) {			
		IRNode parent  = JJNode.tree.getParentOrNull(node);
		String unparse = DebugUnparser.toString(node);
		if (parent != null) {
			String unparse2 = DebugUnparser.toString(parent);
			return unparse.hashCode() + (long) unparse2.hashCode();
		}	
		return unparse.hashCode();
	}
	
	public static void diff(String project, final Sea sea, File location)
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
		
		diff(oldDrops, newDrops);
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
	
	private static void diff(List<Entity> oldDrops, List<Entity> newDrops) {
		// Separate into categories
		Categories categories = new Categories();
		for(Entity e : oldDrops) {
			Category c = categories.getOrCreate(e);
			c.addOld(e);
		}
		for(Entity e : newDrops) {
			Category c = categories.getOrCreate(e);
			c.addNew(e);
		}
		for(Category c : categories.elements()) {
			System.out.println("Category: "+c.name+" in "+c.file);
			c.match(System.out);
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
	static class Category {
		final String file;
		final String name;		
		final Set<Entity> old = new HashSet<Entity>();
		final Set<Entity> newer = new HashSet<Entity>();
		
		public Category(String file, String name) {
			this.file = file;
			this.name = name;
		}

		public void addOld(Entity e) {
			old.add(e);
		}

		public void addNew(Entity e) {
			newer.add(e);
		}
		
		public void match(PrintStream out) {
			match(out, EXACT,  "Exact  ");
			match(out, HASHED, "Hashed ");
			if ("ResultDrop".equals(name)) {
				match(out, RESULT, "Results");
			}
			
			for(Entity o : old) {
				out.println("\tOld    : "+toString(o));
			}
			for(Entity o : newer) {
				out.println("\tNewer  : "+toString(o));
			}
		}

		private void match(PrintStream out, Matcher m, String label) {
			Iterator<Entity> it = newer.iterator();
			while (it.hasNext()) {
				Entity n = it.next();
				for(Entity o : old) {
					if (m.match(n, o)) {
						out.println("\t"+label+": "+toString(n));
						old.remove(o);
						it.remove();
						break;
					}
				}
			}
		}
		
		static String toString(Entity e) {
			return e.getAttribute(OFFSET_ATTR)+" - "+e.getAttribute(MESSAGE_ATTR);
		}
	}
	
	static abstract class Matcher {
		static boolean match(Entity n, Entity o, String attr) {
			String a_n = n.getAttribute(attr);
			String a_o = o.getAttribute(attr);
			return a_n.equals(a_o);
		}
		
		boolean match(Entity n, Entity o) {
			return match(n, o, MESSAGE_ATTR);
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
			return super.match(n, o) && match(n, o, HASH_ATTR);
		}
	};
	
	static final Matcher RESULT = new Matcher() {
		@Override
		boolean match(Entity n, Entity o) {
			return match(n, o, HASH_ATTR) && false;
		}
	};
}
