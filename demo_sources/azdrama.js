var TinyCast = require("/TinyCastSDK");
var $ = require("jquery");

function loadRss(ctx, pageNum) {
	var q = TinyCast.defer();

	if(!ctx) ctx = {type: 1, urls: {1: "http://myrss.nu/drama"}};

	$.ajax({
		type: "GET",
		url: ctx.urls[pageNum],
		dataType: "xml"
	}).done(function(data) {
		q.resolve(data);
	}).fail(function(jqXHR, textStatus, errorThrown) {
		q.reject(errorThrown);
	});

	return q.promise
	.then(function(data) {
		var lst = [];
		data = $(data);
		var items = data.find("item");
		for(var i = 0; i < items.length; i++) {
			var item = $(items[i]);
			var link = item.children('enclosure');
			var type = link.attr('type');
			var itemJs = {};
			itemJs.name = item.children('title').text();
			var url = link.attr('url');
			
			if(type === 'application/rss+xml') {
				itemJs.type = 1;

				var m = /^page ([0-9]+)$/gim.exec(itemJs.name);
				if(m && m[1]) {
					ctx.urls[m[1]] = url;
					continue;
				}

				var path = (url || "").split("?")[0];

				var filmIdM = /\/drama\/info\/([0-9]+)/gim.exec(path);
		
				if(filmIdM && filmIdM[1]) {
					itemJs.filmId = filmIdM[1];
					itemJs.image = ($(item.children('description').text()).attr('src') || "").replace("http://", "https://");
				}

				var episodeIdM = /\/drama\/episode\/([0-9]+)/gim.exec(path);
				if(episodeIdM && episodeIdM[1]) {
					itemJs.episodeId = episodeIdM[1];
				}

			} else if(type === 'video/html') {
				if(!itemJs.name.match(/^[0-9]+p$/gim) && ['vTing', 'vBug'].indexOf(itemJs.name) < 0) continue;

				itemJs.type = 2;
				itemJs.mediaId = url;
				
			} else {
				continue;
			}

			itemJs.urls = {1: url};
			
			lst.push(itemJs);
		}

		return {
			list: lst,
			hasNext: !!(lst.length && ctx.urls[pageNum + 1]),
			ctx: ctx
		};
	});
}

function loadMp4(link, maxTries, disableRedirection) {
	return TinyCast.openUrl(link, null, {disableRedirection: disableRedirection})
	.then(function(res) {
		if(res.statusCode === 429) {
			return maxTries > 0 ? loadMp4(link, maxTries--, disableRedirection) : null;
		} else {
			return res.headers.location || res.headers.Location;
		}
	});
}

function loadLink(item) {
	return new Promise(function(resolve, reject) {
		var url = item.mediaId;

		if(item.name.match(/^[0-9]+p$/gim)) {
			var qs = {};
			var pts = (url.split("?", 2)[1] || "").split('&');
			for(var i = 0; i < pts.length; i++) {
				var kv = pts[i].split('=');
				qs['$' + decodeURIComponent(kv[0] || "")] = decodeURIComponent(kv[1] || "");
			}

			if(qs['$link']) {
				loadMp4(qs['$link'], 100, true)
				.then(function(mp4) {
					resolve(mp4 ? [{file: mp4}] : []);
				})
				.catch(function(err) {
					reject(err);
				});
			} else {
				reject(new Error("no link"));
			}
		} else if(item.name === 'vTing') {
			TinyCast.openUrl(url, null, {disableRedirection: true})
			.then(function(res) {
				return TinyCast.openUrl(res.headers.Location, null, {disableRedirection: true});
			})
			.then(function(res) {
				var m = /Player\(([0-9]+), ([0-9]+),/gim.exec(res.data);
				var body = "id=" + encodeURIComponent(m[1]) + "&server=" + encodeURIComponent(m[2]) + "&url=";
				return TinyCast.openUrl("http://videoembed.co/ajax/player", body, {disableRedirection: true})
				.then(function(res) {
					var m = /"mp4","file":"([^"]+)"/gim.exec(res.data);
					resolve(m[1] ? [{file: m[1]}] : []);
				});
			})
			.catch(function(err) {
				reject(err);
			});

		} else if(item.name === 'vBug') {
			TinyCast.openUrl(url, null, {disableRedirection: true})
			.then(function(res) {
				return TinyCast.openUrl(res.headers.Location, null, {disableRedirection: true});
			})
			.then(function(res) {
				var vb_token = /VB_TOKEN = "([^"]+)"/gim.exec(res.data);
				var vb_id = /VB_ID = "([^"]+)"/gim.exec(res.data);
				var body = "VB_TOKEN=" + encodeURIComponent(vb_token[1]) + "&VB_ID=" + encodeURIComponent(vb_id[1]) + "&VB_NAME=1";
				return TinyCast.openUrl("http://vb.icdrama.se/v/p", body, {disableRedirection: true})
				.then(function(res) {
					var links = [];
					var data = JSON.parse(res.data);
					for(var i = 0; i < data[1].length; i++) {
						var l = data[1][i];
						if(!l.u) continue;

						links.push({file: atob(l.u), name: l.s});
					}
					resolve(links);
				});
			})
			.catch(function(err) {
				reject(err);
			});

		} else {
			reject(new Error("can't handle"));
		}
	});
}

function loadContent(item, pageNum) {
	var q = TinyCast.defer();

	if(!item || item.type === 1) {
		loadRss(item, pageNum)
		.then(function(data) {
			q.resolve(data);
		})
		.catch(function(err) {
			q.reject(err);
		});
	} else if(item.type === 2) {
		loadLink(item).
		then(function(links) {
			q.resolve({list: links});
		})
		.catch(function(err) {
			q.reject(err);
		});
	}

	return q.promise;
}

var lku = {
	loadContent: loadContent
};

TinyCast.setReady(lku);


