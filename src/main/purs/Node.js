"use strict"

var chart = null

exports.updateChart = function(cpuLoad) {
  return function() {
    const data = chart.config.data
    data.datasets[0].data = cpuLoad
    chart.update()
  }
}

exports.createChart = function(cpuLoad) {
  return function() {
    const ctx = document.getElementById("chartBig1").getContext('2d')
    const gradientStroke = ctx.createLinearGradient(0, 230, 0, 50)
    gradientStroke.addColorStop(1, 'rgba(72,72,176,0.1)')
    gradientStroke.addColorStop(0.4, 'rgba(72,72,176,0.0)')
    gradientStroke.addColorStop(0, 'rgba(119,52,169,0)') //purple colors
    chart = new Chart(ctx, {
      type: 'line',
      data: {
        datasets: [{
          label: "CPU Load",
          fill: false,
          backgroundColor: gradientStroke,
          borderColor: '#d346b1',
          borderWidth: 2,
          borderDash: [],
          borderDashOffset: 0.0,
          pointBackgroundColor: '#d346b1',
          pointBorderColor: 'rgba(255,255,255,0)',
          pointHoverBackgroundColor: '#d346b1',
          pointBorderWidth: 20,
          pointHoverRadius: 4,
          pointHoverBorderWidth: 15,
          pointRadius: 4,
          data: cpuLoad,
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
          position: "nearest"
        },
        responsive: true,
        scales: {
          yAxes: [{
            barPercentage: 1.6,
            gridLines: {
              drawBorder: false,
              color: 'rgba(29,140,248,0.0)',
              zeroLineColor: "transparent",
            },
            ticks: {
              suggestedMin: 0,
              // suggestedMax: 1,
              padding: 20,
              fontColor: "#9a9a9a"
            }
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
    return chart
  }
}
