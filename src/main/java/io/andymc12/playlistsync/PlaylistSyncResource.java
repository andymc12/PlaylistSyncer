package io.andymc12.playlistsync;

import static java.util.stream.Collectors.toList;

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
    Logger LOG = Logger.getLogger(PlaylistSyncResource.class.getName());

    private final List<SyncdPlaylist> playlists = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            Properties p = new Properties();
            p.load(new FileInputStream(
                new File(SpotifyplaylistRestApplication.APP_ROOT_DIRECTORY, "ytmusicapi/syncdPlaylists.properties")));
            p.forEach((k, v) -> {
                try {
                    Playlist spotifyPlaylist = SpotifyUtils.getPlaylistSync((String) k);
                    LOG.info("Loaded spotify list: " + spotifyPlaylist);
                    Playlist ytmusicPlaylist = YTMusicUtils.getPlaylist((String) v);
                    LOG.info("Loaded ytm list: " + ytmusicPlaylist);
                    playlists.add(new SyncdPlaylist(spotifyPlaylist, ytmusicPlaylist));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to load playlist properties", e);
                }
            });
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to load initial playlist properties", ex);
            ex.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            Properties p = new Properties();
            playlists.forEach(syncdPlaylist -> {
                p.setProperty(syncdPlaylist.getSpotifyPlaylist().getId(), syncdPlaylist.getYtmusicPlaylist().getId());
            });
            p.store(new FileOutputStream(
                new File(SpotifyplaylistRestApplication.APP_ROOT_DIRECTORY, "ytmusicapi/syncdPlaylists.properties")), "");
        } catch (Exception ex) {
            LOG.severe("Failed to load initial playlist properties");
        }
    }

    @GET
    @Path("/playlists")
    public List<SyncdPlaylist> allSyncdPlaylists() {
        return playlists;
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
                                            @FormParam("backup") String backup) {

        
        SyncdPlaylist list = getExistingPlaylist(spotifyListId);
        Playlist ytmPlaylist = list.getYtmusicPlaylist();
        String prefix = "BACKUP " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ": ";
        Playlist ytmBackupPlaylist = YTMusicUtils.createPlaylist(prefix + ytmPlaylist.getName(),
                                                                 ytmPlaylist.getDescription(),
                                                                 Collections.emptyList());
        LOG.info("Creating Backup of Playlist: " + ytmPlaylist.getName() + " " + ytmPlaylist.getSongs());
        YTMusicUtils.addSongIDsToPlaylist(ytmBackupPlaylist, ytmPlaylist.getSongs().stream().map(Song::getId).collect(toList()));
        ytmBackupPlaylist = waitForBackup(ytmPlaylist, ytmBackupPlaylist.getId());
        if (ytmBackupPlaylist == null) {
            throw new InternalServerErrorException("Could not confirm that backup completed");
        }
        LOG.info("Backup Playlist: " + ytmBackupPlaylist.getName() + " " + ytmBackupPlaylist.getSongs());
        YTMusicUtils.clearPlaylist(ytmPlaylist.getId());
        List<String> songs = splitSongsFromString(songListString);
        YTMusicUtils.addSongStringsToPlaylist(ytmPlaylist, songs);
        YTMusicUtils.addSongIDsToPlaylist(ytmPlaylist, ytmBackupPlaylist.getSongs().stream().map(Song::getId).collect(toList()));

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
            Playlist backup = YTMusicUtils.getPlaylist(backupId);
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

        // clear existing list
        if (!YTMusicUtils.clearPlaylist(ytmPlaylist.getId())) {
            throw new InternalServerErrorException("Failed to clear existing playlist, " + ytmPlaylist.getId());
        };

        Playlist spotifyPlaylist = SpotifyUtils.getPlaylistSync(spotifyListId);
        list.setSpotifyPlaylist(spotifyPlaylist);
        list.setYtmusicPlaylist(YTMusicUtils.addSongsToPlaylist(ytmPlaylist, spotifyPlaylist.getSongs()));

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
            if (existingSync.getSpotifyPlaylist().getId().equals(spotifyListId)) {
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
