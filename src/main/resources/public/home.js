window.onload = function () {
  var ws = new WebSocket(wsUrl)
  ws.onmessage = function(event) {
    console.log(event.data);
  };
};
