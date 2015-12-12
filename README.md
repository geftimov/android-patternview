## android-patternview

[![Join the chat at https://gitter.im/geftimov/android-patternview](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/geftimov/android-patternview?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-android--patternview-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1495) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.eftimoff/android-patternview/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.eftimoff/android-patternview)

View for locking and unlocking.

![svg](https://github.com/geftimov/android-patternview/blob/master/art/rsz_empty_pattern.png) ![svg](https://github.com/geftimov/android-patternview/blob/master/art/rsz_pattern_correct.png) ![svg](https://github.com/geftimov/android-patternview/blob/master/art/rsz_mm.png) ![svg](https://github.com/geftimov/android-patternview/blob/master/art/rsz_small.png) ![svg](https://github.com/geftimov/android-patternview/blob/master/art/rsz_skyscrapers.png)

##### How to use

    <com.eftimoff.patternview.PatternView
        xmlns:patternview="http://schemas.android.com/apk/res-auto"
        android:id="@+id/patternView"
        android:layout_width="250dp"
        android:layout_height="250dp"
        patternview:pathColor="@color/primary_dark_material_light"
        patternview:circleColor="@color/highlighted_text_material_light"
        patternview:dotColor="@color/highlighted_text_material_light"
        patternview:gridRows="4"
        patternview:gridColumns="4"/>
        
##### Attributes

|     attr    	|  default  	|                         mean                         	|
|:-----------:	|:---------:	|:----------------------------------------------------:	|
|   maxSize   	|     0     	|         Maximum size if WRAP_CONTENT is used.        	|
| circleColor 	| #FFFF0000 	|          Color of the selected cell circle.          	|
| dotColor      | circleColor 	|          Color of the cell dot.                     	|
|   gridRows 	|     3     	|         Rows of the grid. Example 4 for 4xcolums.         	|
|   gridColumns  	|     3     	|         Columns of the grid. Example 4 for rowsx4.         	|
|  pathColor  	| #FFFFFF       | The color of the path that is following the pointer. 	|

##### Limitations

1. Padding for the view does not work.

#### TODO

1. See the padding , and why it is not applied.
2. Make wiki for all the settings.

#### Contributors

I want to update this library and make it better. So any help will be appreciated.
Make and pull - request and we can discuss it.

##### Download

	dependencies {
		compile 'com.eftimoff:android-patternview:1.0.5@aar'
	}

##### Changelog

<b>1.0.5</b>

	[Feature] Not only fixed size now. Can set different column and row size.

<b>1.0.4</b>

	[Feature] Added dotColor to be able to change only the color of the dots.
	
<b>1.0.3</b>

	[Fix] Fix a bug with crashing on orientation change.

<b>1.0.2</b>

	[Feature] Support for API 9

## Licence

    Copyright 2015 Georgi Eftimov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
