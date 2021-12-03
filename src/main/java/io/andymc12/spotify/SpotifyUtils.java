package io.andymc12.spotify;

import static io.andymc12.playlistsync.SpotifyplaylistRestApplication.SPOTIFY_CLIENT_ID;
import static io.andymc12.playlistsync.SpotifyplaylistRestApplication.SPOTIFY_CLIENT_SECRET;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.IPlaylistItem;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;

import org.apache.hc.core5.http.ParseException;

import io.andymc12.playlistsync.Playlist;
import io.andymc12.playlistsync.Song;

public class SpotifyUtils {
    private static final Logger LOG = Logger.getLogger(SpotifyUtils.class.getName());

    private static SpotifyApi spotifyApi = new SpotifyApi.Builder().setClientId(SPOTIFY_CLIENT_ID)
            .setClientSecret(SPOTIFY_CLIENT_SECRET).build();

    public static Playlist getPlaylistSync(String playlistId) {
        try {
            return getPlaylist(playlistId).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.log(Level.WARNING, "Failed to lookup playlist", e);
            return new Playlist();
        }
    }

    public static CompletionStage<Playlist> getPlaylist(String playlistId) {
        CompletionStage<SpotifyApi> cs1 = login();
        CompletionStage<com.wrapper.spotify.model_objects.specification.Playlist> cs2 = cs1
                .thenApplyAsync(spotifyApi -> {
                    try {
                        return spotifyApi.getPlaylist(playlistId).build().executeAsync().get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.log(Level.WARNING, "Failed to lookup playlist with ID: " + playlistId, e);
                        return (com.wrapper.spotify.model_objects.specification.Playlist) null;
                    }
                });
        return cs2.thenApplyAsync(SpotifyUtils::convertPlaylist);
    }

    private static Playlist convertPlaylist(Object o) {
        if (!(o instanceof com.wrapper.spotify.model_objects.specification.Playlist)) {
            throw new IllegalArgumentException("Should be Spotify model playlist, but instead is: " + o + " of type: "
                    + (o == null ? null : o.getClass()));
        }
        Playlist playlist = new Playlist();
        com.wrapper.spotify.model_objects.specification.Playlist spotifyPlaylist = (com.wrapper.spotify.model_objects.specification.Playlist) o;
        if (spotifyPlaylist != null) {
            playlist.setId(spotifyPlaylist.getId());
            playlist.setName(spotifyPlaylist.getName());
            playlist.setDescription(spotifyPlaylist.getDescription());
            Paging<PlaylistTrack> p = spotifyPlaylist.getTracks();

            int retrieved = 0;
            while (retrieved < p.getTotal()) {
                for (PlaylistTrack ptrack : p.getItems()) {
                    retrieved++;
                    Song song = new Song();
                    IPlaylistItem item = ptrack.getTrack();
                    if (item instanceof Track) {
                        Track track = (Track) item;
                        song.setSongTitle(track.getName());
                        song.setArtists(Arrays.stream(track.getArtists()).map(artist -> artist.getName())
                                .collect(Collectors.toList()));
                        playlist.getSongs().add(song);
                        LOG.finest(() -> song.toString());
                    }
                }
                if (retrieved < p.getTotal()) {
                    try {
                        p = spotifyApi.getPlaylistsItems(playlist.getId()).offset(retrieved).build().execute();
                    } catch (ParseException | SpotifyWebApiException | IOException e) {
                        LOG.log(Level.WARNING, "Failed to lookup songs beyond default limit of " + p.getLimit(), e);
                    }
                }
            }
        }
        return playlist;
    }

    private static CompletionStage<SpotifyApi> login() {
        return spotifyApi.clientCredentials().build().executeAsync().thenApplyAsync(creds -> {
            spotifyApi.setAccessToken(creds.getAccessToken());
            return spotifyApi;
        });
    }
}
