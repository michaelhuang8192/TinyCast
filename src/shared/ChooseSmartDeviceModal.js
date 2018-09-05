import React, {Component} from 'react';
import {StyleSheet, FlatList, View, SectionList, NativeModules, DeviceEventEmitter, NativeEventEmitter, Platform, Slider} from 'react-native';
import {Container, Header, Title, Content, Footer, FooterTab, Button, Left, Right, Body, Icon, Text, ListItem, Spinner, Toast} from 'native-base';
import _ from 'lodash';
import xml2js from 'react-native-xml2js';
import { connect } from 'react-redux';

import ScreenComponent from '../libs/ScreenComponent';
import Util from '../libs/Util';
import {sprintf} from "sprintf-js";

const RNSmartTvController = NativeModules.RNSmartTvController;
const eventEmitter = Platform.OS === 'ios' ? new NativeEventEmitter(RNSmartTvController) : DeviceEventEmitter;

class MyListItem extends React.PureComponent {
	_onPress = () => {
		this.props.onPressItem(this.props.item);
	}

	render() {
		return (
			<ListItem noIndent onPress={this._onPress}>
			<Text numberOfLines={1}>{this.props.item.name}</Text>
			</ListItem>
			)
	}
}

class CastDeviceControl extends React.Component {
	constructor(props) {
	    super(props);

	    this.state = {
	    	isPlaying: false,
	    	position: 0,
	    	duration: 0,
	    };
	}

	componentDidMount() {
		this.startStatusUpdater();
	}

	_updateStatus = () => {
		var _statusUpdater = this._statusUpdater;
		RNSmartTvController.getPlayerStatus()
		.then(status => {
			if(_statusUpdater !== this._statusUpdater || this.unmounted || !status) return;

			this.setState({
				isPlaying: status.state === 'playing',
				position: status.position || 0,
				duration: status.duration || 0
			});
		});
	};

	startStatusUpdater = () => {
		this.stopStatusUpdater();
		this._statusUpdater = setInterval(() => {
			this._updateStatus();
		}, 1000);
		this._updateStatus();
	};

	stopStatusUpdater = () => {
		if(this._statusUpdater) {
			clearInterval(this._statusUpdater);
			this._statusUpdater = null;
		}
	};

	componentWillUnmount() {
		this.unmounted = true;
		this.stopStatusUpdater();
	}

	_onSlidingComplete = (position) => {
		console.log(">>>seek", position);
		this.setState({draggedPosition: null});
		RNSmartTvController.seek(position || 0)
		.then(() => {

		});
	};

	playOrPause = () => {
		if(!this.state.duration) return;

		Promise.resolve(this.state.isPlaying ? RNSmartTvController.pause() : RNSmartTvController.play())
		.then(() => {

		});
	};

	forward = () => {
		var position = Math.min(this.state.position + 300, this.state.duration);
		RNSmartTvController.seek(position || 0)
		.then(() => {

		});
	};

	backward = () => {
		var position = Math.max(this.state.position - 300, 0);
		RNSmartTvController.seek(position || 0)
		.then(() => {

		});
	};

	_updatePosition = (position) => {
		this.setState({draggedPosition: position});
	};

	formatTime = (sec) => {
		var rmd = sec / 60;
		var sec = sec % 60;
		var min = rmd % 60;
		var hr = rmd / 60;

		return sprintf("%02d:%02d:%02d", hr, min, sec);
	};

	render() {
		return (
		<View style={{flex:0, height:200, borderTopColor: '#cccccc', borderTopWidth:1, padding:10}}>
		<Title numberOfLines={1}>{this.props.device.name}</Title>
		<View style={{flexDirection: 'row', alignItems: 'center', paddingTop:18, paddingBottom:18}}>
			<Slider
				style={{flex: 1}}
	          	step={1}
	          	maximumValue={this.state.duration}
	          	value={this.state.position}
	          	onValueChange={this._updatePosition}
	          	onSlidingComplete={this._onSlidingComplete}
	        />
	        <Text numberOfLines={1} style={{flex:0, width:80, textAlign:'right'}}>{this.formatTime(this.state.draggedPosition != null ? this.state.draggedPosition : this.state.position)}</Text>
        </View>
        <View style={{flexDirection: 'row', justifyContent: 'space-around'}}>
        	<Button light><Icon name='step-backward' type='FontAwesome' /></Button>
        	<Button light onPress={this.backward}><Icon name='backward' type='FontAwesome' /></Button>
        	<Button light onPress={this.playOrPause}><Icon name={this.state.isPlaying ? 'pause' : 'play'} type='FontAwesome' /></Button>
        	<Button light onPress={this.forward}><Icon name='forward' type='FontAwesome' /></Button>
        	<Button light><Icon name='step-forward' type='FontAwesome' /></Button>
        </View>
		</View>
		);
	}
}

