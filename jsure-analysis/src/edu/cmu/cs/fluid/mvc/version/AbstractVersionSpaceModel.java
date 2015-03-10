package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.*;

			  
/**
 * Abstract implementation of {@link VersionSpaceModel}.  Concrete subclasses
 * must implement the interface {@link VersionSpaceModel}.
 * 
 * @author Aaron Greenhouse
 */
public class AbstractVersionSpaceModel
extends AbstractImmutableForestModel
{
  protected final VersionSpaceModelCore verModCore;
  // FIX
  private final Version rootV;
  
  
  //===========================================================
  //== Constructors
  //===========================================================
	
  protected AbstractVersionSpaceModel(
    final String name, final Version rootVersion, final String[] names,
    final ModelCore.Factory mf, final ForestModelCore.Factory fmf,
    final VersionSpaceModelCore.Factory vsmf,
    final AttributeManager.Factory attrFactory )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, fmf, SimpleSlotFactory.prototype );
    rootV       = rootVersion;
    verModCore  = vsmf.create( name, rootVersion, names, forestModCore, this,
			       structLock, attrManager,
			       new LabelChangedCallback() );
  }



  //===========================================================
  //== Attribute Callback for Label Attribute
  //===========================================================

  private class LabelChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object value )
    {
      if( attr == VersionSpaceModel.VNAME ) {
        modelCore.fireModelEvent(
          new AttributeValuesChangedEvent(
                AbstractVersionSpaceModel.this, node, attr, value ) );
      }
    }
  }


	
  //===========================================================
  //== VersionSpaceModel interface methods
  //===========================================================

  public final void addVersionNode(final Version base,final Version added)
  {
    synchronized( structLock ) {
      verModCore.addVersionNode(forestModCore, base, added);
    }
    modelCore.fireModelEvent(new VersionSpaceEvent(this,base,added));
  }

  public final void replaceVersionNode(final Version oldV,final Version newV)
  {
    synchronized( structLock ) {
      verModCore.replaceVersionNode(forestModCore, oldV, newV);
    }
    /* XXX This event is wrong: should be the (this, parent-of-oldV, newV ) */
    modelCore.fireModelEvent(new VersionSpaceEvent(this,oldV,newV));
  }

  public final void addVersionNode(final Version base)
  {
    synchronized( structLock ) {
      addVersionNode(base,Version.getVersion());
    }
  }

/*
  public final Version mergeWithParent(final Version child)
  {
    Version parent = null;
    synchronized( structLock ) {
      parent = verModCore.mergeWithParent(forestModCore,child);
    }
    // break all the cursors watching the parent version
    modelCore.fireModelEvent(new VersionSpaceEvent(this,parent,child));
    return parent;
  }
*/
  
  public final IRSequence getCursors()
  {
    synchronized( structLock ) {
      return verModCore.getCursors();
    }
  }
  
  public final String getName(final IRNode node)
  {
    synchronized( structLock ) {
      return verModCore.getName(node);
    }
  }

  public final void setName(final IRNode node, final String name)
  {
    synchronized( structLock ) {
      verModCore.setName(node,name);
    }
    modelCore.fireModelEvent(
      new AttributeValuesChangedEvent(
            this, node, VersionSpaceModel.VNAME, name ) );
  }

  public final Version getVersion(final IRNode node)
  {
    synchronized( structLock ) {
      if (verModCore == null) {
        return rootV;
      } else {
        return verModCore.getVersion(node);
      }
    }
  }

 // public void associateVersionCursor(VersionCursor vc) {}

  
    
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  @Override
  public final String idNode( final IRNode node ) 
  {
    return getName( node );
  }
  
  @Override
  public final void addNode( final IRNode node, final AVPair[] attrs )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
