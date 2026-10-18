package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import util.ImageAssets;

/** Cached expression artwork with a safe neutral/placeholder fallback. */
public final class Portrait {

    public enum Kind {
        TOY,
        SANTA,
        ELIAS,
        EATER, MARTHA, OLIVER, NOAH, IRIS, CLARA, BEN
    }

    public enum Expression { NEUTRAL, WORRIED, SERIOUS, HAPPY, THINKING, CONCERNED }

    private static final Map<Kind, Map<Expression, BufferedImage>> IMAGES = loadImages();
    private static final BufferedImage EATER_IMAGE = ImageAssets.load("assets/characters/boss/christmas_eater/phase1/christmas_eater_p1_idle_01.png");
    private final Kind kind;
    private final Expression expression;

    private static Map<Kind, Map<Expression, BufferedImage>> loadImages() {
        Map<Kind, Map<Expression, BufferedImage>> images = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            if (kind.ordinal() > Kind.EATER.ordinal()) continue;
            if (kind == Kind.EATER) continue; // A single cinematic remnant image, not expression artwork.
            if (kind == Kind.ELIAS) {
                Map<Expression,BufferedImage> expressions=new EnumMap<>(Expression.class);
                // One supplied portrait; existing dialogue expressions use the neutral fallback.
                expressions.put(Expression.NEUTRAL,ImageAssets.load("assets/characters/elias/portraits/elias_portrait_neutral.png"));
                images.put(kind,expressions);continue;
            }
            Expression[] available = switch (kind) {
                case SANTA -> new Expression[] {Expression.NEUTRAL, Expression.HAPPY,
                        Expression.CONCERNED, Expression.SERIOUS};
                default -> new Expression[] {Expression.NEUTRAL, Expression.WORRIED,
                        Expression.SERIOUS, Expression.HAPPY};
            };
            String character = kind.name().toLowerCase(Locale.ROOT);
            String folder = kind == Kind.SANTA ? "santa" : "children/" + character;
            Map<Expression, BufferedImage> expressions = new EnumMap<>(Expression.class);
            for (Expression expression : available) {
                expressions.put(expression, ImageAssets.load("assets/characters/" + folder
                        + "/portraits/" + character + "_portrait_"
                        + expression.name().toLowerCase(Locale.ROOT) + ".png"));
            }
            images.put(kind, expressions);
        }
        return images;
    }

    public Portrait(Kind kind) {
        this(kind, Expression.NEUTRAL);
    }

    public Portrait(Kind kind, Expression expression) {
        if (kind == null || expression == null) {
            throw new IllegalArgumentException("Portrait kind cannot be null");
        }
        this.kind = kind;
        this.expression = expression;
    }

    public Kind getKind() {
        return kind;
    }

    public void draw(Graphics2D graphics, int x, int y, int size) {
        BufferedImage image = kind == Kind.EATER ? EATER_IMAGE : IMAGES.getOrDefault(kind, Map.of()).get(expression);
        if (kind.ordinal() > Kind.EATER.ordinal()) image = entity.VillageResident.portrait(kind.name());
        if (image == null) image = IMAGES.getOrDefault(kind, Map.of()).get(Expression.NEUTRAL);
        if (image != null) {
            Graphics2D visual = (Graphics2D) graphics.create();
            visual.setColor(new Color(12, 19, 30));
            visual.fillRoundRect(x, y, size, size, 14, 14);
            int inset = 4;
            double scale = (size - inset * 2.0) / Math.max(image.getWidth(), image.getHeight());
            int width = (int) Math.round(image.getWidth() * scale);
            int height = (int) Math.round(image.getHeight() * scale);
            visual.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            visual.drawImage(image, x + (size - width) / 2, y + (size - height) / 2,
                    width, height, null);
            visual.setColor(new Color(205, 184, 137));
            visual.drawRoundRect(x, y, size, size, 14, 14);
            visual.dispose();
            return;
        }
        drawFallback(graphics, x, y, size);
    }

    private void drawFallback(Graphics2D graphics, int x, int y, int size) {
        Color coat = switch (kind) {
            case TOY -> new Color(63, 137, 183);
            case SANTA -> new Color(182, 48, 54);
            case ELIAS -> new Color(72, 87, 99);
            case EATER -> new Color(115, 101, 125);
            default -> new Color(95, 121, 110);
        };
    graphics.setColor(new Color(12, 19, 30));
    graphics.fillRoundRect(x, y, size, size, 14, 14);
    graphics.setColor(new Color(205, 184, 137));
    graphics.drawRoundRect(x, y, size, size, 14, 14);

    int headSize = size / 2;
    int headX = x + (size - headSize) / 2;
    int headY = y + size / 7;
    graphics.setColor(new Color(243, 205, 165));
    graphics.fillOval(headX, headY, headSize, headSize);
    graphics.setColor(new Color(45, 40, 42));
    graphics.fillOval(headX + headSize / 3, headY + headSize / 3,
        Math.max(2, size / 24), Math.max(2, size / 24));
    graphics.fillOval(headX + headSize * 2 / 3, headY + headSize / 3,
        Math.max(2, size / 24), Math.max(2, size / 24));

    graphics.setColor(coat);
    graphics.fillRoundRect(x + size / 8, y + size * 3 / 5,
        size * 3 / 4, size / 3, 12, 12);

    if (kind == Kind.SANTA) {
        graphics.setColor(new Color(210, 55, 57));
        graphics.fillPolygon(new int[] {headX, headX + headSize / 2,
            headX + headSize}, new int[] {headY + 7, headY - size / 7,
            headY + 7}, 3);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(headX + headSize / 2 - size / 20,
            headY - size / 7 - size / 24, size / 10, size / 10);
    }

    graphics.setColor(new Color(255, 232, 174));
    graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, size / 6f));
    String initial = switch (kind) {
        case TOY -> "T";
        case SANTA -> "S";
        case ELIAS -> "E";
        case EATER -> "E";
        default -> kind.name().substring(0,1);
    };
    graphics.drawString(initial, x + size - size / 4, y + size - size / 10);
    }
}
