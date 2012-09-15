package com.surelogic.annotation.bind;

import java.util.logging.Level;

import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.AbstractSuperTypeSearchStrategy;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 */
public class FindLockModelStrategy extends
		AbstractSuperTypeSearchStrategy<LockModel> {
	public FindLockModelStrategy(IBinder bind, String name) {
		super(bind, "lock", name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitClass_internal(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public void visitClass_internal(IRNode type) {
		if (type == null) {
			LOG.severe("Type was null, while looking for a lock " + name);
			searchAfterLastType = false;
		} else {
			final boolean finerIsLoggable = LOG.isLoggable(Level.FINER);
			if (finerIsLoggable) {
				LOG.finer("Looking for lock " + name + " in "
						+ DebugUnparser.toString(type));
			}

			LockModel reg = null;
			for (LockModel r : LockRules.getModels(type)) {
				if (finerIsLoggable) {
					LOG.finer("Looking at " + r.getQualifiedName());
				}
				if (r.getSimpleName().equals(name)) {
					reg = r;
					break;
				}
			}
			if (reg == null && finerIsLoggable) {
				// FIX add code for JCiP models?
				if (finerIsLoggable) {
					LOG.finer("Couldn't find " + name);
				}
			}
			searchAfterLastType = (reg == null);

			if (result == null) {
				// Nothing found yet
				result = reg;
			} else if (!result.equals(reg)) {
				// Found more than one distinct lock
				LOG.warning("Found a duplicate lock: " + reg.getMessage());
			}
		}
	}
}
