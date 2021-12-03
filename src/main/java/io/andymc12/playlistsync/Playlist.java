package io.andymc12.playlistsync;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private List<Song> songs = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(System.lineSeparator());
        sb.append(name).append(System.lineSeparator());
        sb.append(description).append(System.lineSeparator());
        songs.stream().forEach(s -> {
            sb.append(s).append(System.lineSeparator());
        });
        return sb.toString();
    }
}
