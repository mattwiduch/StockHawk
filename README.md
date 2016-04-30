# Stock Hawk - Stock value tracker

<img src="https://cloud.githubusercontent.com/assets/15446842/14938268/e4fbc2c8-0f15-11e6-8fe3-b0e28690a957.png"/>

Stock Hawk is stock tracking application for Android tablets and smartphones running Android 4.0.3 (API level 15) or newer. It demonstrates ability of taking an [app](http://www.mjolnir.io/#/stockhawk/) from a functional state to a production-ready state. The base code was provided by [Sam Chordas] (https://github.com/schordas) for Udacity Android Nanodegree students.

**New Features:**
* New screen which graphs the stock's value over time
* Stock data can be displayed in information and collection widgets
* User interface controls feature descriptive text to allow talkback
* App can be navigated using directional controller such as d-pad
* Includes Polish translation and is fully translatable to other languages
* RTL layouts and text are fully supported
* Several UX enhancements including:
 - Brand new UI with improved color palette, launcher and toolbar icons
 - Stock list can be refreshed on demand by swiping down
 - Stock list can be sorted by symbol, price or change
 - Last update time is displayed on the main screen
 - Tablet layout based on master-detail flow
 
**Bug fixes:**
* App does not crash when user searches for a non-existent stock
* Appropriate messages are shown when errors occur
 


## Dependencies
Stock Hawk uses following third-party libraries:
- [Schematic] (https://github.com/SimonVT/schematic)
- [OkHttp] (http://square.github.io/okhttp/)
- [WilliamChart] (https://github.com/diogobernardino/WilliamChart)
- [ThreeTenABP] (https://github.com/JakeWharton/ThreeTenABP)
- [ButterKnife] (http://jakewharton.github.io/butterknife/)

## Try it out
Try Stock Hawk by following these steps:

1. Download repo
2. Run `./gradlew clean build` in root directory
3. Enjoy!

## License
```
Copyright (C) 2016 Mateusz Widuch

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
