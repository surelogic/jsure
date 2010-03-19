package com.surelogic.jsure.client.eclipse.analysis;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.analysis.IAnalysisMonitor;

import edu.cmu.cs.fluid.dc.AbstractAnalysisModule;
import edu.cmu.cs.fluid.dc.IAnalysis;
import edu.cmu.cs.fluid.util.*;

public class AnalysisDriver extends AbstractAnalysisModule<Void> {
	public static final boolean useJavac = false;
	public static final String ID = "com.surelogic.jsure.client.eclipse.AnalysisDriver";
	
	private IProject project;
	@SuppressWarnings("unchecked")
	private Map args;
	private final List<Pair<IResource,Integer>> resources = 
		new ArrayList<Pair<IResource,Integer>>();
	private final List<ICompilationUnit> cus = new ArrayList<ICompilationUnit>();
	
	/**
	 * @see IAnalysis#preBuild(IProject)
	 */
	public void preBuild(IProject p) {
		project = p;
	}
	
	/**
	 * @see IAnalysis#setArguments(Map)
	 */
	@SuppressWarnings("unchecked")
	public void setArguments(Map args) {
		// Grab build arguments
		/*
		for(Object o : args.entrySet()) {
			Map.Entry e = (Map.Entry) o;
			System.out.println(e.getKey()+" = "+e.getValue());
		}
		*/
		this.args = new HashMap(args);
	}
	
	/**
	 * @see IAnalysis#analyzeResource(IResource, int)
	 */
	public boolean analyzeResource(IResource resource, int kind) {
		//System.out.println("Looking at "+resource);
		
		// TODO filter these first?
		resources.add(new Pair<IResource,Integer>(resource,IntegerTable.newInteger(kind)));
		return false; // call analyzeCompilationUnit()
	}

	/**
	 * @see IAnalysis#needsAST()
	 */
	public boolean needsAST() {
		return false;
	}

	/**
	 * @see IAnalysis#analyzeCompilationUnit(ICompilationUnit, CompilationUnit)
	 */
	public boolean analyzeCompilationUnit(ICompilationUnit file, CompilationUnit ast, 
			IAnalysisMonitor monitor) {
		//System.out.println("Looking at "+file);
		cus.add(file);
		return false;
	}
	
	/**
	 * @see IAnalysis#postBuild(IProject)
	 */
	public void postBuild(IProject p) {
		if (p != this.project) {
			throw new IllegalStateException("Project doesn't match");
		}
		JavacDriver.getInstance().registerBuild(project, args, resources, cus);
		project = null;
		args = null;
		resources.clear();
		cus.clear();
		
		if (useJavac) {
			JavacDriver.getInstance().doBuild(p);
		}
	}
}
