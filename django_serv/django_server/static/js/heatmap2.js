

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

    chart.readDataFrom = function(){
        var allText =[];
        var allTextLines = [];

        var txtFile = new XMLHttpRequest();
        txtFile.open("GET", "file://G:/workspace_DP3/ux-for-ppsdm/example data/sample.txt", false);
        txtFile.onreadystatechange = function()
        {
           if (xmlhttp.readyState == 4) {
               alert(txtFile.responseText);
           }
        };

        document.write(allTextLines);
        document.write(allText);
        document.write(txtFile);

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

    function chart(evt){
        <!-- READ DATA -->
        //Retrieve the first (and only!) File from the FileList object
        var f = evt.target.files[0];
        if (f) {
            var r = new FileReader();
            r.onload = function(e) {
                var contents = e.target.result;
                alert( "Got the file.n"
                  +"name: " + f.name + "n"
                  +"type: " + f.type + "n"
                  +"size: " + f.size + " bytesn"
                  + "starts with: " + contents.substr(1, contents.indexOf("n"))
                 );
            }
            r.readAsText(f);
            alert(r);
        } else {
          alert("Failed to load file");
        }

        d3.select(chart.selector()).selectAll('svg.heatmap').remove(); // remove the existing chart, if it exists

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
                .data(rectData);

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