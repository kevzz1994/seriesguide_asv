package com.battlelancer.seriesguide.ui.people;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.Shadows;
import com.battlelancer.seriesguide.util.Utils;

public class PeopleActivity extends BaseActivity implements PeopleFragment.OnShowPersonListener {

    @Nullable @BindView(R.id.viewPeopleShadowStart) View shadowPeoplePane;
    @BindView(R.id.containerPeople) View containerPeople;

    private boolean isTwoPane;

    interface InitBundle {
        String MEDIA_TYPE = "media_title";
        String ITEM_TMDB_ID = "item_tmdb_id";
        String PEOPLE_TYPE = "people_type";
    }

    enum MediaType {
        SHOW("SHOW"),
        MOVIE("MOVIE");

        private final String value;

        MediaType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    enum PeopleType {
        CAST("CAST"),
        CREW("CREW");

        private final String value;

        PeopleType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    static final int PEOPLE_LOADER_ID = 100;
    static final int PERSON_LOADER_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people);
        ButterKnife.bind(this);
        setupActionBar();

        // if there is a pane shadow, we are in two pane layout
        isTwoPane = shadowPeoplePane != null;

        if (shadowPeoplePane != null) {
            Shadows.getInstance().setShadowDrawable(this, shadowPeoplePane,
                    GradientDrawable.Orientation.RIGHT_LEFT);
        }

        if (savedInstanceState == null) {
            // check if we should directly show a person
            int personTmdbId = getIntent().getIntExtra(PersonFragment.InitBundle.PERSON_TMDB_ID,
                    -1);
            if (personTmdbId != -1) {
                showPerson(null, personTmdbId);

                // if this is not a dual pane layout, remove ourselves from back stack
                if (!isTwoPane) {
                    finish();
                    return;
                }
            }

            PeopleFragment f = new PeopleFragment();
            f.setArguments(getIntent().getExtras());

            // in two-pane mode, list items should be activated when touched
            if (isTwoPane) {
                f.setActivateOnItemClick();
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.containerPeople, f, "people-list")
                    .commit();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                postponeEnterTransition();
                containerPeople.post(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        // allow the people adapter to repopulate during the next layout pass
                        // before starting the transition animation
                        startPostponedEnterTransition();
                    }
                });
            }
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        PeopleType peopleType = PeopleType.valueOf(
                getIntent().getStringExtra(InitBundle.PEOPLE_TYPE));
        setTitle(peopleType == PeopleType.CAST ? R.string.movie_cast : R.string.movie_crew);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(
                    peopleType == PeopleType.CAST ? R.string.movie_cast : R.string.movie_crew);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showPerson(@Nullable View view, int tmdbId) {
        if (isTwoPane) {
            // show inline
            PersonFragment f = PersonFragment.newInstance(tmdbId);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerPeoplePerson, f)
                    .commit();
        } else {
            // start new activity
            Intent i = new Intent(this, PersonActivity.class);
            i.putExtra(PersonFragment.InitBundle.PERSON_TMDB_ID, tmdbId);

            if (view != null) {
                Utils.startActivityWithTransition(this, i, view,
                        R.string.transitionNamePersonImage);
            } else {
                startActivity(i);
            }
        }
    }
}
