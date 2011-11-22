package com.surelogic.jsure.client.eclipse.editors;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.ITypeHierarchyViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.*;

import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.NullAnnotationParseRule;
import com.surelogic.annotation.rules.ScopedPromiseRules;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.*;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.text.XMLLineStyler;
import com.surelogic.common.ui.views.AbstractContentProvider;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.*;
import com.surelogic.xml.IJavaElement;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

public class PromisesXMLEditor extends MultiPageEditorPart implements PromisesXMLReader.Listener {
	enum FileStatus { 		
		READ_ONLY, 
		/** Mutable, but saves to a local file */
		FLUID, 
		/** Mutable in the usual way */
		LOCAL 
	}
	
	public static final boolean hideEmpty = false;
	
	private final PromisesXMLContentProvider provider = new PromisesXMLContentProvider(hideEmpty);
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

	private void createContentsPage(Composite parent) {
       contents = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
       contents.setContentProvider(provider);
       contents.setLabelProvider(provider);
       if (provider.getInput() != null) {
    	   contents.setInput(provider.getInput());
       }
       contents.getControl().addMenuDetectListener(new MenuDetectListener() {
    	   @Override
    	   public void menuDetected(MenuDetectEvent e) {
    		   final Menu menu = new Menu(contents.getControl().getShell(), SWT.POP_UP);
    		   setupContextMenu(menu);
    		   contents.getTree().setMenu(menu);
    	   }
       });
       // http://bingjava.appspot.com/snippet.jsp?id=2208
       contents.addDoubleClickListener(new IDoubleClickListener() {
    	   @Override
    	   public void doubleClick(DoubleClickEvent event) {
    		   if (provider.isMutable()) {
    			   final IStructuredSelection s = (IStructuredSelection) event.getSelection();
    			   //System.out.println("Doubleclik on "+s.getFirstElement());
    			   contents.editElement(s.getFirstElement(), 0);
    		   }
    	   }
       });
       /*
       TreeViewerColumn c = new TreeViewerColumn(contents, SWT.NONE);
       c.setLabelProvider(new ColumnLabelProvider() {
    	   @Override
    	   public Color getForeground(Object element) {
    		   return provider.getForeground(element);
    	   }
    	   @Override
    	   public Image getImage(Object element) {
    		   return provider.getImage(element);
    	   }
    	   @Override
    	   public String getText(Object element) {
    		   return provider.getText(element);
    	   }
       });
       c.setEditingSupport(new EditingSupport(contents) {		
    	   @Override
    	   protected void setValue(Object element, Object value) {
    		   // TODO Auto-generated method stub

    	   }

    	   @Override
    	   protected Object getValue(Object element) {
    		   // TODO Auto-generated method stub
    		   return null;
    	   }

    	   @Override
    	   protected CellEditor getCellEditor(Object element) {
    		   // TODO Auto-generated method stub
    		   return null;
    	   }

    	   @Override
    	   protected boolean canEdit(Object element) {
    		   // TODO Auto-generated method stub
    		   return false;
    	   }
       });	
       */ 
       contents.setComparer(new Comparer());
       contents.setCellEditors(new CellEditor[] { new AnnotationCellEditor(contents.getTree()) });
       contents.setColumnProperties(new String[] { "col1" });
       contents.setCellModifier(new ICellModifier() {
    	   @Override
        public boolean canModify(Object element, String property) {    		 
    		   return provider.isMutable() && ((IJavaElement) element).canModify();
    	   }
    	   @Override
        public Object getValue(Object element, String property) {
    		   //System.out.println("Getting value for "+element);
    		   return element;
    		   //return ((IJavaElement) element).getLabel();
    	   }
    	   @Override
        public void modify(Object element, String property, Object value) {
    		   Item i = (Item) element;
    		   IJavaElement e = (IJavaElement) i.getData();
    		   //System.out.println("Setting value for "+e);
    		   boolean changed = e.modify((String) value, BalloonUtility.errorListener);
    		   if (changed) {
    			   contents.update(e, null);
    			   markAsDirty();
    		   }
    	   }
       });       
       // http://eclipse.dzone.com/tips/treeviewer-two-clicks-edit
       TreeViewerEditor.create(contents, null, new ColumnViewerEditorActivationStrategy(contents) {
    	   @Override
    	   protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent e) {    	
    		   ViewerCell cell = (ViewerCell) e.getSource();
    		   IJavaElement elt = (IJavaElement) cell.getElement();
    		   //System.out.println("Got eae for "+elt);
    		   return elt.canModify();
    	   }
       }, ColumnViewerEditor.DEFAULT);
       
