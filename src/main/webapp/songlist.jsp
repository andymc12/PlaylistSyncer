<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Playlist Syncer</title>
  <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>

  <script type="text/javascript">
    $(document).ready(function(){
        console.log("onload");
        $('*[id*=viewPlaylistForm]').on("submit", function(event){
            event.preventDefault();

            var formValues= $(this).serialize();
            console.log("submitting: " + formValues);

            $.get("/rest/sync/playlistForm", formValues, function(data){
                // Display the returned data in console instead of browser
                //$("#result").html(data);
                console.log("obtained playlist " + data);
                $('#songs').val(data);
            });
        });
    });
  </script>
</head>
<body>
    <div>View Playlist:
        <form id="viewPlaylistForm" onsubmit="return false;">
          Playlist ID: <input id="playlistId" type="text" name="playlistId" value=""/>
          <input type="radio" id="spotify" name="service" value="SPOTIFY"/>
          <label for="spotify">Spotify</label>
          <input type="radio" id="youtube" name="service" value="YOUTUBE"/>
          <label for="youtube">YouTube Music</label>
          <input id="submit" type="submit" value="View List"/>
        </form>
    </div>
    <div>Songs:</div>
    <div><textarea id="songs" name="songs" readonly="true"></textarea></div>
</body>