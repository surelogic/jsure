package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;

import com.surelogic.common.SLUtility;
import com.surelogic.common.xml.Entities;
import com.surelogic.javac.*;

import edu.cmu.cs.fluid.sea.xml.AbstractSeaXmlCreator;

public class JSureProjectsXMLCreator extends AbstractSeaXmlCreator implements PersistenceConstants {
	public JSureProjectsXMLCreator(OutputStream out) throws IOException {
		super(out);
	}

	public void write(Projects projs) {
		try {
			final int indent = 1;
			int i=0;
			Entities.start(PROJECTS, b, 0);
			Entities.addAttribute("path", projs.getRun(), b);
			if (projs.getLocation() != null) {
				Entities.addAttribute(LOCATION, projs.getLocation().getAbsolutePath(), b);
			}
			Entities.addAttribute(IS_AUTO, projs.isAutoBuild(), b);
			Entities.addAttribute(LAST_RUN, projs.getPreviousPartialScan(), b);
			Entities.addAttribute(DATE, SLUtility.toStringHMS(projs.getDate()), b);
			Entities.closeStart(b, false);
			for(JavacProject p : projs) {
				Entities.start(PROJECT, b, indent);
				Entities.addAttribute("id", i, b);
				Entities.addAttribute(NAME, p.getName(), b);
				if (p.getConfig().getLocation() != null) {
					Entities.addAttribute(LOCATION, p.getConfig().getLocation().getAbsolutePath(), b);
				}
				Entities.addAttribute(IS_EXPORTED, p.getConfig().isExported(), b);
				if (p.getConfig().containsJavaLangObject()) {
					Entities.addAttribute(HAS_JLO, p.getConfig().containsJavaLangObject(), b);
				}
				p.getConfig().outputOptionsToXML(this, indent, b);
				Entities.closeStart(b, false);
				flush();
				for(IClassPathEntry cpe : p.getConfig().getClassPath()) {
					cpe.outputToXML(this, indent+1, b);
					flush();
				}
				for(JavaSourceFile f : p.getConfig().getFiles()) {
					f.outputToXML(this, indent+1, b);
					flush();
				}
				for(String pkg : p.getConfig().getPackages()) {
					Entities.start(PACKAGE, b, indent+1);
					Entities.addAttribute(NAME, pkg, b);
					Entities.closeStart(b, true);					
					flush();
				}
				for(Map.Entry<String,Object> option : p.getConfig().getOptions()) {
					Entities.start(OPTION, b, indent+1);
					Entities.addAttribute(NAME, option.getKey(), b);
					Entities.addAttribute(VALUE, option.getValue().toString(), b);
					Entities.closeStart(b, true);	
					flush();
				}
				Entities.end(PROJECT, b, indent);
				flush();
				i++;
			}
			Entities.end(PROJECTS, b, 0);
			flush();
		} finally {
			pw.flush();
		}
	}
	
	private void flush() {
		flushBuffer(pw);
		/*
		System.out.print(b.toString());
		reset();
		*/
	}
}
