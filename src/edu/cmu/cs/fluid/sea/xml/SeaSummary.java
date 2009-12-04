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
		addAttribute(TIME_ATTR, now.toString());
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

	private IRReferenceDrop checkIfReady(Drop d) {
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
		addAttribute(MESSAGE_ATTR, id.getMessage());
		
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
		Listener l = new Listener();
		new JSureSummaryXMLReader(l).read(location);
		/*
		SeaSummary s = new SeaSummary(location);
		s.summarize(project, sea);
		*/
	}
	
	static class Listener implements IXMLResultListener {
		final List<Entity> drops = new ArrayList<Entity>();
		String project;
		Date time;
		
		public void start(String time, String project) {
			try {
				this.time = DateFormat.getDateInstance().parse(time);
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
}
