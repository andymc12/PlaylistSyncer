package io.andymc12.playlistsync;

public class SyncdPlaylist {
    private Playlist spotifyPlaylist;
    private Playlist ytmusicPlaylist;

    public SyncdPlaylist(Playlist spotifyPlaylist, Playlist ytmusicPlaylist) {
        this.spotifyPlaylist = spotifyPlaylist;
        this.ytmusicPlaylist = ytmusicPlaylist;
    }

    public Playlist getSpotifyPlaylist() {
        return spotifyPlaylist;
    }

    public void setSpotifyPlaylist(Playlist spotifyPlaylist) {
        this.spotifyPlaylist = spotifyPlaylist;
    }

    public Playlist getYtmusicPlaylist() {
        return ytmusicPlaylist;
    }

    public void setYtmusicPlaylist(Playlist ytmusicPlaylist) {
        this.ytmusicPlaylist = ytmusicPlaylist;
    }
}
