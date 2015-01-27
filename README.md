## android-pathview

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-android--pathview-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/1421) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.eftimoff/android-pathview/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.eftimoff/android-pathview)

You want to animate svg or normal Paths?<br\>
Change the color, pathWidth or add svg.<br\>
Animate the "procentage" property to make the animation.

### There are two types of paths :

#### 1. From Svg

    <com.eftimoff.androipathview.PathView
    	xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/pathView"
        android:layout_width="150dp"
        android:layout_height="150dp"
        app:pathColor="@android:color/white"
        app:svg="@raw/settings"
        app:pathWidth="5"/>
        

Result

![svg](https://github.com/geftimov/android-pathview/blob/master/art/settings.gif) 

#### 2. From Path

    <com.eftimoff.androipathview.PathView
    	xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/pathView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:pathColor="@android:color/white"
        app:pathWidth="3"/>
        

In Code    
    
```java
    final Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.lineTo(length / 4f, 0.0f);
        path.lineTo(length, height / 2.0f);
        path.lineTo(length / 4f, height);
        path.lineTo(0.0f, height);
	    path.lineTo(length * 3f / 4f, height / 2f);
	    path.lineTo(0.0f, 0.0f);
	    path.close();
	
pathView.setPath(path);
```

Result

![path](https://github.com/geftimov/android-pathview/blob/master/art/path.gif)  

#### Use the animator

    pathView.getPathAnimator().
        delay(100).
        duration(500).
        listenerStart(new AnimationListenerStart()).
        listenerEnd(new AnimationListenerEnd()).
        interpolator(new AccelerateDecelerateInterpolator()).
        start();
        
#### If you want to use the svg colors.

    pathView.useNaturalColors();

##### Limitations

When working with SVGs you can not WRAP_CONTENT your views.

##### Thanks to

* https://github.com/romainguy/road-trip
* http://www.curious-creature.com/2013/12/21/android-recipe-4-path-tracing/
* https://github.com/matthewrkula/AnimatedPathView

##### Contributors

I want to update this library and make it better. So any help will be appreciated.
Make and pull - request and we can discuss it.

##### Download

	dependencies {
		compile 'com.eftimoff:android-pathview:1.0.3@aar'
	}

##### Licence

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




