package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Draws the active conversation as a modal Dark Christmas overlay. */
public final class DialogueBox {

    private static final Color PANEL = new Color(8, 14, 25, 235);
    private static final Color BORDER = new Color(204, 179, 131);
    private static final Color GOLD = new Color(255, 225, 155);
    private static final Color TEXT = new Color(239, 241, 239);
    private DialogueLine cachedLine;
    private Portrait cachedPortrait;
    private int cachedWidth;
    private int lastBoxHeight;
    private Font cachedFont;
    private List<String> cachedText;

    public void draw(Graphics2D graphics, Dialogue dialogue, int width, int height) {
        draw(graphics, dialogue, width, height, false);
    }

    /** Same presentation, without input prompts or page counter for timed cinema. */
    public void drawCinematic(Graphics2D graphics, Dialogue dialogue, int width, int height) {
        draw(graphics, dialogue, width, height, true);
    }

    private void draw(Graphics2D graphics, Dialogue dialogue, int width, int height, boolean cinematic) {
        if (dialogue == null || !dialogue.isActive()) {
            return;
        }
        DialogueLine line = dialogue.getCurrentLine();
        if(line==null)return;
        if(cachedLine!=line){cachedLine=line;cachedPortrait=new Portrait(line.getPortraitKind(),line.getExpression());cachedText=null;}
        int boxWidth = Math.min(760, width - 36);
        int boxHeight = cinematic ? 132 : 184;
        if(!cinematic) {
            graphics.setFont(new Font("Dialog",Font.PLAIN,16));
            int textWidth=boxWidth-18-112-22-20;
            int count=wrappedText(graphics,line.getText(),textWidth).size();
            boxHeight=Math.max(boxHeight,115+(count-1)*23);
        }
        lastBoxHeight=boxHeight;
        int x = (width - boxWidth) / 2;
        int y = height - boxHeight - 20;

        // Timed cinema must leave the restored world's light intact above the panel.
        if(!cinematic){graphics.setColor(new Color(0,0,0,85));graphics.fillRect(0,0,width,height);}
        boolean villager=true; // All speakers share the approved, unchanged Iris parchment.
        if(villager)VillageActivityUI.parchment(graphics,x,y,boxWidth,boxHeight);
        else UITheme.panel(graphics,x,y,boxWidth,boxHeight);

        int portraitSize = cinematic ? 80 : 112;
        int portraitX = x + 18;
        int portraitY = y + 22;
        cachedPortrait.draw(graphics,portraitX,portraitY,portraitSize);

        int textX = portraitX + portraitSize + 22;
        int textWidth = boxWidth - (textX - x) - 20;
        graphics.setColor(villager?new Color(109,51,35):GOLD);
        graphics.setFont(new Font("Serif",Font.BOLD,19));
        graphics.drawString(line.getSpeakerName(), textX, y + 42);

        graphics.setColor(villager?new Color(48,37,33):TEXT);
        graphics.setFont(new Font("Dialog",Font.PLAIN,16));
        drawWrappedText(graphics, line.getText(), textX, y + 75,
                textWidth, 23);

        if (!cinematic) {
        graphics.setColor(villager?new Color(109,51,35):GOLD);
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 13f));
        String nextText = dialogue.getCurrentLineNumber() < dialogue.getLineCount()
                ? "NEXT  ▶  [E]" : "CLOSE  [E]";
        int nextX = x + boxWidth - 112;
        graphics.drawString(nextText, nextX, y + boxHeight - 18);
        graphics.setColor(villager?new Color(70,53,39):new Color(193, 202, 211));
        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 11f));
        graphics.drawString(dialogue.getCurrentLineNumber() + " / "
                + dialogue.getLineCount(), x + 20, y + boxHeight - 18);
        }
    }

    private void drawWrappedText(Graphics2D graphics, String text,
            int x, int startY, int maxWidth, int lineHeight) {
        int y=startY;
        for(String line:wrappedText(graphics,text,maxWidth)){graphics.drawString(line,x,y);y+=lineHeight;}
    }
    private List<String> wrappedText(Graphics2D graphics,String text,int maxWidth) {
        if(cachedText!=null&&cachedWidth==maxWidth&&graphics.getFont().equals(cachedFont))return cachedText;
        FontMetrics metrics=graphics.getFontMetrics();List<String> lines=new ArrayList<>();
        for(String paragraph:text.split("\\n",-1)) {
            if(paragraph.isBlank()){lines.add("");continue;}
            StringBuilder currentLine=new StringBuilder();
            for(String word:paragraph.split("\\s+")) {
                String candidate=currentLine.length()==0?word:currentLine+" "+word;
                if(metrics.stringWidth(candidate)>maxWidth&&currentLine.length()>0) {
                    lines.add(currentLine.toString());currentLine.setLength(0);currentLine.append(word);
                } else {if(currentLine.length()>0)currentLine.append(' ');currentLine.append(word);}
            }
            if(currentLine.length()>0)lines.add(currentLine.toString());
        }
        cachedText=List.copyOf(lines);cachedWidth=maxWidth;cachedFont=graphics.getFont();return cachedText;
    }
}
