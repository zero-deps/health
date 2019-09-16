"use strict"

exports.destroyChart = function(chart) {
  return function() {
    chart.destroy()
  }
}

exports.updateChart = function(chart) {
  return function(values) {
    return function() {
      chart.config.data.datasets[0].data = values.cpuPoints
      chart.config.data.datasets[1].data = values.actPoints
      chart.config.data.datasets[1].actLabels = values.actLabels
      chart.config.data.datasets[2].data = values.memPoints
      chart.update()
    }
  }
}

exports.createChart = function(ref) {
  return function(values) {
    return function() {
      const ctx = ref.current.getContext('2d')
      
      const purpleBg = ctx.createLinearGradient(0, 230, 0, 50)
      purpleBg.addColorStop(1, 'rgba(72,72,176,0.1)')
      purpleBg.addColorStop(0.4, 'rgba(72,72,176,0.0)')
      purpleBg.addColorStop(0, 'rgba(119,52,169,0)')
      
      return new Chart(ctx, {
        type: 'line',
        data: {
          datasets: [{
            backgroundColor: purpleBg,
            borderColor: '#d346b1',
            borderDash: [],
            borderDashOffset: 0.0,
            borderWidth: 2,
            cubicInterpolationMode: 'monotone',
            data: values.cpuPoints,
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
          }, {
            borderColor: '#1f8ef1',
            borderDash: [],
            borderDashOffset: 0.0,
            borderWidth: 2,
            actLabels: values.actLabels,
            data: values.actPoints,
            fill: false,
            label: "Events",
            pointBackgroundColor: '#1f8ef1',
            pointBorderColor: 'rgba(255,255,255,0)',
            pointBorderWidth: 20,
            pointHoverBackgroundColor: '#1f8ef1',
            pointHoverBorderWidth: 15,
            pointHoverRadius: 4,
            pointRadius: 4,
            showLine: false,
            yAxisID: 'left-y-axis'
          }, {
            borderColor: '#00d6b4',
            borderDash: [],
            borderDashOffset: 0.0,
            borderWidth: 2,
            data: values.memPoints,
            fill: false,
            label: "Heap Memory Usage",
            lineTension: 0,
            pointBackgroundColor: '#00d6b4',
            pointBorderColor: 'rgba(255,255,255,0)',
            pointBorderWidth: 20,
            pointHoverBackgroundColor: '#00d6b4',
            pointHoverBorderWidth: 15,
            pointHoverRadius: 4,
            pointRadius: 4,
            yAxisID: 'right-y-axis'
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
                  case 0: return datasetLabel + item.yLabel + "%"
                  case 1: return datasetLabel + data.datasets[1].actLabels[item.index]
                  case 2: return datasetLabel + item.yLabel + " MB"
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
                fontColor: "#9a9a9a",
                callback: function(value) { return value+"%" },
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
                fontColor: "#9a9a9a",
                //stepSize: 0.5,
                callback: function(value) { return value+" MB" },
              },
              gridLines: {
                drawOnChartArea: false,
              },
            }],
            xAxes: [{
              type: 'time',
              time: {
                tooltipFormat: "DD.MM.YYYY HH:mm:ss",
              },
              barPercentage: 1.6,
              gridLines: {
                drawBorder: false,
                color: 'rgba(225,78,202,0.1)',
                zeroLineColor: "transparent",
              },
              ticks: {
                fontColor: "#9a9a9a"
              }
            }]
          }
        }
      })
    }
  }
}
