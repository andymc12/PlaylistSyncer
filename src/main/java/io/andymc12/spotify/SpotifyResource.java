package io.andymc12.spotify;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.andymc12.playlistsync.Playlist;

@Path("/playlist")
@ApplicationScoped
@Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
public class SpotifyResource {

    @GET
    @Path("/{playlist_id}")
    public CompletionStage<Playlist> getPlaylist(@PathParam("playlist_id") String playlistId) {
        return SpotifyUtils.getPlaylist(playlistId);
    }
}
