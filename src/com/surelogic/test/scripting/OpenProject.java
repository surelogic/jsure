package com.surelogic.test.scripting;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class OpenProject extends AbstractCommand {
  public boolean execute(ICommandContext context, String[] contents) throws CoreException {
    IProject p = resolveProject(contents[1]);
    if (p == null) {
      return false;
    }
    if (p.exists() && !p.isOpen()) {
      p.open(null);
      return true;
    }
    return false;
  }
}
