import { combineReducers } from 'redux';
import _ from 'lodash';
import immu from 'immutable';

function settings(state=immu.Map(), action) {
	var pl = action.payload;
	switch(action.type) {
		case "settings_setCastDevice": {
			return state.set('castDevice', pl);
		}
	}

	return state;
}


exports.settings = settings;
