package com.pictoslide.www.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pictoslide.www.R;
import com.pictoslide.www.models.Music;

import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    private final ArrayList<Music> mMusicItems;
    private final ListItemClickListener mOnClickListener;
    private final Context mContext;

    public MusicAdapter(Context mContext, ArrayList<Music> mMusicItems, ListItemClickListener mOnClickListener) {
        this.mMusicItems = mMusicItems;
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
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.music_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        final Music music = mMusicItems.get(position);
        // set music title
        holder.mMusicTitle.setText(music.getTitle());
        // set music year
        holder.mMusicPublishedDate.setText(music.getPublishedAt());

        // set music youtube thumbnail
        Glide.with(mContext)
                .load(music.getThumbnailPath())
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(new ColorDrawable(Color.GRAY))
                .into(holder.mMusicThumbnail);
    }

    @Override
    public int getItemCount() {
        if (mMusicItems != null) {
            return mMusicItems.size();
        }
        return 0;
    }

    public class MusicViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView mMusicThumbnail;
        private final TextView mMusicTitle;
        private final TextView mMusicPublishedDate;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            mMusicThumbnail = itemView.findViewById(R.id.music_thumbnail);
            mMusicTitle = itemView.findViewById(R.id.music_title);
            mMusicPublishedDate = itemView.findViewById(R.id.music_publish_date);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null)
                mOnClickListener.onListItemClick(getAdapterPosition());
        }
    }
}
