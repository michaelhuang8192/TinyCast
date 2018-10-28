import { combineReducers } from 'redux';
import _ from 'lodash';
import immu from 'immutable';

function settings(state=immu.Map({isDemoEnabled: true}), action) {
	var pl = action.payload;
	switch(action.type) {
		case "settings_setCastDevice": {
			return state.set('castDevice', pl);
		}
		case "settings_update": {
			return state.merge(pl);
		}
	}

	return state;
}


exports.settings = settings;
