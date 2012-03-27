$(document).ready(function(){

          $("#controls input").click(
            function() {
              if (this.checked) {
                $("."+this.name).show();
                if (this.name == "transcription") {
                  $(".image").css('width','50%');
                  $(".translation").css('width','50%');
                }
              } else {
                $("."+this.name).hide();
                if (this.name == "transcription") {
                  $(".image").css('width','100%');
                  $(".translation").css('width','100%');
                }
              }
            }
          );
          $("#titledate").append(function() {
            var result = "";
            result += $(".mdtitle:first").text();
            if (result != "") {
              result += " - ";
            }
            if ($("div.hgv .mddate").length > 0) {
              result += $("div.hgv .mddate").map(function (i) {
                return $(this).clone()
                              .children()
                              .remove()
                              .end()
                              .text();
              }).get().join("; ");
            } else {
              result += $(".mddate:first").clone()
                                          .children()
                                          .remove()
                                          .end()
                                          .text();
             
            }
            if ($(".mdprov").length > 0) {
              result += " - ";
              result += $(".mdprov:first").clone()
                                          .children()
                                          .remove()
                                          .end()
                                          .text();
            }
            return result;
          });

          $("#edit-history").mouseover( function(){
          
          	$("#edit-history-list").show("blind", 250);
          
          });
          $("#edit-history").mouseleave( function(){
          
          	$("#edit-history-list").hide("blind", 250);
          
          });
          $("#all-history").mouseover( function(){
          
          	$("#all-history-list").show("blind", 250);
          
          });
          $("#all-history").mouseleave( function(){
          
          	$("#all-history-list").hide("blind", 250);
          
          });

});