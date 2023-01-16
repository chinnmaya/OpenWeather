package com.example.openweather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.openweather.databinding.ActivityMainBinding
import com.example.openweather.models.WeatherResponse
import com.example.openweather.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    var bindings:ActivityMainBinding?=null
    private lateinit var mSharedPrefernces:SharedPreferences

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialogh:Dialog?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings=ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings?.root)
        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        mSharedPrefernces=getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setUpUi()
        }
        if(!isLocationEnabled()){
            Toast.makeText(this,"Please trun on your GPS",Toast.LENGTH_SHORT).show()
            val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,

                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        // Here after all the permission are granted launch the CAMERA to capture an image.
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()

                        }
                        if(report!!.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity,"Location permission denied",Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()

        }
    }
    private fun getLocationWeatherDetails(latitude:Double,longitude:Double){
        if(Constants.isNetworkAvaliable(this)){
           Log.i("ethrenet","${Constants.isNetworkAvaliable(this)}")
            Toast.makeText(this,"You connected to internet",Toast.LENGTH_LONG).show()
            val retrofit:Retrofit=Retrofit.Builder().baseUrl(Constants.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
            val service:WeatherService=retrofit.create<WeatherService>(WeatherService::class.java)
            val listCall:Call<WeatherResponse> = service.getWeather(
                latitude,longitude,Constants.METRIC_UNIT,Constants.API_KEY
            )
            showProgressDialogh()
            listCall.enqueue(object :Callback<WeatherResponse>{

                @RequiresApi(Build.VERSION_CODES.N)
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if(response!!.isSuccess) {
                        hideProgressBar()
                        val weatherList: WeatherResponse = response.body()
                        //Toast.makeText(this@MainActivity,"$weatherList",Toast.LENGTH_LONG).show()
                        val weatherResponseJsonString=Gson().toJson(weatherList)
                        val editor=mSharedPrefernces.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        editor.apply()
                        setUpUi()
                        Log.i("Result","$weatherList")

                    }else{
                        val rc=response.code()
                        when(rc){
                            400 ->{
                                Log.e("Error 400","Bad connection")
                            }
                            404 ->{
                                Log.e("Error 404","Not founf")
                            }else ->{
                                Log.e("Error","Generic Error")
                            }

                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    hideProgressBar()
                    Log.e("Erooor","${t!!.message.toString()}")
                }

            })



        }else{
            Toast.makeText(this,"No internet connection",Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest= com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority=com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper())



    }
    private val mLocationCallback=object :LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            val mLastLocation:Location=locationResult!!.lastLocation
            val mlattitude=mLastLocation.latitude
            Log.i("lattitude=","$mlattitude")
            print(mlattitude)
            val mlongitude=mLastLocation.longitude
            Log.i("longitude=","$mlongitude")
            getLocationWeatherDetails(mlattitude,mlongitude)
        }
    }
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }
    private fun isLocationEnabled():Boolean{
        val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }
    private fun showProgressDialogh(){
        mProgressDialogh=Dialog(this)
        mProgressDialogh!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialogh!!.show()
    }
    private fun hideProgressBar(){
        if(mProgressDialogh!=null){
            mProgressDialogh!!.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setUpUi(){
        val weatherResponseJsonString=mSharedPrefernces.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if(!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList=Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)//converting String to json object
            for(i in weatherList.weather.indices){

                bindings?.tvMain?.text=weatherList.weather[i].main
                bindings?.tvMainDescription?.text=weatherList.weather[i].description
                bindings?.tvTemp?.text=weatherList.main.temp.toString()+getUnit(application.resources.configuration.locales.toString())

                bindings?.tvSunriseTime?.text=unixTime(weatherList.sys.sunrise)

                bindings?.tvSunsetTime?.text=unixTime(weatherList.sys.sunset)
                bindings?.tvHumidity?.text=weatherList.main.humidity.toString()+" percent"
                bindings?.tvMin?.text=weatherList.main.temp_min.toString()+" Min"
                bindings?.tvMax?.text=weatherList.main.temp_max.toString()+" Max"
                bindings?.tvSpeed?.text=weatherList.wind.speed.toString()
                bindings?.tvName?.text=weatherList.name
                bindings?.tvCountry?.text=weatherList.sys.country
                when(weatherList.weather[i].icon){
                    "01d" -> bindings?.ivMain?.setImageResource(R.drawable.sunny)
                    "02d" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "03d" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "04d" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "04n" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "10d" -> bindings?.ivMain?.setImageResource(R.drawable.rain)
                    "11d" -> bindings?.ivMain?.setImageResource(R.drawable.storm)
                    "13d" -> bindings?.ivMain?.setImageResource(R.drawable.snowflake)
                    "01n" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "02n" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "03n" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "10n" -> bindings?.ivMain?.setImageResource(R.drawable.cloud)
                    "11n" -> bindings?.ivMain?.setImageResource(R.drawable.rain)
                    "13n" -> bindings?.ivMain?.setImageResource(R.drawable.snowflake)


                }




            }
        }


    }
    private fun getUnit(value:String):String?{
        var valuee=" C"
        if("US"==value||"LR"==valuee||"MM"==valuee){
            valuee=" F"
        }
        return valuee
    }
    private fun unixTime(timex:Long):String?{
        val date= Date(timex*1000L )
        val sdf=SimpleDateFormat("HH:mm:ss")
        sdf.timeZone= TimeZone.getDefault()
        return sdf.format(date)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh->{
                Toast.makeText(this,"Refreshed",Toast.LENGTH_SHORT).show()
                requestLocationData()
                true
            }else->{
                super.onOptionsItemSelected(item)
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if(bindings==null){
            finish()
        }
    }

}