class ChooseSmartDeviceModal extends ScreenComponent {

	constructor(props) {
	    super(props);

	    //this.props.navigation.setParams({
	    //    renderHeader: this._renderNavHeader
	    //});

	    this.devicesMap = {};
	    this.devicesList = [];
	}

	_renderNavHeader = () => {
		return (
			<Header>
			<Left style={styles.fixed}><Button transparent onPress={this.goBack}><Icon name='arrow-back' /></Button></Left>
			<Body><Title>Pick Your Cast Device</Title></Body>
			<Right style={styles.fixed}></Right>
			</Header>
		);
	};

	_renderItem = ({item}) => {
		return (<MyListItem item={this.devicesMap[item]} onPressItem={this._onPressItem} />);
	};

	isDeviceConnected = () => {
		return Util.waitUtilTimeout(() => {
			return RNSmartTvController.isConnected();
		}, 10000);
	};

	_onPressItem = item => {
		Toast.show({text: "Connecting to " + item.name});
		RNSmartTvController.selectDevice(item.id)
		.then(device => {
			RNSmartTvController.connect();
			return this.isDeviceConnected()
			.then(() => {
				return device;
			});
		})
		.then(device => {
			this.props.dispatch({
				type: "settings_setCastDevice",
        		payload: device
			});
			this.selectedDevice = device;
		})
		.then(() => {
			this.props.navigation.goBack();
		})
		.catch(() => {
			Toast.show({
				text: "Unable to connect to " + item.name,
				buttonText: "OK",
				type: "danger"
			});
		});
	};

	_keyExtractor = (item, index) => {
		return item;
	};

	onDiscoveryFoundDevice = (data) => {
		var device = this.devicesMap[data.id];
		this.devicesMap[data.id] = data;
		if(!device) {
			this.devicesList = this.devicesList.concat(data.id);
		}
		this.setState({});
	}

	componentDidMount() {
		super.componentDidMount();

		this.discoveryListener = eventEmitter.addListener(
			"OnDiscoveryFoundDevice",
			this.onDiscoveryFoundDevice
		);
		RNSmartTvController.startDiscovery(5000);
	}

	componentWillUnmount() {
		super.componentWillUnmount();

		RNSmartTvController.stopDiscovery();
		if(this.discoveryListener) this.discoveryListener.remove();

		var onClose = (this.props.navigation.state.params || {}).onClose;
		if(onClose)
			onClose(this.selectedDevice);
	}

	_getListEmptyComponent = () => {
		return (<Content style={{paddingTop: 20}}><Spinner color='green' /><Text style={{textAlign: "center"}}>Searching...</Text></Content>);
	};

	render() {
		var castDevice = this.props.settings.get('castDevice');
	
		return (
			<Container>
				{this._renderNavHeader()}
				<View style={styles.container}>
					<FlatList
					style={styles.fixed}
					renderItem={this._renderItem}
					data={this.devicesList}
					keyExtractor={this._keyExtractor}
					ListEmptyComponent={this._getListEmptyComponent}
					/>
					{!castDevice ? null : 
					<CastDeviceControl device={castDevice} />
					}
				</View>
			</Container>
		);
	}

}

const styles = StyleSheet.create({
  container: {
  	flex: 1
  },
  fixed: {
  	flex: 0
  }
});

export default connect(
  (state, ownProps) => {
    var params = ownProps.navigation.state.params;
    return {
      settings: state.settings
    };
  }
)(ChooseSmartDeviceModal);


