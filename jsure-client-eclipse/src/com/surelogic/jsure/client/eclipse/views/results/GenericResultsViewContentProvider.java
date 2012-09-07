package com.surelogic.jsure.client.eclipse.views.results;

import java.io.File;
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

import org.eclipse.jface.viewers.Viewer;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.IPromiseDrop;
import edu.cmu.cs.fluid.sea.IProofDrop;
import edu.cmu.cs.fluid.sea.IProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.IResultDrop;
import edu.cmu.cs.fluid.sea.IResultFolderDrop;
import edu.cmu.cs.fluid.sea.ISupportingInformation;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.ProofDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.ResultFolderDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.MaybeTopLevel;
import edu.cmu.cs.fluid.sea.drops.PleaseCount;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;
import edu.cmu.cs.fluid.sea.drops.promises.PromisePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RequiresLockPromiseDrop;
import edu.cmu.cs.fluid.util.ArrayUtil;

abstract class GenericResultsViewContentProvider<T extends IDrop, C extends AbstractContent<T, C>> implements
    IResultsViewContentProvider {
  private static final boolean allowDuplicateNodes = true;
  protected static final Object[] noObjects = ArrayUtil.empty;

  // TODO These are not completely protected, since the arrays get returned
  protected static Object[] m_root = noObjects;
  protected static Object[] m_lastRoot = null;
  protected static long timeStamp = Sea.INVALIDATED;

  private final Sea sea;

  protected static final Logger LOG = SLLogger.getLogger("ResultsViewContentProvider");

  private boolean m_showInferences = true;

  /**
   * @return Returns the showInferences.
   */
  public final boolean isShowInferences() {
    return m_showInferences;
  }

  /**
   * @param showInferences
   *          The showInferences to set.
   */
  public final void setShowInferences(boolean showInferences) {
    this.m_showInferences = showInferences;
  }

  GenericResultsViewContentProvider(Sea sea) {
    this.sea = sea;
  }

  public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    /*
     * This kills the contents
     * 
     * if (newInput == null) { m_root = noObjects; m_lastRoot = null; }
     */
  }

  public void dispose() {
    // nothing to do
  }

  public final Object[] getElements(Object parent) {
    synchronized (GenericResultsViewContentProvider.class) {
      return getElementsInternal();
    }
  }

  protected Object[] getElementsInternal() {
    return (isShowInferences() ? m_root : AbstractContent.<T, C> filterNonInfo(m_root));
  }

  @SuppressWarnings("unchecked")
  public Object getParent(Object child) {
    C c = (C) child;
    return c.getParent();
  }

  public final Object[] getChildren(Object parent) {
    return getChildrenInternal(parent);
  }

  @SuppressWarnings("unchecked")
  protected Object[] getChildrenInternal(Object parent) {
    if (parent instanceof AbstractContent<?, ?>) {
      C item = (C) parent;
      return (isShowInferences() ? item.getChildren() : item.getNonInfoChildren());
    }
    return noObjects;
  }

  public final boolean hasChildren(Object parent) {
    Object[] children = getChildren(parent);
    return (children == null ? false : children.length > 0);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Map used to ensure building a model for the viewer doesn't go into infinite
   * recursion.
   * 
   * @see #encloseDrop(Drop)
   */
  private Map<T, C> m_contentCache = new HashMap<T, C>();

  protected final C putInContentCache(T key, C value) {
    return m_contentCache.put(key, value);
  }

  protected final C getFromContentCache(T key) {
    return m_contentCache.get(key);
  }

  protected final boolean existsInCache(T key) {
    return m_contentCache.containsKey(key);
  }

  /**
   * Encloses in {@link C}items and adds each drop in <code>dropsToAdd</code> to
   * the mutable set of viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          A parent {@link C} object to add children to
   * @param dropsToAdd
   *          the set of drops to enclose and add to the content set
   */
  protected void addDrops(C mutableContentSet, Collection<? extends T> dropsToAdd) {
    for (T drop : dropsToAdd) {
      mutableContentSet.addChild(encloseDrop(drop));
    }
  }

  protected abstract C makeContent(String msg);

  protected abstract C makeContent(String msg, Collection<C> contentRoot);

  protected abstract C makeContent(String msg, T drop);

  protected abstract C makeContent(String msg, ISrcRef ref);

  /**
   * Adds referenced supporting information about a drop to the mutable set of
   * viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          set of all {@link Content} items
   * @param about
   *          the {@link Drop}to add supporting information about
   */
  @SuppressWarnings("unchecked")
  private void addSupportingInformation(C mutableContentSet, IDrop about) {
    Collection<ISupportingInformation> supportingInformation = about.getSupportingInformation();
    int size = supportingInformation.size();
    if (size == 0) {
      // no supporting information, thus bail out
      return;
    } else if (size == 1) {
      ISupportingInformation si = supportingInformation.iterator().next();
      C informationItem = makeContent("supporting information: " + si.getMessage(), si.getSrcRef());
      informationItem.setBaseImageName(CommonImages.IMG_INFO);
      mutableContentSet.addChild(informationItem);
      return;
    }
    // More than one thing
    C siFolder = makeContent("supporting information:");
    siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

    for (Iterator<ISupportingInformation> i = supportingInformation.iterator(); i.hasNext();) {
      ISupportingInformation si = i.next();
      C informationItem = makeContent(si.getMessage(), si.getSrcRef());
      informationItem.setBaseImageName(CommonImages.IMG_INFO);
      siFolder.addChild(informationItem);
    }
    // Improves the presentation in the view
    switch (siFolder.numChildren()) {
    case 0:
      return; // Don't add anything
    case 1:
      mutableContentSet.addChild((C) siFolder.getChildren()[0]);
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
   *          set of all {@link Content} items
   * @param about
   *          the {@link Drop}to add proposed promises about
   */
  @SuppressWarnings("unchecked")
  private void addProposedPromises(C mutableContentSet, IDrop about) {
    Collection<? extends IProposedPromiseDrop> proposals = about.getProposals();
    int size = proposals.size();
    if (size == 0) {
      // no proposed promises, thus bail out
      return;
    } else if (size == 1) {
      IProposedPromiseDrop pp = proposals.iterator().next();
      final C proposalItem = makeContent("proposed promise: " + pp.getJavaAnnotation(), (T) pp);
      proposalItem.setBaseImageName(CommonImages.IMG_ANNOTATION_PROPOSED);
      mutableContentSet.addChild(proposalItem);
      return;
    }
    // More than one thing
    C siFolder = makeContent(I18N.msg("jsure.eclipse.proposed.promise.content.folder"));
    siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

    for (IProposedPromiseDrop pp : proposals) {
      final C proposalItem = makeContent(pp.getJavaAnnotation(), (T) pp);
      proposalItem.setBaseImageName(CommonImages.IMG_ANNOTATION_PROPOSED);
      siFolder.addChild(proposalItem);
    }
    // Improves the presentation in the view
    switch (siFolder.numChildren()) {
    case 0:
      return; // Don't add anything
    case 1:
      mutableContentSet.addChild((C) siFolder.getChildren()[0]);
      mutableContentSet.addChild(siFolder);
      break;
    default:
      mutableContentSet.addChild(siFolder);
      break;
    }
  }

  /**
   * Adds "and" precondition logic information about a drop to the mutable set
   * of viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          A parent {@link Content} object to add children to
   * @param result
   *          the result to add "and" precondition logic about
   */
  @SuppressWarnings("unchecked")
  private void add_and_TrustedPromises(C mutableContentSet, IResultDrop result) {
    // Create a folder to contain the preconditions
    Collection<? extends IProofDrop> trustedPromiseDrops = result.getTrusts();
    int count = trustedPromiseDrops.size();
    // bail out if no preconditions exist
    if (count < 1)
      return;
    C preconditionFolder = makeContent(count + (count > 1 ? " prerequisite assertions:" : " prerequisite assertion:"));
    int flags = 0; // assume no adornments
    flags |= (result.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
    boolean elementsProvedConsistent = true; // assume true

    // add trusted promises to the folder
    for (IProofDrop trustedDrop : trustedPromiseDrops) {
      // ProofDrop trustedDrop = (ProofDrop) j.next();
      preconditionFolder.addChild(encloseDrop((T) trustedDrop));
      elementsProvedConsistent &= trustedDrop.provedConsistent();
    }

    // finish up the folder
    flags |= (elementsProvedConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
    preconditionFolder.setImageFlags(flags);
    preconditionFolder.setBaseImageName(CommonImages.IMG_CHOICE_ITEM);
    mutableContentSet.addChild(preconditionFolder);
  }

  /**
   * Adds "or" precondition logic information about a drop to the mutable set of
   * viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          A parent {@link Content} object to add children to
   * @param result
   *          the result to add "or" precondition logic about
   */
  @SuppressWarnings("unchecked")
  private void add_or_TrustedPromises(C mutableContentSet, IResultDrop result) {
    if (!result.hasOrLogic()) {
      // no "or" logic on this result, thus bail out
      return;
    }

    // Create a folder to contain the choices
    final Collection<String> or_TrustLabels = result.get_or_TrustLabelSet();
    final int or_TrustLabelsSize = or_TrustLabels.size();
    C orContentFolder = makeContent(or_TrustLabelsSize
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
      C choiceFolder = makeContent(key + ":");
      orContentFolder.addChild(choiceFolder);

      // set proof bits properly
      boolean choiceConsistent = true;
      boolean choiceUsesRedDot = false;
      Collection<? extends IProofDrop> choiceSet = result.get_or_Trusts(key);

      // fill in the folder with choices
      for (IProofDrop trustedDrop : choiceSet) {
        // ProofDrop trustedDrop = (ProofDrop) j.next();
        choiceFolder.addChild(encloseDrop((T) trustedDrop));
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
   * Create a {@link Content}item for a drop-sea drop. This is only done once,
   * hence, the same Content item is returned if the same drop is passed to this
   * method.
   * 
   * @param drop
   *          the drop to enclose
   * @return the content item the viewer can use
   */
  @SuppressWarnings("unchecked")
  protected final C encloseDrop(T drop) {
    if (drop == null) {
      LOG.log(Level.SEVERE, "ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
      throw new IllegalArgumentException("ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
    }
    C result = getFromContentCache(drop);
    if (result != null) {
      // in cache
      return result;
    } else if (existsInCache(drop)) {
      return null;
    } else {
      // create & add to cache -- MUST BE IMMEDIATE TO AVOID INFINITE
      // RECURSION
      result = makeContent(drop.getMessage(), drop);
      putInContentCache(drop, result); // to avoid infinite recursion

      if (drop.instanceOf(PromiseDrop.class)) {

        /*
         * PROMISE DROP
         */

        IPromiseDrop promiseDrop = (IPromiseDrop) drop;

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
        matching.addAll(promiseDrop.getMatchingDependents(DropPredicateFactory.matchType(PromiseDrop.class)));
        matching.addAll(promiseDrop.getMatchingDependents(DropPredicateFactory.matchType(InfoDrop.class)));
        addDrops(result, (Collection<? extends T>) matching);
        addDrops(result, (Collection<? extends T>) promiseDrop.getCheckedBy());

      } else if (drop.instanceOf(ResultDrop.class)) {

        /*
         * RESULT DROP
         */
        IResultDrop resultDrop = (IResultDrop) drop;

        // image
        int flags = 0; // assume no adornments
        if (resultDrop.getTrustsComplete().size() > 0) {
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
        add_or_TrustedPromises(result, resultDrop);
        add_and_TrustedPromises(result, resultDrop);
        // TODO add checked promises?
        
      } else if (drop.instanceOf(ResultFolderDrop.class)) {
    	
          /*
           * RESULT DROP
           */
          IResultFolderDrop resultDrop = (IResultFolderDrop) drop;

          // image
          int flags = 0; // assume no adornments
          result.setImageFlags(flags);
          result.setBaseImageName(CommonImages.IMG_FOLDER);
          addDrops(result, (Collection<? extends T>) resultDrop.getContents());
          
      } else if (drop.instanceOf(InfoDrop.class)) {

        /*
         * INFO DROP
         */

        // image
        result.setBaseImageName(drop.instanceOf(WarningDrop.class) ? CommonImages.IMG_WARNING : CommonImages.IMG_INFO);

        // children
        addSupportingInformation(result, drop);
        addProposedPromises(result, drop);

        result.f_isInfo = true;
        result.f_isInfoWarning = drop.instanceOf(WarningDrop.class);

      } else if (drop.instanceOf(PromiseWarningDrop.class)) {

        /*
         * PROMISE WARNING DROP
         */

        // image
        result.setBaseImageName(CommonImages.IMG_WARNING);

        // children
        addSupportingInformation(result, drop);
        result.f_isPromiseWarning = true;
      } else {
        LOG.log(Level.SEVERE, "ResultsViewContentProvider.encloseDrop(Drop) passed an unknown drop type " + drop.getClass());
      }
      return result;
    }
  }

  /**
   * Adds categories to the graph of C nodes rooted contentRoot and returns the
   * new set of root nodes.
   * 
   * @param contentRoot
   *          root of a graph of C nodes
   * @return the new set of C nodes (that should replace contentRoot)
   */
  private Collection<C> categorize(Collection<C> contentRoot) {
    // fake out the recursive function by pretending the root is a C
    // node
    C root = makeContent("", contentRoot);
    categorizeRecursive(root, true, new HashSet<C>(), new HashSet<C>());
    return root.children();
  }

  /**
   * Recursive method to categorize a graph of content nodes.
   * 
   * @param node
   *          the C node to categorize
   * @param atRoot
   *          <code>true</code> if at the C root, <code>false</code> otherwise
   *          (used to control categorization of promise drops)
   * @param existingCategoryFolderSet
   *          a running set tracking what folders we have created used to ensure
   *          we don't categorize things we have already done
   * @param level
   *          hack to avoid an infinite loop
   * 
   * @see #categorize(Set)
   */
  private void categorizeRecursive(C node, boolean atRoot, Set<C> existingCategoryFolderSet, Set<C> contentsOnPathToRoot) {
    Set<C> categorizedChildren = new HashSet<C>();
    Set<C> toBeCategorized = new HashSet<C>();
    Map<Category, C> categoryToFolder = new HashMap<Category, C>();
    for (C item : node.children()) {
      if (existingCategoryFolderSet.contains(item)) {
        /*
         * This is a previously created folder (went around the loop) so just
         * add it to the resulting C set. Do not add it to the worklist to be
         * categorized or an infinite loop will result.
         */
        categorizedChildren.add(item);
      } else {
        toBeCategorized.add(item);
        final IDrop info = item.getDropInfo();
        boolean dontCategorize = false;
        if (info != null) { 
        	if (info.instanceOf(PromiseDrop.class)) {
        		dontCategorize = !atRoot && !(info.instanceOf(RequiresLockPromiseDrop.class))
        		                 && !(info.instanceOf(PleaseFolderize.class));
            }
        	else if (info.instanceOf(ResultDrop.class)) {
        		IResultDrop r = (IResultDrop) info;
        		dontCategorize = r.hasEnclosingFolder();
        	}
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
             * this C item to it
             */
            C categoryFolder = categoryToFolder.get(itemCategory);
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
    for (Map.Entry<Category, C> entry : categoryToFolder.entrySet()) {
      // Category category = entry.getKey();
      C categoryFolder = entry.getValue();
      // for (Iterator<Category> j = categoryToFolder.keySet().iterator();
      // j.hasNext();) {
      // Category category = j.next();
      // C categoryFolder = categoryToFolder.get(category);

      // message
      // category.setCount(categoryFolder.numChildren());
      // categoryFolder.message = category.getFormattedMessage();
      categoryFolder.freezeCount();

      // image (try to show proof status if it makes sense)
      Set<IProofDrop> proofDrops = new HashSet<IProofDrop>();
      Set<IDrop> warningDrops = new HashSet<IDrop>();
      Set<IDrop> infoDrops = new HashSet<IDrop>();

      for (C item : categoryFolder.children()) {
        if (item.getDropInfo().instanceOf(ProofDrop.class)) {
          proofDrops.add((IProofDrop) item.getDropInfo());
        } else if (item.getDropInfo().instanceOf(InfoDrop.class)) {
          infoDrops.add(item.getDropInfo());
          if (item.getDropInfo().instanceOf(WarningDrop.class)) {
            warningDrops.add(item.getDropInfo());
          }
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
          /*
           * if (proofDrop.isInstance(ResultDrop.class)) { localConsistent &=
           * proofDrop.isConsistent(); }
           */
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
    for (Iterator<C> k = toBeCategorized.iterator(); k.hasNext();) {
      C item = k.next();
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!contentsOnPathToRoot.contains(item)) {
        /*
         * Set<C> newCsOnPathToRoot = new HashSet<C>(contentsOnPathToRoot);
         * newCsOnPathToRoot.add(item); categorizeRecursive(item, false,
         * existingCategoryFolderSet, newCsOnPathToRoot);
         */
        // Changed to add/remove the item from the set
        contentsOnPathToRoot.add(item);
        categorizeRecursive(item, false, existingCategoryFolderSet, contentsOnPathToRoot);
        contentsOnPathToRoot.remove(item);
      }
    }
  }

  /**
   * reates folders for the package and type a result is within.
   * 
   * @param contentRoot
   *          root of a graph of C nodes
   */
  private Collection<C> packageTypeFolderize(Collection<C> contentRoot) {
    // fake out the recursive function by pretending the root is a C
    // node
    C root = makeContent("", contentRoot);
    packageTypeFolderizeRecursive(root, true, new HashSet<C>(), new HashSet<C>());
    return root.children();
  }

  private void packageTypeFolderizeRecursive(C node, boolean atRoot, Set<C> existingFolderSet, Set<C> contentsOnPathToRoot) {
    Set<C> newChildren = new HashSet<C>();
    Set<C> toBeFolderized = new HashSet<C>();
    Map<String, Map<String, C>> packageToClassToFolder = new HashMap<String, Map<String, C>>();

    for (C item : node.children()) {
      if (existingFolderSet.contains(item)) {
        /*
         * This is a previously created folder (went around the loop) so just
         * add it to the resulting C set. Do not add it to the worklist to be
         * categorized or an infinite loop will result.
         */
        newChildren.add(item);
      } else {
        toBeFolderized.add(item);

        /*
         * If the drop the C "item" references has a package and a type we'll
         * generate folders for it.
         */
        final IDrop drop = item.getDropInfo();
        boolean hasJavaContext = false;
        if (drop != null
            && (drop.instanceOf(ResultDrop.class) || drop.instanceOf(InfoDrop.class) || drop.instanceOf(PleaseFolderize.class))) {
          boolean resultHasACategory = drop.instanceOf(ResultDrop.class) && drop.getCategory() != null;
          if (resultHasACategory || drop.instanceOf(InfoDrop.class) || drop.instanceOf(PleaseFolderize.class)) {
            ContentJavaContext<T, C> context = new ContentJavaContext<T, C>(item);
            if (context.complete) {
              hasJavaContext = true;
              String packageKey = context.packageName;
              String typeKey = context.typeName;
              Map<String, C> typeToFolder = packageToClassToFolder.get(packageKey);
              if (typeToFolder == null) {
                typeToFolder = new HashMap<String, C>();
                packageToClassToFolder.put(packageKey, typeToFolder);
              }
              C folder = typeToFolder.get(typeKey);
              if (folder == null) {
                // create the class/type folder, save it in the
                // map
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
      Map<?, C> typeToFolder = packageToClassToFolder.get(packageKey);

      C packageFolder = makeContent(packageKey, typeToFolder.values());
      existingFolderSet.add(packageFolder);

      for (C typeFolder : packageFolder.children()) {
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
    for (Iterator<C> k = toBeFolderized.iterator(); k.hasNext();) {
      C item = k.next();
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!contentsOnPathToRoot.contains(item)) {
        /*
         * Set<C> newCsOnPathToRoot = new HashSet<C>(contentsOnPathToRoot);
         * newCsOnPathToRoot.add(item); packageTypeFolderizeRecursive(item,
         * false, existingFolderSet, newCsOnPathToRoot);
         */
        // Changed to add and then remove the item from the set
        contentsOnPathToRoot.add(item);
        packageTypeFolderizeRecursive(item, false, existingFolderSet, contentsOnPathToRoot);
        contentsOnPathToRoot.remove(item);
      }
    }
  }

  private void setConsistencyDecoratorForATypeFolder(C c) {
    boolean hasAResult = false;
    boolean consistent = true;
    boolean hasRedDot = false;
    for (C node : c.children()) {
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

  private void setConsistencyDecoratorForAPackageFolder(C c) {
    boolean hasAResult = false;
    boolean consistent = true;
    boolean hasRedDot = false;
    for (C node : c.children()) {
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

  private void propagateWarningDecorators(Collection<C> contentRoot) {
    // fake out the recursive function by pretending the root is a C
    // node
    C root = makeContent("", contentRoot);
    nodeNeedsWarningDecorator(root, new HashSet<C>());
  }

  /*
   * static class InfoWarning { boolean isInfo = false; boolean isInfoWarning =
   * false; }
   */

  /**
   * Only called by nodeNeedsWarningDecorator and itself
   * 
   * Changed to use C node itself to store status, instead of passing
   * InfoWarning Changed to track all nodes already visited
   */
  private void nodeNeedsWarningDecorator(C node, Set<C> onPath) {
    node.f_isInfoDecorated = node.f_isInfo;
    node.f_isInfoWarningDecorate = node.f_isInfoWarning;

    if (node.getDropInfo() != null && node.getDropInfo().instanceOf(PleaseCount.class)) {
      String value = node.getDropInfo().getAttribute(PleaseCount.COUNT);
      int count = 0;
      if (value != null) {
        count = Integer.valueOf(value);
      }
      node.setCount(count);
      return;
    }

    onPath.add(node);
    /*
     * Add warning decorators the content items we have encountered for the
     * first time
     */
    for (C item : node.children()) {
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

  /*
   * private int count(Collection<C> cc, Set<C> counted) { int i=0; for(C c :
   * cc) { if (counted.contains(c)) { continue; } counted.add(c); i++; i +=
   * count(c.children(), counted); } return i; }
   */

  /**
   * Converts back edges into leaf nodes
   */
  private void breakBackEdges(Collection<C> contentRoot) {
    // System.out.println("C count: "+count(contentRoot, new HashSet<C>()));

    // fake out the recursive function by pretending the root is a C
    // node
    C root = makeContent("", contentRoot);
    breakBackEdges(root, new HashSet<C>());
  }

  /**
   * Converts back edges into leaf nodes Also converts the children to arrays
   * 
   * @param onPath
   *          Nodes already encountered on the path here
   * @param leaves
   *          Leaves previously created
   */
  private void breakBackEdges(C node, Set<C> onPath) {
    /*
     * Integer count = counts.get(node); if (count == null) { counts.put(node,
     * 1); } else { System.out.println(count+": "+node.getMessage());
     * counts.put(node, count++); }
     */
    if (node.children().isEmpty()) {
      node.resetChildren(Collections.<C> emptyList());
      return;
    }
    onPath.add(node);

    // Only used to get consistent results when breaking back edges
    // NOT for the results view (see CNameSorter)
    final List<C> children = new ArrayList<C>(node.children());
    final int size = children.size();
    if (size > 1) {
      Collections.sort(children, new Comparator<C>() {
        public int compare(C o1, C o2) {
          return o1.getMessage().compareTo(o2.getMessage());
        }
      });
    }

    for (int i = 0; i < size; i++) {
      C item = children.get(i);

      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      // System.out.println("Looking at "+node.getMessage()+" -> "+item.getMessage());
      if (!onPath.contains(item)) {
        breakBackEdges(item, onPath);
      } else {
        // Need to replace with a leaf
        // System.out.println("Breaking backedge for: "+item.getMessage());
        C leaf = item.cloneAsLeaf();
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

  // Map<C,Integer> counts = new HashMap<C, Integer>();

  static private class ContentJavaContext<T extends IDrop, C extends AbstractContent<T, C>> {

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
    public ContentJavaContext(final C content) {
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

  public IResultsViewContentProvider buildModelOfDropSea() {
    synchronized (GenericResultsViewContentProvider.class) {
      long viewTime = timeStamp;
      long seaTime = sea.getTimeStamp();
      if (seaTime == Sea.INVALIDATED) {
        seaTime = sea.updateConsistencyProof();
      }

      SLLogger.getLogger().fine("Comparing view (" + viewTime + ") to sea (" + seaTime + ")");
      if (viewTime != Sea.INVALIDATED && viewTime == seaTime) {
        return this;
      }
      SLLogger.getLogger().fine("Building model of Drop-Sea");
      IResultsViewContentProvider rv = buildModelOfDropSea_internal();
      timeStamp = sea.getTimeStamp();
      return rv;
    }
  }

  private static DropPredicate promisePred = DropPredicateFactory.matchType(PromiseDrop.class);

  private static DropPredicate scopedPromisePred = DropPredicateFactory.matchType(PromisePromiseDrop.class);

  /**
   * Matches non-@Promise PromiseDrops
   */
  private static DropPredicate predicate = new DropPredicate() {
    public boolean match(IDrop d) {
      return promisePred.match(d) && !scopedPromisePred.match(d);
    }
  };

  protected abstract boolean dropsExist(Class<? extends Drop> type);

  protected abstract <R extends IDrop> Collection<R> getDropsOfType(Class<? extends Drop> type, Class<R> rType);

  @SuppressWarnings("unchecked")
  protected IResultsViewContentProvider buildModelOfDropSea_internal() {
    // show at the viewer root
    Collection<C> root = new HashSet<C>();

    final Collection<IProofDrop> promiseDrops = getDropsOfType(PromiseDrop.class, IProofDrop.class);
    for (IProofDrop pd : promiseDrops) {
      if (pd.isFromSrc() || pd.derivedFromSrc()) {
        // System.out.println("Considering: "+pd.getMessage());
        if (!pd.hasMatchingDeponents(predicate) || shouldBeTopLevel(pd)) {
          root.add(encloseDrop((T) pd));
        } else {
          // System.out.println("Rejected: "+pd.getMessage());
        }
      }
    }

    final Collection<IDrop> infoDrops = getDropsOfType(InfoDrop.class, IDrop.class);
    if (!infoDrops.isEmpty()) {
      final String msg = "Suggestions and warnings";
      C infoFolder = makeContent(msg);
      infoFolder.setCount(infoDrops.size());

      for (IDrop id : infoDrops) {
        infoFolder.addChild(encloseDrop((T) id));
      }
      infoFolder.setBaseImageName(CommonImages.IMG_INFO);
      infoFolder.f_isInfo = true;
      root.add(infoFolder);
    }

    final Collection<IResultDrop> resultDrops = getDropsOfType(ResultDrop.class, IResultDrop.class);
    for (IResultDrop id : resultDrops) {
      // only show result drops at the main level if they are not attached
      // to a promise drop or a result drop
      if (id.isValid() && ((id.getChecks().isEmpty() && id.getTrusts().isEmpty() && !id.hasEnclosingFolder()) || shouldBeTopLevel(id))) {
        if (id.getCategory() == null) {
          id.setCategory(Messages.DSC_UNPARENTED_DROP);
        }
        root.add(encloseDrop((T) id));
      }
    }

    root = categorize(root);
    root = packageTypeFolderize(root);
    propagateWarningDecorators(root);
    breakBackEdges(root);
    m_lastRoot = m_root;
    m_root = root.toArray();
    // reset our cache, for next time
    m_contentCache = new HashMap<T, C>();

    return this;
  }

  protected static <T extends IDrop> boolean shouldBeTopLevel(T d) {
    // System.out.println("???: "+d.getMessage());
    return d != null && d.instanceOf(MaybeTopLevel.class) && d.requestTopLevel();
  }

  public Object[] getLastElements() {
    synchronized (GenericResultsViewContentProvider.class) {
      return (isShowInferences() ? m_lastRoot : AbstractContent.<T, C> filterNonInfo(m_lastRoot));
    }
  }
}