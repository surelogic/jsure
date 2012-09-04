// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/TestLabeledSequence.java,v 1.15 2007/06/04 16:55:01 aarong Exp $

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelDumper;
import edu.cmu.cs.fluid.mvc.ModelUtils;
import edu.cmu.cs.fluid.ir.*;

public class TestLabeledSequence {
  public static void main(final String[] args) throws Exception {
    final LabeledSequence seq =
      SimpleLabeledSequenceFactory.mutablePrototype.create(
        "My Test Sequence",
        SimpleSlotFactory.prototype);

    final SequenceDumper dumper = new SequenceDumper(seq, System.out);

    int count = 0;
    while (count < args.length) {
      count = processCmd(seq, args, count, dumper);
    }
    ModelUtils.shutdownChain(seq);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static int processCmd(
    final LabeledSequence seq,
    final String[] args,
    int current,
    ModelDumper dumper)
    throws NumberFormatException, InterruptedException {
    final String cmd = args[current++].intern();

    if (cmd == "a") { // append node: "a <label>"
      final String label = args[current++];
      System.out.println(">>>> Append \"" + label + "\" <<<<");
      final IRNode node = new PlainIRNode();
      seq.appendElement(node);
      dumper.waitForBreak();
      seq.setLabel(node, label);
      dumper.waitForBreak();
    } else if (cmd == "ie") { // insert element: "ie <label>"
      final String label = args[current++];
      System.out.println(">>>> Insert \"" + label + "\" <<<<");
      final IRNode node = new PlainIRNode();
      seq.insertElement(node);
      dumper.waitForBreak();
      seq.setLabel(node, label);
      dumper.waitForBreak();
    } else if (cmd == "iea") { // insert element after: "iea <label> <loc>"
      final String label = args[current++];
      final int loc = Integer.parseInt(args[current++]);
      System.out.println(
        ">>>> Insert \"" + label + "\" after " + loc + " <<<<");
      final IRLocation irloc = seq.location(loc);
      final IRNode node = new PlainIRNode();
      seq.insertElementAfter(node, irloc);
      dumper.waitForBreak();
      seq.setLabel(node, label);
      dumper.waitForBreak();
    } else if (cmd == "ieb") { // insert element before: "ieb <label> <loc>"
      final String label = args[current++];
      final int loc = Integer.parseInt(args[current++]);
      System.out.println(
        ">>>> Insert \"" + label + "\" before " + loc + " <<<<");
      final IRLocation irloc = seq.location(loc);
      final IRNode node = new PlainIRNode();
      seq.insertElementBefore(node, irloc);
      dumper.waitForBreak();
      seq.setLabel(node, label);
      dumper.waitForBreak();
    } else if (cmd == "r") { // remove element: "r <loc>"
      final int loc = Integer.parseInt(args[current++]);
      System.out.println(">>>> Remove at \"" + loc + "\" <<<<");
      final IRLocation irloc = seq.location(loc);
      seq.removeElementAt(irloc);
      dumper.waitForBreak();
    } else if (cmd == "s") { // set element at: "s <label> <loc>
      final String label = args[current++];
      final int loc = Integer.parseInt(args[current++]);
      System.out.println(
        ">>>> Set label to \"" + label + "\" at " + loc + " <<<<");
      final IRLocation irloc = seq.location(loc);
      final IRNode node = new PlainIRNode();
      seq.setElementAt(node, irloc);
      dumper.waitForBreak();
      seq.setLabel(node, label);
      dumper.waitForBreak();
    } else if (cmd == "an") { // add node: "an <label> (-|<loc>)"
      final String label = args[current++];
      IRLocation irloc = null;
      if (!args[current].equals("-")) {
        final int loc = Integer.parseInt(args[current++]);
        System.out.println(
          ">>>> addNode \"" + label + "\" at " + loc + " <<<<");
        irloc = seq.location(loc);
      } else {
        System.out.println(">>>> addNode \"" + label + "\" at - <<<<");
      }
      final IRNode node = new PlainIRNode();
      final AVPair[] attrs = new AVPair[(irloc == null ? 1 : 2)];
      attrs[0] = new AVPair(LabeledSequence.LABEL, label);
      if (irloc != null) {
        attrs[1] = new AVPair(SequenceModel.LOCATION, irloc);
      }
      seq.addNode(node, attrs);
      dumper.waitForBreak();
    } else if (cmd == "ml") { // move by setting location: "ml <old> <new>"
      final int loc1 = Integer.parseInt(args[current++]);
      final IRLocation irloc1 = seq.location(loc1);
      final int loc2 = Integer.parseInt(args[current++]);
      final IRLocation irloc2 = seq.location(loc2);
      System.out.println(
        ">>>> Located Move from \"" + loc1 + "\" to " + loc2 + " <<<<");

      final IRNode node = seq.elementAt(irloc1);
      final SlotInfo si = seq.getNodeAttribute(SequenceModel.LOCATION);
      node.setSlotValue(si, irloc2);
      dumper.waitForBreak();
    } else if (cmd == "mi") { // move by setting index: "mi <old> <new>"
      final int loc1 = Integer.parseInt(args[current++]);
      final IRLocation irloc1 = seq.location(loc1);
      final int loc2 = Integer.parseInt(args[current++]);
      System.out.println(
        ">>>> Indexed from \"" + loc1 + "\" to " + loc2 + " <<<<");

      final IRNode node = seq.elementAt(irloc1);
      final SlotInfo si = seq.getNodeAttribute(SequenceModel.INDEX);
      node.setSlotValue(si, Integer.valueOf(loc2));
      dumper.waitForBreak();
    } else if (cmd == "rn") { // rename node: "rn <idx> <lbl>"
      final int loc1 = Integer.parseInt(args[current++]);
      final IRLocation irloc = seq.location(loc1);
      final String label = args[current++];
      System.out.println(
        ">>>> Set label to \"" + label + "\" at " + loc1 + " <<<<");

      final IRNode node = seq.elementAt(irloc);
      final SlotInfo si = seq.getNodeAttribute(LabeledSequence.LABEL);
      node.setSlotValue(si, label);
      dumper.waitForBreak();
    }

    return current;
  }
}
