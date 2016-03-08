var Nodes = (function(){

  var Nodes = React.createClass({
    parseItem: function(rawData) {
      var arr = rawData.split('::');
      return {
        name:  arr[0],
        node:  arr[1],
        param: arr[2],
        time:  arr[3],
        value: arr[4]
      }
    },
    add: function(oldData,newItem) {
      // copy old data
      var result = {};
      for (let name of Object.keys(oldData)) {
        result[name] = {};
        for (let node of Object.keys(oldData[name])) {
          result[name][node] = {};
          result[name][node]['time'] = oldData[name][node]['time'];
          result[name][node]['param'] = {};
          for (let param of Object.keys(oldData[name][node]['param']))
            result[name][node]['param'][param] = oldData[name][node]['param'][param];
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
    getInitialState: function() {
      return {data:{},activeName:null,details:null}
    },
    componentDidMount: function() {
      this.props.handlers.metric = function(msg) {
        if (this.isMounted()) {
          var oldData = this.state.data;
          var newItem = this.parseItem(msg);
          var data = this.add(oldData,newItem);
          var activeName = this.state.activeName !== null ? this.state.activeName : newItem.name;
          this.setState({data:data,activeName:activeName})
        }
      }.bind(this);
    },
    handleChooseTab: function(tab) {
      this.setState({activeName:tab.props.name})
    },
    handleChooseRow: function(node) {
      this.setState({details:{name:this.state.activeName,node:node}})
    },
    render: function() {
      var data = this.state.data;
      var names = Object.keys(data);
      var el;
      if (names.length === 0)
        el = <div className="alert alert-info">Please wait...</div>
      else {
        var activeName = this.state.activeName;
        el =
          <div className="row">
            <div className="col-sm-6 col-md-5 col-lg-4">
              <Tabs names={names} active={activeName} onChoose={this.handleChooseTab} />
              <Table nameData={data[activeName]} onChooseRow={this.handleChooseRow} />
            </div>
            {(() => {
            var details = this.state.details;
            if (details !== null) return (
            <div className="col-sm-6 col-md-5 col-lg-4">
              <h3 style={{marginTop:0}}>{details.name}@{details.node}</h3>
              <h4>Metrics</h4>
              <Metrics data={data[details.name][details.node]['param']} />
            </div>
            )})()}
          </div>
      }
      return <div className="col-xs-12">{el}</div>
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
                    onChoose={this.props.onChooseRow}
                    key={i} />
      }.bind(this));
      var minWidth = {width:'1px'}
      return (
        <div className="table-responsive" style={{marginTop:'10px'}}>
          <table className="table">
            <thead>
              <tr>
                <th>Node</th>
                <th style={minWidth}>Status</th>
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
    render: function() {
      var elapsed = Math.floor((new Date() - this.props.nodeData["time"]) / 1000);
      return (
        <tr className={elapsed < 3 ? "success" : "danger"} style={{cursor:'pointer'}} onClick={this.handleChoose}>
          <td>{this.props.node}</td>
          <td>
          {(() => {
            if (elapsed < 3) return "OK";
            else return <span><span style={{whiteSpace:'nowrap'}}>{elapsed.toUnits()}</span> ago</span>;
          })()}
          </td>
        </tr>
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