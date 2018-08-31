import React, { Component } from 'react';


class ScreenComponent extends Component {
	constructor(props) {
		super(props);

		this._shouldDelayUpdate = false;
	}

	goBack = () => {
    	if(this.props.navigation) this.props.navigation.goBack();
  	};

  	goHome = () => {
  		if(this.props.navigation) this.props.navigation.popToTop();
  	}

	componentDidMount() {
		this._didFocusSubscription = this.props.navigation.addListener(
			'didFocus',
			payload => {
				if(this._shouldDelayUpdate) this.setState({});
			}
		);
	}

	componentWillUnmount() {
		this.didUnmount = true;
		if(this._didFocusSubscription) this._didFocusSubscription.remove();
	}

	shouldComponentUpdate(nextProps, nextState) {
		this._shouldDelayUpdate = !this.props.navigation.isFocused();
		return !this._shouldDelayUpdate;
	}
};
/*
ScreenComponent.navigationOptions = ({ navigation }) => {
  var params = (navigation.state || {}).params || {};

  return {
    header: params.renderHeader && params.renderHeader()
  };
};
*/
export default ScreenComponent;
