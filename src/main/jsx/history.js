var update = React.addons.update;

var UserHistory = React.createClass({
  parseIn: function(str) {
    var arr = str.split('::');
    return {casino:arr[0],user:arr[1],time:arr[2],msg:arr[3],cid:arr[4]};
  },
  getInitialState: function() {
    return {data:[]};
  },
  componentDidMount: function() {
    this.props.handlers.history = function(inStr) {
      if (this.isMounted())
        this.setState({data:update(this.state.data,{$unshift:[this.parseIn(inStr)]})});
    }.bind(this);
  },
  render: function() {
    var rows = this.state.data.reduce(function(acc,item,i,data){
      if (i === 0) strip = true;
      else {
        var last = acc[acc.length-1].strip;
        strip = item.cid === data[i-1].cid ? last : !last;
      }
      return update(acc,{$push:[{strip:strip,item:item,i:i}]});
    },[]).map(function(x){
      var rowClass = x.strip ? 'active' : '';
      var userStyle = timeStyle = {whiteSpace:'nowrap'};
      var item = x.item;
      var time = new Date(parseInt(item.time));
      time = ''+
        ('0'+time.getDate()).slice(-2)+'.'+
        ('0'+time.getMonth()).slice(-2)+'.'+
             time.getFullYear()+' '+
        ('0'+time.getHours()).slice(-2)+':'+
        ('0'+time.getMinutes()).slice(-2)+':'+
        ('0'+time.getSeconds()).slice(-2);
      return (
        <tr className={rowClass} key={x.i}>
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
