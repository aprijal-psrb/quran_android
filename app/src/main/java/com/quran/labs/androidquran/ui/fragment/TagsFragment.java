package com.quran.labs.androidquran.ui.fragment;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.data.Constants;
import com.quran.labs.androidquran.database.BookmarksDBAdapter;
import com.quran.labs.androidquran.database.BookmarksDBAdapter.Bookmark;
import com.quran.labs.androidquran.database.BookmarksDBAdapter.Tag;
import com.quran.labs.androidquran.ui.QuranActivity;
import com.quran.labs.androidquran.ui.helpers.BookmarkHandler;
import com.quran.labs.androidquran.ui.helpers.QuranRow;

import android.app.Activity;
import android.support.v4.util.LongSparseArray;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class TagsFragment extends AbsMarkersFragment {
   
   private static final int[] VALID_SORT_OPTIONS = {R.id.sort_alphabetical, R.id.sort_date};

   public static TagsFragment newInstance(){
      return new TagsFragment();
   }
   
   @Override
   protected int getContextualMenuId() {
      return R.menu.tag_menu;
   }
   
   @Override
   protected int getEmptyListStringId() {
      return R.string.tags_list_empty;
   }
   
   @Override
   protected int[] getValidSortOptions() {
      return VALID_SORT_OPTIONS;
   }
   
   @Override
   protected String getSortPref() {
      return Constants.PREF_SORT_TAGS;
   }
   
   @Override
   protected boolean isValidSelection(QuranRow selected) {
      return selected.isBookmark() || (selected.isBookmarkHeader() && selected.tagId >= 0);
   }
   
   @Override
   protected boolean prepareActionMode(ActionMode mode, Menu menu, QuranRow[] selected) {
      MenuItem editItem = menu.findItem(R.id.cab_edit_tag);
      MenuItem removeItem = menu.findItem(R.id.cab_delete_tag);
      MenuItem tagItem = menu.findItem(R.id.cab_tag_bookmark);
      
      int headers = 0;
      int bookmarks = 0;

      for (QuranRow row : selected) {
         if (row.isBookmarkHeader()) {
            headers++;
         } else if (row.isBookmark()) {
            bookmarks++;
         }
      }

      boolean canEdit = headers == 1 && bookmarks == 0;
      boolean canRemove = (headers + bookmarks) > 0;
      boolean canTag = headers == 0 && bookmarks > 0;
      editItem.setVisible(canEdit);
      removeItem.setVisible(canRemove);
      tagItem.setVisible(canTag);
      return true;
   }
   
   @Override
   protected boolean actionItemClicked(ActionMode mode, int menuItemId,
         QuranActivity activity, QuranRow[] selected) {
      switch (menuItemId) {
      case R.id.cab_delete_tag:
         new RemoveBookmarkTask(true).execute(selected);
         return true;
      case R.id.cab_new_tag:
         activity.addTag();
         return true;
      case R.id.cab_edit_tag:
         if (selected.length == 1) {
            activity.editTag(selected[0].tagId, selected[0].text);
         }
         return true;
      case R.id.cab_tag_bookmark:
         long[] ids = new long[selected.length];
         for (int i = 0; i < selected.length; i++) {
            ids[i] = selected[i].bookmarkId;
         }
         activity.tagBookmarks(ids);
         return true;
      default:
         return false;
      }
   }
   
   @Override
   protected QuranRow[] getItems(){
      return getTags();
   }
   
   private QuranRow[] getTags(){
      BookmarksDBAdapter adapter = null;
      Activity activity = getActivity();
      if (activity != null && activity instanceof BookmarkHandler){
         adapter = ((BookmarkHandler) activity).getBookmarksAdapter();
      }

      if (adapter == null){ return null; }

      List<Tag> tags;
      switch (mCurrentSortCriteria) {
      case R.id.sort_date:
         tags = adapter.getTags(BookmarksDBAdapter.SORT_DATE_ADDED);
         break;
      case R.id.sort_alphabetical:
      default:
         tags = adapter.getTags(BookmarksDBAdapter.SORT_ALPHABETICAL);
         break;
      }
      List<Bookmark> bookmarks = adapter.getBookmarks(true);

      List<QuranRow> rows = new ArrayList<>();
      
      List<Bookmark> unTagged = new ArrayList<>();
      LongSparseArray<List<Bookmark>> tagMap =
          new LongSparseArray<>();
      
      for (Bookmark bookmark : bookmarks){
         List<Tag> bookmarkTags = bookmark.mTags;
         if (bookmarkTags == null) {
            unTagged.add(bookmark);
         } else {
            for (Tag tag : bookmarkTags) {
               List <Bookmark> tagBookmarkList = tagMap.get(tag.mId);
               if (tagBookmarkList == null) {
                  List<Bookmark> newList = new ArrayList<>();
                  newList.add(bookmark);
                  tagMap.put(tag.mId, newList);
               } else {
                  tagBookmarkList.add(bookmark);
               }
            }
         }
      }
      
      for (Tag tag : tags) {
         List<Bookmark> tagBookmarkList = tagMap.get(tag.mId);

         // add the tag header
         final QuranRow.Builder builder = new QuranRow.Builder()
             .withType(QuranRow.BOOKMARK_HEADER)
             .withText(tag.mName)
             .withTagId(tag.mId);
         rows.add(builder.build());

         // no bookmarks in this tag, so move on
         if (tagBookmarkList == null || tagBookmarkList.isEmpty()){ continue; }

         // and now the bookmarks
         for (Bookmark bookmark : tagBookmarkList) {
            QuranRow row = createRowFromBookmark(activity, bookmark, tag.mId);
            rows.add(row);
         }
      }
      
      if (unTagged.size() > 0) {
         final QuranRow.Builder header = new QuranRow.Builder()
             .withType(QuranRow.BOOKMARK_HEADER)
             .withText(activity.getString(R.string.not_tagged));
         rows.add(header.build());

         for (Bookmark bookmark : unTagged) {
            QuranRow row = createRowFromBookmark(activity, bookmark);
            rows.add(row);
         }
      }
      
      return rows.toArray(new QuranRow[rows.size()]);
   }
}
