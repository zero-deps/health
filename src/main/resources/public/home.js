var ws = new WebSocket(wsUrl);
var handlers = {
  metric: function() {},
  msg: function() {}
};
ws.onmessage = function(event) {
  var newData = event.data;
  if (newData.indexOf("metric::") == 0)
    handlers.metric(newData.replace("metric::", ""))
  if (newData.indexOf("msg::") == 0)
    handlers.msg(newData.replace("msg::", ""))
};

React.render(<TabbedTable ws={ws} handlers={handlers}
                          activeName={localStorage["activeName"]} />,
  document.getElementById("tableContainer"));

React.render(<UserHistory ws={ws} handlers={handlers}/>,
  document.getElementById("userHistory"));
