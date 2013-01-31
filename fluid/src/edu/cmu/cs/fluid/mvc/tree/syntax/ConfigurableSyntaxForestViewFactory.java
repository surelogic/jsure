/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/ConfigurableSyntaxForestViewFactory.java,v 1.9 2006/03/30 19:47:20 chance Exp $
 *
 * ConfigurableForestViewFactory.java
 * Created on April 3, 2002, 1:38 PM
 */

package edu.cmu.cs.fluid.mvc.tree.syntax;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ConfigurableViewCore;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.tree.ForestEllipsisPolicy;
import edu.cmu.cs.fluid.mvc.tree.ForestForestModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestProxyAttributePolicy;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.SyntaxTree;

/**
 * Factory that returns minimal implementations of
 * {@link ConfigurableSyntaxForestView}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}. 
 *
 * @author Aaron Greenhouse
 */
public class ConfigurableSyntaxForestViewFactory
implements ConfigurableSyntaxForestView.Factory
{
  /**
   * The singleton reference.
   */
  public static final ConfigurableSyntaxForestView.Factory prototype =
    new ConfigurableSyntaxForestViewFactory();
  
  
  
 
  /**
   * Use the singleton reference {@link #prototype}.
   */
  public ConfigurableSyntaxForestViewFactory()
  {
  }

  
  
  @Override
  public ConfigurableSyntaxForestView create(final String name,
      final SyntaxForestModel src, final VisibilityModel vizModel,
      final AttributeInheritancePolicy aip,
      final ForestProxyAttributePolicy pp,
      final ForestEllipsisPolicy ellipsisPolicy, final boolean expFlat,
      final boolean expPath) throws SlotAlreadyRegisteredException {
    final SyntaxTree tree = new SyntaxTree(name + "-tree_delegate",
        SimpleSlotFactory.prototype);
    final IRSequence<IRNode> roots = SimpleSlotFactory.prototype.newSequence(-1);
    return new ConfigurableSyntaxForestViewImpl(name, src, vizModel,
        ModelCore.simpleFactory, ViewCore.standardFactory,
        new ForestForestModelCore.DelegatingFactory(tree, roots,
            SimpleSlotFactory.prototype, false),
        new SyntaxForestModelCore.StandardFactory(tree),
        ConfigurableViewCore.standardFactory, aip, pp, ellipsisPolicy,
        expFlat, expPath);
  }
}
