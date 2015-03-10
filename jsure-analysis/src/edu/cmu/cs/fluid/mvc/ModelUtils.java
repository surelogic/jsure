/*
 * Created on Oct 27, 2004
 */
package edu.cmu.cs.fluid.mvc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRSequence;

/**
 * Various utility methods for dealing with models and model processing.
 * 
 * @author aarong
 */
public class ModelUtils {
  /**
   * Shutdown all the models in the model&ndash;view chain that terminates in
   * this model. The models are {@link Model#shutdown() shutdown} in a breadth
   * first traversal up the chain. Each model in the chain is shutdown exactly
   * once.
   * 
   * @param model
   *          The terminal model in the chain to shutdown.
   */
  public static void shutdownChain(final Model model) {
    // models that were already shutdown
    final Set<Model> alreadyShutdown = new HashSet<Model>();
    // models to be visited in the breadth-first search
    final LinkedList<Model> queue = new LinkedList<Model>();
    
    queue.addLast(model);
    while (!queue.isEmpty()) {
      final Model current = queue.removeFirst();
      current.shutdown();
      alreadyShutdown.add(current);
      
      for (final Iterator<Model> i = getSrcModels(current); i.hasNext();) {
        final Model srcModel = i.next();
        if (!alreadyShutdown.contains(srcModel)) {
          queue.addLast(srcModel);
        }
      }
    }
  }
  
  /**
   * Get all the models in model&ndash;view chain that terminates in this model.
   * 
   * @param model
   *            The terminal model in the chain of interest.
   * @return An immutable sorted set of all the models in the chain, including
   *         <code>model</code>.  The set is ordered by the model's id.
   */
  public static SortedSet<Model> getModelsInChain(final Model model) {
    final SortedSet<Model> chain = new TreeSet<Model>(GlobalModelInformation.getInstance().getModelIDComparator());
    getModelsInChainHelper(model, chain);
    return Collections.unmodifiableSortedSet(chain);
  }

	private static void getModelsInChainHelper(
        final Model model, final SortedSet<Model> chain) {
    final LinkedList<Model> queue = new LinkedList<Model>();
    queue.addLast(model);

    // Breadth first search
    while (!queue.isEmpty()) {
      final Model current = queue.removeFirst();
      if (chain.add(current)) {
        for (final Iterator<Model> i = getSrcModels(current); i.hasNext();) {
          queue.addLast(i.next());
        }
      }
    }
  }

  public static SortedSet<Model> getModelsInChains(final Set<Model> modelsSet) {
    final Model[] models = new Model[modelsSet.size()];
    return getModelsInChains(modelsSet.toArray(models));
  }
  
  public static SortedSet<Model> getModelsInChains(final Model[] models) {
    final SortedSet<Model> chain = new TreeSet<Model>(GlobalModelInformation.getInstance().getModelIDComparator());
    for (int i = 0; i < models.length; i++) {
      getModelsInChainHelper(models[i], chain);   
    }
    return Collections.unmodifiableSortedSet(chain);
  }
  
  /**
   * Wrap a model action so that it acquires all the locks for all the models
   * in the chains the terminate at the given models.  This method basically
   * takes the given action and invokes {@link Model#atomizeAction(Model.AtomizedModelAction)}
   * on all the models in the chain in the order of their unique ids.  Consistent
   * use of this method for all actions should prevent corruption of the models
   * as well as prevent deadlock by always acquiring the locks in a canonical
   * order.
   */
  public static Model.AtomizedModelAction wrapAction(final Model[] models, final Model.AtomizedModelAction action) {
    final SortedSet orderedModels = getModelsInChains(models);
    return wrapAction(orderedModels, action);
  }
  
  /**
   * A convenience version of the version for multiple modesl
   */
  public static Model.AtomizedModelAction wrapAction(final Model model, final Model.AtomizedModelAction action) {
    final SortedSet orderedModels = getModelsInChain(model);
    return wrapAction(orderedModels, action);
  }
  
  private static Model.AtomizedModelAction wrapAction(final Set orderedModels, final Model.AtomizedModelAction action) {
    Model.AtomizedModelAction wrappedAction = action;
    for (final Iterator i = orderedModels.iterator(); i.hasNext();) {
      final Model m = (Model) i.next();
      wrappedAction = m.atomizeAction(wrappedAction);
    }
    return wrappedAction; 
  }
  
  /**
   * Get the immediate ancestors of a model. This is really a short cut method
   * for seeing if the model has the attribute
   * {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}.
   * 
   * @param model
   *            The model whose ancestors should be returned.
   * @return An iterator over the source models. If the model does not have
   *         ancestors (e.g., it is a pure model) then the empty iterator is
   *         returned.
   */
  @SuppressWarnings("unchecked")
  public static Iterator<Model> getSrcModels(final Model model) {
    try {
      final IRSequence seq = (IRSequence) model.getCompAttribute(View.SRC_MODELS).getValue();
      return seq.elements();
    } catch(final UnknownAttributeException e) {
      return new EmptyIterator<Model>(); 
    }
  }
}
