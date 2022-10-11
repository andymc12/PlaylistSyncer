package io.andymc12.playlistsync;

import java.util.function.Function;

import io.andymc12.spotify.SpotifyUtils;
import io.andymc12.ytmusic.YTMusicUtils;

public enum Service {
    

    SPOTIFY,
    YOUTUBE;

    public static Service fromString(String s) {

        if (s == null || "spotify".equalsIgnoreCase(s))
            return SPOTIFY;
        return YOUTUBE;
    }

    static Function<String, Playlist> playlist(Service service) {
        switch (service){
            case SPOTIFY: return SpotifyUtils::getPlaylistSync;
            case YOUTUBE: return YTMusicUtils::getPlaylist;
        }
        throw new IllegalArgumentException("invalid service - only allowing SPOTIFY and YOUTUBE");
    }
}
