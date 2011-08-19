package com.surelogic.javac.persistence;

import java.io.File;
import java.text.ParseException;
import java.util.Collections;

import org.xml.sax.Attributes;

import com.surelogic.common.SLUtility;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.IXMLResultListener;
import com.surelogic.common.xml.NestedXMLReader;
import com.surelogic.javac.Config;
import com.surelogic.javac.JarEntry;
import com.surelogic.javac.JavaSourceFile;
import com.surelogic.javac.Javac;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.JavacTypeEnvironment;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JSureProjectsXMLReader extends NestedXMLReader implements
		IXMLResultListener, PersistenceConstants {
	private Projects projects;

	public JSureProjectsXMLReader() {
		// Nothing to do
	}

	@Override
	protected final String checkForRoot(String name, Attributes attributes) {
		if (PROJECTS.equals(name)) {
			if (attributes == null) {
				return "";
			}
			String l = attributes.getValue(LOCATION);
			File loc = l == null ? null : new File(l);
			String isAuto = attributes.getValue(IS_AUTO);
			String date = attributes.getValue(DATE);
			if (date == null) {
				// Get the latter half of the label
				String path = attributes.getValue(PATH);
				date = path.substring(path.indexOf(" 201")).trim();

				// Split so I can convert the dashes back to colons
				final String[] split = date.split(" ");
				if (split.length != 2) {
					throw new IllegalArgumentException();
				}
				date = split[0] + ' ' + (split[1].replace('-', ':'));
			}
			try {
				projects = new Projects(loc, "true".equals(isAuto),
						SLUtility.fromStringHMS(date),
						Collections.<String, Object> emptyMap());
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			String last = attributes.getValue(LAST_RUN);
			if (last != null) {
				projects.setPreviousPartialScan(last);
			}
			return attributes.getValue("path");
		}
		return null;
	}

	public final Entity makeEntity(String name, Attributes a) {
		return new Entity(name, a);
	}

	public final void start(String uid, String project) {
		// System.out.println("uid = " + uid);
	}

	public void notify(Entity e) {
		if (!PROJECT.equals(e.getName())) {
			throw new IllegalStateException("Unexpected top-level entity: "
					+ e.getName());
		}
		final String proj = e.getAttribute(NAME);
		final String location = e.getAttribute(LOCATION);
		final boolean isExported = "true".equals(e.getAttribute(IS_EXPORTED));
		if (proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
			// TODO what if this should be JavacEclipse?
			Javac.getDefault().setPreference(IDEPreferences.DEFAULT_JRE, proj);
		}

		final JavacProject p = projects.add(new Config(proj,
				location == null ? null : new File(location), isExported));
		final boolean isSource = "true"
				.equals(e.getAttribute(Config.AS_SOURCE));
		if (isSource) {
			p.getConfig().setAsSource();
		}

		for (Entity nested : e.getReferences()) {
			final String name = nested.getName();
			if (FILE.equals(name)) {
				String path = nested.getAttribute(PATH);
				String file = nested.getAttribute(LOCATION);
				String qname = nested.getAttribute(QNAME);
				String asBinary = nested.getAttribute(AS_BINARY);
				// System.out.println(proj + " has source: " + path);
				p.getConfig().addFile(
						new JavaSourceFile(qname, new File(file), path, "true".equals(asBinary)));
			} else if (JAR.equals(name)) {
				String path = nested.getAttribute(PATH);
				String orig = nested.getAttribute(ORIG_PATH);
				final boolean jarIsExported = "true".equals(nested
						.getAttribute(IS_EXPORTED));
				// System.out.println(proj + " has jar: " + path);
				p.getConfig().addToClassPath(
						new JarEntry(p.getConfig(), new File(path), new File(
								orig), jarIsExported));
			} else if (PROJECT.equals(name)) {
				String pRefName = nested.getAttribute(NAME);
				// System.out.println(proj + " has ref to project " + pRefName);
				final JavacProject pRef = projects.get(pRefName);
				p.getConfig().addToClassPath(pRef.getConfig());
			} else if (PACKAGE.equals(name)) {
				String pkg = nested.getAttribute(NAME);
				p.getConfig().addPackage(pkg);
			} else if (OPTION.equals(name)) {
				String key = nested.getAttribute(NAME);
				String val = nested.getAttribute(VALUE);
				if ("false".equals(val) || "true".equals(val)) {
					p.getConfig().setOption(key, Boolean.parseBoolean(val));
				} else {
					p.getConfig().setOption(key, Integer.parseInt(val));
				}
			} else
				throw new IllegalStateException("Unexpected entity: " + name);
		}
	}

	public final void done() {
		// Nothing to do here?
	}

	public Projects getProjects() {
		return projects;
	}
}
