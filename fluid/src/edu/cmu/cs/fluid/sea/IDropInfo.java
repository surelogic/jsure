package edu.cmu.cs.fluid.sea;

import java.util.*;

import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Category;

public interface IDropInfo {
  String getAttribute(String key);

  String getEntityName();

  <T> T getAdapter(Class<T> type);

  String getType();

  /**
   * receiver <tt>instanceof</tt> type.
   */
  boolean instanceOf(Class<?> type);

  String getMessage();

  boolean isValid();

  boolean hasMatchingDeponents(IDropPredicate p);

  Set<? extends IDropInfo> getMatchingDeponents(IDropPredicate p);

  boolean hasMatchingDependents(IDropPredicate p);

  Set<? extends IDropInfo> getMatchingDependents(IDropPredicate p);

  boolean requestTopLevel();

  int count();

  ISrcRef getSrcRef();

  Category getCategory();

  void setCategory(Category c);

  Collection<ISupportingInformation> getSupportingInformation();

  Collection<? extends IProposedPromiseDropInfo> getProposals();

  void snapshotAttrs(XMLCreator.Builder s);

  Long getTreeHash();

  Long getContextHash();
}
