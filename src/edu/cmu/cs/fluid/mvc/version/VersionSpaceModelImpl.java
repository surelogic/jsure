package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.*;

			  
/**
 * Minimal implementation of VersionSpaceModel.
 * 
 * @author Zia Syed
 */
final class VersionSpaceModelImpl
extends AbstractVersionSpaceModel
implements VersionSpaceModel
{
  protected VersionSpaceModelImpl(
    final String name, final Version rootVersion, final String[] names,
    final ModelCore.Factory mf, final ForestModelCore.Factory fmf,
    final VersionSpaceModelCore.Factory vmf,
    final AttributeManager.Factory attrFactory )
  throws SlotAlreadyRegisteredException
  {    
    super( name, rootVersion, names, mf, fmf, vmf, attrFactory );
    finalizeInitialization();
  }
}
