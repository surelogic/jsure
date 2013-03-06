/*
 * Created on Oct 26, 2004
 */
package edu.cmu.cs.fluid.mvc;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * This class is a low-rent proxy for the yet-to-be-written meta-model of MVC
 * information. Here we have finally been forced to produce some sort of
 * meta-model because we need to be able to assign a global ordering to all the
 * models for concurrency/synchronization purposes.
 * 
 * <p>
 * Currently this class
 * <ul>
 * <li> Keeps track of assigning unique identifiers to models so that they can
 * be put in a cannonical order for locking purposes.
 * <!--
 * <li> Tracks which models a model is a view of. This is used to deconstruct
 * model&ndash;view chains.  This information is available from a View's
 * {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS source models} attribute, but
 * that attribute must be correctly set my the view.  Instead, the intent here
 * is that this information will be updated whenever model listeners are added
 * and removed from models, so it should be more reliable.
 * -->
 * </ul>
 * 
 * <p>
 * There is exactly one instance of this object, obtained by the static method
 * {@link #getInstance}.
 * 
 * @author aarong
 */
public final class GlobalModelInformation {
  /**
   * The one and only reference to an object of this class.
   */
  private static final GlobalModelInformation prototype = new GlobalModelInformation();
  
  /** 
   * Map from models to their information.  <em>The model's information must
   * not contained a (transitively) strong reference to the
   * {@link edu.cmu.cs.fluid.ir.IRNode} that is identified with that model.</em>
   * Access to this map is protected by synchronizing on <code>this</code>.
   */
  private Map<IRNode,ModelRecord> theModels = new WeakHashMap<IRNode,ModelRecord>();
  
  /**
   * The next model ID.  Protected by synchronizing on <code>this</code>.
   * This ID is primarily used for providing a canonical linear ordering 
   * of the models for synchronization purposes.
   */
  private long nextID = 1;
  
  /**
   * Comparator for sorting lists according to their ID.
   */
  private final Comparator<Model> idComparator;
  
  /**
   * Private constructor to prevent instances of the class from being
   * created willy-nilly.
   */
  private GlobalModelInformation() {
    idComparator = new IDComparator();
  }
  
  /**
   * Get the one-and-only instance of this class.
   */
  public static GlobalModelInformation getInstance() {
    return prototype;
  }

  /**
   * A new model has been created and should be added to the global set of
   * models.  This should only be invoked my the {@link ModelCore}
   * constructor.  
   * 
   * <p>(SHould be able to enforce the above restriction if instead we had
   * a global framework of general model listeners so that we could locally
   * (within the <code>GlobalModelInformation</code> instance) listen for 
   * new model creation events and then add the new model to the database. 
   * But such a frameworks causes additional problems regarding synchronization
   * and appropriate ordering of MVC intialization that I don't want to deal
   * with right now.) 
   */
  public synchronized void newModel(final Model model) {
    final ModelRecord record = new ModelRecord(nextID);
    nextID += 1;
    
    theModels.put(model.getNode(), record);
  }

  /**
   * Get the id for the particular model.  The ID is a 64-bit number >= 1.
   * @exception IllegalArgumentException Thrown if the given model is not 
   * registered.
   */
  public synchronized long getModelID(final Model model) {
    final ModelRecord record = theModels.get(model.getNode());
    if (record != null) {
      return record.getID();
    } else {
      throw new IllegalArgumentException("Couldn't find record for model \""
          + model.getName() + "\"");
    }
  }
  
  /**
   * Unsafe (in that it doesn't check that the model exists), 
   * unsynchronized private version of getModelID.
   * Caller must hold the lock. 
   */
  private long unsafeGetModelID(final Model model) {
    return theModels.get(model.getNode()).getID();
  }
  
  /**
   * Sort a list of models (in place) in ascending according to their model ID. 
   * The given list must be modifiable.
   * @see java.util.Collections#sort(java.util.List, java.util.Comparator) 
   */
  public synchronized void sortModelsByID(final List<Model> models) {
    Collections.sort(models, idComparator);
  }
  
  /**
   * Get the comparator for compariing {@link Model}s by their id's.
   */
  public Comparator<Model> getModelIDComparator() {
    return idComparator; 
  }
  
  private class IDComparator implements Comparator<Model> {
    /** Caller must hold the lock on the GlobalModelInformation object */
    @Override
    public int compare(final Model m1, final Model m2) {
      final long id1 = unsafeGetModelID(m1);
      final long id2 = unsafeGetModelID(m2);
      // bit-trickery to avoid branching
      return (int) (((id1-id2) | 0x4000000000000000L) >> 32);
    }
  }
  
  
  
  /**
   * Record of information associated with a model.  Currently just the 
   * model's ID.
   */
  public static class ModelRecord {
    private final long id;
    
    public ModelRecord(final long id) {
      this.id = id;
    }
    
    public long getID() {
      return id;
    }
  }
}
