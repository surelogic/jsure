package edu.cmu.cs.fluid.eclipse;

import java.util.*;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;

@Deprecated
public class QueuingSrcNotifyListener implements ISrcAdapterNotify {
	private Map<CodeInfo, CodeInfo> newITypes = new HashMap<CodeInfo, CodeInfo>();
	private boolean iterating = false;
	final String label;

	public QueuingSrcNotifyListener() {
		this("Unknown");
	}

	public QueuingSrcNotifyListener(String label) {
		this.label = label;
	}

	public void clear() {
		// System.err.println("Clearing listener");
		newITypes.clear();
		iterating = false;
	}

	@Override
	public void run(CodeInfo info) {
		if (iterating) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Trying to add while iterating", new Error());
		} else {
			// System.err.println("Adding "+info.getFileName());
		}
		/*
		 * if (info.getNode().identity() == IRNode.destroyedNode) {
		 * System.out.println("Getting info for destroyed node"); }
		 */
		newITypes.put(info, info);
	}

	@Override
	public void gotNewPackage(IRNode pkg, String name) {
		// do nothing
	}

	public Iterable<CodeInfo> infos() {
		iterating = true;
		return newITypes.keySet();
	}

	public void copyTo(Collection<CodeInfo> c) {
		c.addAll(newITypes.keySet());
	}

	public CodeInfo find(String name) {
		for (CodeInfo info : newITypes.keySet()) {
			if (info.getFileName().equals(name)) {
				return info;
			}
		}
		return null;
		/*
		 * CodeInfo info = new CodeInfo(null, null, name, null); return
		 * (CodeInfo) newITypes.remove(info);
		 */
	}

	/*
	 * public CodeInfo remove(CodeInfo info) { return (CodeInfo)
	 * newITypes.remove(info); }
	 */
	public CodeInfo find(IRNode cu) {
		CodeInfo info = CodeInfo.createMatchTemplate(cu, null);
		return newITypes.get(info);
	}
}
