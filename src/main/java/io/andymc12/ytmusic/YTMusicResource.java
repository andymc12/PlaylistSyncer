package io.andymc12.ytmusic;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.andymc12.playlistsync.Playlist;

@Path("/ytmusic")
@Produces(MediaType.APPLICATION_JSON)
public class YTMusicResource {
    Logger LOG = Logger.getLogger(YTMusicResource.class.getName());

    @GET
    @Path("/playlist/{playlistId}")
    public Playlist getPlaylist(@PathParam("playlistId") String playlistId) {
        return YTMusicUtils.getPlaylist(playlistId);
    }
}