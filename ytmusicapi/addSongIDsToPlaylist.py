#!/Users/andymc/.pyenv/shims/python
#
# Expected arguments are <PlaylistID> <Song1Id> [<Song2Id>...]

from ytmusicapi import YTMusic
from sys import argv

ytmusic = YTMusic("oauth.json")

playlistId = argv[1]

#for songId in argv[2:]:
#  print("adding to playlist: " + songId)
ytmusic.add_playlist_items(playlistId, argv[2:])
