package com.surelogic.jsure.client.eclipse.editors;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ITypeHierarchyViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.NullAnnotationParseRule;
import com.surelogic.annotation.rules.AnnotationRules.Attribute;
import com.surelogic.annotation.rules.ScopedPromiseRules;
import com.surelogic.annotation.rules.ThreadEffectsRules;
import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.CommonImages;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.text.XMLLineStyler;
import com.surelogic.common.ui.views.AbstractContentProvider;
import com.surelogic.jsure.client.eclipse.dialogs.LibraryAnnotationDialog;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.AbstractJavaElementVisitor;
import com.surelogic.xml.AnnotatedJavaElement;
import com.surelogic.xml.AnnotationElement;
import com.surelogic.xml.ClassElement;
import com.surelogic.xml.FunctionParameterElement;
import com.surelogic.xml.IJavaElement;
import com.surelogic.xml.IMergeableElement;
import com.surelogic.xml.IXmlProcessor;
import com.surelogic.xml.MethodElement;
import com.surelogic.xml.NestedClassElement;
import com.surelogic.xml.PackageAccessor;
import com.surelogic.xml.PackageElement;
import com.surelogic.xml.PromisesXMLMerge;
import com.surelogic.xml.PromisesXMLParser;
import com.surelogic.xml.PromisesXMLReader;
import com.surelogic.xml.PromisesXMLWriter;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Pair;

