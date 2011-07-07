/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.*;
import static com.surelogic.common.jsure.xml.JSureXMLReader.SOURCE_REF;

import java.io.*;
import java.net.URI;
import java.util.*;

import com.surelogic.common.xml.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * Really a JSure-specific XML creator
 */
public class AbstractSeaXmlCreator extends XMLCreator {	
	final Map<IRNode,Long> hashes = new HashMap<IRNode, Long>();
	
	Long getHash(IRNode n) {
		Long hash = hashes.get(n);
		if (hash == null) {
			hash = SeaSummary.computeHash(n);
			hashes.put(n, hash);
		}
		return hash;
	}
	
	protected AbstractSeaXmlCreator(OutputStream out) throws IOException {
		super(out);
	}
	
	AbstractSeaXmlCreator(File location) throws IOException {
		super(location != null ? new FileOutputStream(location) : null);
	}
	
	public void addSrcRef(IRNode context, ISrcRef s, String indent, String flavor) {
		if (s == null) {
			return;
		}
		attributes.clear();
		
		b.append(indent);
		Entities.start(SOURCE_REF, b);
		addLocation(s);		
		if (flavor != null) {
			addAttribute(FLAVOR_ATTR, flavor);
		}
		addAttribute(HASH_ATTR, getHash(context));
		addAttribute(CUNIT_ATTR, s.getCUName());
		addAttribute(PKG_ATTR, s.getPackage());
		addAttribute(PROJECT_ATTR, s.getProject());
		b.append("/>\n");
	}
	
	protected void addLocation(ISrcRef ref) {
		if (ref.getOffset() > 0) {
			addAttribute(OFFSET_ATTR, (long) ref.getOffset());
		}
		addAttribute(LINE_ATTR, (long) ref.getLineNumber());
		String path = ref.getRelativePath();
		if (path != null) {
			addAttribute(PATH_ATTR, path);
		}
		URI loc = ref.getEnclosingURI();
		if (loc != null) {
			addAttribute(URI_ATTR, loc.toString());
		}
		Object o = ref.getEnclosingFile();
		if (o != null) {
			addAttribute(FILE_ATTR, o.toString());		
		}
	}
}
