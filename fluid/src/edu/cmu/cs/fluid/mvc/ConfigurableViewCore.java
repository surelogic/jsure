/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ConfigurableViewCore.java,v 1.22 2007/07/10 22:16:30 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.*;

/**
 * Core implemenation of the <code>ConfigurableView</code> interface.
 * <p>Adds the node-level attribute {@link ConfigurableView#PROXY_NODE},
 * {@link ConfigurableView#IS_HIDDEN}, and {@link ConfigurableView#IS_PROXY},
 *
 * <p>Currently proxy nodes are always freshly generated.  This creates
 * a lot of "junk" in the IR, but it is the easiest way to implement
 * things.  It avoid the problem of having of reused proxy nodes
 * reusing old attribute values as well.
 *
 * <p>Users of this class must remember to clear the isProxyFlag
 * when proxy nodes are removed.
 *
 * @author Aaron Greenhouse
 */
public final class ConfigurableViewCore extends AbstractCore {
  //===========================================================
  //== Fields
  //===========================================================

  /** Storage for the {@link ConfigurableView#IS_PROXY} attribute. */
  private final SlotInfo<Boolean> isProxy;

  /** Storage for the {@link ConfigurableView#PROXY_NODE} attribute */
  private final SlotInfo<IRNode> proxyNode;

  /** SlotInfo for {@link ConfigurableView#IS_HIDDEN} attribute */
  private final SlotInfo<Boolean> hidden;

  /**
   * Map from attribute name to the SlotInfo storing the proxy node values
   * for that attribute.  Originates from the {@link ProxyNodeSupport#PROXY_MAP}
   * property of the attribute manager.
   */
  private final Map proxyAttributes;

  //===========================================================
  //== Constructor
  //===========================================================

  protected ConfigurableViewCore(
    final String name,
    final Model model,
    final Object lock,
    final Model srcModel,
    final AttributeManager manager,
    final AttributeInheritanceManager inheritManager,
    final AttributeChangedCallback cb)
    throws SlotAlreadyRegisteredException {
    super(model, lock, manager);
    try {
      final Object obj = inheritManager.getProperty(ProxyNodeSupport.PROXY_MAP);
      proxyAttributes = (Map) obj;
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "ConfigurableViewCore must have an attribute inheritance manager "
          + "that understands the ProxyNodeSupport.PROXY_MAP "
          + " property.");
    }

    final SlotFactory sf = SimpleSlotFactory.prototype;
    isProxy =
      sf.newAttribute(
        name + "-" + ConfigurableView.IS_PROXY,
        IRBooleanType.prototype,
        Boolean.FALSE);
    attrManager.addNodeAttribute(
      ConfigurableView.IS_PROXY,
      Model.STRUCTURAL,
      isProxy);

    proxyNode =
      sf.newAttribute(
        name + "-" + ConfigurableView.PROXY_NODE,
        IRNodeType.prototype,
        null);
    attrManager.addNodeAttribute(
      ConfigurableView.PROXY_NODE,
      Model.STRUCTURAL,
      proxyNode);

