package com.surelogic.jsure.core.scripting;

import org.eclipse.core.resources.*;

/**
 * Abstract class for a command that does something to the specified file(s)
 * 
 * @author Edwin
 */
public abstract class AbstractFileCommand extends AbstractCommand {
	@Override
	public final boolean execute(ICommandContext context, String... contents) throws Exception {
		boolean changed = false;
		for(int i=1; i<contents.length; i++) {
			final IFile f = resolveIFile(contents[i], true);
			if (f == null) {
				continue;
			}
			changed |= execute(context, f);
		}
		return changed;
	}
	
	protected abstract boolean execute(ICommandContext context, IFile f) throws Exception;
}
