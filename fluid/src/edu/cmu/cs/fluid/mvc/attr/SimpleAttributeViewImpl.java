// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/SimpleAttributeViewImpl.java,v 1.7 2007/07/05 18:15:23 aarong Exp $
package edu.cmu.cs.fluid.mvc.attr;

import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ModelEvent;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.set.SetModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * <em>Should probably do something about listening for
 * new attributes</em>.
 *
 * @author Aaron Greenhouse
 */
final class SimpleAttributeViewImpl
extends AbstractModelToAttributeStatefulView
implements SimpleAttributeView
{
  //===========================================================
  //== Constructor
  //===========================================================

  public SimpleAttributeViewImpl(
    final String name, final Model src, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final SetModelCore.Factory smf,
    final AttributeModelCore.Factory amf )
  throws SlotAlreadyRegisteredException
  {
    super( name, src, mf, vf, smf, amf );

    // need to get notification of new attributes some how
    // Listen for NewAttributesEvents

    // initialized the model
    rebuildModel();
    srcModel.addModelListener( srcModelBreakageHandler );
    finalizeInitialization();
  }



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin AbstractModelToModelStatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  @Override
  protected void rebuildModel( final List events )
  {
    // ought to protect against new attributes being added during 
    // a rebuild...

    synchronized( structLock ) {
      clearModel();

      final Iterator nAttrs = srcModel.getNodeAttributes();
      while( nAttrs.hasNext() ) {        
        final String attrName  = (String)nAttrs.next();
	attrModCore.initNodeAttribute( srcModel, setModCore, attrName );
      }
      final Iterator mAttrs = srcModel.getComponentAttributes();
      while( mAttrs.hasNext() ) {        
        final String attrName = (String)mAttrs.next();
	attrModCore.initCompAttribute( srcModel, setModCore, attrName );
      }
    }
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End AbstractModelToModelStatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
