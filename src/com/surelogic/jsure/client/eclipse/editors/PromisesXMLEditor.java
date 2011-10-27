package com.surelogic.jsure.client.eclipse.editors;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.part.EditorPart;

import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.NullAnnotationParseRule;
import com.surelogic.annotation.rules.ScopedPromiseRules;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.BalloonUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.views.AbstractContentProvider;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesLibMerge;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.*;
import com.surelogic.xml.IJavaElement;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

public class PromisesXMLEditor extends EditorPart {
	enum FileStatus { 		
		READ_ONLY, 
		/** Mutable, but saves to a local file */
		FLUID, 
		/** Mutable in the usual way */
		LOCAL 
	}
	
	public static final boolean hideEmpty = false;
	
	private final Provider provider = new Provider();
	private static final JavaElementProvider jProvider = new JavaElementProvider();
	private static final ParameterProvider paramProvider = new ParameterProvider();
	private static final AnnoProvider annoProvider = new AnnoProvider();
    private TreeViewer contents;
    private boolean isDirty = false;
    
    @Override
    public void createPartControl(Composite parent) {
       contents = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
       contents.setContentProvider(provider);
       contents.setLabelProvider(provider);
       if (provider.getInput() != null) {
    	   contents.setInput(provider.getInput());
       }
       contents.getControl().addMenuDetectListener(new MenuDetectListener() {
    	   @Override
    	   public void menuDetected(MenuDetectEvent e) {
    		   final Menu menu = new Menu(contents.getControl().getShell(), SWT.POP_UP);
    		   if (provider.isMutable()) {
    			   setupContextMenu(menu);
    		   }
    		   contents.getTree().setMenu(menu);
    	   }
       });
       // http://bingjava.appspot.com/snippet.jsp?id=2208
       contents.addDoubleClickListener(new IDoubleClickListener() {
    	   @Override
    	   public void doubleClick(DoubleClickEvent event) {
    		   if (provider.isMutable()) {
    			   final IStructuredSelection s = (IStructuredSelection) event.getSelection();
    			   System.out.println("Doubleclik on "+s.getFirstElement());
    			   contents.editElement(s.getFirstElement(), 0);
    		   }
    	   }
       });

       contents.setCellEditors(new CellEditor[] { new TextCellEditor(contents.getTree()) });
       contents.setColumnProperties(new String[] { "col1" });
       contents.setCellModifier(new ICellModifier() {
    	   @Override
        public boolean canModify(Object element, String property) {    		 
    		   return ((IJavaElement) element).canModify();
    	   }
    	   @Override
        public Object getValue(Object element, String property) {
    		   return ((IJavaElement) element).getLabel();
    	   }
    	   @Override
        public void modify(Object element, String property, Object value) {
    		   Item i = (Item) element;
    		   IJavaElement e = (IJavaElement) i.getData();
    		   e.modify((String) value, BalloonUtility.errorListener);
    		   contents.update(e, null);
    	   }
       });
       
       // http://eclipse.dzone.com/tips/treeviewer-two-clicks-edit
       TreeViewerEditor.create(contents, null, new ColumnViewerEditorActivationStrategy(contents) {
    	   @Override
    	   protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent e) {
    		   System.out.println("Got eae: "+e);
    		   ViewerCell cell = (ViewerCell) e.getSource();
    		   IJavaElement elt = (IJavaElement) cell.getElement();
    		   return elt.canModify();
    	   }
       }, ColumnViewerEditor.DEFAULT);
       
