/*
 * Copyright (c) 2005 Brian Goetz and Tim Peierls
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 *
 * Any republication or derived work distributed in source code form
 * must include this copyright and license notice.
 * 
 * 
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
 * The type to which this annotation is applied is not thread-safe. This
 * annotation primarily exists for clarifying the non-thread-safety of a class
 * that might otherwise be assumed to be thread-safe, despite the fact that it
 * is a bad idea to assume a class is thread-safe without good reason.
 * <p>
 * This annotation is <em>not verified</em>, it is intended for documentation of
 * programmer intent only.
 * <p>
 * A type may not be annotated with both <code>&#064;ThreadSafe</code> and
 * <code>&#064;NotThreadSafe</code> except in the case that one refers to the
 * static portion of the declaration and the other refers to the instance
 * portion of the declaration.
 * <p>
 * An annotation type declaration may be annotated with
 * <code>&#064;NotThreadSafe</code> so that any static fields in the type are
 * not to be considered thread-safe. It is, however, a modeling error to
 * restrict the annotation to the instance part.
 * 
 * <h3>Relationship with <code>&#064;Immutable</code></h3>
 * 
 * <p>
 * Thread safety and immutability are two points along the same axis. This set
 * of annotations can actually describe three points along the axis:
 * <dl>
 * <dt><code>&#064;Mutable</code> and <code>&#064;NotThreadSafe</code>
 * <dd>This is the same as being unannotated, or just <code>&#064;Mutable</code>
 * , or just <code>&#064;NotThreadSafe</code>. The type contains mutable state
 * that is not safe to access concurrently from multiple threads.
 * 
 * <dt><code>&#064;Mutable</code> and <code>&#064;ThreadSafe</code>
 * <dd>This is the same as <code>&#064;ThreadSafe</code>. The type contains
 * mutable state that is safe to access concurrently from multiple threads.
 * 
 * <dt><code>&#064;Immutable</code> and <code>&#064;ThreadSafe</code>
 * <dd>This is the same as <code>&#064;Immutable</code>. The type contains no
 * mutable state, and is thus safe to access concurrently from multiple threads.
 * </dl>
 * 
 * <p>
 * The combination <code>&#064;Immutable</code> and
 * <code>&#064;NotThreadSafe</code> applied to the same state is a modeling
 * error because an immutable type is obviously thread safe.
 * <p>
 * By default the static part of a class implementation is considered to be not
 * thread-safe as well as the instance part. It is possible to specify which
 * portions of the class implementation this annotation applies to using the
 * <tt>appliesTo</tt> attribute. This value can be one of
 * <ul>
 * <li><tt>&#064;NotThreadSafe(appliesTo=Part.InstanceAndStatic)</tt> (default)
 * &mdash; The instance part and the static part are intended to be thread-safe.
 * </li>
 * <li><tt>&#064;NotThreadSafe(appliesTo=Part.Instance)</tt> &mdash; The
 * instance part only is intended to be thread-safe.</li>
 * <li><tt>&#064;NotThreadSafe(appliesTo=Part.Static)</tt> &mdash; The static
 * part only is intended to be thread-safe.</li>
 * </ul>
 * 
 * <h3>Semantics:</h3>
 * 
 * Documenting that a type is not thread-safe does not constrain the
 * implementation of the program, it simply clarifies the programmer's intent.
 * 
 * <h3>Examples:</h3>
 * 
 * Most of the collection implementations provided in {@code java.util} are not
 * thread-safe. This could be documented for {@code java.util.ArrayList}, for
 * example, as shown below.
 * 
 * <pre>
 * package java.util;
 * 
 * &#064;NotThreadSafe
 * public class ArrayList extends ... {
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * As an example of treating the static and instance state of a class
 * differently, consider the <code>Point</code> class below. The instances of
 * the class are considered immutable, but the static state is mutable and not
 * thread-safe. This is because the static state is used to maintain a cache of
 * instantiated points.
 * 
 * <pre>
 * &#064;Immutable(appliesTo = Part.Instance)
 * &#064;NotThreadSafe(appliesTo = Part.Static)
 * public final class Point {
 *   private final static List&lt;Point&gt; points = new ArrayList&lt;Point&gt;();
 *   private final int x;
 *   private final int y;
 * 
 *   private Point(final int x, final int y) {
 *     this.x = x;
 *     this.y = y;
 *   }
 * 
 *   public boolean equals(final int x, final int y) {
 *     return this.x == x &amp;&amp; this.y == y;
 *   }
 * 
 *   public static Point getPoint(final int x, final int y) {
 *     for (final Point p : points) {
 *       if (p.equals(x, y))
 *         return p;
 *     }
 *     final Point p = new Point(x, y);
 *     points.add(p);
 *     return p;
 *   }
 * 
 *   // ...
 * }
 * </pre>
 * 
 * <i>Implementation note:</i> This annotation is derived from
 * <code>&#064;NotThreadSafe</code> proposed by Brian Goetz and Tim Peierls in
 * the book <i>Java Concurrency in Practice</i> (Addison-Wesley 2006) we have
 * simply adapted it to have semantics as a promise. Further, the annotation in
 * {@code net.jcip.annotations} may be used instead of this one with the same
 * tool behavior.
 * 
 * @see ThreadSafe
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NotThreadSafe {
  /**
   * Indicates whether the instance state of the class, static state of the
   * class, or both are subject to this annotation.
   * 
   * @return what part of a type declaration is subject to this annotation.
   */
  public Part appliesTo() default Part.InstanceAndStatic;
}
