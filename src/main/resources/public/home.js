var lines = {};

window.onload = function () {
  setInterval(function() {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (xhr.readyState == 4 && xhr.status == 200) {
        var data = JSON.parse(xhr.responseText);
        data.forEach(function(item) {
          // Add node container
          var nodeContainerId = "node_" + item.node
          if (id(nodeContainerId) === null) {
            var nodeContainer = create("div");
            nodeContainer.id = nodeContainerId;
            document.body.appendChild(nodeContainer);
            // Header
            var header = create("h2");
            header.innerHTML = item.node;
            nodeContainer.appendChild(header);
          }
          // Add param container
          var paramContainerId = nodeContainerId + "_" + item.param;
          if (id(paramContainerId) === null) {
            var paramContainer = create("div");
            paramContainer.id = paramContainerId;
            paramContainer.className = "paramContainer";
            id(nodeContainerId).appendChild(paramContainer);
            // Header
            var header = create("h3");
            header.className = "paramHeader";
            header.innerHTML = item.param;
            paramContainer.appendChild(header);
            // Chart
            var chart = create("canvas");
            chart.width = 500;
            chart.height = 250;
            paramContainer.appendChild(chart);
            // Smoothie
            var line = new TimeSeries();
            lines[paramContainerId] = line;
            var smoothie = new SmoothieChart({
              yRangeFunction: function(range) {
                var max = Math.ceil(range.max * 1.5);
                return {min: 0, max: max};
              },
              grid: { strokeStyle: 'rgb(125, 0, 0)', fillStyle: 'rgb(60, 0, 0)', lineWidth: 1, millisPerLine: 1000 }
            });
            smoothie.addTimeSeries(line, { strokeStyle: 'rgb(0, 255, 0)', fillStyle: 'rgba(0, 255, 0, 0.4)', lineWidth: 3 });
            smoothie.streamTo(chart, 3000);
            // Value
            var valueLabel = create("div");
            valueLabel.id = paramContainerId + "_label";
            paramContainer.appendChild(valueLabel);
          }
          // Update data
          lines[paramContainerId].append(item.time, item.value);
          id(paramContainerId + "_label").innerHTML = item.value;
        });
      }
    }
    xhr.open("GET", "get", true);
    xhr.send();
  }, 3000);
};
