package com.example.campus

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

class Bus(val title: String?, var location: Location?, val congestion: Double) : Parcelable {

    constructor(parcel: Parcel) : this(parcel.readString(), parcel.readParcelable<Location>(Location::class.java.classLoader), parcel.readDouble())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeParcelable(location, flags)
        dest.writeDouble(congestion)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Bus> {
        override fun createFromParcel(parcel: Parcel): Bus{
            return Bus(parcel)
        }

        override fun newArray(size: Int): Array<Bus?> {
            return arrayOfNulls(size)
        }
    }
}