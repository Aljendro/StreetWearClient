## Street Wear/Analysis Client

This project is a client for the Street Wear Server, which analyzes
image data and classifies the level of damage the image contains.

### Usages

This project is still a work in progress and is not entirely stable.
Ideally, you will have Android Studio installed on your computer. Go ahead
and clone the project:

```
git clone https://github.com/Aljendro/StreetWearClient.git
```

Next, setup your [device](https://developer.android.com/studio/run/device.html) 
or a [virtual device](https://developer.android.com/studio/run/managing-avds.html) and
click the run button in android studio.

#### TODO
* Refactor the permissions workflow to a helper class, so that we can reuse it across
the application
* Get rid of unneeded dependancies and imports
* Test across devices and across versions for stability
* Keep improving the User Interface
* Plot the street analysis, similar to the pothole plots
* Have some refresh functionality to resend any requests
* Eventually use the Camera2 api
* Test the application for performance



