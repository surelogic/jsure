/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/SynthesizedForestViewFactory.java,v 1.6 2003/07/15 18:39:10 thallora Exp $
 *
 * SynthesizedForestViewFactory.java
 * Created on May 14, 2003, 4:43 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.attr.SortedAttributeView;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory that returns minimal implementations of {@link ConfigurableForestView}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}. 
 *
 * @author Aaron Greenhouse
 */
public class SynthesizedForestViewFactory
implements SynthesizedForestView.Factory
{
  /**
   * The singleton reference.
   */
  public static final SynthesizedForestView.Factory prototype = new SynthesizedForestViewFactory();
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  public SynthesizedForestViewFactory()
  {
  }

  
  
  @Override
  public SynthesizedForestView create(final String name, final Model src, 
                                       final SortedAttributeView sav,
                                       final String labelAttr,
                                       final AttributeInheritancePolicy aip)
  throws SlotAlreadyRegisteredException
  {
    return new SynthesizedForestViewImpl(name, src, sav, labelAttr,
                                          ModelCore.simpleFactory,
                                          ViewCore.standardFactory,
                                          new ForestForestModelCore.StandardFactory(SimpleSlotFactory.prototype, false),
                                          aip);
  }
}
