var ws = new WebSocket('ws://'+window.location.host+'/stats/ws');
var handlers = {
  metric: function() {},
  history: function() {}
};
ws.onmessage = function(event) {
  var newData = event.data;
  if (newData.indexOf('metric::') == 0)
    handlers.metric(newData.replace('metric::', ''))
  if (newData.indexOf('history::') == 0)
    handlers.history(newData.replace('history::', ''))
};

function menuHandler(e) {
  if (!e.target.parentNode.classList.contains('active')) {
    id('metrics-menu').parentNode.classList.toggle('active');
    id('history-menu').parentNode.classList.toggle('active');
    id('metrics').parentNode.classList.toggle('active');
    id('history').parentNode.classList.toggle('active');
  }
}
id('metrics-menu').addEventListener('click',menuHandler);
id('history-menu').addEventListener('click',menuHandler);

ReactDOM.render(<TabbedTable ws={ws} handlers={handlers} />, id('metrics'));
ReactDOM.render(<UserHistory ws={ws} handlers={handlers}/>, id('history'));