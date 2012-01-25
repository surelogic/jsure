/*
 * Created on May 14, 2003
 *  
 */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.attr.SortedAttributeView;
import edu.cmu.cs.fluid.util.*;

/**
 * @author Edwin Chan
 */
@SuppressWarnings("unchecked")
public final class SynthesizedForestViewImpl
  extends AbstractModelToForestStatefulView
  implements SynthesizedForestView {
  /**
	 * Logger for this class
	 */
  static final Logger LOG = SLLogger.getLogger("MV.tree.synth");

  private final Model srcModel;
  private final SortedAttributeView attrView;

  /*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.fluid.mvc.AbstractModelToModelStatefulView#rebuildModel(java.util.List)
	 */
  private final SlotInfo<Object> LABEL;

  private String[] attrNames;
  private SlotInfo[] attrs;

  private final Map<String,Comparator<Object>> comparators = new HashMap<String,Comparator<Object>>();

  /** The attribute inheritance policy. */
  private final AttributeInheritancePolicy attrPolicy;

  /**
	 * @throws SlotAlreadyRegisteredException
	 */
  public SynthesizedForestViewImpl(
    final String name,
    final Model src,
    final SortedAttributeView sav,
    final String labelAttr,
    ModelCore.Factory mf,
    ViewCore.Factory vf,
    ForestModelCore.Factory fmf,
    AttributeInheritancePolicy aip)
    throws SlotAlreadyRegisteredException {
    super(
      name,
      mf,
      vf,
      fmf,
      LocalAttributeManagerFactory.prototype,
      ProxySupportingAttributeManagerFactory.prototype);
    // TODO is this right?
    srcModel = src;
    attrView = sav;
    attrPolicy = aip;
    rebuildAttrs();

    inheritManager.inheritAttributesFromModel(
      srcModel,
      attrPolicy,
      AttributeChangedCallback.nullCallback);
    LABEL = this.getNodeAttribute(labelAttr);

    // Connect model-view chain
    // srcModel.addModelListener( srcModelBreakageHandler );
    attrView.addModelListener(srcModelBreakageHandler);
    // to avoid redundant events
  }

  private void rebuildAttrs() {
    SlotInfo<Boolean> is = attrView.getNodeAttribute(IS_CRITERIA);
    List<String> names = new ArrayList<String>();

    // populate names from attrView
    Iterator<IRNode> enm = attrView.elements();
    while (enm.hasNext()) {
      IRNode n = enm.next();
      if ((n.getSlotValue(is)).booleanValue()) {
        names.add(attrView.getName(n));
      }
    }
    attrNames = names.toArray(new String[names.size()]);
    attrs = new SlotInfo[attrNames.length];
    for (int i = 0; i < attrs.length; i++) {
      attrs[i] = srcModel.getNodeAttribute(attrNames[i]);
    }
  }

  @Override
  protected void rebuildModel(List events) throws InterruptedException {
    // TODO Auto-generated method stub
    LOG.info("Rebuilding " + this.getName());

    synchronized (structLock) {
      try {

        rebuildAttrs();

        // Clear the existing tree model...
        forestModCore.clearForest();

        LOG.fine("Creating root");
        final Iterator nodes =
          (srcModel != null)
            ? srcModel.getNodes()
            : new EmptyIterator<Object>();

        if (attrs.length == 0) {
          LOG.fine("Handling special case of no categorization");
          while (nodes.hasNext()) {
            IRNode n = (IRNode) nodes.next();
            forestModCore.initNode(n);
            forestModCore.addRoot(n);
          }
        } else {
          final Iterator n2 =
            (srcModel != null)
              ? srcModel.getNodes()
              : new EmptyIterator<Object>();
          while (n2.hasNext()) {
            forestModCore.initNode((IRNode) n2.next());
          }

          final IRNode root = new MarkedIRNode(getName() + " root");
          forestModCore.initNode(root);
          forestModCore.addRoot(root);
          root.setSlotValue(LABEL, "root");

          LOG.fine("Categorizing children");
          categorizeChildren(0, root, nodes);
        }
      } catch (InterruptedException e) {
        throw e;
      } catch (Throwable t) {
        LOG.log(Level.SEVERE, "Got error", t);
      }
    }
    LOG.info("Finished rebuilding SFV");

    // Break our views
    modelCore.fireModelEvent(new ModelEvent(this));
  }

  /**
	 * Given the attribute for the level of the tree, create a intermediate nodes
	 * for each distinct value (hanging off of the "root") and hang nodes w/ that
	 * value for the attribute
	 * 
	 * @param level
	 *          The level in the tree
	 * @param root
	 *          The node to add intermediate nodes to
	 * @param nodes
	 *          The nodes to categorize
	 */
  private void categorizeChildren(int level, IRNode root, Iterator nodes)
    throws InterruptedException {
    if (level >= attrs.length || !nodes.hasNext()) {
      return;
    }
    LOG.fine(
      "Categorizing level " + level + " for " + root.getSlotValue(LABEL));
    final SlotInfo attr = attrs[level];
    final Map<Object,IRNode> m = getComparatorMap(attrNames[level]);
    int i = 0;

    while (nodes.hasNext()) {
      final IRNode n = (IRNode) nodes.next();
      if (!srcModel.isPresent(n)) {
        LOG.severe("Ignored: not present in src model = " + n);
        continue;
      }
      final Object val = n.valueExists(attr) ? n.getSlotValue(attr) : null;
      IRNode parent = m.get(val);

      if (parent == null) {
        parent = new PlainIRNode();
        forestModCore.initNode(parent);
        forestModCore.appendSubtree(root, parent);
        parent.setSlotValue(LABEL, val == null ? "(null)" : val);
        m.put(val, parent);
        LOG.fine("Created node for " + val);
      }
      LOG.fine("Adding node -- " + val);
      forestModCore.appendSubtree(parent, n);
      i++;
    }

    if (i == 1) {
      return; // No more splitting to do
    }
    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;

    Iterator it = m.values().iterator();
    while (it.hasNext()) {
      final IRNode n = (IRNode) it.next();
      categorizeChildren(level + 1, n, forestModCore.children(n));
    }
  }

  private Map<Object,IRNode> getComparatorMap(String attr) {
    Comparator<Object> c = comparators.get(attr);
    if (c != null) {
      return new TreeMap<Object,IRNode>(c);
    }
    return new TreeMap<Object,IRNode>(DefaultComparator.prototype());
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.edu.cmu.cs.fluid.mvc.tree.SynthesizedForestView#addComparator(java.lang.String,
	 *      java.util.Comparator)
	 */
  public void addComparator(String attr, Comparator<Object> c) {
    comparators.put(attr, c);
  }

  private static class DefaultComparator<T> implements Comparator<T> {
    /*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
    public int compare(T o1, T o2) {
      return o1.equals(o2) ? 0 : 1;
    }

    static Comparator prototype = new DefaultComparator();
    
    @SuppressWarnings("unchecked")
    static <T> Comparator<T> prototype() { return prototype; }
  }
}
