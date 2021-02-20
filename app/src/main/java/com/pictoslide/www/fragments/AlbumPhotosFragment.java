package com.pictoslide.www.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pictoslide.www.R;
import com.pictoslide.www.activities.PhotoEditorActivity;
import com.pictoslide.www.adapters.PhotosAdapter;
import com.pictoslide.www.models.Photo;
import com.pictoslide.www.utils.GridAutoFitLayoutManager;
import com.pictoslide.www.utils.MainUtil;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class AlbumPhotosFragment extends Fragment implements PhotosAdapter.ListItemClickListener {
    private static final int EDIT_PHOTO_REQUEST_CODE = 200;
    private static final int PICK_PHOTO_FROM_PHONE_GALLERY = 300;
    private static final String PHOTO_ITEMS_LIST_EXTRA = "photo_items_list_extra";
    private static final String PHOTO_ITEMS_LIST_PREFS = "photo_items_list_prefs";
    private MainUtil mMainUtil;
    //private PhotoUtil mPhotoUtil;
    private ArrayList<Photo> mPhotoItems;
    private ArrayList<String> mPhotoAbsolutePaths;

    private String currentPhotoPath;
    FloatingActionButton mOpenCameraButton,mOpenPhoneGallery;

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private RecyclerView mPhotoRecyclerView;
    private TextView mEmptyTextView;
    private PhotosAdapter mPhotosAdapter;
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mMainUtil = new MainUtil(requireContext());
        mPhotoItems = new ArrayList<>();
        mPhotoAbsolutePaths = new ArrayList<>();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPhotoItems.size() > 0) {
            outState.putParcelableArrayList(PHOTO_ITEMS_LIST_EXTRA, mPhotoItems);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.album_photos_fragment, viewGroup, false);
        mOpenCameraButton = rootView.findViewById(R.id.open_camera_floating_action_button);
        mOpenCameraButton.setOnClickListener(view -> capturePhoto());
        mOpenPhoneGallery = rootView.findViewById(R.id.open_phone_gallery_action_button);
        mOpenPhoneGallery.setOnClickListener(v -> pickPhotoFromGallery());
        mPhotoRecyclerView = rootView.findViewById(R.id.recycler_view_photos);
        mEmptyTextView = rootView.findViewById(R.id.empty_photos_tv);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(PHOTO_ITEMS_LIST_EXTRA)) {
                mPhotoItems = savedInstanceState.getParcelableArrayList(PHOTO_ITEMS_LIST_EXTRA);
            }
        }
        setPhotosAdapter(mPhotoItems);
        mCoordinatorLayout = rootView.findViewById(R.id.coordinatorLayout);
        return rootView;
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void addPhotoToPhoneGallery(Uri photoUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(photoUri);
        requireContext().sendBroadcast(mediaScanIntent);
    }

    public void capturePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.pictoslide.www.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void pickPhotoFromGallery() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent chooserIntent = Intent.createChooser(getIntent, getString(R.string.choose_photo_from_text));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});
        startActivityForResult(chooserIntent, PICK_PHOTO_FROM_PHONE_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            cropPhoto();
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Intent intent = new Intent(getActivity(), PhotoEditorActivity.class);
                intent.putExtra(PhotoEditorActivity.PHOTO_URI_EDITOR_EXTRA_NAME,resultUri.toString());
                startActivityForResult(intent,EDIT_PHOTO_REQUEST_CODE);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                mMainUtil.showToastMessage(error.getMessage());
            }
        } else if(requestCode == PICK_PHOTO_FROM_PHONE_GALLERY && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImage = data.getData();
                CropImage.activity(selectedImage)
                        .start(requireContext(),this);
            }
        } else if (requestCode == EDIT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            // edit picture result path..
            if (data != null) {
                currentPhotoPath = data.getStringExtra(PhotoEditorActivity.EDITED_PHOTO_PATH_EXTRA);
                //currentPhotoPath = mPhotoUtil.compressPhoto(new File(currentPhotoPath)).getAbsolutePath();
                String currentPhotoCaption = data.getStringExtra(PhotoEditorActivity.PHOTO_CAPTION_EXTRA);
                mPhotoItems.add(new Photo(currentPhotoPath,currentPhotoCaption));
                saveAllAlbumPhotoPathsListToPreferences();
                mMainUtil.displaySnackBarMessage(mCoordinatorLayout,R.string.new_photo_added, Snackbar.LENGTH_LONG);
                addPhotoToPhoneGallery(Uri.fromFile(new File(currentPhotoPath)));
                mPhotosAdapter.swapPhotos(mPhotoItems);
                if (mPhotoItems.size() <= 0) {
                    showEmptyNoPhotoAddedView();
                } else {
                    hideEmptyNoPhotoAddedView();
                }
            }
        }
    }

    private void setPhotosAdapter(ArrayList<Photo> photoArrayList) {
        GridAutoFitLayoutManager gridAutoFitLayoutManager = new GridAutoFitLayoutManager(getContext(), 150);
        mPhotoRecyclerView.setLayoutManager(gridAutoFitLayoutManager);
        mPhotoRecyclerView.setHasFixedSize(true);
        mPhotosAdapter = new PhotosAdapter(getContext(), photoArrayList,this);
        mPhotoRecyclerView.setAdapter(mPhotosAdapter);
        if (photoArrayList.size() <= 0) {
            showEmptyNoPhotoAddedView();
        } else {
            hideEmptyNoPhotoAddedView();
        }
    }

    private void showEmptyNoPhotoAddedView() {
        mPhotoRecyclerView.setVisibility(View.INVISIBLE);
        mEmptyTextView.setVisibility(View.VISIBLE);
    }
    private void hideEmptyNoPhotoAddedView() {
        mPhotoRecyclerView.setVisibility(View.VISIBLE);
        mEmptyTextView.setVisibility(View.INVISIBLE);
    }
    public void cropPhoto() {
        if (currentPhotoPath != null) {
            File f = new File(currentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(contentUri)
                    .start(requireContext(), this);
        } else {
            mMainUtil.showToastMessage(getString(R.string.failed_photo));
        }

    }

    public ArrayList<Photo> getPhotoItems() {
        return mPhotoItems;
    }

    public ArrayList<String> getPhotoAbsolutePaths() {
        return mPhotoAbsolutePaths;
    }

    private void setPhotoAbsolutePaths() {
        mPhotoAbsolutePaths = new ArrayList<>();
        if (getPhotoItems().size() > 0) {
            for (int i = 0; i < getPhotoItems().size(); i++) {
                mPhotoAbsolutePaths.add(getPhotoItems().get(i).getAbsolutePath() + "|" + getPhotoItems().get(i).getCaption());
            }
        }
    }

    private void saveAllAlbumPhotoPathsListToPreferences() {
        setPhotoAbsolutePaths();
        mMainUtil.clearPreferenceDataByKey(PHOTO_ITEMS_LIST_PREFS);
        mMainUtil.writeDataArrayListStringToSharedPreferences(PHOTO_ITEMS_LIST_PREFS,getPhotoAbsolutePaths());
    }

    @Override
    public void onListItemClick(int clickedItemIndex) {
        // remove photo from photo array list and apply changes
        if (mPhotoItems != null) {
            if (!mPhotoItems.isEmpty()) {
                mPhotoItems.remove(clickedItemIndex);
                saveAllAlbumPhotoPathsListToPreferences();
                mPhotosAdapter.swapPhotos(mPhotoItems);
                mMainUtil.showToastMessage(getString(R.string.photo_removed_text));
                if (mPhotoItems.size() <= 0) {
                    showEmptyNoPhotoAddedView();
                }
            }
        }
    }
}
