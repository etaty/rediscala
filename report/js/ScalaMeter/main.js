var ScalaMeter = (function(parent) {
	var my = { name: "main" };

	/*
	 * ----- public functions -----
	 */

	my.init = function() {
		parent.chart.init();
		parent.filter.init();
		parent.dimensions.init();
		parent.permalink.init();

		load();
	};

	function load() {
		loadModules([parent.filter, parent.chart], parent.filter.update);
	}

	function loadModules(modules, onLoad) {
		var nWaiting = modules.length;

		modules.forEach(function(module) {
			module.load(function() {
				nWaiting--;
				if (nWaiting == 0) {
					onLoad();
				}
			});
		});
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});