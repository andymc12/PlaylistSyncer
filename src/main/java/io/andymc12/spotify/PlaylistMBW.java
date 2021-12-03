package io.andymc12.spotify;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import io.andymc12.playlistsync.Playlist;
import io.andymc12.playlistsync.Song;


@Provider
public class PlaylistMBW implements MessageBodyWriter<Playlist> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE) && Playlist.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Playlist playlist, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        PrintStream ps = new PrintStream(entityStream);
        if (playlist != null) {
            ps.println(playlist.getId());
            ps.println(playlist.getName());
            ps.println(playlist.getDescription());
            for (Song s : playlist.getSongs()) {
                ps.println(s.getSongTitle() + s.getArtists().stream().collect(Collectors.joining(" ")));
            }
        }
    }
}
