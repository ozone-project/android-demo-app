package com.ozoneproject.newsallapps

import android.os.Bundle
import android.util.Log
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import com.ozoneproject.newsallapps.databinding.ActivityMainBinding
import com.usercentrics.sdk.Usercentrics
import com.usercentrics.sdk.UsercentricsBanner
import com.usercentrics.sdk.UsercentricsOptions
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.api.data.InitializationStatus

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setting up ads & cmp

        val options = UsercentricsOptions(settingsId = "x1Y_PNLY58mmMO") // TCF
        Usercentrics.initialize(this, options)

        // "On Android OS, the TC data and TC string shall be stored in the default Shared Preferences for the application context. This can be accessed using the getDefaultSharedPreferences method from the android.preference.PreferenceManager class using the application context."
        Usercentrics.isReady({ status ->
            if (status.shouldCollectConsent) {
                collectConsent()
            } else {
                // Apply consent with status.consents
                Log.d(TAG, "Apply consent with status.consents")
                Log.d(TAG, status.consents.toString())
            }
        }, { error ->
            Log.e(TAG, "Handle non-localized error")
            Log.e(TAG, error.toString())
        })


        //
        // this demo assumes you will assure that PrebidMobile will complete its initialization before you use it.
        //

        // from prebid code
        // get the application context form the main activity https://stackoverflow.com/questions/12659747/call-an-activity-method-from-a-fragment
        PrebidMobile.initializeSdk(applicationContext) { status ->
            if (status == InitializationStatus.SUCCEEDED) {
                Log.d(TAG, "initializeSdk: SDK initialized successfully!")
            } else if (status == InitializationStatus.SERVER_STATUS_WARNING) {
                Log.d(TAG, "initializeSdk: Prebid server status check failed: $status\n${status.description}")
            } else {
                Log.e(TAG, "initializeSdk: SDL initialization error : $status\n${status.description}")
            }
        }


        Log.d(TAG, "setting global prebid values")
        PrebidMobile.setPrebidServerHost(Host.createCustomHost("https://elb.the-ozone-project.com/openrtb2/app"))
        PrebidMobile.setCustomStatusEndpoint("https://elb.the-ozone-project.com/status")

        Log.d(TAG, "PrebidMobile SDK version is : ${PrebidMobile.SDK_VERSION}")


        PrebidMobile.setPrebidServerAccountId("OZONETEST001")
        TargetingParams.setDomain("ardm.io")
        TargetingParams.setStoreUrl("google play store url here")
        TargetingParams.setBundleName("this is the bundleName")

        // OMSDK settings, optional - see https://docs.prebid.org/prebid-mobile/pbm-api/android/pbm-targeting-params-android.html
        TargetingParams.setUserAge(99)
        TargetingParams.setGender(TargetingParams.GENDER.FEMALE)

        TargetingParams.setOmidPartnerName("Google1")
        TargetingParams.setOmidPartnerVersion("3.16.3")

        MobileAds.initialize(this) {}

        // now back to the app stuff

        setSupportActionBar(binding.appBarMain.toolbar)

            // from the initial boilerplate app code - we're not using the little 'mail' icon, bottom right
//        binding.appBarMain.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null)
//                .setAnchorView(R.id.fab).show()
//        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun collectConsent() {
        val banner = UsercentricsBanner(this)
        banner.showFirstLayer { userResponse ->
            Log.d(TAG, "cmp consent user response:")
            Log.d(TAG, userResponse.toString())
            // this should be automatically in default shared preferences
            // Usercentrics seems not to put gdprApplies in there tho (IABTCF_gdprApplies), just the consent string (IABTCF_TCString)
            Usercentrics.instance.getTCFData { tcfData ->
                val tcString = tcfData.tcString
                Log.d(TAG, "cmp consent string is $tcString")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}