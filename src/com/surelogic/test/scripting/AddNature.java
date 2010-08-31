package com.surelogic.test.scripting;

import org.eclipse.core.resources.*;

import edu.cmu.cs.fluid.dc.Nature;

/**
 * Adds the JSure nature to the specified project(s)
 * 
 * @author Edwin
 */
public class AddNature extends AbstractProjectCommand {
	@Override
	protected boolean execute(ICommandContext context, IProject p) throws Exception {
		Nature.onlyAddNatureToProject(p);
		return true;
	}
}
