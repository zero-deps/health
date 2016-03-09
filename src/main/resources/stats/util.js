function id(id) {
  return document.getElementById(id);
}

Array.prototype.flatten = function() {
  return [].concat.apply([], this);
};

Array.prototype.distinct = function() {
  return this.filter(function(v, i, a) {
    return a.indexOf(v) == i;
  });
};

function secToTimeInterval(sec) {
  if (sec === NaN) return NaN.toString();
  var units = [
    [        1, "s"],
    [       60, "m"],
    [     3600, "h"],
    [24 * 3600, "d"]
  ];
  return units.reduceRight(function(prev,curr) {
    var seconds = curr[0],
        unit = curr[1];
    if (prev.parts.length >= 2) return prev;
    var count = Math.floor(prev.remainder / seconds);
    remainder = prev.remainder - count * seconds;
    if (count === 0) return {remainder:remainder,parts:prev.parts};
    else return {remainder:remainder,parts:prev.parts.concat(count+unit)};
  },{remainder:sec,parts:[]}).parts.join(' ');
};

function nsToMs(ns) {
  if (ns === NaN) return NaN.toString();
  return (ns / 1000 / 1000).toFixed(3);
};

function bytesToMb(bytes) {
  if (bytes === NaN) return NaN.toString();
  return (bytes / 1024 / 1024).toFixed(1);
}

function bytesToGb(bytes) {
  if (bytes === NaN) return NaN.toString();
  return (bytes / 1024 / 1024 / 1024).toFixed(3);
}

var nonEmpty = function(value) {
  return value !== ""
}
