package io.andymc12.ytmusic;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import io.andymc12.playlistsync.Playlist;
import io.andymc12.playlistsync.Song;
import io.andymc12.playlistsync.SpotifyplaylistRestApplication;

public class YTMusicUtils {
    private final static Logger LOG = Logger.getLogger(YTMusicUtils.class.getName());
    private static ExecutorService executor = Executors.newFixedThreadPool(4);

    public static Playlist getPlaylist(String playlistId) {
        return getPlaylist(playlistId, 1);
    }
    public static Playlist getPlaylist(String playlistId, int numRetries) {
        RuntimeException initialException = null;
        for (int i=0; i<numRetries; i++) {
            try {
                List<String> lines = runScript("getPlaylist.py", playlistId);
                LOG.finest(() -> lines.stream().collect(Collectors.joining(System.lineSeparator())));
                return playlistFromText(lines);
            } catch (RuntimeException re) {
                if (initialException == null) {
                    initialException = re;
                }
            }
        }
        throw initialException;
    }

    public static Playlist createPlaylist(String name, String description, List<Song> songs) {
        return createPlaylistFromText(name, description, songs.stream().map(Song::toString).collect(toList()));
    }

    public static Playlist createPlaylistFromText(String name, String description, List<String> songs) {
        String[] parms = new String[songs.size() + 2];
        parms[0] = name;
        parms[1] = description;
        for (int i=2; i < parms.length; i++) {
            parms[i] = songs.get(i-2);
        }
        List<String> lines = runScript("createPlaylist.py", parms);
        if (lines.size() > 0) {
            String ytmPlaylistId = lines.get(0);
            return getPlaylist(ytmPlaylistId, 5);
        }
        throw new InternalServerErrorException();
    }

    public static boolean clearPlaylist(String ytMusicPlaylistId) {
        LOG.info("about to clear playlist: " + ytMusicPlaylistId);
        return runScript("clearPlaylist.py", ytMusicPlaylistId).size() < 1;
    }

    public static Playlist addSongsToPlaylist(Playlist list, List<Song> songs) {
        return addSongStringsToPlaylist(list, songs.stream().map(s -> 
            s.getSongTitle() + " " + s.getArtists().stream().collect(Collectors.joining(" "))).collect(toList()));
    }

    public static Playlist addSongStringsToPlaylist(Playlist list, List<String> songs) {
        return addSongsToPlaylist("addSongsToPlaylist.py", list, songs);
    }

    public static Playlist addSongIDsToPlaylist(Playlist list, List<String> songIds) {
        return addSongsToPlaylist("addSongIDsToPlaylist.py", list, songIds);
    }

    private static Playlist addSongsToPlaylist(String scriptName, Playlist list, List<String> songs) {
        final String listId = list.getId();
        if (songs == null || songs.isEmpty()) {
            LOG.warning("No songs to add to list " + listId);
            return list;
        }
        // NEW CODE:
        final int songsPerScriptInvoke = 25;
        final int totalSongSize = songs.size();
        int songsAdded = 0;
        while (songsAdded < totalSongSize) {
            int songsToAdd = min(totalSongSize - songsAdded, songsPerScriptInvoke);
            String[] parms = new String[songsToAdd + 1];
            parms[0] = listId;

            StringBuilder sb = new StringBuilder(parms[0]);
            for (int i = 1; i < parms.length; i++) {
                parms[i] = songs.get(songsAdded + i - 1);
                sb.append(" \"").append(parms[i]).append("\"");
            }
            LOG.info("adding songs: " + sb.toString());
            runScript(scriptName, parms);
            songsAdded += songsToAdd;
        }
        return getPlaylist(listId);
    }

    static Playlist playlistFromText(List<String> text) {
        Playlist p = new Playlist();
        p.setId(text.get(0));
        p.setName(text.get(1));
        p.setDescription(text.get(2));
        Iterator<String> iter;
        try {
            iter = text.subList(3, text.size() - 1).iterator();
            int songCounter = 1;
            Song s = null;
            SongMetaData nextLocation = SongMetaData.ID;
            while (iter.hasNext()) {
                String line = iter.next();
                if (line.startsWith("Song " + songCounter + ": ")) {
                    s = new Song();
                    p.getSongs().add(s);
                    s.setId(line.substring(("Song " + songCounter + ": ").length()).trim());
                    nextLocation = SongMetaData.TITLE;
                    songCounter++;
                    continue;
                }
                if (nextLocation == SongMetaData.TITLE) {
                    s.setSongTitle(line);
                    nextLocation = SongMetaData.ARTIST;
                    continue;
                }
                if (nextLocation == SongMetaData.ARTIST) {
                    s.getArtists().add(line);
                    continue;
                }
            }
        } catch (IllegalArgumentException ex) {
            // expected if getting empty playlist
        }

        return p;
    }

    static List<String> runScript(String scriptName, String... params) {
        LOG.info("runScript " + scriptName + " " + Arrays.asList(params));
        try {
            List<String> lines = new ArrayList<>();
            ProcessBuilder builder = new ProcessBuilder();
            List<String> executable = new ArrayList<>();
            executable.add(pythonLocation());
            executable.add(scriptName);
            for (String param : params) {
                executable.add(param);
            }
            builder.command(executable);
            builder.directory(new File(appDirectory("ytmusicapi")));
            Process process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), lines::add);
            executor.submit(streamGobbler);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("Error running script (" + scriptName + "):");
                lines.forEach(System.out::println);
                throw new InternalServerErrorException();
            }
            return lines;
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            throw new WebApplicationException(Response.serverError().build());
        }
    }

    private static String pythonLocation() {
        return SpotifyplaylistRestApplication.PYTHON_LOCATION;
    }
    private static String appDirectory(String dir) {
        if (!dir.startsWith("/")) {
            dir = "/" + dir;
        }
        return SpotifyplaylistRestApplication.APP_ROOT_DIRECTORY + dir;
    }
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
}
