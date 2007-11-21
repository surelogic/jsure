package com.surelogic.test.scripting;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class TouchFile extends AbstractCommand {
  public boolean execute(ICommandContext context, String[] contents) throws CoreException {
    IFile f = resolveFile(contents[1], true);
    if (f.exists()) {
      f.touch(null);
    } else {
      f.create(new ByteArrayInputStream(noBytes), true, null);
    }  
    return true;
  }
}
