package com.surelogic.javac;

import com.surelogic.analysis.IIRProject;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.SLProgressMonitor;

import edu.cmu.cs.fluid.ide.IClassPathContext;
import edu.cmu.cs.fluid.ide.IDE;

public class JavacProject extends JavaProject implements IIRProject, IClassPathContext {
	private JavacTypeEnvironment tEnv;
	boolean first = true;
	
	JavacProject(String name, SLProgressMonitor monitor) {
		this(null, null, name, monitor);
	}

	JavacProject(Projects p, Config cfg, String name, SLProgressMonitor monitor) {
		super(p, cfg, name, monitor);
		tEnv = new JavacTypeEnvironment(p, this, monitor);		
		initTEnv();
	}

	public Projects getIRParent() {
		return (Projects) getParent();
	}
	
	private void initTEnv() {
		for(String pkg : config.getPackages()) {
        	tEnv.addPackage(pkg, isAsBinary() ? Config.Type.INTERFACE : Config.Type.SOURCE);
        }
	}

	public JavacProject(Projects p, JavacProject oldProject, Config deltaConfig, SLProgressMonitor monitor) 
	throws MergeException {
		super(p, oldProject.config.merge(deltaConfig), oldProject.name, monitor);
		tEnv   = oldProject.tEnv;
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
	
	@Override
	public String toString() {
		return "JavacProject "+hashCode()+": "+name;
	}
	
	void setTypeEnv(JavacTypeEnvironment te) {		
		tEnv = te;
		initTEnv();
	}
	
	@Override
  public JavacTypeEnvironment getTypeEnv() {
		if (first) {
			//System.out.println("First time accessing type env for "+name);
			first = false;
		}
		return tEnv;
	}

	public void init(JavacProject old) throws MergeException {
		old.config.checkForDiffs(config);
		
		System.out.println("Init type env for "+name);
		setTypeEnv(old.tEnv);
		old.deactivate();
	}
	
	@Override
	public void addPackage(String pkgName, Config.Type t) {
		tEnv.addPackage(pkgName, t);
	}
	
	@Override
	public void clear() {
	    IDE.getInstance().removeCompUnitListener(tEnv.getBinder());
		super.clear();
	}
}
