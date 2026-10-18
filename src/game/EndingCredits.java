package game;

import java.util.List;

/** Approved developer roles and credits verified against active runtime audio provenance. */
public final class EndingCredits {
    private EndingCredits() { }
    public record Entry(String role, String credit) { }
    public static final List<Entry> ENTRIES = List.of(
        new Entry("DEVELOPED BY", "JANJIRA WANGKAOOM"),
        new Entry("COURSE", "Object Oriented Programming"),
        new Entry("PROGRAMMING", "JANJIRA WANGKAOOM\nOpenAI Codex"),
        new Entry("GAME DESIGN", "JANJIRA WANGKAOOM"),
        new Entry("STORY & NARRATIVE DESIGN", "JANJIRA WANGKAOOM"),
        new Entry("ART & VISUAL ASSETS", "ChatGPT by OpenAI"),
        new Entry("MUSIC & SOUND", "CC0 1.0 Universal\nSources: OpenGameArt, Freesound, BigSoundBank"),
        new Entry("Original Menu - Wintery loop", "Emma_MA (Emma M. Andersson)"),
        new Entry("Fantasy Song Pack Volume 1", "troubadour"),
        new Entry("JRPG Packs 1 Exploration / 2 Towns / 3 Evil / 5 Action", "Juhani Junkala (SubspaceAudio)"),
        new Entry("Battle Theme A", "cynicmusic\ncynicmusic.com / pixelsphere.org"),
        new Entry("Epic Fight Theme/Boss Battle", "Tarush Singhal"),
        new Entry("Metal / Wood, RPG, Creature, Breaking / Falling / Hit", "rubberduck"),
        new Entry("Magic Spell SFX", "JaggedStone"),
        new Entry("Snow / Gravel and Stone Footsteps", "Corsica_S / Fantozzi\nStep extraction: Iwan Gabovitch (qubodup)"),
        new Entry("Steps in wood floor", "mikeask"),
        new Entry("wind1", "Luke.RUSTLTD"),
        new Entry("The Shop", "LEGIT Audio"),
        new Entry("Fireplace Sound loop", "PagDev"),
        new Entry("Cloche 1h (recording 3446)", "Joseph SARDIN & Axeline T."),
        new Entry("Muffled Distant Explosion", "NenadSimic"),
        new Entry("Glass Break", "Till Behrend\nSubmitted by TinyWorlds"),
        new Entry("Swishes Sound Pack", "artisticdude"),
        new Entry("Sleigh bells 90 bpm loop", "soundstack"),
        new Entry("santa - ho ho ho #2", "canucklovingbrit"),
        new Entry("Fast Sword Swing / Explosion - Freesound 60004 / 442958", "qubodup"),
        new Entry("Achievement Sparkle - Freesound 715067", "SkySpeira"),
        new Entry("Zombie Hit 1 / Zombie Pain 4 - Freesound 555420 / 555421", "tonsil5"),
        new Entry("Monster Yell, Pain or Growl - Freesound 728185", "PaulzSoundz"),
        new Entry("Didgeridoo Monster Roar - Freesound 204912", "Noxdl"),
        new Entry("Audio sources and license evidence", "docs/FINAL_STORY_AUDIO_CREDITS.md\nCC0 1.0: creativecommons.org/publicdomain/zero/1.0/"),
        new Entry("SPECIAL THANKS", "ChatGPT\nOpenAI Codex\nFor assisting with game development,\ncreative design, and\ntechnical implementation."),
        new Entry("THANK YOU FOR PLAYING!", "Every gift carries a little hope.\nEvery act of kindness keeps Christmas alive.\n\nMay your days be filled with warmth,\njoy, and a little Christmas magic.\n\nMERRY CHRISTMAS!")
    );
    public static int entryHeight(Entry entry){return 108+26*(entry.credit().split("\\n",-1).length-1);}
    public static int scrollHeight(){return ENTRIES.stream().mapToInt(EndingCredits::entryHeight).sum();}
    /** Compact menu retains its original seven rows; complete credits scroll at the ending. */
    public static final List<Entry> MENU_ENTRIES = List.of(
        ENTRIES.get(0),ENTRIES.get(1),new Entry("PROGRAMMING","JANJIRA WANGKAOOM / OpenAI Codex"),
        ENTRIES.get(3),ENTRIES.get(4),ENTRIES.get(5),
        new Entry("MUSIC & SOUND","CC0; full credits at journey's end")
    );
}
