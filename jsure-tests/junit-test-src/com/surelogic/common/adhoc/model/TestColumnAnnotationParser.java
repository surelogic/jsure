package com.surelogic.common.adhoc.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

  public void testAddCommas() {
    c = ColumnAnnotationParserUtility.parse("(add-commas)");
    assertTrue(c.isValid());
    assertTrue(c.getAddCommas());
    c = ColumnAnnotationParserUtility.parse("(sum)");
    assertTrue(c.isValid());
    assertFalse(c.getAddCommas());
  }

  public void testHumanReadableDuration() {
    c = ColumnAnnotationParserUtility.parse("(human-readable-duration)");
    assertTrue(c.isValid());
    assertTrue(c.getHumanReadableDuration());
    assertSame(TimeUnit.NANOSECONDS, c.getHumanReadableDurationUnit());
    c = ColumnAnnotationParserUtility.parse("(human-readable-duration unit 'SECONDS')");
    assertTrue(c.isValid());
    assertTrue(c.getHumanReadableDuration());
    assertSame(TimeUnit.SECONDS, c.getHumanReadableDurationUnit());
    c = ColumnAnnotationParserUtility.parse("(human-readable-duration unit 'BOGAS IGNORE ME')");
    assertTrue(c.isValid());
    assertTrue(c.getHumanReadableDuration());
    assertSame(TimeUnit.NANOSECONDS, c.getHumanReadableDurationUnit());
  }
  
  public void testBlankIf() {
    c = ColumnAnnotationParserUtility.parse("(hide)");
    assertTrue(c.isValid());
    assertNull(c.getBlankIf());
    c = ColumnAnnotationParserUtility.parse("(blank-if '')");
    assertTrue(c.isValid());
    assertNull(c.getBlankIf());
    c = ColumnAnnotationParserUtility.parse("(blank-if '0')");
    assertTrue(c.isValid());
    assertEquals("0", c.getBlankIf());
    c = ColumnAnnotationParserUtility.parse("(blank-if 'test do it')");
    assertTrue(c.isValid());
    assertEquals("test do it", c.getBlankIf());
  }

  public void testPrefix() {
    c = ColumnAnnotationParserUtility.parse("(prefix '')");
    assertTrue(c.isValid());
    assertEquals("", c.getPrefix());
    assertEquals("", c.getSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix ' ms')");
    assertTrue(c.isValid());
    assertEquals(" ms", c.getPrefix());
    assertEquals("", c.getSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'test do it')");
    assertTrue(c.isValid());
    assertEquals("test do it", c.getPrefix());
    assertEquals("", c.getSuffix());
  }

  public void testSuffix() {
    c = ColumnAnnotationParserUtility.parse("(suffix '')");
    assertTrue(c.isValid());
    assertEquals("", c.getPrefix());
    assertEquals("", c.getSuffix());
    c = ColumnAnnotationParserUtility.parse("(suffix ' ms')");
    assertTrue(c.isValid());
    assertEquals("", c.getPrefix());
    assertEquals(" ms", c.getSuffix());
    c = ColumnAnnotationParserUtility.parse("(suffix 'test do it')");
    assertTrue(c.isValid());
    assertEquals("", c.getPrefix());
    assertEquals("test do it", c.getSuffix());
  }

  public void testAffix() {
    c = ColumnAnnotationParserUtility.parse("(prefix 'value=')(suffix ' ms')");
    assertTrue(c.isValid());
    assertEquals("value=", c.getPrefix());
    assertEquals(" ms", c.getSuffix());
    c = ColumnAnnotationParserUtility.parse("(suffix ' ms')(prefix 'value=')");
    assertTrue(c.isValid());
    assertEquals("value=", c.getPrefix());
    assertEquals(" ms", c.getSuffix());
  }

  public void testSum() {
    c = ColumnAnnotationParserUtility.parse("(sum)");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(sum suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(sum prefix 'v=')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("v=", c.getAggregatePrefix());
    c = ColumnAnnotationParserUtility.parse("(sum prefix 'v=' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("v=", c.getAggregatePrefix());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(sum)");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("p", c.getAggregatePrefix());
    assertEquals("s", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(sum prefix 'v=' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("v=", c.getAggregatePrefix());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(sum prefix '' suffix '')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("", c.getAggregatePrefix());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(sum on 1 suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.sumPartialRows());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(sum on 1,2,3,4,5 suffix ' ms')");
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
    c = ColumnAnnotationParserUtility.parse("(count suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertFalse(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count prefix 'v=')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("v=", c.getAggregatePrefix());
    c = ColumnAnnotationParserUtility.parse("(count prefix 'v=' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("v=", c.getAggregatePrefix());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(count)");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("p", c.getAggregatePrefix());
    assertEquals("s", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(count prefix 'v=' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("v=", c.getAggregatePrefix());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(count prefix '' suffix '')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("", c.getAggregatePrefix());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count on 1 suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertFalse(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count on 1,2,3,4,5 suffix ' ms')");
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
    c = ColumnAnnotationParserUtility.parse("(count distinct suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct prefix 'v=')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("v=", c.getAggregatePrefix());
    c = ColumnAnnotationParserUtility.parse("(count distinct prefix 'v=' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("v=", c.getAggregatePrefix());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(count distinct)");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("p", c.getAggregatePrefix());
    assertEquals("s", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(count distinct prefix 'v=' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("v=", c.getAggregatePrefix());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(prefix 'p')(suffix 's')(count distinct prefix '' suffix '')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("p", c.getPrefix());
    assertEquals("s", c.getSuffix());
    assertEquals("", c.getAggregatePrefix());
    assertEquals("", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct on 1 suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertNull(c.getCountReplaceValueWith());
    assertTrue(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct on 1,2,3,4,5 suffix ' ms')");
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
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with 'one' prefix 'v=' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertFalse(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals("v=", c.getAggregatePrefix());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with 'one' on 1 suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertFalse(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count replace-value-with '1' on 1,2,3,4,5 suffix ' ms')");
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
    c = ColumnAnnotationParserUtility.parse("(count distinct replace-value-with 'one' suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertTrue(c.countDistinct());
    assertEquals(Collections.emptySet(), c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct replace-value-with 'one' on 1 suffix ' ms')");
    assertTrue(c.isValid());
    assertTrue(c.countPartialRows());
    assertEquals(c.getCountReplaceValueWith(), "one");
    assertTrue(c.countDistinct());
    assertEquals(one, c.getOnSet());
    assertEquals(" ms", c.getAggregateSuffix());
    c = ColumnAnnotationParserUtility.parse("(count distinct replace-value-with 'one' on 1,2,3,4,5 suffix ' ms')");
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