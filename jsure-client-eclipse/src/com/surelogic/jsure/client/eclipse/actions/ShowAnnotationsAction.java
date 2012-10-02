package com.surelogic.jsure.client.eclipse.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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

import com.surelogic.common.IJavaRef;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor;
import com.surelogic.jsure.core.persistence.JavaIdentifierUtil;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;
import com.surelogic.persistence.JavaIdentifier;
import com.surelogic.xml.TestXMLParserConstants;

public class ShowAnnotationsAction implements IEditorActionDelegate {
	private final ASTParser parser;
	
	IStorageEditorInput input;
	ITextSelection selection;
	
	public ShowAnnotationsAction() {
		parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
	}
	
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {		
		if (targetEditor != null && targetEditor.getEditorInput() instanceof IStorageEditorInput) {
			input = (IStorageEditorInput) targetEditor.getEditorInput();
		} else {
			input = null;
		}		
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof ITextSelection) {
			this.selection = (ITextSelection) selection;
		} else {
			selection = null;
		}
	}

	@Override
	public void run(IAction action) {
		if (input != null && selection != null) {
			try {
				ICompilationUnit cu = makeCompUnit(input);
				ASTNode root = parseInput(cu);
				
				final Visitor v = new Visitor();
				root.accept(v);
				final String qname = v.getQualifiedTypeName();
				if (qname != null) {
					/*
					final String xmlRoot = JSurePreferencesUtility.getJSureXMLDirectory().getAbsolutePath();
					String path = xmlRoot+slash+qname.replace('.', slash)+TestXMLParserConstants.SUFFIX;
					IEditorPart editor = EclipseUIUtility.openInEditor(path);
					*/
					String path = qname.replace('.', '/')+TestXMLParserConstants.SUFFIX;
					IEditorPart editor = PromisesXMLEditor.openInEditor(path, false);
					if (editor instanceof PromisesXMLEditor && v.getMethodName() != null) {
						final PromisesXMLEditor pxe = (PromisesXMLEditor) editor;
						pxe.focusOnMethod(v.getMethodName(), v.getMethodParameters());					
					}
				}				
				final String id = JavaIdentifier.omitProject(v.getIdentifier());
				System.out.println("id = "+id);
				if (id != null) {										
					final JSureScanInfo info = JSureDataDirHub.getInstance().getCurrentScanInfo();
					if (info != null) {
						final Map<String,List<IDrop>> promises = preprocessPromises(info);
						List<IDrop> l = promises.get(id);
						if (l != null) {
							for(IDrop d : l) {
								System.out.println("Has promise: @"+d.getMessage());
							}
						}
					}
					/*
					for(IDropInfo d : info.getDropsOfType(PromiseDrop.class)) {
						final ISrcRef ref = d.getSrcRef();						
						if (ref == null) {
							//System.out.println("No src ref:  @"+d.getMessage());
						} else {
							final String rId = omitProject(ref.getJavaId());							
							if (rId == null) {
								System.out.println("No id for @"+d.getMessage());
							}
							if (rId != null && rId.contains("EnumMap")) {
								System.out.println("rId = "+rId);
							}							
							if (id.equals(rId)) {
								System.out.println("Has promise: @"+d.getMessage());
							}
						}
					}	
					*/			
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private Map<String,List<IDrop>> preprocessPromises(final JSureScanInfo info) {
		Map<String,List<IDrop>> rv = new HashMap<String, List<IDrop>>();
		for(IPromiseDrop d : info.getPromiseDrops()) {
			final IJavaRef ref = d.getJavaRef();			
			if (ref == null) {
				//System.out.println("No src ref:  @"+d.getMessage());
			} else {				
				final String rId = JavaIdentifier.omitProject(ref.getJavaId());							
				if (rId == null) {
					System.out.println("No id for @"+d.getMessage());
				} else {
					List<IDrop> l = rv.get(rId);
					if (l == null) { 
						l = new ArrayList<IDrop>();
						rv.put(rId, l);
					}
					l.add(d);
				}
			}
		}
		return rv;
	}
	
	class Visitor extends ASTVisitor {
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
			return offset <= selection.getOffset() &&
				(offset+length) >= (selection.getOffset()+selection.getLength());
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
		
		String getQualifiedTypeName() {
			if (typeB != null) {
				return typeB.getErasure().getQualifiedName();
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
					return JavaIdentifierUtil.encodeParameterType(params[0]);
				default:
					final StringBuilder sb = new StringBuilder();
					for(ITypeBinding t : params) {
						if (sb.length() > 0) {
							sb.append(","); // Same as AbstractFunctionElement.normalize()
						}
						sb.append(JavaIdentifierUtil.encodeParameterType(t));
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
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		parser.setFocalPosition(selection.getOffset());
		return parser.createAST(null);
	}
}
