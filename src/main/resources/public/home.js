var TabbedTable = React.createClass({
  getInitialState: function() {
    return { data: {}, activeName: null };
  },
  componentDidMount: function() {
    var ws = new WebSocket(this.props.wsUrl);
    ws.onmessage = function(event) {
      // event.data: "{name}#{node}#{param}#{time}#{value}"
      var arr = event.data.split('#');
      var data = this.state.data;
      data[arr[0]] = data[arr[0]] || {};
      data[arr[0]][arr[1]] = data[arr[0]][arr[1]] || {};
      data[arr[0]][arr[1]]["param"] = data[arr[0]][arr[1]]["param"] || {};
      data[arr[0]][arr[1]]["param"][arr[2]] = arr[4];
      data[arr[0]][arr[1]]["time"] = new Date();
      if (this.isMounted())
        this.setState({
          data: data,
          activeName: this.state.activeName || arr[0]
        });
    }.bind(this);
  },
  handleChoose: function(tab) {
    console.log("clicked2");
    this.setState({activeName: tab.props.name});
  },
  render: function() {
    var names = Object.keys(this.state.data).sort();
    if (names.length === 0) return <div>No data yet :(</div>
    else
      return (
        <div>
          <Tabs names={names} active={this.state.activeName} onChoose={this.handleChoose} />
          <Table nameData={this.state.data[this.state.activeName]} />
        </div>
      );
  }
});

var Tabs = React.createClass({
  render: function() {
    var tabs = this.props.names.map(function(name) {
      var active = name === this.props.active;
      return <Tab name={name} active={active} onChoose={this.props.onChoose} />;
    }.bind(this));
    return <ul className="nav nav-pills">{tabs}</ul>;
  }
});

var Tab = React.createClass({
  handleChoose: function() {
    console.log("clicked1");
    this.props.onChoose(this);
  },
  render: function() {
    var className = this.props.active ? "active" : "";
    return (
      <li role="presentation" className={className}>
        <a href="#" onClick={this.handleChoose}>{this.props.name}</a>
      </li>
    );
  }
});

var Table = React.createClass({
  render: function() {
    var nameData = this.props.nameData;

    var params = Object.keys(nameData).map(function(node) {
      return Object.keys(nameData[node]["param"]);
    }).flatMap().distinct().sort();

    var header = params.map(function(param) {
      return <th>{param}</th>;
    });

    var rows = Object.keys(nameData).map(function(node) {
      return <Row node={node} params={params} nodeData={nameData[node]} />
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
    var node = this.props.node;
    var params = this.props.params;
    var nodeData = this.props.nodeData;

    var paramCells = params.map(function(param) {
      return <td>{nodeData["param"][param]}</td>;
    });

    var lastUpdated, className;
    var elapsed = Math.floor((this.state.time - nodeData["time"]) / 1000);
    if (elapsed < 1) lastUpdated = "just now";
    else lastUpdated = elapsed.toUnits() + " ago";
    if (elapsed < 3) className = "success";
    else className = "danger";

    return (
      <tr className={className}>
        <td>{node}</td>
        {paramCells}
        <td>{lastUpdated}</td>
      </tr>
    );
  }
});

React.render(<TabbedTable wsUrl={wsUrl} />, document.getElementById("tableContainer"));
