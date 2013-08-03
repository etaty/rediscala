var ScalaMeter = (function(parent) {
	var my = { name: "permalink" };

	var BITLY_ACCESS_TOKEN = "34fda5dc3ef2ea36e6caf295f4a6443b4afa7401";

	var URL_PARAM_NAME = "config";

	var storedData_;

	/*
	 * ----- public functions -----
	 */

	my.init = function() {
		storedData_ = parseUrl();

		window.onhashchange = function() {
			location.reload();
		};
		// window.onhashchange = function() {
		// 	storedData_ = parseUrl();
		// 	parent.main.reload();
		// };

		var permalinkBtn = ".btn-permalink";

		$(permalinkBtn).popover({
			placement: 'bottom',
			trigger: 'manual',
			title: 'Press Ctrl-C to copy <a class="permalink-shorten pull-right">Shorten</a>',
			html: true
		}).click(function(event) {
			event.preventDefault();
			$(this).data('popover').options.content = '<textarea class="permalink-inner" />';
			$(this).popover('toggle');
			longUrl = getPermalinkUrl();
			displayUrl(longUrl);
			if (isOnLocalhost()) {
				$('.permalink-shorten').hide();
			} else {
				$('.permalink-shorten').click(function(event) {
					event.preventDefault();
					bitlyShorten(longUrl, function(shortUrl) {
						displayUrl(shortUrl);
					});
				});
			}
		});
		
		$(':not(' + permalinkBtn + ')').click(function(event) {
			if (!event.isDefaultPrevented()) {
				$(permalinkBtn).popover('hide');
			}
		});
	};

	my.storedData = function() {
		return storedData_;
	};

	/*
	 * ----- private functions -----
	 */

	function getConfig() {
		return {
			filterConfig: parent.filter.getConfig(),
			chartConfig: parent.chart.getConfig()
		};
	}

	function displayUrl(url) {
		$('.permalink-inner')
			.val(url)
			.focus()
			.select()
			.click(function(event) {
				$(this).select();
				event.preventDefault();
			});
	}

	function getPermalinkUrl() {
		var data = {};
		data[URL_PARAM_NAME] = JSON.stringify(getConfig());
		return document.URL.split('#')[0] + "#" + jQuery.param(data);
	}

	function getUrlParams() {
		var match,
			pl     = /\+/g,
			search = /([^&=]+)=?([^&]*)/g,
			decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
			query  = window.location.hash.substring(1);

		var urlParams = {};
		while (match = search.exec(query)) {
			urlParams[decode(match[1])] = decode(match[2]);
		}
		return urlParams;
	};

	function bitlyShorten(longUrl, func) {
		$.getJSON(
			"https://api-ssl.bitly.com/v3/shorten", 
			{ 
				"access_token": BITLY_ACCESS_TOKEN,
				"longUrl": longUrl
			},
			function(response) {
				func(response.data.url);
			}
		);
	}

	function isOnLocalhost() {
		var hostname = window.location.hostname;
		var protocol = window.location.protocol;
		return hostname == "127.0.0.1" || hostname == "localhost" || protocol == "file:";
	}

	function parseUrl() {
		var data = getUrlParams();
		if (data.hasOwnProperty(URL_PARAM_NAME)) {
			return $.parseJSON(data[URL_PARAM_NAME]);
		} else {
			return null;
		}
	}

	parent[my.name] = my;

	return parent;
})(ScalaMeter || {});