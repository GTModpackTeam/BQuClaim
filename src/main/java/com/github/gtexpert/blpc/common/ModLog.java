package com.github.gtexpert.blpc.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Centralized loggers for the BLPC mod.
 * Each category maps to a hierarchical logger under the "blpc" root.
 */
public final class ModLog {

    public static final Logger ROOT = LogManager.getLogger("blpc");
    public static final Logger IO = LogManager.getLogger("blpc/IO");
    public static final Logger PARTY = LogManager.getLogger("blpc/Party");
    public static final Logger SYNC = LogManager.getLogger("blpc/Sync");
    public static final Logger BQU = LogManager.getLogger("blpc/BQu");
    public static final Logger MIGRATION = LogManager.getLogger("blpc/Migration");
    public static final Logger UI = LogManager.getLogger("blpc/UI");
    public static final Logger PROTECTION = LogManager.getLogger("blpc/Protection");
    public static final Logger MODULE = LogManager.getLogger("blpc/Module");

    private ModLog() {}
}
