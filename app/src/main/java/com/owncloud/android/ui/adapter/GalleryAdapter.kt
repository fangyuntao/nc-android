/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 * Copyright (C) 2023 TSI-mc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.databinding.GalleryHeaderBinding
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.Calendar
import java.util.Date

@Suppress("LongParameterList")
class GalleryAdapter(
    val context: Context,
    private val user: User,
    ocFileListFragmentInterface: OCFileListFragmentInterface,
    preferences: AppPreferences,
    transferServiceGetter: ComponentsGetter,
    viewThemeUtils: ViewThemeUtils,
    var columns: Int,
    private val defaultThumbnailSize: Int,
    private val clientFactory: ClientFactory
) : SectionedRecyclerViewAdapter<SectionedViewHolder>(), CommonOCFileListAdapterInterface, PopupTextProvider {
    var files: List<GalleryItems> = mutableListOf()
    private val ocFileListDelegate: OCFileListDelegate
    private var storageManager: FileDataStorageManager

    init {
        storageManager = transferServiceGetter.storageManager

        ocFileListDelegate = OCFileListDelegate(
            context,
            ocFileListFragmentInterface,
            user,
            storageManager,
            false,
            preferences,
            true,
            transferServiceGetter,
            showMetadata = false,
            showShareAvatar = false,
            viewThemeUtils
        )

        setHasStableIds(true)
    }

    override fun showFooters(): Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            GalleryHeaderViewHolder(
                GalleryHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            GalleryRowHolder(
                GalleryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                defaultThumbnailSize.toFloat(),
                ocFileListDelegate,
                storageManager,
                this,
                user,
                clientFactory
            )
        }
    }

    override fun getItemViewType(section: Int, relativePosition: Int, absolutePosition: Int): Int {
        return absolutePosition
    }

    override fun getItemId(section: Int, position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (holder != null) {
            val rowHolder = holder as GalleryRowHolder
            rowHolder.bind(files[section].rows[relativePosition])
        }
    }

    override fun getItemCount(section: Int): Int {
        return files[section].rows.size
    }

    override fun getSectionCount(): Int {
        return files.size
    }

    override fun getPopupText(position: Int): String {
        return DisplayUtils.getDateByPattern(
            files[getRelativePosition(position).section()].date,
            context,
            DisplayUtils.MONTH_YEAR_PATTERN
        )
    }

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder?, section: Int, expanded: Boolean) {
        if (holder != null) {
            val headerViewHolder = holder as GalleryHeaderViewHolder
            val galleryItem = files[section]

            headerViewHolder.binding.month.text = DisplayUtils.getDateByPattern(
                galleryItem.date,
                context,
                DisplayUtils.MONTH_PATTERN
            )
            headerViewHolder.binding.year.text = DisplayUtils.getDateByPattern(
                galleryItem.date,
                context,
                DisplayUtils.YEAR_PATTERN
            )
        }
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) {
        TODO("Not yet implemented")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showAllGalleryItems(
        remotePath: String,
        mediaState: GalleryFragmentBottomSheetDialog.MediaState,
        photoFragment: GalleryFragment
    ) {
        val items = storageManager.allGalleryItems

        val filteredList = items.filter { it != null && it.remotePath.startsWith(remotePath) }

        setMediaFilter(
            filteredList,
            mediaState,
            photoFragment
        )
    }

    // Set Image/Video List According to Selection of Hide/Show Image/Video
    @SuppressLint("NotifyDataSetChanged")
    private fun setMediaFilter(
        items: List<OCFile>,
        mediaState: GalleryFragmentBottomSheetDialog.MediaState,
        photoFragment: GalleryFragment
    ) {
        val finalSortedList: List<OCFile> = when (mediaState) {
            GalleryFragmentBottomSheetDialog.MediaState.MEDIA_STATE_PHOTOS_ONLY -> {
                items.filter { MimeTypeUtil.isImage(it.mimeType) }.distinct()
            }
            GalleryFragmentBottomSheetDialog.MediaState.MEDIA_STATE_VIDEOS_ONLY -> {
                items.filter { MimeTypeUtil.isVideo(it.mimeType) }.distinct()
            }
            else -> items
        }

        if (finalSortedList.isEmpty()) {
            photoFragment.setEmptyListMessage(SearchType.GALLERY_SEARCH)
        }

        files = finalSortedList
            .groupBy { firstOfMonth(it.modificationTimestamp) }
            .map { GalleryItems(it.key, transformToRows(it.value)) }
            .sortedBy { it.date }.reversed()

        Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
    }

    private fun transformToRows(list: List<OCFile>): List<GalleryRow> {
        return list
            .sortedBy { it.modificationTimestamp }
            .reversed()
            .chunked(columns)
            .map { entry -> GalleryRow(entry, defaultThumbnailSize, defaultThumbnailSize) }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        files = emptyList()
        Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
    }

    private fun firstOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.time = Date(timestamp)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)

        return cal.timeInMillis
    }

    fun isEmpty(): Boolean {
        return files.isEmpty()
    }

    fun getItem(position: Int): OCFile? {
        val itemCoordinates = getRelativePosition(position)

        return files
            .getOrNull(itemCoordinates.section())
            ?.rows
            ?.getOrNull(itemCoordinates.relativePos())
            ?.files
            ?.getOrNull(0)
    }

    override fun isMultiSelect(): Boolean {
        return ocFileListDelegate.isMultiSelect
    }

    override fun cancelAllPendingTasks() {
        ocFileListDelegate.cancelAllPendingTasks()
    }

    override fun getItemPosition(file: OCFile): Int {
        val findResult = files
            .asSequence()
            .flatMapIndexed { itemIndex, item ->
                item.rows.withIndex().map { row -> Triple(itemIndex, row.index, row.value) }
            }.find {
                it.third.files.contains(file)
            }

        val (item, row) = findResult ?: Triple(0, 0, null)
        return getAbsolutePosition(item, row)
    }

    override fun swapDirectory(
        user: User,
        directory: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean,
        mLimitToMimeType: String
    ) {
        TODO("Not yet implemented")
    }

    override fun setHighlightedItem(file: OCFile) {
        TODO("Not yet implemented")
    }

    override fun setSortOrder(mFile: OCFile, sortOrder: FileSortOrder) {
        TODO("Not yet implemented")
    }

    override fun addCheckedFile(file: OCFile) {
        ocFileListDelegate.addCheckedFile(file)
    }

    override fun isCheckedFile(file: OCFile): Boolean {
        return ocFileListDelegate.isCheckedFile(file)
    }

    override fun getCheckedItems(): Set<OCFile> {
        return ocFileListDelegate.checkedItems
    }

    override fun removeCheckedFile(file: OCFile) {
        ocFileListDelegate.removeCheckedFile(file)
    }

    override fun notifyItemChanged(file: OCFile) {
        notifyItemChanged(getItemPosition(file))
    }

    override fun getFilesCount(): Int {
        return files.fold(0) { acc, item -> acc + item.rows.size }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun setMultiSelect(boolean: Boolean) {
        ocFileListDelegate.isMultiSelect = boolean
        notifyDataSetChanged()
    }

    override fun clearCheckedItems() {
        ocFileListDelegate.clearCheckedItems()
    }

    @VisibleForTesting
    fun addFiles(items: List<GalleryItems>) {
        files = items
    }

    fun changeColumn(newColumn: Int) {
        columns = newColumn
    }
}
