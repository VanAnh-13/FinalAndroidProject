package com.example.healthylifehub.patterns.command;

/**
 * Command Pattern - Command Interface
 * Encapsulates a request as an object
 */
public interface Command {
    void execute();
    void undo();
    String getDescription();
}
