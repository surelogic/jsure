package com.surelogic.jsure.client.eclipse.model.selection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.common.i18n.I18N;

public final class SelectionManager {

  private static final SelectionManager INSTANCE = new SelectionManager();

  public static SelectionManager getInstance() {
    return INSTANCE;
  }

  private SelectionManager() {
    // singleton
  }

  /**
   * This is a bit of a hack to meet Ready for Rational requirement 3.5.19
   * (persistence of view state). We make the view state use a special key that
   * we deem unlikely to be used by the user.
   * <p>
   * Special methods use this key
   * <p>
   * The value is set right before Eclipse exit and then read (once) and removed
   * before the user would ever see it (unless they looked into the save file).
   */
  private static final String VIEW_STATE_KEY = "persist-view-state-of-findings-quick-search";

  public Selection construct() {
    final Selection result = new Selection(this);
    return result;
  }

  private final Map<String, Selection> f_nameToSelection = new HashMap<>();

  /**
   * Saves a copy of the passed selection. If a previous saved selection with
   * the passed name existed, it is overwritten.
   * 
   * @param name
   *          a name for the saved selection.
   * @param selection
   *          the selection to save a copy or.
   */
  public void saveSelection(String name, Selection selection) {
    if (name == null)
      throw new IllegalArgumentException(I18N.err(44, "name"));
    if (selection == null)
      throw new IllegalArgumentException(I18N.err(44, "selection"));
    Selection copy = new Selection(selection);
    f_nameToSelection.put(name, copy);
    notifySavedSelectionsChanged();
  }

  public void saveViewState(Selection selection) {
    saveSelection(VIEW_STATE_KEY, selection);
  }

  public void removeSavedSelection(String name) {
    if (f_nameToSelection.remove(name) != null) {
      notifySavedSelectionsChanged();
    }
  }

  public void removeViewState() {
    removeSavedSelection(VIEW_STATE_KEY);
  }

  /**
   * Returns a copy of the saved selection with the passed name, or
   * <code>null</code> if no such saved selection exists.
   * 
   * @param name
   *          a saved selection name.
   * @return a copy of the saved selection with the passed name, or
   *         <code>null</code> if no such saved selection exists.
   */
  public Selection getSavedSelection(String name) {
    Selection result = f_nameToSelection.get(name);
    if (result != null)
      result = new Selection(result);
    return result;
  }

  public Selection getViewState() {
    return getSavedSelection(VIEW_STATE_KEY);
  }

  public List<String> getSavedSelectionNames() {
    List<String> result = new ArrayList<>(f_nameToSelection.keySet());
    Collections.sort(result);
    return result;
  }

  public boolean isEmpty() {
    return f_nameToSelection.isEmpty();
  }

  private final List<ISelectionManagerObserver> f_observers = new CopyOnWriteArrayList<>();

  public void addObserver(ISelectionManagerObserver o) {
    if (o == null)
      return;
    /*
     * No lock needed because we are using a util.concurrent collection.
     */
    f_observers.add(o);
  }

  public void removeObserver(ISelectionManagerObserver o) {
    /*
     * No lock needed because we are using a util.concurrent collection.
     */
    f_observers.remove(o);
  }

  /**
   * Do not call this method holding a lock on <code>this</code>. Deadlock could
   * occur as we are invoking an alien method.
   */
  private void notifySavedSelectionsChanged() {
    for (ISelectionManagerObserver o : f_observers)
      o.savedSelectionsChanged(this);
  }

  public void save(File file) {
    SelectionPersistence.save(this, file);
  }

  public void load(File file) {
    SelectionPersistence.load(this, file);
    notifySavedSelectionsChanged();
  }
}
