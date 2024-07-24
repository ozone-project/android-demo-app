package com.ozoneproject.newsallapps.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ozoneproject.newsallapps.databinding.FragmentHomeBinding
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.ozoneproject.newsallapps.R
import org.json.JSONObject
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.BannerParameters
import org.prebid.mobile.ExternalUserId
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.Signals
import org.prebid.mobile.TargetingParams


private const val TAG = "HomeFragment (home)"


class HomeFragment : Fragment(), LocationListener {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    object adConfig {
        val CONFIG_ID_328 = "8000000328"
        val CONFIG_ID_002 = "7771070002"
        val WIDTH = 300
        val HEIGHT = 250
        val HEIGHT_STICKY = 50
        val WIDTH_STICKY = 320
    }
    private var adUnit: BannerAdUnit? = null
    var lastLocation: Location? = null
    var locationPermissionGranted: Boolean = false

    // https://developer.android.com/training/permissions/requesting#kotlin
    // https://developer.android.com/training/basics/intents/result#kotlin
    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                Log.d(TAG, "User granted permission for location")
            } else {
                Log.d(TAG, "user denied permission for location")
            }
            locationPermissionGranted = isGranted
            Log.d(TAG, "Going to restart the ad, with current geo settings")
            createAd()
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Home : create view")
//        val homeViewModel =
//            ViewModelProvider(this).get(HomeViewModel::class.java)

        Log.d(TAG, "PrebidMobile SDK version is : ${PrebidMobile.SDK_VERSION}")


        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // from the initial default code - we're not using this
