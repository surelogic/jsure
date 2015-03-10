/**
 * Contains abstract classes of IAnnotationParseRule that are specialized
 * for the following common cases.
 * <dl>
 * <dt> DefaultBooleanAnnotationParseRule 
 * <dd>   Superclass for rules parsing annotations like @Unique and @SingleThreaded
 * <dt> DefaultSLAnnotationParseRule
 * <dd>   Superclass for rules parsed using Antlr (specifically in SLAnnotations.g)
 * </dl> 
 * 
 * @see com.surelogic.annotation.parse.SLAnnotations.g
 */
package com.surelogic.annotation;