package com.surelogic.javac.coe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.CommonImages;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.jsure.xml.CoE_Constants;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclarations;
import edu.cmu.cs.fluid.java.promise.TextFile;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.ProofDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.ISupportingInformation;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.PleaseCount;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;
import edu.cmu.cs.fluid.sea.drops.promises.RequiresLockPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ArrayUtil;

@SuppressWarnings("unchecked")
public class ResultsViewContentProvider {
  private static final Logger LOG = SLLogger.getLoggerFor(ResultsViewContentProvider.class);
  protected static final Object[] noObjects = ArrayUtil.empty;

  // TODO These are not completely protected, since the arrays get returned
  protected static Object[] m_root = noObjects;
  protected static Object[] m_lastRoot = null;
  protected static long timeStamp = Sea.INVALIDATED;

  private boolean m_showInferences;

  /*
   * public void inputChanged(Viewer v, Object oldInput, Object newInput) {
   * Object[] result = new Object[1]; result[0] = newInput; }
   */

  public void dispose() {
    // nothing to do
  }

  public Object[] getElements(Object parent) {
    synchronized (ResultsViewContentProvider.class) {
      return (m_showInferences ? m_root : Content.filterNonInfo(m_root));
    }
  }

  public Object getParent(Object child) {
    return null;
  }

  public Object[] getChildren(Object parent) {
    if (parent instanceof Content) {
      Content item = (Content) parent;
      return (m_showInferences ? item.getChildren() : item.getNonInfoChildren());
    }
    return noObjects;
  }

