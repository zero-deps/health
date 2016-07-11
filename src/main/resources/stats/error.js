var update = React.addons.update;

var StackTrace = React.createClass({
	displayName: "StackTrace",

	getInitialState: function () {
		return { showTrace: false };
	},

	onClick: function (event) {
		var newState = !this.state.showTrace;
		this.setState({ showTrace: newState });
	},

	render: function () {
		var stackTraces = [];
		for (i = 1; i < this.props.errors.length; i++) {
			stackTraces[2 * i] = this.props.errors[i];
			stackTraces[2 * i + 1] = React.createElement('br');
		}
		return React.createElement(
			"div",
			{ onClick: this.onClick, style: { cursor: "pointer" } },
			this.props.errors[0],
			React.createElement(
				"div",
				null,
				this.state.showTrace ? stackTraces : ""
			)
		);
	}
});

var Errors = React.createClass({
	displayName: "Errors",

	stackTraces: function (json) {
		if (json) {
			return JSON.parse('{"stackTrace":' + json + '}').stackTrace;
		} else {
			return new Array();
		}
	},
	parseIn: function (str) {
		var arr = str.split('::');
		var stackTraces = this.stackTraces(arr[4]);
		var errors = [];
		for (i = 0; i < stackTraces.length; i++) {
			errors[i] = "at " + stackTraces[i].className + "." + stackTraces[i].method + " (" + stackTraces[i].fileName + ":" + stackTraces[i].lineNumber + ")";
		}
		return { name: arr[0], node: arr[1], time: arr[2], msg: arr[3], errors: errors, title: errors[0] };
	},

	getInitialState: function () {
		return { data: [] };
	},

	componentDidMount: function () {
		this.props.handlers.error = function (inStr) {
			if (this.isMounted()) this.setState({ data: update(this.state.data, { $unshift: [this.parseIn(inStr)] }) });
		}.bind(this);
	},

	render: function () {
		var errors = this.state.data.map(function (error) {
			return React.createElement(
				"tr",
				{ onClick: this.onClick },
				React.createElement(
					"td",
					null,
					error.name
				),
				React.createElement(
					"td",
					null,
					error.node
				),
				React.createElement(
					"td",
					null,
					error.time
				),
				React.createElement(
					"td",
					null,
					error.msg
				),
				React.createElement(
					"td",
					null,
					" ",
					React.createElement(StackTrace, { errors: error.errors })
				)
			);
		});

		return React.createElement(
			"div",
			null,
			React.createElement(
				"h1",
				null,
				"History"
			),
			React.createElement(
				"div",
				{ className: "table-responsive" },
				React.createElement(
					"table",
					{ className: "table table-hover" },
					React.createElement(
						"thead",
						null,
						React.createElement(
							"tr",
							null,
							React.createElement(
								"th",
								null,
								"Name"
							),
							React.createElement(
								"th",
								null,
								"Node"
							),
							React.createElement(
								"th",
								null,
								"Time"
							),
							React.createElement(
								"th",
								null,
								"Message"
							),
							React.createElement(
								"th",
								null,
								"StackTrace"
							)
						)
					),
					React.createElement(
						"tbody",
						null,
						errors
					)
				)
			)
		);
	}
});