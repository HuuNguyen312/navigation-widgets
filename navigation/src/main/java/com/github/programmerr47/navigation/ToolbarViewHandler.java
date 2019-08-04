package com.github.programmerr47.navigation;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import androidx.appcompat.view.menu.ListMenuItemView;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.MenuPopupWindow;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import java.lang.reflect.Field;

class ToolbarViewHandler {

  private final Toolbar toolbar;
  private final Handler toolbarHandler;
  private boolean toolbarIsRtl;
  private AbsListView.OnScrollListener onScrollListener;
  private Runnable scrollRunnable;
  private boolean stopped;
  private int mMaxTries;
  private int currentTry;

  private ToolbarViewHandler(Toolbar toolbar, int maxTries) {
    this.mMaxTries = maxTries;
    this.toolbar = toolbar;
    toolbarHandler = new Handler(Looper.getMainLooper());
  }

  static ToolbarViewHandler with(Toolbar toolbar) {
    return with(toolbar, 10);
  }

  static ToolbarViewHandler with(Toolbar toolbar, int maxTries) {
    return new ToolbarViewHandler(toolbar, maxTries);
  }

  @SuppressLint("RestrictedApi")
  ToolbarViewHandler start() {
    toolbar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        } else {
          toolbar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }

        if (ViewCompat.getLayoutDirection(toolbar) == ViewCompat.LAYOUT_DIRECTION_RTL) {
          toolbarIsRtl = true;
        }
      }
    });

    toolbar.setMenuCallbacks(new MenuPresenter.Callback() {
      @Override
      public void onCloseMenu(MenuBuilder menuBuilder, boolean b) {
        stop();
      }

      @Override
      public boolean onOpenSubMenu(MenuBuilder menuBuilder) {
        stopped = false;
        checkViewsMargin();
        return false;
      }
    }, null);

    return this;
  }

  void stop() {
    stopped = true;
    currentTry = 0;
    toolbarHandler.removeCallbacksAndMessages(null);
    scrollRunnable = null;
    removeScrollListener();
  }

  private void removeScrollListener() {
    if (onScrollListener != null) {
      onScrollListener = null;
      ListView listView = getListView();
      if (listView != null) {
        listView.setOnScrollListener(null);
      }
    }
  }

  private void checkViewsMargin() {
    if (currentTry > mMaxTries) {
      stop();
      return;
    }

    if (!stopped) {
      currentTry++;

      toolbarHandler.postDelayed(new Runnable() {
        @SuppressLint("RestrictedApi")
        @Override
        public void run() {
          ListView listView = getListView();

          if (listView != null) {
            onScrollListener = new AbsListView.OnScrollListener() {
              @Override
              public void onScrollStateChanged(AbsListView view, int scrollState) {
              }

              @Override
              public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                popupOnScroll((ListView) view, firstVisibleItem, visibleItemCount, totalItemCount);
              }
            };

            listView.setOnScrollListener(onScrollListener);
          } else {
            checkViewsMargin();
          }
        }
      }, 50);
    }
  }

  @SuppressLint("RestrictedApi")
  private ListView getListView() {
    try {
      Field mMenuViewField = toolbar.getClass().getDeclaredField("mMenuView");
      mMenuViewField.setAccessible(true);

      ActionMenuView actionMenuView = (ActionMenuView) mMenuViewField.get(toolbar);

      Field mPresenterField = actionMenuView.getClass().getDeclaredField("mPresenter");
      mPresenterField.setAccessible(true);
      Object mPresenter = mPresenterField.get(actionMenuView);

      Field mActionButtonPopupField = mPresenter.getClass().getDeclaredField("mActionButtonPopup");
      mActionButtonPopupField.setAccessible(true);
      Field mOverflowPopupField = mPresenter.getClass().getDeclaredField("mOverflowPopup");
      mOverflowPopupField.setAccessible(true);

      MenuPopupHelper menuPopupHelper1 = (MenuPopupHelper) mActionButtonPopupField.get(mPresenter);
      MenuPopupHelper menuPopupHelper2 = (MenuPopupHelper) mOverflowPopupField.get(mPresenter);

      MenuPopupHelper menuPopupHelper = null;

      if (menuPopupHelper1 != null) {
        menuPopupHelper = menuPopupHelper1;
      } else if (menuPopupHelper2 != null) {
        menuPopupHelper = menuPopupHelper2;
      }

      if (menuPopupHelper != null) {
        Field mPopupField = menuPopupHelper.getClass().getSuperclass().getDeclaredField("mPopup");
        mPopupField.setAccessible(true);

        Object mPopup = mPopupField.get(menuPopupHelper);
        Field mPopupField2 = mPopup.getClass().getDeclaredField("mPopup");
        mPopupField2.setAccessible(true);

        MenuPopupWindow mPopup2 = (MenuPopupWindow) mPopupField2.get(mPopup);

//                Field mAdapterField = mPopup2.getClass().getSuperclass().getDeclaredField("mAdapter");
//                mAdapterField.setAccessible(true);

//                Field mDropDownListField = mPopup2.getClass().getSuperclass().getDeclaredField("mDropDownList");
//                mDropDownListField.setAccessible(true);

//                        MenuAdapter menuAdapter = (MenuAdapter) mAdapterField.get(mPopup);

        if (mPopup2 != null) {
          return menuPopupHelper.getListView();
        } else {
          return null;
        }
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }

    return null;
  }

  private void popupOnScroll(final ListView listView, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
    if (scrollRunnable != null) {
      toolbarHandler.removeCallbacks(scrollRunnable);
    }

    scrollRunnable = new Runnable() {
      @Override
      public void run() {
        checkViewsMargin(listView, firstVisibleItem, visibleItemCount, totalItemCount);
        removeScrollRunnable();
      }
    };

    toolbarHandler.postDelayed(scrollRunnable, 50);
  }

  private void removeScrollRunnable() {
    toolbarHandler.removeCallbacks(scrollRunnable);
    scrollRunnable = null;
  }

  private void checkViewsMargin(ListView listView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    ListAdapter adapter = listView.getAdapter();

    if (adapter == null) {
      return;
    }
    for (int i = firstVisibleItem; i < visibleItemCount; ++i) {
      ListMenuItemView child = (ListMenuItemView) getViewByPosition(i, listView);
      if (child == null) {
        return;
      }
      ImageView icon = child.findViewById(androidx.appcompat.R.id.icon);

      if (icon == null) {
        return;
      }
      icon.setAdjustViewBounds(true);
//        int currentHeight = icon.getLayoutParams().height;

      LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, AndroidUtils.dpToPx(20));
      iconLp.gravity = Gravity.CENTER_VERTICAL;

      int marginLeft = (int) (toolbar.getResources().getDisplayMetrics().density * 16);
      int marginRight = 0;
      int margin = (int) (toolbar.getResources().getDisplayMetrics().density * 8);

      if (toolbarIsRtl) {
        // Swap margin left and right
        int marginLeftBeforeChange = marginLeft;
        marginLeft = marginRight;
        marginRight = marginLeftBeforeChange;
      }
      iconLp.setMargins(marginLeft, margin, marginRight, margin);
      icon.setLayoutParams(iconLp);

    }
  }

  private View getViewByPosition(int pos, ListView listView) {
    final int firstListItemPosition = listView.getFirstVisiblePosition();
    final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

    if (pos < firstListItemPosition || pos > lastListItemPosition) {
      return listView.getAdapter().getView(pos, null, listView);
    } else {
      final int childIndex = pos - firstListItemPosition;
      return listView.getChildAt(childIndex);
    }
  }

}
