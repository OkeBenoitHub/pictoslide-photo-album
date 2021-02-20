package com.pictoslide.www.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.balysv.materialripple.MaterialRippleLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pictoslide.www.R;
import com.pictoslide.www.activities.AlbumActivity;
import com.pictoslide.www.activities.AlbumViewerActivity;
import com.pictoslide.www.models.Album;
import com.pictoslide.www.utils.AppConstUtils;
import com.pictoslide.www.utils.MainUtil;
import com.pictoslide.www.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.pictoslide.www.activities.AlbumActivity.ALBUM_ID_EXTRA;
import static com.pictoslide.www.activities.AlbumActivity.MUSIC_DATA_LIST_PREFS;
import static com.pictoslide.www.activities.AlbumActivity.PHOTO_ITEMS_LIST_PREFS;
import static com.pictoslide.www.activities.AlbumViewerActivity.ALBUM_SUB_OWNERS_ID_EXTRA;
import static com.pictoslide.www.activities.AlbumViewerActivity.OWNER_ALBUM_ID_EXTRA;
import static com.pictoslide.www.activities.MainActivity.IS_NEW_ALBUM_EXTRA;

public class AlbumLibAdapter extends RecyclerView.Adapter<AlbumLibAdapter.AlbumLibViewHolder> {
    private ArrayList<Album> mAlbumItems;
    private final AlbumLibAdapter.ListItemClickListener mOnClickListener;
    private final Context context;
    private final NetworkUtil mNetworkUtil;
    private final MainUtil mMainUtil;
    private final FirebaseFirestore mFirebaseFirestore;
    private final ProgressDialog mLoaderDialog;

    public AlbumLibAdapter(Context mContext, ArrayList<Album> mAlbumItems, AlbumLibAdapter.ListItemClickListener mOnClickListener) {
        this.mAlbumItems = mAlbumItems;
        this.mOnClickListener = mOnClickListener;
        this.context = mContext;
        DateTimeUtils.setTimeZone("UTC");
        mNetworkUtil = new NetworkUtil(this.context);
        mMainUtil = new MainUtil(this.context);
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        // show progress dialog
        mLoaderDialog = new ProgressDialog(this.context,R.style.MyAlertDialogStyle);
    }

