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

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IAnalysisResultDrop;
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
   * Map used to ensure building a model for the viewer doesn't go into infinite
   * recursion.
   */
  private final Map<IDrop, ResultsViewContent> m_contentCache = new HashMap<IDrop, ResultsViewContent>();

  private ResultsViewContent putInContentCache(IDrop key, ResultsViewContent value) {
    return m_contentCache.put(key, value);
  }

  private ResultsViewContent getFromContentCache(IDrop key) {
    return m_contentCache.get(key);
  }

  private boolean existsInCache(IDrop key) {
    return m_contentCache.containsKey(key);
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
      mutableContentSet.addChild(encloseDrop(drop));
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
    int size = supportingInformation.size();
    if (size == 0) {
      // no supporting information, thus bail out
      return;
    } else if (size == 1) {
      ISupportingInformation si = supportingInformation.iterator().next();
      ResultsViewContent informationItem = makeContent("supporting information: " + si.getMessage(), si.getSrcRef());
      informationItem.setBaseImageName(CommonImages.IMG_INFO);
      mutableContentSet.addChild(informationItem);
      return;
    }
    // More than one thing
    ResultsViewContent siFolder = makeContent("supporting information:");
    siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

    for (Iterator<ISupportingInformation> i = supportingInformation.iterator(); i.hasNext();) {
      ISupportingInformation si = i.next();
      ResultsViewContent informationItem = makeContent(si.getMessage(), si.getSrcRef());
      informationItem.setBaseImageName(CommonImages.IMG_INFO);
      siFolder.addChild(informationItem);
    }
    // Improves the presentation in the view
    switch (siFolder.numChildren()) {
    case 0:
      return; // Don't add anything
    case 1:
      mutableContentSet.addChild((ResultsViewContent) siFolder.getChildren()[0]);
      mutableContentSet.addChild(siFolder);
      break;
    default:
      mutableContentSet.addChild(siFolder);
      break;
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
    int size = proposals.size();
    if (size == 0) {
      // no proposed promises, thus bail out
      return;
    } else if (size == 1) {
      IProposedPromiseDrop pp = proposals.iterator().next();
      final ResultsViewContent proposalItem = makeContent("proposed promise: " + pp.getJavaAnnotation(), (IDrop) pp);
      proposalItem.setBaseImageName(CommonImages.IMG_ANNOTATION_PROPOSED);
      mutableContentSet.addChild(proposalItem);
      return;
    }
    // More than one thing
    ResultsViewContent siFolder = makeContent(I18N.msg("jsure.eclipse.proposed.promise.content.folder"));
    siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

    for (IProposedPromiseDrop pp : proposals) {
      final ResultsViewContent proposalItem = makeContent(pp.getJavaAnnotation(), pp);
      proposalItem.setBaseImageName(CommonImages.IMG_ANNOTATION_PROPOSED);
      siFolder.addChild(proposalItem);
    }
    // Improves the presentation in the view
    switch (siFolder.numChildren()) {
    case 0:
      return; // Don't add anything
    case 1:
      mutableContentSet.addChild((ResultsViewContent) siFolder.getChildren()[0]);
      mutableContentSet.addChild(siFolder);
      break;
    default:
      mutableContentSet.addChild(siFolder);
      break;
    }
  }

  /**
   * Adds "and" trusts logic information about a result drop to the mutable set
   * of viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          A parent {@link ResultsViewContent} object to add children to
   * @param result
   *          the result to add "and" precondition logic about
   */
  private void add_and_Trusted(ResultsViewContent mutableContentSet, IResultDrop result) {
    // Create a folder to contain the preconditions
    Collection<IPromiseDrop> trustedPromises = new ArrayList<IPromiseDrop>();
    Collection<IAnalysisResultDrop> trustedResults = new ArrayList<IAnalysisResultDrop>();
    for (IProofDrop pd : result.getTrusted_and()) {
      if (pd instanceof IPromiseDrop)
        trustedPromises.add((IPromiseDrop) pd);
      if (pd instanceof IAnalysisResultDrop)
        trustedResults.add((IAnalysisResultDrop) pd);
    }
    
    // X Add promises directly
    for (IProofDrop trustedDrop : trustedPromises) {
      mutableContentSet.addChild(encloseDrop(trustedDrop));
    }

    // Add results and folders directly
    for (IAnalysisResultDrop resultDrop : trustedResults) {
      mutableContentSet.addChild(encloseDrop(resultDrop));
    }

//    /*
//     * Add promises in a prerequisite assertion folder
//     */
//    int count = trustedPromises.size();
//    // bail out if no preconditions exist
//    if (count < 1)
//      return;
//    ResultsViewContent preconditionFolder = makeContent(count
//        + (count > 1 ? " prerequisite assertions:" : " prerequisite assertion:"));
//    int flags = 0; // assume no adornments
//    flags |= (result.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
//    boolean elementsProvedConsistent = true; // assume true
//
//    // add trusted proof drops to the folder
//    for (IProofDrop trustedDrop : trustedPromises) {
//      preconditionFolder.addChild(encloseDrop(trustedDrop));
//      elementsProvedConsistent &= trustedDrop.provedConsistent();
//    }
//
//    // finish up the folder
//    flags |= (elementsProvedConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
//    preconditionFolder.setImageFlags(flags);
//    preconditionFolder.setBaseImageName(CommonImages.IMG_CHOICE_ITEM);
//    mutableContentSet.addChild(preconditionFolder);
  }

  /**
   * Adds "or" trusts logic information about a result drop to the mutable set
   * of viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          A parent {@link ResultsViewContent} object to add children to
   * @param result
   *          the result to add "or" precondition logic about
   */
  private void add_or_Trusted(ResultsViewContent mutableContentSet, IResultDrop result) {
    if (!result.hasOrLogic()) {
      // no "or" logic on this result, thus bail out
      return;
    }

    // Create a folder to contain the choices
    final Collection<String> or_TrustLabels = result.getTrusted_orKeys();
    final int or_TrustLabelsSize = or_TrustLabels.size();
    ResultsViewContent orContentFolder = makeContent(or_TrustLabelsSize
        + (or_TrustLabelsSize > 1 ? " possible prerequisite assertion choices:" : " possible prerequisite assertion choice:"));
    int flags = 0; // assume no adornments
    flags |= (result.get_or_proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
    flags |= (result.get_or_provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
    orContentFolder.setImageFlags(flags);
    orContentFolder.setBaseImageName(CommonImages.IMG_CHOICE);
    mutableContentSet.addChild(orContentFolder);

    // create a folder for each choice
    for (String key : or_TrustLabels) {
      // String key = (String) i.next();
      ResultsViewContent choiceFolder = makeContent(key + ":");
      orContentFolder.addChild(choiceFolder);

      // set proof bits properly
      boolean choiceConsistent = true;
      boolean choiceUsesRedDot = false;
      Collection<? extends IProofDrop> choiceSet = result.getTrusted_or(key);

      // fill in the folder with choices
      for (IProofDrop trustedDrop : choiceSet) {
        // ProofDrop trustedDrop = (ProofDrop) j.next();
        choiceFolder.addChild(encloseDrop(trustedDrop));
        choiceConsistent &= trustedDrop.provedConsistent();
        if (trustedDrop.proofUsesRedDot())
          choiceUsesRedDot = true;
      }
      flags = (choiceUsesRedDot ? CoE_Constants.REDDOT : 0);
      flags |= (choiceConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
      choiceFolder.setImageFlags(flags);
      choiceFolder.setBaseImageName(CommonImages.IMG_CHOICE_ITEM);
    }
  }

  /**
   * Create a {@link ResultsViewContent} item for a drop-sea drop. This is only
   * done once, hence, the same Content item is returned if the same drop is
   * passed to this method.
   * 
   * @param drop
   *          the drop to enclose
   * @return the content item the viewer can use
   */
  private ResultsViewContent encloseDrop(IDrop drop) {
    if (drop == null) {
      LOG.log(Level.SEVERE, "ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
      throw new IllegalArgumentException("ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
    }
    ResultsViewContent result = getFromContentCache(drop);
    if (result != null) {
      // in cache
      return result;
    } else if (existsInCache(drop)) {
      return null;
    } else {
      // create & add to cache
      // MUST BE IMMEDIATE TO AVOID INFINITE RECURSION
      result = makeContent(drop.getMessage(), drop);
      putInContentCache(drop, result); // to avoid infinite recursion

      if (drop instanceof IPromiseDrop) {
        final IPromiseDrop promiseDrop = (IPromiseDrop) drop;

        /*
         * PROMISE DROP
         */

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
        addProposedPromises(result, promiseDrop);

        final Set<IDrop> matching = new HashSet<IDrop>();
        matching.addAll(promiseDrop.getDependentPromises());
        matching.addAll(promiseDrop.getAnalysisHintsAbout());
        addDrops(result, matching);
        addDrops(result, promiseDrop.getCheckedBy());

      } else if (drop instanceof IResultDrop) {
        final IResultDrop resultDrop = (IResultDrop) drop;

        /*
         * RESULT DROP
         */

        // image
        int flags = 0; // assume no adornments
        if (resultDrop.hasTrusted()) {
          // only show reddot and proof status if this results has
          // preconditions
          flags |= (resultDrop.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
          flags |= (resultDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
        }
        result.setImageFlags(flags);
        result.setBaseImageName(resultDrop.isConsistent() ? CommonImages.IMG_PLUS
            : resultDrop.isVouched() ? CommonImages.IMG_PLUS_VOUCH : resultDrop.isTimeout() ? CommonImages.IMG_TIMEOUT_X
                : CommonImages.IMG_RED_X);

        // children
        addSupportingInformation(result, resultDrop);
        addProposedPromises(result, resultDrop);
        add_or_Trusted(result, resultDrop);
        add_and_Trusted(result, resultDrop);

      } else if (drop instanceof IResultFolderDrop) {
        final IResultFolderDrop resultDrop = (IResultFolderDrop) drop;

        /*
         * RESULT FOLDER DROP
         */

        // image
        int flags = 0; // assume no adornments
        flags |= (resultDrop.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
        flags |= (resultDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
        result.setImageFlags(flags);
        result.setBaseImageName(CommonImages.IMG_FOLDER);

        addDrops(result, resultDrop.getContents());
        addProposedPromises(result, resultDrop);

      } else if (drop instanceof IAnalysisHintDrop) {
        final IAnalysisHintDrop infoDrop = (IAnalysisHintDrop) drop;

        /*
         * INFO DROP
         */

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
//          else if (info instanceof IAnalysisResultDrop) {
//            final IAnalysisResultDrop r = (IAnalysisResultDrop) info;
//            dontCategorize = r.isInResultFolder();
//          }
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
   // packageTypeFolderizeRecursive(root, true, new HashSet<ResultsViewContent>(), new HashSet<ResultsViewContent>());
    return root.getChildrenAsCollection();
  }

  private void packageTypeFolderizeRecursive(ResultsViewContent node, boolean atRoot, Set<ResultsViewContent> existingFolderSet,
      Set<ResultsViewContent> contentsOnPathToRoot) {
    Set<ResultsViewContent> newChildren = new HashSet<ResultsViewContent>();
    Set<ResultsViewContent> toBeFolderized = new HashSet<ResultsViewContent>();
    Map<String, Map<String, ResultsViewContent>> packageToClassToFolder = new HashMap<String, Map<String, ResultsViewContent>>();

    for (ResultsViewContent item : node.getChildrenAsCollection()) {
      if (existingFolderSet.contains(item)) {
        /*
         * This is a previously created folder (went around the loop) so just
         * add it to the resulting content set. Do not add it to the worklist to
         * be categorized or an infinite loop will result.
         */
        newChildren.add(item);
      } else {
        toBeFolderized.add(item);

        /*
         * If the drop the content "item" references has a package and a type
         * we'll generate folders for it.
         */
        final IDrop drop = item.getDropInfo();
        boolean hasJavaContext = false;
        if (drop != null && (drop instanceof IResultDrop || drop instanceof IAnalysisHintDrop)) {
          boolean resultHasACategory = drop instanceof IResultDrop && drop.getCategory() != null;
          if (resultHasACategory || drop instanceof IAnalysisHintDrop) {
            ContentJavaContext context = new ContentJavaContext(item);
            if (context.complete) {
              hasJavaContext = true;
              String packageKey = context.packageName;
              String typeKey = context.typeName;
              Map<String, ResultsViewContent> typeToFolder = packageToClassToFolder.get(packageKey);
              if (typeToFolder == null) {
                typeToFolder = new HashMap<String, ResultsViewContent>();
                packageToClassToFolder.put(packageKey, typeToFolder);
              }
              ResultsViewContent folder = typeToFolder.get(typeKey);
              if (folder == null) {
                // create the class/type folder, save it in the map
                folder = makeContent(typeKey);
                folder.setBaseImageName(context.typeIsAnInterface ? CommonImages.IMG_INTERFACE : CommonImages.IMG_CLASS);
                typeToFolder.put(typeKey, folder);
              }
              folder.addChild(item);
            }
          }
        }
        /*
         * If we couldn't figure out the package and class just add the drop
         * back into the children.
         */
        if (!hasJavaContext) {
          newChildren.add(item);
        }
      }
    }

    /*
     * Create the package folders and add associated type folders into it.
     */
    for (Iterator<String> i = packageToClassToFolder.keySet().iterator(); i.hasNext();) {
      String packageKey = i.next();
      Map<?, ResultsViewContent> typeToFolder = packageToClassToFolder.get(packageKey);

      ResultsViewContent packageFolder = makeContent(packageKey, typeToFolder.values());
      existingFolderSet.add(packageFolder);

      for (ResultsViewContent typeFolder : packageFolder.getChildrenAsCollection()) {
        setConsistencyDecoratorForATypeFolder(typeFolder);
      }
      packageFolder.freezeChildrenCount();
      packageFolder.setBaseImageName(CommonImages.IMG_PACKAGE);
      setConsistencyDecoratorForAPackageFolder(packageFolder);

      newChildren.add(packageFolder);
    }

    /*
     * Replace the children of the node parameter with the new children created
     * within this method
     */
    node.resetChildren(newChildren);

    /*
     * Categorize the content items we encountered for the first time
     */
    for (Iterator<ResultsViewContent> k = toBeFolderized.iterator(); k.hasNext();) {
      ResultsViewContent item = k.next();
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!contentsOnPathToRoot.contains(item)) {
        // Changed to add and then remove the item from the set
        contentsOnPathToRoot.add(item);
        packageTypeFolderizeRecursive(item, false, existingFolderSet, contentsOnPathToRoot);
        contentsOnPathToRoot.remove(item);
      }
    }
  }

  private void setConsistencyDecoratorForATypeFolder(ResultsViewContent c) {
    boolean hasAResult = false;
    boolean consistent = true;
    boolean hasRedDot = false;
    for (ResultsViewContent node : c.getChildrenAsCollection()) {
      if (node.getDropInfo() instanceof IProofDrop) {
        IProofDrop d = (IProofDrop) node.getDropInfo();
        hasAResult = true;
        consistent = consistent && d.provedConsistent();
        hasRedDot = hasRedDot || d.proofUsesRedDot();
      }
    }
    if (hasAResult) {
      int flags = c.getImageFlags();

      flags |= (hasRedDot ? CoE_Constants.REDDOT : 0);
      flags |= (consistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);

      c.setImageFlags(flags);
    }
  }

  private void setConsistencyDecoratorForAPackageFolder(ResultsViewContent c) {
    boolean hasAResult = false;
    boolean consistent = true;
    boolean hasRedDot = false;
    for (ResultsViewContent node : c.getChildrenAsCollection()) {
      int flags = node.getImageFlags();
      boolean nConsistent = (flags & CoE_Constants.CONSISTENT) != 0;
      boolean nInconsistent = (flags & CoE_Constants.INCONSISTENT) != 0;
      boolean nHasRedDot = (flags & CoE_Constants.REDDOT) != 0;
      if (nConsistent || nInconsistent) {
        hasAResult = true;
        consistent = consistent && nConsistent;
        hasRedDot = hasRedDot || nHasRedDot;
      }
    }
    if (hasAResult) {
      int flags = c.getImageFlags();

      flags |= (hasRedDot ? CoE_Constants.REDDOT : 0);
      flags |= (consistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);

      c.setImageFlags(flags);
    }
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

  static private class ContentJavaContext {

    /**
     * {@code true} if the entire Java context is well-defined, {@code false}
     * otherwise.
     */
    public boolean complete = false;

    String packageName = "(default)";

    String typeName = "NONE";

    private boolean typeIsAnInterface = false;

    /**
     * Tries to construct a full Java context, if this fails {@link #complete}
     * will be <code>false</code>.
     * 
     * @param content
     *          the viewer content item to obtain the Java context for
     */
    public ContentJavaContext(final ResultsViewContent content) {
      final IDrop info = content.getDropInfo();
      final ISrcRef ref = info.getSrcRef();
      if (ref != null) {
        packageName = ref.getPackage();
        int lastSeparator = ref.getCUName().lastIndexOf(File.separator);
        typeName = lastSeparator < 0 ? ref.getCUName() : ref.getCUName().substring(lastSeparator + 1);
        complete = true;
      }
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
            root.add(encloseDrop(pd));
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
          infoFolder.addChild(encloseDrop(id));
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