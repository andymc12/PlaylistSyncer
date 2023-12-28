#!/Users/andymc/.pyenv/shims/python

######################################################################
# Given a play list ID, this script will return the following output:
#
# <Playlist_ID>
# <Title>
# <Description>
# Song 1: <Song1_ID>
# <Song1_Title>
# <Song1_Artist1>
# [<Song1_Artist2>]...
# Song 2: <Song2_ID>
# <Song2_Title>
# <Song2_Artist1>
# [<Song2_Artist2>]...
# ...
######################################################################

from sys import argv, exit
from ytmusicapi import YTMusic

playlist_id = argv[1]

ytmusic = YTMusic("oauth.json")
#ytmusic = YTMusic()

try:
  playlist = ytmusic.get_playlist(playlist_id)
except:
  print("No playlist found with id: " + playlist_id)
  exit(1)

if playlist == None:
  print("No playlist found with id: " + playlist_id)
  exit(1)

print(playlist_id)
print(playlist['title'])
print(playlist['description'])
tracks = playlist['tracks']
counter = 0
for track in tracks:
  counter += 1
  print("Song %d: %s" % (counter, track['videoId']))
  print(track['title'])
  artists = track['artists']
  for artist in artists:
    print(artist['name'])
