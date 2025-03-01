package com.foobnix.ui2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.DialogsPlaylist;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavoritesFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.starred, R.drawable.glyphicons_50_star);

    FileMetaAdapter recentAdapter;
    ImageView onListGrid;
    View panelRecent;
    String syncronizedBooksTitle;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_starred, container, false);


        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        panelRecent = view.findViewById(R.id.panelRecent);

        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);

        syncronizedBooksTitle = getString(R.string.synchronized_books);


        TxtUtils.underlineTextView(view.findViewById(R.id.clearAllRecent)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialogs.showDialog(getActivity(), getString(R.string.do_you_want_to_clear_everything_), getString(R.string.ok), new Runnable() {

                    @Override
                    public void run() {
                        for (FileMeta f : AppDB.get().getStarsFilesDeprecated()) {
                            f.setIsStar(false);
                            AppDB.get().update(f);
                        }
                        for (FileMeta f : AppDB.get().getStarsFoldersDeprecated()) {
                            f.setIsStar(false);
                            AppDB.get().update(f);
                        }
                        AppData.get().clearFavorites();

                        populate();
                    }
                });

            }
        });

        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        TxtUtils.underlineTextView(view.findViewById(R.id.onPlaylists)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DialogsPlaylist.showPlaylistsDialog(v.getContext(), new Runnable() {

                    @Override
                    public void run() {
                        resetFragment();
                        EventBus.getDefault().post(new NotifyAllFragments());
                    }
                }, null);

            }
        });
        TxtUtils.underlineTextView(view.findViewById(R.id.onTags)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Dialogs.showTagsDialog((FragmentActivity) v.getContext(), null, false, new Runnable() {

                    @Override
                    public void run() {
                        resetFragment();
                        EventBus.getDefault().post(new NotifyAllFragments());
                    }
                });

            }
        });


        recentAdapter.setOnGridOrList(new ResultResponse<ImageView>() {

            @Override
            public boolean onResultRecive(ImageView result) {
                popupMenu(result);
                return false;
            }
        });

        onGridList();

        populate();

        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);

        return view;
    }

    private void popupMenu(final ImageView image) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), image);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, //
                R.string.compact, //
                R.string.grid, //
                R.string.cover//
        );

        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, //
                R.drawable.glyphicons_114_justify_compact, //
                R.drawable.glyphicons_156_show_big_thumbnails, //
                R.drawable.glyphicons_157_show_thumbnails //
        );
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().starsMode = actions.get(index);
                    image.setImageResource(icons.get(index));

                    onGridList();

                    return false;
                }
            });
        }

        p.show();

    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);

    }

    public boolean onBackAction() {
        return false;
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {

        List<FileMeta> all = new ArrayList<FileMeta>();

        List<String> tags = TagData.getAllTagsByFile();
        if (TxtUtils.isListNotEmpty(tags)) {
            for (String tag : tags) {
                if(TxtUtils.isEmpty(tag)){
                    continue;
                }
                FileMeta m = new FileMeta("");
                m.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TAG);
                int count = AppDB.get().getAllWithTag(tag).size();
                m.setPathTxt(tag + " (" + count + ")");
                all.add(m);
            }

            {
                FileMeta empy = new FileMeta();
                empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
                all.add(empy);
            }
        }

        all.addAll(Playlists.getAllPlaylistsMeta());

        {
            FileMeta empy = new FileMeta();
            empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
            all.add(empy);
        }

        all.addAll(AppData.get().getAllFavoriteFolders());


        final List<FileMeta> allFavoriteFiles = AppData.get().getAllFavoriteFiles(true);

        if (TxtUtils.isListNotEmpty(allFavoriteFiles)) {
            FileMeta empy = new FileMeta();
            empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
            all.add(empy);

            all.addAll(allFavoriteFiles);
        }

        final List<FileMeta> allSyncBooks = AppData.get().getAllSyncBooks();
        if (TxtUtils.isListNotEmpty(allSyncBooks)) {

            FileMeta empy = new FileMeta();
            empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
            empy.setTitle(syncronizedBooksTitle);
            all.add(empy);


            all.addAll(allSyncBooks);
        }

        return all;
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        recentAdapter.getItemsList().clear();
        recentAdapter.getItemsList().addAll(items);
        recentAdapter.notifyDataSetChanged();
    }

    public void onGridList() {
        onGridList(AppState.get().starsMode, onListGrid, recentAdapter, null);

    }

    @Override
    public void notifyFragment() {
        populate();
    }

    @Override
    public void resetFragment() {
        onGridList();
        populate();
    }
}
