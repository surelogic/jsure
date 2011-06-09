package com.surelogic.jsure.client.eclipse.editors;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.part.EditorPart;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.*;
import com.surelogic.xml.IJavaElement;

import edu.cmu.cs.fluid.util.ArrayUtil;

public class PromisesXMLEditor extends EditorPart {
	private final Provider provider = new Provider();
	private static final JavaElementProvider jProvider = new JavaElementProvider();
    private TreeViewer contents;
    
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
    		   setupContextMenu(menu);
    		   contents.getTree().setMenu(menu);
    	   }
       });
       // http://bingjava.appspot.com/snippet.jsp?id=2208
       contents.addDoubleClickListener(new IDoubleClickListener() {
    	   @Override
    	   public void doubleClick(DoubleClickEvent event) {
    		   final IStructuredSelection s = (IStructuredSelection) event.getSelection();
    		   System.out.println("Doubleclik on "+s.getFirstElement());
    		   contents.editElement(s.getFirstElement(), 0);
    	   }
       });

       contents.setCellEditors(new CellEditor[] { new TextCellEditor(contents.getTree()) });
       contents.setColumnProperties(new String[] { "col1" });
       contents.setCellModifier(new ICellModifier() {
    	   public boolean canModify(Object element, String property) {
    		   return true;
    	   }
    	   public Object getValue(Object element, String property) {
    		   return "What is this for?";//((MyModel) element).counter + "";
    	   }
    	   public void modify(Object element, String property, Object value) {
    		   /*
    		   element = ((Item) element).getData();
    		   ((MyModel) element).counter = Integer.parseInt(value.toString());
    		   contents.update(element, null);
    		   */
    	   }
       });
       /*
       // http://eclipse.dzone.com/tips/treeviewer-two-clicks-edit
       TreeViewerEditor.create(contents, null, new ColumnViewerEditorActivationStrategy(contents) {
    	   @Override
    	   protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent e) {
    		   System.out.println("Got eae: "+e);
    		   System.out.println("isEditorActivationEvent: "+super.isEditorActivationEvent(e));
    		   return true;
    	   }
       }, ColumnViewerEditor.DEFAULT);
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
	
	class Provider implements ITreeContentProvider, ILabelProvider {
		URI location;
		PackageElement pkg;
		Object[] roots;
		
		URI getInput() {
			return location;
		}
		
		void save(IProgressMonitor monitor) {
			try {
				File f = new File(location);
				PromisesXMLWriter w = new PromisesXMLWriter(f);
				w.write(pkg);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		boolean isDirty() {
			return pkg.isDirty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != location) {
				location = (URI) newInput;				
				System.out.println("Editor got "+location);
				build();
			} else {
				System.out.println("Ignoring duplicate input");
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
					/*
					File f = new File(location);
					System.out.println("location = "+f);
					*/
					InputStream in = location.toURL().openStream();
					PromisesXMLReader r = new PromisesXMLReader();
					r.read(in);
					roots = new Object[1];
					roots[0] = pkg = r.getPackage();					
				} catch (Exception e) {
					pkg = null;
					roots = ArrayUtil.empty;
				}
			}
		}
		
		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof String) {
				return ArrayUtil.empty;
			}
			return ((IJavaElement) element).getChildren();
		}

		@Override
		public Object getParent(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof String) {
				return false;
			}
			return ((IJavaElement) element).hasChildren();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return roots;
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof String) {
				return element.toString();
			}
			return ((IJavaElement) element).getLabel();
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}
		
		@Override
		public void addListener(ILabelProviderListener listener) {
			// Nothing to do
		}
		@Override
		public boolean isLabelProperty(Object element, String property) {
			// Nothing to do
			return false;
		}
		@Override
		public void removeListener(ILabelProviderListener listener) {
			// Nothing to do
		}
		@Override
		public void dispose() {
			// Nothing to do
		}
	}
	
	private void setupContextMenu(final Menu menu) {
		final IStructuredSelection s = (IStructuredSelection) contents.getSelection();
		if (s.size() != 1) {
			return;
		}
		final Object o = s.getFirstElement();
		if (o instanceof AnnotationElement) {
			// TODO add comment
		}
		else if (o instanceof AbstractJavaElement) {
			final AbstractJavaElement j = (AbstractJavaElement) o;
			makeMenuItem(menu, "Add annotation...", new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					// TODO create dialog
					//AnnotationElement a = new AnnotationElement(null, tag, text, attrs);
				}
			});
		
			// TODO add comment
			if (o instanceof AbstractFunctionElement) {
				final AbstractFunctionElement f = (AbstractFunctionElement) o;
				makeMenuItem(menu, "Add parameter...", new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						// TODO create dialog
						FunctionParameterElement p = new FunctionParameterElement(0);						
					}
				});
			} 
			else if (o instanceof ClassElement) {
				final ClassElement c = (ClassElement) o;
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
							if (d.open() == ListSelectionDialog.OK) {
								for(Object o : d.getResult()) {
									IMethod m = (IMethod) o;
									String params = PromisesXMLBuilder.translateParameters(m);									
									c.addMember(m.isConstructor() ? new ConstructorElement(params) : 
										                            new MethodElement(m.getElementName(), params));
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
	
	static class JavaElementProvider implements IStructuredContentProvider, ILabelProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
		}

		@Override
		public Image getImage(Object element) {
			// TODO Auto-generated method stub
			return null;
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
				} catch (JavaModelException e1) {
					// ignore
				}
				return m.getElementName()+'('+PromisesXMLBuilder.translateParameters(m)+')';
			}
			return e.getElementName();
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
			
		}
	}
}
