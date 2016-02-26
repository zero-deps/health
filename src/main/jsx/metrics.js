var TabbedTable = React.createClass({
  parseData: function(rawData) {
    var list = [].concat(rawData);
    var data = list.map(function(rawData) {
      var arr = rawData.split('::');
      var obj = {
        name:  arr[0],
        node:  arr[1],
        param: arr[2],
        time:  arr[3],
        value: arr[4]
      };
      return obj;
    });
    return data;
  },
  packData: function(newData, initData) {
    newData.forEach(function(obj) {
      initData[obj.name] = initData[obj.name] || {};
      initData[obj.name][obj.node] = initData[obj.name][obj.node] || {};
      initData[obj.name][obj.node]["param"] = initData[obj.name][obj.node]["param"] || {};
      initData[obj.name][obj.node]["param"][obj.param] = obj.value;
      initData[obj.name][obj.node]["time"] = obj.time;
    });
    return { data: initData, activeName: this.state.activeName };
  },
  getInitialState: function() {
    return { data: {}, activeName: null };
  },
  componentDidMount: function() {
    this.props.handlers.metric = function(newDataRaw) {
      if (this.isMounted()) {
        var state = this.packData(this.parseData(newDataRaw), this.state.data);
        this.setState(state);
      }
    }.bind(this);
  },
  handleChoose: function(tab) {
    this.setState({activeName: tab.props.name});
  },
  handleRemove: function(node) {
    var ws = this.props.ws;
    var data = this.state.data;
    var name = this.state.activeName;
    // Remove on server
    var params = Object.keys(data[name][node]["param"]);
    params.forEach(function(param) {
      ws.send(name + "::" + node + "::" + param);
    });
    // Remove on client
    delete data[name][node];
    this.setState({data: data});
  },
  render: function() {
    var data = this.state.data;
    var names = Object.keys(data).sort();
    if (names.length === 0) return <div>No data yet :(</div>
    else {
      var activeName = this.state.activeName;
      if (names.indexOf(activeName) == -1) activeName = names[0];
      return (
        <div>
          <Tabs names={names}
                active={activeName}
                onChoose={this.handleChoose} />
          <Table nameData={data[activeName]}
                 onRemove={this.handleRemove} />
        </div>
      );
    }
  }
});

var Tabs = React.createClass({
  render: function() {
    var tabs = this.props.names.map(function(name,i) {
      var active = name === this.props.active;
      return <Tab name={name} active={active} onChoose={this.props.onChoose} key={i} />;
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
    }).flatten().distinct().sort();

    var header = params.map(function(param,i) {
      return <th key={i}>{param}</th>;
    });

    var rows = Object.keys(nameData).map(function(node,i) {
      return <Row node={node}
                  params={params}
                  nodeData={nameData[node]}
                  onRemove={this.props.onRemove}
                  key={i} />
    }.bind(this));

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

    var elapsed = Math.floor((new Date() - nodeData["time"]) / 1000);

    var paramCells = params.map(function(param,i) {
      return <td key={i}>{nodeData["param"][param]}</td>;
    });

    return (
      <tr className={elapsed < 3 ? "success" : "danger"}
          onMouseOver={this.mouseOver}
          onMouseOut={this.mouseOut}>
        <td>{node}</td>
        {paramCells}
        <LastUpdatedCell elapsed={elapsed} />
        <RemoveCell onRemove={this.handleRemove} visible={this.state.hover} />
      </tr>
    );
  }
});

var LastUpdatedCell = React.createClass({
  render: function() {
    var elapsed = this.props.elapsed;
    var text = elapsed < 1 ? "just now" : elapsed.toUnits() + " ago";
    return <td>{text}</td>;
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
