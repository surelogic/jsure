package edu.cmu.cs.fluid.render;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.SimpleComponentSlotFactory;
import edu.cmu.cs.fluid.mvc.set.SetModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;

public final class StyleSetFactory implements StyleSetModel.Factory {
  public static final StyleSetFactory prototype = new StyleSetFactory();

  @Override
  public StyleSetModel create( final String name, 
                               final ExplicitSlotFactory sf ) 
    throws SlotAlreadyRegisteredException
  {
    return 
      new StyleSetImpl(name, sf, 
                       new ModelCore.StandardFactory( sf ),
                       new SetModelCore.StandardFactory(sf, 
                                                        new SimpleComponentSlotFactory(sf)));
  }
}
