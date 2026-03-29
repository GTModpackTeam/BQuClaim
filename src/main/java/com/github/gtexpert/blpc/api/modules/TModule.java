package com.github.gtexpert.blpc.api.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a module. Discovered at FML Construction via ASM scanning.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TModule {

    /** Unique module identifier. */
    String moduleID();

    /** Owning container ID (typically the mod ID). */
    String containerID();

    String name();

    /** Required mod IDs. Module is skipped if any mod is not loaded. */
    String[] modDependencies() default {};

    /** If true, always loaded regardless of config. */
    boolean coreModule() default false;

    String description() default "";
}
