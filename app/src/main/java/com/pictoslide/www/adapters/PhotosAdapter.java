package com.pictoslide.www.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pictoslide.www.R;
import com.pictoslide.www.models.Photo;

import java.io.File;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder> {
    private ArrayList<Photo> mPhotosItems;
    private final ListItemClickListener mOnClickListener;
    private final Context mContext;

    public PhotosAdapter(Context mContext, ArrayList<Photo> mPhotosItems, ListItemClickListener mOnClickListener) {
        this.mPhotosItems = mPhotosItems;
        this.mOnClickListener = mOnClickListener;
        this.mContext = mContext;
    }

    /**
     * The interface that receives onClick messages.
     */
    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex);
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.photo_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        final Photo photo = mPhotosItems.get(position);

        Glide.with(mContext)
                .load(new File(photo.getAbsolutePath()))
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(new ColorDrawable(Color.GRAY))
                .into(holder.mPhotoThumbnail);
    }

    @Override
    public int getItemCount() {
        if (mPhotosItems != null) {
            return mPhotosItems.size();
        }
        return 0;
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final CircleImageView mPhotoThumbnail;
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            mPhotoThumbnail = itemView.findViewById(R.id.photo_thumbnail);
            ImageView removePhotoButton = itemView.findViewById(R.id.removePhotoButton);
            removePhotoButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) mOnClickListener.onListItemClick(getAdapterPosition());
        }
    }

    public void swapPhotos(ArrayList<Photo> photosItems) {
        // assign the passed-in photos list data to the corresponding class field
        mPhotosItems = photosItems;
        notifyDataSetChanged();
    }
}
