package com.almoturg.sprog.presenter;

import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.model.ParentComment;
import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.view.PoemActivity;
import com.almoturg.sprog.model.SprogDbHelper;
import com.almoturg.sprog.util.Util;

import static com.almoturg.sprog.SprogApplication.filtered_poems;

public class PoemPresenter {
    private SprogDbHelper dbHelper;
    private MarkdownConverter markdownConverter;
    private PoemActivity activity;

    private Poem selectedPoem; // The selected poem.
    private Poem mainPoem; // The mainpoem corresponding to the selected one.


    public PoemPresenter(SprogDbHelper dbHelper,
                         MarkdownConverter markdownConverter) {
        this.dbHelper = dbHelper;
        this.markdownConverter = markdownConverter;
    }

    public void attachView(PoemActivity activity, int poem_id) {
        this.activity = activity;
        selectedPoem = filtered_poems.get(poem_id);
        if (selectedPoem.main_poem != null) { // This poem is in the parents of another one
            mainPoem = selectedPoem.main_poem;
        } else {
            mainPoem = selectedPoem;
        }
        CharSequence postContent = null;
        if (mainPoem.post_content != null && mainPoem.post_content.length() > 0){
            postContent = markdownConverter.convertMarkdown(mainPoem.post_content);
        }

        activity.displayPost(markdownConverter.convertMarkdown(mainPoem.post_title),
                mainPoem.post_author, postContent);

        showParents();

        if (mainPoem.content != null && mainPoem.content.length() > 0) {
            boolean is_selected = false;
            if (mainPoem.link.equals(selectedPoem.link)){
                is_selected = true;
            }
            activity.displayMainPoem(mainPoem, is_selected);
        }
    }

    private void showParents() {
        for (ParentComment parent : mainPoem.parents) {
            if (parent.is_poem != null) {
                boolean is_selected = false;
                if (parent.link != null && parent.link.equals(selectedPoem.link)){
                    is_selected = true;}
                activity.displayParentPoem(parent.is_poem, is_selected);
            } else {
                activity.displayParentComment(markdownConverter.convertMarkdown(parent.content),
                        parent.author);
            }
        }
    }

    public void detachView() {
        this.activity = null;
    }

    public String getSelectedPoemID(){
        return Util.last(selectedPoem.link.split("/"));
    }

    public CharSequence getPoemContentString() {
        return markdownConverter.convertPoemMarkdown(selectedPoem.content, selectedPoem.timestamp)
                .toString();
    }

    public void onActionCopy() {
        activity.trackEvent("copy", getSelectedPoemID(), null);
        activity.copyToClipboard(markdownConverter
                .convertPoemMarkdown(selectedPoem.content,selectedPoem.timestamp)
                .toString());

    }

    public void onActionToggleFavorite() {
        selectedPoem.toggleFavorite(dbHelper);
        if (selectedPoem.favorite){
            activity.trackEvent("favorite", Util.last(selectedPoem.link.split("/")), null);
            activity.addedFavorite(selectedPoem);
        } else {
            activity.removedFavorite(selectedPoem);
        }
    }

    public void onActionToReddit(){
        activity.trackEvent("toReddit", getSelectedPoemID(), null);
        activity.openLink(selectedPoem.link + "?context=100");

    }

    public boolean isFavorite() {
        return selectedPoem.favorite;
    }
}
