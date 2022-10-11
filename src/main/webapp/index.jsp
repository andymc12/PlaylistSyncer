<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Playlist Syncer</title>
  <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>

  <script type="text/javascript">
    $(document).ready(function(){
        console.log("onload");
        $("img").hide();
        $('*[id*=createForm]').on("submit", function(event){
            event.preventDefault();

            var formValues= $(this).serialize();
            console.log("submitting: " + formValues);

            $.post("/rest/sync/playlists", formValues, function(data){
                // Display the returned data in console instead of browser
                //$("#result").html(data);
                console.log("created playlist " + data);
            });
        });
        $('*[id*=updateForm]').on("submit", function(event){
            event.preventDefault();

            var formValues= $(this).serialize();
            console.log("submitting: " + formValues);
            var id = $(this).children("#spotifyListId").val();
            console.log("id = " + id);
            $("#" + id).show();

            $.post("/rest/sync/refresh", formValues, function(data){
                // Display the returned data in console instead of browser
                //$("#result").html(data);
                console.log("updated playlist " + data);
                $("#" + id).hide();
            });
        });
    });
  </script>
</head>
<body>
  <div>New Playlist:
    <form id="createForm" onsubmit="return false;">
      Name: <input id="newPlaylistName" type="text" name="name" value=""/>
      Description: <input id="newPlaylistDescription" type="text" name="description" value=""/>
      Spotify Playlist ID: <input id="newPlaylistSpotifyID" type="text" name="spotifyListId" value=""/>
      Enable Duplicate?: <input id="newPlaylistEnableDuplicate" type="checkbox" name="allowDuplicate" value="true"/>
      <input id="submit" type="submit" value="Sync"/>
    </form>
  </div>
  <div>
    <a href="createFromTextForm.jsp">Create From Text</a>
    <a href="songlist.jsp">View Songs</a>
  </div>
  <div>
      <table id="p1" style="width:95%" border="1" frame="void" rules="rows">
        <tr>
            <th style="width:10%; text-align:left">Spotify:</th>
            <th style="width:10%; text-align:left">YouTube Music:</th>
            <th style="text-align:left">Name:</th>
            <th style="text-align:left">Description:</th>
            <td style="text-align:left">Actions:</td>
        </tr>
<%
io.andymc12.playlistsync.PlaylistSyncResource syncer = javax.enterprise.inject.spi.CDI.current().select(io.andymc12.playlistsync.PlaylistSyncResource.class).get();
io.andymc12.playlistsync.Playlist spotify;
io.andymc12.playlistsync.Playlist ytmusic;
for (io.andymc12.playlistsync.SyncdPlaylist spl : syncer.allSyncdPlaylists()) {
    spotify = spl.getSpotifyPlaylist();
    ytmusic = spl.getYtmusicPlaylist();
%>
        <tr>
            <td><a href="https://open.spotify.com/playlist/<%= spotify.getId() %>"><%= spotify.getId() %></a></td>
            <td><a href="https://music.youtube.com/playlist?list=<%= ytmusic.getId() %>"><%= ytmusic.getId() %></a></td>
            <td><%= ytmusic.getName() %></td>
            <td><%= ytmusic.getDescription() %></td>
            <td>
              <form id="updateForm" onsubmit="return false;">
                Backup: <input id="updatePlaylistBackup" type="checkbox" name="backup" value="true" checked/>
                <input type="hidden" id="spotifyListId" name="spotifyListId" value="<%= spotify.getId() %>"/>
                <input id="submit" type="submit" value="Update"/>
              </form>
            </td>
            <td><img id="<%= spotify.getId() %>" src="hula-hooping.gif" /></td>
        </tr>
<%
}
%>
      </table>
  </div>
  <div id="result"></div>
</body>
</html>

