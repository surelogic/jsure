package edu.cmu.cs.fluid.dcf.views.coe;

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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.xml.results.coe.CoE_Constants;

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
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.ProofDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.SupportingInformation;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.MaybeTopLevel;
import edu.cmu.cs.fluid.sea.drops.PleaseCount;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;
import edu.cmu.cs.fluid.sea.drops.promises.PromisePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RequiresLockPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class ResultsViewContentProvider extends
		AbstractResultsViewContentProvider {

	protected static final Object[] noObjects = new Object[0];

	// TODO These are not completely protected, since the arrays get returned
	protected static Object[] m_root = noObjects;
	protected static Object[] m_lastRoot = null;
	protected static long timeStamp = Sea.INVALIDATED;

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (newInput == null) {
			m_root = noObjects;
			m_lastRoot = null;
		}
	}

	public void dispose() {
		// nothing to do
	}

	public final Object[] getElements(Object parent) {
		synchronized (ResultsViewContentProvider.class) {
			return getElementsInternal();
		}
	}

	protected Object[] getElementsInternal() {
		return (isShowInferences() ? m_root : Content.filterNonInfo(m_root));
	}

	public Object getParent(Object child) {
		return null;
	}

	public final Object[] getChildren(Object parent) {
		return getChildrenInternal(parent);
	}

	protected Object[] getChildrenInternal(Object parent) {
		if (parent instanceof Content) {
			Content item = (Content) parent;
			return (isShowInferences() ? item.getChildren() : item
					.getNonInfoChildren());
		}
		return noObjects;
	}

	public final boolean hasChildren(Object parent) {
		Object[] children = getChildren(parent);
		return (children == null ? false : children.length > 0);
	}

	// //////////////////////////////////////////////////////////////////////////

	/**
	 * Map used to ensure building a model for the viewer doesn't go into
	 * infinite recursion.
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
	 *            A parent {@link Content} object to add children to
	 * @param dropsToAdd
	 *            the set of drops to enclose and add to the content set
	 */
	private void addDrops(Content mutableContentSet,
			Set<? extends Drop> dropsToAdd) {
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
	 *            set of all {@link Content}items
	 * @param about
	 *            the {@link Drop}to add supporting information about
	 */
	private void addSupportingInformation(Content mutableContentSet,
			IRReferenceDrop about) {
		Collection<SupportingInformation> supportingInformation = about
				.getSupportingInformation();
		int size = supportingInformation.size();
		if (size == 0) {
			// no supporting information, thus bail out
			return;
		} else if (size == 1) {
			SupportingInformation si = supportingInformation.iterator().next();
			Content informationItem = new Content("supporting information: "
					+ si.getMessage(), si.getLocation());
			informationItem.setBaseImageName(CommonImages.IMG_INFO);
			mutableContentSet.addChild(informationItem);
			return;
		}
		// More than one thing
		Content siFolder = new Content("supporting information:");
		siFolder.setBaseImageName(CommonImages.IMG_FOLDER);

		for (Iterator<SupportingInformation> i = supportingInformation
				.iterator(); i.hasNext();) {
			SupportingInformation si = i.next();
			Content informationItem = new Content(si.getMessage(), si
					.getLocation());
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
	 *            A parent {@link Content} object to add children to
	 * @param result
	 *            the result to add "and" precondition logic about
	 */
	@SuppressWarnings("unchecked")
	private void add_and_TrustedPromises(Content mutableContentSet,
			ResultDrop result) {
		// Create a folder to contain the preconditions
		Set<PromiseDrop> trustedPromiseDrops = result.getTrusts();
		int count = trustedPromiseDrops.size();
		// bail out if no preconditions exist
		if (count < 1)
			return;
		Content preconditionFolder = new Content(count
				+ (count > 1 ? " preconditions:" : " precondition:"));
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
		flags |= (elementsProvedConsistent ? CoE_Constants.CONSISTENT
				: CoE_Constants.INCONSISTENT);
		preconditionFolder.setImageFlags(flags);
		preconditionFolder.setBaseImageName(CommonImages.IMG_CHOICE_ITEM);
		mutableContentSet.addChild(preconditionFolder);
	}

	/**
	 * Adds "or" precondition logic information about a drop to the mutable set
	 * of viewer content items passed into this method.
	 * 
	 * @param mutableContentSet
	 *            A parent {@link Content} object to add children to
	 * @param result
	 *            the result to add "or" precondition logic about
	 */
	private void add_or_TrustedPromises(Content mutableContentSet,
			ResultDrop result) {
		if (!result.hasOrLogic()) {
			// no "or" logic on this result, thus bail out
			return;
		}

		// Create a folder to contain the choices
		Set<String> or_TrustLabels = result.get_or_TrustLabelSet();
		Content orContentFolder = new Content(or_TrustLabels.size()
				+ " precondition choice(s):");
		int flags = 0; // assume no adornments
		flags |= (result.get_or_proofUsesRedDot() ? CoE_Constants.REDDOT : 0);
		flags |= (result.get_or_provedConsistent() ? CoE_Constants.CONSISTENT
				: CoE_Constants.INCONSISTENT);
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
			flags |= (choiceConsistent ? CoE_Constants.CONSISTENT
					: CoE_Constants.INCONSISTENT);
			choiceFolder.setImageFlags(flags);
			choiceFolder.setBaseImageName(CommonImages.IMG_CHOICE_ITEM);
		}
	}

	/**
	 * Create a {@link Content}item for a drop-sea drop. This is only done once,
	 * hence, the same Content item is returned if the same drop is passed to
	 * this method.
	 * 
	 * @param drop
	 *            the drop to enclose
	 * @return the content item the viewer can use
	 */
	@SuppressWarnings("unchecked")
	private Content encloseDrop(Drop drop) {
		if (drop == null) {
			LOG
					.log(Level.SEVERE,
							"ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
			throw new IllegalArgumentException(
					"ResultsViewContentProvider.encloseDrop(Drop) passed a null drop");
		}
		if (m_contentCache.containsKey(drop)) {
			// in cache
			return m_contentCache.get(drop);
		} else {
			// create & add to cache -- MUST BE IMMEDIATE TO AVOID INFINITE
			// RECURSION
			Content result = new Content(drop.getMessage(), drop);
			m_contentCache.put(drop, result); // to avoid infinite recursion

			if (drop instanceof PromiseDrop) {

				/*
				 * PROMISE DROP
				 */

				PromiseDrop promiseDrop = (PromiseDrop) drop;

				// image
				int flags = 0; // assume no adornments
				if (promiseDrop.isIntendedToBeCheckedByAnalysis()) {
					flags |= (promiseDrop.proofUsesRedDot() ? CoE_Constants.REDDOT
							: 0);
					flags |= (promiseDrop.provedConsistent() ? CoE_Constants.CONSISTENT
							: CoE_Constants.INCONSISTENT);
					flags |= (promiseDrop.isCheckedByAnalysis() ? 0
							: CoE_Constants.TRUSTED);
				}
				flags |= (promiseDrop.isAssumed() ? CoE_Constants.ASSUME : 0);
				flags |= (promiseDrop.isVirtual() ? CoE_Constants.VIRTUAL : 0);
				result.setImageFlags(flags);
				result.setBaseImageName(CommonImages.IMG_ANNOTATION);

				// children
				addSupportingInformation(result, promiseDrop);

				Set<Drop> matching = new HashSet<Drop>();
				promiseDrop.addMatchingDependentsTo(matching,
						DropPredicateFactory.matchType(PromiseDrop.class));
				promiseDrop.addMatchingDependentsTo(matching,
						DropPredicateFactory.matchType(InfoDrop.class));
				addDrops(result, matching);
				addDrops(result, promiseDrop.getCheckedBy());

			} else if (drop instanceof ResultDrop) {

				/*
				 * RESULT DROP
				 */

				ResultDrop resultDrop = (ResultDrop) drop;

				// image
				int flags = 0; // assume no adornments
				if (resultDrop.getTrustsComplete().size() > 0) {
					// only show reddot and proof status if this results has
					// preconditions
					flags |= (resultDrop.proofUsesRedDot() ? CoE_Constants.REDDOT
							: 0);
					flags |= (resultDrop.provedConsistent() ? CoE_Constants.CONSISTENT
							: CoE_Constants.INCONSISTENT);
				}
				result.setImageFlags(flags);
				result
						.setBaseImageName(resultDrop.isConsistent() ? CommonImages.IMG_PLUS
								: resultDrop.isVouched() ? CommonImages.IMG_PLUS_VOUCH
										: CommonImages.IMG_RED_X);

				// children
				addSupportingInformation(result, resultDrop);
				add_or_TrustedPromises(result, resultDrop);
				add_and_TrustedPromises(result, resultDrop);

			} else if (drop instanceof InfoDrop) {

				/*
				 * INFO DROP
				 */

				InfoDrop infoDrop = (InfoDrop) drop;

				// image
				result
						.setBaseImageName(drop instanceof WarningDrop ? CommonImages.IMG_WARNING
								: CommonImages.IMG_INFO);

				// children
				addSupportingInformation(result, infoDrop);
				result.f_isInfo = true;
				result.f_isInfoWarning = drop instanceof WarningDrop;

			} else if (drop instanceof PromiseWarningDrop) {

				/*
				 * PROMISE WARNING DROP
				 */

				PromiseWarningDrop promiseWarningDrop = (PromiseWarningDrop) drop;

				// image
				result.setBaseImageName(CommonImages.IMG_WARNING);

				// children
				addSupportingInformation(result, promiseWarningDrop);
				result.f_isPromiseWarning = true;
			} else {
				LOG.log(Level.SEVERE,
						"ResultsViewContentProvider.encloseDrop(Drop) passed an unknown drop type "
								+ drop.getClass());
			}
			return result;
		}
	}

	/**
	 * Adds categories to the graph of Content nodes rooted contentRoot and
	 * returns the new set of root nodes.
	 * 
	 * @param contentRoot
	 *            root of a graph of Content nodes
	 * @return the new set of Content nodes (that should replace contentRoot)
	 */
	private Collection<Content> categorize(Collection<Content> contentRoot) {
		// fake out the recursive function by pretending the root is a Content
		// node
		Content root = new Content("", contentRoot);
		categorizeRecursive(root, true, new HashSet<Content>(),
				new HashSet<Content>());
		return root.children();
	}

	/**
	 * Recursive method to categorize a graph of content nodes.
	 * 
	 * @param node
	 *            the Content node to categorize
	 * @param atRoot
	 *            <code>true</code> if at the Content root, <code>false</code>
	 *            otherwise (used to control categorization of promise drops)
	 * @param existingCategoryFolderSet
	 *            a running set tracking what folders we have created used to
	 *            ensure we don't categorize things we have already done
	 * @param level
	 *            hack to avoid an infinite loop
	 * 
	 * @see #categorize(Set)
	 */
	private void categorizeRecursive(Content node, boolean atRoot,
			Set<Content> existingCategoryFolderSet,
			Set<Content> contentsOnPathToRoot) {
		Set<Content> categorizedChildren = new HashSet<Content>();
		Set<Content> toBeCategorized = new HashSet<Content>();
		Map<Category, Content> categoryToFolder = new HashMap<Category, Content>();
		for (Content item : node.children()) {
			if (existingCategoryFolderSet.contains(item)) {
				/*
				 * This is a previously created folder (went around the loop) so
				 * just add it to the resulting Content set. Do not add it to
				 * the worklist to be categorized or an infinite loop will
				 * result.
				 */
				categorizedChildren.add(item);
			} else {
				toBeCategorized.add(item);
				if (item.f_referencedDrop instanceof PromiseDrop
						&& !atRoot
						&& !(item.f_referencedDrop instanceof RequiresLockPromiseDrop)
						&& !(item.f_referencedDrop instanceof PleaseFolderize)) {
					/*
					 * Only categorize promise drops at the root level
					 */
					categorizedChildren.add(item);
				} else {
					Category itemCategory = item.getCategory();
					if (itemCategory == null) {
						/*
						 * Uncategorized so it doesn't need to be placed within
						 * a folder
						 */
						categorizedChildren.add(item);
					} else {
						/*
						 * Get the correct category folder (created it if
						 * needed) and add this Content item to it
						 */
						Content categoryFolder = categoryToFolder
								.get(itemCategory);
						if (categoryFolder == null) {
							// create the category folder, save it in the map
							categoryFolder = new Content(itemCategory
									.getMessage());
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

				if (item.f_referencedDrop instanceof ProofDrop) {
					proofDrops.add((ProofDrop) item.f_referencedDrop);
				} else if (item.f_referencedDrop instanceof InfoDrop) {
					infoDrops.add((InfoDrop) item.f_referencedDrop);
					if (item.f_referencedDrop instanceof WarningDrop) {
						warningDrops.add((WarningDrop) item.f_referencedDrop);
					}
				}
			}
			if (proofDrops.isEmpty() && !infoDrops.isEmpty()) {
				categoryFolder
						.setBaseImageName(!warningDrops.isEmpty() ? CommonImages.IMG_WARNING
								: CommonImages.IMG_INFO);
				categoryFolder.f_isInfo = true;
			} else if (proofDrops.isEmpty() && infoDrops.isEmpty()) {
				categoryFolder.setBaseImageName(CommonImages.IMG_WARNING);
				categoryFolder.f_isPromiseWarning = true;
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
						localConsistent &= ((ResultDrop) proofDrop)
								.isConsistent();
					}
					if (proofDrop.proofUsesRedDot())
						choiceUsesRedDot = true;
				}
				flags = (choiceUsesRedDot ? CoE_Constants.REDDOT : 0);
				flags |= (choiceConsistent ? CoE_Constants.CONSISTENT
						: CoE_Constants.INCONSISTENT);
				categoryFolder.setImageFlags(flags);
				categoryFolder.setBaseImageName(CommonImages.IMG_FOLDER);
			}
			categorizedChildren.add(categoryFolder);
		}

		/*
		 * Replace the children of the node parameter with the categorized
		 * children created within this method
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
				 * newContentsOnPathToRoot.add(item); categorizeRecursive(item,
				 * false, existingCategoryFolderSet, newContentsOnPathToRoot);
				 */
				// Changed to add/remove the item from the set
				contentsOnPathToRoot.add(item);
				categorizeRecursive(item, false, existingCategoryFolderSet,
						contentsOnPathToRoot);
				contentsOnPathToRoot.remove(item);
			}
		}
	}

	/**
	 * reates folders for the package and type a result is within.
	 * 
	 * @param contentRoot
	 *            root of a graph of Content nodes
	 */
	private Collection<Content> packageTypeFolderize(
			Collection<Content> contentRoot) {
		// fake out the recursive function by pretending the root is a Content
		// node
		Content root = new Content("", contentRoot);
		packageTypeFolderizeRecursive(root, true, new HashSet<Content>(),
				new HashSet<Content>());
		return root.children();
	}

	private void packageTypeFolderizeRecursive(Content node, boolean atRoot,
			Set<Content> existingFolderSet, Set<Content> contentsOnPathToRoot) {
		Set<Content> newChildren = new HashSet<Content>();
		Set<Content> toBeFolderized = new HashSet<Content>();
		Map<String, Map<String, Content>> packageToClassToFolder = new HashMap<String, Map<String, Content>>();

		for (Content item : node.children()) {
			if (existingFolderSet.contains(item)) {
				/*
				 * This is a previously created folder (went around the loop) so
				 * just add it to the resulting Content set. Do not add it to
				 * the worklist to be categorized or an infinite loop will
				 * result.
				 */
				newChildren.add(item);
			} else {
				toBeFolderized.add(item);

				/*
				 * If the drop the Content "item" references has a package and a
				 * type we'll generate folders for it.
				 */
				Drop drop = item.f_referencedDrop;
				boolean hasJavaContext = false;
				if (drop instanceof ResultDrop || drop instanceof InfoDrop
						|| drop instanceof PleaseFolderize) {
					boolean resultHasACategory = drop instanceof ResultDrop
							&& ((ResultDrop) drop).getCategory() != null;
					if (resultHasACategory || drop instanceof InfoDrop
							|| drop instanceof PleaseFolderize) {
						ContentJavaContext context = new ContentJavaContext(
								item);
						if (context.complete) {
							hasJavaContext = true;
							String packageKey = context.packageName;
							String typeKey = context.typeName;
							Map<String, Content> typeToFolder = packageToClassToFolder
									.get(packageKey);
							if (typeToFolder == null) {
								typeToFolder = new HashMap<String, Content>();
								packageToClassToFolder.put(packageKey,
										typeToFolder);
							}
							Content folder = typeToFolder.get(typeKey);
							if (folder == null) {
								// create the class/type folder, save it in the
								// map
								folder = new Content(typeKey);
								folder
										.setBaseImageName(context.typeIsAnInterface ? CommonImages.IMG_INTERFACE
												: CommonImages.IMG_CLASS);
								typeToFolder.put(typeKey, folder);
							}
							folder.addChild(item);
						}
					}
				}
				/*
				 * If we couldn't figure out the package and class just add the
				 * drop back into the children.
				 */
				if (!hasJavaContext) {
					newChildren.add(item);
				}
			}
		}

		/*
		 * Create the package folders and add associated type folders into it.
		 */
		for (Iterator<String> i = packageToClassToFolder.keySet().iterator(); i
				.hasNext();) {
			String packageKey = i.next();
			Map<?, Content> typeToFolder = packageToClassToFolder
					.get(packageKey);

			Content packageFolder = new Content(packageKey, typeToFolder
					.values());
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
		 * Replace the children of the node parameter with the new children
		 * created within this method
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
				packageTypeFolderizeRecursive(item, false, existingFolderSet,
						contentsOnPathToRoot);
				contentsOnPathToRoot.remove(item);
			}
		}
	}

	private void setConsistencyDecoratorForATypeFolder(Content c) {
		boolean hasAResult = false;
		boolean consistent = true;
		boolean hasRedDot = false;
		for (Content node : c.children()) {
			Drop d = node.f_referencedDrop;
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
			flags |= (consistent ? CoE_Constants.CONSISTENT
					: CoE_Constants.INCONSISTENT);

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
			flags |= (consistent ? CoE_Constants.CONSISTENT
					: CoE_Constants.INCONSISTENT);

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
	 * static class InfoWarning { boolean isInfo = false; boolean isInfoWarning
	 * = false; }
	 */

	/**
	 * Only called by nodeNeedsWarningDecorator and itself
	 * 
	 * Changed to use Content node itself to store status, instead of passing
	 * InfoWarning Changed to track all nodes already visited
	 */
	private void nodeNeedsWarningDecorator(Content node, Set<Content> onPath) {
		node.f_isInfoDecorated = node.f_isInfo;
		node.f_isInfoWarningDecorate = node.f_isInfoWarning;

		if (node.f_referencedDrop instanceof PleaseCount) {
			node.setCount(((PleaseCount) node.f_referencedDrop).count());
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
	private void breakBackEdges(Collection<Content> contentRoot) {
		// fake out the recursive function by pretending the root is a Content
		// node
		Content root = new Content("", contentRoot);
		breakBackEdges(root, new HashSet<Content>(),
				new HashMap<Content, Content>());
	}

	/**
	 * Converts back edges into leaf nodes Also converts the children to arrays
	 * 
	 * @param onPath
	 *            Nodes already encountered on the path here
	 * @param leaves
	 *            Leaves previously created
	 */
	private void breakBackEdges(Content node, Set<Content> onPath,
			Map<Content, Content> leaves) {
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
		 * Tries to construct a full Java context, if this fails
		 * {@link #complete} will be <code>false</code>.
		 * 
		 * @param content
		 *            the viewer content item to obtain the Java context for
		 */
		public ContentJavaContext(final Content content) {
			// Get reference IRNode
			if (!(content.f_referencedDrop instanceof IRReferenceDrop))
				return;
			IRReferenceDrop drop = (IRReferenceDrop) content.f_referencedDrop;
			final IRNode node = drop.getNode();
			if (node == null) {
				return;
			}
			/*
			 * if (!node.equals(content.referencedLocation)) { LOG.warning("Node
			 * from ref drop != node in content: "+content.referencedLocation);
			 * }
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
					Iterator<IRNode> e = TypeDeclarations
							.getTypesIterator(types);
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
					typeName = JavaNames.getQualifiedTypeName(type);
					final Operator top = JavaNames.getOperator(type);
					typeIsAnInterface = InterfaceDeclaration.prototype
							.includes(top);
				} else if (isPkg) {
					typeName = "package";
					typeIsAnInterface = false;
				} else {
					LOG.severe("No enclosing type for: "
							+ DebugUnparser.toString(node));
				}
			} else if (TextFile.prototype.includes(op)) {
				typeName = TextFile.getId(node);
				packageName = null;
			} else {
				LOG.warning("Unable to get Java context for "
						+ DebugUnparser.toString(node));
			}
			complete = !typeName.equals(JavaNames.getTypeName(null));
		}
	}

	public IResultsViewContentProvider buildModelOfDropSea() {
		synchronized (ResultsViewContentProvider.class) {
			long viewTime = timeStamp;
			long seaTime = Sea.getDefault().getTimeStamp();
			if (seaTime == Sea.INVALIDATED) {
				seaTime = Sea.getDefault().updateConsistencyProof();
			}

			SLLogger.getLogger().fine(
					"Comparing view (" + viewTime + ") to sea (" + seaTime
							+ ")");
			if (viewTime != Sea.INVALIDATED && viewTime == seaTime) {
				return this;
			}
			SLLogger.getLogger().fine("Building model of Drop-Sea");
			IResultsViewContentProvider rv = buildModelOfDropSea_internal();
			timeStamp = Sea.getDefault().getTimeStamp();
			return rv;
		}
	}

	private static DropPredicate promisePred = DropPredicateFactory
			.matchType(PromiseDrop.class);

	private static DropPredicate scopedPromisePred = DropPredicateFactory
			.matchType(PromisePromiseDrop.class);

	/**
	 * Matches non-@Promise PromiseDrops
	 */
	private static DropPredicate predicate = new DropPredicate() {
		public boolean match(Drop d) {
			return promisePred.match(d) && !scopedPromisePred.match(d);
		}
	};

	@SuppressWarnings("unchecked")
	private IResultsViewContentProvider buildModelOfDropSea_internal() {
		// show at the viewer root
		Collection<Content> root = new HashSet<Content>();

		/*
		 * for (ModelDrop md : Sea.getDefault().getDropsOfType(ModelDrop.class))
		 * { System.out.println("ModelDrop: "+md.getMessage()); }
		 */

		final Set<? extends PromiseDrop> promiseDrops = Sea.getDefault()
				.getDropsOfType(PromiseDrop.class);
		for (PromiseDrop pd : promiseDrops) {
			if (pd.isFromSrc()) {
				if (!pd.hasMatchingDeponents(predicate)) {
					root.add(encloseDrop(pd));
				}
			}
		}

		final Set<? extends InfoDrop> infoDrops = Sea.getDefault()
				.getDropsOfType(InfoDrop.class);
		if (!infoDrops.isEmpty()) {
			final String msg = "Suggestions and warnings";
			Content infoFolder = new Content(msg);
			infoFolder.setCount(infoDrops.size());

			for (InfoDrop id : infoDrops) {
				infoFolder.addChild(encloseDrop(id));
			}
			infoFolder.setBaseImageName(CommonImages.IMG_INFO);
			infoFolder.f_isInfo = true;
			root.add(infoFolder);
		}

		final Set<? extends PromiseWarningDrop> promiseWarningDrops = Sea
				.getDefault().getDropsOfType(PromiseWarningDrop.class);
		if (!promiseWarningDrops.isEmpty()) {
			/*
			 * We have modeling problems...make sure the view that shows them is
			 * visible to the user.
			 */
			if (PreferenceConstants.prototype.getAutoOpenModelingProblemsView()) {
				final UIJob job = new SLUIJob() {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						ViewUtility.showView(ProblemsView.class.getName(),
								null, IWorkbenchPage.VIEW_VISIBLE);
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		}

		final Set<ResultDrop> resultDrops = Sea.getDefault().getDropsOfType(
				ResultDrop.class);
		for (ResultDrop id : resultDrops) {
			// only show result drops at the main level if they are not attached
			// to a promise drop or a result drop
			if (id.isValid()
					&& ((id.getChecks().isEmpty() && id.getTrusts().isEmpty()) || (id instanceof MaybeTopLevel && ((MaybeTopLevel) id)
							.requestTopLevel()))) {
				if (id.getCategory() == null) {
					id.setCategory(Category.getInstance("unparented drops"));
				}
				root.add(encloseDrop(id));
			}
		}
		root = categorize(root);
		root = packageTypeFolderize(root);
		propagateWarningDecorators(root);
		breakBackEdges(root);
		m_lastRoot = m_root;
		m_root = root.toArray();
		// reset our cache, for next time
		m_contentCache = new HashMap<Drop, Content>();

		return this;
	}

	public Object[] getLastElements() {
		synchronized (ResultsViewContentProvider.class) {
			return (isShowInferences() ? m_lastRoot : Content
					.filterNonInfo(m_lastRoot));
		}
	}
}