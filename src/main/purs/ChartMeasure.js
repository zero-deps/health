"use strict"

exports.destroyChart = function(chart) {
  return function() {
    chart.destroy()
  }
}

exports.updateChart = function(chart) {
  return function(values) {
    return function() {
      chart.config.data.datasets[0].data = values.points
      chart.config.data.datasets[0].customLabels = values.labels
      chart.update()
    }
  }
}

exports.createChart = function(ref) {
  return function(values) {
    return function() {
      const ctx = ref.current.getContext('2d')

      const gradientStroke = ctx.createLinearGradient(0, 230, 0, 50);
      gradientStroke.addColorStop(1, 'rgba(29,140,248,0.2)');
      gradientStroke.addColorStop(0.4, 'rgba(29,140,248,0.0)');
      gradientStroke.addColorStop(0, 'rgba(29,140,248,0)'); //blue colors

      return new Chart(ctx, {
        type: 'line',
        responsive: true,
        legend: {
          display: false
        },
        data: {
          labels: values.points.map(function() { return '' }),
          datasets: [{
            fill: true,
            borderColor: '#1f8ef1',
            borderWidth: 2,
            borderDash: [],
            borderDashOffset: 0.0,
            data: values.points,
            customLabels: values.labels,
            pointBackgroundColor: '#1f8ef1',
            pointBorderColor: 'rgba(255,255,255,0)',
            pointBorderWidth: 20,
            pointHoverBackgroundColor: '#1f8ef1',
            pointHoverBorderWidth: 15,
            pointHoverRadius: 4,
            pointRadius: 4,
          }]
        },
        options: {
          maintainAspectRatio: false,
          legend: {
            display: false
          },
          tooltips: {
            backgroundColor: '#f5f5f5',
            titleFontColor: '#333',
            bodyFontColor: '#666',
            bodySpacing: 4,
            xPadding: 12,
            mode: "nearest",
            intersect: 0,
            position: "nearest",
            callbacks: {
              title: function(items, data) { return data.datasets[0].customLabels[items[0].index] },
              label: function(item, data) { return item.yLabel+" ms" },
            },
          },
          responsive: true,
          scales: {
            yAxes: [{
              id: 'left-y-axis',
              position: 'left',
              barPercentage: 1.6,
              gridLines: {
                drawBorder: false,
                color: 'rgba(29,140,248,0.0)',
                zeroLineColor: "transparent",
              },
              ticks: {
                fontColor: "#9a9a9a",
                callback: function(value) { return value+" ms" },
                min: 0,
              }
            }],
            xAxes: [{
              display: false,
            }]
          }
        }
      })
    }
  }
}
