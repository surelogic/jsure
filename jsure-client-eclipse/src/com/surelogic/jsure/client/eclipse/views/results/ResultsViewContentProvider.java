package com.surelogic.jsure.client.eclipse.views.results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.part.PageBook;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.IScopedPromiseDrop;
import com.surelogic.dropsea.ISupportingInformation;
import com.surelogic.dropsea.UiPlaceInASubFolder;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.util.ArrayUtil;

final class ResultsViewContentProvider implements ITreeContentProvider {
  private static final boolean allowDuplicateNodes = true;
  private static final Object[] noObjects = ArrayUtil.empty;

  private static Object[] m_root = noObjects;

  private static final Logger LOG = SLLogger.getLogger("ResultsViewContentProvider");

  private boolean m_showInferences = true;

  /**
   * @return Returns the showInferences.
   */
  boolean isShowInferences() {
    return m_showInferences;
  }

  /**
   * @param showInferences
   *          The showInferences to set.
   */
  void setShowInferences(boolean showInferences) {
    this.m_showInferences = showInferences;
  }

  ResultsViewContentProvider() {
  }

  public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    /*
     * This kills the contents
     */
  }

  public void dispose() {
    // nothing to do
  }

  public final Object[] getElements(Object parent) {
    synchronized (ResultsViewContentProvider.class) {
      return getElementsInternal();
    }
  }

  private Object[] getElementsInternal() {
    return (isShowInferences() ? m_root : ResultsViewContent.filterNonInfo(m_root));
  }

  public Object getParent(Object child) {
    ResultsViewContent c = (ResultsViewContent) child;
    return c.getParent();
  }

  public final Object[] getChildren(Object parent) {
    return getChildrenInternal(parent);
  }

  private Object[] getChildrenInternal(Object parent) {
    if (parent instanceof ResultsViewContent) {
      ResultsViewContent item = (ResultsViewContent) parent;
      return (isShowInferences() ? item.getChildren() : item.getNonInfoChildren());
    }
    return noObjects;
  }

  public boolean hasChildren(Object parent) {
    Object[] children = getChildren(parent);
    return (children == null ? false : children.length > 0);
  }

  /**
   * Map from a <i>drop</i> to a <i>viewer content object</i> used to ensure
   * building a model for the viewer doesn't go into infinite recursion.
   */
  private final Map<IDrop, ResultsViewContent> m_contentCache = new HashMap<IDrop, ResultsViewContent>();

  private ResultsViewContent putInContentCache(IDrop key, @NonNull ResultsViewContent value) {
    if (key == null)
      throw new IllegalArgumentException(I18N.err(44, " key"));
    if (value == null)
      throw new IllegalArgumentException(I18N.err(44, "value"));
    return m_contentCache.put(key, value);
  }

  private @Nullable
  ResultsViewContent getFromContentCache(IDrop key) {
    return m_contentCache.get(key);
  }

  /**
   * Encloses in {@link ResultsViewContent} items and adds each drop in
   * <code>dropsToAdd</code> to the mutable set of viewer content items passed
   * into this method.
   * 
   * @param mutableContentSet
   *          A parent {@link ResultsViewContent} object to add children to
   * @param dropsToAdd
   *          the set of drops to enclose and add to the content set
   */
  private void addDrops(ResultsViewContent mutableContentSet, Collection<? extends IDrop> dropsToAdd) {
    for (IDrop drop : dropsToAdd) {
      mutableContentSet.addChild(encloseDrop(drop, mutableContentSet));
    }
  }

  private ResultsViewContent makeContent(String msg) {
    return new ResultsViewContent(msg, Collections.<ResultsViewContent> emptyList(), null);
  }

  private ResultsViewContent makeContent(String msg, Collection<ResultsViewContent> contentRoot) {
    return new ResultsViewContent(msg, contentRoot, null);
  }

  private ResultsViewContent makeContent(String msg, IDrop drop) {
    return new ResultsViewContent(msg, Collections.<ResultsViewContent> emptyList(), drop);
  }

  private ResultsViewContent makeContent(String msg, ISrcRef ref) {
    return new ResultsViewContent(msg, ref);
  }

  ResultsViewContentProvider buildModelOfDropSea(final TreeViewer treeViewer, File f_viewStatePersistenceFile, PageBook f_viewerbook) {
    final TreeViewerUIState state = new TreeViewerUIState(treeViewer);
    try {
      state.saveToFile(f_viewStatePersistenceFile);
    } catch (IOException e) {
      SLLogger.getLogger().log(Level.WARNING,
          "Trouble when saving ResultsView UI state to " + f_viewStatePersistenceFile.getAbsolutePath(), e);
    }
    try {
      return buildModelOfDropSea_internal();
    } finally {
      f_viewerbook.getDisplay().asyncExec(new Runnable() {
        public void run() {
          state.restoreViewState(treeViewer);
        }
      });
    }
  }

  /**
   * Adds referenced supporting information about a drop to the mutable set of
   * viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          set of all {@link ResultsViewContent} items
   * @param about
   *          the {@link IDrop}to add supporting information about
   */
  private void addSupportingInformation(ResultsViewContent mutableContentSet, IDrop about) {
    Collection<ISupportingInformation> supportingInformation = about.getSupportingInformation();
    // Add directly
    for (ISupportingInformation si : supportingInformation) {
      ResultsViewContent informationItem = makeContent(si.getMessage(), si.getSrcRef());
      informationItem.setBaseImageName(CommonImages.IMG_INFO);
      mutableContentSet.addChild(informationItem);
    }
  }

  /**
   * Adds referenced proposed promises about a drop to the mutable set of viewer
   * content items passed into this method.
   * 
   * @param mutableContentSet
   *          set of all {@link ResultsViewContent} items
   * @param about
   *          the {@link IDrop}to add proposed promises about
   */
  private void addProposedPromises(ResultsViewContent mutableContentSet, IDrop about) {
    Collection<? extends IProposedPromiseDrop> proposals = about.getProposals();
    // Add directly
    for (IProposedPromiseDrop pp : proposals) {
      final ResultsViewContent proposalItem = makeContent("proposed promise: " + pp.getJavaAnnotation(), pp);
      proposalItem.setBaseImageName(CommonImages.IMG_ANNOTATION_PROPOSED);
      mutableContentSet.addChild(proposalItem);
    }
  }

  /**
   * Create a {@link ResultsViewContent} item for a drop-sea drop. This is only
   * done once, hence, the same Content item is returned if the same drop is
   * passed to this method.
   * 
   * @param drop
   *          the drop to enclose
   * @param parentOrNull
   *          the future parent {@link ResultsViewContent} of the
   *          {@link ResultsViewContent} under construction, or {@code null} if
   *          none (a the root).
   * @return the content item the viewer can use
   */
  private ResultsViewContent encloseDrop(IDrop drop, ResultsViewContent parentOrNull) {
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));

    ResultsViewContent result = getFromContentCache(drop);
    if (result != null) {
      // in cache
      return result;
    } else {

      // create & add to cache
      // MUST BE IMMEDIATE TO AVOID INFINITE RECURSION
      result = makeContent(drop.getMessage(), drop);
      putInContentCache(drop, result); // to avoid infinite recursion

      if (drop instanceof IPromiseDrop) {
        /*
         * PROMISE DROP
         */
        final IPromiseDrop promiseDrop = (IPromiseDrop) drop;

        // image
        int flags = 0; // assume no adornments
        if (promiseDrop.isIntendedToBeCheckedByAnalysis()) {
          flags |= (promiseDrop.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
          flags |= (promiseDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
          flags |= (promiseDrop.isCheckedByAnalysis() ? 0 : CoE_Constants.TRUSTED);
        }
        flags |= (promiseDrop.isAssumed() ? CoE_Constants.ASSUME : 0);
        flags |= (promiseDrop.isVirtual() ? CoE_Constants.VIRTUAL : 0);
        result.setImageFlags(flags);
        result.setBaseImageName(CommonImages.IMG_ANNOTATION);

        // children
        addSupportingInformation(result, promiseDrop);
        addDrops(result, promiseDrop.getDependentPromises());
        addDrops(result, promiseDrop.getCheckedBy());
        addProposedPromises(result, promiseDrop);
        addDrops(result, promiseDrop.getAnalysisHintsAbout());

      } else if (drop instanceof IResultDrop) {
        /*
         * RESULT DROP
         */
        final IResultDrop resultDrop = (IResultDrop) drop;

        // image
        int flags = 0; // assume no adornments
        if (resultDrop.hasTrusted()) {
          // only show reddot and proof status if this results has preconditions
          flags |= (resultDrop.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
          flags |= (resultDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
        }
        result.setImageFlags(flags);
        result.setBaseImageName(resultDrop.isConsistent() ? CommonImages.IMG_PLUS
            : resultDrop.isVouched() ? CommonImages.IMG_PLUS_VOUCH : resultDrop.isTimeout() ? CommonImages.IMG_TIMEOUT_X
                : CommonImages.IMG_RED_X);

        // children
        addSupportingInformation(result, resultDrop);
        addDrops(result, resultDrop.getTrusted());
        addProposedPromises(result, resultDrop);

      } else if (drop instanceof IResultFolderDrop) {
        /*
         * RESULT FOLDER DROP
         */
        final IResultFolderDrop folderDrop = (IResultFolderDrop) drop;

        // image
        int flags = 0; // assume no adornments
        flags |= (folderDrop.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
        flags |= (folderDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
        result.setImageFlags(flags);
        result.setBaseImageName(folderDrop.getLogicOperator() == IResultFolderDrop.LogicOperator.AND ? CommonImages.IMG_FOLDER
            : CommonImages.IMG_FOLDER_OR);

        // children
        addDrops(result, folderDrop.getTrusted());
        addProposedPromises(result, folderDrop);

      } else if (drop instanceof IAnalysisHintDrop) {
        /*
         * INFO DROP
         */
        final IAnalysisHintDrop infoDrop = (IAnalysisHintDrop) drop;

        // image
        result.setBaseImageName(infoDrop.getHintType() == IAnalysisHintDrop.HintType.WARNING ? CommonImages.IMG_WARNING
            : CommonImages.IMG_INFO);

        // children
        addSupportingInformation(result, infoDrop);
        addProposedPromises(result, infoDrop);

        result.f_isInfo = true;
        result.f_isInfoWarning = infoDrop.getHintType() == IAnalysisHintDrop.HintType.WARNING;
      } else {
        LOG.log(Level.SEVERE, "ResultsViewContentProvider.encloseDrop(Drop) passed an unknown drop type " + drop.getClass());
      }
      return result;
    }
  }

  /**
   * Adds categories to the graph of {@link ResultsViewContent} nodes rooted
   * contentRoot and returns the new set of root nodes.
   * 
   * @param contentRoot
   *          root of a graph of {@link ResultsViewContent} nodes
   * @return the new set of {@link ResultsViewContent} nodes (that should
   *         replace contentRoot)
   */
  private Collection<ResultsViewContent> categorize(Collection<ResultsViewContent> contentRoot) {
    // fake out the recursive function by pretending the root is a node
    ResultsViewContent root = makeContent("", contentRoot);
    categorizeRecursive(root, true, new HashSet<ResultsViewContent>(), new HashSet<ResultsViewContent>());
    return root.getChildrenAsCollection();
  }

  /**
   * Recursive method to categorize a graph of content nodes.
   * 
   * @param node
   *          the {@link ResultsViewContent} node to categorize
   * @param atRoot
   *          <code>true</code> if at the {@link ResultsViewContent} root,
   *          <code>false</code> otherwise (used to control categorization of
   *          promise drops)
   * @param existingCategoryFolderSet
   *          a running set tracking what folders we have created used to ensure
   *          we don't categorize things we have already done
   * @param level
   *          hack to avoid an infinite loop
   */
  private void categorizeRecursive(ResultsViewContent node, boolean atRoot, Set<ResultsViewContent> existingCategoryFolderSet,
      Set<ResultsViewContent> contentsOnPathToRoot) {
    Set<ResultsViewContent> categorizedChildren = new HashSet<ResultsViewContent>();
    Set<ResultsViewContent> toBeCategorized = new HashSet<ResultsViewContent>();
    Map<Category, ResultsViewContent> categoryToFolder = new HashMap<Category, ResultsViewContent>();
    for (ResultsViewContent item : node.getChildrenAsCollection()) {
      if (existingCategoryFolderSet.contains(item)) {
        /*
         * This is a previously created folder (went around the loop) so just
         * add it to the resulting content set. Do not add it to the worklist to
         * be categorized or an infinite loop will result.
         */
        categorizedChildren.add(item);
      } else {
        toBeCategorized.add(item);
        final IDrop info = item.getDropInfo();
        boolean dontCategorize = false;
        if (info != null) {
          if (info instanceof IPromiseDrop) {
            dontCategorize = !atRoot && !(info.instanceOfIRDropSea(UiPlaceInASubFolder.class));
          }
          // else if (info instanceof IAnalysisResultDrop) {
          // final IAnalysisResultDrop r = (IAnalysisResultDrop) info;
          // dontCategorize = r.isInResultFolder();
          // }
        }
        if (dontCategorize) {
          /*
           * Only categorize promise drops at the root level
           */
          categorizedChildren.add(item);
        } else {
          Category itemCategory = item.getCategory();
          if (itemCategory == null) {
            /*
             * Uncategorized so it doesn't need to be placed within a folder
             */
            categorizedChildren.add(item);
          } else {
            /*
             * Get the correct category folder (created it if needed) and add
             * this content item to it
             */
            ResultsViewContent categoryFolder = categoryToFolder.get(itemCategory);
            if (categoryFolder == null) {
              // create the category folder, save it in the map
              categoryFolder = makeContent(itemCategory.getMessage());
              categoryToFolder.put(itemCategory, categoryFolder);
              existingCategoryFolderSet.add(categoryFolder);
            }
            categoryFolder.addChild(item);
          }
        }
      }
    }

    /*
     * Finish up the category folders add add them to the result
     */
    for (Map.Entry<Category, ResultsViewContent> entry : categoryToFolder.entrySet()) {
      ResultsViewContent categoryFolder = entry.getValue();
      categoryFolder.freezeCount();

      // image (try to show proof status if it makes sense)
      Set<IProofDrop> proofDrops = new HashSet<IProofDrop>();
      Set<IAnalysisHintDrop> warningDrops = new HashSet<IAnalysisHintDrop>();
      Set<IAnalysisHintDrop> infoDrops = new HashSet<IAnalysisHintDrop>();

      for (ResultsViewContent item : categoryFolder.getChildrenAsCollection()) {
        final IDrop drop = item.getDropInfo();
        if (drop instanceof IProofDrop) {
          proofDrops.add((IProofDrop) item.getDropInfo());
        } else if (drop instanceof IAnalysisHintDrop) {
          IAnalysisHintDrop infoDrop = (IAnalysisHintDrop) drop;
          if (infoDrop.getHintType() == IAnalysisHintDrop.HintType.SUGGESTION)
            infoDrops.add(infoDrop);
          else
            warningDrops.add(infoDrop);
        }
      }
      if (proofDrops.isEmpty() && !infoDrops.isEmpty()) {
        categoryFolder.setBaseImageName(!warningDrops.isEmpty() ? CommonImages.IMG_WARNING : CommonImages.IMG_INFO);
        categoryFolder.f_isInfo = true;
      } else if (proofDrops.isEmpty() && infoDrops.isEmpty()) {
        categoryFolder.setBaseImageName(CommonImages.IMG_WARNING);
        categoryFolder.f_isPromiseWarning = true;
      } else {
        // set proof bits properly
        int flags = 0; // assume no adornments
        boolean choiceConsistent = true;
        boolean choiceUsesRedDot = false;
        // boolean localConsistent = true;
        for (IProofDrop proofDrop : proofDrops) {
          choiceConsistent &= proofDrop.provedConsistent();
          if (proofDrop.proofUsesRedDot())
            choiceUsesRedDot = true;
        }
        flags = (choiceUsesRedDot ? CoE_Constants.REDDOT : 0);
        flags |= (choiceConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
        categoryFolder.setImageFlags(flags);
        categoryFolder.setBaseImageName(CommonImages.IMG_FOLDER);
      }
      categorizedChildren.add(categoryFolder);
    }

    /*
     * Replace the children of the node parameter with the categorized children
     * created within this method
     */
    node.resetChildren(categorizedChildren);

    /*
     * Categorize the content items we encountered for the first time
     */
    for (Iterator<ResultsViewContent> k = toBeCategorized.iterator(); k.hasNext();) {
      ResultsViewContent item = k.next();
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!contentsOnPathToRoot.contains(item)) {
        // Changed to add/remove the item from the set
        contentsOnPathToRoot.add(item);
        categorizeRecursive(item, false, existingCategoryFolderSet, contentsOnPathToRoot);
        contentsOnPathToRoot.remove(item);
      }
    }
  }

  /**
   * Creates folders for the package and type a result is within.
   * 
   * @param contentRoot
   *          root of a graph of {@link ResultsViewContent} nodes
   */
  private Collection<ResultsViewContent> packageTypeFolderize(Collection<ResultsViewContent> contentRoot) {
    // fake out the recursive function by pretending the root is node
    ResultsViewContent root = makeContent("", contentRoot);
    // packageTypeFolderizeRecursive(root, true, new
    // HashSet<ResultsViewContent>(), new HashSet<ResultsViewContent>());
    return root.getChildrenAsCollection();
  }

  private void propagateWarningDecorators(Collection<ResultsViewContent> contentRoot) {
    // fake out the recursive function by pretending the root is a node
    ResultsViewContent root = makeContent("", contentRoot);
    nodeNeedsWarningDecorator(root, new HashSet<ResultsViewContent>());
  }

  /**
   * Only called by nodeNeedsWarningDecorator and itself
   * 
   * Changed to use {@link ResultsViewContent} node itself to store status,
   * instead of passing InfoWarning Changed to track all nodes already visited
   */
  private void nodeNeedsWarningDecorator(ResultsViewContent node, Set<ResultsViewContent> onPath) {
    node.f_isInfoDecorated = node.f_isInfo;
    node.f_isInfoWarningDecorate = node.f_isInfoWarning;

    onPath.add(node);
    /*
     * Add warning decorators the content items we have encountered for the
     * first time
     */
    for (ResultsViewContent item : node.getChildrenAsCollection()) {
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!onPath.contains(item)) {
        if (!item.f_donePropagatingWarningDecorators) {
          nodeNeedsWarningDecorator(item, onPath);
        }
        node.f_isInfoDecorated |= item.f_isInfoDecorated;
        node.f_isInfoWarningDecorate |= item.f_isInfoWarningDecorate;
      }
    }
    node.f_donePropagatingWarningDecorators = true;
    onPath.remove(node);
  }

  /**
   * Converts back edges into leaf nodes
   */
  private void breakBackEdges(Collection<ResultsViewContent> contentRoot) {
    // fake out the recursive function by pretending the root is a node
    ResultsViewContent root = makeContent("", contentRoot);
    breakBackEdges(root, new HashSet<ResultsViewContent>());
  }

  /**
   * Converts back edges into leaf nodes Also converts the children to arrays
   * 
   * @param onPath
   *          Nodes already encountered on the path here
   * @param leaves
   *          Leaves previously created
   */
  private void breakBackEdges(ResultsViewContent node, Set<ResultsViewContent> onPath) {
    if (node.getChildrenAsCollection().isEmpty()) {
      node.resetChildren(Collections.<ResultsViewContent> emptyList());
      return;
    }
    onPath.add(node);

    // Only used to get consistent results when breaking back edges
    // NOT for the results view (see CNameSorter)
    final List<ResultsViewContent> children = new ArrayList<ResultsViewContent>(node.getChildrenAsCollection());
    final int size = children.size();
    if (size > 1) {
      Collections.sort(children, new Comparator<ResultsViewContent>() {
        public int compare(ResultsViewContent o1, ResultsViewContent o2) {
          return o1.getMessage().compareTo(o2.getMessage());
        }
      });
    }

    for (int i = 0; i < size; i++) {
      ResultsViewContent item = children.get(i);

      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      // System.out.println("Looking at "+node.getMessage()+" -> "+item.getMessage());
      if (!onPath.contains(item)) {
        breakBackEdges(item, onPath);
      } else {
        // Need to replace with a leaf
        // System.out.println("Breaking backedge for: "+item.getMessage());
        ResultsViewContent leaf = item.cloneAsLeaf();
        // System.out.println("Cloned: "+leaf.getMessage());
        children.set(i, leaf);
      }
    }
    node.resetChildren(children);
    // Now it creates a leaf, if I ever see it again
    if (allowDuplicateNodes) {
      onPath.remove(node);
    }
  }

  ResultsViewContentProvider buildModelOfDropSea_internal() {
    // show at the viewer root
    Collection<ResultsViewContent> root = new HashSet<ResultsViewContent>();

    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();

    if (scan != null) {

      for (IPromiseDrop pd : scan.getPromiseDrops()) {
        if (pd.isFromSrc() || pd.derivedFromSrc()) {
          // System.out.println("Considering: "+pd.getMessage());
          if (showAtTopLevel(pd)) {
            root.add(encloseDrop(pd, null));
          } else {
            // System.out.println("Rejected: "+pd.getMessage());
          }
        }
      }

      final Collection<IAnalysisHintDrop> infoDrops = scan.getAnalysisHintDrops();
      if (!infoDrops.isEmpty()) {
        final String msg = "Suggestions and warnings";
        ResultsViewContent infoFolder = makeContent(msg);
        infoFolder.setCount(infoDrops.size());

        for (IDrop id : infoDrops) {
          infoFolder.addChild(encloseDrop(id, infoFolder));
        }
        infoFolder.setBaseImageName(CommonImages.IMG_INFO);
        infoFolder.f_isInfo = true;
        root.add(infoFolder);
      }
    }

    root = categorize(root);
    root = packageTypeFolderize(root);
    propagateWarningDecorators(root);
    breakBackEdges(root);
    m_root = root.toArray();
    // reset our cache, for next time
    m_contentCache.clear();

    return this;
  }

  private static boolean showAtTopLevel(IPromiseDrop d) {
    if (d == null)
      return false;
    if (d.instanceOfIRDropSea(UiShowAtTopLevel.class))
      return true;
    /*
     * If we have a deponent promise that is not a scoped promise we do not want
     * to show at the top level.
     */
    for (IPromiseDrop pd : d.getDeponentPromises()) {
      if (!(pd instanceof IScopedPromiseDrop))
        return false;
    }
    return true;
  }
}