package com.surelogic.jsure.core.scripting;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class DeleteProject extends AbstractProjectCommand {
	@Override
	protected boolean execute(ICommandContext context, IProject p) throws CoreException {
		if (p.exists()) {
			p.delete(false, true, null);			
			return true;
		}
		return false;
	}  
}
