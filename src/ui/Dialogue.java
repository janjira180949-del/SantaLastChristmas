package ui;

import entity.Child;
import java.util.List;

/** A finite conversation, optionally associated with an existing Child instance. */
public final class Dialogue {

    private final Child child;
    private final List<DialogueLine> lines;
    private int currentLineIndex;

    public Dialogue(Child child, List<DialogueLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Dialogue needs lines");
        }
        this.child = child;
        this.lines = List.copyOf(lines);
    }

    /** A non-child NPC conversation uses the same pages and UI. */
    public Dialogue(List<DialogueLine> lines) { this(null, lines); }

    /** Null for non-child conversations. */
    public Child getChild() {
        return child;
    }

    public synchronized DialogueLine getCurrentLine() {
        return isActive() ? lines.get(currentLineIndex) : null;
    }

    public synchronized int getCurrentLineNumber() {
        return currentLineIndex + 1;
    }

    public int getLineCount() {
        return lines.size();
    }

    public synchronized boolean isActive() {
        return currentLineIndex < lines.size();
    }

    /** Advances one line and reports whether the conversation remains active. */
    public synchronized boolean advance() {
        if (isActive()) {
            currentLineIndex++;
        }
        return isActive();
    }
}
