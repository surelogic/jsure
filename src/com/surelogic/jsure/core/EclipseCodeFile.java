package com.surelogic.jsure.core;

import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.java.AbstractCodeFile;
import edu.cmu.cs.fluid.java.ICodeFile;

@Deprecated
public class EclipseCodeFile extends AbstractCodeFile {
	final ICompilationUnit cu;
	final String id;
	IResource res = null;

	public EclipseCodeFile(ICompilationUnit cu) {
		this.cu = cu;
		this.id = cu != null ? cu.getHandleIdentifier() : null;

		try {
			res = cu.getCorrespondingResource();
		} catch (JavaModelException e) {
			if (e.getMessage().contains("does not exist")) {
				// TODO Ignore for now
			} else {
				SLLogger.getLogger().log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	public EclipseCodeFile(IResource r) {
		this.cu = null;
		this.id = null;
		res = r;
	}

	@Override
	public int hashCode() {
		if (res == null) {
			if (id != null) {
				return id.hashCode();
			}
			return 0;
		}
		return res.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof EclipseCodeFile) {
			EclipseCodeFile o2 = (EclipseCodeFile) o;
			/*
			 * if (id.endsWith("Object.java") && o2.id.endsWith("Object.java"))
			 * { System.out.println("Checking id "+id); }
			 */
			if (id == null || o2.id == null) {
				if (res == null) {
					return (o2.res == null);
				}
				return res.equals(o2.res);
			}
			return id.equals(o2.id);
		} else if (id != null && o instanceof ICodeFile) {
			ICodeFile o2 = (ICodeFile) o;
			return id.equals(o2.getHostEnvResource());
		}
		return false;
	}

	@Override
	public String getPackage() {
		try {
			IPackageDeclaration[] pkgs = cu.getPackageDeclarations();
			if (pkgs.length == 0) {
				return "(default)";
			}
			// System.out.println("Looking at package: "+pkgs[0].getElementName());
			return pkgs[0].getElementName();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public Object getHostEnvResource() {
		return cu;
	}

	@Override
	public String getRelativePath() {
		throw new UnsupportedOperationException();
	}

	public IProject getProject() {
		try {
			return cu.getCorrespondingResource().getProject();
		} catch (JavaModelException e) {
			throw new FluidError("Couldn't find project for "
					+ cu.getElementName(), e);
		}
	}

	@Override
	public String toString() {
		return res.getName() + '@' + hashCode();
	}
}
