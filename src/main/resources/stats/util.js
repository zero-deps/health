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

Number.prototype.toUnits = function() {
  var units = [
    [        1, "second", "seconds"],
    [       60, "minute", "minutes"],
    [     3600,   "hour",   "hours"],
    [24 * 3600,    "day",    "days"]
  ];
  return units.reduceRight(function(prev,curr) {
    var seconds = curr[0],
        singular = curr[1],
        plural = curr[2];
    if (prev.words.length / 2 >= 2) return prev;
    var count = Math.floor(prev.remainder / seconds);
    remainder = prev.remainder - count * seconds;
    if (count === 0) return {remainder:remainder,words:prev.words};
    else if (count === 1) return {remainder:remainder,words:prev.words.concat(count,singular)};
    else return {remainder:remainder,words:prev.words.concat(count,plural)};
  },{remainder:this,words:[]}).words.join(' ');
};

var nonEmpty = function(value) {
  return value !== ""
}
