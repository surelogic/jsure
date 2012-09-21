// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/TestAttributeView.java,v 1.9 2003/07/15 18:39:12 thallora Exp $
package edu.cmu.cs.fluid.mvc.attr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.StringTokenizer;

import edu.cmu.cs.fluid.mvc.sequence.LabeledSequence;
import edu.cmu.cs.fluid.mvc.sequence.SimpleLabeledSequenceFactory;
import edu.cmu.cs.fluid.mvc.set.SetDumper;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;

public class TestAttributeView {
  public static void main(final String[] args) throws Exception {
    // Init source sequence
    final LabeledSequence seq = SimpleLabeledSequenceFactory.mutablePrototype.create("My Test Sequence",
        SimpleSlotFactory.prototype);

    // Init AttributeModel
    final AttributeModel attrModel = SimpleAttributeViewFactory.prototype.create("Attribute Model", seq);

    // Init set dumper
    final SetDumper dumper = new SetDumper(attrModel, System.out);
    dumper.dumpModel(attrModel);

    final IRNode[] attrs = new IRNode[attrModel.size()];
    final Iterator<IRNode> nodes = attrModel.getNodes();
    for (int count = 0; nodes.hasNext();) {
      attrs[count++] = nodes.next();
    }

    PickledAttributeModelState pickledState = null;

    final BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    while (true) {
      System.out.println("quit, save, restore, help, label # <label>");
      System.out.print("> ");
      System.out.flush();

      final String cmd = input.readLine().trim();
      System.out.println("cmd is \"" + cmd + "\"");

      if ("save".equals(cmd)) { // save state
        pickledState = attrModel.getPickledState();
        System.out.println("\nPickle = " + pickledState);
      } else if ("restore".equals(cmd)) { // restore from pickle
        attrModel.setStateFromPickle(pickledState);
        dumper.waitForBreak();
      } else if ("help".equals(cmd)) {
        for (int i = 0; i < attrs.length; i++) {
          System.out.println(i + " = " + attrModel.getName(attrs[i]));
        }
        System.out.println();
      } else if ("quit".equals(cmd)) {
        System.exit(0);
      } else {
        final StringTokenizer st = new StringTokenizer(cmd);
        if (st.hasMoreTokens()) {
          if ("label".equals(st.nextToken())) {
            final IRNode n = attrs[Integer.parseInt(st.nextToken())];
            final String l = st.nextToken();
            attrModel.setLabel(n, l);
            dumper.waitForBreak();
          }
        }
      }
    }
  }
}
