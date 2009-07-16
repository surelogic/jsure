/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/sea/xml/SeaSnapshot.java,v 1.11 2008/06/23 17:27:49 chance Exp $*/
package edu.cmu.cs.fluid.sea.xml;

import java.io.*;
import java.util.*;

import com.surelogic.common.xml.Entities;
import static com.surelogic.jsure.xml.JSureXMLReader.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class SeaSnapshot {	
	public static final String SUFFIX = ".sea.xml";
	
	private final Map<Drop,String> idMap = new HashMap<Drop,String>();
	private final StringBuilder b = new StringBuilder();
	private boolean firstAttr = true;
	private PrintWriter pw = null;
	
	private String computeId(Drop d) {
		String id = idMap.get(d);
		if (id == null) {
			int size = idMap.size();
			id = Integer.toString(size);
			idMap.put(d, id);
		}
		return id;
	}
	
	private void reset() {
		b.setLength(0);
		firstAttr = true;
	}
	
	public void snapshot(String project, final Sea sea, File location) throws IOException {
    	pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(location), "UTF-8"));
		pw.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		
		reset();
		Entities.start(ROOT, b);
		Entities.addAttribute(UID_ATTR, UUID.randomUUID().toString(), b);
		Entities.addAttribute(PROJECT_ATTR, project, b);
		b.append(">\n");
		pw.println(b.toString());
		for(Drop d : sea.getDrops()) {
			snapshotDrop(d);
		}
		pw.println("</"+ROOT+">\n");
		pw.close();
		pw = null;
		//JSureXMLReader.readSnapshot(location, null);
	}
	
	public void snapshotDrop(Drop d) {
		if (idMap.containsKey(d)) {
			return;
		}
		final String id = computeId(d);
		d.preprocessRefs(this);
		reset();
		
		final String name = d.getEntityName();	
		final String type = d.getClass().getSimpleName();
		Entities.start(name, b);
		Entities.addAttribute(TYPE_ATTR, type, b);
		Entities.addAttribute(ID_ATTR, id, b);
		d.snapshotAttrs(this);
		b.append(">\n");
		d.snapshotRefs(this);
		b.append("</"+name+">\n");
		pw.println(b.toString());	
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
	
	public void refDrop(String name, Drop d) {
		refDrop(name, d, null, null);
	}
	
	public void refDrop(String name, Drop d, String attr, String value) {
		b.append("  ");
		Entities.start(name, b);
		Entities.addAttribute(ID_ATTR, computeId(d), b);
		if (attr != null) {
			Entities.addAttribute(attr, value, b);
		}
		b.append("/>\n");
	}

	public void addSrcRef(IRNode context, ISrcRef srcRef) {
		addSrcRef(context, srcRef, "    ");
	}

	private void addSrcRef(IRNode context, ISrcRef s, String indent) {
		if (s == null) {
			return;
		}
		b.append(indent);
		Entities.start(SOURCE_REF, b);
		Entities.addAttribute(LINE_ATTR, s.getLineNumber(), b);
		Object file = s.getEnclosingFile();
		if (file instanceof String) {
			addAttribute(FILE_ATTR, file.toString());
		} else {
			addAttribute(FILE_ATTR, file.toString());
		}		
		addAttribute(HASH_ATTR, s.getHash());
		addAttribute(CUNIT_ATTR, s.getCUName());
		addAttribute(PKG_ATTR, s.getPackage());
		b.append("/>\n");
	}
	
	public void addSupportingInfo(SupportingInformation si) {
		b.append("    ");
		Entities.start(SUPPORTING_INFO, b);
		addAttribute(Drop.MESSAGE, si.getMessage());
		b.append(">\n");
		addSrcRef(si.getLocation(), si.getSrcRef(), "      ");		
		b.append("</"+SUPPORTING_INFO+">\n");
	}
	/*
	private void outputPromiseDropAttrs(StringBuilder b, PromiseDrop d) {
		d.isAssumed();
		d.isCheckedByAnalysis();
		d.isFromSrc();
	}
	*/
}
