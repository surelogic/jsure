/*
 * Created on Oct 28, 2003
 *
 */
package edu.cmu.cs.fluid.ir;

/**
 * @author chance
 *
 */
public class TimeIRSequence {
  public static void main(String[] args) {
  	final int iters = 40000;
  	
  	final IRSequence<Integer> seq1 = SimpleSlotFactory.prototype.newSequence(10);
		initSequence(seq1);
  	
  	final long start1 = System.currentTimeMillis();
  	for(int i=0; i<iters; i++) {
      /*Iterator it = */ seq1.elements();
      /*
  		for (Object o : it) {        
  		}
      */
  	}  	
  	printDiff(start1, "Enumeration");
    
		final IRSequence<Integer> seq2 = SimpleSlotFactory.prototype.newSequence(10);
		initSequence(seq2);

		final long start2 = System.currentTimeMillis();
		for(int i=0; i<iters; i++) {
			IRLocation loc = seq2.firstLocation();
			while (loc != null) {
				seq2.elementAt(loc);
				loc = seq2.nextLocation(loc);
			}
		}  	
		printDiff(start2, "IRLocation");
  }

  static void initSequence(IRSequence<Integer> seq) {
  	final int size = seq.size();
  	for(int i=0; i<size;i++) {
  		seq.setElementAt(i, i);
  	}
  }

  static void printDiff(long start, String msg) {
		long end = System.currentTimeMillis();  	
		System.out.println(msg+" : "+(end-start));
  }
}
