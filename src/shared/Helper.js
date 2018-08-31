
import { AsyncStorage } from "react-native";
import _ from 'lodash';


exports.saveSourceToDisk = (videoSource, names) => {
	return new Promise(function(resolve, reject) {
		var data = [];
		for(var i = 0; i < names.length; i++) {
			var name = names[i];

			var top = videoSource.get(name);
		    if(!top) continue;

		    var childrenJS = [];
		    var children = top.get('children');
		    for(var i = 0; i < children.size; i++) {
		      var id = children.get(i);
		      var item = videoSource.get(id);
		      if(!item) continue;

		      var childJS = item.toJS();
		      childJS.children = undefined;
		      childrenJS.push(childJS);
		    }

		    if(childrenJS.length)
		    	data.push({id: name, children: childrenJS});
		}

		resolve(data);
	}).then(data => {
		return AsyncStorage.setItem("app_videoSource", JSON.stringify(data));
	});
}

exports.loadSourceFromDisk = (dispatch) => {
	return AsyncStorage.getItem("app_videoSource")
	.then(result => {
		return result ? JSON.parse(result) : [];
	})
	.then(data => {
		for(var i = 0; i < data.length; i++) {
			var item = data[i];
			console.log(item);
			addSourceToStore(dispatch, item.id, true, item.children);
		}
	});
};

exports.addSourceToStore = addSourceToStore = (dispatch, id, addBefore, contents, maxSize) => {
	for(var j = 0; j < contents.length; j++) {
		var child = contents[j];
		child.pageNum = undefined;
		child.isEnd = undefined;
	}

	dispatch({
      type: "videoSource_update",
      payload: {
        id: id,
        addBefore: addBefore,
        children: contents,
        maxSize: maxSize
      }
    });
};



