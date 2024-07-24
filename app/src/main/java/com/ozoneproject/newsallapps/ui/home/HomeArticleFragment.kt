package com.ozoneproject.newsallapps.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.ads.AdsMediaSource
import androidx.media3.ui.PlayerView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.ozoneproject.newsallapps.R
import com.ozoneproject.newsallapps.databinding.FragmentHomeArticleBinding
import org.json.JSONObject
import org.prebid.mobile.AdSize
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.BannerParameters
import org.prebid.mobile.ExternalUserId
import org.prebid.mobile.InStreamVideoAdUnit
import org.prebid.mobile.InterstitialAdUnit
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.ResultCode
import org.prebid.mobile.Signals
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.Util
import org.prebid.mobile.VideoParameters
import org.prebid.mobile.api.data.AdUnitFormat
import java.util.EnumSet

private const val TAG = "HomeArticleFragment (home)"

class HomeArticleFragment : Fragment(), LocationListener {

    private var _binding: FragmentHomeArticleBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    object adConfig {
        val CONFIG_ID_INSTREAM = "8000000328"
        val CONFIG_ID_328 = "8000000328"
        val CONFIG_ID_002 = "7771070002"
        val WIDTH = 300
        val HEIGHT = 250
        val WIDTH_INSTREAM = 640
        val HEIGHT_INSTREAM = 480
        val HEIGHT_OUTSTREAM = 179
        val AD_UNIT_ID_INSTREAM = "/22037345/ozone-instream-test"
        val SAMPLE_VIDEO_URL = "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"
    }

    private var adUnit: BannerAdUnit? = null
    private var adUnitInstream: InStreamVideoAdUnit? = null
    private var interstitialAdUnit: InterstitialAdUnit? = null;
    var lastLocation: Location? = null
    var locationPermissionGranted: Boolean = false
    private var mInterstitialAd: InterstitialAd? = null // https://developers.google.com/admob/android/interstitial
    private var interstitialShown: Boolean = false
    var instreamAdsUri: Uri? =null
    var instreamPlayer: ExoPlayer? = null
    // The AdsLoader instance exposes the requestAds method.
    private var instreamAdsLoader: ImaAdsLoader? = null

    private var overrideInstlAdConfigId: String = adConfig.CONFIG_ID_002
    private var overrideInstlFormats: Array<String> = arrayOf("banner", "video")

    // For production apps, Android's Exoplayer offers a more fully featured player compared to VideoView.
    private var playerView: PlayerView? = null

