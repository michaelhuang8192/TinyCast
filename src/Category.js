import React, { Component } from 'react';
import { StyleSheet, FlatList, VirtualizedList, View, SectionList, WebView, ActivityIndicator, NativeModules, Image, RefreshControl } from 'react-native';
import { Container, Header, Title, Content, Footer, FooterTab, Button, Left, Right, Body, Icon, Text, ListItem, Spinner, Thumbnail, Toast } from 'native-base';
import Promise from 'bluebird';
import { connect } from 'react-redux';
import shortid from "shortid";
import _ from 'lodash';

import Util from './libs/Util';
import WebComm from './libs/WebComm';
import ScreenComponent from './libs/ScreenComponent';
import Helper from './shared/Helper.js';

const RNSmartTvController = NativeModules.RNSmartTvController;

class MyListItem extends React.PureComponent {
  _onPress = () => {
    this.props.onPressItem(this.props.item);
  }

  render() {
    var item = this.props.item;
    var data = item.get('data');
    //console.log(">>>", this.props.item.toJS(), data);
    if(item.get('filmId') && data.image) {
      return (
        <ListItem noIndent onPress={this._onPress} style={styles.listItem}>
          <Left style={styles.listItemLeft}><Image resizeMode={'center'} source={{ uri: data.image }} style={styles.listItemImage}/></Left>
          <Body><Text numberOfLines={1}>{this.props.index + 1}. {this.props.item.get('name')}</Text></Body>
        </ListItem>
      );
    } else {
      var style = this.props.isVisited ? styles.visited : null;
      return (
        <ListItem noIndent onPress={this._onPress}>
          <Left style={styles.listItemLeft}><Icon name={item.get('mediaId') ? "play-circle-outline" : "folder-open"} type="MaterialIcons" /></Left>
          <Body><Text style={style}>{this.props.index + 1}. {this.props.item.get('name')}</Text></Body>
        </ListItem>
      );
    }

  }
}

class Category extends ScreenComponent {
  constructor(props) {
    super(props);

    this.state = {isLoading: false};
    this.isDone = false;

    var params = this.getParams();
    this.id = params.id;
    this.isRoot = params.isRoot;
    this.webviewCommQ = params.webviewCommQ || Util.defer();
    this.webComm = new WebComm(
      (msg) => {
        if(this.refs && this.refs.webView) {
          this.refs.webView.postMessage(msg, '*');
        }
      },
      {
        ready: () => {
          console.log(">>>>>ready");
          this.webviewCommQ.resolve(this.webComm);
          return Promise.resolve();
        },
        openUrl: (url, body, options) => {
          url = (url || "").replace("http://", "https://");
          //console.log(">>>>url", url, body, options);
          return RNSmartTvController.openUrl(url, body, options)
          .then(res => {
            return res;
          });
        }
      }
    );

    if(this.isRoot) {
      var srcInfo = this.props.source.get('srcInfo');
      this.webviewCfg = {
        baseUrl: srcInfo.baseUrl,
        html: `
<html>
<head>
<script type="text/javascript" src="https://s3-us-west-2.amazonaws.com/tinycastfiles/sources/sdk.js?{new Date().getTime()}"></script>
<script type="text/javascript" src="${srcInfo.scriptSrc}?{new Date().getTime()}"></script>
</head>
<body>
</body>
</html>`
      };
    }

    this._refreshControl = (<RefreshControl refreshing={this.state.isLoading} onRefresh={this._onRefresh} />);

  }

  _onRefresh = () => {
    //clear
    this.props.dispatch({
      type: "videoSource_update",
      payload: {
        id: this.id,
        addBefore: true,
        children: [],
        maxSize: 0,
        props: {
          pageNum: undefined,
          isEnd: undefined
        }
      }
    });
    console.log(">>>>_onRefresh");
    setTimeout(() => {
      this._loadContent();
    }, 0);
  };

  startDiscovery = () => {
    var defer = Util.defer();
    this.props.navigation.navigate("ChooseSmartDeviceModal", {
      onClose: selectedDevice => { defer.resolve(selectedDevice); }
    });
    return defer.promise;
  };

