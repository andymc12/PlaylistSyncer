#!/Users/andymc/.pyenv/shims/python
#
# Expected arguments are <PlaylistName> <PlaylistDescription> <Song1TitleAndArtist(s)> [<Song2TitleAndArtist(s)>...]

from ytmusicapi import YTMusic
from sys import argv

ytmusic = YTMusic(auth="headers_auth.json")

playlistId = ytmusic.create_playlist(argv[1], argv[2])
print(playlistId)

search_results = []
for line in argv[3:]:
  print("searching for: " + line)
  result_list = ytmusic.search(line) #, "songs", 20, 1)
  for result in result_list:
    #print("found: " + result)
    if 'videoId' in result:
      print("has videoId")
      search_results.append(result)
      break
  #print(line + ': ' + str(result))

#print("search results:")
for resultx in search_results:
  #print("adding to playlist: " + resultx)
  ytmusic.add_playlist_items(playlistId, [resultx['videoId']])

