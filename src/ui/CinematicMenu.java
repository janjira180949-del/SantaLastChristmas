package ui;

import game.EndingCredits;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import util.LightBloom;
import audio.AudioManager;
import audio.AudioCatalog.Effect;

/** Menu-only layout, input, motion and informational pages. No gameplay state ownership. */
public final class CinematicMenu {
    public enum Page { MAIN, STORY, SETTINGS, CREDITS }
    public enum Action { NONE, START, EXIT }
    private static final String[] LABELS={"START","STORY","SETTINGS","CREDITS","EXIT"};
    private static final String[] AUDIO_LABELS={"Master volume","Music volume","SFX volume","Mute"};
    private static final int PRESS_TICKS=6,TRANSITION_TICKS=16;
    private static final Color NAVY=new Color(6,16,31),GOLD=new Color(221,174,115),IVORY=new Color(255,236,201);
    private static final BufferedImage[] ICONS=icons();
    private static final Font LABEL_FONT=new Font("Serif",Font.PLAIN,34),PAGE_TITLE=new Font("Serif",Font.BOLD,27),TEXT_FONT=new Font("Dialog",Font.PLAIN,15);
    private static final String PREMISE="After the catastrophe, Christmas survived in small acts of kindness. Tonight the Spirit Tree is fading. In the last village, Santa keeps one more promise.";
    private final MenuBackdrop backdrop=new MenuBackdrop();
    private final AudioManager audio;
    private final float[] highlights=new float[5];
    private Page page=Page.MAIN;
    private int selected,hover=-1,pressed=-1,pressTicks,ticks,transitionTicks=TRANSITION_TICKS,width,height;
    private Rectangle[] buttons;
    private Rectangle panel,back,motionToggle;
    private Rectangle[] audioRows;
    private boolean reducedMotion,confirmDown,mouseDown;
    private int settingsSelection;
    private Action action=Action.NONE;
    private final List<String> storyLines=new ArrayList<>();
    public CinematicMenu() {this(new AudioManager());}
    public CinematicMenu(AudioManager audio) {this.audio=audio;highlights[0]=1;}
    public Page getPage() {return page;}
    public int getSelected() {return selected;}
    public int getHovered() {return hover;}
    public int getPressTicks() {return pressTicks;}
    public boolean isReducedMotion() {return reducedMotion;}
    public float getHighlight(int row) {return highlights[row];}
    public void resetNavigation() {
        page=Page.MAIN;selected=0;hover=pressed=-1;pressTicks=0;confirmDown=mouseDown=false;
        java.util.Arrays.fill(highlights,0);highlights[0]=1;
        action=Action.NONE;transitionTicks=0;
    }
    public void focusLost() {confirmDown=mouseDown=false;hover=pressed=-1;pressTicks=0;}
    public static Rectangle button(int width,int height,int row) {
        if(row<0||row>=5)throw new IllegalArgumentException("Menu row");
        Rectangle art=MenuBackdrop.artBounds(width,height);double s=art.width/1536.0;
        return new Rectangle(art.x+(int)Math.round(1040*s),art.y+(int)Math.round((420+row*92)*s),
            (int)Math.round(408*s),(int)Math.round(76*s));
    }
    private void layout(int w,int h) {
        if(buttons!=null&&width==w&&height==h)return;
        width=w;height=h;buttons=new Rectangle[5];for(int i=0;i<5;i++)buttons[i]=button(w,h,i);
        int pw=Math.min(630,w-64),ph=Math.min(472,h-72);
        panel=new Rectangle((w-pw)/2,(h-ph)/2,pw,ph);
        back=new Rectangle(panel.x+pw/2-90,panel.y+ph-62,180,38);
        motionToggle=new Rectangle(panel.x+30,panel.y+118,pw-60,38);
        audioRows=new Rectangle[4];
        for(int i=0;i<4;i++)audioRows[i]=new Rectangle(panel.x+30,panel.y+164+i*43,pw-60,38);
        storyLines.clear();BufferedImage metrics=new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);Graphics2D g=metrics.createGraphics();
        try {
            g.setFont(TEXT_FONT);String line="";
            for(String word:PREMISE.split(" ")) {
                String next=line.isEmpty()?word:line+" "+word;
                if(!line.isEmpty()&&g.getFontMetrics().stringWidth(next)>pw-68){storyLines.add(line);line=word;}else line=next;
            }
            if(!line.isEmpty())storyLines.add(line);
        } finally {g.dispose();}
    }
    public Rectangle backButton(int w,int h) {layout(w,h);return new Rectangle(back);}
    public Rectangle motionButton(int w,int h) {layout(w,h);return new Rectangle(motionToggle);}
    public Rectangle audioButton(int w,int h,int row) {layout(w,h);return new Rectangle(audioRows[row]);}
    private int hit(int x,int y) {
        if(page==Page.MAIN) {
            for(int i=0;i<5;i++) {
                Rectangle b=buttons[i];int dx=(int)Math.ceil(b.width*.0125*highlights[i]),dy=(int)Math.ceil(b.height*.0125*highlights[i]);
                if(x>=b.x-dx&&x<b.x+b.width+dx&&y>=b.y-dy&&y<b.y+b.height+dy)return i;
            }
            return -1;
        }
        if(page==Page.SETTINGS&&motionToggle.contains(x,y))return 5;
        if(page==Page.SETTINGS)for(int i=0;i<4;i++)if(audioRows[i].contains(x,y))return 7+i;
        return back.contains(x,y)?6:-1;
    }
    public void mouseMoved(int x,int y,int w,int h) {
        layout(w,h);int next=hit(x,y);
        if(mouseDown&&pressed>=7&&pressed<=9){setSlider(pressed-7,x);return;}
        boolean changed=page==Page.MAIN?next>=0&&next!=selected:next>=0&&next!=hover;
        hover=next;if(page==Page.MAIN&&hover>=0)selected=hover;
        if(changed)audio.playEffect(Effect.HOVER);
    }
    public void mouseExited() {hover=-1;}
    public void mousePressed(int x,int y,int w,int h) {
        layout(w,h);if(pressTicks>0)return;pressed=hit(x,y);mouseDown=pressed>=0;
        if(page==Page.MAIN&&pressed>=0)selected=pressed;
        if(page==Page.SETTINGS&&pressed>=7&&pressed<=9)setSlider(pressed-7,x);
    }
    public void mouseReleased(int x,int y,int w,int h) {
        layout(w,h);if(!mouseDown)return;mouseDown=false;
        if(pressed>=7&&pressed<=9){audio.playEffect(Effect.SETTINGS);pressed=-1;}
        else if(pressed>=0&&hit(x,y)==pressed)pressTicks=PRESS_TICKS;else pressed=-1;
    }
    public void keyPressed(int code) {
        if(code==KeyEvent.VK_ESCAPE){if(page!=Page.MAIN&&pressTicks==0){audio.playEffect(Effect.BACK);show(Page.MAIN);}return;}
        if(pressTicks>0)return;
        if(code==KeyEvent.VK_UP||code==KeyEvent.VK_DOWN) {
            hover=-1;
            if(page==Page.MAIN)selected=Math.floorMod(selected+(code==KeyEvent.VK_UP?-1:1),5);
            else if(page==Page.SETTINGS)settingsSelection=Math.floorMod(settingsSelection+(code==KeyEvent.VK_UP?-1:1),6);
            audio.playEffect(Effect.HOVER);
        } else if(code==KeyEvent.VK_ENTER&&!confirmDown) {
            confirmDown=true;
            if(page==Page.SETTINGS&&settingsSelection>=1&&settingsSelection<=3){adjustVolume(settingsSelection-1,5);return;}
            pressed=page==Page.MAIN?selected:page==Page.SETTINGS?settingsSelection==0?5:settingsSelection==4?10:6:6;
            pressTicks=PRESS_TICKS;
        } else if(page==Page.SETTINGS&&(code==KeyEvent.VK_LEFT||code==KeyEvent.VK_RIGHT)) {
            if(settingsSelection==0){reducedMotion=!reducedMotion;audio.playEffect(Effect.SETTINGS);}
            else if(settingsSelection>=1&&settingsSelection<=3)adjustVolume(settingsSelection-1,code==KeyEvent.VK_LEFT?-5:5);
            else if(settingsSelection==4){audio.setMuted(!audio.isMuted());audio.playEffect(Effect.SETTINGS);}
        }
    }
    public void keyReleased(int code) {if(code==KeyEvent.VK_ENTER)confirmDown=false;}
    public void update() {
        if(!reducedMotion)ticks++;
        if(transitionTicks<TRANSITION_TICKS)transitionTicks++;
        for(int i=0;i<5;i++) {float target=page==Page.MAIN&&(selected==i||hover==i)?1:0;highlights[i]+=(target-highlights[i])*.18f;}
        if(pressTicks>0&&--pressTicks==0){activate(pressed);pressed=-1;}
    }
    private void activate(int row) {
        audio.playEffect(row==0?Effect.START:row==5||row==10?Effect.SETTINGS:row==6?Effect.BACK:row==4?Effect.CONFIRM:Effect.CLICK);
        switch(row) {
            case 0 -> action=Action.START;
            case 1 -> show(Page.STORY);
            case 2 -> show(Page.SETTINGS);
            case 3 -> show(Page.CREDITS);
            case 4 -> action=Action.EXIT;
            case 5 -> reducedMotion=!reducedMotion;
            case 6 -> show(Page.MAIN);
            case 10 -> audio.setMuted(!audio.isMuted());
            default -> { }
        }
    }
    private int volume(int row) {return row==0?audio.getMasterVolume():row==1?audio.getMusicVolume():audio.getSfxVolume();}
    private void setVolume(int row,int value) {
        if(row==0)audio.setMasterVolume(value);else if(row==1)audio.setMusicVolume(value);else audio.setSfxVolume(value);
    }
    private void adjustVolume(int row,int delta) {setVolume(row,volume(row)+delta);audio.playEffect(Effect.SETTINGS);}
    private void setSlider(int row,int x) {
        Rectangle r=audioRows[row];int start=r.x+r.width/2,end=r.x+r.width-62;
        setVolume(row,(int)Math.round(100.0*(x-start)/Math.max(1,end-start)));
        settingsSelection=row+1;
    }
    private void show(Page next) {page=next;hover=-1;settingsSelection=0;transitionTicks=0;}
    public Action pollAction() {Action result=action;action=Action.NONE;return result;}
    public void draw(Graphics2D original,int w,int h) {
        if(w<=0||h<=0)return;layout(w,h);backdrop.draw(original,w,h,ticks,!reducedMotion);
        Graphics2D g=(Graphics2D)original.create();
        try {g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);if(page==Page.MAIN)drawButtons(g);else drawPage(g);}
        finally {g.dispose();}
    }
    private void drawButtons(Graphics2D g) {
        float alpha=.45f+.55f*Math.min(1,transitionTicks/(float)TRANSITION_TICKS);
        Composite old=g.getComposite();g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        for(int i=0;i<5;i++) {
            Rectangle b=buttons[i];float glow=highlights[i];
            LightBloom.draw(g,b.getCenterX(),b.getCenterY(),b.width*.55,.18*glow,LightBloom.GOLD);
            Graphics2D slot=(Graphics2D)g.create();
            try {
                double scale=1+.025*glow-(pressed==i&&(pressTicks>0||mouseDown)?.025:0);
                slot.translate(b.getCenterX(),b.getCenterY());slot.scale(scale,scale);slot.translate(-b.getCenterX(),-b.getCenterY());
                int cut=Math.max(5,b.height/7);
                Polygon outline=new Polygon(new int[]{b.x+cut,b.x+b.width-cut,b.x+b.width,b.x+b.width,b.x+b.width-cut,b.x+cut,b.x,b.x},
                    new int[]{b.y,b.y,b.y+cut,b.y+b.height-cut,b.y+b.height,b.y+b.height,b.y+b.height-cut,b.y+cut},8);
                slot.setPaint(new GradientPaint(b.x,b.y,NAVY,b.x,b.y+b.height,new Color(6+(int)(88*glow),16,31+(int)(3*glow))));slot.fillPolygon(outline);
                slot.setStroke(new BasicStroke(glow>.5?1.6f:1));slot.setColor(glow>.5?IVORY:GOLD);slot.drawPolygon(outline);
                slot.setColor(GOLD);slot.drawLine(b.x+cut,b.y+4,b.x+b.width-cut,b.y+4);
                int icon=Math.max(19,b.height*3/5);slot.drawImage(ICONS[i],b.x+b.height/2-icon/2,b.y+(b.height-icon)/2,icon,icon,null);
                slot.setFont(LABEL_FONT.deriveFont(Math.max(18,b.height*.48f)));slot.setColor(IVORY);
                String label=LABELS[i];slot.drawString(label,b.x+(b.width-slot.getFontMetrics().stringWidth(label))/2+icon/3,
                    b.y+(b.height-slot.getFontMetrics().getHeight())/2+slot.getFontMetrics().getAscent());
                ornament(slot,b.x-4,b.y+b.height/2,Math.max(3,b.height/12));ornament(slot,b.x+b.width+4,b.y+b.height/2,Math.max(3,b.height/12));
            } finally {slot.dispose();}
        }
        g.setComposite(old);g.setColor(new Color(246,225,188,190));g.setFont(new Font("Dialog",Font.PLAIN,12));
        String hint="↑ / ↓  Select     Enter  Confirm";Rectangle last=buttons[4];
        g.drawString(hint,(int)last.getCenterX()-g.getFontMetrics().stringWidth(hint)/2,last.y+last.height+24);
    }
    private void drawPage(Graphics2D g) {
        float fade=Math.min(1,transitionTicks/(float)TRANSITION_TICKS);
        g.setColor(new Color(2,8,19,(int)(155*fade)));g.fillRect(0,0,width,height);g.setComposite(AlphaComposite.SrcOver.derive(fade));
        UITheme.panel(g,panel.x,panel.y,panel.width,panel.height);g.setFont(PAGE_TITLE);g.setColor(IVORY);center(g,page.name(),panel.x+panel.width/2,panel.y+49);
        g.setColor(GOLD);g.drawLine(panel.x+28,panel.y+68,panel.x+panel.width-28,panel.y+68);g.setFont(TEXT_FONT);g.setColor(IVORY);
        if(page==Page.STORY) {
            int y=panel.y+109;for(String line:storyLines){g.drawString(line,panel.x+34,y);y+=27;}
            g.setColor(GOLD);g.drawString("Even when Christmas fades, a small light can bring it back.",panel.x+34,y+33);
        } else if(page==Page.SETTINGS) {
            g.drawString("Presentation and audio",panel.x+30,panel.y+103);UITheme.panel(g,motionToggle.x,motionToggle.y,motionToggle.width,motionToggle.height);
            if(hover==5||settingsSelection==0){g.setColor(GOLD);g.drawRoundRect(motionToggle.x,motionToggle.y,motionToggle.width,motionToggle.height,12,12);}
            g.setColor(IVORY);g.drawString("Menu motion: "+(reducedMotion?"REDUCED":"ON"),motionToggle.x+16,motionToggle.y+25);
            for(int i=0;i<4;i++) {
                Rectangle r=audioRows[i];UITheme.panel(g,r.x,r.y,r.width,r.height);
                if(hover==7+i||settingsSelection==i+1){g.setColor(GOLD);g.drawRoundRect(r.x,r.y,r.width,r.height,12,12);}
                g.setColor(IVORY);g.drawString(AUDIO_LABELS[i],r.x+16,r.y+25);
                if(i==3)g.drawString(audio.isMuted()?"MUTED":"SOUND ON",r.x+r.width/2,r.y+25);
                else {
                    int start=r.x+r.width/2,length=r.width/2-62,y=r.y+18;
                    g.setColor(new Color(38,53,73));g.fillRoundRect(start,y,length,5,5,5);
                    g.setColor(GOLD);g.fillRoundRect(start,y,length*volume(i)/100,5,5,5);
                    g.fillOval(start+length*volume(i)/100-4,y-2,9,9);g.setColor(IVORY);
                    g.drawString(volume(i)+"%",r.x+r.width-52,r.y+25);
                }
            }
            g.setFont(TEXT_FONT.deriveFont(12f));g.setColor(GOLD);
            g.drawString("↑ / ↓ Select   ← / → Adjust   Enter Confirm   Esc Back",panel.x+30,panel.y+355);
            g.drawString("Audio changes apply immediately and save locally.",panel.x+30,panel.y+379);
        } else {
            int y=panel.y+105;for(EndingCredits.Entry entry:EndingCredits.MENU_ENTRIES) {
                g.setColor(GOLD);g.drawString(entry.role(),panel.x+30,y);g.setColor(IVORY);g.drawString(entry.credit(),panel.x+panel.width/2,y);y+=33;
            }
        }
        UITheme.panel(g,back.x,back.y,back.width,back.height);g.setFont(TEXT_FONT);g.setColor(IVORY);
        if(hover==6||page!=Page.SETTINGS||settingsSelection==5){g.setColor(GOLD);g.drawRoundRect(back.x,back.y,back.width,back.height,12,12);}
        center(g,"BACK  [Esc]",back.x+back.width/2,back.y+25);
    }
    private static void center(Graphics2D g,String text,int x,int y) {g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    private static void ornament(Graphics2D g,int x,int y,int size) {
        g.drawLine(x-size,y,x+size,y);g.drawLine(x,y-size,x,y+size);
        g.drawPolygon(new int[]{x,x+size/2,x,x-size/2},new int[]{y-size/2,y,y+size/2,y},4);
    }
    private static BufferedImage[] icons() {
        BufferedImage[] images=new BufferedImage[5];
        for(int i=0;i<images.length;i++) {
            BufferedImage image=new BufferedImage(48,48,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
            try {
                g.setColor(IVORY);g.setStroke(new BasicStroke(2));
                if(i==0) {
                    g.fillRoundRect(11,22,24,12,5,5);g.fillOval(29,13,10,15);g.fillRect(10,24,5,3);
                    g.fillRect(14,32,3,11);g.fillRect(29,32,3,11);
                    g.drawLine(32,15,28,5);g.drawLine(29,9,24,7);g.drawLine(36,15,40,5);g.drawLine(39,8,44,6);
                } else if(i==1) {
                    g.drawRoundRect(7,10,17,28,3,3);g.drawRoundRect(24,10,17,28,3,3);
                    for(int y=17;y<34;y+=6){g.drawLine(11,y,19,y);g.drawLine(29,y,37,y);}
                } else if(i==2) {
                    g.drawOval(13,13,22,22);g.fillOval(19,19,10,10);
                    for(int angle=0;angle<8;angle++){double a=angle*Math.PI/4;g.drawLine(24+(int)(13*Math.cos(a)),24+(int)(13*Math.sin(a)),24+(int)(19*Math.cos(a)),24+(int)(19*Math.sin(a)));}
                } else if(i==3) {
                    g.fillPolygon(new int[]{24,13,18,8,15,4,44,33,40,30,35},new int[]{5,18,18,29,29,39,39,29,29,18,18},11);
                    g.fillRect(21,37,6,7);g.setColor(GOLD);g.fillRect(22,3,4,4);
                } else {
                    g.drawRect(10,7,24,34);g.drawLine(34,7,38,12);g.drawLine(38,12,38,43);g.fillRect(26,24,4,4);g.drawLine(15,7,15,41);
                }
            } finally {g.dispose();}images[i]=image;
        }
        return images;
    }
}
