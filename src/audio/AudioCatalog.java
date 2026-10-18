package audio;

import java.util.*;
/** Phase10.6 runtime routes, generated from the authenticated delivery manifest. */
public final class AudioCatalog {
    private AudioCatalog() { }
    public enum Music {
        MENU("assets/audio/runtime/music/wintery_loop.wav"),
        BASE("assets/audio/phase10_6/music/homestead__loop.wav"),
        VILLAGE("assets/audio/phase10_6/music/town2_where_time_stands_still__loop.wav"),
        VILLAGE_RESTORED("assets/audio/phase10_6/music/springly_sprigs__loop.wav"),
        FOREST("assets/audio/phase10_6/music/wandering_woodlands__loop.wav"),
        FOREST_MATERIALS("assets/audio/phase10_6/music/back_to_nature__loop.wav"),
        FOREST_INVESTIGATION("assets/audio/phase10_6/music/exploration4_prairie_nights__loop.wav"),
        COMBAT("assets/audio/phase10_6/music/action3_preparing_for_battle__loop.wav"),
        FANG_INTRO("assets/audio/phase10_6/music/evil5_whispers_from_beyond__loop.wav"),
        FANG1("assets/audio/phase10_6/music/fang_battle_phase_1.wav"),
        FANG2("assets/audio/phase10_6/music/fang_battle_phase_2.wav"),
        FANG3("assets/audio/phase10_6/music/fang_battle_phase_3.wav"),
        TOWN("assets/audio/phase10_6/music/evil2_catacombs__loop.wav"),
        TITAN_INTRO("assets/audio/phase10_6/music/evil1_ancient_evil_awakens__loop.wav"),
        BOSS("assets/audio/phase10_6/music/titan_battle_phase_1.wav"),
        TITAN2("assets/audio/phase10_6/music/titan_battle_phase_2.wav"),
        TITAN3("assets/audio/phase10_6/music/titan_battle_phase_3.wav"),
        TREE("assets/audio/phase10_6/music/fairy_lights__loop.wav"),
        ENDING("assets/audio/phase10_6/music/snow_day__loop.wav"),
        SLEIGH("assets/audio/phase10_6/music/jaunt__loop.wav"),
        TOWN_PEACEFUL("assets/audio/phase10_6/music/town1_home_town__loop.wav"),
        HOME("assets/audio/phase10_6/music/homestead__loop.wav"),
        CREDITS("assets/audio/phase10_6/music/snow_day__loop.wav"),
        END("assets/audio/phase10_6/music/town1_home_town__loop.wav"),
        GAME_OVER("assets/audio/phase10_6/music/evil5_whispers_from_beyond__loop.wav");
        private final String path;
        Music(String path){this.path=path;}
        public String path(){return path;}
    }
    public enum Ambience {
        BASE("assets/audio/phase10_6/ambience/base.wav"),VILLAGE("assets/audio/phase10_6/ambience/village_frozen.wav"),VILLAGE_RESTORED("assets/audio/phase10_6/ambience/village_restored.wav"),FOREST("assets/audio/phase10_6/ambience/forest.wav"),TOWN("assets/audio/phase10_6/ambience/town_corrupted.wav"),TOWN_PEACEFUL("assets/audio/phase10_6/ambience/town_peaceful.wav");
        private final String path;
        Ambience(String path){this.path=path;}
        public String path(){return path;}
    }
    public enum Effect {
        HOVER("ui.hover",1,1,false),
        CLICK("ui.click",1,1,false),
        START("ui.start",1,1,false),
        BACK("ui.back",1,1,false),
        SETTINGS("ui.settings",1,1,false),
        CONFIRM("ui.confirm",1,1,false),
        SANTA_ATTACK("santa.swing.v01",0.32,3,true),
        ENEMY_HIT("walker.hurt.v01",0.27,2,true),
        SANTA_DAMAGE("santa.damage.v01",0.65,5,true),
        ENEMY_DEFEAT("walker.defeat.v01",0.34,2,true),
        DASH("santa.dash.v01",0.45,2,true),
        CHRISTMAS_BELL("bell.release",0.55,4,true),
        CRYSTAL_BREAK("crystal.break.v01",0.55,4,true),
        QUEST_COMPLETE("quest.complete",0.6,2,true),
        GIFT_PICKUP("pickup.lost_gift",0.9,2,true),
        HEALING_SHRINE("shrine.heal",0.85,2,true),
        NPC_INTERACTION("npc.entry",0.9,2,true),
        BOSS_ATTACK("titan.slam.execute",0.65,2,true),
        BOSS_DAMAGE("titan.hurt.v01",0.45,2,true),
        BOSS_PHASE_TRANSITION("titan.phase2",0.65,4,true),
        BOSS_DEFEAT("titan.collapse",1,4,true),
        SPIRIT_TREE_RESTORATION("tree.start",0.45,2,true),
        SPIRIT_TREE_RISING("tree.rise",0.7,2,true),
        SPIRIT_TREE_BURST("tree.burst",0.7,2,true),
        SPIRIT_TREE_CHIME("tree.chime",0.5,2,true);
        private final String id; private final double gain; private final int priority; private final boolean gameplay;
        Effect(String id,double gain,int priority,boolean gameplay){this.id=id;this.gain=gain;this.priority=priority;this.gameplay=gameplay;}
        public String path(){return cue(id).path();}
        public String eventId(){return id;}
        public double gain(){return gain;}
        public int priority(){return priority;}
        public boolean isGameplay(){return gameplay;}
    }
    public record Cue(String id,String path,double gain,int priority,boolean gameplay,boolean loop) { }
    private static final Map<String,Cue> CUES=new LinkedHashMap<>();
    private static final Map<String,Cue[]> FAMILIES=new HashMap<>();
    static {
        add("ui.hover","assets/audio/phase10_6/sfx/foley__wood_cracking_01.wav",0.2,2,false,false);
        add("ui.click","assets/audio/phase10_6/sfx/rpg__item_wood_02.wav",0.2,2,false,false);
        add("ui.start","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.2,2,false,false);
        add("ui.back","assets/audio/phase10_6/sfx/rpg__book_02.wav",0.2,2,false,false);
        add("ui.settings","assets/audio/phase10_6/sfx/foley__keys_02.wav",0.2,2,false,false);
        add("ui.confirm","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.2,2,false,false);
        add("ui.modal_open","assets/audio/phase10_6/sfx/rpg__book_01.wav",0.2,2,false,false);
        add("ui.modal_close","assets/audio/phase10_6/sfx/rpg__book_02.wav",0.2,2,false,false);
        add("ui.page","assets/audio/phase10_6/sfx/rpg__book_03.wav",0.2,2,false,false);
        add("ui.unavailable","assets/audio/phase10_6/sfx/rpg__lock_03.wav",0.2,2,false,false);
        add("ui.pause","assets/audio/phase10_6/sfx/rpg__book_01.wav",0.2,2,false,false);
        add("ui.resume","assets/audio/phase10_6/sfx/rpg__book_02.wav",0.2,2,false,false);
        add("ui.retry","assets/audio/phase10_6/sfx/rpg__lock_01.wav",0.2,2,false,false);
        add("santa.swing.v01","assets/audio/phase10_6/sfx/rpg__blade_01.wav",1,2,true,false);
        add("santa.swing.v02","assets/audio/phase10_6/sfx/rpg__blade_02.wav",1,2,true,false);
        add("santa.swing.v03","assets/audio/phase10_6/sfx/rpg__blade_03.wav",1,2,true,false);
        add("santa.dash.v01","assets/audio/phase10_6/sfx/rpg__blade_02.wav",0.5,2,true,false);
        add("santa.dash.v02","assets/audio/phase10_6/sfx/rpg__blade_03.wav",0.5,2,true,false);
        add("santa.dash.v03","assets/audio/phase10_6/sfx/rpg__blade_01.wav",0.5,2,true,false);
        add("santa.damage.v01","assets/audio/phase10_6/sfx/rpg__item_stone_02.wav",0.6,5,false,false);
        add("santa.damage.v02","assets/audio/phase10_6/sfx/rpg__item_stone_03.wav",0.6,5,false,false);
        add("santa.damage.v03","assets/audio/phase10_6/sfx/rpg__item_stone_04.wav",0.6,5,false,false);
        add("santa.defeat","assets/audio/phase10_6/sfx/magic__magical_6_0.wav",1,5,false,false);
        add("footstep.snow.v01","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_and_ice_01.wav",0.55,0,true,false);
        add("footstep.snow.v02","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_and_ice_02.wav",0.55,0,true,false);
        add("footstep.snow.v03","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_and_ice_03.wav",0.55,0,true,false);
        add("footstep.snow.v04","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_and_ice_04.wav",0.55,0,true,false);
        add("footstep.gravel.v01","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_01.wav",0.55,0,true,false);
        add("footstep.gravel.v02","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_02.wav",0.55,0,true,false);
        add("footstep.gravel.v03","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_03.wav",0.55,0,true,false);
        add("footstep.gravel.v04","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_04.wav",0.55,0,true,false);
        add("footstep.stone.v01","assets/audio/phase10_6/sfx/footstep_stone_01.wav",0.55,0,true,false);
        add("footstep.stone.v02","assets/audio/phase10_6/sfx/footstep_stone_02.wav",0.55,0,true,false);
        add("footstep.stone.v03","assets/audio/phase10_6/sfx/footstep_stone_03.wav",0.55,0,true,false);
        add("footstep.stone.v04","assets/audio/phase10_6/sfx/footstep_stone_04.wav",0.55,0,true,false);
        add("footstep.wood.v01","assets/audio/phase10_6/sfx/footstep_wood_01.wav",0.55,0,true,false);
        add("footstep.wood.v02","assets/audio/phase10_6/sfx/footstep_wood_02.wav",0.55,0,true,false);
        add("footstep.wood.v03","assets/audio/phase10_6/sfx/footstep_wood_03.wav",0.55,0,true,false);
        add("footstep.wood.v04","assets/audio/phase10_6/sfx/footstep_wood_04.wav",0.55,0,true,false);
        add("walker.warning","assets/audio/phase10_6/sfx/creature__grunt_01.wav",0.6,4,true,false);
        add("walker.strike","assets/audio/phase10_6/sfx/rpg__blade_01.wav",0.6,2,true,false);
        add("walker.hurt.v01","assets/audio/phase10_6/sfx/creature__hurt_01.wav",0.7,2,true,false);
        add("walker.hurt.v02","assets/audio/phase10_6/sfx/creature__hurt_02.wav",0.7,2,true,false);
        add("walker.hurt.v03","assets/audio/phase10_6/sfx/creature__hurt_03.wav",0.7,2,true,false);
        add("walker.defeat.v01","assets/audio/phase10_6/sfx/rpg__creature_die_01.wav",0.7,2,true,false);
        add("walker.defeat.v02","assets/audio/phase10_6/sfx/rpg__creature_roar_01.wav",0.7,2,true,false);
        add("runner.warning","assets/audio/phase10_6/sfx/creature__grunt_03.wav",0.6,4,true,false);
        add("runner.strike","assets/audio/phase10_6/sfx/rpg__blade_03.wav",0.6,2,true,false);
        add("runner.hurt.v01","assets/audio/phase10_6/sfx/creature__hurt_03.wav",0.7,2,true,false);
        add("runner.hurt.v02","assets/audio/phase10_6/sfx/creature__hurt_04.wav",0.7,2,true,false);
        add("runner.hurt.v03","assets/audio/phase10_6/sfx/creature__hurt_05.wav",0.7,2,true,false);
        add("runner.defeat.v01","assets/audio/phase10_6/sfx/rpg__creature_die_01.wav",0.7,2,true,false);
        add("runner.defeat.v02","assets/audio/phase10_6/sfx/rpg__creature_roar_01.wav",0.7,2,true,false);
        add("bell.gather","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.7,2,true,false);
        add("bell.release","assets/audio/phase10_6/sfx/magic__magical_7_0.wav",0.8,2,true,false);
        add("bell.hit.v01","assets/audio/phase10_6/sfx/rpg__item_gem_01.wav",0.5,2,true,false);
        add("bell.hit.v02","assets/audio/phase10_6/sfx/rpg__item_gem_02.wav",0.5,2,true,false);
        add("bell.hit.v03","assets/audio/phase10_6/sfx/rpg__item_gem_03.wav",0.5,2,true,false);
        add("star.cast","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.65,2,true,false);
        add("star.release.v01","assets/audio/phase10_6/sfx/rpg__spell_01.wav",0.65,2,true,false);
        add("star.release.v02","assets/audio/phase10_6/sfx/rpg__spell_02.wav",0.65,2,true,false);
        add("star.release.v03","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.65,2,true,false);
        add("star.enemy_hit.v01","assets/audio/phase10_6/sfx/rpg__item_gem_01.wav",0.7,2,true,false);
        add("star.enemy_hit.v02","assets/audio/phase10_6/sfx/rpg__item_gem_02.wav",0.7,2,true,false);
        add("star.enemy_hit.v03","assets/audio/phase10_6/sfx/rpg__item_gem_03.wav",0.7,2,true,false);
        add("star.wall_hit.v01","assets/audio/phase10_6/sfx/rpg__stones_01.wav",0.6,2,true,false);
        add("star.wall_hit.v02","assets/audio/phase10_6/sfx/rpg__stones_02.wav",0.6,2,true,false);
        add("bomb.throw","assets/audio/phase10_6/sfx/rpg__blade_03.wav",0.6,2,true,false);
        add("bomb.land","assets/audio/phase10_6/sfx/rpg__item_wood_01.wav",0.65,2,true,false);
        add("bomb.fuse","assets/audio/phase10_6/sfx/magic__magical_4.wav",0.4,2,true,false);
        add("bomb.explosion","assets/audio/phase10_6/sfx/magic__magical_7_0.wav",0.85,3,true,false);
        add("bomb.confirmed_hit.v01","assets/audio/phase10_6/sfx/rpg__spell_01.wav",0.6,2,true,false);
        add("bomb.confirmed_hit.v02","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.6,2,true,false);
        add("bomb.confirmed_hit.v03","assets/audio/phase10_6/sfx/rpg__stones_03.wav",0.6,2,true,false);
        add("pickup.lost_gift","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.7,2,false,false);
        add("pickup.spirit","assets/audio/phase10_6/sfx/rpg__item_gem_01.wav",0.7,2,false,false);
        add("pickup.wood","assets/audio/phase10_6/sfx/rpg__item_wood_02.wav",0.7,2,false,false);
        add("pickup.ornament","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.7,2,false,false);
        add("materials.category_ready","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.7,2,false,false);
        add("materials.all_ready","assets/audio/phase10_6/sfx/magic__magical_7_0.wav",0.7,2,false,false);
        add("delivery.success","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.7,2,false,false);
        add("quest.complete","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.7,2,false,false);
        add("shrine.heal","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.7,2,false,false);
        add("shrine.unavailable","assets/audio/phase10_6/sfx/rpg__lock_03.wav",0.15,2,false,false);
        add("npc.entry","assets/audio/phase10_6/sfx/rpg__book_01.wav",0.25,2,false,false);
        add("npc.page","assets/audio/phase10_6/sfx/rpg__book_03.wav",0.15,2,false,false);
        add("village.restore_lights","assets/audio/phase10_6/sfx/rpg__metal_01.wav",0.65,2,false,false);
        add("village.restore_house","assets/audio/phase10_6/sfx/foley__wood_hammer_01.wav",0.65,2,false,false);
        add("village.restore_tree","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.65,2,false,false);
        add("village.restore_campfire","assets/audio/phase10_6/sfx/foley__wood_cracking_02.wav",0.65,2,false,false);
        add("crystal.guarded","assets/audio/phase10_6/sfx/magic__magical_6_0.wav",0.65,2,true,false);
        add("crystal.exposed","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.7,2,true,false);
        add("crystal.break.v01","assets/audio/phase10_6/sfx/rpg__stones_01.wav",0.85,2,true,false);
        add("crystal.break.v02","assets/audio/phase10_6/sfx/rpg__stones_02.wav",0.85,2,true,false);
        add("crystal.break.v03","assets/audio/phase10_6/sfx/rpg__stones_03.wav",0.85,2,true,false);
        add("crystal.collapse","assets/audio/phase10_6/sfx/rpg__stones_04.wav",0.4,2,true,false);
        add("fang.entrance","assets/audio/phase10_6/sfx/creature__howl.wav",0.8,2,false,false);
        add("fang.phase2","assets/audio/phase10_6/sfx/creature__roar_01.wav",0.8,2,true,false);
        add("fang.phase3","assets/audio/phase10_6/sfx/creature__roar_02.wav",0.8,2,true,false);
        add("fang.fall","assets/audio/phase10_6/sfx/fang_fall_impact.wav",0.8,5,false,false);
        add("fang.victory","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.8,2,false,false);
        add("fang.hurt.v01","assets/audio/phase10_6/sfx/creature__hurt_01.wav",0.6,2,true,false);
        add("fang.hurt.v02","assets/audio/phase10_6/sfx/creature__hurt_02.wav",0.6,2,true,false);
        add("fang.hurt.v03","assets/audio/phase10_6/sfx/creature__hurt_03.wav",0.6,2,true,false);
        add("fang.claw.warning","assets/audio/phase10_6/sfx/creature__grunt_01.wav",0.6,4,true,false);
        add("fang.claw.execute","assets/audio/phase10_6/sfx/rpg__blade_01.wav",0.8,3,true,false);
        add("fang.ice_blast.warning","assets/audio/phase10_6/sfx/creature__breath.wav",0.6,4,true,false);
        add("fang.ice_blast.execute","assets/audio/phase10_6/sfx/rpg__spell_01.wav",0.8,3,true,false);
        add("fang.frost_wave.warning","assets/audio/phase10_6/sfx/magic__magical_6_0.wav",0.6,4,true,false);
        add("fang.frost_wave.execute","assets/audio/phase10_6/sfx/rpg__spell_02.wav",0.8,3,true,false);
        add("fang.frost_dash.warning","assets/audio/phase10_6/sfx/creature__barking_02.wav",0.6,4,true,false);
        add("fang.frost_dash.execute","assets/audio/phase10_6/sfx/rpg__blade_03.wav",0.8,3,true,false);
        add("fang.ice_blast.impact","assets/audio/phase10_6/sfx/rpg__stones_02.wav",0.6,2,true,false);
        add("fang.frost_wave.sustain","assets/audio/phase10_6/sfx/magic__magical_4.wav",0.35,2,true,false);
        add("fang.frost_dash.sustain","assets/audio/phase10_6/sfx/rpg__blade_02.wav",0.35,2,true,false);
        add("titan.entrance","assets/audio/phase10_6/sfx/creature__monster_04.wav",0.8,2,false,false);
        add("titan.core","assets/audio/phase10_6/sfx/magic__magical_6_0.wav",0.8,2,false,false);
        add("titan.phase2","assets/audio/phase10_6/sfx/creature__roar_02.wav",0.8,2,true,false);
        add("titan.phase3","assets/audio/phase10_6/sfx/creature__roar_03.wav",0.8,2,true,false);
        add("titan.collapse","assets/audio/phase10_6/sfx/titan_collapse_impact.wav",0.8,5,false,false);
        add("titan.final_victory","assets/audio/phase10_6/sfx/magic__magical_7_0.wav",0.8,2,false,false);
        add("titan.hurt.v01","assets/audio/phase10_6/sfx/rpg__stones_01.wav",0.6,2,true,false);
        add("titan.hurt.v02","assets/audio/phase10_6/sfx/rpg__stones_02.wav",0.6,2,true,false);
        add("titan.hurt.v03","assets/audio/phase10_6/sfx/rpg__metal_02.wav",0.6,2,true,false);
        add("titan.foot_contact.v01","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_01.wav",0.8,0,true,false);
        add("titan.foot_contact.v02","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_02.wav",0.8,0,true,false);
        add("titan.foot_contact.v03","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_03.wav",0.8,0,true,false);
        add("titan.foot_contact.v04","assets/audio/phase10_6/sfx/footsteps__corsica_s_walking_on_snow_covered_gravel_04.wav",0.8,0,true,false);
        add("titan.slam.warning","assets/audio/phase10_6/sfx/foley__metal_sheet_02.wav",0.65,4,true,false);
        add("titan.slam.execute","assets/audio/phase10_6/sfx/rpg__stones_03.wav",0.85,3,true,false);
        add("titan.spikes.warning","assets/audio/phase10_6/sfx/foley__wood_cracking_03.wav",0.65,4,true,false);
        add("titan.spikes.execute","assets/audio/phase10_6/sfx/rpg__stones_01.wav",0.85,3,true,false);
        add("titan.charge.warning","assets/audio/phase10_6/sfx/creature__grunt_05.wav",0.65,4,true,false);
        add("titan.charge.execute","assets/audio/phase10_6/sfx/creature__roar_02.wav",0.85,3,true,false);
        add("titan.beam.warning","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.65,4,true,false);
        add("titan.beam.execute","assets/audio/phase10_6/sfx/magic__magical_6_0.wav",0.85,3,true,false);
        add("titan.storm.warning","assets/audio/phase10_6/sfx/magic__magical_4.wav",0.65,4,true,false);
        add("titan.storm.execute","assets/audio/phase10_6/sfx/magic__magical_7_0.wav",0.85,3,true,false);
        add("titan.slam.wave","assets/audio/phase10_6/sfx/rpg__stones_04.wav",0.6,2,true,false);
        add("titan.spikes.emerge","assets/audio/phase10_6/sfx/rpg__stones_02.wav",0.6,2,true,false);
        add("titan.spikes.collapse","assets/audio/phase10_6/sfx/rpg__stones_04.wav",0.6,2,true,false);
        add("titan.charge.launch","assets/audio/phase10_6/sfx/rpg__blade_03.wav",0.6,2,true,false);
        add("titan.charge.impact","assets/audio/phase10_6/sfx/rpg__stones_03.wav",0.6,2,true,false);
        add("titan.beam.start","assets/audio/phase10_6/sfx/magic__magical_6_0.wav",0.6,2,true,false);
        add("titan.beam.end","assets/audio/phase10_6/sfx/rpg__item_gem_02.wav",0.6,2,true,false);
        add("titan.storm.gather","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.6,2,true,false);
        add("titan.storm.end","assets/audio/phase10_6/sfx/rpg__item_gem_03.wav",0.6,2,true,false);
        add("titan.beam.sustain","assets/audio/phase10_6/sfx/magic__magical_4.wav",0.35,2,true,false);
        add("titan.storm.sustain","assets/audio/phase10_6/sfx/magic__magical_4.wav",0.35,2,true,false);
        add("tree.start","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.7,2,false,false);
        add("tree.rise","assets/audio/phase10_6/sfx/magic__magical_6_0.wav",0.7,2,false,false);
        add("tree.burst","assets/audio/phase10_6/sfx/magic__magical_7_0.wav",0.7,2,false,false);
        add("tree.chime","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.7,2,false,false);
        add("tree.complete","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.7,2,false,false);
        add("celebration.gather","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.6,2,false,false);
        add("celebration.gratitude","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.6,2,false,false);
        add("sleigh.launch","assets/audio/phase10_6/sfx/foley__wood_squeak_01.wav",0.6,2,false,false);
        add("home.arrive","assets/audio/phase10_6/sfx/foley__wood_close_01.wav",0.6,2,false,false);
        add("letters.open","assets/audio/phase10_6/sfx/rpg__book_01.wav",0.4,2,false,false);
        add("letters.turn","assets/audio/phase10_6/sfx/rpg__book_03.wav",0.4,2,false,false);
        add("letters.close","assets/audio/phase10_6/sfx/rpg__book_02.wav",0.4,2,false,false);
        add("letters.ninth_ack","assets/audio/phase10_6/sfx/magic__magical_5.wav",0.4,2,false,false);
        add("credits.enter","assets/audio/phase10_6/sfx/rpg__item_gem_04.wav",0.4,2,false,false);
        add("credits.end","assets/audio/phase10_6/sfx/magic__magical_2.wav",0.4,2,false,false);
        add("ambience.base","assets/audio/phase10_6/ambience/base.wav",0.35,0,true,true);
        add("ambience.village_frozen","assets/audio/phase10_6/ambience/village_frozen.wav",0.35,0,true,true);
        add("ambience.village_restored","assets/audio/phase10_6/ambience/village_restored.wav",0.35,0,true,true);
        add("ambience.forest","assets/audio/phase10_6/ambience/forest.wav",0.35,0,true,true);
        add("ambience.town_corrupted","assets/audio/phase10_6/ambience/town_corrupted.wav",0.35,0,true,true);
        add("ambience.town_peaceful","assets/audio/phase10_6/ambience/town_peaceful.wav",0.35,0,true,true);
        add("ambience.local_crystal","assets/audio/phase10_6/ambience/local_crystal.wav",0.35,0,true,true);
        add("ambience.local_titan_ritual","assets/audio/phase10_6/ambience/local_titan_ritual.wav",0.35,0,true,true);
        add("ambience.local_village_campfire","assets/audio/phase10_6/ambience/local_village_campfire.wav",0.35,0,true,true);
        add("sleigh.board","assets/audio/phase10_6/sfx/sleigh_board.wav",.7,3,false,false);
        add("sleigh.bells.move","assets/audio/phase10_6/sfx/sleigh_bells_move.wav",.8,3,false,false);
        add("sleigh.laugh","assets/audio/phase10_6/sfx/sleigh_laugh.wav",1,4,false,false);
        // Phase4: audio-only replacement routes; music/ambience/cinematic paths stay exact.
        add("delivery.success","assets/audio/phase4/delivery_success.wav",0.65,3,false,false);
        add("santa.swing.v01","assets/audio/phase4/sword_swing_1.wav",0.75,3,true,false);
        add("sword.body.v01","assets/audio/phase4/sword_body_1.wav",0.65,3,true,false);
        add("sword.armor.v01","assets/audio/phase4/sword_armor_1.wav",0.6,3,true,false);
        add("santa.swing.v02","assets/audio/phase4/sword_swing_2.wav",0.75,3,true,false);
        add("sword.body.v02","assets/audio/phase4/sword_body_2.wav",0.65,3,true,false);
        add("sword.armor.v02","assets/audio/phase4/sword_armor_2.wav",0.6,3,true,false);
        add("santa.swing.v03","assets/audio/phase4/sword_swing_3.wav",0.75,3,true,false);
        add("sword.body.v03","assets/audio/phase4/sword_body_3.wav",0.65,3,true,false);
        add("sword.armor.v03","assets/audio/phase4/sword_armor_3.wav",0.6,3,true,false);
        add("bell.release","assets/audio/phase4/bell_large.wav",0.95,4,true,false);
        add("bell.gather","assets/audio/phase4/bell_gather.wav",0.22,1,true,false);
        add("bell.hit.v01","assets/audio/phase4/holy_hit_1.wav",0.35,2,true,false);
        add("star.release.v01","assets/audio/phase4/star_launch_1.wav",0.65,3,true,false);
        add("star.enemy_hit.v01","assets/audio/phase4/star_impact_1.wav",0.42,2,true,false);
        add("bell.hit.v02","assets/audio/phase4/holy_hit_2.wav",0.35,2,true,false);
        add("star.release.v02","assets/audio/phase4/star_launch_2.wav",0.65,3,true,false);
        add("star.enemy_hit.v02","assets/audio/phase4/star_impact_2.wav",0.42,2,true,false);
        add("bell.hit.v03","assets/audio/phase4/holy_hit_3.wav",0.35,2,true,false);
        add("star.release.v03","assets/audio/phase4/star_launch_3.wav",0.65,3,true,false);
        add("star.enemy_hit.v03","assets/audio/phase4/star_impact_3.wav",0.42,2,true,false);
        add("star.cast","assets/audio/phase4/star_gather.wav",0.22,1,true,false);
        add("bomb.explosion","assets/audio/phase4/bomb_impact.wav",0.9,4,true,false);
        add("crystal.break.v01","assets/audio/phase4/crystal_shatter_1.wav",0.8,4,true,false);
        add("crystal.break.v02","assets/audio/phase4/crystal_shatter_2.wav",0.8,4,true,false);
        add("crystal.break.v03","assets/audio/phase4/crystal_shatter_3.wav",0.8,4,true,false);
        add("crystal.impact","assets/audio/phase4/crystal_contact.wav",0.55,3,true,false);
        add("fang.entrance","assets/audio/phase4/fang_intro.wav",0.85,4,false,false);
        add("fang.hurt.v01","assets/audio/phase4/fang_hurt_1.wav",0.48,2,true,false);
        add("mini_fang.hurt.v01","assets/audio/phase4/fang_hurt_1.wav",0.25,1,true,false);
        add("fang.hurt.v02","assets/audio/phase4/fang_hurt_2.wav",0.48,2,true,false);
        add("mini_fang.hurt.v02","assets/audio/phase4/fang_hurt_2.wav",0.25,1,true,false);
        add("fang.hurt.v03","assets/audio/phase4/fang_hurt_3.wav",0.48,2,true,false);
        add("mini_fang.hurt.v03","assets/audio/phase4/fang_hurt_3.wav",0.25,1,true,false);
        add("fang.claw.execute","assets/audio/phase4/fang_claw.wav",0.7,3,true,false);
        add("fang.claw.hit","assets/audio/phase4/fang_claw_hit.wav",0.7,4,true,false);
        add("mini_fang.strike","assets/audio/phase4/mini_fang_attack.wav",0.28,1,true,false);
        add("mini_fang.warning","assets/audio/phase4/mini_fang_warning.wav",0.28,5,true,false);
        add("mini_fang.defeat","assets/audio/phase4/mini_fang_defeat.wav",0.28,1,true,false);
        add("fang.ice_blast.execute","assets/audio/phase4/fang_ice_launch.wav",0.7,3,true,false);
        add("fang.ice_blast.impact","assets/audio/phase4/fang_ice_impact.wav",0.5,2,true,false);
        add("fang.frost_wave.execute","assets/audio/phase4/fang_wave.wav",0.7,3,true,false);
        add("fang.frost_dash.execute","assets/audio/phase4/fang_dash.wav",0.7,3,true,false);
        add("titan.entrance","assets/audio/phase4/titan_roar.wav",0.85,4,false,false);
        add("titan.hurt.v01","assets/audio/phase4/titan_hurt_1.wav",0.6,2,true,false);
        add("titan.hurt.v02","assets/audio/phase4/titan_hurt_2.wav",0.6,2,true,false);
        add("titan.hurt.v03","assets/audio/phase4/titan_hurt_3.wav",0.6,2,true,false);
        add("titan.foot_contact.v01","assets/audio/phase4/titan_step_1.wav",0.62,2,true,false);
        add("titan.foot_contact.v02","assets/audio/phase4/titan_step_2.wav",0.62,2,true,false);
        add("titan.foot_contact.v03","assets/audio/phase4/titan_step_3.wav",0.62,2,true,false);
        add("titan.foot_contact.v04","assets/audio/phase4/titan_step_4.wav",0.62,2,true,false);
        add("titan.charge.warning","assets/audio/phase4/titan_charge_prepare.wav",0.65,5,true,false);
        add("titan.charge.launch","assets/audio/phase4/titan_charge_launch.wav",0.7,3,true,false);
        add("titan.charge.impact","assets/audio/phase4/titan_charge_impact.wav",0.85,4,true,false);
        add("titan.slam.warning","assets/audio/phase4/titan_slam_prepare.wav",0.65,5,true,false);
        add("titan.slam.execute","assets/audio/phase4/titan_slam_land.wav",0.88,4,true,false);
        add("titan.spikes.execute","assets/audio/phase4/titan_spikes.wav",0.75,4,true,false);
        add("titan.storm.execute","assets/audio/phase4/titan_storm.wav",0.65,4,true,false);
        add("titan.beam.warning","assets/audio/phase4/beam_charge.wav",0.6,5,true,false);
        add("titan.beam.execute","assets/audio/phase4/beam_release.wav",0.8,4,true,false);
        add("titan.beam.sustain","assets/audio/phase4/beam_sustain.wav",0.32,2,true,false);
        add("titan.beam.end","assets/audio/phase4/beam_end.wav",0.35,2,true,false);
        add("titan.slam.wave","assets/audio/phase4/titan_slam_land.wav",0.88,4,true,false);
        add("titan.charge.execute","assets/audio/phase4/titan_charge_launch.wav",0.7,3,true,false);
        add("titan.spikes.emerge","assets/audio/phase4/titan_spikes.wav",0.75,4,true,false);
        add("titan.beam.start","assets/audio/phase4/beam_release.wav",0.8,4,true,false);
        add("fang.frost_wave.sustain","assets/audio/phase4/beam_sustain.wav",0.15,2,true,false);
        add("fang.frost_dash.sustain","assets/audio/phase4/beam_sustain.wav",0.15,2,true,false);
        add("titan.storm.sustain","assets/audio/phase4/beam_sustain.wav",0.32,2,true,false);
        for(String id:List.of("fang.claw.warning","fang.ice_blast.warning","fang.frost_wave.warning","fang.frost_dash.warning","titan.spikes.warning","titan.storm.warning")){Cue c=CUES.get(id);add(id,c.path(),c.gain(),5,c.gameplay(),c.loop());}
        // User-selected local recordings: keep historical assets intact; replace only these routes.
        for(int i=1;i<=3;i++)add("santa.swing.v0"+i,"assets/audio/post_phase4/60004_sword_swing.wav",.95,4,true,false);
        add("delivery.success","assets/audio/post_phase4/715067_delivery_success.wav",.95,4,false,false);
        for(String kind:List.of("walker","runner"))for(int i=1;i<=3;i++)
            add(kind+".hurt.v0"+i,"assets/audio/post_phase4/555420_zombie_hit.wav",.95,3,true,false);
        // These two variants inherit the former hurt_02 recording; other boss vocals stay exact.
        add("fang.hurt.v02","assets/audio/post_phase4/555421_creature_hurt_02.wav",.6,2,true,false);
        add("mini_fang.hurt.v02","assets/audio/post_phase4/555421_creature_hurt_02.wav",.25,1,true,false);
        add("bomb.explosion","assets/audio/post_phase4/442958_bomb_explosion.wav",.9,4,true,false);
        add("fang.entrance","assets/audio/post_phase4/728185_fang_intro.wav",.85,4,false,false);
        add("titan.entrance","assets/audio/post_phase4/204912_titan_intro.wav",.75,4,false,false);
        Map<String,List<Cue>> variants=new HashMap<>();
        for(Cue cue:CUES.values()){
            String base=cue.id().replaceFirst("\\.v[0-9]+$","");
            variants.computeIfAbsent(base,k->new ArrayList<>()).add(cue);
        }
        for(var entry:variants.entrySet())FAMILIES.put(entry.getKey(),entry.getValue().toArray(Cue[]::new));
    }
    private static void add(String id,String path,double gain,int priority,boolean gameplay,boolean loop){CUES.put(id,new Cue(id,path,gain,priority,gameplay,loop));}
    public static Collection<Cue> cues(){return Collections.unmodifiableCollection(CUES.values());}
    public static Cue cue(String id){Cue cue=CUES.get(id);if(cue!=null)return cue;Cue[] list=FAMILIES.get(id);return list==null?null:list[0];}
    public static Cue variant(String id,int sequence){Cue direct=CUES.get(id);if(direct!=null)return direct;Cue[] list=FAMILIES.get(id);return list==null?null:list[Math.floorMod(sequence,list.length)];}
}
