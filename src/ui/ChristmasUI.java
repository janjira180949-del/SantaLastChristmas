package ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import util.ImageAssets;

/** Illustrated Phase 2 UI only. Original Iris parchment and portraits are independent. */
public final class ChristmasUI {
    public static final Color INK=new Color(61,38,28), RED=new Color(121,33,28);
    private record Skin(BufferedImage image,int side,int top,int bottom,double scale) { }
    private static final Map<String,Skin> SKINS=new HashMap<>();
    private static final BufferedImage WAX=smallWax();
    private static final BufferedImage[] ICONS=new BufferedImage[5];
    private static final Map<String,BufferedImage> CACHE=new LinkedHashMap<>(64,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,BufferedImage> e){return size()>64;}
    };
    static {
        add("parchment",200,100,70,.25);add("notice",245,75,65,.22);add("tracker",160,85,70,.24);
        for(String color:new String[]{"red","green","wood"})for(String suffix:new String[]{"","_selected"})add("button_"+color+suffix,210,105,105,.14);
        String[] names={"attack","dash","bell","star","gift"};
        for(int i=0;i<names.length;i++){
            var source=load("icon_"+names[i]);var out=new BufferedImage(48,48,BufferedImage.TYPE_INT_ARGB);var g=out.createGraphics();
            nearest(g);double s=Math.min(46.0/source.getWidth(),46.0/source.getHeight());int w=(int)Math.round(source.getWidth()*s),h=(int)Math.round(source.getHeight()*s);
            g.drawImage(source,(48-w)/2,(48-h)/2,w,h,null);g.dispose();ICONS[i]=out;
        }
    }
    private ChristmasUI() { }
    public static void prepare(){ /* static initialization, called before painting */ }
    private static BufferedImage load(String n){var image=ImageAssets.load("assets/ui/phase2/"+n+".png");if(image==null)throw new IllegalStateException("Missing Phase 2 UI artwork: "+n);return image;}
    private static void add(String n,int side,int top,int bottom,double scale){SKINS.put(n,new Skin(load(n),side,top,bottom,scale));}
    public static void nearest(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);}
    public static BufferedImage icon(int index){return ICONS[index];}
    private static BufferedImage smallWax(){var out=new BufferedImage(40,40,BufferedImage.TYPE_INT_ARGB);var g=out.createGraphics();nearest(g);g.drawImage(load("ornament_wax"),0,0,40,40,null);g.dispose();return out;}
    public static void wax(Graphics2D g,int x,int y){g.drawImage(WAX,x,y,null);}
    public static void frame(Graphics2D graphics,String name,Rectangle r){var g=(Graphics2D)graphics.create();nearest(g);g.drawImage(frameImage(name,r.width,r.height),r.x,r.y,null);g.dispose();}
    public static synchronized BufferedImage frameImage(String name,int width,int height){
        width=Math.max(1,width);height=Math.max(1,height);String key=name+":"+width+":"+height;var existing=CACHE.get(key);if(existing!=null)return existing;
        var skin=SKINS.get(name);if(skin==null)throw new IllegalArgumentException(name);
        var source=skin.image;int sw=source.getWidth(),sh=source.getHeight();int side=skin.side,top=skin.top,bottom=skin.bottom;
        // Only edge strips/centers stretch. Ornamented corners keep a uniform scale.
        double cornerScale=Math.min(skin.scale,Math.min((width-2)/(2.0*side),(height-2)/(double)(top+bottom)));
        int dx=(int)Math.round(side*cornerScale),dt=(int)Math.round(top*cornerScale),db=(int)Math.round(bottom*cornerScale);
        int[] sx={0,side,sw-side,sw},sy={0,top,sh-bottom,sh},xs={0,dx,width-dx,width},ys={0,dt,height-db,height};
        var out=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=out.createGraphics();nearest(g);
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)g.drawImage(source,xs[x],ys[y],xs[x+1],ys[y+1],sx[x],sy[y],sx[x+1],sy[y+1],null);
        g.dispose();CACHE.put(key,out);return out;
    }
    /** Letter decoration only; leaves the approved parchment foundation visible. */
    public static void letterBorder(Graphics2D graphics,Rectangle r){
        var g=(Graphics2D)graphics.create();var clip=new java.awt.geom.Area(r);clip.subtract(new java.awt.geom.Area(new Rectangle(r.x+48,r.y+27,r.width-96,r.height-48)));g.clip(clip);frame(g,"parchment",r);g.dispose();
    }
    public static void button(Graphics2D g,String text,Rectangle r,String color,boolean selected,boolean enabled){
        Graphics2D art=(Graphics2D)g.create();if(!enabled)art.setComposite(AlphaComposite.SrcOver.derive(.55f));
        frame(art,"button_"+color+(selected?"_selected":""),r);art.setColor(new Color(255,240,205));
        art.setFont(new Font("Dialog",Font.BOLD,r.height<30?10:13));center(art,text,r.x+r.width/2,r.y+(r.height-art.getFontMetrics().getHeight())/2+art.getFontMetrics().getAscent());art.dispose();
    }
    public static void center(Graphics2D g,String text,int x,int y){g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    public static java.util.List<String> wrap(FontMetrics fm,String text,int width){
        var out=new ArrayList<String>();for(String paragraph:text.split("\\R",-1)){
            if(paragraph.isBlank()){out.add("");continue;}String line="";
            for(String word:paragraph.split("\\s+")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&fm.stringWidth(next)>width){out.add(line);line=word;}else line=next;}out.add(line);
        }return out;
    }
    public static Point directionPosition(int px,int py,Rectangle quest,int width,int height,Rectangle actor){
        Rectangle skill=AbilityHUD.stripBounds(width,height);
        if(px<quest.getMaxX()+16&&py>quest.y-40)py=Math.max(150,quest.y-40);
        if(new Rectangle(px-84,py-12,168,44).intersects(skill))py=skill.y-44;
        Rectangle marker=new Rectangle(px-84,py-12,168,44);
        if(marker.intersects(actor)){
            int right=Math.min(width-90,actor.x+actor.width+100),left=Math.max(90,actor.x-100);
            for(int candidate:new int[]{right,left}){var r=new Rectangle(candidate-84,py-12,168,44);if(!r.intersects(actor)&&!r.intersects(quest)&&!r.intersects(skill)){px=candidate;break;}}
        }
        return new Point(px,py);
    }
    public static void sleighPrompt(Graphics2D g,entity.Santa santa,game.Camera camera,int width,int height){
        int w=180,x=Math.max(12,Math.min(width-w-12,santa.getX()+16-camera.getX()-w/2));
        int y=Math.max(118,Math.min(height-225,(int)santa.getBounds().getMaxY()-camera.getY()+12));
        if(x+w>width-246&&y<292)y=292;
        button(g,"[E] Return Home",new Rectangle(x,y,w,32),"wood",false,true);
    }
    public static boolean suppressedReturnBanner(String message){return "Christmas returns. The sleigh is waiting in the plaza. [E] Return Home.".equals(message);}
    public static Rectangle notificationBounds(Graphics2D g,String message,int width,int height,boolean dialogue){
        int w=Math.min(530,width-330);int h=40+wrap(g.getFontMetrics(),message,w-110).size()*18;
        // One authoritative notice owner; no extra queue, timer, or competing banner.
        return new Rectangle(Math.max(306,(width-w)/2),(dialogue?height-214:height-128)-h-10,w,h);
    }
    public static void notification(Graphics2D graphics,String message,int width,int height,boolean dialogue){notification(graphics,message,width,height,dialogue,null);}
    public static Rectangle safeNotificationBounds(Graphics2D g,String message,int width,int height,boolean dialogue,Rectangle actor){
        var r=notificationBounds(g,message,width,height,dialogue);
        if(!dialogue&&actor!=null&&r.intersects(actor)){
            int w=Math.min(320,width-330),h=40+wrap(g.getFontMetrics(),message,w-110).size()*18;
            r=new Rectangle(width-w-14,height-138-h,w,h);
            if(r.intersects(actor))r=new Rectangle(Math.max(306,(width-r.width)/2),Math.max(118,actor.y-r.height-12),r.width,r.height);
            if(r.intersects(actor)&&actor.y+actor.height+12+r.height<height-128)r.y=actor.y+actor.height+12;
        }return r;
    }
    public static void notification(Graphics2D graphics,String message,int width,int height,boolean dialogue,Rectangle actor){
        if(suppressedReturnBanner(message))return;var g=(Graphics2D)graphics.create();g.setFont(new Font("Dialog",Font.PLAIN,13));
        var r=safeNotificationBounds(g,message,width,height,dialogue,actor);frame(g,"notice",r);g.setColor(INK);int y=r.y+28;
        for(String line:wrap(g.getFontMetrics(),message,r.width-110)){g.drawString(line,r.x+62,y);y+=18;}g.dispose();
    }
}
