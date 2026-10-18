package ui;

import boss.ChristmasEater;
import entity.Santa;
import entity.Enemy;
import game.AreaManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import quest.*;
import spirit.SpiritTree;
import util.ImageAssets;

/** Compact screen-space HUD using the supplied UI artwork. */
public final class HUD {
    private static final BufferedImage HP=ImageAssets.load("assets/ui/hud_hp_bar.png");
    private static final BufferedImage SPIRIT=ImageAssets.load("assets/ui/hud_spirit_bar.png");
    private static final BufferedImage QUEST=ImageAssets.load("assets/ui/quest_panel.png");
    private static final BufferedImage BOSS=ImageAssets.load("assets/ui/boss_hp_bar.png");
    private static final Color TEXT=new Color(246,246,240);
    private final AbilityHUD abilityHud=new AbilityHUD();
    private final BossHud bossHud=new BossHud();
    private long bossHudPaintTime;
    public static Rectangle bossBounds(int viewportWidth){
        int width=Math.min(360,viewportWidth-540);
        return new Rectangle((viewportWidth-width)/2,38,width,69);
    }

    public void draw(Graphics2D g,int w,int h,Santa santa,SpiritTree spirit,game.AbilitySystem abilities,
            AreaManager areas,ChristmasEater boss,String areaMessage,StoryQuestManager story,boolean unlocked) {
        draw(g,w,h,santa,spirit,abilities,areas,boss,areaMessage,story,unlocked,null);
    }
    public void draw(Graphics2D g,int w,int h,Santa santa,SpiritTree spirit,game.AbilitySystem abilities,
            AreaManager areas,ChristmasEater boss,String areaMessage,StoryQuestManager story,boolean unlocked,game.FinalChapter chapter) {
        draw(g,w,h,santa,spirit,abilities,areas,boss,areaMessage,story,unlocked,chapter,null);
    }
    public void draw(Graphics2D g,int w,int h,Santa santa,SpiritTree spirit,game.AbilitySystem abilities,
            AreaManager areas,Enemy boss,String areaMessage,StoryQuestManager story,boolean unlocked,
            game.FinalChapter chapter,game.QuestGuidance.Objective objective) {
        g.setFont(new Font("Dialog",Font.PLAIN,12));

        drawHealth(g,santa.getHealth(),santa.getMaximumHealth());
        g.setFont(new Font("Dialog",Font.BOLD,10));

        drawImage(g,SPIRIT,w-249,12,235,101);
        drawBar(g,w-207,72,158,10,spirit.getSpirit(),spirit.getMaximumSpirit(),new Color(70,190,211));
        g.setColor(TEXT);
        String spiritText="Christmas Spirit  "+spirit.getSpirit()+" / "+spirit.getMaximumSpirit();
        int spiritX=w-53-g.getFontMetrics().stringWidth(spiritText);
        g.drawString(spiritText,spiritX,62);

        if(objective!=null)drawQuestList(g,objective,chapter,w,h);
        else drawQuest(g, h, chapter!=null&&chapter.isUnlocked()&&(areas.getCurrentArea().getType()!=game.Area.Type.CHRISTMAS_TOWN||chapter.celebrationStarted())?chapter.hud(areas.getCurrentArea().getType()):areas.getCurrentArea().getType()==game.Area.Type.SANTA_BASE
            ?new StoryQuestManager.HudPresentation(StoryQuestManager.HudState.NEXT_OBJECTIVE,"THE JOURNEY","The Last Workshop",areas.hasOpeningPresents()?"Depart for The Last Village":"Collect the Presents\nGift Storage [E]")
            :story.getHudPresentation());

        g.setFont(new Font("Dialog",Font.BOLD,12)); g.setColor(new Color(255,231,155));
        String areaName=areas.getCurrentArea().getName();
        int areaWidth=g.getFontMetrics().stringWidth(areaName)+24;
        UITheme.panel(g,(w-areaWidth)/2,10,areaWidth,25);
        g.setColor(UITheme.GOLD);drawCentered(g,areaName,w/2,27);
        abilityHud.draw(g,w,h,santa,abilities);

        long now=System.nanoTime();
        // Presentation time only: no timer, combat callback or game-state mutation.
        double gap=bossHudPaintTime==0?0:(now-bossHudPaintTime)/1e9;
        double elapsed=Math.min(.05,gap);
        bossHudPaintTime=now;
        // The host hides HUD during its fall cutscene. Never bring the dead card back afterward.
        if(gap>.25&&(boss==null||boss.isDefeated()))bossHud.clear();
        bossHud.update(boss,elapsed);
        bossHud.draw(g,bossBounds(w));
        // Retain the previous renderer's outgoing state for callers drawing later screen labels.
        if(boss instanceof boss.BossCombatant&&!boss.isDefeated()){
            g.setFont(new Font("Dialog",Font.PLAIN,11));g.setColor(UITheme.GOLD);
        }
        if(boss!=null&&!boss.isDefeated()&&!(boss instanceof boss.BossCombatant)){
            if(boss instanceof ChristmasEater legacy){
            drawImage(g,BOSS,w/2-250,52,500,121);
            drawBar(g,w/2-190,124,380,13,boss.getHealth(),boss.getMaximumHealth(),new Color(185,43,55));
            g.setColor(TEXT);g.setFont(new Font("Dialog",Font.BOLD,13));
            String title="DARK TITAN  -  PHASE "+(legacy.getPhase().ordinal()+1);
            drawCentered(g,title,w/2,112);
            }
        }
    }
    /** A presentation-only model and four small cached pixel frames. No boss mutation. */
    static final class BossHud {
        private static final Color INK=new Color(7,10,22), ICE=new Color(159,229,252),
                CYAN=new Color(72,222,249), VIOLET=new Color(162,118,236),
                RED=new Color(206,34,63), RED_LIGHT=new Color(255,111,123),
                TRAIL=new Color(233,163,96), WHITE=new Color(242,250,255);
        private static final Font TITLE=new Font("Dialog",Font.BOLD,12),
                SMALL=new Font("Dialog",Font.PLAIN,10), NUMBER=new Font("Dialog",Font.BOLD,9);
        private static final BufferedImage FANG_SMALL=frame(260,false), FANG_WIDE=frame(360,false),
                TITAN_SMALL=frame(260,true), TITAN_WIDE=frame(360,true);
        private Enemy actor;
        private int health,maximum,phase;
        private double trail,opacity,hit,phaseFlash,damageHold;

