import React, { Component } from 'react';
import { StyleSheet, FlatList, View, SectionList, NativeModules } from 'react-native';
import { Container, Header, Title, Content, Footer, FooterTab, Button, Left, Right, Body, Icon, Text, ListItem, Toast } from 'native-base';
import { StackNavigator, DrawerNavigator } from 'react-navigation';
import { connect } from 'react-redux';
import shortid from "shortid";
import _ from 'lodash';

import Category from "./Category"
import Util from './libs/Util';
import ScreenComponent from './libs/ScreenComponent';
import ChooseSmartDeviceModal from './shared/ChooseSmartDeviceModal';
import Helper from './shared/Helper.js';
import immu from './libs/immutable.min';


class MyListItem extends React.PureComponent {
  _onPress = () => {
    this.props.onPressItem(this.props.item);
  }

  render() {
    var item = this.props.item;
    return (
      <ListItem noIndent onPress={this._onPress}>
        <Left style={styles.listItemLeft}><Icon name={item.get('filmId') ? "movie" : "web-asset"} type="MaterialIcons" /></Left>
        <Body><Text numberOfLines={1}>{item.get('name')}</Text></Body>

      </ListItem>
    )
  }
}


class Home extends ScreenComponent {
  constructor(props) {
    super(props);

    this.sources = ["Recent View", "Demo Source", "My Source"];

    this.getChildrenArray = Util.cacheMore((key, immuObj) => {
      return immuObj ? immuObj.get('children').toArray() : [];
    });

    //this.props.navigation.setParams({
    //    renderDrawer: this._renderDrawer
    //});
  }

  _addSource = () => {
    this.props.dispatch({
      type: "videoSource_update",
      payload: {
        id: "Demo Source",
        children: [
          {
            id: "dnvod",
            name: "dnvod",
            srcInfo: {
              baseUrl: 'https://m.dnvod.tv/',
              scriptSrc: 'https://s3-us-west-2.amazonaws.com/tinycastfiles/sources/dnvod.js'
            }
          },
          {
            id: "azdrama",
            name: "azdrama",
            srcInfo: {
              baseUrl: 'http://myrss.nu/drama',
              scriptSrc: 'http://s3-us-west-2.amazonaws.com/tinycastfiles/sources/azdrama.js'
            }
          }
        ]
      }
    });
  };

  startDiscovery = () => {
    this.props.navigation.navigate("ChooseSmartDeviceModal", {
    });
  };

  toggleDrawer = () => {
    this.props.navigation.toggleDrawer();
  };

  _renderNavHeader = () => {
    return (
      <Header>
        <Left>
          <Button transparent onPress={this.toggleDrawer}><Icon name="menu" /></Button>
        </Left>
        <Body><Title>TinyCast</Title></Body>
        <Right>
          <Button transparent onPress={this.startDiscovery}><Icon name={this.props.settings.get('castDevice') ? "cast-connected" : "cast"} type="MaterialIcons" /></Button>
        </Right>
      </Header>
    );
  };

  _renderItem = ({item}) => {
      return (<MyListItem item={this.props.videoSource.get(item)} onPressItem={this._onPressItem} />);
  };

  _onPressItem = item => {
      this.props.navigation.push("Category", {
        isRoot: true,
        id: item.get('id'),
        dispatch: this.props.dispatch
      });
  };

  _renderSectionHeader = ({section: {title}}) => (
    <ListItem itemDivider><Text>{title}</Text></ListItem>
  );

  _keyExtractor = (id, index) => {
    return id;
  };

  componentDidMount() {
    super.componentDidMount();

    Helper.loadSourceFromDisk(this.props.dispatch);
    this._addSource();
  }

  render() {

    var isDemoEnabled = this.props.settings.get('isDemoEnabled');
    var videoSource = this.props.videoSource;
    var lst = [];

    for(var i = 0; i < this.sources.length; i++) {
      var name = this.sources[i];
      if(name == 'Demo Source' && !isDemoEnabled) continue;

      var arr = this.getChildrenArray(name, videoSource.get(name));

      if(arr && arr.length)
        lst.push({
          key: name,
          title: name,
          data: arr
        });
    }

  	return (
        <Container>
            {this._renderNavHeader()}
            <View style={styles.container}>
              <SectionList
              renderItem={this._renderItem}
              renderSectionHeader={this._renderSectionHeader}
              sections={lst}
              keyExtractor={this._keyExtractor}
              />
            </View>
        </Container>
        
    );

  }
}

const styles = StyleSheet.create({
  container: {
   flex: 1
  },
  listItemLeft: {
    flex: 0
  }
});


var HomeS = connect(
  state => {
    return {
      videoSource: state.videoSource,
      settings: state.settings
    };
  }
)(Home);

export default StackNavigator({
  Home: { screen: HomeS },
  Category: { screen: Category },
  ChooseSmartDeviceModal: { screen: ChooseSmartDeviceModal }
}, {
  headerMode: 'none'
});


