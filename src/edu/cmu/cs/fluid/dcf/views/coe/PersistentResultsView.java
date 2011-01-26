package edu.cmu.cs.fluid.dcf.views.coe;

import java.io.*;
import java.util.*;

import org.eclipse.ui.IMemento;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

public class PersistentResultsView extends ResultsView {
  private static final String VIEW_STATE = "view.state";
  private static final boolean useXML = SeaSnapshot.useFullType || AbstractWholeIRAnalysis.useDependencies;
  
  /**
   * TODO Mainly used to store ProposedPromiseDrops?
   * (can this really operate w/o the IRNodes?)
   */
  final Sea sea = Sea.getDefault();//new Sea();
  
  final File viewState;
	
  public PersistentResultsView() {
	  File viewState = null;
	  if (useXML) try {
		  final File jsureData = PreferenceConstants.getJSureDataDirectory();
		  if (jsureData != null) {
			  viewState = new File(jsureData, VIEW_STATE+".xml");
		  } else {
			  viewState = File.createTempFile(VIEW_STATE, ".xml");
		  }   
		  //System.out.println("Using location: "+location);
	  } catch(IOException e) {
		  // Nothing to do
	  }
	  this.viewState = viewState;
  }
  
  @Override
  protected void subscribe() {
	  PersistentDropInfo.getInstance().addListener(this);
  }
  
  /*
  @Override
  public void analysisStarting() {
	  if (location == null || !location.exists()) {
		  super.analysisStarting();
	  }
	  // Ignore this, so we can continue to look at the old results
  }
  
  @Override 
  public void seaChanged() {
	  if (location == null || !location.exists()) {
		  super.seaChanged();
	  } else { 
		  // load it up
		  finishCreatePartControl();
	  }
  }
  */
  
  @Override
  protected void finishCreatePartControl() {
	  if (PersistentDropInfo.getInstance().load()) {
		  // TODO restore viewer state?
		  provider.buildModelOfDropSea_internal();
		  setViewerVisibility(true);
		  System.out.println("Loaded snapshot");

		  // Running too early?
		  if (viewState != null && viewState.exists()) {
			  f_viewerbook.getDisplay().asyncExec(new Runnable() {
				  public void run() {
					  try {
						  //viewer.refresh();
						  loadViewState(viewState);
					  } catch (IOException e) {
						  e.printStackTrace();
					  }
				  }
			  });
		  }
	  }	  
  }
  
  @Override
  public void saveState(IMemento memento) {
	  try {
		saveViewState(viewState);
	} catch (IOException e) {
		e.printStackTrace();
	}
  }
  
  GenericResultsViewContentProvider<Info,Content> provider;
  
  @Override
  protected IResultsViewContentProvider makeContentProvider() {
	  if (!useXML) {
		  return new ResultsViewContentProvider();
	  }
	  return new GenericResultsViewContentProvider<Info,Content>(sea) {		
		{
			provider = this;
		}
		@Override
    	public IResultsViewContentProvider buildModelOfDropSea() {
			if (useXML) {
				try {
					saveViewState(viewState);
				} catch (IOException e) {
					e.printStackTrace();
				}
				PersistentDropInfo.getInstance().load();
				
				try {
					return super.buildModelOfDropSea_internal();
				} finally {
					f_viewerbook.getDisplay().asyncExec(new Runnable() {
						public void run() {
							restoreViewState();
						}
					});
				}
			} else {
				return super.buildModelOfDropSea();
			}
    	}
    	
		@Override
		protected boolean dropsExist(Class<? extends Drop> type) {
			if (!PersistentDropInfo.useInfo) {
				return false;
			}
			return PersistentDropInfo.getInstance().dropsExist(type);
		}

		@Override
		protected <R extends IDropInfo> 
		Collection<R> getDropsOfType(Class<? extends Drop> type, Class<R> rType) {
			if (!PersistentDropInfo.useInfo) {
				return Collections.emptyList();
			}
			return PersistentDropInfo.getInstance().getDropsOfType(type);
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
