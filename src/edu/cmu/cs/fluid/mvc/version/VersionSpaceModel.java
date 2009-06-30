package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.mvc.tree.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.ir.*;

/**
 * Interface for version space model&mdash;a forest model that represents
 * projections of the global IR version space.  The nodes in the tree are
 * always IRNodes that come from the IR shadow tree, i.e., they originate
 * from calls to {@link edu.cmu.cs.fluid.version.Version#getShadowNode}.
 *
 * <p>The {@link DigraphModel#CHILDREN}, {@link SymmetricDigraphModel#PARENTS},
 * {@link ForestModel#LOCATION}, and {@link ForestModel#ROOTS} attributes are 
 * never mutable, and their values are never mutable objects.
 *
 * <p>The version space may have zero or more {@link VersionCursorModel}s
 * associated with it.  They are stored in a sequence-valued model-level
 * attribute {@link #CURSORS}.  The significance of the cursors is that 
 * they affect the commands that are available in the model.  Specifically
 * commands for setting the value of the cursor to a particular version 
 * and for controlling the {@link VersionCursorModel#IS_FOLLOWING} attribute
 * of the cursor appear as controller commands in the VersionSpaceModel.
 * <em>The order of the cursors in the sequence is not significant, and it is
 * up to clients of the model to adopt conventions for their naming and 
 * usage</em>.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link ForestModel#ROOTS}
 * <li>{@link #CURSORS}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link DigraphModel#CHILDREN}
 * <li>{@link SymmetricDigraphModel#PARENTS}
 * <li>{@link ForestModel#LOCATION}
 * <LI>{@link #VNAME}
 * <li>{@link #VERSION}
 * </ul>
 *
 * @author Zia Syed
 * @author Aaron Greenhouse
 */
public interface VersionSpaceModel
extends ForestModel
{
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  public static final String CURSORS = "VersionSpaceModel.CURSORS";
  
  
  
  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * Node-level, String-valued, mutable attribute that gives a human
   * readable name to a version.
   */
  public static final String VNAME = "VersionSpaceModel.name";

  /**
   * The version, as a {@link edu.cmu.cs.fluid.version.Version}, associated with a
   * node.  This attribute is immutable.
   */
  public static final String VERSION = "VersionSpaceModel.version";
  

  
  //===========================================================
  //== Convienence methods for adding nodes as Versions
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
  public void addVersionNode( Version base, Version added );
  
  /**
   * Replace a leaf version with a new version.  The name of the version is
   * set from <code>newV.toString</code>.
   *
   * @param oldV The version to replace.  Must be a leaf node.
   * @param newV The version to replace <code>oldV</code> with.
   * @exception IllegalArgumentException Thrown if <code>oldV</code> is not in
   * the version space, if <code>oldV</code> is the root node, or if 
   * <code>oldV</code> is not a leaf node.
   */
  public void replaceVersionNode( Version oldV, Version newV );
  
  /**
   * Add the current IR version, as determined from
   * {@link edu.cmu.cs.fluid.version.Version#getVersion} version to the version space.
   * 
   * @param base The version from which the current version ought to descend.
   * It is an  error if this version is not already in the version space.
   * @exception IllegalArgumentException Thrown if <code>base</code> is not
   * already in the version space or if <code>added</code> does not descend from
   * <code>base</code> in the global IR Version space.
   */
  public void addVersionNode( Version base );
  

  
  //===========================================================
  //== Convienence methods for getting model attribute values
  //===========================================================

  /** Get the value of the {@link #CURSORS} attribute. */
  public IRSequence getCursors();
  

  
  //===========================================================
  //== Convienence methods for getting node attribute values
  //===========================================================

  /**
   * Get the name of the version node.
   * @param node The node whose version name is returned.
   * @return name of the version node
   */
  public String getName( IRNode node );
  
  /**
   * Set the version name of a node.
   * @param node The node whose name is set.
   * @param name The new version name.
   */
  public void setName( IRNode node, String name );
  
  /**
   * Get the version associated with a node.
   * @param node The node whose version is retreived.
   * @return The version associated with the given node.
   */
  public Version getVersion( IRNode node );
  

  
  //===========================================================
  //== Other methods
  //===========================================================

  /**
   * Record that a particular VersionCursor is observing the model.
   * <em>This really ought to be a class-level attribute of the model</em>.
   * @param vc The version cursor to associate with the model.
   */
  //public void associateVersionCursor( VersionCursor vc );
  
  /*
   * Remove the parent of the given node from the version space, but keep the 
   * subtree rooted at the child.
   * @param child The node whose parent is to be removed from the space.
   * @exception IllegalArgumentException Thrown if the given node is not part
   * of the model, if the given node is the root, if the given node is not a
   * leat node, if the given node as siblings, or if the given node is the
   * child of the root node.
   * @return The version associated with the removed node.
   *
  public Version mergeWithParent( Version child ); 
   **/


  
  //===========================================================
  //== Factory
  //===========================================================
  
  /**
   * Interface for factories the create instances of {@link VersionSpaceModel}.
   */
  public static interface Factory
  {
    /**
     * Create a new version space model.
     * @param name The name of the model.
     * @param root The version to use as the root of the version space.
     * @param names The names of the VersionCursorModels to be created
     *   and associated with the VersonSpaceModel.
     */
    public VersionSpaceModel create( String name, Version root, String[] names )
    throws SlotAlreadyRegisteredException;
  }
}

