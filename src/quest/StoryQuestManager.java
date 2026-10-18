package quest;

import entity.Child;
import entity.Santa;
import game.Area;
import game.WorldMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Sequential main quests; restoration progress is observed from its existing session owner. */
public final class StoryQuestManager {
    public enum HudState { NEXT_OBJECTIVE, ACTIVE, COMPLETE }
    public record HudPresentation(HudState state,String header,String title,String objective) { }
    public enum Stage { DELIVER_PRESENTS, MEET_TOBY, CLEAR_GATE, FIND_GIFT, RETURN_GIFT, RESTORE_VILLAGE,
        TALK_TO_TOBY, INVESTIGATE, DEFEAT_FROST_FANG, TRAVEL_TOWN, REACH_TREE, DEFEAT_BOSS, RESTORE_TREE, CHRISTMAS_RESTORED }
    private final List<StoryQuest> quests;
    private final StoryQuestTarget gift;
    private final Set<String> brokenCrystals=new HashSet<>();
    private Child toby;
    private boolean villageCleared, treeReached, bossDefeated, endingTriggered;
    private boolean frostFangDefeated;
    public StoryQuestManager() { this(()->0); }
    public StoryQuestManager(java.util.function.IntSupplier restorationProgress) { this(restorationProgress,()->0); }
    public StoryQuestManager(java.util.function.IntSupplier restorationProgress,java.util.function.IntSupplier deliveryProgress) {
        quests=List.of(
            new StoryQuest(StoryQuest.Id.LOST_GIFT,"Lost Gift","Find Toby's lost gift and return it to him.",2,StoryQuest.State.LOCKED),
            new StoryQuest(StoryQuest.Id.BREAK_THE_FROST,"Investigate the Black Frost","Investigate and destroy three guarded Black Frost Crystals.",3,StoryQuest.State.LOCKED),
            new StoryQuest(StoryQuest.Id.SAVE_CHRISTMAS,"Save Christmas","Talk to Elias and free the guardian at the Spirit Tree.",3,StoryQuest.State.LOCKED),
            new StoryQuest(StoryQuest.Id.RESTORE_THE_VILLAGE,"Restore the Village","Restore the streetlights, house, Christmas tree and campfire.",4,StoryQuest.State.LOCKED,
                java.util.Objects.requireNonNull(restorationProgress)),
            new StoryQuest(StoryQuest.Id.CHRISTMAS_DELIVERY,"A Christmas Delivery","Deliver presents to Iris, Clara and Ben in any order.",3,StoryQuest.State.ACTIVE,
                java.util.Objects.requireNonNull(deliveryProgress)));

        var p=WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift");
        gift=new StoryQuestTarget(StoryQuest.Id.LOST_GIFT,"Toby's lost gift",p.x(),p.y(),new java.awt.Color(188,78,63));
    }
    public List<StoryQuest> getQuests() { return quests; }
    public List<StoryQuestTarget> getTargets() { return List.of(gift); }
    public StoryQuest getActiveQuest() { return quests.stream().filter(q->q.getState()==StoryQuest.State.ACTIVE).findFirst().orElse(null); }
    public StoryQuest getAvailableQuest() { return quests.stream().filter(q->q.getState()==StoryQuest.State.AVAILABLE).findFirst().orElse(null); }
    public StoryQuest getFocusedQuest() { var active=getActiveQuest();return active==null?quests.get(2):active; }
    public StoryQuestTarget getTargetForActiveQuest() { return quests.get(0).getState()==StoryQuest.State.ACTIVE&&!gift.isCollected()?gift:null; }
    public boolean canStartFor(Child child) { return child!=null&&child.getKind()==Child.Kind.TOY&&quests.get(0).getState()==StoryQuest.State.AVAILABLE; }
    public boolean startAfterDialogue(Child child) {
        if(!canStartFor(child))return false;toby=child;quests.get(0).setState(StoryQuest.State.ACTIVE);return true;
    }
    public boolean isStoryStarted() { return quests.get(0).getState()==StoryQuest.State.ACTIVE||isLostGiftComplete(); }
    public StoryQuest getDeliveryQuest(){return quests.get(4);}
    public boolean isDeliveryComplete(){return getDeliveryQuest().getState()==StoryQuest.State.COMPLETED;}
    public boolean synchronizeDelivery(){
        if(isDeliveryComplete()||getDeliveryQuest().getProgress()!=3)return false;
        getDeliveryQuest().setState(StoryQuest.State.COMPLETED);quests.get(0).setState(StoryQuest.State.AVAILABLE);return true;
    }
    public boolean isVillageCleared() { return villageCleared; }
    public boolean recordVillageCleared() {
        if(villageCleared||!isStoryStarted())return false;
        villageCleared=true;return true;
    }
    public boolean canInteractWithTarget(StoryQuestTarget target,Santa santa) {
        return santa!=null&&target==gift&&villageCleared&&quests.get(0).getState()==StoryQuest.State.ACTIVE&&gift.canInteract(santa)&&!santa.isDefeated();
    }
    public boolean completeTarget(StoryQuestTarget target,Santa santa) {
        if(!canInteractWithTarget(target,santa)||!target.collect())return false;
        quests.get(0).addProgress(1);return true;
    }
    public boolean isGiftFound() { return gift.isCollected(); }
    public boolean canReturnGiftTo(Child child,Santa santa) {
        return child!=null&&child==toby&&santa!=null&&!santa.isDefeated()
                &&quests.get(0).getState()==StoryQuest.State.ACTIVE&&isGiftFound()
                &&child.isWithinInteractionRange(santa);
    }
    /** Called only after the gift-return and restoration-assignment conversation closes. */
    public boolean returnGiftToToby(Child child,Santa santa) {
        if(!canReturnGiftTo(child,santa))return false;
        quests.get(0).addProgress(1);
        quests.get(0).setState(StoryQuest.State.COMPLETED);quests.get(3).setState(StoryQuest.State.ACTIVE);
        child.setCelebrating(true);return true;
    }
    public StoryQuest getRestorationQuest(){return quests.get(3);}
    public boolean synchronizeRestoration(){
        StoryQuest restoration=quests.get(3);
        if(restoration.getState()!=StoryQuest.State.ACTIVE||restoration.getProgress()!=4)return false;
        restoration.setState(StoryQuest.State.COMPLETED);return true;
    }
    public boolean isRestorationComplete(){return quests.get(3).getState()==StoryQuest.State.COMPLETED;}
    public boolean canReceiveBlackFrostClue(Child child,Santa santa){
        return isRestorationComplete()&&quests.get(1).getState()==StoryQuest.State.LOCKED
            &&child!=null&&child==toby&&child.isWithinInteractionRange(santa);
    }
    /** Completing restoration alone never activates crystal encounters. */
    public boolean acceptBlackFrostClue(Child child,Santa santa){
        if(!canReceiveBlackFrostClue(child,santa))return false;
        quests.get(1).setState(StoryQuest.State.ACTIVE);return true;
    }
    public Stage getStage(){
        if(!isDeliveryComplete())return Stage.DELIVER_PRESENTS;
        if(endingTriggered)return Stage.CHRISTMAS_RESTORED;
        if(bossDefeated)return Stage.RESTORE_TREE;
        if(isBossQuestAccepted())return treeReached?Stage.DEFEAT_BOSS:Stage.REACH_TREE;
        if(isChristmasTownLeadUnlocked())return Stage.TRAVEL_TOWN;
        if(isFrostFangAvailable())return Stage.DEFEAT_FROST_FANG;
        if(isCrystalQuestActive())return Stage.INVESTIGATE;
        if(isRestorationComplete())return Stage.TALK_TO_TOBY;
        if(isLostGiftComplete())return Stage.RESTORE_VILLAGE;
        if(isGiftFound())return Stage.RETURN_GIFT;
        if(isStoryStarted())return villageCleared?Stage.FIND_GIFT:Stage.CLEAR_GATE;
        return Stage.MEET_TOBY;
    }
    public boolean isLostGiftComplete() { return quests.get(0).getState()==StoryQuest.State.COMPLETED; }
    public boolean isCrystalQuestActive() { return quests.get(1).getState()==StoryQuest.State.ACTIVE; }
    public int getCrystalsDestroyed() { return brokenCrystals.size(); }
    public boolean recordCrystalDestroyed(String id) {
        if(!isCrystalQuestActive()||id==null||!Set.of("crystal-1","crystal-2","crystal-3").contains(id)||!brokenCrystals.add(id))return false;
        quests.get(1).addProgress(1);
        if(brokenCrystals.size()==3)quests.get(1).setState(StoryQuest.State.COMPLETED);
        return true;
    }
    public boolean isFrostFangAvailable(){return isRestorationComplete()&&brokenCrystals.size()==3&&!frostFangDefeated;}
    public boolean isFrostFangDefeated(){return frostFangDefeated;}
    public boolean recordFrostFangDefeated(){
        if(!isFrostFangAvailable())return false;
        frostFangDefeated=true;quests.get(2).setState(StoryQuest.State.AVAILABLE);return true;
    }
    public boolean isChristmasTownLeadUnlocked() { return isRestorationComplete()&&brokenCrystals.size()==3&&frostFangDefeated; }
    public boolean acceptEliasQuest() {
        if(quests.get(2).getState()!=StoryQuest.State.AVAILABLE)return false;
        quests.get(2).setState(StoryQuest.State.ACTIVE);return true;
    }
    public boolean isBossQuestAccepted() { return quests.get(2).getState()==StoryQuest.State.ACTIVE||endingTriggered; }
    public boolean reachSpiritTree() {
        if(!isBossQuestAccepted()||treeReached)return false;
        treeReached=true;quests.get(2).addProgress(1);return true;
    }
    public boolean isTreeReached() { return treeReached; }
    public boolean recordBossDefeated() {
        if(!isBossQuestAccepted()||!treeReached||bossDefeated)return false;
        bossDefeated=true;quests.get(2).addProgress(1);return true;
    }
    public boolean isBossDefeated() { return bossDefeated; }
    public boolean triggerEnding() {
        if(!bossDefeated||endingTriggered)return false;
        endingTriggered=true;quests.get(2).setState(StoryQuest.State.COMPLETED);return true;
    }
    public boolean isEndingTriggered() { return endingTriggered; }
    public void updateHudPresentation() { }
    public HudPresentation getHudPresentation() {
        if(!isDeliveryComplete())return view("A Christmas Delivery","Deliver presents to Iris, Clara and Ben ("+getDeliveryQuest().getProgress()+"/3)");
        if(endingTriggered)return new HudPresentation(HudState.COMPLETE,"CHRISTMAS RESTORED","Save Christmas","");
        if(bossDefeated)return view("Save Christmas","Restore the Spirit Tree [E]");
        if(isBossQuestAccepted())return view("Save Christmas",treeReached?"Defeat Dark Titan":"Clear the town encounter\nReach the Spirit Tree");
        if(isChristmasTownLeadUnlocked())return new HudPresentation(HudState.NEXT_OBJECTIVE,"NEXT OBJECTIVE","Save Christmas","Travel to Christmas Town\nTalk to Elias [E]");
        if(isFrostFangAvailable())return view("Frost Fang","Defeat Frost Fang\nEastern Forest clearing");
        if(isCrystalQuestActive())return view("Investigate the Black Frost","Black Frost Crystals ("+brokenCrystals.size()+"/3)\nDefeat guards; attack the core");
        if(isRestorationComplete())return view("Talk to Toby","Return to Toby [E]\nAsk about the remaining frost");
        if(isLostGiftComplete())return view("Restore the Village","Restorations ("+quests.get(3).getProgress()+"/4)\nCollect materials in Frozen Forest");
        if(isGiftFound())return view("Lost Gift","Return the gift to Toby [E]");
        if(isStoryStarted())return view("Lost Gift",villageCleared?"Find Toby's lost gift\nFrozen Forest - northern road":"Clear the Forest gate encounter");
        return new HudPresentation(HudState.NEXT_OBJECTIVE,"NEXT OBJECTIVE","Lost Gift","Talk to Toby [E]");
    }
    private HudPresentation view(String title,String objective) { return new HudPresentation(HudState.ACTIVE,"QUEST",title,objective); }
    public String getGuidanceText() { return getHudPresentation().objective(); }
}
