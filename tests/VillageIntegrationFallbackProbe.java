import game.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/** Runs in an intentionally empty cwd: optional atlases and original art are unavailable. */
public class VillageIntegrationFallbackProbe {
    public static void main(String[]args)throws Exception{
        if(entity.VillageResident.prepareAsync().get(10,TimeUnit.SECONDS)!=null||VillageActivityVisuals.prepareAsync().get(10,TimeUnit.SECONDS)!=null)throw new AssertionError("Missing atlas must select native fallback");
        var image=new BufferedImage(2560,1920,2);var g=image.createGraphics();var state=new VillageActivities();
        VillageActivityVisuals.drawProps(g,state,0);state.lightCampfire();VillageActivityVisuals.drawProps(g,state,45);
        for(var site:VillageActivities.Site.values())if(site.resident()){
            entity.VillageResident.draw(g,site,false);
            if(entity.VillageResident.portrait(site.name())==null)throw new AssertionError("Fallback portrait missing: "+site);
        }
        g.dispose();System.out.println("PASS: missing atlas fallback renders every NPC, lantern, campfire and gate");
    }
}
