<html>
    <head>
        <title>Update YouTube Music Playlist ( <%= request.getParameter("playlistId") %> )From Form</title>
        <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>

        <script type="text/javascript">
          $(document).ready(function(){
              console.log("onload");
              $('*[id*=createForm]').on("submit", function(event){
                  event.preventDefault();
      
                  var formValues= $(this).serialize();
                  console.log("submitting: " + formValues);
      
                  $.post("/rest/sync/addSongsToPlaylist", formValues, function(data){
                      // Display the returned data in console instead of browser
                      //$("#result").html(data);
                      console.log("created playlist " + data);
                  });
              });
          });
        </script>
    </head>
    <body>
        <form id="createForm" onsubmit="return false;">
            <input type="hidden" id="spotifyListId" name="spotifyListId" value="<%= request.getParameter("playlistId") %>"/>
            <input type="hidden" id="operation" name="operation" value="<%= request.getParameter("operation") %>"/>
            <div>
                Keep backup?: <input id="backup" type="checkbox" name="backup" value="true"/>
            </div>
            <div>Song list: </div>
            <div><textarea id="songs" name="songs"></textarea></div>
            <div><input id="submit" type="submit" value="Sync"/></div>
        </form>
        <div>
            <a href="index.jsp">Back to main page</a>
        </div>
    </body>
</html>