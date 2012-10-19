package com.surelogic.common.xml;

import junit.framework.TestCase;

public final class TestEntities extends TestCase {

  public void testDEFAULTSingleChar() {
    assertEquals("&gt;", Entities.Holder.DEFAULT.escape(">"));
    assertEquals("&lt;", Entities.Holder.DEFAULT.escape("<"));
    assertEquals("&amp;", Entities.Holder.DEFAULT.escape("&"));
    assertEquals("&apos;", Entities.Holder.DEFAULT.escape("\'"));
    assertEquals("&quot;", Entities.Holder.DEFAULT.escape("\""));
  }

  public void testDEFAULTAllChar() {
    assertEquals("&gt;&lt;&amp;&apos;&quot;", Entities.Holder.DEFAULT.escape("><&\'\""));
  }

  public void testDEFAULTStrings() {
    assertEquals("&lt;&lt;&lt;&lt;---&lt;&lt;&lt;&lt;", Entities.Holder.DEFAULT.escape("<<<<---<<<<"));
    assertEquals("To be &amp; not to be", Entities.Holder.DEFAULT.escape("To be & not to be"));
    assertEquals("&apos;single&apos;", Entities.Holder.DEFAULT.escape("\'single\'"));
    assertEquals("&quot;double&quot;", Entities.Holder.DEFAULT.escape("\"double\""));
    assertEquals("&gt;to do\n&lt;\twork", Entities.Holder.DEFAULT.escape(">to do\n<\twork"));
  }

}