    /**
     * The interface that receives onClick messages.
     */
    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex);
        void onShareAlbumItem(int clickedItemIndex);
    }

    @NonNull
    @Override
    public AlbumLibViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.album_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new AlbumLibAdapter.AlbumLibViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumLibViewHolder holder, int position) {
        final Album album = mAlbumItems.get(position);
        holder.mAlbumName.setText(album.getName());
        String timeAgoStr = MainUtil.getTimeAgo(Long.decode(album.getCreatedAtTime()),context);
        holder.mAlbumCreatedDate.setText(timeAgoStr);
        String categorieText = getAlbumsCategories()[album.getCategory()];
        holder.mAlbumCategorie.setText(categorieText);

        List<String> albumPhotoPathList = album.getPhotoPathsList();
        Random random = new Random();
        String thumbnail = albumPhotoPathList.get(random.nextInt(albumPhotoPathList.size()));
        String[] photoData = thumbnail.split("\\|",-1);
        thumbnail = photoData[0];

        // set music youtube thumbnail
        Glide.with(context)
                .load(thumbnail)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(new ColorDrawable(Color.GRAY))
                .into(holder.mAlbumThumbnail);
    }

    @Override
    public int getItemCount() {
        if (mAlbumItems != null) {
            return mAlbumItems.size();
        }
        return 0;
    }

    public class AlbumLibViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mAlbumName;
        private final TextView mAlbumCreatedDate;
        private final TextView mAlbumCategorie;
        private final CircleImageView mAlbumThumbnail;
        //private final ProgressBar mAlbumLoaderBar;

        public AlbumLibViewHolder(@NonNull View itemView) {
            super(itemView);
            mAlbumName = itemView.findViewById(R.id.album_name);
            mAlbumCreatedDate = itemView.findViewById(R.id.album_created_date);
            mAlbumCategorie = itemView.findViewById(R.id.album_category);
            mAlbumThumbnail = itemView.findViewById(R.id.album_thumbnail);
            //mAlbumLoaderBar = itemView.findViewById(R.id.loader_bar_album);
            MaterialRippleLayout comboDropButton = itemView.findViewById(R.id.comboDropButton);
            comboDropButton.setOnClickListener(view -> {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(context,view);
                //Inflating the Popup using xml file
                popup.getMenuInflater()
                        .inflate(R.menu.popup_album_action, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.view_album) {
                         goToAlbumViewerActivity(getAdapterPosition());
                    } else if (item.getItemId() == R.id.share_album) {
                        int elementId = getAdapterPosition();
                        mOnClickListener.onShareAlbumItem(elementId);
                    } else if (item.getItemId() == R.id.delete_album) {
                        removeAlbumFromLibraryByPosition(getAdapterPosition(), shouldRemoveAlbumFromLibrary -> {});
                    }
                    popup.dismiss();
                    return true;
                });

                popup.show(); //showing popup menu
            });
            MaterialRippleLayout albumItemLayout = itemView.findViewById(R.id.album_item_layout);
            albumItemLayout.setOnClickListener(this);
        }

        private void goToAlbumViewerActivity(int position) {
            final Album album = mAlbumItems.get(position);
            Intent intent = new Intent(context, AlbumViewerActivity.class);
            intent.putExtra(AlbumActivity.ALBUM_NAME_EXTRA,album.getName());
            intent.putExtra(AlbumActivity.ALBUM_CATEGORY_EXTRA,album.getCategory());
            intent.putExtra(AlbumActivity.ALBUM_DESCRIPTION_EXTRA,album.getDescription());
            ArrayList<String> photoAbsolutePathsListData = (ArrayList<String>) album.getPhotoPathsList();
            // pass photo data
            intent.putExtra(PHOTO_ITEMS_LIST_PREFS,photoAbsolutePathsListData);
            ArrayList<String> musicIdsDataListData = (ArrayList<String>) album.getMusicIdsList();
            // pass music data data
            intent.putExtra(MUSIC_DATA_LIST_PREFS,musicIdsDataListData);
            ArrayList<String> albumSubOwnersId = (ArrayList<String>) album.getSubOwnersId();
            // pass sub owners id data
            intent.putExtra(ALBUM_SUB_OWNERS_ID_EXTRA,albumSubOwnersId);
            intent.putExtra(IS_NEW_ALBUM_EXTRA,true);
            intent.putExtra(OWNER_ALBUM_ID_EXTRA,album.getOwnerId());
            intent.putExtra(ALBUM_ID_EXTRA,album.getAlbumId());
            context.startActivity(intent);
        }

        @Override
        public void onClick(View view) {
            int elementId = getAdapterPosition();
            mOnClickListener.onListItemClick(elementId);
        }
    }

    private void removeAlbumFromLibrary(String albumId) {
        // make sure we have some kind of internet
        if (!mNetworkUtil.isUserConnectedToNetwork()) {
            mLoaderDialog.dismiss();
            mMainUtil.showToastMessage(this.context.getString(R.string.no_internet_connection));
            return;
        }
        DocumentReference docRef = mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME).document(albumId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    // album does not exist anymore
                    mMainUtil.showToastMessage(this.context.getString(R.string.owner_deleted_album_text));
                } else {
                    // album still exists
                    Album albumItem = document.toObject(Album.class);
                    String currentUserId = mMainUtil.getUniqueID(this.context);
                    if (albumItem != null) {
                        List<String> photoAbsolutePathsList = albumItem.getPhotoPathsList();
                        List<String> photoPathsList = new ArrayList<>();
                        if (photoAbsolutePathsList.size() > 0) {
                            for (int j = 0; j < photoAbsolutePathsList.size(); j++) {
                                String[] photoData = photoAbsolutePathsList.get(j).split("\\|", -1);
                                photoPathsList.add(photoData[0]);
                            }
                        }
                        if (currentUserId.equals(albumItem.getOwnerId())) {
                            // remove my own album from library
                            deleteAlbumFromLibrary(albumId,photoPathsList);
                        } else {
                            // remove somebody else album from library
                            List<String> albumSubOwnersId = albumItem.getSubOwnersId();
                            if (albumSubOwnersId != null) {
                                albumSubOwnersId.remove(currentUserId); // remove current user as sub owner of album
                                DocumentReference albumOwnerRef = mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME).document(albumId);
                                albumOwnerRef
                                        .update("subOwnersId", albumSubOwnersId)
                                        .addOnSuccessListener(aVoid -> mMainUtil.showToastMessage(this.context.getString(R.string.album_removed_from_library_text)))
                                        .addOnFailureListener(e -> mMainUtil.showToastMessage(this.context.getString(R.string.failed_remove_album_library)));
                            }
                        }
                    }
                }
            } else {
                mMainUtil.showToastMessage(this.context.getString(R.string.owner_deleted_album_text));
            }
        });
    }

    private void deleteAlbumFromLibrary(String albumId,List<String> photoPathsList) {
        if (albumId != null) {
            // remove my own album from library
            mFirebaseFirestore.collection(AppConstUtils.ALBUMS_COLLECTION_NAME).document(albumId)
                    .delete()
                    .addOnSuccessListener(aVoid -> deletePhotosFromAlbum(albumId,photoPathsList))
                    .addOnFailureListener(e -> {
                        //Log.w(TAG, "Error deleting document", e);
                        mMainUtil.showToastMessage(this.context.getString(R.string.failed_remove_album_library));
                    });
        }
    }

    private void deletePhotosFromAlbum(String albumId,List<String> photoPathsList) {
        if (photoPathsList != null) {
            FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
            StorageReference storageReference = firebaseStorage.getReference();
            String currentUserId = mMainUtil.getUniqueID(this.context);
            if (photoPathsList.size() > 0) {
                for (int i = 0; i < photoPathsList.size(); i++) {
                    String photoFullPathFromAlbum = photoPathsList.get(i);
                    String photoRefNameFromAlbum = photoFullPathFromAlbum.substring(photoFullPathFromAlbum.indexOf("JPEG"), photoFullPathFromAlbum.indexOf(".jpg")) + ".jpg";
                    StorageReference photoFromAlbumRef = storageReference.child(currentUserId + "/albums/" + albumId + "/" + photoRefNameFromAlbum);
                    int finalI1 = i;
                    photoFromAlbumRef.delete().addOnSuccessListener(aVoid -> {
                        // File deleted successfully
                        if (finalI1 == photoPathsList.size() - 1) {
                            mLoaderDialog.dismiss();
                            mMainUtil.showToastMessage(this.context.getString(R.string.album_removed_from_library_text));
                        }
                    }).addOnFailureListener(exception -> {
                        // Uh-oh, an error occurred!
                        mLoaderDialog.dismiss();
                        mMainUtil.showToastMessage(this.context.getString(R.string.failed_delete_photos_from_album));
                    });
                }
            }
        }
    }

    public interface onAlertBeforeRemoveAlbumFromLibraryCallback {
        void shouldRemoveAlbumFromLibrary(boolean shouldRemoveAlbumFromLibrary);
    }
    private void alertBeforeRemoveAlbumFromLibrary(String albumId, onAlertBeforeRemoveAlbumFromLibraryCallback onAlertBeforeRemoveAlbumFromLibraryCallback) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context,R.style.Theme_MaterialComponents_DayNight_Dialog_Alert);

        // Set a title for alert dialog
        builder.setTitle(R.string.save_album_title_text);
        builder.setCancelable(false);

        // Ask the final question
        builder.setMessage(R.string.remove_album_from_library_text);
        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            onAlertBeforeRemoveAlbumFromLibraryCallback.shouldRemoveAlbumFromLibrary(true);
            // Do something when user clicked the Yes button
            mLoaderDialog.setMessage(this.context.getString(R.string.removing_album_from_library));
            mLoaderDialog.setCancelable(true);
            mLoaderDialog.setCanceledOnTouchOutside(true);
            mLoaderDialog.show();
            removeAlbumFromLibrary(albumId);
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
            onAlertBeforeRemoveAlbumFromLibraryCallback.shouldRemoveAlbumFromLibrary(false);
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    public String[] getAlbumsCategories() {
        return new String[]{
                context.getString(R.string.album_category_head_text),
                context.getString(R.string.at_home_text),
                context.getString(R.string.At_work_text),
                context.getString(R.string.at_gym_text),
                context.getString(R.string.beach_text),
                context.getString(R.string.birthday_text),
                context.getString(R.string.party_text),
                context.getString(R.string.restaurant_text),
                context.getString(R.string.night_life_text),
                context.getString(R.string.travel_text),
                context.getString(R.string.holiday_text),
                context.getString(R.string.funeral_text),
                context.getString(R.string.family_members_text),
                context.getString(R.string.girlfriend_text),
                context.getString(R.string.boyfriend),
                context.getString(R.string.wedding_text),
                context.getString(R.string.other_text)
        };
    }

    public void swapAlbums(ArrayList<Album> albumItems) {
        // assign the passed-in albums list data to the corresponding class field
        mAlbumItems = albumItems;
        notifyDataSetChanged();
    }

    public void removeAlbumFromLibraryByPosition(int albumPosition, onAlertBeforeRemoveAlbumFromLibraryCallback onAlertBeforeRemoveAlbumFromLibraryCallback) {
        if (!mNetworkUtil.isUserConnectedToNetwork()) {
            mMainUtil.showToastMessage(context.getString(R.string.no_internet_connection));
            return;
        }
        // delete album
        alertBeforeRemoveAlbumFromLibrary(mAlbumItems.get(albumPosition).getAlbumId(),onAlertBeforeRemoveAlbumFromLibraryCallback);
    }
}
