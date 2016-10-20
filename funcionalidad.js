google.charts.load('current', {packages: ['corechart', 'line']});
google.charts.setOnLoadCallback(drawCurveTypes);

function drawCurveTypes() {
      var data = new google.visualization.DataTable();
      data.addColumn('number', 'X');
      data.addColumn('number', 'Entrada');
      data.addColumn('number', 'Salida');

      data.addRows(datos);

      var options = {
        hAxis: {
          title: 'Hora'
        },
        vAxis: {
          title: 'Clientes'
        },
        series: {
          1: {curveType: 'none'}
        }
      };

      var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
      chart.draw(data, options);
    }