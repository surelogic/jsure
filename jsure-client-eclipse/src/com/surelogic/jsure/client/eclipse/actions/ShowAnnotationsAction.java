package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor;
import com.surelogic.jsure.core.persistence.JavaIdentifierUtil;

public class ShowAnnotationsAction implements IEditorActionDelegate {
  private final ASTParser f_parser;
	
	IStorageEditorInput f_input;
	ITextSelection f_selection;
	
  public ShowAnnotationsAction() {
    /*
     * We need this until we can go to 3.7 and up and change to JLS4.
     */
    @SuppressWarnings("deprecation")
	  final ASTParser parser = ASTParser.newParser(AST.JLS3);
		f_parser = parser;
		f_parser.setResolveBindings(true);
		f_parser.setBindingsRecovery(true);
		f_parser.setStatementsRecovery(true);
	}
	
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {		
		if (targetEditor != null && targetEditor.getEditorInput() instanceof IStorageEditorInput) {
			f_input = (IStorageEditorInput) targetEditor.getEditorInput();
		} else {
			f_input = null;
		}		
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof ITextSelection) {
			this.f_selection = (ITextSelection) selection;
		} else {
			selection = null;
		}
	}

	@Override
	public void run(IAction action) {
		if (f_input != null && f_selection != null) {
			try {
				ICompilationUnit cu = makeCompUnit(f_input);
				ASTNode root = parseInput(cu);
				
				final Visitor v = new Visitor();
				root.accept(v);
				final ITypeBinding typeB = v.getQualifiedType();
				if (typeB != null) {
					/*
					final String xmlRoot = JSurePreferencesUtility.getJSureXMLDirectory().getAbsolutePath();
					String path = xmlRoot+slash+qname.replace('.', slash)+TestXMLParserConstants.SUFFIX;
					IEditorPart editor = EclipseUIUtility.openInEditor(path);
					*/				
					IType type = findIType(typeB);
					//String path = qname.replace('.', '/')+TestXMLParserConstants.SUFFIX;
					IEditorPart editor = PromisesXMLEditor.openInXMLEditor(type, false);
					if (editor instanceof PromisesXMLEditor) {
						final PromisesXMLEditor pxe = (PromisesXMLEditor) editor;
						if (v.getMethodName() != null) {
							pxe.focusOnMethod(typeB.getName(), v.getMethodName(), v.getMethodParameters());					
						} 
						else if (typeB.getQualifiedName().startsWith(type.getFullyQualifiedName())) {
							String fullName = typeB.getQualifiedName();
							pxe.focusOnNestedType(fullName.substring(type.getFullyQualifiedName().length()+1, fullName.length()));
						}
					}
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private IType findIType(ITypeBinding type) {
		if (type.getDeclaringClass() != null) {
			return findIType(type.getDeclaringClass());
		}
		if (type.getDeclaringMethod() != null) {
			return findIType(type.getDeclaringMethod().getDeclaringClass());
		}
		return JDTUtility.findIType(null, type.getPackage().getName(), type.getName());
	}

	class Visitor extends ASTVisitor {
		private MethodDeclaration mdecl;
		private MethodInvocation call;
		private IMethodBinding binding;
		private ITypeBinding typeB;
		private Name name;
		
		@Override
		public boolean visit(SimpleName node) {
			return visitName(node);
		}
		
		@Override
		public boolean visit(QualifiedName node) {
			visitName(node.getQualifier());
			return visitName(node);
		}
		
		private boolean visitName(Name node) {
			if (containsSelection(node.getStartPosition(), node.getLength())) {
				if (name != null) {
					if (node.getStartPosition() < name.getStartPosition()) {
						// Keep looking
						return true;
					}
				}
				IBinding b = node.resolveBinding();
				if (b instanceof ITypeBinding) {
					name = node;			
					typeB = (ITypeBinding) b;
				}
			}
			return true;
		}
		
		@Override
		public boolean visit(MethodDeclaration node) {			
			if (containsSelection(node.getStartPosition(), node.getLength())) {
				if (mdecl != null) {
					if (node.getStartPosition() < mdecl.getStartPosition()) {
						// Keep looking
						return true;
					}
				} 
				//System.out.println("Position = "+node.getStartPosition()+" ("+selection.getOffset()+")");
				mdecl = node;
				binding = mdecl.resolveBinding();
				typeB = binding.getDeclaringClass();
			}
			return true;
		}
		
		@Override	
		public boolean visit(MethodInvocation node) {			
			if (containsSelection(node.getStartPosition(), node.getLength())) {
				if (call != null) {
					if (node.getStartPosition() < call.getStartPosition()) {
						// Keep looking
						return true;
					}
				} 
				//System.out.println("Position = "+node.getStartPosition()+" ("+selection.getOffset()+")");
				call = node;
				binding = call.resolveMethodBinding();
				typeB = binding.getDeclaringClass();
			}
			return true;
		}
		
		private boolean containsSelection(int offset, int length) {
			return offset <= f_selection.getOffset() &&
				(offset+length) >= (f_selection.getOffset()+f_selection.getLength());
		}
		
		String getIdentifier() {
			if (binding != null) {
				String id = JavaIdentifierUtil.encodeBinding(binding);
				//System.out.println("id = "+id);
				return id;
			} else {
				System.out.println("No binding for "+call);
			}
			return null;
		}
		
		ITypeBinding getQualifiedType() {
			if (typeB != null) {				
				return typeB.getErasure();
			}
			return null;
		}
		
		String getMethodName() {
			if (binding != null) {
				return binding.getName();
			}
			return null;
		}
		
		String getMethodParameters() {
			if (binding != null) {
				ITypeBinding[] params = binding.getParameterTypes();
				switch (params.length) {
				case 0:
					return "";
				case 1:
					return JavaIdentifierUtil.encodeParameterType(params[0], false);
				default:
					final StringBuilder sb = new StringBuilder();
					for(ITypeBinding t : params) {
						if (sb.length() > 0) {
							sb.append(", "); // Same as AbstractFunctionElement.normalize()
						}
						sb.append(JavaIdentifierUtil.encodeParameterType(t, false));
					}
					return sb.toString();
				}
			}
			return null;
		}
	}
	
	/*
	private String getSource(IStorage storage) throws Exception {
		final StringBuilder sb = new StringBuilder();
		final InputStream s = storage.getContents();
		final BufferedReader r = new BufferedReader(new InputStreamReader(s));
		try {
			final char[] cbuf = new char[1024];
			int read;
			while ((read = r.read(cbuf)) >= 0) {
				sb.append(cbuf, 0, read);
			}
		} finally {
			r.close();
		}
		return sb.toString();
	}
    */

	private ICompilationUnit makeCompUnit(IStorageEditorInput input) throws CoreException {
		return JavaUI.getWorkingCopyManager().getWorkingCopy(input);
		/*
		final IStorage storage = input.getStorage();
		// TODO not right if the editor is modified		
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceRoot wsRoot = workspace.getRoot();
		final IFile file = wsRoot.getFile(storage.getFullPath());
		return JavaCore.createCompilationUnitFrom(file);
		*/
	}
	
	private ASTNode parseInput(ICompilationUnit cu) {
		f_parser.setSource(cu);
		f_parser.setResolveBindings(true);
		f_parser.setBindingsRecovery(true);
		f_parser.setStatementsRecovery(true);
		f_parser.setFocalPosition(f_selection.getOffset());
		return f_parser.createAST(null);
	}
}
