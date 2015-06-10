Array.prototype.flatMap = function() {
  return [].concat.apply([], this);
};

Array.prototype.distinct = function() {
  return this.filter(function(v, i, a) {
    return a.indexOf(v) == i;
  });
};

Number.prototype.toUnits = function() {
  var units = [
    [                  1, "second", "seconds"],
    [                 60, "minute", "minutes"],
    [               3600,   "hour",   "hours"],
    [          24 * 3600,    "day",    "days"],
    [      7 * 24 * 3600,   "week",   "weeks"],
    [30.4368 * 24 * 3600,  "month",  "months"],
    [365.242 * 24 * 3600,   "year",   "years"]
  ];
  var that = this;
  return units.reverse().reduce(function(acc, data) {
    var seconds = data[0];
    var singular = data[1];
    var plural = data[2];
    var integer = Math.floor(that / seconds);
    that -= integer * seconds;
    if (integer === 0) return acc;
    else if (integer === 1) return acc.concat(1, singular);
    else return acc.concat(integer, plural);
  }, []).join(' ');
};