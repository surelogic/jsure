package com.surelogic.jsure.core.scripting;

public interface ICommand {	
  /**
   * @param contents The command line, including the name given to 
   *                 find this command
   *                 
   * @return true if a resource changed
   */
  boolean execute(ICommandContext context, String... contents) throws Exception;
  
  boolean succeeded();
}
