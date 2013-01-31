package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.*;

/**
 * Abstract implemenation of core class for <code>VersionSpaceModel</code>.
 * Adds the node-level attributes {@link VersionSpaceModel#VNAME} and
 * {@link VersionSpaceModel#VERSION} and the model-level attribute
 * {@link VersionSpaceModel#CURSORS}.
 */
public final class VersionSpaceModelCore
extends AbstractCore
{
  //===========================================================
  //== Node level attributes
  //===========================================================

  /** The version name attribute. */
  private final SlotInfo<String> vname;
  
  /** The version attribute. */
  private final SlotInfo<Version> version;
 
  /** The cursors attribute */
  private final ComponentSlot<IRSequence<Model>> cursors;
  
  
  
  //===========================================================
  //== Constructors
  //===========================================================
  
  protected VersionSpaceModelCore(
    final String name, final Version rootVersion, 
    final String[] names, final ForestModelCore fmc,
    final Model model, final Object mutex, final AttributeManager manager,
    final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    super( model, mutex, manager );

    /*
     * Init the node-level attributes.
     */
    SlotFactory sf = SimpleSlotFactory.prototype;
    vname = sf.newAttribute( name + "-" + VersionSpaceModel.VNAME,
                                IRStringType.prototype );
    version = sf.newAttribute( name + "-" + VersionSpaceModel.VERSION,
                                  IRVersionType.prototype );   
    manager.addNodeAttribute(
      VersionSpaceModel.VNAME, Model.INFORMATIONAL, vname, cb );
    manager.addNodeAttribute(
      VersionSpaceModel.VERSION, Model.INFORMATIONAL, version );

    /*
     * Init the root version node.
     */
    final IRNode rootNode = rootVersion.getShadowNode();
    if( !fmc.isNode( rootNode ) ) fmc.initNode( rootNode );
    rootNode.setSlotValue( vname, rootVersion.toString() );
    rootNode.setSlotValue( version, rootVersion );
    fmc.addRoot( rootNode );

    /*
     * Init the model-level CURSORS attribute.  This involves creating
     * VersionCursorModel instances.
     */
    final IRSequence<Model> cursorsSeq =
      SimpleSlotFactory.prototype.newSequence( names.length );
    for( int i = 0; i < names.length; i++ ) {
      cursorsSeq.setElementAt(
        VersionCursorFactory.prototype.create(
          names[i], (VersionSpaceModel) partOf, true ),
        i );
    }
    cursors = SimpleComponentSlotFactory.constantPrototype.predefinedSlot(
                new IRSequenceType<Model>( ModelType.prototype ), cursorsSeq );
    manager.addCompAttribute(
      VersionSpaceModel.CURSORS, Model.STRUCTURAL, cursors );    
  }
  
  
  
  //===========================================================
  //== Attribute Convience Methods
  //===========================================================
  
  /** Get the sequence of version cursors */
  public IRSequence<Model> getCursors()
  {
    return cursors.getValue();
  }
  
  /**
   * Get the name of the version node.
   * @param node The node whose version name is returned.
   * @return name of the version node
   */
  public String getName( final IRNode node )
  {
    return node.getSlotValue( vname );
  }
  
  /**
   * Set the version name of a node.
   * @param node The node whose name is set.
   * @param nm The new version name.
   */
  public void setName(final IRNode node, final String nm) {
    node.setSlotValue(vname,nm);
  }
  
  /**
   * Get the version associated with a node.
   * @param node The node whose version is retreived.
   * @return The version associated with the given node.
   */
  public Version getVersion( final IRNode node) {
    return node.getSlotValue(version);
  }
  
  
  
  //===========================================================
  //== Core functions specific to VersionSpaceModel
  //===========================================================
  
  /**
   * Add a new version to the version space.  The name of the version is
   * set from <code>added.toString</code>.
   * @param base The version from which the new version descends.  It is an
   * error if this version is not already in the version space.
   * @param added The new version to add to the space.  It is an error if this
   * version does not descend from <code>base</code> in the global IR version space.
   * @exception IllegalArgumentException Thrown if <code>base</code> is not
   * already in the version space or if <code>added</code> does not descend from
   * <code>base</code> in the global IR Version space.
   */
  public void addVersionNode(
    final ForestModelCore fmc, final Version base, final Version added )
  {
    final IRNode baseNode = base.getShadowNode();

    // Check the arguments for correctness
    if( !added.comesFrom( base ) || base.equals( added ) ) {
      throw new IllegalArgumentException(
                  "Added version '"+added+"' is not a proper descendent of the base version "+base );
    }

    // check if the base version exists in the tree or not
    if( !fmc.isPresent( baseNode ) ) {
      throw new IllegalArgumentException( "Base version not part of the model." );
    }

    // get IRNode of the target version
    final IRNode addedNode = added.getShadowNode();
    if( !fmc.isNode( addedNode ) ) fmc.initNode( addedNode );
    fmc.addChild( baseNode, addedNode );
    addedNode.setSlotValue( vname, added.toString() );
    addedNode.setSlotValue( version, added );
  }
  
  /*
   * Remove the parent of the given node from the version space, but keep the 
   * subtree rooted at the child.
   * @param child The node whose parent is to be removed from the space.
   * @exception IllegalArgumentException Thrown if the given node is not part
   * of the model, if the given node is the root, if the given node is not a
   * leat node, if the given node as siblings, or if the given node is the
   * child of the root node.
   * @return The version associated with the removed node.
   */
/*  
  public Version mergeWithParent(
    final ForestModelCore fmc, final Version child )
  {
    final IRNode childNode = child.getShadowNode();

    // if childNode not part of forest model core then throw exception
    if( !fmc.isPresent( childNode ) ) {
      throw new IllegalArgumentException( "Child version not part of the model." );
    }

    // if childNode is the root then throw exception
    if( fmc.isRoot( childNode ) ) {
      throw new IllegalArgumentException( "Child version is root" );
    }

    // if childNode is not a leafNode then throw an exception
    if( fmc.hasChildren( childNode ) ) {
      throw new IllegalArgumentException( "Child is not a leaf node" );
    }

    final IRNode parentNode = fmc.getParent( childNode );

    // if childNode is not the only child of the parent Node then
    // throw an excpetion
    if( !fmc.children( parentNode ).nextElement().equals( childNode ) ) {
      throw new IllegalArgumentException( "Child has siblings." );
    }

    // if parentNode is the root then throw an excpetion
    if( fmc.isRoot( parentNode ) ) {
      throw new IllegalArgumentException( "Parent of child is root" );
    }

    final IRNode grandparentNode = fmc.getParent( parentNode );
    final Version removedVersion = getVersion( parentNode );
    fmc.removeSubtree( parentNode );
    fmc.addChild( grandparentNode, childNode );
    return removedVersion;
  }
*/
  
  /**
   * Replace a leaf version with a new version.  The name of the version is
   * set from <code>newV.toString</code>.
   *
   * @param oldV The version to replace.  Must be a leaft node.
   * @param newV The version to replace <code>oldV</code> with.
   * @exception IllegalArgumentException Thrown if <code>oldV</code> is not in
   * the version space, if <code>oldV</code> is the root node, or if
   * <code>oldV</code> is not a leaf node.
   */
  public void replaceVersionNode(
    final ForestModelCore fmc,final Version oldV, final Version newV )
  {
    final IRNode oldNode = oldV.getShadowNode();
    final IRNode newNode = newV.getShadowNode();
    
    if( !fmc.isPresent( oldNode ) ) {
      throw new IllegalArgumentException("Old version not part of the model.");
    }
      
    if(fmc.hasChildren(oldNode)) {
      throw new IllegalArgumentException("Old version is not a leaf node");
    }
      
    if(fmc.isRoot(oldNode)) {
      throw new IllegalArgumentException("Old version can not be root node.");
    }
      
    final IRNode oldNodeParent = fmc.getParent(oldNode);
    // add the newly created IRNode as a child to the base version node
    if(!fmc.isNode(newNode)) fmc.initNode(newNode);
      
    // init the attributes of the new node and replace the old node.
    newNode.setSlotValue(vname,newV.toString());
    newNode.setSlotValue(version,newV);
    fmc.replaceChild(oldNodeParent,oldNode,newNode);
  }
  
  //===========================================================
  //== VersionSpaceModelCore Factory Interfaces/Classes
  //===========================================================
  
  public static interface Factory {
    public VersionSpaceModelCore create(
      String name, Version rootVersion, String[] names, ForestModelCore fmc,
      Model model, Object structLock, AttributeManager manager,
      AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException;
  }
  
  private static class StandardFactory
  implements Factory
  {
    @Override
    public VersionSpaceModelCore create(
      final String name, final Version rootVersion, 
      final String[] names, final ForestModelCore fmc,
      final Model model, final Object structLock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new VersionSpaceModelCore(
                   name, rootVersion, names, fmc, model, structLock, manager, cb );
    }
  }
  
  public static final StandardFactory standardFactory = new StandardFactory();
}




