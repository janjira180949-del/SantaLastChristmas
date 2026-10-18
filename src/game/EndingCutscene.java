package game;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import entity.Child;
import entity.Santa;
import entity.Elias;
import spirit.SpiritTree;
import ui.*;

/** Optional authored ending after restoration. This presentation never awards Spirit or quests. */
public final class EndingCutscene {
    public enum State { NOT_STARTED, SPIRIT_RETURNS, WORLD_RECOVERY, FINAL_DIALOGUE, TITLE, ENDING_MESSAGE, CREDITS, END, HOME_FADE, CELEBRATION, CELEBRATION_DIALOGUE, SLEIGH_DEPARTURE }
    public static final int CAMERA_RETURN_DURATION=90;
    public static final int SLEIGH_DURATION=360, SLEIGH_BOARD_END=48, SLEIGH_BELLS_START=60,
        SLEIGH_MOVE=105, SLEIGH_TAKEOFF=165, SLEIGH_FLIGHT_END=330, SLEIGH_BELL_PERIOD=99;
    /** One authoritative motion curve, shared by the assembly and its camera. */
    public static double sleighFlight(int frame){return ramp(frame,SLEIGH_MOVE,SLEIGH_FLIGHT_END);}
    private State state=State.NOT_STARTED;
    private int frames,page,visualFrames;
    public int getVisualFrames(){return visualFrames;}
    /** Continue the same ambient presentation clock after gratitude, only on active Town updates. */
    public void updateCelebrationAmbient(){if(!hasStarted()&&visualFrames<Integer.MAX_VALUE)visualFrames++;}
    private boolean continueAdventure,homeEnding,cameraBound;
    private double cameraOriginX,cameraOriginY;
    private WorldMap.Point tree;
    private final Point2D.Double[] anchors=new Point2D.Double[3];
    private final Dialogue[] lines={
        line("Toby",Portrait.Kind.TOY,"It's glowing again..."),
        line("Elias",Portrait.Kind.ELIAS,"The guardian can finally rest. We will carry the light together."),
        line("Dark Titan",Portrait.Kind.EATER,"I can finally put it down."),
        line("Toby",Portrait.Kind.TOY,"Santa... is Christmas saved?"),
        line("Santa",Portrait.Kind.SANTA,"For tonight."),
        line("Elias",Portrait.Kind.ELIAS,"And tomorrow?"),
        line("Santa",Portrait.Kind.SANTA,"Tomorrow, we keep its light alive together.")
    };
    private static Dialogue line(String speaker,Portrait.Kind portrait,String text) {
        return new Dialogue(List.of(new DialogueLine(speaker,portrait,Portrait.Expression.THINKING,text)));
    }
    private final Dialogue[] celebrationLines={
        line("Elias",Portrait.Kind.ELIAS,"Our neighbors have come out of their homes. The Spirit Tree is shining again. Thank you, Santa. We will keep its light together."),
        line("Santa",Portrait.Kind.SANTA,"Then Christmas has a home here. Keep a place for one another, even on the coldest nights.")};
    public void startCelebration(WorldMap.Point tree){
        if(hasStarted())return;this.tree=tree;frames=0;visualFrames=0;page=0;homeEnding=false;continueAdventure=true;cameraBound=false;state=State.CELEBRATION;
    }
    public boolean isCelebration(){return state==State.CELEBRATION||state==State.CELEBRATION_DIALOGUE;}
    public void startSleighDeparture(){
        if(hasStarted())return;tree=JourneyHomeVisuals.SLEIGH_APPROACH;cameraBound=false;frames=0;visualFrames=240;page=0;homeEnding=false;state=State.SLEIGH_DEPARTURE;
    }
    public void startEpilogue(WorldMap.Point tree,Camera camera,int width,int height,boolean creditsOnly) {
        if(hasStarted())return;
        continueAdventure=false;homeEnding=false;
        this.tree=tree;page=0;frames=0;state=creditsOnly?State.CREDITS:State.SPIRIT_RETURNS;
    }
    /** The short Town gratitude exchange returns control; homecoming remains playable. */
    public void startChapterEpilogue(WorldMap.Point tree,Camera camera,int width,int height){
        if(hasStarted())return;startCelebration(tree);state=State.CELEBRATION_DIALOGUE;frames=0;visualFrames=240;
    }
    /** Final Letter closure is the only production entry to the final title/credits lifecycle. */
    public void startHomeEnding(boolean creditsOnly){
        if(hasStarted())return;homeEnding=true;continueAdventure=false;tree=null;page=0;frames=0;state=creditsOnly?State.CREDITS:State.HOME_FADE;
    }
    public boolean isHomeEnding(){return homeEnding;}
    public boolean hasStarted(){return state!=State.NOT_STARTED;}
    public boolean isComplete(){return state==State.END;}
    public boolean isWorldVisible(){return state==State.SPIRIT_RETURNS||state==State.WORLD_RECOVERY||state==State.FINAL_DIALOGUE||isCelebration();}
    public State getState(){return state;}
    public int getFrames(){return frames;}
    public static int duration(State state) {
        return switch(state){case SLEIGH_DEPARTURE->SLEIGH_DURATION;case CELEBRATION->240;case HOME_FADE->60;case SPIRIT_RETURNS->180;case WORLD_RECOVERY->240;case TITLE->360;case ENDING_MESSAGE->420;case CREDITS->2400;default->0;};
    }
    public double progress(){return duration(state)==0?0:frames/(double)duration(state);}
    public void update(SpiritTree spiritTree) {
        if(!hasStarted()||isComplete())return;
        if(visualFrames<Integer.MAX_VALUE)visualFrames++;
        if(state==State.CELEBRATION_DIALOGUE){frames=Math.min(180,frames+1);return;}
        if(state==State.FINAL_DIALOGUE){frames=Math.min(180,frames+1);return;}
        if(++frames>=duration(state))advancePresentation();
    }
    public void advancePresentation() {
        if(state==State.CELEBRATION){state=State.CELEBRATION_DIALOGUE;frames=0;page=0;cameraBound=false;return;}
        if(state==State.CELEBRATION_DIALOGUE){if(++page<celebrationLines.length)return;dismissPresentation();return;}
        if(state==State.SLEIGH_DEPARTURE){dismissPresentation();return;}
        if(state==State.HOME_FADE)state=State.TITLE;
        else if(state==State.SPIRIT_RETURNS)state=State.WORLD_RECOVERY;
        else if(state==State.WORLD_RECOVERY)state=State.FINAL_DIALOGUE;
        else if(state==State.FINAL_DIALOGUE){if(++page<lines.length)return;if(continueAdventure){dismissPresentation();return;}state=State.TITLE;}
        else if(state==State.TITLE)state=State.ENDING_MESSAGE;
        else if(state==State.ENDING_MESSAGE)state=State.CREDITS;
        else if(state==State.CREDITS)state=State.END;
        frames=0;cameraBound=false;
    }
    public void dismissPresentation(){state=State.NOT_STARTED;frames=0;page=0;}
    public Dialogue getCinematicDialogue(){return state==State.FINAL_DIALOGUE?lines[page]:state==State.CELEBRATION_DIALOGUE?celebrationLines[page]:null;}
    public void stageActors(Santa santa,List<Child> children,Elias elias,WorldMap map) {
        anchors[0]=new Point2D.Double(santa.getX()+16,santa.getY()+42);
        anchors[1]=safeAnchor(map,tree.x()-100,tree.y()+165);
        anchors[2]=safeAnchor(map,tree.x()+100,tree.y()+170);
    }
    private static Point2D.Double safeAnchor(WorldMap map,int x,int y) {
        WorldMap.Point free=map.safePosition(x-16,y-50,32,50);
        return new Point2D.Double(free.x()+16,free.y()+50);
    }
    public Point2D.Double actorAnchor(int actor){return anchors[actor];}
    public Santa.Direction santaDirection(){return page==1||page==5?Santa.Direction.RIGHT:Santa.Direction.UP;}
    public void frameCamera(Camera camera,int width,int height,WorldMap map) {frameCamera(camera,width,height,map,null);}
    public void frameCamera(Camera camera,int width,int height,WorldMap map,Santa santa) {
        if(tree==null)return;
        if(isCelebration()||state==State.SLEIGH_DEPARTURE){
            if(!cameraBound){cameraOriginX=camera.getX()+width/2.0;cameraOriginY=camera.getY()+height/2.0;cameraBound=true;}
            double x=tree.x(),y=tree.y()+75;
            int pan=(int)Math.max(90,Math.min(180,Math.hypot(x-cameraOriginX,y-cameraOriginY)/2));
            double t=ramp(frames,0,pan);
            if(state==State.CELEBRATION_DIALOGUE&&santa!=null){x=santa.getX()+16;y=santa.getY()+21;y-=TownChapterArt.treeCameraLift(x,y,tree);t=ramp(frames,0,60);
                // The whole gathering stays above the dialogue, including at the smaller viewport.
                for(var feet:TownCelebration.feet())y=Math.max(y,feet.y()-height/2.0+176);
                y=Math.max(y,JourneyHomeVisuals.ELIAS_FEET.y()-height/2.0+176);}
            if(state==State.SLEIGH_DEPARTURE){
                double launch=sleighFlight(frames);x=JourneyHomeVisuals.SLEIGH_ORIGIN.x()+126+350*launch;y=JourneyHomeVisuals.SLEIGH_ORIGIN.y()+76-100*launch-120*launch*launch*launch;
                t=ramp(frames,0,90); // Board first; follow the same accelerating assembly, then hold its exit.
            }
            camera.follow(cameraOriginX+(x-cameraOriginX)*t,cameraOriginY+(y-cameraOriginY)*t,width,height,map.width(),map.height());return;
        }
        if(!cameraBound){cameraOriginX=camera.getX()+width/2.0;cameraOriginY=camera.getY()+height/2.0;cameraBound=true;}
        double sweep=state==State.WORLD_RECOVERY?300*Math.pow(Math.sin(Math.PI*progress()),2):0;
        double t=ramp(frames,0,120);
        camera.follow(cameraOriginX+(tree.x()-sweep-cameraOriginX)*t,cameraOriginY+(tree.y()+40-cameraOriginY)*t,width,height,map.width(),map.height());
    }
    public double hopeProgress(){return state==State.SPIRIT_RETURNS?.45+.55*progress():1;}
    public void drawHope(Graphics2D original) {
        if(tree==null||!isWorldVisible())return;
        Graphics2D g=(Graphics2D)original.create();
        try {
            double spread=state==State.SPIRIT_RETURNS?progress():1;
            util.LightBloom.draw(g,tree.x(),tree.y()-60,100+160*spread,.18+.10*spread,util.LightBloom.GOLD);
            for(int i=0;i<24;i++) {
                double age=Math.floorMod(visualFrames+i*13,210)/210.0,a=i*Math.PI/12,r=35+spread*(60+age*75);
                int x=(int)(tree.x()+Math.cos(a)*r),y=(int)(tree.y()+70+Math.sin(a)*r*.35-age*145);
                g.setColor(new Color(255,222,153,(int)(75*Math.sin(age*Math.PI))));
                g.fillRect(x,y,2,3);
            }
        } finally {g.dispose();}
    }
    public void drawScreen(Graphics2D original,int width,int height,DialogueBox box) {
        if(!hasStarted())return;
        Graphics2D g=(Graphics2D)original.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        if(isWorldVisible())UITheme.letterbox(g,width,height,18);
        if(state==State.HOME_FADE){g.setComposite(AlphaComposite.SrcOver.derive((float)ramp(progress(),0,1)));g.setColor(Color.BLACK);g.fillRect(0,0,width,height);g.setComposite(AlphaComposite.SrcOver);}
        if(state==State.SLEIGH_DEPARTURE){g.setComposite(AlphaComposite.SrcOver.derive((float)ramp(frames,SLEIGH_FLIGHT_END,SLEIGH_DURATION)));g.setColor(Color.BLACK);g.fillRect(0,0,width,height);g.setComposite(AlphaComposite.SrcOver);}
        if(state==State.SLEIGH_DEPARTURE){centered(g,frames>SLEIGH_FLIGHT_END?"CHRISTMAS TOWN AT PEACE":frames<SLEIGH_MOVE?"UNTIL NEXT CHRISTMAS":"SANTA RETURNS HOME",width/2,70,22,UITheme.GOLD);}
        if(state==State.SLEIGH_DEPARTURE&&frames<240){String caption=frames<90?"I only came to deliver presents...":"How did I end up saving all of Christmas?!";for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)centered(g,caption,width/2+dx,106+dy,18,Color.BLACK);centered(g,caption,width/2,106,18,UITheme.GOLD);}
        if(state==State.CELEBRATION){int top=height-120-30;ui.ChristmasUI.frame(g,"parchment",new Rectangle((width-Math.min(620,width-40))/2,top,Math.min(620,width-40),120));centered(g,"CHRISTMAS SAVED",width/2,top+38,22,ui.ChristmasUI.RED);centered(g,"The Spirit Tree shines again.",width/2,top+62,14,ui.ChristmasUI.INK);centered(g,"Friends from The Last Village",width/2,top+80,14,ui.ChristmasUI.INK);centered(g,"gather beneath its light.",width/2,top+98,14,ui.ChristmasUI.INK);}
        if(state==State.SPIRIT_RETURNS||state==State.WORLD_RECOVERY) {
            int boxW=Math.min(700,width-50),left=(width-boxW)/2,top=(height-144)/2;
            ui.ChristmasUI.frame(g,"parchment",new Rectangle(left,top,boxW,144));
            centered(g,state==State.SPIRIT_RETURNS?"Christmas Spirit returns.":"The world begins to recover.",width/2,top+52,Math.min(23,width/35),ui.ChristmasUI.RED);
            centered(g,state==State.SPIRIT_RETURNS?"The Spirit Tree carries Christmas light again.":"Black Frost recedes. Warmth returns to Christmas Town.",width/2,top+80,Math.min(15,width/50),ui.ChristmasUI.INK);
            if(state==State.WORLD_RECOVERY)centered(g,"Santa's promise has given Christmas a future.",width/2,top+104,Math.min(15,width/50),ui.ChristmasUI.INK);
            float fade=(float)Math.max(1-ramp(progress(),0,.13),ramp(progress(),.88,1));g.setComposite(AlphaComposite.SrcOver.derive(fade));
            g.setColor(Color.BLACK);g.fillRect(0,0,width,height);g.setComposite(AlphaComposite.SrcOver);
        }
        Dialogue dialogue=getCinematicDialogue();if(dialogue!=null)box.drawCinematic(g,dialogue,width,height);
        if(state==State.TITLE||state==State.ENDING_MESSAGE||state==State.CREDITS||state==State.END) {
            g.setColor(Color.BLACK);g.fillRect(0,0,width,height);
            if(state==State.TITLE || state==State.ENDING_MESSAGE) {
                float alpha=(float)(ramp(progress(),0,.18)*(1-ramp(progress(),.78,1)));
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                if(state==State.TITLE) {
                    centered(g,homeEnding?"HOME FOR CHRISTMAS":"SANTA: LAST CHRISTMAS",width/2,height/2,Math.min(32,width/24),new Color(255,225,156));
                    g.setColor(new Color(158,129,77));g.drawLine(width/2-110,height/2+25,width/2+110,height/2+25);
                } else centered(g,homeEnding?"Christmas was never truly lost.":"Christmas survives as long as hope remains.",width/2,height/2,Math.min(22,width/35),new Color(239,231,213));
            } else if(state==State.CREDITS) {
                float alpha=(float)(ramp(progress(),0,.04)*(1-ramp(progress(),.96,1)));
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                double scroll=Math.min(1,progress()/.92);
                int y=(int)(height+70-scroll*(height/2+320+EndingCredits.scrollHeight()-EndingCredits.entryHeight(EndingCredits.ENTRIES.get(EndingCredits.ENTRIES.size()-1))));
                centered(g,"SANTA: LAST CHRISTMAS",width/2,y,26,new Color(255,225,156));
                y+=120;
                for(EndingCredits.Entry entry:EndingCredits.ENTRIES) {
                    centered(g,entry.role(),width/2,y,16,new Color(202,179,135));
                    int lineY=y+30;for(String credit:entry.credit().split("\\n")){centered(g,credit,width/2,lineY,20,new Color(235,235,229));lineY+=26;}y+=EndingCredits.entryHeight(entry);
                }
                
            }
        }
        if(state==State.FINAL_DIALOGUE&&page==0&&frames<24){g.setComposite(AlphaComposite.SrcOver.derive(1-frames/24f));g.setColor(Color.BLACK);g.fillRect(0,0,width,height);}
        g.setComposite(AlphaComposite.SrcOver);g.setFont(new Font("Dialog",Font.PLAIN,12));g.setColor(new Color(255,230,176));
        g.drawString(state==State.SLEIGH_DEPARTURE?"[E / Esc] Continue home":homeEnding?"[E] Continue   [Esc] Return to Workshop":"[E] Continue   [Esc] Return to Town",20,height-8);g.dispose();
    }
    private static void centered(Graphics2D g,String text,int x,int y,int size,Color color) {
        g.setFont(new Font("Serif",Font.PLAIN,Math.max(12,size)));g.setColor(color);
        g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);
    }
    private static double ramp(double p,double start,double end){double t=Math.max(0,Math.min(1,(p-start)/(end-start)));return t*t*(3-2*t);}
}
