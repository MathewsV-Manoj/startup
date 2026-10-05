package com.startup.focuno.data.export

import android.content.Context
import android.net.Uri
import com.startup.focuno.data.repository.SummaryRepository
import com.startup.focuno.data.room.FocusSessionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Writes the CSV to a file the person picked. Nothing leaves the phone unless they move that file. */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val summaryRepository: SummaryRepository,
    private val focusSessionDao: FocusSessionDao,
) {
    suspend fun export(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val days = summaryRepository.summaries(today.minusDays(HISTORY_DAYS), today)
        val csv = CsvExport.build(days, focusSessionDao.all(), ZoneId.systemDefault())
        val stream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
        stream.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
        true
    }

    private companion object {
        const val HISTORY_DAYS = 400L
    }
}
