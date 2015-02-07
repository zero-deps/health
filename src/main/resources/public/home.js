window.onload = function () {
  var line = new TimeSeries();
  setInterval(function() {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (xhr.readyState == 4 && xhr.status == 200) {
        var data = JSON.parse(xhr.responseText);
        data.forEach(function(item) {
          if (item.node === "127.0.0.1:4244" &&
              item.param === "cpu") {
            line.append(item.time, item.value);
            document.getElementById("cpu1Vvalue").innerHTML = item.value + "%";
          }
        });
      }
    }
    xhr.open("GET", "get", true);
    xhr.send();
  }, 3000);
  var smoothie = new SmoothieChart({
      yRangeFunction: function(range) {
        var max = Math.floor(range.max + 1)
        return {min: 0, max: max};
      },
      grid: {
        strokeStyle: 'rgb(125, 0, 0)',
        fillStyle: 'rgb(60, 0, 0)',
        lineWidth: 1,
        millisPerLine: 1000
      }
  });
  smoothie.addTimeSeries(line, { strokeStyle:'rgb(0, 255, 0)', fillStyle:'rgba(0, 255, 0, 0.4)', lineWidth:3 });
  smoothie.streamTo(document.getElementById("mycanvas"), 3000);
};