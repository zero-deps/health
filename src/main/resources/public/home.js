var data = [];

var ws = new WebSocket(wsUrl)
ws.onmessage = function(event) {
  // data: "{name}#{node}#{param}#{time}#{value}"
  var arr = event.data.split('#');
  //todo multidimensional array
  data[Array(arr[0], arr[1], arr[2]).join('#')] = arr[4];
  data[Array(arr[0], arr[1]).join('#')] = new Date();
  React.render(<TabbedTable data={data} />, document.getElementById("tableContainer"));
};

var TabbedTable = React.createClass({
  render: function() {
    var names = Object.keys(this.props.data).map(function(key) {
      return key.split('#')[0];
    }).filter(function(v, i, a) {
      return a.indexOf(v) == i;
    }).sort();

    var unique = function(v, i, a) {
      return a.indexOf(v) == i;
    };

    var activeName = names[0];

    var nodes = Object.keys(this.props.data).filter(function(key) {
      return key.split('#')[0] === activeName;
    }).map(function(key) {
      return key.split('#')[1];
    }).filter(unique).sort();

    var params = Object.keys(this.props.data).filter(function(key) {
      return key.split('#')[0] === activeName;
    }).map(function(key) {
      return key.split('#')[2];
    }).filter(unique).sort();

    return (
      <div>
        <Tabs names={names} />
        <Table name={activeName} nodes={nodes} params={params} data={data} />
      </div>
    );
  }
});

var Tabs = React.createClass({
  render: function() {
    var tabs = this.props.names.map(function(name) {
      return <li role="presentation" className="active"><a href="#">{name}</a></li>;
    });
    return (
      <ul className="nav nav-pills">
        {tabs}
      </ul>
    );
  }
});

var Table = React.createClass({
  render: function() {
    var header = this.props.params.map(function(param) {
      return <th>{param}</th>;
    });
    var name = this.props.name;
    var params = this.props.params;
    var data = this.props.data;
    var rows = this.props.nodes.map(function(node) {
      return <Row name={name} node={node} params={params} data={data} />
    });
    return (
      <table className="table">
        <thead>
          <tr>
            <th>Node</th>
            {header}
            <th>Last updated</th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    );
  }
});

var Row = React.createClass({
  getInitialState: function() {
    return { time: new Date() };
  },
  componentDidMount: function() {
    this.timer = setInterval(this.tick, 1000);
  },
  componentWillUnmount: function() {
    clearInterval(this.timer);
  },
  tick: function() {
    this.setState({ time: new Date() });
  },
  render: function() {
    var data = this.props.data;
    var name = this.props.name;
    var node = this.props.node;
    var paramCells = this.props.params.map(function(param) {
      var value = data[Array(name, node, param).join('#')];
      return <td>{value}</td>;
    });

    var lastUpdated, className;
    var elapsed = Math.floor((this.state.time - data[Array(name, node).join('#')]) / 1000);
    if (elapsed < 3) {
      lastUpdated = "just now";
      className = "success";
    } else {
      lastUpdated = elapsed + " seconds ago";
      className = "danger";
    }

    return (
      <tr className={className}>
        <td>{this.props.node}</td>
        {paramCells}
        <td>{lastUpdated}</td>
      </tr>
    );
  }
});
