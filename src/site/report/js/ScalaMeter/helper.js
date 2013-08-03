var ScalaMeter = (function(parent) {
	var my = { name: "helper" };

	var TSV_DATA_KEYS = {
		date: "date",
		paramPrefix: "param-",
		param: "param-size",
		value: "value",
		success: "success",
		cilo: "cilo",
		cihi: "cihi",
		complete: "complete",
		curve: "curve",
		index: "index"
	};

	var SELECT_MODES = obj2enum({
		single: "Single",
		select: "Select",
		all: "All"
	});

	var LABEL_NULL = "none";

	var DATE_FORMAT = (function(d) {return d3.time.format("%Y-%m-%d %H:%M:%S")(new Date(+d)); });

	var NUMBER_FORMAT = createNumberFormat("\u202F");  // 202F: narrow no-break space

	my.curveKey = mapKey(TSV_DATA_KEYS.curve);

	my.dKey = TSV_DATA_KEYS;

	my.isDef = isDef;

	my.mapKey = mapKey;

	my.selectModes = SELECT_MODES;

	my.dateFormat = DATE_FORMAT;

	my.numberFormat = NUMBER_FORMAT;

	my.mainColors = (function() {
		var nGroups = 10;
		var groups = d3.scale.category10().domain(d3.range(nGroups));
		return function(i) {
			return groups(i % nGroups);
		};
	})();

	my.ident = function(d) {
		return d;
	};

	my.ascendingToInt = function(a, b) {
		return d3.ascending(+a, +b);
	};

	my.sortBy = function(fn) {
		return function(a, b) {
			return d3.ascending(fn(a), fn(b));
		};
	};

	my.fKey = function(d) {
		return d.key;
	};

	my.fValue = function(d) {
		return d.value;
	};

	my.unique = function(data, key, sort, includeNull){
		includeNull = isDef(includeNull) ? includeNull : false;
		var values = d3.nest().key(key).map(data, d3.map).keys();
		var hasNull = false;
		values = values.filter(function(d) {
			var isNull = (d == "null");
			hasNull = hasNull || isNull;
			return !isNull;
		}).map(toInt);
		if (isDef(sort)) {
			values = values.sort(sort);
		}
		if (includeNull && hasNull) {
			values.unshift(null);
		}
		return values;
	};

	function isDef(value) {
		return typeof value !== 'undefined'; 
	}

	function mapKey(key) {
		return function(d) { return d.hasOwnProperty(key) ? d[key] : null };
	}

	function obj2enum(obj) {
		var result = { enumAll: [] };
		for (var key in obj) {
			result[key] = {
				key: key,
				value: obj[key]
			};
			result.enumAll.push(result[key]);
		}
		return result;
	};

	function toInt(d) {
		return +d;
	}

	function createNumberFormat(thousandsSeparator) {
		return function(d) {
			if (d == null) {
				return LABEL_NULL;
			} else {
				var parts = d.toString().split(".");
				parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, thousandsSeparator);
				return parts.join(".");
			}
		};
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});