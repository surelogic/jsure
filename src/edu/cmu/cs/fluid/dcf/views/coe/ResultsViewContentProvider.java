package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;
import java.util.logging.Level;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.xml.results.coe.CoE_Constants;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class ResultsViewContentProvider 
extends GenericResultsViewContentProvider<Drop,Content> {
	public ResultsViewContentProvider() {
		super(Sea.getDefault());
	}

	@Override
	protected boolean dropsExist(Class<? extends Drop> type) {
		return !Sea.getDefault().getDropsOfType(type).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <R extends IDropInfo>
	Collection<R> getDropsOfType(Class<? extends Drop> type, Class<R> rType) {
		return (Collection<R>) Sea.getDefault().getDropsOfType(type);
	}
	
	@Override
	protected Content makeContent(String msg) {
		return new Content(msg);
	}

	@Override
	protected Content makeContent(String msg, Collection<Content> contentRoot) {
		return new Content(msg, contentRoot);
	}

	@Override
	protected Content makeContent(String msg, Drop drop) {
		return new Content(msg, drop);
	}

	@Override
	protected Content makeContent(String msg, ISrcRef ref) {
		return new Content(msg, ref);
	}
}