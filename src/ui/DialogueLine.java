package ui;

/** Immutable speaker, portrait and text data for one dialogue line. */
public final class DialogueLine {

    private final String speakerName;
    private final Portrait.Kind portraitKind;
    private final String text;
    private final Portrait.Expression expression;

    public DialogueLine(String speakerName, Portrait.Kind portraitKind, String text) {
        this(speakerName, portraitKind, Portrait.Expression.NEUTRAL, text);
    }

    public DialogueLine(String speakerName, Portrait.Kind portraitKind,
            Portrait.Expression expression, String text) {
        if (speakerName == null || speakerName.isBlank() || portraitKind == null
                || expression == null || text == null || text.isBlank()) {
            throw new IllegalArgumentException("Dialogue line is incomplete");
        }
        this.speakerName = speakerName;
        this.portraitKind = portraitKind;
        this.text = text;
        this.expression = expression;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public Portrait.Kind getPortraitKind() {
        return portraitKind;
    }

    public Portrait.Expression getExpression() { return expression; }

    public String getText() {
        return text;
    }
}
