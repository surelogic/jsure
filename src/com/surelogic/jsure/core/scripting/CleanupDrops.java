package com.surelogic.jsure.core.scripting;

import java.util.*;

import org.eclipse.core.resources.IProject;

import com.surelogic.jsure.core.listeners.ClearProjectListener;

public class CleanupDrops extends AbstractCommand {
	public boolean execute(ICommandContext context, String... contents)	throws Exception {		
		List<IProject> removedProjects;
		if (contents.length <= 1) {
			removedProjects = null;
		} else {
			removedProjects = new ArrayList<IProject>(contents.length);
			for(int i=1; i<contents.length; i++) {
				final IProject p = resolveProject(contents[i], true);
				if (p == null) {
					continue;
				}
				removedProjects.add(p);
			}
		}
		ClearProjectListener.clearJSureState(removedProjects);		
		return false;
	}

}
