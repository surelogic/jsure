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
 * The class to which this annotation is applied is mutable, that is, has state
 * that is changeable. This annotation primarily exists for clarifying the
 * mutability of a class that might otherwise be assumed to be immutable,
 * despite the fact that it is a bad idea to assume a class is immutable without
 * good reason.
 * <p>
 * This annotation is <em>not verified</em>, it is intended for documentation of
 * programmer intent only.
 * <p>
 * A type may not be annotated with both <code>&#064;Immutable</code> and
 * <code>&#064;Mutable</code> except in the case that one refers to the static
 * portion of the declaration and the other refers to the instance portion of
 * the declaration.
 * <p>
 * An annotation type declaration may be annotated with
 * <code>&#064;Mutable</code> so that any static fields in the type are allowed
 * to be mutable. It is, however, a modeling error to restrict the annotation to
 * the instance part.
 * <p>
 * By default the static part of a class implementation is assumed to be mutable
 * as well as the instance part. It is possible to specify which portions of the
 * class implementation this annotation refers to using the <tt>appliesTo</tt>
 * attribute. This value can be one of
 * <ul>
 * <li><tt>&#064;Mutable(appliesTo=Part.InstanceAndStatic)</tt> (default)
 * &mdash; The instance part and the static part are intended to be mutable.</li>
 * <li><tt>&#064;Mutable(appliesTo=Part.Instance)</tt> &mdash; The instance part
 * only is intended to be mutable.</li>
 * <li><tt>&#064;Mutable(appliesTo=Part.Static)</tt> &mdash; The static part
 * only is intended to be mutable.</li>
 * </ul>
 * 
 * <h3>Relationship with <code>&#064;ThreadSafe</code></h3>
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
 * 
 * <h3>Semantics:</h3>
 * 
 * Instances of the type to which this annotation is applied have state that can
 * be seen to change by callers.
 * 
 * <h3>Examples:</h3>
 * 
 * The <tt>Aircraft</tt> class below is declared to be mutable because its
 * position can be changed. Its implementation is also thread-safe, however, not
 * all mutable classes are also thread-safe.
 * 
 * <pre>
 * &#064;Mutable
 * public class Aircraft {
 *   private final Lock stateLock = new ReentrantLock();
 *   ...
 *   &#064;GuardedBy(&quot;stateLock&quot;)
 *   private long x, y;
 *   ...
 *   public void setPosition(long x, long y) {
 *     stateLock.lock();
 *     try {
 *       this.x = x;
 *       this.y = y;
 *     } finally {
 *       stateLock.unlock();
 *     }
 *   }
 *   ...
 * }
 * </pre>
 * 
 * @see Immutable
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mutable {
  /**
   * Indicates whether the instance state of the class, static state of the
   * class, or both are subject to this annotation.
   * 
   * @return what part of a type declaration is subject to this annotation.
   */
  public Part appliesTo() default Part.InstanceAndStatic;
}
