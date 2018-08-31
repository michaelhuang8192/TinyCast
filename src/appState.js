import { combineReducers } from 'redux';

import { videoSource } from './states/videoSource';
import { settings } from './states/settings';


export default combineReducers({
	videoSource,
	settings
});

