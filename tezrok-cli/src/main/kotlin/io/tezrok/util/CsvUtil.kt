package io.tezrok.util

import com.opencsv.CSVReader
import java.io.StringReader

/**
 * Utility to work with CSV.
 */
object CsvUtil {
    /**
     * Converts CSV string to list of records.
     */
    fun fromString(raw: String): List<List<String>> {
        val records: MutableList<List<String>> = ArrayList()
        CSVReader(StringReader(raw)).use { reader ->
            var values = reader.readNext()
            while (values != null) {
                records.add(values.toList())
                values = reader.readNext()
            }
        }

        return records
    }
}