    hidden =
      sf.newAttribute(
        name + "-" + ConfigurableView.IS_HIDDEN,
        IRBooleanType.prototype,
        Boolean.FALSE);
    attrManager.addNodeAttribute(
      ConfigurableView.IS_HIDDEN,
      Model.STRUCTURAL,
      true,
      new Model[] { srcModel },
      hidden,
      cb);
  }

  public void setSourceModels(
    final Model model,
    final ViewCore viewCore,
    final VisibilityModel visModel) {
    // Check for consistency
    final ComponentSlot<Model> visOf =
      visModel.getCompAttribute(VisibilityModel.VISIBILITY_OF);
    final Model m = visOf.getValue();
    if (m != model) {
      throw new IllegalArgumentException(
        "Source Model \""
          + model.getName()
          + "\" is not the source for Source Visibility Model \""
          + visModel.getName()
          + "\"");
    }

    // Initialize Model-level attributes
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence(2);
    srcModels.setElementAt(model, 0);
    srcModels.setElementAt(visModel, 1);
    viewCore.setSourceModels(srcModels);
  }

  public void setProxyNodeAttributes(
    final IRNode proxyNode,
    final AVPair[] pairs)
    throws UnknownAttributeException {
    for (int i = 0; i < pairs.length; i++) {
      final String attr = pairs[i].getAttribute();
      final SlotInfo si = getProxyValuesForAttribute(attr);
      if (si == null) {
        throw new UnknownAttributeException(
          "Attribute \""
            + attr
            + "\" of model \""
            + partOf.getName()
            + "\" does not support proxy nodes.");
      } else {
        proxyNode.setSlotValue(si, pairs[i].getValue());
      }
    }
  }

  //===========================================================
  //== Convienence Methods
  //===========================================================

  public SlotInfo getProxyValuesForAttribute(final String attr) {
    return (SlotInfo) proxyAttributes.get(attr);
  }

  public boolean isProxyNode(final IRNode node) {
    return ( node.getSlotValue(isProxy)).booleanValue();
  }

  public IRNode getProxyNode(final IRNode node) {
    return node.getSlotValue(proxyNode);
  }

  public void setProxyNode(final IRNode node, final IRNode proxy) {
    node.setSlotValue(proxyNode, proxy);
    if (proxy != null) {
      proxy.setSlotValue(isProxy, Boolean.TRUE);
    }
  }

  /** Mark the given node as not being a proxy node. */
  public void clearProxyStatus(final IRNode node) {
    node.setSlotValue(isProxy, Boolean.FALSE);
  }

  /** Remove the proxy node (if it exists) from a given node. */
  public void removeProxyFrom(final IRNode node) {
    final IRNode proxy = getProxyNode(node);
    if (proxy != null) {
      setProxyNode(node, null);
      clearProxyStatus(proxy);
      // bagOfProxies.add( proxy );
    }
  }

  /**
   * Make sure that the given node has a proxy node.  The returned
   * node will have the value of the IS_PROXY attribute set to 
   * <code>true</code>.
   * @return The proxy node associated with <code>node</code>.
   */
  public IRNode generateProxyFor(final IRNode node) {
    IRNode proxy = getProxyNode(node);
    if (proxy == null) {
      proxy = new PlainIRNode();
      setProxyNode(node, proxy);
    } else {
      proxy.setSlotValue(isProxy, Boolean.TRUE);
    }
    return proxy;
  }

  //===========================================================
  //== Node visibility methods
  //===========================================================

  /**
   * Query if a node is hidden.
   * This must always return the inverse of {@link #isShown}.
   */
  public boolean isHidden(final IRNode node) {
    final Boolean b = node.getSlotValue(hidden);
    return b.booleanValue();
  }

  /**
   * Query if a node is shown.
   * This must always return the inverse of {@link #isHidden}.
   */
  public boolean isShown(final IRNode node) {
    return !isHidden(node);
  }

  /**
   * Set the hidden status of a node.
   * @param node The IRNode whose status is to be set.
   * @param isHidden <code>true</code> if the node should be hidden;
   * <code>false</code> if the node should be shown.
   */
  public void setHidden(final IRNode node, final boolean isHidden) {
    node.setSlotValue(hidden, isHidden ? Boolean.TRUE : Boolean.FALSE);
  }

  /**
   * Set the hidden status for all the nodes.
   * @param isHidden <code>true</code> if the nodes should be hidden;
   * <code>false</code> if the nodes should be shown.
   */
  public void setHiddenForAllNodes(final Model model, final boolean isHidden) {
    final Iterator nodes = model.getNodes();
    while (nodes.hasNext()) {
      final IRNode node = (IRNode) nodes.next();
      setHidden(node, isHidden);
    }
  }

  //===========================================================
  //== Command Related stuff
  //===========================================================

  public void setNodeHidden(ConfigurableView cv, Set n, boolean hidden) {
    Iterator i = n.iterator();
    while (i.hasNext()) {
      cv.setHidden((IRNode) i.next(), hidden);
    }
  }

  public boolean existsEllipsisOrNot(
    ConfigurableView cv,
    Iterator it,
    boolean isEllipsisVal) {
    while (it.hasNext()) {
      IRNode n = (IRNode) it.next();
      if (cv.isEllipsis(n) == isEllipsisVal) {
        return true;
      }
    }
    return false;
  }

  public Set<IRNode> expandEllipses(ConfigurableView cv, Set n) {
    Set<IRNode> n2 = new HashSet<IRNode>();
    Iterator it = n.iterator();
    while (it.hasNext()) {
      IRNode i = (IRNode) it.next();
      if (!cv.isPresent(i)) {
        continue;
      }
      if (cv.isEllipsis(i)) {
        n2.addAll(cv.getEllidedNodes(i));
      } else {
        n2.add(i);
      }
    }
    return n2;
  }

  //===========================================================
  //== ViewCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory {
    public ConfigurableViewCore create(
      String name,
      Model model,
      Object structLock,
      Model src,
      AttributeManager manager,
      AttributeInheritanceManager inheritManager,
      AttributeChangedCallback cb)
      throws SlotAlreadyRegisteredException;
  }

  private static class StandardFactory implements Factory {
    @Override
    public ConfigurableViewCore create(
      final String name,
      final Model model,
      final Object structLock,
      final Model src,
      final AttributeManager manager,
      final AttributeInheritanceManager inheritManager,
      final AttributeChangedCallback cb)
      throws SlotAlreadyRegisteredException {
      return new ConfigurableViewCore(
        name,
        model,
        structLock,
        src,
        manager,
        inheritManager,
        cb);
    }
  }

  public static final Factory standardFactory = new StandardFactory();
}
