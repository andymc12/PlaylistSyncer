package io.andymc12.playlistsync;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Song {
    private String id;
    private String songTitle;
    private List<String> artists = new ArrayList<String>();
    private String separator = " "; // for separating title from artist in #toString()

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

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    @Override
    public String toString() {
        return songTitle + separator + artists.stream().collect(Collectors.joining(", "));
    }
}
