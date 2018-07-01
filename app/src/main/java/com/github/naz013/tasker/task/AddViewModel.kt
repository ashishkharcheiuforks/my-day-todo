package com.github.naz013.tasker.task

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.github.naz013.tasker.data.AppDb
import com.github.naz013.tasker.data.Task
import com.github.naz013.tasker.data.TaskGroup
import com.github.naz013.tasker.utils.GoogleDrive
import com.github.naz013.tasker.utils.LocalDrive
import com.github.naz013.tasker.utils.Notifier
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import java.util.*

/**
 * Copyright 2018 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class AddViewModel(application: Application, val id: Int) : AndroidViewModel(application) {

    private val mDb = AppDb.getInMemoryDatabase(application.applicationContext)
    val data: LiveData<TaskGroup?> = mDb.groupDao().loadById(id)

    fun saveTask(summary: String, group: TaskGroup, important: Boolean) {
        launch(CommonPool) {
            group.tasks.add(Task((UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).toInt(), false, summary, group.id, important, "", ""))
            mDb.groupDao().insert(group)
            val googleDrive = GoogleDrive(getApplication())
            val localDrive = LocalDrive(getApplication())
            googleDrive.saveToDrive()
            localDrive.saveToDrive()
        }
    }

    fun saveGroup(group: TaskGroup) {
        launch(CommonPool) {
            mDb.groupDao().insert(group)
            if (group.notificationEnabled) {
                withContext(UI) {
                    Notifier(getApplication()).showNotification(group)
                }
            }
        }
    }

    class Factory(private val application: Application, val id: Int) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AddViewModel(application, id) as T
        }
    }
}