       /*
       //http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.jdt.doc.isv/guide/jdt_api_render.htm
       contents.setContentProvider(new StandardJavaElementContentProvider(true));
       contents.setLabelProvider(new JavaElementLabelProvider());
       */
    }
    
	private void createFluidXMLPage() {
		fluidXML = new TextViewer(getContainer(), SWT.V_SCROLL | SWT.H_SCROLL);
		fluidXML.setDocument(provider.getFluidDocument());
		fluidXML.getTextWidget().setFont(JFaceResources.getTextFont());
		fluidXML.getTextWidget().addLineStyleListener(new XMLLineStyler());
		fluidXML.setEditable(false);		
		
		int index = addPage(fluidXML.getControl());
		setPageText(index, "Source");
	}
	
	private void createLocalXMLPage() {
		final IEditorInput input = provider.getLocalInput();
		if (input == null) {
			return;
		}
		try {
			localXML = new TextEditor() {
				@Override 
				public void createPartControl(Composite parent) {
					super.createPartControl(parent);
					getSourceViewer().getTextWidget().addLineStyleListener(new XMLLineStyler());
				}
				@Override
				public boolean isEditable() {
					return false;
				}
			};
			
			int index = addPage(localXML, input);
			setPageText(index, "Diffs");
		} catch (PartInitException e) {
			SLLogger.getLogger().log(Level.WARNING, "Error creating source page for "+input.getToolTipText(), e);
		}
	}
	
    @Override
    public void init(IEditorSite site, IEditorInput input) {
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
    	//System.out.println("Focus on "+getActivePage());
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
	
    /* TODO
    public void gotoMarker(IMarker marker) {
    }
    */
    
	@Override
	public void doSave(IProgressMonitor monitor) {
		provider.save(monitor);		
		localXML.doRevertToSaved();
		
		if (isDirty()) {
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
			public IStatus runInUIThread(IProgressMonitor monitor) {
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
		final IURIEditorInput input = (IURIEditorInput) provider.getLocalInput();	
		final IDocument doc = localXML.getDocumentProvider().getDocument(input);
		if (doc != null) {
			System.out.println(doc.get());
			StringWriter sw = new StringWriter(doc.getLength());
			PromisesXMLWriter pw = new PromisesXMLWriter(new PrintWriter(sw));
			PackageElement p = provider.pkg.cloneMe();
			if (PromisesXMLContentProvider.saveDiff) {
				p = PromisesXMLMerge.diff(p);
			}
			pw.write(p);
			
			final String updated = sw.toString();			
			doc.set(updated);
			System.out.println(updated);
		}
	}
	
	/*
	private CommentElement makeComment() {
        CommentElement c = CommentElement.make("...");
        c.markAsDirty();
        markAsDirty();
        return c;
	}
	*/
	
	private void setupContextMenu(final Menu menu) {
		final IStructuredSelection s = (IStructuredSelection) contents.getSelection();
		if (s.size() != 1) {
			return;
		}
		final IJavaElement o = (IJavaElement) s.getFirstElement();
		addNavigationActions(menu, o);
		
		if (!provider.isMutable()) {
			return;
		}
		/*
		if (o instanceof CommentElement) {
			final CommentElement c0 = (CommentElement) o;
		    makeMenuItem(menu, "Add comment above this", new SelectionAdapter() {
		        @Override
		        public void widgetSelected(SelectionEvent e) {              
		        	CommentedJavaElement cje = (CommentedJavaElement) c0.getParent();
		            CommentElement c = makeComment();
		            boolean success = cje.addCommentBefore(c, c0);
		            if (success) {
		            	contents.refresh();
		            	contents.expandToLevel(cje, 1);
		            } else {
		            	SLLogger.getLogger().warning("Couldn't add comment before '"+c0.getLabel()+"' in "+cje.getLabel());
		            }
		        }
		    });
		    makeMenuItem(menu, "Add comment below this", new SelectionAdapter() {
		        @Override
		        public void widgetSelected(SelectionEvent e) {              
		        	CommentedJavaElement cje = (CommentedJavaElement) c0.getParent();
		            CommentElement c = makeComment();
		            boolean success = cje.addCommentAfter(c, c0);
		            if (success) {
		            	contents.refresh();
		            	contents.expandToLevel(cje, 1);
		            } else {
		            	SLLogger.getLogger().warning("Couldn't add comment after '"+c0.getLabel()+"' in "+cje.getLabel());
		            }
		        }
		    });
		}		
		if (o instanceof CommentedJavaElement) {
		    final CommentedJavaElement cje = (CommentedJavaElement) o;
		    makeMenuItem(menu, "Append comment", new SelectionAdapter() {
		        @Override
		        public void widgetSelected(SelectionEvent e) {                
		            CommentElement c = makeComment();
		            cje.addComment(c);
		            contents.refresh();
		            contents.expandToLevel(cje, 1);
		        }
		    });
		}
		*/
		if (o instanceof AnnotatedJavaElement) {
			final AnnotatedJavaElement j = (AnnotatedJavaElement) o;
			makeMenuItem(menu, "Add annotation...", new AnnotationCreator(j));
		
			if (o instanceof AbstractFunctionElement) {
				final AbstractFunctionElement f = (AbstractFunctionElement) o;
				if (f.getParams().length() > 0) {
					// Find out which parameters need to be added
					final String[] params = f.getSplitParams();
					final List<Object> newParams = new ArrayList<Object>();
					for(int i=0; i<params.length; i++) {
						final FunctionParameterElement p = f.getParameter(i);
						if (p == null) {
							newParams.add(new NewParameter(i,params[i]));
						} else {
							newParams.add(p);
						}
					}
					if (newParams.size() > 0) {
						makeMenuItem(menu, "Add parameter...", new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {						
								ListSelectionDialog d = new 
								    ListSelectionDialog(contents.getTree().getShell(), newParams.toArray(), 
								    		paramProvider, paramProvider, "Select parameter(s) to add");
								if (d.open() == Window.OK) {
									boolean changed = false;
									for(Object o : d.getResult()) {
										if (o instanceof NewParameter) {
											NewParameter np = (NewParameter) o;
											FunctionParameterElement p = new FunctionParameterElement(np.index);	
											f.setParameter(p);
											changed = true;
										}
									}
									if (changed) {
										markAsDirty();
										contents.refresh();
										contents.expandToLevel(f, 1);
									}
								}
							}
						});
					}
				}
			} 
			else if (o instanceof ClassElement) {
				final ClassElement c = (ClassElement) o;
				
				for(ScopedTargetType t : ScopedTargetType.values()) {
					makeMenuItem(menu, "Add scoped promise for "+t.label+"...", new AnnotationCreator(j, t));
				}
				addActionsOnClasses(menu, c);
			}
		}
		if (o instanceof IMergeableElement) {
			final IMergeableElement me = (IMergeableElement) o;
			makeMenuItem(menu, "Delete", new SelectionAdapter() {
		        @Override
		        public void widgetSelected(SelectionEvent e) {          
		        	final boolean origDirty = provider.pkg.isDirty();
		        	final boolean deletedNew = me.delete();
		        	if (deletedNew && !origDirty) {
		        		// Probably saved after creation, so we need to save it
		        		isDirty = true;
		        	}
		        	markAsDirty();
		          	contents.refresh();
	            	contents.expandToLevel(me.getParent(), 1);
		        }
			});
		}
		new MenuItem(menu, SWT.SEPARATOR);
		makeMenuItem(menu, "Delete All Changes", new SelectionAdapter() {
	        @Override
	        public void widgetSelected(SelectionEvent e) {   			      
	        	final Shell s = contents.getTree().getShell();
	        	if (provider.pkg.isModified()) {	        	
	        		if (MessageDialog.openQuestion(s, "Delete All Changes?", 
	        				"Do you really want to delete all changes?")) {
	        			provider.deleteAllChanges();
	        			contents.refresh();
	        			contents.expandAll();
	        			/*
	        			localXML.doRevertToSaved();			        	
	        			markAsClean();
	        			*/
	        			isDirty = true;
	        			markAsDirty();
	        		}
	        	} else {
	        		MessageDialog.openInformation(s, "No Changes", "There are no changes to delete");
	        	}
	        }
		});
	}

	private void addActionsOnClasses(final Menu menu, final ClassElement c) {
		makeMenuItem(menu, "Add existing method(s)...", new ITypeSelector<IMethod>(c, methodComparator) {
			@Override
			protected void preselect(IType t) throws JavaModelException {
				for(IMethod m : t.getMethods()) {					
					final boolean omitted = Flags.isSynthetic(m.getFlags()) || 
						"<clinit>".equals(m.getElementName()) ||
						m.getElementName().startsWith("access$");						
					if (m.getDeclaringType().equals(t) && !omitted) {
						// TODO check if already there?
						members.add(m);
					}
				}
			}
			@Override
			protected void create(IMethod m) throws JavaModelException {
				String params = PromisesXMLBuilder.translateParameters(m);									
				c.addMember(m.isConstructor() ? new ConstructorElement(params) : 
					                            new MethodElement(m.getElementName(), params));
			}
		});
		makeMenuItem(menu, "Add existing nested type(s)...", new ITypeSelector<IType>(c, typeComparator) {
			@Override
			protected void preselect(IType t) throws JavaModelException {
				for(IType nt : t.getTypes()) {
					// TODO check if already there?
					members.add(nt);
				}
			}
			@Override
			protected void create(IType member) throws JavaModelException {
				c.addMember(new NestedClassElement(member.getElementName()));
			}
		});
	}
	
	abstract class ITypeSelector<T extends IMember> extends SelectionAdapter {
		final ClassElement c;
		final List<T> members = new ArrayList<T>();
		final Comparator<T> comparator;

		ITypeSelector(ClassElement cls, Comparator<T> compare) {
			c = cls;
			comparator = compare;
		}
		
		@Override
		public final void widgetSelected(SelectionEvent e) {
			final IType t = findIType(c, "");						
			if (t == null) {
				return;
			}
			ListSelectionDialog d;
			try {		
				preselect(t);
				Collections.sort(members, comparator);
				d = new ListSelectionDialog(contents.getTree().getShell(), members.toArray(), 
						                    jProvider, jProvider, "Select one or more of these");
				if (d.open() == Window.OK) {
					boolean changed = false;
					for(Object o : d.getResult()) {
						@SuppressWarnings("unchecked")
						T m = (T) o;
						create(m);
						changed = true;
					}
					if (changed) {
						markAsDirty();
						contents.refresh();
						//contents.refresh(c, true);
					}
				}
			} catch (JavaModelException e1) {
				e1.printStackTrace();
			}															
		}

		protected abstract void preselect(final IType t) throws JavaModelException;
		protected abstract void create(T member) throws JavaModelException;
	}
	
	private static final Comparator<IMethod> methodComparator = new Comparator<IMethod>() {
		@Override
		public int compare(IMethod o1, IMethod o2) {
			int rv = o1.getElementName().compareTo(o2.getElementName());
			if (rv == 0) {
				rv = o1.getParameterTypes().length - o2.getParameterTypes().length;
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
		public int compare(IType o1, IType o2) {
			int rv = o1.getElementName().compareTo(o2.getElementName());
			return rv;
		}
	};
	
	private void addNavigationActions(Menu menu, IJavaElement o) {
		if (o instanceof ClassElement) {
			final ClassElement c = (ClassElement) o;
			makeMenuItem(menu, "Open Type Hierarchy", new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					final IViewPart view = EclipseUIUtility.showView(JavaUI.ID_TYPE_HIERARCHY);
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
		
		AnnotationCreator(AnnotatedJavaElement aje, ScopedTargetType t) {		
			j = aje;
			target = t;
			makeScopedPromise = t != null;
		}
		
		AnnotationCreator(AnnotatedJavaElement aje) {
			this(aje, null);
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			final List<String> annos;
			if (!makeScopedPromise) {
				annos = findMissingAnnos(j);
			} else {
				annos = sortSet(remove(findApplicableAnnos(target.op), ScopedPromiseRules.PROMISE));
			}
			ListSelectionDialog d = new ListSelectionDialog(contents.getTree().getShell(), annos.toArray(), 
		    		annoProvider, annoProvider, 
		    		makeScopedPromise ? "Select scoped promise(s) to add for "+target.label : 
		    			                "Select annotation(s) to add");
			if (d.open() == Window.OK) {
				boolean changed = false;
				for(Object o : d.getResult()) {
					final String tag = (String) o;
					final AnnotationElement a;
					final Map<String,String> attrs = Collections.<String,String>emptyMap();
					if (makeScopedPromise) {
						a = new AnnotationElement(j, null, ScopedPromiseRules.PROMISE, "@"+tag+" for "+target.target, attrs);
					} else {
						a = new AnnotationElement(j, null, tag, "", attrs);
					}
					//System.out.println("Created elt: "+a);
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
	}
	
	private List<String> sortSet(final Set<String> s) {
		List<String> rv = new ArrayList<String>(s);		
		Collections.sort(rv);
		return rv;
	}
	
	private Set<String> findApplicableAnnos(final Operator op) {
		final Set<String> annos = new HashSet<String>();
		// Get valid/applicable annos
		for(IAnnotationParseRule<?,?> rule : PromiseFramework.getInstance().getParseDropRules()) {			
			if (!(rule instanceof NullAnnotationParseRule) &&
				rule.declaredOnValidOp(op) && 
				AnnotationElement.isIdentifier(rule.name())) {
				annos.add(rule.name());
			}
		}
		// These should never appear in XML files
		annos.remove(ScopedPromiseRules.ASSUME); 
		return annos;
	}
	
	private Set<String> remove(Set<String> s, String elt) {
		s.remove(elt);
		return s;
	}
	
	private List<String> findMissingAnnos(AnnotatedJavaElement j) {
		final Set<String> annos = findApplicableAnnos(j.getOperator());
		
		// Remove clashes
		for(AnnotationElement a : j.getPromises()) {
			// This will remove it if there should only be one of that kind
			annos.remove(a.getUid());
		}
		return sortSet(annos);
	}

	static class NewParameter {
		final int index;
		final String type;
		
		NewParameter(int i, String t) {
			index = i;
			type = t;
		}		
	}
	
	static IType findIType(ClassElement c, String nameSoFar) {
		String typeName = nameSoFar.isEmpty() ? c.getName() : c.getName()+'.'+nameSoFar;
		if (c instanceof NestedClassElement) {
			ClassElement parent = (ClassElement) c.getParent();
			return findIType(parent, typeName);
		} else { // top-level
			PackageElement pkg = (PackageElement) c.getParent();
			return JDTUtility.findIType(null, pkg.getName(), typeName);
		}
	}
	
	static void makeMenuItem(Menu menu, String label, SelectionListener l) {
		MenuItem item1 = new MenuItem(menu, SWT.PUSH);
		item1.setText(label);
		item1.addSelectionListener(l);
	}
	
	static class JavaElementProvider extends AbstractContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public String getText(Object element) {
			org.eclipse.jdt.core.IJavaElement e = (org.eclipse.jdt.core.IJavaElement) element; 
			if (e instanceof IMethod) {
				IMethod m = (IMethod) e;
				try {
					if (m.isConstructor()) {
						return "new "+m.getElementName()+'('+PromisesXMLBuilder.translateParameters(m)+')';
					}
					return m.getElementName()+'('+PromisesXMLBuilder.translateParameters(m)+')';
				} catch (JavaModelException e1) {
					// ignore
				}
				return m.getElementName()+"(???)";
			}
			return e.getElementName();
		}
	}
	
	class ParameterProvider extends AbstractContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof NewParameter) {
				NewParameter p = (NewParameter) element;
				return FunctionParameterElement.PREFIX+(p.index+1)+" : "+p.type;
			}
			FunctionParameterElement p = (FunctionParameterElement) element;
			return p.getLabel();
		}
		
		@Override
		public Color getForeground(Object element) {
			if (element instanceof FunctionParameterElement) {
				return contents.getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY);
			}
			return null;
		}
	}
	
	static class AnnoProvider extends AbstractContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public String getText(Object element) {
			return element.toString();
		}
	}

	public void focusOnMethod(String name, String params) {
		PackageElement p = provider.pkg;
		if (p != null) {
			final MethodElement m = p.visit(new MethodFinder(name, params));
			if (m != null) {
				focusOn(m);
			}
		}
	}
	
	public void focusOnNestedType(String relativeName) {
		PackageElement p = provider.pkg;
		if (p != null) {
			final NestedClassElement m = p.visit(new TypeFinder(relativeName));
			if (m != null) {
				focusOn(m);
			}
		}
	}
	
	abstract static class ElementFinder<T> extends AbstractJavaElementVisitor<T> {
		ElementFinder() {
			super(null);
		}
		
		@Override
		protected T combine(T old, T result) {
			if (old != null) {
				return old;
			}
			return result;
		}
	}
	
	static class TypeFinder extends ElementFinder<NestedClassElement> {
		final String[] names;
		final Stack<String> types = new Stack<String>();
		
		TypeFinder(String relativeName) {
			names = relativeName.split("\\.");
		}
		
		public NestedClassElement visit(NestedClassElement e) {
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
			for(String name : names) {
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
		
		MethodFinder(String name, String params) {
			this.name = name;
			this.params = params;
		}
		
		@Override
		public MethodElement visit(MethodElement m) {
			if (name.equals(m.getName())) {
				if (params == null || params.equals(m.getParams())) {
					return m;
				}				
			}
			return null;
		}
	}
	
	static class XmlMap extends HashMap<String,Collection<String>> implements IXmlProcessor {
		private static final long serialVersionUID = 1L;
		private final boolean makeUnique;
		
		XmlMap(boolean makeUnique) {
			this.makeUnique = makeUnique;
		}
		
		private Collection<String> getPkg(String qname) {
			Collection<String> c = get(qname);
			if (c == null) {
				c = makeUnique ? new HashSet<String>(4) : new ArrayList<String>(2);
				put(qname, c);
			}
			return c;
		}
		
		@Override
		public void addPackage(String qname) {
			getPkg(qname);
		}

		@Override
		public void addType(String pkg, String name) {
			Collection<String> c = getPkg(pkg);
			c.add(name);
		}		
	}
	
	/**
	 * @return a map of packages to qualified names
	 */
	public static Map<String,Collection<String>> findAllPromisesXML() {
		return findLocalPromisesXML(true);
	}
	
	public static Map<String,Collection<String>> findLocalPromisesXML() {
		return findLocalPromisesXML(false);
	}
	
	private static Map<String,Collection<String>> findLocalPromisesXML(boolean includeFluid) {
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
	public static Pair<File,File> findPromisesXML(String path) {
		File fluid = null; 
		File local = null;		
		final File localXml = JSurePreferencesUtility.getJSureXMLDirectory();
		if (localXml != null) {
			File f = new File(localXml, path);
			/*
			if (f.isFile()) {
				local = f;
			}
			*/
			local = f;
		}
		// Try fluid
		final File xml = PromisesXMLParser.getFluidXMLDir();
		if (xml != null) {
			File f = new File(xml, path);
			/*
			if (f.isFile()) {
				fluid = f;
			}
			*/
			fluid = f;
		}
		return new Pair<File,File>(fluid, local);	
	}
	
	public static IEditorPart openInEditor(String path, boolean readOnly) {
		final IEditorInput i = makeInput(path, readOnly);
		if (i == null || !i.exists()) {
			return null;
		}
		return EclipseUIUtility.openInEditor(i, PromisesXMLEditor.class.getName());
	}
	
	public static IEditorInput makeInput(String relativePath, boolean readOnly) {
		try {
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
		
		Input(String relativePath, boolean ro) throws URISyntaxException {
			readOnly = ro;
			path = relativePath;
			uri = new URI(path);
			
			final int lastSlash = path.lastIndexOf('/');
			if (lastSlash < 0) {
				name = path;
			} else {
				name = path.substring(lastSlash+1);
			}
		}

		@Override
		public boolean exists() {
			final Pair<File,File> f = findPromisesXML(path);
			return f.first().isFile();
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
				public void saveState(IMemento memento) {
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
		public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
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
		public boolean equals(Object o) {
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

	public void focusOn(IJavaElement e) {		
		contents.setSelection(new StructuredSelection(e));
		contents.reveal(e);
	}
	
	static class Comparer implements IElementComparer {
		@Override
		public int hashCode(Object element) {
			if (element instanceof AnnotationElement) {
				return System.identityHashCode(element);
			}
			return element.hashCode();
		}
		
		@Override
		public boolean equals(Object a, Object b) {
			return a == b;
		}		
	}

	@Override
	public void refresh(PackageElement e) {
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
}
