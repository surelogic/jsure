package com.surelogic.jsure.core.scripting;

import org.eclipse.core.resources.*;

/**
 * Abstract class for a command that does something to the specified project(s)
 * 
 * @author Edwin
 */
public abstract class AbstractProjectCommand extends AbstractCommand {
	@Override
	public final boolean execute(ICommandContext context, String... contents) throws Exception {
		boolean changed = false;
		for(int i=1; i<contents.length; i++) {
			final IProject p = resolveProject(contents[i], true);
			if (p == null) {
				continue;
			}
			changed |= execute(context, p);
		}
		return changed;
	}
	
	protected abstract boolean execute(ICommandContext context, IProject p) throws Exception;
}
