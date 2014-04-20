package com.chrome.codereview;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chrome.codereview.model.Issue;
import com.chrome.codereview.model.Reviewer;
import com.chrome.codereview.model.UserIssues;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by sergeyv on 20/4/14.
 */
class UserIssuesAdapter extends BaseAdapter {

    private static final int TYPE_ISSUE = 0;
    private static final int TYPE_GROUP_HEADER = 1;

    private static final long MINUTE = 60 * 1000;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    private static class Box {

        Issue issue;
        int titleResource;

        private Box(int titleResource) {
            this.titleResource = titleResource;
        }

        private Box(Issue issue) {
            this.issue = issue;
        }

        boolean isBoxIssue() {
            return issue != null;
        }

    }

    private List<Box> boxes = new ArrayList<Box>();

    private final LayoutInflater inflater;
    private final Context context;

    public UserIssuesAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return boxes.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public Issue getItem(int position) {
        Box box = boxes.get(position);
        return box.isBoxIssue() ? box.issue : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setUserIssues(UserIssues userIssues) {
        boxes.clear();
        if (userIssues == null) {
            notifyDataSetChanged();
            return;
        }
        addGroup(R.string.header_incoming_reviews, userIssues.incomingReviews());
        addGroup(R.string.header_outgoing_reviews, userIssues.outgoingReviews());
        addGroup(R.string.header_cced_reviews, userIssues.ccReviews());
        addGroup(R.string.header_recently_closed, userIssues.recentlyClosed());
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return boxes.get(position).isBoxIssue() ? TYPE_ISSUE : TYPE_GROUP_HEADER;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (getItemViewType(position) == TYPE_GROUP_HEADER) {
            return getGroupHeaderView(boxes.get(position).titleResource, convertView, parent);
        }

        return getIssueView(position, convertView, parent);
    }

    private View getIssueView(int position, View convertView, ViewGroup parent) {
        Issue issue = boxes.get(position).issue;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.issue_item, parent, false);
        }
        setText(convertView, R.id.subject, issue.subject());
        setText(convertView, R.id.owner, issue.owner());
        TextView reviewers = (TextView) convertView.findViewById(R.id.reviewers);
        reviewers.setText(reviewersSpannable(issue.reviewers()), TextView.BufferType.SPANNABLE);
        setText(convertView, R.id.modified, createAgoText(issue.lastModified()));

        boolean shouldShowDivider = position + 1 < boxes.size() ? boxes.get(position + 1).isBoxIssue() : false;
        int visibility = shouldShowDivider ? View.VISIBLE : View.GONE;
        convertView.findViewById(R.id.list_divider).setVisibility(visibility);
        return convertView;
    }

    private void setText(View view, int id, String text) {
        TextView textView = (TextView) view.findViewById(id);
        textView.setText(text);
    }

    private View getGroupHeaderView(int titleRes, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_group_header, parent, false);
        }
        TextView titleView = (TextView) convertView.findViewById(android.R.id.text1);
        titleView.setText(titleRes);
        return convertView;
    }

    private void addGroup(int titleRes, List<Issue> issues) {
        if (issues.isEmpty()) {
            return;
        }
        boxes.add(new Box(titleRes));
        for (Issue issue : issues) {
            boxes.add(new Box(issue));
        }
    }

    public static Spannable reviewersSpannable(List<Reviewer> reviewers) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean firstReviewer = true;
        for (Reviewer reviewer : reviewers) {
            if (!firstReviewer) {
                builder.append(", ");
            }
            int start = builder.length();
            int end = builder.length() + reviewer.name().length();
            builder.append(reviewer.name());
            ForegroundColorSpan colorSpan = null;
            switch (reviewer.opinion()) {
                case LGTM:
                    colorSpan = new ForegroundColorSpan(Color.argb(255, 0, 155, 0));
                    break;
                case NOT_LGTM:
                    colorSpan = new ForegroundColorSpan(Color.RED);
                    break;
            }
            if (colorSpan != null) {
                builder.setSpan(colorSpan, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            firstReviewer = false;
        }
        return builder;
    }

    public String createAgoText(Date lastModified) {
        long time = System.currentTimeMillis() - lastModified.getTime();
        long[] times = new long[] {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE};
        int[] resources = new int[] {R.plurals.year, R.plurals.month, R.plurals.week, R.plurals.day, R.plurals.hour, R.plurals.minute};
        int i = 0;
        while (time / times[i] == 0) {
            i++;
        }
        long quantity = time / times[i];
        return quantity + " " + context.getResources().getQuantityString(resources[i], (int) quantity) + " " + context.getString(R.string.ago);
    }
}
