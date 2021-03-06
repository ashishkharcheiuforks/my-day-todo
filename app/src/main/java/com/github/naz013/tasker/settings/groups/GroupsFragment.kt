package com.github.naz013.tasker.settings.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.naz013.tasker.R
import com.github.naz013.tasker.arch.BaseFragment
import com.github.naz013.tasker.arch.OnStartDragListener
import com.github.naz013.tasker.data.TaskGroup
import com.github.naz013.tasker.utils.GoogleDrive
import com.github.naz013.tasker.utils.LocalDrive
import com.github.naz013.tasker.utils.launchDefault
import com.google.android.material.snackbar.Snackbar
import com.mcxiaoke.koi.ext.onClick
import kotlinx.android.synthetic.main.fragment_groups.*

class GroupsFragment : BaseFragment(), OnStartDragListener {

    private var mItemTouchHelper: ItemTouchHelper? = null
    private var mAdapter: GroupsListAdapter = GroupsListAdapter()
    private lateinit var viewModel: GroupsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.onClick { findNavController().navigateUp() }
        fabAdd.onClick { openAddScreen() }

        mAdapter = GroupsListAdapter()
        mAdapter.mDragStartListener = this
        mAdapter.callback = { position, action -> performAction(position, action) }
        mAdapter.deleteCallback = { position -> showSnackbar(position) }

        tasksList.layoutManager = LinearLayoutManager(context)
        tasksList.adapter = mAdapter

        val callback = SimpleItemTouchHelperCallback(mAdapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(tasksList)

        updateEmpty()

        initViewModel()
    }

    private fun updateEmpty() {
        if (mAdapter.itemCount == 0) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    private fun showSnackbar(position: Int) {
        val snack = Snackbar.make(coordinator, getString(R.string.delete_this_group_), Snackbar.LENGTH_LONG)
        snack.setAction(getString(R.string.yes)) { mAdapter.delete(position) }
        snack.setActionTextColor(ContextCompat.getColor(context!!, R.color.colorRed))
        snack.show()
    }

    private fun performAction(group: TaskGroup, action: Int) {
        when (action) {
            GroupsListAdapter.EDIT -> openGroup(group)
            GroupsListAdapter.DELETE -> deleteGroup(group)
        }
    }

    private fun deleteGroup(group: TaskGroup) {
        viewModel.deleteGroup(group)
    }

    private fun openAddScreen() {
        findNavController().navigate(GroupsFragmentDirections.actionGroupsFragmentToAddGroupFragment())
    }

    private fun openGroup(group: TaskGroup) {
        val directions = GroupsFragmentDirections.actionGroupsFragmentToAddGroupFragment()
        directions.argId = group.id
        findNavController().navigate(directions)
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(GroupsViewModel::class.java)
        viewModel.data.observe(this, Observer { data -> if (data != null) updateList(data) })
    }

    private fun updateList(data: List<TaskGroup>) {
        mAdapter.setData(data)
        updateEmpty()
    }

    override fun onStartDrag(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        mItemTouchHelper?.startDrag(viewHolder)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.saveGroups(mAdapter.items)
        backupData()
    }

    private fun backupData() {
        val app = activity?.application ?: return
        launchDefault {
            val googleDrive = GoogleDrive(app)
            val localDrive = LocalDrive(app)
            googleDrive.saveToDrive()
            localDrive.saveToDrive()
        }
    }
}