var TabbedTable = React.createClass({
  parseData: function(data) {
    var list = [].concat(data);
    var objects = list.map(function(data) {
      var arr = data.split('#');
      var obj = {
        name:  arr[0],
        node:  arr[1],
        param: arr[2],
        time:  arr[3],
        value: arr[4]
      };
      return obj;
    });
    return objects;
  },
  packData: function(objects, initial, activeName, currentTime) {
    objects.forEach(function(obj) {
      initial[obj.name] = initial[obj.name] || {};
      initial[obj.name][obj.node] = initial[obj.name][obj.node] || {};
      initial[obj.name][obj.node]["param"] = initial[obj.name][obj.node]["param"] || {};
      initial[obj.name][obj.node]["param"][obj.param] = obj.value;
      initial[obj.name][obj.node]["time"] = currentTime ? new Date() : obj.time;
    });
    if (Object.keys(initial).indexOf(activeName) == -1)
      activeName = Object.keys(initial).sort()[0];
    return { data: initial, activeName: activeName };
  },
  getInitialState: function() {
    return this.packData(this.parseData(this.props.lastData), {}, this.props.activeName, false);
  },
  componentDidMount: function() {
    this.props.ws.onmessage = function(event) {
      if (this.isMounted()) {
        var state = this.packData(this.parseData(event.data), this.state.data, this.state.activeName, true);
        this.setState(state);
      }
    }.bind(this);
  },
  handleChoose: function(tab) {
    var activeName = tab.props.name;
    localStorage["activeName"] = activeName;
    this.setState({activeName: activeName});
  },
  handleRemove: function(node) {
    var ws = this.props.ws;
    var data = this.state.data;
    var name = this.state.activeName;
    // Remove on server
    var params = Object.keys(data[name][node]["param"]);
    params.forEach(function(param) {
      ws.send(name + "#" + node + "#" + param);
    });
    // Remove on client
    delete data[name][node];
    this.setState({data: data});
  },
  render: function() {
    var names = Object.keys(this.state.data).sort();
    if (names.length === 0) return <div>No data yet :(</div>
    else
      return (
        <div>
          <Tabs names={names}
                active={this.state.activeName}
                onChoose={this.handleChoose} />
          <Table nameData={this.state.data[this.state.activeName]}
                 onRemove={this.handleRemove} />
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

    var self = this;
    var rows = Object.keys(nameData).map(function(node) {
      return <Row node={node}
                  params={params}
                  nodeData={nameData[node]}
                  onRemove={self.props.onRemove} />
    });

    return (
      <table className="table">
        <thead>
          <tr>
            <th>Node</th>
            {header}
            <th>Last updated</th>
            <th></th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    );
  }
});

var Row = React.createClass({
  getInitialState: function() {
    return { hover: false };
  },
  componentDidMount: function() {
    this.timer = setInterval(this.tick, 1000);
  },
  componentWillUnmount: function() {
    clearInterval(this.timer);
  },
  tick: function() {
    this.forceUpdate();
  },
  handleRemove: function() {
    this.props.onRemove(this.props.node);
  },
  mouseOver: function() {
    this.setState({hover: true});
  },
  mouseOut: function() {
    this.setState({hover: false});
  },
  render: function() {
    var node = this.props.node;
    var params = this.props.params;
    var nodeData = this.props.nodeData;

    var paramCells = params.map(function(param) {
      return <td>{nodeData["param"][param]}</td>;
    });

    var lastUpdated, className;
    var elapsed = Math.floor((new Date() - nodeData["time"]) / 1000);
    if (elapsed < 1) lastUpdated = "just now";
    else lastUpdated = elapsed.toUnits() + " ago";
    if (elapsed < 3) className = "success";
    else className = "danger";

    return (
      <tr className={className}
          onMouseOver={this.mouseOver}
          onMouseOut={this.mouseOut}>
        <td>{node}</td>
        {paramCells}
        <td>{lastUpdated}</td>
        <RemoveCell onRemove={this.handleRemove} visible={this.state.hover} />
      </tr>
    );
  }
});

var RemoveCell = React.createClass({
  handleRemove: function() {
    this.props.onRemove();
  },
  render: function() {
    return (
      <td>
        <span className={this.props.visible ? "" : "invisible"}
              onClick={this.handleRemove}>
          <span className="glyphicon glyphicon-remove-circle" aria-hidden="true"></span>
        </span>
        <span className="sr-only">Remove</span>
      </td>
    );
  }
});

var ws = new WebSocket(wsUrl);

React.render(<TabbedTable ws={ws}
                          lastData={lastData}
                          activeName={localStorage["activeName"]} />,
  document.getElementById("tableContainer"));
