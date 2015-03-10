package edu.cmu.cs.fluid.pattern;

class USet
{
  private boolean ans;
  private Integer wc;

  public USet(boolean a, Integer w)
  {
    ans = a;
    wc = w;
  }

  public boolean containswc()
  {
    return ans;
  }

  public Integer getwc()
  {
    return wc;
  }
}