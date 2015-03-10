package edu.cmu.cs.fluid.pattern;

import java.util.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.*;

public class CTComp implements Comparator
{
  private SyntaxTree st;

  public CTComp(SyntaxTree synt)
  {
    st = synt;
  }
  
  @Override
  public int compare(Object x, Object y)
  {
    IRNode xn = (IRNode)x;
    IRNode yn = (IRNode)y;

    if (st.numChildren(xn) < st.numChildren(yn))
      return -1;
    else if (st.numChildren(xn) > st.numChildren(yn))
      return 1;
    else if (st.getOperator(xn).name().compareTo(
             st.getOperator(yn).name()) != 0)
      return st.getOperator(xn).name().compareTo(
             st.getOperator(yn).name());
    else
    {
      int tmp;
      for (int c=0; c<st.numChildren(xn); c++)
      {
        tmp = compare(st.getChild(xn, c), st.getChild(yn, c));
        if (tmp != 0)
          return tmp;
      }
    }
    return 0;
  }
}
