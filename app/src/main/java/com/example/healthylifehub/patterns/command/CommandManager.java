package com.example.healthylifehub.patterns.command;

import java.util.Stack;

/**
 * Command Pattern - Invoker
 * Manages command execution and undo/redo operations
 */
public class CommandManager {
    private static CommandManager instance;
    private final Stack<Command> undoStack;
    private final Stack<Command> redoStack;
    private static final int MAX_HISTORY = 20;

    private CommandManager() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
    }

    public static synchronized CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Clear redo stack after new command
        
        // Limit history size
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.remove(0);
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (canUndo()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    public void redo() {
        if (canRedo()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }

    public String getLastCommandDescription() {
        if (!undoStack.isEmpty()) {
            return undoStack.peek().getDescription();
        }
        return null;
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    public int getUndoStackSize() {
        return undoStack.size();
    }

    public int getRedoStackSize() {
        return redoStack.size();
    }
}
