package com.harrisonog.devicemediagallery.ui.screens.duplicates

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.harrisonog.devicemediagallery.data.repository.DuplicateRepository
import com.harrisonog.devicemediagallery.data.worker.DuplicateDetectionWorker
import com.harrisonog.devicemediagallery.domain.model.DuplicateGroup
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicatesUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val showDeleteConfirmDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val duplicateRepository: DuplicateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        loadDuplicateGroups()
        observeWorkerState()
    }

    private fun loadDuplicateGroups() {
        viewModelScope.launch {
            duplicateRepository.getDuplicateGroups().collect { groups ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        duplicateGroups = groups,
                        error = null
                    )
                }
            }
        }
    }

    private fun observeWorkerState() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(DuplicateDetectionWorker.WORK_NAME)
                .collect { workInfos ->
                    val isScanning = workInfos.any {
                        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                    }
                    _uiState.update { it.copy(isScanning = isScanning) }
                }
        }
    }

    fun startScan() {
        val workRequest = OneTimeWorkRequestBuilder<DuplicateDetectionWorker>()
            .build()

        workManager.enqueueUniqueWork(
            DuplicateDetectionWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        _uiState.update { it.copy(isScanning = true) }
    }

    fun toggleItemSelection(item: MediaItem) {
        _uiState.update { state ->
            val newSelection = if (item.uri.toString() in state.selectedItems) {
                state.selectedItems - item.uri.toString()
            } else {
                state.selectedItems + item.uri.toString()
            }
            state.copy(selectedItems = newSelection)
        }
    }

    fun selectAllInGroup(group: DuplicateGroup) {
        _uiState.update { state ->
            val groupUris = group.mediaItems.drop(1).map { it.uri.toString() }.toSet()
            state.copy(selectedItems = state.selectedItems + groupUris)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptySet()) }
    }

    fun showDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun dismissDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun deleteSelectedDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteConfirmDialog = false) }

            val selectedUris = _uiState.value.selectedItems
            val itemsToDelete = _uiState.value.duplicateGroups
                .flatMap { it.mediaItems }
                .filter { it.uri.toString() in selectedUris }

            val result = duplicateRepository.deleteDuplicates(itemsToDelete)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedItems = emptySet(),
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to delete duplicates"
                        )
                    }
                }
            )
        }
    }

    fun clearDuplicateCache() {
        viewModelScope.launch {
            duplicateRepository.clearDuplicateCache()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
