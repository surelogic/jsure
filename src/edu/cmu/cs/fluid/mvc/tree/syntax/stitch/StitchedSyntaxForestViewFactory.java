/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/stitch/StitchedSyntaxForestViewFactory.java,v 1.5 2006/03/30 19:47:21 chance Exp $
 *
 * Created on April 3, 2002, 1:38 PM
 */

package edu.cmu.cs.fluid.mvc.tree.syntax.stitch;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.tree.ForestForestModelCore;
import edu.cmu.cs.fluid.mvc.tree.stitch.IStitchTreeTransform;
import edu.cmu.cs.fluid.mvc.tree.syntax.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.tree.*;

/**
 * Factory that returns minimal implementations of
 * {@link StitchedSyntaxForestView}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}. 
 *
 * @author Aaron Greenhouse
 */
public class StitchedSyntaxForestViewFactory
{
  /**
   * The singleton reference.
   */
  public static final StitchedSyntaxForestViewFactory prototype =
    new StitchedSyntaxForestViewFactory();
 
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private StitchedSyntaxForestViewFactory()
  {
  }
  
  public StitchedSyntaxForestView create(final String name,
      final SyntaxTree tree,
      final SyntaxForestModel src, 
      final AttributeInheritancePolicy aip,
      final IStitchTreeTransform.Factory xform) throws SlotAlreadyRegisteredException {

    final SlotFactory sf   = SimpleSlotFactory.prototype;
    final SyntaxTree tree2 = StitchedTreeFactory.createTree(tree);
    final IRSequence<IRNode> roots = sf.newSequence(-1);

    return new StitchedSyntaxForestView(name, src,
        ModelCore.simpleFactory, ViewCore.standardFactory,
        new ForestForestModelCore.DelegatingFactory(tree2, roots,
            SimpleSlotFactory.prototype, false),
        new SyntaxForestModelCore.StandardFactory(tree2),
        aip,
        xform);
  }
}
