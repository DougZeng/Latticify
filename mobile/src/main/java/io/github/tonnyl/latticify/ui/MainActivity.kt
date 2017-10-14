package io.github.tonnyl.latticify.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import io.github.tonnyl.latticify.R
import io.github.tonnyl.latticify.data.repository.TeamRepository
import io.github.tonnyl.latticify.data.repository.UsersRepository
import io.github.tonnyl.latticify.glide.GlideLoader
import io.github.tonnyl.latticify.ui.about.AboutActivity
import io.github.tonnyl.latticify.ui.auth.AuthActivity
import io.github.tonnyl.latticify.ui.channels.ChannelsFragment
import io.github.tonnyl.latticify.ui.channels.ChannelsPresenter
import io.github.tonnyl.latticify.ui.channels.add.AddChannelActivity
import io.github.tonnyl.latticify.ui.directory.DirectoryFragment
import io.github.tonnyl.latticify.ui.directory.DirectoryPresenter
import io.github.tonnyl.latticify.ui.groups.GroupsFragment
import io.github.tonnyl.latticify.ui.groups.GroupsPresenter
import io.github.tonnyl.latticify.ui.ims.IMsFragment
import io.github.tonnyl.latticify.ui.ims.IMsPresenter
import io.github.tonnyl.latticify.ui.invite.InviteActivity
import io.github.tonnyl.latticify.ui.profile.ProfileActivity
import io.github.tonnyl.latticify.ui.profile.ProfilePresenter
import io.github.tonnyl.latticify.ui.search.SearchActivity
import io.github.tonnyl.latticify.ui.settings.SettingsActivity
import io.github.tonnyl.latticify.ui.starred.StarredItemsFragment
import io.github.tonnyl.latticify.ui.starred.StarredItemsPresenter
import io.github.tonnyl.latticify.ui.status.SetStatusActivity
import io.github.tonnyl.latticify.util.AccessTokenManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mChannelsFragment: ChannelsFragment
    private lateinit var mImsFragment: IMsFragment
    private lateinit var mGroupsFragment: GroupsFragment
    private lateinit var mStarredItemsFragment: StarredItemsFragment
    private lateinit var mDirectoryFragment: DirectoryFragment

    private val mFragments = mutableListOf<Fragment>()

    private var checkedItemId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initViews()

        fab.setOnClickListener {
            when (checkedItemId) {
                R.id.nav_direct_messages -> {

                }
                R.id.nav_channels -> {
                    startActivity(Intent(this, AddChannelActivity::class.java),
                            ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
                }
                R.id.nav_groups -> {

                }
                R.id.nav_directory -> {

                }
            }
        }

        navView.getHeaderView(0).accountActionImageView.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
        }

        getMyInfo()

        getTeamInfo()

    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
            }
            R.id.action_set_status -> {
                startActivity(Intent(this, SetStatusActivity::class.java),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
            }
            R.id.action_invite_members_to_team -> {
                startActivity(Intent(this, InviteActivity::class.java),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
            }
            R.id.action_snooze -> {
                showSnoozeNotificationsDialog()
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_starred_items) {
            fab.hide()
        } else {
            fab.show()
        }
        when (item.itemId) {
            R.id.nav_direct_messages -> {
                showFragmentAndHideRest(mImsFragment)
                titleTextView.text = getString(R.string.nav_messages)
                checkedItemId = R.id.nav_direct_messages
            }
            R.id.nav_channels -> {
                showFragmentAndHideRest(mChannelsFragment)
                titleTextView.text = getString(R.string.nav_channels)
                checkedItemId = R.id.nav_channels
            }
            R.id.nav_groups -> {
                showFragmentAndHideRest(mGroupsFragment)
                titleTextView.text = getString(R.string.nav_groups)
                checkedItemId = R.id.nav_groups
            }
            R.id.nav_directory -> {
                showFragmentAndHideRest(mDirectoryFragment)
                titleTextView.text = getString(R.string.directory)
                checkedItemId = R.id.nav_directory
            }
            R.id.nav_starred_items -> {
                showFragmentAndHideRest(mStarredItemsFragment)
                titleTextView.text = getString(R.string.nav_starred)
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
            }
            R.id.nav_about -> {
                startActivity(Intent(this, AboutActivity::class.java),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun initViews() {
        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
        navView.setCheckedItem(R.id.nav_direct_messages)
        checkedItemId = R.id.nav_direct_messages

        mChannelsFragment = ChannelsFragment.newInstance()
        mImsFragment = IMsFragment.newInstance()
        mDirectoryFragment = DirectoryFragment.newInstance()
        mStarredItemsFragment = StarredItemsFragment.newInstance()
        mGroupsFragment = GroupsFragment.newInstance()

        addFragments(mChannelsFragment,
                mImsFragment,
                mGroupsFragment,
                mDirectoryFragment,
                mStarredItemsFragment)

        ChannelsPresenter(mChannelsFragment)
        IMsPresenter(mImsFragment)
        GroupsPresenter(mGroupsFragment)
        DirectoryPresenter(mDirectoryFragment)
        StarredItemsPresenter(mStarredItemsFragment)

        showFragmentAndHideRest(mImsFragment)

    }

    private fun showFragmentAndHideRest(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .apply { mFragments.forEach { hide(it) } }
                .show(fragment)
                .commit()
    }

    private fun addFragments(vararg fragments: Fragment) {
        fragments.forEach {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, it, it::class.java.simpleName)
                    .commit()
            mFragments.add(it)
        }
    }

    private fun getMyInfo() {
        AccessTokenManager.getAccessToken().userId?.let {
            UsersRepository.info(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ userWrapper ->
                        if (userWrapper.ok) {
                            GlideLoader.loadAvatar(userAvatarImageView, userWrapper.user.profile.image192)

                            userAvatarImageView.setOnClickListener {
                                startActivity(Intent(this, ProfileActivity::class.java).apply { putExtra(ProfilePresenter.KEY_EXTRA_USER, userWrapper.user) },
                                        ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity).toBundle())
                            }
                        }
                    }, {

                    })
        }
    }

    private fun getTeamInfo() {
        TeamRepository.info()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.ok) {
                        with(it.team) {
                            GlideLoader.loadAvatar(avatarImageView, icon.image132)

                            teamNameTextView.text = name
                            teamUrlTextView.text = getString(R.string.team_url).format(domain)
                        }
                    }
                }, {
                    it.printStackTrace()
                })

    }

    private fun showSnoozeNotificationsDialog() {

    }

}
