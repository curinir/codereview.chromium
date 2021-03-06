package com.chrome.codereview;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.chrome.codereview.model.Issue;
import com.chrome.codereview.model.PatchSet;
import com.chrome.codereview.model.PatchSetFile;
import com.chrome.codereview.model.PublishData;
import com.chrome.codereview.model.Reviewer;
import com.chrome.codereview.model.TryBotResult;
import com.chrome.codereview.utils.BaseFragment;
import com.chrome.codereview.utils.CachedLoader;
import com.chrome.codereview.utils.ViewUtils;
import com.google.android.gms.auth.GoogleAuthException;

import org.apache.http.auth.AuthenticationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by sergeyv on 18/4/14.
 */
public class IssueDetailsFragment extends BaseFragment implements DialogInterface.OnClickListener, ExpandableListView.OnChildClickListener {

    public static final String EXTRA_ISSUE_ID = "EXTRA_ISSUE_ID";
    public static final int REQUEST_CODE_DIFF = 1;

    private static final int ISSUE_LOADER_ID = 0;
    private static final int COMMIT_LOADER_ID = 2;
    private static final String PUBLISH_DATA_ARG = "publishData";

    private static class IssueLoader extends CachedLoader<Issue> {

        private int issueId;
        private final PublishData publishData;

        public IssueLoader(Context context, int issueId, PublishData publishData) {
            super(context);
            this.issueId = issueId;
            this.publishData = publishData;
        }

        @Override
        public Issue loadInBackground() {
            Issue issue = null;
            try {
                issue = serverCaller().publishAndReloadIssue(issueId, publishData);
            } catch (GoogleAuthException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticationException e) {
                e.printStackTrace();
            }
            return issue;
        }
    }

    private static class CommitLoader extends CachedLoader<Boolean> {

        private final int issuedId;
        private final int patchSetId;
        private final boolean commit;

        public CommitLoader(Context context, int issuedId, int patchSetId, boolean commit) {
            super(context);
            this.issuedId = issuedId;
            this.patchSetId = patchSetId;
            this.commit = commit;
        }

        @Override
        public Boolean loadInBackground() {
            try {
                serverCaller().checkCQBit(issuedId, patchSetId, commit);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GoogleAuthException e) {
                e.printStackTrace();
            } catch (AuthenticationException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    private LoaderManager.LoaderCallbacks<Issue> issueLoaderCallback = new LoaderManager.LoaderCallbacks<Issue>() {

        @Override
        public Loader<Issue> onCreateLoader(int id, Bundle args) {
            enableMenuButtons(false);
            startProgress();

            PublishData publishData = args != null ? (PublishData) args.getParcelable(PUBLISH_DATA_ARG) : null;
            return new IssueLoader(getActivity(), issueId, publishData);
        }

        @Override
        public void onLoadFinished(Loader<Issue> loader, Issue issue) {
            enableMenuButtons(true);
            IssueDetailsFragment.this.issue = issue;
            issueDetailsAdapter.setIssue(issue);
            stopProgress();
            if (issue == null) {
                return;
            }

            getActivity().getActionBar().setTitle(issue.subject());
            commitItem.setIcon(issue.isInCQ() ? R.drawable.ic_action_stop : R.drawable.ic_action_play);
        }

        @Override
        public void onLoaderReset(Loader<Issue> loader) {
            issue = null;
            issueDetailsAdapter.setIssue(null);
        }
    };

    private LoaderManager.LoaderCallbacks<Boolean> commitLoaderCallback = new LoaderManager.LoaderCallbacks<Boolean>() {
        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            startProgress();
            commitItem.setEnabled(false);
            int patchSetId = issue.patchSets().get(issue.patchSets().size() - 1).id();
            return new CommitLoader(getActivity(), issueId, patchSetId, !issue.isInCQ());
        }

        @Override
        public void onLoadFinished(Loader<Boolean> loader, Boolean data) {
            getLoaderManager().restartLoader(ISSUE_LOADER_ID, null, issueLoaderCallback);
        }

        @Override
        public void onLoaderReset(Loader<Boolean> loader) {

        }
    };

    private int issueId;
    private Issue issue;
    private AlertDialog publishDialog;
    private IssueDetailsAdapter issueDetailsAdapter;
    private MenuItem commitItem;
    private MenuItem publishItem;
    private boolean menuItemState;

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_issue_detail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        issueId = getActivity().getIntent().getIntExtra(EXTRA_ISSUE_ID, -1);
        issueDetailsAdapter = new IssueDetailsAdapter(this);
        View layout = super.onCreateView(inflater, container, savedInstanceState);
        ExpandableListView listView = (ExpandableListView) layout.findViewById(android.R.id.list);
        listView.setOnChildClickListener(this);
        listView.setAdapter(issueDetailsAdapter);
        if (issueId != -1) {
            getActivity().getActionBar().setTitle(getString(R.string.issue) + " " + issueId);
        }
        return layout;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
        if (issueId == -1) {
            issueDetailsAdapter.setIssue(null);
        }
        refresh();
    }

