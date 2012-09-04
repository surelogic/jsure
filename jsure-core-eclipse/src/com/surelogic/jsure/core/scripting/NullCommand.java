package com.surelogic.jsure.core.scripting;

/**
 * Does nothing, echoing the command
 * 
 * @author Edwin
 */
public class NullCommand extends AbstractCommand {
  public static final ICommand prototype = new NullCommand();

  @Override
  public boolean execute(ICommandContext context, String... contents) {
    System.out.println("Doing nothing for: "+contents[0]);
    return false;
  }
}
