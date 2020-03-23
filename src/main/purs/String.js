"use strict"

exports.startsWith = function(searchString) {
  return function (str) {
    return str.startsWith(searchString)
  }
}

exports.includes = function(searchString) {
  return function (str) {
    return str.includes(searchString)
  }
}
