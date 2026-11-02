package com.example.adenmedicaltest.data.repository

import android.content.Context
import com.example.adenmedicaltest.data.model.SyllabusData
import com.example.adenmedicaltest.data.model.SyllabusSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class SyllabusRepository(private val context: Context) {

    private var cachedSyllabus: Map<String, SyllabusData>? = null

    suspend fun getSyllabus(subjectId: String): SyllabusData? = withContext(Dispatchers.IO) {
        if (cachedSyllabus == null) {
            loadSyllabus()
        }
        cachedSyllabus?.get(subjectId)
    }

    suspend fun getAllSyllabus(): Map<String, SyllabusData> = withContext(Dispatchers.IO) {
        if (cachedSyllabus == null) {
            loadSyllabus()
        }
        cachedSyllabus ?: emptyMap()
    }

    private fun loadSyllabus() {
        try {
            val inputStream = context.assets.open("syllabus_data.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonStr = reader.use { it.readText() }
            val root = JSONObject(jsonStr)

            val map = mutableMapOf<String, SyllabusData>()
            for (key in listOf("english", "chemistry", "biology")) {
                if (root.has(key)) {
                    val obj = root.getJSONObject(key)
                    val title = obj.optString("title", "")
                    val sectionsArr = obj.optJSONArray("sections")

                    val sectionsList = mutableListOf<SyllabusSection>()
                    if (sectionsArr != null) {
                        for (i in 0 until sectionsArr.length()) {
                            val secObj = sectionsArr.getJSONObject(i)
                            val heading = secObj.optString("heading", "")
                            val itemsArr = secObj.optJSONArray("items")
                            val itemsList = mutableListOf<String>()
                            if (itemsArr != null) {
                                for (j in 0 until itemsArr.length()) {
                                    itemsList.add(itemsArr.getString(j))
                                }
                            }
                            sectionsList.add(SyllabusSection(heading, itemsList))
                        }
                    }
                    map[key] = SyllabusData(title, sectionsList)
                }
            }
            cachedSyllabus = map
        } catch (e: Exception) {
            e.printStackTrace()
            cachedSyllabus = emptyMap()
        }
    }
}
