# Ozone Android demo app 2024

This Ozone header bidding SDK is based on the Prebid Mobile SDK; you can use all documentation found there to understand how it works. Ozone's header bidding server
has certain custom requirements which is the reason for using this library.

This demo app shows how to use the Ozone Android library to request bids, and then the Google ad manager library to receive and display ads. Specifically it contains the following:

- 300x250 banner ad
- 300x250 outstream video ad
- 300x250 instream video ad
- 320x50 sticky footer ad
- full screen interstitial banner ad
- full screen interstitial video ad

You can use the code as an example of how to integrate the libraries into your own app but note that you MUST NOT copy & paste the config values found here into any production code. 
Every publisher, app and website will have its own requirements for config values sent to the Ozone server.
You must get the correct ID's for your app, and its various individual ads (every single ad *might* have its own values), from Ozone and your app MUST be tested by Ozone for correctness before release.


## It also integrates with 

- CMP
- Location/tracking request dialog


This app includes the jar files available from the Ozone repo: `https://github.com/ozone-project/inapp-android-sdk-jar-files/tree/main` . Be sure to always pull the latest code, which will always be in the `main` branch.


# How to use this app

1. Pull the code down
2. open up in android studio
3. adjust build settings/java versions etc until it builds 
4. There are only 2 pages: the Home page, then an Article page which is the destination for all the clicks
5. If you want, you can copy the jars out of the app/libs directory and use them in your own app. Check there have been no bug fixes or updates since this version.
6. To check the current version call `PrebidMobile.SDK_VERSION`  