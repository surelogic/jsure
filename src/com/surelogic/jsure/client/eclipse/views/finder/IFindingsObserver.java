package com.surelogic.jsure.client.eclipse.views.finder;

public interface IFindingsObserver {
  void findingsLimited(boolean isLimited);
  void findingsDisposed();
}
