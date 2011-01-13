package com.surelogic.jsure.client.eclipse.analysis;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.eclipse.*;

import edu.cmu.cs.fluid.dc.Majordomo;
import edu.cmu.cs.fluid.util.Pair;

public class JavacBuild {
	private static List<Pair<IResource, Integer>> pairUpResources(List<IResource> resources, Integer kind) {
		if (resources.isEmpty()) {
			return Collections.emptyList();
		}
		List<Pair<IResource, Integer>> result = new ArrayList<Pair<IResource,Integer>>(resources.size());
		for(IResource r : resources) {
			result.add(new Pair<IResource, Integer>(r, kind));
		}
		return result;
	}
	
	private static final Map<?,?>  buildArgs = 
		Collections.singletonMap(Majordomo.BUILD_KIND, Integer.toString(IncrementalProjectBuilder.FULL_BUILD));
	
	public static void analyze(List<IJavaProject> selectedProjects) {
		try {
			for(IJavaProject p : selectedProjects) {			
				boolean noErrors = JDTUtility.noCompilationErrors(p, new NullProgressMonitor());		
				if (noErrors) {
					// Collect resources and CUs for build
					JavaProjectResources jpr = JDTUtility.collectAllResources(p);
					JavacDriver.getInstance().registerBuild(p.getProject(), buildArgs,							
							pairUpResources(jpr.resources, IResourceDelta.ADDED), jpr.cus);
				} else {
					// TODO what error to print?
					BalloonUtility.showMessage("Compile Errors in "+p.getElementName(), 
							"JSure is unable to analyze "+p.getElementName()+
							" due to some compilation errors.  Please fix (or do a clean build).");
					return;
				}
			}
			JavacEclipse.initialize();
			System.out.println("Configuring build");	
			JavacDriver.getInstance().configureBuild(buildArgs, true);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}

