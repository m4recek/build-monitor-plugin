package com.groupsolver.jenkins.plugins.buildboard;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BuildBoardLogger {
    private final String className;

    public static <T> BuildBoardLogger forClass(Class<T> sourceClass) {
        return new BuildBoardLogger(sourceClass.getName());
    }

    public void debug(String sourceMethod, String template, Object... arguments) {
        log(Level.FINEST, sourceMethod, template, arguments);
    }

    public void info(String sourceMethod, String template, Object... arguments) {
        log(Level.INFO, sourceMethod, template, arguments);
    }

    public void warning(String sourceMethod, String template, Object... arguments) {
        log(Level.WARNING, sourceMethod, template, arguments);
    }

    public void error(String sourceMethod, String template, Object... arguments) {
        log(Level.SEVERE, sourceMethod, template, arguments);
    }

    // --

    private final Logger logger;

    private BuildBoardLogger(String className) {
        this.className = className;
        this.logger = Logger.getLogger("GroupSolver Build Board");
    }

    private void log(Level level, String sourceMethod, String template, Object... arguments) {
        logger.logp(level, className, sourceMethod, template, arguments);
    }
}
