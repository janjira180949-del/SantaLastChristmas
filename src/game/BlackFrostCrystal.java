package game;

import entity.Santa;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/** A guarded objective: only a completed wave exposes its breakable core. */
public final class BlackFrostCrystal {
    public enum State { DORMANT, GUARDED, VULNERABLE, BROKEN }
    private final Encounter encounter;
    private State state=State.DORMANT;
    private int effectFrames;
    private int exposureAttackSequence;
    private int visualTicks;
    private int impactFrames,lastVisualAttack=-1;
    public int getImpactFrames(){return impactFrames;}
    public int getVisualTicks(){return visualTicks;}
    private static final java.awt.image.BufferedImage[] CORE_ART=coreArtwork();
    private static java.awt.image.BufferedImage[] coreArtwork() {
        var images=new java.awt.image.BufferedImage[3];
        for(int i=0;i<3;i++) {
            var image=new java.awt.image.BufferedImage(40,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
            Color edge=i==2?new Color(252,221,152):new Color(139,217,229),face=i==2?new Color(168,131,94):new Color(41,77,111);
            g.setColor(new Color(12,29,51));g.fillPolygon(new int[]{20,38,30,10,2},new int[]{1,25,62,62,25},5);
            g.setColor(face);g.fillPolygon(new int[]{20,35,28,20},new int[]{4,26,59,48},4);
            g.setColor(i==2?new Color(220,179,118):new Color(78,144,169));g.fillPolygon(new int[]{20,5,12,20},new int[]{4,26,59,48},4);
            g.setColor(i==2?new Color(248,218,154):new Color(102,189,207));g.fillPolygon(new int[]{20,35,20,5},new int[]{4,26,35,26},4);
            g.setColor(edge);g.drawLine(20,4,35,26);g.drawLine(5,26,12,59);g.drawLine(12,59,28,59);
            g.setColor(new Color(13,28,43));g.fillPolygon(new int[]{20,27,20,13},new int[]{29,41,54,41},4);
            g.setColor(i==2?new Color(255,242,192):new Color(184,238,243));g.fillRect(18,36,4,12);g.fillRect(16,40,8,3);
            g.dispose();images[i]=image;
        }
        return images;
    }
    public BlackFrostCrystal(Encounter encounter) { this.encounter=encounter; }
    public String getId() { return encounter.getId(); }
    public State getState() { return state; }
    public WorldMap.Point getPosition() { return encounter.getCenter(); }
    public Rectangle2D getBounds() { var p=getPosition(); return new Rectangle2D.Double(p.x()-18,p.y()-30,36,60); }
    public void update(Santa santa) {
        visualTicks++;
        if(impactFrames>0)impactFrames--;
        if(effectFrames>0)effectFrames--;
        if(state==State.BROKEN)return;
        // A contact response is visual only: no HP, reward or change to the fresh-swing gate.
        if(santa.isAttacking()&&santa.getAttackSequence()!=lastVisualAttack&&santa.getAttackBounds().intersects(getBounds())){
            lastVisualAttack=santa.getAttackSequence();impactFrames=8;
        }
        State previous=state;
        state=switch(encounter.getState()) {case READY->State.DORMANT;case ACTIVE->State.GUARDED;case COMPLETE->State.VULNERABLE;};
        if(state==State.VULNERABLE && previous!=state) exposureAttackSequence=santa.getAttackSequence();
    }
    public boolean tryBreak(Santa santa) {
        if(state!=State.VULNERABLE || santa.getAttackSequence()<=exposureAttackSequence || !santa.isAttacking() || !santa.getAttackBounds().intersects(getBounds()))return false;
        state=State.BROKEN;effectFrames=36;return true;
    }
    /** Compatibility entry point; live Forest paints the body below actor warnings. */
    public void draw(Graphics2D original) { drawGround(original);drawForeground(original); }
    public void drawGround(Graphics2D original) {
        Graphics2D g=(Graphics2D)original.create();var p=getPosition();
        try {
            if(ForestLandmarkArt.drawCore(g,this,visualTicks,effectFrames))return;
            // Missing/pending artwork retains a playable cached core; no decoding in paint.
            if(state!=State.BROKEN)g.drawImage(CORE_ART[state==State.VULNERABLE?2:0],p.x()-20,p.y()-32,null);
            else {g.setColor(new Color(105,126,143,90));g.fillOval(p.x()-22,p.y()+22,44,12);}
        } finally {g.dispose();}
    }
    public void drawForeground(Graphics2D original) {
        if(state==State.BROKEN)return;Graphics2D g=(Graphics2D)original.create();var p=getPosition();
        try {
            g.setFont(new Font("Dialog",Font.BOLD,12));g.setColor(state==State.VULNERABLE?new Color(255,217,120):new Color(154,211,229));
            String label=state==State.VULNERABLE?"CORE EXPOSED - ATTACK":state==State.GUARDED?"DEFEAT THE GUARDIANS":"BLACK FROST CRYSTAL";
            g.drawString(label,p.x()-g.getFontMetrics().stringWidth(label)/2,ForestLandmarkArt.labelY(getId(),p.y()));
        } finally {g.dispose();}
    }
}
