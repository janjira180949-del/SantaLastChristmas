package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import java.io.File;
import util.LightBloom;

/** Shared immutable Village art, prepared outside paint; finite local restoration responses. */
public final class VillageRestorationVisuals {
    public static final WorldMap.Point TREE_ANCHOR=WorldMap.forArea(Area.Type.VILLAGE).point("landmarks/Christmas Tree");
    public static final WorldMap.Point TREE_APPROACH=new WorldMap.Point(TREE_ANCHOR.x(),TREE_ANCHOR.y()+16);
    public static final Rectangle TREE_BOUNDS=new Rectangle(TREE_ANCHOR.x()-112,TREE_ANCHOR.y()-276,224,276);
    public static final Rectangle HOUSE_BOUNDS=new Rectangle(2057,1484,270,260);
    public static final Rectangle MARKET_BOUNDS=new Rectangle(988,650,180,170);
    public static final int RESPONSE_DURATION=90;
    private record Art(BufferedImage terrain,BufferedImage coldHouse,BufferedImage warmHouse,BufferedImage[] trees,BufferedImage[] markets,VillageBuildingVisuals buildings,double[][] windowLights) { }
    private static final CompletableFuture<Art> ART=CompletableFuture.supplyAsync(VillageRestorationVisuals::load);
    private static int compositions;
    private FinalChapter.Restoration response;
    private int responseStart;
    public static CompletableFuture<?> prepareAsync(){return ART;}
    public static int compositionCount(){return compositions;}
    private static Art ready(CompletableFuture<Art> future){try{return future.getNow(null);}catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException ex){return null;}}
    private static BufferedImage read(String path){try{return ImageIO.read(new File(path));}catch(java.io.IOException|RuntimeException ex){return null;}}
    private static Art load(){
        BufferedImage source=read("assets/world/village_clean.png");
        BufferedImage mask=read("SantaLastChristmas_MapPackage/assets/environment/buildings/shop.png");
        BufferedImage lampMask=read("SantaLastChristmas_MapPackage/assets/environment/decorations/lamp.png");
        BufferedImage scaledMask=mask==null?null:scale(mask,270,260,false);
        BufferedImage scaledLamp=lampMask==null?null:scale(lampMask,54,130,false);
        VillageBuildingVisuals.Layout layout=VillageBuildingVisuals.loadLayout();
        BufferedImage original=source==null?null:VillageEnvironmentLayers.build(source,scaledMask,scaledLamp);
        BufferedImage terrain=source==null?null:VillageEnvironmentLayers.build(source,scaledMask,scaledLamp,layout);
        BufferedImage cold=read("assets/world/phase9_2/damaged_house.png"),warm=read("assets/world/final_chapter/repaired_house.png");
        BufferedImage trees=read("assets/world/phase9_2/tree_states.png"),markets=read("assets/world/phase9_2/market_states.png");
        BufferedImage[] treeStates=new BufferedImage[2],marketStates=new BufferedImage[3];
        for(int i=0;i<2;i++)if(trees!=null)treeStates[i]=scale(trees.getSubimage(i*trees.getWidth()/2,0,trees.getWidth()/2,trees.getHeight()),224,276,true);
        for(int i=0;i<3;i++)if(markets!=null){int a=i*markets.getWidth()/3,b=(i+1)*markets.getWidth()/3;marketStates[i]=scale(markets.getSubimage(a,0,b-a,markets.getHeight()),180,170,true);}
        BufferedImage coldHouse=cold==null?fallbackHouse(false):scale(cold,270,260,true),warmHouse=warm==null?fallbackHouse(true):scale(warm,270,260,true);
        for(int i=0;i<2;i++)if(treeStates[i]==null)treeStates[i]=fallbackTree(i==1);
        for(int i=0;i<3;i++)if(marketStates[i]==null)marketStates[i]=fallbackMarket(i);
        VillageBuildingVisuals buildings=original==null?null:VillageBuildingVisuals.prepare(layout,original,marketStates,terrain);
        if(buildings!=null){Graphics2D floor=terrain.createGraphics();buildings.draw(floor,null);floor.dispose();}
        compositions++;return new Art(terrain,coldHouse,warmHouse,treeStates,marketStates,buildings,scanWindowLights(source));
    }
    private static double[][] scanWindowLights(BufferedImage source){
        if(source==null)return new double[0][2];
        double[][] anchors=new double[48][2];int count=0;
        for(int y=0;y<source.getHeight()&&count<48;y+=96)for(int x=0;x<source.getWidth()&&count<48;x+=96){
            int pixels=0,sumX=0,sumY=0;
            for(int py=y;py<Math.min(y+96,source.getHeight());py+=3)for(int px=x;px<Math.min(x+96,source.getWidth());px+=3){
                if(px>=1050&&px<1540&&py>=770&&py<1240)continue;
                int rgb=source.getRGB(px,py),r=(rgb>>>16)&255,green=(rgb>>>8)&255,b=rgb&255;
                if(r>175&&green>85&&green<205&&r>green*1.2&&b<75){pixels++;sumX+=px;sumY+=py;}
            }
            if(pixels>=9){
                double px=sumX/(double)pixels,py=sumY/(double)pixels;
                if(VillageBuildingVisuals.RELOCATED_HOUSE_SOURCE.contains(px,py)){px+=326;py+=200;}
                anchors[count][0]=px;anchors[count++][1]=py;
            }
        }return java.util.Arrays.copyOf(anchors,count);
    }
    public static double[][] windowLights(){Art art=ready(ART);return art==null?null:art.windowLights;}
    public static VillageBuildingVisuals buildingVisuals(){Art art=ready(ART);return art==null?null:art.buildings;}
    /** Exception-only, prepared once: landmarks remain visible when optional files are absent. */
    private static BufferedImage fallbackHouse(boolean warm){
        var image=new BufferedImage(270,260,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setColor(new Color(79,66,66));g.fillRoundRect(26,92,218,160,8,8);g.setColor(new Color(51,61,78));g.fillPolygon(new int[]{5,133,264},new int[]{100,12,100},3);
        g.setColor(new Color(186,205,220));g.setStroke(new BasicStroke(10));g.drawLine(8,95,132,17);g.drawLine(132,17,260,95);
        g.setColor(warm?new Color(240,186,89):new Color(36,48,71));for(int x:new int[]{48,176})g.fillRect(x,132,42,57);
        g.setColor(new Color(45,35,32));g.fillRect(109,164,47,88);g.dispose();return image;
    }
    private static BufferedImage fallbackTree(boolean warm){
        var image=new BufferedImage(224,276,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setColor(new Color(66,46,37));g.fillRect(102,222,20,45);
        for(int i=0;i<5;i++){int y=25+i*40,half=25+i*18;g.setColor(new Color(28,60+i*3,55));g.fillPolygon(new int[]{112-half,112,112+half},new int[]{y+67,y,y+67},3);g.setColor(new Color(191,211,222));g.fillPolygon(new int[]{112-half+8,112,112+half-8},new int[]{y+43,y,y+43},3);}
        if(warm){g.setColor(new Color(246,196,88));for(int i=0;i<12;i++)g.fillOval(54+(i*37)%112,85+(i*43)%155,7,7);g.fillOval(104,12,16,16);}
        g.dispose();return image;
    }
    private static BufferedImage fallbackMarket(int state){
        var image=new BufferedImage(180,170,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setColor(new Color(82,59,49));g.fillRect(14,43,8,115);g.fillRect(158,43,8,115);g.fillRect(19,114,144,44);
        for(int i=0;i<8;i++){g.setColor(i%2==0?new Color(126,63,65):new Color(186,187,173));g.fillRect(8+i*20,30,20,40);}
        g.setColor(new Color(203,218,229));g.fillRoundRect(8,25,160,9,8,8);
        if(state>0){g.setColor(new Color(247,185,75));g.fillOval(26,72,14,20);g.fillOval(141,72,14,20);}
        if(state==2){for(int i=0;i<3;i++){g.setColor(new Color(156,57+i*24,51));g.fillRect(38+i*35,91,27,24);g.setColor(new Color(233,188,94));g.fillRect(49+i*35,91,4,24);}}
        g.dispose();return image;
    }
    public static boolean replacedLamp(double x,double y){
        return (x>=885&&x<939&&y>=814&&y<944)||(x>=1685&&x<1739&&y>=814&&y<944)||(x>=1269&&x<1323&&y>=1422&&y<1552);
    }
    private static BufferedImage scale(BufferedImage source,int w,int h,boolean trim){
        int l=0,t=0,r=source.getWidth()-1,b=source.getHeight()-1;
        if(trim){l=source.getWidth();t=source.getHeight();r=b=-1;for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)>40){l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}if(r<l)return null;}
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double ratio=Math.min(w/(double)(r-l+1),h/(double)(b-t+1));int dw=(int)Math.round((r-l+1)*ratio),dh=(int)Math.round((b-t+1)*ratio);
        g.drawImage(source,(w-dw)/2,h-dh,(w+dw)/2,h,l,t,r+1,b+1,null);g.dispose();return result;
    }
    public static void drawTerrain(Graphics2D g){
        drawTerrain(g,null);
    }
    public static void drawTerrain(Graphics2D g,FinalChapter chapter){
        Art art=ready(ART);if(art!=null&&art.terrain!=null){g.drawImage(art.terrain,0,0,null);if(art.buildings!=null)art.buildings.drawChanges(g,chapter);return;}
        g.setColor(new Color(67,93,122));g.fillRect(0,0,2560,1920);
        g.setColor(new Color(47,48,58));for(var solid:WorldMap.forArea(Area.Type.VILLAGE).solids())g.fill(solid);
    }
    public static void drawHouse(Graphics2D g,FinalChapter chapter){Art art=ready(ART);BufferedImage image=art==null?null:chapter.restored(FinalChapter.Restoration.HOUSE)?art.warmHouse:art.coldHouse;if(image!=null)g.drawImage(image,HOUSE_BOUNDS.x,HOUSE_BOUNDS.y,null);}
    public static void drawTree(Graphics2D g,FinalChapter chapter){
        Art art=ready(ART);BufferedImage image=art==null?null:art.trees[chapter.restored(FinalChapter.Restoration.TREE)?1:0];
        if(image!=null)g.drawImage(image,TREE_BOUNDS.x,TREE_BOUNDS.y,null);
    }
    public static int marketState(FinalChapter chapter){return chapter.isComplete()?2:chapter.restored(FinalChapter.Restoration.STREETLIGHTS)?1:0;}
    public static void drawMarket(Graphics2D g,FinalChapter chapter){Art art=ready(ART);if(art!=null&&art.markets[marketState(chapter)]!=null)g.drawImage(art.markets[marketState(chapter)],MARKET_BOUNDS.x,MARKET_BOUNDS.y,null);}
    public void respond(FinalChapter.Restoration objective,int tick){response=objective;responseStart=tick;}
    public void clearResponse(){response=null;}
    public boolean responseActive(int tick){if(response!=null&&(tick<responseStart||tick-responseStart>=RESPONSE_DURATION))clearResponse();return response!=null;}
    public void drawResponse(Graphics2D g,int tick){
        if(!responseActive(tick))return;var p=response==FinalChapter.Restoration.TREE?TREE_ANCHOR:response.approach;
        double f=(tick-responseStart)/(double)RESPONSE_DURATION,strength=Math.sin(Math.PI*f)*.18;
        LightBloom.draw(g,p.x(),p.y()-25,110,strength,LightBloom.GOLD);
        g.setColor(new Color(255,218,125,(int)(180*(1-f))));for(int i=0;i<12;i++){double a=i*Math.PI/6,r=14+f*55;g.fillOval(p.x()+(int)(Math.cos(a)*r),p.y()-25+(int)(Math.sin(a)*r*.65),3,3);}
    }
    public static void drawLights(Graphics2D g,FinalChapter chapter,int ticks){
        double pulse=.94+.06*Math.sin(ticks/64.0);
        if(chapter.restored(FinalChapter.Restoration.HOUSE)){LightBloom.draw(g,2195,1638,125,.17*pulse,LightBloom.GOLD);LightBloom.draw(g,2180,1560,58,.12,LightBloom.GOLD);}
        if(chapter.restored(FinalChapter.Restoration.TREE)){LightBloom.draw(g,TREE_ANCHOR.x(),TREE_ANCHOR.y()-30,145,.17*pulse,LightBloom.GOLD);LightBloom.draw(g,TREE_ANCHOR.x(),TREE_BOUNDS.y+18,35,.11,LightBloom.GOLD);}
        if(marketState(chapter)>0)LightBloom.draw(g,1078,768,110,(marketState(chapter)==2?.14:.07)*pulse,LightBloom.GOLD);
        if(chapter.isComplete())LightBloom.draw(g,1296,976,235,.09,LightBloom.GOLD);
        if(VillageBuildingVisuals.helpStage(chapter)>0)LightBloom.draw(g,1448,755,35,(chapter.isComplete()?.12:.05)*pulse,LightBloom.GOLD);
    }
}
