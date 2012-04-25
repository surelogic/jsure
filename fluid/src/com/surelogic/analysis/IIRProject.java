/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/IIRProject.java,v 1.2 2008/08/13 18:52:51 chance Exp $*/
package com.surelogic.analysis;

import edu.cmu.cs.fluid.ide.IClassPath;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

public interface IIRProject extends IClassPath {
	String getName();
	ITypeEnvironment getTypeEnv();
	IIRProjects getParent();
}
