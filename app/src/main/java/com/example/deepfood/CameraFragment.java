

package com.example.deepfood;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.legacy.app.FragmentCompat;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Basic fragments for the Camera. */
public class CameraFragment extends Fragment
    implements FragmentCompat.OnRequestPermissionsResultCallback {

    private FoodDB foodDB;

    /** Tag for the {@link Log}. */
  private static final String TAG = "deepFood";

  private static final String FRAGMENT_DIALOG = "dialog";

  private static final String HANDLE_THREAD_NAME = "CameraBackground";

  private static final int PERMISSIONS_REQUEST_CODE = 1;

  private final Object lock = new Object();
  private boolean runClassifier = false;
  private boolean checkedPermissions = false;
  private TextView textView;
    private TextView textView2;
  private FloatingActionButton fab;
  private BottomAppBar bottomAppBar;

  private TFLiteClassifier classifier;
  float x1, x2, y1, y2;

  /** Max preview width that is guaranteed by Camera2 API */
  private static final int MAX_PREVIEW_WIDTH = 1920;

  /** Max preview height that is guaranteed by Camera2 API */
  private static final int MAX_PREVIEW_HEIGHT = 1080;

  /**
   * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
   * TextureView}.
   */
  private final TextureView.SurfaceTextureListener surfaceTextureListener =
      new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
          openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
          configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
          return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
      };

  /** ID of the current {@link CameraDevice}. */
  private String cameraId;

  /** An {@link TView} for camera preview. */
  private TView tView;

  /** A {@link CameraCaptureSession } for camera preview. */
  private CameraCaptureSession cameraCaptureSession;

  /** A reference to the opened {@link CameraDevice}. */
  private CameraDevice cameraDevice;

  /** The {@link Size} of camera preview. */
  private Size size;

  /** {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state. */
  private final CameraDevice.StateCallback stateCallback =
      new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice currentCameraDevice) {
          // This method is called when the camera is opened.  We start camera preview here.
          cameraSemaphoreLock.release();
          cameraDevice = currentCameraDevice;
          cameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice currentCameraDevice) {
          cameraSemaphoreLock.release();
          currentCameraDevice.close();
          cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice currentCameraDevice, int error) {
          cameraSemaphoreLock.release();
          currentCameraDevice.close();
          cameraDevice = null;
          Activity activity = getActivity();
          if (null != activity) {
            activity.finish();
          }
        }
      };

  /** An additional thread for running tasks that shouldn't block the UI. */
  private HandlerThread handlerThread;

  /** A {@link Handler} for running tasks in the background. */
  private Handler handler;

  /** An {@link ImageReader} that handles image capture. */
  private ImageReader imgReader;

  /** {@link CaptureRequest.Builder} for the camera preview */
  private CaptureRequest.Builder previewRequestBuilder;

  /** {@link CaptureRequest} generated by {@link #previewRequestBuilder} */
  private CaptureRequest captureRequest;

  /** A {@link Semaphore} to prevent the app from exiting before closing the camera. */
  private Semaphore cameraSemaphoreLock = new Semaphore(1);

  /** A {@link CameraCaptureSession.CaptureCallback} that handles events related to capture. */
  private CameraCaptureSession.CaptureCallback captureCallback =
      new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull CaptureResult partialResult) {}

        @Override
        public void onCaptureCompleted(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull TotalCaptureResult result) {}
      };

  /**
   * Shows a {@link Toast} on the UI thread for the classification results.
   *
   * @param text The message to show
   */
  private void displayOnUI(final String text) {
      Matcher m = Pattern.compile(":([^:]*):").matcher(text);

      String s = text;
//      s = s.substring(0,s.indexOf(':'));


    final Activity activity = getActivity();
    if (activity != null) {
      activity.runOnUiThread(
          new Runnable() {
            @Override
            public void run() {

              textView.setText(text);

//              heading_list.setText(text.split(":")[0]);
              if(m.find()){
                  textView2.setText(m.group(1));
              }



            }
          });
    }
  }

  /**
   * Resizes image.
   *
   * Attempting to use too large a preview size could  exceed the camera bus' bandwidth limitation,
   * resulting in gorgeous previews but the storage of garbage capture data.
   *
   * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that is
   * at least as large as the respective texture view size, and that is at most as large as the
   * respective max size, and whose aspect ratio matches with the specified value. If such size
   * doesn't exist, choose the largest one that is at most as large as the respective max size, and
   * whose aspect ratio matches with the specified value.
   *
   * @param choices The list of sizes that the camera supports for the intended output class
   * @param textureViewWidth The width of the texture view relative to sensor coordinate
   * @param textureViewHeight The height of the texture view relative to sensor coordinate
   * @param maxWidth The maximum width that can be chosen
   * @param maxHeight The maximum height that can be chosen
   * @param aspectRatio The aspect ratio
   * @return The optimal {@code Size}, or an arbitrary one if none were big enough
   */
  private static Size chooseOptimalSize(
      Size[] choices,
      int textureViewWidth,
      int textureViewHeight,
      int maxWidth,
      int maxHeight,
      Size aspectRatio) {

    // Collect the supported resolutions that are at least as big as the preview Surface
    List<Size> bigEnough = new ArrayList<>();
    // Collect the supported resolutions that are smaller than the preview Surface
    List<Size> notBigEnough = new ArrayList<>();
    int w = aspectRatio.getWidth();
    int h = aspectRatio.getHeight();
    for (Size option : choices) {
      if (option.getWidth() <= maxWidth
          && option.getHeight() <= maxHeight
          && option.getHeight() == option.getWidth() * h / w) {
        if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
          bigEnough.add(option);
        } else {
          notBigEnough.add(option);
        }
      }
    }

    // Pick the smallest of those big enough. If there is no one big enough, pick the
    // largest of those not big enough.
    if (bigEnough.size() > 0) {
      return Collections.min(bigEnough, new CompareSizesByArea());
    } else if (notBigEnough.size() > 0) {
      return Collections.max(notBigEnough, new CompareSizesByArea());
    } else {
      Log.e(TAG, "Couldn't find any suitable preview size");
      return choices[0];
    }
  }

  public static CameraFragment newInstance() {
    return new CameraFragment();
  }

  /** Layout the preview and buttons. */
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.camera_fragment, container, false);
  }

  /** Connect the buttons to their event handler. */
  @Override
  public void onViewCreated(final View view, Bundle savedInstanceState) {
    tView = (TView) view.findViewById(R.id.texture);
    textView = (TextView) view.findViewById(R.id.text);
    textView2 = (TextView) view.findViewById(R.id.text2);
    fab = (FloatingActionButton)view.findViewById(R.id.fab);
    bottomAppBar = (BottomAppBar)view.findViewById(R.id.bar);



    foodDB = new FoodDB(getContext());


//      activity.setOnClickListener(new View.OnClickListener() {
//          @Override
//          public void onClick(View view) {
//              Intent intent = new Intent(getContext(), FoodActivity.class);
//              startActivity(intent);
//          }
//      });
      bottomAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
              switch (item.getItemId()) {
                  case R.id.navigation_list:
                      Intent intent = new Intent(getContext(), FoodActivity.class);
                        startActivity(intent);
                      break;
              }
              return false;
          }
      });

  }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bottom_app_bar,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.navigation_list:
