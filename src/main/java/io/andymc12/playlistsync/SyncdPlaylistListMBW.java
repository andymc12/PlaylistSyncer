package io.andymc12.playlistsync;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;


@Provider
public class SyncdPlaylistListMBW implements MessageBodyWriter<List<SyncdPlaylist>> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE) && List.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(List<SyncdPlaylist> playlists, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        PrintStream ps = new PrintStream(entityStream);
        if (playlists != null) {
            for (SyncdPlaylist playlist : playlists) {
                ps.println("SyncdPlaylist");
                ps.println("Spotify Playlist " + playlist.getSpotifyPlaylist().toText());
                ps.println("YTMusic Playlist " + playlist.getYtmusicPlaylist().toText());
            }
        } else {
            ps.println("Null playlist");
        }
    }
}
