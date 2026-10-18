package ui;

import game.Area;
import java.awt.*;
import java.util.Locale;

/** Screen-space area introduction. Its clock advances only during live gameplay. */
public final class AreaTitleOverlay {
    public static final int DURATION_FRAMES=180;
    private Area.Type area;
    private int age=DURATION_FRAMES;
    private static final RadialGradientPaint BACKDROP=new RadialGradientPaint(0,0,290,new float[]{0,.65f,1},new Color[]{
        new Color(6,12,25,150),new Color(6,12,25,85),new Color(6,12,25,0)});

    public void show(Area.Type area) { this.area=area;age=0; }
    public void update() { if(isActive())age++; }
    public boolean isActive() { return area!=null&&age<DURATION_FRAMES; }
    public float getOpacity() {
        if(!isActive())return 0;
        return Math.min(1f,Math.min(age/30f,(DURATION_FRAMES-age)/45f));
    }
    public String getTitle() {
        if(area==null)return "";
        return switch(area) {
            case SANTA_BASE -> "Santa's Base";
            case VILLAGE -> "The Last Village";
            case FOREST -> "Frozen Forest";
            case CHRISTMAS_TOWN -> "Christmas Town";
        };
    }
    public String getSubtitle() {
        if(area==null)return "";
        return switch(area) {
            case SANTA_BASE -> "The Last Workshop";
            case VILLAGE -> "A Place That Still Holds Hope";
            case FOREST -> "Where the Black Frost Spreads";
            case CHRISTMAS_TOWN -> "The Heart of the Last Christmas";
        };
    }
    public void draw(Graphics2D graphics,int width) {
        if(!isActive()||getOpacity()==0)return;
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver.derive(getOpacity()));
            int cx=width/2;
            // A soft backdrop keeps the authored scene visible behind the title.
            Graphics2D backdrop=(Graphics2D)g.create();
            backdrop.translate(cx,90);backdrop.scale(1,.24);
            backdrop.setPaint(BACKDROP);
            backdrop.fillOval(-290,-290,580,580);backdrop.dispose();
            String title=getTitle().toUpperCase(Locale.ROOT);
            int fontSize=width<1000?30:38;
            g.setFont(new Font("Serif",Font.BOLD,fontSize));
            int x=cx-g.getFontMetrics().stringWidth(title)/2;
            g.setColor(new Color(4,9,19,235));g.drawString(title,x+2,87);
            g.setColor(UITheme.IVORY);g.drawString(title,x,85);
            g.setFont(new Font("Serif",Font.ITALIC,16));
            String subtitle=getSubtitle();x=cx-g.getFontMetrics().stringWidth(subtitle)/2;
            g.setColor(new Color(4,9,19));g.drawString(subtitle,x+1,111);
            g.setColor(new Color(231,202,149));g.drawString(subtitle,x,110);
            g.setStroke(new BasicStroke(1));g.setColor(new Color(219,184,119,170));
            g.drawLine(cx-158,126,cx-18,126);g.drawLine(cx+18,126,cx+158,126);
            for(int i=0;i<3;i++) {
                double a=i*Math.PI/3;int dx=(int)(Math.cos(a)*6),dy=(int)(Math.sin(a)*6);
                g.drawLine(cx-dx,126-dy,cx+dx,126+dy);
            }
        } finally { g.dispose(); }
    }
}
