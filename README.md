# PlaylistSyncer
An app for creating playlists in YouTube Music from text or from an existing Spotify playlist.

## Installation

### Prereqs

- JDK 8 or higher - recommended 11 or higher
- Python 3
- Maven 3.6 or higher
- Spotify Developer account - client ID and secret
- YouTube Music account - Regular (free) YouTube account should work

### Setup

First, clone this repo.

In the `PlaylistSyncer` directory, create a file called `config.properties` that includes the following info:
```
app.root.directory=<path_to_this_directory>
python.location=<path_to_python3_binary>
spotify.client.id=5<client_id>
spotify.client.secret=<client_secret>
```

Last, you will need to create a `headers_auth.json` file in the `ytmusicapi` directory. You will need to follow the instructions from the ytmusicapi site at https://ytmusicapi.readthedocs.io/en/stable/setup.html#authenticated-requests

Now you are ready to run the app.  From the `PlaylistSyncer` directory, invoke this command:
```
mvn install liberty:run
```

Then navigate your browser to: http://localhost:8080