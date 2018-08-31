
import React, { Component } from 'react';
import { Provider } from 'react-redux';
import { createStore } from 'redux';
import { StackNavigator, createDrawerNavigator } from 'react-navigation';
import getTheme from './native-base-theme/components';
import { StyleProvider, Container, Header, Title, Content, Footer, FooterTab, Button, Left, Right, Body, Icon, Text, ListItem } from 'native-base';

import Home from "./src/Home/Home";
import appState from "./src/appState";

import { YellowBox } from 'react-native'
YellowBox.ignoreWarnings(['Warning: isMounted(...) is deprecated']);

const RootNavigator = createDrawerNavigator({
    Home: {
        screen: Home
    },
});

const store = createStore(appState);

export default class App extends Component {
    render() {
        return (
            <Provider store={store}>
                <StyleProvider style={getTheme()}>
                    <RootNavigator />
                </StyleProvider>
            </Provider>
        );
    }
};
