package com.surelogic.jsure.core.scripting;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Close the specified project(s) if open
 * 
 * @author Edwin
 */
public class CloseProject extends AbstractProjectCommand {
	@Override
	protected boolean execute(ICommandContext context, IProject p) throws CoreException {
		if (p.exists() && p.isOpen()) {
			p.close(null);
			return true;
		}
		return false;
	}
}
