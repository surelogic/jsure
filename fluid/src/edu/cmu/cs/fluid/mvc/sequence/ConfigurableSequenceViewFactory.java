/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/ConfigurableSequenceViewFactory.java,v 1.10 2006/03/29 19:54:51 chance Exp $
 *
 * ConfigurableSetViewFactory.java
 * Created on March 25, 2002, 10:54 AM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory implementation that returns minimal implementations of 
 * {@link ConfigurableSequenceView}.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor.  Clients should use the {@link #prototype} field
 * to access the only instances of this class.
 *
 * @author Aaron Greenhouse
 */
public final class ConfigurableSequenceViewFactory
implements ConfigurableSequenceView.Factory
{
  /**
   * The singleton reference.
   */
  public static final ConfigurableSequenceView.Factory prototype =
    new ConfigurableSequenceViewFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private ConfigurableSequenceViewFactory()
  {
  }
  
  /**
   * Create a new instances of {@link ConfigurableSequenceView}.
   * @param name The name of the new model.
   * @param src The SetModel to be viewed.
   * @param vizModel The Visibility Model of the provided SetModel.
   * @param attrPolicy The policy for inheriting attributes in the
   *  exported model.
   * @param proxyPolicy The policy for attributing proxy nodes.
   */
  @Override
  public ConfigurableSequenceView create(
    final String name, final SequenceModel src, final VisibilityModel vizModel,
    final AttributeInheritancePolicy attrPolicy,
    final ProxyAttributePolicy proxyPolicy )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = SimpleSlotFactory.prototype.newSequence(~0);
    return new ConfigurableSequenceViewImpl( 
                 name, src, vizModel, ModelCore.simpleFactory,
                 ViewCore.standardFactory, 
                 new SequenceModelCore.StandardFactory(
                       seq,
                       SimpleSlotFactory.prototype, false ),
                 ConfigurableViewCore.standardFactory, 
                 attrPolicy, proxyPolicy );
  }
}
