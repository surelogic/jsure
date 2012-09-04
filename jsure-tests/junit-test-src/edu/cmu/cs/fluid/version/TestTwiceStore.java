/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TestTwiceStore.java,v 1.1 2007/08/21 23:35:57 boyland Exp $*/
package edu.cmu.cs.fluid.version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.IRIntegerType;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.IRSequenceType;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.UniqueID;

/**
 * Test case for BUG 892: Canonicalized Tree not persisted correctly.
 * This test takes three runs:
 * <ol>
 * <li> Define a list, insert two element, persist.
 * <li> Load in list, insert a new element, persist change.
 * <li> Load in both deltas, get last element in list.
 * </ol>
 * @author boyland
 */
public class TestTwiceStore {

  final static FileLocator floc = IRPersistent.fluidFileLocator;
  
  SlotInfo<IRSequence<Integer>> attr;
  { 
    try {
      attr = VersionedSlotFactory.prototype.newAttribute("TEST.attr", 
          new IRSequenceType<Integer>(IRIntegerType.prototype));
    } catch (SlotAlreadyRegisteredException e) {
      e.printStackTrace();
    }
  }
  
  final Bundle bundle;
  final VersionedRegion region;
  final Era era[];

  final IRNode n1;
  final IRNode n2;
  
  TestTwiceStore() throws IOException {
    bundle = new Bundle();
    bundle.saveAttribute(attr);
    era = new Era[1];
    Version init = Version.getInitialVersion();
    Version.setVersion(init);
    era[0] = new Era(init);
    Version.setDefaultEra(era[0]);
    region = new VersionedRegion();
    Version.bumpVersion();
    n1 = new PlainIRNode(region);
    n2 = new PlainIRNode(region);
    IRSequence<Integer> seq1 = VersionedSlotFactory.dependent.<Integer>newSequence(~0);
    IRSequence<Integer> seq2 = VersionedSlotFactory.dependent.<Integer>newSequence(~0);
    n1.setSlotValue(attr, seq1);
    n2.setSlotValue(attr, seq2);
    System.out.println("Verison is " + Version.getVersion());
    seq1.insertElement(1776);
    seq2.insertElement(0xFAFA);
    System.out.println("Verison is " + Version.getVersion());
    seq1.insertElement(1066);
    seq2.appendElement(0xE1E10);
    System.out.println("Verison is " + Version.getVersion());
    era[0].complete();
    era[0].describe(System.out);
    region.finishNodes();
    region.store(floc);
    bundle.store(floc);
    era[0].store(floc);
    VersionedChunk.get(region, bundle).getDelta(era[0]).store(floc);
    OutputStream fw = floc.openFileWrite("TestTwiceStored1.out");
    Writer fr = new OutputStreamWriter(fw);
    fr.write(region.getID()+"\n");
    fr.write(bundle.getID()+"\n");
    fr.write(era[0].getID()+"\n");
    fr.close();
  }
  
  TestTwiceStore(UniqueID rid, UniqueID bid, UniqueID eid) throws IOException {
    region = VersionedRegion.loadVersionedRegion(rid, floc);
    bundle = Bundle.loadBundle(bid, floc);
    era = new Era[2];
    era[0] = Era.loadEra(eid, floc);
    n1 = region.getDelta(era[0]).getNode(1);
    n2 = region.getDelta(era[0]).getNode(2);
    VersionedChunk.get(region, bundle).getDelta(era[0]).load(floc);
  }
  
  void doMiddleMutation() throws IOException {
    Version v= era[0].getVersion(era[0].maxVersionOffset());
    Version.setVersion(v);
    era[1] = new Era(v);
    Version.setDefaultEra(era[1]);
    IRSequence<Integer> seq1 = n1.getSlotValue(attr);
    IRSequence<Integer> seq2 = n2.getSlotValue(attr);
    seq1.insertElement(410);
    seq2.appendElement(0x7777);
    era[1].complete();
    era[1].store(floc);
    VersionedChunk.get(region, bundle).getDelta(era[1]).store(floc);
    OutputStream fw = floc.openFileWrite("TestTwiceStored2.out");
    Writer fr = new OutputStreamWriter(fw);
    fr.write(era[1].getID()+"\n");
    fr.close();
  }
  
  void testState(IRNode n) throws IOException {
    IRSequence<Integer> seq = n.getSlotValue(attr);
    int size = seq.size();
    seq.describe(System.out);
    for (int i=0; i < size; ++i) {
      try {
        if (seq.validAt(i)) {
          System.out.print("Valid at " + i + ": ");
          System.out.println(seq.elementAt(i));
        } else {
          System.out.println("Not valid at " + i);
        }
      } catch (RuntimeException e) {
        System.out.println("Error at " + i + ": " + e);
      }
    }
  }
  
  TestTwiceStore(UniqueID rid, UniqueID bid, UniqueID eid1, UniqueID eid2) throws IOException {
    this(rid,bid,eid1);
    era[1] = Era.loadEra(eid2,floc);
    VersionedChunk.get(region, bundle).getDelta(era[1]).load(floc);
    Version.setVersion(era[1].getVersion(era[1].maxVersionOffset()));
  }
  
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    VersionedRegion.ensureLoaded();
    VersionedChunk.ensureLoaded();
    Era.ensureLoaded();
    Bundle.ensureLoaded();
    IRPersistent.setTraceIO(true);
    TestTwiceStore tts;
    Version.getInitialVersion().getEra().complete();
    try {
      switch (Integer.parseInt(args[0])) {
      default:
      case 0: tts = new TestTwiceStore(); break;
      case 1 : {
        BufferedReader br = new BufferedReader(new InputStreamReader(floc.openFileRead("TestTwiceStored1.out")));
        UniqueID rid, bid, eid;
        rid = UniqueID.parseUniqueID(br.readLine());
        bid = UniqueID.parseUniqueID(br.readLine());
        eid = UniqueID.parseUniqueID(br.readLine());
        tts = new TestTwiceStore(rid,bid,eid);
        tts.doMiddleMutation();
      } break;
      case 2 : {
        BufferedReader br = new BufferedReader(new InputStreamReader(floc.openFileRead("TestTwiceStored1.out")));
        UniqueID rid, bid, eid1, eid2;
        rid = UniqueID.parseUniqueID(br.readLine());
        bid = UniqueID.parseUniqueID(br.readLine());
        eid1 = UniqueID.parseUniqueID(br.readLine());
        br = new BufferedReader(new InputStreamReader(floc.openFileRead("TestTwiceStored2.out")));
        eid2 = UniqueID.parseUniqueID(br.readLine());
        tts = new TestTwiceStore(rid,bid,eid1,eid2);
       }
      }
      tts.testState(tts.n1);
      tts.testState(tts.n2);
    } catch (NumberFormatException e) {
      System.err.println("Must be int in range [0,3): " + args[0]);
    } catch (IOException e) {
      System.err.println(e);
    }
  }

}
