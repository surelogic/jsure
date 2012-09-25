package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.*;

import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.dropsea.*;
import com.surelogic.dropsea.ir.*;
import com.surelogic.dropsea.ir.drops.ScopedPromiseDrop;
import com.surelogic.dropsea.irfree.DropTypeUtility;

/**
 * Temporary to support SeaSummary
 * 
 * @author Edwin
 */
@Deprecated
public class DropFactory {
	public static IDrop create(Entity drop, Entity ref) {
		IRFreeDrop d = (IRFreeDrop) create(drop);
		d.setSrcRef(IRFreeDrop.makeSrcRef(new SourceRef(ref)));
		return d;
	}
	
	// Modified from SeaEntity.makeEntity()
	public static IDrop create(Entity entity) {
		final String type = entity.getAttribute(TYPE_ATTR);
		if (type != null) {
			final Class<?> thisType = DropTypeUtility.findType(type);
			if (thisType != null) {
				if (ProposedPromiseDrop.class.isAssignableFrom(thisType)) {
					return new IRFreeProposedPromiseDrop(entity, thisType);
				} else if (ScopedPromiseDrop.class.isAssignableFrom(thisType)) {
					return new IRFreeScopedPromiseDrop(entity, thisType);
				} else if (PromiseDrop.class.isAssignableFrom(thisType)) {
					return new IRFreePromiseDrop(entity, thisType);
				} else if (HintDrop.class.isAssignableFrom(thisType)) {
					/*
					 * The old scheme used WarningDrop as a subtype of InfoDrop. The new
					 * scheme just has AnalysisHintDrop with an attribute, hint type. We
					 * need to set the hint type attribute correctly if we are dealing
					 * with an old scan. No extra work for InfoDrop (default is
					 * SUGGESTION), but we need to explicitly set the hint type to WARNING
					 * if an old WarningDrop.
					 */
					if (type.endsWith("WarningDrop"))
						return new IRFreeHintDrop(entity, thisType, IHintDrop.HintType.WARNING);
					else
						return new IRFreeHintDrop(entity, thisType);
				} else if (ResultDrop.class.isAssignableFrom(thisType)) {
					return new IRFreeResultDrop(entity, thisType);
				} else if (ResultFolderDrop.class.isAssignableFrom(thisType)) {
					return new IRFreeResultFolderDrop(entity, thisType);
				} else if (ModelingProblemDrop.class.isAssignableFrom(thisType)) {
					return new IRFreeModelingProblemDrop(entity, thisType);
				}
			}
		}
		return null;
	}
}
