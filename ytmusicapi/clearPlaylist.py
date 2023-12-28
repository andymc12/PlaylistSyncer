#!/Users/andymc/.pyenv/shims/python

# Expected arguments are <PlaylistID>

from ytmusicapi import YTMusic
from sys import argv

ytmusic = YTMusic("oauth.json")

playlistId = argv[1]

playlist = ytmusic.get_playlist(playlistId)
if len(playlist['tracks']) > 0:
  ytmusic.remove_playlist_items(playlistId, playlist['tracks'])
