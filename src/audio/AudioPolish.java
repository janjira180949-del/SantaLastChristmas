package audio;

import static audio.AudioCatalog.*;

/** Presentation-only mix policy. Frame counts are audio samples, independent of gameplay timing. */
public final class AudioPolish {
    private AudioPolish() { }
    public static int transitionMillis(Music previous,Music next) {
        if(next==Music.MENU)return 1000;
        if(previous==Music.SLEIGH&&next==Music.HOME)return 100;
        if(previous==Music.FANG1||previous==Music.FANG2||previous==Music.FANG3||previous==Music.BOSS||previous==Music.TITAN2||previous==Music.TITAN3){
            if(next==Music.TREE||next==Music.FOREST_INVESTIGATION)return 750;
            return 500;
        }
        if(next==Music.FANG1||next==Music.FANG2||next==Music.FANG3||next==Music.TITAN2||next==Music.TITAN3)return 500;
        if(next==Music.ENDING)return 1750;
        if(next==Music.BOSS)return 750;
        if(previous==Music.MENU||next==Music.MENU||previous==null)return 1000;
        return 1250;
    }
    /** World pixels: full within 96, smooth falloff to 20% at 720 and beyond. */
    public static double distanceGain(double listenerX,double listenerY,double sourceX,double sourceY) {
        double distance=Math.hypot(sourceX-listenerX,sourceY-listenerY);
        if(!Double.isFinite(distance))return .20;
        double t=Math.max(0,Math.min(1,(distance-96)/624));
        return 1-.80*t*t*(3-2*t);
    }
    /** Small downward-only gain changes; unchanged samples, no pitch processing or random tests. */
    public static double variationGain(Effect effect,int sequence) {
        return switch(effect) {
            case SANTA_ATTACK,ENEMY_HIT,ENEMY_DEFEAT,BOSS_DAMAGE -> switch(sequence&3) {
                case 0 -> 1;case 1 -> .94;case 2 -> .98;default -> .91;
            };
            default -> 1;
        };
    }
    static int duckMillis(Effect effect) {
        return switch(effect) {
            case BOSS_PHASE_TRANSITION,SPIRIT_TREE_RISING -> 1800;
            case BOSS_DEFEAT -> 2000;
            case SPIRIT_TREE_RESTORATION -> 2200;
            case SPIRIT_TREE_BURST -> 1200;
            case SPIRIT_TREE_CHIME -> 1000;
            default -> 0;
        };
    }
    /** One envelope per manager. Overlapping cues extend a hold; they never multiply reductions. */
    static final class Dynamics {
        private int duckHold,combatHold;
        private double duck=1,combat=1;
        void duck(int millis){duckHold=Math.max(duckHold,millis*44100/1000);}
        double advance(boolean dialogue,boolean fighting) {
            if(fighting)combatHold=44100*2;
            else if(combatHold>0)combatHold--;
            boolean held=dialogue||duckHold>0;
            if(duckHold>0)duckHold--;
            duck=approach(duck,held?.80:1,held?.20/4410:.20/35280);
            combat=approach(combat,combatHold>0?.94:1,.06/22050);
            // Strongest reduction wins, including dialogue during combat.
            return Math.min(duck,combat);
        }
        private static double approach(double value,double target,double step) {
            return value<target?Math.min(target,value+step):Math.max(target,value-step);
        }
    }
}
