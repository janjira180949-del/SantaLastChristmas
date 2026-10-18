package quest;

/** One lightweight sequential story objective. */
public final class StoryQuest {

    public enum State {
        LOCKED,
        AVAILABLE,
        ACTIVE,
        COMPLETED
    }

    public enum Id {
        LOST_GIFT,
        BREAK_THE_FROST,
        SAVE_CHRISTMAS,
        RESTORE_THE_VILLAGE,
        CHRISTMAS_DELIVERY
    }

    private final Id id;
    private final String name;
    private final String objective;
    private final int requiredProgress;
    private int progress;
    private State state;
    private final java.util.function.IntSupplier observedProgress;

    StoryQuest(Id id, String name, String objective, int requiredProgress,
            State state) {
        this(id,name,objective,requiredProgress,state,null);
    }

    StoryQuest(Id id,String name,String objective,int requiredProgress,State state,
            java.util.function.IntSupplier observedProgress) {
        if (id == null || name == null || name.isBlank()
                || objective == null || objective.isBlank()
                || requiredProgress <= 0 || state == null) {
            throw new IllegalArgumentException("Story quest is incomplete");
        }
        this.id = id;
        this.name = name;
        this.objective = objective;
        this.requiredProgress = requiredProgress;
        this.state = state;
        this.observedProgress=observedProgress;
    }

    public Id getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getObjective() {
        return objective;
    }

    public State getState() {
        return state;
    }

    public int getProgress() {
        return observedProgress==null?progress:state==State.LOCKED?0:
                Math.max(0,Math.min(requiredProgress,observedProgress.getAsInt()));
    }

    public int getRequiredProgress() {
        return requiredProgress;
    }

    void setState(State state) {
        this.state = state;
        if (state == State.COMPLETED) {
            progress = requiredProgress;
        }
    }

    void addProgress(int amount) {
        if (state == State.ACTIVE && amount > 0) {
            progress = Math.min(requiredProgress, progress + amount);
        }
    }
}
