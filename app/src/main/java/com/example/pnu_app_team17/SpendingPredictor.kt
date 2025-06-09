package com.example.pnu_app_team17

import org.tensorflow.lite.Interpreter
import java.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpendingPredictor(private val interpreter: Interpreter) {

    fun predictNextSobi(sobiItems: List<SobiItem>): PredictedSobi {
        val input = sobiItems.toTFLiteInput() // [1][5][5]

        val amountOutput = Array(1) { FloatArray(1) }
        val categoryOutput = Array(1) { FloatArray(Category.values().size) }
        val dateOutput = Array(1) { FloatArray(3) }

        val inputs: Array<Any> = arrayOf(input)
        val outputs: MutableMap<Int, Any> = mapOf(
            0 to amountOutput,
            1 to categoryOutput,
            2 to dateOutput
        ).toMutableMap()

        interpreter.runForMultipleInputsOutputs(inputs, outputs)

        val amountNorm = amountOutput[0][0]
        val categoryIdx = categoryOutput[0].indexOfMax()
        val category = Category.fromIndex(categoryIdx)

        val dowNorm = dateOutput[0][0]
        val dayNorm = dateOutput[0][1]
        val monthNorm = dateOutput[0][2]

        val day = (dayNorm * 31f).toInt().coerceIn(1, 31)
        val month = (monthNorm * 12f).toInt().coerceIn(1, 12)
        val year = Calendar.getInstance().get(Calendar.YEAR)

        val dateStr = String.format("%04d-%02d-%02d", year, month, day)
        val denormAmount = (amountNorm * 50000).toInt()

        return PredictedSobi(dateStr, category, denormAmount)
    }

    private fun FloatArray.indexOfMax(): Int = this.indices.maxByOrNull { this[it] } ?: 0
}

data class PredictedSobi(
    val date: String,
    val category: Category,
    val amount: Int
)