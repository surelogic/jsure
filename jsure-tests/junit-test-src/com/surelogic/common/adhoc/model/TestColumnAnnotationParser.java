package com.surelogic.common.adhoc.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import com.surelogic.common.Justification;

public class TestColumnAnnotationParser extends TestCase {

  private ColumnAnnotation c;
  private Set<Integer> one;
  private Set<Integer> oneThruFive;

  @Override
  protected void setUp() throws Exception {
    one = new HashSet<Integer>();
    one.add(1);
    oneThruFive = new HashSet<Integer>();
    oneThruFive.add(1);
    oneThruFive.add(2);
    oneThruFive.add(3);
    oneThruFive.add(4);
    oneThruFive.add(5);
  }

  @Override
  protected void tearDown() throws Exception {
    c = null;
    one = null;
    oneThruFive = null;
  }

  public void testEmpty() {
    c = ColumnAnnotationParserUtility.parse("");
    assertTrue(c.isValid());
    assertFalse(c.isLastTreeColumn());
    assertFalse(c.isLastInitiallyVisible());
    assertEquals(c.getJustification(), Justification.LEFT);
    assertFalse(c.isHidden());
    assertFalse(c.definesAnIconForAnotherColumn());
    assertNull(c.getIconName());
    assertFalse(c.sumPartialRows());
    assertFalse(c.countPartialRows());
    assertFalse(c.countDistinct());
    assertEquals("", c.getAggregateSuffix());
    assertEquals(Collections.emptySet(), c.getOnSet());
  }

  public void testJustification() {
    c = ColumnAnnotationParserUtility.parse("(r)");
    assertTrue(c.isValid());
    assertEquals(c.getJustification(), Justification.RIGHT);
    c = ColumnAnnotationParserUtility.parse("(right)");
    assertTrue(c.isValid());
    assertEquals(c.getJustification(), Justification.RIGHT);

    c = ColumnAnnotationParserUtility.parse("(c)");
    assertTrue(c.isValid());
    assertEquals(c.getJustification(), Justification.CENTER);
    c = ColumnAnnotationParserUtility.parse("(center)");
    assertTrue(c.isValid());
    assertEquals(c.getJustification(), Justification.CENTER);

    c = ColumnAnnotationParserUtility.parse("(l)");
    assertTrue(c.isValid());
    assertEquals(c.getJustification(), Justification.LEFT);
    c = ColumnAnnotationParserUtility.parse("(left)");
    assertTrue(c.isValid());
    assertEquals(c.getJustification(), Justification.LEFT);
  }

  public void testTreeTable() {
    c = ColumnAnnotationParserUtility.parse("|");
    assertTrue(c.isValid());
    assertTrue(c.isLastTreeColumn());
    assertFalse(c.isLastInitiallyVisible());
    c = ColumnAnnotationParserUtility.parse("]");
    assertTrue(c.isValid());
    assertFalse(c.isLastTreeColumn());
    assertTrue(c.isLastInitiallyVisible());
    c = ColumnAnnotationParserUtility.parse("]|");
    assertTrue(c.isValid());
    assertTrue(c.isLastTreeColumn());
    assertTrue(c.isLastInitiallyVisible());
    c = ColumnAnnotationParserUtility.parse("|]");
    assertTrue(c.isValid());
    assertTrue(c.isLastTreeColumn());
    assertTrue(c.isLastInitiallyVisible());
  }

  public void testHide() {
    c = ColumnAnnotationParserUtility.parse("(hide)");
    assertTrue(c.isValid());
    assertTrue(c.isHidden());
  }

  public void testIcon() {
    c = ColumnAnnotationParserUtility.parse("(icon)");
    assertTrue(c.isValid());
    assertTrue(c.definesAnIconForAnotherColumn());
    String iconName = "plug.gif";
    c = ColumnAnnotationParserUtility.parse("('" + iconName + "')");
    assertTrue(c.isValid());
    assertEquals(iconName, c.getIconName());
    assertTrue(c.getShowIconWhenEmpty());
    c = ColumnAnnotationParserUtility.parse("('" + iconName + "'...)");
    assertTrue(c.isValid());
    assertEquals(iconName, c.getIconName());
    assertFalse(c.getShowIconWhenEmpty());
    iconName = "Plus.GIF";
    c = ColumnAnnotationParserUtility.parse("('" + iconName + "')");
    assertTrue(c.isValid());
    assertEquals(iconName, c.getIconName());
    assertTrue(c.getShowIconWhenEmpty());
    c = ColumnAnnotationParserUtility.parse("('" + iconName + "'  ...   )");
    assertTrue(c.isValid());
    assertEquals(iconName, c.getIconName());
    assertFalse(c.getShowIconWhenEmpty());
    c = ColumnAnnotationParserUtility.parse("('')");
    assertTrue(c.isValid());
    assertNull(c.getIconName());
  }

  public void testSum() {
    c = ColumnAnnotationParserUtility.parse("(sum)");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(sum ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(sum on 1 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(sum on 1,2,3,4,5 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(oneThruFive, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
  }

  public void testCount() {
    c = ColumnAnnotationParserUtility.parse("(count)");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertFalse(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertFalse(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count on 1 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertFalse(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count on 1,2,3,4,5 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertFalse(c.countDistinct());
    assertEquals(oneThruFive, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
  }

  public void testCountDistinct() {
    c = ColumnAnnotationParserUtility.parse("(count distinct)");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct on 1 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct on 1,2,3,4,5 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(oneThruFive, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
  }

  public void testCountReplaceValuesWithOne() {
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with '1')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "1");
    assertFalse(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with '')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "");
    assertFalse(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with 'one' ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertFalse(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with 'one' on 1 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertFalse(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with '1' on 1,2,3,4,5 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "1");
    assertFalse(c.countDistinct());
    assertEquals(oneThruFive, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
  }

  public void testCountReplaceValuesWithOneDistinct() {
    c = ColumnAnnotationParserUtility.parse("(count distinct replace-value-with 'one')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct replace-value-with 'one' ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct replace-value-with 'one' on 1 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertTrue(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct replace-value-with 'one' on 1,2,3,4,5 ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertTrue(c.countDistinct());
    assertEquals(oneThruFive, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
  }

  public void testBad() {
    c = ColumnAnnotationParserUtility.parse("()");
    assertFalse(c.isValid());
    c = ColumnAnnotationParserUtility.parse("(***hide)");
    assertFalse(c.isValid());
    c = ColumnAnnotationParserUtility.parse("(c)(r)(HIDE)");
    assertFalse(c.isValid());
  }
}