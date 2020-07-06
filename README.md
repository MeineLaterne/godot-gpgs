# Google Play Game Services (Android) for Godot Game Engine
This is a Google Play Game Services module for the Godot Game Engine. This module is meant to be used for games deployed to an Android device. This module allows you to use nearly all the features provided by the Google Play Games Service API for Android including:
- Log-in with Google
- Information about the signed in player (including their profile pictures)
- Achievements
- Leaderboards
- Saved Games (Snapshots)
- Device network status

This module uses Google Play Services' powerful Tasks API to execute various operations asynchronously. To handle the results from these asynchronous operations, a lot of relevant callback functions are also provided and can be used in GDScript.

**NOTE:** This module is compatible with Godot 3.2.2 but has not been tested with the Mono version. So, using this module for developing Godot games using C# could lead to unforeseen errors.

## Setup
1. Clone or download this repository
2. Place the Godot Java library into gpgs/libs
3. Setup your Project in your Google Play Console
4. Place your game-ids.xml file inside gpgs/src/main/res/values
5. Run `gradlew build`
6. Find the compiled library under gpgs/build/outputs/aar (it's called GooglePlayGameServices.1.0.0.release.aar or something similar)
7. Enable 'custom build' in your Godot project
8. Move your compiled aar and the GooglePlayGameServices.gdap file into your android/plugins folder
10. Enable the following permissions in the Export menu
	- Access Network State
	- Internet

## Using the module in your game
1. To use the module in GDScript
	```python
	if Engine.has_singleton("GooglePlayGameServices"):
		gpgs = Engine.get_singleton("GooglePlayGameServices")
		gpgs.init(get_instance_id(), true)
	```
2. Now, you should be able to call the functions in the `gpgs` object (singleton) in order to use the Google Play Game Services.

## Functions and Callbacks
See the [Wiki](https://github.com/Kopfenheim/godot-gpgs/wiki) for a description of the various functions that you can call on the `gpgs` object and the various callbacks that you can listen for in your GDScript file
