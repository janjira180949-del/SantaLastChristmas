package game;
import java.awt.*;import java.awt.image.BufferedImage;import util.ImageAssets;import util.PixelArt;
/** Cached original PNG effects. Simulation owners supply all ages; painting never advances them. */
public final class Phase3Vfx {
 private static final BufferedImage[] SMOKE=load("smoke",16,4,128,128),BEAM=load("beam",8,2,512,256);
 private Phase3Vfx(){}public static void prepare(){}
 private static BufferedImage[] load(String name,int count,int cols,int w,int h){var sheet=ImageAssets.load("assets/vfx/phase3/"+name+"_sheet.png");if(sheet==null)throw new IllegalStateException("Missing Phase3 "+name);var out=new BufferedImage[count];for(int i=0;i<count;i++)out[i]=sheet.getSubimage(i%cols*w,i/cols*h,w,h);return out;}
 public static void smoke(Graphics2D original,double x,double foot,int age,double scale,boolean foreground){
  if(age<0||age>=84)return;int index=age<60?Math.min(9,age/6):Math.min(15,10+(age-60)/4);var g=(Graphics2D)original.create();
  try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);float a=foreground?.25f:.8f;if(age>=60)a*=1-(age-60)/24f;g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0,a)));int size=(int)(96*scale);g.drawImage(SMOKE[index],(int)x-size/2,(int)foot-size+size*8/128,size,size,null);}finally{g.dispose();}
 }
 public static void beam(Graphics2D original,double x,double y,double dx,double dy,double length,int ticks,int warning,int active){
  int age=ticks-warning;var g=(Graphics2D)original.create();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.translate(x,y);g.rotate(Math.atan2(dy,dx));var im=BEAM[Math.floorMod(ticks/3,8)];
   if(age<0){g.setComposite(AlphaComposite.SrcOver.derive((float)(.25+.45*ticks/(double)warning)));g.drawImage(im,-18,-28,38,56,0,0,150,256,null);}
   else if(age<active){int front=(int)Math.min(length,(age+1)*64);g.drawImage(im,0,-48,front,96,null);}
   else if(age<active+12){g.setComposite(AlphaComposite.SrcOver.derive(.35f*(1-(age-active)/12f)));g.drawImage(im,0,-48,(int)length,96,null);}
  }finally{g.dispose();}
 }
}