        void update(Enemy candidate,double seconds) {
            double dt=Math.max(0,Math.min(.05,seconds));
            if(candidate instanceof boss.BossCombatant information&&!candidate.isDefeated()) {
                if(candidate!=actor) {
                    actor=candidate;health=candidate.getHealth();maximum=candidate.getMaximumHealth();
                    trail=health;phase=information.getPhaseIndex();opacity=0;hit=phaseFlash=damageHold=0;
                }
                int next=candidate.getHealth();
                if(next<health){hit=.18;damageHold=.2;trail=Math.max(trail,health);}
                if(next>health)trail=next;
                health=next;maximum=candidate.getMaximumHealth();
                if(phase!=information.getPhaseIndex()){phase=information.getPhaseIndex();phaseFlash=.65;}
                opacity=Math.min(1,opacity+dt/.25);
            } else {
                // Abandonment is immediate; defeat has a visual exit, never a stale live HP bar.
                if(actor==null)return;
                if(!actor.isDefeated()){clear();return;}
                health=0;opacity=Math.max(0,opacity-dt/.2);
                if(opacity==0){clear();return;}
            }
            hit=Math.max(0,hit-dt);phaseFlash=Math.max(0,phaseFlash-dt);
            damageHold=Math.max(0,damageHold-dt);
            if(damageHold==0)trail=health+(trail-health)*Math.exp(-dt*8);
            if(Math.abs(trail-health)<.1)trail=health;
        }
        void clear(){actor=null;health=maximum=phase=0;trail=opacity=hit=phaseFlash=damageHold=0;}
        void draw(Graphics2D original,Rectangle bounds) {
            if(actor==null||opacity<=0)return;
            boolean titan=actor instanceof boss.DarkTitan;
            Graphics2D g=(Graphics2D)original.create();
            try {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)opacity));
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int x=bounds.x,y=bounds.y+(int)Math.round((1-opacity)*5),w=bounds.width;
                BufferedImage art=titan?(w<=260?TITAN_SMALL:TITAN_WIDE):(w<=260?FANG_SMALL:FANG_WIDE);
                // Preserve the end silhouettes; only the quiet center stretch adapts to width.
                g.drawImage(art,x,y,x+64,y+69,0,0,64,69,null);
                g.drawImage(art,x+64,y,x+w-36,y+69,64,0,art.getWidth()-36,69,null);
                g.drawImage(art,x+w-36,y,x+w,y+69,art.getWidth()-36,0,art.getWidth(),69,null);
                int barX=x+60,barY=y+32,barW=w-82;
                int actual=(int)((long)barW*health/Math.max(1,maximum));
                int trailing=(int)Math.round(barW*Math.min(maximum,trail)/Math.max(1,maximum));
                g.setColor(INK);g.fillRect(barX,barY,barW,13);
                g.setColor(TRAIL);g.fillRect(barX,barY,trailing,13);
                g.setColor(RED);g.fillRect(barX,barY,actual,13);
                g.setColor(new Color(247,57,81));g.fillRect(barX,barY+2,actual,3);
                g.setColor(RED_LIGHT);g.fillRect(barX,barY,actual,1);
                g.setColor(new Color(133,16,43));g.fillRect(barX,barY+10,actual,3);
                if(hit>0){g.setColor(new Color(255,231,213,(int)(hit/.18*150)));g.fillRect(barX,barY,actual,13);}
                g.setFont(TITLE);g.setColor(WHITE);
                String name=((boss.BossCombatant)actor).getBossName();
                centered(g,name,x+60+(w-118)/2,y+24);
                for(int i=0;i<3;i++) {
                    int px=x+w-43+i*12,py=y+13;
                    diamond(g,px,py,5,i<=phase?(titan?VIOLET:ICE):new Color(43,57,76));
                    diamond(g,px,py,3,i==phase?CYAN:i<phase?(titan?VIOLET:ICE):INK);
                    if(i==phase){g.setColor(WHITE);g.fillRect(px-1,py-2,2,2);}
                }
                if(phaseFlash>0){g.setColor(new Color(174,243,255,(int)(phaseFlash/.65*220)));g.drawRect(barX-2,barY-2,barW+3,16);}
                g.setFont(NUMBER);g.setColor(WHITE);centered(g,health+" / "+maximum,barX+barW/2,barY+10);
                g.setFont(SMALL);g.setColor(titan?new Color(203,187,242):ICE);
                String status=health==0?"DEFEATED":((boss.BossCombatant)actor).getCombatStatus();
                if(status.isBlank())status="PHASE "+(phase+1)+" / 3";
                while(g.getFontMetrics().stringWidth(status)>w-20&&status.length()>4)
                    status=status.substring(0,status.length()-4)+"...";
                centered(g,status,x+w/2,y+61);
            } finally {g.dispose();}
        }
        private static void centered(Graphics2D g,String text,int x,int y){
            g.setColor(INK);g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2+1,y+1);
            g.setColor(WHITE);g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);
        }
        private static void diamond(Graphics2D g,int x,int y,int r,Color color){
            g.setColor(color);g.fillPolygon(new int[]{x,x+r,x,x-r},new int[]{y-r,y,y+r,y},4);
        }
        private static BufferedImage frame(int width,boolean titan) {
            BufferedImage image=new BufferedImage(width,69,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();
            Color edge=titan?new Color(80,63,113):new Color(64,113,165),light=titan?VIOLET:ICE;
            // Distinct armored slabs versus a thin, fractured rim with hanging icicles.
            int[] xs={4,14,46,58,width-22,width-10,width-3,width-12,57,42,13,4};
            int[] ys=titan?new int[]{18,7,9,18,18,25,39,51,51,57,55,45}
                    :new int[]{20,13,14,26,26,30,39,49,49,54,51,43};
            g.setColor(INK);g.fillPolygon(xs,ys,xs.length);
            g.setColor(edge);g.setStroke(new BasicStroke(titan?5:3));g.drawPolyline(xs,ys,xs.length);
            g.setColor(light);g.setStroke(new BasicStroke(1));g.drawLine(59,28,width-22,28);
            g.drawLine(59,48,width-21,48);
            g.setColor(new Color(17,25,42));g.fillRect(59,29,width-80,18);
            g.setColor(new Color(36,45,65));g.drawLine(59,29,width-22,29);
            for(int i=0;i<8;i++) {
                int x=65+i*(width-96)/8;
                if(titan) {
                    g.setColor(new Color(43,32,63));g.fillPolygon(new int[]{x-6,x+8,x+10,x},new int[]{20,16,22,27},4);
                    diamond(g,x+2,50,4,edge);diamond(g,x+2,50,2,VIOLET);
                } else {
                    int h=5+(i*7)%6;
                    g.setColor(edge);g.fillPolygon(new int[]{x-4,x+4,x},new int[]{49,49,49+h},3);
                    g.setColor(ICE);g.drawLine(x-3,49,x,49+h-1);
                    g.setColor(CYAN);g.fillRect(x+2,26,2,2);
                }
            }
            for(int i=0;i<3;i++) {
                int x=width-30+i*8,top=titan?19-i*6:27-i*5;
                g.setColor(edge);g.fillPolygon(new int[]{x-4,x+2,x+7,x+1},new int[]{38,top,top+4,48},4);
                g.setColor(light);g.drawLine(x+2,top,x-2,38);
                g.setColor(CYAN);g.fillRect(x+1,top+4,2,3);
            }
            // Reuse the approved character face as a small medallion, not the concept collage.
            String source=titan?"assets/phase10_3/titan/heavy_walk/frame_01.png"
                    :"assets/bosses/frost_fang/idle/frame_01.png";
            BufferedImage portrait=ImageAssets.load(source);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if(titan){
                g.setColor(edge);g.fillPolygon(new int[]{4,17,41,56,48,29,9},new int[]{28,5,5,25,49,56,48},7);
                g.setColor(VIOLET);g.drawLine(17,6,6,28);g.drawLine(48,25,39,48);
                if(portrait!=null)g.drawImage(portrait,6,3,53,51,200,20,331,164,null);
                diamond(g,30,47,7,edge);diamond(g,30,47,4,new Color(232,66,112));diamond(g,29,45,2,WHITE);
            }else{
                g.setColor(edge);g.fillPolygon(new int[]{3,11,24,44,57,44,23,5},new int[]{30,13,6,10,33,52,56,44},8);
                if(portrait!=null)g.drawImage(portrait,3,4,57,55,17,104,101,183,null);
                g.setColor(ICE);g.drawLine(3,31,8,21);g.drawLine(20,54,37,51);
            }
            g.dispose();return image;
        }
    }
    private record QuestRow(String text,int style){ }
    private static java.util.List<QuestRow> questRows(game.QuestGuidance.Objective objective,boolean collapsed,game.FinalChapter chapter){
        var rows=new java.util.ArrayList<QuestRow>();
        rows.add(new QuestRow("MAIN QUEST",0));
        rows.add(new QuestRow(objective.title()+(objective.progress().isBlank()?"":"  "+objective.progress()),1));
        rows.add(new QuestRow(objective.action(),2));
        if(collapsed&&objective.title().equals("Restore the Village")&&chapter.isUnlocked()&&!chapter.isComplete()){
            for(var material:game.FinalChapter.Material.values()){
                boolean relevant=chapter.activeObjectives().stream().anyMatch(task->task.material==material);
                if(relevant)rows.add(new QuestRow(material.label+": "+chapter.count(material)+"/3",2));
            }
        }
        if(!collapsed){
            for(String detail:objective.details())rows.add(new QuestRow(detail,2));
            if(!objective.optional().isEmpty()){
                rows.add(new QuestRow("OPTIONAL - Christmas Presents",0));
                for(String recipient:objective.optional())rows.add(new QuestRow(recipient,2));
            }
        }
        return rows;
    }
    // Wrapping and hit testing use this exact width, font and line height at every viewport size.
    private static java.util.List<String> questLines(String text,int style){
        FontMetrics metrics=style==2?QUEST_BODY_METRICS:QUEST_METRICS;
        var lines=new java.util.ArrayList<String>();String line="";
        for(String word:text.split("\\s+")){
            String next=line.isEmpty()?word:line+" "+word;
            // Conservative measured Dialog 12 width using the shared cached metrics.
            if(metrics.stringWidth(next)>232&&!line.isEmpty()){lines.add(line);line=word;}else line=next;
        }
        if(!line.isEmpty())lines.add(line);return lines;
    }
    private static final Font QUEST_FONT=new Font("Dialog",Font.PLAIN,11);
    private static final FontMetrics QUEST_METRICS=questMetrics();
    private static final FontMetrics QUEST_BODY_METRICS=questBodyMetrics();
    private static FontMetrics questBodyMetrics(){var image=new BufferedImage(1,1,2);var g=image.createGraphics();g.setFont(QUEST_FONT);var metrics=g.getFontMetrics();g.dispose();return metrics;}
    private static FontMetrics questMetrics(){var image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();g.setFont(new Font("Dialog",Font.BOLD,12));var result=g.getFontMetrics();g.dispose();return result;}
    public static Rectangle questBounds(game.QuestGuidance.Objective objective,game.FinalChapter chapter,int width,int height){
        if(objective==null||chapter==null)return new Rectangle();
        int lines=0;for(var row:questRows(objective,chapter.trackerCollapsed(),chapter))lines+=questLines(row.text(),row.style()).size();
        int panelHeight=52+lines*16;
        return new Rectangle(14,height-14-panelHeight,280,panelHeight);
    }
    public static Rectangle questToggleBounds(game.QuestGuidance.Objective objective,game.FinalChapter chapter,int width,int height){
        var box=questBounds(objective,chapter,width,height);
        return box.isEmpty()?new Rectangle():new Rectangle(box.x+177,box.y+7,94,23);
    }
    public static boolean questClick(game.QuestGuidance.Objective objective,game.FinalChapter chapter,int x,int y,int width,int height){
        if(!questBounds(objective,chapter,width,height).contains(x,y))return false;
        if(questToggleBounds(objective,chapter,width,height).contains(x,y))chapter.toggleTracker();
        return true;
    }
    private static void drawQuestList(Graphics2D original,game.QuestGuidance.Objective objective,game.FinalChapter chapter,int width,int height){
        Graphics2D g=(Graphics2D)original.create();var box=questBounds(objective,chapter,width,height);
        ChristmasUI.frame(g,"tracker",box);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Dialog",Font.BOLD,12));g.setColor(UITheme.GOLD);g.drawString("QUESTS",box.x+24,box.y+23);
        var toggle=questToggleBounds(objective,chapter,width,height);
        ChristmasUI.button(g,chapter.trackerCollapsed()?"Expand [+]":"Collapse [-]",toggle,"wood",false,true);
        int y=box.y+49;
        for(var row:questRows(objective,chapter.trackerCollapsed(),chapter)){
            g.setColor(row.style()==0?UITheme.GOLD:row.style()==1?UITheme.IVORY:UITheme.MUTED);
            g.setFont(row.style()==2?QUEST_FONT:new Font("Dialog",Font.BOLD,12));
            for(String line:questLines(row.text(),row.style())){g.drawString(line,box.x+24,y);y+=16;}
        }
        if(numericProgress(objective.progress())){
            var m=java.util.regex.Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(objective.progress());
            if(m.find()){int value=Integer.parseInt(m.group(1)),total=Integer.parseInt(m.group(2));
                g.setColor(new Color(22,29,39));g.fillRect(box.x+24,box.y+box.height-12,232,4);
                g.setColor(UITheme.GOLD);g.fillRect(box.x+24,box.y+box.height-12,(int)(232*Math.min(1,value/(double)Math.max(1,total))),4);}
        }
        g.dispose();
    }
    private static boolean numericProgress(String text){return text.matches(".*\\d+\\s*/\\s*\\d+.*");}
    /** These exact rectangles are also used by GamePanel's mouse input owner. */
    public static Rectangle restorationBounds(game.FinalChapter chapter,int width,int height){
        if(chapter==null||!chapter.trackerVisible())return new Rectangle();
        int h=34;
        if(!chapter.trackerCollapsed())for(var objective:game.FinalChapter.Restoration.values())if(!chapter.restored(objective))h+=objective.material==null?26:38;
        return new Rectangle(Math.max(8,width-246),126,234,h);
    }
    public static Rectangle restorationToggleBounds(game.FinalChapter chapter,int width,int height){
        Rectangle r=restorationBounds(chapter,width,height);return r.isEmpty()?new Rectangle():new Rectangle(r.x+r.width-29,r.y+6,22,22);
    }
    public static boolean restorationClick(game.FinalChapter chapter,int x,int y,int width,int height){
        Rectangle box=restorationBounds(chapter,width,height);if(!box.contains(x,y))return false;
        if(restorationToggleBounds(chapter,width,height).contains(x,y))chapter.toggleTracker();
        return true;
    }
    public static void drawRestorationTracker(Graphics2D original,game.FinalChapter chapter,int width,int height){
        Rectangle r=restorationBounds(chapter,width,height);if(r.isEmpty())return;
        Graphics2D g=(Graphics2D)original.create();UITheme.panel(g,r.x,r.y,r.width,r.height);
        g.setColor(UITheme.GOLD);g.setFont(new Font("Dialog",Font.BOLD,11));
        g.drawString("Village Restoration  "+chapter.completed()+"/4",r.x+12,r.y+22);
        Rectangle toggle=restorationToggleBounds(chapter,width,height);g.drawRoundRect(toggle.x,toggle.y,toggle.width,toggle.height,5,5);
        g.drawLine(toggle.x+6,toggle.y+11,toggle.x+16,toggle.y+11);if(chapter.trackerCollapsed())g.drawLine(toggle.x+11,toggle.y+6,toggle.x+11,toggle.y+16);
        if(!chapter.trackerCollapsed()){
            int y=r.y+52;
            for(var objective:game.FinalChapter.Restoration.values())if(!chapter.restored(objective)){
                g.setFont(new Font("Dialog",Font.BOLD,11));g.setColor(UITheme.IVORY);g.drawString(objective.label,r.x+12,y);
                if(objective.material!=null){g.setFont(new Font("Dialog",Font.PLAIN,11));g.setColor(UITheme.MUTED);g.drawString(objective.material.label+": "+chapter.count(objective.material)+" / 3",r.x+12,y+16);y+=38;}else y+=26;
            }
        }
        g.dispose();
    }
    private void drawCentered(Graphics2D g,String s,int cx,int y){g.drawString(s,cx-g.getFontMetrics().stringWidth(s)/2,y);}
    private void drawImage(Graphics2D g,BufferedImage im,int x,int y,int w,int h){ if(im!=null)g.drawImage(im,x,y,w,h,null); }
    private void drawBar(Graphics2D g,int x,int y,int w,int h,int v,int m,Color c){g.setColor(new Color(20,24,30,190));g.fillRoundRect(x,y,w,h,h,h);int f=(int)((long)w*Math.max(0,Math.min(m,v))/Math.max(1,m));g.setColor(c);g.fillRoundRect(x,y,f,h,h,h);}
    // Regions measured inside the HP artwork at its existing 245 x 101 size.
    // The old fill rectangle (55,72,162,10) was below the illustrated red bar.
    static final Rectangle HP_LABEL_REGION=new Rectangle(52,43,34,32);
    static final Rectangle HP_BAR_REGION=new Rectangle(97,54,132,15);

    void drawHealth(Graphics2D graphics,int current,int maximum) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            drawImage(g,HP,HP_X,HP_Y,HP_WIDTH,HP_HEIGHT);
            Rectangle bar=HP_BAR_REGION;
            drawBar(g,bar.x,bar.y,bar.width,bar.height,current,maximum,new Color(199,55,63));
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font("Dialog",Font.BOLD,10));
            g.setColor(TEXT);
            drawRegionCentered(g,"HP",HP_LABEL_REGION);
            drawRegionCentered(g,current+" / "+maximum,bar);
        } finally { g.dispose(); }
    }

    private void drawRegionCentered(Graphics2D g,String text,Rectangle region) {
        FontMetrics metrics=g.getFontMetrics();
        int x=region.x+(region.width-metrics.stringWidth(text))/2;
        int baseline=region.y+(region.height-metrics.getAscent()-metrics.getDescent())/2
                +metrics.getAscent();
        g.drawString(text,x,baseline);
    }

    // Quest layout uses cropped artwork strips; the source PNG is unchanged.
    static final int HP_X=14, HP_Y=12, HP_WIDTH=245, HP_HEIGHT=101;
    static final int QUEST_X=14, QUEST_BOTTOM_MARGIN=14;
    static int questY(int screenHeight) { return screenHeight-QUEST_HEIGHT-QUEST_BOTTOM_MARGIN; }
    static final int QUEST_WIDTH=280, QUEST_HEIGHT=180;
    static final int SAFE_X=40, SAFE_Y=45, SAFE_WIDTH=192, SAFE_HEIGHT=86;
    static final int TEXT_INSET=2; // Allows antialiased glyph overhang inside the safe rectangle.
    static final int HEADER_BASELINE=12, TITLE_BASELINE=32, OBJECTIVE_BASELINE=53;
    static final int LINE_HEIGHT=14;
    static final int HEADER_FONT_SIZE=11, TITLE_FONT_SIZE=12, OBJECTIVE_FONT_SIZE=11;
    static final Color COMPLETE_TEXT=new Color(112,235,135);

    /** Nine runtime slices retain the frame while reclaiming its unused interior.
     * Outer strips keep the wreath/ribbon/lantern; the center uses only the dark
     * source region to the right of the three circles (x=108..220).
     * Decorations get small edge bands instead of setting the content width.
     */
    private void drawQuestArtwork(Graphics2D g) {
        if(QUEST==null) return;
        int[] sourceX={0,55,220,284}, sourceY={0,80,345,444};
        int[] destX={0,28,248,QUEST_WIDTH}, destY={0,40,135,QUEST_HEIGHT};
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for(int row=0;row<3;row++) for(int col=0;col<3;col++) {
            int sx=col==1 && row==1 ? 108 : sourceX[col];
            g.drawImage(QUEST,destX[col],destY[row],
                    destX[col+1],destY[row+1],
                    sx,sourceY[row],sourceX[col+1],sourceY[row+1],null);
        }
    }

    void drawQuest(Graphics2D graphics, int screenHeight, StoryQuestManager.HudPresentation view) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.translate(QUEST_X,questY(screenHeight));
            drawQuestArtwork(g);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int x=SAFE_X+TEXT_INSET, y=SAFE_Y;
            g.setColor(view.state()==StoryQuestManager.HudState.COMPLETE ? COMPLETE_TEXT : TEXT);
            g.setFont(new Font("Dialog",Font.BOLD,HEADER_FONT_SIZE));
            g.drawString(view.header(),x,y+HEADER_BASELINE);
            g.setFont(new Font("Dialog",Font.BOLD,TITLE_FONT_SIZE));
            drawQuestLines(g,view.title(),x,y+TITLE_BASELINE);
            g.setColor(new Color(255,231,169));
            g.setFont(new Font("Dialog",Font.PLAIN,OBJECTIVE_FONT_SIZE));
            drawQuestLines(g,view.objective(),x,y+OBJECTIVE_BASELINE);
        } finally { g.dispose(); }
    }

    static java.util.List<String> wrapQuestText(FontMetrics metrics,String text) {
        java.util.List<String> lines=new java.util.ArrayList<>();
        if(text.isBlank()) return lines;
        for (String paragraph : text.split("\\R", -1)) {
            String line="";
            for(String word:paragraph.split("\\s+")) {
                String candidate=line.isEmpty()?word:line+" "+word;
                if(!line.isEmpty() && metrics.stringWidth(candidate)>SAFE_WIDTH-2*TEXT_INSET) {
                    lines.add(line);
                    line=word;
                } else line=candidate;
            }
            if(!line.isEmpty()) lines.add(line);
        }
        return lines;
    }

    private void drawQuestLines(Graphics2D g,String text,int x,int baseline) {
        for(String line:wrapQuestText(g.getFontMetrics(),text)) {
            g.drawString(line,x,baseline);
            baseline+=LINE_HEIGHT;
        }
    }
}