//                Intent intent = new Intent(getContext(), FoodActivity.class);
//                startActivity(intent);
//                return true;
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Load the model and labels. */
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    try {
      classifier = new TFLiteClassifier(getActivity());
    } catch (IOException e) {
      Log.e(TAG, "Failed to initialize an image classifier.");
    }
    startBgThread();
  }


  @Override
  public void onResume() {
    super.onResume();
    startBgThread();

    // When the screen is turned off and turned back on, the SurfaceTexture is already
    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    // the SurfaceTextureListener).
    if (tView.isAvailable()) {
      openCamera(tView.getWidth(), tView.getHeight());
    } else {
      tView.setSurfaceTextureListener(surfaceTextureListener);
    }
  }


  @Override
  public void onPause() {
    closeCamera();
    stopBgThread();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    classifier.close();
    super.onDestroy();
  }

  /**
   * Sets up member variables related to camera.
   *
   * @param width The width of available size for camera preview
   * @param height The height of available size for camera preview
   */
  private void cameraOutputs(int width, int height) {
    Activity activity = getActivity();
    CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    try {
      for (String cameraId : manager.getCameraIdList()) {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
          continue;
        }

        // // For still image captures, we use the largest available size.
        Size largest =
            Collections.max(
                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
        imgReader =
            ImageReader.newInstance(
                largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/ 2);

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        // noinspection ConstantConditions
        /* Orientation of the camera sensor */
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swappedDimensions = false;
        switch (displayRotation) {
          case Surface.ROTATION_0:
          case Surface.ROTATION_180:
            if (sensorOrientation == 90 || sensorOrientation == 270) {
              swappedDimensions = true;
            }
            break;
          case Surface.ROTATION_90:
          case Surface.ROTATION_270:
            if (sensorOrientation == 0 || sensorOrientation == 180) {
              swappedDimensions = true;
            }
            break;
          default:
            Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }

        Point displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
          rotatedPreviewWidth = height;
          rotatedPreviewHeight = width;
          maxPreviewWidth = displaySize.y;
          maxPreviewHeight = displaySize.x;
        }

        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
          maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
          maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        size =
            chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture.class),
                rotatedPreviewWidth,
                rotatedPreviewHeight,
                maxPreviewWidth,
                maxPreviewHeight,
                largest);

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
          tView.setAspectRatio(size.getWidth(), size.getHeight());
        } else {
          tView.setAspectRatio(size.getHeight(), size.getWidth());
        }

        this.cameraId = cameraId;
        return;
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    } catch (NullPointerException e) {
      // Currently an NPE is thrown when the Camera2API is used but not supported on the
      // device this code runs.
      ErrorDialog.newInstance(getString(R.string.camera_error))
          .show(getChildFragmentManager(), FRAGMENT_DIALOG);
    }
  }

  private String[] getRequiredPermissions() {
    Activity activity = getActivity();
    try {
      PackageInfo info =
          activity
              .getPackageManager()
              .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  /** Opens the camera specified by {@link CameraFragment#cameraId}. */
  private void openCamera(int width, int height) {
    if (!checkedPermissions && !allPermissionsGranted()) {
      FragmentCompat.requestPermissions(this, getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
      return;
    } else {
      checkedPermissions = true;
    }
    cameraOutputs(width, height);
    configureTransform(width, height);
    Activity activity = getActivity();
    CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    try {
      if (!cameraSemaphoreLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
        throw new RuntimeException("Time out.");
      }
      cameraManager.openCamera(cameraId, stateCallback, handler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted.", e);
    }
  }

  private boolean allPermissionsGranted() {
    for (String permission : getRequiredPermissions()) {
      if (ContextCompat.checkSelfPermission(getActivity(), permission)
          != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /** Closes the current {@link CameraDevice}. */
  private void closeCamera() {
    try {
      cameraSemaphoreLock.acquire();
      if (null != cameraCaptureSession) {
        cameraCaptureSession.close();
        cameraCaptureSession = null;
      }
      if (null != cameraDevice) {
        cameraDevice.close();
        cameraDevice = null;
      }
      if (null != imgReader) {
        imgReader.close();
        imgReader = null;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted.", e);
    } finally {
      cameraSemaphoreLock.release();
    }
  }

  /** Starts a background thread and its {@link Handler}. */
  private void startBgThread() {
    handlerThread = new HandlerThread(HANDLE_THREAD_NAME);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    synchronized (lock) {
      runClassifier = true;
    }
    handler.post(periodicClassification);
  }

  /** Stops the background thread and its {@link Handler}. */
  private void stopBgThread() {
    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
      synchronized (lock) {
        runClassifier = false;
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /** Takes photos and classify them periodically. */
  private Runnable periodicClassification =
      new Runnable() {
        @Override
        public void run() {
          synchronized (lock) {
            if (runClassifier) {
              frameClassification();
            }
          }
          handler.post(periodicClassification);
        }
      };

  /** Creates a new for camera preview. */
  private void cameraPreview() {
    try {
      SurfaceTexture texture = tView.getSurfaceTexture();
      assert texture != null;

      // We configure the size of default buffer to be the size of camera preview we want.
      texture.setDefaultBufferSize(size.getWidth(), size.getHeight());

      // This is the output Surface we need to start preview.
      Surface surface = new Surface(texture);

      // We set up a CaptureRequest.Builder with the output Surface.
      previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      previewRequestBuilder.addTarget(surface);

      // Here, we create a CameraCaptureSession for camera preview.
      cameraDevice.createCaptureSession(
          Arrays.asList(surface),
          new CameraCaptureSession.StateCallback() {

            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
              // The camera is already closed
              if (null == cameraDevice) {
                return;
              }

              // When the session is ready, we start displaying the preview.
              CameraFragment.this.cameraCaptureSession = cameraCaptureSession;
              try {
                // Auto focus should be continuous for camera preview.
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                // Finally, we start displaying the camera preview.
                captureRequest = previewRequestBuilder.build();
                CameraFragment.this.cameraCaptureSession.setRepeatingRequest(
                        captureRequest, captureCallback, handler);
              } catch (CameraAccessException e) {
                e.printStackTrace();
              }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
              displayOnUI("Failed");
            }
          },
          null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * Configures the necessary transformation to `tView`. This
   * method should be called after the camera preview size is determined in cameraOutputs and
   * also the size of `tView` is fixed.
   *
   * @param viewWidth The width of `tView`
   * @param viewHeight The height of `tView`
   */
  private void configureTransform(int viewWidth, int viewHeight) {
    Activity activity = getActivity();
    if (null == tView || null == size || null == activity) {
      return;
    }
    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    Matrix matrix = new Matrix();
    RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
    RectF bufferRect = new RectF(0, 0, size.getHeight(), size.getWidth());
    float centerX = viewRect.centerX();
    float centerY = viewRect.centerY();
    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
      bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
      matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
      float scale =
          Math.max(
              (float) viewHeight / size.getHeight(),
              (float) viewWidth / size.getWidth());
      matrix.postScale(scale, scale, centerX, centerY);
      matrix.postRotate(90 * (rotation - 2), centerX, centerY);
    } else if (Surface.ROTATION_180 == rotation) {
      matrix.postRotate(180, centerX, centerY);
    }
    tView.setTransform(matrix);
  }

  /** Classifies a frame from the preview stream. */
  private void frameClassification() {
    if (classifier == null || getActivity() == null || cameraDevice == null) {
      displayOnUI("Uninitialized");
      return;
    }
    Bitmap bitmap =
        tView.getBitmap(TFLiteClassifier.DIM_IMG_SIZE_X, TFLiteClassifier.DIM_IMG_SIZE_Y);
    String textToShow = classifier.classify(bitmap);
    bitmap.recycle();
    displayOnUI(textToShow);
      List<String> textViews= new ArrayList<String>();
      fab.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              Food food = new Food(null, textView.getText().toString().split(":")[0], textView2.getText().toString());
              foodDB.insertFood(food);
          }
      });


  }

  /** Compares two {@code Size}s based on their areas. */
  private static class CompareSizesByArea implements Comparator<Size> {

    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow
      return Long.signum(
          (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
  }

  /** Shows an error message dialog. */
  public static class ErrorDialog extends DialogFragment {

    private static final String ARG_MESSAGE = "message";

    public static ErrorDialog newInstance(String message) {
      ErrorDialog dialog = new ErrorDialog();
      Bundle args = new Bundle();
      args.putString(ARG_MESSAGE, message);
      dialog.setArguments(args);
      return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final Activity activity = getActivity();
      return new AlertDialog.Builder(activity)
          .setMessage(getArguments().getString(ARG_MESSAGE))
          .setPositiveButton(
              android.R.string.ok,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  activity.finish();
                }
              })
          .create();
    }
  }
}
