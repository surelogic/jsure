package com.surelogic.test.scripting;

/**
 * Does nothing, echoing the command
 * 
 * @author Edwin
 */
public class NullCommand extends AbstractCommand {
  public static final ICommand prototype = new NullCommand();

  public boolean execute(ICommandContext context, String... contents) {
    System.out.println("Doing nothing for: "+contents[0]);
    return false;
  }
}
