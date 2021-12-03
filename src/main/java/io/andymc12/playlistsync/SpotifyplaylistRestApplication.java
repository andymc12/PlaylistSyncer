package io.andymc12.playlistsync;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/")
public class SpotifyplaylistRestApplication extends Application {
    public final static String APP_ROOT_DIRECTORY = System.getProperty("app.root.directory", ".");
    public final static String PYTHON_LOCATION = System.getProperty("python.location");

    public static final String SPOTIFY_CLIENT_ID = System.getProperty("spotify.client.id");
    public static final String SPOTIFY_CLIENT_SECRET = System.getProperty("spotify.client.secret");
}
