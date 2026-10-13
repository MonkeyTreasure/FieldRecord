package com.fieldrecord.presentation.gallery

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.repository.FieldPhotoRepository

class FieldPhotoPagingSource(
    private val repository: FieldPhotoRepository,
    private val startTime: Long,
    private val endTime: Long
) : PagingSource<Int, FieldPhoto>() {

    override fun getRefreshKey(state: PagingState<Int, FieldPhoto>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FieldPhoto> {
        return try {
            val page = params.key ?: 0
            val photos = repository.getPhotosByDateRange(startTime, endTime)
            // Simplified: return all in one page for now since it's local
            LoadResult.Page(
                data = photos,
                prevKey = if (page == 0) null else page - 1,
                nextKey = null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
