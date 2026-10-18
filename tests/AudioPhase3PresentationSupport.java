/** Removes exactly the Phase 3 audio hooks, retaining historical whole-source gameplay guards. */
final class AudioPhase3PresentationSupport {
    private AudioPhase3PresentationSupport() { }
    static String withoutPhase3(String source) {
        return source.replace("import audio.AudioCatalog.Ambience;\n", "")
            .replace("gameplayAudio=new audio.GameplayAudio(effectPlayback==null?audio::playEffect:(effect,gain)->effectPlayback.accept(effect));",
                     "gameplayAudio=new audio.GameplayAudio(effectPlayback==null?audio::playEffect:effectPlayback);")
            .replace(ATMOSPHERE, "")
            .replace("gameplayAudio.playWorld(CRYSTAL_BREAK,santa,crystal.getPosition().x(),crystal.getPosition().y());", "gameplayAudio.play(CRYSTAL_BREAK);");
    }
    private static final String ATMOSPHERE="""
        audio.playAmbience(state==State.MAIN_MENU?null:switch(areaManager.getCurrentArea().getType()) {
            case VILLAGE -> Ambience.VILLAGE;
            case FOREST -> Ambience.FOREST;
            case CHRISTMAS_TOWN -> Ambience.TOWN;
        });
        audio.setDialogueActive(state!=State.MAIN_MENU&&(dialogueManager.isActive()||storyOverlay.isActive()));
        Encounter active=encounterManager.getActive();
        audio.setCombatActive(state==State.PLAYING&&track!=Music.BOSS&&track!=Music.ENDING
                &&active!=null&&active.getState()==Encounter.State.ACTIVE);
""";
}
