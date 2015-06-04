var data = [];

var ws = new WebSocket(wsUrl)
ws.onmessage = function(event) {
  // data: "{name}#{node}#{param}#{time}#{value}"
  var arr = event.data.split('#');
  data[Array(arr[0], arr[1], arr[2]).join('#')] = arr[4];
  React.render(<TabbedTable data={data} />, document.getElementById('tableContainer'));
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
  render: function() {
    var data = this.props.data;
    var name = this.props.name;
    var node = this.props.node;
    var cells = this.props.params.map(function(param) {
      var value = data[Array(name, node, param).join('#')];
      return <td>{value}</td>;
    });
    return (
      <tr>
        <td>{this.props.node}</td>
        {cells}
        <td>just now</td>
      </tr>
    );
  }
});
