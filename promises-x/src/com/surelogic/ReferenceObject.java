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
 * Indicates that instances of this type are not fundamentally defined by their
 * attributes. A reference object has a meaningful identity separate from the
 * value of its fields&mdash;the reference to the object in the heap. Some
 * examples of types that define reference objects include {@link Thread} and
 * {@link Object}.
 * <p>
 * This annotation is related, but not entirely synonymous, with an <i>entity
 * object</i> in the software design literature. A reference object is a
 * lower-level concept that uses the reference to the object in the heap as its
 * identity. While an entity object could be implemented in Java using a
 * reference object, it could also use an <tt>id</tt> field or some other
 * mechanism. Therefore, all reference objects are entity objects, but not vice
 * versa.
 * <p>
 * All <i>enum</i> type declarations are reference objects simply due to the
 * restrictions imposed by the Java programming language. They are treated as if
 * they were annotated with <code>&#064;ReferenceObject</code>&mdash;but no
 * user-supplied annotation is needed, it is implied. It is a modeling problem
 * for any <i>enum</i> type to be annotated as either a
 * <code>&#064;ReferenceObject</code> or a <code>&#064;ValueObject</code>.
 * <p>
 * This annotation is currently verified, <em>but not defined</em>, by
 * restricting which {@link #equals(Object)} and {@link #hashCode()} methods
 * instances invoke at runtime. All reference objects <i>must</i> invoke these
 * methods on {@link java.lang.Object}&mdash;neither they nor their parent types
 * can override the {@link #equals(Object)} and {@link #hashCode()} methods.
 * <p>
 * Equality comparisons on reference objects are not checked because it is
 * possible to use either reference equality comparisons (<tt>==</tt>) or
 * {@link #equals(Object)} comparisons. This is because the
 * {@link java.lang.Object#equals(Object)} method is implemented using
 * <tt>==</tt> as shown below.
 * 
 * <pre>
 * public boolean equals(Object obj) {
 *   return (this == obj);
 * }
 * </pre>
 * <p>
 * A type may not be annotated with both <code>&#064;ReferenceObject</code> and
 * <code>&#064;ValueObject</code>.
 * <p>
 * Subtypes of a type other than {@link java.lang.Object} annotated with
 * <code>&#064;ReferenceObject</code> must also be explicitly annotated
 * <code>&#064;ReferenceObject</code>. It is a modeling error if they are not.
 * Note that it is <b>not</b> a modeling error to have one or more types in the
 * type hierarchy between {@link java.lang.Object} and the type annotated as a
 * <code>&#064;ReferenceObject</code> which are not annotated as a
 * <code>&#064;ReferenceObject</code> (this is to avoid unnecessary annotation
 * on the Java libraries).
 * <p>
 * An interface may be annotated with <code>&#064;ReferenceObject</code>. In
 * this case all subtypes must also be explicitly annotated with
 * <code>&#064;ReferenceObject</code>. It is a modeling error if they are not.
 * In some cases this may be useful to allow comparisons with the interface type
 * using <tt>==</tt> while being sure that no implementing classes override
 * {@link #equals(Object)} and {@link #hashCode()} methods&mdash;which would
 * cause the comparisons using <tt>==</tt> to be possibly incorrect.
 * <p>
 * An annotation type declaration may <b>not</b> be annotated with
 * <code>&#064;ReferenceObject</code>.
 * 
 * <h3>Semantics:</h3>
 * 
 * The identity of an object at runtime of a type annotated with
 * <code>&#064;ReferenceObject</code> is determined by its reference in the
 * heap. Two distinct reference objects in the heap are never equal.
 * 
 * <h3>Examples:</h3>
 * 
 * The below thread is annotated to use reference equality.
 * 
 * <pre>
 * &#064;ReferenceObject
 * public final class ServerProxy extends Thread {
 *   ...
 * }
 * </pre>
 * 
 * Note that neither {@link java.lang.Thread} or {@code ServerProxey} override
 * {@link #equals(Object)} and {@link #hashCode()} methods.
 * <p>
 * The {@code Point} class below would normally be annotated as a
 * <code>&#064;ValueObject</code>. However, a Java implementation of the
 * <i>Flyweight pattern</i> is used to allow it to use reference equality
 * semantics. This design pattern is described by Gamma, Helm, Johnson, and
 * Vlissides in <i>Design Patterns: Elements of Reusable Object-Oriented
 * Software</i> (Addison-Wesley 1995). Note that this code is likely to be
 * <b>much</b> slower than implementing {@code Point} as a value object.
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
public @interface ReferenceObject {
  // Marker annotation
}
