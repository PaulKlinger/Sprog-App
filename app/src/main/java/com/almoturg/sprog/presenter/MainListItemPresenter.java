package com.almoturg.sprog.presenter;

import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.view.PoemsListAdapter;

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

    public void onClick() {
        if (!expanded) {
            expanded = true;
            if (!poem.read) {
                poem.read = true;
                mainPresenter.addNewReadPoem(poem);
            }
            view.expand(mainPresenter.getMarkdownConverter()
                    .convertPoemMarkdown(poem.content, poem.timestamp));
        } else if (mainPresenter.poemsReady()){
            view.startPoemActivity(filtered_poems.indexOf(poem));
        }
    }
}
