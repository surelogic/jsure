// $Header:
// /cvs/fluid/fluid/src/edu/cmu/cs/fluid/render/StyledForestViewFactory.java,v
// 1.6 2003/07/15 18:39:11 thallora Exp $
package edu.cmu.cs.fluid.render;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.predicate.AttributePredicate;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.tree.AbstractForestToForestStatefulView;

/**
 * A class that configures an AbstractForestToForestStatefulView to have an
 * attribute describing how each node should be highlighted.
 */
public final class StyledForestViewFactory {
  /**
	 * The attribute describing how each node should be highlighted. The color is
	 * represented as a String
	 */
  public static final String HIGHLIGHT_COLOR =
    "StyledForestView.highlightColor";

  /** A reusable prototype for configuring the views */
  public static final StyledForestViewFactory prototype =
    new StyledForestViewFactory();

  public final AbstractForestToForestStatefulView configure(
    final AbstractForestToForestStatefulView src,
    final PredicateModel predModel,
    final StyleSetModel palette,
    final SlotFactory sf) {
    // Check if predModel is styled
    if (!predModel.isNodeAttribute(StyledView.STYLE)) {
      return null;
    }
    // Stored as a string: Oxff00ff
    final SlotInfo color =
      src.addNodeAttribute(HIGHLIGHT_COLOR, IRStringType.prototype, sf, true);

    StyleRebuilder styler = new StyleRebuilder(color, palette, predModel);
    src.addRebuilder(styler);

    final ModelListener ml = new ThreadedModelAdapter(styler);
    predModel.addModelListener(ml);
    palette.addModelListener(ml);

    return src;
  }
}

class StyleRebuilder extends ModelAdapter implements Rebuilder {
  /**
	 * Logger for this class
	 */
  static final Logger LOG = SLLogger.getLogger("MV.style.rebuild");

  public static void debug(String s) {
    LOG.fine(s);
  }

  static final InterruptedException cannedInterrupt =
    new InterruptedException();

  final SlotInfo color, style;
  final StyleSetModel palette;
  final PredicateModel predModel;

  StyleRebuilder(
    final SlotInfo color,
    final StyleSetModel pal,
    final PredicateModel preds) {
    this.color = color;
    palette = pal;
    predModel = preds;

    style = preds.getNodeAttribute(StyledView.STYLE);
  }

  /** Rebuilds style info for a ForestModel */
  @Override
  public void rebuild(ModelToModelStatefulView sv, List events) {
    try {
      rebuildModel(sv, events);
    } catch (InterruptedException e) {
      // should rebuild!
    }
  }

  /**
	 * Called whenever the styled PredicateModel changes.
	 */
  @Override
  public void breakView(ModelEvent e) {
    try {
      oneEvent.set(0, e);
      rebuildModel((Model) e.getSource(), oneEvent);
    } catch (InterruptedException ex) {
      // should rebuild!
    }
  }

  final List<ModelEvent> oneEvent = new ArrayList<ModelEvent>(1);

  /// Styling algorithm
  private final List<StyInfo> styInfo = new Vector<StyInfo>();

  static class StyInfo {
    SlotInfo attr;
    AttributePredicate pred;
    String style;
  }

  final boolean hasStyling(IRNode pred) {
    return pred.valueExists(style);
  }

  final IRNode getStyle(IRNode pred) {
    return (IRNode) pred.getSlotValue(style);
  }

  protected final void updateStyInfo(List events) throws InterruptedException {
    if (styInfo.size() > 0) {
      boolean update = false;
      for (Iterator i = events.iterator(); i.hasNext();) {
        ModelEvent m = (ModelEvent) i.next();
        if (m.getSource() instanceof PredicateModel) {
          update = true;
          break;
        }
      }
      if (!update)
        return;

      styInfo.clear();
    }

    for (Iterator it = predModel.getNodes(); it.hasNext();) {
      IRNode pred = (IRNode) it.next();

      if (predModel.isStyled(pred)) {
        StyInfo info = new StyInfo();
        info.attr = predModel.getAttribute(pred);
        info.pred = predModel.getPredicate(pred);

        if (!hasStyling(pred)) {
          throw new FluidError("No styling on predicate");
        }
        info.style = palette.getColor(getStyle(pred));
        styInfo.add(info);
      }
    }
  }

  protected final void rebuildModel(Model src, List l)
    throws InterruptedException {
    debug("Rebuilding styled forest view");
    // synchronized( structLock ) {
    currentThread = Thread.currentThread();

    // Clear the old labels? (just relabel everything in the model)

    try {
      // pre-compute style info for each predicate
      updateStyInfo(l);

      // Label nodes
      // FIX assuming one style/color dominates
      // 
      for (Iterator i = src.getNodes(); i.hasNext();) {
        IRNode n = (IRNode) i.next();
        String c = "0x000000";

        if (currentThread.isInterrupted())
          throw cannedInterrupt;

        for (Iterator it = styInfo.iterator(); it.hasNext();) {
          StyInfo info = (StyInfo) it.next();

          if (n.valueExists(info.attr)
            && info.pred.includesValue(n.getSlotValue(info.attr))) {
            // it's a match, so set it as a possible style
            c = info.style;

            // FIX merge styles?
            break;
          }
        }
        n.setSlotValue(color, c);
      }
    } catch (Exception exp) {
      debug("caught an exception when rebuilding the diff: " + exp);
      exp.printStackTrace();
    }
    //}
    // Break our views
    // modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  Thread currentThread;
}
