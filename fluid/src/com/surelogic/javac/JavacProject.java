package com.surelogic.javac;

import java.io.File;
import java.util.*;

import com.surelogic.analysis.IIRProject;
import com.surelogic.common.jobs.SLProgressMonitor;

import edu.cmu.cs.fluid.ide.IClassPathContext;

public class JavacProject implements IIRProject, IClassPathContext {
	private final Projects parent;
	private final Config config;
	private final String name;	
	private JavacTypeEnvironment tEnv;
	private Map<File,File> mappedJars = new HashMap<File, File>();
	boolean active = true;
	boolean first = true;
	final boolean containsJavaLangObject;
	
	JavacProject(String name, SLProgressMonitor monitor) {
		this(null, null, name, monitor);
	}

	JavacProject(Projects p, Config cfg, String name, SLProgressMonitor monitor) {
		parent = p;
		config = cfg;
		this.name = name;
		containsJavaLangObject = cfg == null ? false : cfg.containsJavaLangObject();
		tEnv = new JavacTypeEnvironment(p, this, monitor);		
		initTEnv();
	}
	
	public Projects getParent() {
	    return parent;
	}

	private void initTEnv() {
		for(String pkg : config.getPackages()) {
        	tEnv.addPackage(pkg);
        }
	}

	public JavacProject(Projects p, JavacProject oldProject, Config deltaConfig, SLProgressMonitor monitor) 
	throws MergeException {
		parent = p;
		name   = oldProject.name;
		config = oldProject.config.merge(deltaConfig);
		tEnv   = oldProject.tEnv;
		containsJavaLangObject = oldProject.containsJavaLangObject();
		tEnv.setProject(this);
	}

	boolean conflictsWith(JavacProject old) {
		try {
			old.config.checkForDiffs(this.config);
			return false;
		} catch(MergeException e) {
			return true;
		}
	}
	
	/*
	// Only used by copy()
	private JavacProject(Projects p, Config c, JavacTypeEnvironment tEnv) {
		parent = p;
		name = c.getProject(); 
		config = c.copy();
		this.tEnv = tEnv.copy(this);
	}
	
	JavacProject copy(Projects p) {
		final JavacProject copy = new JavacProject(p, config, tEnv);
		copy.active = this.active;
		copy.first = this.first;
		copy.mappedJars.putAll(this.mappedJars);
		return copy;
	}
	*/
	
	public Config getConfig() {
		return config;
	}
	
	@Override
	public String toString() {
		return "JavacProject "+hashCode()+": "+name;
	}
	
	public String getName() {
		return name;
	}
	
	void setTypeEnv(JavacTypeEnvironment te) {		
		tEnv = te;
		initTEnv();
	}
	
	public JavacTypeEnvironment getTypeEnv() {
		if (first) {
			//System.out.println("First time accessing type env for "+name);
			first = false;
		}
		return tEnv;
	}

	public boolean shouldExistAsIProject() {
		return !name.startsWith(JavacTypeEnvironment.JRE_NAME);
	}
	
	public boolean isAsBinary() {
		return !config.getBoolOption(Config.AS_SOURCE);
	}

	public void mapJar(File path, File orig) {
		//System.out.println("Mapping "+path+" to "+orig);
		mappedJars.put(path, orig);
	}

	public void collectMappedJars(Map<File, File> collected) {
		collected.putAll(mappedJars);
		/*
		for(Map.Entry<File, File> e : mappedJars.entrySet()) {
			Object replaced = collected.put(e.getKey(), e.getValue());
			if (replaced != null) {
				System.out.println("Replaced mapping for "+e.getKey());
			}
		}
		*/
	}

	public void init(JavacProject old) throws MergeException {
		old.config.checkForDiffs(config);
		
		System.out.println("Init type env for "+name);
		setTypeEnv(old.tEnv);
		old.deactivate();
	}
	
	public void deactivate() {
		active = false;
	}

	public boolean isActive() {
		return active;
	}

	public boolean containsJavaLangObject() {
		return containsJavaLangObject;
	}
}
