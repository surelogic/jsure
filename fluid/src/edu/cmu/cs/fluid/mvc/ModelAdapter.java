package edu.cmu.cs.fluid.mvc;


/**
 * Class for <code>ModelListener</code> adpaters.
 *
 * @author Aaron Greenhouse
 */
public class ModelAdapter implements ModelListener
{
  @Override
  public void breakView(final ModelEvent e) {
    // do nothing
  }
  
  @Override
  public void removedFromModel(final Model m) {
     // do nothing
  }
  
  @Override
  public void addedToModel(final Model m) {
    // do nothing
  }
}
