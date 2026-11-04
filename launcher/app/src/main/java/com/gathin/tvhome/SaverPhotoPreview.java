package com.gathin.tvhome;
import android.app.Activity;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.widget.*;
import java.util.concurrent.*;
/** One sampled, local gallery image at a time; obsolete decodes never replace the newer focus. */
final class SaverPhotoPreview extends LinearLayout {
    private final Activity a;private final ImageView image;private final TextView title;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private Future<?> loading;
    private boolean closed;private int generation;private Bitmap current;
    SaverPhotoPreview(Activity a){
        super(a);this.a=a;setOrientation(VERTICAL);image=new ImageView(a);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription(a.getString(R.string.visual_photo_preview));addView(image,new LinearLayout.LayoutParams(-1,0,1));title=GlassDialog.text(a,"",10);title.setGravity(Gravity.CENTER);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);addView(title);
    }
    void show(SaverCatalog.Photo photo){
        if(closed||photo==null)return;int token=++generation;title.setText(photo.title);if(loading!=null)loading.cancel(true);
        loading=worker.submit(()->{Bitmap result;try{result=SaverImage.load(a,photo.file,true,512,288);}catch(Exception|OutOfMemoryError e){return;}
            a.runOnUiThread(()->{if(closed||token!=generation){if(result!=null)result.recycle();return;}current=result;image.setImageBitmap(result);});});
    }
    void close(){closed=true;generation++;if(loading!=null)loading.cancel(true);worker.shutdownNow();image.setImageDrawable(null);current=null;}
}