public class PromisesXMLEditor extends MultiPageEditorPart implements
		PromisesXMLReader.Listener {
	enum FileStatus {
		READ_ONLY,
		/** Mutable, but saves to a local file */
		FLUID,
		/** Mutable in the usual way */
		LOCAL
	}

	public static final boolean hideEmpty = false;

	private final PromisesXMLContentProvider provider = new PromisesXMLContentProvider(
			hideEmpty);
	private static final JavaElementProvider jProvider = new JavaElementProvider();
	private final ParameterProvider paramProvider = new ParameterProvider();
	private static final AnnoProvider annoProvider = new AnnoProvider();
	private TreeViewer contents;

	/**
	 * Really used to check if we deleted all the changes
	 */
	private boolean isDirty = false;
	private TextViewer fluidXML;
	private TextEditor localXML;

	public PromisesXMLEditor() {
		PromisesXMLReader.listenForRefresh(this);
	}

	@Override
	protected void createPages() {
		createContentsPage(getContainer());
		int index = addPage(contents.getControl());
		setPageText(index, "Editor");

		createFluidXMLPage();
		createLocalXMLPage();
		updateTitle();
	}

	private void updateTitle() {
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	private void createContentsPage(final Composite parent) {
		contents = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION);
		contents.setContentProvider(provider);
		contents.setLabelProvider(provider);
		if (provider.getInput() != null) {
			contents.setInput(provider.getInput());
		}
		contents.getControl().addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(final MenuDetectEvent e) {
				final Menu menu = new Menu(contents.getControl().getShell(),
						SWT.POP_UP);
				setupContextMenu(menu);
				contents.getTree().setMenu(menu);
			}
		});
		// http://bingjava.appspot.com/snippet.jsp?id=2208
		contents.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				if (provider.isMutable()) {
					final IStructuredSelection s = (IStructuredSelection) event
							.getSelection();
					// System.out.println("Doubleclik on "+s.getFirstElement());
					// contents.editElement(s.getFirstElement(), 0);
					Object o = s.getFirstElement();
					if (o instanceof AnnotationElement) {
						AnnotationElement a = (AnnotationElement) o;
						if (a.canModify()) {
							startAnnotationEditDialog(a);
						} else {
							LibraryAnnotationDialog.cannotEdit(a.getLabel());
						}
					}
				}
			}
		});

		contents.setComparer(new Comparer());
		/*
		 * contents.setCellEditors(new CellEditor[] { new
		 * AnnotationCellEditor(contents.getTree()) });
		 * contents.setColumnProperties(new String[] { "col1" });
		 * contents.setCellModifier(new ICellModifier() {
		 * 
		 * @Override public boolean canModify(Object element, String property) {
		 * return provider.isMutable() && ((IJavaElement) element).canModify();
		 * }
		 * 
		 * @Override public Object getValue(Object element, String property) {
		 * //System.out.println("Getting value for "+element); return element;
		 * //return ((IJavaElement) element).getLabel(); }
		 * 
		 * @Override public void modify(Object element, String property, Object
		 * value) { Item i = (Item) element; IJavaElement e = (IJavaElement)
		 * i.getData(); //System.out.println("Setting value for "+e); boolean
		 * changed = e.modify((String) value, BalloonUtility.errorListener); if
		 * (changed) { contents.update(e, null); markAsDirty(); } } }); //
		 * http://eclipse.dzone.com/tips/treeviewer-two-clicks-edit
		 * TreeViewerEditor.create(contents, null, new
		 * ColumnViewerEditorActivationStrategy(contents) {
		 * 
		 * @Override protected boolean
		 * isEditorActivationEvent(ColumnViewerEditorActivationEvent e) {
		 * ViewerCell cell = (ViewerCell) e.getSource(); IJavaElement elt =
		 * (IJavaElement) cell.getElement();
		 * //System.out.println("Got eae for "+elt); return elt.canModify(); }
		 * }, ColumnViewerEditor.DEFAULT);
		 */
		/*
		 * //http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.jdt.doc.
		 * isv/guide/jdt_api_render.htm contents.setContentProvider(new
		 * StandardJavaElementContentProvider(true));
		 * contents.setLabelProvider(new JavaElementLabelProvider());
		 */
	}

	private void createFluidXMLPage() {
		fluidXML = new TextViewer(getContainer(), SWT.V_SCROLL | SWT.H_SCROLL);
		fluidXML.setDocument(provider.getFluidDocument());
		fluidXML.getTextWidget().setFont(JFaceResources.getTextFont());
		fluidXML.getTextWidget().addLineStyleListener(new XMLLineStyler());
		fluidXML.setEditable(false);

		int index = addPage(fluidXML.getControl());
		setPageText(index, "Baseline");
	}

	private void createLocalXMLPage() {
		final IEditorInput input = provider.getLocalInput();
		if (input == null) {
			return;
		}
		try {
			localXML = new TextEditor() {
				@Override
				public void createPartControl(final Composite parent) {
					super.createPartControl(parent);
					getSourceViewer().getTextWidget().addLineStyleListener(
							new XMLLineStyler());
				}

				@Override
				public boolean isEditable() {
					return false;
				}
			};

			int index = addPage(localXML, input);
			setPageText(index, "Diffs");
		} catch (PartInitException e) {
			SLLogger.getLogger().log(Level.WARNING,
					"Error creating source page for " + input.getToolTipText(),
					e);
		}
	}

	@Override
	public void init(final IEditorSite site, final IEditorInput input) {
		setSite(site);
		setInput(input);
		if (input instanceof IURIEditorInput) {
			IURIEditorInput f = (IURIEditorInput) input;
			if (contents != null) {
				contents.setInput(f.getURI());
			} else {
				provider.inputChanged(contents, null, f.getURI());
				if (f instanceof Input) {
					Input i = (Input) f;
					if (i.readOnly) {
						provider.markAsReadOnly();
					}
				}
			}
		}
		setPartName(input.getName());
	}

	@Override
	public void setFocus() {
		// System.out.println("Focus on "+getActivePage());
		switch (getActivePage()) {
		case 0:
			if (contents != null) {
				contents.getControl().setFocus();
			}
			break;
		case 1:
			if (fluidXML != null) {
				fluidXML.getControl().setFocus();
			}
			break;
		case 2:
			if (localXML != null) {
				localXML.setFocus();
			}
			break;
		}
	}

	/*
	 * TODO public void gotoMarker(IMarker marker) { }
	 */

	@Override
	public void doSave(final IProgressMonitor monitor) {
		boolean wasDirty = isDirty();
		provider.save(monitor);
		localXML.doRevertToSaved();

		if (wasDirty) {
			isDirty = false;
			fireDirtyProperty();
			updateTitle(); // does this help refresh?
			PromisesXMLReader.refresh(provider.pkg);
		}
	}

	@Override
	public void doSaveAs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispose() {
		PromisesXMLReader.stopListening(this);

		if (isDirty()) {
			// Nuke changes
			provider.deleteUnsavedChanges();
		}
		super.dispose();
	}

	@Override
	public boolean isDirty() {
		return isDirty || provider.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void fireDirtyProperty() {
		// This shouldn't be necessary, but Eclipse doesn't seem to
		// realize that the editor is dirty otherwise
		new SLUIJob() {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				firePropertyChange(IEditorPart.PROP_DIRTY);
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private void markAsDirty() {
		fireDirtyProperty();

		// otherwise already dirty
		syncLocalXMLEditor();
		PromisesXMLReader.refresh(provider.pkg);
	}

	private void markAsClean() {
		isDirty = false;
		fireDirtyProperty();
		PromisesXMLReader.refresh(provider.pkg);
	}

	private void syncLocalXMLEditor() {
		final IURIEditorInput input = (IURIEditorInput) provider
				.getLocalInput();
		final IDocument doc = localXML.getDocumentProvider().getDocument(input);
		if (doc != null) {
			// System.out.println(doc.get());
			StringWriter sw = new StringWriter(doc.getLength());
			PromisesXMLWriter pw = new PromisesXMLWriter(new PrintWriter(sw));
			PackageElement p = provider.pkg.cloneMe(null);
			if (PromisesXMLContentProvider.saveDiff) {
				p = PromisesXMLMerge.diff(p);
			}
			pw.write(p);

			final String updated = sw.toString();
			doc.set(updated);
			// System.out.println(updated);
		}
	}

	private void setupContextMenu(final Menu menu) {
		final IStructuredSelection s = (IStructuredSelection) contents
				.getSelection();
		if (s.size() != 1) {
			return;
		}
		final IJavaElement o = (IJavaElement) s.getFirstElement();
		addNavigationActions(menu, o);

		if (!provider.isMutable()) {
			return;
		}

		if (o instanceof AnnotatedJavaElement) {
			final AnnotatedJavaElement j = (AnnotatedJavaElement) o;
			makeMenuItem(menu, "Add Annotation...", new AnnotationCreator(j));

			/*
			 * if (o instanceof AbstractFunctionElement) { final
			 * AbstractFunctionElement f = (AbstractFunctionElement) o; if
			 * (f.getParams().length() > 0) { // Find out which parameters need
			 * to be added final String[] params = f.getSplitParams(); final
			 * List<Object> newParams = new ArrayList<Object>(); for(int i=0;
			 * i<params.length; i++) { final FunctionParameterElement p =
			 * f.getParameter(i); if (p == null) { newParams.add(new
			 * NewParameter(i,params[i])); } else { newParams.add(p); } } if
			 * (newParams.size() > 0) { makeMenuItem(menu, "Add parameter...",
			 * new SelectionAdapter() {
			 * 
			 * @Override public void widgetSelected(SelectionEvent e) {
			 * ListSelectionDialog d = new
			 * ListSelectionDialog(contents.getTree().getShell(),
			 * newParams.toArray(), paramProvider, paramProvider,
			 * "Select parameter(s) to add"); if (d.open() == Window.OK) {
			 * boolean changed = false; for(Object o : d.getResult()) { if (o
			 * instanceof NewParameter) { NewParameter np = (NewParameter) o;
			 * FunctionParameterElement p = new
			 * FunctionParameterElement(np.index); f.setParameter(p); changed =
			 * true; } } if (changed) { markAsDirty(); contents.refresh();
			 * contents.expandToLevel(f, 1); } } } }); } } } else
			 */
			if (o instanceof ClassElement) {
				final ClassElement c = (ClassElement) o;

				for (ScopedTargetType t : ScopedTargetType.values()) {
					makeMenuItem(menu, "Add Scoped Promise For " + t.label
							+ "...", new AnnotationCreator(j, t));
				}
				addActionsOnClasses(menu, c);
			}
		}

		if (o instanceof IMergeableElement) {
			addActionsForAnnotations(menu, o);
		}
		new MenuItem(menu, SWT.SEPARATOR);
		makeMenuItem(menu, "Revert All Changes", new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Shell s = contents.getTree().getShell();
				if (provider.pkg.isModified()) {
					if (MessageDialog.openQuestion(s, "Revert All Changes?",
							"Do you really want to revert all changes?")) {
						provider.deleteAllChanges();
						contents.refresh();
						contents.expandAll();
						/*
						 * localXML.doRevertToSaved(); markAsClean();
						 */
						isDirty = true;
						markAsDirty();
					}
				} else {
					MessageDialog.openInformation(s, "No Changes",
							"There are no changes to revert");
				}
			}
		});
	}

	private void addActionsForAnnotations(final Menu menu, final IJavaElement o) {
		if (o instanceof AnnotationElement) {
			final AnnotationElement a = (AnnotationElement) o;
			MenuItem m = makeMenuItem(menu, "Edit Annotation...",
					SLImages.getImage(CommonImages.IMG_ANNOTATION),
					new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent se) {
							startAnnotationEditDialog(a);
						}
					});
			m.setEnabled(a.canModify());

			MenuItem m2 = makeMenuItem(menu, "Revert",
					SLImages.getImage(CommonImages.IMG_ANNOTATION),
					new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent se) {
							a.revert();
							isDirty = true;
							markAsDirty();
						}
					});
			m2.setEnabled(a.canRevert());
		}
		final IMergeableElement me = (IMergeableElement) o;
		makeMenuItem(menu, "Delete", SLImages.getImage(CommonImages.IMG_RED_X),
				new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						final Shell s = contents.getTree().getShell();
						if (MessageDialog.openQuestion(
								s,
								"Delete Annotation?",
								"Do you really want to delete @"
										+ me.getLabel() + "?")) {
							final boolean origDirty = provider.pkg.isDirty();
							final boolean deletedNew = me.delete();
							if (deletedNew && !origDirty) {
								// Probably saved after creation, so we need to
								// save it
								isDirty = true;
							}
							markAsDirty();
							contents.refresh();
							contents.expandToLevel(me.getParent(), 1);
						}
					}
				});
	}

	private void addActionsOnClasses(final Menu menu, final ClassElement c) {
		/*
		 * makeMenuItem(menu, "Add existing method(s)...", new
		 * ITypeSelector<IMethod>(c, methodComparator) {
		 * 
		 * @Override protected void preselect(IType t) throws JavaModelException
		 * { for(IMethod m : t.getMethods()) { final boolean omitted =
		 * Flags.isSynthetic(m.getFlags()) ||
		 * "<clinit>".equals(m.getElementName()) ||
		 * m.getElementName().startsWith("access$"); if
		 * (m.getDeclaringType().equals(t) && !omitted) { // TODO check if
		 * already there? members.add(m); } } }
		 * 
		 * @Override protected void create(IMethod m) throws JavaModelException
		 * { String params = PromisesXMLBuilder.translateParameters(m);
		 * c.addMember(m.isConstructor() ? new ConstructorElement(params) : new
		 * MethodElement(m.getElementName(), params)); } }); makeMenuItem(menu,
		 * "Add existing nested type(s)...", new ITypeSelector<IType>(c,
		 * typeComparator) {
		 * 
		 * @Override protected void preselect(IType t) throws JavaModelException
		 * { for(IType nt : t.getTypes()) { // TODO check if already there?
		 * members.add(nt); } }
		 * 
		 * @Override protected void create(IType member) throws
		 * JavaModelException { c.addMember(new
		 * NestedClassElement(member.getElementName())); } });
		 */
	}

	abstract class ITypeSelector<T extends IMember> extends SelectionAdapter {
		final ClassElement c;
		final List<T> members = new ArrayList<T>();
		final Comparator<T> comparator;

		ITypeSelector(final ClassElement cls, final Comparator<T> compare) {
			c = cls;
			comparator = compare;
		}

		@Override
		public final void widgetSelected(final SelectionEvent e) {
			final IType t = findIType(c, "");
			if (t == null) {
				return;
			}
			ListSelectionDialog d;
			try {
				preselect(t);
				Collections.sort(members, comparator);
				d = new ListSelectionDialog(contents.getTree().getShell(),
						members.toArray(), jProvider, jProvider,
						"Select one or more of these");
				if (d.open() == Window.OK) {
					boolean changed = false;
					for (Object o : d.getResult()) {
						@SuppressWarnings("unchecked")
						T m = (T) o;
						create(m);
						changed = true;
					}
					if (changed) {
						markAsDirty();
						contents.refresh();
						// contents.refresh(c, true);
					}
				}
			} catch (JavaModelException e1) {
				e1.printStackTrace();
			}
		}

		protected abstract void preselect(final IType t)
				throws JavaModelException;

		protected abstract void create(T member) throws JavaModelException;
	}

	private static final Comparator<IMethod> methodComparator = new Comparator<IMethod>() {
		@Override
		public int compare(final IMethod o1, final IMethod o2) {
			int rv = o1.getElementName().compareTo(o2.getElementName());
			if (rv == 0) {
				rv = o1.getParameterTypes().length
						- o2.getParameterTypes().length;
			}
			if (rv == 0) {
				try {
					rv = o1.getSignature().compareTo(o2.getSignature());
				} catch (JavaModelException e) {
					// ignore
				}
			}
			return rv;
		}
	};

	private static final Comparator<IType> typeComparator = new Comparator<IType>() {
		@Override
		public int compare(final IType o1, final IType o2) {
			int rv = o1.getElementName().compareTo(o2.getElementName());
			return rv;
		}
	};

	private void addNavigationActions(final Menu menu, final IJavaElement o) {
		if (o instanceof ClassElement) {
			final ClassElement c = (ClassElement) o;
			makeMenuItem(menu, "Open Type Hierarchy", new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final IViewPart view = EclipseUIUtility
							.showView(JavaUI.ID_TYPE_HIERARCHY);
					if (view instanceof ITypeHierarchyViewPart) {
						final ITypeHierarchyViewPart v = (ITypeHierarchyViewPart) view;
						final IType t = findIType(c, "");
						if (t != null) {
							v.setInputElement(t);
						}
					}
				}
			});
		}
	}

	private class AnnotationCreator extends SelectionAdapter {
		final AnnotatedJavaElement j;
		final ScopedTargetType target;
		final boolean makeScopedPromise;

		AnnotationCreator(final AnnotatedJavaElement aje,
				final ScopedTargetType t) {
			j = aje;
			target = t;
			makeScopedPromise = t != null;
		}

		AnnotationCreator(final AnnotatedJavaElement aje) {
			this(aje, null);
		}

		@Override
		public void widgetSelected(final SelectionEvent e) {
			final List<String> annos;
			if (!makeScopedPromise) {
				annos = findMissingAnnos(j);
			} else {
				annos = sortSet(remove(findApplicableAnnos(target.op),
						ScopedPromiseRules.PROMISE));
			}
			ListSelectionDialog d = new ListSelectionDialog(contents.getTree()
					.getShell(), annos.toArray(), annoProvider, annoProvider,
					makeScopedPromise ? "Select scoped promise(s) to add for "
							+ target.label.toLowerCase()
							: "Select annotation(s) to add") {

				@Override
				protected void configureShell(Shell shell) {
					super.configureShell(shell);
					shell.setImage(SLImages
							.getImage(CommonImages.IMG_ANNOTATION));
					shell.setText("Add Annotation");
				}
			};
			if (d.open() == Window.OK) {
				boolean changed = false;
				for (Object o : d.getResult()) {
					final String tag = (String) o;
					final String contents = getDefaultContents(tag,
							makeScopedPromise);
					final AnnotationElement a;
					final Map<String, String> attrs = Collections
							.<String, String> emptyMap();
					if (makeScopedPromise) {
						a = new AnnotationElement(j, null,
								ScopedPromiseRules.PROMISE, "@" + tag
										+ contents + " for " + target.target,
								attrs);
					} else {
						a = new AnnotationElement(j, null, tag, contents, attrs);
					}
					// System.out.println("Created elt: "+a);
					j.addPromise(a);
					a.markAsModified();
					changed = true;

				}
				if (changed) {
					contents.refresh();
					contents.expandToLevel(j, 1);
					markAsDirty();
				}
			}
		}

		private String getDefaultContents(final String tag,
				final boolean isScopedPromise) {
			if (ThreadEffectsRules.STARTS.equals(tag)) {
				return isScopedPromise ? "(nothing)" : "nothing";
			}
			return "";
		}
	}

	private List<String> sortSet(final Set<String> s) {
		List<String> rv = new ArrayList<String>(s);
		Collections.sort(rv);
		return rv;
	}

	private Set<String> findApplicableAnnos(final Operator op) {
		final Set<String> annos = new HashSet<String>();
		// Get valid/applicable annos
		for (IAnnotationParseRule<?, ?> rule : PromiseFramework.getInstance()
				.getParseDropRules()) {
			if (!(rule instanceof NullAnnotationParseRule)
					&& rule.declaredOnValidOp(op)
					&& AnnotationElement.isIdentifier(rule.name())) {
				annos.add(rule.name());
			}
		}
		// These should never appear in XML files
		annos.remove(ScopedPromiseRules.ASSUME);
		return annos;
	}

	private Set<String> remove(final Set<String> s, final String elt) {
		s.remove(elt);
		return s;
	}

	private List<String> findMissingAnnos(final AnnotatedJavaElement j) {
		final Set<String> annos = findApplicableAnnos(j.getOperator());

		// Remove clashes
		for (AnnotationElement a : j.getPromises()) {
			// This will remove it if there should only be one of that kind
			annos.remove(a.getUid());
		}
		return sortSet(annos);
	}

	static class NewParameter {
		final int index;
		final String type;

		NewParameter(final int i, final String t) {
			index = i;
			type = t;
		}
	}

	static IType findIType(final ClassElement c, final String nameSoFar) {
		String typeName = nameSoFar.isEmpty() ? c.getName() : c.getName() + '.'
				+ nameSoFar;
		if (c instanceof NestedClassElement) {
			ClassElement parent = (ClassElement) c.getParent();
			return findIType(parent, typeName);
		} else { // top-level
			PackageElement pkg = (PackageElement) c.getParent();
			return JDTUtility.findIType(null, pkg.getName(), typeName);
		}
	}

	static MenuItem makeMenuItem(final Menu menu, final String label,
			final SelectionListener l) {
		return makeMenuItem(menu, label, null, l);
	}

	static MenuItem makeMenuItem(final Menu menu, final String label,
			final Image image, final SelectionListener l) {
		MenuItem item1 = new MenuItem(menu, SWT.PUSH);
		item1.setText(label);
		item1.addSelectionListener(l);
		if (image != null) {
			item1.setImage(image);
		}
		return item1;
	}

	static class JavaElementProvider extends AbstractContentProvider {
		@Override
		public Object[] getElements(final Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public String getText(final Object element) {
			org.eclipse.jdt.core.IJavaElement e = (org.eclipse.jdt.core.IJavaElement) element;
			if (e instanceof IMethod) {
				IMethod m = (IMethod) e;
				try {
					if (m.isConstructor()) {
						return "new " + m.getElementName() + '('
								+ PromisesXMLBuilder.translateParameters(m)
								+ ')';
					}
					return m.getElementName() + '('
							+ PromisesXMLBuilder.translateParameters(m) + ')';
				} catch (JavaModelException e1) {
					// ignore
				}
				return m.getElementName() + "(???)";
			}
			return e.getElementName();
		}
	}

	class ParameterProvider extends AbstractContentProvider {
		@Override
		public Object[] getElements(final Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public String getText(final Object element) {
			if (element instanceof NewParameter) {
				NewParameter p = (NewParameter) element;
				return FunctionParameterElement.PREFIX + (p.index + 1) + " : "
						+ p.type;
			}
			FunctionParameterElement p = (FunctionParameterElement) element;
			return p.getLabel();
		}

		@Override
		public Color getForeground(final Object element) {
			if (element instanceof FunctionParameterElement) {
				return contents.getControl().getDisplay()
						.getSystemColor(SWT.COLOR_GRAY);
			}
			return null;
		}
	}

	static class AnnoProvider extends AbstractContentProvider {
		@Override
		public Object[] getElements(final Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public String getText(final Object element) {
			return element.toString();
		}
	}

	public void focusOnMethod(final String name, final String params) {
		PackageElement p = provider.pkg;
		if (p != null) {
			final MethodElement m = p.visit(new MethodFinder(name, params));
			if (m != null) {
				focusOn(m);
			}
		}
	}

	public void focusOnNestedType(final String relativeName) {
		PackageElement p = provider.pkg;
		if (p != null) {
			final NestedClassElement m = p.visit(new TypeFinder(relativeName));
			if (m != null) {
				focusOn(m);
			}
		}
	}

	abstract static class ElementFinder<T> extends
			AbstractJavaElementVisitor<T> {
		ElementFinder() {
			super(null);
		}

		@Override
		protected T combine(final T old, final T result) {
			if (old != null) {
				return old;
			}
			return result;
		}
	}

	static class TypeFinder extends ElementFinder<NestedClassElement> {
		final String[] names;
		final Stack<String> types = new Stack<String>();

		TypeFinder(final String relativeName) {
			names = relativeName.split("\\.");
		}

		@Override
		public NestedClassElement visit(final NestedClassElement e) {
			types.clear();

			// Find out where this is
			NestedClassElement here = e;
			while (here != null) {
				types.push(here.getName());

				IJavaElement p = here.getParent();
				if (p instanceof NestedClassElement) {
					here = (NestedClassElement) p;
				} else {
					break;
				}
			}
			// Compare against names
			for (String name : names) {
				if (types.isEmpty()) {
					return null; // No match
				}
				String type = types.pop();
				if (!name.equals(type)) {
					return null; // No match
				}
			}
			if (!types.isEmpty()) {
				return null; // Still something to match
			}
			return e;
		}
	}

	static class MethodFinder extends ElementFinder<MethodElement> {
		final String name, params;

		MethodFinder(final String name, final String params) {
			this.name = name;
			this.params = params;
		}

		@Override
		public MethodElement visit(final MethodElement m) {
			if (name.equals(m.getName())) {
				if (params == null || params.equals(m.getParams())) {
					return m;
				}
			}
			return null;
		}
	}

	static class XmlMap extends HashMap<String, Collection<String>> implements
			IXmlProcessor {
		private static final long serialVersionUID = 1L;
		private final boolean makeUnique;

		XmlMap(final boolean makeUnique) {
			this.makeUnique = makeUnique;
		}

		private Collection<String> getPkg(final String qname) {
			Collection<String> c = get(qname);
			if (c == null) {
				c = makeUnique ? new HashSet<String>(4)
						: new ArrayList<String>(2);
				put(qname, c);
			}
			return c;
		}

		@Override
		public void addPackage(final String qname) {
			Collection<String> c = getPkg(qname);
			c.add(qname);
		}

		@Override
		public void addType(final String pkg, final String name) {
			Collection<String> c = getPkg(pkg);
			c.add(name);
		}
	}

	/**
	 * @return a map of packages to qualified names
	 */
	public static Map<String, Collection<String>> findAllPromisesXML() {
		return findLocalPromisesXML(true);
	}

	public static Map<String, Collection<String>> findLocalPromisesXML() {
		return findLocalPromisesXML(false);
	}

	private static Map<String, Collection<String>> findLocalPromisesXML(
			final boolean includeFluid) {
		final XmlMap map = new XmlMap(true);
		if (includeFluid) {
			final File xml = PromisesXMLParser.getFluidXMLDir();
			PackageAccessor.findPromiseXMLsInDir(map, xml);
		}
		final File localXml = JSurePreferencesUtility.getJSureXMLDirectory();
		if (localXml != null) {
			PackageAccessor.findPromiseXMLsInDir(map, localXml);
		}
		return map;
	}

	/**
	 * @return non-null Pair of files for fluid and local
	 */
	private static Pair<File, File> findPromisesXML(final String path) {
		File fluid = null;
		File local = null;
		final File localXml = JSurePreferencesUtility.getJSureXMLDirectory();
		if (localXml != null) {
			File f = new File(localXml, path);
			/*
			 * if (f.isFile()) { local = f; }
			 */
			local = f;
		}
		// Try fluid
		final File xml = PromisesXMLParser.getFluidXMLDir();
		if (xml != null) {
			File f = new File(xml, path);
			/*
			 * if (f.isFile()) { fluid = f; }
			 */
			fluid = f;
		}
		return new Pair<File, File>(fluid, local);
	}

	public static IEditorPart openInEditor(final String path,
			final boolean readOnly) {
		final IEditorInput i = makeInput(path, readOnly);
		if (i == null) {// || !i.exists()) {
			return null;
		}
		return EclipseUIUtility.openInEditor(i,
				PromisesXMLEditor.class.getName());
	}

	public static IEditorInput makeInput(final String relativePath,
			final boolean readOnly) {
		try {
			if (!relativePath.endsWith(TestXMLParserConstants.SUFFIX)) {
				return null;
			}
			return new Input(relativePath, readOnly);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	private static class Input implements IURIEditorInput {
		private final boolean readOnly;
		private final String path;
		private final String name;
		private final URI uri;

		Input(final String relativePath, final boolean ro)
				throws URISyntaxException {
			readOnly = ro;
			path = relativePath;
			uri = new URI(path);

			final int lastSlash = path.lastIndexOf('/');
			if (lastSlash < 0) {
				name = path;
			} else {
				name = path.substring(lastSlash + 1);
			}
		}

		@Override
		public boolean exists() {
			final Pair<File, File> f = PromisesXMLParser.findPromisesXML(path);
			return f.first().isFile() || f.second().isFile();
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null; // TODO
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public IPersistableElement getPersistable() {
			return new IPersistableElement() {
				@Override
				public void saveState(final IMemento memento) {
					memento.putString(PromisesXMLFactory.PATH, path);
					memento.putBoolean(PromisesXMLFactory.READ_ONLY, readOnly);
				}

				@Override
				public String getFactoryId() {
					return PromisesXMLFactory.class.getName();
				}
			};
		}

		@Override
		public String getToolTipText() {
			return path;
		}

		@Override
		public Object getAdapter(
				@SuppressWarnings("rawtypes") final Class adapter) {
			if (adapter == Object.class) {
				return this;
			}
			return null;
		}

		@Override
		public URI getURI() {
			return uri;
		}

		@Override
		public boolean equals(final Object o) {
			if (o instanceof IURIEditorInput) {
				IURIEditorInput i = (IURIEditorInput) o;
				return uri.equals(i.getURI());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return path.hashCode();
		}
	}

	public void focusOn(final IJavaElement e) {
		contents.setSelection(new StructuredSelection(e));
		contents.reveal(e);
	}

	static class Comparer implements IElementComparer {
		@Override
		public int hashCode(final Object element) {
			if (element instanceof AnnotationElement) {
				return System.identityHashCode(element);
			}
			return element.hashCode();
		}

		@Override
		public boolean equals(final Object a, final Object b) {
			return a == b;
		}
	}

	@Override
	public void refresh(final PackageElement e) {
		if (e == provider.pkg) {
			contents.refresh();
			// Doesn't change what's saved on disk
		}
	}

	@Override
	public void refreshAll() {
		provider.build();
		contents.refresh();
		fluidXML.refresh();
		localXML.doRevertToSaved();
	}

	void startAnnotationEditDialog(final AnnotationElement a) {
		// Collect initial attribute values
		Map<Attribute, String> initialAttrs = new TreeMap<Attribute, String>();
		for (Map.Entry<String, Attribute> e : a.getAttributeDefaults()
				.entrySet()) {
			if (AnnotationConstants.VALUE_ATTR.equals(e.getKey())) {
				initialAttrs.put(e.getValue(), a.getContents());
			} else {
				// TODO push into AnnoElt?
				String value = a.getAttribute(e.getKey());
				if (value == null
						&& e.getValue().getDefaultValueOrNull() != null) {
					value = e.getValue().getDefaultValueOrNull(); // the default
				}
				initialAttrs.put(e.getValue(), value);
			}
		}
		Map<Attribute, String> changedAttrs = LibraryAnnotationDialog.edit(a,
				initialAttrs);
		if (!changedAttrs.isEmpty()) {
			boolean modified = false;
			// edit contents and attrs
			for (Map.Entry<Attribute, String> e : changedAttrs.entrySet()) {
				if (AnnotationConstants.VALUE_ATTR.equals(e.getKey().getName())) {
					// modified |=
					// a.modify(a.getPromise()+'('+changedAttrs.get(AnnotationConstants.VALUE_ATTR)+')',
					// null);
					modified |= a.modifyContents(e.getValue());
				} else {
					a.setAttribute(e.getKey().getName(), e.getValue());
					modified = true;
				}
			}
			if (modified) {
				isDirty = true;
				markAsDirty();
			}
		}
		/*
		 * // TODO Assumes all attributes are boolean ListSelectionDialog d =
		 * new ListSelectionDialog(contents.getTree().getShell(),
		 * a.getAttributeDefaults().keySet().toArray(), annoProvider,
		 * annoProvider, ""); final Set<String> initiallySet = new
		 * TreeSet<String>();
		 * 
		 * d.setInitialSelections(initiallySet.toArray()); if (d.open() ==
		 * Window.OK) { // Figure out which changed Set<String> nowUnset = new
		 * TreeSet<String>(initiallySet); Set<String> nowSet = new
		 * TreeSet<String>(); for(Object o : d.getResult()) { if
		 * (!nowUnset.remove(o)) { nowSet.add(o.toString()); } } // Update the
		 * attributes for(String s : nowUnset) { a.setAttribute(s, "false"); }
		 * for(String s : nowSet) { a.setAttribute(s, "true"); } if
		 * (!nowSet.isEmpty() || !nowUnset.isEmpty()) { markAsDirty(); } }
		 */
	}
}
