/********************************************************\
| WARNING:                                               |
| Do not edit `src\main\resources\stats\metrics.js file. |
| It is autogenerated.                                   |
| Make changes in `src\main\jsx\metrics.js`.             |
\********************************************************/

var Nodes = function () {

  var Nodes = React.createClass({
    displayName: 'Nodes',

    parseItem: function (rawData) {
      var arr = rawData.split('::');
      return {
        name: arr[0],
        node: arr[1],
        param: arr[2],
        time: arr[3],
        value: arr[4]
      };
    },
    add: function (oldData, newItem) {
      // copy old data
      var result = {};
      for (name of Object.keys(oldData)) {
        result[name] = {};
        for (node of Object.keys(oldData[name])) {
          result[name][node] = {};
          result[name][node]['time'] = oldData[name][node]['time'];
          result[name][node]['param'] = {};
          for (param of Object.keys(oldData[name][node]['param'])) result[name][node]['param'][param] = oldData[name][node]['param'][param];
        }
      }
      // add new item
      result[newItem.name] = result[newItem.name] || {};
      result[newItem.name][newItem.node] = result[newItem.name][newItem.node] || {};
      result[newItem.name][newItem.node]['param'] = result[newItem.name][newItem.node]['param'] || {};
      result[newItem.name][newItem.node]['param'][newItem.param] = newItem.value;
      result[newItem.name][newItem.node]['time'] = newItem.time;
      return result;
    },
    getInitialState: function () {
      return { data: {}, activeName: null, details: null };
    },
    componentDidMount: function () {
      this.props.handlers.metric = function (msg) {
        if (this.isMounted()) {
          var oldData = this.state.data;
          var newItem = this.parseItem(msg);
          var data = this.add(oldData, newItem);
          var activeName = this.state.activeName !== null ? this.state.activeName : newItem.name;
          this.setState({ data: data, activeName: activeName });
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
      var data = this.state.data;
      var names = Object.keys(data);
      var el;
      if (names.length === 0) el = React.createElement(
        'div',
        { className: 'alert alert-info' },
        'Please wait...'
      );else {
        var activeName = this.state.activeName;
        el = React.createElement(
          'div',
          { className: 'row' },
          React.createElement(
            'div',
            { className: 'col-sm-6 col-md-5 col-lg-4' },
            React.createElement(Tabs, { names: names, active: activeName, onChoose: this.handleChooseTab }),
            React.createElement(Table, { nameData: data[activeName], onChooseRow: this.handleChooseRow })
          ),
          (() => {
            var details = this.state.details;
            if (details !== null) return React.createElement(
              'div',
              { className: 'col-sm-6 col-md-5 col-lg-8' },
              React.createElement(
                'div',
                { className: 'row' },
                React.createElement(
                  'div',
                  { className: 'col-xs-12' },
                  React.createElement(
                    'h3',
                    { style: { marginTop: 0 } },
                    details.name,
                    '@',
                    details.node
                  )
                )
              ),
              React.createElement(
                'div',
                { className: 'row' },
                (() => {
                  var xs = data[details.name][details.node]['param'];
                  if (Object.keys(xs).some(x => x.startsWith('service.'))) return React.createElement(
                    'div',
                    { className: 'col-lg-6' },
                    React.createElement(
                      'h4',
                      null,
                      'Services'
                    ),
                    React.createElement(Services, { data: data[details.name][details.node]['param'] })
                  );
                })(),
                React.createElement(
                  'div',
                  { className: 'col-lg-6' },
                  React.createElement(
                    'h4',
                    null,
                    'Metrics'
                  ),
                  React.createElement(Metrics, { data: data[details.name][details.node]['param'] })
                )
              )
            );
          })()
        );
      }
      return React.createElement(
        'div',
        { className: 'col-xs-12' },
        el
      );
    }
  });

  var Tabs = React.createClass({
    displayName: 'Tabs',

    render: function () {
      var tabs = this.props.names.map(function (name, i) {
        var active = name === this.props.active;
        return React.createElement(Tab, { name: name, active: active, onChoose: this.props.onChoose, key: i });
      }.bind(this));
      return React.createElement(
        'ul',
        { className: 'nav nav-pills' },
        tabs
      );
    }
  });

  var Tab = React.createClass({
    displayName: 'Tab',

    handleChoose: function () {
      this.props.onChoose(this);
    },
    render: function () {
      var className = this.props.active ? 'active' : '';
      return React.createElement(
        'li',
        { role: 'presentation', className: className },
        React.createElement(
          'a',
          { href: '#', onClick: this.handleChoose },
          this.props.name
        )
      );
    }
  });

  var Table = React.createClass({
    displayName: 'Table',

    render: function () {
      var nameData = this.props.nameData;
      var rows = Object.keys(nameData).map(function (node, i) {
        return React.createElement(Row, { node: node,
          nodeData: nameData[node],
          onChoose: this.props.onChooseRow,
          key: i });
      }.bind(this));
      var minWidth = { width: '1px' };
      return React.createElement(
        'div',
        { className: 'table-responsive', style: { marginTop: '10px' } },
        React.createElement(
          'table',
          { className: 'table' },
          React.createElement(
            'thead',
            null,
            React.createElement(
              'tr',
              null,
              React.createElement(
                'th',
                null,
                'Node'
              ),
              React.createElement(
                'th',
                { style: minWidth },
                'Status'
              )
            )
          ),
          React.createElement(
            'tbody',
            null,
            rows
          )
        )
      );
    }
  });

  var Row = React.createClass({
    displayName: 'Row',

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
      var elapsed = Math.floor((new Date() - this.props.nodeData["time"]) / 1000);
      return React.createElement(
        'tr',
        { className: elapsed < 3 ? "success" : "danger", style: { cursor: 'pointer' }, onClick: this.handleChoose },
        React.createElement(
          'td',
          null,
          this.props.node
        ),
        React.createElement(
          'td',
          null,
          (() => {
            if (elapsed < 3) return React.createElement(
              'div',
              { style: { textAlign: 'center' } },
              'OK'
            );else return React.createElement(
              'div',
              null,
              React.createElement(
                'span',
                { style: { whiteSpace: 'nowrap' } },
                elapsed.toUnits()
              ),
              ' ago'
            );
          })()
        )
      );
    }
  });

  var Services = React.createClass({
    displayName: 'Services',

    format: function (id, name) {
      var x = this.props.data["service." + id];
      if (x === undefined) return '';
      var liClass = 'list-group-item' + (x === 'started' ? ' list-group-item-success' : '') + (x === 'stopped' ? ' list-group-item-danger' : '');
      return React.createElement(
        'li',
        { className: liClass },
        (() => {
          if (x !== 'started' && x !== 'stopped') return React.createElement(
            'span',
            { className: 'badge' },
            x
          );
        })(),
        name
      );
    },
    render: function () {
      return React.createElement(
        'ul',
        { className: 'list-group' },
        this.format('geoip', 'GeoIP'),
        this.format('favorite', 'Favorite')
      );
    }
  });

  var Metrics = React.createClass({
    displayName: 'Metrics',

    format: function (v) {
      return v !== undefined ? v : 'N/A';
    },
    render: function () {
      var data = this.props.data;
      return React.createElement(
        'ul',
        { className: 'list-group' },
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['sys.uptime'])
          ),
          'Uptime'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['cpu.count'])
          ),
          'CPU Count'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['cpu.load'])
          ),
          'CPU Load'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['mem.heap'])
          ),
          'Memory Heap'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['mem.free'])
          ),
          'Memory Free'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['mem.total'])
          ),
          'Memory Total'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['mem.max'])
          ),
          'Memory Max'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['root./.usable'])
          ),
          'FS Usable'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['root./.free'])
          ),
          'FS Free'
        ),
        React.createElement(
          'li',
          { className: 'list-group-item' },
          React.createElement(
            'span',
            { className: 'badge' },
            this.format(data['root./.total'])
          ),
          'FS Total'
        )
      );
    }
  });

  return Nodes;
}();