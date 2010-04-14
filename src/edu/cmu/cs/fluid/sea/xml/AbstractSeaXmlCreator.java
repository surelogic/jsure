/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.jsure.xml.AbstractXMLReader.FILE_ATTR;
import static com.surelogic.jsure.xml.AbstractXMLReader.LINE_ATTR;

import java.io.*;
import java.net.URI;
import java.util.*;

import com.surelogic.common.xml.Entities;

import edu.cmu.cs.fluid.java.ISrcRef;

public class AbstractSeaXmlCreator {
	protected final Map<String,String> attributes = new HashMap<String,String>();
	protected final StringBuilder b = new StringBuilder();
	protected boolean firstAttr = true;
	protected final PrintWriter pw;
	
	AbstractSeaXmlCreator(File location) throws IOException {
		if (location != null) {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(location), "UTF-8"));
			pw.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		} else {
			pw = null;
		}
	}
		
	protected void flushBuffer(PrintWriter pw) {
		if (pw != null) {
			pw.println(b.toString());
			reset();
		}
	}
	
	protected final void reset() {
		b.setLength(0);
		firstAttr = true;
		attributes.clear();
	}
	 
	public void addAttribute(String name, boolean value) {
		if (value) {
			addAttribute(name, Boolean.toString(value));
		}
	}
	
	public void addAttribute(String name, Long value) {		
		if (value == null) {
			return;
		}
		addAttribute(name, value.toString());		
	}
	
	public void addAttribute(String name, String value) {
		if (firstAttr) {
			firstAttr = false;
		} else {
			b.append("\n\t");
		}
		Entities.addAttribute(name, value, b);
		attributes.put(name, value);
	}
	
	protected void addLocation(ISrcRef ref) {
		addAttribute(LINE_ATTR, (long) ref.getLineNumber());
		String file = ref.getRelativePath();
		if (file == null) {
			URI loc = ref.getEnclosingURI();
			if (loc != null) {
				file = loc.toString();
			}
		}
		if (file == null) {
			Object o = ref.getEnclosingFile();
			if (o instanceof String) {
				file = o.toString();
			} else {
				file = o.toString();
			}
		}
		if (file != null) {
			addAttribute(FILE_ATTR, file);
		}
	}
}
