package io.andymc12.playlistsync;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Song {
    private String id;
    private String songTitle;
    private List<String> artists = new ArrayList<String>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public List<String> getArtists() {
        return artists;
    }

    public void setArtists(List<String> artists) {
        this.artists = artists;
    }

    @Override
    public String toString() {
        return songTitle + "  " + artists.stream().collect(Collectors.joining(", "));
    }
}
