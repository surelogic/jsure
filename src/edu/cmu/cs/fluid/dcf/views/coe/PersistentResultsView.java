package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

public class PersistentResultsView extends ResultsView {
  /**
   * Mainly used to store ProposedPromiseDrops?
   * (can this really operate w/o the IRNodes?)
   */
  final Sea sea = new Sea();

  Collection<Info> dropInfo = null;
  
  @Override
  protected IResultsViewContentProvider makeContentProvider() {
    return new GenericResultsViewContentProvider<Info,Content>(sea) {		
		@Override
		protected boolean dropsExist(Class<? extends Drop> type) {
			for(Info i : dropInfo) {
				if (i.isInstance(type)) {
					return true;
				}
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <R extends IDropInfo> 
		Collection<R> getDropsOfType(Class<? extends Drop> type, Class<R> rType) {
			List<R> rv = new ArrayList<R>();
			for(Info i : dropInfo) {
				if (i.isInstance(type)) {
					rv.add((R) i);
				}
			}
			return rv;
		}
		
		@Override
		protected Content makeContent(String msg) {
			return new Content(msg, Collections.<Content>emptyList(), null);
		}
		
		@Override
		protected Content makeContent(String msg, Collection<Content> contentRoot) {
			return new Content(msg, contentRoot, null);
		}

		@Override
		protected Content makeContent(String msg, Info drop) {
			return new Content(msg, Collections.<Content>emptyList(), drop);
		}

		@Override
		protected Content makeContent(String msg, ISrcRef ref) {
			return new Content(msg, ref);
		}
    };
  }
  
  static class Content extends AbstractContent<Info,Content>{
	Content(String msg, Collection<Content> content, Info drop) {
		super(msg, content, drop);
	}	  
	Content(String msg, ISrcRef ref) {
		super(msg, ref);
	}
  }
}