  public boolean hasChildren(Object parent) {
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
  private Map<Drop, Content> m_contentCache = new HashMap<Drop, Content>();

  /**
   * Encloses in {@link Content}items and adds each drop in
   * <code>dropsToAdd</code> to the mutable set of viewer content items passed
   * into this method.
   * 
   * @param mutableContentSet
   *          A parent {@link Content} object to add children to
   * @param dropsToAdd
   *          the set of drops to enclose and add to the content set
   */
  private void addDrops(Content mutableContentSet, Set<? extends Drop> dropsToAdd) {
    for (Iterator<? extends Drop> i = dropsToAdd.iterator(); i.hasNext();) {
      Drop drop = i.next();
      mutableContentSet.addChild(encloseDrop(drop));

    }
  }

  /**
   * Adds referenced supporting information about a drop to the mutable set of
   * viewer content items passed into this method.
   * 
   * @param mutableContentSet
   *          set of all {@link Content}items
   * @param about
   *          the {@link Drop}to add supporting information about
   */
  private void addSupportingInformation(Content mutableContentSet, IRReferenceDrop about) {
    Collection<ISupportingInformation> supportingInformation = about.getSupportingInformation();
    int size = supportingInformation.size();
    if (size == 0) {
      // no supporting information, thus bail out
      return;
    } else if (size == 1) {
      ISupportingInformation si = supportingInformation.iterator().next();
      Content informationItem = new Content("supporting information: " + si.getMessage(), si.getLocation());
      informationItem.setBaseImageName(CommonImages.IMG_INFO);
      mutableContentSet.addChild(informationItem);
      return;
    }
    // More than one thing
    Content siFolder = new Content("supporting information:");
    siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

    for (Iterator<ISupportingInformation> i = supportingInformation.iterator(); i.hasNext();) {
      ISupportingInformation si = i.next();
      Content informationItem = new Content(si.getMessage(), si.getLocation());
      informationItem.setBaseImageName(CommonImages.IMG_INFO);
      siFolder.addChild(informationItem);
    }
    // TODO why do I need this code?
    switch (siFolder.numChildren()) {
    case 0:
      return; // Don't add anything
    case 1:
      mutableContentSet.addChild((Content) siFolder.getChildren()[0]);
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
  private void add_and_TrustedPromises(Content mutableContentSet, ResultDrop result) {
    // Create a folder to contain the preconditions
    Set<PromiseDrop> trustedPromiseDrops = result.getTrusts();
    int count = trustedPromiseDrops.size();
    // bail out if no preconditions exist
    if (count < 1)
      return;
    Content preconditionFolder = new Content(count + (count > 1 ? " prerequisite assertions:" : " prerequisite assertion:"));
    int flags = 0; // assume no adornments
    flags |= (result.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
    boolean elementsProvedConsistent = true; // assume true

    // add trusted promises to the folder
    for (ProofDrop trustedDrop : trustedPromiseDrops) {
      // ProofDrop trustedDrop = (ProofDrop) j.next();
      preconditionFolder.addChild(encloseDrop(trustedDrop));
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
  private void add_or_TrustedPromises(Content mutableContentSet, ResultDrop result) {
    if (!result.hasOrLogic()) {
      // no "or" logic on this result, thus bail out
      return;
    }

    // Create a folder to contain the choices
    Set<String> or_TrustLabels = result.get_or_TrustLabelSet();
    final int or_TrustLabelsSize = or_TrustLabels.size();
    Content orContentFolder = new Content(or_TrustLabelsSize
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
      Content choiceFolder = new Content(key + ":");
      orContentFolder.addChild(choiceFolder);

      // set proof bits properly
      boolean choiceConsistent = true;
      boolean choiceUsesRedDot = false;
      Set<? extends ProofDrop> choiceSet = result.get_or_Trusts(key);

      // fill in the folder with choices
      for (ProofDrop trustedDrop : choiceSet) {
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
   * Create a {@link Content}item for a drop-sea drop. This is only done once,
   * hence, the same Content item is returned if the same drop is passed to this
   * method.
   * 
   * @param drop
   *          the drop to enclose
   * @return the content item the viewer can use
   */
  private Content encloseDrop(Drop drop) {
    if (drop == null) {
      LOG.log(Level.SEVERE, "ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
      throw new IllegalArgumentException("ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
    }
    if (m_contentCache.containsKey(drop)) {
      // in cache
      return m_contentCache.get(drop);
    } else {
      // create & add to cache -- MUST BE IMMEDIATE TO AVOID INFINITE
      // RECURSION
      Content result = new Content(drop.getMessage(), drop);
      m_contentCache.put(drop, result); // to avoid infinite recursion

      // ///////////////
      // PROMISE DROP //
      // ///////////////
      if (drop instanceof PromiseDrop) {
        PromiseDrop promiseDrop = (PromiseDrop) drop;

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
        result.setBaseImageName(CommonImages.IMG_PROMISE);

        // children
        addSupportingInformation(result, promiseDrop);

        final Set<Drop> matching = new HashSet<Drop>();
        matching.addAll(promiseDrop.getMatchingDependents(DropPredicateFactory.matchType(PromiseDrop.class)));
        matching.addAll(promiseDrop.getMatchingDependents(DropPredicateFactory.matchType(InfoDrop.class)));
        addDrops(result, matching);
        addDrops(result, promiseDrop.getCheckedBy());

        // //////////////
        // RESULT DROP //
        // //////////////
      } else if (drop instanceof ResultDrop) {
        ResultDrop resultDrop = (ResultDrop) drop;

        // image
        int flags = 0; // assume no adornments
        if (resultDrop.getTrustsComplete().size() > 0) {
          // only show reddot and proof status if this results has
          // preconditions
          flags |= (resultDrop.proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
          flags |= (resultDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
        }
        result.setImageFlags(flags);
        result.setBaseImageName(resultDrop.isConsistent() ? CommonImages.IMG_PLUS : CommonImages.IMG_RED_X);

        // children
        addSupportingInformation(result, resultDrop);
        add_or_TrustedPromises(result, resultDrop);
        add_and_TrustedPromises(result, resultDrop);

        // ////////////
        // INFO DROP //
        // ////////////
      } else if (drop instanceof InfoDrop) {
        InfoDrop infoDrop = (InfoDrop) drop;

        // image
        result.setBaseImageName(drop instanceof WarningDrop ? CommonImages.IMG_WARNING : CommonImages.IMG_INFO);

        // children
        addSupportingInformation(result, infoDrop);
        result.isInfo = true;
        result.isInfoWarning = drop instanceof WarningDrop;

        // ///////////////////////
        // PROMISE WARNING DROP //
        // ///////////////////////
      } else if (drop instanceof PromiseWarningDrop) {
        PromiseWarningDrop promiseWarningDrop = (PromiseWarningDrop) drop;

        // image
        result.setBaseImageName(CommonImages.IMG_WARNING);

        // children
        addSupportingInformation(result, promiseWarningDrop);
        result.isPromiseWarning = true;
      } else {
        LOG.log(Level.SEVERE, "ResultsViewContentProvider.encloseDrop(Drop) passed an unknown drop type " + drop.getClass());
      }
      return result;
    }
  }

  /**
   * Adds categories to the graph of Content nodes rooted contentRoot and
   * returns the new set of root nodes.
   * 
   * @param contentRoot
   *          root of a graph of Content nodes
   * @return the new set of Content nodes (that should replace contentRoot)
   */
  private Collection<Content> categorize(Collection<Content> contentRoot) {
    // fake out the recursive function by pretending the root is a Content
    // node
    Content root = new Content("", contentRoot);
    categorizeRecursive(root, true, new HashSet<Content>(), new HashSet<Content>());
    return root.children();
  }

  /**
   * Recursive method to categorize a graph of content nodes.
   * 
   * @param node
   *          the Content node to categorize
   * @param atRoot
   *          <code>true</code> if at the Content root, <code>false</code>
   *          otherwise (used to control categorization of promise drops)
   * @param existingCategoryFolderSet
   *          a running set tracking what folders we have created used to ensure
   *          we don't categorize things we have already done
   * @param level
   *          hack to avoid an infinite loop
   * 
   * @see #categorize(Set)
   */
  private void categorizeRecursive(Content node, boolean atRoot, Set<Content> existingCategoryFolderSet,
      Set<Content> contentsOnPathToRoot) {
    Set<Content> categorizedChildren = new HashSet<Content>();
    Set<Content> toBeCategorized = new HashSet<Content>();
    Map<Category, Content> categoryToFolder = new HashMap<Category, Content>();
    for (Content item : node.children()) {
      if (existingCategoryFolderSet.contains(item)) {
        /*
         * This is a previously created folder (went around the loop) so just
         * add it to the resulting Content set. Do not add it to the worklist to
         * be categorized or an infinite loop will result.
         */
        categorizedChildren.add(item);
      } else {
        toBeCategorized.add(item);
        if (item.referencedDrop instanceof PromiseDrop && !atRoot && !(item.referencedDrop instanceof RequiresLockPromiseDrop)
            && !(item.referencedDrop instanceof PleaseFolderize)) {
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
             * this Content item to it
             */
            Content categoryFolder = categoryToFolder.get(itemCategory);
            if (categoryFolder == null) {
              // create the category folder, save it in the map
              categoryFolder = new Content(itemCategory.getMessage());
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
    for (Map.Entry<Category, Content> entry : categoryToFolder.entrySet()) {
      // Category category = entry.getKey();
      Content categoryFolder = entry.getValue();
      // for (Iterator<Category> j = categoryToFolder.keySet().iterator();
      // j.hasNext();) {
      // Category category = j.next();
      // Content categoryFolder = categoryToFolder.get(category);

      // message
      // category.setCount(categoryFolder.numChildren());
      // categoryFolder.message = category.getFormattedMessage();
      categoryFolder.freezeCount();

      // image (try to show proof status if it makes sense)
      Set<ProofDrop> proofDrops = new HashSet<ProofDrop>();
      Set<WarningDrop> warningDrops = new HashSet<WarningDrop>();
      Set<InfoDrop> infoDrops = new HashSet<InfoDrop>();

      for (Content item : categoryFolder.children()) {

        if (item.referencedDrop instanceof ProofDrop) {
          proofDrops.add((ProofDrop) item.referencedDrop);
        } else if (item.referencedDrop instanceof InfoDrop) {
          infoDrops.add((InfoDrop) item.referencedDrop);
          if (item.referencedDrop instanceof WarningDrop) {
            warningDrops.add((WarningDrop) item.referencedDrop);
          }
        }
      }
      if (proofDrops.isEmpty() && !infoDrops.isEmpty()) {
        categoryFolder.setBaseImageName(!warningDrops.isEmpty() ? CommonImages.IMG_WARNING : CommonImages.IMG_INFO);
        categoryFolder.isInfo = true;
      } else if (proofDrops.isEmpty() && infoDrops.isEmpty()) {
        categoryFolder.setBaseImageName(CommonImages.IMG_WARNING);
        categoryFolder.isPromiseWarning = true;
      } else {
        // set proof bits properly
        int flags = 0; // assume no adornments
        boolean choiceConsistent = true;
        boolean choiceUsesRedDot = false;
        boolean localConsistent = true;
        for (Iterator<ProofDrop> l = proofDrops.iterator(); l.hasNext();) {
          ProofDrop proofDrop = l.next();
          choiceConsistent &= proofDrop.provedConsistent();
          if (proofDrop instanceof ResultDrop) {
            localConsistent &= ((ResultDrop) proofDrop).isConsistent();
          }
          if (proofDrop.proofUsesRedDot())
            choiceUsesRedDot = true;
        }
        flags = (choiceUsesRedDot ? CoE_Constants.REDDOT : 0);
        flags |= (choiceConsistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);
        categoryFolder.setImageFlags(flags);
        categoryFolder.setBaseImageName(atRoot ? CommonImages.IMG_TALLYHO : (localConsistent ? CommonImages.IMG_FOLDER
            : CommonImages.IMG_FOLDER));
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
    for (Iterator<Content> k = toBeCategorized.iterator(); k.hasNext();) {
      Content item = k.next();
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!contentsOnPathToRoot.contains(item)) {
        /*
         * Set<Content> newContentsOnPathToRoot = new
         * HashSet<Content>(contentsOnPathToRoot);
         * newContentsOnPathToRoot.add(item); categorizeRecursive(item, false,
         * existingCategoryFolderSet, newContentsOnPathToRoot);
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
   *          root of a graph of Content nodes
   */
  private Collection<Content> packageTypeFolderize(Collection<Content> contentRoot) {
    // fake out the recursive function by pretending the root is a Content
    // node
    Content root = new Content("", contentRoot);
    packageTypeFolderizeRecursive(root, true, new HashSet<Content>(), new HashSet<Content>());
    return root.children();
  }

  private void packageTypeFolderizeRecursive(Content node, boolean atRoot, Set<Content> existingFolderSet,
      Set<Content> contentsOnPathToRoot) {
    Set<Content> newChildren = new HashSet<Content>();
    Set<Content> toBeFolderized = new HashSet<Content>();
    Map<String, Map<String, Content>> packageToClassToFolder = new HashMap<String, Map<String, Content>>();

    for (Content item : node.children()) {
      if (existingFolderSet.contains(item)) {
        /*
         * This is a previously created folder (went around the loop) so just
         * add it to the resulting Content set. Do not add it to the worklist to
         * be categorized or an infinite loop will result.
         */
        newChildren.add(item);
      } else {
        toBeFolderized.add(item);

        /*
         * If the drop the Content "item" references has a package and a type
         * we'll generate folders for it.
         */
        Drop drop = item.referencedDrop;
        boolean hasJavaContext = false;
        if (drop instanceof ResultDrop || drop instanceof InfoDrop || drop instanceof PleaseFolderize) {
          boolean resultHasACategory = drop instanceof ResultDrop && ((ResultDrop) drop).getCategory() != null;
          if (resultHasACategory || drop instanceof InfoDrop || drop instanceof PleaseFolderize) {
            ContentJavaContext context = new ContentJavaContext(item);
            if (context.complete) {
              hasJavaContext = true;
              String packageKey = context.packageName;
              String typeKey = context.typeName;
              Map<String, Content> typeToFolder = packageToClassToFolder.get(packageKey);
              if (typeToFolder == null) {
                typeToFolder = new HashMap<String, Content>();
                packageToClassToFolder.put(packageKey, typeToFolder);
              }
              Content folder = typeToFolder.get(typeKey);
              if (folder == null) {
                // create the class/type folder, save it in the
                // map
                folder = new Content(typeKey);
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
      Map<?, Content> typeToFolder = packageToClassToFolder.get(packageKey);

      Content packageFolder = new Content(packageKey, typeToFolder.values());
      existingFolderSet.add(packageFolder);

      for (Content typeFolder : packageFolder.children()) {
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
    for (Iterator<Content> k = toBeFolderized.iterator(); k.hasNext();) {
      Content item = k.next();
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!contentsOnPathToRoot.contains(item)) {
        /*
         * Set<Content> newContentsOnPathToRoot = new
         * HashSet<Content>(contentsOnPathToRoot);
         * newContentsOnPathToRoot.add(item);
         * packageTypeFolderizeRecursive(item, false, existingFolderSet,
         * newContentsOnPathToRoot);
         */
        // Changed to add and then remove the item from the set
        contentsOnPathToRoot.add(item);
        packageTypeFolderizeRecursive(item, false, existingFolderSet, contentsOnPathToRoot);
        contentsOnPathToRoot.remove(item);
      }
    }
  }

  private void setConsistencyDecoratorForATypeFolder(Content c) {
    boolean hasAResult = false;
    boolean consistent = true;
    boolean hasRedDot = false;
    for (Content node : c.children()) {
      Drop d = node.referencedDrop;
      if (d instanceof ProofDrop) {
        hasAResult = true;
        ProofDrop pd = (ProofDrop) d;
        consistent = consistent && pd.provedConsistent();
        hasRedDot = hasRedDot || pd.proofUsesRedDot();
      }
    }
    if (hasAResult) {
      int flags = c.getImageFlags();

      flags |= (hasRedDot ? CoE_Constants.REDDOT : 0);
      flags |= (consistent ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT);

      c.setImageFlags(flags);
    }
  }

  private void setConsistencyDecoratorForAPackageFolder(Content c) {
    boolean hasAResult = false;
    boolean consistent = true;
    boolean hasRedDot = false;
    for (Content node : c.children()) {
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

  private void propagateWarningDecorators(Collection<Content> contentRoot) {
    // fake out the recursive function by pretending the root is a Content
    // node
    Content root = new Content("", contentRoot);
    nodeNeedsWarningDecorator(root, new HashSet<Content>());
  }

  /*
   * static class InfoWarning { boolean isInfo = false; boolean isInfoWarning =
   * false; }
   */

  /**
   * Only called by nodeNeedsWarningDecorator and itself
   * 
   * Changed to use Content node itself to store status, instead of passing
   * InfoWarning Changed to track all nodes already visited
   */
  private void nodeNeedsWarningDecorator(Content node, Set<Content> onPath) {
    node.isInfoDecorated = node.isInfo;
    node.isInfoWarningDecorate = node.isInfoWarning;

    if (node.referencedDrop instanceof PleaseCount) {
      node.setCount(((PleaseCount) node.referencedDrop).count());
    }

    onPath.add(node);
    /*
     * Add warning decorators the content items we have encountered for the
     * first time
     */
    for (Content item : node.children()) {
      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!onPath.contains(item)) {
        if (!item.donePropagatingWarningDecorators) {
          nodeNeedsWarningDecorator(item, onPath);
        }
        node.isInfoDecorated |= item.isInfoDecorated;
        node.isInfoWarningDecorate |= item.isInfoWarningDecorate;
      }
    }
    node.donePropagatingWarningDecorators = true;
    onPath.remove(node);
  }

  /**
   * Converts back edges into leaf nodes
   */
  private void breakBackEdges(Collection<Content> contentRoot) {
    // fake out the recursive function by pretending the root is a Content
    // node
    Content root = new Content("", contentRoot);
    breakBackEdges(root, new HashSet<Content>(), new HashMap<Content, Content>());
  }

  /**
   * Converts back edges into leaf nodes Also converts the children to arrays
   * 
   * @param onPath
   *          Nodes already encountered on the path here
   * @param leaves
   *          Leaves previously created
   */
  private void breakBackEdges(Content node, Set<Content> onPath, Map<Content, Content> leaves) {
    if (node.children().isEmpty()) {
      node.resetChildren(Collections.<Content> emptyList());
      return;
    }
    onPath.add(node);

    final List<Content> children = new ArrayList<Content>(node.children());
    final int size = children.size();
    for (int i = 0; i < size; i++) {
      Content item = children.get(i);

      /*
       * Guard against infinite recursion (drop-sea is a graph)
       */
      if (!onPath.contains(item)) {
        breakBackEdges(item, onPath, leaves);
      } else {
        // Need to replace with a leaf
        Content leaf = leaves.get(item);
        if (leaf == null) {
          leaf = item.cloneAsLeaf();
          leaves.put(item, leaf);
        }
        children.set(i, leaf);
      }
    }
    node.resetChildren(children);
    onPath.remove(node);
  }

  static private class ContentJavaContext {

    /**
     * Flags if the entire Java context is well-defined
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
    public ContentJavaContext(final Content content) {
      // Get reference IRNode
      if (!(content.referencedDrop instanceof IRReferenceDrop))
        return;
      IRReferenceDrop drop = (IRReferenceDrop) content.referencedDrop;
      final IRNode node = drop.getNode();
      if (node == null) {
        return;
      }
      /*
       * if (!node.equals(content.referencedLocation)) { LOG.warning("Node from
       * ref drop != node in content: "+content.referencedLocation); }
       */
      final Operator op = JavaNames.getOperator(node);
      final boolean isCU = CompilationUnit.prototype.includes(op);
      final boolean isPkg = PackageDeclaration.prototype.includes(op);

      // determine package
      IRNode cu = null;
      if (isCU) {
        cu = node;
      } else {
        cu = VisitUtil.getEnclosingCompilationUnit(node);
      }
      if (cu != null) {
        String proposedPackageName = VisitUtil.getPackageName(cu);
        if (proposedPackageName != null) {
          packageName = proposedPackageName;
        }

        // determine enclosing type
        IRNode type = null;
        if (isCU) {
          IRNode types = CompilationUnit.getDecls(node);
          Iterator<IRNode> e = TypeDeclarations.getTypesIterator(types);
          while (e.hasNext()) {
            type = e.next();
            if (JavaNode.getModifier(type, JavaNode.PUBLIC)) {
              break; // Found the main type
            }
          }
        } else if (!isPkg) {
          type = VisitUtil.getEnclosingType(node);
          if (type == null && op instanceof TypeDeclInterface) {
            type = node;
          }
        }
        if (type != null) {
          typeName = JavaNames.getRelativeTypeName(type);
          final Operator top = JavaNames.getOperator(type);
          typeIsAnInterface = InterfaceDeclaration.prototype.includes(top);
        } else if (isPkg) {
          typeName = "package";
          typeIsAnInterface = false;
        } else {
          LOG.severe("No enclosing type for: " + DebugUnparser.toString(node));
        }
      } else if (TextFile.prototype.includes(op)) {
        typeName = TextFile.getId(node);
        packageName = null;
      } else {
        LOG.warning("Unable to get Java context for " + DebugUnparser.toString(node));
      }
      complete = !typeName.equals(JavaNames.getTypeName(null));
    }
  }

  public ResultsViewContentProvider buildModelOfDropSea() {
    synchronized (ResultsViewContentProvider.class) {
      long viewTime = timeStamp;
      long seaTime = Sea.getDefault().getTimeStamp();
      if (seaTime == Sea.INVALIDATED) {
        seaTime = Sea.getDefault().updateConsistencyProof();
      }

      SLLogger.getLogger().log(Level.INFO, "Comparing view (" + viewTime + ") to sea (" + seaTime + ")");
      if (viewTime != Sea.INVALIDATED && viewTime == seaTime) {
        return this;
      }
      SLLogger.getLogger().fine("Building model of Drop-Sea");
      ResultsViewContentProvider rv = buildModelOfDropSea_internal();
      timeStamp = Sea.getDefault().getTimeStamp();
      return rv;
    }
  }

  private ResultsViewContentProvider buildModelOfDropSea_internal() {
    Collection<Content> root = new HashSet<Content>(); // show at the
    // viewer root

    Set<? extends PromiseDrop> promiseDrops = Sea.getDefault().getDropsOfType(PromiseDrop.class);
    for (PromiseDrop pd : promiseDrops) {
      // PromiseDrop pd = (PromiseDrop) i.next();
      if (pd.isFromSrc()) {
        if (!pd.hasMatchingDeponents(DropPredicateFactory.matchType(PromiseDrop.class))) {
          root.add(encloseDrop(pd));
        }
      }
    }

    Set<? extends InfoDrop> infoDrops = Sea.getDefault().getDropsOfType(InfoDrop.class);
    if (!infoDrops.isEmpty()) {
      final String msg = "Suggestions and warnings";
      Content infoFolder = new Content(msg);
      infoFolder.setCount(infoDrops.size());

      boolean hasWarning = false;
      for (InfoDrop id : infoDrops) {
        // InfoDrop id = (InfoDrop) j.next();
        if (id instanceof WarningDrop)
          hasWarning = true;
        infoFolder.addChild(encloseDrop(id));
      }
      infoFolder.setBaseImageName(hasWarning ? CommonImages.IMG_WARNING : CommonImages.IMG_INFO);
      infoFolder.isInfo = true;
      root.add(infoFolder);
    }

    Set<? extends PromiseWarningDrop> promiseWarningDrops = Sea.getDefault().getDropsOfType(PromiseWarningDrop.class);
    for (PromiseWarningDrop id : promiseWarningDrops) {
      // PromiseWarningDrop id = (PromiseWarningDrop) j.next();
      // only show info drops at the main level if they are not attached
      // to a promise drop or a result drop
      root.add(encloseDrop(id));
    }

    Set<ResultDrop> resultDrops = Sea.getDefault().getDropsOfType(ResultDrop.class);
    for (ResultDrop id : resultDrops) {
      // ResultDrop id = (ResultDrop) j.next();
      // only show result drops at the main level if they are not attached
      // to a promise drop or a result drop

      if (id.isValid() && ((id.getChecks().isEmpty() && id.getTrusts().isEmpty()))) {
        if (id.getCategory() == null) {
          id.setCategory(Messages.DSC_UNPARENTED_DROP);
        }
        root.add(encloseDrop(id));
      }
    }
    /*
     * Set<ModuleModel> moduleDrops =
     * Sea.getDefault().getDropsOfExactType(ModuleModel.class); for (ModuleModel
     * id : moduleDrops) { root.add(encloseDrop(id)); }
     */

    root = categorize(root);
    root = packageTypeFolderize(root);
    propagateWarningDecorators(root);
    breakBackEdges(root);
    m_lastRoot = m_root;
    m_root = root.toArray();
    m_contentCache = new HashMap<Drop, Content>(); // reset our cache, for
    // next time
    // m_contentCache.clear();

    return this;
  }

  public Object[] getLastElements() {
    synchronized (ResultsViewContentProvider.class) {
      return (m_showInferences ? m_lastRoot : Content.filterNonInfo(m_lastRoot));
    }
  }
}