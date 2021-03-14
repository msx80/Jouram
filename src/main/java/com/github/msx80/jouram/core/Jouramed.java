package com.github.msx80.jouram.core;


/**
 * Additional interface that will be wired in the proxy beside the user one,
 * to be able to retrieve the reference to the manager
 *
 */
interface Jouramed {
	// do not change this method name
	InstanceManager getJouram();
}
