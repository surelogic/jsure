package com.surelogic.javac;

import java.io.File;

import com.surelogic.analysis.IIRProject;
import com.surelogic.common.jobs.NullSLProgressMonitor;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;

/**
 * @author Edwin.Chan
 * @lock L is this protects Instance
 */
public class JavacFileLocator extends AbstractJavaFileLocator<String,IIRProject> {
	private JavacFileLocator() {
		cleanupTempFiles();
	}

	@Override
	protected JavaCanonicalizer getCanonicalizer(IIRProject proj) {
		ITypeEnvironment te = IDE.getInstance().getTypeEnv(proj);
		return new JavaCanonicalizer(te.getBinder());
	}

	@Override
	protected ITypeEnvironment getTypeEnvironment(IIRProject proj) {
		return IDE.getInstance().getTypeEnv(proj);
	}
	
	static JavacFileLocator makeLocator() {
		JavacFileLocator l = new JavacFileLocator();
		finishInit(l);
		return l;
	}

	@Override
	public long mapTimeStamp(long time) {
		//return time == IResource.NULL_STAMP ? IJavaFileStatus.NO_TIME : time;
		return time;
	}

	@Override
	protected File getDataDirectory() {
		return new File(".");
	}

	@Override
	protected String getIdHandle(String id) {
		return id;
	}

	@Override
	protected String getProjectHandle(IIRProject proj) {
		return proj.getName();
	}

	@Override
	protected String getIdFromHandle(String handle) {
		return handle;
	}

	@Override
	protected IIRProject getProjectFromHandle(String handle) {
		return new JavacProject(handle, new NullSLProgressMonitor());
	}
}
