package com.surelogic.jsure.core.driver;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.Unused;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.PromiseMatcher;
import com.surelogic.javac.Util;
import com.surelogic.jsure.core.listeners.NotificationHub;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.util.*;

public class AnalysisDriver extends AbstractAnalysisModule<Unused> {
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
	@Override
	public void preBuild(IProject p) {
		project = p;
		JavacDriver.getInstance().preBuild(p);
	}
	
	/**
	 * @see IAnalysis#setArguments(Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
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
	@Override
	public boolean analyzeResource(IResource resource, int kind) {
		//System.out.println("Looking at "+resource);
		
		// TODO filter these first?
		resources.add(new Pair<IResource,Integer>(resource,IntegerTable.newInteger(kind)));
		return false; // call analyzeCompilationUnit()
	}

	/**
	 * @see IAnalysis#needsAST()
	 */
	@Override
	public boolean needsAST() {
		return false;
	}

	/**
	 * @see IAnalysis#analyzeCompilationUnit(ICompilationUnit, CompilationUnit)
	 */
	@Override
	public boolean analyzeCompilationUnit(ICompilationUnit file, CompilationUnit ast, 
			IAnalysisMonitor monitor) {
		//System.out.println("Looking at "+file);
		cus.add(file);
		return false;
	}
	
	/**
	 * @see IAnalysis#postBuild(IProject)
	 */
	@Override
	public void postBuild(IProject p) {
		System.out.println("Done with project "+p.getName());
		if (p != this.project) {
			throw new IllegalStateException("Project doesn't match");
		}
		System.out.println("AnalysisDriver: "+p.getName()+" with "+cus.size()+" CUs");
		JavacDriver.getInstance().registerBuild(project, args, resources, cus);
		project = null;
		resources.clear();
		cus.clear();
		
		if (IDE.useJavac) {						
			JavacEclipse.initialize();
			SLLogger.getLogger().fine("Configuring build");
    		if (Util.useResultsXML) {
				try {
					boolean ok = PromiseMatcher.findAndLoad(JSurePreferencesUtility.getJSureDataDirectory());
	    			if (ok) {
	    				Sea.getDefault().updateConsistencyProof();
	    				NotificationHub.notifyAnalysisCompleted();
	    				return;
	    			}
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
    		}    		
			JavacDriver.getInstance().configureBuild(args, false);
		}
		args = null;
	}
}
