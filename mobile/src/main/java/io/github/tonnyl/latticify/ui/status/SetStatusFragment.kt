package io.github.tonnyl.latticify.ui.status

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import io.github.tonnyl.latticify.R
import kotlinx.android.synthetic.main.fragment_set_status.*

/**
 * Created by lizhaotailang on 11/10/2017.
 */
class SetStatusFragment : Fragment(), SetStatusContract.View {

    private lateinit var mPresenter: SetStatusContract.Presenter
    private var mMenu: Menu? = null

    companion object {
        @JvmStatic
        fun newInstance() = SetStatusFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_set_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        statusEditText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mMenu?.getItem(0)?.isEnabled = !s.isNullOrBlank()
            }

        })

        meetingTextView.setOnClickListener {
            statusEditText.setText(R.string.in_a_meeting)
        }

        commutingTextView.setOnClickListener {
            statusEditText.setText(R.string.commuting)
        }

        outSickTextView.setOnClickListener {
            statusEditText.setText(R.string.out_sick)
        }

        vacationingTextView.setOnClickListener {
            statusEditText.setText(R.string.vacationing)
        }

        workingRemotelyTextView.setOnClickListener {
            statusEditText.setText(R.string.working_remotely)
        }

        mPresenter.subscribe()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mPresenter.unsubscribe()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.menu_done, menu)
        mMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
            }
            R.id.action_done -> {
                mPresenter.updateStatus("", statusEditText.text.toString())
            }
        }
        return true
    }

    override fun setPresenter(presenter: SetStatusContract.Presenter) {
        mPresenter = presenter
    }

    override fun setStatus(statusEmoji: String, status: String) {
        statusEditText.setText(status)
    }

    override fun showStatusUpdated() {
        Toast.makeText(context, getString(R.string.status_updated), Toast.LENGTH_SHORT).show()

        activity?.onBackPressed()
    }

    override fun showUpdateStatusFailed(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

}