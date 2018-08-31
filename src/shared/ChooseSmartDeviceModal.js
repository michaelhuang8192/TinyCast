import React, {Component} from 'react';
import {StyleSheet, FlatList, View, SectionList, NativeModules, DeviceEventEmitter, NativeEventEmitter, Platform} from 'react-native';
import {Container, Header, Title, Content, Footer, FooterTab, Button, Left, Right, Body, Icon, Text, ListItem, Spinner} from 'native-base';
import _ from 'lodash';
import xml2js from 'react-native-xml2js';
import { connect } from 'react-redux';

import ScreenComponent from '../libs/ScreenComponent';
import Util from '../libs/Util';

const RNSmartTvController = NativeModules.RNSmartTvController;
const eventEmitter = Platform.OS === 'ios' ? new NativeEventEmitter(RNSmartTvController) : DeviceEventEmitter;

class MyListItem extends React.PureComponent {
	_onPress = () => {
		this.props.onPressItem(this.props.item);
	}

	render() {
		return (
			<ListItem noIndent onPress={this._onPress}>
			<Text>{this.props.item.name}</Text>
			</ListItem>
			)
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
			<Left style={styles.fixedWidth}><Button transparent onPress={this.goBack}><Icon name='arrow-back' /></Button></Left>
			<Body><Title>Pick Your Cast Device</Title></Body>
			<Right style={styles.fixedWidth}></Right>
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
		return (
			<Container>
				{this._renderNavHeader()}
				<Content>
					<View style={styles.container}>
						<FlatList
						renderItem={this._renderItem}
						data={this.devicesList}
						keyExtractor={this._keyExtractor}
						ListEmptyComponent={this._getListEmptyComponent}
						/>
					</View>
				</Content>
			</Container>
		);
	}

}

const styles = StyleSheet.create({
  container: {
   flex: 1
  },
  fixedWidth: {
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


