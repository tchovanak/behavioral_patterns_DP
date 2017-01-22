

function heatmap(){

    var yCatCount = 100;
    var xCatCount = 100;
    var height = 110;
    var width = 750;

    var maxRange = 100;

    var selector = "heatmap-container";
    var colorRange = ['#D8E6E7', '#218380'];
    var rectData = [[0.5,0,0], [0.7,0,1]];
    var SQUARE_WIDTH = width / xCatCount;
    var SQUARE_HEIGHT = height / yCatCount;

    var SQUARE_PADDING = 0.0;

    chart.readDataFrom = function(file){
        var allText =[];
        var allTextLines = [];
        var Lines = [];
        var txtFile = new XMLHttpRequest();
        txtFile.open("GET", "file://G:/workspace_DP3/ux-for-ppsdm/example data/sample.txt", true);
        txtFile.onreadystatechange = function()
        {
            allText = txtFile.responseText;
            allTextLines = allText.split(/\r\n|\n/);
        };
        document.write(allTextLines);<br>
        document.write(allText);<br>
        document.write(txtFile);<br>
    }

    chart.selector = function (value) {
        if (!arguments.length) { return selector; }
        selector = value;
        return chart;
    };

    chart.colorRange = function (value) {
        if (!arguments.length) { return colorRange; }
        colorRange = value;
        return chart;
    };

    function chart(){
        d3.select(chart.selector()).selectAll('svg.heatmap').remove(); // remove the existing chart, if it exists
        var timeRange = chart.counter > maxRange ? maxRange : chart.counter;

        // color range
        var color = d3.scale.linear()
          .range(chart.colorRange())
          .domain([0, 1]);

        drawChart();

        function drawChart(){
            var svg = d3.select(chart.selector())
                .append('svg')
                .attr('width', width)
                .attr('class', 'heatmap')
                .attr('height', height)
                .style('padding', '36px');

            rects = svg.selectAll('.rect')
                .data(rectData);  //  array of days for the last yr

            rects.enter().append('rect')
                .attr('class', 'rect')
                .attr('width', SQUARE_WIDTH)
                .attr('height', SQUARE_HEIGHT)
                .attr('fill', function(d) { return color(d[0]); })
                .attr('x', function (d, i) {
                  return d[1] * (SQUARE_WIDTH + SQUARE_PADDING);
                })
                .attr('y', function (d, i) {
                  return d[2] * (SQUARE_HEIGHT + SQUARE_PADDING);
                });
        }
    }

    return chart;
}