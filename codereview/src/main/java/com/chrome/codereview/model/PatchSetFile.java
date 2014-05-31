package com.chrome.codereview.model;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.List;

/**
 * Created by sergeyv on 21/4/14.
 */
public class PatchSetFile {

    public enum Status {
        ADDED("A"),
        MODIFIED("M"),
        DELETED("D"),
        MOVED_MODIFIED("A +") {
            @Override
            public String toString() {
                return "A+";
            }
        };

        private final String text;

        Status(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private final int id;
    private final Status status;
    private final String path;
    private final int numAdded;
    private final int numRemoved;
    private final List<Comment> comments;
    private final int numberOfDrafts;

    public PatchSetFile(int id, Status status, String path, int numAdded, int numRemoved, List<Comment> comments) {
        this.id = id;
        this.status = status;
        this.path = path;
        this.numAdded = numAdded;
        this.numRemoved = numRemoved;
        this.comments = comments;

        int numberOfDrafts = 0;
        for (Comment comment: comments) {
            numberOfDrafts += comment.isDraft() ? 1 : 0;
        }
        this.numberOfDrafts = numberOfDrafts;
    }

    public int id() {
        return id;
    }

    public Status status() {
        return status;
    }

    public String path() {
        return path;
    }

    public int added() {
        return numAdded;
    }

    public int numAdded() {
        return numAdded;
    }

    public int numRemoved() {
        return numRemoved;
    }

    public int numberOfDrafts() {
        return numberOfDrafts;
    }

    public List<Comment> comments() {
        return this.comments;
    }

    public int numberOfComments() {
        return this.comments.size() - numberOfDrafts;
    }

    public static PatchSetFile from(String path, JSONObject metaData) throws JSONException, ParseException {
        int id = metaData.getInt("id");
        int numAdded = metaData.getInt("num_added");
        int numRemoved = metaData.getInt("num_removed");
        String statusString = metaData.getString("status");
        Status status = null;
        for (Status s: Status.values()) {
            if (TextUtils.equals(s.text, statusString)) {
                status = s;
                break;
            }
        }
        if (status == null) {
            throw new IllegalArgumentException("Unknown status: " + statusString);
        }
        List<Comment> comments = Comment.from(metaData.getJSONArray("messages"));
        return new PatchSetFile(id, status, path, numAdded, numRemoved, comments);
    }
}
