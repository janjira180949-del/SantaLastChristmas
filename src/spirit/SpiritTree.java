package spirit;

/** Story progression only: never gates movement, abilities or exits. */
public class SpiritTree {
    private int spirit;
    private final int maximumSpirit;
    private boolean restorationStarted;
    public SpiritTree() { this(100); }
    public SpiritTree(int maximumSpirit) {
        if(maximumSpirit<=0)throw new IllegalArgumentException("Maximum Spirit must be positive");
        this.maximumSpirit=maximumSpirit;
    }
    public void addSpirit(int amount) { spirit=(int)Math.max(0,Math.min(maximumSpirit,(long)spirit+amount)); }
    public void beginRestoration() { restorationStarted=true; }
    public boolean isRestorationStarted() { return restorationStarted; }
    public int getSpirit() { return spirit; }
    public int getMaximumSpirit() { return maximumSpirit; }
}
