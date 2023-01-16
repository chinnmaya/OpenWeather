package com.example.openweather.models

import com.weatherapp.models.Main
import com.weatherapp.models.Sys
import com.weatherapp.models.Weather
import com.weatherapp.models.Wind

data class WeatherResponse(
    val coord:Coord,
    val weather:ArrayList<Weather>,
    val base:String,
    val main: Main,
    val visibility:Int,
    val wind: Wind,
    val cloud:Cloud,
    val dt:Int,
    val sys: Sys,
    val id:Int,
    val name:String,
    val cod:Int,
):java.io.Serializable
