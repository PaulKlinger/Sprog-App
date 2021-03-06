package com.almoturg.sprog.presenter;

import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.model.ParentComment;
import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.view.PoemActivity;
import com.almoturg.sprog.model.SprogDbHelper;

import static com.almoturg.sprog.model.Poems.filtered_poems;

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
        if (poem_id >= filtered_poems.size()) {
            // Go back to main activity if the app was cleared from memory
            // (It might make sense to go back to the poem page after loading poems?)
            activity.openMainActivityAndClearBack();
            return;
        }
        selectedPoem = filtered_poems.get(poem_id);

        if (selectedPoem.main_poem != null) { // This poem is in the parents of another one
            mainPoem = selectedPoem.main_poem;
        } else {
            mainPoem = selectedPoem;
        }
        CharSequence postContent = null;
        if (mainPoem.post_content != null && mainPoem.post_content.length() > 0) {
            postContent = markdownConverter.convertMarkdown(mainPoem.post_content);
        }

        activity.displayPost(markdownConverter.convertMarkdown(mainPoem.post_title),
                mainPoem.post_author, postContent, mainPoem.post_url);

        showParents();

        if (mainPoem.content != null && mainPoem.content.length() > 0) {
            boolean is_selected = false;
            if (mainPoem.link.equals(selectedPoem.link)) {
                is_selected = true;
            }
            activity.displayMainPoem(mainPoem, is_selected);
        }
    }

    private void showParents() {
        for (ParentComment parent : mainPoem.parents) {
            if (parent.is_poem != null) {
                boolean is_selected = false;
                if (parent.link != null && parent.link.equals(selectedPoem.link)) {
                    is_selected = true;
                }
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

    public String getSelectedPoemID() {
        return selectedPoem.getId();
    }

    public CharSequence getPoemContentString() {
        if (selectedPoem == null) {
            // See comment in isFavorite().
            return "";
        }
        return markdownConverter.convertPoemMarkdown(selectedPoem.content, selectedPoem.timestamp)
                .toString();
    }

    public void copyPoemText(Poem poem) {
        activity.trackEvent("copy", poem.getId(), null);
        copyText(markdownConverter
                .convertPoemMarkdown(poem.content, poem.timestamp)
                .toString(), "Poem copied to clipboard");
    }

    public void copyText(String text, String toast_text) {
        activity.copyToClipboard(text, toast_text);
    }

    public void onActionCopy() {
        activity.trackEvent("copy", getSelectedPoemID(), null);
        activity.copyToClipboard(markdownConverter
                .convertPoemMarkdown(selectedPoem.content, selectedPoem.timestamp)
                .toString(), "Poem copied to clipboard");

    }

    public void onActionToggleFavorite() {
        selectedPoem.toggleFavorite(dbHelper);
        if (selectedPoem.favorite) {
            activity.trackEvent("favorite", getSelectedPoemID(), null);
            activity.addedFavorite(selectedPoem);
        } else {
            activity.removedFavorite(selectedPoem);
        }
    }

    public void onActionToReddit() {
        activity.trackEvent("toReddit", getSelectedPoemID(), null);
        activity.openLink(selectedPoem.link + "?context=100");

    }

    public boolean isFavorite() {
        if (selectedPoem == null) {
            // This should never be reached because selectedPoem is set in attachView, which is
            // called in PoemActivity.onCreate. It still happened several times though...
            // This might have something to do with the issue mentioned here:
            // https://github.com/JakeWharton/ActionBarSherlock/issues/459
            // i.e. onCreateOptionsMenu is called after finish() (when we determined that the
            // poems haven't been loaded yet and go back to MainActivity).
            // If this is the case just returning false should fix it.
            return false;
        }
        return selectedPoem.favorite;
    }
}
