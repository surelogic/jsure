package com.surelogic.jsure.scripting;

import org.eclipse.core.resources.*;

import edu.cmu.cs.fluid.dc.Nature;

/**
 * Removes the JSure nature from the specified project(s)
 * 
 * @author Edwin
 */
public class RemoveNature extends AbstractProjectCommand {
	@Override
	protected boolean execute(ICommandContext context, IProject p) throws Exception {
		Nature.removeNatureFromProject(p);
		return true;
	}
}
