var ScalaMeter = (function(parent) {
	var my = { name: "dimensions" };

	/*
	 * ----- imports -----
	 */
	var h;

	var keys_,
		params_;

	my.init = function() {
		h = parent.helper;

		keys_ = [];
		params_ = d3.map();
	};

	my.addParam = function(key) {
		if (!params_.has(key)) {
			params_.set(key, addDimension(key, key.substr(h.dKey.paramPrefix.length)));
			keys_.push(key);
		}
	};

	my.filterValues = function(data, legendOrder) {
		keys_.forEach(function(key, i) {
			var dim = params_.get(key);
			dim.filteredValues(h.unique(data, dim.keyFn(), i == 0 ? d3.ascending : legendOrder, true));
		});
	};

	my.add = function(key) {
		var newDim = addDimension(key, key);
		params_.set(key, newDim);
		keys_.push(key);
		return newDim;
	}

	my.get = function(key) {
		return params_.get(key);
	};

	my.getAll = function() {
		return keys_.map(function(key) {
			return params_.get(key);
		});
	};

	my.keys = function(_) {
		if (!arguments.length) return keys_;
		keys_ = _;
	};

	function addDimension(key, caption) {
		return (function() {
			var key_ = key,
				caption_ = caption,
				selectMode_ = h.selectModes.single,
				selectedValues_ = d3.set(),
				expanded_ = false,
				format_ = h.numberFormat,
				keyFn_ = h.mapKey(key),
				filterContainer_,
				values_,
				filteredValues_,
				cfDimension_;

			function updateCrossfilter() {
				cfDimension_.filterFunction(function(d) {
					return d == null || selectedValues_.has(d);
				});
			}

			function updateSelectMode() {
				switch (selectMode_) {
					case h.selectModes.single:
						var selectedValue = values_[0];
						for (var i = 0; i < values_.length; i++) {
							if (selectedValues_.has(values_[i])) {
								selectedValue = values_[i];
								break;
							}
						}
						selectedValues_ = d3.set([selectedValue]);
						break;
					case h.selectModes.all:
						selectedValues_ = d3.set(values_);
						break;
				}
			}

			return {
				init: function(data, cfDimension) {
					values_ = h.unique(data, h.mapKey(key), d3.ascending);

// if (key == h.dKey.date) {
// 	//generate random dates over the past 5 years
// 	var today = +new Date();
// 	for(i=0; i<5000; i++){
// 			values_.push(today - 50 * 86400 * 1000 - Math.round(Math.random() * 5 * 365 * 86400 * 1000));
// 	}
// 	values_.sort(d3.ascending);
// }

					selectedValues_ = d3.set(values_);
					cfDimension_ = cfDimension;
				},

				updateCrossfilter : updateCrossfilter,

				selectMode: function(_) {
					if (!arguments.length) return selectMode_;
					selectMode_ = _;
					updateSelectMode();
				},

				selectedValues: function(_) {
					if (!arguments.length) return selectedValues_;
					selectedValues_ = _;
				},

				filteredValues: function(_) {
					if (!arguments.length) return filteredValues_;
					filteredValues_ = _;
				},

				caption: function(_) {
					if (!arguments.length) return caption_;
					caption_ = _;
				},

				format: function(_) {
					if (!arguments.length) return format_;
					format_ = _;
				},

				filterContainer: function(_) {
					if (!arguments.length) return filterContainer_;
					filterContainer_ = _;
				},

				expanded: function(_) {
					if (!arguments.length) return expanded_;
					expanded_ = _;
				},

				key: function() {
					return key_;
				},

				keyFn: function() {
					return keyFn_;
				},

				clickValue: function(value) {
					if (selectMode_ == h.selectModes.single) {
						selectedValues_ = d3.set([value]);
					} else {
						if (selectedValues_.has(value)) {
							selectedValues_.remove(value);
						} else {
							selectedValues_.add(value);
						}
						if (selectedValues_.values().length == values_.length) {
							selectMode_ = h.selectModes.all;
						} else {
							selectMode_ = h.selectModes.select;
						}
					}
				},

				getAllValues: function() {
					return values_;
				},

				selectLast: function() {
					selectedValues_ = d3.set([values_[values_.length - 1]]);
				}
			};
		})();
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});