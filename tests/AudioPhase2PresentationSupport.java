import java.nio.charset.StandardCharsets;

/** Exact authorized additions only; historical whole-source hashes still guard gameplay. */
final class AudioPhase2PresentationSupport {
    private AudioPhase2PresentationSupport() { }
    static byte[] originalBytes(String path,byte[] bytes) {
        bytes=OverhaulPreservationSupport.previous(path,bytes);
        if(!path.equals("src/entity/Enemy.java")&&!path.equals("src/boss/ChristmasEater.java")&&!path.equals("src/game/GamePanel.java"))return bytes;
        // All three audited pre-edit owners used CRLF. Restore that byte representation too.
        return withoutPhase2(path,new String(bytes,StandardCharsets.UTF_8).replace("\r\n","\n"))
            .replace("\n","\r\n").getBytes(StandardCharsets.UTF_8);
    }
    static String withoutPhase2(String path,String source) {
        if(path.equals("src/entity/Enemy.java"))return source
            .replace("    // Presentation sequence only; never participates in combat calculations.\n    private long attackEventSequence;\n    public final long getAttackEventSequence() { return attackEventSequence; }\n    protected final void notifyAttackExecuted() { attackEventSequence++; }\n", "")
            .replace("            notifyAttackExecuted();\n", "");
        if(path.equals("src/boss/ChristmasEater.java"))return source.replace("                notifyAttackExecuted();\n", "");
        if(!path.equals("src/game/GamePanel.java"))return source;
        source=AudioPhase3PresentationSupport.withoutPhase3(source);
        return source.replace("import static audio.AudioCatalog.Effect.*;\n", "")
            .replace("    private final audio.GameplayAudio gameplayAudio;\n", "")
            .replace("    public GamePanel() {\n        this(null);\n    }\n    /** Optional presentation sink for deterministic event tests without an audio device. */\n    public GamePanel(java.util.function.Consumer<audio.AudioCatalog.Effect> effectPlayback) {\n        gameplayAudio=new audio.GameplayAudio(effectPlayback==null?audio::playEffect:effectPlayback);", "    public GamePanel() {")
            .replace("        gameplayAudio.reset();\n", "")
            .replace("gameplayAudio.resolveCombat();gameplayAudio.observeQuests(storyQuestManager);syncAudio();", "syncAudio();")
            .replace("            gameplayAudio.restorationTick(treeTransformation.getFrames());\n", "")
            .replace("if(pendingDash.getAndSet(false)&&santa.startDash(upPressed,downPressed,leftPressed,rightPressed))gameplayAudio.play(DASH);", "if(pendingDash.getAndSet(false))santa.startDash(upPressed,downPressed,leftPressed,rightPressed);")
            .replace("        gameplayAudio.captureCombat(santa,enemies);\n", "")
            .replace("if(santa.startAttack())gameplayAudio.play(SANTA_ATTACK);", "santa.startAttack();")
            .replace("if(pendingAbility.getAndSet(0)==1&&abilitySystem.use(1,santa,enemies,solids))gameplayAudio.play(CHRISTMAS_BELL);", "if(pendingAbility.getAndSet(0)==1)abilitySystem.use(1,santa,enemies,solids);")
            .replace("                gameplayAudio.play(CRYSTAL_BREAK);\n", "")
            .replace("else if(point.use(santa,false)){gameplayAudio.play(HEALING_SHRINE);notifyPlayer(\"HP Restored\");}", "else if(point.use(santa,false))notifyPlayer(\"HP Restored\");")
            .replace("            gameplayAudio.play(NPC_INTERACTION);\n", "")
            .replace("if(dialogueManager.startFor(child,santa,storyQuestManager)){gameplayAudio.play(NPC_INTERACTION);clearCutsceneInput();return true;}", "if(dialogueManager.startFor(child,santa,storyQuestManager)){clearCutsceneInput();return true;}")
            .replace("treeTransformation.start();gameplayAudio.restorationTick(0);", "treeTransformation.start();")
            .replace("            gameplayAudio.play(GIFT_PICKUP);\n", "");
    }
}
