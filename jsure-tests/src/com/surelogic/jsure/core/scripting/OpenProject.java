package com.surelogic.jsure.core.scripting;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class OpenProject extends AbstractProjectCommand {
	@Override
	protected boolean execute(ICommandContext context, IProject p) throws CoreException {
		if (p.exists() && !p.isOpen()) {
			p.open(null);
			return true;
		}
		return false;
	}  
}
