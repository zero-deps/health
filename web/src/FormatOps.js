"use strict"

exports.formatNum = num => new Intl.NumberFormat("uk-UA").format(num)

exports.formatLocal = options => new Intl.DateTimeFormat('uk-UA', options).format
