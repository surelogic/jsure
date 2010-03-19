package edu.cmu.cs.fluid.java.adapter;

import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.parse.JJNode;

public class AdapterUtil {
	/**
	 * Logger for this class
	 */
	static final Logger LOG = SLLogger.getLogger("fluid.adapter");
	
	/**
	 * @return true if destroyed
	 */
	public static boolean destroyOldCU(IRNode cu) {
		/*
		List<IRNode> temp = new ArrayList<IRNode>();
		for(IRNode n : JavaPromise.bottomUp(cu)) {
			temp.add(n);
		}
		 */
		if (JJNode.versioningIsOn) {
			return false;
		}

		final IRRegion region = IRRegion.getOwnerOrNull(cu);
		if (region != null) {
			region.destroy();
		} else {
			boolean destroyed = false;
			final Iterator<IRNode> enm = JavaPromise.bottomUp(cu);
			while (enm.hasNext()) {
				IRNode n = enm.next();
				destroyNode(n);
				if (!destroyed) {
					destroyed = true;
				}
			}
		}
		/*
		for(IRNode n : temp) {
			if (n.identity() != IRNode.destroyedNode) {
				System.out.println("Not destroyed: "+JJNode.tree.getOperator(n)+" in "+handle);
			}
		}
		 */
		return true;
	}
	
	private static IPromiseProcessor destroyer = new AbstractNodePromiseProcessor() {
		public String getIdentifier() {
			return "Promise destroyer";
		}

		@Override
		protected void process(IRNode n) {
			destroyNode(n);
		}
	};

	/**
	 * Destroy an IRNode and all the promises on it
	 * 
	 * @param n
	 */
	@SuppressWarnings("deprecation")
	private static void destroyNode(IRNode n) {
		PromiseFramework.getInstance().processPromises(n, destroyer);
		if (n != null) {
			n.destroy();
			if (n.identity() != IRNode.destroyedNode) {
				throw new IllegalArgumentException("Could not destroy " + n);
			}
		} else {
			LOG.warning("Trying to destroy a null node");
		}
	}
}
