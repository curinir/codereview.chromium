package com.chrome.codereview;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.chrome.codereview.model.Issue;
import com.chrome.codereview.model.UserIssues;
import com.chrome.codereview.phone.IssueDetailActivity;
import com.chrome.codereview.requests.ServerCaller;
import com.chrome.codereview.utils.BaseListFragment;
import com.chrome.codereview.utils.CachedLoader;

/**
 * Created by sergeyv on 13/4/14.
 */
public class UserIssuesFragment extends BaseListFragment implements LoaderManager.LoaderCallbacks<UserIssues> {

    private static class IssuesLoader extends CachedLoader<UserIssues> {

        private final String userName;

        public IssuesLoader(Context context, String userName) {
            super(context);
            this.userName = userName;
        }

        @Override
        public UserIssues loadInBackground() {
            return ServerCaller.from(getContext()).loadIssuesForUser(userName);
        }
    }

    private UserIssuesAdapter issuesAdapter;

    @Override
    public Loader<UserIssues> onCreateLoader(int i, Bundle bundle) {
        startProgress();
        return new IssuesLoader(this.getActivity(), ServerCaller.from(getActivity()).getAccountName());
    }

    @Override
    public void onLoadFinished(Loader<UserIssues> listLoader, UserIssues issues) {
        issuesAdapter.setUserIssues(issues);
        stopProgress();
        setListAdapter(issuesAdapter);
    }

    @Override
    public void onLoaderReset(Loader<UserIssues> listLoader) {
        issuesAdapter.setUserIssues(null);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_user_issues;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = super.onCreateView(inflater, container, savedInstanceState);
        issuesAdapter = new UserIssuesAdapter(getActivity());
        getLoaderManager().initLoader(0, new Bundle(), this);
        return layout;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Issue issue = issuesAdapter.getItem(position);
        if (issue == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), IssueDetailActivity.class);
        intent.putExtra(IssueDetailsFragment.EXTRA_ISSUE_ID, issue.id());
        startActivity(intent);
    }
}