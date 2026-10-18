package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Modal story cards used for the prologue and major lore reveals. */
public final class StoryOverlay {
    private List<String> pages = List.of();
    private int pageIndex;
    private int frames;
    private boolean bossIntro;
    private String bossTitle="DARK TITAN";
    private WorldMap.Point bossAnchor;
    private entity.Enemy cinematicBoss;
    private boolean cameraBound;
    private int pageStart;
    private double originX,originY,viewX,viewY,returnX,returnY;
    public static int revealDuration(boolean fang){return fang?150:180;}
    public static final int RETURN_DURATION=120;
    private static double ease(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    public void bindBoss(entity.Enemy boss){cinematicBoss=boss;}
    /** Existing entrance frames, at the actual actor anchor; no combat clock is advanced by paint. */
    public void drawBossEntrance(Graphics2D g,entity.Enemy enemy,entity.Santa santa){
        boolean fang=enemy instanceof boss.FrostFang;
        int count=fang?8:4;
        double progress=pageIndex==0?ease(frames/(fang?90.0:120.0)):1-ease((frames-pageStart)/45.0);
        int frame=(int)Math.round((count-1)*progress);
        double x=enemy.getX()+enemy.getSize()/2.0,foot=enemy.getY()+enemy.getSize();
        if(fang)boss.BossArtwork.drawFang(g,"entrance",frame,x,foot,santa.getX()+16>x);
        else {
            WorldMap.Point root=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
            double settle=pageIndex==0?ease((frames-65)/55.0):1;
            double revealX=root.x()+(x-root.x())*settle,revealFoot=root.y()+64+(foot-root.y()-64)*settle;
            Graphics2D reveal=(Graphics2D)g.create();reveal.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)ease(Math.max(0,(frames-45)/55.0))));
            boss.BossArtwork.draw(reveal,"titan","entrance",frame,revealX,revealFoot);reveal.dispose();
        }
        if(!fang)boss.BossArtwork.effect(g,"titan_blast",8+frames/8%4,x,foot-116,30,30,0);
    }
    /** Presentation follows the existing phase-transition timer without adding recovery time. */
    public static void drawBossTransition(Graphics2D g,entity.Enemy enemy){
        if(!(enemy instanceof boss.BossCombatant actor)||actor.getTransitionFrames()==0)return;
        boolean fang=enemy instanceof boss.FrostFang;int duration=fang?60:72;
        int age=duration-actor.getTransitionFrames();
        double pulse=Math.sin(Math.PI*age/duration),x=enemy.getX()+enemy.getSize()/2.0,y=enemy.getY()+enemy.getSize();
        Graphics2D fx=(Graphics2D)g.create();
        try{
            fx.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)(.65*pulse)));
            boss.BossArtwork.effect(fx,fang?"wave":"titan_slam",age/5,x,y-5,fang?210:280,fang?100:130,0);
            if(!fang)boss.BossArtwork.effect(fx,"titan_blast",8+age/6%4,x,y-116,40,40,0);
            for(int i=0;i<6;i++){double a=i*Math.PI/3+age*.012;
                boss.BossArtwork.effect(fx,fang?"hurt":"titan_hurt",age/4+i,x+Math.cos(a)*(35+age*.6),y-50+Math.sin(a)*28,16,20,a);}
        }finally{fx.dispose();}
    }
    public int getFrames() {return frames;}
    public boolean isBossIntro() {return bossIntro;}
    public void update() {if(isActive()&&frames<36000)frames++;}

    public boolean isActive() { return pageIndex < pages.size(); }

    public void start(String... textPages) {
        pages = List.of(textPages);
        pageIndex = 0;
        frames=0;bossIntro=false;bossAnchor=null;bossTitle="DARK TITAN";cinematicBoss=null;cameraBound=false;pageStart=0;
    }
    public void startBoss(String... textPages) {start(textPages);bossIntro=true;}
    public void startBossAt(String title,WorldMap.Point anchor,String... textPages){startBoss(textPages);bossTitle=title;bossAnchor=anchor;}
    public void frameCamera(Camera camera,entity.Santa santa,WorldMap map,int width,int height) {
        double t=ease(frames/150.0),x=santa.getX()+16,y=santa.getY()+21;
        if(cinematicBoss instanceof boss.DarkTitan)y-=125;
        if(bossIntro){
            if(!cameraBound){originX=camera.getX()+width/2.0;originY=camera.getY()+height/2.0;viewX=originX;viewY=originY;cameraBound=true;}
            WorldMap.Point anchor=bossAnchor==null?map.point("landmarks/Corrupted Town Center"):bossAnchor;
            double focusX=cinematicBoss==null?anchor.x():cinematicBoss.getX()+cinematicBoss.getSize()/2.0;
            double focusY=cinematicBoss==null?anchor.y():cinematicBoss.getY()+cinematicBoss.getSize()-(cinematicBoss instanceof boss.FrostFang?72:65);
            if(pageIndex==0){double reveal=ease(frames/(double)revealDuration(bossTitle.equals("FROST FANG")));
                viewX=originX+(focusX-originX)*reveal;viewY=originY+(focusY-originY)*reveal;
            }else{double back=ease((frames-pageStart)/(double)RETURN_DURATION);viewX=returnX+(x-returnX)*back;viewY=returnY+(y-returnY)*back;}
            x=viewX;y=viewY;
        }else{x+=Math.sin(frames/240.0)*35;y-=t*68;}
        camera.follow(x,y,width,height,map.width(),map.height());
    }

    public void advance() {
        if(isActive()){pageIndex++;pageStart=frames;returnX=viewX;returnY=viewY;}
    }
    public void drawAtmosphere(Graphics2D original,entity.Santa santa,WorldMap map,Camera camera,int width,int height) {
        if(!isActive())return;
        Graphics2D g=(Graphics2D)original.create();
        double fade=Math.min(1,frames/150.0);fade=fade*fade*(3-2*fade);
        g.setColor(new Color(6,16,37,bossIntro?90:(int)(20+35*fade)));
        g.fillRect(camera.getX(),camera.getY(),width,height);
        if(bossIntro) {
            WorldMap.Point tree=bossAnchor==null?map.point("landmarks/Corrupted Town Center"):bossAnchor;
            double x=cinematicBoss==null?tree.x():cinematicBoss.getX()+cinematicBoss.getSize()/2.0;
            double y=cinematicBoss==null?tree.y()+15:cinematicBoss.getY()+cinematicBoss.getSize();
            double energy=pageIndex==0?ease(frames/90.0):1-ease((frames-pageStart)/45.0);
            util.LightBloom.draw(g,x,y-35,160,.20*energy,bossTitle.equals("FROST FANG")?util.LightBloom.CYAN:util.LightBloom.RED);
            g.setComposite(java.awt.AlphaComposite.SrcOver.derive((float)(.38*energy)));
            boss.BossArtwork.effect(g,bossTitle.equals("FROST FANG")?"wave":"titan_wave",frames/7,x,y-8,bossTitle.equals("FROST FANG")?190:270,90,0);
        } else util.LightBloom.draw(g,santa.getX()+16,santa.getY()+21,76,.2*(1-fade),util.LightBloom.GOLD);
        g.dispose();
    }

    public void draw(Graphics2D original, int width, int height) {
        if (!isActive()) return;
        Graphics2D g=(Graphics2D)original.create();
        java.awt.Rectangle panel=panelBounds(g,width,height);
        int boxW=panel.width,boxH=panel.height,x=panel.x,y=panel.y;
        g.setFont(new Font("Dialog",Font.PLAIN,14));
        var lines=ui.ChristmasUI.wrap(g.getFontMetrics(),pages.get(pageIndex),boxW-92);
        ui.ChristmasUI.frame(g,"parchment",new java.awt.Rectangle(x,y,boxW,boxH));
        g.setFont(new Font("Serif",Font.BOLD,22));g.setColor(ui.ChristmasUI.RED);
        drawCentered(g,bossIntro?bossTitle:"SANTA: LAST CHRISTMAS",width/2,y+(bossIntro?34:45));
        g.setFont(new Font("Serif",Font.ITALIC,13));g.setColor(ui.ChristmasUI.INK);
        drawCentered(g,bossIntro?"The Guardian Beneath the Black Frost":"One more promise. One last light.",width/2,y+(bossIntro?54:66));
        g.setFont(new Font("Dialog",Font.PLAIN,14));int lineY=y+(bossIntro?77:95);
        for(String line:lines){drawCentered(g,line,width/2,lineY);lineY+=bossIntro?18:22;}
        ui.ChristmasUI.button(g,"[E] Continue / skip scene",new java.awt.Rectangle(width/2-132,y+boxH-(bossIntro?40:42),264,bossIntro?28:30),"wood",false,true);
        g.dispose();
    }

    /** Read-only geometry; both boss pages share their maximum measured body height. */
    public java.awt.Rectangle panelBounds(Graphics2D g,int width,int height) {
        g.setFont(new Font("Dialog",Font.PLAIN,14));
        int w=Math.min(bossIntro?720:650,width-48),count=0;
        if(bossIntro){for(String page:pages)count=Math.max(count,ui.ChristmasUI.wrap(g.getFontMetrics(),page,w-92).size());}
        else if(isActive())count=ui.ChristmasUI.wrap(g.getFontMetrics(),pages.get(pageIndex),w-92).size();
        int h=bossIntro?118+count*18:140+count*22;
        return new java.awt.Rectangle((width-w)/2,bossIntro?height-h-30:(height-h)/2,w,h);
    }

    private static List<String> wrap(FontMetrics fm, String text, int width) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\\n")) {
            String line = "";
            for (String word : paragraph.split(" ")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && fm.stringWidth(next) > width) { out.add(line); line = word; }
                else line = next;
            }
            if (!line.isEmpty()) out.add(line);
            if (paragraph.isEmpty()) out.add("");
        }
        return out;
    }

    private static void drawCentered(Graphics2D g, String text, int cx, int y) {
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