    // https://developer.android.com/training/permissions/requesting#kotlin
    // https://developer.android.com/training/basics/intents/result#kotlin
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                Log.d(
                    TAG,
                    "User granted permission for location"
                )
            } else {
                Log.d(
                    TAG,
                    "user denied permission for location"
                )
            }
            locationPermissionGranted = isGranted
            Log.d(
                TAG,
                "Going to restart the ad, with current geo settings"
            )
            createAd()
            createAd2()
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "create view")


        this.interstitialShown = false

        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null

                Log.d(TAG, "Going to load the in-page ad")
                createAd()
                createAd2()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }

        _binding = FragmentHomeArticleBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }


    /**
     * this is called after onViewCreated
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "*** resuming HomeArticle page")

        // look at the arguments passed to us - do we need to show an interstitial?
        val arguments = arguments
        var showInstl = arguments?.getBoolean("show_interstitial", false)
        if(showInstl === null) {
            showInstl = false
        }
        overrideInstlAdConfigId = arguments?.getString("ad_config_id") ?: overrideInstlAdConfigId

        overrideInstlFormats = arguments?.getString("ad_formats")?.split(",")?.toTypedArray() ?: overrideInstlFormats

        Log.d(TAG, "showIntersitial: $showInstl")

        if(PrebidMobile.isSdkInitialized()) {
            // when switching back to this screen
            if(showInstl && !interstitialShown) {
                Log.d(TAG, "onResume: Going to load interstitial")
                showInterstitial()
                interstitialShown = true
            } else {
                Log.d(TAG, "onResume: Going to load ad")
                createAd()
                createAd2()
            }
        } else {
            Log.d(TAG, "onResume: Not going to load ad")
        }
    }

    private fun showInterstitial() {

        Log.d(TAG, "showInterstitial starting...")
        // https://docs.prebid.org/prebid-mobile/modules/rendering/android-sdk-integration-gam.html#interstitial-api <-- note that this is for a newer version
        val interstitialBannerParams = BannerParameters()
        val interstitialVideoParams = VideoParameters(listOf("video/mp4"))

        // test using a different placementId for interstitial
        Log.d(TAG,"instl adunit config ID : $overrideInstlAdConfigId")

        var interstitialAdUnit: InterstitialAdUnit? = null

        var interstitialAdTypes: EnumSet<AdUnitFormat> = EnumSet.noneOf(AdUnitFormat::class.java)
        if(overrideInstlFormats.contains("banner")) {
            interstitialAdTypes.add(AdUnitFormat.BANNER)
        }
        if(overrideInstlFormats.contains("video")) {
            interstitialAdTypes.add(AdUnitFormat.VIDEO)
        }

        // 2. Configure banner parameters
        /**
         * If you want to request Banner interstitial you can use the first constructor or the second.
         */
        if(overrideInstlFormats.contains("banner")) {
//            interstitialAdUnit = InterstitialAdUnit(overrideInstlAdConfigId, 80, 80) // you could use this for banner interstitial if you want, but the other constructor is compatible with both banner & video types.
            interstitialAdUnit = InterstitialAdUnit(overrideInstlAdConfigId, interstitialAdTypes )
            interstitialAdUnit.setMinSizePercentage(80, 80)

            Log.d(TAG,"Setting banner params for interstitial")
            interstitialBannerParams.api = listOf(Signals.Api.MRAID_1, Signals.Api.MRAID_2, Signals.Api.MRAID_3, Signals.Api.OMID_1 )
            interstitialAdUnit.bannerParameters = interstitialBannerParams
        }

        /**
         * NOTE !!!!! If you want to request Video interstitial you have to use the second constructor
         */
        if(overrideInstlFormats.contains("video")) {
            interstitialAdUnit = InterstitialAdUnit(overrideInstlAdConfigId, interstitialAdTypes )
            interstitialAdUnit.setMinSizePercentage(80, 80)
            Log.d(TAG,"Setting video params for interstitial")
            interstitialVideoParams.protocols = listOf(Signals.Protocols.VAST_2_0)
            interstitialVideoParams.playbackMethod = listOf(Signals.PlaybackMethod.AutoPlaySoundOff)
            interstitialVideoParams.api = listOf(Signals.Api.MRAID_1, Signals.Api.MRAID_2, Signals.Api.MRAID_3, Signals.Api.OMID_1 )
            interstitialAdUnit.videoParameters = interstitialVideoParams
        }

        interstitialAdUnit?.ozoneSetImpAdUnitCode("mpu")
        Log.d(TAG, "Interstitial - Calling ozoneSetCustomDataTargeting")

        interstitialAdUnit?.ozoneSetCustomDataTargeting(JSONObject("""{"keywords": [
                  "interstitial",
                  "news",
                  "politics",
                  "borris johnson",
                  "rishi sunack"
                ],
                "oztestmode": "ios_test",
                "pos": "mpu",
                "section": "news"}"""))


        // make changes for Ozone
        Log.d(TAG, "Setting Ozone vars for interstitial")

        TargetingParams.setAppPageName("https://www.ardm.io/sport")

        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            TargetingParams.setUserLatLng(lastLocation?.latitude?.toFloat(), lastLocation?.longitude?.toFloat())
        }

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        interstitialAdUnit?.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set the targeting keys
            Log.d(TAG, "fetchDemand callback for instl. request targeting is: " + request.customTargeting.toString())
