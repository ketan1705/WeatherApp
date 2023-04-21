package android.com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Telephony.Mms.Addr
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {


    val api: String = "Your API KEY"


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()


//        whetherTask().execute()
    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                //final latitude and longitude code here

                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(applicationContext, "", Toast.LENGTH_SHORT).show()
                    } else {
                        /*val latitude = location.latitude
                        val longitude = location.longitude*/
/*                        val geocoder = Geocoder(applicationContext, Locale.getDefault())
                        val cityName = geocoder.getCityName(latitude, longitude)*/
                        Log.d("latitude", location.latitude.toString())
                        Log.d("longitude:", location.longitude.toString())

                        val weatherTask = WeatherTask()
                        weatherTask.execute(location.latitude.toString(), location.longitude.toString())

                       /* if (cityName != null) {
                            Log.d("cityName", cityName.toString())

                        } else {
                            Toast.makeText(applicationContext, "", Toast.LENGTH_SHORT).show()

                        }*/

                    }
                }


            } else {
                // setting open here
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))


            }
        } else {

            //request permission here
            requestPermission()

        }

    }


    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class WeatherTask {
        fun execute(latitude: String?, longitude: String?) {
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorTxt).visibility = View.GONE
            CoroutineScope(Dispatchers.Main).launch {

                try {
                    val response = withContext(Dispatchers.IO) {
                        URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$api").readText(
                            Charsets.UTF_8
                        )
                    }
                    val jsonObj = JSONObject(response)
                    val main = jsonObj.getJSONObject("main")
                    val sys = jsonObj.getJSONObject("sys")
                    val wind = jsonObj.getJSONObject("wind")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                    val updatedAt: Long = jsonObj.getLong("dt")
                    val updatedAtText = "Updated at: " + SimpleDateFormat(
                        "dd/MM/yyyy hh:mm a", Locale.ENGLISH
                    ).format(Date(updatedAt * 1000))
                    val temp = main.getString("temp") + "°C "
                    val tempMin = "Min Temp: " + main.getString("temp_min") + "°C"
                    val tempMax = "Max Temp: " + main.getString("temp_max") + "°C"
                    val pressure = main.getString("pressure")
                    val humidity = main.getString("humidity")
                    val sunrise: Long = sys.getLong("sunrise")
                    val sunset: Long = sys.getLong("sunset")
                    val windSpeed = wind.getString("speed")
                    val seaLevel = main.getString("sea_level")
                    val weatherDescription = weather.getString("description")
                    val address = jsonObj.getString("name") + ", " + sys.getString("country")

                    findViewById<TextView>(R.id.address).text = address
                    findViewById<TextView>(R.id.updatedAt).text = updatedAtText
                    findViewById<TextView>(R.id.status).text =
                        weatherDescription.replaceFirstChar { it.uppercase() }
                    findViewById<TextView>(R.id.temp).text = temp
                    findViewById<TextView>(R.id.temp_min).text = tempMin
                    findViewById<TextView>(R.id.temp_max).text = tempMax
                    findViewById<TextView>(R.id.sunrise).text =
                        SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
                    findViewById<TextView>(R.id.sunset).text =
                        SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
                    findViewById<TextView>(R.id.wind).text = windSpeed
                    findViewById<TextView>(R.id.humidity).text = humidity
                    findViewById<TextView>(R.id.pressure).text = pressure
                    findViewById<TextView>(R.id.seaLevel).text = seaLevel

                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

                } catch (e: Exception) {
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<TextView>(R.id.errorTxt).visibility = View.VISIBLE
                }
            }
        }
    }

}
/*

private fun Geocoder.getCityName(latitude: Double, longitude: Double):String? {

    val addresses: List<Address> = getFromLocation(latitude, longitude, 1) as List<Address>
    if (addresses.isNotEmpty()) {
        return addresses[0].locality
    }
    return null
}

*/
