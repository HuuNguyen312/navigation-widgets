package com.github.programmerr47.navigation;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class AndroidUtils {
    private AndroidUtils() {}

    public static int dpToPx(float dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static float dp(@NonNull Context context, float dp) {
        return applyDimen(context, dp, COMPLEX_UNIT_DIP);
    }

    private static float applyDimen(@NonNull Context context, float value, int unit) {
        Resources res = context.getResources();
        return TypedValue.applyDimension(unit, value, res.getDisplayMetrics());
    }

    @ColorInt
    public static int color(@NonNull Context context, @ColorRes int id) {
        return ContextCompat.getColor(context, id);
    }

    @SuppressWarnings("unchecked")
    public static <T> T bind(View view, int id) {
        return (T) view.findViewById(id);
    }
}