//            binding.interstitialAdView.setAdSize( AdSize(400, 800))
//            binding.interstitialAdView.adUnitId = "/22037345/inapp-test-adunit"
//            binding.interstitialAdView.loadAd(request)

            // https://developers.google.com/admob/android/interstitial
            // how to send a nullable var to a function that expects a non-nullable parameter
            activity?.applicationContext?.let {
                Log.d(TAG, "Going to load interstitial ad...")
                InterstitialAd.load(it, "/22037345/inapp-test-adunit", request, object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, "Failed to load ad: " + adError.toString())
                        mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mInterstitialAd = interstitialAd

                        // If you are prefetching then you would do this at an appropriate time later...
                        displayInterstitial()
                    }
                })
            }
        }
    }

    /**
     * After the interstitial has been fetched, now display it:
     */
    private fun displayInterstitial() {
        activity?.let {
            if (mInterstitialAd != null) {
                Log.d("TAG", "Showing the interstitial")
                mInterstitialAd?.show(it)
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "*** onViewCreated 1")

        getLocation()
//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }

        // set test devices like this
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(listOf("ABCDEF012345")).build()
        )

        // make changes for Ozone
        Log.d(TAG, "Setting Ozone vars")

        TargetingParams.setAppPageName("https://www.ardm.io")
        TargetingParams.setSubjectToCOPPA(false) // false by default
        // do not call the adserver, without prebid, eg:
        // val request = AdManagerAdRequest.Builder().build()
        //  binding.adView.loadAd(request)
        // you will do this in the prebid fetchDemand call back function

        // for instream:

        binding.playButtonFrag4.setOnClickListener {
            createInstreamAd()
        }
    }

    // code snippets from prebid example https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html
    private fun createAd() {

        // 1. Create BannerAdUnit
        adUnit = BannerAdUnit(adConfig.CONFIG_ID_328, adConfig.WIDTH, adConfig.HEIGHT_OUTSTREAM)

        // Do this IF you want to auto refresh the ad. This makes a call to the Ozone prebid adserver each time.
        // Note you will need to call adUnit?.stopAutoRefresh() at appropriate times to stop it, eg. if going to another page.
        //  adUnit?.setAutoRefreshInterval(30)

        // 2. Configure banner parameters
        val parameters = BannerParameters()
        parameters.api = listOf(
            Signals.Api.MRAID_1,
            Signals.Api.MRAID_2,
            Signals.Api.MRAID_3,
            Signals.Api.OMID_1
        )
        adUnit?.bannerParameters = parameters
        adUnit?.ozoneSetImpAdUnitCode("mpu")
        adUnit?.ozoneSetCustomDataTargeting(JSONObject("""{"testKey": "testVal1"}"""))

        // Prebid docs: https://docs.prebid.org/prebid-mobile/prebid-mobile-privacy-regulation.html
        // these 2 should be set automatically in default shared preferences but Usercentrics seems not to set SubjectToGDPR
        TargetingParams.setSubjectToGDPR(true)
//    TargetingParams.setGDPRConsentString("...") // see https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#how-do-third-party-sdks-vendors-access-the-consent-information-in-app
//        TargetingParams.setPublisherName() // do not set this - it's not useful
        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            Log.d(TAG, "setting location info in the auction call")
            TargetingParams.setUserLatLng(
                lastLocation?.latitude?.toFloat(),
                lastLocation?.longitude?.toFloat()
            )
        } else {
            Log.d(TAG,"NOT going to set location info in the auction call")
        }

        TargetingParams.storeExternalUserId(
            ExternalUserId(
                "criteo.com",
                "f_oxPV9oSGRLb3FFJTJGbGdQOHZkWlNCZlV6WDh0T0R2YkZTalJ4NTd6U21LOE5sbTdOcXlJdDBlM3F0eVVRdk9HQ2xQdGlzSkZsWkxTQUttWGtPT3MxVlBLb3N6dmw3Wm8zbEF5WTglMkZBeU1UMjVxc3AzR2JWUkkyRklqQnppazlNeHpwMFVPcVlPZGIwYlEzbmpsem5pTFltNU9BJTNEJTNE",
                1,
                null
            )
        )

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        adUnit?.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set targeting keys on the request object, ready to send to the adserver.
            Log.d(
                TAG,
                "fetchDemand callback. request targeting is: " + request.customTargeting.toString()
            )
            binding.adView1.loadAd(request)
        }
    }

    /**
     * Create the 2nd banner ad on the page
     */
    private fun createAd2() {

        Log.d(TAG, "Creating adView2")
        // 1. Create MPU scratch BannerAdUnit
        var adUnit2 = BannerAdUnit(adConfig.CONFIG_ID_328, adConfig.WIDTH, adConfig.HEIGHT)
        val parameters = BannerParameters()
        parameters.api = listOf(
            Signals.Api.MRAID_1,
            Signals.Api.MRAID_2,
            Signals.Api.MRAID_3,
            Signals.Api.OMID_1
        )
        adUnit2.bannerParameters = parameters

        adUnit2.ozoneSetImpAdUnitCode("mpu-3")
        adUnit2.ozoneSetCustomDataTargeting(JSONObject("""{"section": "news",
            "pos":"mpu-3",
            "keywords": [
                "page2_banner", "brighton beach", "sea", "storms"
            ],
            "oztestmode": "ios_test"}"""))
        TargetingParams.setSubjectToGDPR(true)
        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            Log.d(TAG, "setting location info in the auction call")
            TargetingParams.setUserLatLng(
                lastLocation?.latitude?.toFloat(),
                lastLocation?.longitude?.toFloat()
            )
        } else {
            Log.d(TAG,"NOT going to set location info in the auction call")
        }

        TargetingParams.storeExternalUserId(
            ExternalUserId(
                "criteo.com",
                "f_oxPV9oSGRLb3FFJTJGbGdQOHZkWlNCZlV6WDh0T0R2YkZTalJ4NTd6U21LOE5sbTdOcXlJdDBlM3F0eVVRdk9HQ2xQdGlzSkZsWkxTQUttWGtPT3MxVlBLb3N6dmw3Wm8zbEF5WTglMkZBeU1UMjVxc3AzR2JWUkkyRklqQnppazlNeHpwMFVPcVlPZGIwYlEzbmpsem5pTFltNU9BJTNEJTNE",
                1,
                null
            )
        )

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        adUnit2.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set targeting keys on the request object, ready to send to the adserver.
            Log.d(
                TAG,
                "fetchDemand callback. request targeting is: " + request.customTargeting.toString()
            )
            binding.adView2.loadAd(request)
        }
    }

    private fun createInstreamAd() {
        Log.d(TAG, "*** createInstreamAd")
        // check whether the user allows geo location
        getLocation()

        // 1. Create VideoAdUnit
        adUnitInstream = InStreamVideoAdUnit(adConfig.CONFIG_ID_INSTREAM, adConfig.WIDTH_INSTREAM, adConfig.HEIGHT_INSTREAM)

        // 2. configure video parameters:
        var videoParams: VideoParameters = configureVideoParameters()
        // now add the ozone-specific params
        val jsonExt = JSONObject(
            """{
            "context": "instream",
            "playerSize": [[640,480]],
            "format": [{"w": 640, "h":480}]
            }""".trimMargin()
        );
        videoParams.ozoneSetExt(jsonExt)
        adUnitInstream?.videoParameters = videoParams
        adUnitInstream?.ozoneSetImpAdUnitCode("video-ad") // this may not be needed in app context
        val jsonObj = JSONObject(
            """{
            "section": "sport",
            "pos":"video-ad",
            "keywords": [
                "boxing", "tyson fury", "anthony joshua", "eddie hearn"
            ],
            "oztestmode": "ios_test"
            }
        }""".trimIndent()
        )

        adUnitInstream?.ozoneSetCustomDataTargeting(jsonObj)
//        TargetingParams.setPlacementId("8000000328") // 20230124 - do not do this; we use the placementId from the adunit configId and this should no longer be available in the library (find a newer one if this method is still available in your library)

        TargetingParams.setAppPageName("https://www.ardm.io/other_page")
        TargetingParams.setSubjectToCOPPA(false) // false by default

        // 3. init player view
        playerView = PlayerView(requireContext())
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
        binding.instreamViewContainer.addView(playerView, params)

        // 4. make a bid request to prebid server
        adUnitInstream?.fetchDemand { _: ResultCode?, keysMap: Map<String?, String?>? ->

            Log.d(TAG, "fetchDemand got keys: " + keysMap.toString())
            // 5. Prepare the creative URI
            val sizes = HashSet<AdSize>()
            sizes.add(AdSize(adConfig.WIDTH_INSTREAM, adConfig.HEIGHT_INSTREAM))
//            sizes.add(AdSize(400, 300)) // this is how to add alternative sizes
            val prebidURL = Util.generateInstreamUriForGam(
                adConfig.AD_UNIT_ID_INSTREAM, sizes, keysMap
            )
            instreamAdsUri = Uri.parse(prebidURL)

            // 6. Init the player
            initializePlayer()
        }
    }


    // https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html#instream-video-api
    private fun configureVideoParameters(): VideoParameters {
        return VideoParameters(listOf("video/x-flv", "video/mp4")).apply {
            placement = Signals.Placement.InStream

            api = listOf(
//                Signals.Api.VPAID_1,
//                Signals.Api.VPAID_2
                // Ozone want 3,5,6,7 to be sent (6 & 7 will be for video but you may be able to send all 4 always - check first)
                Signals.Api.MRAID_1,
                Signals.Api.MRAID_2,
                Signals.Api.MRAID_3,
                Signals.Api.OMID_1
            )

            maxBitrate = 1500
            minBitrate = 300
            maxDuration = 30
            minDuration = 5
            playbackMethod = listOf(Signals.PlaybackMethod.AutoPlaySoundOn)
            protocols = listOf(
                Signals.Protocols.VAST_2_0
            )
        }
    }


    @OptIn(UnstableApi::class)
    private fun initializePlayer() {

        Log.d(TAG, "instream: initializePlayer")

        instreamAdsLoader = ImaAdsLoader.Builder(requireContext()).build()

        val playerBuilder = ExoPlayer.Builder(requireContext())
        instreamPlayer = playerBuilder.build()
        playerView!!.player = instreamPlayer
        instreamAdsLoader!!.setPlayer(instreamPlayer)

        val uri = Uri.parse(adConfig.SAMPLE_VIDEO_URL)

        /*
        https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide#exoplayer
        After migrating from ExoPlayer v2 to Media3, you may see a lot of unstable API lint errors. This may make it seem like Media3 is 'less stable' than ExoPlayer v2. This is not the case. The 'unstable' parts of the Media3 API have the same level of stability as the whole of the ExoPlayer v2 API surface, and the guarantees of the stable Media3 API surface are not available in ExoPlayer v2 at all. The difference is simply that a lint error now alerts you to the different levels of stability.
         */

        val mediaItem = MediaItem.fromUri(uri)
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(requireContext(), getString(R.string.app_name))
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val mediaSource: MediaSource = mediaSourceFactory.createMediaSource(mediaItem)
        val dataSpec = DataSpec(instreamAdsUri!!)
        val adsMediaSource = AdsMediaSource(
            mediaSource, dataSpec, "ad", mediaSourceFactory,
            instreamAdsLoader!!, playerView!!
        )
        instreamPlayer?.setMediaSource(adsMediaSource)
        instreamPlayer?.playWhenReady = true
        instreamPlayer?.prepare()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getLocationTrackingOK(): Boolean {
        Log.d(
            TAG,
            "getLocationTrackingOK : $locationPermissionGranted"
        )
        return locationPermissionGranted
    }


    /**
     * See if we can get the users location, and if so store it in lastLocation
     */
    private fun getLocation() {
        if (activity?.applicationContext?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED && activity?.applicationContext?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(
                TAG,
                "user has not granted location permission"
            )
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            locationPermissionGranted = false // currently the permission is false
        } else {
            Log.d(
                TAG,
                "user has granted location permission"
            )
            val mgr = activity?.applicationContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lastLocation = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            locationPermissionGranted = true
        }
    }

    /**
     * interface function for LocationListener
     */
    override fun onLocationChanged(location: Location) {
        Log.d(
            TAG,
            "onLocationChanged with location $location"
        )
        Log.d(
            TAG, String.format(
                "Lat:\t %f\nLong:\t %f\nAlt:\t %f\nBearing:\t %f", location.latitude,
                location.longitude, location.altitude, location.bearing
            )
        )
        lastLocation = location
    }


}