       /*
       //http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.jdt.doc.isv/guide/jdt_api_render.htm
       contents.setContentProvider(new StandardJavaElementContentProvider(true));
       contents.setLabelProvider(new JavaElementLabelProvider());
       */
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
    	   }
       }
       setPartName(input.getName());
    }
    
    @Override
    public void setFocus() {
       if (contents != null) {
          contents.getControl().setFocus();
       }
    }	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		provider.save(monitor);		
		if (isDirty) {
			isDirty = false;
			fireDirtyProperty();
		}
	}

	@Override
	public void doSaveAs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDirty() {
		return provider.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	class Provider extends AbstractContentProvider implements ITreeContentProvider {
		URI location;
		FileStatus status = null;
		PackageElement pkg;
		Object[] roots;
		
		boolean isMutable() {
			return status == FileStatus.FLUID || status == FileStatus.LOCAL;
		}
		
		URI getInput() {
			return location;
		}
		
		void save(IProgressMonitor monitor) {
			if (status == null || status == FileStatus.READ_ONLY) {
				return;
			}
			try {
				final File root = JSurePreferencesUtility.getJSureXMLDirectory();
				File f = new File(root, location.toASCIIString());
				PromisesXMLWriter w = new PromisesXMLWriter(f);
				w.write(pkg);
				pkg.markAsClean();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		boolean isDirty() {
			if (pkg == null) {
				return false;
			}
			return pkg.isDirty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != location) {
				if (newInput instanceof URI) {
					location = (URI) newInput;				
					System.out.println("Editor got "+location);
					build();
				}
			} else {
				System.out.println("Ignoring duplicate input");
				/*
				IType t = JDTUtility.findIType(null, pkg.getName(), pkg.getClassElement().getName());
				contents.setInput(t);
                */
			}
			if (viewer != null && viewer == contents) {
				contents.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						contents.expandAll();
					}					
				});

			}
		}

		void build() {
			if (location != null) {
				try {
					InputStream in = null;					
					try {
						in = location.toURL().openStream();
						status = FileStatus.READ_ONLY;
					} catch(IllegalArgumentException e) {
						final String path = location.toASCIIString();
						Pair<File,FileStatus> rv = findPromisesXML(path);
						if (rv != null) {
							in = new FileInputStream(rv.first());
							status = rv.second();
						} else {
							throw e;
						}
					}
					PromisesXMLReader r = new PromisesXMLReader();
					r.read(in);
					roots = new Object[1];
					roots[0] = pkg = r.getPackage();					
					if (true) {
						if (PromisesXMLBuilder.updateElements(pkg)) {
							System.out.println("Added elements");
						}
					}
				} catch (Exception e) {
					pkg = null;
					roots = ArrayUtil.empty;
				}
			}
		}
		
		@Override
		public Object[] getChildren(Object element) {
			Object[] children = ((IJavaElement) element).getChildren();
			if (hideEmpty) {
				final List<IJavaElement> nonLeaf = new ArrayList<IJavaElement>();
				for(Object o : children) {
					IJavaElement c = (IJavaElement) o;
					if (c.hasChildren() || c instanceof IMergeableElement) {
						nonLeaf.add(c);
					}
				}
				return nonLeaf.toArray();
			}
			return children;
		}

		@Override
		public Object getParent(Object element) {
			return ((IJavaElement) element).getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((IJavaElement) element).hasChildren();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return roots;
		}
		
		@Override
		public String getText(Object element) {
			return ((IJavaElement) element).getLabel();
		}

		@Override
		public Image getImage(Object element) {
			IJavaElement e = (IJavaElement) element;
			return SLImages.getImage(e.getImageKey());
		}
		
		@Override
		public Color getForeground(Object element) {
			IJavaElement e = (IJavaElement) element;
			if (e.isModified()) {
				if (colorRed == null) {
					colorRed = contents.getControl().getDisplay().getSystemColor(SWT.COLOR_RED);
				}
				return colorRed;
			} 
			return null;
		}
		
		private Color colorRed;
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
		if (!isDirty) {
			isDirty = true;
			fireDirtyProperty();
		}
		// otherwise already dirty
	}
	
	private CommentElement makeComment() {
        CommentElement c = CommentElement.make("...");
        c.markAsDirty();
        markAsDirty();
        return c;
	}
	
	private void setupContextMenu(final Menu menu) {
		final IStructuredSelection s = (IStructuredSelection) contents.getSelection();
		if (s.size() != 1) {
			return;
		}
		final IJavaElement o = (IJavaElement) s.getFirstElement();
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
		
		if (o instanceof AnnotatedJavaElement) {
			final AnnotatedJavaElement j = (AnnotatedJavaElement) o;
			makeMenuItem(menu, "Add annotation...", new AnnotationCreator(j));
		
			if (o instanceof AbstractFunctionElement) {
				final AbstractFunctionElement f = (AbstractFunctionElement) o;
				if (f.getParams().length() > 0) {
					// Find out which parameters need to be added
					final String[] params = f.getSplitParams();
					final List<NewParameter> newParams = new ArrayList<NewParameter>();
					for(int i=0; i<params.length; i++) {
						if (f.getParameter(i) == null) {
							newParams.add(new NewParameter(i,params[i]));
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
									for(Object o : d.getResult()) {
										NewParameter np = (NewParameter) o;
										FunctionParameterElement p = new FunctionParameterElement(np.index);	
										f.setParameter(p);
									}
									contents.refresh();
									contents.expandToLevel(f, 1);
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
				makeMenuItem(menu, "Add existing method(s)...", new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						final IType t = findIType(c, "");						
						ListSelectionDialog d;
						try {
							List<IMethod> methods = new ArrayList<IMethod>();
							for(IMethod m : t.getMethods()) {
								if (m.getDeclaringType().equals(t) && !"<clinit>".equals(m.getElementName())) {
									methods.add(m);
								}
							}
							Collections.sort(methods, new Comparator<IMethod>() {
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
							});
							d = new ListSelectionDialog(contents.getTree().getShell(), methods.toArray(), 
									                    jProvider, jProvider, "Select method(s)");
							if (d.open() == Window.OK) {
								for(Object o : d.getResult()) {
									IMethod m = (IMethod) o;
									String params = PromisesXMLBuilder.translateParameters(m);									
									c.addMember(m.isConstructor() ? new ConstructorElement(params) : 
										                            new MethodElement(m.getElementName(), params));
									markAsDirty();
								}
								contents.refresh();
								//contents.refresh(c, true);
							}
						} catch (JavaModelException e1) {
							e1.printStackTrace();
						}															
					}
				});
			}
		}
		if (o instanceof IMergeableElement) {
			final IMergeableElement me = (IMergeableElement) o;
			makeMenuItem(menu, "Delete", new SelectionAdapter() {
		        @Override
		        public void widgetSelected(SelectionEvent e) {          
		        	me.delete();
		        	markAsDirty();
		          	contents.refresh();
	            	contents.expandToLevel(me.getParent(), 1);
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
				for(Object o : d.getResult()) {
					final String tag = (String) o;
					final AnnotationElement a;
					final Map<String,String> attrs = Collections.<String,String>emptyMap();
					if (makeScopedPromise) {
						a = new AnnotationElement(null, ScopedPromiseRules.PROMISE, "@"+tag+" for "+target.target, attrs);
					} else {
						a = new AnnotationElement(null, tag, "", attrs);
					}
					j.addPromise(a);
					a.markAsModified();
					markAsDirty();
				}
				contents.refresh();
				contents.expandToLevel(j, 1);
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
	
	static class ParameterProvider extends AbstractContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public String getText(Object element) {
			NewParameter p = (NewParameter) element;
			return "["+p.index+"] : "+p.type;
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
				//contents.expandToLevel(m, 1);
				contents.setSelection(new StructuredSelection(m));
				contents.reveal(m);
			}
		}
	}
	
	static class MethodFinder extends AbstractJavaElementVisitor<MethodElement> {
		final String name, params;
		
		MethodFinder(String name, String params) {
			super(null);
			this.name = name;
			this.params = params;
		}

		@Override
		protected MethodElement combine(MethodElement old, MethodElement result) {
			if (old != null) {
				return old;
			}
			return result;
		}
		
		@Override
		public MethodElement visit(MethodElement m) {
			if (name.equals(m.getName())) {
				if (params.equals(m.getParams())) {
					return m;
				}				
			}
			return null;
		}
	}
	
	/**
	 * @return true if local
	 */
	static Pair<File,FileStatus> findPromisesXML(String path) {
		final File localXml = JSurePreferencesUtility.getJSureXMLDirectory();
		if (localXml != null) {
			File f = new File(localXml, path);
			if (f.isFile()) {
				return new Pair<File,FileStatus>(f, FileStatus.LOCAL);
			}
		}
		// Try fluid
		final File xml = PromisesLibMerge.getFluidXMLDir();
		if (xml != null) {
			File f = new File(xml, path);
			if (f.isFile()) {
				return new Pair<File,FileStatus>(f, FileStatus.FLUID);
			}
		}
		return null;	
	}
	
	public static IEditorPart openInEditor(String path) {
		return EclipseUIUtility.openInEditor(makeInput(path), PromisesXMLEditor.class.getName());
	}
	
	public static IEditorInput makeInput(String relativePath) {
		try {
			return new Input(relativePath);
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	private static class Input implements IURIEditorInput {
		private final String path;
		private final String name;
		private final URI uri;
		
		Input(String relativePath) throws URISyntaxException {
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
			return findPromisesXML(path) != null;
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

		@SuppressWarnings("unchecked")
		@Override
		public Object getAdapter(Class adapter) {
			if (adapter == Object.class) {
				return this;
			}
			return null;
		}

		@Override
		public URI getURI() {
			return uri;
		}
	}
}
