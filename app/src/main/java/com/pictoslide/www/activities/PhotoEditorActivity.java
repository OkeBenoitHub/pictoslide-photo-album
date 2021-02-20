package com.pictoslide.www.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.pictoslide.www.R;
import com.pictoslide.www.utils.MainUtil;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;

public class PhotoEditorActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 100;
    private Intent mIntent;
    private MainUtil mMainUtil;
    public static final String PHOTO_URI_EDITOR_EXTRA_NAME = "photoUri";
    private PhotoEditor mPhotoEditor;
    private String m_Text = "";
    private int insertedTextColor;
    public final static String EDITED_PHOTO_PATH_EXTRA = "edited_photo_path_extra";
    public final static String PHOTO_CAPTION_EXTRA = "photo_caption_extra";
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_editor);

        mMainUtil = new MainUtil(this);

        PhotoEditorView mPhotoEditorView = findViewById(R.id.photoEditorView);

        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        int nightModeFlags =
                getApplicationContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;

        boolean isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        if (isNightMode && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        mIntent = getIntent();
        if (mIntent != null) {
            if (mIntent.hasExtra(PHOTO_URI_EDITOR_EXTRA_NAME)) {
                Uri photoIntentUri = Uri.parse(mIntent.getStringExtra(PHOTO_URI_EDITOR_EXTRA_NAME));
                mPhotoEditorView.getSource().setAdjustViewBounds(true);
                mPhotoEditorView.getSource().setScaleType(ImageView.ScaleType.FIT_CENTER);
                mPhotoEditorView.getSource().setImageURI(photoIntentUri);
                // Photo editor
                mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                        .setPinchTextScalable(true)
                        .build();
                setUpPhotoFilterSpinner();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.draw_on_photo) {
            if (mPhotoEditor != null) {
                // draw on picture
                mPhotoEditor.setBrushDrawingMode(true);
                mPhotoEditor.setBrushSize(15.0f);
                mPhotoEditor.setBrushColor(getResources().getColor(R.color.colorPrimary));
                new ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle(getString(R.string.brush_color_dialog_title_text))
                        .setPreferenceName("MyColorPickerDialog")
                        .setPositiveButton(getString(R.string.confirm),
                                (ColorListener) (color, fromUser) -> mPhotoEditor.setBrushColor(color))
                        .setNegativeButton(getString(R.string.cancel),
                                (dialogInterface, i) -> dialogInterface.dismiss())
                        .attachAlphaSlideBar(true) // default is true. If false, do not show the AlphaSlideBar.
                        .attachBrightnessSlideBar(true)  // default is true. If false, do not show the BrightnessSlideBar.
                        .show();
            }
        } else if (item.getItemId() == R.id.eraser) {
            if (mPhotoEditor != null) {
                mPhotoEditor.brushEraser();
            }
        } else if (item.getItemId() == R.id.set_filter) {
            if (mPhotoEditor != null) {
                LinearLayout filterBox = findViewById(R.id.filter_box);
                filterBox.setVisibility(View.VISIBLE);
                Button closeFilterBoxButton = findViewById(R.id.closeFilterBoxButton);
                closeFilterBoxButton.setOnClickListener(v -> filterBox.setVisibility(View.INVISIBLE));
            }
        } else if (item.getItemId() == R.id.insert_text) {
            if (mPhotoEditor != null) {
                new ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle(getString(R.string.specify_text_to_add))
                        .setPreferenceName("MyColorPickerDialog")
                        .setPositiveButton(getString(R.string.confirm),
                                (ColorListener) (color, fromUser) -> {
                                    insertedTextColor = color;
                                    openTextToBeInsertedDialog();
                                })
                        .setNegativeButton(getString(R.string.cancel),
                                (dialogInterface, i) -> dialogInterface.dismiss())
                        .attachAlphaSlideBar(true) // default is true. If false, do not show the AlphaSlideBar.
                        .attachBrightnessSlideBar(true)  // default is true. If false, do not show the BrightnessSlideBar.
                        .show();
            }
        } else if (item.getItemId() == R.id.undo_changes) {
            if (mPhotoEditor != null) {
                mPhotoEditor.undo();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void openTextToBeInsertedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.text_to_insert_text);

        // Set up the input
        final EditText input = new EditText(this);
        input.setSingleLine();
        FrameLayout container = new FrameLayout(getApplicationContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        input.setLayoutParams(params);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        container.addView(input);
        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            m_Text = input.getText().toString().trim();
            if (!m_Text.equalsIgnoreCase("")) {
                input.setError(null);
                mPhotoEditor.addText(m_Text, insertedTextColor);
            } else {
                input.setError("Enter a text");
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setUpPhotoFilterSpinner() {
        Spinner spinner = findViewById(R.id.filter_list);
        String[] filter_list = getFilters();
        final List<String> filters_categories_list = new ArrayList<>(Arrays.asList(filter_list));
        final ArrayAdapter<String> filters_category_adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item, filters_categories_list) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                int nightModeFlags =
                        getApplicationContext().getResources().getConfiguration().uiMode &
                                Configuration.UI_MODE_NIGHT_MASK;

                boolean isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                if (position == 0) {
                    // Set the hint text color gray
                    tv.setTextColor(getResources().getColor(R.color.colorPrimary));
                } else {
                    tv.setTextColor(Color.BLACK);
                    if (isNightMode)
                        tv.setTextColor(Color.WHITE);
                }
                return view;
            }
        };

        filters_category_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(filters_category_adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mPhotoEditor == null) {
                    return;
                }
                switch (position) {
                    case 1:
                        mPhotoEditor.setFilterEffect(PhotoFilter.NONE);
                        break;
                    case 2:
                        mPhotoEditor.setFilterEffect(PhotoFilter.AUTO_FIX);
                        break;
                    case 3:
                        mPhotoEditor.setFilterEffect(PhotoFilter.BLACK_WHITE);
                        break;
                    case 4:
                        mPhotoEditor.setFilterEffect(PhotoFilter.BRIGHTNESS);
                        break;
                    case 5:
                        mPhotoEditor.setFilterEffect(PhotoFilter.CONTRAST);
                        break;
                    case 6:
                        mPhotoEditor.setFilterEffect(PhotoFilter.CROSS_PROCESS);
                        break;
                    case 7:
                        mPhotoEditor.setFilterEffect(PhotoFilter.DOCUMENTARY);
                        break;
                    case 8:
                        mPhotoEditor.setFilterEffect(PhotoFilter.DUE_TONE);
                        break;
                    case 9:
                        mPhotoEditor.setFilterEffect(PhotoFilter.FILL_LIGHT);
                        break;
                    case 10:
                        mPhotoEditor.setFilterEffect(PhotoFilter.FISH_EYE);
                        break;
                    case 11:
                        mPhotoEditor.setFilterEffect(PhotoFilter.FLIP_HORIZONTAL);
                        break;
                    case 12:
                        mPhotoEditor.setFilterEffect(PhotoFilter.FLIP_VERTICAL);
                        break;
                    case 13:
                        mPhotoEditor.setFilterEffect(PhotoFilter.GRAIN);
                        break;
                    case 14:
                        mPhotoEditor.setFilterEffect(PhotoFilter.GRAY_SCALE);
                        break;
                    case 15:
                        mPhotoEditor.setFilterEffect(PhotoFilter.NEGATIVE);
                        break;
                    case 16:
                        mPhotoEditor.setFilterEffect(PhotoFilter.POSTERIZE);
                        break;
                    case 17:
                        mPhotoEditor.setFilterEffect(PhotoFilter.ROTATE);
                        break;
                    case 18:
                        mPhotoEditor.setFilterEffect(PhotoFilter.SATURATE);
                        break;
                    case 19:
                        mPhotoEditor.setFilterEffect(PhotoFilter.SEPIA);
                        break;
                    case 20:
                        mPhotoEditor.setFilterEffect(PhotoFilter.SHARPEN);
                        break;
                    case 21:
                        mPhotoEditor.setFilterEffect(PhotoFilter.TEMPERATURE);
                        break;
                    case 22:
                        mPhotoEditor.setFilterEffect(PhotoFilter.TINT);
                        break;
                    case 23:
                        mPhotoEditor.setFilterEffect(PhotoFilter.VIGNETTE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private String[] getFilters() {
        return new String[]{
                getString(R.string.choose_filter_effect_text),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.no_effect_filter_text).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.auto_fix).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.black_white).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.brightness).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.contrast).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.cross_process).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.documentary).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.due_tone).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.fill_light).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.fish_eye).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.flip_horizontal).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.flip_vertical).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.grain).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.gray_scale).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.negative).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.Rotate).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.Saturate).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.sepia).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.sharpen).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.temperature).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.tint).toLowerCase()),
                mMainUtil.capitalizeEachWordFromString(getString(R.string.vignette).toLowerCase())
        };
    }

    public void saveEditedPhoto(View view) {
        if (mPhotoEditor != null) {
            try {
                filePath = createImageFile();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
                    return;
                }
                mPhotoEditor.saveAsFile(filePath, new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        mIntent = new Intent();
                        mIntent.putExtra(EDITED_PHOTO_PATH_EXTRA, imagePath);
                        EditText photoCaptionEdt = findViewById(R.id.photo_caption);
                        String photoCaptionValue = photoCaptionEdt.getText().toString().trim();
                        if (photoCaptionValue.isEmpty()) photoCaptionValue = "";
                        mIntent.putExtra(PHOTO_CAPTION_EXTRA,photoCaptionValue);
                        setResult(PhotoEditorActivity.RESULT_OK, mIntent);
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        mMainUtil.showToastMessage(getString(R.string.failed_to_save_image_text));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay!
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {

                    mPhotoEditor.saveAsFile(filePath, new PhotoEditor.OnSaveListener() {
                        @Override
                        public void onSuccess(@NonNull String imagePath) {
                            mIntent = new Intent();
                            mIntent.putExtra(EDITED_PHOTO_PATH_EXTRA, imagePath);
                            EditText photoCaptionEdt = findViewById(R.id.photo_caption);
                            String photoCaptionValue = photoCaptionEdt.getText().toString().trim();
                            if (photoCaptionValue.isEmpty()) photoCaptionValue = "";
                            mIntent.putExtra(PHOTO_CAPTION_EXTRA,photoCaptionValue);
                            setResult(PhotoEditorActivity.RESULT_OK, mIntent);
                            finish();
                        }

                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            mMainUtil.showToastMessage(getString(R.string.failed_to_save_image_text));
                        }
                    });
                }

            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                mMainUtil.showToastMessage("Pictoslide needs to access to storage to save photo");
            }
        }
    }

    public String createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        return image.getAbsolutePath();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_photo_editor_menu,menu);
        return true;
    }
}