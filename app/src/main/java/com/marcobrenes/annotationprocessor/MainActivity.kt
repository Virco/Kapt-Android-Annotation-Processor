package com.marcobrenes.annotationprocessor

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.marcobrenes.api.Kson

@Kson
data class User(val name: String, val email: String)

@Kson
data class Car(val color: String, val doors: Int, val wheels: Int = 4)

@Kson
data class Bike(val speeds: Int = 7, val color: String)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.textview)
        val user = User("marco", "marcovbrenes@gmail.com")
        val car = Car("white", 2)
        val bike = Bike(color = "blue", speeds = 4)

        val output = """
            |As Extension Funtion Calls
            |${user.asJson()}
            |${bike.asJson()}
            |${bike.asJson()}
            |
            |As a new Class with a Companion function
            |${Kson_User.asJson(user)}
            |${Kson_Bike.asJson(bike)}
            |${Kson_Car.asJson(car)}
            """.trimMargin()

        textView.text = output
    }
}
