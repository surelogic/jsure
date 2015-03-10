/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/analysis/SampleClass.java,v 1.3 2007/09/06 03:07:20 boyland Exp $*/
package edu.uwm.cs.fluid.java.analysis;

/**
 * A sample class to be operated on by the analyses in this directory.
 * @author boyland
 */
public class SampleClass {
  static class Nested {
    static int field = 0;
    static {
      int x = 7;
      x = 4;
      field = x;
    }
  }
   public int controlFlow(int a) {
     int j=45;
     for (int loop_i=0; loop_i < a; ++loop_i) {
       int k=a-loop_i;
       if (loop_i+loop_i == a && (loop_i&5)==1) {
         ++k;
         continue;
       }
       if (loop_i+loop_i+loop_i == a || loop_i*3 == a) {
         try {
           if (k < j) throw new RuntimeException("oops!");
         } catch (Exception e) {
           ++k;
           return k+a;
         } finally {
           if (loop_i == j) break;
           if (this.equals(null)) continue;
         }
       }
     }
     return a+=4;
   }
}
