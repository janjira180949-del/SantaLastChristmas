package ui;

import game.VillageActivities;
import game.VillageActivities.Site;
import game.FinalChapter;
import java.awt.*;
import java.awt.image.BufferedImage;

/** The existing activity modal: collection, tracking, selection, confirmation and receipt. */
public final class VillageActivityUI {
    public enum Mode { NONE, RESTORATION, DELIVERY, CONFIRM, DELIVERED, STORAGE, SLEIGH, LETTERS }
    private static final int WIDTH=900,HEIGHT=520;
    private static final Font TITLE=new Font("Serif",Font.BOLD,27),BODY=new Font("Dialog",Font.PLAIN,16),SMALL=new Font("Dialog",Font.BOLD,14);
    private static final Color INK=new Color(57,40,34),RED=new Color(133,44,34);
    private static final BufferedImage PAPER=paper();
    private Mode mode=Mode.NONE;
    private Site recipient;
    private int selected;
    private String message="";
    private java.util.List<FinalChapter.Letter> letters=java.util.List.of();
    private int letterIndex;
    private boolean returnRequested,finishRequested;
    public Mode mode(){return mode;}
    public boolean isOpen(){return mode!=Mode.NONE;}
    public int selected(){return selected;}
    public void open(Mode next,Site origin,VillageActivities state){mode=next;recipient=origin;message="";returnRequested=finishRequested=false;if(mode==Mode.DELIVERY)selected=0;if(mode==Mode.SLEIGH)selected=1;if(mode==Mode.LETTERS){letterIndex=0;letters=FinalChapter.gratitudeLetters(state);}}
    public void close(){mode=Mode.NONE;recipient=null;message="";}
    public void feedback(String value){message=value;}
    public void update(VillageActivities state){ }
    public int letterIndex(){return letterIndex;}
    public java.util.List<FinalChapter.Letter> letters(){return letters;}
    public boolean pollReturnRequested(){boolean value=returnRequested;returnRequested=false;return value;}
    public boolean pollFinishRequested(){boolean value=finishRequested;finishRequested=false;return value;}
    private void nextLetter(){if(letterIndex<letters.size()-1)letterIndex++;else {finishRequested=true;close();}}
    public void key(int key,VillageActivities state){
        if(mode==Mode.LETTERS){if(key==java.awt.event.KeyEvent.VK_LEFT)letterIndex=Math.max(0,letterIndex-1);if(key==java.awt.event.KeyEvent.VK_RIGHT)letterIndex=Math.min(letters.size()-1,letterIndex+1);if(key==java.awt.event.KeyEvent.VK_E||key==java.awt.event.KeyEvent.VK_ENTER)nextLetter();}
        else if(mode==Mode.SLEIGH){if(key==java.awt.event.KeyEvent.VK_LEFT)selected=0;if(key==java.awt.event.KeyEvent.VK_RIGHT)selected=1;if(key==java.awt.event.KeyEvent.VK_ENTER||key==java.awt.event.KeyEvent.VK_E){if(selected==0)returnRequested=true;close();}}
    }
    public static double scale(int w,int h){return Math.min(1,Math.min(Math.max(1,w-32)/(double)WIDTH,Math.max(1,h-32)/(double)HEIGHT));}
    public static Rectangle windowBounds(int w,int h){double s=scale(w,h);int width=(int)(WIDTH*s),height=(int)(HEIGHT*s);return new Rectangle((w-width)/2,(h-height)/2,width,height);}
    public Point screenPoint(int x,int y,int w,int h){double s=scale(w,h);Rectangle box=windowBounds(w,h);return new Point(box.x+(int)Math.round(x*s),box.y+(int)Math.round(y*s));}
    public void click(int sx,int sy,int w,int h,VillageActivities state){
        if(!isOpen())return;Rectangle window=windowBounds(w,h);if(!window.contains(sx,sy))return;
        double s=scale(w,h);int x=(int)((sx-window.x)/s),y=(int)((sy-window.y)/s);
        if(new Rectangle(746,463,120,38).contains(x,y)||new Rectangle(858,8,34,34).contains(x,y)){close();return;}
        if(new Rectangle(34,463,120,38).contains(x,y)){
            if(mode==Mode.CONFIRM)mode=Mode.DELIVERY;
            message="";return;
        }
        switch(mode){
            case SLEIGH->{if(new Rectangle(34,408,300,38).contains(x,y)){returnRequested=true;close();}else if(new Rectangle(484,408,380,38).contains(x,y))close();}
            case LETTERS->{if(new Rectangle(34,408,260,38).contains(x,y))letterIndex=Math.max(0,letterIndex-1);else if(new Rectangle(604,408,260,38).contains(x,y))nextLetter();}
            case DELIVERY->{for(int i=0;i<3;i++)if(new Rectangle(34,105+i*96,832,88).contains(x,y))selected=i;
                if(new Rectangle(604,408,260,38).contains(x,y)&&recipient!=null&&recipient.recipient()&&state.presentsCollected()&&!state.delivered(selected)){mode=Mode.CONFIRM;message="";}}
            case CONFIRM->{if(new Rectangle(604,408,260,38).contains(x,y))mode=state.deliver(selected,recipient)?Mode.DELIVERED:Mode.DELIVERY;}
            case DELIVERED->{if(new Rectangle(604,408,260,38).contains(x,y)){mode=Mode.DELIVERY;message="";}}
            case RESTORATION,STORAGE->{if(new Rectangle(604,408,260,38).contains(x,y))close();}
            default->{ }
        }
    }
    private static BufferedImage paper(){
        BufferedImage image=new BufferedImage(900,520,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setPaint(new GradientPaint(0,0,new Color(213,178,126),400,400,new Color(245,225,184)));g.fillRect(0,0,900,520);
        java.util.Random random=new java.util.Random(712);
        for(int i=0;i<12000;i++){g.setColor(new Color(95,59,33,random.nextInt(14)));int x=random.nextInt(900),y=random.nextInt(520);g.fillRect(x,y,2,1);}
        g.dispose();return image;
    }
    public static void parchment(Graphics2D g,int x,int y,int w,int h){
        g.drawImage(PAPER,x,y,w,h,null);g.setColor(new Color(84,54,37));g.setStroke(new BasicStroke(2));g.drawRoundRect(x+2,y+2,w-4,h-4,12,12);
        g.setColor(new Color(178,125,65));g.drawRoundRect(x+8,y+8,w-16,h-16,10,10);
        for(int dx:new int[]{x+14,x+w-14})for(int dy:new int[]{y+14,y+h-14}){g.setColor(RED);g.fillOval(dx-4,dy-4,8,8);g.setColor(new Color(63,91,62));g.drawLine(dx-8,dy+6,dx+8,dy-6);}
    }
    private int hoverX=-1,hoverY=-1;
    public void hover(int x,int y,int width,int height){Rectangle window=windowBounds(width,height);double s=scale(width,height);hoverX=(int)((x-window.x)/s);hoverY=(int)((y-window.y)/s);}
    private void button(Graphics2D g,String text,int x,int y,int width,boolean active){
        String color=text.equals("Return to Base")||text.equals("Finish Reading")?"red":text.startsWith("Stay")||text.contains("Letter")?"green":"wood";
        var r=new Rectangle(x,y,width,38);boolean selected=r.contains(hoverX,hoverY)||(mode==Mode.SLEIGH&&((x==34&&selected()==0)||(x==484&&selected()==1)));
        ChristmasUI.button(g,text,r,color,selected,active);
    }
    public static void present(Graphics2D g,int index,int x,int y,int size){
        BufferedImage art=game.VillageActivityVisuals.presentImage(index);
        if(art!=null){g.drawImage(art,x-4,y-10,size+8,size+12,null);return;}
        Color color=new Color(index==0?165:40,index==1?110:47,index==2?145:55);
        g.setColor(new Color(30,19,20,65));g.fillOval(x-4,y+size-6,size+8,14);
        g.setPaint(new GradientPaint(x,y,color.brighter(),x+size,y+size,color.darker()));g.fillRoundRect(x,y,size,size,5,5);
        g.setColor(new Color(248,199,97));g.fillRect(x+size/2-3,y-2,6,size+3);g.fillRect(x-2,y+size/3,size+4,6);
        g.setStroke(new BasicStroke(3));g.drawOval(x+size/2-18,y-14,18,15);g.drawOval(x+size/2,y-14,18,15);
        g.setColor(new Color(255,231,167));g.drawLine(x+4,y+4,x+size-5,y+4);g.setColor(new Color(75,42,30));g.drawRoundRect(x,y,size,size,5,5);
    }
    public void draw(Graphics2D original,int w,int h,VillageActivities state){
        if(!isOpen())return;Graphics2D g=(Graphics2D)original.create();g.setColor(new Color(3,8,17,190));g.fillRect(0,0,w,h);
        Rectangle box=windowBounds(w,h);g.translate(box.x,box.y);double s=scale(w,h);g.scale(s,s);
        parchment(g,0,0,WIDTH,HEIGHT);if(mode==Mode.LETTERS){ChristmasUI.letterBorder(g,new Rectangle(0,0,WIDTH,HEIGHT));ChristmasUI.wax(g,800,22);}g.setColor(RED);g.setFont(TITLE);
        UITheme.centered(g,switch(mode){case LETTERS->"Letters of Gratitude";case SLEIGH->"The Journey Home";case RESTORATION->recipient.restoration().label;case STORAGE->"Gift Storage";case CONFIRM->"A Present for "+recipient.label;case DELIVERED->"Delivered!";default->"Christmas Delivery";},450,49);
        g.setFont(BODY);g.setColor(INK);UITheme.centered(g,mode==Mode.SLEIGH?"Are you ready to return home?":mode==Mode.LETTERS?"A little warmth, sent from the people you helped.":mode==Mode.DELIVERY?(state.presentsCollected()?"Find each neighbor and press E to give their present.":"Gift Storage prepares the presents for the opening journey."):"A little kindness brings warmth home.",450,79);
        switch(mode){
            case SLEIGH->{g.drawImage(game.JourneyHomeVisuals.sleighImage(),210,142,480,230,null);button(g,"Return to Base",34,408,300,true);button(g,"Stay in Christmas Town",484,408,380,true);}
            case LETTERS->drawLetters(g);
            case RESTORATION->drawRequirements(g,state);
            case DELIVERY->drawCard(g,state);
            case CONFIRM,DELIVERED->drawRecipient(g);
            case STORAGE->{for(int i=0;i<3;i++)present(g,i,185+i*230,220,90);g.setFont(BODY);g.setColor(INK);UITheme.centered(g,"COLLECTED 3 / 3 - Oliver will help you find their homes.",450,370);button(g,"Continue the journey",604,408,260,true);}
            default->{ }
        }
        g.setFont(SMALL);g.setColor(INK);UITheme.centered(g,mode==Mode.LETTERS?(letterIndex==letters.size()-1?"[Left] Previous   [E / Enter] Finish   [ESC] Close":"[Left] Previous   [Right / E] Next   [ESC] Close"):mode==Mode.SLEIGH?"[Left / Right] Choose   [Enter] Confirm   [ESC] Stay":"Presents delivered "+state.deliveries()+" / 3   |   Village restored "+state.chapter.completed()+" / 4",mode==Mode.LETTERS||mode==Mode.SLEIGH?365:450,490);
        if(!message.isEmpty()&&mode!=Mode.DELIVERED){g.setFont(new Font("Dialog",Font.PLAIN,13));text(g,message,36,421,540,18);}
        if(mode==Mode.CONFIRM)button(g,"Cancel",34,463,120,true);
        button(g,"Close [ESC]",746,463,120,true);ChristmasUI.button(g,"X",new Rectangle(858,8,34,34),"red",new Rectangle(858,8,34,34).contains(hoverX,hoverY),true);g.dispose();
    }
    private void drawLetters(Graphics2D g){
        var letter=letters.get(letterIndex);boolean finale=letterIndex==letters.size()-1;
        if(finale){g.setColor(new Color(170,116,44));g.setStroke(new BasicStroke(2));g.drawRoundRect(22,94,856,297,12,12);g.setFont(new Font("Serif",Font.BOLD,27));UITheme.centered(g,"\u2605",170,177);g.setFont(new Font("Serif",Font.ITALIC,18));UITheme.centered(g,"With love",170,214);UITheme.centered(g,"from everyone",170,242);}
        else new Portrait(letter.portrait(),Portrait.Expression.HAPPY).draw(g,52,128,164);
        g.setFont(new Font("Serif",Font.BOLD,21));g.setColor(RED);g.drawString("From: "+letter.sender(),260,128);g.setFont(SMALL);g.drawString((letterIndex+1)+" / "+letters.size(),796,127);
        g.setColor(new Color(178,125,65));g.drawLine(260,143,850,143);g.setColor(INK);g.setFont(new Font("Serif",Font.PLAIN,18));text(g,letter.text(),260,174,574,23);
        button(g,"Previous Letter",34,408,260,letterIndex>0);button(g,finale?"Finish Reading":"Next Letter",604,408,260,true);
    }
    private void drawRequirements(Graphics2D g,VillageActivities state){
        var objective=recipient.restoration();
        if(objective.material!=null)g.drawImage(game.FinalChapterVisuals.materialImage(objective.material),104,164,112,124,null);
        else g.drawImage(game.VillageActivityVisuals.lanternImage(0,false),120,148,90,180,null);
        g.setFont(BODY);g.setColor(INK);text(g,state.chapter.requirement(objective),280,175,540,29);
        g.setFont(SMALL);g.drawString(state.chapter.isUnlocked()?"Return here and press E when ready.":"Help Toby and explore Frozen Forest for the materials.",280,325);
        g.drawString("Your materials stay with you until the full set is ready.",280,357);
        button(g,"Continue exploring",604,408,260,true);
    }
    private void drawCard(Graphics2D g,VillageActivities state){
        for(int i=0;i<3;i++){int y=105+i*96;g.setColor(new Color(125,86,47,i==selected?32:12));g.fillRoundRect(34,y,832,88,6,6);g.setColor(new Color(126,86,50));g.setStroke(new BasicStroke(i==selected?3:1));g.drawRoundRect(34,y,832,88,6,6);
            present(g,i,58,y+28,40);g.setFont(BODY);g.setColor(INK);text(g,VillageActivities.CLUES[i],114,y+33,560,24);g.setFont(SMALL);g.setColor(state.delivered(i)?RED:INK);g.drawString(state.delivered(i)?"DELIVERED":state.presentsCollected()?"READY":"AT BASE",742,y+47);if(state.delivered(i))g.drawRect(730,y+23,121,38);
        }
        boolean can=recipient!=null&&recipient.recipient()&&state.presentsCollected()&&!state.delivered(selected);button(g,can?"Select for "+recipient.label:state.presentsCollected()?"Visit a recipient":"Collect at Santa's Base",604,408,260,can);g.setFont(SMALL);g.setColor(INK);g.drawString("Deliveries are optional. Visit each neighbor and press E.",36,400);
    }
    private void drawRecipient(Graphics2D g){
        new Portrait(Portrait.Kind.valueOf(recipient.name())).draw(g,100,151,190);g.setColor(INK);g.setFont(TITLE);UITheme.centered(g,recipient.label,195,375);present(g,selected,488,232,86);g.setColor(INK);g.setFont(BODY);text(g,VillageActivities.CLUES[selected],360,155,454,24);
        if(mode==Mode.DELIVERED){g.setColor(RED);g.rotate(-.12,676,298);g.setStroke(new BasicStroke(3));g.drawRect(582,274,216,49);g.setFont(TITLE);g.drawString("DELIVERED",600,308);g.rotate(.12,676,298);g.setColor(INK);g.setFont(BODY);UITheme.centered(g,recipient.label+" smiles. A little kindness found its home.",570,376);}
        button(g,mode==Mode.CONFIRM?"Confirm delivery":"Delivery card",604,408,260,true);
    }
    private static void text(Graphics2D g,String text,int x,int y,int width,int lineHeight){
        for(String paragraph:text.split("\\n",-1)){
            StringBuilder line=new StringBuilder();for(String word:paragraph.split(" ")){String candidate=line.length()==0?word:line+" "+word;if(g.getFontMetrics().stringWidth(candidate)>width&&line.length()>0){g.drawString(line.toString(),x,y);y+=lineHeight;line.setLength(0);}if(line.length()>0)line.append(' ');line.append(word);}g.drawString(line.toString(),x,y);y+=lineHeight;
        }
    }
}
