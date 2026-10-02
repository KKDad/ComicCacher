package org.stapledon.engine.batch.scheduler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets where a {@link DailyJobScheduler} bean's startup makeup run goes relative to the others. {@link StartupJobRunner} runs the makeup
 * checks lightest first; a scheduler without this annotation weighs {@link #DEFAULT}, and equal weights keep their bean registration order.
 * Put it on the scheduler's {@code @Bean} method.
 * <p>
 * Use it when one job's makeup run saves work for another's, e.g. PromoteFromDevJob copies strips that ComicDownloadJob would otherwise fetch.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CatchUpWeight {

    /** The weight of a scheduler without the annotation. */
    int DEFAULT = 0;

    /** Lighter (lower) weights run first. */
    int value();
}