  _renderNavHeader = () => {
    return (
      <Header>
        <Left><Button transparent onPress={this.goBack}><Icon name='arrow-back' /></Button></Left>
        <Body><Title>{this.props.source.get('name')}</Title></Body>
        <Right>
          <Button transparent onPress={this.goHome}><Icon name='home' type="MaterialIcons" /></Button>
          <Button transparent onPress={this.startDiscovery}><Icon name={this.props.settings.get('castDevice') ? "cast-connected" : "cast"} type="MaterialIcons" /></Button>
        </Right>
      </Header>
    );
  };

  getParams = () => {
    return this.props.navigation.state.params;
  };

  _onMessage = (event) => {
    //console.log(">>onMsg", event);
    if(event.nativeEvent && event.nativeEvent.data) {
      var msg = event.nativeEvent.data;
      if(!msg || !this.webComm.isValidMsg(msg)) return;

      this.webComm.dispatchMsg(msg);
    }
  };

  _renderItem = ({item, index}) => {
    var itemV = this.props.videoSource.get(item);
    var episodeId = itemV.get('episodeId');
    var isVisited = episodeId && this.props.visited && this.props.visited.get(episodeId);
    return (<MyListItem index={index} key={item} isVisited={isVisited} item={itemV} onPressItem={this._onPressItem} />);
  };

  _loadContent = () => {
    var source = this.props.source;
    console.log(">>>>source", source.toJS());
    if(this.state.isLoading || !this.webviewComm || source.get('isEnd')) return;
    
    var nextPageNum = (source.get('pageNum') || 0) + 1;

    this.setState({isLoading: true});

    return this.webviewComm.makeCall("loadContent", [source.get('data'), nextPageNum])
    .then((res) => {
      var list = _.map(res.list, (v) => {
        return {
          id: shortid.generate(),
          name: v.name,
          data: v,
          mediaId: v.mediaId,
          episodeId: v.episodeId,
          filmId: v.filmId
        };
      });

      var newProps = {
        pageNum: nextPageNum,
        isEnd: !res.hasNext
      };
      if(res.ctx) newProps.data = res.ctx;

      this.props.dispatch({
        type: "videoSource_update",
        payload: {
          id: this.id,
          props: newProps,
          children: list
        }
      });

    })
    .catch(function(err) {
      console.log(">>_loadContent -> error:", err);
    })
    .finally(() => {
      this.setState({isLoading: false});
    });
  };

  playMedia = (link) => {
    return RNSmartTvController.playMedia(link);
  };

  loadFile = (item) => {
    if(!this.webviewComm) return;

    Toast.show({text: "Fetching video links..."});
    console.log(">>>loadFile", item.get('data'));
    this.webviewComm.makeCall("loadContent", [item.get('data')])
    .then(res => {
      console.log(">loadFile:", res);

      if(res && res.list && res.list[0] && res.list[0].file) {
        var link = res.list[0].file;
        Toast.show({text: "Playing..."});
        return this.playMedia(link)
        .then(res => {
          if(!res)
            Toast.show({text: "Unable to play", buttonText: "OK", type: "danger"});
        });
      } else {
        Toast.show({text: "Unable to fetch links", buttonText: "OK", type: "danger"});
      }
    })
    .catch(err => {
      Toast.show({text: "Unable to load", buttonText: "OK", type: "danger"});
      console.log(">>loadFile -> error:", err);
    });
  };

