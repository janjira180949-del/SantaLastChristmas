package entity;
import java.awt.*;import java.awt.image.BufferedImage;import boss.BossArtwork;import util.PixelArt;
/** Minor encounter-owned summon, deliberately not a boss or a story identity. */
public final class MiniFrostFang extends Enemy {
 public static final int HEALTH=80,DAMAGE=6,SIZE=26,PREPARATION=45,RENDER_SIZE=121;
 private int preparation=PREPARATION;
 private static final java.util.concurrent.CompletableFuture<BufferedImage[][]> ART=BossArtwork.prepareAsync().thenApply(ignored->prepareArt());
 private static BufferedImage[][] prepareArt(){var art=new BufferedImage[4][8];
 String[] names={"idle","walk","claw_swipe","hurt"};for(int n=0;n<names.length;n++)for(int i=0;i<8;i++){var frame=BossArtwork.frame("fang",names[n],i);if(frame!=null)art[n][i]=PixelArt.scale(frame,RENDER_SIZE,RENDER_SIZE);}return art;}
 public MiniFrostFang(double x,double y){super(x,y,HEALTH,1.8,SIZE);}
 public int getPreparationFrames(){return preparation;}
 @Override public void update(Santa santa,Iterable<? extends java.awt.geom.Rectangle2D> solids){if(preparation>0){preparation--;return;}super.update(santa,solids);}
 @Override public boolean tryAttackSanta(Santa santa){return preparation==0&&super.tryAttackSanta(santa);}
 @Override protected int getAttackDamage(){return DAMAGE;}@Override protected int getWarningDuration(){return 36;}@Override protected int getAttackCooldown(){return 70;}
 @Override public double getHealthBarY(){return getY()-46;}
 @Override public void drawAttackWarning(Graphics2D g){if(isWarning())for(int i=0;i<12;i++){double a=i*Math.PI/6;BossArtwork.effect(g,"warning",i,getX()+13+Math.cos(a)*43,getY()+13+Math.sin(a)*43,8,8,a);}}
 @Override public void draw(Graphics2D original){int n=getHitFlashFrames()>0?3:isWarning()?2:getVisualState()==VisualState.MOVING?1:0;var art=ART.getNow(null);if(art==null)return;var im=art[n][getMovementAnimationFrame()*2];if(im==null)return;var g=(Graphics2D)original.create();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);int x=(int)getX()+13-RENDER_SIZE/2,y=(int)getY()+26-RENDER_SIZE*240/256;if(getMovementDirection().equals("right"))g.drawImage(im,x+RENDER_SIZE,y,-RENDER_SIZE,RENDER_SIZE,null);else g.drawImage(im,x,y,null);game.Phase3Vfx.smoke(g,getX()+13,getY()+26,60+PREPARATION-preparation,.65,false);}finally{g.dispose();}}
}