//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        // set the image & text for first element
        val imgView1: ImageView = binding.element1.singleElementImage
        imgView1.setImageResource( R.drawable.thyme )
        val textView1: TextView = binding.element1.singleElementText
        textView1.setText("This button will load the article page without an interstitial. There will be an outstream video ad on the page.")

        val imgView2: ImageView = binding.element2.singleElementImage
        imgView2.setImageResource( R.drawable.thyme2 )
        val textView2: TextView = binding.element2.singleElementText
        textView2.setText("This button will load the article page via an interstitial which will be a scratch ad (banner type). Note that in this version of the app we are requesting the interstitial just-in-time, rather than pre-loading it. ")

        binding.element1.readMoreButton.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "element1 readmore button clicked")
            val navController = findNavController()
            Log.d(TAG, "Found nav controller")
            navController.navigate(R.id.action_nav_home_to_homeArticleFragment, bundleOf(Pair("show_interstitial", false)))
        })
        binding.element2.readMoreButton.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "element2 readmore button clicked")
            val navController = findNavController()
            Log.d(TAG, "Found nav controller")
            navController.navigate(R.id.action_nav_home_to_homeArticleFragment, bundleOf(Pair("show_interstitial", true)))
        })


        binding.btnInstlScratch.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "Scratch button clicked")
            val navController = findNavController()
            Log.d(TAG, "Found nav controller")
            navController.navigate(R.id.action_nav_home_to_homeArticleFragment, bundleOf(Pair("show_interstitial", true), Pair("ad_config_id", "8000000328"), Pair("ad_formats", "banner"))) // comma separated list like "banner,video" @todo make this an array
        })
        binding.btnInstlVideo.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "Video button clicked")
            val navController = findNavController()
            Log.d(TAG, "Found nav controller")
            // @TODO - NOTE THAT in the 2.1.1 library it was only possible to call banner interstitials, not video interstitials
            navController.navigate(R.id.action_nav_home_to_homeArticleFragment, bundleOf(Pair("show_interstitial", true), Pair("ad_config_id", "1500000324"), Pair("ad_formats", "video")))
        })
        binding.btnInstlIcal.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "iCal button clicked")
            val navController = findNavController()
            Log.d(TAG, "Found nav controller")
            navController.navigate(R.id.action_nav_home_to_homeArticleFragment, bundleOf(Pair("show_interstitial", true), Pair("ad_config_id", "1500000324"), Pair("ad_formats", "banner")))
        })


        return root
    }


    /**
     * this is called after onViewCreated
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "*** resuming Home page")
        if(PrebidMobile.isSdkInitialized()) {
            // when switching back to this screen
            Log.d(TAG, "onResume: Going to load ad")
            createAd()
            createStickyBannerAd()
        } else {
            Log.d(TAG, "onResume: Not going to load ad")
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

    }


    // code snippets from prebid example https://docs.prebid.org/prebid-mobile/pbm-api/android/android-sdk-integration-gam-original-api.html
    private fun createAd() {
        // 1. Create BannerAdUnit
        adUnit = BannerAdUnit(adConfig.CONFIG_ID_002, adConfig.WIDTH, adConfig.HEIGHT)

        // Do this IF you want to auto refresh the ad. This makes a call to the Ozone prebid adserver each time.
        // Note you will need to call adUnit?.stopAutoRefresh() at appropriate times to stop it, eg. if going to another page.

        //  adUnit?.setAutoRefreshInterval(30)

        // 2. Configure banner parameters
        val parameters = BannerParameters()
        parameters.api = listOf(Signals.Api.MRAID_1, Signals.Api.MRAID_2, Signals.Api.MRAID_3, Signals.Api.OMID_1)
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
            TargetingParams.setUserLatLng(lastLocation?.latitude?.toFloat(), lastLocation?.longitude?.toFloat())
        } else {
            Log.d(TAG, "NOT going to set location info in the auction call")
        }

        TargetingParams.storeExternalUserId(ExternalUserId("criteo.com", "f_oxPV9oSGRLb3FFJTJGbGdQOHZkWlNCZlV6WDh0T0R2YkZTalJ4NTd6U21LOE5sbTdOcXlJdDBlM3F0eVVRdk9HQ2xQdGlzSkZsWkxTQUttWGtPT3MxVlBLb3N6dmw3Wm8zbEF5WTglMkZBeU1UMjVxc3AzR2JWUkkyRklqQnppazlNeHpwMFVPcVlPZGIwYlEzbmpsem5pTFltNU9BJTNEJTNE", 1, null ))

        // 4. Make a bid request to Prebid Server
        val request = AdManagerAdRequest.Builder().build()
        adUnit?.fetchDemand(request) {
            // inside the callback we will call for an ad. Prebid will have set targeting keys on the request object, ready to send to the adserver.
            Log.d(TAG, "fetchDemand callback. request targeting is: " + request.customTargeting.toString())
            binding.adView1.loadAd(request)
        }
    }

    private fun createStickyBannerAd() {
        val adUnitSticky = BannerAdUnit(adConfig.CONFIG_ID_328, adConfig.WIDTH_STICKY, adConfig.HEIGHT_STICKY)
        val parameters = BannerParameters()
        parameters.api = listOf(Signals.Api.MRAID_1, Signals.Api.MRAID_2, Signals.Api.MRAID_3, Signals.Api.OMID_1)
        adUnitSticky.bannerParameters = parameters
        adUnitSticky.ozoneSetImpAdUnitCode("mpu")
        adUnitSticky.ozoneSetCustomDataTargeting(JSONObject("""{"testKey": "testVal1"}"""))
        TargetingParams.setSubjectToGDPR(true)
        PrebidMobile.setShareGeoLocation(getLocationTrackingOK())
        if(getLocationTrackingOK()) {
            TargetingParams.setUserLatLng(lastLocation?.latitude?.toFloat(), lastLocation?.longitude?.toFloat())
        }
        TargetingParams.storeExternalUserId(ExternalUserId("criteo.com", "f_oxPV9oSGRLb3FFJTJGbGdQOHZkWlNCZlV6WDh0T0R2YkZTalJ4NTd6U21LOE5sbTdOcXlJdDBlM3F0eVVRdk9HQ2xQdGlzSkZsWkxTQUttWGtPT3MxVlBLb3N6dmw3Wm8zbEF5WTglMkZBeU1UMjVxc3AzR2JWUkkyRklqQnppazlNeHpwMFVPcVlPZGIwYlEzbmpsem5pTFltNU9BJTNEJTNE", 1, null ))
        val request = AdManagerAdRequest.Builder().build()
        adUnitSticky.fetchDemand(request) {
            Log.d(TAG, "fetchDemand callback for sticky. request targeting is: " + request.customTargeting.toString())
            binding.adViewStickyFooter.loadAd(request)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getLocationTrackingOK(): Boolean {
        Log.d(TAG, "getLocationTrackingOK : $locationPermissionGranted")
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
            Log.d(TAG, "user has not granted location permission")
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            locationPermissionGranted = false // currently the permission is false
        } else {
            Log.d(TAG, "user has granted location permission")
            val mgr = activity?.applicationContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lastLocation = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            locationPermissionGranted = true
        }
    }

    /**
     * interface function for LocationListener
     */
    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged with location $location")
        Log.d(
            TAG, String.format(
                "Lat:\t %f\nLong:\t %f\nAlt:\t %f\nBearing:\t %f", location.latitude,
                location.longitude, location.altitude, location.bearing
            ))
        lastLocation = location
    }


}