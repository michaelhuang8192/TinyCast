var TinyCast = require("/TinyCastSDK");
var $ = require("jquery");

function loadMainCategory(id, pageNum) {
	return new Promise(function(resolve, reject) {
		$.ajax({
			type: "GET",
			url: "https://m2.dnvod.tv/api/list/index",
			data: {
				cinema: 1,
				page: pageNum,
				cid: "0,1," + id,
				size: 24,
				isn: 0,
				isfree: -1
			}
		}).done(function(data) {
			resolve(data);
		}).fail(function(jqXHR, textStatus, errorThrown) {
			reject(errorThrown);
		});
	});
}

function getSubCategories(id) {
	return new Promise(function(resolve, reject) {
		$.ajax({
			type: "GET",
			url: "https://m.dnvod.tv/movie/list/" + id,
			dataType: "html"
		}).done(function(data) {
			resolve(data);
		}).fail(function(jqXHR, textStatus, errorThrown) {
			reject(errorThrown);
		});
	});
}

function loadPlay(item) {
	return new Promise(function(resolve, reject) {
		$.ajax({
			type: "GET",
			url: "https://m2.dnvod.tv/api/video/detail",
			data: {
				id: item.linkId,
				ispath: false,
				cinema: 1,
				device: "mobile",
				player: "CkPlayer",
				tech: "HLS",
				country: "US",
				lang: "none",
				v: 1
			}
		}).done(function(data) {
			resolve(data);
		}).fail(function(jqXHR, textStatus, errorThrown) {
			reject(errorThrown);
		});
	});
}

function loadLink(item) {
	return new Promise(function(resolve, reject) {
		$.ajax({
			type: "GET",
			url: "https://m2.dnvod.tv/api/video/play",
			data: {
				id: item.linkId,
				ispath: true,
				cinema: 1,
				device: "mobile",
				player: "CkPlayer",
				tech: "HLS",
				country: "US",
				lang: "none",
				v: 1
			}
		}).done(function(data) {
			resolve(data);
		}).fail(function(jqXHR, textStatus, errorThrown) {
			reject(errorThrown);
		});
	});
}

var mainCates = [
	{
		type: 0,
		cateId: 3,
		name: "电影"
	},
	{
		type: 0,
		cateId: 4,
		name: "电视剧"
	},
	{
		type: 0,
		cateId: 5,
		name: "综艺"
	},
	{
		type: 0,
		cateId: 6,
		name: "动漫"
	}
];

var subCates =  {
	4: [
		{"type":0,"cateId":"4,13","name":"国产剧"},
		{"type":0,"cateId":"4,14","name":"港台剧"},
		{"type":0,"cateId":"4,16","name":"欧美剧"},
		{"type":0,"cateId":"4,15","name":"韩剧"},
		{"type":0,"cateId":"4,114","name":"日剧"},
		{"type":0,"cateId":"4,18","name":"英剧"},
		{"type":0,"cateId":"4,17","name":"新马泰"}
	]
};

function getPopular(count) {
	if(!count) return null;

	if(count < 1000) return count;
	if(count >= 1000) return (count / 1000).toFixed(1) + "K";
}

function loadContent(item, pageNum) {
	var q = TinyCast.defer();

	if(!item) {
		q.resolve(pageNum == 1 ? mainCates : []);
	} else if(item.type === 0) {
		if(item.cateId === 4) {
			q.resolve(pageNum == 1 ? subCates[item.cateId] : []);
		} else {
			loadMainCategory(item.cateId, pageNum)
			.then(function(res) {
				var info = res.data.info || [];
				for(var i = 0; i < info.length; i++) {
					var mov = info[i];
					var pop = getPopular(mov.Hot);
					mov.name = mov.Title + (pop ? " ("+pop+")" : "");
					mov.type = 1;
					mov.linkId = mov.Contxt;
					mov.filmId = mov.linkId;
					mov.image = "https:" + mov.ImgPath;
				}
				q.resolve(info);
			})
			.catch(function(err) {
				q.reject(err);
			});
		}
	} else if(item.type === 1) {
		if(pageNum != 1) {
			q.resolve([]);
		} else {
			loadPlay(item)
			.then(function(res) {
				var info = (res.data.info || [])[0];
				var data = [];

				var langs = !item.noLang && info.LanguageList || [];
				for(var i = 0; i < langs.length; i++) {
					var lang = langs[i];
					lang.name = lang.Title;
					lang.type = 1;
					lang.linkId = lang.Link;
					lang.noLang = true;
					data.push(lang);
				}

				var series = info.GuestSeriesList || [];
				for(var i = 0; i < series.length; i++) {
					var serie = series[i];
					serie.linkId = serie.Key;
					serie.name = serie.Name;
					serie.type = 2;
					serie.episodeId = serie.linkId;
					serie.mediaId = serie.linkId;
					data.push(serie);
				}

				q.resolve(data);
			})
			.catch(function(err) {
				q.reject(err);
			})
		}
	} else if(item.type === 2) {
		loadLink(item)
		.then(function(res) {
			var info = (res.data.info || [])[0];
			var data = [];

			var paths = info.FlvPathList || [];
			for(var i = paths.length - 1; i < paths.length; i++) {
				var path = paths[i];
				data.push({file: path.Result});
			}
			q.resolve(data);
		})
		.catch(function(err) {
			q.reject(err);
		});

	} else {
		q.resolve([]);
	}

	return q.promise
	.then(function(lst) {
		return {
			list: lst,
			hasNext: !!lst.length
		}
	});
}

var lku = {
	loadContent: loadContent
};

TinyCast.setReady(lku);

