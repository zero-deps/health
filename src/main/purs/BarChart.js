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
        type: 'bar',
        responsive: true,
        legend: {
          display: false
        },
        data: {
          labels: ['1','2','3','4','5'],
          datasets: [{
            fill: true,
            backgroundColor: gradientStroke,
            hoverBackgroundColor: gradientStroke,
            borderColor: '#1f8ef1',
            borderWidth: 2,
            borderDash: [],
            borderDashOffset: 0.0,
            data: values.points,
            customLabels: values.labels,
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
              label: function(item, data) { return item.yLabel + " ms" },
            },
          },
          responsive: true,
          scales: {
            yAxes: [{
              gridLines: {
                drawBorder: false,
                color: 'rgba(29,140,248,0.1)',
                zeroLineColor: "transparent",
              },
              ticks: {
                suggestedMin: 0,
                padding: 20,
                fontColor: "#9e9e9e"
              }
            }],
            xAxes: [{
              display: false,
              gridLines: {
                drawBorder: false,
                color: 'rgba(29,140,248,0.1)',
                zeroLineColor: "transparent",
              },
              ticks: {
                padding: 20,
                fontColor: "#9e9e9e"
              }
            }]
          }
        }
      })
    }
  }
}
