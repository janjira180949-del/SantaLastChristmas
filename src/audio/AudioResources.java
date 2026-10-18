package audio;

import java.io.*;
import java.nio.file.*;
import javax.sound.sampled.*;

/** Resolve once during asynchronous decoding, including the existing IDE launch layouts. */
public final class AudioResources {
    public static final AudioFormat FORMAT=new AudioFormat(44100,16,2,true,false);
    private AudioResources() { }
    public static byte[] decode(String path) throws IOException,UnsupportedAudioFileException {
        try(InputStream input=open(path); AudioInputStream source=AudioSystem.getAudioInputStream(new BufferedInputStream(input));
            AudioInputStream pcm=AudioSystem.getAudioInputStream(FORMAT,source)) {
            byte[] bytes=pcm.readNBytes(64*1024*1024+1);
            if(bytes.length>64*1024*1024||bytes.length%4!=0)throw new IOException("Invalid or oversized PCM: "+path);
            return bytes;
        } catch(IllegalArgumentException e) {throw new IOException("Unsupported audio: "+path,e);}
    }
    private static InputStream open(String path)throws IOException {
        InputStream resource=AudioResources.class.getClassLoader().getResourceAsStream(path);
        if(resource!=null)return resource;
        for(Path directory=Path.of("").toAbsolutePath().normalize();directory!=null;directory=directory.getParent()) {
            Path candidate=directory.resolve(path);if(Files.isRegularFile(candidate))return Files.newInputStream(candidate);
            try(var children=Files.list(directory)) {
                for(Path child:children.filter(Files::isDirectory).toList()) {
                    candidate=child.resolve(path);if(Files.isRegularFile(candidate))return Files.newInputStream(candidate);
                }
            } catch(IOException|SecurityException ignored) { /* Try remaining lookup locations. */ }
        }
        throw new FileNotFoundException(path);
    }
}
