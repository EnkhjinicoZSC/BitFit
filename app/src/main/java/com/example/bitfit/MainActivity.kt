package com.example.bitfit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var nutritionRV: RecyclerView
    private lateinit var nutritionAdapter: FoodAdapter
    private val nutrition = mutableListOf<Food>()
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nutritionRV = findViewById(R.id.foodListView)
        nutritionAdapter = FoodAdapter(this, nutrition)
        nutritionRV.adapter = nutritionAdapter
        nutritionRV.layoutManager = LinearLayoutManager(this).also {
            val dividerItemDecorator = DividerItemDecoration(this, it.orientation)
            nutritionRV.addItemDecoration(dividerItemDecorator)
        }

        lifecycleScope.launch {
            (application as FoodApplication).db.FoodDao().getAll().collect { databaseList ->
                val mappedList = databaseList.map { entity ->
                    Food(entity.foodName, entity.totalCalories)
                }
                nutrition.clear()
                nutrition.addAll(mappedList)
                runOnUiThread { nutritionAdapter.notifyDataSetChanged() }
            }
        }

        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Assuming foodName and totalCalories are the keys you used in DetailActivity
                val foodName = result.data?.getStringExtra("foodName")
                val totalCalories = result.data?.getStringExtra("totalCalories")
                if (foodName != null && totalCalories != null) {
                    val nutritionResult = Food(foodName, totalCalories)
                    nutrition.add(nutritionResult)
                    nutritionAdapter.notifyDataSetChanged()
                    lifecycleScope.launch(IO) {
                        (application as FoodApplication).db.FoodDao().insert(
                            FoodEntity(
                                foodName = nutritionResult.foodName,
                                totalCalories = nutritionResult.totalCalories
                            )
                        )
                    }
                }
            }
        }

        val addNutritionButton = findViewById<Button>(R.id.AddItemButton)
        addNutritionButton.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            startForResult.launch(intent)
        }
    }
}
