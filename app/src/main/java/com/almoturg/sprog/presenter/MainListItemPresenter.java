package com.almoturg.sprog.presenter;

import android.support.v7.widget.RecyclerView;

import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.ui.PoemsListAdapter;

import static com.almoturg.sprog.SprogApplication.filtered_poems;

public class MainListItemPresenter {
    private Poem poem;
    private PoemsListAdapter.ViewHolder view;
    private boolean expanded;

    private MainPresenter mainPresenter;


    public MainListItemPresenter(PoemsListAdapter.ViewHolder view, MainPresenter mainPresenter) {
        this.view = view;
        this.mainPresenter = mainPresenter;
    }

    public void setPoem(Poem poem){
        this.poem = poem;
        expanded = false;
    }

    public MarkdownConverter getMarkdownConverter() {
        return mainPresenter.markdownConverter;
    }

    public void onClick() {
        if (!expanded) {
            if (!poem.read) {
                poem.read = true;
                mainPresenter.addNewReadPoem(poem);
            }
            view.expand(mainPresenter.markdownConverter
                    .convertPoemMarkdown(poem.content, poem.timestamp));
        } else if (!mainPresenter.updating && !mainPresenter.processing){
            view.startPoemActivity(filtered_poems.indexOf(poem));
        }
    }
}
