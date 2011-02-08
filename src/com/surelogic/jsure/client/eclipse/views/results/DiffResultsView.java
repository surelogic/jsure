package com.surelogic.jsure.client.eclipse.views.results;

public class DiffResultsView extends ResultsView {
  @Override
  protected IResultsViewContentProvider makeContentProvider() {
    return new DiffResultsViewContentProvider();
  }
}
