package ace.io.ocr.receiptocr;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.googlecode.leptonica.android.Binarize;
import com.googlecode.leptonica.android.Convert;
import com.googlecode.leptonica.android.Enhance;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Rotate;
import com.googlecode.leptonica.android.Skew;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class BinaryActivity extends Activity implements OnClickListener {
	Button button_load, button_otsu, button_sauv, button_enhance, button_otsugen, button_to8, button_reset, button_skew;
	ImageView image;
	TessBaseAPI baseapi;
	TextView text_view_skew, text_view_img_src;
	
	private static final int RESULT_LOAD_IMAGE = 1;
	private static final String[] IN_FILES = 
		{	
		"/storage/sdcard0/receipts/01.jpg",
		"/storage/sdcard0/receipts/02.jpg",
		"/storage/sdcard0/receipts/03.jpg",
		"/storage/sdcard0/receipts/04.jpg",
		"/storage/sdcard0/receipts/05.jpg",
		"/storage/sdcard0/receipts/06.jpg",
		};
	//private static final int PICK_FROM_GALLERY = 2; 
	Pix pix = null;//, lastPix = null;
	String image_source;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_binary_testing);
		
		AutoTask task = new AutoTask();
		task.execute(IN_FILES);
		
//		button_load = (Button)findViewById(R.id.button_load_img);
//		button_load.setOnClickListener(this);
//		
//		button_otsu = (Button)findViewById(R.id.button_otsu);
//		button_otsu.setOnClickListener(this);
//		
//		button_sauv = (Button)findViewById(R.id.button_sauvola);
//		button_sauv.setOnClickListener(this);
//		
//		button_enhance = (Button)findViewById(R.id.button_enhance);
//		button_enhance.setOnClickListener(this);
//		
//		button_otsugen = (Button)findViewById(R.id.button_otsu_generic);
//		button_otsugen.setOnClickListener(this);
//		
//		button_to8 = (Button)findViewById(R.id.button_to8);
//		button_to8.setOnClickListener(this);
//		
//		button_reset = (Button)findViewById(R.id.button_reset);
//		button_reset.setOnClickListener(this);
//		
//		button_skew = (Button)findViewById(R.id.button_skew);
//		button_skew.setOnClickListener(this);
//		
//		image = (ImageView)findViewById(R.id.imageView1);
//		
//		text_view_skew = (TextView)findViewById(R.id.textView_skew);
//		text_view_img_src = (TextView)findViewById(R.id.textView_file_src);
//		
//		baseapi = new TessBaseAPI();
//		baseapi.init("/storage/sdcard0/tesseract/", "eng", TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onClick(View v) 
	{
		Pix tempPix = null;
		switch(v.getId())
		{
		case R.id.button_load_img:
			Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, RESULT_LOAD_IMAGE);
			break;
		case R.id.button_otsu:
			tempPix = Binarize.otsuAdaptiveThreshold(pix, pix.getWidth()+10, pix.getHeight()+10, 25, 25, 0);
			break;
		case R.id.button_sauvola:
			tempPix = Binarize.sauvolaBinarizeTiled(pix, 7, .35f, 3, 3);
			break;
		case R.id.button_enhance:
			tempPix = Enhance.unsharpMasking(pix, 7, .5f);
			break;
		case R.id.button_otsu_generic:
			tempPix = Binarize.otsuAdaptiveThreshold(pix);
			break;
		case R.id.button_to8:
			tempPix = Convert.convertTo8(pix);
			break;
		case R.id.button_reset:
			baseapi.setImage(pix);
			Pixa pixa = baseapi.getWords();
			Bitmap bmp = WriteFile.writeBitmap(pix);
			Canvas canvas = new Canvas(bmp);
			try {
				FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory()+"/Android/data/" + getApplicationContext().getPackageName() + "/Files/decode" + ".txt");
			
	//			String[] words = new String[pixa.size()];
				for(int i = 0; i<pixa.size(); i++)
				{
					Rect rect = pixa.getBoxRect(i);
					Paint paint =  new Paint();
					paint.setColor(Color.GREEN);
					canvas.drawLine(rect.left, rect.bottom, rect.left, rect.top, paint);
					canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint);
					canvas.drawLine(rect.right, rect.top, rect.left, rect.top, paint);
					canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, paint);
					baseapi.setRectangle(rect);
					byte[] str = (baseapi.getUTF8Text()+"\n").getBytes();
					out.write(str, 0, str.length);
					//canvas.drawRect(rect,paint);
	//				baseapi.setRectangle(pixa.getBoxRect(i));
	//				words[i] = baseapi.getUTF8Text();
				}
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			image.setImageBitmap(bmp);
			//WriteFile.writeImpliedFormat(pixa.getPix(0), new File("/storage/sdcard0/ocr/images"));
			//v.getContext().get
			//String pathto = Environment.getExternalStorageDirectory()+"/Android/data/" + getApplicationContext().getPackageName() + "/Files";
			//text_view_img_src.setText(pathto);
			//WriteFile.writeFiles(pixa, new File(pathto), "word_", Constants.IFF_BMP);
			pixa.recycle();
			//if(lastPix!=null)
			//tempPix = lastPix.copy();
			break;
		case R.id.button_skew:
			float skew = Skew.findSkew(pix);
			tempPix = Rotate.rotate(pix, skew);
			text_view_skew.setText(""+skew);
			break;
		default:break;
		}
		if(tempPix != null)
		{
			//if(lastPix!=null)
				//lastPix.recycle();
			//if(pix!=null)
			//	lastPix = pix.copy();
				pix.recycle();
			pix = tempPix.copy();
			tempPix.recycle();
			image.setImageBitmap(WriteFile.writeBitmap(pix));
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, 
		       Intent imageReturnedIntent) {
		    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

		    switch(requestCode) { 
		    case RESULT_LOAD_IMAGE:
		        if(resultCode == RESULT_OK && imageReturnedIntent != null){  
		            Uri selectedImage = imageReturnedIntent.getData();
		            String[] filePathColumn = {MediaStore.Images.Media.DATA};

		            Cursor cursor = getContentResolver().query(
		                               selectedImage, filePathColumn, null, null, null);
		            cursor.moveToFirst();

		            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		            image_source = cursor.getString(columnIndex);
		            cursor.close();
		            int rotate = 0;
		            try{
		            	ExifInterface exif = new ExifInterface(image_source);
		            	int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
		            	switch(exifOrientation)
		            	{
		            	case ExifInterface.ORIENTATION_ROTATE_90:
		            		rotate = 90;
		            		break;
		            	case ExifInterface.ORIENTATION_ROTATE_180:
		            		rotate = 180;
		            		break;
		            	case ExifInterface.ORIENTATION_ROTATE_270:
		            		rotate = 270;
		            		break;
		            	}
		            }
		            catch(IOException e)
		            {
		            	
		            }
		            Bitmap imagebm = getScaledImage(2, BitmapFactory.decodeFile(image_source));
		            //image.setImageBitmap(imagebm);
		            
		            pix = ReadFile.readBitmap(imagebm);
		            imagebm.recycle();
		            if(rotate!=0)pix = Rotate.rotate(pix, rotate, true, true);
		            image.setImageBitmap(WriteFile.writeBitmap(pix));
		            
		        }
		    }
		}
	
	Bitmap getScaledImage(int scale, Bitmap image)
	{
		return Bitmap.createScaledBitmap(image, image.getWidth()/scale, image.getHeight()/scale, true);
	}
	
	void handleClick(int id)
	{
		//Pix tempPix;
		
		//tempPix = AdaptiveMap.backgroundNormMorph(pix);
		//tempPix = AdaptiveMap.backgroundNormMorph(pix, 16, 3, 128);
		//tempPix = Binarize.otsuAdaptiveThreshold(pix);
		//tempPix = Binarize.otsuAdaptiveThreshold(pix, pix.getWidth()+10, pix.getHeight()+10, 25, 25, 0);
		//tempPix = Binarize.sauvolaBinarizeTiled(pix, whsize, factor, nx, ny);
	}

}
