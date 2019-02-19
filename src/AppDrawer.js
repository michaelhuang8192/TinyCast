import React, {Component} from 'react';
import {StyleSheet, FlatList, View, SectionList, NativeModules, DeviceEventEmitter, NativeEventEmitter, Platform, Slider, ScrollView} from 'react-native';
import {Container, Header, Title, Content, Footer, FooterTab, Button, Left, Right, Body, Icon, Text, ListItem, Spinner, Toast, Switch} from 'native-base';
import _ from 'lodash';
import xml2js from 'react-native-xml2js';
import { connect } from 'react-redux';
import { DrawerItems, SafeAreaView } from 'react-navigation';

class AppDrawer extends React.PureComponent {

	update_isDemoEnabled = (v) => {
		this.props.dispatch({
	      type: "settings_update",
	      payload: {
	        isDemoEnabled: v
	      }
	    });
	};

	render() {
		return (
		<ScrollView>
		    <SafeAreaView style={styles.container} forceInset={{ top: 'always', horizontal: 'never' }}>
		    <Header>
			    <Body>
	            	<Title>TinyCast</Title>
	            </Body>
	        </Header>
		    <Content>
		    <ListItem icon>
	            <Left>
	              <Button style={{ backgroundColor: "#007AFF" }}>
	                <Icon active name="link" />
	              </Button>
	            </Left>
	            <Body>
	              <Text>Enable Demo</Text>
	            </Body>
	            <Right>
	              <Switch onValueChange={this.update_isDemoEnabled} value={this.props.settings.get('isDemoEnabled')} />
	            </Right>
          	</ListItem>
          	</Content>
		    </SafeAreaView>
		</ScrollView>
		);
	}

}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});


export default connect(
  (state, ownProps) => {
    return {
      settings: state.settings
    };
  }
)(AppDrawer);

