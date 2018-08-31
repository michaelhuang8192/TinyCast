import { combineReducers } from 'redux';
import _ from 'lodash';
import immu from '../libs/immutable.min';

function videoSource(state=immu.Map(), action) {
	var pl = action.payload;
	switch(action.type) {
		case "videoSource_update": {
			var source = state.get(pl.id, immu.Map());
			var children = source.get('children', immu.List());
			var visited = source.get('visited', immu.Set())
				.concat((pl.props || {}).visited || []);

			var ids = _.map(pl.children, v => v.id);
			var idsSet = new Set(ids);
			children = children.filter(v => {
				return !idsSet.has(v);
			});
			
			if(pl.addBefore) {
				children = children.unshift(...ids);
				if(pl.maxSize != null && children.size > pl.maxSize) {
					var delIds = [];
					for(var i = pl.maxSize; i < children.length; i++)
						delIds.push(children.get(i));
					state = removeIds(state, delIds);
					children = children.slice(0, pl.maxSize);
				}
			} else
				children = children.push(...ids);

			var source = source.merge(pl.props || {}, {children: children, visited: visited});
			state = state.withMutations(m => {
				m = m.set(pl.id, source);
				for(var i = 0; i < pl.children.length; i++) {
					var s = pl.children[i];
					var c = m.get(s.id, immu.Map());
					children = c.get('children', immu.List()).push(...(s.children || []));
					visited = c.get('visited', immu.Set()).concat(s.visited || []);
					c = c.merge(s, {
						children: children,
						visited: visited
					});
					m = m.set(s.id, c);
				}
				return m;
			});

			return state;
		}
	}

	return state;
}

function removeIds(state, delIds) {
	var ids = [...delIds];
	return state.withMutations(m => {
		while(ids.length) {
			var id = ids.pop();
			var item = m.get(id);
			m = m.delete(id);
			var children = item ? item.get('children') : null;
			if(!children || !children.size) continue;
			ids.push.apply(ids, children.toJS());
		}
		return m;
	});
}

exports.videoSource = videoSource;