  _onPressItem = (item) => {
      var params = this.getParams();
      var episodeId = item.get('episodeId') || params.episodeId;
      var rootId = this.props.rootId;

      if(item.get('mediaId')) {
        RNSmartTvController.getCurrentCastDevice()
        .then(device => {
            return device ? RNSmartTvController.isConnected() : false;
        })
        .then(isConnected => {
            if(!isConnected) {
              setTimeout(() => {
                this.props.dispatch({
                  type: "settings_setCastDevice",
                  payload: undefined
                });
              }, 0);
            }

            return isConnected ? true : 
              this.startDiscovery().then(device => { return !!device; });
        })
        .then(isConnected => {
          if(isConnected)
            return this.loadFile(item);
        });

        var groupItem = this.props.groupItem;
        if(groupItem) {
          var rootItem = this.props.rootItem;
          var recentViewId = rootId + "$" + groupItem.get('filmId');
          if(rootItem && rootItem.get('isRecentView'))
            recentViewId = rootId;

          groupItem = groupItem.toJS();
          groupItem.id = recentViewId;
          groupItem.isRecentView = true;
          groupItem.srcInfo = rootItem.get('srcInfo');
          groupItem.visited = [episodeId];
          groupItem.children = [];

          Helper.addSourceToStore(
            this.props.dispatch, "Recent View", true, [groupItem], 6
          );

          setTimeout(() => {
            Helper.saveSourceToDisk(
              this.props.videoSource, ['Recent View', 'My Source']
            );
          }, 0);
          
        }

      } else {
        this.props.navigation.push("Category", {
          isRoot: false,
          id: item.get('id'),
          dispatch: this.props.dispatch,
          webviewCommQ: this.webviewCommQ,
          groupId: this.props.groupId,
          episodeId: episodeId,
          rootId: rootId
        });
      }
  };

  _keyExtractor = (item, index) => item;

  _onEndReached = (info) => {
    if(!this.webviewComm) return;

    this._loadContent();
  };

  _getItemCount = (data) => {
    return data ? data.size : 0;
  };

  _getItem = (data, index) => {
    return data.get(index);
  }

  _onError = (err) => {
    console.log(">>errr", err);
  }

  componentDidMount() {
    super.componentDidMount();

    this.webviewCommQ.promise
    .then((webviewComm) => {
      this.webviewComm = webviewComm;

      var source = this.props.source;
      if(source.get('isEnd') || source.get('children').size) {
        this.setState({});
        return;
      }

      this._loadContent();
    });
  }

  render() {
    var params = this.getParams();

    var webView = null;
    if(params.isRoot) {
      webView = <WebView
        ref="webView"
        source={this.webviewCfg}
        style={styles.webview}
        mixedContentMode={'always'}
        onMessage={this._onMessage}
        onError={this._onError}
      />;
    }

    var loader = null;
    if(this.state.isLoading || !this.webviewComm) {
      loader = <ActivityIndicator size="large" color="#0000ff" style={{position:"absolute", left:"50%", top:"50%", marginLeft:-50, marginTop:-50, width:100, height:100}} />;
    }

  	return (
      <Container>
          {this._renderNavHeader()}
          <View style={styles.container}>
          <VirtualizedList
          data={this.props.source.get('children')}
          style={styles.contentList}
          renderItem={this._renderItem}
          keyExtractor={this._keyExtractor}
          onEndReachedThreshold={0.1}
          onEndReached={this._onEndReached}
          getItemCount={this._getItemCount}
          getItem={this._getItem}
          extraData={this.props.visited}
          refreshControl={this._refreshControl}
          />
          {webView}
          {loader}
          </View>
      </Container>
    );

  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  webview: {
    flex: 0,
    width: 0,
    height:0,
    display: 'none'
  },
  contentList: {
    height: "100%",
    width: "100%"
  },
  listItemImage: {
    height: 120, width: 80
  },
  listItem: {
    height: 126
  },
  listItemLeft: {
    flex: 0
  },
  visited: {
    color: 'blue'
  }
});

export default connect(
  (state, ownProps) => {
    var params = ownProps.navigation.state.params;
    var source = state.videoSource.get(params.id);
    var groupId = source && source.get('filmId') ? params.id : params.groupId;
    var groupItem = state.videoSource.get(groupId);
    var rootId = params.isRoot ? params.id : params.rootId;
    var rootItem = state.videoSource.get(rootId);

    var visited;
    if(groupItem && rootItem) {
      if(rootItem.get('isRecentView'))
        visited = groupItem.get('visited');
      else {
        var vid = rootId + "$" + groupItem.get('filmId');
        visited = state.videoSource.getIn([vid, "visited"]);
      }
    }

    return {
      source: source,
      videoSource: state.videoSource,
      settings: state.settings,
      groupId: groupId,
      groupItem: groupItem,
      rootId: rootId,
      rootItem: rootItem,
      visited: visited
    };
  }
)(Category);

