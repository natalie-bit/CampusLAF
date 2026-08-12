package com.reichman.campuslostandfound;

import android.content.Context;

// Resolves an item's stored image name to a drawable resource.
// Falls back to a placeholder if there's no matching bundled image.
public class ItemImages {

    public static int getImageResource(Context context, String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return android.R.drawable.ic_menu_report_image; // placeholder
        }
        // Look up a drawable whose name matches photoUrl (e.g. "pic_bottle")
        int resId = context.getResources()
                .getIdentifier(photoUrl, "drawable", context.getPackageName());
        if (resId != 0) {
            return resId; // found a bundled image
        }
        return android.R.drawable.ic_menu_report_image; // placeholder if not found
    }
}