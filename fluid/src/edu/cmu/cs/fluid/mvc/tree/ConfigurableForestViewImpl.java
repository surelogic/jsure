// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/ConfigurableForestViewImpl.java,v 1.11 2007/07/05 18:15:17 aarong Exp $

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ConfigurableViewCore;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;


/**
 * A minimal implementation of {@link ConfigurableForestView}/
 *
 * @author Aaron Greenhouse
 */
final class ConfigurableForestViewImpl
extends AbstractConfigurableForest
implements ConfigurableForestView
{
  //===========================================================
  //== Constructor
  //===========================================================

  protected ConfigurableForestViewImpl(
    final String name, final ForestModel src, final VisibilityModel vizModel,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf, final ConfigurableViewCore.Factory cvf,
    final AttributeInheritancePolicy aip,  final ForestProxyAttributePolicy pp,
    final ForestEllipsisPolicy ePolicy, final boolean ef, final boolean ep )
  throws SlotAlreadyRegisteredException
  {
    super( name, src, vizModel, mf, vf, fmf, cvf, aip, pp, ePolicy,
           ef, ep );
           
    rebuildModel();       
    finalizeInitialization();
  }

  
  
  //===========================================================
  //== Methods for building the exported model
  //===========================================================

  @Override
  protected void setupNode( final IRNode n )
  {
    forestModCore.initNode( n );
  }

  @Override
  protected void setupEllipsisNode( final IRNode n )
  {
    forestModCore.initNode( n );
  }

  @Override
  protected void addSubtree( final IRNode parent, final IRNode n,
      final boolean sameParent, final int oldPos )
  {
    forestModCore.appendSubtree(parent, n);
  }
}
