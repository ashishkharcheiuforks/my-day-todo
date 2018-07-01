package com.github.naz013.tasker.group.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.naz013.tasker.R
import com.github.naz013.tasker.arch.NestedFragment
import com.github.naz013.tasker.data.Task
import com.github.naz013.tasker.data.TaskGroup
import com.github.naz013.tasker.task.AddTaskFragment
import com.github.naz013.tasker.task.AddViewModel
import com.github.naz013.tasker.utils.GoogleDrive
import com.github.naz013.tasker.utils.LocalDrive
import com.github.naz013.tasker.utils.Notifier
import com.github.naz013.tasker.utils.Prefs
import com.mcxiaoke.koi.ext.onClick
import kotlinx.android.synthetic.main.fragment_view_group.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

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
class ViewGroupFragment : NestedFragment() {

    companion object {
        const val TAG = "ViewGroupFragment"
        private const val ARG_ID = "arg_id"
        fun newInstance(id: Int): ViewGroupFragment {
            val fragment = ViewGroupFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_ID, id)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var mGroupId: Int = 0
    private var mGroup: TaskGroup? = null
    private lateinit var viewModel: AddViewModel
    private val mAdapter = TasksListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mGroupId = arguments?.getInt(ARG_ID)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_view_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab.onClick { navInterface?.moveBack() }
        fabAdd.onClick { navInterface?.openFragment(AddTaskFragment.newInstance(mGroupId), AddTaskFragment.TAG) }
        fabNotification.onClick { showNotification() }

        tasksList.layoutManager = LinearLayoutManager(context)

        mAdapter.callback = { list -> saveUpdates(list) }
        mAdapter.deleteCallback = { position -> showSnackbar(position) }
        tasksList.adapter = mAdapter

        updateEmpty()

        initViewModel()
    }

    private fun showNotification() {
        val group = mGroup ?: return
        if (group.notificationEnabled) {
            Notifier(context!!).hideNotification(group)
        } else {
            Notifier(context!!).showNotification(group)
        }
        group.notificationEnabled = !group.notificationEnabled
        viewModel.saveGroup(group)
    }

    private fun updateEmpty() {
        if (mAdapter.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    private fun showSnackbar(position: Int) {
        val snack = Snackbar.make(coordinator, getString(R.string.delete_this_task_), Snackbar.LENGTH_LONG)
        snack.setAction(getString(R.string.yes)) { mAdapter.delete(position) }
        snack.setActionTextColor(ContextCompat.getColor(context!!, R.color.colorRed))
        snack.show()
    }

    private fun saveUpdates(list: List<Task>) {
        val group = mGroup
        if (group != null) {
            group.tasks.clear()
            group.tasks.addAll(list)
            viewModel.saveGroup(group)
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, AddViewModel.Factory(activity?.application!!, mGroupId)).get(AddViewModel::class.java)
        viewModel.data.observe(this, Observer { group -> if (group != null) showGroup(group) })
    }

    private fun showGroup(group: TaskGroup) {
        mGroup = group

        fabNotification.setImageResource(if (group.notificationEnabled) R.drawable.ic_silence else R.drawable.ic_alarm)
        if (group.notificationEnabled) {
            Notifier(context!!).showNotification(group)
        } else {
            Notifier(context!!).hideNotification(group)
        }

        titleView.text = group.name
        var list = group.tasks
        val important = Prefs.getInstance(context!!).getImportant()
        val importantIds = Prefs.getInstance(context!!).getStringList(Prefs.IMPORTANT_FIRST_IDS)
        if (important == Prefs.ENABLED || (important == Prefs.CUSTOM && importantIds.contains(group.id.toString()))) {
            list = list.sortedByDescending { it.important }.toMutableList()
        }
        mAdapter.setData(list.sortedByDescending { it.dt }.sortedBy { it.done })
        updateEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        backupData()
    }

    private fun backupData() {
        val app = activity?.application ?: return
        launch(CommonPool) {
            val googleDrive = GoogleDrive(app)
            val localDrive = LocalDrive(app)
            googleDrive.saveToDrive()
            localDrive.saveToDrive()
        }
    }
}