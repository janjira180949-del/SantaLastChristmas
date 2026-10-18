package game;

import ui.AbilityHUD;
import ui.ChristmasUI;

import boss.ChristmasEater;
import entity.Child;
import entity.Enemy;
import entity.Santa;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import quest.StoryQuestManager;
import quest.StoryQuestTarget;
import spirit.SpiritTree;
import ui.DialogueBox;
import ui.DialogueManager;
import ui.VillageActivityUI;
import ui.Portrait;
import ui.HUD;
import ui.GameScreens;
import ui.CinematicMenu;
import ui.AreaTitleOverlay;
import audio.AudioCatalog.Music;
import audio.AudioCatalog.Ambience;
import static audio.AudioCatalog.Effect.*;

@SuppressWarnings("serial") // Swing sessions are never serialized.
public final class GamePanel extends JPanel implements Runnable, KeyListener {
    private static final long serialVersionUID = 2L;

    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_NANOS = 1_000_000_000L / TARGET_FPS;

    private Thread gameThread;
    private volatile boolean running = false;

    private Santa santa;
    private final Camera camera = new Camera();
    private final SantaBaseVisuals baseVisuals;
    private final WorkshopInteractions workshopInteractions=new WorkshopInteractions();
    private VillageActivities villageActivities=new VillageActivities();
    private final ui.VillageActivityUI activityWindow=new ui.VillageActivityUI();
    private VillageActivities.Site activityDialogueSite;
    private final VillageRestorationVisuals restorationVisuals=new VillageRestorationVisuals();
    private final GiftDeliveryPresentation giftDelivery=new GiftDeliveryPresentation();
    private VillageActivities.Site interactionFocus;
    private boolean deliverySkip;
    private final AtomicReference<Point> pendingActivityClick=new AtomicReference<>();
    private final java.util.concurrent.atomic.AtomicBoolean pendingActivityClose=new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.concurrent.atomic.AtomicInteger pendingActivityKey=new java.util.concurrent.atomic.AtomicInteger();
    private AreaManager areaManager;
    private List<Child> children;
    // One map marker owns the Town Keeper's exploration anchor.
    private final entity.Elias elias = createTownKeeper();
    private static entity.Elias createTownKeeper(){
        var p=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("npc/Elias");
        return new entity.Elias(p.x()-16,p.y()-24);
    }
    private final List<Enemy> enemies;
    private StoryQuestManager storyQuestManager;
    private SpiritTree spiritTree;
    private EndingCutscene endingCutscene = new EndingCutscene();
    private BossFallPresentation bossFall=new BossFallPresentation();
    private int celebrationCameraReturn;
    private double celebrationCameraX,celebrationCameraY;
    private void returnCelebrationCamera(){
        celebrationCameraX=camera.getX()+Math.max(1,getWidth())/2.0;
        celebrationCameraY=camera.getY()+Math.max(1,getHeight())/2.0;
        celebrationCameraReturn=EndingCutscene.CAMERA_RETURN_DURATION;
    }
    private final EndingTownVisuals endingTownVisuals = new EndingTownVisuals();
    private SpiritTreeTransformation treeTransformation=new SpiritTreeTransformation();
    private final ChristmasTownVisuals townVisuals=new ChristmasTownVisuals();
    private final FrozenForestVisuals forestVisuals=new FrozenForestVisuals();
    private final StoryOverlay storyOverlay = new StoryOverlay();
    private boolean bossLoreShown;
    private ChristmasEater.Phase lastBossNarrativePhase;
    private final DialogueManager dialogueManager;
    private final DialogueBox dialogueBox;
    private final HUD hud;
    private final audio.AudioManager audio=new audio.AudioManager();
    private final audio.GameplayAudio gameplayAudio;
    private final AdventureAudio adventureAudio=new AdventureAudio(audio);
    private final CinematicMenu mainMenu=new CinematicMenu(audio);
    private final ui.AreaTitleOverlay areaTitle=new ui.AreaTitleOverlay();
    private final AbilitySystem abilitySystem = new AbilitySystem();
    private final CombatVisuals combatVisuals=new CombatVisuals();
    private final WorldAtmosphere worldAtmosphere=new WorldAtmosphere();
    private final java.util.concurrent.atomic.AtomicInteger pendingAbility = new java.util.concurrent.atomic.AtomicInteger();
    private ChristmasEater christmasEater;
    private Enemy activeBoss;
    private String bossIntroId="",pendingBossVictory="";
    private int lastBossPhase=-1,shakeFrames;
    private long observedFootstep;
    private String areaMessage;
    private int areaMessageFrames;
    private boolean activityNotice;

    private boolean developerTestKeyDown;
    private final java.util.concurrent.atomic.AtomicInteger pendingDeveloperTestToggles =
            new java.util.concurrent.atomic.AtomicInteger();

    public enum State { MAIN_MENU, PLAYING, PAUSED, GAME_OVER, ENDING }
    private State state = State.MAIN_MENU;
    private EncounterManager encounterManager;
    private List<BlackFrostCrystal> crystals;
    private final List<HealingPoint> healingPoints = new ArrayList<>();
    private final java.util.concurrent.atomic.AtomicBoolean pendingDash = new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.concurrent.atomic.AtomicInteger pendingScreenAction = new java.util.concurrent.atomic.AtomicInteger();
    private boolean dashKeyDown, bellKeyDown,pauseKeyDown;
    private WorldMap.Point areaCheckpoint;
    public synchronized State getState() { return state; }

    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean interactionKeyDown;
    private Point skillMouse = new Point(400,300);
    private boolean starKeyDown,bombKeyDown;
    private final AtomicReference<Point> pendingAttack = new AtomicReference<>();
    private final java.util.concurrent.atomic.AtomicBoolean pendingInteraction =
            new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.concurrent.atomic.AtomicBoolean pendingDialogueAdvance =
            new java.util.concurrent.atomic.AtomicBoolean();

