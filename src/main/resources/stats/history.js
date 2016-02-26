var UserHistory = React.createClass({
  parseIn: function(str) {
    var arr = str.split('::');
    return {casino:arr[0],user:arr[1],time:arr[2],msg:arr[3],cid:arr[4]};
  },
  getInitialState: function() {
    return {data:Immutable.List()};
  },
  componentDidMount: function() {
    this.props.handlers.msg = function(inStr) {
      if (this.isMounted())
        this.setState({data:this.state.data.unshift(this.parseIn(inStr))});
    }.bind(this);
  },
  render: function() {
    var rows = this.state.data.map(function(item) {
      var time = new Date(parseInt(item.time)).toString();
      var userStyle = timeStyle = { whiteSpace: 'nowrap' };
      return (
        <tr>
          <td>{item.casino}</td>
          <td style={userStyle}>{item.user}</td>
          <td>{item.msg}</td>
          <td>{item.cid}</td>
          <td style={timeStyle}>{time}</td>
        </tr>
      );
    });
    return (
      <table className="table table-hover">
        <caption>User History</caption>
        <thead>
          <tr>
            <th>Casino</th>
            <th>User</th>
            <th>Message</th>
            <th>Correlation</th>
            <th>Time</th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    );
  }
});
