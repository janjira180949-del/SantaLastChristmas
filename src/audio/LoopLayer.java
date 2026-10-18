package audio;

import java.util.Objects;

/** Output-thread-owned, two-slot linear crossfade. No decoding, timers or per-frame allocation. */
final class LoopLayer {
    private static final class Voice {
        String path;
        byte[] data;
        int position,remaining;
        double gain,target,step;
        void fade(double value,int frames) {
            target=value;remaining=Math.max(1,frames);step=(target-gain)/remaining;
        }
        void advance() {
            if(remaining>0&&--remaining==0)gain=target;
            else if(remaining>0)gain+=step;
            if(gain==0&&target==0){path=null;data=null;position=0;}
        }
    }
    private final Voice[] voices={new Voice(),new Voice()};
    private String applied,pending;
    private boolean waiting;
    private double left,right;

    /** Keep current audio until decoding completes. Empty PCM means missing, and fades to silence. */
    void request(String path,byte[] ready,int frames) {
        if(Objects.equals(path,applied)&&!waiting)return;
        if(path!=null&&ready==null)return;
        if(path==null||ready.length<4) {
            for(Voice voice:voices)voice.fade(0,frames);
            applied=path;waiting=false;return;
        }
        Voice chosen=null;
        for(Voice voice:voices)if(path.equals(voice.path)){chosen=voice;break;}
        if(chosen==null)for(Voice voice:voices)if(voice.data==null){chosen=voice;break;}
        if(chosen==null) {
            // A third destination waits for a free slot; never abruptly evict audible PCM.
            if(!waiting||!Objects.equals(path,pending)) {
                for(Voice voice:voices)voice.fade(0,frames);
                pending=path;waiting=true;
            }
            return;
        }
        if(chosen.data==null){chosen.path=path;chosen.data=ready;chosen.position=0;chosen.gain=0;}
        for(Voice voice:voices)voice.fade(voice==chosen?1:0,frames);
        applied=path;waiting=false;
    }
    void advance() {
        left=0;right=0;double total=0;
        for(Voice voice:voices){voice.advance();total+=voice.gain;}
        // Reversing an interrupted transition must never add two full-volume tracks.
        double scale=1/Math.max(1,total);
        for(Voice voice:voices)if(voice.data!=null) {
            left+=sample(voice.data,voice.position)*voice.gain*scale;
            right+=sample(voice.data,voice.position+2)*voice.gain*scale;
            voice.position+=4;if(voice.position>=voice.data.length)voice.position=0;
        }
    }
    boolean has(String path) {
        for(Voice voice:voices)if(voice.data!=null&&Objects.equals(path,voice.path))return true;
        return false;
    }
    double left(){return left;}
    double right(){return right;}
    boolean isActive(){for(Voice voice:voices)if(voice.data!=null)return true;return false;}
    int copyPaths(String[] paths,int offset){for(Voice voice:voices)if(voice.data!=null&&offset<paths.length)paths[offset++]=voice.path;return offset;}
    void clear(){for(Voice voice:voices){voice.data=null;voice.path=null;voice.gain=voice.target=0;voice.remaining=0;}applied=pending=null;waiting=false;}
    private static int sample(byte[] data,int offset){return (short)((data[offset]&255)|(data[offset+1]<<8));}
}
