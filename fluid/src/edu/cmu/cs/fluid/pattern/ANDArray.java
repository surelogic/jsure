package edu.cmu.cs.fluid.pattern;

import java.util.*;

@SuppressWarnings("all")
class ANDArray
{
  private int[] dims;
  private ArrayList anda;
  private int[] cit;

  public ANDArray(ArrayList al)
  {
    dims = new int[al.size()];
    for (int c=0; c<al.size(); c++)
      dims[c] = ((Integer)al.get(c)).intValue();

    cit = new int[al.size()];
   
    anda = new ArrayList(dims[0]);
    recinit(anda, 0);
  }

  private void recinit(ArrayList parent, int pos)
  {
    if (pos < dims.length-1)
    {
      ArrayList tal;
      for (int c=0; c<dims[pos]; c++)
      {
        tal = new ArrayList(dims[pos+1]);
        recinit(tal, pos+1);
        parent.add(c, tal);
      }
    } 
  }

  public boolean set(int[] cord, Integer val)
  {
    if (cord.length != dims.length)
      return false;
    else
    {
      ArrayList al = anda;
      for (int c=0; c<cord.length; c++)
      {
        if ((cord[c] < 0) || (cord[c] > dims[c]))
          return false;
        else
        {
          if (c < cord.length-1)
            al = (ArrayList)al.get(cord[c]);
          else
            al.add(cord[c], val);
        }  
      }  
    }
    return true;
  }

  public Integer get(int[] cord)
  {
    if (cord.length != dims.length)
      return null;
    else
    {
      ArrayList al = anda;
      for (int c=0; c<cord.length; c++)
      {
        if ((cord[c] < 0) || (cord[c] > dims[c]))
          return null;
        else
        {
          if (c < cord.length-1)
            al = (ArrayList)al.get(cord[c]);
          else
            return (Integer)al.get(cord[c]);
        }  
      }  
    }
    return null;
  }

  public void resetIt()
  {
    for (int c=0; c<cit.length; c++)
      cit[c] = -1;
  }   

  public boolean hasNext()
  {
    for (int c=0; c<dims.length; c++)
      if (cit[c] < dims[c]-1)
        return true;
    return false;
  }

  public int[] next()
  {
    int c = 0;
    boolean carry = true;
    while ((c < cit.length) && (carry))
    {
      cit[c] = (cit[c]+1) % dims[c];
      carry = cit[c++] == 0;
    }
    return cit;
  }
}