package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;

import com.surelogic.jsure.core.persistence.JavaIdentifierUtil;

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
				ICompilationUnit cu = makeCompUnit(input.getStorage());
				ASTNode root = parseInput(cu);
				
				Visitor v = new Visitor();
				root.accept(v);
				v.getIdentifier();
				// TODO how do I look it up?
				// TODO where do I output to
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class Visitor extends ASTVisitor {
		private MethodInvocation call;
		
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
			}
			return true;
		}
		
		private boolean containsSelection(int offset, int length) {
			return offset <= selection.getOffset() &&
				(offset+length) >= (selection.getOffset()+selection.getLength());
		}
		
		String getIdentifier() {
			if (call != null) {
				final IMethodBinding mb = call.resolveMethodBinding();
				if (mb != null) {
					String id = JavaIdentifierUtil.encodeBinding(mb);
					System.out.println("id = "+id);
					return id;
				} else {
					System.out.println("No binding for "+call);
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

	private ICompilationUnit makeCompUnit(IStorage storage) {
		// TODO not right if the editor is modified		
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceRoot wsRoot = workspace.getRoot();
		final IFile file = wsRoot.getFile(storage.getFullPath());
		return JavaCore.createCompilationUnitFrom(file);
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
