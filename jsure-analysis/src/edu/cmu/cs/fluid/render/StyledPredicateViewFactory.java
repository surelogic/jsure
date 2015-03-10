package edu.cmu.cs.fluid.render;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;

public final class StyledPredicateViewFactory
// implements StyledPredicateView.Factory
{
  static void debug(String message) {
    Model.MV.fine(message);
  }

  public static final StyledPredicateViewFactory prototype =
    new StyledPredicateViewFactory();

  /*
	 * public StyledPredicateView create( final String name, final PredicateModel
	 * src, final StyleSetModel palette, final SlotFactory sf ) throws
	 * SlotAlreadyRegisteredException { return new StyledPredicateViewImpl(name,
	 * src, sf, new ModelCore.StandardFactory( sf ), ViewCore.standardFactory,
	 * LocalAttributeManagerFactory.prototype,
	 * BasicAttributeInheritanceManagerFactory.prototype, palette); }
	 */

  public PredicateModel configure(
    final PredicateModel src,
    final StyleSetModel palette,
    final SlotFactory sf) {
    if (!src.isNodeAttribute(StyledView.STYLE)) {
      final String name = src.getName();
      final SlotFactory msf = new MySlotFactory(palette, sf);
      src.addNodeAttribute(StyledView.STYLE, IRNodeType.prototype, msf, true);

      System.err.println("this      = " + this);
      System.err.println("predModel = " + name);
      System.err.println("palette   = " + palette.getName());
    }
    return src;
  }

  private static class MySlotFactory extends UnimplementedSlotFactory {
    final StyleSetModel palette;
    final SlotFactory sf;

    MySlotFactory(final StyleSetModel palette, final SlotFactory sf) {
      this.palette = palette;
      this.sf = sf;
    }

    /**
		 * Create a new named (possibly persistent) attribute wiith a default
		 * value.
		 * 
		 * @throws SlotAlreadyRegisteredException
		 *           if a slot with this name already exists. @precondition
		 *           nonNull(name)
		 */
    @Override
    public SlotInfo newAttribute(String name, IRType type)
      throws SlotAlreadyRegisteredException {
      final SlotInfo<IRNode> realStyle =
        sf.newAttribute(
          name + "-real-" + StyledView.STYLE,
          IRNodeType.prototype);
      return new MySlotInfo(realStyle, palette);
    }
  }

  private static class MySlotInfo extends SlotInfoWrapper<IRNode> {
    final StyleSetModel palette;

    MySlotInfo(SlotInfo<IRNode> realSI, StyleSetModel palette) {
      super(realSI);
      this.palette = palette;
    }

    @Override
    protected boolean valueExists(IRNode n) {
      return true;
    }

    @Override
    protected IRNode getSlotValue(IRNode pred) {
      if (super.valueExists(pred)) {
        return super.getSlotValue(pred);
      }
      // create a style
      final IRNode style = StylePalette.getNewStyle(palette);
      debug("Assigning a new style " + style + " to " + pred);
      super.setSlotValue(pred, style);

      // Does this cause any events?
      return style;
    }
  }
}
