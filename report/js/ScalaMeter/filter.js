var ScalaMeter = (function(parent) {
	var my = { name: "filter" };

	/*
	 * ----- imports -----
	 */
	var h,
		dKey;

	/*
	 * ----- constants -----
	 */
	var TSV_DATE_FORMAT,
		DATE_FILTER_WIDTH;

	/*
	 * ----- private fields -----
	 */
	var	dataConcat_,
		// rawdata,
		filter_,
		selectedCurves_,
		scopeTree_,
		filterDimensions_;

	/*
	 * ----- public functions -----
	 */
	
	my.init = function() {
		h = parent.helper;
		dKey = h.dKey;

		TSV_DATE_FORMAT = d3.time.format.utc("%Y-%m-%dT%H:%M:%SZ");
		DATE_FILTER_WIDTH = 12;

		dataConcat_ = [];
		selectedCurves_ = d3.set([0]);
		filterDimensions_ = parent.dimensions;
	};

	my.load = function(onLoad) {
		var storedData = parent.permalink.storedData();
		readData(parent.data, function() {
			initFilters();
			if (storedData != null) {
				setConfig(storedData.filterConfig);
			}
			filter_.updateFilters();
			initTree();
			updateCurveDim();
			onLoad();
		});
	};

	my.update = function() {
		updateChart();
	}

/*
	my.rawdata = function(_) {
		if (!arguments.length) return rawdata;
		rawdata = _;
		return my;
	};
	*/

	my.getConfig = function() {
		return {
			curves: selectedCurves_.values(),
			order: filterDimensions_.keys(),
			filters: filterDimensions_.getAll().map(function(dim) {
				return dim.selectedValues().values();
			})
		};
	};

	/*
	 * ----- private functions -----
	 */
	
	function initFilters() {
		var my = {};

		my.getData = getData;

		my.updateFilters = updateFilters;

		my.getDim = getDimension;

		var cFilter_ = crossfilter(dataConcat_),
			filterDim_ = {},
			dateDim_;

		var root = d3.select(".filters");

		var data = getData();

		dateDim_ = filterDimensions_.add(dKey.date);
		dateDim_.init(data, getDimension(dKey.date));
		dateDim_.format(h.dateFormat);

		var keyDay = function (d) { return +d3.time.day(new Date(+d)); };

		var uniqueDays = h.unique(dateDim_.getAllValues(), keyDay, d3.ascending);
		var dayFrom = uniqueDays.length - DATE_FILTER_WIDTH;

		var nestDates = d3.nest()
			.key(function (d) { return +d3.time.year(new Date(+d)); }).sortKeys(h.ascendingToInt)
			.key(function (d) { return +d3.time.month(new Date(+d)); }).sortKeys(h.ascendingToInt)
			.key(keyDay).sortKeys(h.ascendingToInt).sortValues(d3.ascending);

		createFilters();

		$(".filters").sortable({
			handle: ".filter-container-header",
			update: function(event, ui) {
				filterDimensions_.keys($(this).sortable("toArray"));
				updateChart();
			}
		});
		$(".filters").disableSelection();

		function getDimension(key) {
			if (!filterDim_.hasOwnProperty(key)) {
				filterDim_[key] = cFilter_.dimension(h.mapKey(key));
			}
			return filterDim_[key];
		}

		function getData() {
			return getDimension(dKey.curve).top(Infinity);
		};

		function timesOfDay(d) {
			var dateBadges = d3.select(this).selectAll(".timeofday").data(d.values, h.ident);
			var badgeFormat = d3.time.format("%H:%M:%S");

			dateBadges.enter()
				.append("span")
				.text(function (d) { return badgeFormat(new Date(+d)); })
				.attr("class", "label timeofday filter-value")
				.each(updateLabels(dateDim_))
				.on("click", function(d) {
					clickBadge(dateDim_, d);
				});

			dateBadges.exit().remove();
		}

		function days(d) {
			var g = d3.select(this).selectAll(".day").data(d.values, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "day");
			g
				.text(function(d) { return d3.time.format("%d")(new Date(+d.key)); } )
				.order()
				.each(timesOfDay);
			g.exit().remove();
		}

		function months(d) {
			var g = d3.select(this).selectAll(".month").data(d.values, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "month")
				.append("div")
				.attr("class", "caption-outer")
				.append("span")
				.attr("class", "caption");

			g.selectAll(".caption")
				.text(function(d) { return d3.time.format("%b")(new Date(+d.key)); } )

			g
				.order()
				.each(days);
			g.exit().remove();
		}

		function filterDates() {
			var dateFrom = uniqueDays[dayFrom];
			var dayTo = dayFrom + DATE_FILTER_WIDTH;
			var dateTo = dayTo < uniqueDays.length ? uniqueDays[dayTo] : Infinity;
			return dateDim_.getAllValues().filter(function(d) {
				return d >= dateFrom && d < dateTo;
			});
		}

		function updateDateFilter(container) {
			dayFrom = Math.max(0, Math.min(uniqueDays.length - DATE_FILTER_WIDTH, dayFrom));
			var dateList = dateDim_.expanded() ? filterDates() : dateDim_.selectedValues().values();

			var nestedDates = nestDates.entries(dateList);

			var g = container.select(".filter-values").selectAll(".year").data(nestedDates, h.fKey);

			g.enter()
				.append("div")
				.attr("class", "year")
				.append("div")
				.attr("class", "caption-outer")
				.append("span")
				.attr("class", "caption");

			g.selectAll(".caption")
				.text(function(d) { return d3.time.format("%Y")(new Date(+d.key)); } );	

			g
				.order()
				.each(months);
			g.exit().remove();
		}

		function createDateSlider(parentNode) {
			var container = parentNode.append("div");
			
			container.attr("class", "date-slider-container");

			container.append("a")
				.on("click", function() {
					dayFrom--;
					updateDateFilter(dateDim_.filterContainer());
				})
				.attr("class", "btn btn-mini date-slider-btn")
				.append("i")
				.attr("class", "icon-arrow-left");

			container.append("div")
				.attr("class", "date-slider-center")
				.append("div")
				.attr("class", "date-slider");

			container.append("a")
				.on("click", function() {
					dayFrom++;
					updateDateFilter(dateDim_.filterContainer());
				})
				.attr("class", "btn btn-mini date-slider-btn")
				.append("i")
				.attr("class", "icon-arrow-right");

			$(".date-slider").slider({
				orientation: "horizontal",
				min: 0,
				max: dayFrom,
				value: dayFrom,
				slide: function (event, ui) {
					dayFrom = ui.value;
					updateDateFilter(dateDim_.filterContainer());
				}
			});
		}

		function updateSelection(dim) {
			dim.updateCrossfilter();
			updateFilters();
			updateChart();
		}

		function clickBadge(dim, d) {
			if (!dim.expanded()) {
				toggleExpanded(dim);
			} else {
				dim.clickValue(d);
				updateSelection(dim);
			}
		}

		function toggleExpanded(dim) {
			dim.expanded(!dim.expanded());
			var container = dim.filterContainer();
			container
				.classed("filter-expanded", dim.expanded())
				.classed("filter-collapsed", !dim.expanded());
			if (dim == dateDim_) {
				updateDateFilter(container);
			}
		}

		function createFilter(paramName, i) {
			var container = d3.select(this);

			var dim = filterDimensions_.get(paramName);
			dim.filterContainer(container);
			if (dim == dateDim_) {
				dim.selectLast();
				dim.selectMode(h.selectModes.single);
			} else {
				dim.init(data, getDimension(paramName));
				dim.selectMode(i == 0 ? h.selectModes.select : h.selectModes.single);
			}
			dim.updateCrossfilter();

			var content = '' +
				'<div class="filter-container-header">' +
					dim.caption() +
				'</div>' +
				'<div class="tabbable tabs-below">' +
					'<div class="tab-content">' +
						'<i class="filter-expand icon-chevron-down filter-hidecollapsed"></i>' +
						'<i class="filter-expand icon-chevron-right filter-hideexpanded"></i>' +
						'<span class="filter-values"></span>' +
					'</div>' +
					'<ul class="nav nav-tabs filter-hidecollapsed"></ul>' +
				'</div>';

			container
				.attr("id", h.ident)
				.attr("class", "filter-container filter-collapsed")
				.html(content);

			container.select("ul").selectAll("li").data(h.selectModes.enumAll)
				.enter()
				.append("li")
				.append("a")
				.attr("data-toggle", "tab")
				.text(h.fValue)
				.on("click", function(d) {
					dim.selectMode(d);
					updateSelection(dim);
				});

			container.selectAll(".filter-expand")
				.on("click", function() {
					toggleExpanded(dim);
				});

			var valuesRoot = container.select(".filter-values");
			if (dim == dateDim_) {
				if (uniqueDays.length > DATE_FILTER_WIDTH) {
					valuesRoot.call(createDateSlider);
				}
			} else {
				var badges = valuesRoot.selectAll(".filter-value").data(dim.getAllValues());

				badges.enter()
					.append("span")
					.attr("class", "label filter-value")
					.on("click", function(d) {
						clickBadge(dim, d);
					})
					.text(h.numberFormat);
			}
		}

		function updateLabels(dim) {
			return function(d) {
				var selected = dim.selectedValues().has(d);
				d3.select(this)
					.classed("label-info", selected)
					.classed("filter-hidecollapsed", !selected);
			};
		}

		function updateFilter(paramName) {
			var dim = filterDimensions_.get(paramName);
			var container = d3.select(this);

			if (dim == dateDim_) {
				updateDateFilter(container);
			}

			container.select("ul").selectAll("li")
				.classed("active", function(d) { return d == dim.selectMode(); });

			container.selectAll(".filter-value")
				.each(updateLabels(dim));
		}

		function updateFilters() {
			var containers = root.selectAll(".filter-container").data(filterDimensions_.keys(), h.ident);

			containers
				.order()
				.each(updateFilter);
		}

		function createFilters() {
			var containers = root.selectAll(".filter-container").data(filterDimensions_.keys(), h.ident);

			containers.enter()
				.append("div")
				.each(createFilter);			
		}

		filter_ = my;
	}

/*
	function showdata(data) {
		function addCols(row) {
			header.selectAll("th").data(d3.keys(row)).enter().append("th").text(h.ident);
			d3.select(this).selectAll("td")
				.data(d3.values(row))
				.enter()
				.append("td").text(h.ident);
		}
		
		var container = d3.select(rawdata);
		
		var header = container.select(".dataheader");
		if (header.empty()) {
			header = container.append("tr").attr("class", "dataheader");
		}
		
		var rows = container.selectAll(".datavalues").data(data, h.mapKey(dKey.index));
		
		rows.enter().append("tr").attr("class", "datavalues").each(addCols);
		rows.exit().remove();
	}
	*/

	/*
	 * Initialize the dynatree widget, using the
	 * data accumulated in scopeTree.
	 */
	function initTree() {
		var children = convertTree(scopeTree_, "");

		$(".tree").dynatree({
			onSelect: function(flag, node) {
				var selectedNodes = node.tree.getSelectedNodes();
				var selectedKeys = $.map(selectedNodes, function(node){
					return +node.data.key;
				});
				selectedCurves_ = d3.set(selectedKeys);
				updateCurveDim();
				updateChart();
			},
			onQueryActivate: function(flag, node) {
				node.toggleSelect();
				return false;
			},
			children: children,
			checkbox: true, // Show checkboxes.
			clickFolderMode: 1, // 1:activate, 2:expand, 3:activate and expand
			selectMode: 3, // 1:single, 2:multi, 3:multi-hier
			noLink: true, // Use <span> instead of <a> tags for all nodes,
			classNames: {
				nodeIcon: "none"
			}
		});

		function convertTree(node, title) {
			var children = [];
			for (var child in node.children) {
				children.push(
					convertTree(node.children[child], child)
				);
			}
			if (title != "") {
				title = '<span class="dynatree-adjtext">' + title + '</span>';
				if (node.id != -1) {
					var color = h.mainColors(node.id);
					title = '<div class="dynatree-square" style="background-color:' + color + '"></div>' + title;
				}
				return {
					key: "" + node.id,
					title: title,
					expand: true,
					select: selectedCurves_.has(node.id),
					children: children
				}
			} else {
				return children;
			}
		}
	}

	function updateChart() {
		parent.chart.update(filter_.getData()); 
		/*if (h.isDef(rawdata)) {
			showdata(data);
		}*/
	}

	function readData(data, onLoad) {
		var tsvWaiting = data.index.length;
		var scopeTree = { children: [] };

		data.index.forEach(function(curve, id) {
			curve.scope.push(curve.name);
			addScope(scopeTree, curve.scope, id);
			if (h.isDef(data.tsvData)) {
				tsvReady(d3.tsv.parse(data.tsvData[id]), id);
			} else {
				d3.tsv(curve.file, function(error, tsvData) {
					tsvReady(tsvData, id);
				});
			}
		});

		function tsvReady(tsvData, curveId) {
			if (tsvData.length != 0) {
				parseData(tsvData, curveId);
			}
			tsvWaiting--;
			if (tsvWaiting == 0) {
				scopeTree_ = scopeTree;
				onLoad();
			}
		}

		function addScope(node, scope, leafId) {
			var nodeName = scope[0];
			var isLeaf = scope.length == 1;
			if (!node.children.hasOwnProperty(nodeName)) {
				node.children[nodeName] = {
					"id": isLeaf ? leafId : -1,
					"children": []
				}
			}
			if (!isLeaf) {
				scope.shift();
				addScope(node.children[nodeName], scope, leafId);
			}
		}

		function parseData(tsvData, curveId) {
			for (var key in tsvData[0]) {
				if (key.slice(0, dKey.paramPrefix.length) == dKey.paramPrefix) {
					// key starts with "param-"
					filterDimensions_.addParam(key);
				}
			}
			var offset = dataConcat_.length;
			tsvData.forEach(function(d, i) {
				filterDimensions_.keys().forEach(function(paramName) {
					if (d.hasOwnProperty(paramName)) {
						d[paramName] = +d[paramName];
					}
				});
				d[dKey.value] = +d[dKey.value];			
				d[dKey.date] = +TSV_DATE_FORMAT.parse(d[dKey.date]);
				d[dKey.curve] = curveId;
				d[dKey.index] = offset + i;
			});
			dataConcat_ = dataConcat_.concat(tsvData);
		}
	}

	function setConfig(_) {
		selectedCurves_ = d3.set(_.curves);
		filterDimensions_.keys(_.order);
		for (var i = 0; i < _.order.length; i++) {
			var dim = filterDimensions_.get(_.order[i]);
			dim.selectMode(h.selectModes.select);
			dim.selectedValues(d3.set(_.filters[i]));
			dim.updateCrossfilter();
		};
	}
	
	function updateCurveDim() {
		filter_.getDim(dKey.curve).filterFunction(function (d) {
			return selectedCurves_.has(d);
		});
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});