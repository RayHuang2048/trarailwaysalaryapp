package com.ray.trarailwaysalaryapp.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Represents the name of a station in both Chinese and English.
 */
@Parcelize
data class StationName(
    @SerializedName("Zh_tw") val zhTw: String,
    @SerializedName("En") val en: String
) : Parcelable

/**
 * Represents a single train station.
 */
@Parcelize
data class Station(
    @SerializedName("StationID") val stationID: String,
    @SerializedName("StationName") val stationName: StationName,
    @SerializedName("LocationCity") val locationCity: String
) : Parcelable

/**
 * Represents the full API response for the list of all stations.
 */
data class StationResponse(
    @SerializedName("Stations") val stations: List<Station>
)
