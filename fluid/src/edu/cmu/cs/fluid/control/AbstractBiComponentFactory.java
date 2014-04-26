package edu.cmu.cs.fluid.control;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * A class for creating bicomponents and keeping a simple cache.
 */
public abstract class AbstractBiComponentFactory implements BiComponentFactory {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("FLUID.control");

	public AbstractBiComponentFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Component getComponent(IRNode node) {
		return getBiComponent(node,false);
	}
	
	public Component getComponent(IRNode node, boolean quiet) {
		return getBiComponent(node,quiet);
	}
	
	/**
	 * Create a traditional component for the node
	 * @param node IR node to create a CFG component for, must not be null
	 * @return Component for this node
	 */
	protected abstract Component getUnregisteredComponent(IRNode node, boolean quiet);
	
	@Override
	public BiComponent getBiComponent(IRNode node, boolean quiet) {
		Component comp = this.getUnregisteredComponent(node, quiet);
		if (comp == null) return null;
		if (comp.factory != null) {
			LOG.warning("'unregistered' component is actually registered: " + JJNode.toString(node));
		}
		BiComponent bic = new BiComponent(this,node);
		bic.assumeIdentity(comp,quiet);
		return registerBiComponent(node,bic);
	}

	private ConcurrentMap<IRNode,BiComponent> registry = new ConcurrentHashMap<IRNode,BiComponent>();
	
	/**
	 * Register this bi-component and return it, or the bi-component previously
	 * registered for this node.
	 * @param node node to register bi-component for, must not be null
	 * @param bic bi-component to regsiter for this node, must not be null
	 * @return
	 */
	protected BiComponent registerBiComponent(IRNode node, BiComponent bic) {
		assert node != null;
		assert bic != null;
		BiComponent old;
		if ((old = registry.putIfAbsent(node, bic)) != null) {
			return old;
		}
		return bic;
	}
	
	@Override
	public void clean() {
		Iterator<Map.Entry<IRNode, BiComponent>> it = registry.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<IRNode, BiComponent> entry = it.next();
			if (entry.getKey().identity() == IRNode.destroyedNode) {
				it.remove();
			}
		}
	}
	
	@Override
	public void clear() {
		registry.clear();
	}
}
