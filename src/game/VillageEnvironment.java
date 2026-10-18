package game;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import util.ImageAssets;

/** Art-only Village prop clusters. No collision or gameplay objects are created. */
public final class VillageEnvironment {
    public record Prop(String name,int x,int y,BufferedImage image){
        public Rectangle2D bounds(){return new Rectangle2D.Double(x,y-image.getHeight(),image.getWidth(),image.getHeight());}
        public void draw(Graphics2D g){g.drawImage(image,x,y-image.getHeight(),null);}
        public void draw(Graphics2D g,boolean obscure){
            Composite previous=g.getComposite();
            if(obscure&&(name.startsWith("pine")||name.equals("dead_tree")))g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.58f));
            draw(g);g.setComposite(previous);
        }
    }
    private static List<Prop> props;
    private static final Map<String,BufferedImage> SPRITES=new HashMap<>();
    private VillageEnvironment() { }
    private static BufferedImage sprite(String name,int percent){
        String key=name+percent;BufferedImage known=SPRITES.get(key);if(known!=null)return known;
        BufferedImage art=ImageAssets.load("assets/world/village_props/"+name+".png");if(art==null)throw new IllegalStateException("Missing reused prop "+name);
        if(percent!=100){BufferedImage small=new BufferedImage(art.getWidth()*percent/100,art.getHeight()*percent/100,2);var g=small.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(art,0,0,small.getWidth(),small.getHeight(),null);g.dispose();art=small;}
        SPRITES.put(key,art);return art;
    }
    public static synchronized List<Prop> props(){
        if(props!=null)return props;var map=WorldMap.forArea(Area.Type.VILLAGE);var source=ImageAssets.load("assets/world/village_clean.png");var road=PackedSnowPath.polish(source);boolean[] ground=ExistingSnowGround.groundMask(source,road);var mask=PackedSnowPath.createOverlay(source);List<Prop> placed=new ArrayList<>();Random rng=new Random(0x56494c4cL);
        // Deliberate pockets: houses/plaza, outer forest margins, and an abandoned corner.
        int[][] anchors={{810,620},{810,1410},{1670,500},{1770,1690},{160,610},{2390,510},{2350,1740},{740,1800},{170,1730},{180,1170},{1680,235},{2250,850}};
        for(int c=0;c<anchors.length;c++){
            boolean dark=c==3||c==5||c==8;String[] names=dark?new String[]{"dead_tree","snow_rock","winter_bush","pine_small","wooden_fence"}:new String[]{"pine_medium","winter_bush",c<4?"warm_lamp":"snow_rock","pine_small",c==0?"bench":c==1?"present":"snow_rock"};
            if(c==0)names=new String[]{"bench","pine_medium","winter_bush","warm_lamp","pine_small"};
            if(c==9)names=new String[]{"wooden_fence","pine_small","winter_bush","snow_rock"};
            for(int n=0;n<names.length;n++){
                int percent=names[n].equals("pine_small")?(c%3==0?72:100):100;BufferedImage art=sprite(names[n],percent);
                for(int trial=0;trial<60;trial++){
                    int x=anchors[c][0]-art.getWidth()/2+rng.nextInt(421)-210,y=anchors[c][1]+rng.nextInt(281)-140;
                    Rectangle2D box=new Rectangle2D.Double(x-14,y-art.getHeight()-14,art.getWidth()+28,art.getHeight()+28);
                    if(box.getMinX()<32||box.getMinY()<32||box.getMaxX()>map.width()-32||box.getMaxY()>map.height()-32)continue;
                    boolean safe=true;
                    for(var point:map.points().entrySet())if(point.getKey().startsWith("npc/")||point.getKey().startsWith("exit/")||point.getKey().startsWith("entry/")||point.getKey().equals("spawn")){
                        if(point.getKey().equals("exit/to_santa_base"))continue; // Return arch does not rearrange the approved decorative clusters.
                        // Keep approved decorative clusters unchanged when Toby moves along the road.
                        var p=point.getKey().equals("npc/Toby")?new WorldMap.Point(528,720):point.getValue();if(box.intersects(p.x()-135,p.y()-135,270,270))safe=false;
                    }
                    // Preserve approved prop clearances after retiring two NPC registrations.
                    if(box.intersects(2032-135,592-135,270,270)||box.intersects(2032-135,1392-135,270,270)
                            ||box.intersects(1296-135,208-135,270,270))safe=false;
                    if(box.intersects(1050,770,490,460))safe=false; // central tree/intersection breathing room
                    for(var p:placed)if(p.bounds().intersects(box))safe=false;
                    for(int yy=(int)box.getMinY();safe&&yy<box.getMaxY();yy+=4)for(int xx=(int)box.getMinX();xx<box.getMaxX();xx+=4)
                        if(!ground[yy*source.getWidth()+xx]||(mask.getRGB(xx,yy)>>>24)!=0){safe=false;break;}
                    if(!safe)continue;placed.add(new Prop(names[n],x,y,art));break;
                }
            }
        }
        props=List.copyOf(placed);return props;
    }
    /** Add props to the same foot-position painter queue as characters. */
    public record DrawRow(int footY,Runnable draw) { }
    public static void drawSorted(List<DrawRow> rows){rows.sort(Comparator.comparingInt(DrawRow::footY));for(var row:rows)row.draw.run();}
}
