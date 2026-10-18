import entity.*;import java.awt.image.BufferedImage;import java.awt.Graphics2D;import java.io.File;import javax.imageio.ImageIO;
public class EnemyArtworkTest { public static void main(String[]a)throws Exception{
 String[][] cfg={{"walker","walk"},{"runner","run"}};String[] d={"down","up","left","right"};int n=0;
 for(String[]c:cfg)for(String x:d)for(int f=1;f<=4;f++){File p=new File("assets/characters/enemies/"+c[0]+"/"+c[1]+"/"+c[0]+"_"+c[1]+"_"+x+"_0"+f+".png");if(!p.isFile()||ImageIO.read(p)==null)throw new AssertionError(p);n++;}
 BufferedImage im=new BufferedImage(300,200,BufferedImage.TYPE_INT_ARGB);Graphics2D g=im.createGraphics();new Walker(20,20).draw(g);new Runner(80,20).draw(g);g.dispose();
 System.out.println("PASS: "+n+" four-direction enemy movement frames and render smoke test");}}
