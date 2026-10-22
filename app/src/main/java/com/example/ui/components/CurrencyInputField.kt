package com.example.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.example.domain.FinancialEngine

@Composable
fun CurrencyInputField(
  value: Long,
  onValueChange: (Long) -> Unit,
  label: String = "Nominal",
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  supportingText: @Composable (() -> Unit)? = null
) {
  val formattedText = if (value == 0L) "" else FinancialEngine.formatRupiah(value, includePrefix = false)

  OutlinedTextField(
    value = formattedText,
    onValueChange = { newText ->
      val parsed = FinancialEngine.parseRupiahInput(newText)
      onValueChange(parsed)
    },
    label = { Text(label) },
    prefix = {
      Text(
        "Rp ",
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    },
    placeholder = { Text("0") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    singleLine = true,
    isError = isError,
    supportingText = supportingText,
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline
    ),
    modifier = modifier.testTag("currency_input_field")
  )
}
