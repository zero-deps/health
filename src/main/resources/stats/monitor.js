var ws = new WebSocket("ws://"+window.location.host+"/stats/ws");
var handlers = {
  metric: function() {},
  history: function() {}
};
ws.onmessage = function(event) {
  var newData = event.data;
  if (newData.indexOf("metric::") == 0)
    handlers.metric(newData.replace("metric::", ""))
  if (newData.indexOf("history::") == 0)
    handlers.msg(newData.replace("history::", ""))
};

React.render(<TabbedTable ws={ws} handlers={handlers} />,
  document.getElementById("tableContainer"));

React.render(<UserHistory ws={ws} handlers={handlers}/>,
  document.getElementById("userHistory"));
