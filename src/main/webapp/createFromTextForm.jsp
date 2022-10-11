<html>
    <head>
        <title>Create New YouTube Music Playlist From Form</title>
        <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>

        <script type="text/javascript">
          $(document).ready(function(){
              console.log("onload");
              $('*[id*=createForm]').on("submit", function(event){
                  event.preventDefault();
      
                  var formValues= $(this).serialize();
                  console.log("submitting: " + formValues);
      
                  $.post("/rest/sync/playlistFromForm", formValues, function(data){
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
            <div>
                Name: <input id="newPlaylistName" type="text" name="name" value=""/>
                Description: <input id="newPlaylistDescription" type="text" name="description" value=""/>
                Enable Duplicate?: <input id="newPlaylistEnableDuplicate" type="checkbox" name="allowDuplicate" value="true"/>
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