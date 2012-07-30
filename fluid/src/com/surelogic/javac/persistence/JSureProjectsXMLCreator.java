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
			b.start(PROJECTS);
			b.addAttribute("path", projs.getRun());
			if (projs.getLocation() != null) {
				b.addAttribute(LOCATION, projs.getLocation().getAbsolutePath());
			}
			b.addAttribute(IS_AUTO, projs.isAutoBuild());
			b.addAttribute(LAST_RUN, projs.getPreviousPartialScan());
			b.addAttribute(DATE, SLUtility.toStringHMS(projs.getDate()));
			for(JavacProject p : projs) {
				final Builder pb = b.nest(PROJECT);
				pb.addAttribute("id", i);
				pb.addAttribute(NAME, p.getName());
				if (p.getConfig().getLocation() != null) {
					pb.addAttribute(LOCATION, p.getConfig().getLocation().getAbsolutePath());
				}
				pb.addAttribute(IS_EXPORTED, p.getConfig().isExported());
				if (p.getConfig().containsJavaLangObject()) {
					pb.addAttribute(HAS_JLO, p.getConfig().containsJavaLangObject());
				}
				p.getConfig().outputOptionsToXML(pb);

				for(IClassPathEntry cpe : p.getConfig().getClassPath()) {
					cpe.outputToXML(pb);
				}
				for(JavaSourceFile f : p.getConfig().getFiles()) {
					f.outputToXML(pb);
				}
				for(String pkg : p.getConfig().getPackages()) {
					final Builder pkb = b.nest(PACKAGE);
					pkb.addAttribute(NAME, pkg);
					pkb.end();
				}
				for(Map.Entry<String,Object> option : p.getConfig().getOptions()) {
					final Builder ob = b.nest(OPTION);
					ob.addAttribute(NAME, option.getKey());
					ob.addAttribute(VALUE, option.getValue().toString());
					ob.end();
				}
				pb.end();
				i++;
			}
			b.end();
		} finally {
			flushBuffer();
		}
	}
}
