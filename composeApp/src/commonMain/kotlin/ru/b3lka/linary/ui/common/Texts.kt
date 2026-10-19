package ru.b3lka.linary.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiarySource
import ru.b3lka.linary.core.repository.DiaryWriteException
import ru.b3lka.linary.presentation.format.DateFormatter
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.entries_count
import ru.b3lka.linary.resources.error_decryption
import ru.b3lka.linary.resources.error_io
import ru.b3lka.linary.resources.error_key_missing
import ru.b3lka.linary.resources.error_not_diary
import ru.b3lka.linary.resources.error_not_sync_folder
import ru.b3lka.linary.resources.error_unexpected
import ru.b3lka.linary.resources.error_unknown_format
import ru.b3lka.linary.resources.error_unsupported_version
import ru.b3lka.linary.resources.error_write_conflict
import ru.b3lka.linary.resources.error_write_io
import ru.b3lka.linary.resources.error_write_read_only
import ru.b3lka.linary.resources.error_write_verification
import ru.b3lka.linary.resources.months_genitive
import ru.b3lka.linary.resources.months_nominative
import ru.b3lka.linary.resources.pattern_day_month
import ru.b3lka.linary.resources.pattern_long_date
import ru.b3lka.linary.resources.pattern_month_year
import ru.b3lka.linary.resources.source_backup
import ru.b3lka.linary.resources.source_sync_folder
import ru.b3lka.linary.resources.weekdays_full
import ru.b3lka.linary.resources.weekdays_short

/**
 * Форматтер дат на строках текущего языка.
 */
@Composable
fun rememberDateFormatter(): DateFormatter {
    val monthsNominative = stringArrayResource(Res.array.months_nominative)
    val monthsGenitive = stringArrayResource(Res.array.months_genitive)
    val weekdaysShort = stringArrayResource(Res.array.weekdays_short)
    val weekdaysFull = stringArrayResource(Res.array.weekdays_full)
    val patterns = DateFormatter.Patterns(
        monthYear = stringResource(Res.string.pattern_month_year),
        dayMonth = stringResource(Res.string.pattern_day_month),
        longDate = stringResource(Res.string.pattern_long_date),
    )
    return remember(monthsNominative, monthsGenitive, weekdaysShort, weekdaysFull, patterns) {
        DateFormatter(monthsNominative, monthsGenitive, weekdaysShort, weekdaysFull, patterns)
    }
}

/**
 * «5 записей» / «5 entries».
 */
@Composable
fun entriesCountText(count: Int): String = pluralStringResource(Res.plurals.entries_count, count, count)

@Composable
fun sourceText(source: DiarySource): String = when (source.kind) {
    DiarySource.Kind.SyncFolder -> stringResource(Res.string.source_sync_folder, source.path)
    DiarySource.Kind.Backup -> stringResource(Res.string.source_backup, source.path)
}

@Composable
fun errorText(error: DiaryLoadException): String {
    val details = error.details.orEmpty()
    return when (error.reason) {
        DiaryLoadException.Reason.NotSyncFolder -> stringResource(Res.string.error_not_sync_folder)
        DiaryLoadException.Reason.UnsupportedSyncVersion ->
            stringResource(Res.string.error_unsupported_version, details)
        DiaryLoadException.Reason.DecryptionFailed -> stringResource(Res.string.error_decryption)
        DiaryLoadException.Reason.KeyMissing -> stringResource(Res.string.error_key_missing)
        DiaryLoadException.Reason.NotDiaryDatabase -> stringResource(Res.string.error_not_diary)
        DiaryLoadException.Reason.UnknownFormat -> stringResource(Res.string.error_unknown_format)
        DiaryLoadException.Reason.Io -> stringResource(Res.string.error_io, details)
        DiaryLoadException.Reason.Unexpected -> stringResource(Res.string.error_unexpected, details)
    }
}

@Composable
fun writeErrorText(error: DiaryWriteException): String {
    val details = error.details.orEmpty()
    return when (error.reason) {
        DiaryWriteException.Reason.ReadOnly -> stringResource(Res.string.error_write_read_only)
        DiaryWriteException.Reason.Conflict -> stringResource(Res.string.error_write_conflict)
        DiaryWriteException.Reason.Verification -> stringResource(Res.string.error_write_verification, details)
        DiaryWriteException.Reason.Io -> stringResource(Res.string.error_write_io, details)
        DiaryWriteException.Reason.Unexpected -> stringResource(Res.string.error_unexpected, details)
    }
}
