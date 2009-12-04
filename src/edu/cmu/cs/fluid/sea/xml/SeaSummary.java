package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.jsure.xml.JSureSummaryXMLReader.*;

import java.io.*;
import java.util.Date;

import com.surelogic.common.xml.Entities;

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
		Entities.addAttribute(TIME_ATTR, now.toString(), b);
		Entities.addAttribute(PROJECT_ATTR, project, b);
		b.append(">\n");
		pw.println(b.toString());
		for(Drop d : sea.getDrops()) {
			summarizeDrop(d);
		}
		pw.println("</"+ROOT+">\n");
		pw.close();
	}

	private void summarizeDrop(Drop d) {
		if (d instanceof PromiseDrop) {
			@SuppressWarnings("unchecked")
			PromiseDrop pd = (PromiseDrop) d;
			if (!pd.isFromSrc()) {
				// no need to do anything
				return;
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
				return;				
			}			
			reset();
			
			final String name = d.getEntityName();	
			final String type = d.getClass().getSimpleName();
			Entities.start(name, b);
			addAttribute(TYPE_ATTR, type);
			addAttribute(MESSAGE_ATTR, d.getMessage());
			addLocation(ref);
			addAttribute(OFFSET_ATTR, (long) ref.getOffset());
			addAttribute(HASH_ATTR, computeHash(id.getNode()));
			// Omitting supporting info
			
			d.snapshotAttrs(this);
			b.append("/>\n");
			//b.append("</"+name+">\n");
			pw.println(b.toString());
		} 
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
}
