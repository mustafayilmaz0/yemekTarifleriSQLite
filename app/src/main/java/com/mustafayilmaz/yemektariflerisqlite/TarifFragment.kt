package com.mustafayilmaz.yemektariflerisqlite

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_tarif.*
import java.io.ByteArrayOutputStream

class TarifFragment : Fragment() {

    var secilenGorsel: Uri? = null
    var secilenBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tarif, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_save.setOnClickListener {
            save(it)
        }
        img_insert.setOnClickListener {
            gorselSec(it)
        }

        arguments?.let {
            var gelenBilgi = TarifFragmentArgs.fromBundle(it).bilgi

            if (gelenBilgi.equals("menudengeldim")) {
                // yemek eklemeye geldi
                text_food_name.setText("")
                text_food_recipe.setText("")
                btn_save.visibility = View.VISIBLE

                val gorselSecmeArkaPlani =
                    BitmapFactory.decodeResource(context?.resources, R.drawable.img_insert)
                img_insert.setImageBitmap(gorselSecmeArkaPlani)

            } else {
                // daha önce olusturulan yemegı gormeye geldi
                btn_save.visibility = View.INVISIBLE

                val secilenId = TarifFragmentArgs.fromBundle(it).id

                context?.let {
                    try {
                        val db = it.openOrCreateDatabase("Yemekler", Context.MODE_PRIVATE, null)
                        val cursor = db.rawQuery(
                            "SELECT * FROM yemekler WHERE id = ?",
                            arrayOf(secilenId.toString())
                        )

                        val yemekIsmiIndex = cursor.getColumnIndex("yemekismi")
                        val yemekMalzemesiIndex = cursor.getColumnIndex("yemekmalzemesi")
                        val yemekGorseli = cursor.getColumnIndex("gorsel")

                        while (cursor.moveToNext()) {
                            text_food_name.setText(cursor.getString(yemekIsmiIndex))
                            text_food_recipe.setText(cursor.getString(yemekMalzemesiIndex))

                            val byteDizisi = cursor.getBlob(yemekGorseli)
                            val bitmap =
                                BitmapFactory.decodeByteArray(byteDizisi, 0, byteDizisi.size)
                            img_insert.setImageBitmap(bitmap)
                        }
                        cursor.close()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun save(view: View) {
        // SQlite 'a kaydetme
        val yemekIsmi = text_food_name.text.toString()
        val yemekMalzemesi = text_food_recipe.text.toString()

        if (secilenBitmap != null) {
            val kucukBitmap = kucukBitmapOlustur(secilenBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteDizisi = outputStream.toByteArray()

            try {
                context?.let {
                    val database = it.openOrCreateDatabase("Yemekler", Context.MODE_PRIVATE, null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler (id INTEGER PRIMARY KEY, yemekismi VARCHAR, yemekmalzemesi VARCHAR, gorsel BLOB)")

                    val sqlString =
                        "INSERT INTO yemekler(yemekismi, yemekmalzemesi, gorsel) VALUES(?,?,?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, yemekIsmi)
                    statement.bindString(2, yemekMalzemesi)
                    statement.bindBlob(3, byteDizisi)
                    statement.execute()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val action = TarifFragmentDirections.actionTarifFragment2ToListeFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }

    fun gorselSec(view: View) {
        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //izin verilmedi, izin istememiz gerekiyor
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            } else {
                //izin zaten verilmiş tekrar istemeden galeriye git
                val galeriIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent, 2)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // izni aldık
                val galeriIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent, 2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            secilenGorsel = data.data

            try {
                context?.let {
                    if (secilenGorsel != null) {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source =
                                ImageDecoder.createSource(it.contentResolver, secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            img_insert.setImageBitmap(secilenBitmap)
                        } else {
                            secilenBitmap =
                                MediaStore.Images.Media.getBitmap(it.contentResolver, secilenGorsel)
                            img_insert.setImageBitmap(secilenBitmap)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun kucukBitmapOlustur(kullanicininSectigiBitmap: Bitmap, maximumBoyut: Int): Bitmap {

        var width = kullanicininSectigiBitmap.width
        var height = kullanicininSectigiBitmap.height
        val bitmapOrani: Double = width.toDouble() / height.toDouble()

        if (bitmapOrani > 1) {
            // görselimiz yatay
            width = maximumBoyut
            val kisaltilmisHeight = width / bitmapOrani
            height = kisaltilmisHeight.toInt()
        } else {
            // görselimiz dikey
            height = maximumBoyut
            val kisaltilmisWidth = height * bitmapOrani
            width = kisaltilmisWidth.toInt()
        }
        return Bitmap.createScaledBitmap(kullanicininSectigiBitmap, width / 2, height / 2, true)
    }
}