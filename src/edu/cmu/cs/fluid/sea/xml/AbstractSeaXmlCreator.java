/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.jsure.xml.AbstractXMLReader.FILE_ATTR;
import static com.surelogic.jsure.xml.AbstractXMLReader.LINE_ATTR;

import java.io.*;

import com.surelogic.common.xml.Entities;

import edu.cmu.cs.fluid.java.ISrcRef;

public class AbstractSeaXmlCreator {
	protected final StringBuilder b = new StringBuilder();
	protected boolean firstAttr = true;
	protected final PrintWriter pw;
	
	AbstractSeaXmlCreator(File location) throws IOException {
    	pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(location), "UTF-8"));
		pw.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
	}
	
	protected final void reset() {
		b.setLength(0);
		firstAttr = true;
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
	}
	
	protected void addLocation(ISrcRef ref) {
		Entities.addAttribute(LINE_ATTR, ref.getLineNumber(), b);
		Object file = ref.getEnclosingFile();
		if (file instanceof String) {
			addAttribute(FILE_ATTR, file.toString());
		} else {
			addAttribute(FILE_ATTR, file.toString());
		}
	}
}
