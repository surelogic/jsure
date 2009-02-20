/*
 * Created on Jan 25, 2005
 *
 */
package edu.cmu.cs.fluid.dcf.views.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.eclipse.EclipseCodeFile;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.ModulePromises;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;

/**
 * @author Edwin
 */
public class FastViewerPage extends ContentOutlinePage implements IFastViewerPage {
  static final Logger LOG = SLLogger.getLogger("FastViewer.outline");
  static final ISharedImages javaImages = JavaUI.getSharedImages();
  
  static final SyntaxTreeInterface tree = JJNode.tree;
  final IRNode compUnit;
  final ContentProvider content;
  
  /**
   * @param icu
   */
  public FastViewerPage(ICompilationUnit icu) {
    IRNode top;
    SourceCUDrop drop = SourceCUDrop.queryCU(new EclipseCodeFile(icu));
    if (drop != null) {
      top = drop.cu;
    } else {
      top = null;
      LOG.warning("Couldn't find drop for "+icu.getElementName());
    }
    compUnit = top;
    content  = new ContentProvider();
  }

  /**
   * Subclasses must extend this method configure the tree viewer 
   * with a proper content provider, label provider, and input element.
   */
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);

    TreeViewer viewer = getTreeViewer();
    viewer.setContentProvider(content);
    viewer.setLabelProvider(content);
    viewer.setInput(compUnit);
    viewer.expandToLevel(3);
  }
  
  private static class ContentProvider extends LabelProvider
    implements ITreeContentProvider 
  {
    public ContentProvider() {
    }
    
    private IRNode convert(Object o) {
      try {
        return (IRNode) o;
      }
      catch(ClassCastException e) {
        LOG.severe("Trying to cast a "+o.getClass().getName()+" to IRNode");
        return null;
      }
    }
    
    public Object getParent(Object o) {
      return tree.getParentOrNull(convert(o));
    }

    public boolean hasChildren(Object o) {
      return tree.hasChildren(convert(o));
    }

    private static final Object[] noChildren = {};
    
    public Object[] getChildren(Object o) {
      IRNode n      = convert(o);
      Iterator<IRNode> e = tree.children(n);
      if (!e.hasNext()) {
        return noChildren;
      }
      List<IRNode> l = new ArrayList<IRNode>();
      while (e.hasNext()) {
        l.add(e.next());
      }
      return l.toArray();
    }

    public Object[] getElements(Object elt) {
      return getChildren(elt);
    }

    /* (non-Javadoc)
     * Method declared on IContentProvider.
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public String getText(Object o) {
      IRNode n    = convert(o);
      if (ModulePromises.apiDecls(n).hasNext()) {
        return DebugUnparser.toString(n);
      }
      Operator op = tree.getOperator(n);
      String rv   = op.name(); 
      String id   = JJNode.getInfoOrNull(n);
      if (id != null) {
        rv = rv +" "+id;
      }
      return rv;
    }    

    @Override
    public Image getImage(Object o) {
      return null;
    }  
  }
}
