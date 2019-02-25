"use strict"

var chart = null
var actionsMap = new Map()

exports.destroyChart = function() {
  if (chart === null) {
    console.error("chart is not created")
    return
  }
  chart.destroy()
  chart = null
}

exports.updateChart = function(data) {
  return function() {
    if (chart === null) {
      console.error("chart is not created")
      return
    }
    var data = chart.config.data
    data.datasets[0].data = cpuLoad
    chart.update()
  }
}

exports.createChart = function(ref) {
  return function(data) {
    return function() {
      if (chart !== null) {
        console.error("chart already exists")
        return
      }
      const ctx = ref.current.getContext('2d')
      const purpleBg = ctx.createLinearGradient(0, 230, 0, 50)
      purpleBg.addColorStop(1, 'rgba(72,72,176,0.1)')
      purpleBg.addColorStop(0.4, 'rgba(72,72,176,0.0)')
      purpleBg.addColorStop(0, 'rgba(119,52,169,0)')
      return chart = new Chart(ctx, {
        type: 'line',
        data: {
          datasets: [{
            backgroundColor: purpleBg,
            borderColor: '#d346b1',
            borderDash: [],
            borderDashOffset: 0.0,
            borderWidth: 2,
            cubicInterpolationMode: 'monotone',
            data: data,
            fill: true,
            label: "CPU Load",
            pointBackgroundColor: '#d346b1',
            pointBorderColor: 'rgba(255,255,255,0)',
            pointBorderWidth: 20,
            pointHoverBackgroundColor: '#d346b1',
            pointHoverBorderWidth: 15,
            pointHoverRadius: 4,
            pointRadius: 4,
            yAxisID: 'left-y-axis'
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
            position: "nearest",
            callbacks: {
              label: function(item, data) {
                var datasetIndex = item.datasetIndex
                var dataset = data.datasets[item.datasetIndex]
                var datasetLabel = dataset.label + ": "
                switch (datasetIndex) {
                  case 0:
                    return datasetLabel + item.yLabel + "%"
                  case 1:
                    return datasetLabel + actionsMap.get(dataset.data[item.index].t)
                  case 2:
                    return datasetLabel + item.yLabel + " GB"
                }
              },
            },
          },
          responsive: true,
          scales: {
            yAxes: [{
              id: 'left-y-axis',
              type: 'logarithmic',
              position: 'left',
              barPercentage: 1.6,
              gridLines: {
                drawBorder: false,
                color: 'rgba(29,140,248,0.0)',
                zeroLineColor: "transparent",
              },
              ticks: {
                suggestedMin: 0,
                suggestedMax: 5,
                padding: 20,
                fontColor: "#9a9a9a",
                callback: function(value, index, values) {
                  return value + "%";
                },
              }
            }, {
              id: 'right-y-axis',
              type: 'linear',
              position: 'right',
              barPercentage: 1.6,
              gridLines: {
                drawBorder: false,
                color: 'rgba(29,140,248,0.0)',
                zeroLineColor: "transparent",
              },
              ticks: {
                padding: 20,
                fontColor: "#9a9a9a",
                stepSize: 0.5,
                callback: function(value, index, values) {
                  return value + " GB";
                },
              },
              gridLines: {
                drawOnChartArea: false,
              },
            }],
            xAxes: [{
              type: 'time',
              barPercentage: 1.6,
              gridLines: {
                drawBorder: false,
                color: 'rgba(225,78,202,0.1)',
                zeroLineColor: "transparent",
              },
              ticks: {
                padding: 20,
                fontColor: "#9a9a9a"
              }
            }]
          }
        }
      })
    }
  }
}
