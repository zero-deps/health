var update=React.addons.update;var UserHistory=function(){var MsgCell=React.createClass({displayName:'MsgCell',getInitialState:function(){return {details:false};},handleToggleMsg:function(){this.setState({details:!this.state.details});},render:function(){if(this.state.details)return React.createElement('td',{style:{cursor:'zoom-out'},onClick:this.handleToggleMsg},React.createElement('pre',null,JSON.stringify(this.props.msg,null,' ')));else return React.createElement('td',{style:{cursor:'zoom-in'},onClick:this.handleToggleMsg,title:JSON.stringify(this.props.msg)},this.props.msg.$type);}});return React.createClass({parseIn:function(str){var arr=str.split('::');var msg=JSON.parse(arr[3]);var cid=msg["correlationId"];return {casino:arr[0],user:arr[1],time:arr[2],msg:msg,cid:cid};},getInitialState:function(){return {data:[]};},componentDidMount:function(){this.props.handlers.history=function(inStr){if(this.isMounted())this.setState({data:update(this.state.data,{$unshift:[this.parseIn(inStr)]})});}.bind(this);},render:function(){var rows=this.state.data.reduce(function(acc,item,i,data){if(i===0)strip=true;else {var last=acc[acc.length-1].strip;strip=item.cid===data[i-1].cid?last:!last;}return update(acc,{$push:[{strip:strip,item:item,i:i}]});},[]).map(function(x){var rowClass=x.strip?'active':'';var userStyle={whiteSpace:'nowrap'};var timeStyle={whiteSpace:'pre'};var item=x.item;var time=new Date(parseInt(item.time));time=''+('0'+time.getDate()).slice(-2)+'.'+('0'+time.getMonth()).slice(-2)+'.'+time.getFullYear()+'\n'+('0'+time.getHours()).slice(-2)+':'+('0'+time.getMinutes()).slice(-2)+':'+('0'+time.getSeconds()).slice(-2);return React.createElement('tr',{className:rowClass,key:x.i},React.createElement('td',null,item.casino),React.createElement('td',{style:userStyle},item.user),React.createElement(MsgCell,{msg:item.msg}),React.createElement('td',null,item.cid),React.createElement('td',{style:timeStyle},time));});return React.createElement('div',null,React.createElement('h1',null,'History'),React.createElement('div',{className:'table-responsive'},React.createElement('table',{className:'table table-hover'},React.createElement('thead',null,React.createElement('tr',null,React.createElement('th',null,'Casino'),React.createElement('th',null,'User'),React.createElement('th',null,'Message'),React.createElement('th',{title:'Correlation ID',style:{cursor:'help'}},'CID'),React.createElement('th',null,'Time'))),React.createElement('tbody',null,rows))));}});}();