    public GamePanel() {
        this(null);
    }
    /** Optional presentation sink for deterministic event tests without an audio device. */
    public GamePanel(java.util.function.Consumer<audio.AudioCatalog.Effect> effectPlayback) {
        this(effectPlayback,new SantaBaseVisuals());
    }
    /** Optional Base presentation fixture; production constructors always use the shared cache. */
    public GamePanel(java.util.function.Consumer<audio.AudioCatalog.Effect> effectPlayback, SantaBaseVisuals basePresentation) {
        ChristmasUI.prepare();
        Phase3Vfx.prepare();
        entity.VillageResident.prepareAsync();
        VillageActivityVisuals.prepareAsync();
        FinalChapterVisuals.prepareAsync();
        JourneyHomeVisuals.prepareAsync();
        VillageRestorationVisuals.prepareAsync();
        boss.BossArtwork.prepareAsync();
        TownChapterArt.prepareAsync();
        TownCelebration.prepareAsync();
        baseVisuals=java.util.Objects.requireNonNull(basePresentation);
        gameplayAudio=new audio.GameplayAudio(effectPlayback==null?audio::playEffect:(effect,gain)->effectPlayback.accept(effect));
        abilitySystem.skills().bindAudio(audio::playEvent,(id,owner)->audio.playOwned(id,owner,true),audio::stopOwner);
        if(effectPlayback==null)gameplayAudio.bindEvents(audio::playEvent,(id,owner)->audio.playOwned(id,owner,true),audio::stopOwner);
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent event) {
                synchronized(GamePanel.this) {mainMenu.focusLost();clearCutsceneInput();starKeyDown=bombKeyDown=dashKeyDown=bellKeyDown=interactionKeyDown=developerTestKeyDown=pauseKeyDown=false;}
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                synchronized(GamePanel.this) {
                    skillMouse=event.getPoint();
                    if (event.getButton() != MouseEvent.BUTTON1) return;
                    if(state==State.MAIN_MENU) {
                        requestFocusInWindow();mainMenu.mousePressed(event.getX(),event.getY(),getWidth(),getHeight());
                    } else if(state==State.PAUSED) {
                        if(GameScreens.button(getWidth(),getHeight(),0).contains(event.getPoint()))pendingScreenAction.set(5);
                    } else if(state==State.GAME_OVER||endingCutscene.isComplete()) {
                        for(int row=0;row<(state==State.GAME_OVER?2:1);row++) if(GameScreens.button(getWidth(),getHeight(),row).contains(event.getPoint()))
                            pendingScreenAction.set(row==0&&state==State.GAME_OVER?2:3);
                     } else if(state==State.PLAYING&&activityWindow.isOpen()) {
                        pendingActivityClick.set(event.getPoint());
                    } else if(isGameplayHudVisible()&&AbilityHUD.contains(getWidth(),getHeight(),event.getX(),event.getY())) {
                        pendingAttack.set(null);
                    } else if(isGameplayHudVisible()&&HUD.questClick(currentObjective(),villageActivities.chapter,event.getX(),event.getY(),getWidth(),getHeight())) {
                        pendingAttack.set(null);
                    } else if(state==State.PLAYING&&!giftDelivery.isActive()&&!bossFall.isActive()&&!dialogueManager.isActive()&&!storyOverlay.isActive()) {
                        pendingAttack.set(aimInWorld(event.getPoint()));
                    }
                }
            }
            @Override public void mouseReleased(MouseEvent event) {
                synchronized(GamePanel.this) {
                    if(state==State.MAIN_MENU&&event.getButton()==MouseEvent.BUTTON1)
                        mainMenu.mouseReleased(event.getX(),event.getY(),getWidth(),getHeight());
                }
            }
            @Override public void mouseExited(MouseEvent event) {
                synchronized(GamePanel.this) {if(state==State.MAIN_MENU)mainMenu.mouseExited();}
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent event) {
                synchronized(GamePanel.this) {
                    skillMouse=event.getPoint();
                    if(state==State.MAIN_MENU)mainMenu.mouseMoved(event.getX(),event.getY(),getWidth(),getHeight());
                }
            }
            @Override public void mouseDragged(MouseEvent event) {mouseMoved(event);}
        });

        enemies = new ArrayList<>();
        dialogueManager = new DialogueManager();
        dialogueBox = new DialogueBox();
        hud = new HUD();
        resetSession();
        syncAudio();
    }

    private void resetSession() {
        adventureAudio.reset();giftDelivery.clear();interactionFocus=null;deliverySkip=false;gameplayAudio.reset();villageActivities=new VillageActivities();activityWindow.close();activityDialogueSite=null;
        storyQuestManager=new StoryQuestManager(villageActivities.chapter::completed,villageActivities::deliveries); areaManager=new AreaManager(storyQuestManager,villageActivities);
        var spawn=WorldMap.forArea(Area.Type.SANTA_BASE).point("spawn");
        santa=new Santa(spawn.x()-16,spawn.y()-21);
        var toby=WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby");
        children=new ArrayList<>();children.add(new Child("Toby",Child.Kind.TOY,toby.x()-15,toby.y()-21,new Color(63,137,183)));
        spiritTree=new SpiritTree();endingCutscene=new EndingCutscene();bossFall=new BossFallPresentation();celebrationCameraReturn=0;treeTransformation=new SpiritTreeTransformation();encounterManager=new EncounterManager();
        crystals=encounterManager.getEncounters().stream().filter(e->e.getId().startsWith("crystal-")).map(BlackFrostCrystal::new).toList();
        healingPoints.clear();
        healingPoints.add(new HealingPoint(Area.Type.FOREST,720,1168));
        healingPoints.add(new HealingPoint(Area.Type.FOREST,1664,1280));
        healingPoints.addAll(HealingPoint.townRestPoints());
        enemies.clear();christmasEater=null;activeBoss=null;bossIntroId=pendingBossVictory="";lastBossPhase=-1;shakeFrames=0;observedFootstep=0;bossLoreShown=false;lastBossNarrativePhase=null;worldAtmosphere.reset();
        dialogueManager.close();workshopInteractions.reset();storyOverlay.start();abilitySystem.reset();clearCutsceneInput();
        areaMessage=null;areaMessageFrames=0;loadCurrentAreaContent();updateCamera();
    }

    public synchronized void beginAdventure() {
        resetSession();state=State.PLAYING;
        storyOverlay.start("Christmas Eve has arrived!\n\nSanta has three presents to deliver\nin The Last Village.\n\nJust another peaceful Christmas...\nor so he thinks.");
        syncAudio();
    }

    public synchronized void startGame() {
        if(running)return;
        audio.start();syncAudio();
        running=true;gameThread=new Thread(this,"game-loop");gameThread.start();requestFocusInWindow();
    }
    public void stopGame() { running=false;if(gameThread!=null)gameThread.interrupt();audio.close(); }

    @Override
    public void run() {
        long nextFrameTime = System.nanoTime();

        while (running) {
            update();
            repaint();

            nextFrameTime += FRAME_TIME_NANOS;
            long waitTime = nextFrameTime - System.nanoTime();

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime / 1_000_000L,
                            (int) (waitTime % 1_000_000L));
                } catch (InterruptedException e) {
                    running = false;
                    Thread.currentThread().interrupt();
                }
            } else {
                // If a frame is late, restart the schedule instead of spinning.
                nextFrameTime = System.nanoTime();
            }
        }
    }

    private synchronized void update() {
        audio.beginUpdate();try {updateFrame();} finally {gameplayAudio.resolveCombat();gameplayAudio.observeQuests(storyQuestManager);syncAudio();}
    }
    private void syncAudio() {
        adventureAudio.sync(state,areaManager.getCurrentArea().getType(),santa,storyQuestManager,villageActivities,
            endingCutscene,treeTransformation,activeBoss,storyOverlay.isActive(),dialogueManager.isActive(),activityWindow,
            pendingBossVictory,bossFall,crystals,encounterManager.getActive());
    }
    private void updateFrame() {
        int screenAction=pendingScreenAction.getAndSet(0);
        if(screenAction==1&&state==State.MAIN_MENU)beginAdventure();
        else if(screenAction==2&&state==State.GAME_OVER)retryEncounter();
        else if(screenAction==3){resetSession();mainMenu.resetNavigation();state=State.MAIN_MENU;}
        else if(screenAction==4&&state==State.PLAYING){clearCutsceneInput();state=State.PAUSED;}
        else if(screenAction==5&&state==State.PAUSED){clearCutsceneInput();state=State.PLAYING;}
        else if(screenAction==6&&treeTransformation.isComplete()){
            if(endingCutscene.getState()==EndingCutscene.State.SLEIGH_DEPARTURE){finishSleighJourney();}
            else {if(endingCutscene.isCelebration())villageActivities.chapter.completeCelebration();endingCutscene.dismissPresentation();clearCutsceneInput();state=State.PLAYING;}
        }
        else if(screenAction==7&&villageActivities.chapter.letterFinished()&&state==State.PLAYING){endingCutscene.startHomeEnding(true);clearCutsceneInput();}
        else if(screenAction==8&&endingCutscene.hasStarted()){
            boolean celebration=endingCutscene.isCelebration(),departure=endingCutscene.getState()==EndingCutscene.State.SLEIGH_DEPARTURE;
            endingCutscene.advancePresentation();
            if(!endingCutscene.hasStarted()){
                if(departure)finishSleighJourney();
                else {if(celebration){villageActivities.chapter.completeCelebration();returnCelebrationCamera();}state=State.PLAYING;clearCutsceneInput();notifyPlayer("Christmas returns. The sleigh is waiting in the plaza. [E] Return Home.");}
            }
        }
        if(screenAction==9&&bossFall.isActive()){bossFall.finish();clearCutsceneInput();}
        if(state==State.MAIN_MENU) {
            mainMenu.update();
            CinematicMenu.Action menuAction=mainMenu.pollAction();
            if(menuAction==CinematicMenu.Action.START)beginAdventure();
            else if(menuAction==CinematicMenu.Action.EXIT) {
                stopGame();
                javax.swing.SwingUtilities.invokeLater(()-> {
                    java.awt.Window window=javax.swing.SwingUtilities.getWindowAncestor(this);
                    if(window!=null)window.dispatchEvent(new java.awt.event.WindowEvent(window,java.awt.event.WindowEvent.WINDOW_CLOSING));
                });
            }
            return;
        }
        if(state==State.GAME_OVER||state==State.PAUSED)return;
        if(celebrationCameraReturn>0)celebrationCameraReturn--;
        // Screen actions can resume the simulation in this update; accepted release/contact audio must resume with it.
        audio.setGameplaySuspended(storyOverlay.isActive()||dialogueManager.isActive()||activityWindow.isOpen()||bossFall.isActive()||endingCutscene.hasStarted());
        if(treeTransformation.isActive()) {
            // In-world restoration: retain movement and the exact authoritative 180-tick owner.
            if(pendingDash.getAndSet(false))santa.startDash(upPressed,downPressed,leftPressed,rightPressed);
            clearCombatInput();santa.update(upPressed,downPressed,leftPressed,rightPressed,currentMap().solids());worldAtmosphere.update();
            if(treeTransformation.update(spiritTree)){
                state=State.PLAYING;
                if(villageActivities.chapter.beginCelebration()){
                    endingCutscene.startCelebration(currentMap().point("landmarks/Corrupted Town Center"));state=State.ENDING;
                }
                notifyPlayer("Christmas Spirit returns. The Town neighbors gather beneath the tree.");
            }
            gameplayAudio.restorationTick(treeTransformation.getFrames());
            updateCamera();return;
        }
        if(bossFall.isActive()){clearCutsceneInput();santa.stopForCutscene();bossFall.update();if(!bossFall.isActive())finishBossVictory();worldAtmosphere.update();updateCamera();return;}
        if(!pendingBossVictory.isEmpty())finishBossVictory();
        if(endingCutscene.hasStarted()) {
            boolean departure=endingCutscene.getState()==EndingCutscene.State.SLEIGH_DEPARTURE;
            clearCutsceneInput();for(Child child:children)child.updateVisual();elias.updateVisual();endingCutscene.update(spiritTree);
            if(departure&&!endingCutscene.hasStarted())finishSleighJourney();updateCamera();return;
        }
        if((pendingDeveloperTestToggles.getAndSet(0)&1)!=0)santa.toggleDeveloperTestMode();
        if(santa.isDefeated()){enterGameOver();return;}
        if(giftDelivery.isActive()){
            clearCutsceneInput();santa.stopForCutscene();worldAtmosphere.update();
            if(giftDelivery.update(deliverySkip)){
                var recipient=giftDelivery.recipient();giftDelivery.clear();deliverySkip=false;
                dialogueManager.startNarrative(villageActivities.deliveryThanks(recipient));activityDialogueSite=recipient;activityFeedback(false);
            }
            updateCamera();return;
        }
        if(activityWindow.isOpen()) {
            clearCombatInput();clearMovementInput();santa.stopForCutscene();
            if(pendingActivityClose.getAndSet(false)){activityWindow.close();clearCutsceneInput();return;}
            Point click=pendingActivityClick.getAndSet(null);
            if(click!=null)activityWindow.click(click.x,click.y,getWidth(),getHeight(),villageActivities);
            int key=pendingActivityKey.getAndSet(0);if(key!=0)activityWindow.key(key,villageActivities);
            activityWindow.update(villageActivities);activityFeedback();
            if(activityWindow.pollReturnRequested()&&villageActivities.chapter.celebrated()){
                endingCutscene.startSleighDeparture();state=State.ENDING;santa.stopForCutscene();clearCutsceneInput();updateCamera();return;
            }
            if(activityWindow.pollFinishRequested()&&villageActivities.chapter.homeReturned()){
                villageActivities.chapter.finishLetter();endingCutscene.startHomeEnding(false);santa.stopForCutscene();clearCutsceneInput();return;
            }
            if(!activityWindow.isOpen())clearCutsceneInput();
            return;
        }
        worldAtmosphere.update();
        if(areaManager.getCurrentArea().getType()==Area.Type.SANTA_BASE)workshopInteractions.updateResponse(worldAtmosphere.getTicks());
        for(Child child:children)child.updateVisual();elias.updateVisual();
        for(var target:storyQuestManager.getTargets())target.updateVisual();
        if(storyOverlay.isActive()) {
            storyOverlay.update();updateCamera();
            clearCombatInput();
            if(pendingDialogueAdvance.getAndSet(false)){audio.playEvent("npc.page");storyOverlay.advance();if(!storyOverlay.isActive())updateCamera();}
            return;
        }
        if(dialogueManager.isActive()) {
            clearCombatInput();
            if(pendingDialogueAdvance.getAndSet(false)) {
                Child child=dialogueManager.getActiveDialogue().getChild();
                boolean eliasSpeaking=dialogueManager.isEliasDialogueActive();
                boolean returningGift=dialogueManager.isGiftReturnDialogueActive();
                boolean receivingClue=dialogueManager.isBlackFrostClueDialogueActive();audio.playEvent("npc.page");
                dialogueManager.advance();
                if(!dialogueManager.isActive()) {
                    if(eliasSpeaking){if(storyQuestManager.acceptEliasQuest()){spiritTree.addSpirit(5);notifyPlayer("SAVE CHRISTMAS - Reach the Spirit Tree.");}}
                    else if(returningGift&&storyQuestManager.returnGiftToToby(child,santa)) {
                        spiritTree.addSpirit(20);villageActivities.chapter.unlock();
                        notifyPlayer("LOST GIFT COMPLETE - Christmas Spirit +20. RESTORE THE VILLAGE: collect materials in Frozen Forest, then bring warmth back to the village.");
                     } else if(receivingClue&&storyQuestManager.acceptBlackFrostClue(child,santa))notifyPlayer("INVESTIGATE THE BLACK FROST - Find the three crystals, defeat their guardians and break their cores.");
                     else if(storyQuestManager.startAfterDialogue(child))notifyPlayer("LOST GIFT - Clear the Forest gate, find Toby's gift, and bring it back to him.");
                    if(activityDialogueSite!=null){
                        
                        activityDialogueSite=null;clearCutsceneInput();santa.stopForCutscene();
                    }
                }
            }
            return;
        }
        if(pendingInteraction.getAndSet(false)&&handleInteraction())return;
        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN&&villageActivities.chapter.celebrated())endingCutscene.updateCelebrationAmbient();
        areaTitle.update();
        for(HealingPoint point:healingPoints)point.update();
        if(areaMessageFrames>0)areaMessageFrames--;
        List<Rectangle2D> solids=currentMap().solids();
        if(pendingDash.getAndSet(false)&&santa.startDash(upPressed,downPressed,leftPressed,rightPressed))gameplayAudio.play(DASH);
        santa.update(upPressed,downPressed,leftPressed,rightPressed,playerMovementSolids(solids));
        updateChildInteractions();
        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN
                &&encounterManager.isComplete("town")&&nearStoryPoint(spiritTreeInteractionPoint()))storyQuestManager.reachSpiritTree();
        encounterManager.advance(santa,enemies);
        encounterManager.startNearby(areaManager.getCurrentArea().getType(),santa,enemies,this::isEncounterAllowed);
        christmasEater=enemies.stream().filter(e->e instanceof ChristmasEater).map(e->(ChristmasEater)e).findFirst().orElse(null);
        activeBoss=enemies.stream().filter(e->e instanceof boss.BossCombatant).findFirst().orElse(null);
        if(activeBoss instanceof boss.BossCombatant actor&&!bossIntroId.equals(actor.getBossName())) {
            bossLoreShown=true;bossIntroId=actor.getBossName();lastBossPhase=-1;observedFootstep=actor.getFootstepSequence();
            if(activeBoss instanceof boss.FrostFang)storyOverlay.startBossAt("FROST FANG",boss.FrostFang.ARENA,"The final crystal shatters.\nA terrifying roar echoes through the forest.\nFrost Fang awakens, blocking the road\nto Christmas Town!","Beware its deadly claws and icy breath!\nDodge its marked attacks,\nthen strike when it leaves itself open.");
            else storyOverlay.startBossAt("DARK TITAN",currentMap().point("landmarks/Corrupted Town Center"),"Once the protector of the Spirit Tree,\nDark Titan now stands consumed\nby Black Frost.\nThe fallen guardian blocks\nSanta's final path.","To save the guardian, Santa must break\nBlack Frost's hold.\nDodge its devastating attacks,\nthen strike when it leaves itself open.");
            storyOverlay.bindBoss(activeBoss);updateCamera();clearCutsceneInput();return;
        }
        combatVisuals.beginFrame(enemies);
        gameplayAudio.captureCombat(santa,enemies);
        for(Enemy enemy:enemies){enemy.advanceCombatTimers();enemy.update(santa,solids);}
        if(shakeFrames>0)shakeFrames--;
        if(activeBoss instanceof boss.BossCombatant actor&&actor.getFootstepSequence()!=observedFootstep){observedFootstep=actor.getFootstepSequence();shakeFrames=6;}
        if(santa.isDefeated()){enterGameOver();return;}
        Point attack=pendingAttack.getAndSet(null);
        if(attack!=null){santa.setAttackDirection(attack.x-santa.getX()-16,attack.y-santa.getY()-21);if(santa.startAttack())gameplayAudio.play(SANTA_ATTACK);}
        abilitySystem.update();
        abilitySystem.skills().update(enemies,solids);
        int skillRequests=pendingAbility.getAndSet(0);
        if((skillRequests&1)!=0){if(abilitySystem.use(1,santa,enemies,solids))gameplayAudio.play(CHRISTMAS_BELL);else audio.playEvent("ui.unavailable");}
        Point aim=aimInWorld(skillMouse);
        aim.x=Math.max(8,Math.min(currentMap().width()-8,aim.x));aim.y=Math.max(8,Math.min(currentMap().height()-8,aim.y));
        if((skillRequests&2)!=0&&!abilitySystem.skills().cast(2,santa,aim.x,aim.y,solids))audio.playEvent("ui.unavailable");
        if((skillRequests&4)!=0&&!abilitySystem.skills().cast(3,santa,aim.x,aim.y,solids))audio.playEvent("ui.unavailable");
        CombatSystem.update(santa,enemies,gameplayAudio::swordContact);
        combatVisuals.resolve();
        if(santa.isDefeated()){enterGameOver();return;}
        Encounter completed=encounterManager.finishActive();
        if(completed!=null) {
            switch(completed.getId()) {
                case "village" -> {storyQuestManager.recordVillageCleared();notifyPlayer("Gate cleared. Find Toby's gift on the northern Forest road.");}
                case "boss","frost-fang" -> {Enemy fallen=completed.getMembers().stream().filter(e->e instanceof boss.BossCombatant).findFirst().orElse(null);bossFall=new BossFallPresentation();bossFall.start(fallen,completed.getId().equals("boss")?currentMap().point("landmarks/Corrupted Town Center"):boss.FrostFang.ARENA);pendingBossVictory=completed.getId();abilitySystem.clearEffects();clearCutsceneInput();santa.stopForCutscene();christmasEater=null;activeBoss=null;shakeFrames=0;if(completed.getId().equals("boss"))restoreSpiritTreeClearance();}
                case "town" -> notifyPlayer("Town road cleared. Reach the Spirit Tree to the south.");
                case "village-outskirts","forest-road","forest-clearing","town-side-road","forest-camp","forest-ambush","forest-exit" -> { /* Completion remains authoritative; redundant road toast omitted. */ }
                default -> notifyPlayer("Crystal core exposed. Attack it to break the frost.");
            }
        }
        for(BlackFrostCrystal crystal:crystals) {
            crystal.update(santa);
            if(areaManager.getCurrentArea().getType()==Area.Type.FOREST&&crystal.getImpactFrames()==8)audio.playEvent("crystal.impact");
            if(areaManager.getCurrentArea().getType()==Area.Type.FOREST&&storyQuestManager.isCrystalQuestActive()&&crystal.tryBreak(santa)
                    &&storyQuestManager.recordCrystalDestroyed(crystal.getId())) {
                gameplayAudio.playWorld(CRYSTAL_BREAK,santa,crystal.getPosition().x(),crystal.getPosition().y());
                spiritTree.addSpirit(15);
                notifyPlayer(switch(storyQuestManager.getCrystalsDestroyed()) {
                    case 1 -> "Black Frost weakened. One anchor is broken. Christmas Spirit +15.";
                    case 2 -> "The forest begins to recover. Two anchors are broken. Christmas Spirit +15.";
                    default -> "THREE ANCHORS BROKEN - Frost Fang waits in the eastern clearing. Defeat it to open Christmas Town. Christmas Spirit +15.";
                });
            }
        }
        if(activeBoss instanceof boss.BossCombatant actor&&lastBossPhase!=actor.getPhaseIndex()) {
            lastBossPhase=actor.getPhaseIndex();
            // Phase identity already lives in the boss HUD; avoid a second panel over its face.
        }
        if(areaManager.getCurrentArea().getType()==Area.Type.SANTA_BASE&&!villageActivities.chapter.homeReturned()&&workshopInteractions.shouldShowDeparture(santa))
            notifyPlayer(villageActivities.presentsCollected()?"Santa: The village is waiting. It's time to go.":"Don't forget the presents. Gift Storage is in the north-east workshop.");
        areaManager.updateUnlockedAreas();handleExitZone();updateCamera();
    }
    private boolean isEncounterAllowed(Encounter encounter) {
        return switch(encounter.getId()) {
            case "village" -> storyQuestManager.isStoryStarted();
            case "village-outskirts" -> storyQuestManager.isVillageCleared();
            case "forest-road","forest-clearing","forest-camp","forest-ambush","forest-exit" -> storyQuestManager.isStoryStarted();
            case "town-side-road" -> storyQuestManager.isChristmasTownLeadUnlocked()&&!storyQuestManager.isBossDefeated();
            case "town" -> storyQuestManager.isChristmasTownLeadUnlocked()&&!storyQuestManager.isBossDefeated();
            case "boss" -> storyQuestManager.isTreeReached()&&encounterManager.isComplete("town");
            case "frost-fang" -> storyQuestManager.isFrostFangAvailable();
            default -> storyQuestManager.isCrystalQuestActive();
        };
    }
    private void notifyPlayer(String message) { areaMessage=message;areaMessageFrames=240;activityNotice=false; }
    private void finishBossVictory(){
        if(pendingBossVictory.equals("frost-fang")){
            if(storyQuestManager.recordFrostFangDefeated())notifyPlayer("FROST FANG DEFEATED - The eastern gate to Christmas Town is open.");
        }else if(pendingBossVictory.equals("boss")&&storyQuestManager.recordBossDefeated()){
            spiritTree.addSpirit(15);notifyPlayer("The guardian is free. Return to the Spirit Tree and press E.");
        }
        pendingBossVictory="";areaManager.updateUnlockedAreas();
    }
    /** Player-only exception matching the existing hidden live-Titan Tree presentation. */
    private List<Rectangle2D> playerMovementSolids(List<Rectangle2D> solids) {
        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN
                &&villageActivities.chapter.celebrated()&&!endingCutscene.hasStarted()){
            solids=new java.util.ArrayList<>(solids);solids.addAll(JourneyHomeVisuals.parkedFootprints());
        }
        if(areaManager.getCurrentArea().getType()!=Area.Type.CHRISTMAS_TOWN
                ||!(activeBoss instanceof boss.DarkTitan)
                ||(storyOverlay.isActive()&&storyOverlay.isBossIntro()))return solids;
        var root=currentMap().point("landmarks/Corrupted Town Center");
        Rectangle2D treeBase=new Rectangle2D.Double(root.x()-112,root.y()-112,192,192);
        return solids.stream().filter(solid->!solid.equals(treeBase)).toList();
    }
    /** Tree reappears at the existing fall scene: clear its base before that scene starts. */
    private void restoreSpiritTreeClearance() {
        if(areaManager.getCurrentArea().getType()!=Area.Type.CHRISTMAS_TOWN)return;
        var root=currentMap().point("landmarks/Corrupted Town Center");
        if(!santa.getBounds().intersects(root.x()-112,root.y()-112,192,192))return;
        int x=santa.getX(),y=santa.getY();WorldMap.Point nearest=null;double distance=Double.MAX_VALUE;
        var solids=currentMap().solids();
        for(int[] candidate:new int[][]{{x,root.y()-154},{x,root.y()+88},{root.x()-144,y},{root.x()+80,y}}) {
            if(util.Collision.intersectsAny(new Rectangle2D.Double(candidate[0],candidate[1]-8,32,50),solids))continue;
            double d=Math.hypot(candidate[0]-x,candidate[1]-y);
            if(d<distance){distance=d;nearest=new WorldMap.Point(candidate[0],candidate[1]);}
        }
        if(nearest==null){var free=currentMap().safePosition(x,y-8,32,50);nearest=new WorldMap.Point(free.x(),free.y()+8);}
        santa.setPosition(nearest.x(),nearest.y());
    }
    private Point aimInWorld(Point screen){Point world=camera.screenToWorld(screen);int shake=!mainMenu.isReducedMotion()&&shakeFrames>0?(shakeFrames%2==0?2:-2):0;world.translate(-shake,shake);return world;}
    private void clearCombatInput() { pendingAttack.set(null);pendingAbility.set(0);pendingDash.set(false);pendingInteraction.set(false); }
    private void cancelGiftDelivery(){
        if(giftDelivery.isActive())villageActivities.pollFeedback(); // Discard only the canceled presentation's pending notice.
        giftDelivery.clear();deliverySkip=false;
    }
    private void enterGameOver() { cancelGiftDelivery();activityWindow.close();state=State.GAME_OVER;clearCutsceneInput(); }
    private void retryEncounter() {
        celebrationCameraReturn=0;cancelGiftDelivery();interactionFocus=null;activityWindow.close();dialogueManager.close();activityDialogueSite=null;
        if(!encounterManager.retry(santa,enemies))santa.restoreForRetry(areaCheckpoint.x(),areaCheckpoint.y());
        if(areaManager.getCurrentArea().getType()==Area.Type.SANTA_BASE){dialogueManager.close();workshopInteractions.reset();}
        abilitySystem.reset();combatVisuals.reset();clearCutsceneInput();state=State.PLAYING;
        notifyPlayer("Try again. Watch the warnings and use your dash.");updateCamera();
    }
    private WorkshopInteractions.Site nearbyWorkshopSite() {
        return areaManager.getCurrentArea().getType()==Area.Type.SANTA_BASE&&isGameplayHudVisible()
            ?WorkshopInteractions.nearby(santa):null;
    }
    private boolean handleInteraction() {
        updateChildInteractions();
        if(areaManager.getCurrentArea().getType()==Area.Type.SANTA_BASE) {
            var site=nearbyWorkshopSite();
            if(site==WorkshopInteractions.Site.LETTER_DESK&&villageActivities.chapter.homeReturned()){
                activityWindow.open(VillageActivityUI.Mode.LETTERS,null,villageActivities);clearCutsceneInput();santa.stopForCutscene();return true;
            }
            if(site==WorkshopInteractions.Site.GIFT_STORAGE&&!villageActivities.presentsCollected()) {
                villageActivities.collectPresents();activityFeedback();
                
                clearCutsceneInput();santa.stopForCutscene();return true;
            }
            if(site==WorkshopInteractions.Site.GIFT_STORAGE){notifyPlayer("Presents Collected 3/3");clearCutsceneInput();return true;}
            if(site!=null&&dialogueManager.startNarrative(site.lines(villageActivities.chapter.homeReturned()))) {
                workshopInteractions.respond(site,worldAtmosphere.getTicks());
                clearCutsceneInput();santa.stopForCutscene();return true;
            }
            return false;
        }
        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN&&villageActivities.chapter.celebrated()
                &&FinalChapter.inReach(santa,currentMap(),JourneyHomeVisuals.SLEIGH_APPROACH)){
            activityWindow.open(VillageActivityUI.Mode.SLEIGH,null,villageActivities);clearCutsceneInput();santa.stopForCutscene();return true;
        }
        if(handlePostBossInteraction())return true;
        if(encounterManager.getActive()!=null){if(areaManager.getCurrentArea().getType()==Area.Type.FOREST&&villageActivities.chapter.nearby(santa)!=null)notifyPlayer("Clear the nearby encounter before collecting.");return false;}
        if(areaManager.getCurrentArea().getType()==Area.Type.FOREST){
            var pickup=villageActivities.chapter.nearby(santa);
            if(pickup!=null&&villageActivities.chapter.collect(pickup.id())){activityFeedback(false);return true;}
        }
        for(HealingPoint point:healingPoints)if(point.getArea()==areaManager.getCurrentArea().getType()&&point.isNearby(santa)) {
            if(point.isUsed()){audio.playEvent("shrine.unavailable");notifyPlayer("Shelter Already Used");}
            else if(point.use(santa,false)){gameplayAudio.play(HEALING_SHRINE);notifyPlayer("HP Restored");}
            else {audio.playEvent("shrine.unavailable");notifyPlayer("HP is already full.");}
            return true;
        }
        if(areaManager.getCurrentArea().getType()==Area.Type.FOREST&&completeNearbyStoryTarget())return true;
        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN
                &&dialogueManager.startFor(elias,santa,storyQuestManager.isBossQuestAccepted(),storyQuestManager.isBossDefeated(),encounterManager.isComplete("town"))) {
            gameplayAudio.play(NPC_INTERACTION);
            clearCutsceneInput();return true;
        }
        if(areaManager.getCurrentArea().getType()==Area.Type.VILLAGE&&handleVillageActivity())return true;
        Child child=findNearbyChild();
        if(dialogueManager.startFor(child,santa,storyQuestManager)){gameplayAudio.play(NPC_INTERACTION);clearCutsceneInput();return true;}
        return false;
    }

    private void activityFeedback(){activityFeedback(true);}
    /** Consume the existing event feedback without suppressing its transaction or audio observer. */
    private void activityFeedback(boolean showBanner){
        String message=villageActivities.pollFeedback();if(message!=null){activityWindow.feedback(message);if(showBanner){notifyPlayer(message);activityNotice=true;}}
    }
    /** Compare both existing NPC owners instead of allowing Toby to suppress nearer residents. */
    private VillageActivities.Site nearestVillageSite(){
        var site=VillageActivities.nearby(santa);
        if(interactionFocus!=null&&VillageActivities.inReach(santa,interactionFocus)&&site!=null){
            double x=santa.getX()+16,y=santa.getY()+21;
            if(Math.hypot(x-interactionFocus.x,y-interactionFocus.y)<=Math.hypot(x-site.x,y-site.y)+10)site=interactionFocus;
        }
        interactionFocus=site;var child=findNearbyChild();
        if(site==null||child==null)return site;
        double sx=santa.getX()+16,sy=santa.getY()+21;
        return Math.hypot(sx-site.x,sy-site.y)<Math.hypot(sx-child.getX()-15,sy-child.getY()-21)?site:null;
    }
    private VillageActivities.Site nearbyVillageActivity(){
        return areaManager.getCurrentArea().getType()==Area.Type.VILLAGE&&isGameplayHudVisible()&&encounterManager.getActive()==null
            &&!giftDelivery.isActive()?nearestVillageSite():null;
    }
    private boolean handleVillageActivity(){
        var site=nearbyVillageActivity();if(site==null)return false;
        if(site.resident()){
            if(villageActivities.canDeliver(site)&&villageActivities.deliver(villageActivities.presentFor(site),site)){
                areaMessage=null;areaMessageFrames=0;activityNotice=false;
                storyQuestManager.synchronizeDelivery();giftDelivery.start(santa,site);deliverySkip=false;activityDialogueSite=site;gameplayAudio.play(NPC_INTERACTION);
            }else if(dialogueManager.startNarrative(villageActivities.conversation(site,storyQuestManager))){activityDialogueSite=site;gameplayAudio.play(NPC_INTERACTION);}
        }else{
            var objective=site.restoration();
            if(villageActivities.chapter.restore(objective)){
                storyQuestManager.synchronizeRestoration();restorationVisuals.respond(objective,worldAtmosphere.getTicks());activityFeedback();
            }
            else if(site==VillageActivities.Site.CAMPFIRE){notifyPlayer(villageActivities.chapter.requirement(objective));activityNotice=true;}
            else activityWindow.open(VillageActivityUI.Mode.RESTORATION,site,villageActivities);
        }
        clearCutsceneInput();santa.stopForCutscene();return true;
    }
    private WorldMap currentMap() { return WorldMap.forArea(areaManager.getCurrentArea().getType()); }

    private void updateCamera() {
        if(bossFall.isActive()){celebrationCameraReturn=0;bossFall.frameCamera(camera,Math.max(1,getWidth()),Math.max(1,getHeight()),currentMap(),santa);return;}
        if (endingCutscene.hasStarted()) {
            celebrationCameraReturn=0;endingCutscene.frameCamera(camera, Math.max(1, getWidth()), Math.max(1, getHeight()), currentMap(),santa);
            return;
        }
        if(storyOverlay.isActive()) {celebrationCameraReturn=0;storyOverlay.frameCamera(camera,santa,currentMap(),Math.max(1,getWidth()),Math.max(1,getHeight()));return;}
        double targetY=santa.getY()+21.0;
        if(activeBoss instanceof boss.DarkTitan&&areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN)targetY-=125;
        if(!(activeBoss instanceof boss.DarkTitan)&&areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN) {
            var tree=currentMap().point("landmarks/Corrupted Town Center");
            targetY-=TownChapterArt.treeCameraLift(santa.getX()+16,targetY,tree);
        }
        double targetX=santa.getX()+16.0;
        if(celebrationCameraReturn>0){
            double t=1-celebrationCameraReturn/(double)EndingCutscene.CAMERA_RETURN_DURATION;t=t*t*(3-2*t);
            targetX=celebrationCameraX+(targetX-celebrationCameraX)*t;
            targetY=celebrationCameraY+(targetY-celebrationCameraY)*t;
        }
        camera.follow(targetX, targetY,
                Math.max(1, getWidth()), Math.max(1, getHeight()), currentMap().width(), currentMap().height());
    }

    private Child findNearbyChild() {
        Child closestChild = null;
        double closestDistance = Double.MAX_VALUE;
        for (Child child : children) {
            if (!child.canInteract()) {
                continue;
            }
            double dx = santa.getX() + 16.0 - (child.getX() + 15.0);
            double dy = santa.getY() + 21.0 - (child.getY() + 21.0);
            double distance = Math.hypot(dx, dy);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestChild = child;
            }
        }
        return closestChild;
    }

    private boolean areChildrenInCurrentArea() {
        return areaManager.getCurrentArea().getType()==Area.Type.VILLAGE;
    }
    private WorldMap.Point spiritTreeInteractionPoint() {
        WorldMap.Point tree=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
        return new WorldMap.Point(tree.x(),tree.y()+128);
    }

    private boolean nearStoryPoint(WorldMap.Point point) {
        return point != null && !santa.isDefeated()
                && Math.hypot(santa.getX() + 16.0 - point.x(),
                        santa.getY() + 21.0 - point.y()) <= 58.0;
    }

    private boolean handlePostBossInteraction() {
        if(areaManager.getCurrentArea().getType()!=Area.Type.CHRISTMAS_TOWN
                ||!nearStoryPoint(spiritTreeInteractionPoint()))return false;
        if(treeTransformation.isComplete()){startEndingPresentation(false);return true;}
        if(!storyQuestManager.triggerEnding())return false;
        spiritTree.beginRestoration();treeTransformation.start();gameplayAudio.restorationTick(0);
        areaMessage=null;areaMessageFrames=0;
        for(int i=0;i<AreaTitleOverlay.DURATION_FRAMES;i++)areaTitle.update();
        townVisuals.prepare();endingTownVisuals.prepare();
        clearCutsceneInput();santa.stopForCutscene();state=State.PLAYING;updateCamera();return true;
    }
    private void startEndingPresentation(boolean creditsOnly) {
        if(!treeTransformation.isComplete())return;
        endingCutscene.startChapterEpilogue(WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center"),camera,
                Math.max(1,getWidth()),Math.max(1,getHeight()));
        endingCutscene.stageActors(santa,children,elias,WorldMap.forArea(Area.Type.CHRISTMAS_TOWN));
        clearCutsceneInput();santa.stopForCutscene();state=State.ENDING;
    }
    private void drawPostBossInteractions(Graphics2D graphics) {
        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN&&storyQuestManager.isBossDefeated()&&!treeTransformation.isActive())
            drawStoryMarker(graphics,spiritTreeInteractionPoint(),treeTransformation.isComplete()?"[E] Thank the neighbors":"[E] Restore the Spirit Tree");
    }

    /** Text-only story marker, matching existing E interactions without new artwork. */
    private void drawStoryMarker(Graphics2D graphics, WorldMap.Point point, String label) {
        graphics.setFont(new Font("Dialog", Font.BOLD, 12));
        int width = graphics.getFontMetrics().stringWidth(label);
        graphics.setColor(new Color(8, 14, 24, 220));
        graphics.fillRoundRect(point.x() - width / 2 - 8, point.y() - 59,
                width + 16, 24, 8, 8);
        graphics.setColor(new Color(255, 225, 145));
        graphics.drawString(label, point.x() - width / 2, point.y() - 42);
        graphics.drawOval(point.x() - 8, point.y() - 8, 16, 16);
    }

    private boolean completeNearbyStoryTarget() {
        StoryQuestTarget target = storyQuestManager.getTargetForActiveQuest();
        if (target != null && storyQuestManager.completeTarget(target, santa)) {
            gameplayAudio.play(GIFT_PICKUP);
            return true;
        }
        return false;
    }

    private void clearCutsceneInput() {
        if(state==State.ENDING||state==State.GAME_OVER||bossFall.isActive()||endingCutscene.hasStarted()||treeTransformation.isActive())abilitySystem.clearEffects();
        clearMovementInput();
        pendingAttack.set(null);
        pendingAbility.set(0);
        pendingDash.set(false);
        pendingInteraction.set(false);
        pendingDialogueAdvance.set(false);
        pendingDeveloperTestToggles.set(0);pendingActivityClick.set(null);pendingActivityClose.set(false);pendingActivityKey.set(0);
    }

    public boolean isGameplayHudVisible() {
        return state==State.PLAYING && !bossFall.isActive() && !storyOverlay.isActive() && !endingCutscene.hasStarted() && !dialogueManager.isActive() && !activityWindow.isOpen();
    }

    private void clearMovementInput() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
    }

    private void updateChildInteractions() {
        if (areChildrenInCurrentArea()) {
            for (Child child : children) {
                child.updateInteraction(santa);
            }
        } else {
            for (Child child : children) {
                child.clearInteractionState();
            }
        }
    }

    /** Returns the persistent Toby actor for story and ending use. */
    public List<Child> getChildren() {
        return Collections.unmodifiableList(children);
    }

    private void loadCurrentAreaContent() {
        celebrationCameraReturn=0;audio.cancelGameplay();abilitySystem.clearEffects();pendingAbility.set(0);
        cancelGiftDelivery();interactionFocus=null;restorationVisuals.clearResponse();
        activityWindow.close();if(activityDialogueSite!=null)dialogueManager.close();activityDialogueSite=null;
        pendingActivityClick.set(null);pendingActivityClose.set(false);
        encounterManager.leaveArea(enemies);
        enemies.clear();christmasEater=null;activeBoss=null;shakeFrames=0;
        combatVisuals.reset();
        areaCheckpoint=new WorldMap.Point(santa.getX(),santa.getY());
        areaTitle.show(areaManager.getCurrentArea().getType());
        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN)townVisuals.prepare();
    }

    private boolean handleExitZone() {
        for (ExitZone gate : areaManager.getCurrentExitZones()) {
            if (!gate.intersects(santa.getBounds())) continue;
            if(encounterManager.getActive()!=null){notifyPlayer("Defeat the encounter before leaving.");return false;}
            Area.Type source=areaManager.getCurrentArea().getType();
            if(!areaManager.tryTravel(gate.getDestination())) {
                notifyPlayer(source==Area.Type.SANTA_BASE?(villageActivities.chapter.homeReturned()?"Read the Letters at the workshop desk.":"Don't forget the presents. Collect them at Gift Storage."):
                    gate.getDestination()==Area.Type.FOREST?"Talk to Toby and clear the village encounter first.":
                    !storyQuestManager.isRestorationComplete()?"Return Toby's gift and restore the Village first.":
                    storyQuestManager.isFrostFangAvailable()?"Defeat Frost Fang in the eastern clearing to open Christmas Town.":
                    !storyQuestManager.isCrystalQuestActive()&&!storyQuestManager.isChristmasTownLeadUnlocked()?"Return to Toby to investigate the remaining Black Frost.":"Investigate and break all three Black Frost Crystals to open this gate.");return false;
            }
            WorldMap.Point arrival = areaManager.arrivalFrom(source);
            santa.setPosition(arrival.x(), arrival.y());
            clearMovementInput();
            pendingAttack.set(null);
            areaMessage=null;areaMessageFrames=0;
            loadCurrentAreaContent();
            updateChildInteractions();
            updateCamera();
            if(source==Area.Type.SANTA_BASE){workshopInteractions.reset();notifyPlayer("The letter was right...\nThe Christmas lights are fading.\nI should check on everyone.");}
            return true;
        }
        return false;
    }

    private void finishSleighJourney(){
        endingCutscene.dismissPresentation();clearCutsceneInput();state=State.PLAYING;
        if(!areaManager.returnHome()){notifyPlayer("The journey home is not ready yet.");return;}
        var arrival=JourneyHomeVisuals.HOME_ARRIVAL;santa.setPosition(arrival.x(),arrival.y());
        dialogueManager.close();workshopInteractions.reset();loadCurrentAreaContent();updateChildInteractions();updateCamera();
        notifyPlayer("Home for Christmas - Read the Letters at the Letter Desk.");
    }

    private boolean isCurrentMainExitUnlocked() { return areaManager.isNextAreaAvailable(); }

    private int villageAtmosphereBrightness = 70;

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics = (Graphics2D) g.create();
        if(state==State.MAIN_MENU) {
            mainMenu.draw(graphics,getWidth(),getHeight());
            graphics.dispose();return;
        }
        updateCamera();

        if (endingCutscene.hasStarted()) {
            drawEnding(graphics);
            if(endingCutscene.isComplete())GameScreens.draw(graphics,getWidth(),getHeight(),endingCutscene.isHomeEnding()?"THE END":"CHRISTMAS LIVES ON",endingCutscene.isHomeEnding()?"Christmas was never truly lost.":"Christmas survives as long as hope remains.","RETURN TO MAIN MENU [ESC]");
            graphics.dispose();
            return;
        }
        Graphics2D screen = graphics;
        graphics = (Graphics2D) screen.create();
        int shake=!mainMenu.isReducedMotion()&&shakeFrames>0?(shakeFrames%2==0?2:-2):0;
        graphics.translate(-camera.getX()+shake, -camera.getY()-shake);
        boolean base=areaManager.getCurrentArea().getType()==Area.Type.SANTA_BASE;
        boolean town=areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN;
        if(base)baseVisuals.drawTerrain(graphics);
        else if(town)townVisuals.drawTerrain(graphics,treeTransformation.isRestored());
        else if(areaManager.getCurrentArea().getType()==Area.Type.FOREST) {
            forestVisuals.drawTerrain(graphics);
            for(StoryQuestTarget storyTarget:storyQuestManager.getTargets())forestVisuals.drawCollectedPatch(graphics,storyTarget);
        }
        else VillageRestorationVisuals.drawTerrain(graphics,villageActivities.chapter);
        if(areaManager.getCurrentArea().getType()==Area.Type.VILLAGE)FinalChapterVisuals.drawHouse(graphics,villageActivities.chapter);
        if(!base&&(!town||!treeTransformation.isRestored()))worldAtmosphere.drawDepth(graphics,areaManager.getCurrentArea().getType(),currentMap(),camera,getWidth(),getHeight());
        if(areaManager.getCurrentArea().getType()==Area.Type.FOREST)
            forestVisuals.drawGroundEffects(graphics,camera,getWidth(),getHeight(),worldAtmosphere.getTicks(),crystals);
        if(areaManager.getCurrentArea().getType()==Area.Type.FOREST) {
            for(var crystal:crystals)crystal.drawGround(graphics);
            for(var point:healingPoints)if(point.getArea()==Area.Type.FOREST)point.drawGround(graphics);
        }
        if(areaManager.getCurrentArea().getType()==Area.Type.FOREST)
            FinalChapterVisuals.drawMaterials(graphics,villageActivities.chapter,camera,getWidth(),getHeight());
        if(base)baseVisuals.drawLights(graphics,worldAtmosphere.getTicks(),camera,getWidth(),getHeight());
        if(base)workshopInteractions.drawResponse(graphics,worldAtmosphere.getTicks());
        if(base){VillageActivityVisuals.drawStorage(graphics,villageActivities);JourneyHomeVisuals.drawLetterDesk(graphics,villageActivities.chapter);}
        storyOverlay.drawAtmosphere(graphics,santa,currentMap(),camera,getWidth(),getHeight());
        if(town){
            townVisuals.drawGroundLights(graphics,treeTransformation.isRestored(),worldAtmosphere.getTicks());
            if(treeTransformation.isComplete())endingTownVisuals.drawGroundIllumination(graphics,currentMap().point("landmarks/Corrupted Town Center"),1,true);

        }
        if (areaManager.getCurrentArea().getType() == Area.Type.FOREST) {
            for (StoryQuestTarget storyTarget : storyQuestManager.getTargets()) {
                storyTarget.drawPickup(graphics);
            }
        }
        if (!endingCutscene.hasStarted()) for (ExitZone gate : areaManager.getCurrentExitZones()) {
            if(!base&&gate.getDestination()!=Area.Type.SANTA_BASE)gate.draw(graphics, areaManager.isConnectionAvailable(gate.getDestination()),
                    currentMap().width(), currentMap().height());
        }
        if (areaManager.getCurrentArea().getType() == Area.Type.FOREST) {
            StoryQuestTarget target = storyQuestManager.getTargetForActiveQuest();
            if (target != null) {
                target.drawMarker(graphics,
                        storyQuestManager.canInteractWithTarget(target, santa));
            }
        }
        BossArenaVisuals.draw(graphics,activeBoss,worldAtmosphere.getTicks(),town,treeTransformation.isRestored()||storyQuestManager.isBossDefeated(),this);
        if(encounterManager.getActive()!=null)encounterManager.getActive().drawWarning(graphics);
        boolean villageProps = areaManager.getCurrentArea().getType() == Area.Type.VILLAGE;
        if (villageProps) {
            VillageRestorationVisuals.drawMarket(graphics,villageActivities.chapter);

            VillageActivityVisuals.drawProps(graphics,villageActivities,worldAtmosphere.getTicks());
            Graphics2D worldGraphics = graphics;
            var rows = new java.util.ArrayList<VillageEnvironment.DrawRow>();
            rows.add(new VillageEnvironment.DrawRow(VillageRestorationVisuals.TREE_ANCHOR.y(),
                () -> VillageRestorationVisuals.drawTree(worldGraphics,villageActivities.chapter)));
            for (var prop : VillageEnvironment.props()) {
                boolean behind=santa.getBounds().getMaxY()<prop.y() && prop.bounds().intersects(santa.getBounds());
                if(areChildrenInCurrentArea()) for(Child child:children)
                    behind|=child.getBounds().getMaxY()<prop.y() && prop.bounds().intersects(child.getBounds());
                final boolean obscure=behind;
                rows.add(new VillageEnvironment.DrawRow(prop.y(), () -> {
                    prop.draw(worldGraphics,obscure);
                }));
            }
            if (areChildrenInCurrentArea()) for (Child child : children)
                rows.add(new VillageEnvironment.DrawRow((int)child.getBounds().getMaxY(), () -> child.draw(worldGraphics)));
            for (Enemy enemy : enemies)
                rows.add(new VillageEnvironment.DrawRow((int)enemy.getBounds().getMaxY(), () -> {
                    enemy.drawAttackWarning(worldGraphics);
                    enemy.draw(worldGraphics);
                    enemy.drawHitFeedback(worldGraphics);
                    drawEnemyHealth(worldGraphics, enemy);
                }));
            rows.add(new VillageEnvironment.DrawRow((int)santa.getBounds().getMaxY(), () -> {if(giftDelivery.isActive())santa.drawCinematic(worldGraphics,santa.getX()+16,santa.getY()+42,giftDelivery.facing());else santa.draw(worldGraphics);}));
            for(var site:VillageActivities.Site.values())if(site.resident())
                rows.add(new VillageEnvironment.DrawRow(site.y+21,()->entity.VillageResident.draw(worldGraphics,site,
                    site.recipient()&&villageActivities.delivered(site.ordinal()-3),giftDelivery.isActive()&&giftDelivery.recipient()==site?giftDelivery.reactionOffset():0)));
            VillageEnvironment.drawSorted(rows);giftDelivery.draw(graphics);
        } else {
        if (areChildrenInCurrentArea()) {
            for (Child child : children) {
                child.draw(graphics);
            }
        }
        for (Enemy enemy : enemies) {
            enemy.drawAttackWarning(graphics);
            if(enemy instanceof boss.BossCombatant&&storyOverlay.isActive()&&storyOverlay.isBossIntro())
                storyOverlay.drawBossEntrance(graphics,enemy,santa);
            else enemy.draw(graphics);
            StoryOverlay.drawBossTransition(graphics,enemy);
            if (!endingCutscene.hasStarted()) {
                enemy.drawHitFeedback(graphics);
                drawEnemyHealth(graphics, enemy);
            }
        }
        }
        if(encounterManager.getActive()!=null)encounterManager.getActive().drawSpawnForeground(graphics);
        if(areaManager.getCurrentArea().getType()==Area.Type.FOREST)for(var crystal:crystals)crystal.drawForeground(graphics);
        for(var point:healingPoints)if(point.getArea()==areaManager.getCurrentArea().getType()) {
            if(point.getArea()==Area.Type.FOREST)point.drawForeground(graphics,santa,encounterManager.getActive()!=null);
            else point.draw(graphics,santa,encounterManager.getActive()!=null);
        }
        if(areChildrenInCurrentArea()&&storyQuestManager.isGiftFound()&&!storyQuestManager.isLostGiftComplete()) {
            Child toby=children.get(0);
            if(toby.canInteract())drawStoryMarker(graphics,new WorldMap.Point(toby.getX()+15,toby.getY()+21),"[E] Return Toby's gift");
        }
        if(town)TownChapterArt.drawTree(graphics,currentMap().point("landmarks/Corrupted Town Center"),activeBoss,storyOverlay,
            storyQuestManager.isBossDefeated()||pendingBossVictory.equals("boss"),treeTransformation,worldAtmosphere.getTicks(),bossFall.isActive()?bossFall.getFrames():-1);
        bossFall.drawWorld(graphics);
        if (!endingCutscene.hasStarted()) abilitySystem.draw(graphics);
        combatVisuals.draw(graphics);
        if (areaManager.getCurrentArea().getType() == Area.Type.CHRISTMAS_TOWN&&!endingCutscene.isWorldVisible()&&endingCutscene.getState()!=EndingCutscene.State.SLEIGH_DEPARTURE)
            elias.draw(graphics, santa);
        if(town&&villageActivities.chapter.celebrated())JourneyHomeVisuals.drawSleigh(graphics,santa,-1);
        if (!villageProps) santa.draw(graphics);
        if (!endingCutscene.hasStarted()&&!bossFall.isActive()) drawPostBossInteractions(graphics);
        if (villageProps) VillageAtmosphere.draw(graphics, camera.getX(), camera.getY(),
                getWidth(), getHeight(), villageAtmosphereBrightness,villageActivities);
        if(!base&&!town&&areaManager.getCurrentArea().getType()!=Area.Type.FOREST)
            worldAtmosphere.drawLights(graphics,areaManager.getCurrentArea().getType(),camera,getWidth(),getHeight(),villageActivities.chapter);
        if(villageProps)VillageActivityVisuals.drawLights(graphics,villageActivities,worldAtmosphere.getTicks());
        if(villageProps){VillageRestorationVisuals.drawLights(graphics,villageActivities.chapter,worldAtmosphere.getTicks());restorationVisuals.drawResponse(graphics,worldAtmosphere.getTicks());}
        if(town&&villageActivities.chapter.celebrated()){TownCelebration.draw(graphics,endingCutscene.getVisualFrames(),children);JourneyHomeVisuals.drawParkedForeground(graphics,santa);}
        if(town)townVisuals.drawSnow(graphics,treeTransformation.isRestored(),worldAtmosphere.getTicks(),camera,getWidth(),getHeight());
        else if(base)baseVisuals.drawParticles(graphics,worldAtmosphere.getTicks(),camera,getWidth(),getHeight());
        else worldAtmosphere.drawSnow(graphics,areaManager.getCurrentArea().getType(),currentMap(),camera,getWidth(),getHeight());
        if(town)townVisuals.drawForeground(graphics,treeTransformation.isRestored(),worldAtmosphere.getTicks());
        graphics.dispose();
        graphics = screen;
        if (dialogueManager.isActive()) {
            dialogueBox.draw(graphics, dialogueManager.getActiveDialogue(),
                    getWidth(), getHeight());
            if(activityDialogueSite!=null&&activityDialogueSite.recipient()&&activityNotice&&areaMessageFrames>0&&areaMessage!=null)
                drawStoryNotification(graphics,areaMessage);
        } else if (isGameplayHudVisible()||state==State.PAUSED) {
            hud.draw(graphics, getWidth(), getHeight(), santa, spiritTree,
                    abilitySystem, areaManager, activeBoss,
                    areaMessageFrames > 0 ? areaMessage : null,
                    storyQuestManager, isCurrentMainExitUnlocked(),villageActivities.chapter,currentObjective());
            drawObjectiveDirection(graphics);
            if(activeBoss==null)areaTitle.draw(graphics,getWidth());
            if (areaMessageFrames > 0 && areaMessage != null) {
                drawStoryNotification(graphics, areaMessage);
            }
            if(base){
                var site=nearbyWorkshopSite();
                if(site==WorkshopInteractions.Site.GIFT_STORAGE)VillageActivityVisuals.drawStoragePrompt(graphics,site,camera,getWidth(),getHeight(),villageActivities);
                else if(site==WorkshopInteractions.Site.LETTER_DESK&&villageActivities.chapter.homeReturned())
                    FinalChapterVisuals.drawPrompt(graphics,"[E] Read the Letters",santa,camera,getWidth(),getHeight());
                else WorkshopInteractions.drawPrompt(graphics,site,camera,getWidth(),getHeight());
            }
            if(base||villageProps)VillageActivityVisuals.drawTravelPrompt(graphics,areaManager,santa,camera,getWidth(),getHeight());
            if(villageProps){
                var site=nearbyVillageActivity();
                if(site!=null&&!site.resident())FinalChapterVisuals.drawPrompt(graphics,"[E] "+(villageActivities.chapter.restored(site.restoration())?"Restoration complete":site.label),santa,camera,getWidth(),getHeight());
                else VillageActivityVisuals.drawPrompt(graphics,site,santa,camera,getWidth(),getHeight(),villageActivities);
            }
            if(town&&villageActivities.chapter.celebrated()&&FinalChapter.inReach(santa,currentMap(),JourneyHomeVisuals.SLEIGH_APPROACH))
                ChristmasUI.sleighPrompt(graphics,santa,camera,getWidth(),getHeight());
            if(areaManager.getCurrentArea().getType()==Area.Type.FOREST){
                var pickup=villageActivities.chapter.nearby(santa);
                if(pickup!=null)FinalChapterVisuals.drawPrompt(graphics,"[E] Collect "+pickup.material().label,santa,camera,getWidth(),getHeight());
            }
        }
        activityWindow.hover(skillMouse.x,skillMouse.y,getWidth(),getHeight());
        activityWindow.draw(graphics,getWidth(),getHeight(),villageActivities);
        if (storyOverlay.isActive()) storyOverlay.draw(graphics, getWidth(), getHeight());
        if(state==State.GAME_OVER)GameScreens.draw(graphics,getWidth(),getHeight(),"GAME OVER","The last light has not gone out. Try again.","RETRY ENCOUNTER [R]","MAIN MENU [ESC]");
        if(state==State.PAUSED)GameScreens.drawPause(graphics,getWidth(),getHeight(),skillMouse);
        bossFall.drawScreen(graphics,getWidth(),getHeight());
        if(treeTransformation.isActive()){
            graphics.setFont(new Font("Dialog",Font.BOLD,13));graphics.setColor(new Color(255,226,164));
            graphics.drawString("Christmas Spirit returning... "+treeTransformation.getFrames()+" / 180",getWidth()/2-140,115);
        }
        if (santa.isDeveloperTestMode() && !bossFall.isActive() && !endingCutscene.hasStarted()) {
            graphics.setColor(new Color(0, 0, 0, 170));
            graphics.fillRoundRect(12, 82, 126, 22, 6, 6);
            graphics.setColor(new Color(255, 224, 111));
            graphics.drawString("DEV TEST MODE", 20, 98);
        }
        graphics.dispose();
    }

    /** Optional authored outcome/credits after restoration; no repeated transformation or rewards. */
    private void drawEnding(Graphics2D screen) {
        if(endingCutscene.getState()==EndingCutscene.State.HOME_FADE){
            Graphics2D world=(Graphics2D)screen.create();world.translate(-camera.getX(),-camera.getY());
            baseVisuals.drawTerrain(world);baseVisuals.drawLights(world,worldAtmosphere.getTicks(),camera,getWidth(),getHeight());
            VillageActivityVisuals.drawStorage(world,villageActivities);JourneyHomeVisuals.drawLetterDesk(world,villageActivities.chapter);santa.draw(world);world.dispose();
        }
        if(endingCutscene.isWorldVisible()||endingCutscene.getState()==EndingCutscene.State.SLEIGH_DEPARTURE) {
            Graphics2D world=(Graphics2D)screen.create();world.translate(-camera.getX(),-camera.getY());
            WorldMap.Point tree=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
            townVisuals.drawTerrain(world,true);
            endingTownVisuals.drawGroundIllumination(world,tree,endingCutscene.hopeProgress(),true);
            townVisuals.drawGroundLights(world,true,endingCutscene.getVisualFrames());
            townVisuals.drawTree(world,true);
            endingCutscene.drawHope(world);
            if(endingCutscene.isCelebration()||endingCutscene.getState()==EndingCutscene.State.SLEIGH_DEPARTURE){
                TownCelebration.draw(world,endingCutscene.getVisualFrames(),children,endingCutscene.isCelebration());
                elias.drawCinematic(world,JourneyHomeVisuals.ELIAS_FEET.x(),JourneyHomeVisuals.ELIAS_FEET.y(),entity.Elias.Direction.RIGHT);
                world.setFont(new Font("Dialog",Font.BOLD,12));world.setColor(new Color(255,216,128));
                world.drawString("Elias",JourneyHomeVisuals.ELIAS_FEET.x()-14,JourneyHomeVisuals.ELIAS_FEET.y()-65);
                if(endingCutscene.isCelebration())santa.drawCinematic(world,santa.getX()+16,santa.getY()+42,Santa.Direction.UP);
                else if(endingCutscene.getFrames()<EndingCutscene.SLEIGH_FLIGHT_END)JourneyHomeVisuals.drawSleigh(world,santa,endingCutscene.getFrames());
            }else endingTownVisuals.drawActors(world,endingCutscene,santa,children,elias);
            townVisuals.drawForeground(world,true,endingCutscene.getVisualFrames());world.dispose();
        }
        endingCutscene.drawScreen(screen,getWidth(),getHeight(),dialogueBox);
    }

    private void drawStoryNotification(Graphics2D g, String message) {
        ChristmasUI.notification(g,message,getWidth(),getHeight(),dialogueManager.isActive(),
                new java.awt.Rectangle(santa.getX()-camera.getX()-35,santa.getY()-camera.getY()-55,102,110));
    }
    private QuestGuidance.Objective currentObjective(){
        return QuestGuidance.resolve(storyQuestManager,villageActivities,areaManager.getCurrentArea().getType(),santa,encounterManager,crystals);
    }
    private void drawObjectiveDirection(Graphics2D g) {
        var objective=currentObjective();WorldMap.Point target=objective.target();String label=objective.targetLabel();
        Area.Type area=areaManager.getCurrentArea().getType();
        if(target==null)return;
        int tx=target.x()-camera.getX(),ty=target.y()-camera.getY();
        if(tx>=35&&tx<getWidth()-35&&ty>=140&&ty<getHeight()-90)return;
        int px=Math.max(55,Math.min(getWidth()-55,tx)),py=Math.max(150,Math.min(getHeight()-90,ty));
        if(area==Area.Type.SANTA_BASE)py=Math.min(py,getHeight()-160);
        var questBox=HUD.questBounds(objective,villageActivities.chapter,getWidth(),getHeight());
        var marker=ChristmasUI.directionPosition(px,py,questBox,getWidth(),getHeight(),
                new java.awt.Rectangle(santa.getX()-camera.getX()-35,santa.getY()-camera.getY()-55,102,110));
        px=marker.x;py=marker.y;
        double a=Math.atan2(ty-py,tx-px);
        Graphics2D arrow=(Graphics2D)g.create();arrow.translate(px,py);arrow.rotate(a);
        arrow.setColor(new Color(255,224,140));arrow.fillPolygon(new int[]{12,-6,-6},new int[]{0,-7,7},3);arrow.dispose();
        g.setFont(new Font("Dialog",Font.BOLD,11));int textW=g.getFontMetrics().stringWidth(label);
        int textX=Math.max(6,Math.min(getWidth()-textW-6,px-textW/2));
        g.setColor(new Color(8,14,24,220));g.fillRoundRect(textX-4,py+12,textW+8,20,6,6);
        g.setColor(new Color(255,224,140));g.drawString(label,textX,py+26);
    }

    private void drawEnemyHealth(Graphics2D graphics, Enemy enemy) {
        if(enemy instanceof boss.BossCombatant)return; // The active boss has one screen-space bar.
        if (enemy.getHealth() >= enemy.getMaximumHealth()) {
            return;
        }
        int x = (int) enemy.getX();
        int y = (int) Math.floor(enemy.getHealthBarY());
        int width = enemy.getSize();
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(x, y, width, 4);
        graphics.setColor(new Color(230, 75, 71));
        graphics.fillRect(x, y, width * enemy.getHealth()
                / enemy.getMaximumHealth(), 4);
    }

    @Override
    public synchronized void keyPressed(KeyEvent event) {
        // Latch even blocked presses, so holding a key through a modal cannot cast on dismissal.
        int skillKey=event.getKeyCode();
        if(skillKey==KeyEvent.VK_1){if(bellKeyDown)return;bellKeyDown=true;}
        if(skillKey==KeyEvent.VK_2){if(starKeyDown)return;starKeyDown=true;}
        if(skillKey==KeyEvent.VK_3){if(bombKeyDown)return;bombKeyDown=true;}
        if(state==State.MAIN_MENU){mainMenu.keyPressed(event.getKeyCode());return;}
        if(state==State.PAUSED){
            if(event.getKeyCode()==KeyEvent.VK_ESCAPE&&!pauseKeyDown){pauseKeyDown=true;pendingScreenAction.set(5);}
            if(event.getKeyCode()==KeyEvent.VK_ENTER)pendingScreenAction.set(5);return;
        }
        if(state==State.PLAYING&&giftDelivery.isActive()){
            if(event.getKeyCode()==KeyEvent.VK_E&&!interactionKeyDown){interactionKeyDown=true;deliverySkip=true;}
            if(event.getKeyCode()==KeyEvent.VK_ESCAPE&&!pauseKeyDown){pauseKeyDown=true;deliverySkip=true;}return;
        }
        if(state==State.GAME_OVER){if(event.getKeyCode()==KeyEvent.VK_R)pendingScreenAction.set(2);if(event.getKeyCode()==KeyEvent.VK_ESCAPE)pendingScreenAction.set(3);return;}
        if(activityWindow.isOpen()){
            if(event.getKeyCode()==KeyEvent.VK_ESCAPE&&!pauseKeyDown){pauseKeyDown=true;pendingActivityClose.set(true);}
            if(event.getKeyCode()==KeyEvent.VK_E){if(!interactionKeyDown){interactionKeyDown=true;pendingActivityKey.set(KeyEvent.VK_E);}}
            else if(event.getKeyCode()==KeyEvent.VK_LEFT||event.getKeyCode()==KeyEvent.VK_RIGHT||event.getKeyCode()==KeyEvent.VK_ENTER)pendingActivityKey.set(event.getKeyCode());
            if(event.getKeyCode()==KeyEvent.VK_SPACE)dashKeyDown=true;
            if(event.getKeyCode()==KeyEvent.VK_1)bellKeyDown=true;
            if(event.getKeyCode()==KeyEvent.VK_F1)developerTestKeyDown=true;
            return;
        }
        if(bossFall.isActive()){if(event.getKeyCode()==KeyEvent.VK_E&&!interactionKeyDown){interactionKeyDown=true;pendingScreenAction.set(9);}if(event.getKeyCode()==KeyEvent.VK_ESCAPE&&!pauseKeyDown){pauseKeyDown=true;pendingScreenAction.set(9);}return;}
        if(treeTransformation.isActive()&&(event.getKeyCode()==KeyEvent.VK_E||event.getKeyCode()==KeyEvent.VK_1
            ||event.getKeyCode()==KeyEvent.VK_2||event.getKeyCode()==KeyEvent.VK_3||event.getKeyCode()==KeyEvent.VK_C))return;
        if(endingCutscene.hasStarted()){
            if(event.getKeyCode()==KeyEvent.VK_ESCAPE)pendingScreenAction.set(endingCutscene.isComplete()?3:6);
            if(event.getKeyCode()==KeyEvent.VK_E&&!interactionKeyDown){interactionKeyDown=true;pendingScreenAction.set(8);}
            return;
        }
        if(state!=State.PLAYING)return;
        if(event.getKeyCode()==KeyEvent.VK_C&&villageActivities.chapter.letterFinished()&&!dialogueManager.isActive()&&!storyOverlay.isActive()){pendingScreenAction.set(7);return;}
        if(event.getKeyCode()==KeyEvent.VK_ESCAPE&&!dialogueManager.isActive()&&!storyOverlay.isActive()){
            if(!pauseKeyDown){pauseKeyDown=true;pendingScreenAction.set(4);}return;
        }
        if (event.getKeyCode() == KeyEvent.VK_F1) {
            if (!developerTestKeyDown) {
                developerTestKeyDown = true;
                pendingDeveloperTestToggles.incrementAndGet();
            }
            return;
        }
        if (event.getKeyCode() == KeyEvent.VK_E) {
            if (!interactionKeyDown) {
                interactionKeyDown = true;
                if (dialogueManager.isActive() || storyOverlay.isActive()) {
                    pendingDialogueAdvance.set(true);
                } else {
                    pendingInteraction.set(true);
                }
            }
            return;
        }
        if (dialogueManager.isActive() || storyOverlay.isActive()) {
            return;
        }

        if(event.getKeyCode()==KeyEvent.VK_1){pendingAbility.getAndUpdate(v->v|1);return;}
        if(event.getKeyCode()==KeyEvent.VK_2){pendingAbility.getAndUpdate(v->v|2);return;}
        if(event.getKeyCode()==KeyEvent.VK_3){pendingAbility.getAndUpdate(v->v|4);return;}
        if(event.getKeyCode()==KeyEvent.VK_SPACE){if(!dashKeyDown){dashKeyDown=true;pendingDash.set(true);}return;}
        switch (event.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                upPressed = true;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                downPressed = true;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                leftPressed = true;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                rightPressed = true;
                break;
            default:
                break;
        }
    }

    @Override
    public synchronized void keyReleased(KeyEvent event) {
        if(event.getKeyCode()==KeyEvent.VK_1)bellKeyDown=false;
        if(event.getKeyCode()==KeyEvent.VK_2)starKeyDown=false;
        if(event.getKeyCode()==KeyEvent.VK_3)bombKeyDown=false;
        if(state==State.MAIN_MENU){mainMenu.keyReleased(event.getKeyCode());return;}
        if(event.getKeyCode()==KeyEvent.VK_ESCAPE){pauseKeyDown=false;return;}
        if(event.getKeyCode()==KeyEvent.VK_SPACE){dashKeyDown=false;return;}
        if(event.getKeyCode()==KeyEvent.VK_1){bellKeyDown=false;return;}
        if(event.getKeyCode()==KeyEvent.VK_2){starKeyDown=false;return;}
        if(event.getKeyCode()==KeyEvent.VK_3){bombKeyDown=false;return;}
        if (event.getKeyCode() == KeyEvent.VK_F1) {
            developerTestKeyDown = false;
            return;
        }
        if (event.getKeyCode() == KeyEvent.VK_E) {
            interactionKeyDown = false;
            return;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                upPressed = false;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                downPressed = false;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                leftPressed = false;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                rightPressed = false;
                break;
            default:
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
        // Movement uses keyPressed and keyReleased for held-key input.
    }
}


