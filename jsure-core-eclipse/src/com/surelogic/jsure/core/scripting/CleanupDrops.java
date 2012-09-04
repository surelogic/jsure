package com.surelogic.jsure.core.scripting;

public class CleanupDrops extends AbstractCommand {
	@Override
	public boolean execute(ICommandContext context, String... contents)	throws Exception {		
		/*
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
		*/
		System.out.println("Ignoring cleanup");
		return false;
	}

}
