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
 * Container annotation for multiple {@link Assume} annotations. It is a
 * modeling error for an entity to have both an {@code Assumes} and an
 * {@code Assume} annotation.
 * 
 * <h3>Semantics:</h3>
 * 
 * This annotation holds a list of {@link Assume} annotations without imposing
 * any further constraint on the program's implementation.
 * 
 * <h3>Examples:</h3>
 * 
 * Below we declare two {@link Assume} annotations for the same declaration. The
 * first places a {@link Starts} annotation on the no-argument constructor for
 * the {@code IllegalArgumentExcpetion} in {@code java.lang}. The second places
 * a {@link Starts} annotation on the constructor that takes a single argument
 * of type {@code long} for the {@code WaiterPreferenceSemaphore} class in any
 * package.
 * 
 * <pre>
 * package EDU.oswego.cs.dl.util.concurrent;
 * 
 * public class Rendezvous implements Barrier {
 * 
 *   &#064;Starts("nothing")
 *   &#064;Assumes( {
 *     &#064;Assume("Starts(nothing) for new() in IllegalArgumentException in java.lang"),
 *     &#064;Assume("Starts(nothing) for new(long) in WaiterPreferenceSemaphore")
 *   })
 *   public Rendezvous(int parties, RendezvousFunction function) {
 *     if (parties &lt;= 0)
 *       throw new IllegalArgumentException();
 *     parties_ = parties;
 *     rendezvousFunction_ = function;
 *     entryGate_ = new WaiterPreferenceSemaphore(parties);
 *     slots_ = new Object[parties];
 *   }
 *   ...
 * }
 * </pre>
 * 
 * @see Assume
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD })
public @interface Assumes {
  /**
   * The {@link Assume} annotations to apply to the class.
   * 
   * @return the {@link Assume} annotations to apply to the class.
   */
  Assume[] value();
}
