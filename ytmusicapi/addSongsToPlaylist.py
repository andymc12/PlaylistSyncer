#!/Users/andymc/.pyenv/shims/python
#
# Expected arguments are <PlaylistID> <Song1TitleAndArtist(s)> [<Song2TitleAndArtist(s)>...]

from ytmusicapi import YTMusic
from sys import argv

ytmusic = YTMusic("oauth.json")

playlistId = argv[1]

search_results = []
for line in argv[2:]:
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
  try:
    status, videoDetails = ytmusic.add_playlist_items(playlistId, [resultx['videoId']])
    print("add status: " + status)
  except Exception as ex:
    print("Caught exception adding ", resultx, " to playlist ", playlistId)
    print(type(ex))
    for arg in ex.args:
      print(arg)
    raise ex
  
