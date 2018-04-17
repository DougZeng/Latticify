package io.github.tonnyl.latticify.ui.chat

import android.content.*
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.airbnb.epoxy.EpoxyModel
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import io.github.tonnyl.charles.Charles
import io.github.tonnyl.latticify.R
import io.github.tonnyl.latticify.data.Channel
import io.github.tonnyl.latticify.data.Message
import io.github.tonnyl.latticify.data.event.UserTyping
import io.github.tonnyl.latticify.epoxy.LoadMoreModel_
import io.github.tonnyl.latticify.glide.CharlesGlideV4Engine
import io.github.tonnyl.latticify.glide.MatisseGlideV4Engine
import io.github.tonnyl.latticify.ui.channel.profile.ChannelProfileActivity
import io.github.tonnyl.latticify.ui.channel.profile.ChannelProfilePresenter
import io.github.tonnyl.latticify.ui.message.MessageActivity
import io.github.tonnyl.latticify.ui.message.MessagePresenter
import io.github.tonnyl.latticify.util.Constants
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.layout_input.*
import kotlinx.android.synthetic.main.layout_message_action.*
import java.util.concurrent.TimeUnit

/**
 * Created by lizhaotailang on 06/10/2017.
 *
 */
class ChatFragment : Fragment(), ChatContract.View {

    override var ignoreScrollChange: Boolean = false

    private lateinit var mPresenter: ChatContract.Presenter

    private var mIsLoading = false
    private val mAdapter = ChatMessageAdapter()
    private val mLoadMoreModel = LoadMoreModel_()

    private val mCompositeDisposable = CompositeDisposable()

    private var mSubTitle = ""

    private var mIsEditingMessage = false
    private var mEditingMessage: Message? = null

    private val mUserTypingReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val userTyping = it.getParcelableExtra<UserTyping>(Constants.BROADCAST_EXTRA)

