

function heatmap(){

    var maxRange = 100;
    var width = 750;
    var height = 110;
    var counter = 0;
    var selector = "heatmap-container";
    var colorRange = ['#D8E6E7', '#218380'];
    var rectData = [[0.5,0.6,0.7,0.8], [0.4,0.5,0.6,0.7]];
    var SQUARE_WIDTH = 11;
    var SQUARE_HEIGHT = 11;
    var SQUARE_PADDING = 2;
    var MONTH_LABEL_PADDING = 6;

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
                .attr('fill', function(d) { return color(countForDate(d)); })
                .attr('x', function (d, i) {
                  var cellDate = moment(d);
                  var result = cellDate.week() - firstDate.week() + (firstDate.weeksInYear() * (cellDate.weekYear() - firstDate.weekYear()));
                  return result * (SQUARE_LENGTH + SQUARE_PADDING);
                })
                .attr('y', function (d, i) {
                  return MONTH_LABEL_PADDING + formatWeekday(d.getDay()) * (SQUARE_LENGTH + SQUARE_PADDING);
                });
        }
    }

    return chart;
}