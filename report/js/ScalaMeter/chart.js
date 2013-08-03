var ScalaMeter = (function(parent) {
	var my = { name: "chart" };

	/*
	 * ----- imports -----
	 */
	var h,
		dKey,
		mapKey;

	/*
	 * ----- constants -----
	 */
	var MARGIN,
		WIDTH,
		HEIGHT,
		MIN_LEGEND_WIDTH,
		CHART_TYPES,
		Y_AXIS_CAPTION,
		ROOT_NODE;

	/*
	 * ----- private fields -----
	 */
	var
		svg_ = null,
		showCI_ = false,
		chartType_;

	/*
	 * ----- public functions -----
	 */	

	my.init = function() {
		h = parent.helper;
		dKey = h.dKey;
		mapKey = h.mapKey;

		MARGIN = {
			top : 20,
			right : 20,
			bottom : 30,
			left : 50
		};
		WIDTH = 900;
		HEIGHT = 500;
		MIN_LEGEND_WIDTH = 120;
		CHART_TYPES = {
			line: 0,
			bar: 1
		};
		Y_AXIS_CAPTION = "running time [ms]";
		ROOT_NODE = ".chart";

		chartType_ = CHART_TYPES.line;

		createSVG();
	};

	my.load = function(onLoad) {
		var storedData = parent.permalink.storedData();
		if (storedData != null) {
			setConfig(storedData.chartConfig);
		}
		onLoad();
	};

	my.getConfig = function() {
		return {
			type: chartType_,
			showCI: showCI_
		};
	};

	my.chartTypes = function() {
		return CHART_TYPES;
	};

	my.setType = function(_) {
		chartType_ = _;
		parent.filter.update();
	};

	my.toggleCI = function() {
		showCI_ = !showCI_;
		parent.filter.update();
	};

	my.update = function(data) {
		var filterDimensions = parent.dimensions;
		var keysCurveColor = h.unique(data, h.curveKey, d3.ascending);
		var legendWidth = MIN_LEGEND_WIDTH + 20 * keysCurveColor.length;
		var W = WIDTH - MARGIN.left - MARGIN.right - legendWidth;
		var H = HEIGHT - MARGIN.top - MARGIN.bottom;
		var allDimensions = filterDimensions.getAll();

		var keyAbscissa,
				showXGrid,
				lineData = [],
				groupedLineData = [],
				barData = [];

		// scales
		var x,
			y = d3.scale.linear()
				.domain([0, d3.max(data, mapKey(dKey.value))])
				.range([H, 0]);

		var legendDimensions = allDimensions.slice(1);

		var legendTitle = legendDimensions.map(function(dim) {
			return dim.caption();
		}).join(" \u2192 ");

		var legendSize = 1;

		var xAxisDim = allDimensions[0];

		var barScale = function() { return 0; };

		switch(chartType_) {
			case CHART_TYPES.line:
				filterDimensions.filterValues(data, d3.descending);
				lineData = data;
				showXGrid = true;
				keyAbscissa = xAxisDim.keyFn();

				if (xAxisDim.key() == dKey.date) {
					x = d3.scale.ordinal()
								.domain(xAxisDim.filteredValues())
								.rangePoints([0, W]);
				} else {
					x = d3.scale.linear()
								.domain(d3.extent(data, keyAbscissa))
								.range([0, W]);
				}

				var nestLineData = d3.nest().key(h.curveKey);
				legendDimensions.forEach(function(dim) {
					nestLineData.key(dim.keyFn());
				});
				nestLineData.sortValues(h.sortBy(keyAbscissa));

				groupedLineData = nestLineData.entries(lineData);
				break;
			case CHART_TYPES.bar:
				filterDimensions.filterValues(data, d3.ascending);
				barData = data;
				showXGrid = false;

				function extendBarScale(parent, xi, keyFn) {
					return function(d) {
						return parent(d) + xi(keyFn(d));
					};
				}

				var parentWidth = W;
				var xN = allDimensions.map(function(dim) {
					var xi = d3.scale.ordinal()
						.domain(dim.filteredValues())
						.rangeRoundBands([0, parentWidth], 0.1);
					parentWidth = xi.rangeBand();
					barScale = extendBarScale(barScale, xi, dim.keyFn());
					return xi;
				});
				var xi = d3.scale.ordinal()
					.domain(keysCurveColor)
					.rangeRoundBands([0, parentWidth], 0);
				barScale = extendBarScale(barScale, xi, h.curveKey);

				var barWidth = xi.rangeBand();
				x = xN[0];

				break;
		}

		legendDimensions.forEach(function(dim) {
			legendSize *= dim.filteredValues().length;
		});

		var brightnessScale = d3.scale.linear()
			.domain([0, legendSize - 1])
			.range([-1, 1]);

		d3.transition().each(function() {
			// axis and grid
			if (showXGrid) {
				var xGrid = d3.svg.axis().scale(x).orient("bottom").tickSize(-H, 0, 0).tickFormat("");
				svg_.select(".x.grid")
					.attr("transform", "translate(0," + H + ")")
					.transition().call(xGrid);
			} else {
				svg_.select(".x.grid").selectAll("*").remove();
			}

			var yGrid = d3.svg.axis().scale(y).orient("left").tickSize(-W, 0, 0).tickFormat("");
			svg_.select(".y.grid").transition().call(yGrid);

			var xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(xAxisDim.format());
			svg_.select(".x.axis")
				.attr("transform", "translate(0," + H + ")")
				.transition()
				.call(xAxis)
				.select(".axis-caption")
				.attr("x", W)
				.text(xAxisDim.caption());

			var yAxis = d3.svg.axis().scale(y).orient("left");
			svg_.select(".y.axis").transition().call(yAxis);

			var bars = svg_.selectAll("rect").data(barData, mapKey(dKey.index));

			bars.enter()
				.append("rect")
				.style("fill-opacity", 0);

			bars
				.each(hoverable("bar", legendKey))
				.each(bar);

			bars.exit().remove();

			var barsCI = svg_.selectAll(".bar-ci").data(showCI_ ? barData : [], mapKey(dKey.index));

			barsCI.enter()
				.append("g")
				.attr("class", "bar-ci")
				.each(createBarCI);

			barsCI.each(barCI);

			barsCI.exit().remove();

			// data points
			var points = svg_.selectAll('circle').data(lineData, mapKey(dKey.index));

			points.enter().append('circle')
				.attr('r', 5);

			points
				.each(hoverable("line", legendKey))
				.style("stroke",  function(d) { return colorMap(d); })
				.transition()
				.attr('cx', function (d) { return x(keyAbscissa(d)); })
				.attr('cy', function (d) { return y(d[dKey.value]); });

			points.exit().remove();

			// data paths
			var curves = svg_.selectAll(".curve").data(groupedLineData, h.fKey);

			curves.enter()
				.append("g")
				.attr("class", "curve");
			
			curves.each(curveFn(path, 1));

			curves.exit().remove();

			// confidence intervals (areas)
			var curvesCI = svg_.selectAll(".curve-ci").data(showCI_ ? groupedLineData : [], h.fKey);

			curvesCI.enter()
				.insert("g", ":first-child")
				.attr("class", "curve-ci");
			
			curvesCI.each(curveFn(areaCI, 1));

			curvesCI.exit().remove();

			svg_.select(".legend-container")
				.attr("transform", "translate(" + (W + legendWidth - 20 * keysCurveColor.length) + ", 0)")
				.each(legendGroup(0, 0, legendSize, ""))
				.select(".legend-title")
				.attr("x", -4)
				.attr("y", 13)
				.text(legendTitle);
		});

		function curveColorWithShade(curveKey, shade) {
			return d3.hsl(h.mainColors(curveKey)).brighter(brightnessScale(shade));
		}

		function colorMap(d) {
			var index = 0;
			legendDimensions.forEach(function(dim) {
				index = index * dim.filteredValues().length + dim.filteredValues().indexOf(dim.keyFn()(d));
			});
			return curveColorWithShade(h.curveKey(d), index);
		}

		function path(d) {
			var line = d3.svg.line().x(function(d) {
				return x(keyAbscissa(d));
			}).y(function(d) {
				return y(d[dKey.value]);
			});

			var d0 = d.values[0];
			var g = d3.select(this);
			var color = colorMap(d0);
			var pathData;
			if (keyAbscissa(d0) == null) {
				pathData = "M0," + y(d0[dKey.value]) + "H" + W;
			} else {
				pathData = line(d.values);
			}
			g.select("path")
				.attr("style", "stroke-opacity:0.7;stroke:" + color)
				.each(hoverable("line", legendKey(d0)))
				.transition()
				.attr("d", pathData);
		}

		function areaCI(d) {
			var area = d3.svg.area().x(function(d) {
				return x(keyAbscissa(d));
			}).y0(function(d) {
				return y(d[dKey.cilo]);
			}).y1(function(d) {
				return y(d[dKey.cihi]);
			});

			var g = d3.select(this);
			var color = colorMap(d.values[0]);
			g.select("path")
				.attr("class", "area-" + legendKey(d.values[0]))
				.attr("style", "stroke-opacity:0.2;stroke:" + color + ";fill-opacity:0.1;fill:" + color)
				.transition()
				.attr("d", area(d.values));
		}

		function curveFn(fn, depth) {
			return function(d) {
				var groups = d3.select(this).selectAll(".group-" + depth).data(d.values, h.fKey);

				var subGroups = groups.enter()
					.append("g")
					.attr("class", "group-" + depth);

				if (depth < legendDimensions.length) {
					groups.each(curveFn(fn, depth + 1));
				} else {
					subGroups
						.append("path");

					groups.each(fn);
				}
				groups.exit().remove();
			}
		}

		function bar(d) {
			d3.select(this)
				.style("fill", colorMap(d))
				.transition()
				.attr("width", barWidth)
				.attr("x", barScale(d))
				.attr("y", y(d[dKey.value]))
				.attr("height", H - y(d[dKey.value]))
				.transition()
				.style("fill-opacity", 1);

			var ttContent = legendDimensions.map(function(dim) {
				return dim.caption() + ": " + dim.format()(dim.keyFn()(d));
			});
			ttContent.push("success: " + d[dKey.success]);

			$(this).tooltip({title: ttContent.join("<br/>"), container: "body", html: true});
		}

		function createBarCI() {
			var g = d3.select(this);

			g.append("line")
				.attr("class", "bar-cilo bar-ci-v bar-cilo-v").style("stroke-opacity", 0);
			g.append("line")
				.attr("class", "bar-cihi bar-ci-v bar-cihi-v").style("stroke-opacity", 0);
			g.append("line")
				.attr("class", "bar-cilo bar-ci-h bar-cilo-h").style("stroke-opacity", 0);
			g.append("line")
				.attr("class", "bar-cihi bar-ci-h bar-cihi-h").style("stroke-opacity", 0);
		}

		function barCI(d) {
			var g = d3.select(this)
			var x0 = barScale(d);
			var xMiddle = x0 + barWidth / 2;
			var y0 = y(d[dKey.value]);
			var yCilo = y(d[dKey.cilo]);
			var yCihi = y(d[dKey.cihi]);

			lineTransition(".bar-ci-h", function(nodes) {
				return nodes
					.attr("x1", x0 + 1)
					.attr("x2", x0 + barWidth - 1);	});
			lineTransition(".bar-cilo-h", function(nodes) {
				return nodes
					.attr("y1", yCilo)
					.attr("y2", yCilo); });
			lineTransition(".bar-cihi-h", function(nodes) {
				return nodes
					.attr("y1", yCihi)
					.attr("y2", yCihi); });

			lineTransition(".bar-ci-v", function(nodes) {
				return nodes
					.attr("x1", xMiddle)
					.attr("x2", xMiddle); });
			lineTransition(".bar-cilo-v", function(nodes) {
				return nodes
					.attr("y1", y0)
					.attr("y2", yCilo); });
			lineTransition(".bar-cihi-v", function(nodes) {
				return nodes
					.attr("y1", y0)
					.attr("y2", yCihi); });

			function lineTransition(selector, fn) {
				g.selectAll(selector)
					.transition()
					.call(fn)
					.transition()
					.style("stroke-opacity", 1);
			}
		}

		function legendRow(shade) {
			return function(d, i) {
				var g = d3.select(this);
				var rects = g.selectAll("rect").data(keysCurveColor, h.ident);
				rects.enter().append("rect")
					.attr("width", 18)
					.attr("height", 18)
					.style("fill", function(curveKey) { return curveColorWithShade(curveKey, shade + i) } );
				rects.attr("x", function(d, i) { return 20 * i; } );
				rects.exit().remove();
			};
		}

		function legendGroup(depth, parentId, groupSize, lKeyPrefix) {
			return function(d, i) {
				var id = parentId + i * groupSize;
				var dim = legendDimensions[depth];
				var groups = d3.select(this).selectAll(".legend-grp-" + depth).data(dim.filteredValues(), h.ident);
				var subGroupSize = groupSize / dim.filteredValues().length;
				var lKey = (depth == 0) ? "" : lKeyPrefix + d + "-";

				var labels = groups.enter()
					.append("g")
					.attr("class", "legend-grp-" + depth)
					.append("text")
					.attr("x", -4)
					.attr("y", 13)
					.text(dim.format());

				if (depth < legendDimensions.length - 1) {
					labels
						.attr("style", "font-weight:bold");
					groups
						.attr("transform", function(d, i) { return "translate(0, " + ((1 + i * (subGroupSize + 1)) * 20) + ")"; })
						.each(legendGroup(depth + 1, id, subGroupSize, lKey));
				} else {
					groups
						.attr("transform", function(d, i) { return "translate(0, " + ((1 + i) * 20) + ")"; })
						.each(hoverable("legend", function(d) { return lKey + d; }))
						.each(legendRow(id));
				}
				groups.exit().remove();
			};
		}

		function legendKey(d) {
			return legendDimensions.map(function(dim) {
				var key = dim.keyFn()(d);
				return key == null ? "null" : key;
			}).join("-");
		}
	}

	/*
	 * ----- private functions -----
	 */

	function setConfig(config) {
		chartType_ = config.type;
		showCI_ = config.showCI;

		d3.selectAll(".nav-charttype li")
			.classed("active", function(d, i) {
				return i == chartType_;
			});

		if (showCI_) {
			$('.btn-showCI').button('toggle');
		}
	}

	function createSVG() {
		svg_ = d3.select(ROOT_NODE)
			.append("svg")
			.attr("width", WIDTH)
			.attr("height", HEIGHT)
			.append("g")
			.attr("transform", "translate(" + MARGIN.left + "," + MARGIN.top + ")");

		// grid
		svg_.append("g")
			.attr("class", "x grid");
		svg_.append("g")
			.attr("class", "y grid");

		// x axis
		svg_.append("g")
			.attr("class", "x axis")
			.append("text")
			.attr("class", "axis-caption")
			.attr("y", -6)
			.style("text-anchor", "end");

		// y axis
		svg_.append("g")
			.attr("class", "y axis")
			.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", 6).attr("dy", ".71em")
			.style("text-anchor", "end")
			.text(Y_AXIS_CAPTION);

		// legend
		svg_.append("g")
			.attr("class", "legend-container")
			.append("text")
			.attr("class", "legend-title");
	}

	function hoverable(className, getId) {
		return function(d) {
			var id = ($.isFunction(getId)) ? getId(d) : getId;
			d3.select(this)
				.classed(className, true)
				.classed(className + "-" + id, true)
				.on("mouseover", function() { mover(id) })
				.on("mouseout", function() { mout(id) });
		};
	}

	function mover(id) {
		var line = d3.selectAll(".line-" + id);
		var area = d3.select(".area-" + id);
		var legend = d3.select(".legend-" + id)
		var bars = d3.selectAll(".bar-" + id);

		line.transition().style("stroke-width", 4);

		area.transition()
			.style("stroke-opacity", 1)
			.style("fill-opacity", 0.3);

		legend.select("text").attr("style", "font-weight:bold");
		legend.selectAll("rect")
			.transition()
			.style("stroke-opacity", 1);


		bars.transition().style("stroke-opacity", 1);
	}

	function mout(id) {
		var line = d3.selectAll(".line-" + id);
		var area = d3.select(".area-" + id);
		var legend = d3.select(".legend-" + id)
		var bars = d3.selectAll(".bar-" + id);

		line.transition().style("stroke-width", 1.5);

		area.transition()
			.style("stroke-opacity", 0.2)
			.style("fill-opacity", 0.1);

		legend.select("text").attr("style", "font-weight:normal");
		legend.selectAll("rect")
			.transition()
			.style("stroke-opacity", 0);

		bars.transition().style("stroke-opacity", 0);
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});