package com.surelogic.jsure.client.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.surelogic.common.core.EclipseUtility;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class UniquenessPreferenceHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final Command command = event.getCommand();
    boolean oldValue = HandlerUtil.toggleCommandState(command);
    EclipseUtility.setBooleanPreference(IDEPreferences.SCAN_MAY_RUN_UNIQUENESS, !oldValue);

    return null;
  }
}
