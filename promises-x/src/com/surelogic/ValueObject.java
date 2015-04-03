/*
 * Copyright (c) 2015 SureLogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that instances of this type are fundamentally defined by their
 * attributes. A value object has no meaningful identity separate from the value
 * of its fields. Some examples of types that define value objects include
 * {@link Boolean}, {@link Integer}, {@link java.util.ArrayList}, and
 * {@link java.util.HashSet}.
 * <p>
 * This annotation is currently verified, <em>but not defined</em>, by
 * restricting which {@link #equals(Object)} and {@link #hashCode()} methods
 * instances invoke at runtime. All value objects <i>must</i> override these
 * methods on {@link java.lang.Object}. The override of the
 * {@link #equals(Object)} and {@link #hashCode()} methods is allowed to be done
 * within a parent type (for a good example of this approach, see the
 * implementation of the {@link java.util.AbstractList} class&mdash;and note
 * that this class is declared <tt>abstract</tt>).
 * <p>
 * Equality comparisons on value objects are checked in code outside of its
 * defining type hierarchy: no client code may perform a comparison of a value
 * object with another object using <tt>==</tt>, the {@link #equals(Object)}
 * method must be used for all comparisons.
 * <p>
 * A type may not be annotated with both <code>&#064;ValueObject</code> and
 * <code>&#064;ReferenceObject</code>.
 * <p>
 * Subtypes of a type annotated with <code>&#064;ValueObject</code> must also be
 * explicitly annotated <code>&#064;ValueObject</code>. It is a modeling error
 * if they are not. In addition, only the leaf nodes of a hierarchy of classes
 * annotated with <code>&#064;ValueObject</code> are allowed to be
 * instantiable&mdash;all parent classes (except for {@link java.lang.Object})
 * must be declared <tt>abstract</tt>. This is because <b>there is no way to
 * extend an instantiable class and add a value component while preserving the
 * {@link #equals(Object)} contract</b> in the Java programming language. Please
 * refer to <i>Item 8: Obey the general contract when overriding equals</i> in
 * <i>Effective Java (Second Edition)</i> by Joshua Bloch (Addison-Wesley 2008).
 * for further information about this problem.
 * <p>
 * An interface may be annotated with <code>&#064;ValueObject</code>. In this
 * case all subtypes must also be explicitly annotated with
 * <code>&#064;ValueObject</code>. It is a modeling error if they are not. In
 * some cases this may be useful to ensure that no comparisons with the
 * interface type are using <tt>==</tt>.
 * <p>
 * An annotation type declaration may <b>not</b> be annotated with
 * <code>&#064;ValueObject</code>.
 * <p>
 * All <i>enum</i> type declarations are <i>reference objects</i> simply due the
 * restrictions imposed by the Java programming language. They are treated as if
 * they were annotated with <code>&#064;ReferenceObject</code>&mdash;but no
 * user-supplied annotation is needed, it is implied. It is a modeling problem
 * for any <i>enum</i> type to be annotated as either an
 * <code>&#064;ReferenceObject</code> or a <code>&#064;ValueObject</code>.
 * 
 * <h3>Semantics:</h3>
 * 
 * The identity of an object at runtime of a type annotated with
 * <code>&#064;ValueObject</code> is determined by its attributes. Two distinct
 * value objects in the heap may be equal.
 * 
 * <h3>Examples:</h3>
 * 
 * The mutable object below represents a simple Cartesian coordinate. It
 * correctly overrides the {@link #equals(Object)} and {@link #hashCode()}
 * methods.
 * 
 * <pre>
 * &#064;Mutable
 * &#064;ValueObject
 * public final class Point {
 * 
 *   public Point(int x, int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   public Point(Point p) {
 *     x = p.x;
 *     y = p.y;
 *   }
 * 
 *   private int x, y;
 * 
 *   public int getX() {
 *     return x;
 *   }
 * 
 *   public void setX(int x) {
 *     this.x = x;
 *   }
 * 
 *   public int getY() {
 *     return y;
 *   }
 * 
 *   public void setY(int y) {
 *     this.y = y;
 *   }
 * 
 *   &#064;Override
 *   public int hashCode() {
 *     final int prime = 31;
 *     int result = 1;
 *     result = prime * result + x;
 *     result = prime * result + y;
 *     return result;
 *   }
 * 
 *   &#064;Override
 *   public boolean equals(Object obj) {
 *     if (this == obj)
 *       return true;
 *     if (obj == null)
 *       return false;
 *     if (getClass() != obj.getClass())
 *       return false;
 *     Point other = (Point) obj;
 *     if (x != other.x)
 *       return false;
 *     if (y != other.y)
 *       return false;
 *     return true;
 *   }
 * }
 * </pre>
 * 
 * The following client code would be identified as problematic.
 * 
 * <pre>
 * public class Base {
 * 
 *   Point center = new Point(1, 1);
 * 
 *   public void setCenter(Point p) {
 *     if (p != null &amp;&amp; p != center) {
 *       center = p;
 *     }
 *   }
 * 
 *   public boolean isCenter(Point p) {
 *     return p != null &amp;&amp; p == center;
 *   }
 *   ...
 * }
 * </pre>
 * 
 * The comparison <tt>p != center</tt> in the <tt>setCenter</tt> method uses
 * reference equality rather than value equality&mdash;it should be replaced
 * with either <tt>!p.equals(center)</tt> or <tt>!center.equals(p)</tt>. The
 * comparison <tt>p == center</tt> in the <tt>isCenter</tt> method also uses
 * reference equality rather than value equality&mdash;it should be replaced
 * with either <tt>p.equals(center)</tt> or <tt>center.equals(p)</tt>.
 * <p>
 * It is also possible to have immutable value objects, we could change
 * {@code Point}'s implementation to be immutable as shown in the example code
 * below.
 * 
 * <pre>
 * &#064;Immutable
 * &#064;ValueObject
 * public final class Point {
 * 
 *   public Point(int x, int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   public Point(Point p) {
 *     x = p.x;
 *     y = p.y;
 *   }
 * 
 *   private final int x, y;
 * 
 *   public int getX() {
 *     return x;
 *   }
 * 
 *   public int getY() {
 *     return y;
 *   }
 * 
 *   &#064;Override
 *   public int hashCode() {
 *     // SAME
 *   }
 * 
 *   &#064;Override
 *   public boolean equals(Object obj) {
 *     // SAME
 *   }
 * }
 * </pre>
 * 
 * Note that the client code shown in the {@code Base} class above would
 * <b>still be problematic</b> even if the immutable implementation of
 * {@code Point} is used. To change the {@code Point} class into a reference
 * object a Java implementation of the <i>Flyweight pattern</i> must be
 * implemented. This design pattern is described by Gamma, Helm, Johnson, and
 * Vlissides in <i>Design Patterns: Elements of Reusable Object-Oriented
 * Software</i> (Addison-Wesley 1995). One such approach is shown in the code
 * below. Note that the code below is likely to be <b>much</b> slower than
 * implementing {@code Point} as we did above.
 * 
 * <pre>
 * &#064;Immutable
 * &#064;ReferenceObject
 * public final class Point {
 * 
 *   private static final List&lt;Point&gt; INSTANCES = new ArrayList&lt;Point&gt;();
 * 
 *   public static Point getOnlyInstance(int x, int y) {
 *     for (Point p : INSTANCES) {
 *       if (p.x == x &amp;&amp; p.y == y)
 *         return p;
 *     }
 *     Point result = new Point(x, y);
 *     INSTANCES.add(result);
 *     return result;
 *   }
 * 
 *   private Point(int x, int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   public Point(Point p) {
 *     x = p.x;
 *     y = p.y;
 *   }
 * 
 *   private final int x, y;
 * 
 *   public int getX() {
 *     return x;
 *   }
 * 
 *   public int getY() {
 *     return y;
 *   }
 * }
 * </pre>
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ValueObject {
  // Marker annotation
}
