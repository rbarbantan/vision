var response;

function rgbToHex(r, g, b) {
    if (r > 255 || g > 255 || b > 255)
        throw "Invalid color component";
    return ((r << 16) | (g << 8) | b).toString(16);
}

function wrapText(context, text, topLeft, bottomRight) {
        context.fillStyle = "#fff";

        var cars = text.split("\n");
        var lineHeight = Math.floor((bottomRight.y - topLeft.y)/cars.length);
        context.font = lineHeight + 'px Electric';

        for (var ii = 0; ii < cars.length; ii++) {
            context.fillText(cars[ii], topLeft.x, topLeft.y+ ii*lineHeight);

            /*var line = "";
            var words = cars[ii].split(" ");

            for (var n = 0; n < words.length; n++) {
                var testLine = line + words[n] + " ";
                var metrics = context.measureText(testLine);
                var testWidth = metrics.width;

                if (testWidth > maxWidth) {
                    context.fillText(line, x, y);
                    line = words[n] + " ";
                    y += lineHeight;
                }
                else {
                    line = testLine;
                }
            }

            context.fillText(line, x, y);
            y += lineHeight;*/

        }
     }

// Put event listeners into place
window.addEventListener("DOMContentLoaded", function() {
    // Grab elements, create settings, etc.
    var canvas = document.getElementById("canvas"),
        context = canvas.getContext("2d"),
        video = document.getElementById("video"),
        videoObj = { "video": true },
        image_format= "jpeg",
        jpeg_quality= 85,
        errBack = function(error) {
            console.log("Video capture error: ", error.code);
        };


    // Put video listeners into place
    if(navigator.getUserMedia) { // Standard
        navigator.getUserMedia(videoObj, function(stream) {
            video.src = stream;
            video.play();
            $("#snap").show();
        }, errBack);
    } else if(navigator.webkitGetUserMedia) { // WebKit-prefixed
        navigator.webkitGetUserMedia(videoObj, function(stream){
            video.src = window.webkitURL.createObjectURL(stream);
            video.play();
            $("#snap").show();
        }, errBack);
    } else if(navigator.mozGetUserMedia) { // moz-prefixed
        navigator.mozGetUserMedia(videoObj, function(stream){
            video.src = window.URL.createObjectURL(stream);
            video.play();
            $("#snap").show();
        }, errBack);
    }
          // video.play();       these 2 lines must be repeated above 3 times
          // $("#snap").show();  rather than here once, to keep "capture" hidden
          //                     until after the webcam has been activated.

    // Get-Save Snapshot - image
    document.getElementById("snap").addEventListener("click", function() {
        context.drawImage(video, 0, 0, 640, 480);
        $("#video").hide();
        $("#canvas").show();
        $("#snap").hide();
        $("#reset").show();
        //$("#upload").show();

        var dataUrl = canvas.toDataURL();
        $("#uploading").show();
        $.ajax({
          type: "POST",
          url: "/ajax",
          data: {
             imgBase64: dataUrl,
          }
        }).done(function(msg) {
          response = JSON.parse(msg);
          var c2 = canvas.getContext("2d");

          for(var i=1; i<response.length; i++) {
              var rect = response[i].boundingPoly;

              var p = c2.getImageData(rect.vertices[0].x, rect.vertices[0].y, 1, 1).data;
              var hex = "#" + ("000000" + rgbToHex(p[0], p[1], p[2])).slice(-6);


              var textH = rect.vertices[2].y - rect.vertices[0].y;
              var textW = rect.vertices[1].x - rect.vertices[0].x;
              var angle = Math.atan2((rect.vertices[1].y-rect.vertices[0].y),(rect.vertices[1].x-rect.vertices[0].x));// * 180 / Math.PI;
              //console.log("angle: " + angle);
              var fontsize = 200;
              do {
                fontsize--;
                c2.font = fontsize + 'px Electric';
              } while (c2.measureText(response[i].description).width > textW)
              //console.log(fontsize);
              c2.save();
              c2.translate(rect.vertices[0].x + textW/2, rect.vertices[0].y+textH/2);
              c2.rotate(angle);

              c2.fillStyle = hex;
              c2.beginPath();
              /*c2.moveTo(rect.vertices[0].x, rect.vertices[0].y);
              c2.lineTo(rect.vertices[1].x, rect.vertices[1].y);
              c2.lineTo(rect.vertices[2].x, rect.vertices[3].y);
              c2.lineTo(rect.vertices[3].x, rect.vertices[3].y);*/
              c2.moveTo(-textW/2,-textH/2);
              c2.lineTo(textW/2, -textH/2);
              c2.lineTo(textW/2, textH/2);
              c2.lineTo(-textW/2, textH/2);
              c2.closePath();
              c2.fill();

              c2.fillStyle = "#000";
              c2.font = (fontsize + 1) + 'px Electric-solid';
              //c2.fillText(response[i].description, rect.vertices[0].x-1, rect.vertices[0].y+fontsize+1);
              c2.fillText(response[i].description, -textW/2, textH/2);
              c2.fillStyle = "#ff0";
              c2.font = fontsize + 'px Electric';
              c2.fillText(response[i].description, -textW/2, textH/2);
              c2.restore();
              //wrapText(c2, response.description, rect.vertices[0], rect.vertices[2]);

          }



          $("#uploading").hide();
          $("#uploaded").show();
          });
    });
    // reset - clear - to Capture New Photo
    document.getElementById("reset").addEventListener("click", function() {
        $("#video").show();
        $("#canvas").hide();
        $("#snap").show();
        $("#reset").hide();
        //$("#upload").hide();
    });
    // Upload image to sever
    /*document.getElementById("upload").addEventListener("click", function(){

        });
    });*/
}, false);
