var Nodes = function () {

  var Nodes = React.createClass({
    displayName: "Nodes",

    parseItem: function (rawData) {
      var arr = rawData.split('::');
      return {
        param: arr[0],
        value: arr[1],
        time: arr[2],
        name: arr[3],
        node: arr[4]
      };
    },
    getInitialState: function () {
      return { data: [], activeName: null, details: null, names: new Set() };
    },
    componentDidMount: function () {
      this.props.handlers.metric = function (msg) {
        if (this.isMounted()) {
          var newItem = this.parseItem(msg);
          this.state.data.push(newItem);
          var activeName = this.state.activeName !== null ? this.state.activeName : newItem.name;
          this.state.names.add(newItem.name);
          this.setState({ activeName: activeName, newItem: newItem });
        }
      }.bind(this);
    },
    handleChooseTab: function (tab) {
      this.setState({ activeName: tab.props.name, details: null });
    },
    handleChooseRow: function (node) {
      this.setState({ details: { name: this.state.activeName, node: node } });
    },
    render: function () {
      var el;
      if (this.state.data.length === 0) el = React.createElement(
        "div",
        { className: "alert alert-info" },
        "Please wait..."
      );else {
        var activeName = this.state.activeName;
        var activeSystemData = _.groupBy(this.state.data, e => e.name)[activeName];
        el = React.createElement(
          "div",
          { className: "row" },
          React.createElement(
            "div",
            { className: "col-sm-6 col-md-5 col-lg-4" },
            React.createElement(Tabs, { names: Array.from(this.state.names), active: activeName, onChoose: this.handleChooseTab }),
            React.createElement(Table, { nameData: activeSystemData, onChooseRow: this.handleChooseRow })
          ),
          (() => {
            var details = this.state.details;
            if (details !== null) {
              var activeNodeData = _.groupBy(activeSystemData, e => e.node)[details.node];
              return React.createElement(
                "div",
                { className: "col-sm-6 col-md-5 col-lg-8" },
                React.createElement(
                  "div",
                  { className: "row" },
                  React.createElement(
                    "div",
                    { className: "col-xs-12" },
                    React.createElement(
                      "h3",
                      { style: { marginTop: 0 } },
                      details.name,
                      "@",
                      details.node
                    )
                  )
                ),
                React.createElement(
                  "div",
                  { className: "row" },
                  React.createElement(
                    "div",
                    { className: "col-lg-6" },
                    React.createElement(
                      "h4",
                      null,
                      "Metrics"
                    ),
                    React.createElement(Metrics, { data: activeNodeData,
                      name: details.name,
                      node: details.node })
                  )
                )
              );
            }
          })()
        );
      }
      return React.createElement(
        "div",
        { className: "col-xs-12" },
        el
      );
    }
  });

  var Tabs = React.createClass({
    displayName: "Tabs",

    render: function () {
      var tabs = this.props.names.map(function (name, i) {
        var active = name === this.props.active;
        return React.createElement(Tab, { name: name, active: active, onChoose: this.props.onChoose, key: i });
      }.bind(this));
      return React.createElement(
        "ul",
        { className: "nav nav-pills" },
        tabs
      );
    }
  });

  var Tab = React.createClass({
    displayName: "Tab",

    handleChoose: function () {
      this.props.onChoose(this);
    },
    render: function () {
      var className = this.props.active ? 'active' : '';
      return React.createElement(
        "li",
        { role: "presentation", className: className },
        React.createElement(
          "a",
          { href: "#", onClick: this.handleChoose },
          this.props.name
        )
      );
    }
  });

  var Table = React.createClass({
    displayName: "Table",

    render: function () {
      var nameData = this.props.nameData;
      var groupedData = _.groupBy(nameData, data => data.node);
      var rows = [];
      _.forIn(groupedData, function (value, key) {
        rows.push(React.createElement(Row, { node: key,
          nodeData: groupedData[key],
          onChoose: this.props.onChooseRow
        }));
      }.bind(this));

      var minWidth = { width: '1px' };
      return React.createElement(
        "div",
        { className: "table-responsive", style: { marginTop: '10px' } },
        React.createElement(
          "table",
          { className: "table" },
          React.createElement(
            "thead",
            null,
            React.createElement(
              "tr",
              null,
              React.createElement(
                "th",
                null,
                "Node"
              ),
              React.createElement(
                "th",
                { style: minWidth },
                "Status"
              )
            )
          ),
          React.createElement(
            "tbody",
            null,
            rows
          )
        )
      );
    }
  });

  var Row = React.createClass({
    displayName: "Row",

    componentDidMount: function () {
      this.timer = setInterval(this.tick, 1000);
    },
    componentWillUnmount: function () {
      clearInterval(this.timer);
    },
    tick: function () {
      this.forceUpdate();
    },
    handleChoose: function () {
      this.props.onChoose(this.props.node);
    },
    render: function () {
      var lastData = this.props.nodeData[this.props.nodeData.length - 1];
      var elapsed = Math.floor((new Date() - lastData["time"]) / 1000);
      var active = elapsed < 5;
      return React.createElement(
        "tr",
        { className: active ? "success" : "danger", style: { cursor: 'pointer' }, onClick: this.handleChoose },
        React.createElement(
          "td",
          null,
          this.props.node
        ),
        React.createElement(
          "td",
          null,
          (() => {
            if (active) return React.createElement(
              "div",
              { style: { textAlign: 'center' } },
              "OK"
            );else return React.createElement(
              "div",
              null,
              React.createElement(
                "span",
                { style: { whiteSpace: 'nowrap' } },
                secToTimeInterval(elapsed)
              ),
              " ago"
            );
          })()
        )
      );
    }
  });

  var Metrics = React.createClass({
    displayName: "Metrics",

    render: function () {
      var data = this.props.data;

      var uptime = _.findLast(data, function (obj) {
        return obj.param == 'sys.uptime';
      });

      var cpu = _.findLast(data, function (obj) {
        return obj.param == 'cpu.load';
      });

      var memTotal = _.findLast(data, function (obj) {
        return obj.param == 'mem.total';
      });
      var memUsed = _.findLast(data, function (obj) {
        return obj.param == 'mem.used';
      });

      var fsTotal = _.findLast(data, function (obj) {
        return obj.param == 'root./.total';
      });
      var fsUsed = _.findLast(data, function (obj) {
        return obj.param == 'root./.used';
      });

      var cpuVal = cpu === undefined ? 0 : Math.round(cpu.value * 100);
      var memVal = memTotal === undefined || memUsed === undefined || memTotal.value == 0 ? 0 : Math.round(memUsed.value / memTotal.value * 100);
      var fsVal = fsTotal === undefined || fsUsed === undefined || fsTotal.value == 0 ? 0 : Math.round(fsUsed.value / fsTotal.value * 100);

      var cpu = _.filter(data, function (stat) {
        return stat.param == 'cpu.load';
      });
      var cpuData = _.map(cpu, function (e) {
        return [new Date(+e.time), Math.round(e.value * 100)];
      });

      return React.createElement(
        "div",
        null,
        React.createElement(
          "ul",
          { className: "list-group" },
          React.createElement(
            "li",
            { className: "list-group-item" },
            React.createElement(
              "span",
              { className: "badge" },
              secToTimeInterval(uptime === undefined ? 0 : uptime.value)
            ),
            "Uptime"
          )
        ),
        React.createElement(
          "div",
          null,
          React.createElement(StatsGauge, { gauge_id: 'gauge_' + this.props.name + '_' + this.props.node, cpu_val: cpuVal, mem_val: memVal, fs_val: fsVal })
        ),
        React.createElement(
          "div",
          null,
          React.createElement(CpuChart, { cpu_id: 'cpu_line_chart_' + this.props.name + '_' + this.props.node, cpuData: cpuData })
        )
      );
    }
  });

  var StatsGauge = React.createClass({
    displayName: "StatsGauge",

    getInitialState: function () {
      var data = google.visualization.arrayToDataTable([['Label', 'Value'], ['Memory', 0], ['CPU', 0], ['FS', 0]]);

      var max = +this.props.max;
      var options = {
        width: 400, height: 120,
        redFrom: 90, redTo: 100,
        yellowFrom: 75, yellowTo: 90,
        minorTicks: 5
      };
      return { options: options, data: data, chart: null };
    },
    componentDidMount: function () {
      var chart = new google.visualization.Gauge(document.getElementById(this.props.gauge_id));
      this.setState({ chart: chart });
      this.draw();
    },
    componentDidUpdate: function () {
      this.draw();
    },
    draw: function () {
      this.state.data.setValue(0, 1, this.props.mem_val);
      this.state.data.setValue(1, 1, this.props.cpu_val);
      this.state.data.setValue(2, 1, this.props.fs_val);
      if (this.state.chart !== null) this.state.chart.draw(this.state.data, this.state.options);
    },
    render: function () {
      return React.createElement("div", { id: this.props.gauge_id });
    }
  });

  var CpuChart = React.createClass({
    displayName: "CpuChart",

    getInitialState: function () {

      var data = new google.visualization.DataTable();
      data.addColumn('date', 'Time');
      data.addColumn('number', 'CPU');

      var options = {
        chart: {
          title: 'CPU usage',
          subtitle: 'in %'
        },
        width: 600,
        height: 200
      };
      return { options: options, data: data, chart: null };
    },

    componentDidMount: function () {
      var chart = new google.charts.Line(document.getElementById(this.props.cpu_id));
      this.setState({ chart: chart });
      this.draw();
    },
    componentDidUpdate: function () {
      this.draw();
    },
    draw: function () {
      var nr = this.state.data.getNumberOfRows();
      if (this.props.cpuData.length > nr) {
        this.state.data.insertRows(nr, this.props.cpuData.slice(nr - 1, this.props.cpuData.length));
        if (this.state.chart !== null) this.state.chart.draw(this.state.data, this.state.options);
      }
    },
    render: function () {
      return React.createElement("div", { id: this.props.cpu_id });
    }
  });

  return Nodes;
}();