    @Override
    protected void refresh() {
        if (issueId != -1) {
            getLoaderManager().restartLoader(ISSUE_LOADER_ID, new Bundle(), this.issueLoaderCallback);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.issue_detail, menu);
        publishItem = menu.getItem(0);
        commitItem = menu.getItem(1);
        enableMenuButtons(menuItemState);
    }

    private String getTextFromPublishDialog(int id) {
        return ((EditText) publishDialog.findViewById(id)).getText().toString();
    }

    private String getReviewerString(String userText) {
        StringTokenizer tokenizer = new StringTokenizer(userText, ", ");
        List<String> mails = new ArrayList<String>(10);
        Map<String, Reviewer> nameToReviewer = new HashMap<String, Reviewer>();
        for (Reviewer reviewer: issue.reviewers()) {
            nameToReviewer.put(reviewer.name(), reviewer);
        }
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken();
            if (nameToReviewer.containsKey(name)) {
                mails.add(nameToReviewer.get(name).email());
            } else {
                mails.add(name + "@chromium.org");
            }
        }

        return TextUtils.join(", ", mails);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String prefix = dialog.BUTTON_NEUTRAL == which ? "lgtm.\n" : "";
        String message = prefix + getTextFromPublishDialog(R.id.publish_message);
        String subject = getTextFromPublishDialog(R.id.publish_subject);
        String cc = getTextFromPublishDialog(R.id.publish_cc);
        String reviewers = getReviewerString(getTextFromPublishDialog(R.id.publish_reviewers));
        PublishData publishData = new PublishData(issueId, message, subject, cc, reviewers);
        Bundle bundle = new Bundle();
        bundle.putParcelable(PUBLISH_DATA_ARG, publishData);
        getLoaderManager().restartLoader(ISSUE_LOADER_ID, bundle, this.issueLoaderCallback);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        Object patchSetObject = issueDetailsAdapter.getGroup(groupPosition);
        if (!(patchSetObject instanceof PatchSet)) {
            return false;
        }
        PatchSet patchSet = (PatchSet) patchSetObject;
        PatchSetFile file = (PatchSetFile) issueDetailsAdapter.getChild(groupPosition, childPosition);
        if (file != null) {
            DiffActivity.startDiffActivity(this, REQUEST_CODE_DIFF, issueId, patchSet, file.id());
        } else {
            showTryBotResultsDialog(patchSet);
        }
        return true;
    }

    public void showPublishDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.publish_action);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View publishView = inflater.inflate(R.layout.publish_dialog, null);
        builder.setView(publishView);
        if (issue != null) {
            ViewUtils.setText(publishView, R.id.publish_subject, issue.subject());
            publishView.findViewById(R.id.publish_message).requestFocus();
            ViewUtils.setText(publishView, R.id.publish_reviewers, TextUtils.join(", ", issue.reviewers()));
            ViewUtils.setText(publishView, R.id.publish_cc, issue.ccdString());
        }
        builder.setPositiveButton(R.string.publish_action, this);
        builder.setNeutralButton(R.string.quick_lgtm, this);
        builder.setNegativeButton(android.R.string.cancel, null);
        publishDialog = builder.create();
        publishDialog.show();
    }

    public void showTryBotResultsDialog(PatchSet patchSet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final TryBotsResultsAdapter adapter = new TryBotsResultsAdapter(getActivity(), patchSet);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TryBotResult result = adapter.getItem(which);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(result.url()));
                startActivity(intent);
            }
        });
        builder.setTitle(getActivity().getString(R.string.try_bots_dialog_title));
        builder.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_publish:
                showPublishDialog();
                return true;
            case R.id.action_commit:
                if (issue != null) {
                    getLoaderManager().restartLoader(COMMIT_LOADER_ID, null, commitLoaderCallback);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.fail_to_commit), Toast.LENGTH_LONG).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == DiffFragment.RESULT_REFRESH) {
            getLoaderManager().restartLoader(ISSUE_LOADER_ID, null, issueLoaderCallback);
        }
    }

    private void enableMenuButtons(boolean enabled) {
        menuItemState = enabled;
        if (publishItem == null || commitItem == null) {
            return;
        }
        publishItem.setEnabled(enabled);
        commitItem.setEnabled(enabled);
    }
}

