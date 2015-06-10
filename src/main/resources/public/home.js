var data = {};

Array.prototype.flatMap = function() {
  return [].concat.apply([], this);
};
Array.prototype.distinct = function() {
  return this.filter(function(v, i, a) {
    return a.indexOf(v) == i;
  });
};
Number.prototype.toUnits = function() {
  var units = [
    [                  1, "second", "seconds"],
    [                 60, "minute", "minutes"],
    [               3600,   "hour",   "hours"],
    [          24 * 3600,    "day",    "days"],
    [      7 * 24 * 3600,   "week",   "weeks"],
    [30.4368 * 24 * 3600,  "month",  "months"],
    [365.242 * 24 * 3600,   "year",   "years"]
  ];
  var that = this;
  return units.reverse().reduce(function(acc, data) {
    var seconds = data[0];
    var singular = data[1];
    var plural = data[2];
    var integer = Math.floor(that / seconds);
    that -= integer * seconds;
    if (integer === 0) return acc;
    else if (integer === 1) return acc.concat(1, singular);
    else return acc.concat(integer, plural);
  }, []).join(' ');
};

var ws = new WebSocket(wsUrl)
ws.onmessage = function(event) {
  // data: "{name}#{node}#{param}#{time}#{value}"
  var arr = event.data.split('#');
  data[arr[0]] = data[arr[0]] || {};
  data[arr[0]][arr[1]] = data[arr[0]][arr[1]] || {};
  data[arr[0]][arr[1]]["param"] = data[arr[0]][arr[1]]["param"] || {};
  data[arr[0]][arr[1]]["param"][arr[2]] = arr[4];
  data[arr[0]][arr[1]]["time"] = new Date();
  React.render(<TabbedTable data={data} />, document.getElementById("tableContainer"));
};

var TabbedTable = React.createClass({
  render: function() {
    var names = Object.keys(this.props.data).sort();
    var activeName = names[0];
    return (
      <div>
        <Tabs names={names} />
        <Table nameData={data[activeName]} />
      </div>
    );
  }
});

var Tabs = React.createClass({
  render: function() {
    var tabs = this.props.names.map(function(name) {
      return <li role="presentation" className="active"><a href="#">{name}</a></li>;
    });
    return <ul className="nav nav-pills">{tabs}</ul>;
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
    if (elapsed < 3) {
      lastUpdated = "just now";
      className = "success";
    } else {
      lastUpdated = elapsed.toUnits() + " ago";
      className = "danger";
    }

    return (
      <tr className={className}>
        <td>{node}</td>
        {paramCells}
        <td>{lastUpdated}</td>
      </tr>
    );
  }
});
