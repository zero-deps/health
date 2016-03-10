var update = React.addons.update;

var StackTrace = React.createClass({
	getInitialState: function() {
		return { showTrace: false };
	},
	
	onClick: function(event) {
		var newState = (! this.state.showTrace);
		this.setState({ showTrace: newState });
	},
	
	render: function() {
		var stackTraces = [];
		for(i = 1; i < this.props.errors.length; i++){
			stackTraces[2*i] =  this.props.errors[i];
			stackTraces[2*i+1] = React.createElement('br');
		}
		return (<div onClick={this.onClick} style={{cursor:"pointer"}}>
			{this.props.errors[0]}
			<div >
				{this.state.showTrace ? stackTraces : ""}
			</div>
		</div>);
	}
})

var Errors = React.createClass({
	stackTraces: function(json) {
		if (json) {
			return JSON.parse('{"stackTrace":' + json + '}').stackTrace;
		} else {
			return new Array();
		}
	},
	parseIn: function(str) {
		var arr = str.split('::');
		var stackTraces = this.stackTraces(arr[4]);
		var errors = [];
		for(i = 0; i < stackTraces.length; i++){
			errors[i] = "at " + stackTraces[i].className + "." + stackTraces[i].method + " (" + stackTraces[i].fileName + ":" + stackTraces[i].lineNumber + ")";
		}
		return {name:arr[0],node:arr[1],time:arr[2],msg:arr[3], errors:errors, title: errors[0]};
	},
	
	getInitialState: function() {
		return {data:[]};
	},
	
	componentDidMount: function() {
		this.props.handlers.error = function(inStr) {
	        if (this.isMounted())
	          this.setState({data:update(this.state.data,{$unshift:[this.parseIn(inStr)]})});
	      }.bind(this);
	},
    
	render: function() {
		var errors = this.state.data.map(function(error) {
			return <tr onClick={this.onClick}>
				<td>{error.name}</td>
				<td>{error.node}</td>
				<td>{error.time}</td>
				<td>{error.msg}</td>
				<td> <StackTrace errors = {error.errors}/></td>
			</tr>;
		});
		    		
        return  (
        		<div>
			        <h1>History</h1>
			        <div className="table-responsive">
			          <table className="table table-hover">
			            <thead>
			              <tr>
			                <th>Name</th>
			                <th>Node</th>
			                <th>Time</th>
			                <th>Message</th>
			                <th>StackTrace</th>
			              </tr>
			            </thead>
			            <tbody>{errors}</tbody>
			          </table>
			        </div>
			      </div>
      )
	}
})