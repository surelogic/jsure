package edu.cmu.cs.fluid.dcf.views.coe;

import java.io.File;
import java.util.*;

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
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.ImportName;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.java.promise.TextFile;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.MaybeTopLevel;
import edu.cmu.cs.fluid.sea.drops.PleaseCount;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

abstract class GenericResultsViewContentProvider<T extends IDropInfo, C extends AbstractContent<T,C>> 
extends	AbstractResultsViewContentProvider {
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
		synchronized (GenericResultsViewContentProvider.class) {
			return getElementsInternal();
		}
	}

	protected Object[] getElementsInternal() {
		return (isShowInferences() ? m_root : AbstractContent.<T,C>filterNonInfo(m_root));
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
		if (parent instanceof AbstractContent<?,?>) {
			C item = (C) parent;
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
	 * Encloses in {@link C}items and adds each drop in
	 * <code>dropsToAdd</code> to the mutable set of viewer content items passed
	 * into this method.
	 * 
	 * @param mutableContentSet
	 *            A parent {@link C} object to add children to
	 * @param dropsToAdd
	 *            the set of drops to enclose and add to the content set
	 */
	protected void addDrops(C mutableContentSet,
			Set<? extends T> dropsToAdd) {
		for (T drop : dropsToAdd) {
			mutableContentSet.addChild(encloseDrop(drop));
		}
	}

	protected abstract C makeContent(String msg);
	protected abstract C makeContent(String msg, Collection<C> contentRoot);

	/**
	 * Create a {@link C}item for a drop-sea drop. This is only done once,
	 * hence, the same C item is returned if the same drop is passed to
	 * this method.
	 * 
	 * @param drop
	 *            the drop to enclose
	 * @return the content item the viewer can use
	 */
	protected abstract C encloseDrop(T drop);

	/**
	 * Adds categories to the graph of C nodes rooted contentRoot and
	 * returns the new set of root nodes.
	 * 
	 * @param contentRoot
	 *            root of a graph of C nodes
	 * @return the new set of C nodes (that should replace contentRoot)
	 */
	private Collection<C> categorize(Collection<C> contentRoot) {
		// fake out the recursive function by pretending the root is a C
		// node
		C root = makeContent("", contentRoot);
		categorizeRecursive(root, true, new HashSet<C>(),
				new HashSet<C>());
		return root.children();
	}

	/**
	 * Recursive method to categorize a graph of content nodes.
	 * 
	 * @param node
	 *            the C node to categorize
	 * @param atRoot
	 *            <code>true</code> if at the C root, <code>false</code>
	 *            otherwise (used to control categorization of promise drops)
	 * @param existingCategoryFolderSet
	 *            a running set tracking what folders we have created used to
	 *            ensure we don't categorize things we have already done
	 * @param level
	 *            hack to avoid an infinite loop
	 * 
	 * @see #categorize(Set)
	 */
	private void categorizeRecursive(C node, boolean atRoot,
			Set<C> existingCategoryFolderSet,
			Set<C> contentsOnPathToRoot) {
		Set<C> categorizedChildren = new HashSet<C>();
		Set<C> toBeCategorized = new HashSet<C>();
		Map<Category, C> categoryToFolder = new HashMap<Category, C>();
		for (C item : node.children()) {
			if (existingCategoryFolderSet.contains(item)) {
				/*
				 * This is a previously created folder (went around the loop) so
				 * just add it to the resulting C set. Do not add it to
				 * the worklist to be categorized or an infinite loop will
				 * result.
				 */
				categorizedChildren.add(item);
			} else {
				toBeCategorized.add(item);
				final IDropInfo info = item.getDropInfo();
				if (info != null && info.isInstance(PromiseDrop.class)
						&& !atRoot
						&& !(info.isInstance(RequiresLockPromiseDrop.class))
						&& !(info.isInstance(PleaseFolderize.class))) {
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
						 * needed) and add this C item to it
						 */
						C categoryFolder = categoryToFolder
								.get(itemCategory);
						if (categoryFolder == null) {
							// create the category folder, save it in the map
							categoryFolder = makeContent(itemCategory
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
			Set<IDropInfo> proofDrops = new HashSet<IDropInfo>();
			Set<IDropInfo> warningDrops = new HashSet<IDropInfo>();
			Set<IDropInfo> infoDrops = new HashSet<IDropInfo>();

			for (C item : categoryFolder.children()) {
				if (item.getDropInfo().isInstance(ProofDrop.class)) {
					proofDrops.add(item.getDropInfo());
				} else if (item.getDropInfo().isInstance(InfoDrop.class)) {
					infoDrops.add(item.getDropInfo());
					if (item.getDropInfo().isInstance(WarningDrop.class)) {
						warningDrops.add(item.getDropInfo());
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
				for (IDropInfo proofDrop : proofDrops) {	
					choiceConsistent &= proofDrop.provedConsistent();
					if (proofDrop.isInstance(ResultDrop.class)) {
						localConsistent &= proofDrop.isConsistent();
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
		for (Iterator<C> k = toBeCategorized.iterator(); k.hasNext();) {
			C item = k.next();
			/*
			 * Guard against infinite recursion (drop-sea is a graph)
			 */
			if (!contentsOnPathToRoot.contains(item)) {
				/*
				 * Set<C> newCsOnPathToRoot = new
				 * HashSet<C>(contentsOnPathToRoot);
				 * newCsOnPathToRoot.add(item); categorizeRecursive(item,
				 * false, existingCategoryFolderSet, newCsOnPathToRoot);
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
	 *            root of a graph of C nodes
	 */
	private Collection<C> packageTypeFolderize(
			Collection<C> contentRoot) {
		// fake out the recursive function by pretending the root is a C
		// node
		C root = makeContent("", contentRoot);
		packageTypeFolderizeRecursive(root, true, new HashSet<C>(),
				new HashSet<C>());
		return root.children();
	}

	private void packageTypeFolderizeRecursive(C node, boolean atRoot,
			Set<C> existingFolderSet, Set<C> contentsOnPathToRoot) {
		Set<C> newChildren = new HashSet<C>();
		Set<C> toBeFolderized = new HashSet<C>();
		Map<String, Map<String, C>> packageToClassToFolder = new HashMap<String, Map<String, C>>();

		for (C item : node.children()) {
			if (existingFolderSet.contains(item)) {
				/*
				 * This is a previously created folder (went around the loop) so
				 * just add it to the resulting C set. Do not add it to
				 * the worklist to be categorized or an infinite loop will
				 * result.
				 */
				newChildren.add(item);
			} else {
				toBeFolderized.add(item);

				/*
				 * If the drop the C "item" references has a package and a
				 * type we'll generate folders for it.
				 */
				final IDropInfo drop = item.getDropInfo();
				boolean hasJavaContext = false;
				if (drop != null && (drop.isInstance(ResultDrop.class) || drop.isInstance(InfoDrop.class)
						|| drop.isInstance(PleaseFolderize.class))) {
					boolean resultHasACategory = drop.isInstance(ResultDrop.class)
							&& drop.getCategory() != null;
					if (resultHasACategory || drop.isInstance(InfoDrop.class)
							|| drop.isInstance(PleaseFolderize.class)) {
						ContentJavaContext<T,C> context = new ContentJavaContext<T,C>(item);
						if (context.complete) {
							hasJavaContext = true;
							String packageKey = context.packageName;
							String typeKey = context.typeName;
							Map<String, C> typeToFolder = packageToClassToFolder
									.get(packageKey);
							if (typeToFolder == null) {
								typeToFolder = new HashMap<String, C>();
								packageToClassToFolder.put(packageKey,
										typeToFolder);
							}
							C folder = typeToFolder.get(typeKey);
							if (folder == null) {
								// create the class/type folder, save it in the
								// map
								folder = makeContent(typeKey);
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
			Map<?, C> typeToFolder = packageToClassToFolder
					.get(packageKey);

			C packageFolder = makeContent(packageKey, typeToFolder
					.values());
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
		 * Replace the children of the node parameter with the new children
		 * created within this method
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
				 * Set<C> newCsOnPathToRoot = new
				 * HashSet<C>(contentsOnPathToRoot);
				 * newCsOnPathToRoot.add(item);
				 * packageTypeFolderizeRecursive(item, false, existingFolderSet,
				 * newCsOnPathToRoot);
				 */
				// Changed to add and then remove the item from the set
				contentsOnPathToRoot.add(item);
				packageTypeFolderizeRecursive(item, false, existingFolderSet,
						contentsOnPathToRoot);
				contentsOnPathToRoot.remove(item);
			}
		}
	}

	private void setConsistencyDecoratorForATypeFolder(C c) {
		boolean hasAResult = false;
		boolean consistent = true;
		boolean hasRedDot = false;
		for (C node : c.children()) {
			IDropInfo d = node.getDropInfo();
			if (d.isInstance(ProofDrop.class)) {
				hasAResult = true;
				consistent = consistent && d.provedConsistent();
				hasRedDot = hasRedDot || d.proofUsesRedDot();
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
			flags |= (consistent ? CoE_Constants.CONSISTENT
					: CoE_Constants.INCONSISTENT);

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
	 * static class InfoWarning { boolean isInfo = false; boolean isInfoWarning
	 * = false; }
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

		if (node.getDropInfo() != null && node.getDropInfo().isInstance(PleaseCount.class)) {
			node.setCount(node.getDropInfo().count());
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
	private int count(Collection<C> cc, Set<C> counted) {
		int i=0;
		for(C c : cc) {
			if (counted.contains(c)) {
				continue;
			}
			counted.add(c);
			i++;
			i += count(c.children(), counted);
		}
		return i;
	}
	*/
	
	/**
	 * Converts back edges into leaf nodes
	 */
	private void breakBackEdges(Collection<C> contentRoot) {
		//System.out.println("C count: "+count(contentRoot, new HashSet<C>()));
		
		// fake out the recursive function by pretending the root is a C
		// node
		C root = makeContent("", contentRoot);
		breakBackEdges(root, new HashSet<C>());
	}

	/**
	 * Converts back edges into leaf nodes Also converts the children to arrays
	 * 
	 * @param onPath
	 *            Nodes already encountered on the path here
	 * @param leaves
	 *            Leaves previously created
	 */
	private void breakBackEdges(C node, Set<C> onPath) {
		/*
		Integer count = counts.get(node);
		if (count == null) {
			counts.put(node, 1);
		} else {
			System.out.println(count+": "+node.getMessage());
			counts.put(node, count++);
		}
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
			//System.out.println("Looking at "+node.getMessage()+" -> "+item.getMessage());
			if (!onPath.contains(item)) {
				breakBackEdges(item, onPath);
			} else {
				// Need to replace with a leaf
				//System.out.println("Breaking backedge for: "+item.getMessage());
				C leaf = item.cloneAsLeaf();
				//System.out.println("Cloned: "+leaf.getMessage());
				children.set(i, leaf);
			}
		}
		node.resetChildren(children);
		// Now it creates a leaf, if I ever see it again
		//onPath.remove(node);
	}

	//Map<C,Integer> counts = new HashMap<C, Integer>();
	
	static private class ContentJavaContext<T extends IDropInfo, C extends AbstractContent<T,C>> {
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
		public ContentJavaContext(final C content) {
			final IDropInfo info = content.getDropInfo();		
			// Get reference IRNode
			if (!info.isInstance(IRReferenceDrop.class)) {
				final ISrcRef ref = info.getSrcRef();
				if (ref != null) {
					packageName = ref.getPackage();
					int lastSeparator = ref.getCUName().lastIndexOf(File.separator);
					typeName = lastSeparator < 0 ? ref.getCUName() : ref.getCUName().substring(lastSeparator+1);					
					return;
				}	
				return;
			}
			IRReferenceDrop ird = info.getAdapter(IRReferenceDrop.class);
			final IRNode node = ird.getNode();
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
			final boolean isPkg = PackageDeclaration.prototype.includes(op) || 
			                      ImportName.prototype.includes(op);

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
					type = VisitUtil.getPrimaryType(node);				
				} else if (!isPkg) {
					type = VisitUtil.getClosestType(node);
					while (type != null && JJNode.tree.getOperator(type) instanceof CallInterface) {
						type = VisitUtil.getEnclosingType(type);
					}
				}
				if (type != null) {
					typeName = JavaNames.getRelativeTypeName(type);
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
			} else if (node.identity() == IRNode.destroyedNode) {
				System.out.println("Ignoring destroyed node: "
						+ content.getDropInfo().getMessage());
			} else {
				LOG.warning("Unable to get Java context for "
						+ DebugUnparser.toString(node));
			}
			complete = !typeName.equals(JavaNames.getTypeName(null));
		}
	}

	public IResultsViewContentProvider buildModelOfDropSea() {
		synchronized (GenericResultsViewContentProvider.class) {
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

	/**
	 * Promise/InfoDrops
	 */
	protected abstract void buildModelFromDrops(Collection<C> root);
	protected abstract void buildModelForResultDrops(Collection<C> root);
	protected abstract boolean dropsExist(Class<? extends Drop> type);
	
	private IResultsViewContentProvider buildModelOfDropSea_internal() {
		// show at the viewer root
		Collection<C> root = new HashSet<C>();

		buildModelFromDrops(root);

		if (dropsExist(PromiseWarningDrop.class)) {
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

		if (dropsExist(ProposedPromiseDrop.class)) {
			/*
			 * We have modeling problems...make sure the view that shows them is
			 * visible to the user.
			 */
			if (PreferenceConstants.prototype.getAutoOpenProposedPromiseView()) {
				final UIJob job = new SLUIJob() {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						ViewUtility.showView(ProposedPromiseView.class
								.getName(), null, IWorkbenchPage.VIEW_VISIBLE);
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		}

		buildModelForResultDrops(root);
		
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

	protected static <T extends IDropInfo> boolean shouldBeTopLevel(T d) {
		// System.out.println("???: "+d.getMessage());
		return d != null && d.isInstance(MaybeTopLevel.class)
				&& d.requestTopLevel();
	}

	public Object[] getLastElements() {
		synchronized (GenericResultsViewContentProvider.class) {
			return (isShowInferences() ? m_lastRoot : AbstractContent
					.<T,C>filterNonInfo(m_lastRoot));
		}
	}
}