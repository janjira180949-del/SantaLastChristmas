package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/** A persistent stationary Village child with separate story and visual states. */
public final class Child {

    public enum Kind {
        TOY("Toby");

        private final String displayName;

        Kind(String displayName) { this.displayName = displayName; }

        public String getDisplayName() { return displayName; }
    }

    public enum StoryState {
        NOT_MET,
        MET,
        HELPED,
        REUNITED
    }

    public enum VisualState { IDLE, TALK, CELEBRATE }

    private static final ChildSprites SPRITES = new ChildSprites();
    private boolean talking;
    private boolean celebrating;
    private int visualTicks;
    public void updateVisual() {visualTicks=(visualTicks+1)%36000;}
    private int breath() {return visualTicks%120<60?0:-1;}

    public VisualState getVisualState() {
        return talking ? VisualState.TALK
                : celebrating ? VisualState.CELEBRATE : VisualState.IDLE;
    }

    public void setTalking(boolean talking) { this.talking = talking; }

    /** Presentation hook for future ending scenes; does not advance story state. */
    public void setCelebrating(boolean celebrating) { this.celebrating = celebrating; }

    private static final int WIDTH = 30;
    private static final int HEIGHT = 42;
    private static final double INTERACTION_RANGE = 58.0;

    private final String name;
    private final Kind kind;
    private int x;
    private int y;
    private final Color clothingColor;
    private final Rectangle2D bounds;
    private final Ellipse2D interactionArea;
    private StoryState storyState = StoryState.NOT_MET;
    private boolean canInteract;

    public Child(String name, Kind kind, int x, int y, Color clothingColor) {
        if (name == null || name.isBlank() || kind == null
                || clothingColor == null) {
            throw new IllegalArgumentException("Child configuration is invalid");
        }
        this.name = name;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.clothingColor = clothingColor;
        bounds = new Rectangle2D.Double(x, y, WIDTH, HEIGHT);
        interactionArea = new Ellipse2D.Double();
        updateGeometry();
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /** Repositions this same NPC object for future story or ending scenes. */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        canInteract = false;
        updateGeometry();
    }

    public Rectangle2D getBounds() {
        return (Rectangle2D) bounds.clone();
    }

    public Ellipse2D getInteractionArea() {
        return (Ellipse2D) interactionArea.clone();
    }

    public boolean canInteract() {
        return canInteract;
    }

    /** Updates proximity state using Santa's existing world position. */
    public void updateInteraction(Santa santa) {
        if (santa == null) {
            throw new IllegalArgumentException("Santa cannot be null");
        }
        canInteract=isWithinInteractionRange(santa);
    }
    /** The same proximity/solid check protects both prompts and story commits. */
    public boolean isWithinInteractionRange(Santa santa){
        if(santa==null||santa.isDefeated())return false;
        var solids=game.WorldMap.forArea(game.Area.Type.VILLAGE).solids();
        if(util.Collision.intersectsAny(santa.getBounds(),solids))return false;
        double sx=santa.getX()+16.0,sy=santa.getY()+21.0,cx=x+15.0,cy=y+21.0;
        if(Math.hypot(sx-cx,sy-cy)>=INTERACTION_RANGE)return false;
        for(var solid:solids)if(solid.intersectsLine(sx,sy,cx,cy))return false;
        return true;
    }

    /** Clears the nearby state when the Village is not the active area. */
    public void clearInteractionState() {
        canInteract = false;
    }

    public StoryState getStoryState() {
        return storyState;
    }

    /** Advances the future story state by one step; no dialogue is started here. */
    public void advanceStoryState() {
        if (storyState.ordinal() < StoryState.values().length - 1) {
            storyState = StoryState.values()[storyState.ordinal() + 1];
        }
    }

    private void updateGeometry() {
        bounds.setRect(x, y, WIDTH, HEIGHT);
        interactionArea.setFrame(
                x + WIDTH / 2.0 - INTERACTION_RANGE,
                y + HEIGHT / 2.0 - INTERACTION_RANGE,
                INTERACTION_RANGE * 2.0, INTERACTION_RANGE * 2.0);
    }

    /** Quiet cinematic pose; suppress names, interaction outlines and celebration. */
    public void drawCinematic(Graphics2D graphics, double feetX, double feetY, boolean speaking) {
        if (!SPRITES.draw(graphics, kind, speaking ? VisualState.TALK : VisualState.IDLE, feetX, feetY+breath())) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.translate(feetX-(x+15),feetY-(y+42)); drawFallback(g); g.dispose();
        }
    }

    public void draw(Graphics2D graphics) {
        graphics.setColor(new Color(20, 25, 32, 75));
        graphics.fillOval(x - 2, y + HEIGHT - 4, WIDTH + 4, 9);

        if (!SPRITES.draw(graphics, kind, getVisualState(), x + WIDTH / 2.0, y + HEIGHT+breath())) {
            drawFallback(graphics);
        }

        graphics.setColor(new Color(10,18,31,210));graphics.fillRoundRect(x-12,y-27,53,19,4,4);
        graphics.setColor(new Color(247,231,194));
        graphics.drawString(name, x - 7, y - 14);
        if (canInteract) {
            graphics.setColor(new Color(255, 227, 139));
            graphics.drawString("[E] Talk", x - 6, y + HEIGHT + 13);
        }
    }

    private void drawFallback(Graphics2D graphics) {
        graphics.setColor(clothingColor);
        graphics.fillRoundRect(x + 3, y + 17, WIDTH - 6, HEIGHT - 17,
                10, 10);
        graphics.setColor(new Color(243, 205, 165));
        graphics.fillOval(x + 7, y, WIDTH - 14, 22);
        graphics.setColor(new Color(54, 43, 42));
        graphics.fillOval(x + 10, y + 8, 3, 3);
        graphics.fillOval(x + 18, y + 8, 3, 3);
        graphics.setColor(new Color(112, 56, 53));
        graphics.drawArc(x + 12, y + 11, 7, 5, 200, 140);

        drawAppearance(graphics);

    }

    private void drawAppearance(Graphics2D graphics) {
        switch (kind) {
            case TOY:
                graphics.setColor(new Color(245, 218, 108));
                graphics.fillOval(x + 21, y + 23, 12, 12);
                graphics.setColor(new Color(185, 54, 57));
                graphics.fillRect(x + 25, y + 25, 4, 8);
                graphics.fillRect(x + 23, y + 27, 8, 4);
                break;
            default:
                throw new IllegalStateException("Unknown child appearance");
        }
    }
}
