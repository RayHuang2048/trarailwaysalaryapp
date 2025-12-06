package com.ray.trarailwaysalaryapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.ray.trarailwaysalaryapp.data.Station
import com.ray.trarailwaysalaryapp.data.StationResponse
import java.io.File
import java.io.InputStreamReader

class StationSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _stations = MutableLiveData<List<Station>>()
    val stations: LiveData<List<Station>> = _stations

    init {
        loadStations()
    }

    private fun loadStations() {
        val context = getApplication<Application>().applicationContext
        val file = File(context.filesDir, "stations.json")
        if (file.exists()) {
            try {
                val reader = InputStreamReader(file.inputStream())
                val stationResponse = Gson().fromJson(reader, StationResponse::class.java)
                _stations.postValue(stationResponse.stations)
            } catch (e: Exception) {
                // Handle error
            }
        } else {
            // Handle case where file doesn't exist
        }
    }
}