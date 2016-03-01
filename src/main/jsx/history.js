var update = React.addons.update;

var UserHistory = (function(){

  var MsgCell = React.createClass({
    getInitialState: function() {
      return {details:false};
    },
    handleToggleMsg: function() {
      this.setState({details:!this.state.details});
    },
    render: function() {
      var msg = this.state.details ?
        <pre>{JSON.stringify(this.props.msg,null,' ')}</pre> :
        this.props.msg.$type;
      var cursor = this.state.details ? 'zoom-out' : 'zoom-in';
      return <td style={{cursor:cursor}} onClick={this.handleToggleMsg}>{msg}</td>;
    }
  });

  return React.createClass({
    parseIn: function(str) {
      var arr = str.split('::');
      var msg = JSON.parse(arr[3]);
      var cid = msg["correlationId"];
      return {casino:arr[0],user:arr[1],time:arr[2],msg:msg,cid:cid};
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
            <MsgCell msg={item.msg}/>
            <td>{item.cid}</td>
            <td style={timeStyle}>{time}</td>
          </tr>
        );
      });
      return (
        <div>
          <h1>History</h1>
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Casino</th>
                  <th>User</th>
                  <th>Message</th>
                  <th title="Correlation ID" style={{cursor:'help'}}>CID</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>{rows}</tbody>
            </table>
          </div>
        </div>
      );
    }
  });
})();