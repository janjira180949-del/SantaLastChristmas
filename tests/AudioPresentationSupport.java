/** Removes only the explicit audio observer/lifecycle additions for historical source guards. */
final class AudioPresentationSupport {
    private AudioPresentationSupport() { }
    static String withoutAudio(String source) {
        source=AudioPhase2PresentationSupport.withoutPhase2("src/game/GamePanel.java",source);
        return source.replace("import audio.AudioCatalog.Music;\n", "")
            .replace("    private final audio.AudioManager audio=new audio.AudioManager();\n    private final CinematicMenu mainMenu=new CinematicMenu(audio);", "    private final CinematicMenu mainMenu=new CinematicMenu();")
            .replace("        syncAudio();\n", "")
            .replace("        audio.start();syncAudio();\n", "")
            .replace("gameThread.interrupt();audio.close();", "gameThread.interrupt();")
            .replace(OBSERVER,"    private synchronized void update() {\n");
    }
    private static final String OBSERVER="""
    private synchronized void update() {
        try {updateFrame();} finally {syncAudio();}
    }
    private void syncAudio() {
        Music track;
        if(state==State.MAIN_MENU)track=Music.MENU;
        else if(treeTransformation.isActive()||endingCutscene.hasStarted())track=Music.ENDING;
        else if(christmasEater!=null&&!christmasEater.isDefeated()&&!storyQuestManager.isBossDefeated())track=Music.BOSS;
        else track=switch(areaManager.getCurrentArea().getType()) {
            case VILLAGE -> Music.VILLAGE;
            case FOREST -> Music.FOREST;
            case CHRISTMAS_TOWN -> Music.TOWN;
        };
        audio.playMusic(track);
    }
    private void updateFrame() {
""";
}