                activity?.let {
                    (it as ChatActivity).supportActionBar?.subtitle = "${userTyping.user} is typing…"
                }
                val disposable = Observable.timer(2000L, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            activity?.let {
                                (it as ChatActivity).supportActionBar?.subtitle = mSubTitle
                            }
                        }, {

                        })
                mCompositeDisposable.add(disposable)
            }
        }

    }

    private val mMessageReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                mPresenter.handleMessageEvent(it)
            }
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(): ChatFragment = ChatFragment()

        val REQUEST_CHOOSE_IMAGE = 101
        val REQUEST_CHOOSE_FILE = 102

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout.isEnabled = false
        context?.let {
            swipeRefreshLayout.setColorSchemeColors(it.getColor(R.color.colorAccent))
        }
        swipeRefreshLayout.setOnRefreshListener {
            mPresenter.fetchData()
        }

        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            adapter = mAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!ignoreScrollChange) {
                        val totalItemCount = recyclerView?.adapter?.itemCount
                        val lastVisibleItemPosition = recyclerView?.layoutManager?.let {
                            (it as LinearLayoutManager).findLastVisibleItemPosition()
                        }

                        if (!mIsLoading
                                && totalItemCount != null
                                && lastVisibleItemPosition != null
                                && lastVisibleItemPosition == totalItemCount - 1) {
                            mIsLoading = true
                            mPresenter.fetchDataOfNextPage()
                        }
                    }
                }
            })
        }

        messageEditText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(editable: Editable?) {
                if (editable.isNullOrBlank()) {
                    sendMessageImageView.clearColorFilter()
                } else {
                    context?.let {
                        sendMessageImageView.setColorFilter(it.getColor(R.color.colorPrimary))
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        sendMessageImageView.setOnClickListener {
            if (!messageEditText.text.isNullOrBlank()) {
                if (mIsEditingMessage && mEditingMessage != null) {
                    mEditingMessage?.let {
                        mPresenter.updateMessage(messageEditText.text.toString(), it)
                    }
                } else {
                    mPresenter.sendMessage(messageEditText.text.toString())
                    messageEditText.setText("")
                }
            }
        }

        plusImageView.setOnClickListener {
            showBottomSheetDialog()
        }

        setHasOptionsMenu(true)

        mPresenter.subscribe()
    }

    override fun onResume() {
        super.onResume()

        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(mUserTypingReceiver, IntentFilter(Constants.FILTER_USER_TYPING))
            LocalBroadcastManager.getInstance(it).registerReceiver(mMessageReceiver, IntentFilter(Constants.FILTER_MESSAGE))
        }
    }

    override fun onPause() {
        super.onPause()

        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(mUserTypingReceiver)
            LocalBroadcastManager.getInstance(it).unregisterReceiver(mMessageReceiver)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mPresenter.unsubscribe()
        mCompositeDisposable.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.menu_channel, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        when (id) {
            android.R.id.home -> {
                activity?.onBackPressed()
            }
            R.id.action_view_details -> {
                mPresenter.viewDetails()
            }
            R.id.action_invite_members_to_channel -> {

            }
            R.id.action_directory -> {

            }
        }
        return true
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_copy_text -> {

            }
            R.id.action_copy_link_to_message -> {

            }
            R.id.action_share_message -> {

            }
            R.id.action_add_reaction -> {

            }
            R.id.action_star -> {

            }
            R.id.action_pin_to_conversation -> {

            }
            R.id.action_edit_message -> {

            }
            R.id.action_delete_message -> {

            }
        }

        return true
    }

    override fun setPresenter(presenter: ChatContract.Presenter) {
        mPresenter = presenter
    }

    override fun showData(epoxyModels: Collection<EpoxyModel<*>>) {
        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        errorView.visibility = View.GONE
        mAdapter.removeAllModels()
        mAdapter.addModels(epoxyModels)
    }

    override fun showDataOfNextPage(epoxyModels: Collection<EpoxyModel<*>>) {
        mAdapter.addModels(epoxyModels)
    }

    override fun addModel(epoxyModel: EpoxyModel<*>) {
        mAdapter.addModel(epoxyModel)
    }

    override fun setLoadingIndicator(loading: Boolean) {
        swipeRefreshLayout.isRefreshing = loading
        emptyView.visibility = View.GONE
        errorView.visibility = View.GONE

        mIsLoading = loading
    }

    override fun showLoadingMore(loadingMore: Boolean) {
        if (loadingMore) {
            addModel(mLoadMoreModel)
        } else {
            mAdapter.removeModel(mLoadMoreModel)
        }

        mIsLoading = loadingMore
    }

    override fun showEmptyView() {
        emptyView.visibility = View.VISIBLE
    }

    override fun showErrorView() {
        errorView.visibility = View.VISIBLE
    }

    override fun showChannel(channel: Channel) {
        with(activity as ChatActivity) {
            supportActionBar?.title = channel.name

            mSubTitle = "${channel.numMembers} members"
            supportActionBar?.subtitle = mSubTitle
        }
    }

    override fun gotoChannelDetails(channel: Channel) {
        activity?.let {
            context?.startActivity(Intent(context, ChannelProfileActivity::class.java).apply { putExtra(ChannelProfilePresenter.KEY_EXTRA_CHANNEL, channel) })
        }
    }

    override fun gotoMessageDetails(message: Message) {
        activity?.let {
            context?.startActivity(Intent(context, MessageActivity::class.java).apply { putExtra(MessagePresenter.KEY_EXTRA_MESSAGE, message) })
        }
    }

    override fun insertNewMessage(epoxyModel: EpoxyModel<*>, position: Int) {
        mAdapter.insertModel(epoxyModel, position)
    }

    override fun deleteMessage(epoxyModel: EpoxyModel<*>) {
        mAdapter.removeModel(epoxyModel)
    }

    override fun updateMessage(epoxyModel: EpoxyModel<*>, message: Message) {
        mAdapter.updateModel(epoxyModel, message)
    }

    override fun showMessageActions(message: Message) {
        context?.let {
            val dialog = BottomSheetDialog(it)
            val view = layoutInflater.inflate(R.layout.layout_message_actions, null)
            dialog.setContentView(view)

            view.findViewById<TextView>(R.id.actionCopyText).setOnClickListener {
                dialog.dismiss()

                context?.let { context ->
                    (message.text
                            ?: message.attachments?.getOrNull(0)?.let { "${it.title}\n${it.text}" })?.let {
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("text", it)
                        manager.primaryClip = clipData

                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(context, R.string.copy_failed, Toast.LENGTH_SHORT).show()
                    }
                }

            }

            view.findViewById<TextView>(R.id.actionShareMessage).setOnClickListener {
                dialog.dismiss()

                messageActionLayout.visibility = View.VISIBLE
                actionImageView.setImageResource(R.drawable.ic_forward_black_24dp)
                userNameTextView.text = message.user
                messageContentTextView.text = message.text
                dismissImageView.setOnClickListener {
                    messageActionLayout.visibility = View.GONE
                }
            }

            view.findViewById<TextView>(R.id.actionStar).apply {
                setText(if (message.isStarred != true) R.string.star else R.string.unstar)
                setCompoundDrawablesWithIntrinsicBounds(context?.getDrawable(if (message.isStarred != true) R.drawable.ic_star_border_black_24dp else R.drawable.ic_star_black_24dp), null, null, null)

                setOnClickListener {
                    dialog.dismiss()

                    mPresenter.starMessage(message.ts, message.isStarred != true)
                }
            }

            view.findViewById<TextView>(R.id.actionPinToConversation).setOnClickListener {
                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.actionEditMessage).setOnClickListener {
                mIsEditingMessage = true
                mEditingMessage = message

                dismissImageView.setOnClickListener {
                    dismissMessageAction()
                }

                dialog.setOnDismissListener {
                    actionImageView.setImageResource(R.drawable.ic_edit_black_24dp)
                    userNameTextView.text = message.user
                    messageContentTextView.text = message.text
                    messageEditText.setText(message.text)
                    // Move the cursor to end
                    messageEditText.setSelection(messageEditText.text.length)
                    sendMessageImageView.setImageResource(R.drawable.ic_done_black_24dp)
                    messageActionLayout.visibility = View.VISIBLE
                }

                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.actionDeleteMessage).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    override fun dismissMessageAction() {
        messageEditText.setText("")
        sendMessageImageView.setImageResource(R.drawable.ic_send_black_24dp)
        messageActionLayout.visibility = View.GONE

        mIsEditingMessage = false
        mEditingMessage = null
    }

    override fun showMessageStarred(starred: Boolean) {
        Toast.makeText(context, if (starred) R.string.msg_starred else R.string.msg_unstarred, Toast.LENGTH_SHORT).show()
    }

    private fun showBottomSheetDialog() {
        context?.let {
            val dialog = BottomSheetDialog(it)
            val view = layoutInflater.inflate(R.layout.layout_chat_bottom_sheet, null)
            dialog.setContentView(view)

            view.findViewById<TextView>(R.id.actionCamera).setOnClickListener {
                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.actionGallery).setOnClickListener {
                dialog.dismiss()

                Matisse.from(this)
                        .choose(MimeType.allOf())
                        .imageEngine(MatisseGlideV4Engine())
                        .countable(true)
                        .maxSelectable(1)
                        .theme(R.style.Latticify_MatisseStyle)
                        .forResult(REQUEST_CHOOSE_IMAGE)
            }

            view.findViewById<TextView>(R.id.actionFile).setOnClickListener {
                dialog.dismiss()

                Charles.from(this)
                        .choose()
                        .imageEngine(CharlesGlideV4Engine())
                        .progressRate(true)
                        .maxSelectable(1)
                        .theme(R.style.Latticify_CharlesStyle)
                        .forResult(REQUEST_CHOOSE_FILE)
            }

            view.findViewById<TextView>(R.id.actionAt).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

}