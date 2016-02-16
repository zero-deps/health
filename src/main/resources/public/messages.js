var UserHistory = React.createClass({
  parseData: function(data) {
    var list = [].concat(data);
    return list.map(function(item) {
      var arr = item.split('::');
      return {
        casino: arr[0],
        user:   arr[1],
        time:   arr[2],
        msg:    arr[3]
      };
    });
  },
  getInitialState: function() {
    return {data: this.parseData(this.props.data)};
  },
  componentDidMount: function() {
    this.props.handlers.msg = function(newData) {
      if (this.isMounted())
        this.setState({data: this.state.data.concat(this.parseData(newData))});
    }.bind(this);
  },
  render: function() {
    var rows = this.state.data.reverse().map(function(item) {
      var time = new Date(parseInt(item.time)).toString();
      return (
        <tr>
          <td>{item.casino}</td>
          <td>{item.user}</td>
          <td>{item.msg}</td>
          <td>{time}</td>
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
            <th>Time</th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    );
  }
});
