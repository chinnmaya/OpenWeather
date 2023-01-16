package com.example.openweather

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {
    const val API_KEY:String="27a06b9b1175eea5580503bbc0edcd18"
    const val BASE_URL:String="http://api.openweathermap.org/data/"
    const val METRIC_UNIT:String="metric"
    const val PREFERENCE_NAME="weatherpreference"
    const val WEATHER_RESPONSE_DATA="weather_response_data"
    fun isNetworkAvaliable(context:Context):Boolean{
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            val network=connectivityManager.activeNetwork?:return false
            val activeNetwork=connectivityManager.getNetworkCapabilities(network)?:return false
            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->return true
                else->return false
            }

        }else{
            val networkInfo=connectivityManager.activeNetworkInfo
            return networkInfo!=null && networkInfo.isConnectedOrConnecting


        }

    }
}