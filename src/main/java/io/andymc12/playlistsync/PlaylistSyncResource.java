package io.andymc12.playlistsync;

import static java.util.stream.Collectors.toList;
import static io.andymc12.playlistsync.Operation.*;
import static io.andymc12.playlistsync.SpotifyplaylistRestApplication.APP_ROOT_DIRECTORY;
import static io.andymc12.playlistsync.SpotifyplaylistRestApplication.PYTHON_LOCATION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import io.andymc12.spotify.SpotifyUtils;
import io.andymc12.ytmusic.YTMusicUtils;

@Path("/sync")
@ApplicationScoped
@Named("playlistSync")
@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
public class PlaylistSyncResource {
    private static final Logger LOG = Logger.getLogger(PlaylistSyncResource.class.getName());
    private static final String LS = System.lineSeparator();

    private final List<SyncdPlaylist> playlists = new ArrayList<>();
    private boolean saveOnExit = true;

    @PostConstruct
    public void init() {
        try {
            LOG.info(() -> "app.root.directory = " + APP_ROOT_DIRECTORY + LS +
                           "python.location = " + PYTHON_LOCATION);
            Properties p = new Properties();
            p.load(new FileInputStream(
                new File(APP_ROOT_DIRECTORY, "ytmusicapi/syncdPlaylists.properties")));
            p.forEach((k, v) -> {
                Playlist spotifyPlaylist, ytmusicPlaylist;
                try {
                    spotifyPlaylist = SpotifyUtils.getPlaylistSync((String) k);
                    LOG.info("Loaded spotify list: " + spotifyPlaylist);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to load Spotify playlist properties: " + k, e);
                    saveOnExit = false;
                    return;
                }
                try {
                    ytmusicPlaylist = YTMusicUtils.getPlaylist((String) v);
                    LOG.info("Loaded ytm list: " + ytmusicPlaylist);
                    playlists.add(new SyncdPlaylist(spotifyPlaylist, ytmusicPlaylist));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to load YouTube playlist properties: " + v, e);
                    saveOnExit = false;
                }
            });
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to load initial playlist properties", ex);
            ex.printStackTrace();
            saveOnExit = false;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (saveOnExit) {
            try {
                Properties p = new Properties();
                playlists.forEach(syncdPlaylist -> {
                    p.setProperty(syncdPlaylist.getSpotifyPlaylist().getId(),
                            syncdPlaylist.getYtmusicPlaylist().getId());
                });
                p.store(new FileOutputStream(
                        new File(APP_ROOT_DIRECTORY, "ytmusicapi/syncdPlaylists.properties")), "");
            } catch (Exception ex) {
                LOG.severe("Failed to load initial playlist properties");
            }
        }
    }

    @GET
    @Path("/playlists")
    public List<SyncdPlaylist> allSyncdPlaylists() {
        return playlists;
    }

    @GET
    @Produces("text/html")
    @Path("/songs/{spotifyPlayListId}")
    public String getSongsFromSpotifyPlaylist(@PathParam("spotifyPlayListId") String spotifyListId,
            @QueryParam("sep") @DefaultValue(" // ") String sep) {
        Playlist spotifyPlaylist = SpotifyUtils.getPlaylistSync(spotifyListId);
        spotifyPlaylist.setTitleArtistSeparator(sep);
        return "<html><p>" + spotifyPlaylist.getSongs().stream().map(Song::toString).collect(Collectors.joining ("<br>")) + "</p></html>";
    }

    @POST
    @Path("/playlists")
    public SyncdPlaylist createNewPlaylist(@FormParam("name") String name,
                                           @FormParam("description") String description,
                                           @FormParam("spotifyListId") String spotifyListId,
                                           @FormParam("allowDuplicate") @DefaultValue("false") String dupeStr) {

        // check for duplicates:
        boolean dupe = "true".equals(dupeStr);
        Optional<SyncdPlaylist> existing = getExistingPlaylistOptional(spotifyListId);
        if (!dupe && existing.isPresent()) {
            LOG.warning("Ignoring attempt to create duplicate SyncdPlaylist (spotify id: " + spotifyListId + ")");
            return existing.get();
        }

        Playlist spotifyPlaylist = SpotifyUtils.getPlaylistSync(spotifyListId);
        Playlist ytmusicPlaylist = YTMusicUtils.createPlaylist(name, description, spotifyPlaylist.getSongs());
        SyncdPlaylist syncdPlaylist = new SyncdPlaylist(spotifyPlaylist, ytmusicPlaylist);
        if (!dupe) {
            playlists.add(syncdPlaylist);
        }
        return syncdPlaylist;
    }

    @POST
    @Path("/playlistFromForm")
    public boolean createNewPlaylistFromForm(@FormParam("name") String name,
                                           @FormParam("description") String description,
                                           @FormParam("songs") String songListString) {

        List<String> songs = splitSongsFromString(songListString);
        Playlist ytmusicPlaylist = YTMusicUtils.createPlaylistFromText(name, description, songs);
        return ytmusicPlaylist != null;
    }

    @POST
    @Path("/addSongsToPlaylist")
    public SyncdPlaylist addSongsToPlayList(@FormParam("spotifyListId") String spotifyListId,
                                            @FormParam("songs") String songListString,
                                            @FormParam("backup") String backup,
                                            @FormParam("operation") @DefaultValue("PREPEND") Operation operation) {

        LOG.info(() -> "addSongsToPlayList spotifyListId=" + spotifyListId + " backup=" + backup +
            " operation = " + operation + " songs: " + songListString);

        SyncdPlaylist list = getExistingPlaylist(spotifyListId);
        Playlist ytmPlaylist = list.getYtmusicPlaylist();
        Playlist ytmBackupPlaylist = null;
        if ("true".equalsIgnoreCase(backup) || PREPEND.equals(operation)) {
            String prefix = "BACKUP " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ": ";
            ytmBackupPlaylist = YTMusicUtils.createPlaylist(prefix + ytmPlaylist.getName(),
                                                                 ytmPlaylist.getDescription(),
                                                                 Collections.emptyList());
            LOG.info("Creating Backup of Playlist: " + ytmPlaylist.getName() + " " + ytmPlaylist.getSongs());
            YTMusicUtils.addSongIDsToPlaylist(ytmBackupPlaylist, ytmPlaylist.getSongs().stream().map(Song::getId).collect(toList()));
            ytmBackupPlaylist = waitForBackup(ytmPlaylist, ytmBackupPlaylist.getId());
            if (ytmBackupPlaylist == null) {
                throw new InternalServerErrorException("Could not confirm that backup completed");
            }
            LOG.info("Backup Playlist: " + ytmBackupPlaylist.getName() + " " + ytmBackupPlaylist.getSongs());
        }

        if (!APPEND.equals(operation)) {
            YTMusicUtils.clearPlaylist(ytmPlaylist.getId());
        }
        
        List<String> songs = splitSongsFromString(songListString);
        
        Playlist updatedPlaylist = YTMusicUtils.addSongStringsToPlaylist(ytmPlaylist, songs);

        if (PREPEND.equals(operation)) {
            updatedPlaylist = YTMusicUtils.addSongIDsToPlaylist(ytmPlaylist, ytmBackupPlaylist.getSongs().stream().map(Song::getId).collect(toList()));
        }

        int expectedNumOfSongs;
        if (REPLACE.equals(operation)) {
            expectedNumOfSongs = songs.size();
        } else {
            expectedNumOfSongs = songs.size() + ytmPlaylist.getSongs().size();
        }

        if (updatedPlaylist.getSongs().size() != expectedNumOfSongs) {
            LOG.severe("Unexpected number of songs in updated playlist - expected " + expectedNumOfSongs + 
                " but only found " + updatedPlaylist.getSongs().size() + " - will not delete backup.");
            return list;
        }

        if (!"true".equals(backup)) {
            YTMusicUtils.clearPlaylist(ytmBackupPlaylist.getId());
        }

        return list;
    }

    private List<String> splitSongsFromString(String songListString) {
        return Arrays.asList(songListString.split("\n"));
    }

    private Playlist waitForBackup(Playlist original, String backupId) {
        int MAX_TRIES = 60;
        long WAIT_TIME = 1000; // 1 sec
        for (int i=0; i< MAX_TRIES; i++) {
            Playlist backup;
            try {
                backup = YTMusicUtils.getPlaylist(backupId);
            } catch (InternalServerErrorException isee) {
                continue;
            }
            if (backup.getSongs() != null && backup.getSongs().size() == original.getSongs().size()) {
                return backup;
            }
            try { Thread.sleep(WAIT_TIME); } catch (Exception ex) {}
        }
        return null;
    }
    
    @POST
    @Path("/refresh")
    public SyncdPlaylist refreshPlaylist(@FormParam("spotifyListId") String spotifyListId,
                                         @FormParam("backup") String backup) {

        SyncdPlaylist list = getExistingPlaylist(spotifyListId);
        Playlist ytmPlaylist = list.getYtmusicPlaylist();

        // backup existing list if requested (default)
        if ("true".equals(backup)) {
            String prefix = "BACKUP " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ": ";
            YTMusicUtils.createPlaylist(prefix + ytmPlaylist.getName(),
                                        ytmPlaylist.getDescription(),
                                        ytmPlaylist.getSongs());
        }

        Playlist spotifyPlaylist = SpotifyUtils.getPlaylistSync(spotifyListId);

        int retries = 3;
        while (retries > 0) {
            try {
                // clear existing list
                if (!YTMusicUtils.clearPlaylist(ytmPlaylist.getId())) {
                    throw new InternalServerErrorException("Failed to clear existing playlist, " + ytmPlaylist.getId());
                }

                list.setSpotifyPlaylist(spotifyPlaylist);
                list.setYtmusicPlaylist(YTMusicUtils.addSongsToPlaylist(ytmPlaylist, spotifyPlaylist.getSongs()));
                retries = 0;
            } catch (Exception ex) {
                String msg = retries-- > 0 ? "Caught exception - retrying" : "Caught exception - no more retries";
                LOG.warning(msg);
                ex.printStackTrace();
            }
        }
        return list;
    }

    @GET
    @Path("playlistForm")
    public Playlist getPlaylistForm(@FormParam("playlistId") String playlistId, @FormParam("service") Service service) {
        System.out.println("ANDY: playlistId: " + playlistId + " service: " + service);
        return getPlaylist(playlistId, service);
    }

    @GET
    @Path("playlist")
    public Playlist getPlaylist(@QueryParam("playlistId") String playlistId, @QueryParam("service") Service service) {
        return Service.playlist(service).apply(playlistId);
    }

    Optional<SyncdPlaylist> getExistingPlaylistOptional(String spotifyListId) {
        for (SyncdPlaylist existingSync : playlists) {
            if (existingSync != null && existingSync.getSpotifyPlaylist().getId().equals(spotifyListId)) {
                return Optional.of(existingSync);
            }
        }
        return Optional.empty();
    }

    private SyncdPlaylist getExistingPlaylist(String spotifyListId) {
        Optional<SyncdPlaylist> existing = getExistingPlaylistOptional(spotifyListId);
        if (existing.isEmpty()) {
            throw new WebApplicationException("No record of a sync'd spotify list with ID: " + spotifyListId, 404);
        }
        return existing.get();
    }
}
