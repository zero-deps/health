var Nodes = (function(){

  var Nodes = React.createClass({
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
      return { data: {}, activeName: null, nodeDetails: null };
    },
    componentDidMount: function() {
      this.props.handlers.metric = function(newDataRaw) {
        if (this.isMounted()) {
          var state = this.packData(this.parseData(newDataRaw), this.state.data);
          this.setState(state);
        }
      }.bind(this);
    },
    handleChooseTab: function(tab) {
      this.setState({activeName: tab.props.name});
    },
    handleChooseRow: function(node) {
      this.setState({nodeDetails:node})
    },
    handleRemoveRow: function(node) {
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
      var el;
      if (names.length === 0)
        el = <div className="alert alert-info">Please wait...</div>
      else {
        var activeName = this.state.activeName;
        if (names.indexOf(activeName) == -1) activeName = names[0];
        el =
          <div className="row">
            <div className="col-sm-6 col-md-5 col-lg-4">
              <Tabs names={names} active={activeName} onChoose={this.handleChooseTab} />
              <Table nameData={data[activeName]} onRemoveRow={this.handleRemoveRow} onChooseRow={this.handleChooseRow} />
            </div>
            {(() => {
              var nodeDetails = this.state.nodeDetails;
              if (nodeDetails !== null) return (
                <div className="col-sm-6 col-md-5 col-lg-4">
                  <h3>{activeName}@{nodeDetails}</h3>
                  <h4>Metrics</h4>
                  <Metrics data={data[activeName][nodeDetails]['param']} />
                </div>
              )
            })()}
          </div>
      }
      return (
        <div className="col-xs-12">
          <h1>Nodes</h1>
          {el}
        </div>
      )
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

      var rows = Object.keys(nameData).map(function(node,i) {
        return <Row node={node}
                    nodeData={nameData[node]}
                    onRemove={this.props.onRemoveRow}
                    onChoose={this.props.onChooseRow}
                    key={i} />
      }.bind(this));

      return (
        <div className="table-responsive" style={{marginTop:'10px'}}>
          <table className="table">
            <thead>
              <tr>
                <th>Node</th>
                <th style={{width:'1px'}}>Status</th>
                <th style={{width:'1px'}}></th>
              </tr>
            </thead>
            <tbody>{rows}</tbody>
          </table>
        </div>
      );
    }
  });

  var Row = React.createClass({
    componentDidMount: function() {
      this.timer = setInterval(this.tick, 1000);
    },
    componentWillUnmount: function() {
      clearInterval(this.timer);
    },
    tick: function() {
      this.forceUpdate();
    },
    handleChoose: function() {
      this.props.onChoose(this.props.node);
    },
    handleRemove: function() {
      this.props.onRemove(this.props.node);
    },
    render: function() {
      var elapsed = Math.floor((new Date() - this.props.nodeData["time"]) / 1000);
      return (
        <tr className={elapsed < 3 ? "success" : "danger"} style={{cursor:'pointer'}} onClick={this.handleChoose}>
          <td>{this.props.node}</td>
          <LastUpdatedCell elapsed={elapsed} />
          <RemoveCell onRemove={this.handleRemove} />
        </tr>
      );
    }
  });

  var LastUpdatedCell = React.createClass({
    render: function() {
      var elapsed = this.props.elapsed;
      var text = elapsed < 3 ? "OK" : elapsed.toUnits() + " ago";
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
          <span style={{cursor:'pointer'}}
                onClick={this.handleRemove}>
            <span className="glyphicon glyphicon-remove-circle" aria-hidden="true"></span>
          </span>
          <span className="sr-only">Remove</span>
        </td>
      );
    }
  });

  var Metrics = React.createClass({
    render: function() {
      var data = this.props.data;
      return (
        <ul className="list-group">
          <li className="list-group-item">
            <span className="badge">{data['CPU'] ? data['CPU'] : 'N/A'}</span>
            CPU
          </li>
          <li className="list-group-item">
            <span className="badge">{data['Heap'] ? data['Heap'] : 'N/A'}</span>
            Heap
          </li>
        </ul>
      );
    }
  });

  return Nodes;
})();